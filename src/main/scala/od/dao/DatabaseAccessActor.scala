package od.dao

import java.sql.PreparedStatement

import akka.actor.{Actor, ActorSelection}
import od.dao.DBConnectionPool.{RetrieveOneConnection, RetrievedConnection}

import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.ask
import akka.util.Timeout
import od.auxiliary.autoClose

trait DatabaseAccessActor extends Actor{

  //use actorSelection for retrieving, not ActorRef
  val connPoolActorRef:ActorSelection

  implicit val timeout:Timeout

  def requestForConn(actionId: Int):Unit = {
    connPoolActorRef ! RetrieveOneConnection(actionId)
  }

  def askForConn(actionId:Int):Future[RetrievedConnection] =
    ask(connPoolActorRef, RetrieveOneConnection(actionId)).mapTo[RetrievedConnection]

  def withPreparedStatement[T](prep_statement:String)(f: (PreparedStatement) => T)(implicit executionContext: ExecutionContext):Future[T] = {
    askForConn(1).map{
      x => autoClose(x.conn){
        conn =>
          autoClose(conn.prepareStatement(prep_statement))(f)
      }
    }
  }
}
