package com.redhat.sso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import mjson.Json;

@Path("/")
public class Controller{

  @GET
  @Path("/search/grouped")
  public Response search(@Context HttpServletRequest request) throws JsonGenerationException, JsonMappingException, IOException{
    String filter=request.getParameter("filter");
//    String fields=request.getParameter("fields");
//    String filter="tag(offering_cloudforms)";
    String fields="tags,subject,content";
//    return Response.status(200).entity(com.redhat.sso.utils.Json.newObjectMapper(true).writeValueAsString(search(filter, fields))).build();
//    return Response.status(200).header("Access-Control-Allow-Origin",  "*").header("Content-Type","application/json").entity(search(filter, fields)).build();
    return Response.status(200).header("Access-Control-Allow-Origin",  "*").header("Content-Type","application/json").entity(com.redhat.sso.utils.Json.newObjectMapper(true).writeValueAsString(searchByGroup(filter, fields, "offering_"))).build();
  }
  
  @GET
  @Path("/search/grouped2")
  public Response search2(@Context HttpServletRequest request) throws JsonGenerationException, JsonMappingException, IOException{
    String filter=request.getParameter("filter");
//    String fields=request.getParameter("fields");
//    String filter="tag(offering_cloudforms)";
    String fields="tags,subject,content";
//    return Response.status(200).entity(com.redhat.sso.utils.Json.newObjectMapper(true).writeValueAsString(search(filter, fields))).build();
//    return Response.status(200).header("Access-Control-Allow-Origin",  "*").header("Content-Type","application/json").entity(search(filter, fields)).build();
    return Response.status(200)
        .header("Access-Control-Allow-Origin",  "*")
        .header("Content-Type","application/json")
        .entity(com.redhat.sso.utils.Json.newObjectMapper(true).writeValueAsString(searchByGroup2(filter, fields, "offering_")))
        .build();
  }

  
  @GET
  @Path("/search/document")
  public Response searchByDocument(@Context HttpServletRequest request) throws JsonGenerationException, JsonMappingException, IOException{
    String filter=request.getParameter("filter");
    String fields="tags,subject,content";
    return Response.status(200).header("Access-Control-Allow-Origin",  "*").header("Content-Type","application/json").entity(searchByDocument(filter, fields)).build();
  }

  
  public static void main(String[] asd) throws JsonGenerationException, JsonMappingException, IOException{
//    String result=new Controller().search("tag(offering_cloudforms)", "tags,subject,content");
    List<Offering> result=new Controller().searchByGroup("sso_searchable", "tags,subject,content", "offering_");
    
    System.out.println(com.redhat.sso.utils.Json.newObjectMapper(true).writeValueAsString(result));
  }
  
  
  
  private void login(HttpURLConnection cnn, String username, String password) throws ProtocolException{
    // java 8
//    String encoding = java.util.Base64.getEncoder().encodeToString((username+":"+password).getBytes());
    // java 7
    String encoding = Base64.encodeBase64String((username+":"+password).getBytes());
    cnn.setRequestProperty  ("Authorization", "Basic " + encoding);
  }
  
  private void writeLog(String filepath, String payload) throws IOException{
    FileOutputStream fos=new FileOutputStream(new File(filepath));
    IOUtils.write(payload, fos);
    IOUtils.closeQuietly(fos);
  }
  
  class Offering2{
    String offering;
    String description;
    List<String> relatedProducts=new ArrayList<String>();
    List<String> relatedSolutions=new ArrayList<String>();
    List<Document2> documents=new ArrayList<Controller.Document2>();
    public String getOffering()                {return offering;}
    public String getDescription()             {return description;}
    public List<Document2> getDocuments()      {return documents;}
    public List<String> getRelatedProducts()   {return relatedProducts;}
    public List<String> getRelatedSolutions()  {return relatedSolutions;}
  }
  class Document2{
    String id;
    String name;
    String url;
    List<String> tags;
    public String getId()            {return id;}
    public String getName()          {return name;}
    public String getUrl()           {return url;}
    public List<String> getTags()    {return tags;}
    
    public Document2(String id, String name, String url, List<Object> tags){
      this.id=id;
      this.name=name;
      this.url=url;
      this.tags=new ArrayList<String>();
      for(Object tag:tags)
        this.tags.add((String)tag);
    }
  }
  
  class Offering{
    String offering;
    String description;
    List<Document> documents=new ArrayList<Controller.Document>();
    String documents2;
    public String getOffering()           {return offering;}
    public String getDescription()        {return description;}
//    public List<Document> getDocuments()  {return documents;}
    public String getDocuments2()         {return documents2;}
  }
  
