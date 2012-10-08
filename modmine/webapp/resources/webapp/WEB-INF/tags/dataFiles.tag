<%@ tag body-content="empty" import="java.util.Set, java.util.List"%>
<%@ attribute name="files" required="true" type="java.lang.Object" %>
<%@ attribute name="dccId" required="true" type="java.lang.String" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<%
  Object o = jspContext.getAttribute("files");
    int filesSize;
  if(o instanceof Set) {
     Set<org.intermine.model.bio.ResultFile> files = (Set)jspContext.getAttribute("files");
     filesSize = files.size();
  } else {
   List<org.intermine.model.bio.ResultFile> files = (List)jspContext.getAttribute("files");
   filesSize = files.size();
  }
%>

  <c:set var="nr" value="<%=filesSize%>" />
  <c:choose>
  <c:when test="${nr > 30}">
  <br></br>
    <a href="http://submit.modencode.org/submit/public/download/${dccId}?root=data"
      title="Access the submission ${nr} files" class="value extlink">
    <c:out value="${nr} files" /> </a>
  </c:when>
  <c:otherwise>
  <c:forEach items="${files}" var="file" varStatus="file_status">
<%--
   <a href="${WEB_PROPERTIES['ftp.prefix']}/${dccId}/extracted/${file}"
      title="Download ${file}" class="value extlink"> <c:out
      value="${file}" /> </a>
      --%>
   <a href="${file.url}"
      title="Download ${file.type} file ${file.name}" class="value extlink"> <c:out
      value="${file.name}" /> </a>
  </c:forEach>
</c:otherwise>
</c:choose>