import play.api.{Logger, mvc}
import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
class ErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    val fullMessage = "An error occurred: " + statusCode + (if (message.nonEmpty) ", " + message else "")
    Logger.logger.error(fullMessage)
    Future.successful(
      Status(statusCode)(views.html.error(fullMessage))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    Logger.logger.error("A server error occurred: " + exception.getMessage, exception)
    Future.successful(
      InternalServerError(views.html.error("A server error occurred: " + exception.getMessage))
    )
  }
}