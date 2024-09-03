package io.kadai.adapter.systemconnector.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

class SystemResponseTest {

  @Test
  void should_MapIntValueToCorrectHttpStatus_When_RelatedSystemResponseConstructorIsCalled() {
    assertThat(new SystemResponse(200, null).getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void should_MapHttpStatusCodeToCorrectHttpStatus_When_RelatedSystemResponseConstructorIsCalled() {
    assertThat(new SystemResponse(HttpStatusCode.valueOf(200), null).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }
}
