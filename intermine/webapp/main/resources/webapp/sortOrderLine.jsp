<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- sortOrderLine.jsp -->

<div id="sortOrderDivs">

  <c:forEach var="path" items="${sortOrderStrings}" varStatus="status">
    <c:set var="pathString" value="${path}"/>
    <im:sortableDiv path="${pathString}" sortOrderPaths="${sortOrderPaths}" idPrefix="sorting" idPostfix="_${status.index}">
      <div id="querySortOrder">
          ${fn:replace(pathString, ".", " > ")}       
      </div>
    </im:sortableDiv>
  </c:forEach>
</div>
<!-- sortOrderLine.jsp -->