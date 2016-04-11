package components.storage

trait StorageEngine {
  def read(key: String): Option[Array[Byte]]

  def write(key: String, data: Array[Byte]): Boolean

  def delete(key: String): Boolean
}
