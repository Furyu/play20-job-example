package actors

import play.api.Logger

import akka.actor.Actor
import akka.util.duration._


/**
 * @author KUOKA Yusuke
 */

class SleepyActor extends Actor {
  def receive = {
    case Delay(ms) =>
      Logger.info("SleepyActor is sleeping %d ms".format(ms))
//      Thread.sleep(ms)
//      sender ! ms
      context.system.scheduler.scheduleOnce(1000 milliseconds, sender, ms)
  }
}

case class Delay(ms: Long)