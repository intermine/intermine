<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- templateSettings.jsp -->

<html:xhtml/>

<c:if test="${TEMPLATE_BUILD_STATE != null}">

  <im:heading>
  <c:choose>
    <c:when test="${TEMPLATE_BUILD_STATE.updatingTemplate != null}">
      <fmt:message key="templateBuilder.editingTemplate">
        <fmt:param value="${TEMPLATE_BUILD_STATE.updatingTemplate.name}"/>
      </fmt:message>
    </c:when>
    <c:otherwise>
      Building a new template query
     <im:manualLink section="manualCreateTemplates.shtml"/>
    </c:otherwise>
  </c:choose>
  </im:heading>

  <im:body>

  <fmt:message key="templateBuilder.save.msg"/>

  <html:form action="/templateSettingsAction">
    <div align="center">
    <p>
    <table border="0" width="10%">
      <tr>
        <td width="1%" align="right" nowrap><fmt:message key="templateBuilder.shortName"/></td>
        <td nowrap>
          <html:text property="name" size="32"/>
          <c:if test="${empty TEMPLATE_BUILD_STATE.name}">
            <span class="errors">(Required)</span>
          </c:if>
        </td>
      </tr>
      <tr>
        <td align="right"><fmt:message key="templateBuilder.templateTitle"/></td>
        <td nowrap>
          <html:text property="title" size="55"/>
          <c:if test="${empty TEMPLATE_BUILD_STATE.title}">
            <span class="errors">(Required)</span>
          </c:if>
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
      <%--<tr>
        <td align="right"><fmt:message key="templateBuilder.keywords"/></td>
        <td><html:text property="keywords" size="45"/></td>
      </tr>--%>
      <tr>
        <td align="center" colspan="2">
          <html:submit>Update settings</html:submit>
        </td>
      </tr>
    </table>
    </p>
    </div>
  </html:form>
  </im:body>

</c:if>

<!-- /templateSettings.jsp -->
