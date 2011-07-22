
<%@tag import="org.intermine.util.DynamicUtil"%>
<%@ tag body-content="empty" %>
<%@ attribute name="instanceofObject" required="true" type="java.lang.Object"%>
<%@ attribute name="instanceofClass" required="true" %>
<%@ attribute name="instanceofVariable" required="true" %>

<%-- evaluate whether the object is an instance of the class --%>

<%
    String variable = (String) jspContext.getAttribute("instanceofVariable");

    try {
        Object o = jspContext.getAttribute("instanceofObject");
        String c = (String) jspContext.getAttribute("instanceofClass");

        Class<?> clazz = Class.forName(c);

        if (DynamicUtil.isInstance(o, clazz)) {
            request.setAttribute(variable, "true");
        } else {
            request.setAttribute(variable, "false");
        }
   } catch (Exception e) {
      request.setAttribute(variable, "");
   }
%>
