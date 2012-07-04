<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="name" required="false" %>
<%@ attribute name="objectIdentifier" required="false" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="userName" required="false" %>

<%@ attribute name="outVarName" required="true" rtexprvalue="false" %>

<%@ variable name-from-attribute="outVarName" 
    variable-class="java.util.List" alias="outVar" scope="AT_END" %>

<%
   org.intermine.api.profile.ProfileManager manager =
       org.intermine.web.SessionMethods.getProfileManager(application);

   java.util.List tags = 
       manager.getTags(getName(), getObjectIdentifier(), getType(), getUserName());
   jspContext.setAttribute("tags", tags);
%>

<c:set var="outVar" value="${tags}"/>
