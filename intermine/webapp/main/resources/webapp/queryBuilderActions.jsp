<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- queryBuilderActions.jsp -->
    <div class="heading">
      <fmt:message key="view.actions"/>
    </div>
    <div class="body actions" align="right">
      <p><html:form action="/queryBuilderViewAction" styleId="submitform">
        <html:image property="action" src="images/show_results.png" disabled="${fn:length(viewStrings) <= 0}">
          <fmt:message key="view.showresults"/>
        </html:image>
      </html:form><p/>
	  <c:if test="${PROFILE.loggedIn && (NEW_TEMPLATE == null && EDITING_TEMPLATE == null) && fn:length(viewStrings) > 0}">
	      <p><form action="<html:rewrite action="/queryBuilderChange"/>" method="post">
	        or...&nbsp;<input type="hidden" name="method" value="startTemplateBuild"/>
	        <input type="submit" value="Start building a template query" />
	      </form><p/>
	  </c:if>
        <p>
          <tiles:insert page="queryBuilderSaveQuery.jsp"/>
        </p>
    </div>
<!-- /queryBuilderActions.jsp -->
