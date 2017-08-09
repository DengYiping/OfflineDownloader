package od.dao

import akka.actor.{Actor, ActorSelection}
import od.dao.DBConnectionPool.{RetrieveOneConnection, RetrievedConnection}

import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout

trait DatabaseAccessActor extends Actor{

  //use actorSelection for retrieving, not ActorRef
  val connPoolActorRef:ActorSelection

  implicit val timeout:Timeout

  def requestForConn(actionId: Int):Unit = {
    connPoolActorRef ! RetrieveOneConnection(actionId)
  }

  def askForConn(actionId:Int):Future[RetrievedConnection] =
    ask(connPoolActorRef, RetrieveOneConnection(actionId)).mapTo[RetrievedConnection]
}
