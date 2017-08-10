package od.web
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import od.auxiliary.ConfigLoad
import od.dao.{DBConnectionPool, FileInfoInserter, FileInfoRetriever}
import FileInfoInserter.FileInfo
import akka.pattern.ask
import scala.io.StdIn
import scala.concurrent.duration._

object WebServer {
  def main(args: Array[String]): Unit ={
    implicit val system = ActorSystem("od")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout:akka.util.Timeout = 5.seconds
    //run http service
    val route = WebRoute.route
    val port = ConfigLoad.conf.getInt("web.port")
    val bindAddr = ConfigLoad.conf.getString("web.address")
    val bindingFuture = Http().bindAndHandle(route, bindAddr, port)

    val db_pool_ref = system.actorOf(DBConnectionPool.props("com.mysql.jdbc.Driver",
      "jdbc:mysql://localhost/od",
      "root",
      "13574206950"
    ),"ConnPoolActor")

    val inserter_ref = system.actorOf(Props[FileInfoInserter],"inserter")
    val retriever_ref = system.actorOf(Props[FileInfoRetriever],"retriver")

    /*
    * Code for testing
    *

    TimeUnit.SECONDS.sleep(5)
    val f_info = FileInfo(
      hash = "1111111111111111111111111111111111111111",
      size = 30,
      name = "fucker.zip")

    inserter_ref ! f_info
    ask(retriever_ref, FileInfoRetriever.CheckIfHashExist("1111111111111111111111111111111111111111")).foreach(println)

    */

    println(s"server successfully running on $bindAddr:$port")
    StdIn.readLine() //block the thread until enter to quit
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())  //stop the server
  }
}
