<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- bagUploadConfirm.jsp -->
<html:xhtml/>
<h2> <fmt:message key="bagUploadConfirm.description"/> </h2>
<div>
  <html:form action="/bagUploadConfirm" focus="text" method="post" enctype="multipart/form-data">
    <p>
    Found ${matchesCount} matches of type ${bagType}. 
    </p>
    <input type="hidden" name="matchIDs" value="${matchesString}"/>
    <input type="hidden" name="bagType" value="${bagType}"/>
    <fmt:message key="bagUploadConfirm.bagName"/>: 
    <input type="text" name="bagName" value="${bagName}" size="20"/>
    <html:submit property="action">
      <fmt:message key="bagUploadConfirm.submitOK"/>
    </html:submit>
  </html:form>
</div>
<!-- /bagUploadConfirm.jsp -->
