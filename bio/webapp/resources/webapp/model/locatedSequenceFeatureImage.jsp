<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- locatedSequenceFeatureImage.jsp -->
<fmt:setBundle basename="model"/>


<c:if test="${(!empty object.chromosomeLocation || cld.unqualifiedName == 'Chromosome')
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
  
  <c:if test="${type == 'ArtificialDeletions'}">
    <c:set var="type" value="${type}+TransposableElementInsertionSites"/>
    <c:set var="label" value="${label}-TransposableElementInsertionSites"/>
  </c:if>
  
  <c:if test="${type != 'Genes'}">
    <c:set var="type" value="${type}+Genes"/>
    <c:set var="label" value="${label}-Genes"/>
  </c:if>
  
  <c:set var="name" value="FlyMineInternalID_${object.id}"/>

  <c:if test="${cld.unqualifiedName == 'MRNA' || cld.unqualifiedName == 'Transcript'}">
    <c:set var="name" value="mRNA:${name}"/>
  </c:if>

  <c:if test="${cld.unqualifiedName == 'Chromosome'}">
    <c:set var="name" value="${object.organism.genus}_${object.organism.species}_chr_${object.identifier}"/>
  </c:if>

  <c:if test="${cld.unqualifiedName == 'CDS'}">
    <%-- special case CDS FlyMineInternalIDs aren't in the GBrowse database,
         so use gene ID instead, but add the CDS track --%>
    <c:set var="name" value="FlyMineInternalID_${object.gene.id}"/>
    <c:set var="type" value="${type}+CDSs"/>
    <c:set var="label" value="${label}-CDSs"/>
  </c:if>

  <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?source=${WEB_PROPERTIES['gbrowse.database.source']};label=${label};name=${name};width=750" target="_new">
    <div>
      <fmt:message key="locatedSequenceFeature.GBrowse.message"/>
    </div>
    <c:if test="${cld.unqualifiedName != 'Chromosome'}">
      <div>
        <html:img style="border: 1px solid black" src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?source=${WEB_PROPERTIES['gbrowse.database.source']};type=${type};name=${name};width=400;b=1" title="GBrowse"/>
      </div>
    </c:if>
  </html:link>
</c:if>
<!-- /locatedSequenceFeatureImage.jsp -->
