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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.redhat.sso.utils.RegExHelper;
import com.redhat.sso.utils.StrParse;

import mjson.Json;

@Path("/")
public class Controller2{
  private static final Logger log=Logger.getLogger(Controller2.class);

  public static void main(String[] asd) throws JsonGenerationException, JsonMappingException, IOException{
    System.setProperty("username", "sa_offering_search");
    System.setProperty("password", "RspvYYReEoo=");
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
    if (d.name.toLowerCase().contains("overview")) return 1+d.name.length();
    if (d.name.toLowerCase().contains("sales kit")) return 100;
    if (d.name.toLowerCase().contains("definition")) return 200;
    if (d.name.toLowerCase().contains("datasheet")) return 300;
    if (d.name.toLowerCase().contains("slide")) return 400;
    if (d.name.toLowerCase().contains("task")) return 500;
    return 100+d.name.length();
  }
  
  private List<Offering> search(String commonTag, String fields, String groupBy) throws IOException{
    int max=100;
    
    String searchUrl="https://mojo.redhat.com/api/core/v3/contents?filter=type(document,file)&filter=tag(" + commonTag + ")&fields=" + fields+"&count="+max;
    
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
      log.debug("adding document: ("+arrayItem.at("id").asString()+" - "+arrayItem.at("resources").at("html").at("ref").asString()+") "+arrayItem.at("subject").asString());
      result.add(new Document(
          arrayItem.at("id").asString(),
          arrayItem.at("subject").asString(),
          null,
          arrayItem.at("content").at("text").asString(),
          arrayItem.at("resources").at("html").at("ref").asString(), //url
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
      if (d.tags.contains("doc_overview") || d.tags.contains("community_offering")){
        overviews.add(d);
        remove.add(d);
      }
    }
    for(Document d:remove) alldocuments.remove(d); remove.clear();
    log.debug(overviews.size() +" overview documents found");
    
    // Now we have a list of overviews, and a separate list (initial) for all other docs
    
    for(Document overview:overviews){
//      log.debug("Found overview: ("+String.format("%s7",overview.id) +") "+overview.name);
      
      if (overview.tags.contains("community_offering")){
        // then this is a submitted community offering with no linked documents so extract the values here
        Offering o=new Offering();
//        o.offering=StrParse.get(overview.name).rightOf(":").trim();
        o.offering=overview.name;
        o.description=extract(overview, overview.description, new String[]{"OFFERING OVERVIEW:"});
        o.type=extractType(overview);
        o.relatedProducts=Arrays.asList(extract(overview, overview.description, new String[]{"PRODUCTS/TECHNOLOGY FOCUS:"}));
        overview.name=StrParse.get(overview.name).rightOf(":").trim();
        o.documents.add(overview);
        
        String d1x=extract(overview, overview.description, new String[]{"COMMUNITY OFFERING DEFINITION DOCUMENT:"});
        o.documents.add(new Document(null, "Offering Definition Document", null, null, d1x, null));
        String d2x=extract(overview, overview.description, new String[]{"SUPPORTING DOCUMENTATION:"});
        o.documents.add(new Document(null, "Supporting Documentation", null, null, d2x, null));
        
        for(Document d:o.documents){
        	d.description=null;
        }
        
        offerings.add(o);
        
      }else{
//        if (true) continue; //DEV ONLY DEV ONLY DEV ONLY DEV ONLY
        // then this is a portfolio or standard offering, solution or program, and has associated documents to link
        
        Offering o=new Offering();
        
        o.type=extractType(overview);
        
        // PROGRAM
        if ("program".equalsIgnoreCase(o.type)){
          // get data from sales kit landing pages
        	
        }
        
        // ################
        // ### SOLUTION ###
        // ################
        if ("solution".equalsIgnoreCase(o.type)){
        	// get data from sales kit landing pages
        	o.offering=Jsoup.parse(StrParse.get(overview.name).leftOf("-").trim()).text();
        	overview.name="Sales Kit";
        	o.description=extractDescription2(overview, overview.description);
        	o.related.addAll(extractSectionListToDocuments("<[Hh]\\d.*?>(.+?)</[Hh]\\d>", overview.description, new String[]{"OFFERINGS"}));
        	o.related.addAll(extractSectionListToDocuments("<[Hh]\\d.*?>(.+?)</[Hh]\\d>", overview.description, new String[]{"STANDARD OFFERINGS"}));
        	o.related.addAll(extractSectionListToDocuments("<[Hh]\\d.*?>(.+?)</[Hh]\\d>", overview.description, new String[]{"RELATED OFFERINGS"}));
        	o.relatedProducts.addAll(extractSectionListToStrings("<[Hh]\\d.*?>(.+?)</[Hh]\\d>", overview.description, new String[]{"TRAINING"}));
        	
//        	Matcher m=Pattern.compile("class.*=.*\"url\".*href=\"(.+?)\".*").matcher(overview.description);
        	
        	String url=RegExHelper.extract(overview.description, "<span.+?class=\\\"url\\\".*?>.*?<a.*?href=\\\"(.*?)\\\"");
        	if (null!=url){
        		o.documents.add(new Document(null, "Sales Kit", null, null, url, null));
        	}else{
        		o.documents.add(overview);
        	}
        	overview.description="";
        	
        }
        
        // ################
        // ### OFFERING ###
        // ################
        if (o.type.contains("_offering")){
        	// get data from overview pages
        	
        	int docTitlePosition=Math.max(overview.name.toLowerCase().indexOf("overview"), overview.name.toLowerCase().indexOf(" page"));
          if (overview.name.lastIndexOf("-")>docTitlePosition){
          	o.offering=StrParse.get(overview.name).rightOf("-").trim();
          	overview.name=StrParse.get(overview.name).leftOf("-").trim();
          }else{
          	o.offering=StrParse.get(overview.name).leftOf("-").trim();
          	overview.name=StrParse.get(overview.name).rightOf("-").trim();
          }
          
//          o.offering=StrParse.get(overview.name).rightOf("-").trim();
          o.description=extractDescription(overview, overview.description, new String[]{"DESCRIPTION:", "Description:"});
          
    //      System.out.println("configs: "+truncate.size());
//          if (truncate.containsKey("offering") && o.offering.length()>Integer.parseInt(truncate.get("offering")))
//            o.offering=o.offering.substring(0, Integer.parseInt(truncate.get("offering"))>o.offering.length()?o.offering.length():Integer.parseInt(truncate.get("offering")))+"...";
          
//          if (truncate.containsKey("description") && o.description.length()>Integer.parseInt(truncate.get("description")))
//            o.description=o.description.substring(0, Integer.parseInt(truncate.get("description"))>o.description.length()?o.description.length():Integer.parseInt(truncate.get("description")))+"...";
          
          o.relatedProducts.addAll(extractHtmlList(overview, overview.description, new String[]{"PRODUCTS &amp; TRAINING:","Products &amp; Training:", "PRODUCTS USED:"}));
    //      o.relatedSolutions.addAll(extractProducts(overview.description, "RELATED SOLUTIONS:"));
          
          o.related.addAll(extractSectionListToDocuments("<[Hh]\\d>(.+?)</[Hh]\\d>", overview.description, new String[]{"RELATED SOLUTIONS:","Related Solutions:"}));
          o.related.addAll(extractSectionListToDocuments("<[Hh]\\d>(.+?)</[Hh]\\d>", overview.description, new String[]{"RELATED OFFERINGS:","Related Offerings:"}));
          
          //now, if the overview has a "Related Documents" section, then append those links too
          o.documents.addAll(extractOtherDocuments2(overview, overview.description, new String[]{"OTHER MATERIALS:", "Other Materials:"}));
          
//          overview.name=StrParse.get(overview.name).leftOf("-").trim();
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
          		log.debug("Overview ("+o.offering+"):: Adding (Mojo) document -> ("+d.id+")"+d.name);
          		o.documents.add(d);
          		remove.add(d);
          	}
          }
          alldocuments.removeAll(remove); remove.clear();
        }
          
        log.debug("Overview ("+o.offering+") type="+o.type);
        
        
        // ############################
        // ### VALIDATE AND FINESSE ###
        // ############################
        if (o.description!=null)
        		o.description=o.description.replaceAll("\\?", " ");
        for(Document d:o.related){
        	String fullName=d.name;
        	d.name=truncateBefore(d.name, 40).replaceAll("\\?", " ");
        	d.alt=fullName;
        	d.description=null; // clear this before sending to client-side
        }
        for(String d:o.relatedProducts)
        	d=d.replaceAll("\\?", " ");
        for(Document d:o.documents){
        	String fullName=d.name;        	
        	d.name=truncateBefore(d.name, 30).replaceAll("\\?", " ");
        	d.alt=fullName;
        	d.description=null; // clear this before sending to client-side
        }
        
        
        // re-order the documents in alphabetical order
        Collections.sort(o.documents, new Comparator<Document>(){
          public int compare(Document o1, Document o2){
            return priority(o1).compareTo(priority(o2));
          }});
        
        offerings.add(o);
        
        //// NEW - this creates the list of offerings in the "associated with" column
        //if ("solution".equalsIgnoreCase(o.type)){
        //	// look up all other overview docs for the same "solution_?" tag and add it as a link in the relatedSOP's list
        //	for(String tag:overview.tags){
        //		if (tag.startsWith("solution_")){
        //			for (Document d:overviews){
        //				if (d.tags.contains(tag) && overview.id!=d.id /* ie. not the current overview doc */){
        //					o.related.add(new Document(null, StrParse.get(d.name).rightOf("-"), null, d.url, null));
        //				}
        //			}
        //		}
        //	}
        //}
        //
        //// NEW - this adds all similarly tagged solutions as "associated with" documents for this offering
        //if (o.type.contains("_offering")){
        //	for(String tag:overview.tags){
        //		
        //	}
        //}
        
        
      }
        
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
  
  private String extractType(Document src){
    String result=null;
    for(String tag:src.getTags()){
    	if ("program".equalsIgnoreCase(tag)) return "program";
    }
    for(String tag:src.getTags()){
    	if ("solution".equalsIgnoreCase(tag)) return "solution";
    }
    for(String tag:src.getTags()){
      if ("portfolio_offering".equalsIgnoreCase(tag)) return "portfolio_offering";
      if ("community_offering".equalsIgnoreCase(tag)) return "community_offering";
      if ("standard_offering".equalsIgnoreCase(tag)) return "standard_offering";
    }
    return result;
  }
  
  private String extract(Document src, String descriptionHtml, String[] tokensInOrder){
    descriptionHtml=descriptionHtml.replaceAll("&#160;", " ");
    String token=null;
    int iDesc=-1;
    for(String t:tokensInOrder){
      if ((iDesc=descriptionHtml.indexOf(t))>=0){
        token=t;
        break;
      }
    }
    if (iDesc<0){
      log.error("Unable to find \""+arrayToString(tokensInOrder)+"\" in document: "+src.getUrl());
      return "NOT FOUND: "+arrayToString(tokensInOrder);
    }
    
    int start=descriptionHtml.substring(0, iDesc).lastIndexOf("<p");
    int end=descriptionHtml.indexOf("</p>", start+1);
    end=descriptionHtml.indexOf("</p>", end+1);
    
    if (start<0 || end<0){
      return "Are you sure one of \""+arrayToString(tokensInOrder)+"\" is within &lt;p&gt; tags?";
    }
    String description=descriptionHtml.substring(start, end); 
    
    String result=Jsoup.parse(description, "UTF-8").text().toString().substring(token.length()).trim(); // strip any html elements (inc the header/token
    
//    System.out.println("DESCRIPTION = "+result);
    
    return result;
  }
  
  private String extractDescription2(Document src, String descriptionHtml){
  	// find span with class="description", extract all text within that span
  	int s=descriptionHtml.indexOf("class=\"description\"");
  	s=descriptionHtml.indexOf(">", s+1);
  	
  	//Matcher m=Pattern.compile("<.*span.*?class=\"description\".*?>(.+?)</span>").matcher(descriptionHtml);
  	
  	int e=descriptionHtml.indexOf("</span>", s);
  	String result=descriptionHtml.substring(s+1, e);
  	return Jsoup.parse(result).text();
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
//    System.out.println(descriptionHtml);
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
      return "Are you sure one of \""+arrayToString(tokensInOrder)+"\" is within &lt;h1&gt; tags?";
    }
    String description=descriptionHtml.substring(start, end); 
    
    String result=Jsoup.parse(description, "UTF-8").text().toString().substring(token.length()).trim(); // strip any html elements (inc the header/token
    
    // this is a hack to remove any non printable characters from the description that JSoup has poorly converted.
    StringBuffer sb=new StringBuffer(result);
    for(int i=result.length()-1;i>=0;i--)
      if ((int)sb.charAt(i)<32 || (int)sb.charAt(i)>126) sb.deleteCharAt(i);
    
    return sb.toString().trim();
  }
  
