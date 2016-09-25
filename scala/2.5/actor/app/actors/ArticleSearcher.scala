package actors

import akka.actor.{Actor, Props}
import models._

object ArticleSearcher {

  case class Query(keyword: String)

  case class Result(idSet: Set[String])

  def props(articleIndex: ArticleIndex): Props = Props(classOf[ArticleSearcher], articleIndex)
}

class ArticleSearcher(articleIndex: ArticleIndex) extends Actor {

  import ArticleSearcher._

  override def receive: Receive = {
    case Query(keyword: String) =>
      sender ! Result(articleIndex.search(keyword))
  }
}
