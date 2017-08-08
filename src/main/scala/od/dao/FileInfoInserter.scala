package od.dao

import akka.actor.{Actor, ActorLogging}
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

import akka.event.Logging
import od.auxiliary.ConfigLoad
import od.dao.DBConnectionPool.{RetrieveOneConnection, RetrievedConnection}

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}

object FileInfoInserter{
  case class FileInfo(hash:String, size:Int, name: String)
}

class FileInfoInserter extends DatabaseAccessActor with ActorLogging{
  import FileInfoInserter.FileInfo
  implicit val executionContext = context.system.dispatcher

  //defined in DatabaseAccessActor
  val connPoolActorRef = context.actorSelection("/ConnPoolActor")

  val queue = new mutable.Queue[FileInfo]()
  def receive = {


    case f_info:FileInfo =>
      queue.enqueue(f_info)
      askForConn(1)


    case RetrievedConnection(_, conn) =>
      //pop out the info that need to be stored
      val fileInfo = queue.dequeue()






      //wrap it into future so that it don't block
      val result:Future[Int] = Future{




        try{
          //to protect SQL insertion

          //prepare statement
          val stmt = conn.prepareStatement("INSERT INTO od (hash, size, name) VALUES (?, ?, ?)")

          //bind value
          stmt.setString(1,fileInfo.hash)
          stmt.setInt(2,fileInfo.size)
          stmt.setString(3, fileInfo.name)


          //insert
          stmt.executeUpdate()

        }catch{
          case e1: SQLException =>
            log.error("SQL Insertion Error,SQLException: " + e1.getMessage +
              "|SQLState: " + e1.getSQLState +
              "|VendorError: " + e1.getErrorCode)
            -1
          case _ => log.error("Unknown SQL error")
            -1
        }finally {
          //close the connection
          conn.close()
        }



      }








      result.onComplete{
        case Success(code) =>
          if(code == 0){
            log.error("SQL execution error, the file information is not stored")

          }
        case Failure(f) =>
          log.error("Unknowned Error During executing SQL")
      }
  }

}
