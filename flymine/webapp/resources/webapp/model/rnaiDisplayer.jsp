<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- rnaiDisplayer.jsp -->
<div class="basic-table">
<h3>RNAi</h3>

<c:choose>
  <c:when test="${!empty noRNAiMessage }">
    <p>${noRNAiMessage}</p>
  </c:when>
  <c:otherwise>

    <table>
    <c:forEach items="${results}" var="parentEntry">
      <c:set var="score" value="${parentEntry.key}" />
        <thead>
          <tr><th colspan="2">${score}</th></tr>
        </thead>
        <tbody>
        <tr>
          <c:choose>
            <c:when test="${empty parentEntry.value}">
              <tr>
                <td class="smallnote" colspan="2"><i>No results in this category.</i></td>
              </tr>
            </c:when>
            <c:otherwise>
              <c:forEach items="${parentEntry.value}" var="entry">
                <tr>
                  <td>
                    <c:set var="screen" value="${entry.key}" />
                    <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${screen.id}">
                    <c:out value="${screen.field}"/>
                    </html:link>
                  </td>
                  <td>PubMed:
                    <c:set var="pubmed" value="${entry.value}" />
                    <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${pubmed.id}">
                    <c:out value="${pubmed.field}"/>
                    </html:link>
                  </td>
                </tr>
              </c:forEach>
            </c:otherwise>
          </c:choose>
        </tr>
        </tbody>
    </c:forEach>
    </table>

  </c:otherwise>
</c:choose>
</div>
<!-- /rnaiDisplayer.jsp -->
