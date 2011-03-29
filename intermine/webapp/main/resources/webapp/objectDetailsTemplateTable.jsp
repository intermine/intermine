<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- objectDetailsTemplateTable.jsp -->

<% if (pageContext.getAttribute("org.apache.struts.taglib.tiles.CompContext", PageContext.REQUEST_SCOPE) != null) { %>
  <tiles:importAttribute/>
<% } %>

<html:xhtml/>

<div style="overflow-x: auto;">
  <c:if test="${(reportObject != null || interMineIdBag !=null) && resultsTable != null}">

    <%-- Results table --%>
  <tiles:insert name="resultsTable.tile">
     <tiles:put name="pagedResults" beanName="resultsTable" />
     <tiles:put name="inlineTable" value="true" />
     <tiles:put name="currentPage" value="report" />
  </tiles:insert>

  </c:if>
</div>

<%-- Produce show in table link --%>

<c:set var="extra" value=""/>
  <c:choose>
  <c:when test="${! empty interMineIdBag}">
    <c:set var="extra" value="&amp;bagName=${interMineIdBag.name}"/>
  </c:when>
  <c:otherwise>
     <c:set var="extra" value="${extra}&amp;idForLookup=${reportObject.object.id}" />
  </c:otherwise>
  </c:choose>
<p class="in_table">
<html:link styleClass="theme-1-color" action="/modifyDetails?method=runTemplate&amp;name=${templateQuery.name}&amp;scope=global${extra}&amp;trail=${param.trail}">
  Show all in a table
</html:link>
</p>

<%-- Update ui given results of this template --%>

<c:choose>
  <c:when test="${resultsTable == null}">
    <script type="text/javascript">
      <%-- Please don't CDATA this script element. See [762] --%>
      $('img_${fn:replace(placement, ' ', '_')}_${templateQuery.name}').src='images/blank.gif';
    </script>
  </c:when>
  <c:otherwise>
    <script type="text/javascript">
      <%-- Please don't CDATA this script element. See [762] --%>
      id = '${fn:replace(placement, ' ', '_')}_${templateQuery.name}';
      if (${resultsTable.exactSize} == 0) {
        $('img_'+id).src='images/plus-disabled.gif';
        $('label_'+id).className='nullStrike';
        $('count_'+id).innerHTML='no results';
        $('img_'+id).parentNode.href='#';
        $('img_'+id).parentNode.onclick = function(){return false;};
      } //else {
        //$('count_'+id).innerHTML='<a href=\"modifyDetails.do?method=runTemplate&amp;name=${templateQuery.name}&amp;scope=global${extra}&amp;trail=${param.trail}\" title=\"View results of this template in a table\">${resultsTable.exactSize} results</a>';
      //}
    </script>
  </c:otherwise>
</c:choose>


<!-- /objectDetailsTemplateTable.jsp -->
