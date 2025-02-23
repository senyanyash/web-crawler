package api

import config.Configs.CrawlerConfig
import model.Errors.BadRequestException
import parser.HtmlTitleParser
import zio.http.Method.POST
import zio.{Chunk, RIO, ZIO}
import zio.http.Status.{BadRequest, InternalServerError, Ok}
import zio.http.codec.{Doc, PathCodec}
import zio.http.endpoint.Endpoint
import zio.http.endpoint.openapi.{OpenAPIGen, SwaggerUI}
import zio.http.{Body, Charsets, Client, Middleware, Request, Response, Routes, UrlInterpolatorHelper, handler}
import zio.json.DecoderOps
import zio.stream.ZStream
import model.CrawlerProtocol._
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

object CrawlerApi {
  val crawlerEndpoint = Endpoint((POST / "crawler") ?? Doc.p("Get titles of websites"))
    .in[List[String]].examplesIn("Valid rq body" -> List("https://www.youtube.com/", "https://2gis.ru/spb"))
    .out[Chunk[CrawlerResponse]].examplesOut("Success output" -> Chunk(CrawlerResponse("https://www.youtube.com/", Some("YouTube"), None), CrawlerResponse("https://2gis.ru/spb", Some("Карта Санкт-Петербурга: улицы, дома и организации города — 2ГИС"), None)))
    .outError[ErrorResponse](BadRequest)
    .outError[ErrorResponse](InternalServerError)

  val swaggerRoute = SwaggerUI.routes(PathCodec.empty / "swagger-ui", OpenAPIGen.fromEndpoints(title = "Web Crawler API", version = "1.0", crawlerEndpoint))

  val crawlerRoutes = Routes(
    POST / "crawler" -> handler(getWebsiteTitleByUrl(_: Request))
  ).handleError {
    case e: BadRequestException => Response(status = BadRequest, body = Body.from(ErrorResponse(e.getMessage)))
    case e => Response(status = InternalServerError, body = Body.from(ErrorResponse(e.getMessage)))
  }

  val routes = (crawlerRoutes ++ swaggerRoute) @@ Middleware.requestLogging()

  private def getWebsiteTitleByUrl(rq: Request): RIO[Client with CrawlerConfig, Response] =
    for {
      urls <- rq.body.asString(Charsets.Utf8)
        .flatMap(b => ZIO.fromEither(b.fromJson[List[String]])).map(list => ZIO.attempt(list.map(s => url"$s")))
        .mapError(_ => BadRequestException(s"Неверный формат входных данных. Ожидается массив строк url в body"))
      cfg <- ZIO.service[CrawlerConfig]
      titlesByUrl <- ZStream.from(urls).mapZIOPar(cfg.concurrentRequests)(HtmlTitleParser.parseTitleForUrl).runCollect
    } yield Response(status = Ok, body = Body.from(titlesByUrl))


}
