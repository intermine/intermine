<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str"%>

<!-- featuresOverlaps.jsp -->

<html:xhtml />

<style type="text/css">



<c:if test="${empty pageSize}">
    <c:set var="pageSize" value="10"/>
</c:if>

<c:set var="initValue" value="0"/>
<c:if test="${empty currentUniqueId}">
    <c:set var="currentUniqueId" value="${initValue}" scope="application"/>
</c:if>
<c:set var="tableContainerId" value="_submission-features_${currentUniqueId}" scope="request"/>
<c:set var="currentUniqueId" value="${currentUniqueId + 1}" scope="application"/>

<tiles:importAttribute />


input.query {
    -moz-background-clip: border;
    -moz-background-origin: padding;
    -moz-background-size: auto auto;
    background-attachment: scroll;
    background-color: #EEEEEE;
    background-position: 0 0;
    background-repeat: repeat;
    border-bottom-color: #FFFFFF;
    border-bottom-style: solid;
    border-bottom-width: 1px;
    border-left-color-ltr-source: physical;
    border-left-color-rtl-source: physical;
    border-left-color-value: #DDDDDD;
    border-left-style-ltr-source: physical;
    border-left-style-rtl-source: physical;
    border-left-style-value: solid;
    border-left-width-ltr-source: physical;
    border-left-width-rtl-source: physical;
    border-left-width-value: 1px;
    border-right-color-ltr-source: physical;
    border-right-color-rtl-source: physical;
    border-right-color-value: #FFFFFF;
    border-right-style-ltr-source: physical;
    border-right-style-rtl-source: physical;
    border-right-style-value: solid;
    border-right-width-ltr-source: physical;
    border-right-width-rtl-source: physical;
    border-right-width-value: 1px;
    border-top-color: #DDDDDD;
    border-top-style: solid;
    border-top-width: 1px;
    color: #333333;
    display: inline-block;
    font-size: 12px;
    font-style: italic;
    padding-bottom: 2px;
    padding-left: 2px;
    padding-right: 5px;
}

input.query:hover {
    background-color: #FFFFFF;
    color: #000000;
}

img.tinyQuestionMark {
  padding-bottom:4px;
  padding-left:0px;
}

</style>
<script type="text/javascript">

    jQuery('#modENCODECategory').addClass('feature');

</script>

<%-- OVERLAPPING GENES

For some features is not reasonable to look for overlapping genes, list of
relevant ones compiled by rachel.
        TFBindingSite
        BindingSite
        InsulatorBindingSite
        ProteinBindingSite
        HistoneBindingSite
        TranscriptionEndSite
        PolyASite
        SL1AcceptorSite
        SL2AcceptorSite

        FivePrimeUTR
        ThreePrimeUTR
        ThreePrimeUST
        ThreePrimeRST

        CopyNumberVariation
        TSS
        PolyASignalSequence
        --%>


    <h3>Find overlapping or nearby features</h3>
    <table>
    <tbody>
        <tr>
            <td>
            <html:form action="/featuresOverlapsAction" method="post">
                  Find
                  <html:select styleId="typeSelector" property="overlapFindType">
                            <html:option value="Gene">Genes</html:option>
                            <html:option value="Exon">Exons</html:option>
                            <html:option value="Intron">Introns</html:option>
                            <html:option value="IntergenicRegion">IntergenicRegions</html:option>
                          </html:select>

                 with a flanking region of

            <!-- insert slider -->
            <html:hidden styleId="distance" property="distance" value="0" />

            <html:hidden styleId="givenFt" property="overlapFeatureType" value="${givenFeatureType}" />
            <html:hidden styleId="givenBag" property="featuresList" value="${featuresList}" />

            <tiles:insert name="submissionOverlapsNonLinearSlider.jsp">
               <tiles:put name="sliderIdentifier" value="distance-slider" />
               <tiles:put name="defaultValue" value="0" />
            </tiles:insert>

            <html:select styleId="typeSelector" property="direction">
            <html:option value="bothways">both ways</html:option>
              <html:option value="upstream">upstream</html:option>
              <html:option value="downstream">downstream</html:option>
            </html:select>

            overlapping the ${givenFeatureType}s of this list.

                <html:submit property="overlaps" styleClass="query">Show Results</html:submit>
            </html:form>
            </td>
        </tr>
    </tbody>
</table>


<!-- /featuresOverlaps.jsp -->
