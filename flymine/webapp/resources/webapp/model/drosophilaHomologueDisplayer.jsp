<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- drosophilaHomologueDisplayer.jsp -->

<div>

<h3>Drosophila 12 genomes homology</h3>
<table>
  <tr>
    <c:forEach items="${homologues}" var="entry">
      <td style="font-size: 0.9em;"><strong><c:out value="${entry.key}"/></strong></td>
    </c:forEach>
  </tr>

  <tr>
  <c:forEach items="${homologues}" var="entry">
    <c:set var="genes" value="${entry.value}"/>
    <c:choose>
      <c:when test="${empty genes}">
        <td style="background-color: orange;"></td>
      </c:when>
      <c:otherwise>
        <c:forEach items="${genes}" var="resultElement">
          <td style="font-size: 0.9em"><a class="theme-1-color" href="objectDetails.do?id=${resultElement.id}">${resultElement.field}</a></td>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </c:forEach>
  </tr>

</table>

</div>

<!-- /drosophilaHomologueDisplayer.jsp -->