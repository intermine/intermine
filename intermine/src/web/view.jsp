<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute/>

<!-- view.jsp -->
<c:if test="${!empty view}">
  <br/>
  Current view:<br/>
  <br/>
  <table class="results" cellspacing="0">
    <tr>
      <c:forEach var="path" items="${view}" varStatus="status">
        <th>
          <c:out value="${path}"/>
          <c:if test="${!status.first}">
            [<html:link action="/viewChange?method=moveLeft&index=${status.index}">&lt; </html:link>]
          </c:if>
          <c:if test="${!status.last}">
            [<html:link action="/viewChange?method=moveRight&index=${status.index}">&gt;</html:link>]
          </c:if>
          [<html:link action="/viewChange?method=removeFromView&path=${path}">x</html:link>]
        </th>
      </c:forEach>
    </tr>
  </table>

  <%--     
    <hr/>
    <div>
      <html:link action="/classChooser">
        (Broken) Add a class to the query...
      </html:link>
    </div>
    --%>
  
  <c:if test="${QUERY != null}">
    <br>
      <%--     
        Need to improved the estimates:
        <c:if test="${RESULTS_TABLE == null}">
          <tiles:get name="queryStatistics"/>
        </c:if>
        --%>
      <div>
        <html:link action="/viewChange?method=runQuery">
          <fmt:message key="view.showresults"/>
        </html:link>
      </div>
      <div>
        <html:link action="/viewChange?method=export">
          <fmt:message key="results.export"/>
        </html:link>
      </div>
    </c:if>
    <tiles:get name="saveQuery"/>
  </c:if>
</c:if>
<!-- /view.jsp -->
