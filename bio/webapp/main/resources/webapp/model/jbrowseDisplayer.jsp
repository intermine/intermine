<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- jBrowseDisplayer.jsp -->

<c:if test="${((!empty reportObject.object.chromosomeLocation && !empty reportObject.object.chromosome)
                || cld.unqualifiedName == 'Chromosome') && cld.unqualifiedName != 'ChromosomeBand'}">

  <div class="geneInformation">

    <h3 class="overlapping">Genome Browser</h3>

    <c:set var="baseUrl" value="/jbrowse"/>
    <c:set var="chr" value="${reportObject.object.chromosomeLocation.locatedOn.primaryIdentifier}"/>
    <c:set var="padding" value="${10}"/>
    <c:set var="offset" value="${fn:substringBefore((reportObject.object.length * 0.1), '.')}"/>

    <c:set var="start" value="${reportObject.object.chromosomeLocation.start - offset}"/>
    <c:set var="end" value="${reportObject.object.chromosomeLocation.end + offset}"/>
    <c:choose>
        <c:when test="${reportObject.type == 'SNP'}">
            <c:set var="tracks" value="Gene Track,SNPs"/>
        </c:when>
        <c:otherwise>
            <c:set var="tracks" value="Gene Track,mRNA Track"/>
        </c:otherwise>
    </c:choose>
    <c:set var="genus" value="${reportObject.object.organism.genus}"/>
    <c:set var="species" value="${reportObject.object.organism.species}"/>

    <c:set var="jbLink" value="${baseUrl}?loc=${genus}_${species}_chr_${chr}:${start}..${end}&tracks=${tracks}"/>

    <p>Click and drag the browser to move the view.  Drag and drop tracks from left menu into the main
	   panel to see the data. Clicking on individual features to open a report page for that feature.
	    <br/>
	    <strong>*</strong> denotes SNPs that are mapped to multiple genome position.
    <a href="${jbLink}" target="jbrowse">Centre on ${reportObject.object.symbol}</a></p>
	<iframe name="jbrowse" height="300px" width="98%" style="border: 1px solid #dfdfdf; padding: 1%" src="${jbLink}"></iframe>
    <p><a href="javascript:;" onclick="jQuery('iframe').css({height: '600px'});">Expand viewer</a>&nbsp;(more about <a href="http://jbrowse.org">JBrowse</a>)</p>
</div>

<!--

<script type="text/javascript">
/* <![CDATA[ */
var bookmarkCallback = function(brwsr) {
    return window.location.protocol
       + "//" + window.location.host
       + window.location.pathname
       + "?loc=" + brwsr.visibleRegion()
       + "&tracks=" + brwsr.visibleTracks();
    }
var dataDir = window.location.protocol
       + "//" + window.location.host
	   + "/jbrowse/data";
var b = new Browser({
    containerID: "GenomeBrowser",
    refSeqs: refSeqs,
    trackData: trackInfo,
    defaultTracks: "DNA,gene,mRNA,noncodingRNA",
    location: "chr${chr}:${start}..${end}",
    tracks: "${tracks}",
    bookmark: bookmarkCallback,
    dataRoot: dataDir
});
/* ]]> */
</script>

-->

</c:if>
<!-- /jBrowseDisplayer.jsp -->
