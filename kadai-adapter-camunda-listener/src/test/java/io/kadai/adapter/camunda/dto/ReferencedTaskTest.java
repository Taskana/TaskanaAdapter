package io.kadai.adapter.camunda.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Acceptance test for ReferencedTask setters and getters. */
class ReferencedTaskTest {

  private final String theValue = "blablabla";
  private ReferencedTask theTask;

  ReferencedTaskTest() {
    theTask = new ReferencedTask();
    theTask.setAssignee("1");
    theTask.setBusinessProcessId("2");
    theTask.setClassificationKey("3");
    theTask.setDescription("bla");
    theTask.setCreated("4");
    theTask.setDomain("5");
    theTask.setPlanned("6");
    theTask.setDue("7");
    theTask.setId("8");
    theTask.setName("9");
    theTask.setOutboxEventId("10");
    theTask.setOutboxEventType("11");
    theTask.setOwner("12");
    theTask.setCustomInt1("13");
    theTask.setCustomInt2("14");
    theTask.setCustomInt3("15");
    theTask.setCustomInt4("16");
    theTask.setCustomInt5("17");
    theTask.setCustomInt6("18");
    theTask.setCustomInt7("19");
    theTask.setCustomInt8("20");
  }

  @Test
  void should_ReturnBusinessProcessId_When_BusinessProcessIdWasSet() {
    theTask.setBusinessProcessId(theValue);
    assertThat(theValue).isEqualTo(theTask.getBusinessProcessId());
  }

  @Test
  void should_ReturnId_When_IdWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setId(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getId());
  }

  @Test
  void should_ReturnOutboxEventId_When_OutboxEventIdWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setOutboxEventId(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getOutboxEventId());
  }

  @Test
  void should_ReturnOutboxEventType_When_OutboxEventTypeWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setOutboxEventType(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getOutboxEventType());
  }

  @Test
  void should_ReturnName_When_NameWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setName(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getName());
  }

  @Test
  void should_ReturnAssignee_When_AssigneeWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setAssignee(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getAssignee());
  }

  @Test
  void should_ReturnCreated_When_CreatedWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setCreated(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getCreated());
  }

  @Test
  void should_ReturnFollowUp_When_FollowUpWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setDue(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getDue());
  }

  @Test
  void should_ReturnDue_When_DueWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setDue(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getDue());
  }

  @Test
  void should_ReturnDescription_When_DescriptionWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setDescription(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getDescription());
  }

  @Test
  void should_ReturnOwner_When_OwnerWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setOwner(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getOwner());
  }

  @Test
  void should_ReturnPriority_When_PriorityWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setPriority(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getPriority());
  }

  @Test
  void should_ReturnSuspended_When_SuspendedWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setSuspended(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getSuspended());
  }

  @Test
  void should_ReturnSystemUrl_When_SystemUrlWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setsystemUrl(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getsystemUrl());
  }

  @Test
  void should_ReturnTaskDefinitionKey_When_TaskDefinitionKeyWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setTaskDefinitionKey(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getTaskDefinitionKey());
  }

  @Test
  void should_ReturnVariables_When_VariablesWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setVariables(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getVariables());
  }

  @Test
  void should_ReturnDomain_When_DomainWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setDomain(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getDomain());
  }

  @Test
  void should_ReturnClassificationKey_When_ClassificationKeyWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setClassificationKey(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getClassificationKey());
  }

  @Test
  void should_ReturnWorkbasketKey_When_WorkbasketKeyWasSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setWorkbasketKey(theValue);
    assertThat(theValue).isEqualTo(referencedTask.getWorkbasketKey());
  }

  @Test
  void should_returnCustomIntegers_when_CustomIntegersWereSet() {
    ReferencedTask referencedTask = new ReferencedTask();
    referencedTask.setCustomInt1("1");
    referencedTask.setCustomInt2("2");
    referencedTask.setCustomInt3("3");
    referencedTask.setCustomInt4("4");
    referencedTask.setCustomInt5("5");
    referencedTask.setCustomInt6("6");
    referencedTask.setCustomInt7("7");
    referencedTask.setCustomInt8("8");

    assertThat(referencedTask.getCustomInt1()).isEqualTo("1");
    assertThat(referencedTask.getCustomInt2()).isEqualTo("2");
    assertThat(referencedTask.getCustomInt3()).isEqualTo("3");
    assertThat(referencedTask.getCustomInt4()).isEqualTo("4");
    assertThat(referencedTask.getCustomInt5()).isEqualTo("5");
    assertThat(referencedTask.getCustomInt6()).isEqualTo("6");
    assertThat(referencedTask.getCustomInt7()).isEqualTo("7");
    assertThat(referencedTask.getCustomInt8()).isEqualTo("8");
  }

  @Test
  void should_ReturnString_When_ToStringIsCalled() {
    assertThat(theTask.toString()).isNotNull();
  }

  @Test
  void should_CheckEquality_When_EqualsIsCalled() {
    ReferencedTask refTask2 = theTask;
    refTask2.setWorkbasketKey("nochnkey");
    assertThat(refTask2).isEqualTo(theTask);
    refTask2 = theTask;
    assertThat(theTask).isEqualTo(refTask2);
    assertThat(theTask).isNotEqualTo("aString");
    assertThat(theTask).isNotEqualTo(null);
    theTask = new ReferencedTask();
    theTask.setAssignee("1");
    theTask.setBusinessProcessId("2");
    theTask.setClassificationKey("3");
    theTask.setDescription("bla");
    theTask.setCreated("4");
    theTask.setDomain("5");
    theTask.setPlanned("6");
    theTask.setDue("7");
    theTask.setId("8");
    theTask.setName("9");
    theTask.setOutboxEventId("10");
    theTask.setOutboxEventType("11");
    theTask.setOwner("12");
    theTask.setWorkbasketKey("13");
    theTask.setCustomInt1("14");
    theTask.setCustomInt2("15");
    theTask.setCustomInt3("16");
    theTask.setCustomInt4("17");
    theTask.setCustomInt5("18");
    theTask.setCustomInt6("19");
    theTask.setCustomInt7("20");
    theTask.setCustomInt8("21");

    refTask2 = new ReferencedTask();
    refTask2.setAssignee("1");
    refTask2.setBusinessProcessId("2");
    refTask2.setClassificationKey("3");
    refTask2.setDescription("bla");
    refTask2.setCreated("4");
    refTask2.setDomain("5");
    refTask2.setPlanned("6");
    refTask2.setDue("7");
    refTask2.setId("8");
    refTask2.setName("9");
    refTask2.setOutboxEventId("10");
    refTask2.setOutboxEventType("11");
    refTask2.setOwner("12");
    refTask2.setWorkbasketKey("13");
    refTask2.setCustomInt1("14");
    refTask2.setCustomInt2("15");
    refTask2.setCustomInt3("16");
    refTask2.setCustomInt4("17");
    refTask2.setCustomInt5("18");
    refTask2.setCustomInt6("19");
    refTask2.setCustomInt7("20");
    refTask2.setCustomInt8("21");

    assertThat(refTask2).isEqualTo(theTask);
    refTask2.setWorkbasketKey("anotherOne");
    assertThat(refTask2).isNotEqualTo(theTask);
  }
}
