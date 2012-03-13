package actors

import play.api.Play.current
import play.api.Logger
import play.api.libs.concurrent.Akka
import akka.actor.{Props, Actor}
import akka.event.Logging

/**
 * @author KUOKA Yusuke
 */

class LoggedActor extends Actor {

  implicit val log = Logging(context.system, this)

  import akka.event.LoggingReceive

  def receive = LoggingReceive {
    case msg: String ⇒
      Logger.info("msg = %s".format(msg))
      sender ! msg
    case millisecs: Long ⇒
      Thread.sleep(millisecs)
      sender ! "Slept %d msecs".format(millisecs)
    case SimulateException =>
      // ActorSystemがこの例外をキャッチして、Actorが再起動されるはず。
      throw new RuntimeException("This is a simulated exception.")
  }

}

object SimulateException

object LoggedActor {

  val actor = Akka.system.actorOf(Props[LoggedActor], name = "loggedActor")

  import akka.util.duration._
  Akka.system.scheduler.schedule(0 seconds, 10 seconds, actor, "tick")

}