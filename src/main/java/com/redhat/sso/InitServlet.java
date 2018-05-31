package com.redhat.sso;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class InitServlet extends HttpServlet {
	
  @Override
  public void init(ServletConfig config) throws ServletException {
    Heartbeat.start(120000l); // 2 mins
    super.init(config);
  }

  @Override
  public void destroy() {
    super.destroy();
  }

}