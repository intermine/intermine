<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- showProperties.jsp -->
<html:xhtml/>

<div class="body">
    <c:choose>
        <c:when test="${IS_SUPERUSER}">
            <div class="collection-table">

                <div class="add-property">
                    <h3><fmt:message key="edit-properties.add"/></h3>
                    <html:form action="/editProperty">
                        <label>
                            Key:
                            <html:text property="propertyName" size="100"/>
                        </label>
                        <label>
                            Value:
                            <html:text property="propertyValue" size="100"/>
                        </label>
                        <html:submit titleKey="edit-properties.submit"/>
                    </html:form>
                </div>
                
                <h3><fmt:message key="edit-properties.title"/></h3>
                <table>
                    <thead>
                        <tr>
                            <th>Key</th>
                            <th>Origins</th>
                            <th>Value</th>
                        </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="entry" items="${PROPERTIES_ORIGINS}">
                        <tr>
                            <td><c:out value="${entry.key}"/></td>
                            <td>
                                <ul class="property-origins">
                                    <c:forEach var="origin" items="${entry.value}">
                                        <li><c:out value="${origin}"/></li>
                                    </c:forEach>
                                </ul>
                            </td>
                            <td>
                                <span class="current-value">
                                    <code><c:out value="${WEB_PROPERTIES[entry.key]}"/></code>
                                </span>
                                <div class="edit-property">
                                    <html:form action="/editProperty">
                                        <label>
                                            <fmt:message key="edit-properties.label"/>
                                            <html:text property="propertyValue" value="${WEB_PROPERTIES[entry.value]}" size="100"/>
                                        </label>
                                        <html:hidden property="propertyName" value="${entry.key}"/>
                                    </html:form>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <h3>Shhh! Super secret stuff...</h3>
        </c:otherwise>
    </c:choose>
</div>

<!-- /showProperty -->
