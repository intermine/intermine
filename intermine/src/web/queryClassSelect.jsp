<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:form action="/queryClassSelect">
    Add a new class to query<br/>
    <html:select property="className">
        <html:options name="model" property="classNames" labelName="model" labelProperty="unqualifiedClassNames"/>
    </html:select>

    <html:submit property="action">
        <bean:message key="button.select"/>
    </html:submit>
</html:form>
