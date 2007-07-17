<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- templates.jsp -->
<html:xhtml/>

<div class="body">
<table padding="0px" margin="0px" width="100%">
  <tr>
    <td valign="top" width="30%">
      <div id="pageDesc"><p><fmt:message key="begin.templates"/></p><br><img src="images/templates_desc.gif" width="328" height="67" alt="Templates Desc"></div>
      <script type="text/javascript">
      	Nifty("div#pageDesc","big");
      </script>
      <div id="actionsLeft">
         <h2>Actions:</h2>
           <html:link action="/summariseAllTemplates" titleKey="begin.summariseAllTemplatesDesc">
             <fmt:message key="begin.summariseAllTemplates"/>
           </html:link>
         <BR/>
           <html:link action="/import" titleKey="begin.importTemplatesDesc">
             <fmt:message key="begin.importTemplates"/>
           </html:link>
      </div>
    </td>
    <td valign="top" width="70%">
    <div  class="webSearchable">
		<h2><fmt:message key="dataCategories.title"/></h2>
    
		<c:choose>
		    <c:when test="${!empty ASPECTS}">
		       <tiles:insert name="aspects.tile">
					<tiles:put name="iconSize" value="30"/>
				</tiles:insert>		      
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
    </td>
  </tr>
</table>
</div>
<!-- /templates.jsp -->
