<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- jBrowse.jsp -->


<c:if test="${((!empty object.chromosomeLocation && !empty object.chromosome)
                || cld.unqualifiedName == 'Chromosome') && cld.unqualifiedName != 'ChromosomeBand'}">

  <div class="geneInformation">

    <h3 class="overlapping">Genome Browser</h3>

    <strong style="color:red;">NOTE - this is a demo version of the viewer, an updated version is coming soon</strong>

    <c:set var="baseUrl" value="http://jbrowse.org/ucsc/hg19"/>
    <c:set var="chr" value="${object.chromosomeLocation.locatedOn.primaryIdentifier}"/>
    <c:set var="padding" value="${10}"/>
    <c:set var="offset" value="${fn:substringBefore((object.length * 0.1), '.')}"/>

    <c:set var="start" value="${object.chromosomeLocation.start - offset}"/>
    <c:set var="end" value="${object.chromosomeLocation.end + offset}"/>
    <c:set var="tracks" value="DNA,knownGene,snp131"/>
    <c:set var="jbLink" value="${baseUrl}?loc=chr${chr}:${start}..${end}&tracks=${tracks}"/>

    <p>Click and drag to move.  Drag and drop tracks from left menu.
    <a href="${jbLink}" target="jbrowse">Centre on ${object.primaryIdentifier}</a></p>
    <iframe src="${jbLink}" name="jbrowse" width="98%" height="300px" style="border : 1px solid #dfdfdf; padding : 1%;"></iframe>
    <p><a href="http://jbrowse.org">JBrowse</a> genome browser</p>
</div>

</c:if>
<!-- /jBrowse.jsp -->