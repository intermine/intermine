<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- queryBuilderActions.jsp -->
    <div class="heading">
      <fmt:message key="view.actions"/>
      <span style="font-size: 0.8em; font-weight:100; padding: 0px 10px 0px 10px">
        <a href="/${WEB_PROPERTIES['webapp.path']}/wsCodeGen.do?method=perl&source=pathQuery" target="_blank">Perl</a>
        <span>|</span>
        <a href="/${WEB_PROPERTIES['webapp.path']}/wsCodeGen.do?method=java&source=pathQuery" target="_blank">Java</a>
        <a href="/${WEB_PROPERTIES['webapp.path']}/api.do" target="_blank"><span>[help]</span></a>
      </span>
    </div>
    <div class="body actions" align="right">
      <p><html:form action="/queryBuilderViewAction" styleId="submitform">
        <input type="submit" value="<fmt:message key="view.showresults"/>" 
            <c:if test="${fn:length(viewStrings) <= 0}">disabled</c:if>/> 
      </html:form></p>
    <c:if test="${PROFILE.loggedIn && (NEW_TEMPLATE == null && EDITING_TEMPLATE == null) && fn:length(viewStrings) > 0}">
        <p><form action="<html:rewrite action="/queryBuilderChange"/>" method="post">
          or...&nbsp;<input type="hidden" name="method" value="startTemplateBuild"/>
          <input class="template" type="submit" value="Start building a template query" />
        </form><p/>
    </c:if>
        <p>
          <tiles:insert page="queryBuilderSaveQuery.jsp"/>
        </p>
    </div>
<!-- /queryBuilderActions.jsp -->
