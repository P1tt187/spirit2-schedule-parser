package org.fhs.spirit.scheduleparser


import java.security.MessageDigest
import java.util.{Calendar, Locale}
import java.util.regex.Pattern

import org.fhs.spirit.model.{Alternative, Time}
import org.fhs.spirit.scheduleparser.enumerations.EDuration._
import org.fhs.spirit.scheduleparser.enumerations.ELectureKind._
import org.fhs.spirit.scheduleparser.enumerations.EScheduleKind._
import org.fhs.spirit.scheduleparser.enumerations._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

import scala.annotation.tailrec
import scala.util.parsing.json.{JSONArray, JSONObject}

/**
  * @author fabian
  *         on 31.08.15.
  *         converts a html schedule to json
  */
object ScheduleToJSONConverter {

  private val TIME = "Zeit"

  private val DATEFORMAT = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN)

  def apply(schedule: String, scheduleKind: EScheduleKind = REGULAR, course: String = "", multiLecturers: Map[String, List[String]] = Map()): Option[String] = {

    if (schedule.contains("404") && schedule.contains("Not Found")) {
      return Some("[]")
    }
    try {
      Some(scheduleKind match {
        case REGULAR =>
          parseRegular(schedule, multiLecturers, course)
        case BLOCK =>
          //println(schedule)
          parseBlock(schedule, multiLecturers, course)
      })
    } catch {
      case e: Exception =>
        System.err.println(this.getClass.getSimpleName + e.printStackTrace())
        None
    }
  }

  /** search for the tr element which contains the headline Time */
  private def findTimeIndex(table: Element) = {

    val trs = table.select("tr")
    (0 to trs.size() - 1).map {
      trx =>
        if (trs.get(trx).children().find(child => child.nodeName().equals("th") && child.text().equalsIgnoreCase(TIME)).nonEmpty) {
          trx
        } else {
          -1
        }
    }.sorted.reverse.head

  }

  private def parseBlock(schedule: String, multiLecturers: Map[String, List[String]], course: String): String = {

    val columnIndex = parseBlockHeadline(schedule)
    val indexColumn = columnIndex.map { case (key, value) => (value, key) }
    val document = Jsoup.parse(schedule.replaceAll("(?i)<br[^>]*>", "br2n"))
    val courseSelector = document.select("font").first()
    val extractedCourse = if (courseSelector == null) {
      course
    } else {
      courseSelector.text()
    }
    val scheduleDate: DateTime = parseScheduleDate(document)

    val extractetTitleSelector = document.select("h2").get(0)
    val extractedTitle = if (extractetTitleSelector != null) {
      val titlePrefix = extractetTitleSelector.select("font").get(1).text()

      val titleText = extractetTitleSelector.text()
      val titleSuffix = titleText.substring(titleText.indexOf(titlePrefix) + titlePrefix.length)

      (titlePrefix + " " + titleSuffix).replaceAll("br2n", "")
    } else {
      ""
    }
    val alternatives = parseAlternatives(document)

    if (columnIndex.size == 1) {
      return parseRegular(schedule, multiLecturers, extractedCourse, BLOCK, extractedTitle)
    }

    val tbody = document.select("tbody").first()

    val trSelector = tbody.children()

    val scheduleData = (findTimeIndex(tbody) + 1 to trSelector.size() - 1).toList.flatMap {
      idx =>
        val tr = trSelector.get(idx)
        if (tr.children().size() == 0) {
          None
        } else {
          val timeData = parseTime(trSelector.get(idx).children().get(columnIndex(TIME)).text())

          (0 to tr.children().size() - 1).toList.flatMap {
            trIdx =>
              val trChild = tr.children().get(trIdx)
              if ("td".equalsIgnoreCase(trChild.nodeName())) {
                if (columnIndex(TIME) != trIdx) {
                  trChild.select("td").flatMap {
                    td =>
                      //println(td.children().select("td"))
                      td.children().select("td")

                  }.map {
                    td =>
                      val lectureData: JSONObject = extractLectureInformationFromTd(multiLecturers, indexColumn, timeData, trIdx, td, extractedCourse, BLOCK, alternatives)
                      Some(lectureData)
                  }
                } else {
                  None
                }
              }
              else {
                None
              }
          }
        }
    }.filter(_.nonEmpty).map(_.get)
    "{\"title\":\"" + extractedTitle + "\"," + "\"scheduleData\":[" + scheduleData.mkString(",") + "]," + " \"scheduleDate\":" + scheduleDate.getMillis + "}"
  }

  private def parseBlockHeadline(schedule: String): Map[String, Int] = {
    val document = Jsoup.parse(schedule)
    val table = document.select("table").first()

    var columnIndex = Map[String, Int]()

    val headlineIndex = findTimeIndex(table)
    val headPattern = Pattern.compile("[a-zäöüA-ZÄÖÜ]+, \\d+[.]? [a-zäöüA-ZÄÖÜ]+")
    val headlineData = table.select("tr").get(headlineIndex).children()
    (0 to headlineData.size() - 1).foreach {
      headIdx =>
        val headText = headlineData.get(headIdx).text()
        val headMatcher = headPattern.matcher(headText)
        if (headMatcher.find()) {
          val theDate = Calendar.getInstance()
          val dateParts = headText.replaceAll("[.]", "").split(",")(1).trim.split(" ")
          val monthOption = EMonthMapper.findByName(dateParts(1))
          if (!monthOption.isPresent) {
            throw new IllegalArgumentException("search for month " + dateParts(1) + " but found nothing")
          }
          theDate.set(Calendar.MONTH, monthOption.get().getMonthconstant)
          theDate.set(Calendar.DAY_OF_MONTH, dateParts(0).toInt)
          theDate.set(Calendar.HOUR_OF_DAY, 0)
          theDate.set(Calendar.MINUTE, 0)
          theDate.set(Calendar.SECOND, 0)
          theDate.set(Calendar.MILLISECOND, 0)

          columnIndex += theDate.getTimeInMillis.toString -> headIdx

        } else if (TIME.equalsIgnoreCase(headText.trim)) {
          columnIndex += TIME -> headIdx
        }
    }
    columnIndex
  }

  private def parseAlternatives(document: Document) = {
    var result = List[Alternative]()
    @tailrec
    def hasAlternatives(fontSelect: Elements, fontIdx: Int = 0): Boolean = {
      if (fontIdx == fontSelect.size()) {
        false
      } else {
        val font = fontSelect.get(fontIdx)
        val alternativesPresent = font.text().replaceAll("\\s+", " ").contains("Raumbelegung")
        if (alternativesPresent) {
          true
        } else {
          hasAlternatives(fontSelect, fontIdx + 1)
        }
      }
    }

    if (hasAlternatives(document.select("font"))) {
      val fontsSelect = document.select("font")
      for (fontIdx <- 0 until fontsSelect.size()) {
        val font = fontsSelect.get(fontIdx)
        if (font.text().contains("====")) {
          val fonttext = font.text().split("br2n")
          val alternatives = (3 until fonttext.size).map { altIdx =>
            val line = fonttext(altIdx).replace(160.toChar,' ').replaceAll("\\s+", " ").trim
            val parts = line.split(" ")
            val day = EWeekdays.findConstantByName(parts(0).trim).get()
            val duration = parts(1).trim match {
              case "w" => WEEKLY
              case "g" => EVEN
              case "u" => UNEVEN
              case _ => WEEKLY
            }
            val hour = parts(2).trim
            val room = parts(3).trim
            val lecture = if (parts.length == 5) {
              parts(4).trim
            }
            else {
              parts(4) + " " + parts(5)
            }
            Alternative(day, duration, hour, room, lecture)
          }
          result ++= alternatives
        }
      }
    }
    result
  }
  @throws(classOf[Exception])
  private def parseRegular(schedule: String, multiLecturers: Map[String, List[String]], course: String, scheduleKind: EScheduleKind = REGULAR, title: String = ""): String = {
    /** Stores the meaning of each column by Index */
    val columnIndex = parseRegularHeadline(schedule)
    val indexColumn = columnIndex.map { case (key, value) => (value, key) }

    val document = Jsoup.parse(schedule.replaceAll("(?i)<br[^>]*>", "br2n"))
    val alternatives = parseAlternatives(document)
    val scheduleDate: DateTime = parseScheduleDate(document)


    val tbody = document.select("tbody").first()

    val trSelector = tbody.children()

    val scheduleData = (1 to trSelector.size() - 1).toList.flatMap {
      idx =>
        val tr = trSelector.get(idx)
        if (tr.children().size() == 0) {
          None
        } else {

          val timeData = parseTime(trSelector.get(idx).children().get(columnIndex(TIME)).text())

          (0 to tr.children().size() - 1).toList.flatMap {
            trIdx =>
              val trChild = tr.children().get(trIdx)
              if ("td".equalsIgnoreCase(trChild.nodeName())) {
                if (columnIndex(TIME) != trIdx) {
                  trChild.select("td").flatMap {
                    td =>
                      //println(td.children().select("td"))
                      td.children().select("td")

                  }.map {
                    td =>
                      val lectureData: JSONObject = extractLectureInformationFromTd(multiLecturers, indexColumn, timeData, trIdx, td, course, scheduleKind, alternatives)
                      Some(lectureData)
                  }
                } else {
                  None
                }
              }
              else {
                None
              }
          }
        }
    }.filter(_.nonEmpty).map(_.get)

    "{\"title\":\"" + title + "\"," + " \"scheduleData\":[" + scheduleData.mkString(",") + "]," + " \"scheduleDate\":" + scheduleDate.getMillis + "}"
  }
  @throws(classOf[Exception])
  def parseScheduleDate(document: Document): DateTime = {
    val dateString = document.select("center").find(p => p.text().startsWith("Stand vom:")) match {
      case None => "01.01.2016"
      case Some(element) => element.select("b").first().text()
    }
    val scheduleDate = DATEFORMAT.parseDateTime(dateString)
    scheduleDate
  }

  @throws(classOf[Exception])
  private def extractLectureInformationFromTd(multiLecturers: Map[String, List[String]], indexColumn: Map[Int, String], timeData: Time, trIdx: Int, td: Element, course: String, scheduleKind: EScheduleKind, alternatives:List[Alternative]): JSONObject = {
    val lectureKind = if (td.select("img").first().attr("src").contains("buch")) {
      LECTURE
    } else {
      EXERSISE
    }

    val content = td.text().replaceAll("br2n", "\n")
    val contentParts = content.split("\n")
    val lectureName = removeObsoleteChars(contentParts(0))
    val roomParts = contentParts(1).trim().replace(160.toChar, ' ').replaceAll("\\s+", " ").split(" ")
    val room = removeObsoleteChars(roomParts(0))

    val duration = if (roomParts.length > 1) {
      roomParts(1) match {
        case "w" => WEEKLY
        case "g" => EVEN
        case "u" => UNEVEN
        case _ => WEEKLY
      }
    } else {
      WEEKLY
    }
    val groupIndex = if (roomParts.length > 2) {
      roomParts(2)
    } else {
      ""
    }

    val docents = multiLecturers.getOrElse(contentParts(2), List(contentParts(2))).map( removeObsoleteChars )

    val lectureAlternatives: List[JSONObject] = extractAlternatives(indexColumn, trIdx, alternatives, room, lectureName)

    val uuid: String = mkChecksum(indexColumn, timeData, trIdx, lectureKind, lectureName, room, duration, docents)

    val lectureData = JSONObject(Map[String, Any](
      "time" -> JSONObject(Map(
        "startHour" -> timeData.startHour,
        "startMinute" -> timeData.startMinute,
        "stopHour" -> timeData.stopHour,
        "stopMinute" -> timeData.stopMinute,
        "weekday" -> indexColumn(trIdx)
      )),
      "duration" -> duration.toString,
      "room" -> room,
      "lectureName" -> lectureName,
      "lectureKind" -> lectureKind.name(),
      "groupIndex" -> groupIndex,
      "docents" -> JSONArray(docents),
      "course" -> JSONArray(List(course)),
      "scheduleKind" -> scheduleKind.name(),
      "longTitle" -> "",
      "uuid" -> uuid,
      "alternatives" -> JSONArray(lectureAlternatives)
    ))
    lectureData
  }

  @throws(classOf[Exception])
  def extractAlternatives(indexColumn: Map[Int, String], trIdx: Int, alternatives: List[Alternative], room: String, lectureName:String): List[JSONObject] = {
    val lectureAlternatives = if (room.contains("*")) {
      alternatives.filter(a => EWeekdays.valueOf(indexColumn(trIdx)) ==
        EWeekdays.valueOf(a.day.toString) && lectureName.replace(160.toChar, ' ').replaceAll("\\s+", "").trim
        .toUpperCase().contains(a.lecture.replaceAll("\\s+", ""))
      ).map {
        alt =>
          JSONObject(Map[String, Any](
            "weekday" -> alt.day.name(),
            "duration" -> alt.duration.name(),
            "hour" -> alt.hour,
            "room" -> alt.room,
            "lecture" -> alt.lecture
          ))
      }
    } else {
      List[JSONObject]()
    }
    lectureAlternatives
  }

  @throws(classOf[Exception])
  def mkChecksum(indexColumn: Map[Int, String], timeData: Time, trIdx: Int, lectureKind: ELectureKind, lectureName: String, room: String, duration: EDuration, docents: List[String]): String = {
    val sha512 = MessageDigest.getInstance("SHA-512")
    (List(timeData.startHour.toString.getBytes, timeData.startMinute.toString.getBytes, timeData.stopHour.toString.getBytes(), timeData.stopMinute.toString.getBytes(),
      indexColumn.getOrElse(trIdx, "").getBytes(), duration.toString.getBytes, room.getBytes(), lectureKind.toString.getBytes(), lectureName.getBytes()
    ) ++ docents.map(_.getBytes)).foreach(sha512.update)
    val sb = new StringBuilder
    sha512.digest().foreach { b =>
      sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1))
    }
    sha512.reset()
    val uuid = sb.toString()
    uuid
  }

  private def parseTime(timeString: String): Time = {

    val timeParts = timeString.split("[-]")
    val startParts = timeParts(0).split("[.:]")
    val endParts = timeParts(1).split("[.:]")


    val startHour = startParts(0).trim.toInt
    val startMinute = startParts(1).trim.toInt
    val stopHour = endParts(0).trim.toInt
    val stopMinute = endParts(1).trim.toInt

    Time(startHour, startMinute, stopHour, stopMinute)
  }

  private def parseRegularHeadline(schedule: String): Map[String, Int] = {
    /** Stores the meaning of each column by Index */
    var columnIndex = Map[String, Int]()
    val document = Jsoup.parse(schedule)

    val tbody = document.select("tbody").first()

    val trSelector = tbody.children()

    (0 to trSelector.size() - 1).foreach {
      idx =>
        val tr = trSelector.get(idx)
        (0 to tr.children().size() - 1).foreach {
          trIdx =>
            val trChild = tr.children().get(trIdx)
            if ("th".equalsIgnoreCase(trChild.nodeName())) {
              if (TIME.equalsIgnoreCase(trChild.text())) {
                columnIndex += TIME -> trIdx
              }
              else {
                val weekdayResult = EWeekdays.findConstantByName(trChild.text().trim)
                if (!weekdayResult.isPresent) {
                  columnIndex += trChild.text() -> trIdx
                } else {
                  columnIndex += weekdayResult.get().name() -> trIdx
                }
              }
            }
        }
    }

    columnIndex
  }

  implicit def elementsToList(nodes: Elements): List[Element] = {
    (0 to nodes.size() - 1).map {
      idx =>
        nodes.get(idx)
    }.toList
  }

  private def removeObsoleteChars(str:String) = {
    str.replaceAll("_"," ").trim
  }

}
