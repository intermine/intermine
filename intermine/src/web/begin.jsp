<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- begin.jsp -->

<%-- Build a query --%>

<table class="box" cellspacing="0" cellpadding="6" border="0" width="100%" align="center">
  <tr>
  
    <th class="title" align="left"><fmt:message key="begin.heading.build"/></th>
    <th class="help" align="right" nowrap="nowrap">
      [<html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/manualStartingaquery.html">
        <fmt:message key="begin.link.help"/>
      </html:link>]
      [<html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/manualQuickstart.html">
        <fmt:message key="begin.link.quickstart"/>
      </html:link>]
    </th>
    
  </tr>
  <tr>
    <td valign="top" align="left">
      <c:forEach items="${CATEGORIES}" var="category">
        <b><c:out value="${category}"/></b>
        <p style="text-align: left;">
          <c:set var="classes" value="${CATEGORY_CLASSES[category]}"/>
          <c:set var="catSize" value="${fn:length(classes)}"/>
          <c:forEach items="${classes}" var="classname" varStatus="status">
            <a href="<html:rewrite page="/queryClassSelect.do"/>?action=<fmt:message key="button.selectClass"/>&className=${classname}" title="<c:out value="${classDescriptions[classname]}"/>">
            <span class="type">${classname}</span></a><c:if test="${status.index+1 < catSize}">,</c:if>
          </c:forEach>
          <c:if test="${!empty CATEGORY_TEMPLATES[category]}">
            <br/><span class="smallnote"><fmt:message key="begin.or"/> <html:link action="/templates" paramId="category" paramName="category"><fmt:message key="begin.related.templates"/></html:link></span>
          </c:if>
        </p>  
      </c:forEach>
      
    </td>
    <td valign="bottom" align="right" nowrap="nowrap">
    
      <html:link action="/classChooser">
        <fmt:message key="begin.list.all.classes"/>
        <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
      </html:link><br/>
      <html:link action="/tree">
        <fmt:message key="begin.browse.model"/>
        <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
      </html:link><br/>
      <html:link action="/bagBuild">
        <fmt:message key="begin.upload.identifiers"/>
        <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
      </html:link><br/>
    </td>
  </tr>
</table>

<%-- /Build a query --%>


<%-- Browse - only show if begin.browse.template has been defined in model web.properties --%>

<c:if test="${!empty WEB_PROPERTIES['begin.browse.template'] && !empty GLOBAL_TEMPLATE_QUERIES[browseTemplateName]}">
  <p>

  <table class="box" cellspacing="0" cellpadding="6" border="0" width="100%" align="center">
    <tr>

      <th class="title" align="left"><fmt:message key="begin.heading.browse"/></th>
      <th class="help" align="right">
        [<html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/manualStartingaquery.html">
          <fmt:message key="begin.link.help"/>
        </html:link>]
      </th>

    </tr>
    <tr>
      <td colspan="2" align="center">
        <html:form action="/browseAction">
          <fmt:message key="begin.input.browse"/>:
          <html:hidden property="attributeOps(1)" value="${browseOperator}"/>
          <html:text property="attributeValues(1)"/>
          <input type="hidden" name="templateType" value="global"/>
          <input type="hidden" name="queryName" value="${browseTemplateName}"/>
          <html:submit property="skipBuilder"><fmt:message key="begin.input.submit"/></html:submit>
          <br/>
          <span class="smallnote">${WEB_PROPERTIES["begin.browse.prompt"]}</span>
        </html:form>
      </td>
    </tr>
  </table>

</c:if>
<%-- /Browse --%>


<p>

<tiles:insert attribute="globaltemplates">
  <tiles:put name="templates" beanName="GLOBAL_TEMPLATE_QUERIES" beanScope="application"/>
  <tiles:put name="headingKey" value="begin.heading.templates"/>
  <tiles:put name="templateType" value="global"/>
  <tiles:put name="showDelete" value="0"/>
</tiles:insert>

<p>

<c:set var="userTemplates" value="${PROFILE.savedTemplates}"/>
<tiles:insert attribute="usertemplates">
  <tiles:put name="templates" beanName="userTemplates"/>
  <tiles:put name="headingKey" value="begin.heading.mytemplates"/>
  <tiles:put name="templateType" value="user"/>
  <tiles:put name="showDelete" value="1"/>
  <tiles:put name="showEdit" value="1"/>
</tiles:insert>

<p>

<c:if test="${!empty PROFILE.savedTemplates && IS_SUPERUSER}">
<span class="smallnote">
  <html:link action="/exportTemplates?type=user" titleKey="begin.exportTemplatesDesc">
    <fmt:message key="begin.exportTemplates"/>
  </html:link><br/>
  <html:link action="/import" titleKey="begin.importTemplatesDesc">
    <fmt:message key="begin.importTemplates"/>
  </html:link>
</span>
</c:if>

<!-- /begin.jsp -->
