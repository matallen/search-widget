package com.redhat.sso.model;

import java.util.ArrayList;
import java.util.List;

public class Offering{
  public String offering;
  public String description;
  public String type;
//  public List<String> relatedProducts=new ArrayList<String>();
  public List<Document> relatedProducts=new ArrayList<Document>();
  public List<Document> related=new ArrayList<Document>();
  public List<Document> documents=new ArrayList<Document>();
  
  public String getType()                    {return type;}
  public String getOffering()                {return offering;}
  public String getDescription()             {return description;}
  public List<Document> getDocuments()       {return documents;}
//  public List<String> getRelatedProducts()   {return relatedProducts;}
  public List<Document> getRelatedProducts()       {return relatedProducts;}
  public List<Document> getRelated()         {return related;}
}
