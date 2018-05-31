package com.redhat.sso;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

public class Heartbeat {
  private static final Logger log = Logger.getLogger(Heartbeat.class);
  private static Timer t;
  
  public static void runOnce(){
    new HeartbeatRunnable().run();
  }
  
  public static void start(long intervalInMs) {
    t = new Timer("cop-ninja-heartbeat", false);
    t.scheduleAtFixedRate(new HeartbeatRunnable(), 20000l, intervalInMs);
  }

  public static void stop() {}
  

  static class HeartbeatRunnable extends TimerTask {
    
    @Override
    public void run() {
      log.info("Heartbeat fired");
      
      String url=System.getenv("PING_URL");
      
      if (url==null || "".equals(url)) return;
        try{
        URL obj=new URL(url);
        HttpURLConnection con=(HttpURLConnection)obj.openConnection();
        con.setRequestMethod("GET");
        int responseCode=con.getResponseCode();
  
        log.debug("Ping: called '"+url+"', response code was: "+responseCode);
        if (200!=responseCode){
          BufferedReader in=new BufferedReader(new InputStreamReader(con.getInputStream()));
          String inputLine;
          StringBuffer response=new StringBuffer();
    
          while ((inputLine=in.readLine()) != null){
            response.append(inputLine);
          }
          in.close();
          
          Properties props = new Properties();
          props.put("mail.smtp.starttls.enable", "true");
          props.put("mail.smtp.auth", "true");
          props.put("mail.smtp.host", "smtp.gmail.com");
          props.put("mail.smtp.port", "587");
          
          Session session = Session.getInstance(props);
          MimeMessage message = new MimeMessage(session);
          message.setFrom(new InternetAddress("mojo-search-widget@redhat.com"));
          message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse("mallen@redhat.com"));
          message.setSubject("Health Check Ping Failure: code="+responseCode);
          message.setText("Hit the url \""+url+"\" and got the response code \""+responseCode+"\" and payload \""+response.toString()+"\"");
          Transport.send(message);
        }
      }catch(Exception e){
        throw new RuntimeException(e);
      }
    }      
  }

}
