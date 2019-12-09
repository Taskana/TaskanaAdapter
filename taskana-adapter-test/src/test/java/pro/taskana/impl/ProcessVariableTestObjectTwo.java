package pro.taskana.impl;

import java.io.Serializable;
import java.util.Date;

public class ProcessVariableTestObjectTwo implements Serializable {

    String StringFieldObjectTwo;
    int intFieldObjectTwo;
    double doubleFieldObjectTwo;
    boolean booleanFieldObjectTwo;
    Date dateFieldObjectTwo;

    public ProcessVariableTestObjectTwo(String stringFieldObjectTwo, int intFieldObjectTwo, double doubleFieldObjectTwo,
        boolean booleanFieldObjectTwo, Date dateFieldObjectTwo) {
        StringFieldObjectTwo = stringFieldObjectTwo;
        this.intFieldObjectTwo = intFieldObjectTwo;
        this.doubleFieldObjectTwo = doubleFieldObjectTwo;
        this.booleanFieldObjectTwo = booleanFieldObjectTwo;
        this.dateFieldObjectTwo = dateFieldObjectTwo;
    }

    public String getStringFieldObjectTwo() {
        return StringFieldObjectTwo;
    }

    public void setStringFieldObjectTwo(String stringFieldObjectTwo) {
        StringFieldObjectTwo = stringFieldObjectTwo;
    }

    public int getIntFieldObjectTwo() {
        return intFieldObjectTwo;
    }

    public void setIntFieldObjectTwo(int intFieldObjectTwo) {
        this.intFieldObjectTwo = intFieldObjectTwo;
    }

    public double getDoubleFieldObjectTwo() {
        return doubleFieldObjectTwo;
    }

    public void setDoubleFieldObjectTwo(double doubleFieldObjectTwo) {
        this.doubleFieldObjectTwo = doubleFieldObjectTwo;
    }

    public boolean isBooleanFieldObjectTwo() {
        return booleanFieldObjectTwo;
    }

    public void setBooleanFieldObjectTwo(boolean booleanFieldObjectTwo) {
        this.booleanFieldObjectTwo = booleanFieldObjectTwo;
    }

    public Date getDateFieldObjectTwo() {
        return dateFieldObjectTwo;
    }

    public void setDateFieldObjectTwo(Date dateFieldObjectTwo) {
        this.dateFieldObjectTwo = dateFieldObjectTwo;
    }
}
