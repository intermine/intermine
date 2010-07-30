<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
    prefix="str"%>

<link rel="stylesheet" href="model/css/keywordSearch.css" type="text/css" media="screen" title="no title" charset="utf-8">

<tiles:importAttribute />

<html:xhtml />

<div class="body">

<tiles:insert name="keywordSearch.tile"/>

<c:if test="${!empty searchTerm}">
<div class="keywordSearchResults">

<div>
    Search Term: <b><c:out value="${searchTerm}"/></b>
</div>
<c:if test="${!empty searchCategory || !empty searchOrganism}">
	<div>
	    Search restricted to:
	    <c:if test="${!empty searchCategory}">
	       <b>type = '<c:out value="${searchCategory}"/>'</b>
	        <c:if test="${!empty searchOrganism}">
	           and
	        </c:if>
	    </c:if>
        <c:if test="${!empty searchOrganism}">
            <b>organism = '<c:out value="${searchOrganism}"/>'</b>
        </c:if>
	</div>
</c:if>

<c:if test="${searchTotalHits == 0}">
    <div style="margin-top: 1em; text-align: center;">
        Your search returned no results!
        Please try one of these steps to broaden your search:
        <ul>
            <li>Add an asterisk (*) to the end of a word to search for partial matches</li>
            <li>Search for synonyms by writing <i>(word1 OR word2)</i></li>
        </ul>
    </div>
