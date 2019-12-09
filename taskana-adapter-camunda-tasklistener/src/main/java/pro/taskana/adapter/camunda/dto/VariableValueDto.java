package pro.taskana.adapter.camunda.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.util.Map;

public class VariableValueDto {

    protected String type;
    @JsonRawValue
    protected Object value;
    protected Map<String, Object> valueInfo;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Map<String, Object> getValueInfo() {
        return valueInfo;
    }

    public void setValueInfo(Map<String, Object> valueInfo) {
        this.valueInfo = valueInfo;
    }


    public VariableValueDto(String type, Object value, Map<String, Object> valueInfo) {
        this.type = type;
        this.value = value;
        this.valueInfo = valueInfo;
    }
}
