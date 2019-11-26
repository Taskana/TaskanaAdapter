package pro.taskana.adapter.camunda.parselistener;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.impl.task.listener.ClassDelegateTaskListener;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.taskana.adapter.camunda.tasklistener.TaskanaTaskListener;

import java.util.List;

public class TaskanaParseListener extends AbstractBpmnParseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaTaskListener.class);


    @Override
    public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
        ActivityBehavior behavior = activity.getActivityBehavior();
        if (behavior instanceof UserTaskActivityBehavior) {

            TaskDefinition userTask = ((UserTaskActivityBehavior) behavior).getTaskDefinition();

                    userTask.addTaskListener(TaskListener.EVENTNAME_CREATE, TaskanaTaskListener.getInstance());
                    userTask.addTaskListener(TaskListener.EVENTNAME_COMPLETE,TaskanaTaskListener.getInstance());
                    userTask.addTaskListener(TaskListener.EVENTNAME_DELETE,TaskanaTaskListener.getInstance());
        }

    }
}
