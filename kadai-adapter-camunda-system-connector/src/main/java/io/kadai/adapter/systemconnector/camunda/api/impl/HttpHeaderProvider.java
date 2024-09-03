package io.kadai.adapter.systemconnector.camunda.api.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class HttpHeaderProvider {

  private static String undefined = "undefined";

  @Value("${kadai-system-connector-camunda-rest-api-user-name:undefined}")
  private String camundaRestApiUserName;

  @Value("${kadai-system-connector-camunda-rest-api-user-password:undefined}")
  private String camundaRestApiUserPassword;

  @Value("${kadai-system-connector-outbox-rest-api-user-name:undefined}")
  private String outboxRestApiUserName;

  @Value("${kadai-system-connector-outbox-rest-api-user-password:undefined}")
  private String outboxRestApiUserPassword;

  public HttpHeaders camundaRestApiHeaders() {
    if (undefined.equals(camundaRestApiUserName)) {
      return new HttpHeaders();
    } else {
      String plainCreds = camundaRestApiUserName + ":" + camundaRestApiUserPassword;
      return encodeHttpHeaders(plainCreds);
    }
  }

  public HttpHeaders outboxRestApiHeaders() {
    if (undefined.equals(outboxRestApiUserName)) {
      return new HttpHeaders();
    } else {
      String plainCreds = outboxRestApiUserName + ":" + outboxRestApiUserPassword;
      return encodeHttpHeaders(plainCreds);
    }
  }

  public HttpEntity<Void> prepareNewEntityForCamundaRestApi() {
    HttpHeaders headers = getHttpHeadersForCamundaRestApi();
    return new HttpEntity<>(headers);
  }

  public HttpEntity<String> prepareNewEntityForCamundaRestApi(String requestBody) {
    HttpHeaders headers = getHttpHeadersForCamundaRestApi();
    return new HttpEntity<>(requestBody, headers);
  }

  public HttpEntity<Void> prepareNewEntityForOutboxRestApi() {
    HttpHeaders headers = getHttpHeadersForOutboxRestApi();
    return new HttpEntity<>(headers);
  }

  public HttpEntity<String> prepareNewEntityForOutboxRestApi(String requestBody) {
    HttpHeaders headers = getHttpHeadersForOutboxRestApi();
    return new HttpEntity<>(requestBody, headers);
  }

  HttpHeaders getHttpHeadersForCamundaRestApi() {
    HttpHeaders headers = camundaRestApiHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  HttpHeaders getHttpHeadersForOutboxRestApi() {
    HttpHeaders headers = outboxRestApiHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private HttpHeaders encodeHttpHeaders(String credentials) {
    byte[] credentialsBytes = credentials.getBytes(StandardCharsets.US_ASCII);
    String encodedCredentials = Base64.getEncoder().encodeToString(credentialsBytes);
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Basic " + encodedCredentials);
    return headers;
  }
}
