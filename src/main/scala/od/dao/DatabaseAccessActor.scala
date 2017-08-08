package od.dao

import akka.actor.{Actor, ActorSelection}
import od.dao.DBConnectionPool.RetrieveOneConnection


trait DatabaseAccessActor extends Actor{

  //use actorSelection for retrieving, not ActorRef
  val connPoolActorRef:ActorSelection

  def askForConn(actionId: Int):Unit = {
    connPoolActorRef ! RetrieveOneConnection(actionId)
  }
}
