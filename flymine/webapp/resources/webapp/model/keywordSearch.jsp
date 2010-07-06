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

<link rel="stylesheet" href="model/css/modmine_search.css" type="text/css" media="screen" title="no title" charset="utf-8">

<tiles:importAttribute />

<html:xhtml />

<div class="modMineSearch">

  <h2>Search modMine</h2>
  <p><i>Search for modENCODE submissions by metadata</i></p>
    <html:form action="/keywordSearchAction" focus="searchTerm">
      <!-- <input style="" id="modMineSearchInput" name="value" type="text" focus = "value" class="modMineSearchBox">-->
      <html:text property="searchTerm" style="width: 350px;" />     
      <html:submit>Search</html:submit>
    </html:form>
    <div class="examples">
    <ul>
    <li>
    	All fields will be searched, for example:
    	antibody names (e.g. <i>'PolII</i>', <i>'H3K4me1</i>', <i>'CP190</i>'),
    	lab names (e.g. <i>'Reinke</i>', <i>'Snyder'</i>),
    	data types (e.g. <i>'UTR'</i>, <i>'bindingsite'</i>)
    </li>
    <li>Boolean search is supported: e.g. <i>'fly AND embryo'</i> to combine terms, <i>'fly AND NOT embryo'</i> to exclude one</li>
    </ul>
    </div>
</div>

<!-- /keywordSearch.jsp -->
