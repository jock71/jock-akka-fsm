import akka.actor.{ActorSystem, Inbox, Props}
import jok.akka.fsm.Fsm
import jok.akka.fsm.Fsm._

import scala.concurrent.duration._


//object TestEntryExitFsm {
case object Toggle

//}

class TestEntryExitFsm extends Fsm {


  def stateOff: Receive = {
    case Entry =>
      println("stateOff:Entry")
      doEntry()

    case exit: Exit =>
      println("stateOff:Exit")
      doExit(exit)

    case Toggle =>
      println("stateOff:toggle message received")
      changeState(stateOn)

    case unknown =>
      println(s"stateOff: received unknown $unknown")

  }

  def stateOn: Receive = {
    case Entry =>
      println("stateOn:Entry")
      doEntry()

    case exit: Exit =>
      println("stateOn:Exit")
      doExit(exit)

    case Toggle =>
      println("stateOn:toggle message received")
      changeState(stateOff)

    case unknown =>
      println(s"stateOn: received unknown $unknown")

  }


  override val receive: Receive = stateOff

}


object HelloJokFsm extends App {


  // Create the 'helloakka' actor system
  val system = ActorSystem("helloakka")

  // Create the 'greeter' actor
  //val greeter = system.actorOf(Props[Greeter], "greeter")

  val hsm = system.actorOf(Props[TestEntryExitFsm], "hfsm")

  // Create an "actor-in-a-box"
  val inbox = Inbox.create(system)

  hsm ! Entry

  system.scheduler.schedule(0.seconds, 5.second, hsm, Toggle)(system.dispatcher, hsm)

  hsm ! Toggle
  hsm ! Toggle


}

