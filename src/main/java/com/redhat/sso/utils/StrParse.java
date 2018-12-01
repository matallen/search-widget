package com.redhat.sso.utils;

public class StrParse{
  private String value;
  public StrParse(String value){
    this.value=value;
  }
  public static StrParse get(String value){
    return new StrParse(value);
  }
  public String rightOf(String separator){
    if (value.lastIndexOf(separator)>=0){
      return value.substring(value.lastIndexOf(separator)+1);
    }else
      return value;
  }
  public String leftOf(String separator){
    if (value.lastIndexOf("-")>=0){
      return value.substring(0, value.lastIndexOf(separator)-1);
    }else
      return value;
  }
  public String toString(){
    return value;
  }
}
