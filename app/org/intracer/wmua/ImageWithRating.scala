package org.intracer.wmua

import java.text.DecimalFormat

case class ImageWithRating(
                            image: Image,
                            selection: Seq[Selection],
                            countFromDb: Int = 0,
                            rank: Option[Int] = None,
                            rank2: Option[Int] = None) extends Ordered[ImageWithRating] {

  val ownJuryRating = new OwnRating(selection.headOption.getOrElse(new Selection(0, image.pageId, 0, 0, 0)))

  def unSelect(): Unit =
    ownJuryRating.unSelect()

  def select(): Unit =
    ownJuryRating.select()

  def isSelected: Boolean = ownJuryRating.isSelected

  def isRejected: Boolean = ownJuryRating.isRejected

  def isUnrated: Boolean = ownJuryRating.isUnrated

  def rate = ownJuryRating.rate

  def rate_=(rate: Int) {
    ownJuryRating.rate = rate
  }

  def rankStr = (
    for (r1 <- rank; r2 <- rank2)
      yield s"$r1-$r2."
    ).orElse(
    for (r1 <- rank)
      yield r1 + "."
  ).getOrElse("")

  def totalRate(round: Round): Double =
    if (selection.size == 1 && selection.head.juryId != 0)
      selection.head.rate
    else if (ratedJurors(round) == 0)
      0.0
    else
      rateSum.toDouble / ratedJurors(round)

  def rateSum = selection.foldLeft(0)((acc, s) => acc + Math.max(s.rate, 0))

  def jurors = selection.map(s => s.juryId).toSet

  def ratedJurors(round: Round): Int =
    if (round.isBinary) {
      round._allJurors
    } else if (selection.headOption.exists(_.juryId == 0) && !round.optionalRate)
      countFromDb
    else if (selection.size == 1 && selection.headOption.exists(_.juryId != 0))
      1
    else if (round.optionalRate)
      round.activeJurors
    else selection.count(_.rate > 0)

  def rateString(round: Round) = if (ratedJurors(round) == 0) "0" else s"${Formatter.fmt.format(totalRate(round))} ($rateSum / ${ratedJurors(round)})"

  def pageId = image.pageId

  def title = image.title

  def compare(that: ImageWithRating) = (this.pageId - that.pageId).signum
}

object ImageWithRating {

  def rankImages(orderedImages: Seq[ImageWithRating], round: Round) = {
    rank(orderedImages.map(_.rateSum))
  }

  def rank(orderedRates: Seq[Int]): Seq[String] = {

    val sizeByRate = orderedRates.groupBy(identity).mapValues(_.size)
    val startByRate = sizeByRate.keys.map { rate => rate -> (orderedRates.indexOf(rate) + 1) }.toMap

    orderedRates.map {
      rate =>
        val (start, size) = (startByRate(rate), sizeByRate(rate))
        if (size == 1)
          start.toString
        else
          start + "-" + (start + size - 1)
    }
  }


}

class Rating

class OneJuryRating(val selection: Selection) extends Rating {
  def isSelected: Boolean = selection.rate > 0

  def isRejected: Boolean = selection.rate < 0

  def isUnrated: Boolean = selection.rate == 0

  def rate = selection.rate
}

class OwnRating(selection: Selection) extends OneJuryRating(selection) {

  def unSelect() {
    selection.rate = -1
  }

  def select() {
    selection.rate = 1
  }

  def rate_=(rate: Int) {
    selection.rate = rate
  }

}

class TotalRating extends Rating


object Formatter {
  val fmt = new DecimalFormat("0.00")
}
