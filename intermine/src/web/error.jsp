<%@ page import="java.util.*" %>
<%@ page import="java.io.*" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<h2><bean:message key="error.title"/></h2>

<html:messages id="error">
  <c:out value="${error}"/><br/>
</html:messages>

<h2><bean:message key="error.stacktrace"/></h2>
<c:out value="${stacktrace}"/><br/>
