package jok.akka.fsm

import akka.actor.Actor.Receive
/**
  * Created by andrea on 08/11/15.
 */
trait Fsm {

  case object Exit
  case object Entry

  def changeState(targetState: Receive): Unit
}
