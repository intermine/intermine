<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<html:xhtml/>

<!-- bagRunnerMsg.jsp -->
<tiles:useAttribute id="lookupResults" name="lookupResults" />

<c:forEach var="bagQueryResultEntry" items="${lookupResults}">
   <c:set var="matchesCount" value="${bagQueryResultEntry.matches}"/>
   <c:set var="unresolvedCount" value="${fn:length(bagQueryResultEntry.unresolved)}"/>
   <c:set var="duplicateCount" value="${fn:length(bagQueryResultEntry.duplicates)}"/>
   <c:set var="translatedCount" value="${fn:length(bagQueryResultEntry.translated)}"/>
   <c:set var="lowQualityCount" value="${fn:length(bagQueryResultEntry.lowQuality)}"/>
   <c:set var="wildcardCount" value="${fn:length(bagQueryResultEntry.wildcards)}"/>

   <c:if test="${((matchesCount > 0 &&  PORTAL_QUERY_FLAG == 'true') 
   || unresolvedCount > 0 || duplicateCount > 0 || translatedCount > 0 || lowQualityCount > 0 || wildcardCount > 0) && lookupReportOpen != 'true'}">
     <div class="lookupReport">
     <c:set var="lookupReportOpen" value="true" />
   </c:if>

   <%-- Only display if coming from the portal --%>
   <c:if test="${matchesCount > 0 &&  PORTAL_QUERY_FLAG == 'true'}">
   <c:choose>
     <c:when test="${resultsTable.size == 0}">
        <c:if test="${matchesCount > 0}">
          <div class="lookupError">
            <c:choose>      
              <c:when test="${matchesCount == 1}">
                <fmt:message key="results.lookup.noresults.one">
                  <fmt:param value="${bagQueryResultEntry.matches}"/>
                  <fmt:param value="${bagQueryResultEntry.type}"/>
                </fmt:message>
              </c:when>
              <c:when test="${matchesCount > 1}">
                <fmt:message key="results.lookup.noresults.many">
                  <fmt:param value="${bagQueryResultEntry.matches}"/>
                  <fmt:param value="${bagQueryResultEntry.type}"/>
                </fmt:message>
              </c:when>
            </c:choose>
          </div>
        </c:if>
      </c:when>
      <c:otherwise>
        <div class="lookupWarn">
         <c:choose>
         <c:when test="${!empty messageDisplayer}">
           <jsp:include page="${messageDisplayer}" />
         </c:when>
         <c:otherwise>
         <fmt:message key="results.lookup.matches.many">
            <fmt:param value="${bagQueryResultEntry.matches}"/>
         </fmt:message>
         </c:otherwise>
         </c:choose>
        </div>
      </c:otherwise>
    </c:choose>
    <c:remove var="PORTAL_QUERY_FLAG"/>
    </c:if>
    
      <c:if test="${unresolvedCount > 0}">
        <div class="lookupError">
          <c:choose>
            <c:when test="${unresolvedCount == 1}">
              <fmt:message key="results.lookup.unresolved.one">
                <fmt:param value="${bagQueryResultEntry.type}"/>
              </fmt:message>
            </c:when>
            <c:when test="${unresolvedCount > 1}">
              <fmt:message key="results.lookup.unresolved.many">
                <fmt:param value="${bagQueryResultEntry.type}"/>
              </fmt:message>
            </c:when>
          </c:choose>
          <c:forEach var="identifier" items="${bagQueryResultEntry.unresolved}" varStatus="status">
            <c:if test="${status.index != 0}">,</c:if>
            <c:out value="${identifier}"/>
          </c:forEach>
          <c:if test="${bagQueryResultEntry.hasExtraConstraint}">
            <fmt:message key="results.lookup.in"/>
            <c:out value="${bagQueryResultEntry.extraConstraint}"/>
          </c:if>
        </div>
      </c:if>

      <c:if test="${duplicateCount > 0}">
        <div class="lookupWarn">
          <c:choose>
            <c:when test="${duplicateCount == 1}">
              <fmt:message key="results.lookup.duplicate.one"/>:
            </c:when>
            <c:when test="${duplicateCount > 1}">
              <fmt:message key="results.lookup.duplicate.many"/>:
            </c:when>
          </c:choose>  
          <c:forEach var="identifier" items="${bagQueryResultEntry.duplicates}" varStatus="status">
            <c:if test="${status.index != 0}">,</c:if>
            <c:out value="${identifier}"/>
          </c:forEach>
        </div>
      </c:if>

  <c:if test="${translatedCount > 0}">
    <div class="lookupWarn">
      <c:choose>
        <c:when test="${translatedCount == 1}">
          <fmt:message key="results.lookup.translated.one">
            <fmt:param value="${bagQueryResultEntry.type}"/>
          </fmt:message>
        </c:when>
        <c:when test="${translatedCount > 1}">
          <fmt:message key="results.lookup.translated.many">
            <fmt:param value="${bagQueryResultEntry.type}"/>
          </fmt:message>
        </c:when>
      </c:choose>         
      <c:forEach var="identifier" items="${bagQueryResultEntry.translated}" varStatus="status">
        <c:if test="${status.index != 0}">,</c:if>
        <c:out value="${identifier}"/>
      </c:forEach>
    </div>
  </c:if>


  <c:if test="${lowQualityCount > 0}">
    <div class="lookupWarn">
      <c:choose>
        <c:when test="${lowQualityCount == 1}">
          <fmt:message key="results.lookup.lowQuality.one"/>:
        </c:when>
        <c:when test="${lowQualityCount > 1}">
          <fmt:message key="results.lookup.lowQuality.many"/>:
        </c:when>
      </c:choose>         
      <c:forEach var="identifier" items="${bagQueryResultEntry.lowQuality}" varStatus="status">
        <c:if test="${status.index != 0}">,</c:if>
        <c:out value="${identifier}"/>
      </c:forEach>
    </div>
  </c:if>
  
  <c:if test="${wildcardCount > 0}">
    <div class="lookupWarn">
      <c:choose>
        <c:when test="${wildcardCount == 1}">
          <c:forEach var="wildcard" items="${bagQueryResultEntry.wildcards}" varStatus="status">
            <c:choose>
              <c:when test="${fn:length(wildcard.value) == 1}">
                <fmt:message key="results.lookup.wildcard.oneone"/>
                <c:out value="${wildcard.key}"/>
              </c:when>
              <c:otherwise>
                <fmt:message key="results.lookup.wildcard.one"/>:
                <c:out value="${wildcard.key}"/> (<c:out value="${fn:length(wildcard.value)}"/>)
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </c:when>
        <c:when test="${wildcardCount > 1}">
          <fmt:message key="results.lookup.wildcard.many"/>:
          <c:forEach var="wildcard" items="${bagQueryResultEntry.wildcards}" varStatus="status">
            <c:if test="${status.index != 0}">,</c:if>
            <c:out value="${wildcard.key}"/> (<c:out value="${fn:length(wildcard.value)}"/>)
          </c:forEach>
        </c:when>
      </c:choose>         
    </div>
  </c:if>
</c:forEach>
<c:if test="${lookupReportOpen == 'true'}">
  </div>
</c:if>
<%-- /bagRunnerMsg.jsp --%>