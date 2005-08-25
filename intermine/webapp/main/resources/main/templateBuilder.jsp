<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- templateBuilder.jsp -->

<html:xhtml/>
<div class="body">
  <fmt:message key="templateBuilder.intro"/>
  <p style="text-align:center"> 
    <c:if test="${!empty EDITING_TEMPLATE}">
      <i><fmt:message key="templateBuilder.editingTemplate">
        <fmt:param value="${EDITING_TEMPLATE.name}"/>
      </fmt:message></i>
    </c:if>
  </p>
  <html:form action="/buildTemplate">
    <p style="text-align:center">
      <fmt:message key="templateBuilder.shortName"/>
      <html:text property="shortName" size="32"/>
    </p>
    <p style="text-align:center">
      <fmt:message key="templateBuilder.templateDescription"/>
      <html:text property="description" size="55"/>
    </p>
    <p style="text-align:center">
      <fmt:message key="templateBuilder.keywords"/>
      <html:text property="keywords" size="55"/>
    </p>
    <%--
    <p style="text-align:center">
      <fmt:message key="templateBuilder.category"/>
      <html:select property="category">
        <html:options name="CATEGORIES"/>
      </html:select>
    </p>
    --%>
    <c:if test="${IS_SUPERUSER}">
      <p style="text-align:center">
        <html:checkbox property="important"/>
        <fmt:message key="templateBuilder.important"/>
      </p>
    </c:if>
    <br/>
    <table border="0" width="10%" cellspacing="0" class="templateBuilder" align="center">
      <tr>
        <th width="1%" align="center" nowrap="nowrap">
          &nbsp;<fmt:message key="templateBuilder.editable"/>&nbsp;
        </th>
        <th width="1%" align="center">
          <fmt:message key="templateBuilder.constraint"/>
        </th>
        <th width="97%" align="center">
          <fmt:message key="templateBuilder.label"/>
        </th>
        <c:if test="${IS_SUPERUSER}">
          <th width="1%" align="center">
            <fmt:message key="templateBuilder.identifier"/>
          </th>
        </c:if>
      </tr>
      <c:set var="index" value="${0}"/>
      <c:forEach var="entry" items="${TEMPLATE_PATHQUERY.nodes}" varStatus="status">
        <c:set var="node" value="${entry.value}"/>
        <c:if test="${node.attribute && !empty node.constraints}">
          <c:forEach var="constraint" items="${node.constraints}" varStatus="status">
            <c:set var="index" value="${index+1}"/>
            <tr>
              <td align="center" nowrap="nowrap">
                <html:checkbox property="constraintEditable(${index})"/>
              </td>
              <td align="center" nowrap="nowrap">
                ${node.path}
                <%-- maybe we should have a tag to display constraints? --%>
                <span class="constraint">
                  <c:out value="${constraint.op}"/>
                  <c:choose>
                    <c:when test="${constraint.value.class.name == 'java.util.Date'}">
                      <fmt:formatDate dateStyle="SHORT" value="${constraint.value}"/>
                    </c:when>
                    <c:otherwise>
                      <c:out value=" ${constraintDisplayValues[constraint]}"/>
                    </c:otherwise>
                  </c:choose>
                </span>
              </td>
              <td align="center" nowrap="nowrap">
                <html:text property="constraintLabel(${index})" size="35"/>
              </td>
              <c:if test="${IS_SUPERUSER}">
                <td align="center" nowrap="nowrap">
                  <html:text property="constraintIdentifier(${index})" size="10"/>
                </td>
              </c:if>
            </tr>
          </c:forEach>
        </c:if>
      </c:forEach>
    </table><br/>
    <p style="text-align:center">
      <html:submit>
        <c:choose>
          <c:when test="${empty EDITING_TEMPLATE}">
            <fmt:message key="templateBuilder.submit"/>
          </c:when>
          <c:otherwise>
            <fmt:message key="templateBuilder.submitUpdate"/>
          </c:otherwise>
        </c:choose>
      </html:submit>
      <html:submit property="preview">
        <c:choose>
          <c:when test="${!showPreview}">
            <fmt:message key="templateBuilder.submitToPreview"/>
          </c:when>
          <c:otherwise>
            <fmt:message key="templateBuilder.refreshPreview"/>
          </c:otherwise>
        </c:choose>
      </html:submit>
    </p>
  </html:form>
  <br/><br/>
  <c:if test="${showPreview}">
    <div align="center">
      <div id="tmplPreview">
        <div class="previewTitle">
          <fmt:message key="templateBuilder.previewtitle"/>
        </div><br/>
        <tiles:get name="preview"/>
      </div>
    </div>
  </c:if>
</div>

<!-- /templateBuilder.jsp -->
