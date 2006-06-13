<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- constraintSettings.jsp -->

<html:xhtml/>

<im:heading>
  Template constraint settings
</im:heading>

<c:set var="editable" value="${constraint.editable}"/>

<im:body>
    <table border="0" width="100%">
      <tr>
        <td align="right"><fmt:message key="templateBuilder.editable"/></td>
        <td><input type="checkbox" name="editable" ${editable ? 'checked="checked"' : ''} /></td>
      </tr>
      <tr>
        <td align="right" nowrap><fmt:message key="templateBuilder.label"/></td>
        <td><html:text property="templateLabel" size="20"
             value="${editable ? constraint.description : ''}"/></td>
      </tr>
      <c:if test="${IS_SUPERUSER}">
        <tr>
          <td align="right"><fmt:message key="templateBuilder.identifier"/></td>
          <td><html:text property="templateId" size="18"
               value="${editable ? constraint.identifier : ''}"/></td>
        </tr>
      </c:if>
      <c:if test="${editingTemplateConstraint}">
        <tr>
          <td>&nbsp;</td>
          <td>
            <html:submit property="template">
              Update
            </html:submit>
          </td>
        </tr>
      </c:if>
    </table>
</im:body>

<!-- /constraintSettings.jsp -->
