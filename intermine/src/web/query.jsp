<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<c:if test="${cld != null}">
    <html:form action="/query">
        <c:out value="${cld.unqualifiedName}"/><br/>
          
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
      
      <%--        <c:forEach var="field" items="${cld.referenceDescriptors}">
        <tr><td><c:out value="${field.name}"/></td>
          <td><html:text property="fieldValue(field.name)"/></td></tr>
      </c:forEach>
      
      <c:forEach var="field" items="${cld.collectionDescriptors}">
        <tr><td><c:out value="${field.name}"/></td>
          <td><html:text property="fieldValue(field.name)"/></td></tr>
      </c:forEach>
      --%>
      
            <tr><td>
                <html:submit property="action">
                    <bean:message key="button.submit"/>
                </html:submit>
            </td></tr>
        </table>    

    </html:form>      
</c:if>

  