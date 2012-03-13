package controllers

import play.api.mvc._
import play.api.Play.current
import com.typesafe.config.ConfigFactory
import play.api.Logger
import actors._
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

// ドキュメントには import play.libs.Akka._ とあるが、間違い。
import play.api.libs.concurrent._

import akka.actor._

import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._


trait Computation {

  def longComputation(): String = {
    Thread.sleep(3000)
    1 + 1 toString
  }
}

/**
 * @author KUOKA Yusuke
 */

object AkkaExamples extends Controller with Computation {

  def akkaToPlay = Action {
    Async {
      implicit val timeout = Timeout(1 seconds)

      //
      //                  akka.dispatch.Future[Any]
      // -- mapTo ------> akka.dispatch.Future[String]
      // -- asPromise --> play.api.libs.concurrent.Promise[String]
      // -- map --------> play.api.libs.concurrent.Promise[play.api.mvc.Result[String]]
      //
      // 1. (LoggedActor.acot ? "message") は Actorへメッセージを送信し、akka.dispatch.Future[Any] を返す。
      // 2. mapTo[String] により akka.dispatch.Future[String] へ変換する。
      // 3. asPromise により play.api.libs.concurrent.Promise[String] へ変換する。
      // 4. Actorから返信が来たら、 { msg => Ok(msg) } というブロックを評価してHTTPレスポンスを返す。
      (LoggedActor.actor ? "message1").mapTo[String].asPromise.map { msg =>
        Ok(msg)
      }
    }
  }

  def executeBlockAsync = Action {
    Async {
      implicit val timeout = Timeout(1 seconds)

      //
      //            play.api.libs.concurrent.Promise[String]
      // -- map --> play.api.libs.concurrent.Promise[play.api.mvc.Result[String]]
      //
      // 1. longComputation()　は呼び出してから3秒後にStringを返す関数。
      // 2. Akka.future { longComputation() } により、Promise[String] が返される。
      // 3. リクエストから3秒後に longComputation() の結果resultに対して、
      //    { result => Ok("Got: " + result) }
      //    が呼び出される。
      Akka.future { longComputation() }.map { result =>
        Ok("Got: " + result)
      }
    }
  }

  // 次の例外を発生させて、500 Internal Server Error となるはず。
  // akka.pattern.AskTimeoutException: null
  def checkTimeout = Action {
    Async {
      implicit val timeout = Timeout(1 seconds)

      (LoggedActor.actor ? 1500L).mapTo[String].asPromise.map { msg =>
        Ok(msg)
      }
    }
  }
  
  def checkPoolSize = Action {
    Async {
      // implicit val timeout = Timeout(0.5 seconds) にすればタイムアウト発生
      implicit val timeout = Timeout(1.5 seconds)

      // 設定項目については Akka のドキュメントを参照。
      // Dispatchers (Scala) — Akka Documentation
      // http://akka.io/docs/akka/2.0-RC1/scala/dispatchers.html
      val config = ConfigFactory.parseString("""
        akka.loglevel = DEBUG
        akka.actor.debug {
          receive = on
          autoreceive = on
          lifecycle = on
        }
        akka.actor.default-dispatcher = {
                  executor = "fork-join-executor"
                  fork-join-executor {
                      parallelism-min = 2
                      parallelism-factor = 1.0
                      parallelism-max = 2
                  }
              }
        my-dispatcher {
                  executor = "fork-join-executor"
                  fork-join-executor {
                      parallelism-min = 1
                      parallelism-factor = 1.0
                      parallelism-max = 1
                  }
              }
        """)
      val system = ActorSystem.create("tempSystem", config)
      val actors = for (n <- 1 to 2) yield system.actorOf(Props[SleepyActor]/*.withDispatcher("my-dispatcher")*/, "myActor" + n)

      def askToSleep(actor: ActorRef) = {
        Logger.info("Asking an actor to sleep for 1000 ms")

        val reply = (actor ? Delay(1000)).mapTo[Long]

        Logger.info("Asked an actor to sleep for 1000 ms")

        reply
      }
      // これだと並列実行されない!
      // for {
      //   num1 <- askToSleep(actors(0))
      //   num2 <- askToSleep(actors(1))
      // } yield (num1, num2)
      //
      // こうする
      val p1 = askToSleep(actors(0))
      val p2 = askToSleep(actors(1))
      val p3 = for {
        num1 <- p1
        num2 <- p2
      } yield (num1, num2)
      p3.asPromise.map { result =>
        system.shutdown()
        Ok("Done " + result)
      }
      // ここで問題。
      // タイムアウトは 1.5 秒
      // actor1とactor2はそれぞれメッセージ受信から1秒後に応答を返す。
      // リクエストから Ok("Done" ...) のレスポンスを返すまでに、タイムアウトは発生するか？
    }
  }
  
  def scheduleAtDate = Action {

    import java.text.SimpleDateFormat
    import java.util.Calendar
    import java.util.Date
    
    import akka.util.Duration
    
    implicit val timeout = Timeout(2 second)
    
    val myScheduler = Akka.system.actorOf(Props[MyScheduler])

    def calendarOneSecondFromNow = {
      val cal = Calendar.getInstance()
      cal.add(Calendar.SECOND, 1)
      cal
    }

    val df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    Async {
      val akkaFuture = for {
        one <- (myScheduler ? WaitUntil(calendarOneSecondFromNow))
        two <- (myScheduler ? WaitUntil(calendarOneSecondFromNow.getTime))
      } yield Ok("Done. (one=%s, two=%s)".format(df.format(one), df.format(two)))
      akkaFuture.asPromise
    }
  }

}
