package pro.taskana.adapter.systemconnector.camunda.config;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.springframework.stereotype.Component;

@Component
public class CamundaSystemUrls {

	Set<String> camundaSystemURLs = new HashSet<>(); 
	
    public CamundaSystemUrls(String strUrls) {                   
        if (strUrls != null) {
            StringTokenizer st = new StringTokenizer(strUrls, ",");
            while (st.hasMoreTokens()) {
                camundaSystemURLs.add(st.nextToken().trim());
            }
        }
    }
    
    public Set<String> getUrls() {
        return camundaSystemURLs;        
    }

	@Override
	public String toString() {
		return "CamundaSystemUrls [camundaSystemURLs=" + camundaSystemURLs + "]";
	}
}
