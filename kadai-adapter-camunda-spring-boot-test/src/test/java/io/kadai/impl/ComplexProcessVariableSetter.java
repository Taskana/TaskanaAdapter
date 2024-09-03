package io.kadai.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.Variables.SerializationDataFormats;
import org.camunda.bpm.engine.variable.value.ObjectValue;

/**
 * This class is responsible for setting complex variables containing Objects on a camunda user
 * task.
 */
public class ComplexProcessVariableSetter implements JavaDelegate {

  @Override
  public void execute(DelegateExecution delegateExecution) throws Exception {

    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    Date date = dateFormatter.parse("01-01-1970 13:12:11");

    String specialCharactersString =
        "\fForm feed \b Backspace \t Tab \\Backslash \n newLine \r Carriage return \" DoubleQuote";

    if (delegateExecution.getCurrentActivityName().equals("ComplexVariableSetter")) {
      ProcessVariableTestObjectTwo processVariableTestObjectTwo =
          new ProcessVariableTestObjectTwo("stringValueObjectTwo", 2, 2.2, true, date);
      ProcessVariableTestObject processVariableTestObject =
          new ProcessVariableTestObject(
              specialCharactersString,
              1,
              1.1,
              false,
              Collections.singletonList(processVariableTestObjectTwo));

      delegateExecution.setVariable("attribute1", processVariableTestObject);
      delegateExecution.setVariable("attribute2", 5);
      delegateExecution.setVariable("attribute3", true);

    } else if (delegateExecution.getCurrentActivityName().equals("BigComplexVariableSetter")) {

      List<ProcessVariableTestObjectTwo> processVariableTestObjectTwoList = new ArrayList<>();
      for (int i = 0; i < 10000; i++) {
        processVariableTestObjectTwoList.add(
            new ProcessVariableTestObjectTwo("stringValueObjectTwo", 2, 2.2, true, date));
      }
      ProcessVariableTestObject bigProcessVariableTestObject =
          new ProcessVariableTestObject(
              specialCharactersString, 1, 1.1, false, processVariableTestObjectTwoList);

      ObjectValue customerDataValue =
          Variables.objectValue(bigProcessVariableTestObject)
              .serializationDataFormat(SerializationDataFormats.JSON)
              .create();

      delegateExecution.setVariable("attribute1", customerDataValue);
    }
  }
}
