package com.redhat.sso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.jsoup.Jsoup;

import com.redhat.sso.model.Document;
import com.redhat.sso.model.Offering;
import com.redhat.sso.model.Solution;
import com.redhat.sso.utils.StrParse;

import mjson.Json;

@Path("/")
public class Controller2{
  private static final Logger log=Logger.getLogger(Controller2.class);

  public static void main(String[] asd) throws JsonGenerationException, JsonMappingException, IOException{
    System.setProperty("username", "sa_offering_search");
    System.setProperty("password", "RspvYYReEoo=");
    List<Offering> result=new Controller2().searchByGroup2("sso_searchable", "tags,subject,content", "offering_");
    System.out.println(com.redhat.sso.utils.Json.newObjectMapper(true).writeValueAsString(result));
  }

  @GET
  @Path("/search")
  public Response search2(@Context HttpServletRequest request) throws JsonGenerationException, JsonMappingException, IOException{
    String filter=request.getParameter("filter");
    String groupBy=null==request.getParameter("groupBy")?"offering_":request.getParameter("groupBy");
    String fields="tags,subject,content";
    return Response.status(200)
        .header("Access-Control-Allow-Origin",  "*")
        .header("Content-Type","application/json")
        .header("Cache-Control", "no-store, must-revalidate, no-cache, max-age=0")
        .header("Pragma", "no-cache")
        .entity(com.redhat.sso.utils.Json.newObjectMapper(true).writeValueAsString(searchByGroup2(filter, fields, groupBy)))
        .build();
  }
  
  private static Map<String, String> truncate=new HashMap<String, String>();
  @GET
  @Path("/config/{action}/{field}/{value}")
  public Response config(@PathParam(value="action") String action, @PathParam(value="field") String field, @PathParam(value="value") String value, @Context HttpServletRequest request) throws JsonGenerationException, JsonMappingException, IOException{
    if ("truncate".equalsIgnoreCase(action))
      truncate.put(field, value);
    
    System.out.println("Saving config: "+action+" "+field+" substring("+value+")");
    return Response.status(200).build();
  }
  
  @GET
  @Path("/lastMessage")
  public Response lastMessage(@Context HttpServletRequest request) throws JsonGenerationException, JsonMappingException, IOException{
    return Response.status(200)
        .header("Access-Control-Allow-Origin",  "*")
        .header("Content-Type","application/json")
        .header("Cache-Control", "no-store, must-revalidate, no-cache, max-age=0")
        .header("Pragma", "no-cache")
        .entity(IOUtils.toString(new FileInputStream(new File("logs/last-message-source.json"))))
        .build();
  }
  

