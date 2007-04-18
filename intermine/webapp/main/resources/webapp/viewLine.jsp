<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- viewLine.jsp -->

<div id="viewDivs">
  <c:forEach var="path" items="${viewStrings}" varStatus="status">
    <c:set var="pathString" value="${path}"/>
    <tiles:insert page="/viewElement.jsp">
      <tiles:put name="pathString" value="${pathString}"/>
      <tiles:put name="errorPath" value="false"/>
    </tiles:insert>
  </c:forEach>
</div>
<div>
  <c:forEach var="path" items="${errorPaths}" varStatus="status">
    <c:set var="pathString" value="${path}"/>
    <tiles:insert page="/viewElement.jsp">
      <tiles:put name="pathString" value="${pathString}"/>
      <tiles:put name="errorPath" value="true"/>
    </tiles:insert>
  </c:forEach>
</div>

<!-- /viewLine.jsp -->

