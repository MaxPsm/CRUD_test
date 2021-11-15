package models

import com.google.inject.Inject
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.MySQLProfile.api._


case class Author(id:Long, name: String, books : Seq[String] = Seq.empty){

  def updateBooks(newBooks: Seq[String]) = {
    this.copy(books=newBooks)
  }
}

case class AuthorTable(tag:Tag) extends Table[Author](tag,"authors") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def name = column[String]("name")

  type Data = (Long, String)

  def constructAuthor: Data => Author = {
    case (id, name) =>
      Author(id, name)
  }

  def extractAuthor: PartialFunction[Author, Data] = {
    case Author(id, name,  _) =>
      (id, name)
  }

  override def * = (id, name) <> (constructAuthor, extractAuthor.lift)
}

case class AuthorFormData(name: String, books:Seq[String])

object AuthorForm {
  val form = Form(
    mapping(
      "name" -> nonEmptyText,
      "books" -> seq(nonEmptyText)
    )(AuthorFormData.apply)(AuthorFormData.unapply)
  )
}

class AuthorsCommandList @Inject() (
                                   protected val dbConfigProvider: DatabaseConfigProvider
                                 )(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  var booksList = TableQuery[BookTable]
  var authorsList = TableQuery[AuthorTable]
  var booksAndAuthorsList = TableQuery[BooksAndAuthorsTable]

  private def getAuthorsBooks(id:Long): Future[Seq[String]] =
    db.run {
      val innerJoin = for {
        (ab, a) <- authorsList join booksAndAuthorsList on (_.id === _.id_author) join booksList on (_._2.id_book === _.id)
      } yield (ab._1, a.title)
      innerJoin.filter(_._1.id === id).map(_._2).result
    }

  private def addBooksToAuthors(authors: Seq[Author]): Future[Seq[Author]] =
    Future.sequence(
      authors.map { a =>
        getAuthorsBooks(a.id).map(books => a.updateBooks(books))
      }
    )

  def getAllAuthors = {
    val queryAuthors = db.run(authorsList.result)
    for {
      authors <- queryAuthors
      authorsWithBooks <- addBooksToAuthors(authors)
    } yield authorsWithBooks.sortBy(_.id)
  }

  def getAuthorById(id:Long) = {
    val foundAuthor = db.run(authorsList.filter(_.id === id).result)
    for {
      author <- foundAuthor
      authorWIthBooks <- addBooksToAuthors(author)
    } yield authorWIthBooks
  }
}


