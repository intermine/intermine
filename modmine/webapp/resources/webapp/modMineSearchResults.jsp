<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
    prefix="str"%>


<tiles:importAttribute />

<html:xhtml />

<div class="body">

<tiles:insert name="modMineSearch.tile"/>

Search Term: <c:out value="${searchTerm}"/>

<div>

<c:out value="Matching submissions: ${fn:length(submissions)}"/>
<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
<tr>
    <th>DCC id</th>
    <th>Name</th>
    <th>Group</th>
    <th>Date</th>
    <th>Details</th>
    <th>Search score</th>
</tr>
<c:forEach items="${submissions}" var="subResult">
  <c:set var="sub" value="${subResult.key}"/>
  <tr>
      <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.id}"><c:out value="${sub.dCCid}"></c:out></html:link></td>
      <td>PI: <html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.project.id}"><c:out value="${sub.project.surnamePI}"/></html:link><br/>
          Lab: <html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.lab.id}"><c:out value="${sub.lab.surname}"/></html:link><br/>
      </td>
      <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.id}"><c:out value="${sub.title}"></c:out></html:link></td>
      <td><fmt:formatDate value="${sub.publicReleaseDate}" type="date"/></td>
      <td>
        <c:forEach items="${sub.properties}" var="prop">
          <c:out value="${prop.type}: "/><html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${prop.id}"><c:out value="${prop.name}"/></html:link><br/>
        </c:forEach>
      </td>
      <td><c:out value="${subResult.value}"/></td>
</tr>
</c:forEach>
</table>

<%--<c:out value="results size: ${pagedResults.exactSize}"/>--%>


<%-- Table displaying results elements --%>
<%--<tiles:insert name="resultsTable.tile">
     <tiles:put name="pagedResults" beanName="pagedResults" />
     <tiles:put name="currentPage" value="searchResults" />
</tiles:insert>--%>
</div>

</div>