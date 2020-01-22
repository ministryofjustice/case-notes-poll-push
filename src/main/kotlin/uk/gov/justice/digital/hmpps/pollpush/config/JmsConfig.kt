package uk.gov.justice.digital.hmpps.pollpush.config

import com.amazon.sqs.javamessaging.ProviderConfiguration
import com.amazon.sqs.javamessaging.SQSConnectionFactory
import com.amazonaws.auth.*
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.support.destination.DynamicDestinationResolver
import javax.jms.Session

@Configuration
@EnableJms
open class JmsConfig {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  open fun jmsListenerContainerFactory(awsSqsClient: AmazonSQS): DefaultJmsListenerContainerFactory {
    val factory = DefaultJmsListenerContainerFactory()
    factory.setConnectionFactory(SQSConnectionFactory(ProviderConfiguration(), awsSqsClient))
    factory.setDestinationResolver(DynamicDestinationResolver())
    factory.setConcurrency("1")
    factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE)
    factory.setErrorHandler { t: Throwable? -> log.error("Error caught in jms listener", t) }
    return factory
  }

  @Bean
  @ConditionalOnProperty(name = ["sqs.provider"], havingValue = "aws")
  open fun awsSqsClient(@Value("\${sqs.aws.access.key.id}") accessKey: String,
                        @Value("\${sqs.aws.secret.access.key}") secretKey: String,
                        @Value("\${sqs.endpoint.region}") region: String): AmazonSQS =
      AmazonSQSClientBuilder.standard()
          .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKey, secretKey)))
          .withRegion(region)
          .build()

  @Bean
  @ConditionalOnProperty(name = ["sqs.provider"], havingValue = "aws")
  open fun awsSqsDlqClient(@Value("\${sqs.aws.dlq.access.key.id}") accessKey: String,
                           @Value("\${sqs.aws.dlq.secret.access.key}") secretKey: String,
                           @Value("\${sqs.endpoint.region}") region: String): AmazonSQS =
      AmazonSQSClientBuilder.standard()
          .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKey, secretKey)))
          .withRegion(region)
          .build()

  @Bean("awsSqsClient")
  @ConditionalOnExpression("'\${sqs.provider}'.equals('localstack') and '\${sqs.embedded}'.equals('false')")
  open fun awsSqsClientLocalstack(@Value("\${sqs.endpoint.url}") serviceEndpoint: String,
                                  @Value("\${sqs.endpoint.region}") region: String): AmazonSQS =
      AmazonSQSClientBuilder.standard()
          .withEndpointConfiguration(EndpointConfiguration(serviceEndpoint, region))
          .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
          .build()

  @Bean("awsSqsDlqClient")
  @ConditionalOnExpression("'\${sqs.provider}'.equals('localstack') and '\${sqs.embedded}'.equals('false')")
  open fun awsSqsDlqClientLocalstack(@Value("\${sqs.endpoint.url}") serviceEndpoint: String,
                                     @Value("\${sqs.endpoint.region}") region: String): AmazonSQS =
      AmazonSQSClientBuilder.standard()
          .withEndpointConfiguration(EndpointConfiguration(serviceEndpoint, region))
          .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
          .build()

  // TODO remove this bean and class SqsConfig - using to debug properties in build
  @Bean
  @ConfigurationProperties(prefix = "sqs")
  open fun sqsConfig(): SqsConfig {
    return SqsConfig()
  }
}

open class SqsConfig {
  var embedded: String = "none"
  var provider: String = "none"

  override fun toString(): String {
    return "embedded=${embedded}, provider=${provider}"
  }
}
