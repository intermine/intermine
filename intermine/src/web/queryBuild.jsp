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
    <font class="queryViewFromItemTitle"> <c:out value="${cld.unqualifiedName}"/> </font> 
    <font class="queryViewFromItemAlias"> <c:out value="${endStr}"/></font>              

    <table border="0">        
      <c:forEach var="field" items="${cld.allAttributeDescriptors}">
        <tr>
          <td>
            <c:out value="${field.name}"/>
          </td>
          <td>
            <html:select property="fieldOp(${field.name})">
              <c:forEach items="${ops[field.name]}" var="entry">
                <html:option value="${entry.key}"><c:out value="${entry.value}"/></html:option>
              </c:forEach>
            </html:select>
          </td>
          <td>
            <html:text property="fieldValue(${field.name})"/>
          </td>
        </tr>
        </c:forEach>

        <c:forEach var="field" items="${cld.referenceDescriptors}">
          <tr>
            <td>
              <c:out value="${field.name}"/>
            </td>
            <td>
              <html:select property="fieldOp(${field.name})">
                <c:forEach items="${ops[field.name]}" var="entry">
                  <html:option value="${entry.key}"><c:out value="${entry.value}"/></html:option>
                </c:forEach>
              </html:select>
            </td>
            <td>
              <html:select property="fieldValue(${field.name})">
                <c:forEach items="${aliases[field.name]}" var="alias">
                  <html:option value="${alias}"><c:out value="${alias}"/></html:option>
                </c:forEach>
              </html:select>
            </td>
          </tr>
        </c:forEach>

        <c:forEach var="field" items="${cld.collectionDescriptors}">
          <tr>
            <td>
              <c:out value="${field.name}"/>
            </td>
            <td>
              <html:select property="fieldOp(${field.name})">
                <c:forEach items="${ops[field.name]}" var="entry">
                  <html:option value="${entry.key}"><c:out value="${entry.value}"/></html:option>
                </c:forEach>
              </html:select>
            </td>
            <td>
              <html:select property="fieldValue(${field.name})">
                <c:forEach items="${aliases[field.name]}" var="alias">
                  <html:option value="${alias}"><c:out value="${alias}"/></html:option>
                </c:forEach>
              </html:select>
            </td>
          </tr>
        </c:forEach>
      
            <tr><td>
        <html:submit property="action">
          <bean:message key="button.submit"/>
        </html:submit>
        </td>
        <c:if test="${!empty aliasStr}">
          <td>
            <html:submit property="action">
              <bean:message key="button.remove"/>
            </html:submit>
          </td>
         </c:if>
      </tr>
    </table>    

  </html:form>      
</c:if>

  
