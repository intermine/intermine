<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- geneLong.jsp -->
<fmt:setBundle basename="model"/>

<c:set var="seenIdentifiers" value=""/>

<c:if test="${!empty object.synonyms}">
  <fmt:message key="synonyms.external.links"/>:
  <div style="margin-left: 20px">
    <table cellpadding="4">
      <c:forEach items="${object.synonyms}" var="thisSynonym">
        <c:set var="sourceName" value="${thisSynonym.source.name}"/>
        <c:set var="linkProperty"
            value="${sourceName}.${object.organism.genus}.${object.organism.species}.url.prefix"/>
        <c:set var="searchString" value="-:${thisSynonym.value}:-"/>
        <c:if test="${!empty WEB_PROPERTIES[linkProperty] 
                      && (thisSynonym.type == 'identifier' || thisSynonym.type == 'accession'
                          || (thisSynonym.type == 'name' 
                          && thisSynonym.source.title == 'HUGO')) 
                          && ! fn:contains(seenIdentifiers, searchString)}">
          <tr>
            <td>
              <html:link href="${WEB_PROPERTIES[linkProperty]}${thisSynonym.value}"
                         title="${sourceTitle}: ${thisSynonym.value}"
                         target="view_window">
                <html:img src="model/${sourceName}_logo_small.png"/>
              </html:link>
            </td>
            <td>
              <html:link href="${WEB_PROPERTIES[linkProperty]}${thisSynonym.value}"
                         title="${sourceName}: ${thisSynonym.value}"
                         target="view_window">
                ${thisSynonym.value}
              </html:link>
            </td>
          </tr>
          <c:set var="seenIdentifiers" 
                 value="${seenIdentifiers} -:${thisSynonym.value}:-"/>
        </c:if>
      </c:forEach>
    </table>
  </div>
</c:if>
<!-- /geneLong.jsp -->
