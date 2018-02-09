package com.redhat.sso.model;

import java.util.ArrayList;
import java.util.List;

public class Offering{
  public String offering;
  public String description;
  public List<String> relatedProducts=new ArrayList<String>();
  public List<String> relatedSolutions=new ArrayList<String>();
  public List<Document> documents=new ArrayList<Document>();
  public String getOffering()                {return offering;}
  public String getDescription()             {return description;}
  public List<Document> getDocuments()      {return documents;}
  public List<String> getRelatedProducts()   {return relatedProducts;}
  public List<String> getRelatedSolutions()  {return relatedSolutions;}
}
