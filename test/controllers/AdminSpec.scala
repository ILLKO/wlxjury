package controllers

import db.scalikejdbc.InMemDb
import org.intracer.wmua.{ContestJury, User}
import org.specs2.mutable.Specification

class AdminSpec extends Specification with InMemDb {

  val sender = User("Admin User", "email@server.com", None, contest = None)

  "fill template" should {

    "fill contest info" in {
      inMemDbApp {
        val contest = ContestJury(name = "Wiki Loves Earth", year = 2016, country = "Ukraine", images = None, id = None)
        val template = "Organizing committee of {{ContestType}} {{ContestYear}} {{ContestCountry}} is glad to welcome you as a jury member\n" +
          "Please visit {{JuryToolLink}}\n" +
          "Regards, {{AdminName}}"
        val filled = Admin.fillGreeting(template, contest, sender, sender)
        filled === "Organizing committee of Wiki Loves Earth 2016 Ukraine is glad to welcome you as a jury member\n" +
          "Please visit http://jury.wikilovesearth.org.ua/\n" +
          "Regards, Admin User"
      }
    }
  }
}
