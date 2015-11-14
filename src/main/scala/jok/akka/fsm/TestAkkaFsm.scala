package jok.akka.fsm

import akka.actor.{Actor, ActorRef, FSM}

import scala.concurrent.duration._


trait EnterExit[S, SD] {
  this: FSM[S, SD] ⇒


  private def _onEnter(state: S): Unit = {
    entryMap.get(state) match {
      case Some(enterHandler) => enterHandler(nextStateData)
      case _ => // no entry defined for this state
    }
  }

  private def _onExit(state: S): Unit = {
    exitMap.get(state) match {
      case Some(exitHandler) => exitHandler(stateData)
      case _ => // no exit handler defined for this state
    }
  }


  onTransition {
    case (from: S, to: S) =>
      try _onExit(from) finally _onEnter(to)
  }

  def onEntry(state: S)(onEnterFn: PartialFunction[SD, Unit]): Unit = {
    entryMap(state) = onEnterFn
  }


  def onExit(state: S)(onExitFn: PartialFunction[SD, Unit]): Unit = {
    exitMap(state) = onExitFn
  }

  private val entryMap = new scala.collection.mutable.HashMap[S, PartialFunction[SD, Unit]]()
  private val exitMap = new scala.collection.mutable.HashMap[S, PartialFunction[SD, Unit]]()
}

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

class DayWorker(name: String, left: ActorRef, right: ActorRef) extends Actor
with FSM[State, StateData]
with EnterExit[State, StateData] {

  // let's start our day awake
  startWith(Waiting, AwakeData(None, None))

  onEntry(Waiting) {
    case AwakeData(Some(_), None) =>
      println("fine, we are entering/re-entering Waiting state with valid goToSleep timer and no workinprogress")
    case _ => // no valid data while entering Waiting state (refresh AwakeData with valid info)
      setTimer("goToSleep", GoToSleep, 10 second)
      println("Waiting.onEntry(%s)".format(name))
      stay() using AwakeData(Some("goToSleep"), None)
  }
  when(Waiting) {
    case Event(Work(workId), data: AwakeData) =>
      println(s"Work($workId) event received while Waiting. Let's start working")
      goto(Working) using data.copy(workId = Some(workId))
    case Event(GoToSleep, _) =>
      println("time for sleeping...")
      goto(Sleeping)
    case Event(WakeUp, _) =>
      println("I'm already awake, no need to awake me again")
      stay()
  }
  onExit(Waiting) {
    case tc: AwakeData =>
      println("Waiting.onExit(%s)".format(name))
    case tc: StateData =>
  }

  onEntry(Working) {
    case AwakeData(Some(_), Some(workId)) =>
      println(s"enter into Working state. In a second I should finish $workId")
      setTimer("workInProgressTimer", WorkCompleted, 1 second)
    case any =>
      println(s"Error:entering Working state with invalid data $any")
  }
  when(Working) {
    case Event(Work(workId), _) =>
      println(s"Work($workId) event received while working. Discarding: only one work per time")
      stay()
    case Event(GoToSleep, AwakeData(_, Some(wip))) =>
      println("time to sleep aborting current work...")
      goto(Sleeping)
    case Event(WakeUp, _) =>
      println("I'm already awake, no need to awake me again")
      stay()
    case any =>
      println("In Working $any received")
      stay()
  }
  onExit(Working) {
    case _ =>
      println("exit from Working")
      cancelTimer("workInProgressTimer")
  }

  onEntry(Sleeping) {
    case _ =>
      val wakeUpTimer = "wakeUpTimer"
      setTimer(wakeUpTimer, WakeUp, 10 second)
      stay using SleepingData(Some(wakeUpTimer))
  }
  when(Sleeping) {
    case Event(Work(workId), _) =>
      println(s"Work($workId) event received while sleeping. Discarding: don't bother, I'm sleeping")
      stay()
    case Event(GoToSleep, _) =>
      println("I'm sleeping, no need to tell to sleep...")
      stay()
    case Event(WakeUp, _) =>
      println("It's time to wake up")
      goto(Waiting)
    case any =>
      println("In Sleeping $any received")
      stay()
  }
  onExit(Sleeping) {
    case SleepingData(Some(wakeUpTimer)) =>
      cancelTimer(wakeUpTimer)
    case any =>
      println("exit from Sleeping with $any data")
  }


}


