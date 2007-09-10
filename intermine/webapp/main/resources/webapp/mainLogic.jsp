<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- mainLogic.jsp -->

<html:xhtml/>
<div class="heading">
  <fmt:message key="query.constraintLogic"/>
</div>
<div class="body">
  <c:choose>
    <c:when test="${param.editExpression != null}">
      <html:form action="/mainAction">
        <input type="test" name="expr" size="30" value="${QUERY.constraintLogic}"/>
        <html:submit property="expression" style="font-size: 11px">
          <fmt:message key="query.logicUpdate"/>
        </html:submit>
      </html:form>
    </c:when>
    <c:otherwise>
      <c:forEach items="${fn:split(QUERY.constraintLogic, ' ')}" var="item">
        <c:choose>
          <c:when test="${item == 'and' || item == 'or'}">
            <span class="and">${item}</span>
          </c:when>
          <c:otherwise>
            <span class="constraint" style="font-size: 13px;"><b>${item}</b></span>
          </c:otherwise>
        </c:choose>
      </c:forEach>
      <c:choose>
        <c:when test="${fn:length(QUERY.allConstraints) == 1}">
          <div class="smallnote altmessage"><fmt:message key="query.oneConstraint"/></div>
        </c:when>
        <c:when test="${fn:length(QUERY.allConstraints) == 0}">
          <div class="smallnote altmessage"><fmt:message key="query.noConstraints"/></div>
        </c:when>
        <c:otherwise>
          &nbsp;
          <html:link action="/query?editExpression" style="font-size: 11px">
            <fmt:message key="query.logicEdit"/>
          </html:link>
        </c:otherwise>
      </c:choose>
    </c:otherwise>
  </c:choose>
</div>

<!-- /mainLogic.jsp -->
