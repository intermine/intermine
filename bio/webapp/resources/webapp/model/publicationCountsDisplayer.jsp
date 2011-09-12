<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- publicationCountsDisplayer.jsp -->
<div class="basic-table">
<h3>Publications</h3>

<c:choose>
  <c:when test="${!empty noResults }">
    <p>${noResults}</p>
  </c:when>
  <c:otherwise>

    <table>
    <tr>
        <th>PubMed</th>
        <th>Title</th>
        <th>Number of genes mentioned</th>
    </tr>
    <c:forEach items="${results}" var="entry">

      <c:set var="pub" value="${entry.key}" />
      <c:set var="total" value="${entry.value}" />

       <tr>
            <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${pub.id}"><c:out value="${pub.pubMedId}"/></html:link></td>
            <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${pub.id}"><c:out value="${pub.title}"/></html:link></td>
            <td><c:out value="${total}"/></td>
        </tr>

    </c:forEach>
    </table>
    <div class="show-in-table outer">
      <html:link action="/collectionDetails?id=${object.id}&amp;field=publications&amp;trail=${param.trail}">
        Show all in a table &raquo;
      </html:link>
  </c:otherwise>
</c:choose>
</div>
<!-- /publicationCountsDisplayer.jsp -->
