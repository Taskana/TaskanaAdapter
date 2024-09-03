package io.kadai.camunda.camundasystemconnector.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.camunda.api.impl.CamundaTaskRetriever;
import io.kadai.camunda.camundasystemconnector.configuration.CamundaConnectorTestConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * Unit test class for Camunda System Connector.
 *
 * @author bbr
 */
@ContextConfiguration(classes = {CamundaConnectorTestConfiguration.class})
@SpringBootTest
class RetrieveCamundaTaskAccTest {

  @Autowired RestTemplate restTemplate;

  @Autowired CamundaTaskRetriever taskRetriever;

  @Autowired ObjectMapper objectMapper;

  private MockRestServiceServer mockServer;

  @BeforeEach
  void setUp() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  void should_GetActiveCamundaTask() {

    String timeStamp = "2019-01-14T15:22:30.811+0000";

    ReferencedTask expectedTask = new ReferencedTask();
    expectedTask.setOutboxEventId("1");
    expectedTask.setOutboxEventType("create");
    expectedTask.setId("801aca2e-1b25-11e9-b283-94819a5b525c");
    expectedTask.setName("modify Request");
    expectedTask.setAssignee("admin");
    expectedTask.setOwner("admin");
    expectedTask.setDescription("blabla");
    expectedTask.setCreated(timeStamp);
    expectedTask.setPriority("50");
    expectedTask.setTaskDefinitionKey("Task_0yogl0i");
    expectedTask.setClassificationKey("Schaden_1");
    expectedTask.setDomain("DOMAIN_B");
    expectedTask.setCustomInt1("1");
    expectedTask.setCustomInt2("2");
    expectedTask.setCustomInt3("3");
    expectedTask.setCustomInt4("4");
    expectedTask.setCustomInt5("5");
    expectedTask.setCustomInt6("6");
    expectedTask.setCustomInt7("7");
    expectedTask.setCustomInt8("8");

    String expectedReplyBody =
        "{"
            + "\n"
            + "\"camundaTaskEvents\":"
            + "[\n "
            + " {\n"
            + "   \"id\": 1,\n"
            + "   \"type\": \"create\",\n"
            + "   \"systemEngineIdentifier\": \"default\",\n"
            + "   \"created\": \"1970-01-01T10:48:16.436+0100\",\n"
            + "   \"payload\": "
            + " \"{\\\"id\\\":\\\"801aca2e-1b25-11e9-b283-94819a5b525c\\\","
            + "            \\\"created\\\":\\\"2019-01-14T15:22:30.811+0000\\\","
            + "            \\\"priority\\\":\\\"50\\\","
            + "            \\\"name\\\":\\\"modify Request\\\","
            + "            \\\"assignee\\\":\\\"admin\\\","
            + "            \\\"description\\\":\\\"blabla\\\","
            + "            \\\"owner\\\":\\\"admin\\\","
            + "            \\\"taskDefinitionKey\\\":\\\"Task_0yogl0i\\\","
            + "            \\\"classificationKey\\\":\\\"Schaden_1\\\","
            + "            \\\"domain\\\":\\\"DOMAIN_B\\\","
            + "            \\\"customInt1\\\":\\\"1\\\","
            + "            \\\"customInt2\\\":\\\"2\\\","
            + "            \\\"customInt3\\\":\\\"3\\\","
            + "            \\\"customInt4\\\":\\\"4\\\","
            + "            \\\"customInt5\\\":\\\"5\\\","
            + "            \\\"customInt6\\\":\\\"6\\\","
            + "            \\\"customInt7\\\":\\\"7\\\","
            + "            \\\"customInt8\\\":\\\"8\\\""
            + "           },\"\n"
            + " }\n"
            + "]"
            + "}";

    String camundaSystemUrl = "http://localhost:8080/";
    String requestUrl = camundaSystemUrl + "events?type=create";
    String systemEngineIdentifier = "default";

    mockServer
        .expect(requestTo(requestUrl))
        .andExpect(method(HttpMethod.GET))
        .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(expectedReplyBody, MediaType.APPLICATION_JSON));

    List<ReferencedTask> actualResult = null;
    try {
      actualResult =
          taskRetriever.retrieveNewStartedCamundaTasks(
              camundaSystemUrl, systemEngineIdentifier, null);
    } catch (Exception e) {
      e.printStackTrace();
    }

    assertThat(actualResult).isNotEmpty();
    assertThat(actualResult.get(0)).isEqualTo(expectedTask);
  }

  @Test
  void should_GetFinishedCamundaTask() {

    ReferencedTask expectedTask = new ReferencedTask();
    expectedTask.setId("2275fb87-1065-11ea-a7a0-02004c4f4f50");
    expectedTask.setOutboxEventId("16");
    expectedTask.setOutboxEventType("complete");

    String expectedReplyBody =
        "{"
            + "\n"
            + "\"camundaTaskEvents\":"
            + "[\n"
            + "    {\n"
            + "        \"id\": 16,\n"
            + "        \"type\": \"complete\",\n"
            + "        \"systemEngineIdentifier\": \"default\",\n"
            + "        \"created\": \"2019-11-26T16:55:52.460+0100\",\n"
            + "        \"payload\": \"{\\\"id\\\":\\\"2275fb87-1065-11ea-a7a0-02004c4f4f50\\\"}\"\n"
            + "    }\n"
            + "]"
            + "}";

    String camundaSystemUrl = "http://localhost:8080";
    String camundaSystemEngineIdentifier = "default";
    mockServer
        .expect(requestTo(camundaSystemUrl + "/events?type=complete&type=delete"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(expectedReplyBody, MediaType.APPLICATION_JSON));

    List<ReferencedTask> actualResult =
        taskRetriever.retrieveFinishedCamundaTasks(
            camundaSystemUrl, camundaSystemEngineIdentifier, null);

    assertThat(actualResult).isNotEmpty();
    assertThat(actualResult.get(0)).isEqualTo(expectedTask);
  }
}
