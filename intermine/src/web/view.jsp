<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute/>

<!-- view.jsp -->
  <div class="heading">
    <fmt:message key="view.notEmpty.description"/><im:helplink key="view.help.output"/>
  </div>
  <c:choose>
  <c:when test="${empty QUERY.view}">
    <div class="body">
      <fmt:message key="view.empty.description"/>
    </div>
  </c:when>
  <c:otherwise>
    <div class="view">
      <div class="body">
      <c:if test="${fn:length(QUERY.view) > 1}">
        <div>
          <fmt:message key="view.columnOrderingTip"/>
        </div>
      </c:if>
      <br/>
      
      <div>
        <c:forEach var="path" items="${QUERY.view}" varStatus="status">
          <div class="viewpath" id="showing${fn:replace(path,".","")}"
                          onMouseOver="enterPath('${fn:replace(path,".","")}')"
                          onMouseOut="exitPath('${fn:replace(path,".","")}')">
            <div>
              <nobr>
                <c:if test="${not fn:contains(path, '.')}">
                  ${path}
                </c:if>
                <c:if test="${fn:contains(path, '.')}">
                  ${fn:substringBefore(path, ".")}.${fn:substringAfter(path, ".")}
                </c:if>
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
    </div>
    <div class="heading">
      <fmt:message key="view.actions"/><im:helplink key="view.help.actions"/>
    </div>
    <div class="body">
      <div style="width: 100%"> <%-- IE table width bug workaround --%>
      <table width="100%">
        <tr>
          <td align="left">
            <tiles:get name="saveQuery"/>
          </td>
          <c:if test="${!empty PROFILE.username}">
            <td align="right">
              <tiles:get name="createTemplate"/>
            </td>
          </c:if>
        </tr>
      </table>
      </div>
    </div>
  </c:otherwise>
</c:choose>
<!-- /view.jsp -->
