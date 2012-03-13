package actors

import java.util.{Calendar, Date}
import akka.util.duration._
import akka.util.Duration
import akka.actor.{Props, Actor}
import play.api.libs.concurrent.{Promise, Akka}

/**
 * @author KUOKA Yusuke
 */

class MyScheduler extends Actor {

  import MyScheduler._

  def receive = {
    case WaitUntilDate(d: Date) =>
      context.system.scheduler.scheduleOnce(dateToDuration(d), sender, new Date)
    case WaitUntilCalendar(c: Calendar) =>
      context.system.scheduler.scheduleOnce(calendarToDuration(c), sender, new Date)
  }
}

object MyScheduler {

  def dateToDuration(d: Date): Duration = {
    val calendarForDate = Calendar.getInstance()
    calendarForDate.setTime(d)

    calendarToDuration(calendarForDate)
  }

  def calendarToDuration(c: Calendar): Duration = {
    val calendarForNow = Calendar.getInstance()

    (c.getTimeInMillis - calendarForNow.getTimeInMillis) milliseconds
  }

}

case class WaitUntilDate(d: Date)
case class WaitUntilCalendar(c: Calendar)

object WaitUntil {
  def apply(d: Date): WaitUntilDate = WaitUntilDate(d)
  def apply(c: Calendar): WaitUntilCalendar = WaitUntilCalendar(c)
}
