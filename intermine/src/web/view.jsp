<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute/>

<!-- view.jsp -->
<c:if test="${empty VIEW}">
  <fmt:message key="view.empty.description"/>
</c:if>
<c:if test="${!empty VIEW}">
  <div class="view">
    <fmt:message key="view.notempty.description"/>
    <br/>
    <table class="results" cellspacing="0">
      <tr>
        <c:forEach var="path" items="${VIEW}" varStatus="status">
          <th>
            <nobr>
              <c:out value="${path}"/>
              <c:if test="${!status.first}">
                [
                <html:link action="/viewChange?method=moveLeft&index=${status.index}">
                  &lt; 
                </html:link>
                ]
              </c:if>
              <c:if test="${!status.last}">
                [
                <html:link
                   action="/viewChange?method=moveRight&index=${status.index}">
                  &gt;
                </html:link>
                ]
              </c:if>
              [
              <html:link action="/viewChange?method=removeFromView&path=${path}">
                x
              </html:link>
              ]
            </nobr>
          </th>
        </c:forEach>
      </tr>
    </table>
  </div>
</c:if>
<c:if test="${!empty VIEW}">
  <br/>
  <tiles:get name="saveQuery"/>
  <div>
    <html:link action="/viewChange?method=runQuery">
      <fmt:message key="view.showresults"/>
    </html:link>
  </div>
</c:if>
<!-- /view.jsp -->
