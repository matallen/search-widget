package com.redhat.sso.model;

public class Solution{
  public String name;
  public String url;
  
  public String getUrl()           {return url;}
  public String getName()          {return name;}
  
  public Solution(String name, String url){
    this.name=name;
    this.url=url;
  }
}
