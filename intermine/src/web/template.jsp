<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<html:form action="/templateAction">
  <html:hidden property="queryName" value="${queryName}"/>
  <c:out value="${templateQuery.indexedDescription}"/><br/><br/>
  <table>
    <c:forEach items="${nodes}" var="node" varStatus="status">
      <tr>
        <td align="right">
          <c:out value="[${status.count}]"/>
        </td>
        <td/>
        <td>
          <c:out value="${names[node]}"/>
        </td>
        <td>
          <html:select property="attributeOps(${status.count})">
            <c:forEach items="${ops[node]}" var="op">
              <html:option value="${op.key}">
                <c:out value="${op.value}"/>
              </html:option>
            </c:forEach>
          </html:select>
        </td>
        <td>
          <html:text property="attributeValues(${status.count})"/>
        </td>
      </tr>
    </c:forEach>
  </table>
  <br/>
  <html:submit property="action"/>
</html:form>