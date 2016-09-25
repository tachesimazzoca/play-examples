package models

import javax.inject.Singleton

import scala.collection.mutable

@Singleton
class ArticleRepository {
  val rows: mutable.Map[String, Article] = new mutable.HashMap

  def find(id: String): Option[Article] = rows.get(id)

  def save(article: Article) = {
    rows(article.id) = article
  }

  def delete(id: String) = {
    rows -= id
  }

  def findAll: Seq[Article] =
    rows.values.toList.sorted(Ordering.by((_: Article).postedAt).reverse)
}
