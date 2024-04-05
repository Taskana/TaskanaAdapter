package pro.taskana.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.spi.routing.api.TaskRoutingProvider;
import pro.taskana.spi.routing.internal.TaskRoutingManager;
import pro.taskana.taskrouting.ExampleTaskRouter;

@SpringBootTest(classes = TaskanaAdapterApplication.class)
class TaskanaAdapterApplicationTest {

  private final List<TaskRoutingProvider> taskRoutingProviders;

  TaskanaAdapterApplicationTest(@Autowired TaskanaEngine taskanaEngine) throws Exception {
    TaskRoutingManager taskRoutingManager =
        (TaskRoutingManager) getValueFromPrivateFieldOfSuperclass(taskanaEngine,
            "taskRoutingManager");
    this.taskRoutingProviders =
        (List<TaskRoutingProvider>)
            getValueFromPrivateField(taskRoutingManager, "taskRoutingProviders");
  }

  @Test
  void should_AutowireExampleTaskRouter_When_ApplicationIsStarting() {
    assertThat(taskRoutingProviders).isNotNull().hasSize(1);
    assertThat(taskRoutingProviders.get(0)).isInstanceOf(ExampleTaskRouter.class);
  }

  private Object getValueFromPrivateField(Object obj, String fieldName)
      throws NoSuchFieldException, IllegalAccessException {
    Field nameField = obj.getClass().getDeclaredField(fieldName);
    nameField.setAccessible(true);

    return nameField.get(obj);
  }

  private Object getValueFromPrivateFieldOfSuperclass(Object obj, String fieldName)
      throws NoSuchFieldException, IllegalAccessException {
    Field nameField = obj.getClass().getSuperclass().getDeclaredField(fieldName);
    nameField.setAccessible(true);

    return nameField.get(obj);
  }
}
