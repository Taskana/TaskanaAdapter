package pro.taskana.camunda.camundasystemconnector.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskCompleter;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskRetriever;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaVariableRetriever;

import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;

@Configuration
public class CamundaConnectorTestConfiguration {

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
