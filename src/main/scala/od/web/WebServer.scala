package od.web
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import od.auxiliary.ConfigLoad


import scala.io.StdIn
import scala.concurrent.Future

object WebServer {
  def main(args: Array[String]): Unit ={
    implicit val system = ActorSystem("od")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    //run http service
    val route = WebRoute.route
    val port = ConfigLoad.conf.getInt("web.port")
    val bindAddr = ConfigLoad.conf.getString("web.address")
    val bindingFuture = Http().bindAndHandle(route, bindAddr, port)

    println(s"server successfully running on $bindAddr:$port")
    StdIn.readLine() //block the thread until enter to quit
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())  //stop the server
  }
}
