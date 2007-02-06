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
      <strong>
        <span id="matchCount">${matchCount}</span> ${bagUploadConfirmForm.bagType}(s)
      </strong>
      currently in your bag.<br/>
      Also found&nbsp;
      <c:if test="${fn:length(duplicates)>0}">
        <strong><span id="duplicateCount">${fn:length(duplicates)}</span> duplicate(s)</strong>
      </c:if>
      <c:if test="${fn:length(lowQualityMatches)>0}">
        ,<strong>
          <span id="lowQCount">${fn:length(lowQualityMatches)}</span>
          low quality matches
        </strong>
      </c:if>
      <c:if test="${fn:length(convertedObjects)>0}">
        ,<strong>
          <span id="convertedCount">${fn:length(convertedObjects)}</span>
          objects found by converting types
        </strong>
      </c:if>
      <c:if test="${fn:length(unresolved)>0}">
        ,<strong>${fn:length(unresolved)} unresolved</strong> identifier(s).
      </c:if>
    </div>
    <div class="blueBg">
      <c:set var="totalObjects" value="${fn:length(issues) + matchCount + fn:length(unresolved)}"/>
      <c:choose>
        <c:when test="${matchCount < totalObjects}">
          <p>
            Only <strong><span id="matchCount">${matchCount}</span></strong>
            of the <strong>${totalObjects}</strong>
            identifier(s) you provided will be saved in your bag.
          </p>
        </c:when>
        <c:otherwise>
          Matches found for all identifiers
        </c:otherwise>
      </c:choose>
      <fmt:message key="bagUploadConfirm.bagName"/>:
      <html:text property="bagName" size="20"/>
      <html:submit property="submit">
        <fmt:message key="bagUploadConfirm.submitOK"/>
      </html:submit>
    </div>
  </div>
  <c:if test="${!empty duplicates || ! empty lowQualityMatches}">
    <div class="heading">
      <fmt:message key="bagUploadConfirm.issues"/>
    </div>
    <div class="body">
    <c:if test="${! empty lowQualityMatches}">
    <p><fmt:message key="bagUploadConfirm.lowQ"  />
        <c:set var="issueMap" value="${lowQualityMatches}"/>
        <tiles:insert name="bagUploadConfirmIssue.tile">
          <tiles:put name="message" value="${message}"/>
          <tiles:put name="issueMap" beanName="issueMap"/>
          <tiles:put name="issueType" value="lowQ"/>
        </tiles:insert>
    </p>
    </c:if>
    <c:if test="${! empty duplicates}">
    <p><fmt:message key="bagUploadConfirm.duplicatesHeader"  />
        <c:set var="issueMap" value="${duplicates}"/>
        <tiles:insert name="bagUploadConfirmIssue.tile">
          <tiles:put name="message" value="${message}"/>
          <tiles:put name="issueMap" beanName="issueMap"/>
          <tiles:put name="issueType" value="duplicate"/>
        </tiles:insert>
    </p>
    </c:if>
    </div>
  </c:if>

<c:if test="${!empty convertedObjects}">
  <div class="heading">
    <fmt:message key="bagUploadConfirm.convertedHeader"/>
  </div>
  <div class="body">
  <p><fmt:message key="bagUploadConfirm.converted">
        <fmt:param value="${bagUploadConfirmForm.bagType}"/>
      </fmt:message>
      <c:set var="issueMap" value="${convertedObjects}"/>
      <tiles:insert name="bagUploadConfirmIssue.tile">
        <tiles:put name="message" value="${message}"/>
        <tiles:put name="issueMap" beanName="issueMap"/>
        <tiles:put name="issueType" value="converted"/>
      </tiles:insert>
  </p>
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
        <c:forEach items="${unresolved}" var="unresolvedIdentifer">${unresolvedIdentifer.key} </c:forEach>
      </p>
    </div>
    <div class="body">
      <html:submit property="goBack">
        <fmt:message key="bagUploadConfirm.goBack"/>
      </html:submit>
    </div>
  </c:if>
</html:form>
<!-- /bagUploadConfirm.jsp -->
