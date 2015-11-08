package jok.akka.fsm

import akka.actor.{Actor, Stash}
import jok.akka.fsm.Fsm.{Entry, Exit}

/**
 * Created by andrea on 07/11/15.
 * Simple abstract Actor extension providing methods to
 * implements exit/entry conditions
 */
abstract class Fsm extends Actor
with Stash {

  def changeState(newState: Receive): Unit = {
    // println("changeState($newState)")
    context.become(waitExit(newState), discardOld = false)
    self ! Exit(newState)
  }

  def waitExit(newState: Receive): Receive = {
    case exit: Exit =>
      // println("waitExit received Exit")
      self ! exit
      context.unbecome();
    case any =>
      // println(s"waitExit msg $any Stashed")
      stash()
  }

  def waitEntry(entryState: Receive): Receive = {
    case Entry =>
      //println("waitEntry Entry received");
      self ! Entry
      context.unbecome()
      context.become(entryState, discardOld = true)
    case any =>
      // println(s"waitEntry msg $any Stashed");
      stash()
  }

  def doExit(exit: Exit): Unit = {
    //println("executing doExit")
    context.become(waitEntry(exit.newState), discardOld = false)
    self ! Entry
  }

  def doEntry(): Unit = {
    // println("executing doEntry")
    unstashAll()
  }

}

object Fsm {

  case class Exit(newState: Actor.Receive)

  case object Entry

}
