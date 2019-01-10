package pro.taskana.camunda.camundasystemconnector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.client.RestTemplate;

import pro.taskana.camunda.camundasystemconnector.api.impl.CamundaTaskRetriever;

@Configuration
@DependsOn(value= {"springContextProvider"})
public class CamundaSystemConnectorConfiguration {

    @Bean
    CamundaSystemUrls camundaSystemUrls(@Value("${taskana-camunda-camundasystemconnector.camundaSystemURLs}") final String strCamundaSystemurls) {
        return new CamundaSystemUrls(strCamundaSystemurls);
    }

    @Bean
    CamundaTaskRetriever camundaTaskRetriever(RestTemplateBuilder builder) {
        return new CamundaTaskRetriever(builder);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

}
