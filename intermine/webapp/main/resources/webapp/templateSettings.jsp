<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<!-- escapeXml -->
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>


<!-- templateSettings.jsp -->

<html:xhtml/>

<c:if test="${EDITING_TEMPLATE != null || NEW_TEMPLATE != null}">

  <div class="listHeading">
  <c:choose>
    <c:when test="${EDITING_TEMPLATE != null}">
      <fmt:message key="templateBuilder.editingTemplate">
        <fmt:param value="${fn:escapeXml(QUERY.name)}"/>
      </fmt:message>
    </c:when>
    <c:otherwise>      
      <fmt:message key="templateBuilder.new"/>
    </c:otherwise>
  </c:choose>
  </div>

  <div class="body">

  <fmt:message key="templateBuilder.saveIntro"/>

  <html:form styleId="queryBuilderTemplateSettings" action="/templateSettingsAction">
    <div align="center">
    <table>
      <tr>
        <td align="right" valign="top" nowrap><fmt:message key="templateBuilder.shortName"/>
          <c:if test="${empty QUERY.name}">
            <span class="errors">(Required)</span>
          </c:if>
        </td>
        <td nowrap>
          <input type="text" value="<c:if test="${not empty QUERY.name}"><c:out value="${QUERY.name}" escapeXml="true"/></c:if>" size="32" name="name">
          <br/>
          <small><i><fmt:message key="templateBuilder.nameHelp"/></i></small>
        </td>
      </tr>
      <tr>
        <td align="right" valign="top"><fmt:message key="templateBuilder.templateTitle"/>
          <c:if test="${empty QUERY.title}">
            <span class="errors">(Required)</span>
          </c:if>
        </td>
        <td nowrap>
          <input type="text" value="<c:if test="${not empty QUERY.title}"><c:out value="${QUERY.title}" escapeXml="true"/></c:if>" size="55" name="title">
          <br/>
          <small><i><fmt:message key="templateBuilder.titleHelp"/></i></small>
        </td>
      </tr>
      <tr>
        <td align="right"><fmt:message key="templateBuilder.templateDescription"/></td>
        <td nowrap>
          <textarea rows="3" cols="55" name="description"><c:if test="${not empty QUERY.description}"><c:out value="${QUERY.description}" escapeXml="true"/></c:if></textarea>
        </td>
      </tr>
      <tr>
        <td align="right"><fmt:message key="templateBuilder.templateComment"/></td>
        <td nowrap>
          <input type="text" value="<c:if test="${not empty QUERY.comment}"><c:out value="${QUERY.comment}" escapeXml="true"/></c:if>" size="55" name="comment">
        </td>
      </tr>
      <tr>
        <td align="center" colspan="2">
          <html:submit>Update settings</html:submit>
        </td>
      </tr>
    </table>
    </div>
  </html:form>
  </div>

</c:if>

<!-- /templateSettings.jsp -->
