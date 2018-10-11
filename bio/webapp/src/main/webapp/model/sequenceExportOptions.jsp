<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- sequenceExportOptions.jsp -->
<html:xhtml />

<legend>Sequence to export:</legend>
<ol>
<c:forEach items="${exportClassPaths}" var="entry" varStatus="status">
  <c:set var="path" value="${entry.key}" />
  <c:choose>
    <c:when test="${fn:length(exportClassPaths) == 1}">
      <li><html:hidden property="sequencePath" value="${path}" />
            <label>${entry.value}</label></li>
    </c:when>
    <c:otherwise>
       <c:choose>
                <c:when test="${status.first}">
                    <li><input type="radio" name="sequencePath" value="${path}" checked="checked" /><label>${entry.value}</label></li>
                </c:when>
                <c:otherwise>
                     <li><input type="radio" name="sequencePath" value="${path}" /><label>${entry.value}</label></li>
                </c:otherwise>
            </c:choose>
    </c:otherwise>
  </c:choose>
</c:forEach>
</ol>

<!-- /sequenceExportOptions.jsp -->
