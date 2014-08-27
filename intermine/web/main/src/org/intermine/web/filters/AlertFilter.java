package org.intermine.web.filters;


import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.intermine.web.logic.session.SessionMethods;
import java.util.HashMap;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.net.URL;

/**
 * Moniters for a message file and displays the message to web users.
 * Message file should contain 1 line with the message to be displayed.
 * This filter needs to be configured in web.xml
 * Parameters
 *       file: the absolute path and filename of the message file (required).
 *       checkFrequency the number of seconds to wait between checks for a file.
 *       displayFrequency then number of seconds to wait between displaying repeated messages.
 * @author Steve Neuahuser
 */
public class AlertFilter implements Filter
{
    private FilterConfig fc;
   
    private static long lastCheck;
    private static HashMap<String,Long> sessions = new HashMap<String,Long>();
    // how long between checks for a file
    private static long checkFrequency = 60000; // 10 min in ms
    // how long between messages to the same user
    private static long displayFrequency = 30000; // 5 min in ms
    private static String message; 
    private static String fileName;
    
    /**
     * Do the filtering.
     * {@inheritDoc}
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;

	HttpServletRequest httpReq = (HttpServletRequest) req;
	// skip webservice URIs and just alert on actions (not .img or .js, etc.)
	if((httpReq.getRequestURI().indexOf("service") == -1)&&
	   (httpReq.getRequestURI().indexOf(".do") != -1)){	
		check(httpReq);
        }
	chain.doFilter(req, response);
    }

    
    private void check(HttpServletRequest httpReq){

	String sessionID = httpReq.getSession().getId();        
        long now = System.currentTimeMillis();
        if((now - checkFrequency) > lastCheck){
		lastCheck = now;
        	checkForFile();
        }
        if(this.message != null){
        	// there is a message to display
        	Long lastAlert = sessions.get(sessionID);
        	if((lastAlert == null) || ((now - displayFrequency) > lastAlert)){
			sessions.put(sessionID,now);
	 		SessionMethods.recordMessage(message, httpReq.getSession());
           	 }
        }
    }
    
    private void checkForFile(){
       if(this.fileName != null){
       	try{ 
		File file = new File(this.fileName);
	
        	if(file.exists()){
			BufferedReader in = new BufferedReader(new FileReader(file));
			this.message = in.readLine();	
        	}else{
			this.message = null;
                }
		if((this.message == null) || (this.message.trim().length()== 0)){
			this.message = null;
			this.sessions.clear();
		}                
       }catch(Exception ex){
	 this.message = null;
	 this.sessions.clear();
       }
      }
    }

    /**
     * Initialise this Filter.
     * {@inheritDoc}
     */
    public void init(FilterConfig filterConfig) {
        this.fc = filterConfig;
	this.fileName = fc.getInitParameter("file");
	String val = fc.getInitParameter("checkFrequency");
	try{
		this.checkFrequency = new Long(val) * 1000;
	}catch(Exception e){}
	
	val = fc.getInitParameter("displayFrequency");
	try{
		this.displayFrequency = new Long(val) * 1000;

	}catch(Exception e){}

    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
       // empty
    }
}
