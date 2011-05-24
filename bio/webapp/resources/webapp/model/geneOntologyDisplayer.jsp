<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- geneOntologyDisplayer.jsp -->
<h3>Gene Ontology</h3>

<table>
<c:choose>
  <c:when test="${!empty noGoMessage }">
    ${noGoMessage}
  </c:when>
  <c:otherwise>

<c:forEach items="${goTerms}" var="parentEntry">
  <c:set var="parentTerm" value="${parentEntry.key}" />
    <tr>
      <td colspan="2" style="padding-top: 8px;"><h4>${parentTerm}</h4></td>
      <c:choose>
        <c:when test="${empty parentEntry.value}">
          <tr>
            <td class="smallnote" colspan="2"><i>No terms in this category.</i></td>
          </tr>
        </c:when>
        <c:otherwise>
          <c:forEach items="${parentEntry.value}" var="entry">
          <tr>
            <td style="padding-right: 10px;">
              <c:set var="term" value="${entry.key}" />
              <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${term.id}" title="${term.description}">
              <c:out value="${term.name}"/>
              </html:link>
              <img alt="?" title="${term.description}"
                   src="images/icons/information-small-blue.png" style="padding-bottom: 4px;"
                   class="tinyQuestionMark" />
            </td>
            <td>
              <c:set var="evidence" value="${entry.value}" />
              <c:forEach items="${entry.value}" var="evidence">
                <c:out value="${evidence}"/>
                <c:if test="${!empty codes[evidence] }">
                  <img alt="?" title="${codes[evidence]}"
                    src="images/icons/information-small-blue.png" style="padding-bottom: 4px;"
                    class="tinyQuestionMark" />
                </c:if>
                &nbsp;
              </c:forEach>
            </td>
          </tr>
        </c:forEach>
      </c:otherwise>
    </c:choose>

</c:forEach>
</table>
  </c:otherwise>
</c:choose>
<!-- /geneOntologyDisplayer.jsp -->
