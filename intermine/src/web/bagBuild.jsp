<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- bagBuild.jsp -->
<div class="body bagBuild">
  <html:form action="/buildBag" focus="text" method="POST" enctype="multipart/form-data">
    <fmt:message key="bagBuild.bagFromText"/>
    <br/>
    <html:textarea property="text" rows="20" cols="80"/>
    <br/>
    <html:reset>
      <fmt:message key="bagBuild.reset"/>
    </html:reset>
    <br/>
    <fmt:message key="bagBuild.or"/>
    <br/>
    <fmt:message key="bagBuild.bagFromFile"/>
    <br/>
    <html:file property="formFile"/>
    <br/>
    <br/>
    <fmt:message key="bagBuild.bagNamePrompt"/>: <html:text property="bagName"/><br/>
    <html:submit property="action">
      <fmt:message key="bagBuild.makeStringBag"/>
    </html:submit>
  </html:form>
</div>
<!-- /bagBuild.jsp -->
