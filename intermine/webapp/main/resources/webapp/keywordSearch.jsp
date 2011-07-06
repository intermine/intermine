<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/imutil.tld" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
    prefix="str"%>

<im:debug message="START keywordSearch.jsp"/>

<link rel="stylesheet" href="css/keywordSearch.css" type="text/css" media="screen" title="no title" charset="utf-8">

<tiles:importAttribute />

<html:xhtml />

<div class="keywordSearch">
    <div class="form">
        <h2>Search <c:if test="${!empty searchBag}">the list "${searchBag}"</c:if><c:if test="${empty searchBag}">our database</c:if> by keyword</h2>
        <form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
            <c:forEach items="${searchFacetValues}" var="facetValue">
                <input type="hidden" name="facet_${facetValue.key}" value="${facetValue.value}" />
            </c:forEach>
            <c:if test="${!empty searchBag}">
                <input type="hidden" name="searchBag" value="${searchBag}" />
            </c:if>
            <input type="text" id="keywordSearch" name="searchTerm" value="<c:out value="${searchTerm}"></c:out>" />
            <input type="submit" name="searchSubmit" value="Search" />
            <c:if test="${!empty searchTerm || !empty searchFacetValues}">
            <br />
                <a href="<c:url value="/keywordSearchResults.do"><c:param name="searchBag" value="${searchBag}" /></c:url>">
                    Back to index
                </a>
            </c:if>
            <c:if test="${!empty searchFacetValues}">
                <span>- or -</span>
                <input type="submit" name="searchSubmitRestricted" value="Search (with current restrictions)" />
            </c:if>
        </form>
    </div>
    <div class="examples">
        <h4>Examples</h4>
        <c:choose>
          <c:when test="${empty WEB_PROPERTIES['keywordSearch.text']}">
            <ul>
                <li>
                    Search this entire website. Enter <strong>identifiers</strong>, <strong>names</strong> or <strong>keywords</strong> for
                    genes, pathways, authors, ontology terms, etc.  (e.g. <i>eve</i>, <i>embryo</i>, <i>zen</i>, <i>allele</i>)
                </li>
                <li>
                    Use <i>OR</i> to search for either of two terms (e.g. <i>fly OR drosophila</i>) or quotation marks to search for phrases  (e.g. <i>"dna binding"</i>).
                </li>
                <li>
                    <strong>Boolean search syntax</strong> is supported: e.g. <i>dros*</i> for partial matches or <i>fly AND NOT embryo</i> to exclude a term
                </li>
            </ul>
          </c:when>
          <c:otherwise>
            ${WEB_PROPERTIES['keywordSearch.text']}
          </c:otherwise>
        </c:choose>
    </div>
    <div style="clear:both;"></div>
</div>

<im:debug message="END keywordSearch.jsp"/>
