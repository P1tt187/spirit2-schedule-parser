package org.fhs.spirit.model

import org.fhs.spirit.scheduleparser.enumerations.{EDuration, EWeekdays}

/**
  * @author fabian 
  *         on 17.04.16.
  */
case class Alternative(day: EWeekdays, duration: EDuration, hour: String, room: String, lecture: String)
