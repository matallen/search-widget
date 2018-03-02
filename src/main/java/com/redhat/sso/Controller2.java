package com.redhat.sso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
    System.setProperty("username", "redacted");
    System.setProperty("password", "redacted");
    List<Offering> result=new Controller2().search("sso_searchable", "tags,subject,content", "offering_");
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
        //.entity(com.redhat.sso.utils.Json.newObjectMapper(true).writeValueAsString(searchByGroup2(filter, fields, groupBy)))
        .entity(com.redhat.sso.utils.Json.newObjectMapper(true).writeValueAsString(search(filter, fields, groupBy)))
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
  
  private Integer priority(Document d){
    if (d.name.toLowerCase().contains("offering page")) return 0;
    if (d.name.toLowerCase().contains("definition")) return 1;
    if (d.name.toLowerCase().contains("datasheet")) return 2;
    if (d.name.toLowerCase().contains("slide")) return 3;
    if (d.name.toLowerCase().contains("task")) return 4;
    return 100+d.name.length();
  }
  
  private List<Offering> search(String commonTag, String fields, String groupBy) throws IOException{
    int max=100;
    
    String searchUrl="https://mojo.redhat.com/api/core/v3/contents?filter=tag(" + commonTag + ")&fields=" + fields+"&count="+max;
    
    List<Offering> allOfferings=new ArrayList<Offering>();
    List<Document> alldocuments=new ArrayList<Document>();
    
    Json response=callMojoApi(searchUrl);
    alldocuments.addAll(getDocuments(response));
    while (null!=getNext(response)){
      response=callMojoApi(getNext(response));
      alldocuments.addAll(getDocuments(response));
    }
    allOfferings.addAll(aggregateIntoOfferings(alldocuments, groupBy));
    return allOfferings;
  }
  
  private String getNext(Json response){
    if (response.has("links")){
      if (response.at("links").has("next")){
        return response.at("links").at("next").asString();
      }
    }
    return null;
  }
  
  private List<Document> getDocuments(Json json){
    List<Document> result=new ArrayList<Document>();
    int size=json.at("list").asJsonList().size();
    log.debug("Found "+size+" documents");
    for(int i=0;i<size;i++){
      Json arrayItem=json.at("list").at(i);
      log.debug("adding document: "+arrayItem.at("subject").asString());
      result.add(new Document(
          arrayItem.at("id").asString(),
          arrayItem.at("subject").asString(),
          arrayItem.at("content").at("text").asString(),
          arrayItem.at("resources").at("html").at("ref").asString(),
          arrayItem.at("tags").asList()
          ));
    }
    return result;
  }
  
  private List<Offering> aggregateIntoOfferings(List<Document> alldocuments, String groupBy){
    List<Offering> offerings=new ArrayList<Offering>();
    
    List<Document> overviews=new ArrayList<Document>();
    List<Document> remove=new ArrayList<Document>();
    for(Document d:alldocuments){
      // find all overview docs
      if (d.tags.contains("doc_overview")){ // TODO: change to doc_overview
        overviews.add(d);
        remove.add(d);
      }
    }
    for(Document d:remove) alldocuments.remove(d); remove.clear();
    log.debug(overviews.size() +" overview documents found");
    
    // Now we have a list of overviews, and a separate list (initial) for all other docs
    
    for(Document overview:overviews){
      Offering o=new Offering();
      o.offering=StrParse.get(overview.name).rightOf("-").trim();
      o.description=extractDescription(overview, overview.description, new String[]{"DESCRIPTION:", "Description:"});
      
//      System.out.println("configs: "+truncate.size());
      if (truncate.containsKey("offering") && o.offering.length()>Integer.parseInt(truncate.get("offering")))
        o.offering=o.offering.substring(0, Integer.parseInt(truncate.get("offering"))>o.offering.length()?o.offering.length():Integer.parseInt(truncate.get("offering")))+"...";
      
      if (truncate.containsKey("description") && o.description.length()>Integer.parseInt(truncate.get("description")))
        o.description=o.description.substring(0, Integer.parseInt(truncate.get("description"))>o.description.length()?o.description.length():Integer.parseInt(truncate.get("description")))+"...";
      
      o.relatedProducts.addAll(extractHtmlList(overview, overview.description, new String[]{"PRODUCTS &amp; TRAINING:","Products &amp; Training:", "PRODUCTS USED:"}));
//      o.relatedSolutions.addAll(extractProducts(overview.description, "RELATED SOLUTIONS:"));
      o.relatedSolutions.addAll(extractSolutions(overview, overview.description, new String[]{"RELATED SOLUTIONS:","Related Solutions:"}));
      
      //now, if the overview has a "Related Documents" section, then append those links too
      o.documents.addAll(extractOtherDocuments(overview, overview.description, new String[]{"OTHER MATERIALS:", "Other Materials:"}));
      
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
      for (Document d:alldocuments){
        if (d.tags.contains(groupTag)){
          d.name=StrParse.get(d.name).leftOf("-").trim();
          d.description="";
          o.documents.add(d);
          remove.add(d);
        }
      }
      for(Document d:remove) alldocuments.remove(d); remove.clear();
      

      
      
      // re-order the documents in alphabetical order
      Collections.sort(o.documents, new Comparator<Document>(){
        public int compare(Document o1, Document o2){
          return priority(o1).compareTo(priority(o2));
      }});
      
      offerings.add(o);
    }
    
    log.debug("aggregated into "+offerings.size() +" offerings");
    
    return offerings;
  }
  
  
  private Json callMojoApi(String searchUrl) throws IOException{
//    String searchUrl="https://mojo.redhat.com/api/core/v3/contents?filter=tag(" + commonTag + ")&fields=" + fields+"&count="+max;
    log.debug("calling: "+searchUrl);
    HttpsURLConnection cnn=(HttpsURLConnection)new URL(searchUrl).openConnection();
    cnn.setRequestMethod("GET");
    cnn.setDoOutput(true);
    login(cnn, getUsername(), getPassword());
    new File("logs").mkdirs();
    StringBuffer sb=new StringBuffer(readAndLog("logs/last-message-source.json", cnn.getInputStream()));
//    return sb.toString();
    Json x=mjson.Json.read(sb.toString());
    return x;
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

  
  private String extractDescription(Document src, String descriptionHtml, String[] tokensInOrder){
    String token=null;
    int iDesc=-1;
    for(String t:tokensInOrder){
      if ((iDesc=descriptionHtml.indexOf(t))>=0){
        token=t;
        break;
      }
    }
    
//    int iDesc=descriptionHtml.indexOf(token); //find DESCRIPTION
//    if (iDesc<0) descriptionHtml.indexOf(token); //find Description
    
    if (iDesc<0){
      log.error("Unable to find \""+arrayToString(tokensInOrder)+"\" in document: "+src.getUrl());
      return "DESCRIPTION NOT FOUND";
    }
    
    int start=descriptionHtml.substring(0, iDesc).lastIndexOf("<h1"); // find the last H1 before DESCRIPTION"
    if (start==-1) start=descriptionHtml.substring(0, iDesc).lastIndexOf("<H1"); // just in case it's uppercase
    
    int end=descriptionHtml.indexOf("<h1", start+1); // find the next header h1 as the end point
    if(end==-1) end=descriptionHtml.indexOf("<H1", start+1); // just in case it's uppercase
    
    if (start<0 || end<0){
      return "Are you sure \""+token+"\" is within &lt;h1&gt; tags?";
    }
    String description=descriptionHtml.substring(start, end); 
    
    return Jsoup.parse(description).text().toString().substring(token.length()).trim(); // strip any html elements (inc the header/token
  }
  
  private String arrayToString(String[] a){
    StringBuffer sb=new StringBuffer();
    for(String s:a)
      sb.append(s).append(", ");
    return sb.substring(0, sb.length()>2?sb.length()-2:0);
  }
  private List<String> extractHtmlList(Document src, String descriptionHtml, String[] tokensInOrder){
    String token=null;
    int iDesc=-1;
    for(String t:tokensInOrder){
      if ((iDesc=descriptionHtml.indexOf(t))>=0){
        token=t;
        break;
      }
    }
    
//    int iDesc=descriptionHtml.indexOf(token);
    if (iDesc<0){
      log.error("Unable to find any \""+arrayToString(tokensInOrder)+"\" in document: "+src.getUrl());
      return Arrays.asList("MISSING: \""+token+"\""); //abort early if the header token is not in the document
    }
    
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
  
  private List<Solution> extractSolutions(Document src, String descriptionHtml, String[] tokensInOrder){
    String token=null;
    int iDesc=-1;
    for(String t:tokensInOrder){
      if ((iDesc=descriptionHtml.indexOf(t))>=0){
        token=t;
        break;
      }
    }
    
//    int iDesc=descriptionHtml.indexOf(token);
    if (iDesc<0){
      log.error("Unable to find \""+arrayToString(tokensInOrder)+"\" in document: "+src.getUrl());
      return Arrays.asList(new Solution("MISSING: \""+token+"\"", null)); //abort early if the header token is not in the document
    }
    
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
  
  
  private List<Document> extractOtherDocuments(Document src, String html, String[] tokensInOrder){
    String token=null;
    int iDesc=-1;
    for(String t:tokensInOrder){
      if ((iDesc=html.indexOf(t))>=0){
        token=t;
        break;
      }
    }
    
    if (iDesc<0) return new ArrayList<Document>();
    
    int ulStart=html.indexOf("ul", iDesc);
    int ulEnd=html.indexOf("/ul", ulStart);
    
    String ul=html.substring(ulStart, ulEnd);
    
    List<Document> result=new ArrayList<Document>();
    
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
      }
      name=Jsoup.parse(li).text().toString().trim();
      
      String id=null;
      String description=null;
      List<Object> tags=null;
      result.add(new Document(id, name, description, url, tags));
      start=ul.indexOf("<li", end);
    }
    
    return result;
  }
  
}
