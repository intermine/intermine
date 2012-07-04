<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<tiles:importAttribute name="mapOfInlineLists" ignore="false" />
<tiles:importAttribute name="placement" />

<c:forEach items="${mapOfInlineLists}" var="lists">
  <c:if test="${lists.key == placement}">
    <c:forEach items="${lists.value}" var="list" varStatus="status">
      <c:set var="pathString" value="${reportObject.classDescriptor.unqualifiedName}.${list.prefix}"/>
      <c:set var="listDisplayName" value="${imf:formatFieldStr(pathString, INTERMINE_API, WEBCONFIG)}"/>

      <div class="inline-list<c:if test="${list.size == 0}"> gray</c:if>">
        <a name="${list.prefix}" class="anchor"></a>
        <h3>
          <c:if test="${IS_SUPERUSER}">
            <div class="right">
              <c:set var="descriptor" value="${list.descriptor}" />
              <tiles:insert name="inlineTagEditor.tile">
                <tiles:put name="taggable" beanName="descriptor" />
                <tiles:put name="show" value="true" />
              </tiles:insert>
            </div>
          </c:if>
          ${list.size}&nbsp;${listDisplayName}
        </h3>
        <c:if test="${list.size > 0}">
          <ul>
            <c:choose>
              <c:when test="${list.showLinksToObjects}">
                <c:forEach items="${list.items}" var="item" varStatus="status">
                  <li><a href="<c:out value="${WEB_PROPERTIES['path']}" />report.do?id=${item.id}"
                  title="Show '${item.value}' detail">${item.value}</a><c:if test="${status.count < list.size}">, </c:if></li>
                </c:forEach>
              </c:when>
              <c:otherwise>
                <c:forEach items="${list.items}" var="item" varStatus="status">
                  <li>${item.value}<c:if test="${status.count < list.size}">, </c:if></li>
                </c:forEach>
              </c:otherwise>
            </c:choose>
          </ul>
        </c:if>
      </div>
    </c:forEach>
  </c:if>
</c:forEach>
