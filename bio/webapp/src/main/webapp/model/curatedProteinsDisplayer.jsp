<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- curatedProteinsDisplayer.jsp -->
<div id="curated-proteins-displayer" class="collection-table">

  <style>
  	#curated-proteins-displayer table tbody tr { display:none; }
  	#curated-proteins-displayer table tbody tr.swissprot { display:table-row; }
  </style>

  <div class="header">
    <h3>
    	<div class="right"><a href="#">Toggle Proteins from other Data Sets</a></div>
    	Proteins
    </h3>
  </div>

  <table>
    <thead>
      <tr>
        <th>Primary Identifier</th>
        <th>Primary Accession</th>
        <th>Organism</th>
        <th>Is Uniprot Canonical</th>
        <th>Is SwissProt Curate?</th>
        <th>Length</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="protein" items="${results}">
        <tr<c:if test="${protein.value.isSwissProtCurate}"> class="swissprot"</c:if>>
        	<td><html:link action="/report?id=${protein.value.id}">${protein.key}</html:link></td>
        	<td>${protein.value.primaryAccession}</td>
        	<td>${protein.value.organismName}</td>
        	<td>${protein.value.isUniprotCanonical}</td>
        	<td>${protein.value.isSwissProtCurate}</td>
        	<td>${protein.value.length}</td>
        </tr>
      </c:forEach>
    </tbody>
  </table>

  <div class="show-in-table">
    <html:link action="/collectionDetails?id=${reportObject.object.id}&amp;field=proteins">
      Show all in a table &raquo;
    </html:link>
  </div>

  <script type="text/javascript">
  (function() {
    var t = jQuery('#curated-proteins-displayer');
    t.find('h3 div.right a').click(function(e) {
    	t.find('table tbody tr').toggle();
    	e.preventDefault();
    });
  })();
  </script>
</div>
<!-- /curatedProteinsDisplayer.jsp -->