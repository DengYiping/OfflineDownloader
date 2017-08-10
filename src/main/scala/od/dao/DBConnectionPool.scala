package od.dao

import java.sql.Connection

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import com.jolbox.bonecp.{BoneCP, BoneCPConfig}


object DBConnectionPool{
  sealed trait DBcommand
  case object ClosePool extends DBcommand

  /*
  * ActionId is a field specified by user to distinguish different action request. After retrieving the connection,
  * the actor will send back the actionId along with the connection.
  * User can use a random number for actionId
  * */
  case class RetrieveOneConnection(actionId:Int) extends DBcommand
  case class RetrievedConnection(actionId:Int, conn:Connection)


  def props(jdbcDriver:String,
            url:String,
            username:String,
            password:String) = Props(new DBConnectionPool(jdbcDriver,url,username,password))
}

class DBConnectionPool(jdbcDriver:String, url:String, username:String, password:String) extends Actor with ActorLogging {
  import od.dao.DBConnectionPool.{ClosePool, RetrieveOneConnection, RetrievedConnection}
  private var connPool:BoneCP = null

  //check if connection pool is successfully setup
  private def isReady:Boolean = {
    if(connPool == null)
      false
    else
      true
  }


  def receive = {
    case RetrieveOneConnection(id) =>
      if(isReady)
        sender() ! RetrievedConnection(id, connPool.getConnection)
      else{
        log.error("Fatal error, should have the connection pool ready, system shutdown")
        context.system.terminate()
      }

    case ClosePool =>
      //simply close the actor, and postStop will do the clean up job
      self ! PoisonPill
  }



  //initialization
  override def preStart(): Unit = {

    super.preStart()

    //loading the driver
    try{
      Class.forName(jdbcDriver).newInstance()
    }
    catch{
      case e: Exception =>
        log.error("Error in loading the driver, system will shutdown, please quit")
        context.system.terminate()
    }



    //build up connection pool
    try{
      val config = new BoneCPConfig()
      config.setJdbcUrl(url)
      config.setUsername(username)
      config.setPassword(password)

      //Partition settings
      config.setMaxConnectionsPerPartition(5)
      config.setMinConnectionsPerPartition(2)
      config.setPartitionCount(3)

      //spin up the connection pool
      connPool = new BoneCP(config)
      log.info("Connection pool successfully created.")
    } catch {
      case ex:Exception =>
        log.error("Error in building a connection pool, system will shutdown, please quit")
        connPool = null
        context.system.terminate()
    }


  }


  //clean up
  override def postStop(): Unit = {

    super.postStop()

    if(isReady){
      connPool.shutdown()
      log.info("Connection Pool is closed")
      connPool = null
    }
  }


}
