<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<c:if test="${cld != null}">
  <html:form action="/query">

    <%-- Tell the user which queryClass they're editing --%>
    <c:choose>
      <c:when test="${!empty aliasStr}">
        <c:set var="startStr" value="Edit"/>
        <c:set var="endStr" value="${aliasStr}"/>
      </c:when>
      <c:otherwise>
        <c:set var="startStr" value="New"/>
        <c:set var="endStr" value=""/>
      </c:otherwise>
    </c:choose>
    <c:out value="${startStr}"/> 
    <font class="queryViewFromItemTitle"><c:out value="${cld.unqualifiedName}"/></font> 
    <font class="queryViewFromItemAlias"><c:out value="${endStr}"/></font>

    <%-- Allow the user to add a new constraint to a field of this queryClass --%>
    <table border="0">
      <tr><td>
      <html:select property="newFieldName">
        <c:forEach var="field" items="${cld.allFieldDescriptors}">
          <html:option value="${field.name}"><c:out value="${field.name}"/></html:option>
        </c:forEach>
      </html:select>
      <html:submit property="action">
        <bean:message key="queryclass.addConstraint"/>
      </html:submit>
      </td></tr>
    </table>

     <%-- Display the current constraints on this queryClass--%>
    <table border="0">
      <c:forEach items="${constraints}" var="constraint">
        <tr>
          <td>
            <c:out value="${constraint.value}"/>
          </td>
          <td>
            <html:select property="fieldOp(${constraint.key})">
              <c:forEach items="${ops[constraint.value]}" var="op">
                <html:option value="${op.key}"><c:out value="${op.value}"/></html:option>
              </c:forEach>
            </html:select>
          </td>
          <td>
            <c:choose>
              <c:when test="${aliases[constraint.value] != null}">
                <html:select property="fieldValue(${constraint.key})">
                  <c:forEach items="${aliases[constraint.value]}" var="value">
                    <html:option value="${value}"><c:out value="${value}"/></html:option>
                  </c:forEach>
                </html:select>
              </c:when>
              <c:otherwise>
                <html:text property="fieldValue(${constraint.key})"/>
              </c:otherwise>
            </c:choose>
          </td>
          <td>
            <html:link action="/changequery?method=removeConstraint&constraintName=${constraint.key}">
            <bean:message key="queryclass.removeConstraint"/>
            </html:link>
          </td>
        </tr>
      </c:forEach>
    </table>

    <%-- Buttons to submit or remove this queryClass --%>  
    <table border="0">
      <tr>
        <td>
        <html:submit property="action">
          <bean:message key="queryclass.add"/>
        </html:submit>
        </td>
        <c:if test="${!empty aliasStr}">
          <td>
            <html:submit property="action">
              <bean:message key="queryclass.remove"/>
            </html:submit>
          </td>
        </c:if>
      </tr>
    </table>    

  </html:form>      
</c:if>

  
