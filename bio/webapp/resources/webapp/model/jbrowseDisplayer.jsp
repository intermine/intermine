<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- jBrowseDisplayer.jsp -->

<c:if test="${((!empty reportObject.object.chromosomeLocation && !empty reportObject.object.chromosome)
                || cld.unqualifiedName == 'Chromosome') && cld.unqualifiedName != 'ChromosomeBand'}">

  <div class="geneInformation">

    <h3 class="overlapping">Genome Browser</h3>

    <c:set var="padding" value="${10}"/>

    <c:set var="jbLink" value="${jbrowseInstall}?data=${dataLoc}&loc=${segment}&tracks=${tracks}&nav=0&tracklist=0"/>

    <p>Click and drag the browser to move the view.  Drag and drop tracks from left menu into the main
	   panel to see the data. Clicking on individual features to open a report page for that feature.
	    <br/>
	    <strong>*</strong> denotes SNPs that are mapped to multiple genome position.
    <a href="${jbLink}" target="jbrowse">Centre on ${reportObject.object.symbol}</a></p>
    <iframe name="jbrowse" height="300px" width="98%" style="border: 1px solid #dfdfdf; padding: 1%" src="${jbLink}">
    </iframe>
    <p>
        <a href="javascript:;" onclick="jQuery('iframe').css({height: '600px'});">
            Expand viewer
        </a>
        &nbsp;
        (more about <a href="http://jbrowse.org">JBrowse</a>)
    </p>
</div>

</c:if>
<!-- /jBrowseDisplayer.jsp -->
