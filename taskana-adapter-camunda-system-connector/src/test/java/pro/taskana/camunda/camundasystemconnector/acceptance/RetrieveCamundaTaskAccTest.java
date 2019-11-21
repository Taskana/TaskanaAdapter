package pro.taskana.camunda.camundasystemconnector.acceptance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskRetriever;
import pro.taskana.camunda.camundasystemconnector.configuration.CamundaConnectorTestConfiguration;

@RunWith(SpringRunner.class) // SpringRunner is an alias for the SpringJUnit4ClassRunner
@ContextConfiguration(classes = {CamundaConnectorTestConfiguration.class})
@SpringBootTest
@Ignore
public class RetrieveCamundaTaskAccTest {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    CamundaTaskRetriever taskRetriever;

    private MockRestServiceServer mockServer;

    @Before
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testGetActiveCamundaTasks() throws ParseException {

        String timeStamp = "2019-01-14T15:22:30.811+0000";

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .withLocale(Locale.ROOT)
                .withZone(ZoneId.of("UTC"));

        Instant createdAfter = Instant.from(dateTimeFormatter.parse("2019-01-14T15:22:29.811+0000"));


        Date date = java.sql.Timestamp.valueOf(createdAfter.atZone(ZoneId.systemDefault()).toLocalDateTime());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        String expectedBody = "{\"createdAfter\": \"" + formatter.format(date) + "\"}";



        ReferencedTask expectedTask = new ReferencedTask();
        expectedTask.setId("801aca2e-1b25-11e9-b283-94819a5b525c");
        expectedTask.setName("modify Request");
        expectedTask.setCreated(timeStamp);
        expectedTask.setPriority("50");
        expectedTask.setSuspended("false");
        expectedTask.setTaskDefinitionKey("Task_0yogl0i");

        ReferencedTask[] expectedResultBody = new ReferencedTask[] {expectedTask};
        ResponseEntity<ReferencedTask[]> expectedResult = new ResponseEntity<ReferencedTask[]>(expectedResultBody,
                HttpStatus.OK);

        String expectedReplyBody = "[{" +
                "        \"id\": \"801aca2e-1b25-11e9-b283-94819a5b525c\",\r\n" +
                "        \"name\": \"modify Request\",\r\n" +
                "        \"assignee\": null,\r\n" +
                "        \"created\": \"2019-01-14T15:22:30.811+0000\",\r\n" +
                "        \"due\": null,\r\n" +
                "        \"followUp\": null,\r\n" +
                "        \"delegationState\": null,\r\n" +
                "        \"description\": null,\r\n" +
                "        \"executionId\": \"7df99ab8-1b0f-11e9-b283-94819a5b525c\",\r\n" +
                "        \"owner\": null,\r\n" +
                "        \"parentTaskId\": null,\r\n" +
                "        \"priority\": 50,\r\n" +
                "        \"processDefinitionId\": \"generatedFormsQuickstart:1:2454fb85-1b0b-11e9-b283-94819a5b525c\",\r\n"
                +
                "        \"processInstanceId\": \"7df99ab8-1b0f-11e9-b283-94819a5b525c\",\r\n" +
                "        \"taskDefinitionKey\": \"Task_0yogl0i\",\r\n" +
                "        \"caseExecutionId\": null,\r\n" +
                "        \"caseInstanceId\": null,\r\n" +
                "        \"caseDefinitionId\": null,\r\n" +
                "        \"suspended\": false,\r\n" +
                "        \"formKey\": null,\r\n" +
                "        \"tenantId\": null\r\n" +
                "    }]";

        String camundaSystemUrl = "http://localhost:8080/";
        String requestUrl = camundaSystemUrl + "taskana-outbox/rest/events?type=create";

        mockServer
                .expect(requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(expectedReplyBody, MediaType.APPLICATION_JSON));

        List<ReferencedTask> actualResult = null;
        try {
            actualResult = taskRetriever.retrieveActiveCamundaTasks(camundaSystemUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertNotNull(actualResult);
        assertEquals(expectedTask, actualResult.get(0));
    }

    //@Test
    public void testGetFinishedCamundaTasks() throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        Date date = formatter.parse("2019-01-14T15:22:30.811+0100");
        Instant createdAfter = date.toInstant();

        String expectedBody = "{\"finished\" : \"true\", \"createdAfter\": \"" + formatter.format(date) + "\"}";

        ReferencedTask expectedTask = new ReferencedTask();
        expectedTask.setId("801aca2e-1b25-11e9-b283-94819a5b525c");
        expectedTask.setName("modify Request");
        expectedTask.setCreated(formatter.format(date));
        expectedTask.setPriority("50");
        expectedTask.setSuspended("false");

//        ReferencedTask[] expectedResultBody = new ReferencedTask[] {expectedTask};
//        ResponseEntity<ReferencedTask[]> expectedResult = new ResponseEntity<ReferencedTask[]>(expectedResultBody, HttpStatus.OK);

        String expectedReplyBody = "[{" +
                "        \"id\": \"0146d379-fc67-11e8-84f7-94819a5b525c\",\r\n" +
                "        \"processDefinitionKey\": \"loan_approval\",\r\n" +
                "        \"processDefinitionId\": \"loan_approval:2:6c515a29-fc64-11e8-91c8-94819a5b525c\",\r\n" +
                "        \"processInstanceId\": \"0146ac63-fc67-11e8-84f7-94819a5b525c\",\r\n" +
                "        \"executionId\": \"0146ac63-fc67-11e8-84f7-94819a5b525c\",\r\n" +
                "        \"caseDefinitionKey\": null,\r\n" +
                "        \"caseDefinitionId\": null,\r\n" +
                "        \"caseInstanceId\": null,\r\n" +
                "        \"caseExecutionId\": null,\r\n" +
                "        \"activityInstanceId\": \"Task_1er4qhz:0146d378-fc67-11e8-84f7-94819a5b525c\",\r\n" +
                "        \"name\": null,\r\n" +
                "        \"description\": null,\r\n" +
                "        \"deleteReason\": \"completed\",\r\n" +
                "        \"owner\": null,\r\n" +
                "        \"assignee\": \"peter\",\r\n" +
                "        \"startTime\": \"2018-12-10T11:33:16.806+0100\",\r\n" +
                "        \"endTime\": \"2019-01-15T15:22:53.440+0100\",\r\n" +
                "        \"duration\": 3124176634,\r\n" +
                "        \"taskDefinitionKey\": \"Task_1er4qhz\",\r\n" +
                "        \"priority\": 50,\r\n" +
                "        \"due\": null,\r\n" +
                "        \"parentTaskId\": null,\r\n" +
                "        \"followUp\": null,\r\n" +
                "        \"tenantId\": null\r\n" +
                " }]";


        String camundaSystemUrl = "http://localhost:8080/engine-rest";
        mockServer.expect(requestTo(camundaSystemUrl + "history/task/" ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType( org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(content().string( expectedBody))
                .andRespond(withSuccess(expectedReplyBody, MediaType.APPLICATION_JSON));

        List<ReferencedTask> actualResult = taskRetriever.retrieveActiveCamundaTasks(camundaSystemUrl);

        assertNotNull(actualResult);
        assertEquals(expectedTask, actualResult.get(0));
    }

}