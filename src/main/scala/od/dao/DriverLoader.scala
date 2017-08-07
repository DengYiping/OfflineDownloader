package od.dao

object DriverLoader {
  //Load JDBC driver
  def load(): Unit ={
    try{
      Class.forName("com.mysql.jdbc.Driver").newInstance()
    }
    catch{
      case e: Exception => println("Error in loading the driver")
    }
  }
}
