<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- queryBuild.jsp -->
<html:form action="/query">
  <c:choose>
    <c:when test="${empty QUERY_CLASSES}">
      <fmt:message key="query.empty"/>
    </c:when>
    <c:otherwise>
      <fmt:message key="query.current"/>
    </c:otherwise>
  </c:choose>
  <br/><br/>
  <c:forEach items="${QUERY_CLASSES}" var="entry" varStatus="classStatus">
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
    <c:if test="${EDITING_ALIAS == null}">
[<html:link action="/changequery?method=editClass&alias=${alias}"><fmt:message key="button.edit"/></html:link>]
[<html:link action="/changequery?method=removeClass&alias=${alias}"><fmt:message key="button.remove"/></html:link>]
    </c:if>
    <br/>
    <c:choose>
      <c:when test="${alias == EDITING_ALIAS}">

        <c:choose>

          <c:when test="${!empty allFieldNames}">
          <div class="queryBuildQueryClasses">
            <table>
            <c:forEach items="${queryClass.constraintNames}" var="constraintName">
              <c:set var="fieldName" value="${queryClass.fieldNames[constraintName]}"/>
              <tr>
                <td>
              <c:out value="${fieldName}"/>
                </td>
                <td>
              <html:select property="fieldOps(${constraintName})">
                <c:forEach items="${validOps[fieldName]}" var="validOp">
                  <html:option value="${validOp.key}">
                    <c:out value="${validOp.value}"/>
                  </html:option>
                </c:forEach>
              </html:select> 
                </td>
                <td>
                  <c:choose>
                    <c:when test="${validAliases[fieldName] != null}">
                      <html:select property="fieldValue(${constraintName})">
                        <c:forEach items="${validAliases[fieldName]}" var="validAlias">
                          <html:option value="${validAlias}">
                            <c:out value="${validAlias}"/>
                          </html:option>
                        </c:forEach>
                      </html:select>
                    </c:when>
                    <c:otherwise>
                      <html:text property="fieldValues(${constraintName})"/>
                    </c:otherwise>
                  </c:choose>
                </td>
              </tr>
            </c:forEach>
            </table>
            </div>
            <fmt:message key="query.addconstraint"/>
            <html:select property="newFieldName">
              <c:forEach items="${allFieldNames}" var="fieldName">
                <html:option value="${fieldName}"><c:out value="${fieldName}"/></html:option>
              </c:forEach>
            </html:select>
            <html:submit property="action"><fmt:message key="button.add"/></html:submit>
          </c:when>
          <c:otherwise>
            <fmt:message key="query.nofield"/>
          </c:otherwise>
        </c:choose>
        <br/>
        <html:submit property="action"><fmt:message key="button.update"/></html:submit>
        <br/>

      </c:when>
      <c:otherwise>
  
        <div class="queryBuildQueryClasses">
        <table>
        <c:forEach items="${queryClass.constraintNames}" var="constraintName" 
                   varStatus="constraintStatus">
          <tr>
            <td class="queryBuildFirstCell">
            </td>
            <td>
          <font class="queryViewConstraintLeft">
            <c:out value="${queryClass.fieldNames[constraintName]}"/>
          </font>
            </td>
            <td>
          <font class="queryViewConstraintOp">
            <c:out value="${queryClass.fieldOps[constraintName]}"/>
          </font>
            </td>
            <td>
          <font class="queryViewConstraintRight">
            <c:out value="${queryClass.fieldValues[constraintName]}"/>
          </font>
            </td>
          </tr>
        </c:forEach>
        </table>
        </div>
      </c:otherwise>
    </c:choose>

    <c:if test="${!classStatus.last}"><hr/></c:if>
  </c:forEach>

  <c:if test="${EDITING_ALIAS == null}">
    <c:if test="${!empty QUERY_CLASSES}">
      <html:submit property="action">
        <fmt:message key="query.run"/>
      </html:submit>
    </c:if>
    <c:if test="${ADVANCED_MODE}">
      <html:submit property="action">
        <fmt:message key="query.editfql"/>
      </html:submit>
    </c:if>
  </c:if>
</html:form>
<!-- /queryBuild.jsp -->
