package db

import org.intracer.wmua.ContestJury

/**
  * Interface for accessing contests in WLX Jury.
  * TODO Temporarily called ContestJury to prevent collision with Contest from scalawiki wlx part
  */
trait ContestJuryDao {

  /** TODO more than one parallel round
    * @param id contest Id
    * @return Id of the current round
    */
  def currentRound(id: Long): Option[Long]

  /** @return all contests grouped by country name */
  def byCountry: Map[String, Seq[ContestJury]]

  /** @param id contest Id
    * @return optional contest by id
    */
  def byId(id: Long): Option[ContestJury]

  /** @param id contest Id
    * @return optional contest by id
    */
  def find(id: Long): Option[ContestJury]

  def find(name: String, country: String, year: Int): Option[ContestJury]

  /** @return all contests */
  def findAll(): Seq[ContestJury]

  /** @return number of contests */
  def countAll(): Long

  def updateImages(id: Long, images: Option[String]): Int

  def setCurrentRound(id: Long, round: Option[Long]): Int

  def create(id: Option[Long],
             name: String,
             year: Int,
             country: String,
             images: Option[String],
             currentRound: Option[Long],
             monumentIdTemplate: Option[String]): ContestJury

  def batchInsert(contests: Seq[ContestJury]): Seq[Int]

}
