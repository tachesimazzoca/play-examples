package test

object Helpers {
  def inTest: Map[String, String]= {
    Map(
      "db.default.driver" -> "com.mysql.jdbc.Driver",
      "db.default.url" -> "jdbc:mysql://localhost/play_forum_test",
      "db.default.user" -> "play_forum",
      "db.default.pass" -> "play_forum"
    )
  }
}
