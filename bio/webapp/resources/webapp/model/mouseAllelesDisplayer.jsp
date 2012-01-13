<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- mouseAllelesDisplayer.jsp -->

<div class="inline-list" id="mouse-alleles">
  <style>
  #mouse-alleles span.size-1 { font-size:10px; }
  #mouse-alleles span.size-2 { font-size:12px; }
  #mouse-alleles span.size-3 { font-size:14px; }
  #mouse-alleles div.header h3 { border:0; }
  #mouse-alleles ul li:not(:last-child) { margin-right:10px; }
  </style>

  <h3>Mouse Alleles</h3>
  <c:if test="${not empty counts}">
  <c:forEach var="homologue" items="${counts}">
    <c:if test="${not homologue.value['isMouser']}">
      <div class="header"><h3>${homologue.key} Mouse Homologue</h3></div>
    </c:if>
    <ul>
    <c:forEach var="term" items="${homologue.value['terms']}">
      <li>
      <span class="size-<c:choose>
        <c:when test="${term.value < 2}">1</c:when>
        <c:when test="${term.value < 5}">2</c:when>
        <c:otherwise>3</c:otherwise>
      </c:choose>"><html:link action="/report?id=${homologue.value['homologueId']}">${term.key}</html:link> (${term.value})</span>
      </li>
    </c:forEach>
    </ul>
  </c:forEach>
  </c:if>
</div>

<!-- /mouseAllelesDisplayer.jsp -->