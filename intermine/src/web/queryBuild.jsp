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
        <c:forEach var="fieldNum" varStatus="status" items="${fields[field.name]}">
          <tr>
            <td>
              <c:if test="${status.first}">
                <c:out value="${field.name}"/>
              </c:if>
            </td>
            <td>
              <html:select property="fieldOp(${fieldNum})">
                <c:forEach items="${ops[field.name]}" var="entry">
                  <html:option value="${entry.key}"><c:out value="${entry.value}"/></html:option>
                </c:forEach>
              </html:select>
            </td>
            <td>
              <html:text property="fieldValue(${fieldNum})"/>
            </td>
            <td>
              <%-- <c:if test="${status.last}">
                <html:submit property="action" onclick="document.forms[1].addField.value='${field.name}'">
                  <bean:message key="queryclass.addfield"/>
                </html:submit>
              </c:if> --%>
            </td>
          </tr>
        </c:forEach>
      </c:forEach>

        
      <c:forEach var="field" items="${cld.referenceDescriptors}">
        <c:forEach var="fieldNum" varStatus="status" items="${fields[field.name]}">
          <tr>
            <td>
              <c:if test="${status.first}">
                <c:out value="${field.name}"/>
              </c:if>
            </td>
            <td>
              <html:select property="fieldOp(${fieldNum})">
                <c:forEach items="${ops[field.name]}" var="entry">
                  <html:option value="${entry.key}"><c:out value="${entry.value}"/></html:option>
                </c:forEach>
              </html:select>
            </td>
            <td>
              <html:select property="fieldValue(${fieldNum})">
                <c:forEach items="${aliases[field.name]}" var="alias">
                  <html:option value="${alias}"><c:out value="${alias}"/></html:option>
                </c:forEach>
              </html:select>
            </td>
            <td>
              <%-- <c:if test="${status.last}">
                <html:submit property="action" onclick="document.forms[1].addField.value='${field.name}'">
                  <bean:message key="queryclass.addfield"/>
                </html:submit>
              </c:if> --%>
            </td>
          </tr>
        </c:forEach>
      </c:forEach>
    
        
      <c:forEach var="field" items="${cld.collectionDescriptors}">
        <c:forEach var="fieldNum" varStatus="status" items="${fields[field.name]}">
          <tr>
            <td>
              <c:if test="${status.first}">
                <c:out value="${field.name}"/>
              </c:if>
            </td>
            <td>
              <html:select property="fieldOp(${fieldNum})">
                <c:forEach items="${ops[field.name]}" var="entry">
                  <html:option value="${entry.key}"><c:out value="${entry.value}"/></html:option>
                </c:forEach>
              </html:select>
            </td>
            <td>              
              <html:select property="fieldValue(${fieldNum})">
                <c:forEach items="${aliases[field.name]}" var="alias">
                  <html:option value="${alias}"><c:out value="${alias}"/></html:option>
                </c:forEach>
              </html:select>
            </td>
            <td>
              <%-- <c:if test="${status.last}">
                <html:submit property="action" onclick="document.forms[1].addField.value='${field.name}'">
                  <bean:message key="queryclass.addfield"/>
                </html:submit>
              </c:if> --%>
            </td>
          </tr>
        </c:forEach>
      </c:forEach>
    



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

  
