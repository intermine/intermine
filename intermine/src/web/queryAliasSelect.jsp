<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>


<c:if test="${query != null}">
    <html:form action="/queryAliasSelect">
        <font size="-1">Edit class in query</font><br/>
        <html:select property="alias">
            <c:forEach items="${query.aliases}" var="entry">
                <c:set var="alias" scope="page" value="${entry.value}"/>
                <html:option value="<%= (String) pageContext.getAttribute("alias") %>">
                    <c:out value="${alias} ${classNames[entry.key.type.name]}"/>
                </html:option>
            </c:forEach>
        </html:select>

        <html:submit property="action">
            <bean:message key="button.select"/>
        </html:submit>
    </html:form>
</c:if>
