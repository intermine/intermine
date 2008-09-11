<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- quickSearch.jsp -->
<c:set var="ids">
  <fmt:bundle basename="model"><fmt:message key="model.template.example"/></fmt:bundle>
</c:set>
<c:set var="tpls">
  <fmt:bundle basename="model"><fmt:message key="model.bag.example"/></fmt:bundle>
</c:set>
<c:set var="bgs">
  <fmt:bundle basename="model"><fmt:message key="model.quickSearch.example"/></fmt:bundle>
</c:set>

<script type="text/javascript" src="js/browse.js"></script>
<script type="text/javascript">
function updateExample(i) {
  if (i==1) {
     $('quickSearchInput').value = '<c:out value="${ids}"/>';
  } else if (i==2) {
     $('quickSearchInput').value = '<c:out value="${tpls}"/>';
  } else {
     $('quickSearchInput').value = '<c:out value="${bgs}"/>';
  }
  $('quickSearchInput').style.color = '#666';
  $('quickSearchInput').style.fontStyle = 'italic';
}
function clearElement(e) {
   var value =document.getElementById('quickSearchInput').value;
   if( value == '<c:out value="${ids}"/>' || value == '<c:out value="${tpls}"/>' || value == '<c:out value="${bgs}"/>') {
      e.value = "";
      $('quickSearchInput').style.color = '#000';
      $('quickSearchInput').style.fontStyle = 'normal';
   }
}

</script>
<tiles:importAttribute name="menuItem" ignore="true"/>
<html:form action="/quickSearchAction" style="display:inline;">
  <fmt:message key="header.search.pre"/>
  <select name="quickSearchType" id="quickSearchType" onchange="updateExample(selectedIndex);" style="font-size:1em;">
  <option value="ids" selected>Identifiers</option>
  <option value="tpls" <c:if test="${quickSearchType=='tpls'}">selected</c:if>>Templates&nbsp;&nbsp;</option>
  <option value="bgs" <c:if test="${quickSearchType=='bgs'}">selected</c:if>>Lists</option>
  </select>
<fmt:message key="header.search.mid"/>
<input style="width:260px;color:#666;font-style:italic;font-size:1em" type="text" id="quickSearchInput" name="value" value="<c:choose><c:when test="${quickSearchType=='tpls'}"><c:out value="${tpls}"/></c:when><c:when test="${quickSearchType=='bgs'}"><c:out value="${bgs}"/></c:when><c:otherwise><c:out value="${ids}"/></c:otherwise></c:choose>" onFocus="clearElement(this);" />
<html:submit><fmt:message key="header.search.button"/></html:submit>
</html:form>
<!-- /quickSearch.jsp -->