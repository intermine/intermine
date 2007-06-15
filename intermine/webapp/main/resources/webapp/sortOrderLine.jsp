<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- sortOrderLine.jsp -->
<html:xhtml/>
<div id="sortOrderDivs"></div>

<c:forEach var="results" items="${sortOrderMap}" varStatus="status">
  <c:set var="sortField" value="${results.key}"/>
  <c:set var="sortDirection" value="${results.value}"/>

  <im:sortableDiv path="${sortField}" sortOrderPaths="${sortOrderMap}" idPrefix="sorting"
                  idPostfix="_${status.index}">

    <div id="querySortOrder">
      ${fn:replace(sortField, ".", " > ")}
    </div>

    <!-- down = asc, up = desc -->
    <img src="images/${sortDirection}.gif" id="sortImg"
         onclick="javascript:reverseSortDirection();" alt="sort"
         title="Click to reverse the sort order">

  </im:sortableDiv>
</c:forEach>


<!-- sortOrderLine.jsp -->
