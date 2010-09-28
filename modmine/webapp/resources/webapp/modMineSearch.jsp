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
      <html:text property="searchTerm" style="width: 350px;" />
      <html:submit>Search</html:submit>
    </html:form>
    <div class="examples">
      All fields will be searched, for example:
      antibody names (e.g. <i>PolII</i>, <i>H3K4me1</i>, <i>CP190</i>),
      lab names (e.g. <i>Reinke</i>, <i>Snyder</i>),
      data types (e.g. <i>UTR</i>, <i>bindingsite</i>). The search supports:
      <p></p>
      - Boolean operators: e.g. <i>fly AND embryo</i> to combine terms, <i>fly AND NOT embryo</i> to exclude one
    <br> - Partial matches: e.g. <i>dros*</i>
    </br>
    <br> - Phrase matches: e.g. <i>"dna binding site"
    </br>
    <!--
    <ul>
    <li> - Boolean operators: e.g. <i>fly AND embryo</i> to combine terms, <i>fly AND NOT embryo</i> to exclude one</li>
    </li>
    <li> - Partial matches: e.g. <i>dros*</i></li>
    <li> - Phrase matches: e.g. <i>"dna binding site"</li>
    </ul>
    -->
    </div>
</div>

<!-- /modMineSearch.jsp -->
