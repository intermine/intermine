<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<%@ attribute name="searchResult" required="true" type="org.intermine.web.search.KeywordSearchResult" %>
<%@ attribute name="index"        required="true" type="java.lang.Integer" %>
<%@ attribute name="showSelector" required="true" type="java.lang.Boolean" %>

<%-- An individual search result --%>

<%-- The row class --%>
<c:choose>
  <%-- yes, this not the mathematical definition of odd and even. We treat the 1st row as odd, etc --%>
  <c:when test="${index mod 2 == 0}">
    <c:set var="rowClass" value="odd"/>
  </c:when>
  <c:otherwise>
    <c:set var="rowClass" value="even"/>
  </c:otherwise>
</c:choose>

<tr class="keywordSearchResult ${rowClass}">

  <%-- Cell 0: a selector if the user has selected any facets --%>
  <c:if test="${showSelector}">
    <td>
      <input type="checkbox"
            class="item"
            value="${searchResult.id}"
            onclick="updateCheckStatus(this.checked)" /></td>
  </c:if>

  <%-- Cell 1: The Type of the search result --%>
  <td>
    <c:forEach items="${searchResult.types}" var="type" varStatus="typeLoop">
      <c:out value="${imf:formatPathStr(type, INTERMINE_API, WEBCONFIG)}"/>
      <c:if test="${! typeLoop.last}">,</c:if>
    </c:forEach>
  </td>

  <%-- Cell 2: The details of the search result --%>
  <td>
    <im:searchResultKeyFields searchResult="${searchResult}"/>
    <im:searchResultFieldTable searchResult="${searchResult}"/>
  </td>

  <%-- Cell 3: The relevance of this search result --%>
  <td class="relevance">
    <c:choose>
      <c:when test="${searchResult.points mod 2 == 0}">
        <c:forEach var="i" begin="1" end="${searchResult.points div 2}">
          <div class="bullet full">&bull;</div>
        </c:forEach>
        <c:forEach var="i" begin="1" end="${5-(searchResult.points div 2)}">
          <div class="bullet empty">&bull;</div>
        </c:forEach>
      </c:when>
      <c:otherwise>
        <c:forEach var="i" begin="1" end="${(searchResult.points div 2)+0.5}">
          <div class="bullet full">&bull;</div>
        </c:forEach>
        <c:forEach var="i" begin="1" end="${5-((searchResult.points div 2)+0.5)}">
          <div class="bullet empty">&bull;</div>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </td>

</tr>
