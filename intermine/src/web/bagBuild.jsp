<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- bagBuild.jsp -->
<div class="bagBuild">
  <html:form action="/buildBag" focus="text" method="POST" enctype="multipart/form-data">
    <fmt:message key="bagBuild.bagFromText"/>
    <br/>
    <html:textarea property="text" rows="20" cols="80"/>
    <br/>
    <html:reset>
      <fmt:message key="bagBuild.reset"/>
    </html:reset>
    <br/>
    <fmt:message key="bagBuild.bagFromFile"/>
    <br/>
    <html:file property="formFile"/>
    <br/>
    <fmt:message key="bagBuild.bagNamePrompt"/>: <html:text property="bagName"/><br/>
    <html:submit property="action">
      <fmt:message key="bagBuild.makeStringBag"/>
    </html:submit>
  </html:form>
</div>
<!-- /bagBuild.jsp -->
