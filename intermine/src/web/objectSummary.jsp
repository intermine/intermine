<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<tiles:importAttribute/>

<!-- objectSummary.jsp -->
<c:choose>
  <c:when test="${empty leafClds}">
    <c:out value="${object}"/>
  </c:when>
  <c:otherwise>
    <nobr>
      <html:link action="/objectDetails?id=${object.id}">
        <font class="resultsCellTitle">
          <c:forEach var="cld" items="${leafClds}">
            <c:out value="${cld.unqualifiedName}"/>
          </c:forEach>
        </font>
      </html:link>
    </nobr>
    <br/>
    <table>
      <tr>
        <c:forEach var="cld" items="${leafClds}">
          <td valign="top">
            <c:choose>
              <c:when test="${!empty webconfig.types[cld.name].shortDisplayers}">
                <c:forEach items="${webconfig.types[cld.name].shortDisplayers}" var="displayer">
                  <c:set var="cld" value="${cld}" scope="request"/>
                  <tiles:insert beanName="displayer" beanProperty="src"/>
                </c:forEach>
              </c:when>
              <c:otherwise>
                <c:set var="cld" value="${cld}" scope="request"/>
                <tiles:insert name="/allFields.jsp"/>
              </c:otherwise>
            </c:choose>
          </td>
        </c:forEach>
      </tr>
    </table>
  </c:otherwise>
</c:choose>
<!-- /objectSummary.jsp -->
