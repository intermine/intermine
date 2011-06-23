<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- homologueDisplayer.jsp -->

<div>

<h3>Homologues</h3>
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
        <td style="font-size: 0.9em">
          <c:forEach items="${genes}" var="resultElement">
            <a class="theme-1-color" href="report.do?id=${resultElement.id}">${resultElement.field}</a><br/>
          </c:forEach>
        </td>
      </c:otherwise>
    </c:choose>
  </c:forEach>
  </tr>

</table>

</div>

<!-- /homologueDisplayer.jsp -->
