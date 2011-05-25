<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<script language="javascript">
    function getWebserviceUrl() {
        var url = "/${WEB_PROPERTIES['webapp.path']}/exportQuery.do?as=link&serviceFormat=tab";
        var callback = function(data) {
            jQuery('div.popup').show().find('input').val(data).select();
        };
        jQuery.get(url, callback, "text");
        return false;
    }
</script>
<div id="sidebar">
   <div id="showResultButton" 
   <c:if test="${fn:length(viewStrings) <= 0}">class='button inactive'</c:if>
   <c:if test="${fn:length(viewStrings) > 0}">class='button'</c:if>>
      <div class="left"></div>
          <html:form action="/queryBuilderViewAction" styleId="submitform">
          <input id="showResult" type="submit" name="showResult"
          value='<fmt:message key="view.showresults"/>'/>
          </html:form><div class="right"></div>
  </div>
</div>
<!-- queryBuilderActions.jsp -->
<div class="queryActions">
<table>
  <tr>
    <td>
    <div class="heading" style="border:none">
          <a id="permalink" style="text-decoration:none;font-size:11px" target="new" href="" onclick="javascript:getWebserviceUrl();return false;"
    title="Get a URL to run this query from the command line or a script">web service URL</a>
    <div class="popup" style="display:none;">
      <span class="close" onclick="jQuery('div.popup').hide();return false;"></span>
      <p style="width:95%;">
      Use the URL below to fetch results for this query from the command line or a script 
      <i>(please note that you will need to use authentication to access private templates and lists)</i>:
      </p>
    <input type="text" value="None">
    </div>
  </div>
    </td>
    <td>
      <a href="/${WEB_PROPERTIES['webapp.path']}/wsCodeGen.do?method=perl&source=pathQuery" target="_blank">Perl</a>
      <span>|</span>
      <a href="/${WEB_PROPERTIES['webapp.path']}/wsCodeGen.do?method=python&source=pathQuery" target="_blank">Python</a>
      <span>|</span>
      <a href="/${WEB_PROPERTIES['webapp.path']}/wsCodeGen.do?method=java&source=pathQuery" target="_blank">Java</a>
      <a href="/${WEB_PROPERTIES['webapp.path']}/api.do" target="_blank"><span>[help]</span></a>
    </td>
    <td>
    <a href="/${WEB_PROPERTIES['webapp.path']}/exportQuery.do?as=xml" title="Export this query as XML"><fmt:message key="query.export.as"/></a>
    </td>
  </tr>
</table>
</div>

    <div class="body actions" align="right">
    <c:if test="${PROFILE.loggedIn && (NEW_TEMPLATE == null && EDITING_TEMPLATE == null) && fn:length(viewStrings) > 0}">
        <p><form action="<html:rewrite action="/queryBuilderChange"/>" method="post">
          <input type="hidden" name="method" value="startTemplateBuild"/>
          <input class="template" type="submit" value="Start building a template query" />
        </form><p/>
    </c:if>
        <p>
          <tiles:insert page="queryBuilderSaveQuery.jsp"/>
        </p>
    </div>
<!-- /queryBuilderActions.jsp -->
