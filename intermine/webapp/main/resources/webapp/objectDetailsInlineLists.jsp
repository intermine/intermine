<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<tiles:importAttribute name="object" />
<tiles:importAttribute name="isThisHeader" />

<c:if test="${!empty object.inlineLists}">
  <c:choose>
    <c:when test="${isThisHeader}">
      <c:forEach items="${object.inlineLists}" var="list" varStatus="status">
        <c:if test="${list.size > 0 && isThisHeader == list.showInHeader}">
          <div class="box grid_12">
            ${list.prefix}:
            <c:choose>
              <c:when test="${list.showLinksToObjects}">
                <c:forEach items="${list.items}" var="item" varStatus="status">
                  <a href="<c:out value="${WEB_PROPERTIES['path']}" />objectDetails.do?id=${item.id}" target="new"
                  title="Show '${item.value}' detail">${item.value}</a><c:if test="${status.count < list.size}">, </c:if>
                </c:forEach>
              </c:when>
              <c:otherwise>
                <c:forEach items="${list.items}" var="item" varStatus="status">
                  ${item.value}<c:if test="${status.count < list.size}">, </c:if>
                </c:forEach>
              </c:otherwise>
            </c:choose>
          </div>
          <div style="clear:both;">&nbsp;</div>
        </c:if>
      </c:forEach>
    </c:when>
    <c:otherwise>
      <c:forEach items="${object.inlineLists}" var="list" varStatus="status">
        <c:if test="${list.size > 0 && isThisHeader == list.showInHeader}">
          <div class="box grid_12">
            <h3>${list.size} ${list.prefix}</h3>
            <c:choose>
              <c:when test="${list.showLinksToObjects}">
                <c:forEach items="${list.items}" var="item" varStatus="status">
                  <a href="<c:out value="${WEB_PROPERTIES['path']}" />objectDetails.do?id=${item.id}" target="new"
                  title="Show '${item.value}' detail">${item.value}</a><c:if test="${status.count < list.size}">, </c:if>
                </c:forEach>
              </c:when>
              <c:otherwise>
                <c:forEach items="${list.items}" var="item" varStatus="status">
                  ${item.value}<c:if test="${status.count < list.size}">, </c:if>
                </c:forEach>
              </c:otherwise>
            </c:choose>
          </div>
          <div style="clear:both;">&nbsp;</div>
        </c:if>
      </c:forEach>
    </c:otherwise>
  </c:choose>
</c:if>