  class Document{
    String id;
    String name;
    String description;
    String url;
    List<String> tags;
    public String getId()            {return id;}
    public String getName()          {return name;}
    public String getDescription()   {return description;}
    public String getUrl()           {return url;}
    public List<String> getTags()    {return tags;}
    
    public Document(String id, String name, String description, String url, List<Object> tags){
      this.id=id;
      this.name=name;
      this.description=description;
      this.url=url;
      this.tags=new ArrayList<String>();
      for(Object tag:tags)
        this.tags.add((String)tag);
    }
  }

  private String getUsername(){
    String value=System.getenv("username");
    if (value==null) value=System.getProperty("username");
    if (value==null) System.out.println("ERROR: No username configured in environment or property variables");
    return value;
  }

  private String getPassword(){
    String value=System.getenv("password");
    if (value==null) value=System.getProperty("password");
    if (value==null) System.out.println("ERROR: No password configured in environment or property variables");
    return value;
  }

  private List<Offering> searchByGroup(String commonTag, String fields, String groupBy){
    StringBuffer sb=new StringBuffer();
    
    new File("logs").mkdirs();
    String searchUrl="https://mojo.redhat.com/api/core/v3/contents?filter=tag(" + commonTag + ")&fields=" + fields;

    
    List<Offering> offerings=new ArrayList<Controller.Offering>();
    try{
      HttpsURLConnection cnn=(HttpsURLConnection)new URL(searchUrl).openConnection();
      cnn.setRequestMethod("GET");
      cnn.setDoOutput(true);
      
      
      
      
      login(cnn, getUsername(), getPassword());
      
      // read
      BufferedReader br=new BufferedReader(new InputStreamReader(cnn.getInputStream()));
      String buf;
      while ((buf=br.readLine())!=null)
        sb.append(buf+"\n");
      br.close();
      writeLog("logs/last-message-source.json", sb.toString());
      
      // manipulate json message. I decided to strip rather than include the fields I need. Perhaps include would be more bulletproof but this way was quick.
      Json x=mjson.Json.read(sb.toString());
      x=x.delAt("startIndex");
      x=x.delAt("itemsPerPage");
      
      List<Document> initial=new ArrayList<Controller.Document>();
      
      int size=x.at("list").asJsonList().size();
      for(int i=0;i<size;i++){
        Json arrayItem=x.at("list").at(i);
        initial.add(new Document(
            arrayItem.at("id").asString(),
            arrayItem.at("subject").asString(),
//            arrayItem.at("description").asString(),
            "Lorem ipsum",
            arrayItem.at("resources").at("html").at("ref").asString(),
            arrayItem.at("tags").asList()
            ));
      }
      
      //attempt 2
      List<Document> overviews=new ArrayList<Controller.Document>();
      List<Document> remove=new ArrayList<Controller.Document>();
      for(Document d:initial){
        // find all overview docs
        if (d.tags.contains("offering_overview")){
          overviews.add(d);
          remove.add(d);
        }
      }
      for(Document d:remove) initial.remove(d); remove.clear();
      
      // Now we have a list of overviews, and a separate list (initial) for all other docs
      
      for(Document overview:overviews){
        Offering o=new Offering();
        o.offering=overview.name;
        o.description=overview.description;
        o.documents2="<a href='"+overview.getUrl()+"'>"+overview.getName()+"</a><br/>";
        String groupTag="";
        // find the offering tag to hunt down the related docs
        for(String tag:overview.tags){
          if (tag.startsWith(groupBy)){
            groupTag=tag; break;
          }
        }
        // find the related docs using the groupTag
        for (Document d:initial){
          if (d.tags.contains(groupTag)){
            o.documents2+="<a href='"+d.getUrl()+"'>"+d.getName()+"</a><br/>";
            remove.add(d);
          }
        }
        for(Document d:remove) initial.remove(d); remove.clear();
        
        offerings.add(o);
      }
      
      return offerings;
      
    }catch (MalformedURLException e){
      e.printStackTrace();
    }catch (IOException e){
      e.printStackTrace();
    }
    
    return null;
  }
  
