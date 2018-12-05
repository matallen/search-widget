package com.redhat.sso.model;

import java.util.ArrayList;
import java.util.List;

public class Document{
  public String id;
  public String name;
  public String alt;
//  public String type;
  public String description;
  public String url;
  public List<String> tags;
  public String getId()            {return id;}
  public String getName()          {return name;}
  public String getAlt()          {return alt;}
//  public String getType()          {return type;}
//  public String getDescription()   {return description;}
  public String getUrl()           {return url;}
  public List<String> getTags()    {return tags;}
  
  public Document(String id, String name, String alt,/*String type,*/ String description, String url, List<Object> tags){
    this.id=id;
    this.name=name;
    this.alt=alt;
//    this.type=type;
    this.description=description;
    this.url=url;
    if (null!=tags){
      this.tags=new ArrayList<String>();
      for(Object tag:tags)
        this.tags.add((String)tag);
    }
  }
}