  private String arrayToString(String[] a){
    StringBuffer sb=new StringBuffer();
    for(String s:a)
      sb.append(s).append(", ");
    return sb.substring(0, sb.length()>2?sb.length()-2:0);
  }
  private List<String> extractHtmlList(Document src, String html, String[] tokensInOrder){
    String token=null;
    int iDesc=-1;
    for(String t:tokensInOrder){
      if ((iDesc=html.indexOf(t))>=0){
        token=t;
        break;
      }
    }
    
//    int iDesc=descriptionHtml.indexOf(token);
    if (iDesc<0){
      log.error("Unable to find any \""+arrayToString(tokensInOrder)+"\" in document: "+src.getUrl());
      return Arrays.asList("MISSING: \""+token+"\""); //abort early if the header token is not in the document
    }
    
    int ulStart=html.indexOf("ul", iDesc);
    int ulEnd=html.indexOf("/ul", ulStart);
    
    String ul=html.substring(ulStart, ulEnd);
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
        li=li.replaceAll("&nbsp;", " ");
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
  
  
  private Map<String, Pattern> regexCache=new HashMap<String, Pattern>();
  private Integer indexOf(String html, String regex){
    return indexOf(html, regex, 0);
  }
  private Integer indexOf(String html, String regex, int fromIndex){
    //Matcher m=Pattern.compile(regex).matcher(fromIndex<html.length()?html.substring(fromIndex):html);
    if (!regexCache.containsKey(regex))
      regexCache.put(regex, Pattern.compile(regex));
    
    Matcher m=regexCache.get(regex).matcher(html);
    int result=-1;
    while (m.find() && result<fromIndex)
      result=m.start();
    return result;
  }
  
  
  private static Pattern LI_ITERATOR_REGEX=Pattern.compile("<[Ll][Ii].*?>(.+?)</[Ll][Ii]>");
  
  private List<Document> extractOtherDocuments2(Document src, String html, String[] tokensInOrder){
    Matcher m=Pattern.compile("<[Hh]\\d.*>(.+?)</[Hh]\\d>").matcher(html);
    int sectStart=-1;
    int sectEnd=html.length();
    List<String> tokens=Arrays.asList(tokensInOrder);
    
    if(m.find()){
    	for (int i=1;i<=m.groupCount();i++){
        String headerTitle=Jsoup.parse(m.group(i)).text().trim();
        if (tokens.contains(headerTitle)){ // you've found a matching header
        	sectStart=m.start(i);
        	sectEnd=m.groupCount()>i?m.start(i+1):html.length();
        	break;
        }
    		
    	}
    }
    
//    while(m.find()){
//      String headerTitle=Jsoup.parse(m.group(1)).text().trim();
//      if (tokens.contains(headerTitle)){ // you've found a matching header
//        sectStart=m.start(1);
//        if (m.find())
//          sectEnd=m.start();
//        break;
//      }
//    }
    
    if (sectStart<0) return new ArrayList<Document>();
    
    // cut just the section we're interested in (the h1 to next start of h1 or end of doc if there is no more sections)
    String htmlSubsection=html.substring(sectStart, sectEnd);
    
    int ulStart=-1;
    ulStart=indexOf(htmlSubsection, "<[UL|ul].*>");
    
    if (ulStart<0) return new ArrayList<Document>(); // exit early if we cant find a list
    
    int ulEnd=indexOf(htmlSubsection, "</[Uu][Ll].*>");
    
    String ul=htmlSubsection.substring(ulStart, ulEnd+"</ul>".length());// cut the list section so we can parse it easier
    
    List<Document> result=new ArrayList<Document>();
    
    Matcher m2=LI_ITERATOR_REGEX.matcher(ul);
    while (m2.find()){
      String item=m2.group(1);
      String name="";
      String url=null;
      if (item.indexOf("<a ")>=0){
        int hrefStart=item.indexOf("href=")+"href=".length()+1;
        url=item.substring(hrefStart, item.indexOf("\"", hrefStart));
      }
      name=Jsoup.parse(item).text().toString().trim();
      
      // check name is not too long, if it is then truncate it
//      name=truncate(name,30);
      
      //if (name.length()>30){
      //	int to=name.indexOf(" ", 30); // next space after 30 chars
      //	if (to<0) to=name.length();// if there's no space after 30 chars then just go to the end
      //	name=name.substring(0, to)+"...";
      //}
      
      String id=null;
      String description=null;
      List<Object> tags=null;
//      log.debug("Overview ("+o.offering+"):: Adding (Other Materials) document -> ("+url+")"+name);
      result.add(new Document(id, name, null, description, url, tags));
    }
    
    return result;
  }
  
  private String truncateAfter(String input, Integer length){
    // check input is not too long, if it is then truncate it
    if (input.length()>length){
    	int to=input.indexOf(" ", length); // next space after 30 chars
    	if (to<0) to=input.length();// if there's no space after 30 chars then just go to the end
    	input=input.substring(0, to)+"...";
    }
    return input;
  }

  private String truncateBefore(String input, Integer length){
    // check input is not too long, if it is then truncate it
    if (input.length()>length){
    	String tmp=input.substring(0, length); // last space before 30 chars
    	int to=tmp.lastIndexOf(" ");
    	if (to<0) to=input.length();// if there's no space after 30 chars then just go to the end
    	input=input.substring(0, to).trim()+"...";
    }
    return input;
  }
  
//  private List<String> extractListFromSection(String matcher, String[] tokensInOrder, Document src){
//  	
//  }
    
//  private List<Document> genericExtractListFromSection(String html, String[] tokensInOrder){
//  	return genericExtractListFromSection("<[Hh]\\d>(.+?)</[Hh]\\d>", html, tokensInOrder);
//  }
  
  private List<String> extractSectionListToStrings(String matcher, String html, String[] tokensInOrder){
  	Matcher m=Pattern.compile(matcher).matcher(html);
    int sectStart=-1;
    int sectEnd=html.length();
    List<String> tokens=Arrays.asList(tokensInOrder);
    
    while(m.find()){
      String headerTitle=Jsoup.parse(m.group(1)).text().trim();
      if (tokens.contains(headerTitle)){ // you've found a matching header
        sectStart=m.start();
        if (m.find())
          sectEnd=m.start();
      }
    }
    
    if (sectStart<0) return new ArrayList<String>();
    
 // cut just the section we're interested in (the h1 to next start of h1 or end of doc if there is no more sections)
    String htmlSubsection=html.substring(sectStart, sectEnd);
    
    int ulStart=-1;
    ulStart=indexOf(htmlSubsection, "<[UL|ul].*>");
    
    if (ulStart<0) return new ArrayList<String>(); // exit early if we cant find a list
    
    int ulEnd=indexOf(htmlSubsection, "</[Uu][Ll].*>");
    
    String ul=htmlSubsection.substring(ulStart, ulEnd+"</ul>".length());// cut the list section so we can parse it easier
    
    List<String> result=new ArrayList<String>();
    
    Matcher m2=LI_ITERATOR_REGEX.matcher(ul);
    while (m2.find()){
      String item=m2.group(1);
      String name="";
      String url=null;
      if (item.indexOf("<a ")>=0){
        int hrefStart=item.indexOf("href=")+"href=".length()+1;
        url=item.substring(hrefStart, item.indexOf("\"", hrefStart));
      }
      name=Jsoup.parse(item).text().toString().trim();
      name=Jsoup.parse(item).text().toString()
      		.replaceAll("&nbsp;", "")
      		.trim();
      
//      // check name is not too long, if it is then truncate it
//      if (name.length()>30){
//      	int to=name.indexOf(" ", 30); // next space after 30 chars
//      	if (to<0){
//      		to=name.length();// if there's no space after 30 chars then just go to the end
//      	}else
//      		name=name.substring(0, to)+"...";
//      }
      
      String id=null;
      String description=null;
      List<Object> tags=null;
//      log.debug("Overview ("+o.offering+"):: Adding (Other Materials) document -> ("+url+")"+name);
      
      result.add("<a href='"+url+"'>"+name+"</a>");
      
//      result.add(new Document(id, name, description, url, tags));
    }
    
    return result;
  }
  
  /**
   * Looks through each header matching the "matcher" regex and tries to find the "tokens" (potential header titles). If it finds on then it looks for the next <ul> and parses that list only 
   * @param matcher
   * @param html
   * @param tokensInOrder
   * @return
   */
  private List<Document> extractSectionListToDocuments(String matcher, String html, String[] tokensInOrder){
    Matcher m=Pattern.compile(matcher).matcher(html);
    int sectStart=-1;
    int sectEnd=html.length();
    List<String> tokens=Arrays.asList(tokensInOrder);
    
    while(m.find()){
      String headerTitle=Jsoup.parse(m.group(1)).text().trim();
      if (tokens.contains(headerTitle)){ // you've found a matching header
        sectStart=m.start();
        if (m.find())
          sectEnd=m.start();
      }
    }
    
    if (sectStart<0) return new ArrayList<Document>();
    
    // cut just the section we're interested in (the h1 to next start of h1 or end of doc if there is no more sections)
    String htmlSubsection=html.substring(sectStart, sectEnd);
    
    int ulStart=-1;
    ulStart=indexOf(htmlSubsection, "<[UL|ul].*>");
    
    if (ulStart<0) return new ArrayList<Document>(); // exit early if we cant find a list
    
    int ulEnd=indexOf(htmlSubsection, "</[Uu][Ll].*>");
    
    String ul=htmlSubsection.substring(ulStart, ulEnd+"</ul>".length());// cut the list section so we can parse it easier
    
    List<Document> result=new ArrayList<Document>();
    
    Matcher m2=LI_ITERATOR_REGEX.matcher(ul);
    while (m2.find()){
      String item=m2.group(1);
      String name="";
      String url=null;
      if (item.indexOf("<a ")>=0){
        int hrefStart=item.indexOf("href=")+"href=".length()+1;
        url=item.substring(hrefStart, item.indexOf("\"", hrefStart));
      }
      name=Jsoup.parse(item).text().toString()
      		.replaceAll("&nbsp;", "")
      		.trim();
      
      
//      name=truncateBefore(name, 30);
//      // check name is not too long, if it is then truncate it
//      if (name.length()>30){
//      	int to=name.indexOf(" ", 30); // next space after 30 chars
//      	if (to<0){
//      		to=name.length();// if there's no space after 30 chars then just go to the end
//      	}else
//      		name=name.substring(0, to)+"...";
//      }
      
      String id=null;
      String description=null;
      List<Object> tags=null;
//      log.debug("Overview ("+o.offering+"):: Adding (Other Materials) document -> ("+url+")"+name);
      result.add(new Document(id, name, null, description, url, tags));
    }
    
    return result;
  }
  
}
