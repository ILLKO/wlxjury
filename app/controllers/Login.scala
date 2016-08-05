package controllers

import db.scalikejdbc.{RoundJdbc, UserJdbc}
import org.intracer.wmua.{Round, User}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{Lang, Messages}
import play.api.mvc._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Results._

object Login extends Controller with Secured {

  def index = withAuth {
    user =>
      implicit request =>
        indexRedirect(user)
  }

  def indexRedirect(user: User): Result = {
    if (user.hasAnyRole(User.ORG_COM_ROLES)) {
      Redirect(routes.Rounds.currentRoundStat())
    } else if (user.hasAnyRole(User.JURY_ROLES)) {
      val maybeRound = RoundJdbc.current(user)
      maybeRound.fold {
        Redirect(routes.Login.error("no.round.yet"))
      } {
        round =>
          if (round.isBinary) {
            Redirect(routes.Gallery.list(user.id.get, 0, "all", round.id.get))
          } else {
            Redirect(routes.Gallery.byRate(user.id.get, 0, "all", 0))
          }
      }
    } else if (user.hasRole(User.ROOT_ROLE)) {
      Redirect(routes.Contests.list())
    } else if (user.hasAnyRole(User.ADMIN_ROLES)) {
      Redirect(routes.Admin.users())
    } else {
      Redirect(routes.Login.error("You don't have permission to access this page"))
    }
  }

  def login = Action {
    implicit request =>
      Ok(views.html.index(loginForm))
  }

  def auth() = Action {
    implicit request =>

      loginForm.bindFromRequest.fold(
        formWithErrors => // binding failure, you retrieve the form containing errors,
          BadRequest(views.html.index(formWithErrors)),
        value => {
          // binding success, you get the actual value
          val user = UserJdbc.login(value._1, value._2).get
          val result = indexRedirect(user).withSession(Security.username -> value._1.trim)
          user.lang.fold(result)(l => result.withLang(Lang(l)))
        }
      )
  }

  /**
    * Logout and clean the session.
    *
    * @return Index page
    */
  def logout = Action {
    Redirect(routes.Login.login()).withNewSession
  }

  def error(message: String) = withAuth {
    user =>
      implicit request =>
        Ok(views.html.error(message, user, user.id.get, user))
  }

  val loginForm = Form(
    tuple(
      "login" -> nonEmptyText(),
      "password" -> nonEmptyText()
    ) verifying("invalid.user.or.password", fields => fields match {
      case (l, p) => UserJdbc.login(l, p).isDefined
    })
  )
}



