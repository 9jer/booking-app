package com.example.propertyservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.openfeign.client.config.user-service.url=http://localhost:${wiremock.server.port}",
                "spring.cloud.openfeign.client.config.booking-service.url=http://localhost:${wiremock.server.port}",
                "spring.kafka.properties.schema.registry.url=mock://test-registry",
                "spring.kafka.producer.properties.schema.registry.url=mock://test-registry",
                "spring.kafka.consumer.properties.schema.registry.url=mock://test-registry",
                "spring.kafka.consumer.auto-offset-reset=earliest"
        }
)
@AutoConfigureWireMock(port = 0)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

}