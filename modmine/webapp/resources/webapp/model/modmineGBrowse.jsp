<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- modmineGBrowse.jsp -->
<%--TODO check all this cases, with list of possible types from toronto --%>
<%--
<c:if test="${((!empty object.chromosomeLocation && !empty object.chromosome)
                || cld.unqualifiedName == 'Chromosome')
                && cld.unqualifiedName != 'Exon'
                && cld.unqualifiedName != 'CDS'}">
--%>

<tiles:importAttribute />

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

<c:set var="label" value="-"/>

<%-- in case there are >1 submissions associated TODO: check if the + is ok (or - like in labels?)--%>
<c:forEach items="${object.submissions}" var="sub" varStatus="status">


   <c:forEach items="${subTracks}" var="subTracks" varStatus="subt_status">
      <c:if test="${subTracks.key == sub.dCCid}">
      
        <c:forEach items="${subTracks.value}" var="track" varStatus="track_status">

<c:choose>
<c:when test="${track_status.first}">
     <c:set var="label" value="${track}" /> 
</c:when>
<c:otherwise>
     <c:set var="label" value="${label}-${track}" /> 
</c:otherwise>
</c:choose>

        ${subTracks.value}
        </c:forEach>             
      </c:if>
   </c:forEach>

<c:choose>
<c:when test="${status.first}">
     <c:set var="ds" value="${sub.dCCid}" /> 
</c:when>
<c:otherwise>
     <c:set var="ds" value="${ds}-${sub.dCCid}" /> 
</c:otherwise>
</c:choose>
</c:forEach>


<%-- display starts  --%>

<c:choose>
<c:when test="${fn:length(label) > 2 }">
<c:set var="link" value="${WEB_PROPERTIES['gbrowse.prefix']}/${gbrowseSource}/?start=${start};end=${end};ref=${ref};label=Genes;${label};width=750"></c:set>
</c:when>
<c:otherwise>
<c:set var="link" value="${WEB_PROPERTIES['gbrowse.prefix']}/${gbrowseSource}/?start=${start};end=${end};ref=${ref};ds=${ds};width=75"></c:set>
</c:otherwise>
</c:choose>

<%--
  <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${gbrowseSource}/?start=${start};end=${end};ref=${ref};ds=${ds};width=750">
    <div>
      modENCODE genome browser view (GBrowse):
    </div>
    <c:if test="${cld.unqualifiedName != 'Chromosome'}">
        <html:img style="border: 1px solid black" 
        src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${gbrowseSource}/?start=${istart};end=${iend};ref=${ref};ds=${ds};width=400;b=1" title="GBrowse"/>
</c:if>
<br>

<hr></hr>
--%>

    <div>
<html:link href="${link}">
      modENCODE genome browser view (GBrowse):

    <c:if test="${cld.unqualifiedName != 'Chromosome'}">
 <c:choose>
<c:when test="${fn:length(label) > 1 }">
  <html:img style="border: 1px solid black" 
  src="${WEB_PROPERTIES['gbrowse.prefix']}/${gbrowseSource}/?start=${start};end=${end};ref=${ref};label=Genes;${label};width=400;b=1" title="GBrowse"/>
</c:when>
<c:otherwise>
        <html:img style="border: 1px solid black" 
        src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${gbrowseSource}/?start=${istart};end=${iend};ref=${ref};ds=${ds};width=400;b=1" title="GBrowse"/>
</c:otherwise>
</c:choose>
</c:if>

</html:link>
    </div>

<%--
  </html:link>
--%>

<br>



</c:if>

<!-- /modmineGBrowse.jsp -->
