<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<c:if test="${cld != null}">
  <html:form action="/query">

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
            <html:text property="fieldValue(${constraint.key})"/>
          </td>
          <td>
            <html:link action="/changequery?method=removeConstraint&constraintName=${constraint.key}">
            <bean:message key="queryclass.removeConstraint"/>
            </html:link>
          </td>
        </tr>
      </c:forEach>
    </table>

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

  