  private List<Offering> searchByGroup2(String commonTag, String fields, String groupBy){
    
    new File("logs").mkdirs();
    String searchUrl="https://mojo.redhat.com/api/core/v3/contents?filter=tag(" + commonTag + ")&fields=" + fields+"&count=100"; // 100 results is the max
    
    log.debug("searchUrl = "+searchUrl);
    
    
    List<Offering> offerings=new ArrayList<Offering>();
    try{
      HttpsURLConnection cnn=(HttpsURLConnection)new URL(searchUrl).openConnection();
      cnn.setRequestMethod("GET");
      cnn.setDoOutput(true);
      
      login(cnn, getUsername(), getPassword());
      
      // read json message
      StringBuffer sb=new StringBuffer(readAndLog("logs/last-message-source.json", cnn.getInputStream()));
      
      log.debug("mojo returned "+sb.length()+" characters. The first 50 are: "+sb.substring(0, sb.length()<50?sb.length():50));
      
      Json x=mjson.Json.read(sb.toString());
      List<Document> initial=new ArrayList<Document>();
      int size=x.at("list").asJsonList().size();
      log.debug("Found "+size+" documents");
      for(int i=0;i<size;i++){
        Json arrayItem=x.at("list").at(i);
        log.debug("adding document: "+arrayItem.at("subject").asString());
        initial.add(new Document(
            arrayItem.at("id").asString(),
            arrayItem.at("subject").asString(),
            arrayItem.at("content").at("text").asString(),
            arrayItem.at("resources").at("html").at("ref").asString(),
            arrayItem.at("tags").asList()
            ));
      }
      
      List<Document> overviews=new ArrayList<Document>();
      List<Document> remove=new ArrayList<Document>();
      for(Document d:initial){
        // find all overview docs
        if (d.tags.contains("doc_overview")){ // TODO: change to doc_overview
          overviews.add(d);
          remove.add(d);
        }
      }
      for(Document d:remove) initial.remove(d); remove.clear();
      log.debug(overviews.size() +" overview documents/offerings found");
      
      // Now we have a list of overviews, and a separate list (initial) for all other docs
      
      for(Document overview:overviews){
        Offering o=new Offering();
        o.offering=StrParse.get(overview.name).rightOf("-").trim();
        o.description=extractDescription(overview.description, "DESCRIPTION:");
        
//        System.out.println("configs: "+truncate.size());
        if (truncate.containsKey("offering"))
          o.offering=o.offering.substring(0, Integer.parseInt(truncate.get("offering"))>o.offering.length()?o.offering.length():Integer.parseInt(truncate.get("offering")));
        if (truncate.containsKey("description"))
          o.description=o.description.substring(0, Integer.parseInt(truncate.get("description"))>o.description.length()?o.description.length():Integer.parseInt(truncate.get("description")));
        
        o.relatedProducts.addAll(extractHtmlList(overview.description, "PRODUCTS USED:"));
//        o.relatedSolutions.addAll(extractProducts(overview.description, "RELATED SOLUTIONS:"));
        o.relatedSolutions.addAll(extractSolutions(overview.description, "RELATED SOLUTIONS:"));
        
        overview.name=StrParse.get(overview.name).leftOf("-").trim();
        overview.description="";
        
        o.documents.add(overview);
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
            d.name=StrParse.get(d.name).leftOf("-").trim();
            d.description="";
            o.documents.add(d);
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
  
  
  private void login(HttpURLConnection cnn, String username, String password) throws ProtocolException{
    // java 8
//    String encoding = java.util.Base64.getEncoder().encodeToString((username+":"+password).getBytes());
    // java 7
    String encoding = Base64.encodeBase64String((username+":"+password).getBytes());
    cnn.setRequestProperty  ("Authorization", "Basic " + encoding);
  }
  
  private String readAndLog(String filepath, InputStream in) throws IOException{
    StringBuffer sb=new StringBuffer();
    BufferedReader br=new BufferedReader(new InputStreamReader(in));
    String buf;
    while ((buf=br.readLine())!=null)
      sb.append(buf+"\n");
    br.close();
    
    FileOutputStream fos=new FileOutputStream(new File(filepath));
    IOUtils.write(sb.toString(), fos);
    IOUtils.closeQuietly(fos);
    return sb.toString();
  }
  
  private String getUsername(){
    String value=System.getenv("username");
    if (value==null) value=System.getProperty("username");
    if (value==null) System.out.println("ERROR: No username configured in environment or property variables");
    if (value==null) log.error("ERROR: No username configured in environment or property variables");
    return value;
  }

  private String getPassword(){
    String value=System.getenv("password");
    if (value==null) value=System.getProperty("password");
    if (value==null) System.out.println("ERROR: No password configured in environment or property variables");
    if (value==null) log.error("ERROR: No password configured in environment or property variables");
    return value;
  }

  
  private String extractDescription(String descriptionHtml, String token){
    int iDesc=descriptionHtml.indexOf(token); //find DESCRIPTION
    
    int start=descriptionHtml.substring(0, iDesc).lastIndexOf("<h1>"); // find the last H1 before DESCRIPTION"
    if (start==-1) start=descriptionHtml.substring(0, iDesc).lastIndexOf("<H1>"); // just in case it's uppercase
    
    int end=descriptionHtml.indexOf("<h1>", start+1); // find the next header h1 as the end point
    if(end==-1) end=descriptionHtml.indexOf("<H1>", start+1); // just in case it's uppercase
    
    String description=descriptionHtml.substring(start, end); 
    
    return Jsoup.parse(description).text().toString().substring(token.length()).trim(); // strip any html elements (inc the header/token
  }
  private List<String> extractHtmlList(String descriptionHtml, String token){
    int iDesc=descriptionHtml.indexOf(token);
    if (iDesc<0) return Arrays.asList("NOT FOUND: \""+token+"\""); //abort early if the header token is not in the document
    
    int ulStart=descriptionHtml.indexOf("ul", iDesc);
    int ulEnd=descriptionHtml.indexOf("/ul", ulStart);
    
    String ul=descriptionHtml.substring(ulStart, ulEnd);
    // now just split by <li>
    
    List<String> result=new ArrayList<String>();
    
    //loop
    int end=0;
    int start=ul.indexOf("<li", end);
    while (start>0){
      end=ul.indexOf("</li>", start);
      String li=ul.substring(start, end);
      String item;
      if (li.indexOf("<a ")>=0){
        int hrefStart=li.indexOf("href=")+"href=".length()+1;
        String url=li.substring(hrefStart, li.indexOf("\"", hrefStart));
        item="<a href=\""+url+"\">"+Jsoup.parse(li).text().toString().trim()+"</a>";
      }else{
        item=Jsoup.parse(li).text().toString().trim();  
      }
//        li=li.substring(li.indexOf("<a "), li.indexOf("</a>")+"</a>".length()); // strip everything except for a link if it exists
//      String item=Jsoup.parse(li).text().toString().trim();
      result.add(item);
      start=ul.indexOf("<li", end);
    }
    
    return result;
  }
  
  private List<Solution> extractSolutions(String descriptionHtml, String token){
    int iDesc=descriptionHtml.indexOf(token);
    if (iDesc<0) return Arrays.asList(new Solution("MISSING: \""+token+"\"", null)); //abort early if the header token is not in the document
    
    int ulStart=descriptionHtml.indexOf("ul", iDesc);
    int ulEnd=descriptionHtml.indexOf("/ul", ulStart);
    
    String ul=descriptionHtml.substring(ulStart, ulEnd);
    // now just split by <li>
    
    List<Solution> result=new ArrayList<Solution>();
    
    //loop
    int end=0;
    int start=ul.indexOf("<li", end);
    while (start>0){
      end=ul.indexOf("</li>", start);
      String li=ul.substring(start, end);
      String name="";
      String url=null;
      if (li.indexOf("<a ")>=0){
        int hrefStart=li.indexOf("href=")+"href=".length()+1;
        url=li.substring(hrefStart, li.indexOf("\"", hrefStart));
//        item="<a href=\""+url+"\">"+Jsoup.parse(li).text().toString().trim()+"</a>";
      }
      name=Jsoup.parse(li).text().toString().trim();  
//        li=li.substring(li.indexOf("<a "), li.indexOf("</a>")+"</a>".length()); // strip everything except for a link if it exists
//      String item=Jsoup.parse(li).text().toString().trim();
      result.add(new Solution(name, url));
      start=ul.indexOf("<li", end);
    }
    
    return result;
  }
  
}