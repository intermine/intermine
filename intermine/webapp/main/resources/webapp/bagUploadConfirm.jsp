<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- bagUploadConfirm.jsp -->
<html:xhtml/>
<html:form action="/bagUploadConfirm" focus="text" method="post" enctype="multipart/form-data">
<html:hidden property="matchIDs" styleId="matchIDs"/>
<html:hidden property="bagType"/>
<script type="text/javascript" src="js/baguploadconfirm.js"></script>
  <div class="body" align="center">
    <div id="uploadConfirmMessage">
      <strong><span id="matchCount">${matchCount}</span> ${bagUploadConfirmForm.bagType}(s)</strong> currently in your bag.<br/>
	  <strong><span id="duplicateCount">${fn:length(issues)}</span> duplicate(s)</strong> and <strong>${fn:length(unresolved)} unresolved</strong> identifier(s).
    </div>
    <div class="blueBg">
       <p>Only <strong><span id="matchCount">${matchCount}</span></strong> of the <strong>${fn:length(issues) + matchCount + fn:length(unresolved)}</strong> identifier(s) you provided will be saved in your bag.</p>        
       <c:if test="${matchCount > 0}">
          <fmt:message key="bagUploadConfirm.bagName"/>:
          <html:text property="bagName" size="20"/>
          <html:submit property="submit">
            <fmt:message key="bagUploadConfirm.submitOK"/>
          </html:submit>
        </c:if>
    </div>
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

  <div class="body">
    <html:submit property="goBack">
      <fmt:message key="bagUploadConfirm.goBack"/>
    </html:submit>
  </div>

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
