<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
    prefix="str"%>

<!--  modMineSearch.jsp -->

<link rel="stylesheet" href="model/css/modmine_search.css" type="text/css" media="screen" title="no title" charset="utf-8">

<tiles:importAttribute />

<html:xhtml />

<div class="modMineSearch">

  <h2>Search modMine</h2>
  <p><i>Search for modENCODE submissions by metadata</i></p>
    <html:form action="/modMineSearchAction" focus="searchTerm">
      <!-- <input style="" id="modMineSearchInput" name="value" type="text" focus = "value" class="modMineSearchBox">-->
      <html:text property="searchTerm"/>     
      <html:submit>Search</html:submit>
    </html:form>
    <div class="examples">
    <ul>
    <li>Antibody names: PolII, H3K4me1, CP190</li>
    <li>Experiment types: ChIP-chip, RNA-seq</li>
    <li>Use "" to search for a phrase, "Chromatin binding"</li>
    <li>Combine terms with AND, fly AND embryo</li>
    </ul>
    </div>
</div>

<!-- /modMineSearch.jsp -->
