package uk.gov.ons.br.utils

case class Port(value: Int) {
  require(value >= Port.Min && value <= Port.Max, s"Port [$value] is outside the valid range [${Port.Min}-${Port.Max}]")
}

object Port {
  private val Min = 0
  private val Max = 65535
}

case class BaseUrl(protocol: String, host: String, port: Port, prefix: Option[String] = None)

object BaseUrl {
  def asUrlString(baseUrl: BaseUrl): String =
    s"${baseUrl.protocol}://${baseUrl.host}:${baseUrl.port.value}${baseUrl.prefix.fold("")(p => "/" + p)}"
}