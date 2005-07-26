<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- geneLong.jsp -->
<fmt:setBundle basename="model"/>

<c:if test="${!empty object.synonyms}">
  <fmt:message key="synonyms.external.links"/>:
  <div style="margin-left: 20px">
    <table cellpadding="4">
      <c:forEach items="${object.synonyms}" var="thisSynonym">
        <c:set var="sourceTitle" value="${thisSynonym.source.title}"/>
        <c:set var="linkProperty" value="${sourceTitle}.${object.organism.genus}.${object.organism.species}.url.prefix"/>
        <c:if test="${!empty WEB_PROPERTIES[linkProperty] 
                      && (thisSynonym.type == 'identifier' || thisSynonym.type == 'accession'
                          || (thisSynonym.type == 'name' 
                          && thisSynonym.source.title == 'HUGO'))}">
          <tr>
            <td>
              <html:img src="model/${sourceTitle}_logo_small.png"/>
            </td>
            <td>
              <html:link href="${WEB_PROPERTIES[linkProperty]}${thisSynonym.value}"
                         title="${sourceTitle}: ${thisSynonym.value}"
                         target="view_window">
                ${thisSynonym.value}
              </html:link>
            </td>
          </tr>
        </c:if>
      </c:forEach>
    </table>
  </div>
</c:if>
<!-- /geneLong.jsp -->
