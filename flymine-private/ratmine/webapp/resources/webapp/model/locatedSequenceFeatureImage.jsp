<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- locatedSequenceFeatureImage.jsp -->
<!-- modified from original - Andrew Vallejos -->


<c:if test="${((!empty object.chromosomeLocation && !empty object.chromosome)
                || cld.unqualifiedName == 'Chromosome')
            && object.organism.abbreviation != 'MM' && object.organism.abbreviation != 'MD'
            && object.organism.abbreviation != 'RN' && cld.unqualifiedName != 'ChromosomeBand'}">

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

  <c:set var="name" value="${object.primaryIdentifier}"/>

  <c:if test="${cld.unqualifiedName == 'MRNA' || cld.unqualifiedName == 'Transcript'}">
    <c:set var="name" value="MRNA:${name}"/>
  </c:if>

  <c:if test="${cld.unqualifiedName == 'Chromosome'}">
    <c:set var="name" value="${object.organism.genus}_${object.organism.species}_chr_${object.primaryIdentifier}"/>
  </c:if>

    <c:if test="${type == 'Genes'}">
	  <c:set var="primaryIdentifier" value="${object.primaryIdentifier}"/>
	  <c:set var="symbol" value="Sequence:${object.symbol}"/>
	  <c:set var="type" value="RGD_curated_genes"/>
	</c:if>

	<c:if test="${type == 'Qtls'}">
	  <c:set var="primaryIdentifier" value="${object.primaryIdentifier}"/>
	  <c:set var="symbol" value="${object.symbol}"/>
	  <c:set var="type" value="QTLS"/>
	  <c:set var="options" value="QTLS+1"/>
	</c:if>

<!-- type = ${type} -->

	<html:link
href="http://rgd.mcw.edu/gb/gbrowse/rgd_904/?name=RGD${primaryIdentifier};label=${type}" target="_blank">
      <div>
        <html:img style="border: 1px solid black" src="http://www.rgd.mcw.edu/gb/gbrowse_img/rgd_904/?type=${type};name=${symbol};width=500;options=${options}" title="GBrowse"/>
      </div>
	</html:link>
</c:if>
<!-- /locatedSequenceFeatureImage.jsp -->
