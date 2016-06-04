package org.intracer.wmua.cmd

import org.intracer.wmua.{ContestJury, Image}
import org.scalawiki.MwBot
import org.scalawiki.dto.{Namespace, Page, Revision}
import org.scalawiki.query.SinglePageQuery
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.Future

class ImageTextFromCategorySpec extends Specification with Mockito {

  def contestImage(id: Long, contest: Long) =
    Image(id, contest, s"File:Image$id.jpg", "", "", 0, 0, Some(s"12-345-$id"))

  def revision(id: Long, text: String) = new Page(Some(id), Namespace.FILE, s"File:Image$id.jpg", revisions = Seq(
    new Revision(Some(id + 100), Some(id), content = Some(text))
  ))

  "appendImages" should {
    "get images empty" in {
      implicit ee: ExecutionEnv =>
        val category = "Category:Category Name"
        val contestId = 13
        val images = Seq.empty[Image]
        val revisions = Seq.empty[Page]

        val query = mock[SinglePageQuery]

        query.revisionsByGenerator("categorymembers", "cm",
          Set.empty, Set("content", "timestamp", "user", "comment"), limit = "50", titlePrefix = None
        ) returns Future.successful(revisions)

        val commons = mock[MwBot]
        commons.page(category) returns query

        val contest = ContestJury(Some(contestId), "WLE", 2015, "Ukraine", Some(category))

        ImageTextFromCategory(category, contest, None, commons).apply() must be_==(images).await
    }

    "get images one image with text" in {
      implicit ee: ExecutionEnv =>
        val category = "Category:Category Name"
        val contestId = 13
        val imageId = 11
        val images = Seq(contestImage(imageId, contestId).copy(description = Some("descr"), monumentId = Some("")))
        val revisions = Seq(revision(imageId, "{{Information|description=descr}}"))

        val query = mock[SinglePageQuery]

        query.revisionsByGenerator("categorymembers", "cm",
          Set.empty, Set("content", "timestamp", "user", "comment"), limit = "50", titlePrefix = None
        ) returns Future.successful(revisions)

        val commons = mock[MwBot]
        commons.page(category) returns query

        val contest = ContestJury(Some(contestId), "WLE", 2015, "Ukraine", Some(category), Some(0), None)

        ImageTextFromCategory(category, contest, None, commons).apply() must be_==(images).await
      }

    "get images one image with descr and monumentId" in {
      implicit ee: ExecutionEnv =>

        val category = "Category:Category Name"
        val idTemplate = "monumentId"
        val contestId = 13
        val imageId = 11
        val descr = s"descr. {{$idTemplate|12-345-$imageId}}"
        val images = Seq(contestImage(imageId, contestId).copy(description = Some(descr)))
        val revisions = Seq(revision(imageId, s"{{Information|description=$descr}}"))

        val query = mock[SinglePageQuery]

        query.revisionsByGenerator("categorymembers", "cm",
          Set.empty, Set("content", "timestamp", "user", "comment"), limit = "50", titlePrefix = None
        ) returns Future.successful(revisions)

        val commons = mock[MwBot]
        commons.page(category) returns query

        val contest = ContestJury(Some(contestId), "WLE", 2015, "Ukraine", Some(category), Some(0), None)

        ImageTextFromCategory(category, contest, Some(idTemplate), commons).apply() must be_==(images).await
    }

  }

}
