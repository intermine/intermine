<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<!-- queryAliasChange.jsp -->
<c:if test="${query != null}">
    <font size="-1"><bean:message key="query.modifyclass"/></font>
    <br/>
    <table>
        <c:forEach var="fromElement" items="${query.from}">
            <tr>
                <td align="top">
                    <font class="queryViewFromItemTitle">
                        <c:out value="${perFromTitle[fromElement]}"/>
                    </font>
                </td>
                <td align="top">
                    <font class="queryViewFromItemAlias">
                        "<c:out value="${perFromAlias[fromElement]}"/>"
                    </font>
                </td>
                <td align="top">
                    <c:forEach var="pc" items="${perFromConstraints[fromElement]}">
                        <font class="queryViewConstraintLeft">
                            <c:out value="${pc.left}"/>
                        </font> 
                        <font class="queryViewConstraintOp">
                            <c:out value="${pc.op}"/>
                        </font> 
                        <font class="queryViewConstraintRight">
                            <c:out value="${pc.right}"/>
                        </font>
                        <br/>
                    </c:forEach>
                </td>
                <td align="top">
                    <html:link action="/changealias.do?method=remove&alias=${perFromAlias[fromElement]}">
                        [<bean:message key="button.remove"/>]
                    </html:link>
                </td>
                <td align="top">
                    <html:link action="/changealias.do?method=edit&alias=${perFromAlias[fromElement]}">
                        [<bean:message key="button.edit"/>]
                    </html:link>
                </td>
            </tr>
        </c:forEach>
    </table>
</c:if>
<!-- /queryAliasChange.jsp -->
