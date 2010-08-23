<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- quickSearch.jsp -->
<c:set var="ids" value="${WEB_PROPERTIES['quickSearch.identifiers']}"/>

<script type="text/javascript">
function updateExample(i) {
  if (isUpdatedByUser(jQuery('#quickSearchInput').val())) {
   return;
  }
  jQuery('#quickSearchInput').val('${ids}');
  jQuery('#quickSearchInput').css("color", "#666");
  jQuery('#quickSearchInput').css("fontStyle","italic");
}

function clearElement(e) {
   var value =document.getElementById('quickSearchInput').searchTerm;
   if(value == '${ids}') {
      e.value = "";
      jQuery('#quickSearchInput').css("color", "#000");
      jQuery('#quickSearchInput').css("fontStyle","normal");
   }
}

</script>
<form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get" style="display:inline;">
<fmt:message key="header.search"/>
<input style="width:150px;color:#666;font-style:italic;font-size:1em" type="text" id="quickSearchInput" name="searchTerm" value="${ids}" onFocus="clearElement(this);" />
<input type="submit" name="searchSubmit" value="GO" />
</form>
<!-- /quickSearch.jsp -->
