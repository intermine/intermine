<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%-- <%@ include file="session.jsp" %> --%>

<html:form action="/query">
    <html:select property="cldName">
        <html:options name="model" property="classNames" labelName="model" labelProperty="classNames"/>
    </html:select>

    <html:submit property="action">
        <bean:message key="button.select"/>
    </html:submit>

    <br/>


    <logic:present scope="session" name="cld">
       <table border="0">        
        <c:forEach var="field" items="${cld.fieldDescriptors}">
            <tr><td><c:out value="${field.name}"/></td>
            <td><html:text property="fieldValue(field.name)"/></td></tr>
        </c:forEach>
        </table>
    </logic:present>
</html:form>
