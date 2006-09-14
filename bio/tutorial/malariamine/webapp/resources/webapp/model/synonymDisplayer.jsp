<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- synonymDisplayer.jsp -->
<fmt:setBundle basename="model"/>

<c:set var="seenIdentifiers" value=""/>

<c:if test="${!empty object.synonyms}">
  <fmt:message key="synonyms.external.links"/>:
  <div style="margin-left: 20px">
    <table cellpadding="4">
      <c:forEach items="${object.synonyms}" var="thisSynonym">

        <c:set var="sourceName" value="${fn:replace(thisSynonym.source.name, ' ', '_')}"/>
        <c:set var="genus" value="${object.organism.genus}"/>
        <c:set var="species" value="${object.organism.species}"/>
        <c:choose>
            <c:when test='${genus == null || species == null}'>
                <c:set var="linkProperty" value="${sourceName}.url.prefix"/>
            </c:when>
            <c:otherwise>
                <c:set var="linkProperty" value="${sourceName}.${genus}.${species}.url.prefix"/>
            </c:otherwise>
        </c:choose>
        <c:set var="linkPrefix" value="${WEB_PROPERTIES[linkProperty]}"/>
        <c:set var="href" value="${linkPrefix}${thisSynonym.value}"/>
        <c:if test="${!empty WEB_PROPERTIES[linkProperty]
                      && (thisSynonym.type == 'identifier' 
                          || ((thisSynonym.type == 'name' || thisSynonym.type == 'accession')
                               && sourceName == 'HUGO'))
                      && (sourceName != 'FlyBase' || fn:startsWith(thisSynonym.value, 'FBgn'))
                      && seenUrls[linkPrefix] == null}">
          
          <jsp:useBean id="seenUrls" scope="page" class="java.util.HashMap">
            <c:set target="${seenUrls}" property="${linkPrefix}" value="${linkPrefix}"/>
          </jsp:useBean>
          <tr>
            <td>
              <html:link href="${href}"
                         title="${sourceName}: ${thisSynonym.value}"
                         target="view_window">
                <html:img src="model/${sourceName}_logo_small.png"/>
              </html:link>
            </td>
            <td>
              <html:link href="${href}"
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
<!-- /synonymDisplayer.jsp -->
