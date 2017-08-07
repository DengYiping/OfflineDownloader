package od.dao

import akka.actor.Actor
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import akka.event.Logging
import od.auxiliary.ConfigLoad

case class FileInfo(hash:String, size:Int)

class FileInfoInserter extends Actor {
  val log = Logging(context.system,this)
  def receive = {
    case f_info:FileInfo =>
      try{
        //to protect the connection
        val conn = DriverManager.getConnection(ConfigLoad.db_addr,ConfigLoad.db_user,ConfigLoad.db_pwd)


        try{
          //to protect SQL insertion

          //prepare statement
          val stmt = conn.prepareStatement("INSERT INTO od (hash, size) VALUES (?, ?)")

          //bind value
          stmt.setString(1,f_info.hash)
          stmt.setInt(2,f_info.size)

          //insert
          stmt.executeUpdate()

        }catch{
          case e1: SQLException =>
            log.error("SQL Insertion Error,SQLException: " + e1.getMessage +
              "|SQLState: " + e1.getSQLState +
              "|VendorError: " + e1.getErrorCode)
          case _ => log.error("Unknown SQL error")
        }finally {
          //close the connection
          conn.close()
        }

      }catch {
        case e1: SQLException =>
          log.error("SQL Connection error, SQLException: " + e1.getMessage +
            "|SQLState: " + e1.getSQLState +
            "|VendorError: " + e1.getErrorCode)
        case _ => log.error("Unknown SQL error")
      }

  }
}
