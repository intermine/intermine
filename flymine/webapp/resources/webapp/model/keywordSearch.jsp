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
        <div>
		  <input type="text" name="searchTerm" value="<c:out value="${searchTerm}"></c:out>" style="width: 350px;" />  
		  <input type="submit" value="Search" />
		</div>
    </form>
    
    <div class="examples">
	    <ul>
            <li>
                Use <i>AND</i> to combine two terms (e.g. <i>fly AND embryo</i>)
                or quotations marks to search for phrases  (e.g. <i>"dna binding"</i>)
            </li>
            <li>
                Boolean search syntax is supported: e.g. <i>dros*</i> for partial matches or <i>fly AND NOT embryo</i> to exclude a term
            </li>
	    </ul>
    </div>
</div>

<!-- /keywordSearch.jsp -->
