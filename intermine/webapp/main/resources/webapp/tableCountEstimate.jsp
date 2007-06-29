<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html:xhtml/>

<!-- tableCountEstimate.jsp -->

<tiles:useAttribute id="pagedTable" name="pagedTable" 
                    classname="org.intermine.web.logic.results.PagedTable"/>
<fmt:message key="results.pageinfo.rowrange">
  <fmt:param value="${resultsTable.startRow+1}"/>
  <fmt:param value="${resultsTable.endRow+1}"/>
</fmt:message>
<span class="resBar">&nbsp;|&nbsp;</span>
<span id="resultsCountEstimate" style="display: inline">
  <img src="images/spinner.gif"/>
  <fmt:message key="results.pageinfo.estimate"/>
  ${resultsTable.size}
  <im:helplink key="results.help.estimate"/>
</span>

<span id="resultsCountExact" style="display: none">
  <fmt:message key="results.pageinfo.exact"/>
  <span  style="display: inline" id="resultsCountExactSize"> </span>
</span>

<fmt:message key="results.pageinfo.exact" var="exactMessage"/>

<script language="JavaScript">
  <!--
function callback(results) {
    document.getElementById('resultsCountEstimate').style.display='none';
    document.getElementById('resultsCountExact').style.display='inline';
    var size = results[0][0];
    document.getElementById('resultsCountExactSize').innerHTML=size;
    document.resultsCountText = "${exactMessage} " + size;
    return true;
}

window.onload = function() {
    getResults(${qid}, ${POLL_REFRESH_SECONDS}*1000, callback);
}

window.status = '';
  //-->
</script>

<!-- /tableCountEstimate.jsp -->
