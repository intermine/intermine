<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- users.jsp -->
<html:xhtml/>
&nbsp;
<c:choose>
<c:when test="${IS_SUPERUSER}">
<link rel="stylesheet" type="text/css" href="css/sorting.css"/>
<h2><fmt:message key="users.heading"/></h2>
<p class="apikey"><fmt:message key="users.description"/></p>
<html:form action="/modifySuperUser">
      <table class="bag-table sortable-onload-2 rowstyle-alt colstyle-alt no-arrow">
        <thead>
            <tr>
              <th align="left" nowrap class="sortable"><fmt:message key="users.namecolumnheader"/></th>
              <th align="left" nowrap class="sortable"><fmt:message key="users.superusercolumnheader"/></th>
            </tr>
          </thead>
          <tbody>
          <c:forEach items="${users}" var="user" varStatus="status">
            <tr>
              <td class="sorting">${user}</td>
              <td align="center">
                <html:multibox property="superUsers">
                  <c:out value="${user}"/>
                </html:multibox>
              </td>
            </tr>
            </c:forEach>
           </tbody>
        </table>
        <input type="submit" value="Save"/>
        <input type="reset" value="Reset"/>
        </html:form>
</c:when>
<c:otherwise>
   <h3>Shhh! Super secret stuff...</h3>
</c:otherwise>
</c:choose>

<!-- /users.jsp -->
