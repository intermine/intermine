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
  <table cellspacing="0">
    <tr>
      <c:forEach var="path" items="${view}" varStatus="status">
        <td>
          <c:out value="${path}"/>
          <c:if test="${!status.first}">
            <html:link action="/viewChange?method=moveLeft&index=${status.index}">&lt; </html:link>
          </c:if>
          <c:if test="${!status.last}">
            <html:link action="/viewChange?method=moveRight&index=${status.index}">&gt;</html:link>
          </c:if>
          <html:link action="/viewChange?method=removeFromView&path=${path}">x</html:link>
        </td>
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

  <hr/>
  <c:if test="${QUERY != null}">
    <c:if test="${RESULTS_TABLE == null}">

<%--     
Need to improved the estimates:
 <tiles:get name="queryStatistics"/>
--%>

    </c:if>
    <div>
      <html:link action="/viewChange?method=runQuery">
        <fmt:message key="view.showresults"/>
      </html:link>
    </div>
  </c:if>
</div>

</c:if>
<!-- /view.jsp -->
