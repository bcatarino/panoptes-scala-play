package objects

import play.api.mvc.{Headers, RequestHeader}

class TestRequestHeader(m: String, p: String) extends RequestHeader {
  override def id: Long = 0

  override def secure: Boolean = true

  override def uri: String = ""

  override def remoteAddress: String = ""

  override def queryString: Map[String, Seq[String]] = Map[String, Seq[String]]()

  override def method: String = m

  override def headers: Headers = new Headers(Seq())

  override def path: String = p

  override def version: String = ""

  override def tags: Map[String, String] = Map[String, String]()
}
