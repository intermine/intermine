<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<!-- reportUnplacedInlineLists.jsp -->

<tiles:importAttribute name="listOfUnplacedInlineLists" ignore="true" />

<c:if test="${!empty listOfUnplacedInlineLists}">
  <c:forEach items="${listOfUnplacedInlineLists}" var="list" varStatus="status">
    <div class='<c:if test="${list.size == 0}">gray</c:if>'>
      <h3>
        <c:if test="${IS_SUPERUSER}">
          <span class="tag-editor">
            <c:set var="descriptor" value="${list.descriptor}" />
            <tiles:insert name="inlineTagEditor.tile">
              <tiles:put name="taggable" beanName="descriptor" />
              <tiles:put name="show" value="true" />
            </tiles:insert>
          </span>
        </c:if>
        ${list.size}&nbsp;${list.prefix}
      </h3>
      <c:if test="${list.size > 0}">
        <c:choose>
          <c:when test="${list.showLinksToObjects}">
            <c:forEach items="${list.items}" var="item" varStatus="status">
              <a href="<c:out value="${WEB_PROPERTIES['path']}" />report.do?id=${item.id}"
                 title="Show '${item.value}' detail">${item.value}</a><c:if test="${status.count < list.size}">, </c:if>
            </c:forEach>
          </c:when>
          <c:otherwise>
            <c:forEach items="${list.items}" var="item" varStatus="status">
              ${item.value}<c:if test="${status.count < list.size}">, </c:if>
            </c:forEach>
          </c:otherwise>
        </c:choose>
    </c:if>
    </div>
    <div class="clear"></div>
  </c:forEach>
</c:if>

<!-- reportUnplacedInlineLists.jsp -->
