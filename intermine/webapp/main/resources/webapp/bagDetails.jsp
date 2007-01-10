<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- bagDetails.jsp -->
<html:xhtml/>

<script type="text/javascript">
<!--//<![CDATA[
  var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
  var detailsType = 'bag';
//]]>-->
</script>
<script type="text/javascript" src="js/inlinetemplate.js">
  var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
</script>

<div class="heading">
    Bag Contents
</div>

<div class="body" >
<p>The bag <b>${bag.name}</b> contains elements of type:<b>${bag.type}</b>.</p>
<html:form action="/modifyBagDetailsAction?removeFromBag=true">
<html:hidden property="bagName" value="${bag.name}"/>

<table><tr><td width="50%">
<table class="results" cellspacing="0">
  <tr>
  	<th>&nbsp;</th>
    <c:forEach var="column" items="${pagedColl.columns}" varStatus="status">
      <th align="center" valign="top">
	<div>              
	  <c:out value="${fn:replace(column.path, '.', '&nbsp;> ')}" escapeXml="false"/>
	</div>
      </th>
    </c:forEach>
  </tr>
 
  <c:forEach items="${pagedColl.rows}" var="row" varStatus="status">
    <c:set var="object" value="${row[0]}" scope="request"/>
       <c:set var="rowClass">
       <c:choose>
          <c:when test="${status.count % 2 == 1}">odd</c:when>
          <c:otherwise>even</c:otherwise>
       </c:choose>
    </c:set>
    <tr class="${rowClass}">
     <td align="center" class="checkbox" >
       <html:multibox property="selectedElements" value="${object.id}" />
     </td>
     <c:forEach var="column" items="${pagedColl.columns}" varStatus="status2">
       <td>
        <c:set var="resultElement" value="${row[column.index]}" scope="request"/>
          <c:choose>
            <c:when test="${pagedColl.columnNames[column.index] == 'Gene.chromosomeLocation'}">
           </c:when>
	        <c:when test="${resultElement.keyField}">
	          <html:link action="/objectDetails?id=${resultElement.id}">
		        <c:out value="${resultElement.field}" />
	          </html:link>
	        </c:when>
	        <c:otherwise>
		        <c:out value="${resultElement.field}" />
		    </c:otherwise>
		  </c:choose>
       </td>
     </c:forEach>
    </tr>
  </c:forEach>
</table>
<br/>
<html:submit property="remove">
    Remove
</html:submit>
</html:form>
</td>

<td valign="top" align="center" width="50%">
<i>Here will be the bag description</i>
</td>
</tr></table>
</div>

<br/>

<c:if test="${! empty graphDisplayerArray}">
<div class="heading">
  Widgets
</div>
<div class="body">
  <c:forEach items="${graphDisplayerArray}" var="htmlContent">
    <div class="widget">
      <c:out value="${htmlContent[0]}" escapeXml="false"/>
      <p><c:out value="${htmlContent[1]}"/></p>
    </div>
  </c:forEach>
</div>
</c:if>

<div class="heading">
  Templates
</div>

<div class="body">
    <%-- Each aspect --%>
    <c:forEach items="${CATEGORIES}" var="aspect">
      <tiles:insert name="objectDetailsAspect.tile">
        <tiles:put name="placement" value="aspect:${aspect}"/>
        <tiles:put name="interMineIdBag" beanName="bag"/>
      </tiles:insert>
    </c:forEach>
</div>

<c:if test="${pagedColl.size > 0}">
  <c:set var="tableName" value="${bag.name}" scope="request"/>
  <c:set var="tableType" value="bag" scope="request"/>
  <c:set var="pagedTable" value="${pagedColl}" scope="request"/>
  <tiles:get name="export.tile"/>
</c:if>

<!-- /bagDetails.jsp -->
