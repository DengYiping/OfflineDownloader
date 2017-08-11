package od.dao

import akka.actor.ActorLogging
import od.auxiliary.{SHA1Hasher, TryWith, autoClose}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import akka.pattern.pipe
object UserManager{
  sealed trait UserManageRequest
  trait UserManageResponse

  case class AddUser(username:String, password:String, e_mail:String){
    /*
    * Check if the parameter passed in is acceptable
    * */
    def isValid:Boolean = {
      if(username.length > 20 || username.length < 5)
        false
      else if(password.length > 30 || password.length < 5)
        false
      else if(!e_mail.contains("@") || e_mail.length < 5 || e_mail.length > 20)
        false
      else
        true
    }
  }
  case class LoginRequest(username:String, password_hash:String){
    /*
    * Check if the parameter passed in is acceptable
    * */
    def isValid:Boolean = {
      if(username.length > 20 || username.length < 5)
        false
      else if(password_hash.length != 40)
        false
      else
        true
    }
  }

  case class AddUserResponse(isOK:Boolean) extends UserManageResponse
  case class LoginResponse(isOK:Boolean, id: Int = -1) extends UserManageResponse

}
class UserManager extends DatabaseAccessActor with ActorLogging{
  import UserManager.{AddUser,LoginRequest,AddUserResponse,LoginResponse}
  val connPoolActorRef = context.actorSelection("../ConnPoolActor")
  val timeout = 10.seconds
  implicit val executionContext = context.system.dispatcher

  def existUser(username:String):Future[Boolean] = {
    withPreparedStatement("SELECT id, password FROM user WHERE username = ?"){
      stmt =>
        stmt.setString(1, username)
        autoClose(stmt.executeQuery())(_.next())
    }
  }

  def receive = {



    case add_user: AddUser =>
      if (add_user.isValid) {
        existUser(add_user.username) flatMap {
          case true =>
            Future(AddUserResponse(false))
          case false =>
            withPreparedStatement("INSERT INTO user (username, password, e_mail) VALUES (?, ?)"){
              stmt =>
                stmt.setString(1, add_user.username)
                stmt.setString(2, SHA1Hasher(add_user.password))
                stmt.setString(3, add_user.e_mail)
                stmt.executeUpdate()
            }.map(x => AddUserResponse(x != -1))
        } recover { case e:Exception => AddUserResponse(false) } pipeTo sender()
      } else {
        sender() ! AddUserResponse(false)
      }



    case login_r:LoginRequest =>
      if(login_r.isValid){
        withPreparedStatement("SELECT id, password FROM user WHERE username = ?"){
          stmt =>
            stmt.setString(1, login_r.username)
            autoClose(stmt.executeQuery()){
              resultSet =>
                if (resultSet.next()) {
                  if (resultSet.getString(2) == login_r.password_hash)
                    LoginResponse(true, resultSet.getInt(1))
                  else
                    LoginResponse(false)
                } else {
                  LoginResponse(false)
                }
            }
        } recover {case e: Exception => LoginResponse(false)} pipeTo sender()
      }



    case _ =>
  }
}
