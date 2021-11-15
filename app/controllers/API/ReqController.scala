package controllers.API

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}
import models.Book
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import models._
import play.api.libs.json.Format.GenericFormat

import javax.inject._
import play.api.libs.json._
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

//API endpoints to which the application will respond
class ReqController @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents) (implicit ec: ExecutionContext)
  extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile]{
  implicit val bookFormat: OFormat[Book] = Json.format[Book]
  implicit val books_authorsFormat: OFormat[BooksAndAuthors] = Json.format[BooksAndAuthors]
  implicit val authorFormat: OFormat[Author] = Json.format[Author]


  def getAllBooks = Action.async { implicit request =>

    val newCommandList = new BooksCommandList(dbConfigProvider)
    newCommandList.getAllBooks.map(books =>
      if (books.nonEmpty) Ok(Json.toJson(books)) else BadRequest("There are no books in the database right now"))
  }

  def getBookById(id: Long) = Action.async { implicit request =>

    val newCommandList = new BooksCommandList(dbConfigProvider)
    val book = newCommandList.getBookById(id)
    book.map(book =>
      if(book.isEmpty) BadRequest("Bad request: No Book with this id") else Ok(Json.toJson(book)))
  }

  def getAllAuthors = Action.async { implicit request =>

    val newCommandList = new AuthorsCommandList(dbConfigProvider)
    newCommandList.getAllAuthors.map(authors =>
      if (authors.nonEmpty) Ok(Json.toJson(authors)) else BadRequest("There are no authors in the database right now"))
  }

  def getAuthorById(id: Long) = Action.async { implicit request =>

    val newCommandList = new AuthorsCommandList(dbConfigProvider)
    val author = newCommandList.getAuthorById(id)
    author.map(author =>
      if(author.isEmpty) BadRequest("Bad request: No Author with this id") else Ok(Json.toJson(author)))
  }

  def addNewBook = Action.async { implicit request: Request[AnyContent] =>
    BookForm.form.bindFromRequest.fold(
      // if any error in submitted data
      errorForm => {
        errorForm.errors.foreach(println)
        Future.successful(BadRequest("Error!"))
      },
      data => {
        val newBook = Book(0, data.title, data.year, data.authors)
        val newCommandList = new BooksCommandList(dbConfigProvider)
        newCommandList.add(newBook).map(Ok(_))
      })
  }

  def updateBook(id:Long):Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    BookForm.form.bindFromRequest.fold(
      // if any error in submitted data
      errorForm => {
        errorForm.errors.foreach(println)
        Future.successful(BadRequest("Error!"))
      },
      data => {
        val updatedBook = Book(id, data.title, data.year, data.authors)
        val newCommandList = new BooksCommandList(dbConfigProvider)
        newCommandList.update(updatedBook).map(Ok(_))
      })
  }

  def deleteBook(id:Long):Action[AnyContent] = Action.async {implicit request: Request[AnyContent] =>
    val newCommandList = new BooksCommandList(dbConfigProvider)
    newCommandList.delete(id).map(Ok(_))
  }
}

//def updateBookWithoutNewAuthors(id:Long):Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
//  BookForm.form.bindFromRequest.fold(
//  // if any error in submitted data
//  errorForm => {
//  errorForm.errors.foreach(println)
//  Future.successful(BadRequest("Error!"))
//  },
//  data => {
//  val updatedBook = Book(id, data.title, data.year)
//  val newCommandList = new BooksCommandList(dbConfigProvider,cc)
//  newCommandList.updateWithoutNewAuthors(updatedBook.id,updatedBook.title, updatedBook.year).map(Ok(_))
//  })
//  }

//  def getAll = Action.async {
//    val book = new Book(id = 0, title = "Преступление", year = 1992)
//    val newCommandList = new BooksCommandList(dbConfigProvider,cc)
//    newCommandList.add(book,List("Толстой", "Иван")).map(Ok(_))
//  }