<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
    prefix="str"%>

<!-- keywordSearch.jsp -->

<link rel="stylesheet" href="css/keywordSearch.css" type="text/css" media="screen" title="no title" charset="utf-8">

<tiles:importAttribute />

<html:xhtml />

<div class="keywordSearch">
  <h2>
     <c:if test="${!empty searchBag}">
         List Search: ${searchBag}
     </c:if>
     <c:if test="${empty searchBag}">
         Keyword Search
     </c:if>
  </h2>
  <p><i>Search <c:if test="${!empty searchBag}"><b>the list "${searchBag}"</b></c:if><c:if test="${empty searchBag}">our database</c:if> by keyword</i></p>
    <form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
        <c:forEach items="${searchFacetValues}" var="facetValue">
            <!-- modmine special: category is taken care of later -->
            <c:if test="${facetValue.key != 'Category'}">
                <input type="hidden" name="facet_${facetValue.key}" value="${facetValue.value}" />
            </c:if>
        </c:forEach>
        <c:if test="${!empty searchBag}">
            <input type="hidden" name="searchBag" value="${searchBag}" />
        </c:if>
        <div>
          <c:if test="${!empty searchTerm || !empty searchFacetValues}">
	          <a href="<c:url value="/keywordSearchResults.do"><c:param name="searchBag" value="${searchBag}" /></c:url>">
	             &laquo; Back to index</a>
          </c:if>
		  <input type="text" name="searchTerm" value="<c:out value="${searchTerm}"></c:out>" style="width: 350px;" /> 
		  
		  <!-- modmine special: only search submission -> change output format -->
          <select name="facet_Category">
              <option value="<c:if test="${searchFacetValues['Category'] != null && searchFacetValues['Category'] != 'Submission'}">${searchFacetValues['Category']}</c:if>">
                any object
              </option>
              <option value="Submission"<c:if test="${searchFacetValues['Category'] == 'Submission'}"> selected="selected"</c:if>>
                only submissions
              </option>
          </select>  
		  
		  <!-- modmine special: no "search entire db" button because that would conflict with Category select above -->
          <input type="submit" name="searchSubmitRestricted"
                value="Search" />
        </div>
    </form>
    
    <div class="examples">
	    <ul>
            <li>
                Search all of modMine.  Enter identifiers, names or keywords for
				genes, pathways, authors, ontology terms, etc.  (e.g. <i>eve</i>, <i>embryo</i>,
				<i>zen</i>, <i>allele</i>)
            </li>
            <li>
                Use <i>OR</i> to search for either of two terms (e.g. <i>fly OR drosophila</i>)
                or quotation marks to search for phrases  (e.g. <i>"dna binding"</i>)
            </li>
            <li>
                Boolean search syntax is supported: e.g. <i>dros*</i> for partial matches or <i>fly AND NOT embryo</i> to exclude a term
            </li>
	    </ul>
    </div>
</div>

<!-- /keywordSearch.jsp -->
