<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<tiles:importAttribute/>

<!-- view.jsp -->
<c:choose>
  <c:when test="${empty QUERY.view}">
    <fmt:message key="view.empty.description"/>
  </c:when>
  <c:otherwise>
    <div class="view">
      <div class="paneTitle">
        <fmt:message key="view.notEmpty.description"/>
      </div>
      <%--      <c:if test="${QUERY.view.size > 1}">--%>  <%-- FIXME with JSTL fn:length --%>
      <div>
        <fmt:message key="view.columnOrderingTip"/>
      </div>
      <%--      </c:if>--%>
      <br/>
      
      <div>
        <c:forEach var="path" items="${QUERY.view}" varStatus="status">
          <div class="viewpath" id="showing${fn:replace(path,".","")}"
                          onMouseOver="enterPath('${fn:replace(path,".","")}')"
                          onMouseOut="exitPath('${fn:replace(path,".","")}')">
            <div>
              <nobr>
                <c:out value="${path}"/>
              </nobr>
            </div>
            <div>
              <nobr>
                <c:if test="${!status.first}">
                  <fmt:message key="view.moveLeftHelp" var="moveLeftTitle">
                    <fmt:param value="${path}"/>
                  </fmt:message>
                  [
                  <html:link action="/viewChange?method=moveLeft&index=${status.index}"
                    title="${moveLeftTitle}">
                    <fmt:message key="view.moveLeftSymbol"/>
                  </html:link>
                  ]
                </c:if>
                <c:if test="${!status.last}">
                  <fmt:message key="view.moveRightHelp" var="moveRightTitle">
                    <fmt:param value="${path}"/>
                  </fmt:message>
                  [
                  <html:link action="/viewChange?method=moveRight&index=${status.index}"
                    title="${moveRightTitle}">
                    <fmt:message key="view.moveRightSymbol"/>
                  </html:link>
                  ]
                </c:if>
                <fmt:message key="view.removeFromViewHelp" var="removeFromViewTitle">
                  <fmt:param value="${path}"/>
                </fmt:message>
                [
                <html:link action="/viewChange?method=removeFromView&path=${path}"
                  title="${removeFromViewTitle}">
                  <fmt:message key="view.removeFromViewSymbol"/>
                </html:link>
                ]
              </nobr>
            </div>
          </div>
        </c:forEach>
      </div>
      <div style="clear:left">
        <br/>
        <html:form action="/viewAction">
          <html:submit property="action">
            <fmt:message key="view.showresults"/>
          </html:submit>
        </html:form>
      </div>
    </div>
    <br/>
    <div>
      <tiles:get name="saveQuery"/>
    </div>
  </c:otherwise>
</c:choose>
<!-- /view.jsp -->
