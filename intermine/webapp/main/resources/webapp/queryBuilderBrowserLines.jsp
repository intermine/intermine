<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- queryBuilderBrowserLines.jsp -->

<html:xhtml/>

<%--
<style>
.indent-1 {background:rgba(255, 0, 0, 0.05);}
.indent-2 {background:rgba(0, 255, 0, 0.05);}
.indent-3 {background:rgba(0, 0, 255, 0.05);}
.indent-4 {background:rgba(255, 0, 255, 0.05);}
</style>
--%>

<c:set var="indent" value="0"/>

<c:forEach var="node" items="${nodes}">
  
  <c:set var="node" value="${node}" scope="request"/>
  
  <div id="${node.pathString}" class="browserline indent-${node.indentation} <c:if test="${node.isNull}"> empty</c:if>">
  	<tiles:insert page="/queryBuilderBrowserLine.jsp"/>
  </div>

</c:forEach>

<!-- /queryBuilderBrowserLines.jsp -->