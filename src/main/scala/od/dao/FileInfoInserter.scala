package od.dao

import akka.actor.{Actor, ActorLogging}
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

import akka.event.Logging
import od.auxiliary.{ConfigLoad, TryWith, autoClose}
import od.dao.DBConnectionPool.{RetrieveOneConnection, RetrievedConnection}

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration._
object FileInfoInserter{
  case class FileInfo(hash:String, size:Int, name: String)
}

class FileInfoInserter extends DatabaseAccessActor with ActorLogging{
  import FileInfoInserter.FileInfo
  implicit val executionContext = context.system.dispatcher

  //defined in DatabaseAccessActor
  val connPoolActorRef = context.actorSelection("/ConnPoolActor")
  val timeout = 10.seconds
  val queue = new mutable.Queue[FileInfo]()
  def receive = {


    case f_info:FileInfo =>
      queue.enqueue(f_info)
      requestForConn(1)


    case RetrievedConnection(_, conn) =>
      //pop out the info that need to be stored
      if(!queue.isEmpty){
        val fileInfo = queue.dequeue()


        val result:Future[Int] = Future{
          //wrap it into future so that it don't block

          try{
            //to protect SQL insertion
            autoClose(conn){
              conn_ =>
                //prepare statement
                autoClose(conn.prepareStatement("INSERT INTO offlinefile (hash, size, name) VALUES (?, ?, ?)")){
                  stmt =>
                    stmt.setString(1,fileInfo.hash)
                    stmt.setInt(2,fileInfo.size)
                    stmt.setString(3, fileInfo.name)

                    //insert
                    stmt.executeUpdate()
                }
            }
          }
          catch{
            case e1: SQLException =>
              log.error("SQL Insertion Error,SQLException: " + e1.getMessage +
                "|SQLState: " + e1.getSQLState +
                "|VendorError: " + e1.getErrorCode)
              -1
            case _ => log.error("Unknown SQL error")
              -1
          }

        }

        result.onComplete{
          case Success(code) =>
            if(code == -1){
              log.error("SQL execution error, the file information is not stored")

            }
          case Failure(f) =>
            log.error("Unknowned Error During executing SQL")
        }



      }
      else
      {
        conn.close()
      }










  }

}
