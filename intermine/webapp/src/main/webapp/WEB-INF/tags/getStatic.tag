<%@ tag body-content="empty" %>

<%@ attribute name="path" required="true" %>

<%
   String propertiesAttribute = org.intermine.web.logic.Constants.WEB_PROPERTIES;
   java.util.Properties webProperties = 
      (java.util.Properties) session.getServletContext().getAttribute(propertiesAttribute);
   String prefix = webProperties.getProperty("project.helpLocation");
   try {
      out.write(org.intermine.web.logic.WebUtil.getStaticPage(prefix, getPath()));
   } catch (java.io.IOException e) {
%>
      Couldn't retrieve: ${path} (<%= prefix + getPath() %>)
<%
   }
%>
