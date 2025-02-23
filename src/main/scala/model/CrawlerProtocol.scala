package model

import zio.schema.{DeriveSchema, Schema}

object CrawlerProtocol {

  case class CrawlerResponse(
                              url: String,
                              success: Option[String],
                              error: Option[String]
                            )

  case class ErrorResponse(message: String)

  implicit val errorRsSchema: Schema[ErrorResponse] = DeriveSchema.gen
  implicit val crawlerResponseSchema: Schema[CrawlerResponse] = DeriveSchema.gen
}
