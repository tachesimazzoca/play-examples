package actors

import akka.actor.{Actor, ActorLogging, Props}
import models.{Article, ArticleIndex}

object ArticleIndexer {

  case class Entry(article: Article)

  case class Delete(id: String)

  def props(articleIndex: ArticleIndex): Props = Props(classOf[ArticleIndexer], articleIndex)
}

class ArticleIndexer(articleIndex: ArticleIndex) extends Actor with ActorLogging {

  import ArticleIndexer._

  override def receive: Receive = {
    case Entry(article) => {
      articleIndex.save(article)
      log.info("Updated the keywords of " + article.id)
    }

    case Delete(id) => {
      articleIndex.delete(id)
      log.info("Deleted the keywords of " + id)
    }
  }
}
