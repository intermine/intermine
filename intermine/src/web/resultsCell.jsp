<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%--/**
     * Render a results cell
     */
--%>

<tiles:importAttribute scope="request"/>

<!-- resultsCell.jsp -->
<c:choose>
  <%-- check whether we have a business object or a plain java object --%>
  <c:when test="${clds != null}">
    <table>
      <tr>
        <td align="left">
          <html:link action="/changeResults?method=details&rowIndex=${rowIndex}&columnIndex=${columnIndex}">
            <font class="resultsCellTitle">
              <c:forEach var="cld" items="${leafClds}">
                <c:out value="${cld.unqualifiedName}"/>
              </c:forEach>
            </font>
          </html:link>
        </td>
      </tr>
      <tr>
        <c:forEach var="cld" items="${leafClds}">
          <c:choose>
            <%-- check whether any displayers are registered for this type --%>
            <c:when test="${!empty webconfig.types[cld.name].shortDisplayers}">
              <c:forEach items="${webconfig.types[cld.name].shortDisplayers}" var="displayer">
                <td>
                  <tiles:insert beanName="displayer" beanProperty="src"/>
                </td>
              </c:forEach>
            </c:when>
            <c:otherwise>
              <td>
                <tiles:insert name="/allFields.jsp" />
              </td>
            </c:otherwise>
          </c:choose>
        </c:forEach>
      </tr>
    </table>
  </c:when>
  <c:otherwise>
    <font class="resultsCellValue">
      <c:out value="${object}"/>
    </font>
  </c:otherwise>
</c:choose>
<!-- /resultsCell.jsp -->
