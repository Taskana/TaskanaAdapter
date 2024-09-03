package io.kadai.impl;

import java.io.Serializable;
import java.util.Date;

/** Test entity for a complex process variable. */
public class ProcessVariableTestObjectTwo implements Serializable {

  private String stringFieldObjectTwo;
  private int intFieldObjectTwo;
  private double doubleFieldObjectTwo;
  private boolean booleanFieldObjectTwo;
  private Date dateFieldObjectTwo;

  public ProcessVariableTestObjectTwo() {}

  public ProcessVariableTestObjectTwo(
      String stringFieldObjectTwo,
      int intFieldObjectTwo,
      double doubleFieldObjectTwo,
      boolean booleanFieldObjectTwo,
      Date dateFieldObjectTwo) {
    this.stringFieldObjectTwo = stringFieldObjectTwo;
    this.intFieldObjectTwo = intFieldObjectTwo;
    this.doubleFieldObjectTwo = doubleFieldObjectTwo;
    this.booleanFieldObjectTwo = booleanFieldObjectTwo;
    this.dateFieldObjectTwo = dateFieldObjectTwo;
  }

  public String getstringFieldObjectTwo() {
    return stringFieldObjectTwo;
  }

  public void setstringFieldObjectTwo(String stringFieldObjectTwo) {
    this.stringFieldObjectTwo = stringFieldObjectTwo;
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
