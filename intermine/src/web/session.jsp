<%@ page import="java.util.Enumeration" %>

Your session contains
<% Enumeration atts = session.getAttributeNames();
while (atts.hasMoreElements()) {
        String attName = (String) atts.nextElement();
        out.println(attName + " : " + session.getAttribute(attName));
} %>

