package model

object Errors {
  case class BadRequestException(msg: String) extends RuntimeException(msg)
}
