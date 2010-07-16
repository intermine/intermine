<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- modmineGBrowse.jsp -->
<%--TODO check all this cases, with list of possible types from toronto --%>
<%--
<c:if test="${((!empty object.chromosomeLocation && !empty object.chromosome)
                || cld.unqualifiedName == 'Chromosome')
                && cld.unqualifiedName != 'Exon'
                && cld.unqualifiedName != 'CDS'}">
--%>
<c:if test="${((!empty object.chromosomeLocation && !empty object.chromosome)
                || cld.unqualifiedName == 'Chromosome')}">


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

<c:set var="offset" value="${(end-start)/10}"/>
<c:set var="istart" value="${start-offset}"/>
<c:set var="iend" value="${end+offset}"/>


<%-- in case there are >1 submissions associated TODO: check if the + is ok (or - like in labels?)--%>
<c:forEach items="${object.submissions}" var="sub" varStatus="status">
<c:choose>
<c:when test="${status.first}">
     <c:set var="ds" value="${sub.dCCid}" /> 
</c:when>
<c:otherwise>
     <c:set var="ds" value="${ds}-${sub.dCCid}" /> 
</c:otherwise>
</c:choose>
      </c:forEach>

  <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${gbrowseSource}/?start=${start};end=${end};ref=${ref};label=Genes;ds=${ds};width=750">
    <div>
      modENCODE genome browser view (GBrowse):
    </div>
    <c:if test="${cld.unqualifiedName != 'Chromosome'}">
        <html:img style="border: 1px solid black" 
        src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${gbrowseSource}/?start=${istart};end=${iend};ref=${ref};ds=${ds};width=400;b=1" title="GBrowse"/>
</c:if>
  </html:link>
<br>

</c:if>

<!-- /modmineGBrowse.jsp -->
