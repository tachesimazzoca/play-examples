package models

import javax.inject.Singleton

import scala.collection.mutable

@Singleton
class ArticleIndex {
  val keywordToIdMap: mutable.Map[String, mutable.Set[String]] = mutable.Map()
  val idToKeywordMap: mutable.Map[String, mutable.Set[String]] = mutable.Map()

  val normalizeKeyword: String => String =
    _.trim.toLowerCase.replaceAll("[^a-z]", "")

  def save(article: Article) = {
    Thread.sleep(5000L) // simulate waiting for another transaction
    val keywords = mutable.Set(article.content.split(" ").map(normalizeKeyword).filterNot(_.isEmpty): _*)
    keywords.foreach { keyword =>
      if (!keywordToIdMap.contains(keyword))
        keywordToIdMap(keyword) = mutable.Set()
      keywordToIdMap(keyword) += article.id
    }
    idToKeywordMap(article.id) = keywords
  }

  def delete(id: String) = {
    Thread.sleep(5000L) // simulate waiting for another transaction
    val removedKeywords = idToKeywordMap.getOrElse(id, Set.empty)
    idToKeywordMap -= id
    removedKeywords.foreach {
      keywordToIdMap(_) -= id
    }
  }

  def search(keyword: String): Set[String] =
    keywordToIdMap.getOrElse(normalizeKeyword(keyword), Set.empty).toSet
}
