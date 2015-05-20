<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<%@ attribute name="searchResult" required="true" type="org.intermine.web.search.KeywordSearchResult" %>

<%-- The link to the search result --%>
<%-- link in results should go to object details unless other link is in config --%>
<%-- TODO: TOO MUCH LOGIC ALERT!! this should live in a controller!! --%>
<c:set var="extlink" value="" />
<c:set var="linkClass" value=""/>
<c:set var="linkTarget" value=""/>
<c:choose>
  <c:when test="${!empty searchResult.linkRedirect}">
    <c:set var="detailsLink" value="${searchResult.linkRedirect}" scope="request" />
    <c:set var="linkClass" value="extlink"/>
    <c:set var="linkTarget" value="_blank"/>
  </c:when>
  <c:otherwise>
    <c:set var="detailsLink"
        value="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${searchResult.id}&amp;trail=${param.trail}|${searchResult.id}"
        scope="request" />
  </c:otherwise>
</c:choose>

<div class="objectKeys"> <%-- The key fields. --%>
  <%-- A link pointing to a report page. --%>
  <a href="${detailsLink}" class="${linkClass}" target="${linkTarget}">

    <c:forEach items="${searchResult.keyFieldValues}" var="field" varStatus="status">
      <c:set var="fieldLabel" value="${imf:formatFieldChain(field.field, INTERMINE_API, WEBCONFIG)}"/>
      <span title="<c:out value="${fieldLabel}"/>" class="objectKey">
        <im:searchResultField field="${field}" nullValue="-"/>
      </span>
      <c:if test="${! status.last }"><span class="divider">|</span></c:if>
    </c:forEach>
  </a>
</div> <%-- end objectKeys --%>

