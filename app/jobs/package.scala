package jobs

import java.util.{Calendar, Date}
import akka.util.Duration
import akka.util.duration._

/**
 * @author KUOKA Yusuke
 */

package object jobs {

  implicit def dateToDuration(d: Date): Duration = {
    val calendarForDate = Calendar.getInstance()
    calendarForDate.setTime(d)

    calendarToDuration(calendarForDate)
  }

  implicit def calendarToDuration(c: Calendar): Duration = {
    val calendarForNow = Calendar.getInstance()

    (c.getTimeInMillis - calendarForNow.getTimeInMillis) milliseconds
  }

}
