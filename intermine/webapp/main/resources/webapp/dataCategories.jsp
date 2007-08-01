<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- dataCategories -->
<html:xhtml/>

<div class="body">
  <div id="leftCol">
      <div id="pageDesc" class="pageDesc"><p><fmt:message key="dataCategories.intro"/></p></div>
	     <div class="actionArea">
	     <h2>Actions:</h2>
           <a href="/sources.html"><fmt:message key="dataCategories.action1"/></a><BR/> 
           <html:link action="/templates">
             <fmt:message key="dataCategories.action2"/>
           </html:link>
        </div>
   </div>
<div id="rightCol">
   <im:roundbox titleKey="dataCategories.title" stylename="welcome">
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
		 </im:roundbox>
</div>
</div>
</div>
<script type="text/javascript">
	Nifty("div#pageDesc","big");
</script>
<!-- /dataCategories -->
