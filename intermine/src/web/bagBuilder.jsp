<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- bagBuilder.jsp -->
<div class="bagBuilder">
  <html:form action="/buildBag" focus="text" method="POST" enctype="multipart/form-data">
    <fmt:message key="bagBuilder.bagFromText"/>
    <br/>
    <html:textarea property="text" rows="20" cols="80"/>
    <br/>
    <html:reset>
      <fmt:message key="bagBuilder.reset"/>
    </html:reset>
    <br/>
    <fmt:message key="bagBuilder.bagFromFile"/>
    <br/>
    <html:file property="formFile"/>
    <br/>
    <html:submit property="action">
      <fmt:message key="bagBuilder.makeStringBag"/>
    </html:submit>
  </html:form>
</div>
<!-- /iqlQuery.jsp -->
