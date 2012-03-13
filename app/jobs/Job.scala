package jobs

/**
 * @author KUOKA Yusuke
 */

import play.api.libs.concurrent._

import akka.pattern.ask
import java.util.Date
import akka.util.{Duration, Timeout}
import akka.actor.{Actor, Props, ActorRef}

object Job {

  import play.api.Play.current

  import jobs.dateToDuration

  /**
   * 関数をバックグラウンドで実行するためのアクターへの参照
   */
  val jobInvoker = Akka.system.actorOf(Props[JobInvoker])

  /**
   * PlayデフォルトのアクターシステムのScheduler
   */
  val scheduler = Akka.system.scheduler

  /**
   * 非同期で、指定した関数をすぐに評価する。
   * @param f 関数
   * @param timeout 関数の評価完了までのタイムアウト
   * @tparam T 関数の結果型
   * @return 関数の評価結果のPromise
   */
  def now[T](f: () => T)(implicit timeout: Timeout): Promise[T] =
    (jobInvoker ? Invoke(f)).asPromise.map(_.asInstanceOf[T])

  /**
   * 非同期で、指定した時間待ったあと関数を評価する。
   * @param delay 関数を評価するまでの待ち時間
   * @param f 待ち時間のあと評価する関数
   * @param timeout 関数の評価完了までのタイムアウト。この時間より関数の実行に時間がかかった場合、AskTimeoutException が発生する。
   * @tparam T 関数の結果型
   * @return 関数の評価結果のPromise
   */
  def scheduleOnce[T](delay: Duration)(f: () => T)(implicit timeout: Timeout): Promise[T] =
    (jobInvoker ? InvokeDelayed(f, delay)).asPromise.map(_.asInstanceOf[T])

  /**
   * 非同期で、指定した日時に一回だけ処理を実行する。
   * @param date 日時
   * @param f 指定日時に実行する処理を記述した関数
   * @return
   */
  def scheduleAt(date: Date)(f: () => Unit) {
    scheduler.scheduleOnce(dateToDuration(date), jobInvoker, Invoke(f))
  }

  /**
   * 指定したディレイと間隔で繰り返し、処理を実行する。
   * @param delay 初回の実行までのディレイ
   * @param interval 初回の実行後、処理を繰り返し実行する間隔
   * @param f 処理内容を記述した関数
   * @return
   */
  def schedule(delay: Duration, interval: Duration)(f: () => Unit) {
    scheduler.schedule(delay, interval, jobInvoker, Invoke(f))
  }

}

/**
 * ジョブをデフォルトのアクターシステムで実行し、結果を返送する。
 * @param job ジョブの内容を表す関数。
 */
case class Invoke(job: () => Any)

/**
 * 指定した時間待ってから、ジョブを実行し、結果を返送する。
 * @param job ジョブの内容を表す関数
 * @param delay ジョブが開始するまでのディレイ。
 */
case class InvokeDelayed(job: () => Any, delay: Duration)

/**
 * ジョブを非同期で実行するアクター
 */
class JobInvoker extends Actor {

  import play.api.Play.current

  /**
   * Playが管理しているデフォルトのアクターシステム
   */
  val scheduler = Akka.system.scheduler

  /**
   * ジョブの実行結果を sender とは別の ActorRef へ送信する。
   * @param job
   * @param origin
   */
  case class InvokeWithOrigin(job: () => Any, origin: ActorRef)

  def receive = {
    case Invoke(job) =>
      sender ! job()
    case InvokeDelayed(job, delay) =>
      scheduler.scheduleOnce(delay, self, InvokeWithOrigin(job, sender))
    case InvokeWithOrigin(job, origin) =>
      origin ! job()
  }
}