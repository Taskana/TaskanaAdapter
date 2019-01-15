package pro.taskana.camunda.camundasystemconnector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.client.RestTemplate;

import pro.taskana.camunda.camundasystemconnector.api.impl.CamundaTaskCompleter;
import pro.taskana.camunda.camundasystemconnector.api.impl.CamundaTaskRetriever;
import pro.taskana.camunda.camundasystemconnector.api.impl.CamundaVariableRetriever;

@Configuration
@DependsOn(value= {"springContextProvider"})
public class CamundaSystemConnectorConfiguration {

    @Bean
    CamundaSystemUrls camundaSystemUrls(@Value("${taskana-camunda-camundasystemconnector.camundaSystemURLs}") final String strUrls) {
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
