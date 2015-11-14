package jok.akka.fsm

import akka.actor.Actor

/**
  * Created by andrea on 08/11/15.
  */
abstract class EntryExitFsm extends Actor with Fsm {
  private var currentState: Receive = receive

  def changeState(targetState: Receive): Unit = {
    currentState(Exit)
    targetState(Entry)
    context.become(targetState, discardOld = true)
  }
}
