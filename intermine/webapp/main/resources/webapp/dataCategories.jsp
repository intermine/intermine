
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- dataCategories -->
<html:xhtml/>


<div class="body">
<table border="0">
<tr>
<td valign="top" width="50%">
    
      <div id="pageDesc" class="pageDesc"><fmt:message key="dataCategories.intro"/></div>

</td>
<td valign="top" width="50%">


	<div style=" width:75%">
	     <h2>Actions:</h2>
           <a href="/sources.html"><fmt:message key="dataCategories.action1"/></a>
  <BR/> 
           <html:link action="/templates">
             <fmt:message key="dataCategories.action2"/>
           </html:link>
            <BR/><BR/>         
       </div>
           
</td>
</tr>
</table>


<div class="pageDesc" id="pageDesc" style="width:70%">

		<h2><fmt:message key="dataCategories.title"/></h2>
    
		<c:choose>
		    <c:when test="${!empty ASPECTS}">
		       <tiles:insert name="aspects.tile"/>
		    </c:when>
		    <c:otherwise>
		      <c:forEach items="${CATEGORIES}" var="category">
		        <c:if test="${!empty CATEGORY_CLASSES[category]}">
		          <div class="heading"><c:out value="${category}"/></div>
		          <div class="body">
		            <c:set var="classes" value="${CATEGORY_CLASSES[category]}"/>
		            <c:forEach items="${classes}" var="classname" varStatus="status">
		              <a href="<html:rewrite page="/queryClassSelect.do"/>?action=<fmt:message key="button.selectClass"/>&amp;className=${classname}" title="<c:out value="${classDescriptions[classname]}"/>">
		                ${classname}</a><c:if test="${!status.last}">,</c:if>
		            </c:forEach>
		            <c:if test="${!empty CATEGORY_TEMPLATES[category]}">
		              <br/><span class="smallnote"><fmt:message key="begin.or"/> <html:link action="/templates" paramId="category" paramName="category"><fmt:message key="begin.related.templates"/></html:link></span>
		            </c:if>
		          </div>
		          <im:vspacer height="5"/>
		        </c:if>
		      </c:forEach>
		    </c:otherwise>
		  </c:choose>
</div>

</div>


<!-- /dataCategories -->
