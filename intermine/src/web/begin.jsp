<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- begin.jsp -->


<%-- Build a query --%>

<table class="box" cellspacing="0" cellpadding="6" border="0" width="100%" align="center">
  <tr>
  
    <th class="title" align="left"><fmt:message key="begin.heading.build"/></th>
    <th class="help" align="right">
      [<html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/manualStartingaquery.html">
        <fmt:message key="begin.link.help"/>
      </html:link>]
      [<html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/manualQuickstart.html">
        <fmt:message key="begin.link.quickstart"/>
      </html:link>]
    </th>
    
  </tr>
  <tr>
    <td valign="top">
      <c:forEach items="${CATEGORIES}" var="category">
        <b><c:out value="${category}"/></b>
        <p>
          <c:set var="subnames" value="${CATEGORY_CLASSES[category]}"/>
          <c:forEach items="${subnames}" var="subname">
            <a href="<html:rewrite page="/queryClassSelect.do"/>?action=<fmt:message key="button.selectClass"/>&className=${subname}">${subname}</a>,
          </c:forEach>
          <c:if test="${!empty CATEGORY_TEMPLATES[category]}">
            <br/><span class="smallnote"><fmt:message key="begin.or"/> <html:link action="/templates" paramId="category" paramName="category"><fmt:message key="begin.related.templates"/></html:link></span>
          </c:if>
          <br/>
          <span class="smallnote"><fmt:message key="begin.or"/> <html:link action="/classChooser"><fmt:message key="begin.view.all.classes"/></html:link></span>
        </p>  
      </c:forEach>
      
    </td>
    <td valign="bottom" align="right">
    
      <fmt:message key="begin.list.all.classes"/>
      <html:link action="/classChooser">
        <img class="arrow" src="images/right-arrow.png" alt="->"/>
      </html:link><br/>
      <fmt:message key="begin.browse.model"/>
      <html:link action="/tree">
        <img class="arrow" src="images/right-arrow.png" alt="->"/>
      </html:link><br/>
      <fmt:message key="begin.upload.identifiers"/>
      <html:link action="/bagBuild">
        <img class="arrow" src="images/right-arrow.png" alt="->"/>
      </html:link><br/>
    </td>
  </tr>
</table>

<%-- /Build a query --%>

<c:if test="${!empty WEB_PROPERTIES['begin.browse.template']}">
  <p/>

  <%-- Browse - only show if begin.browse.template has been defined in model web.properties --%>

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
          <fmt:message key="begin.input.browse"/>
          <html:hidden property="attributeOps(1)" value="${browseOperator}"/>
          <!-- tell action to skip query builder -->
          <html:hidden property="skipBuilder" value="1"/>
          <html:text property="attributeValues(1)"/>
          <html:submit><fmt:message key="begin.input.submit"/></html:submit>
          <br/>
          <span class="smallnote">${WEB_PROPERTIES["begin.browse.prompt"]}</span>
        </html:form>
      </td>
    </tr>
  </table>

  <%-- /Browse --%>
</c:if>


<p/>

<%-- Templates --%>

<table class="box" cellspacing="0" cellpadding="6" border="0" width="100%" align="center">
  <tr>
    <th align="left" class="title"><fmt:message key="begin.heading.templates"/></th>
    <th align="right" class="help">
      [<html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/manualStartingaquery.html">
        <fmt:message key="begin.link.help"/>
      </html:link>]
    </th>
  </tr>
  <tr>
    <td colspan="2" valign="top">
      
      <c:forEach items="${TEMPLATE_QUERIES}" var="templateQuery" end="10">
        <c:out value="${templateQuery.value.cleanDescription}"/>
        <html:link action="/template?name=${templateQuery.key}">
          <img class="arrow" src="images/right-arrow.png" alt="->"/>
        </html:link>
        <br/>
      </c:forEach>
      <br/>
      <span class="smallnote">
        <fmt:message key="begin.or"/> <html:link action="/templates"><fmt:message key="begin.templates.view.all"/></html:link>
      </span>
      
    </td>
  </tr>
</table>

<%-- /Templates --%>

<p/>



<!-- /begin.jsp -->
