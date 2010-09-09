<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- templateSettings.jsp -->

<html:xhtml/>

<c:if test="${EDITING_TEMPLATE != null || NEW_TEMPLATE != null}">

  <div class="listHeading">
  <c:choose>
    <c:when test="${EDITING_TEMPLATE != null}">
      <fmt:message key="templateBuilder.editingTemplate">
        <fmt:param value="${QUERY.name}"/>
      </fmt:message>
    </c:when>
    <c:otherwise>      
      <fmt:message key="templateBuilder.new"/>
    </c:otherwise>
  </c:choose>
  </div>

  <div class="body">

  <fmt:message key="templateBuilder.saveIntro"/>

  <html:form action="/templateSettingsAction">
    <div align="center">
    <p>
    <table border="0" width="10%">
      <tr>
        <td width="1%" align="right" valign="top" nowrap><fmt:message key="templateBuilder.shortName"/>
          <c:if test="${empty QUERY.name}">
            <span class="errors"> (Required)</span>
          </c:if>        
        </td>
        <td nowrap>
          <html:text property="name" size="32"/>
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
          <html:text property="title" size="55"/>
          <br/>
          <small><i><fmt:message key="templateBuilder.titleHelp"/></i></small>
        </td>
      </tr>
      <tr>
        <td align="right"><fmt:message key="templateBuilder.templateDescription"/></td>
        <td nowrap>
          <html:textarea property="description" cols="55" rows="3"/>
        </td>
      </tr>
      <tr>
        <td align="right"><fmt:message key="templateBuilder.templateComment"/></td>
        <td nowrap>
          <html:text property="comment" size="55"/>
        </td>
      </tr>
      <tr>
        <td align="center" colspan="2">
          <html:submit>Update settings</html:submit>
        </td>
      </tr>
    </table>
    </p>
    </div>
  </html:form>
  </div>

</c:if>

<!-- /templateSettings.jsp -->
