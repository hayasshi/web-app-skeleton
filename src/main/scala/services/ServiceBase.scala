package services

import akka.stream.{ ActorAttributes, Attributes }
import akka.stream.scaladsl.Flow

trait ServiceBase {

  protected def nonBlockingFlow[A, B, C](flow: Flow[A, B, C], signature: String): Flow[A, B, C] = {
    val tagName = s"${getClass.getSimpleName}#$signature"
    flow.withAttributes(Attributes.name(tagName)).log(tagName).async
  }

  protected def blockingFlow[A, B, C](flow: Flow[A, B, C], signature: String): Flow[A, B, C] = {
    val tagName = s"${getClass.getSimpleName}#$signature"
    val attribute = ActorAttributes.dispatcher("application.blocking-io-dispatcher")
    flow.withAttributes(Attributes.name(tagName) and attribute).log(tagName).async
  }

}
