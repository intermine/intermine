<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<html:form action="/templateAction">
  <c:out value="${templateQuery.description}"/><br/><br/>
  <table border="0" cellspacing="0">
    <c:set var="index" value="${0}"/>
    <c:forEach items="${templateQuery.nodes}" var="node">
      <c:forEach items="${constraints[node]}" var="con">
        <c:set var="index" value="${index+1}"/>
        <c:if test="${!empty con.description}">
          <tr>
            <td align="right" valign="top" rowspan="2">
              <c:out value="[${index}]"/>
            </td>
            <td colspan="3">
              <i><c:out value="${con.description}"/></i>
            </td>
          </tr>
        </c:if>
        <tr>
          <c:if test="${empty con.description}">
            <td align="right">
              <c:out value="[${index}]"/>
            </td>
          </c:if>
          <td>
            <c:out value="${names[con]}"/>
          </td>
          <td>
            <html:select property="attributeOps(${index})">
              <c:forEach items="${ops[con]}" var="op">
                <html:option value="${op.key}">
                  <c:out value="${op.value}"/>
                </html:option>
              </c:forEach>
            </html:select>
          </td>
          <td>
            <html:text property="attributeValues(${index})"/>
          </td>
        </tr>
      </c:forEach>
    </c:forEach>
  </table>
  <c:if test="${empty previewTemplate}">
    <br/>
    <html:submit property="skipBuilder"><fmt:message key="template.submitToResults"/></html:submit>
    <html:submit><fmt:message key="template.submitToQuery"/></html:submit>
  </c:if>
</html:form>