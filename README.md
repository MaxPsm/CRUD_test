# CRUD_test

Данный репозиторий содержит реализацию CRUD-api для библиотеки с использованием Play framework и библиотеки Slick. Используемая база данных - MySql.


Инициализация базы данных находится в файле init.sql.

Конфигурация базы находится в файле application.conf, используемое название базы - "library".

#Реализованные методы:

GET     /                   - получить список всех книг        
GET     /api/books/:id          - получить книгу по её id      
GET     /api/authors/     - получить список авторов

GET     /api/authors/:id         - получить автора по его id  
POST    /api/books/add         - добавить новую книгу      
PUT     /api/books/update/:id        - изменить существующую книгу по её id

DELETE  /api/books/delete/:id      - удалить книгу по id        