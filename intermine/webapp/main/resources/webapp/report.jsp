<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<!-- objectDetails.jsp -->
<html:xhtml/>

<table>
  <c:forEach var="field" items="${reportObject.objectSummaryFields}">
    <tr>
      <td>${field.name}</td>
      <c:choose>
        <c:when test="${field.valueHasDisplayer}">
          <!-- pass value to displayer -->
          <c:set var="interMineObject" value="${reportObject.object}" scope="request"/>
          <td><tiles:insert page="${field.displayerPage}"><tiles:put name="expr" value="${field.name}" /></tiles:insert></td>
        </c:when>
        <c:otherwise>
          <td>${field.value}</td>
        </c:otherwise>
      </c:choose>
      <td>
        <td><im:typehelp type="${field.pathString}"/></td>
      </td>
    </tr>
  </c:forEach>
</table>