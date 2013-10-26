package controllers

import play.api.mvc.{Request, SimpleResult, Controller}
import org.intracer.wmua.User
import play.api.data.Form
import play.api.data.Forms._
import play.api.templates.Html
import java.util
import org.wikipedia.Wiki
import scala.collection.JavaConverters._
import scala.collection.{SortedSet, mutable}
import java.io.IOException
import scalikejdbc.SQLInterpolation._
import play.api.mvc.SimpleResult

object Gallery extends Controller with Secured {

  def pages = 15

  val Selected = "selected"

  val Filter = "filter"

  val UrlInProgress = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Icon_tools.svg/120px-Icon_tools.svg.png"

  def list(page: Int = 1) = withAuth {
    username =>
      implicit request =>
        val user = User.byUserName.get(username.trim).get

        var files = userFiles(user)

        val pageFiles: Seq[String] = files.slice((page - 1) * (filesPerPage(user) + 1), Math.min(page * (filesPerPage(user) + 1), files.size))
        Ok(views.html.gallery(user, pageFiles, user.selected, page))
  }

  def selected() = withAuth {
    username =>
      implicit request =>
        val user = User.byUserName.get(username.trim).get

        val files = userFiles(user).filter(user.selected.contains)

        Ok(views.html.selected(user, files, user.selected, 0))
  }


  def round2(regionId: String, round: Int) = withAuth {
    username =>
      implicit request =>
        val user = User.byUserName.get(username.trim).get

        val files = if (round>1) {
          Selection.findAllBy(sqls.eq(Selection.c.round, round-1)).map(_.filename).toSet.toSeq
        } else {
          userFiles(user)
        }
        val selected = mutable.SortedSet(Selection.findAllBy(sqls.eq(Selection.c.round, round)).map(_.filename): _*)



        val filesInRegion = KOATUU.filesInRegion(files, regionId)
        val selectedInRegion = mutable.SortedSet(KOATUU.filesInRegion(selected.toSeq, regionId): _*)

        Ok(views.html.round2(user, filesInRegion, selectedInRegion, regionId, round))
  }

  def largeRound2(regionId: String, index: Int, round: Int) = withAuth {
    username =>
      implicit request =>

        val user = User.byUserName.get(username.trim).get

        val files = if (round>1) {
          Selection.findAllBy(sqls.eq(Selection.c.round, round-1)).map(_.filename).toSet.toSeq
        } else {
          userFiles(user)
        }

        val selected = mutable.SortedSet(Selection.findAllBy(sqls.eq(Selection.c.round, round)).map(_.filename): _*)

        val filesInRegion = KOATUU.filesInRegion(files, regionId)
        val selectedInRegion = mutable.SortedSet(KOATUU.filesInRegion(selected.toSeq, regionId): _*)

        show2(index, filesInRegion, user, false, user.login, selectedInRegion, 0, Some(regionId), round)
  }


  def large(index: Int) = withAuth {
    username =>
      implicit request =>

        show(index, username.trim)
  }

  def largeSelected(index: Int) = withAuth {
    username =>
      implicit request =>

        show(index, username.trim, showSelected = true)
  }


  def userFiles(user: User): Seq[String] = {
    val index = User.getUserIndex(user.login) % 16 + 1

    if (user.files.isEmpty) {

      val path: String =
        "commons:Wiki Loves Monuments 2013 in Ukraine Jury/" + index


      val images: mutable.WrappedArray[String] = try {
        Global.w.getImagesOnPage(path)
      } catch {
        case e: IOException =>
          Global.w.getImagesOnPage(path)
      }
      user.files ++= util.Arrays.asList[String](images: _*).asScala
    }
    user.files
  }


  def select(index: Int, round: Int = 1) = withAuth {
    username =>
      implicit request =>

        val user = User.byUserName.get(username.trim).get
        val selected = user.selected

        val file = userFiles(user)(index)
        if (selected.contains(file)) {

          Selection.destroy(filename = file, email = user.login.trim.toLowerCase)

          selected -= file
        }
        else {
          selected += file
          val selection = Selection.create(filename = file, fileid = "" + index, juryid = user.id.toInt, round = round, email = user.login.trim.toLowerCase)
        }

        show(index, username)
  }

  def selectRound2(regionId: String, fileHash: Int, round: Int = 2) = withAuth {
    username =>
      implicit request =>
        val user = User.byUserName.get(username.trim).get

        val files = if (round>1) {
          Selection.findAllBy(sqls.eq(Selection.c.round, round-1)).map(_.filename).toSet.toSeq
        } else {
          userFiles(user)
        }

        val selected = mutable.SortedSet(Selection.findAllBy(sqls.eq(Selection.c.round, round)).map(_.filename): _*)

        val filesInRegion = KOATUU.filesInRegion(files, regionId)
        val selectedInRegion = mutable.SortedSet(KOATUU.filesInRegion(selected.toSeq, regionId): _*)

        val file = KOATUU.fileHashes(fileHash.toString)

        val index = filesInRegion.indexOf(file)

        val selection = Selection.create(filename = file, fileid = "" + index, juryid = user.id.toInt, round = round, email = user.login.trim.toLowerCase)

        Redirect(routes.Gallery.largeRound2(regionId, index, round))
  }

