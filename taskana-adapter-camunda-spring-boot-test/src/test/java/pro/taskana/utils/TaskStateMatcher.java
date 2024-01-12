package pro.taskana.utils;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import pro.taskana.task.api.TaskState;
import pro.taskana.task.api.models.TaskSummary;

public class TaskStateMatcher extends BaseMatcher<TaskSummary> {

  private final TaskState expectedTaskState;

  public TaskStateMatcher(TaskState expectedTaskState) {
    this.expectedTaskState = expectedTaskState;
  }

  @Override
  public boolean matches(Object currentTaskSummary) {
    if (currentTaskSummary instanceof TaskSummary) {
      return ((TaskSummary) currentTaskSummary).getState() == expectedTaskState;
    } else {
      return false;
    }
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("expecting TaskState = ").appendText(expectedTaskState.name());
  }
}
