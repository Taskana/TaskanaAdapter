package pro.taskana.adapter.systemconnector.camunda.config;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.springframework.stereotype.Component;

import pro.taskana.common.api.LoggerUtils;

/** Holds the URlS (Camunda REST Api and outbox REST) of the configured camunda systems. @ */
@Component
public class CamundaSystemUrls {

  Set<SystemUrlInfo> theCamundaSystemUrls = new HashSet<>();

  public CamundaSystemUrls(String strUrls) {
    if (strUrls != null) {
      StringTokenizer systemTokenizer = new StringTokenizer(strUrls, ",");
      while (systemTokenizer.hasMoreTokens()) {
        String currentUrlPair = systemTokenizer.nextToken().trim();
        SystemUrlInfo urlInfo = new SystemUrlInfo();
        urlInfo.setSystemRestUrl(currentUrlPair.substring(0, currentUrlPair.indexOf('|')).trim());
        urlInfo.setSystemTaskEventUrl(
            currentUrlPair
                .substring(currentUrlPair.indexOf('|') + 1, currentUrlPair.length())
                .trim());

        theCamundaSystemUrls.add(urlInfo);
      }
    }
  }

  public Set<SystemUrlInfo> getUrls() {
    return theCamundaSystemUrls;
  }

  @Override
  public String toString() {
    return "CamundaSystemUrls [camundaSystemUrls="
        + LoggerUtils.setToString(theCamundaSystemUrls)
        + "]";
  }

  /** Holds the URS (Camunda REST Api and outbox REST) of a specific camunda system. */
  public static class SystemUrlInfo {

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
      return "SystemUrlInfo [systemRestUrl="
          + systemRestUrl
          + ", systemTaskEventUrl="
          + systemTaskEventUrl
          + "]";
    }
  }
}
