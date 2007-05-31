<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- templateSearch.jsp -->

<html:xhtml/>

<script type="text/javascript">
<!--
window.onload = function() { document.getElementById("queryString").focus(); }
// -->
</script>

<div class="body">
  <html:form action="/templateSearch" method="get">
    <fmt:message key="templateSearch.search.label"/>
    <html:text property="queryString" size="40" styleId="queryString"/>
    <html:select property="type">
      <html:option key="templateSearch.form.global" value="global"/>
      <html:option key="templateSearch.form.user" value="user"/>
      <html:option key="templateSearch.form.all" value="ALL"/>
    </html:select>
    <html:submit><fmt:message key="templateSearch.form.submit"/></html:submit>
  </html:form>
  <p class="smallnote">
    <fmt:message key="begin.searchtemplates.help.message"/>
  </p>
  
  <c:if test="${!empty results}">
    <p>
      <b>${resultCount}</b> results for <b>${queryString}</b>. <span class="tmplSearchTime">(${querySeconds} seconds)</span>
      <c:if test="${empty PROFILE_MANAGER || empty PROFILE.username}">
        <br/><i><fmt:message key="template.notlogged">
            <fmt:param>
              <im:login/>
            </fmt:param>
          </fmt:message>
        </i>
      </c:if>
    </p>
    <c:forEach items="${results}" var="entry" varStatus="status">
      <%--
         <span style="width:111px"><fmt:formatNumber value="${entry.value*100}" maxFractionDigits="0"/>%</span>
      --%>
      <fmt:formatNumber value="${entry.value*10}" maxFractionDigits="0" var="heat"/>
      <img class="searchHeatImg" src="images/heat${heat}.gif" width="${heat*2}" height="10"
           style="margin-right:${24-(heat*2)}px"/>
      <im:templateLine type="${templateTypes[entry.key]}" templateQuery="${entry.key}" desc="${highlighted[entry.key]}"/>
      <c:if test="${!status.last}">
        <hr class="tmplSeperator"/>
      </c:if>
    </c:forEach>
  </c:if>
  
  <c:if test="${empty results && !empty param.queryString}">
    <p>
      <b><fmt:message key="templateSearch.noresults"/></b>
    </p>
  </c:if>
  
  <p>
    <html:link action="/templates"><fmt:message key="templateSearch.viewall"/></html:link>
  </p>
  
</div>


<!-- /templateSearch.jsp -->
