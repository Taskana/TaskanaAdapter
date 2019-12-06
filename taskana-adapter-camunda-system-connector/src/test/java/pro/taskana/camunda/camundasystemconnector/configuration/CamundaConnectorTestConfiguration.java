package pro.taskana.camunda.camundasystemconnector.configuration;

import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskCompleter;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskRetriever;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaVariableRetriever;

/**
 * Configuration for test of Camunda System Connector.
 *
 * @author bbr
 */
@Configuration
public class CamundaConnectorTestConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    CamundaTaskRetriever camundaTaskRetriever() {
        return new CamundaTaskRetriever();
    }

    @Bean
    CamundaTaskCompleter camundaTaskCompleter() {
        return new CamundaTaskCompleter();
    }

    @Bean
    CamundaVariableRetriever camundaVariableRetriever() {
        return new CamundaVariableRetriever();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder(new MockServerRestTemplateCustomizer());
    }

}
