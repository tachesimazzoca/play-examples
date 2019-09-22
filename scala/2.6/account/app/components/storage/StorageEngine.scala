package components.storage

trait StorageEngine {
  def read(key: String): Option[Array[Byte]]

  def write(key: String, data: Array[Byte]): Unit

  def delete(key: String): Unit

  def gc(lifetime: Long): Unit
}
