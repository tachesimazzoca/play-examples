package components.storage

import java.net.{URLDecoder, URLEncoder}

class Storage(engine: StorageEngine) {

  private def serialize(data: Map[String, String]): Array[Byte] =
    data.map { case (k, v) =>
      URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
    }.mkString("&").getBytes

  private def unserialize(bytes: Array[Byte]): Map[String, String] = {
    new String(bytes, "UTF-8").split("&")
      .map(_.split("=", 2))
      .map(p => (URLDecoder.decode(p(0), "UTF-8"), URLDecoder.decode(p(1), "UTF-8")))
      .toMap
  }

  def create(data: Map[String, String]): String = {
    val key = java.util.UUID.randomUUID().toString
    engine.write(key, serialize(data))
    key
  }

  def read(key: String): Option[Map[String, String]] =
    engine.read(key).map { bytes =>
      unserialize(bytes)
    }

  def write(key: String, data: Map[String, String]):Boolean =
    engine.write(key, serialize(data))

  def delete(key: String): Boolean = engine.delete(key)
}
