<%@ tag body-content="empty" %>
<%@ attribute name="type" required="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%-- wraps helplink to properly format the type context help --%>

<%
   String type = (String) jspContext.getAttribute("type");

   request.setAttribute("field", type.substring(type.lastIndexOf(".") + 1));

   org.intermine.objectstore.ObjectStore os = 
      (org.intermine.objectstore.ObjectStore) application.getAttribute("OBJECTSTORE");
   org.intermine.metadata.Model model = os.getModel();
   java.util.Map classDescriptions = (java.util.Map) application.getAttribute("classDescriptions");
   String helpText = org.intermine.web.logic.HelpUtil.getHelpText(model, classDescriptions, type);
   request.setAttribute("helpText", helpText);
%>

<c:if test="${!empty helpText}"><im:helplink text="${field}: ${helpText}"/></c:if>
