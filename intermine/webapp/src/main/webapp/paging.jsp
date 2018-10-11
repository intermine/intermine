<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- paging.jsp -->

<tiles:importAttribute name="resultsTable" ignore="true" />
<tiles:importAttribute name="currentPage" ignore="true" />
<tiles:importAttribute name="bag" ignore="true" />
<tiles:importAttribute name="invalid" ignore="true" />

<html:xhtml/>

<c:choose>
    <c:when test="${invalid}">
      <span>Showing ${bag.size} values</span>
    </c:when>
    <c:otherwise>

  <%-- Paging controls --%>
    <tiles:insert page="/tablePageLinks.jsp">
      <tiles:put name="resultsTable" beanName="resultsTable" />
      <tiles:put name="currentPage" value="${currentPage}" />
      <tiles:put name="bagName" value="${bag.name}" />
    </tiles:insert>
    &nbsp;|&nbsp;
    <fmt:message key="results.pageinfo.estimate" var="estimateMessage"/>
    <fmt:message key="results.pageinfo.exact" var="exactMessage"/>
    <%-- "Displaying xxx to xxx of xxx rows" messages --%>
    <c:choose>
      <c:when test="${!empty bag}">
        <html:link action="/bagResultsTable.do?bagName=${bag.name}&amp;trail=|bag.${bag.name}">
          View all ${bag.size} records as results
        </html:link>
        <script language="JavaScript">
          <!--
              document.resultsCountText = "${exactMessage} ${bag.size}";
            -->
        </script>
      </c:when>
      <c:when test="${resultsTable.sizeEstimate}">
        <tiles:insert name="tableCountEstimate.tile">
          <tiles:put name="pagedTable" beanName="resultsTable"/>
        </tiles:insert>
        <script language="JavaScript">
          <!--
              document.resultsCountText = "<img src='images/spinner.gif'/> ${estimateMessage} ${resultsTable.estimatedSize}";
              if (jQuery("span#top-estimated-size").exists()) jQuery("span#top-estimated-size").html(document.resultsCountText.replace(/[A-Za-z:$-]/g, ""));
            -->
        </script>
      </c:when>
      <c:otherwise>
        <c:choose>
          <c:when test="${resultsTable.startRow == 0 &&
                        resultsTable.endRow == resultsTable.estimatedSize - 1}">
            <fmt:message key="results.pageinfo.allrows">
              <fmt:param value="${resultsTable.estimatedSize}"/>
            </fmt:message>
          </c:when>
          <c:otherwise>
            <fmt:message key="results.pageinfo.rowrange">
              <fmt:param value="${resultsTable.startRow+1}"/>
              <fmt:param value="${resultsTable.endRow+1}"/>
            </fmt:message>
            <span class="resBar">&nbsp;|&nbsp;</span>
            ${exactMessage}
            ${resultsTable.estimatedSize}
          </c:otherwise>
        </c:choose>
        <script language="JavaScript">
          <!--
              document.resultsCountText = "${exactMessage} ${resultsTable.estimatedSize}";
              if (jQuery("span#top-estimated-size").exists()) jQuery("span#top-estimated-size").html(document.resultsCountText.replace(/[A-Za-z:$-]/g, ""));
            -->
        </script>
      </c:otherwise>
    </c:choose>
    </c:otherwise>
</c:choose>

<!-- /paging.jsp -->
