<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml/>

<div style="float:left;width:45%;">
	<im:roundbox title="Templates" stylename="welcome">
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
            <tiles:put name="showSearchBox" value="false"/>
      </tiles:insert>
      <p><html:link action="/templates">View all ${templateCount} templates...</html:link></p>
	</im:roundbox>	

	<im:roundbox title="Data Categories" stylename="welcome" >
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
		  
		  <p><a href="sources.shtml">View all 1,234 datasources...</a></p>
	</im:roundbox>	
	<im:roundbox title="QueryBuilder" stylename="welcome">
	        <div class="body">
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
	<im:roundbox title="Lists" stylename="welcome" >
	   <p>Lists of identifiers can be used in queries and can also be analysed
       (most frequent GO terms, expression data, etc...).</p>
      <p><u>Have a look at some Lists we've made for you:</u></p>
      <tiles:insert name="webSearchableList.tile">
            <tiles:put name="limit" value="3"/>
             <%-- bag or template? --%>
            <tiles:put name="type" value="bag"/>
            <%-- user or global --%>
            <tiles:put name="scope" value="global"/>
            <tiles:put name="tags" value="im:frontpage"/>
            <tiles:put name="showSearchBox" value="false"/>
      </tiles:insert> 

      <p><u>...or create your own List:</u></p>
      <html:link action="/mymine.do?page=bags"><img src="images/go_to_bag_page.png" align="center" width="317" height="80" alt="Go To Bag Page" border="0"></html:link>
	</im:roundbox>

	<im:roundbox title="Welcome" stylename="welcome" >
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
    <a href="tour_1.html" target="_blank"  onclick="javascript:window.open('tour_1.html','_manual','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">Take a tour</a>
    </im:roundbox>
	
</div>


<!-- /begin.jsp -->
