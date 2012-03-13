package controllers

import play.api.mvc._
import play.api.Logger

import akka.util.Timeout
import akka.util.duration._

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

/**
 * @author KUOKA Yusuke
 */

object JobExamples extends Controller {

  /**
   * ジョブをすぐに、一回きり実行する例。
   */
  def jobNow = Action {

    import jobs._

    implicit val timeout = Timeout(1 second)

    Async {
      // すぐに "2" というレスポンスが返るはず。
      Job.now(() => 1 + 1).map(result => Ok(result.toString))
    }
  }

  /**
   * ジョブを一定時間後に、一回きり実行する例。
   * @return
   */
  def jobOnce = Action {

    import jobs._

    implicit val timeout = Timeout(3 seconds)

    Async {
      Job.scheduleOnce(1 second){ () =>
        // リクエストからだいたい１秒後にレスポンスが返るはず
        val df = new SimpleDateFormat("HH:mm:ss")
        Ok(df.format(new Date))
      }
    }
  }

  def jobDate = Action {

    import jobs._

    // 次の分の0秒に実行する
    val cal = Calendar.getInstance()
    cal.add(Calendar.MINUTE, 1)
    cal.set(Calendar.SECOND, 0)

    Job.scheduleAt(cal.getTime) { () =>
      // 次の0秒にログが出力されるはず。
      val df = new SimpleDateFormat("HH:mm:ss")
      Logger.info("Invoked at " + df.format(new Date))
    }

    // リクエスト後、"Scheduled." というレスポンスがすぐに返るはず。
    Ok("Scheduled.")
  }

  def jobSchedule = Action {

    import jobs._

    Job.schedule(3 seconds, 5 seconds) { () =>
      // リクエストから３秒後に１回目、それ以降は５秒おきにログが出力されるはず。
      val df = new SimpleDateFormat("HH:mm:ss")
      Logger.info("Invoked at " + df.format(new Date))
    }

    // すぐに "Scheduled." というレスポンスが返るはず。
    Ok("Scheduled.")
  }

}
