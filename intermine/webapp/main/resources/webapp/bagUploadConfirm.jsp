<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

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
  </div>
  <div class="heading">
    <fmt:message key="bagUploadConfirm.unresolvedDesc"/>
  </div>
  <div class="body">
    <p>
    ${fn:length(unresolved)} identifiers couldn't be found anywhere in the
    database.  Please check that you didn't paste in your shopping list by
    mistake.  The unresolved identifiers were:
    </p>
    <p style="font-weight: bold">
    <c:forEach items="${unresolved}" var="unresolvedIdentifer">${unresolvedIdentifer} </c:forEach>
    </p>
  </div>
</html:form>
<!-- /bagUploadConfirm.jsp -->
