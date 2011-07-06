<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- homologueDisplayer.jsp -->

<div class="basic-table">
<h3>Homologues</h3>
<table class="tiny-font">
  <thead>
  <tr>
    <c:forEach items="${homologues}" var="entry">
      <th><c:out value="${entry.key}"/></th>
    </c:forEach>
  </tr>
  </thead>
  <tbody>
	  <tr>
	  <c:forEach items="${homologues}" var="entry">
	    <c:set var="genes" value="${entry.value}"/>
	    <c:choose>
	      <c:when test="${empty genes}">
	        <td></td>
	      </c:when>
	      <c:otherwise>
	        <td class="one-line">
	          <c:forEach items="${genes}" var="resultElement">
	            <a href="report.do?id=${resultElement.id}">${resultElement.field}</a>
	          </c:forEach>
	        </td>
	      </c:otherwise>
	    </c:choose>
	  </c:forEach>
	  </tr>
  </tbody>
</table>
</div>

<!-- /homologueDisplayer.jsp -->
