<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="javax.servlet.jsp.jstl.core.LoopTagStatus" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<!-- rnaiDisplayer.jsp -->

<h3>RNAi</h3>

<c:choose>
  <c:when test="${!empty noRNAiMessage}">
    <p>${noRNAiMessage}</p>
  </c:when>
  <c:otherwise>

    <table>
    <c:forEach items="${rnaiResults}" var="parentEntry">
      <c:set var="score" value="${parentEntry.key}" />
        <tr>
          <td colspan="2" style="padding-top: 8px;"><h4>${score}</h4></td>
          <c:choose>
            <c:when test="${empty parentEntry.value}">
              <tr>
                <td class="smallnote" colspan="2"><i>No results.</i></td>
              </tr>
            </c:when>
            <c:otherwise>
              <c:forEach items="${parentEntry.value}" var="entry">
              <tr>
                <td style="padding-right: 10px;">
                  <c:set var="screen" value="${entry}" />
                  <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${screen.id}">
                  <c:out value="${screen.name}"/>
                  </html:link>
                </td>
                <td>
                  <c:set var="publication" value="${screen.publication}" />
                  <c:out value="${publication.pubMedId}"/>
                </td>
              </tr>
            </c:forEach>
          </c:otherwise>
        </c:choose>
    </c:forEach>
    </table>

  </c:otherwise>
</c:choose>

<!-- /rnaiDisplayer.jsp -->
