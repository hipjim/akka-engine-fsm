package something

import akka.actor.{Props, ActorSystem}
import akka.actor.{FSM, Actor}
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global

sealed trait EngineState

case object RUNNING extends EngineState
case object STOPPED extends EngineState
case object STARTING extends EngineState
case object STOPPING extends EngineState

sealed trait Action
case object StartEngine extends Action
case object EngineStarted extends Action
case object StopEngine extends Action
case object EngineStopped extends Action

class EngineFsmActor extends Actor with FSM[EngineState, String] {

  startWith(STOPPED, "")

  when(STOPPED) {
    case Event(StartEngine, _) => goto(STARTING)
  }

  when(RUNNING) {
    case Event(StopEngine, _) => goto(STOPPING)
  }

  when(STOPPING) {
    case Event(EngineStopped, _) => goto(STOPPED)
  }

  when(STARTING) {
    case Event(EngineStarted, _) => goto(RUNNING)
  }

  onTransition {
    case STOPPED -> STARTING => {
      println("Engine is starting in 5 seconds")
      scheduleOnce(EngineStarted, 5)
    }

    case STARTING -> RUNNING => {
      println("Engine is running for 10 seconds")
      scheduleOnce(StopEngine, 10)
    }

    case RUNNING -> STOPPING => {
      println("Engine will stop in 5 seconds")
      scheduleOnce(EngineStopped, 5)
    }

    case STOPPING -> STOPPED => {
      println("Engine is stopped")
      context.stop(self)
      context.system.shutdown()
    }
  }

  private def scheduleOnce(action:Action, duration:Long) {
    context.system.scheduler.scheduleOnce(FiniteDuration(duration, TimeUnit.SECONDS), self, action)
  }

  initialize

}

object Runner extends App {
  val system = ActorSystem("akka-fsm")
  val actor = system.actorOf(Props[EngineFsmActor], "engine-actor")
  actor ! StartEngine
}
