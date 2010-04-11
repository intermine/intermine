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

<c:out value="results size: ${pagedResults.exactSize}"/>

<%-- Table displaying results elements --%>
<tiles:insert name="resultsTable.tile">
     <tiles:put name="pagedResults" beanName="pagedResults" />
     <tiles:put name="currentPage" value="searchResults" />
</tiles:insert>
</div>

Scores:
MAP = <c:out value="${scores}"/>
<c:forEach items="${scores.keySet}" var="score">
   <c:out value="${scores[score]}  -  ${score}"/>
</c:forEach>
</div>