package uk.gov.justice.digital.hmpps.pollpush.config

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.testcontainers.containers.localstack.LocalStackContainer


@Configuration
@ConditionalOnProperty(name = ["sqs.provider"], havingValue = "embedded-localstack")
@Profile("test-queue")
open class JmsLocalStackConfig(private val localStackContainer: LocalStackContainer) {

  @Bean
  open fun awsSqsClient(): AmazonSQS {
    return AmazonSQSClientBuilder.standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
            .withCredentials(localStackContainer.defaultCredentialsProvider)
            .build()
  }

  @Bean
  open fun awsSqsDlqClient(): AmazonSQS {
    return AmazonSQSClientBuilder.standard()
            .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
            .withCredentials(localStackContainer.defaultCredentialsProvider)
            .build()
  }
}
