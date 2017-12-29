package org.fhs.spirit.test

//import org.specs2._

import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.{Formatter, Scanner}

import org.fhs.spirit.scheduleparser.ScheduleToJSONConverter
import org.fhs.spirit.scheduleparser.ScheduleToJSONConverter._
import org.fhs.spirit.scheduleparser.enumerations.{EScheduleKind, EWeekdays}
import org.jsoup.Jsoup
import org.specs2.mutable._

import scala.io.Source
import scala.util.parsing.json.JSON

/**
  * @author fabian
  *         on 31.08.15.
  */
object ParserSpec extends Specification {


  "Basic Tests" should {
    "access ressources" in {

      Source.fromURL(getClass().getResource("/plan/s_bai2.html"))

      1 mustEqual 1
    }
  }

  "Weekday Tests" should {
    "parse weekday" in {
      val days = Array("Montag","Dienstag","Mittwoch","Donnerstag","Freitag","Samstag", "Sonnabend","Sonntag")
      val parseResults = days.map(weekday => EWeekdays.findConstantByName(weekday))

      parseResults.forall(_.isPresent) must beTrue

    }
  }


  "Schedule Parsing" should {
    "Regular Schedule" in {
      var buffer = new StringBuilder

      val courses = List("bai", "bais", "bamc", "bamm", "bawi", "mai")

      val courseNames = courses.flatMap {
        c =>
          (1 to 6).map {
            i =>
              c + i
          }
      }

      courseNames.map { c =>
        var scanner = new Scanner(getClass().getResourceAsStream("/plan/s_" + c + ".html"), StandardCharsets.ISO_8859_1.toString)

        while (scanner.hasNext) {
          buffer.append(scanner.nextLine())
        }

        scanner close()
        val htmlAsString = buffer.toString()

        scanner = new Scanner(getClass().getResourceAsStream("/MultiLecturer.json"))
        buffer = new StringBuilder
        while (scanner.hasNext) {
          buffer.append(scanner.nextLine())
        }
        scanner close()

        val multilecturer = JSON.parseFull(buffer.toString()).get.asInstanceOf[Map[String, List[String]]]

        val parseResult = ScheduleToJSONConverter(htmlAsString, EScheduleKind.REGULAR, "",multilecturer).get
        try {
          scanner = new Scanner(getClass().getResourceAsStream("/result/" + c + ".json"))
          val comparson = scanner.nextLine()
          scanner.close()

          comparson mustEqual parseResult
        } catch {
          case e:NullPointerException=>
            val output = new Formatter(c + ".json")
            output.format("%s",parseResult)
            output.flush()
            output.close()
        }
      }



      //println(tbody.children().first())

      1 + 1 mustEqual 2
    }

    "Block Schedule" in {

      val baseUrl = "http://my.fh-sm.de/~fbi-x/Stundenplan/"
      val outcome = "bindex.html"

      val bindex = Jsoup.connect(baseUrl + outcome).get

      val blockRefs = bindex.select("a").map(_.attr("href")).distinct
      if (blockRefs.nonEmpty) {
        blockRefs.foreach {
          block =>
            val stream = new URL(baseUrl + block).openStream()
            val parseResult = ScheduleToJSONConverter(Jsoup.parse(stream, StandardCharsets.ISO_8859_1.toString, baseUrl + block).toString, EScheduleKind.BLOCK)
            stream.close()

            //val parseResult = ScheduleToJSONConverter(Jsoup.connect(baseUrl + block).get.toString, EScheduleKind.BLOCK)
            //println("******" + new URL(baseUrl + block).toString + "*********")
            parseResult match {
              case Some(result) =>
                try {
                  val scanner = new Scanner(getClass.getResourceAsStream("/result/" + block + ".json"))
                  val comparson = scanner.nextLine()
                  scanner.close()
                  result mustEqual comparson
                } catch {
                  case np: NullPointerException =>
                    val output = new Formatter(new File(block + ".json"))
                    output.format("%s", result)
                    output.flush()
                    output.close()
                  // failure(block + " has no compare file ")
                }

              case None =>
            }
        }
      }

      //println(ScheduleToJSONConverter(Jsoup.connect(baseUrl + blockRefs.last).get.toString, EScheduleKind.BLOCK))


      1 + 1 mustEqual 2
    }
  }
}
