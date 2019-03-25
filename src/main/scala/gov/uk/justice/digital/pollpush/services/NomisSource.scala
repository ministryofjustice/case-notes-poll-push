package gov.uk.justice.digital.pollpush.services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import com.google.inject.name.Named
import gov.uk.justice.digital.pollpush.data.{PullResult, SourceCaseNote}
import gov.uk.justice.digital.pollpush.traits.{BulkSource, SourceToken}
import grizzled.slf4j.Logging
import org.json4s.Formats
import org.json4s.native.Serialization._
import scala.concurrent.ExecutionContext.Implicits.global

class NomisSource @Inject() (@Named("sourceUrl") sourceUrl: String, @Named("noteTypes") noteTypes: Seq[String], sourceToken: SourceToken, @Named("slack") slack: Int)
                            (implicit val formats: Formats,
                             implicit val system: ActorSystem,
                             implicit val materializer: ActorMaterializer) extends BulkSource with Logging {

  private val http = Http()

  private val filter = noteTypes.filterNot(_.isEmpty).map(s => s"&note_type=$s").mkString("")

  private implicit val unmarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(MediaTypes.`application/json`).map { json =>

    logger.debug(s"Received from Nomis: $json")

    read[Seq[SourceCaseNote]](json)
  }

  override def pull(from: DateTime, until: DateTime) = {

    val uri = s"$sourceUrl?from_datetime=${from.minus(slack * 1000).toIsoDateTimeString}.000Z$filter"

    logger.info(s"Requesting from Nomis: $uri (with $slack slack seconds)")

    http.singleRequest(
      HttpRequest(
        HttpMethods.GET,
        Uri(uri),
        List(Authorization(OAuth2BearerToken(sourceToken.generate())))))
      .flatMap {

        case response @ HttpResponse(statusCode, _, _, _) if statusCode.isFailure =>

          response.discardEntityBytes()
          throw new Exception(statusCode.value)

        case HttpResponse(_, _, entity, _) =>

          Unmarshal(entity.withoutSizeLimit()).to[Seq[SourceCaseNote]].map(PullResult(_, Some(from), Some(until), None))

      }.recover { case error: Throwable => PullResult(Seq(), Some(from), Some(until), Some(error)) }
  }
}
