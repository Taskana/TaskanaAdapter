package pro.taskana.impl;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class ComplexProcessVariableSetter implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date date = dateFormatter.parse("01-01-1970 13:12:11");

        ProcessVariableTestObjectTwo processVariableTestObjectTwo = new ProcessVariableTestObjectTwo("stringValueObjectTwo",2,2.2,true, date);
        ProcessVariableTestObject processVariableTestObject = new ProcessVariableTestObject("stringValue",1,1.1,false,processVariableTestObjectTwo);

        delegateExecution.setVariable("attribute1",processVariableTestObject);
        delegateExecution.setVariable("attribute2","attribute2Value");
        delegateExecution.setVariable("attribute3","attribute3Value");

    }
}
