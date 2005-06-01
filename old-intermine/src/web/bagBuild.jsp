<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- bagBuild.jsp -->
<html:xhtml/>

<div class="body bagBuild">
  <html:form action="/buildBag" focus="text" method="post" enctype="multipart/form-data">
    <fmt:message key="bagBuild.bagFromText1"/>
    <br/>
    <fmt:message key="bagBuild.bagFromText2"/>
    <br/>
    <html:textarea property="text" rows="20" cols="80"/>
    <br/>
    <html:reset>
      <fmt:message key="bagBuild.reset"/>
    </html:reset>
    <br/>
    <fmt:message key="bagBuild.or"/>
    <br/>
    <fmt:message key="bagBuild.bagFromFile"/>:
    <br/>
    <html:file property="formFile"/>
    <br/>
    <br/>
    <fmt:message key="bagBuild.bagNamePrompt"/>: <html:text property="bagName"/>
    <br/>
    <fmt:message key="bagBuild.extraHelpText1"/>
    <br/>
    <fmt:message key="bagBuild.extraHelpText2"/>
    <br/>
    <html:submit property="action">
      <fmt:message key="bagBuild.makeStringBag"/>
    </html:submit>
  </html:form>
</div>
<!-- /bagBuild.jsp -->
