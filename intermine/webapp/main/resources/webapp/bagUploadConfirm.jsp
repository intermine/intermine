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
    Found ${matchCount} matches of type ${bagType}.
    </p>
    <html:hidden property="matchIDs"/>
    <html:hidden property="bagType"/>
    <fmt:message key="bagUploadConfirm.bagName"/>: 
    <html:text property="bagName" size="20"/>
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

  <html:submit property="action">
    <fmt:message key="bagUploadConfirm.submitOK"/>
  </html:submit>

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
