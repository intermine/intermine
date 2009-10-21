<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- modmineGBrowse.jsp -->
<%--TODO check all this cases, with list of possible types from toronto --%>
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


<c:set var="start" value="${object.chromosomeLocation.start}"/>
<c:set var="end" value="${object.chromosomeLocation.end}"/>
<c:set var="ref" value="${object.chromosome.primaryIdentifier}"/>

<%-- in case there are >1 submissions associated TODO: check if the + is ok (or - like in labels?)--%>
<c:forEach items="${object.submissions}" var="sub" varStatus="status">
<c:choose>
<c:when test="${status.first}">
     <c:set var="ds" value="${sub.dCCid}" /> 
</c:when>
<c:otherwise>
     <c:set var="ds" value="${ds}+${sub.dCCid}" /> 
</c:otherwise>
</c:choose>
      </c:forEach>

  <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${gbrowseSource}/?start=${start};end=${end};ref=${ref};ds=${ds};width=750">
    <div>
      modENCODE genome browser view (GBrowse):
    </div>
    <c:if test="${cld.unqualifiedName != 'Chromosome'}">
        <html:img style="border: 1px solid black" 
        src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${gbrowseSource}/?start=${start};end=${end};ref=${ref};type=${type};ds=${ds};width=400;b=1" title="GBrowse"/>
</c:if>
  </html:link>
<br>


<%--  THIS WORKS FOR ONLY A FEW FEATURES
  <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${gbrowseSource}/?name=${name};width=750">
    <div>
      modENCODE genome browser view (GBrowse):
    </div>
    <c:if test="${cld.unqualifiedName != 'Chromosome'}">
        <html:img style="border: 1px solid black" 
        src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${gbrowseSource}/?name=${name};width=400;b=1" title="GBrowse"/>
</c:if>
  </html:link>
<br>
<hr>
--%>



</c:if>

<!-- /modmineGBrowse.jsp -->