  def unselectRound2(regionId: String, fileHash: Int, round: Int = 2) = withAuth {
    username =>
      implicit request =>
        val user = User.byUserName.get(username.trim).get

        val files = if (round>1) {
          Selection.findAllBy(sqls.eq(Selection.c.round, round-1)).map(_.filename).toSet.toSeq
        } else {
          userFiles(user)
        }

        val selected = mutable.SortedSet(Selection.findAllBy(sqls.eq(Selection.c.round, round)).map(_.filename): _*)

        val filesInRegion = KOATUU.filesInRegion(files, regionId)
        val selectedInRegion = mutable.SortedSet(KOATUU.filesInRegion(selected.toSeq, regionId): _*)

        val file = KOATUU.fileHashes(fileHash.toString)

        val index = filesInRegion.indexOf(file)

        Selection.destroy(filename = file, email = user.login.trim.toLowerCase)

        Redirect(routes.Gallery.largeRound2(regionId, index, round))
  }



  def unselect(index: Int) = withAuth {
    username =>
      implicit request =>

        val user = User.byUserName.get(username.trim).get
        val selected = user.selected

        val selectedFiles = userFiles(user).filter(user.selected.contains)
        val file = selectedFiles(index)
        if (selected.contains(file)) {

          Selection.destroy(filename = file, email = user.login.trim.toLowerCase)

          selected -= file
        }

        val newIndex = if (index > selected.size - 1)
          selected.size - 1
        else index

        if (newIndex >= 0) {
          show(newIndex, username, showSelected = true)
        } else {
          Ok(views.html.selected(user, Seq.empty, user.selected, 0))
        }

  }


  def selectWiki(file: String, user: User) {
    var text = Global.w.getPageText(file)
    if (!text.contains("WLM 2013 in Ukraine Round One " + user.fullname)) {
      val newCat: String = s"[[Category:WLM 2013 in Ukraine Round One ${user.fullname}]]"
      text += "\n" + newCat
      Global.w.edit(file, text, s"Adding $newCat")
    }
  }

  def deselectWiki(file: String, user: User) {
    var text = Global.w.getPageText(file)
    if (text.contains("WLM 2013 in Ukraine Round One " + user.fullname)) {
      val newCat: String = s"\\Q[[Category:WLM 2013 in Ukraine Round One ${user.fullname}]]\\E"
      text = text.replaceAll(newCat, "")
      Global.w.edit(file, text, s"Removing [[Category:WLM 2013 in Ukraine Round One ${user.fullname}]]")
    }
  }

  def show(index: Int, username: String, showSelected: Boolean = false)(implicit request: Request[Any]): SimpleResult[Html] = {
    val user = User.byUserName.get(username).get

    var files = userFiles(user)

    if (showSelected) {
      files = files.filter(user.selected.contains)
    }
    val selected = user.selected
    val page = (index / filesPerPage(user)) + 1

    show2(index, files, user, showSelected, username, selected, page)
  }


  def show2(index: Int, files: Seq[String], user: User, showSelected: Boolean, username: String, selected: mutable.SortedSet[String],
    page: Int, region: Option[String] = None, round: Int = 1)
           (implicit request: Request[Any]): SimpleResult[Html] = {
    val extraRight = if (index - 2 < 0) 2 - index else 0
    val extraLeft = if (files.size < index + 3) index + 3 - files.size else 0


    val left = Math.max(0, index - 2)
    val right = Math.min(index + 3, files.size)
    val start = Math.max(0, left - extraLeft)
    var end = Math.min(files.size, right + extraRight)


    if (!region.isDefined) {
      if (showSelected) {
        Ok(views.html.largeSelected(User.byUserName.get(username).get, files, selected, index, start, end, page))
      } else {
        Ok(views.html.large(User.byUserName.get(username).get, files, selected, index, start, end, page))
      }
    } else {
      Ok(views.html.largeRound2(User.byUserName.get(username).get, files, selected, index, start, end, region.get, round))
    }
  }

  def filesPerPage(user: User): Int = {
    userFiles(user).size / pages
  }

  val loginForm = Form(
    tuple(
      "login" -> nonEmptyText(),
      "password" -> nonEmptyText()
    ) verifying("invalid.user.or.password", fields => fields match {
      case (l, p) => User.login(l, p).isDefined
    })
  )

}
