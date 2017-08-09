package od.dao

import java.sql.{Connection, SQLException}

import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.concurrent.duration._
import od.auxiliary.autoClose
import akka.pattern.pipe

object FileInfoRetriever{
  sealed trait DBRetriveRequest{
    def buildResponse(conn: Connection):DBRetrieveResponse
  }

  case class CheckIfHashExist(hash:String) extends DBRetriveRequest{
    override def buildResponse(conn: Connection): DBRetrieveResponse = {

      try{
        //autoCloser will handle the closing of every Closeable Object, and we simply handle all the sqlException
        autoClose(conn){
          conn_ => autoClose(conn_.prepareStatement("SELECT id FROM offlinefile where hash = ?")){
            stmt =>
            stmt.setString(1, hash)
            autoClose(stmt.executeQuery()){
              result => IfHashExist(result.next())
            }//end of query closer

          }//end of stmt closer

        }//end of conn closer

      }//end of try block
      catch{
        case sqlEx:SQLException =>
          IfHashExist(false)
        case _ =>
          IfHashExist(false)
      }

    }//end of buildResponse function
  }




  sealed trait DBRetrieveResponse
  case class IfHashExist(isExisted:Boolean) extends DBRetrieveResponse



}
class FileInfoRetriever extends DatabaseAccessActor with ActorLogging{
  import od.dao.FileInfoRetriever.DBRetriveRequest

  implicit val executionContext = context.system.dispatcher
  val connPoolActorRef = context.actorSelection("/ConnPoolActor")
  val timeout = 10.seconds

  def receive = {
    case request: DBRetriveRequest =>
      askForConn(1).map{x => request.buildResponse(x.conn)} pipeTo sender()
    case _ =>
      //TODO do nothing here. Possibly memory leak because of timeout of Future, and Connection object pass into the actor
  }
}
