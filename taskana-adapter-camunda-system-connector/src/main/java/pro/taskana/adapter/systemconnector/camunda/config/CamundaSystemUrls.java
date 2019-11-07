package pro.taskana.adapter.systemconnector.camunda.config;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.springframework.stereotype.Component;

import pro.taskana.impl.util.LoggerUtils;

@Component
public class CamundaSystemUrls {

    Set<SystemURLInfo> camundaSystemURLs = new HashSet<>();

    public CamundaSystemUrls(String strUrls) {
        if (strUrls != null) {
            StringTokenizer systemTokenizer = new StringTokenizer(strUrls, ",");
            while (systemTokenizer.hasMoreTokens()) {
                String currentURLPair = systemTokenizer.nextToken().trim();
                SystemURLInfo urlInfo = new SystemURLInfo();
                urlInfo.setSystemRestUrl(currentURLPair.substring(0, currentURLPair.indexOf('|')).trim());
                urlInfo.setSystemTaskEventUrl(
                    currentURLPair.substring(currentURLPair.indexOf('|') + 1, currentURLPair.length()).trim());

                camundaSystemURLs.add(urlInfo);
            }
        }
    }

    public Set<SystemURLInfo> getUrls() {
        return camundaSystemURLs;
    }

    @Override
    public String toString() {
        return "CamundaSystemUrls [camundaSystemURLs=" + LoggerUtils.setToString(camundaSystemURLs) + "]";
    }

    public static class SystemURLInfo {

        private String systemRestUrl;
        private String systemTaskEventUrl;

        public String getSystemRestUrl() {
            return systemRestUrl;
        }

        public void setSystemRestUrl(String systemRestUrl) {
            this.systemRestUrl = systemRestUrl;
        }

        public String getSystemTaskEventUrl() {
            return systemTaskEventUrl;
        }

        public void setSystemTaskEventUrl(String systemTaskEventUrl) {
            this.systemTaskEventUrl = systemTaskEventUrl;
        }

        @Override
        public String toString() {
            return "SystemURLInfo [systemRestUrl=" + systemRestUrl + ", systemTaskEventUrl=" + systemTaskEventUrl + "]";
        }

    }
}
