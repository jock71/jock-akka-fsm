package jok.akka.fsm

import akka.actor._

import scala.concurrent.duration._



/**
  * Created by andrea on 11/11/15.
  */
class TestAkkaFsm {

}

trait State

trait StateData

case class AwakeData(
                      gotoSleepTimer: Option[String],
                      workId: Option[Int]) extends StateData

case class SleepingData(wakeUpTimer: Option[String]) extends StateData

// FSM States
case object Waiting extends State

case object Sleeping extends State

case object Working extends State

// FSM Messages
case class Work(wordId: Int)

case object WakeUp

case object GoToSleep

case object WorkCompleted

class DayWorker extends Actor
with FsmMod[State, StateData] {
  val goToSleepTmr = "goToSleep"
  // let's start our day awake
  println("staring FSM, initializing data for Waiting state (since first entry does not work)")
  setTimer(goToSleepTmr, GoToSleep, 10 second)
  startWith(Waiting, AwakeData(Some(goToSleepTmr), None))

  onEntry(Waiting) {
    case data@AwakeData(Some(_), None) =>
      println("Waiting.onEntry() fine, we are entering/re-entering Waiting state with valid goToSleep timer and no workinprogress")
      data
    case _ =>
      println("Waiting.onEntry() just waken-up let's create goToSleepTmr")
      setTimer(goToSleepTmr, GoToSleep, 10 second)
      AwakeData(Some(goToSleepTmr), None)
  }
  when(Waiting) {
    case Event(Work(workId), data: AwakeData) =>
      println(s"Waiting.when(Work($workId)) event received while Waiting. Let's start working")
      goto(Working) using data.copy(workId = Some(workId))
    case Event(GoToSleep, _) =>
      println("Waiting.when(GoToSleep) event received: time for sleeping...")
      goto(Sleeping)
    case Event(WakeUp, _) =>
      println("Waiting.when(WakeUp) event received: I'm already awake, no need to awake me again")
      stay()
    case any =>
      println("ERROR: Waiting.when($any) received")
      throw new Exception
  }
  onExit(Waiting) {
    case data =>
      println(s"Waiting.onExit() with $data data")
      data
  }

  onEntry(Working) {
    case data@AwakeData(Some(_), Some(workId)) =>
      println(s"Working.onEntry(): enter into Working state. In two seconds I should finish workID:$workId")
      setTimer("workInProgressTimer", WorkCompleted, 2 second)
      data
    case data =>
      println(s"ERROR:Working.onEntry() entering Working state with invalid data $data")
      data
  }
  when(Working) {
    case Event(Work(workId), _) =>
      println(s"Working.when(Work($workId)) event received while working on another job. Discarding: only one work per time")
      stay()
    case Event(GoToSleep, AwakeData(_, Some(wip))) =>
      println("Working.when(GoToSleep) event received: time to sleep aborting current work...")
      goto(Sleeping)
    case Event(WakeUp, _) =>
      println("Working.when(WakeUp) event received: I'm already awake, no need to awake me again")
      stay()
    case Event(WorkCompleted, awakeData: AwakeData) =>
      println(s"Working.when(WorkCompleted) event received: state, workId:${awakeData.workId} completed ")
      goto(Waiting) using awakeData.copy(workId = None)
    case any =>
      println(s"ERROR working.when($any) event received")
      throw new Exception
  }
  onExit(Working) {
    case data =>
      println(s"Working.exit() with $data data")
      cancelTimer("workInProgressTimer")
      data
  }

  onEntry(Sleeping) {
    case any =>
      println(s"Sleeping.onEntry with $any data")
      val wakeUpTimer = "wakeUpTimer"
      setTimer(wakeUpTimer, WakeUp, 10 second)
      SleepingData(Some(wakeUpTimer))
  }
  when(Sleeping) {
    case Event(Work(workId), _) =>
      println(s"Sleeping.when(Work($workId)) event received. Discarding request: don't bother, I'm sleeping")
      stay()
    case Event(GoToSleep, _) =>
      println(s"Sleeping.when(GoToSleep) event received. I'm sleeping, no need to tell to sleep...")
      stay()
    case Event(WakeUp, _) =>
      println(s"Sleeping.when(WakeUp) event received. It's time to wake up")
      goto(Waiting)
    case any =>
      println(s"ERROR: Sleeping.when($any) event received")
      throw new Exception
  }
  onExit(Sleeping) {
    case data@SleepingData(Some(wakeUpTimer)) =>
      println(s"Sleeping.onExit with a wakeUpTimer that should be expired")
      cancelTimer(wakeUpTimer)
      SleepingData(None)
    case data =>
      println(s"ERROR: Sleeping.onExit exit from Sleeping with $data data")
      throw new Exception
  }


}


object TestAkkaFsmWithEntryExitTrait extends App {


  // Create the 'helloakka' actor system
  val system = ActorSystem("test")

  // Create the 'greeter' actor
  //val greeter = system.actorOf(Props[Greeter], "greeter")

  val hsm = system.actorOf(Props[DayWorker], "dayWorker")

  // Create an "actor-in-a-box"
  val inbox = Inbox.create(system)

  hsm ! Work(1)
  system.scheduler.scheduleOnce(1.seconds, hsm, Work(2))(system.dispatcher, hsm)
  system.scheduler.scheduleOnce(4.seconds, hsm, Work(3))(system.dispatcher, hsm)
  system.scheduler.scheduleOnce(5.seconds, hsm, Work(4))(system.dispatcher, hsm)
  system.scheduler.scheduleOnce(15.seconds, hsm, Work(5))(system.dispatcher, hsm)

  system.scheduler.schedule(20.seconds, 3.seconds, hsm, Work(888))(system.dispatcher, hsm)

}

