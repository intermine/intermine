<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- geneStructureDisplayer.jsp -->

<div>

<c:set var="gene" value="${displayObject.object}"/>

<h3>Gene structure</h3>

<c:if test="${!empty gene.transcripts}">

  <table>
    <c:forEach items="${gene.transcripts}" var="transcript">

      <tr>
        <td>
          Gene <c:out value="${gene.symbol} ${gene.primaryIdentifier}"/>
        </td>
        <td>
          Transcript <c:out value="${transcript.primaryIdentifier}"/>
        </td>
      </tr>

    </c:forEach>

  </table>
</c:if>
<hr />

</div>

<!-- /geneStructureDisplayer.jsp -->
