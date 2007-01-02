<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- bagUploadConfirm.jsp -->
<html:xhtml/>
<html:form action="/bagUploadConfirm" focus="text" method="post" enctype="multipart/form-data">
  <div class="heading">
    <fmt:message key="bagUploadConfirm.matchesDesc"/>
  </div>
  <div class="body">
    <p>
    Found ${fn:length(matches)} matches of type ${bagType}. 
    </p>
    <input type="hidden" name="matchIDs" value="${matchesString}"/>
    <input type="hidden" name="bagType" value="${bagType}"/>
    <fmt:message key="bagUploadConfirm.bagName"/>: 
    <input type="text" name="bagName" value="${bagName}" size="20"/>
    <html:submit property="action">
      <fmt:message key="bagUploadConfirm.submitOK"/>
    </html:submit>
    <c:if test="${fn:length(unresolved) == 0}">
      <p>
        You have no unresolved identifiers.
      </p>
    </c:if>
  </div>
  <c:if test="${!empty issues['DUPLICATE']}">
    <div class="heading">
      <fmt:message key="bagUploadConfirm.duplicates"/>
    </div>
    <div class="body">
      <c:forEach var="entry" items="${issues['DUPLICATE']}">
        <c:set var="message" value="${entry.key}"/>
        <c:set var="duplicates" value="${entry.value}"/>
        <tiles:insert name="bagUploadConfirmDuplicates.tile">
          <tiles:put name="message" value="${message}"/>
          <tiles:put name="duplicates" beanName="duplicates"/>
        </tiles:insert>
      </c:forEach>
    </div>
  </c:if>
  <c:if test="${fn:length(unresolved) > 0}">
    <div class="heading">
      <fmt:message key="bagUploadConfirm.unresolvedDesc"/>
    </div>
    <div class="body">
      <p>
        ${fn:length(unresolved)}  identifiers couldn't be found anywhere in the
        database.  Please check that you didn't paste in your shopping list by
        mistake.  The unresolved identifiers were:
      </p>
      <p style="font-weight: bold">
        <c:forEach items="${unresolved}" var="unresolvedIdentifer">${unresolvedIdentifer} </c:forEach>
      </p>
    </div>
  </c:if>
</html:form>
<!-- /bagUploadConfirm.jsp -->
