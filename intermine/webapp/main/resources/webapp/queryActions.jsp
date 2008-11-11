<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- queryActions.jsp -->
    <div class="heading">
      <fmt:message key="view.actions"/>
    </div>
    <div class="body actions" align="right">
      <p><html:form action="/viewAction" styleId="submitform">
        <html:image property="action" src="theme/show_results.jpg">
          <fmt:message key="view.showresults"/>
        </html:image>
      </html:form><p/>
	  <c:if test="${!empty PROFILE.username && TEMPLATE_BUILD_STATE == null}">
	      <p><form action="<html:rewrite action="/mainChange"/>" method="post">
	        or...&nbsp;<input type="hidden" name="method" value="startTemplateBuild"/>
	        <input type="submit" value="Start building a template query" />
	      </form><p/>
	  </c:if>
        <p>
          <tiles:insert page="saveQuery.jsp"/>
        </p>
    </div>
<!-- /queryActions.jsp -->