  private List<Offering2> searchByGroup2(String commonTag, String fields, String groupBy){
    StringBuffer sb=new StringBuffer();
    
    new File("logs").mkdirs();
    String searchUrl="https://mojo.redhat.com/api/core/v3/contents?filter=tag(" + commonTag + ")&fields=" + fields;

    
    List<Offering2> offerings=new ArrayList<Controller.Offering2>();
    try{
      HttpsURLConnection cnn=(HttpsURLConnection)new URL(searchUrl).openConnection();
      cnn.setRequestMethod("GET");
      cnn.setDoOutput(true);
      
      login(cnn, getUsername(), getPassword());
      
      // read
      BufferedReader br=new BufferedReader(new InputStreamReader(cnn.getInputStream()));
      String buf;
      while ((buf=br.readLine())!=null)
        sb.append(buf+"\n");
      br.close();
      writeLog("logs/last-message-source.json", sb.toString());
      
      // manipulate json message. I decided to strip rather than include the fields I need. Perhaps include would be more bulletproof but this way was quick.
      Json x=mjson.Json.read(sb.toString());
      x=x.delAt("startIndex");
      x=x.delAt("itemsPerPage");
      
      List<Document2> initial=new ArrayList<Controller.Document2>();
      
      int size=x.at("list").asJsonList().size();
      for(int i=0;i<size;i++){
        Json arrayItem=x.at("list").at(i);
        initial.add(new Document2(
            arrayItem.at("id").asString(),
            arrayItem.at("subject").asString(),
//            arrayItem.at("description").asString(),
//            "Lorem ipsum",
            arrayItem.at("resources").at("html").at("ref").asString(),
            arrayItem.at("tags").asList()
            ));
      }
      
      //attempt 2
      List<Document2> overviews=new ArrayList<Controller.Document2>();
      List<Document2> remove=new ArrayList<Controller.Document2>();
      for(Document2 d:initial){
        // find all overview docs
        if (d.tags.contains("offering_overview")){
          overviews.add(d);
          remove.add(d);
        }
      }
      for(Document2 d:remove) initial.remove(d); remove.clear();
      
      // Now we have a list of overviews, and a separate list (initial) for all other docs
      
      for(Document2 overview:overviews){
        Offering2 o=new Offering2();
        o.offering=overview.name;
        o.description="Lorem Ipsum";//overview.description;
//        o.documents2="<a href='"+overview.getUrl()+"'>"+overview.getName()+"</a><br/>";
        
        o.relatedProducts.add("test");
        o.relatedSolutions.add("test");
        
        o.documents.add(overview);
        String groupTag="";
        // find the offering tag to hunt down the related docs
        for(String tag:overview.tags){
          if (tag.startsWith(groupBy)){
            groupTag=tag; break;
          }
        }
        // find the related docs using the groupTag
        for (Document2 d:initial){
          if (d.tags.contains(groupTag)){
//            o.documents2+="<a href='"+d.getUrl()+"'>"+d.getName()+"</a><br/>";
            o.documents.add(d);
            remove.add(d);
          }
        }
        for(Document2 d:remove) initial.remove(d); remove.clear();
        
        offerings.add(o);
      }
      
      return offerings;
      
    }catch (MalformedURLException e){
      e.printStackTrace();
    }catch (IOException e){
      e.printStackTrace();
    }
    
    return null;
  }
  
  
  private String searchByDocument(String filter, String fields){
    StringBuffer sb=new StringBuffer();
    
    new File("logs").mkdirs();
    String searchUrl="https://mojo.redhat.com/api/core/v3/contents?filter=tag(" + filter + ")&fields=" + fields;

    try{
      HttpsURLConnection cnn=(HttpsURLConnection)new URL(searchUrl).openConnection();
      cnn.setRequestMethod("GET");
      cnn.setDoOutput(true);
      
      login(cnn, "sa_offering_search", "RspvYYReEoo=");
      
      // read
      BufferedReader br=new BufferedReader(new InputStreamReader(cnn.getInputStream()));
      String buf;
      while ((buf=br.readLine())!=null)
        sb.append(buf+"\n");
      br.close();
      writeLog("logs/last-message-source.json", sb.toString());
      
      // manipulate json message. I decided to strip rather than include the fields I need. Perhaps include would be more bulletproof but this way was quick.
      Json x=mjson.Json.read(sb.toString());
      x=x.delAt("startIndex");
      x=x.delAt("itemsPerPage");
      
      int size=x.at("list").asJsonList().size();
      for(int i=0;i<size;i++){
        Json arrayItem=x.at("list").at(i);
        
        arrayItem.set("url", arrayItem.at("resources").at("html").at("ref").asString());
        arrayItem.delAt("resources");
        arrayItem.delAt("typeCode");
        arrayItem.delAt("type");
        String content=arrayItem.at("content").at("text").asString();
        arrayItem.delAt("content");
//        arrayItem.set("content", content);
        
        // strip html and grab a section to be the description
        arrayItem.set("description", "Lorem ipsum");// dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
//        arrayItem.set("description", Jsoup.parse(content).text().toString());
      }
      
      ObjectMapper mapper=com.redhat.sso.utils.Json.newObjectMapper(true);
      Object json = mapper.readValue(x.at("list").toString(), Object.class);
      String result=mapper.writeValueAsString(json);
      writeLog("logs/last-message-converted.json", result);
      
      System.out.print(".");
      
      return result;

    }catch (MalformedURLException e){
      e.printStackTrace();
    }catch (IOException e){
      e.printStackTrace();
    }
    return "";
  }

}
