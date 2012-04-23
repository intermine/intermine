<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str"%>

<!-- submissionGeneratedFeaturesDisplayer.jsp -->

<html:xhtml />

<style type="text/css">

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


<c:choose>
<c:when test="${fn:length(featureCounts) ge 1}">
<div class="collection-table">
<h3>
Features
</h3>
  <table cellpadding="5" cellspacing="5" border="0" class="resultstables" width="100%">
  <thead>
  <tr>
  <td>Feature type</td>
  <td align="right">View data</td>
  <td colspan="4" >Export</td>
  <td>Action</td>
  </tr>
</thead>
<tbody>
<%--
     <tr>
        <th>Feature type</th>
        <th>View data</th>
        <th colspan="4" >Export</th>
        <th>Action</th>
      </tr>
--%>
      <c:forEach items="${featureCounts}" var="fc" varStatus="status">
        <c:if test='${fc.key != "Chromosome"}'>
          <tr>
            <td>
              ${fc.key}

                <c:forEach items="${expFeatDescription}" var="fdes" varStatus="fdes_status">
                    <c:if test="${fn:substringBefore(fdes.key, '+') == object.experiment.name && fn:substringAfter(fdes.key, '+') == fc.key}">
                      <img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?" title="${fdes.value }">
                    </c:if>
                </c:forEach>
            </td>

            <td align="middle" style="padding-left: 6px;">
            <a href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${object.dCCid}&feature=${fc.key}" style="text-decoration: none;">${fc.value} </a>

            </td>
            <td align="left" style="padding-left: 6px;">
              <a href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&format=tab&submission=${object.dCCid}&feature=${fc.key}" title="Tab-delimited values" style="text-decoration: none;">TAB</a>
            </td>
            <td align="left" style="padding-left: 6px;" >
              <a href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&format=csv&submission=${object.dCCid}&feature=${fc.key}" title="Comma-separated values" style="text-decoration: none;">CSV</a>
            </td>

            <c:set var="isUnloc" value="false"></c:set>
            <c:forEach items="${unlocatedFeat}" var="uft" varStatus="uft_status">
                <c:if test="${uft.key == object.dCCid}">
                    <c:forEach items="${uft.value}" var="uftv" varStatus="uftv_status">
                        <c:if test="${uftv == fc.key}">
                            <c:set var="isUnloc" value="true">
                            </c:set>
                        </c:if>
                    </c:forEach>
                </c:if>
            </c:forEach>
            <c:choose>
            <c:when test="${isUnloc == 'true' }">
              <td><i>GFF3</i><td><i>SEQUENCE</i>
            </c:when>
          <c:otherwise>
            <td align="left" style="padding-left: 6px;">
              <a href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&format=gff3&submission=${object.dCCid}&feature=${fc.key}" title="GFF3" style="text-decoration: none;">GFF3</a>
              (<a href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&format=gff3&submission=${object.dCCid}&feature=${fc.key}&UCSC" title="GFF3 for UCSC" style="text-decoration: none;">for UCSC</a>)
            </td>
            <td align="left" style="padding-left: 6px;">
              <a href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&format=sequence&submission=${object.dCCid}&feature=${fc.key}" title="FASTA" style="text-decoration: none;">SEQUENCE</a>
            </td>
            </c:otherwise>
            </c:choose>


            <td align="left" style="padding-left: 6px;" >
              <a href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=whatever&action=list&format=sequence&submission=${object.dCCid}&feature=${fc.key}" title="Create a list of ${fc.key}" style="text-decoration: none;">create LIST</a>
            </td>
          </tr>
        </c:if>




<c:forEach items="${subFeatEL}" var="subEL" varStatus="subEL_status">
<c:if test="${subEL.key == object.dCCid}" >
<c:forEach items="${subEL.value}" var="subELF" varStatus="subELF_status">
<c:if test="${subELF.key == fc.key}" >
<tr><td><i>Expression Levels</i>
<td>
<i>
<html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=subEL&action=results&submission=${object.dCCid}&feature=${fc.key}">${subELF.value} </html:link>
</i>
</td>
<td align="left" style="padding-left: 6px;">
<i>
<a href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=subEL&action=export&format=tab&submission=${object.dCCid}&feature=${fc.key}" title="Tab-delimited values" style="text-decoration: none;">TAB</a>
</i>
</td>
<td align="left" style="padding-left: 6px;" >
<i>
<a href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=subEL&action=export&format=csv&submission=${object.dCCid}&feature=${fc.key}" title="Comma-separated values" style="text-decoration: none;">CSV</a>
</i>
  </td>

</tr>

</c:if>
</c:forEach>
</c:if>
</c:forEach>

        <%-- SOURCE FILE --%>
        <c:forEach items="${subFeatFileSource}" var="subFFS" varStatus="subFFS_status">
          <c:if test="${subFFS.key == object.dCCid}" >
          <c:forEach items="${subFFS.value}" var="FFS" varStatus="FFS_status">
          <c:if test="${FFS.key == fc.key}" >
            <c:forEach items="${FFS.value}" var="FS" varStatus="FS_status">


            <c:if test="${FS.value != fc.value}" >
            <c:if test="${FS_status.first}" >
            <tr><td><i>Source files:</i><td><td><td><td><td><td>
            </c:if>
            <tr><td align=right><i>
            ${fn:replace(FS.key, "_", " ")}
            </i></td>
            <td align="middle">
            <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${object.dCCid}&feature=${fc.key}&file=${FS.key}">${FS.value} </html:link>
<td>
            <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${object.dCCid}&feature=${fc.key}&file=${FS.key}&format=tab">TAB</html:link>
<td>
            <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${object.dCCid}&feature=${fc.key}&file=${FS.key}&format=csv">CSV</html:link>
<%--
            <c:set var="isUnloc" value="false"></c:set>
            <c:forEach items="${unlocatedFeat}" var="uft" varStatus="uft_status">
                <c:if test="${uft.key == object.dCCid}">
                    <c:forEach items="${uft.value}" var="uftv" varStatus="uftv_status">
                        <c:if test="${uftv == fc.key}">
                            <c:set var="isUnloc" value="true">
                            </c:set>
                        </c:if>
                    </c:forEach>
                </c:if>
            </c:forEach>
--%>

            <c:choose>
            <c:when test="${isUnloc == 'true' }">
              <td><i>GFF3</i><td><i>SEQUENCE</i>
            </c:when>
          <c:otherwise>
         <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${object.dCCid}&feature=${fc.key}&file=${FS.key}&format=gff3">GFF3</html:link>
        (<html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${object.dCCid}&feature=${fc.key}&file=${FS.key}&format=gff3&UCSC">for UCSC</html:link>)
         <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${object.dCCid}&feature=${fc.key}&file=${FS.key}&format=sequence">SEQUENCE</html:link>

               </c:otherwise>
            </c:choose>
               <td>
               <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=whatever&action=list&submission=${object.dCCid}&feature=${fc.key}&file=${FS.key}">create&nbsp;LIST</html:link>
          </c:if>
          </c:forEach>
          </c:if>
        </c:forEach>
        </c:if>
      </c:forEach>


        </c:forEach>

      <!-- end submission loop -->
  </table>

<%-- OVERLAPPING GENES

For some features is not reasonable to look for overlapping genes, list of
relevant ones compiled by rachel. TODO add check for unlocated features?
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

<c:forEach items="${featureCounts}" var="fc" varStatus="status">
    <c:if test="${fn:endsWith(fc.key, 'Site') || fn:contains(fc.key, 'Prime')
           || fc.key == 'CopyNumberVariation' || fc.key == 'TSS'
           || fc.key == 'PolyASignalSequence'}">
    <c:set var="gffeat" value ='true' />
    </c:if>
</c:forEach>

<c:if test="${gffeat == 'true'}">


    <h3>Find overlapping or nearby features</h3>
    <table>
    <tbody>
        <tr>
            <td>
            <html:form action="/submissionOverlapsAction" method="post">
                <html:hidden property="submissionTitle" value="${object.title}" />
                <html:hidden property="submissionId" value="${object.id}" />
                <html:hidden property="submissionDCCid" value="${object.dCCid}" />
                  Find
                  <html:select styleId="typeSelector" property="overlapFindType">
                            <html:option value="Gene">Genes</html:option>
                            <html:option value="Exon">Exons</html:option>
                            <html:option value="Intron">Introns</html:option>
                            <html:option value="IntergenicRegion">IntergenicRegions</html:option>
                          </html:select>

                 with a flanking region of

<!--
              <html:select styleId="typeSelector" property="distance">
              <html:option value="0">0</html:option>
              <html:option value="0.5kb">.5kb</html:option>
              <html:option value="1.0kb">1kb</html:option>
              <html:option value="2.0kb">2kb</html:option>
              <html:option value="5.0kb">5kb</html:option>
              <html:option value="10.0kb">10kb</html:option>
           </html:select>
-->
            <!-- insert slider -->
            <html:hidden styleId="distance" property="distance" value="0" />

            <tiles:insert name="submissionOverlapsNonLinearSlider.jsp">
               <tiles:put name="sliderIdentifier" value="distance-slider" />
               <tiles:put name="defaultValue" value="0" />
            </tiles:insert>

            <html:select styleId="typeSelector" property="direction">
            <html:option value="bothways">both ways</html:option>
              <html:option value="upstream">upstream</html:option>
              <html:option value="downstream">downstream</html:option>
            </html:select>

            overlapping the

            <html:select styleId="typeSelector" property="overlapFeatureType">
                    <c:forEach items="${featureCounts}" var="fc" varStatus="status">
                    <c:if test="${fn:endsWith(fc.key, 'Site') || fn:contains(fc.key, 'Prime')
                        || fc.key == 'CopyNumberVariation' || fc.key == 'TSS'
                        || fc.key == 'PolyASignalSequence'}">
                         <html:option value="${fc.key}">${fc.key}s</html:option>
                      </c:if>
                    </c:forEach>
                  </html:select>
                  generated by this submission.
                <html:submit property="overlaps" styleClass="query">Show Results</html:submit>
            </html:form>
            </td>
        </tr>
    </tbody>
</table>



    </c:if>
  </div>
  </c:when>

  <c:otherwise>
     <h4>This submission has no generated features.</h4>
    <br/>
  </c:otherwise>
</c:choose>

<!-- /submissionGeneratedFeaturesDisplayer.jsp -->