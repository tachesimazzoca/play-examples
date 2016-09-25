package controllers

import javax.inject.{Inject, Singleton}

import actors._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import models._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class ArticleController @Inject() (
  actorSystem: ActorSystem,
  articleRepository: ArticleRepository,
  articleIndex: ArticleIndex
)(implicit ec: ExecutionContext) extends Controller {

  val articleIndexer = actorSystem.actorOf(
    ArticleIndexer.props(articleIndex), "article-indexer")
  val articleSearcher = actorSystem.actorOf(
    ArticleSearcher.props(articleIndex), "article-searcher")

  def index = Action {
    Ok(views.html.article.index(articleRepository.findAll))
  }

  def entry = Action(parse.urlFormEncoded) { implicit request =>
    val article = Article(
      java.util.UUID.randomUUID().toString,
      request.body.getOrElse("content", Seq("")).head,
      System.currentTimeMillis
    )
    articleRepository.save(article)
    articleIndexer ! ArticleIndexer.Entry(article)
    Redirect(routes.ArticleController.index())
  }

  def delete(id: String) = Action {
    articleRepository.delete(id)
    articleIndexer ! ArticleIndexer.Delete(id)
    Redirect(routes.ArticleController.index())
  }

  def search = Action.async { implicit request =>
    request.getQueryString("q").map { q =>
      implicit val timeout: Timeout = 5.seconds
      (articleSearcher ? ArticleSearcher.Query(q))
        .mapTo[ArticleSearcher.Result]
        .map { result =>
          val entries = (for {
            id <- result.idSet
            entry <- articleRepository.find(id)
          } yield entry).toSeq
          Ok(views.html.article.search(q, entries))
        }
    }.getOrElse {
      Future {
        Redirect(routes.ArticleController.index())
      }
    }
  }
}
