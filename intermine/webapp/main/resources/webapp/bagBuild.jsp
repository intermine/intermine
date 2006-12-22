<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- bagBuild.jsp -->
<html:xhtml/>
<h2>Create a new bag</h2>
<div class="bagBuild">
  <html:form action="/buildBag" focus="text" method="post" enctype="multipart/form-data">
    <p>
      <fmt:message key="bagBuild.bagFromText1"/>
      <br/>
      <fmt:message key="bagBuild.bagFromText2"/>
      <br/>
      <p>
      <html:select property="type">
      	<c:forEach items="${typeList}" var="type">
           <html:option value="${type}">${type}</html:option>
      	</c:forEach>
      </html:select>
      </p>
      <br/>
      <html:textarea property="text" rows="10" cols="40"/>
      <br/>
      <html:reset>
        <fmt:message key="bagBuild.reset"/>
      </html:reset>
    </p>
    <p>
      <fmt:message key="bagBuild.or"/>
    </p>
    <p>
      <fmt:message key="bagBuild.bagFromFile"/>:
      <br/>
      <html:file property="formFile"/>
    </p>
    <p>
      <fmt:message key="bagBuild.bagNamePrompt"/>: <html:text property="bagName"/>
    </p>
    <p>
      <fmt:message key="bagBuild.extraHelpText1"/>
      <br/>
      <fmt:message key="bagBuild.extraHelpText2"/>
    </p>
    <p>
      <html:submit property="action">
        <fmt:message key="bagBuild.makeStringBag"/>
      </html:submit>
    </p>
  </html:form>
</div>
<!-- /bagBuild.jsp -->