</c:if>
<c:if test="${searchTotalHits > 0}">
	<div style="margin-top: 1em;">
	   <c:out value="Showing results ${searchOffset + 1} to ${fn:length(searchResults) + searchOffset} out of ${searchTotalHits}"/>
	   <c:if test="${searchTotalHits > fn:length(searchResults)}">
	        <div class="pages">
	            <c:choose>
		            <c:when test="${searchOffset > 0}">                     
			            <a href="<c:url value="/keywordSearchResults.do"><c:param name="searchTerm" value="${searchTerm}" /><c:param name="searchBag" value="${searchBag}" /><c:param name="searchCategory" value="${searchCategory}" /><c:param name="searchOrganism" value="${searchOrganism}" /><c:param name="searchOffset" value="0" /></c:url>">
			               &lt;&lt;&nbsp;First
			            </a>
			            &nbsp;&nbsp;      
		                <a href="<c:url value="/keywordSearchResults.do"><c:param name="searchTerm" value="${searchTerm}" /><c:param name="searchBag" value="${searchBag}" /><c:param name="searchCategory" value="${searchCategory}" /><c:param name="searchOrganism" value="${searchOrganism}" /><c:param name="searchOffset" value="${searchOffset - searchPerPage}" /></c:url>">
		                   &lt;&nbsp;Previous
		                </a>
		            </c:when>
		            <c:otherwise>
		               &lt;&lt;&nbsp;First
	                   &nbsp;&nbsp; 
		               &lt;&nbsp;Previous
		            </c:otherwise>
	            </c:choose>
	            &nbsp;|&nbsp;  
	            <c:choose>
		            <c:when test="${searchOffset + searchPerPage < searchTotalHits}">                     
		                <a href="<c:url value="/keywordSearchResults.do"><c:param name="searchTerm" value="${searchTerm}" /><c:param name="searchBag" value="${searchBag}" /><c:param name="searchCategory" value="${searchCategory}" /><c:param name="searchOrganism" value="${searchOrganism}" /><c:param name="searchOffset" value="${searchOffset + searchPerPage}" /></c:url>">
		                   Next&nbsp;&gt;
		                </a>
		                &nbsp;&nbsp;   
		                <a href="<c:url value="/keywordSearchResults.do"><c:param name="searchTerm" value="${searchTerm}" /><c:param name="searchBag" value="${searchBag}" /><c:param name="searchCategory" value="${searchCategory}" /><c:param name="searchOrganism" value="${searchOrganism}" /><c:param name="searchOffset" value="${searchTotalHits - searchTotalHits % searchPerPage}" /></c:url>">
		                   Last&nbsp;&gt;&gt;
		                </a>
		            </c:when>
		            <c:otherwise>
		               Next&nbsp;&gt;
	                   &nbsp;&nbsp; 
		               Last&nbsp;&gt;&gt;
		            </c:otherwise>
	            </c:choose>
	        </div>
	   </c:if>
	</div>
	
	<div style="clear: both;">
	
	<div class="facets">
	    <c:if test="${categoryFacets != null && !empty categoryFacets}">
	        <c:choose>
		        <c:when test="${searchCategory != ''}">
		            <div class="facetHeader">Type: <i>${searchCategory}</i></div>
	                <div class="facetContents">
		            <a href="<c:url value="/keywordSearchResults.do">
	                       <c:param name="searchTerm" value="${searchTerm}" />
                           <c:param name="searchBag" value="${searchBag}" />
	                       <c:param name="searchOrganism" value="${searchOrganism}" />
	                </c:url>">                           
		                &laquo; show all
		            </a>
		            </div>
		        </c:when>
		        <c:otherwise>
			        <div class="facetHeader">Hits by Type</div>
			        <div class="facetContents">
			        <ol>
			            <c:forEach items="${categoryFacets}" var="facet">
			                <li>
			                    <a href="<c:url value="/keywordSearchResults.do">
			                           <c:param name="searchTerm" value="${searchTerm}" />
                                       <c:param name="searchBag" value="${searchBag}" />
			                           <c:param name="searchCategory" value="${facet.value}" />
		                               <c:param name="searchOrganism" value="${searchOrganism}" />
	                            </c:url>" title="Click to only show '<c:out value="${facet.value}" />'">
		                           <c:out value="${facet.value}" />:
			                       <c:out value="${facet.facetValueHitCount}"></c:out>
			                    </a>
			                </li>
			            </c:forEach>
			        </ol>
			        </div>
		        </c:otherwise>
	        </c:choose>
		</c:if>
		
	    <c:if test="${organismFacets != null && !empty organismFacets}">
	        <c:choose>
	            <c:when test="${searchOrganism != ''}">
	                <div class="facetHeader">Organism: <i>${searchOrganism}</i></div>
	                <div class="facetContents">
	                <a href="<c:url value="/keywordSearchResults.do">
	                       <c:param name="searchTerm" value="${searchTerm}" />
                           <c:param name="searchBag" value="${searchBag}" />
	                       <c:param name="searchCategory" value="${searchCategory}" />
	                </c:url>">                           
	                    &laquo; show all
	                </a>
	                </div>
	            </c:when>
	            <c:otherwise>
	                <div class="facetHeader">Hits by Organism</div>
	                <div class="facetContents">
	                <ol>
	                    <c:forEach items="${organismFacets}" var="facet">
	                        <li>
	                            <a href="<c:url value="/keywordSearchResults.do">
	                                   <c:param name="searchTerm" value="${searchTerm}" />
                                       <c:param name="searchBag" value="${searchBag}" />
	                                   <c:param name="searchOrganism" value="${facet.value}" />
	                                   <c:param name="searchCategory" value="${searchCategory}" />
	                            </c:url>" title="Click to only show '<c:out value="${facet.value}" />'">
	                               <c:out value="${facet.value}" />:
	                               <c:out value="${facet.facetValueHitCount}"></c:out>
	                            </a>
	                        </li>
	                    </c:forEach>
	                </ol>
	                </div>
	            </c:otherwise>
	        </c:choose>
	    </c:if>
	</div>
	
	<div class="resultTableContainer">
		<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
		<tr>
		    <th>Type</th>
		    <th>Details</th>
		    <th>Search score</th>
		</tr>
		<c:forEach items="${searchResults}" var="searchResult">
		  <tr class="keywordSearchResult">
		      <td><c:out value="${searchResult.type}"></c:out></td>
		      <td>
		          <div class="objectKeys">
		          <html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${searchResult.id}">
		          <c:forEach items="${searchResult.keyFields}" var="field" varStatus="status">
		            <c:set var="fieldConfig" value="${searchResult.fieldConfigs[field]}"/>
		            <span title="<c:out value="${field}"/>" class="objectKey">
		               <c:choose>    
		               <%-- print each field configured for this object --%>
		                <c:when test="${!empty fieldConfig && !empty fieldConfig.displayer}">
		                  <c:set var="interMineObject" value="${searchResult.object}" scope="request"/>
		                  <span class="value">
		                    <tiles:insert page="${fieldConfig.displayer}">
		                      <tiles:put name="expr" value="${fieldConfig.fieldExpr}" />
		                    </tiles:insert>
		                  </span>
		                </c:when>
		                <c:when test="${!empty fieldConfig && !empty fieldConfig.fieldExpr}">
		                  <c:set var="outVal" value="${searchResult.fieldValues[fieldConfig.fieldExpr]}"/>
		                  <span class="value">${outVal}</span>
		                  <c:if test="${empty outVal}">
		                    -
		                  </c:if>
		                </c:when>
		                <c:otherwise>
		                  -
		                </c:otherwise>
		              </c:choose>
		            </span>
		            <c:if test="${! status.last }">
		                <span class="objectKey">|</span>
		            </c:if>
		          </c:forEach>
		          </html:link>
		          </div>
		          
			      <%-- print each field configured for this object --%>
		          <c:forEach items="${searchResult.additionalFields}" var="field">
		            <c:set var="fieldConfig" value="${searchResult.fieldConfigs[field]}"/>
			        <div class="objectField">
			           <c:choose>	
			           <%-- print each field configured for this object --%>
			            <c:when test="${!empty fieldConfig && !empty fieldConfig.displayer}">
                          <span class="objectFieldName"><c:out value="${field}"/>:</span>
                       
			              <c:set var="interMineObject" value="${searchResult.object}" scope="request"/>
			              <span class="value">
			                <tiles:insert page="${fieldConfig.displayer}">
			                  <tiles:put name="expr" value="${fieldConfig.fieldExpr}" />
			                </tiles:insert>
			              </span>
			            </c:when>
			            
			            <c:when test="${!empty fieldConfig && !empty fieldConfig.fieldExpr}">
                          <c:set var="outVal" value="${searchResult.fieldValues[fieldConfig.fieldExpr]}"/>
			              <c:if test="${!empty outVal}">
                            <span class="objectFieldName"><c:out value="${field}"/>:</span>
                          </c:if>
                       
			              <span class="value" style="font-weight: bold;">${outVal}</span>
			              <c:if test="${empty outVal}">
			                &nbsp;<%--for IE--%>
			              </c:if>
			            </c:when>
			            <c:otherwise>
			              &nbsp;<%--for IE--%>
			            </c:otherwise>
			          </c:choose>
			        </div>
			      </c:forEach>
		      </td>      
		      <td><img height="10" width="${searchResult.points * 5}" src="images/heat${searchResult.points}.gif" alt="${searchResult.points}/10" title="${searchResult.points}/10"/></td>
		</tr>
		</c:forEach>
		</table>
	</div>
	
	</div>
</c:if>

</div>
</c:if>

</div>