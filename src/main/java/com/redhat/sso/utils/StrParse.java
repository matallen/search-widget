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
    if (value.indexOf(separator)>=0){
      return value.substring(value.indexOf(separator)+1);
    }else
      return value;
  }
  public String leftOf(String separator){
    if (value.indexOf("-")>=0){
      return value.substring(0, value.indexOf(separator)-1);
    }else
      return value;
  }
  public String toString(){
    return value;
  }
}
