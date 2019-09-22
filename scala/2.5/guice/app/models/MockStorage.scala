package models

class MockStorage extends Storage {
  var data: Map[String, String] = Map.empty

  def read(key: String): Option[String] = data.get(key)

  def write(key: String, value: String) {
    data = data.updated(key, value)
  }

  def delete(key: String) = {
    data = data - key
  }

  def count = data.size
}
