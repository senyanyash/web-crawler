package api

import config.Configs.CrawlerConfig
import model.CrawlerProtocol.{BadRequestResponse, CrawlerResponse, ErrorResponse, InternalServerErrorResponse}
import model.Errors.BadRequestException
import parser.HtmlTitleParser
import zio.http.Method.POST
import zio.{Chunk, ZIO}
import zio.http.Status.{BadRequest, InternalServerError}
import zio.http.codec.{Doc, HttpCodec, PathCodec}
import zio.http.endpoint.Endpoint
import zio.http.endpoint.openapi.{OpenAPIGen, SwaggerUI}
import zio.http.{Client, Middleware, UrlInterpolatorHelper}
import zio.stream.ZStream

object CrawlerApi {
  val endpoint = Endpoint((POST / "crawler") ?? Doc.p("Get titles of websites"))
    .in[List[String]].examplesIn("Valid rq body" -> List("https://www.youtube.com/", "https://2gis.ru/spb"))
    .out[Chunk[CrawlerResponse]].examplesOut("Success output" -> Chunk(CrawlerResponse("https://www.youtube.com/", Some("YouTube"), None), CrawlerResponse("https://2gis.ru/spb", Some("Карта Санкт-Петербурга: улицы, дома и организации города — 2ГИС"), None)))
    .outErrors[ErrorResponse](
      HttpCodec.error[BadRequestResponse](BadRequest),
      HttpCodec.error[InternalServerErrorResponse](InternalServerError),
    )

  val swaggerRoute = SwaggerUI.routes(PathCodec.empty / "swagger-ui", OpenAPIGen.fromEndpoints(title = "Web Crawler API", version = "1.0", endpoint))

  val routes = (endpoint.implement(getWebsiteTitleByUrl).toRoutes ++ swaggerRoute) @@ Middleware.requestLogging()

  private def getWebsiteTitleByUrl(input: List[String]): ZIO[Client with CrawlerConfig, ErrorResponse, Chunk[CrawlerResponse]] =
    (for {
      urls <- ZIO.attempt(input.distinct.map(e => url"$e"))
        .mapError(_ => BadRequestException(s"Неверный формат входных данных. Ожидается массив строк url в body"))
      cfg <- ZIO.service[CrawlerConfig]
      titlesByUrl <- ZStream.from(urls).mapZIOPar(cfg.concurrentRequests)(HtmlTitleParser.parseTitleForUrl).runCollect
    } yield titlesByUrl)
      .mapError {
        case e: BadRequestException => BadRequestResponse(e.getMessage)
        case e => InternalServerErrorResponse(e.getMessage)
      }


}
