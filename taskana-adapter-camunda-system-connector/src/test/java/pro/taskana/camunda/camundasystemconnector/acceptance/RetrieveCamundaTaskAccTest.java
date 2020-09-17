package pro.taskana.camunda.camundasystemconnector.acceptance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskRetriever;
import pro.taskana.camunda.camundasystemconnector.configuration.CamundaConnectorTestConfiguration;

/**
 * Unit test class for Camunda System Connector.
 *
 * @author bbr
 */
@RunWith(SpringRunner.class) // SpringRunner is an alias for the SpringJUnit4ClassRunner
@ContextConfiguration(classes = {CamundaConnectorTestConfiguration.class})
@SpringBootTest
public class RetrieveCamundaTaskAccTest {

  @Autowired RestTemplate restTemplate;

  @Autowired CamundaTaskRetriever taskRetriever;

  @Autowired ObjectMapper objectMapper;

  private MockRestServiceServer mockServer;

  @Before
  public void setUp() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  public void testGetActiveCamundaTasks() {

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

    String expectedReplyBody =
        "{"
            + "\n"
            + "\"camundaTaskEvents\":"
            + "[\n "
            + " {\n"
            + "   \"id\": 1,\n"
            + "   \"type\": \"create\",\n"
            + "   \"created\": \"1970-01-01T10:48:16.436+0100\",\n"
            + "   \"payload\": "
            + " \"{\\\"id\\\":\\\"801aca2e-1b25-11e9-b283-94819a5b525c\\\","
            + "            \\\"created\\\":\\\"2019-01-14T15:22:30.811+0000\\\","
            + "            \\\"priority\\\":\\\"50\\\","
            + "            \\\"name\\\":\\\"modify Request\\\","
            + "            \\\"assignee\\\":\\\"admin\\\","
            + "            \\\"description\\\":\\\"blabla\\\","
            + "            \\\"owner\\\":\\\"admin\\\","
            + "            \\\"taskDefinitionKey\\\":\\\"Task_0yogl0i\\\", "
            + "            \\\"classificationKey\\\":\\\"Schaden_1\\\","
            + "            \\\"domain\\\":\\\"DOMAIN_B\\\""
            + "           }\"\n"
            + " }\n"
            + "]"
            + "}";

    String camundaSystemUrl = "http://localhost:8080/";
    String requestUrl = camundaSystemUrl + "events?type=create";

    mockServer
        .expect(requestTo(requestUrl))
        .andExpect(method(HttpMethod.GET))
        .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(expectedReplyBody, MediaType.APPLICATION_JSON));

    List<ReferencedTask> actualResult = null;
    try {
      actualResult = taskRetriever.retrieveNewStartedCamundaTasks(camundaSystemUrl);
    } catch (Exception e) {
      e.printStackTrace();
    }

    assertNotNull(actualResult);
    assertEquals(expectedTask, actualResult.get(0));
  }

  @Test
  public void testGetFinishedCamundaTasks() {

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
            + "        \"created\": \"2019-11-26T16:55:52.460+0100\",\n"
            + "        \"payload\": \"{\\\"id\\\":\\\"2275fb87-1065-11ea-a7a0-02004c4f4f50\\\"}\"\n"
            + "    }\n"
            + "]"
            + "}";

    String camundaSystemUrl = "http://localhost:8080";
    mockServer
        .expect(
            requestTo(
                camundaSystemUrl + "/events?type=complete&type=delete"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(expectedReplyBody, MediaType.APPLICATION_JSON));

    List<ReferencedTask> actualResult =
        taskRetriever.retrieveFinishedCamundaTasks(camundaSystemUrl);

    assertNotNull(actualResult);
    assertEquals(expectedTask, actualResult.get(0));
  }
}
