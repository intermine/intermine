<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<tiles:importAttribute scope="request"/>

<!-- resultsCell.jsp -->
<c:choose>
  <%-- check whether we have a business object or a plain java object --%>
  <c:when test="${!empty clds}">
    <table>
      <tr>
        <html:link action="/changeResults?method=details&rowIndex=${rowIndex}&columnIndex=${columnIndex}">
          <font class="resultsCellTitle">
            <c:forEach var="cld" items="${leafClds}">
              <c:out value="${cld.unqualifiedName}"/>
            </c:forEach>
          </font>
        </html:link>
        <c:forEach var="cld" items="${leafClds}">
          <td>
            <table>
              <tr>
                <td>
                  [
                </td>
                <c:choose>
                  <%-- check whether any displayers are registered for this type --%>
                  <c:when test="${!empty webconfig.types[cld.name].shortDisplayers}">
                    <c:forEach items="${webconfig.types[cld.name].shortDisplayers}" var="displayer">
                      <td valign="top">
                        <c:set var="cld" value="${cld}" scope="request"/>
                        <tiles:insert beanName="displayer" beanProperty="src"/>
                      </td>
                    </c:forEach>
                  </c:when>
                  <c:otherwise>
                    <td valign="top">
                      <c:set var="cld" value="${cld}" scope="request"/>
                      <tiles:insert name="/allFields.jsp"/>
                    </td>
                  </c:otherwise>
                </c:choose>
                <td>
                  ]
                </td>
              </tr>
            </table>
          </td>
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
