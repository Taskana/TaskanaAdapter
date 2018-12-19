package pro.taskana.camunda.camundasystemconnector.config;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import pro.taskana.camunda.camundasystemconnector.spi.impl.CamundaSystemConnectorProviderImpl;

@Configuration
public class CamundaSystemConnectorConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(CamundaSystemConnectorConfiguration.class);
            
    @Bean
    CamundaSystemUrls camundaSystemUrls(@Value("${taskana-camunda-camundasystemconnector.camundaSystemURLs}") final String strCamundaSystemurls) {
    	LOGGER.error("CamundaSystemConnectorConfiguration.camundaSystemUrls called. strCamundaSystemurls = {}, exc = {}", 
    			strCamundaSystemurls, new Exception("debug") );
    	return new CamundaSystemUrls(strCamundaSystemurls);
    }


    public CamundaSystemConnectorConfiguration() {
//    	LOGGER.error("CamundaSystemConnectorConfiguration ctor. strCamundaSystemurls = {}, exc = {}", strCamundaSystemurls,
//    			new Exception("debug"));
    }
    
}
