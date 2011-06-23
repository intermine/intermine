<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- gbrowseDisplayer.jsp -->

<c:if test="${((!empty object.chromosomeLocation && !empty object.chromosome)
                || cld.unqualifiedName == 'Chromosome') && cld.unqualifiedName != 'ChromosomeBand'}">

<div id="gBrowse" class="feature">

  <h3><fmt:message key="sequenceFeature.GBrowse.message"/></h3>

  <c:set var="type" value="${cld.unqualifiedName}s"/>

  <c:if test="${cld.unqualifiedName == 'MRNA' || cld.unqualifiedName == 'Transcript'
              || cld.unqualifiedName == 'Pseudogene'}">
    <c:set var="type" value="Genes"/>
  </c:if>

  <c:set var="label" value="${type}"/>

  <c:if test="${type == 'TilingPathSpans'}">
    <c:set var="type" value="${type}+ReversePrimers+ForwardPrimers+PCRProducts"/>
    <c:set var="label" value="${label}-ReversePrimers-ForwardPrimers-PCRProducts"/>
  </c:if>

  <c:if test="${type == 'PCRProducts'}">
    <c:set var="type" value="${type}+ReversePrimers+ForwardPrimers+TilingPathSpans"/>
    <c:set var="label" value="${label}-ReversePrimers-ForwardPrimers-TilingPathSpans"/>
  </c:if>

  <c:if test="${type == 'ReversePrimers'}">
    <c:set var="type" value="${type}+ForwardPrimers+TilingPathSpans+PCRProducts"/>
    <c:set var="label" value="${label}-ForwardPrimers-TilingPathSpans-PCRProducts"/>
  </c:if>

  <c:if test="${type == 'ForwardPrimers'}">
    <c:set var="type" value="${type}+ReversePrimers+TilingPathSpans+PCRProducts"/>
    <c:set var="label" value="${label}-ReversePrimers-TilingPathSpans-PCRProducts"/>
  </c:if>

  <c:if test="${type == 'ChromosomalDeletions'}">
    <c:set var="type" value="${type}+TransposableElementInsertionSites"/>
    <c:set var="label" value="${label}-TransposableElementInsertionSites"/>
  </c:if>

  <c:if test="${type != 'Genes'}">
    <c:set var="type" value="${type}+Genes"/>
    <c:set var="label" value="${label}-Genes"/>
  </c:if>

  <c:set var="name" value="${object.primaryIdentifier}"/>

  <c:if test="${cld.unqualifiedName == 'MRNA' || cld.unqualifiedName == 'Transcript'}">
    <c:set var="name" value="MRNA:${name}"/>
  </c:if>

  <c:if test="${cld.unqualifiedName == 'Chromosome'}">
    <c:set var="name" value="${object.organism.genus}_${object.organism.species}_chr_${object.primaryIdentifier}"/>
  </c:if>

  <c:if test="${cld.unqualifiedName == 'CDS'}">
    <%-- special case CDS FlyMineInternalIDs aren't in the GBrowse database,
         so use gene ID instead, but add the CDS track --%>
    <c:set var="name" value="${object.gene.primaryIdentifier}"/>
    <c:set var="type" value="${type}+CDSs"/>
    <c:set var="label" value="${label}-CDSs"/>
  </c:if>
  <c:choose>
  <c:when test="${WEB_PROPERTIES['gbrowse.database.source'] != null}">
    <div align="center">
      <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?source=${WEB_PROPERTIES['gbrowse.database.source']};label=${label};name=${name};width=750"></html:link>
    </div>
  </c:when>
  <c:otherwise>
    <p class="gbrowse-not-configured"><i>GBrowse is not configured in web.properties</i></p>
  </c:otherwise>
  </c:choose>

<br/>
</div>

<c:if test="${cld.unqualifiedName != 'Chromosome'}">
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
          jQuery('#gBrowse a').html("The genome browser could not be loaded.")
          .attr('style', 'color:#ff0000;font-weight:bold;');
        })
        // set the attributes of the image
        .attr('src', "${WEB_PROPERTIES['gbrowse_image.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?source=${WEB_PROPERTIES['gbrowse.database.source']};type=${type};name=${name};width=600;b=1")
        .attr('style', 'border:1px solid #000;')
        .attr('title', 'GBrowse');
  });
</script>
</c:if>

</c:if>
<!-- /gbrowseDisplayer.jsp -->