package od.auxiliary

import com.typesafe.config.{Config, ConfigFactory}

object ConfigLoad {
  val conf:Config = ConfigFactory.load()

  //build up jdbc address
  lazy val db_addr:String = "jdbc:mysql://" +
    conf.getString("db.address") + ":" +
    conf.getString("db.port") + "/od"
  lazy val db_user:String = conf.getString("db.username")
  lazy val db_pwd:String = conf.getString("db.password")
}
