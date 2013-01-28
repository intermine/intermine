<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- gbrowseDisplayer.jsp -->

<c:set var="object" value="${reportObject.object}"/>

<c:choose>
<c:when test="${!empty object.chromosomeLocation && !empty object.chromosome && object.organism.taxonId==10090}">

<div id="gBrowse" class="feature basic-table">
  <h3><fmt:message key="sequenceFeature.GBrowse.message"/></h3>

  <c:set var="loc" value="${object.chromosomeLocation}" />  
  <c:set var="name" value="${loc.locatedOn.primaryIdentifier}:${loc.start}..${loc.end}" />
  <c:set var="label0" value="MGI_Genome_Features" />
  <c:set var="label1" value="MGI_Genome_Features-MGI_NCBI-MGI_VEGA-MGI_ENSEMBL" />

  <c:choose>
  <c:when test="${WEB_PROPERTIES['gbrowse.database.source'] != null}">
    <div class="loading">
      <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?start=${loc.start};stop=${loc.end};ref=${loc.locatedOn.primaryIdentifier};label=${label1}"></html:link>
    </div>
	<script type="text/javascript">
	  jQuery(document).ready(function() {
	      var img = new Image();
	      // wrap our new image in jQuery
	      jQuery(img)
	        // once the image has loaded, execute this code
	        .load(function() {
	          // 'remove' loading
	          jQuery("#gBrowse div").removeClass('loading');
	          // attach image
	          jQuery('#gBrowse a').html(this);
	        })
	        .error(function() {
	          // 'remove' loading
	          jQuery("#gBrowse div").removeClass('loading');
	          // notify the user that the image could not be loaded
	          jQuery('#gBrowse').addClass('warning').append(jQuery('</p>', { 'text': 'There was a problem rendering the displayer, image could not be fetched.' }));
	        })
	        // set the attributes of the image
	        .attr('src', "${WEB_PROPERTIES['gbrowse_image.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?t=${label0};name=${name};width=600")
	        .attr('style', 'border:1px solid #000;')
	        .attr('title', 'GBrowse');
	  });
	</script>
  </c:when>
  <c:otherwise>
   	<p>There was a problem rendering the displayer, check: <code>WEB_PROPERTIES['gbrowse.database.source']</code>.</p>
	<script type="text/javascript">
		jQuery('#gBrowse').addClass('warning');
	</script>
  </c:otherwise>
  </c:choose>
</div>
</c:when>
</c:choose>
<!-- /gbrowseDisplayer.jsp -->
