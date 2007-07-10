<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml/>

<div style="float:left;width:45%;">
	<im:roundbox title="Templates" color="roundcorner">
<p>Templates are predefined queries designed to perform a particular task. Each one has a description and a form to fill in. For example, there are templates to find GO annotation for a gene, to retrieve protein-protein interactions or protein structures.</p>
	   <tiles:insert name="webSearchableList.tile">
      	    <!-- optional -->
            <tiles:put name="limit" value="5"/>
            <!-- bag or template? -->
            <tiles:put name="type" value="template"/>
            <!-- user or global -->
            <tiles:put name="scope" value="global"/>
            <tiles:put name="tags" value="im:public"/>
            <tiles:put name="showDescriptions" value="false"/>
      </tiles:insert>
	</im:roundbox>	

	<im:roundbox title="Data Categories" color="roundcorner" >
	      	  <c:choose>
		    <c:when test="${!empty ASPECTS}">
		      <p><fmt:message key="begin.aspect.intro"/></p>
		      <tiles:insert page="/aspectIcons.jsp"/>
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
	<im:roundbox title="Custom Queries" color="roundcorner">
	        <div class="body">
	          <html:link action="/mymine.do?page=bags">
	            <fmt:message key="begin.upload.identifiers"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link><br/>
	          <html:link action="/classChooser">
	            <fmt:message key="begin.list.all.classes"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link><br/>
	          <html:link action="/tree">
	            <fmt:message key="begin.browse.model"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link><br/>
	          <html:link action="/importQueries?query_builder=yes">
	            <fmt:message key="begin.import.query"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link><br/>
	        </div>   
	</im:roundbox>
</div>

<div style="margin-left:50%;width:45%;">

	<im:roundbox title="bag list test" color="roundcorner" >
          
      <html:form action="/modifyBag">
        
        <tiles:insert name="wsBagTable.tile">
          <tiles:put name="limit" value="5"/>
          <tiles:put name="scope" value="global"/>
          <tiles:put name="makeCheckBoxes" value="true"/>
          <tiles:put name="showDescriptions" value="false"/>
        </tiles:insert>

</html:form>
        </im:roundbox>	

	<im:roundbox title="template list test" color="roundcorner" >
        
        <tiles:insert name="wsTemplateTable.tile">
          <tiles:put name="limit" value="5"/>
          <tiles:put name="scope" value="global"/>
          <tiles:put name="makeCheckBoxes" value="true"/>
          <tiles:put name="showDescriptions" value="false"/>
        </tiles:insert>
        </im:roundbox>	
</div>

<div style="margin-left:50%;width:45%;">
	<im:roundbox title="Bags" color="roundcorner" >
	   <tiles:insert name="bagFrontPage.tile" />
	</im:roundbox>

	<im:roundbox title="Welcome" color="roundcorner" >
	<p>
      FlyMine is an integrated database of genomic, expression and protein data for
      <i>Drosophila</i>, <i>Anopheles</i> and
      <i>C. elegans</i>.
      Integrating data makes it possible to run sophisticated data mining queries
      that span domains of biological knowledge.
    </p>
    
    <p>
      <a href="what.xml">What can I do with FlyMine?</a>
    </p>
    
    <p style="color:#c00;"> 
      This is release 8.0 of FlyMine.  See the <a href="release-notes.xml">release notes</a> to find out whats new.
    </p>

  	<tiles:insert name="tipWrapper.tile"/>
    <a href="tour_1.html" target="_blank"        onclick="javascript:window.open('tour_1.html','_manual','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">Take a tour</a>
    </im:roundbox>
	
</div>


<!-- /begin.jsp -->
