<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- gbrowseDisplayer.jsp -->

<c:if test="${((!empty object.chromosomeLocation && !empty object.chromosome)
                || className == 'Chromosome') && className != 'ChromosomeBand'}">

<div id="gBrowse" class="feature">

  <h3><fmt:message key="sequenceFeature.GBrowse.message"/></h3>

  <c:set var="type" value="${className}s"/>

  <c:if test="${className == 'MRNA' || className == 'Transcript'
              || className == 'Pseudogene'}">
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

  <c:if test="${className == 'MRNA' || className == 'Transcript'}">
    <c:set var="name" value="MRNA:${name}"/>
  </c:if>

  <c:if test="${className == 'Chromosome'}">
    <c:set var="name" value="${object.organism.genus}_${object.organism.species}_chr_${object.primaryIdentifier}"/>
  </c:if>

  <c:if test="${className == 'CDS'}">
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

</c:if>
<!-- /gbrowseDisplayer.jsp -->