<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>


<c:if test="${query != null}">
    <font size="-1"><bean:message key="query.modifyclass"/></font><br/>
    <table>
        <c:forEach items="${query.aliases}" var="entry">
            <tr>
                <c:set var="alias" scope="page" value="${entry.value}"/>
                <td>
                    <c:out value="${alias}"/>
                </td>
                <td>
                    <c:out value="${classNames[entry.key.type.name]}"/>
                </td>
                <td>
                    <html:link action="/changealias.do?method=remove&alias=${alias}">
[<bean:message key="queryclass.removelink"/>]
                    </html:link>
                </td>
                <td>
                    <html:link action="/changealias.do?method=edit&alias=${alias}">
[<bean:message key="queryclass.editlink"/>]
                    </html:link>
                </td>
            </tr>
        </c:forEach>
    </table>
</c:if>
