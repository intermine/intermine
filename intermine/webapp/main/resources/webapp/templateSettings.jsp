<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>
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

  <html:form action="/createTemplate">
    <div align="center">
    <table>
      <tr>
        <td align="right" valign="top" nowrap><fmt:message key="templateBuilder.shortName"/>
          <c:if test="${empty QUERY.name}">
            <span class="errors">(Required)</span>
          </c:if>
        </td>
        <td nowrap>
          <input type="text" onchange="updateTemplate('name', this.value);" value="<c:if test="${not empty QUERY.name}"><c:out value="${QUERY.name}" escapeXml="true"/></c:if>" size="32" name="name">
          <br/>
          <small><i><fmt:message key="templateBuilder.nameHelp"/></i></small>
        </td>
      </tr>
      <tr>
        <td align="right" valign="top"><fmt:message key="templateBuilder.templateTitle"/>
        </td>
        <td nowrap>
          <input type="text" onchange="updateTemplate('title', this.value);" value="<c:if test="${not empty QUERY.title}"><c:out value="${QUERY.title}" escapeXml="true"/></c:if>" size="55" name="title">
          <br/>
          <small><i><fmt:message key="templateBuilder.titleHelp"/></i></small>
        </td>
      </tr>
      <tr>
        <td align="right"><fmt:message key="templateBuilder.templateDescription"/></td>
        <td nowrap>
          <textarea rows="3" cols="55" name="description" onchange="updateTemplate('description', this.value);"><c:if test="${not empty QUERY.description}"><c:out value="${QUERY.description}" escapeXml="true"/></c:if></textarea>
        </td>
      </tr>
      <tr>
        <td align="right"><fmt:message key="templateBuilder.templateComment"/></td>
        <td nowrap>
          <input type="text" onchange="updateTemplate('comment', this.value);" value="<c:if test="${not empty QUERY.comment}"><c:out value="${QUERY.comment}" escapeXml="true"/></c:if>" size="55" name="comment">
        </td>
      </tr>
    </table>
    <input type="hidden" name="actionType" id="actionType"/>
    </div>
</html:form>
  <link rel="stylesheet" type="text/css" href="css/templatePreview.css"/>

  <imutil:disclosure id="template-preview" opened="true" styleClass="body">
  <imutil:disclosureBody style="align:center;">
      <div id="tmplPreview">
        <div class="previewTitle">
          <fmt:message key="templateBuilder.previewtitle"/>
        </div>
        <fmt:message key="templatePreview.sort.instructions"/>
        <tiles:insert name="template.tile">
          <tiles:put name="builder" value="yes"/>
        </tiles:insert>
      </div>
  </imutil:disclosureBody>
  </imutil:disclosure>

    <div align="center">
      <table border="0">
        <tr>
          <td>
            <form action="<html:rewrite action="/queryBuilderChange"/>" method="post">
              <input type="hidden" name="method" value="stopTemplateBuild"/>
              <input type="submit" value="Cancel template building" />
            </form>
          </td>
          <td>
            <c:if test="${PROFILE.loggedIn && !empty QUERY}">
                <fmt:message key="${NEW_TEMPLATE != null ?
                    'templateBuilder.save' : 'templateBuilder.update'}" var="saveLabel"/>
                <html:button value="${saveLabel}" property="saveTemplate" onclick="jQuery('#actionType').val('SAVE');jQuery('#templateSettingsForm').submit();"/>
            </c:if>
          </td>
          <td>
            <c:if test="${PROFILE.loggedIn && !empty QUERY}">
                <fmt:message key="${NEW_TEMPLATE != null ?
                    'templateBuilder.saveandrun' : 'templateBuilder.updateandrun'}" var="saveAndRunLabel"/>
                <html:button value="${saveAndRunLabel}" property="saveAndRunTemplate" onclick="jQuery('#actionType').val('RUN');jQuery('#templateSettingsForm').submit();"/>
            </c:if>
          </td>
        </tr>
      </table>
      <br/>
    </div>
  </div>

</c:if>

<!-- /templateSettings.jsp -->
