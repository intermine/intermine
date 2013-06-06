<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- gbrowseDisplayer.jsp -->

<c:set var="object" value="${reportObject.object}"/>

<c:choose>
<c:when test="${!empty object.chromosomeLocation && !empty object.chromosome && object.organism.taxonId==10090}">

<div id="gBrowse" class="feature basic-table">
  <h3><fmt:message key="sequenceFeature.GBrowse.message"/></h3>

  <c:set var="loc" value="${object.chromosomeLocation}" />  
  <c:set var="chrom" value="${loc.locatedOn.primaryIdentifier}" />
  <c:if test="${fn:startsWith(chrom, '0')}">
	<c:set var="chrom" value="${fn:substring(chrom, 1, 2)}" />
  </c:if>
  <c:set var="name" value="${chrom}:${loc.start}..${loc.end}" />

  <c:choose>
  <c:when test="${WEB_PROPERTIES['gbrowse.database.source'] != null}">
    <div class="loading">
      <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?start=${loc.start};stop=${loc.end};ref=${chrom};label=${WEB_PROPERTIES['gbrowse.tracks']}"></html:link>
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
	        .attr('src', "${WEB_PROPERTIES['gbrowse_image.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?t=${WEB_PROPERTIES['gbrowse_image.tracks']};name=${name};width=600")
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
