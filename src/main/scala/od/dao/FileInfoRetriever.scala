package od.dao

import akka.actor.{Actor, ActorLogging, ActorRef}

class FileInfoRetriever extends DatabaseAccessActor with ActorLogging{
  val connPoolActorRef = context.actorSelection("/ConnPoolActor")
  def receive = {
    case _ =>
  }
}
