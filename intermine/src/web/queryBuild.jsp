<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- queryBuild.jsp -->
<html:form action="/query">
  <fmt:message key="query.current"/>
  <br/><br/>
  <c:forEach items="${queryClasses}" var="entry" varStatus="classStatus">
    <c:set var="alias" value="${entry.key}"/>
    <c:set var="queryClass" value="${entry.value}"/>
    <font class="queryViewFromItemTitle">
      <c:forTokens items="${queryClass.type}" delims="." var="token" varStatus="status">
        <c:if test="${status.last}">
          <c:out value="${token}"/>
        </c:if>
      </c:forTokens>
    </font>
    <font class="queryViewFromItemAlias">
      <c:out value="${alias}"/>
    </font>
    <c:if test="${editingAlias == null}">
[<html:link action="/changequery?method=editClass&alias=${alias}"><fmt:message key="button.edit"/></html:link>]
[<html:link action="/changequery?method=removeClass&alias=${alias}"><fmt:message key="button.remove"/></html:link>]
    </c:if>
    <br/>
    
    <c:choose>
      <c:when test="${alias == editingAlias}">

        <c:choose>

          <c:when test="${allFieldNames != null}">
            <c:forEach items="${queryClass.constraintNames}" var="constraintName">
              <c:out value="${queryClass.fieldNames[constraintName]}"/>
              <html:select property="fieldOps(${constraintName})">
                <c:forEach items="${validOps[queryClass.fieldNames[constraintName]]}" var="validOp">
                  <html:option value="${validOp.key}">
                    <c:out value="${validOp.value}"/>
                  </html:option>
                </c:forEach>
              </html:select> 
              <html:text property="fieldValues(${constraintName})"/>
              <c:if test="${constraintErrors != null}">
                <c:if test="${null != constraintErrors[constraintName]}">
                  <c:out value="${constraintErrors[constraintName]}"/>
                </c:if>
              </c:if>
              <br/>
            </c:forEach>
            <fmt:message key="query.addconstraint"/>
            <html:select property="newFieldName">
              <c:forEach items="${allFieldNames}" var="fieldName">
                <html:option value="${fieldName}"><c:out value="${fieldName}"/></html:option>
              </c:forEach>
            </html:select>
            <html:submit property="action"><fmt:message key="button.add"/></html:submit>
            <br/>
          </c:when>

          <c:otherwise>
            <fmt:message key="query.nofield"/>
          </c:otherwise>
        </c:choose>
        <br/>
        <html:submit property="action"><fmt:message key="button.update"/></html:submit>
      
      </c:when>
      <c:otherwise>
  
        <c:forEach items="${queryClass.constraintNames}" var="constraintName" 
                   varStatus="constraintStatus">
          <font class="queryViewConstraintLeft">
            <c:out value="${queryClass.fieldNames[constraintName]}"/>
          </font>
          <font class="queryViewConstraintOp">
            <c:out value="${queryClass.fieldOps[constraintName]}"/>
          </font>
          <font class="queryViewConstraintRight">
            <c:out value="${queryClass.fieldValues[constraintName]}"/>
          </font>
            <br/>
        </c:forEach>
        
      </c:otherwise>
    </c:choose>
    <c:if test="${!classStatus.last}"><hr/></c:if>
  </c:forEach>
  <%-- only display the run query button if at least one queryclass is present --%>
  <c:if test="${queryClass != null}">
    <br/><br/>
    <html:submit property="action"><fmt:message key="query.reset"/></html:submit>
    <html:submit property="action"><fmt:message key="query.run"/></html:submit>
  </c:if>
</html:form>
<!-- /queryBuild.jsp -->
