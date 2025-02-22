package model

import zio.schema.DeriveSchema

object CrawlerProtocol {
  case class CrawlerResponse(
                              url: String,
                              success: Option[String],
                              error: Option[String]
                            )

  abstract class ErrorResponse(message: String)

  case class BadRequestResponse(message: String) extends ErrorResponse(message)

  case class InternalServerErrorResponse(message: String) extends ErrorResponse(message)

  implicit val badRequestRsSchema = DeriveSchema.gen[BadRequestResponse]
  implicit val internalServerErrorRsSchema = DeriveSchema.gen[InternalServerErrorResponse]
  implicit val crawlerResponseSchema = DeriveSchema.gen[CrawlerResponse]
}
