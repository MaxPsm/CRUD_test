package controllers

import akka.stream.Materializer
import controllers.API.ReqController
import models.BooksCommandList
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.test._
import play.api.test.Helpers.{contentAsString, _}

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.mvc._



/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class ReqControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "ReqController GET" should {

    "ReqController GET with BooksCommandList" should {
      "render get books page from the application" in {
        val bkl = inject[BooksCommandList]
        val controller = inject[ReqController]
        val home = controller.getAllBooks().apply(FakeRequest(GET, "/"))
        val bkl_home = bkl.getAllBooks
        val titles = bkl_home.map(_.map(_.title))

        status(home) mustBe OK
        contentType(home) mustBe Some("application/json")
        contentAsString(home) must include ("id")
        contentAsString(home) must include ("title")
        contentAsString(home) must include ("year")
        contentAsString(home) must include ("authors")

        for ( x <- titles;
              y <- x) {
          contentAsString(home) must include (y)
        }

      }
    }

    "render get authors page from the application" in {
      val controller = inject[ReqController]
      val home = controller.getAllAuthors().apply(FakeRequest(GET, "/authors"))

      status(home) mustBe OK
      contentType(home) mustBe Some("application/json")
      contentAsString(home) must include ("id")
      contentAsString(home) must include ("books")
    }

    "render the index page from the router" in {
      val request = FakeRequest(GET, "/")
      val home = route(app, request).get

      status(home) mustBe OK
      contentType(home) mustBe Some("application/json")
    }
  }
}

class ExampleEssentialActionSpec extends PlaySpec with GuiceOneAppPerSuite {

  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val Action                     = app.injector.instanceOf(classOf[DefaultActionBuilder])

  "Post action" should {
    "can parse a JSON body" in {
      val action: EssentialAction = Action { request =>
        val value = (request.body.asJson.get \ "title").as[String]
        Ok(value)
      }

      val request = FakeRequest(POST, "/api/books/add").withJsonBody(Json.parse("""{ "title": "value" }"""))

      val result = call(action, request)

      status(result) mustEqual OK
      contentAsString(result) mustEqual "value"
    }
  }
}
