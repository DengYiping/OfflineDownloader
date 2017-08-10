package od.dao

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.concurrent.duration._
import od.auxiliary.{TryWith, autoClose}
import akka.pattern.pipe


object FileInfoRetriever{
  sealed trait DBRetriveRequest{
    def buildResponse(conn: Connection):DBRetrieveResponse
  }

  case class CheckIfHashExist(hash: String) extends DBRetriveRequest{
    override def buildResponse(conn: Connection): DBRetrieveResponse = {
        //autoCloser will handle the closing of every Closeable Object, and we simply handle all the sqlException
        TryWith(conn){
          conn_ => autoClose(conn_.prepareStatement("SELECT id FROM offlinefile where hash = ?")){
            stmt =>
            stmt.setString(1, hash)
            autoClose(stmt.executeQuery()){
              result => IfHashExist(result.next())
            }//end of query closer

          }//end of stmt closer
        }.getOrElse(IfHashExist(false))//end of conn closer, drop all the exception inside

    }//end of buildResponse function
  }

  case class FileInfo(hash:String, size:Int, name: String, create_time:LocalDateTime)

  //TODO add testings
  case class RetrieveFileInfoByHash(hash: String) extends DBRetriveRequest{
    override def buildResponse(conn: Connection): DBRetrieveResponse = {
      TryWith(conn){
        conn_ => autoClose(conn_.prepareStatement("SELECT size,name,create_time FROM offlinefile where hash = ?")){
          stmt =>
            stmt.setString(1, hash)
            autoClose(stmt.executeQuery()){
              resultSet =>
                if(resultSet.next()){
                  val size = resultSet.getInt(1)
                  val name = resultSet.getString(2)
                  val create_time = resultSet.getTimestamp(3).toLocalDateTime
                  FileInfoRetrieved(Some(FileInfo(hash,size,name,create_time)))
                }
                else{
                  FileInfoRetrieved(None)
                }
            }//end of query closer
        }//end of stmt closer
      }.getOrElse(FileInfoRetrieved(None))
    }
  }

  sealed trait DBRetrieveResponse
  case class IfHashExist(isExisted: Boolean) extends DBRetrieveResponse
  case class FileInfoRetrieved(fileInfo: Option[FileInfo]) extends DBRetrieveResponse


}
class FileInfoRetriever extends DatabaseAccessActor with ActorLogging{
  import od.dao.FileInfoRetriever.DBRetriveRequest

  implicit val executionContext = context.system.dispatcher
  val connPoolActorRef = context.actorSelection("../ConnPoolActor")
  val timeout = 10.seconds

  def receive = {
    case request: DBRetriveRequest =>
      askForConn(1).map{x => request.buildResponse(x.conn)} pipeTo sender()
    case _ =>
      //TODO do nothing here. Possibly memory leak because of timeout of Future, and Connection object pass into the actor
  }
}
