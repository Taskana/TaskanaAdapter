package pro.taskana.adapter.systemconnector.camunda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.client.RestTemplate;

import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskCompleter;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskRetriever;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaVariableRetriever;

@Configuration
@DependsOn(value= {"springContextProvider"})
public class CamundaSystemConnectorConfiguration {

    @Bean
    CamundaSystemUrls camundaSystemUrls(@Value("${taskana-system-connector-camundaSystemURLs}") final String strUrls) {
        return new CamundaSystemUrls(strUrls);
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

}
