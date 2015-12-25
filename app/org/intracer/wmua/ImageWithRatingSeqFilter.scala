package org.intracer.wmua


trait ImageFilterGen extends (() => Seq[ImageWithRating] => Seq[ImageWithRating]) {

  type ImageSeqFilter = Seq[ImageWithRating] => Seq[ImageWithRating]

  def imageFilter(p: Image => Boolean): ImageSeqFilter =
    (images: Seq[ImageWithRating]) => {
      val result = images.filter(i => p(i.image))
      println(s"imageFilter: ${toString()}\n images before: ${images.size}, images after: ${result.size}")
      result
    }

  def imageRatingFilter(p: ImageWithRating => Boolean): ImageSeqFilter =
    (images: Seq[ImageWithRating]) => {
      val result = images.filter(p)
      println(s"imageRatingFilter: ${toString()}\n images before: ${images.size}, images after: ${result.size}")
      result
    }

}

case class IncludeRegionIds(regionIds: Set[String]) extends ImageFilterGen {
  override def apply = imageFilter(_.region.exists(regionIds.contains))
}

case class ExcludeRegionIds(regionIds: Set[String]) extends ImageFilterGen {
  override def apply = imageFilter(!_.region.exists(regionIds.contains))
}

case class IncludePageIds(pageIds: Set[Long]) extends ImageFilterGen {
  def apply = imageFilter(i => pageIds.contains(i.pageId))
}

case class ExcludePageIds(pageIds: Set[Long]) extends ImageFilterGen {
  def apply = imageFilter(i => !pageIds.contains(i.pageId))
}

//  Source.fromFile("porota 2 kolo najlepsie hodnotenie 6.0.txt")(scala.io.Codec.UTF8).getLines().map(_.replace(160.asInstanceOf[Char], ' ').trim).toSet
case class IncludeTitles(titles: Set[String]) extends ImageFilterGen {
  def apply = imageFilter(i => titles.contains(i.title))
}

case class ExcludeTitles(titles: Set[String]) extends ImageFilterGen {
  def apply = imageFilter(i => titles.contains(i.title))
}

case class IncludeJurorId(jurors: Set[Long]) extends ImageFilterGen {
  def apply = imageRatingFilter(i => i.selection.map(_.juryId).toSet.intersect(jurors).nonEmpty)
}

case class ExcludeJurorId(jurors: Set[Long]) extends ImageFilterGen {
  def apply = imageRatingFilter(i => i.selection.map(_.juryId).toSet.intersect(jurors).isEmpty)
}

case class SelectTopByRating(topN: Int, round: Round) extends ImageFilterGen {
  def apply = (images: Seq[ImageWithRating]) => images.sortBy(-_.totalRate(round)).take(topN)
}

case class SelectedAtLeast(by: Int) extends ImageFilterGen {
  def apply = imageRatingFilter(i => i.selection.count(_.rate > 0) >= by)
}

case class MegaPixelsAtLeast(mpx: Int) extends ImageFilterGen {
  def apply = imageFilter(_.mpx >= mpx)
}

object ImageWithRatingSeqFilter {
  def funGenerators(round: Round,
                    includeRegionIds: Set[String] = Set.empty,
                    excludeRegionIds: Set[String] = Set.empty,
                    includePageIds: Set[Long] = Set.empty,
                    excludePageIds: Set[Long] = Set.empty,
                    includeTitles: Set[String] = Set.empty,
                    excludeTitles: Set[String] = Set.empty,
                    includeJurorId: Set[Long] = Set.empty,
                    excludeJurorId: Set[Long] = Set.empty,
                    selectTopByRating: Option[Int] = None,
                    selectedAtLeast: Option[Int] = None
                   ): Seq[ImageFilterGen] = {

    val setMap = Map(
      includeRegionIds -> IncludeRegionIds(includeRegionIds),
      excludeRegionIds -> ExcludeRegionIds(excludeRegionIds),
      includePageIds -> IncludePageIds(includePageIds),
      excludePageIds -> ExcludePageIds(excludePageIds),
      includeTitles -> IncludeTitles(includeTitles),
      excludeTitles -> ExcludeTitles(excludeTitles),
      includeJurorId -> IncludeJurorId(includeJurorId),
      excludeJurorId -> ExcludeJurorId(excludeJurorId)
    )

    val optionMap = Map(
      selectTopByRating -> selectTopByRating.map(top => SelectTopByRating(top, round)),
      selectedAtLeast -> selectedAtLeast.map(n => SelectedAtLeast(n))
    )

    (setMap.filterKeys(_.nonEmpty).values ++ optionMap.values.flatten).toSeq
  }

  def makeFunChain(gens: Seq[ImageFilterGen]): (Seq[ImageWithRating] => Seq[ImageWithRating]) =
    Function.chain(gens.map(_.apply))
}