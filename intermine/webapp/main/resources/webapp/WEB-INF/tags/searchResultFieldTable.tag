<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<%@ attribute name="searchResult" required="true" type="org.intermine.web.search.KeywordSearchResult" %>

<%-- print each field configured for this object --%>
<table class="inner">
  <%-- Show a row for each additional field. --%>
  <c:forEach items="${searchResult.additionalFieldValues}" var="field">
    <tr class="objectField">
      <td class="objectFieldName">
        <c:out value="${imf:formatFieldChain(field.field, INTERMINE_API, WEBCONFIG)}"/>:
      </td>
      <td class="value">
        <im:searchResultField field="${field}" nullValue=""/>
      </td>
    </tr>
  </c:forEach>
</table>

<%-- Offer to use this result in a template --%>
<c:if test="${searchResult.templates != null}">
  <c:forEach items="${searchResult.templates}" var="template">
    <c:if test="${template.value.valid}">
      <div>
        <html:link action="/template?name=${template.value.name}&amp;scope=global&amp;idForLookup=${searchResult.id}"
                    title="Use this search result in a query">
          <span class="templateTitle">${template.value.title}</span>
          <img border="0" class="arrow" src="images/icons/templates-16.png" />
        </html:link>
      </div>
    </c:if>
  </c:forEach>
</c:if>
