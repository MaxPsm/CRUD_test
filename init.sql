
CREATE TABLE `library`.`books` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(45) NOT NULL,
  `year` INT NOT NULL,
  PRIMARY KEY (`id`));

CREATE TABLE `library`.`authors` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL UNIQUE,
  PRIMARY KEY (`id`));

CREATE TABLE `library`.`books_authors` (
  `id_book` INT NOT NULL,
  `id_author` INT NOT NULL,
  FOREIGN KEY (id_book)  REFERENCES books (id)  ON DELETE CASCADE,
  FOREIGN KEY (id_author)  REFERENCES authors (id)  ON DELETE CASCADE
);
