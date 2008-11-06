<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- modmineGBrowse.jsp -->
<fmt:setBundle basename="model"/>

<c:if test="${((!empty object.chromosomeLocation && !empty object.chromosome) 
                || cld.unqualifiedName == 'Chromosome')
                && cld.unqualifiedName != 'Exon'
                && cld.unqualifiedName != 'CDS'}">

  <c:set var="type" value="${cld.unqualifiedName}s"/>

  <c:if test="${cld.unqualifiedName == 'MRNA' || cld.unqualifiedName == 'Transcript'
              || cld.unqualifiedName == 'Pseudogene'}">
    <c:set var="type" value="Genes"/>
  </c:if>

  <c:set var="label" value="${type}"/>

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

  <c:if test="${cld.unqualifiedName == 'CDS'}">
    <%-- special case show genes instead of CDSs --%>
    <c:set var="name" value="${object.gene.primaryIdentifier}"/>
    <c:set var="type" value="${type}+CDSs"/>
    <c:set var="label" value="${label}-CDSs"/>
  </c:if>

  <c:choose>
    <c:when test="${object.organism.taxonId == 6239}">
      <c:set var="gbrowseSource" value="worm"/>
    </c:when>
    <c:otherwise>
      <c:set var="gbrowseSource" value="fly"/>
    </c:otherwise>
  </c:choose>

  <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${gbrowseSource}?source=${gbrowseSource};type=${label};name=${name};width=750">
    <div>
      modENCODE genome browser view (GBrowse):
    </div>
    <c:if test="${cld.unqualifiedName != 'Chromosome'}">
      <div>
        <html:img style="border: 1px solid black" src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${gbrowseSource}?source=${gbrowseSource};type=${type};name=${name};width=400;b=1" title="GBrowse"/>
      </div>
    </c:if>
  </html:link>
</c:if>
<!-- /modmineGBrowse.jsp -->
