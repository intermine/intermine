<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- viewLabels.jsp -->
<html:xhtml/>

<c:set var="im_model" value="${INTERMINE_API.model}"/>

<h2><fmt:message key="viewlabels.heading"/></h2>

<select id="classNameSelector">
    <c:forEach var="cd" items="${im_model.classDescriptors}">
        <c:set var="classname" value="${cd.unqualifiedName}"/>
        <option value="${classname}">
            <c:out value="${imf:formatPathStr(classname, INTERMINE_API, WEBCONFIG)}"/>
        </option>
    </c:forEach>
</select>

<c:forEach var="cd" items="${im_model.classDescriptors}" varStatus="cdStatus">
  <c:set var="classname" value="${cd.unqualifiedName}" />
  <c:choose>
    <c:when test="${cdStatus.first}">
        <c:set var="css" value=""/>
    </c:when>
    <c:otherwise>
        <c:set var="css" value="display: none;"/>
    </c:otherwise>
  </c:choose>

  <div class="collection-table classLabels" id="labelsFor${classname}" style="${css}">
      <h3>${classname} &rarr; ${imf:formatPathStr(classname, INTERMINE_API, WEBCONFIG)}</h3>
      <table>
      <tbody>
          <c:choose>
            <c:when test="${empty cd.fieldDescriptors}">
              <td><fmt:message key="viewlabels.nofields"/></td>
            </c:when>
      <c:otherwise>
        <c:forEach var="fd" items="${cd.fieldDescriptors}">
                <tr>
                    <td><c:out value="${fd.name}"/></td>
                    <c:set var="pathStr" value="${classname}.${fd.name}"/>
                    <td><c:out value="${imf:formatFieldStr(pathStr, INTERMINE_API, WEBCONFIG)}"/></td>
                </tr>
                </c:forEach>
        </c:otherwise>
          </c:choose>
      </tbody>
      </table>
  </div>
</c:forEach>

<html:link action="/labelListing"><fmt:message key="viewlabels.csv"/></html:link>

<!-- /viewLabels.jsp -->



