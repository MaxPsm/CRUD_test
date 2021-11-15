package models

import slick.jdbc.MySQLProfile.api._


case class BooksAndAuthors(idBook:Long, idAuthor:Long)

case class BooksAndAuthorsTable(tag:Tag) extends Table[BooksAndAuthors](tag,"books_authors") {

  def id_book = column[Long]("id_book")

  def id_author = column[Long]("id_author")

  def idBookFK =
    foreignKey("books_authors_ibfk_1", id_book, TableQuery[BookTable])(_.id, onDelete=ForeignKeyAction.Cascade)

  def idAuthorFK =
    foreignKey("books_authors_ibfk_2", id_author, TableQuery[AuthorTable])(_.id, onDelete=ForeignKeyAction.Cascade)


  override def * = (id_book, id_author) <> (BooksAndAuthors.tupled, BooksAndAuthors.unapply)
}

