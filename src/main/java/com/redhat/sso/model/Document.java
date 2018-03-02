package com.redhat.sso.model;

import java.util.ArrayList;
import java.util.List;

public class Document{
  public String id;
  public String name;
  public String description;
  public String url;
  public List<String> tags;
  public String getId()            {return id;}
  public String getName()          {return name;}
//  public String getDescription()   {return description;}
  public String getUrl()           {return url;}
  public List<String> getTags()    {return tags;}
  
  public Document(String id, String name, String description, String url, List<Object> tags){
    this.id=id;
    this.name=name;
    this.description=description;
    this.url=url;
    if (null!=tags){
      this.tags=new ArrayList<String>();
      for(Object tag:tags)
        this.tags.add((String)tag);
    }
  }
}
