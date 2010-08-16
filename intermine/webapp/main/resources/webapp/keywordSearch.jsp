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
  <h2>Keyword Search</h2>
  <p><i>Search our database by keyword</i></p>
    <form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
        <c:forEach items="${searchFacetValues}" var="facetValue">
            <input type="hidden" name="facet_${facetValue.key}" value="${facetValue.value}" />
        </c:forEach>
        <div>
          <c:if test="${!empty searchTerm || !empty searchFacetValues}">
	          <a href="<c:url value="/keywordSearchResults.do" />">
	             &laquo; Back to index</a>
          </c:if>
		  <input type="text" name="searchTerm" value="<c:out value="${searchTerm}"></c:out>" style="width: 350px;" /> 
          <c:if test="${!empty searchFacetValues}">
              <!-- <select name="searchKeepRestrictions">
                <option value="on">
                  <c:if test="${empty searchTerm}">
                    in this list
                  </c:if>
                  <c:if test="${!empty searchTerm}">
                        restricted to 
				        <c:forEach items="${searchFacetValues}" var="facetValue" varStatus="facetValueStatus">
				            ${facetValue.value}<c:if test="${!facetValueStatus.last}">,</c:if>
				        </c:forEach>
                  </c:if>
                </option>
                <option value="">
                    in entire database
                </option>
              </select>
              
              <input type="submit" name="searchSubmitRestricted"
                value="Search (only <c:forEach items="${searchFacetValues}" var="facetValue" varStatus="facetValueStatus">${facetValue.value}<c:if test="${!facetValueStatus.last}">, </c:if></c:forEach>)" />
               -->
              <input type="submit" name="searchSubmitRestricted"
                value="Search (with current restrictions)" />
          </c:if>
          <input type="submit" name="searchSubmit" value="Search entire database" />
		</div>
    </form>
    
    <div class="examples">
	    <ul>
            <li>
                Search all of FlyMine.  Enter identifiers, names or keywords for
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
