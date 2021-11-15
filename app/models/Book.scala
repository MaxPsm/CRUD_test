package models

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.MySQLProfile.api._

import com.google.inject.Inject


case class Book(
                 id : Long,
                 title: String,
                 year: Int,
                 authors: Seq[String] = Seq.empty) {

  def updateAuthors(newAuthors: Seq[String]) = {
    this.copy(authors=newAuthors)
  }
}


case class BookTable(tag:Tag) extends Table[Book](tag,"books") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def title = column[String]("title")

  def year = column[Int]("year")

  type Data = (Long, String, Int)

  def constructBook: Data => Book = {
    case (id, title, year) =>
      Book(id, title, year)
  }

  def extractBook: PartialFunction[Book, Data] = {
    case Book(id, title, year,  _) =>
      (id, title, year)
  }

  override def * = (id, title, year) <> (constructBook, extractBook.lift)
}

case class BookFormData(title: String, year: Short, authors:Seq[String])

object BookForm {
  val form = Form(
    mapping(
      "title" -> nonEmptyText,
      "year" -> shortNumber,
      "authors" -> seq(nonEmptyText)
    )(BookFormData.apply)(BookFormData.unapply)
  )
}

trait BookRepo {
  def add(book: Book): Future[String]
  def getAllBooks: Future[Seq[Book]]
  def getBookById(id:Long) : Future[Seq[Book]]
  def update(book: Book): Future[String]
  def delete(id: Long): Future[String]
}

class BooksCommandList @Inject() (
                                   protected val dbConfigProvider: DatabaseConfigProvider
                             )(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with BookRepo {

  var booksList = TableQuery[BookTable]
  var authorsList = TableQuery[AuthorTable]
  var booksAndAuthorsList = TableQuery[BooksAndAuthorsTable]

  def add(book: Book): Future[String] = {
    val knownAuthors = authorsList.filter(_.name.inSet(book.authors)).map(x => (x.id, x.name)).result

    val booksAuthorsId = for {
      x <- db.run(knownAuthors)
      knownNames = x.map(_._2)
      newAuthors = book.authors.filterNot(knownNames.contains(_))
      addNewAuthors = authorsList.map(x => (x.id, x.name)) ++= newAuthors.map((1, _))
      authorQuery = db.run(addNewAuthors)
      _ <- authorQuery
      newAuthorsIds = authorsList.filter(_.name.inSet(newAuthors)).map(x => x.id).result
      booksNewAuthorsId <- db.run(newAuthorsIds)
    } yield booksNewAuthorsId ++ x.map(_._1)

    val bookIdQuery =
      (booksList returning booksList.map(_.id)) += Book(1, book.title, book.year)

    for{
      bookId <- db.run(bookIdQuery)
      authorIDs <- booksAuthorsId
      newAuthorsBook = authorIDs.map((bookId, _))
      _ <- db.run(booksAndAuthorsList.map(x => (x.id_book, x.id_author)) ++= newAuthorsBook)
    } yield "Book successfully added"
  }

  def update(book: Book): Future[String]  ={
    val knownAuthors = authorsList.filter(_.name.inSet(book.authors)).map(x => (x.id, x.name)).result

    val booksAuthorsId = for {
      x <- db.run(knownAuthors)
      knownNames = x.map(_._2)
      newAuthors = book.authors.filterNot(knownNames.contains(_))
      addNewAuthors = authorsList.map(x => (x.id, x.name)) ++= newAuthors.map((1, _))
      authorQuery = db.run(addNewAuthors)
      _ <- authorQuery
      newAuthorsIds = authorsList.filter(_.name.inSet(newAuthors)).map(x => x.id).result
      booksNewAuthorsId <- db.run(newAuthorsIds)
    } yield booksNewAuthorsId ++ x.map(_._1)

    val updatedBook = booksList.filter(_.id === book.id).map(x => (x.title,x.year))
    db.run(updatedBook.update(book.title, book.year))

    db.run(booksAndAuthorsList.filter(_.id_book === book.id).delete)

    for{
      authorIDs <- booksAuthorsId
      newAuthorsBook = authorIDs.map((book.id, _))
      _ <- db.run(booksAndAuthorsList.map(x => (x.id_book, x.id_author)) ++= newAuthorsBook)
    } yield "Book successfully updated"
  }


  def delete(id: Long): Future[String] = {
    val deleteBookAction = booksList.filter(_.id === id).delete
    db.run(deleteBookAction)
    Future.successful("Book successfully deleted")
  }

  private def getBookAuthors(id:Long): Future[Seq[String]] =
    db.run {
      val innerJoin = for {
        (ab, a) <- booksList join booksAndAuthorsList on (_.id === _.id_book) join authorsList on (_._2.id_author === _.id)
      } yield (ab._1, a.name)
      innerJoin.filter(_._1.id === id).map(_._2).result
    }

  private def addAuthorsToBooks(books: Seq[Book]): Future[Seq[Book]] =
    Future.sequence(
      books.map { b =>
        getBookAuthors(b.id).map(authors => b.updateAuthors(authors))
      }
    )

    def getAllBooks = {
      val queryBooks = db.run(booksList.result)
      for {
        books <- queryBooks
        booksWithAuthors <- addAuthorsToBooks(books)
      } yield booksWithAuthors.sortBy(_.id)
    }


  def getBookById(id:Long) = {
    val foundBook = db.run(booksList.filter(_.id === id).result)
    for {
      book <- foundBook
      bookWithAuthors <- addAuthorsToBooks(book)
    } yield bookWithAuthors
  }

}

//  val booksAuthorsId = db.run(knownAuthors).flatMap{ x =>
//    val knownNames = x.map(_._2)
//    val newAuthors = authors.filterNot(knownNames.contains(_))
//    db.run(authorsList.map(x => (x.id, x.name)) ++= newAuthors.map((1, _))).flatMap{ _ =>
//      db.run(authorsList.filter(_.name.inSet(newAuthors)).map(x => x.id).result).map(_ ++ x.map(_._1))
//    }
//  }
//def updateWithoutNewAuthors(bookID:Long, newTitle:String, newYear:Int): Future[String]  ={
//  val updatedBook = booksList.filter(_.id === bookID).map(x => (x.title,x.year))
//  db.run(updatedBook.update(newTitle, newYear)).map(_ => "Book successfully updated")
////  }
//  def getAllBooks = {
//    val queryBooks = db.run(booksList.result)
//    for {
//      books <- queryBooks
//      booksWithAuthors <- addAuthorsToBooks(books) if books.nonEmpty
//    } yield booksWithAuthors.sortBy(_.id)
//  }
//
