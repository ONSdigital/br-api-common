package uk.gov.ons.br.repository.solr


import javax.inject.Inject
import org.apache.solr.common.SolrDocument
import org.slf4j.Logger
import uk.gov.ons.br.repository.{SearchRepository, SearchResult}
import uk.gov.ons.br.{ResultRepositoryError, ServerRepositoryError}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SolrSearchRepository[U] @Inject() (solrClient: SolrClient,
                                         solrDocumentMapper: SolrDocumentMapper[U])
                                        (implicit ec: ExecutionContext, logger: Logger) extends SearchRepository[U] {
  override def searchFor(term: String): Future[SearchResult[U]] = {
    if (logger.isDebugEnabled()) logger.debug("Searching for term [{}]", term)
    solrClient.searchFor(term).map { queryResponse =>
      if (logger.isDebugEnabled()) logger.debug("Found [{}] documents matching term [{}]", queryResponse.getResults.getNumFound, term)
      val documents = queryResponse.getResults.asScala.toVector
      val units = documents.flatMap(fromDocument)
      if (logger.isDebugEnabled()) logger.debug("[{}] documents successfully converted into [{}] results", documents.size, units.size)
      if (documents.nonEmpty && units.isEmpty) Left(ResultRepositoryError(s"Unable to construct a unit from ANY of the matching documents"))
      else Right(units)
    }.recover {
      case NonFatal(t) => Left(ServerRepositoryError(s"${t.getClass.getSimpleName} - ${t.getMessage}"))
    }
  }

  private def fromDocument(document: SolrDocument): Option[U] = {
    val optUnit = solrDocumentMapper.fromDocument(document)
    optUnit.fold(logger.warn(s"Unable to construct a unit from the document {}", document)) { unit =>
      if (logger.isDebugEnabled) logger.debug("Constructed unit [{}] from the document [{}].", unit, document)
    }
    optUnit
  }
}
