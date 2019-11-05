package pro.taskana.adapter.camunda.parselistener;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import pro.taskana.adapter.camunda.tasklistener.TaskanaTaskListener;

public class TaskanaParseListener extends AbstractBpmnParseListener {

    @Override
    public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
        ActivityBehavior behavior = activity.getActivityBehavior();
        if (behavior instanceof UserTaskActivityBehavior) {
            ((UserTaskActivityBehavior) behavior).getTaskDefinition()
                    .addTaskListener(TaskListener.EVENTNAME_CREATE, TaskanaTaskListener.getInstance());
        }
    }
}
