<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- queryClassSelect -->
<html:form action="/queryClassSelect">
  <font size="-1"><fmt:message key="query.addclass"/></font>

  <br/>

  <html:select property="className" size="10">
    <html:options name="model" property="classNames" labelName="model" labelProperty="unqualifiedClassNames"/>
  </html:select>

  <br/>

  <html:submit property="action">
    <fmt:message key="button.select"/>
  </html:submit>

  <html:submit property="action">
    <fmt:message key="button.browse"/>
  </html:submit>
</html:form>
<!-- /queryClassSelect -->
