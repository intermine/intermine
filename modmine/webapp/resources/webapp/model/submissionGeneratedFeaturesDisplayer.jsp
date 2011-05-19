<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
  prefix="str"%>

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

<h3>
  Features
</h3>

<c:choose>
<c:when test="${fn:length(featureCounts) ge 1}">
<div>
  <table cellpadding="0" cellspacing="0" border="0" class="table" width="100%">
      <tr>
        <th colspan="" style="padding-left: 6px;" class="theme-5-background">Feature type</th>
        <th colspan="" style="padding-left: 6px;" class="theme-5-background" align="middle">View data</th>
        <th colspan="4" style="padding-left: 6px;" class="theme-5-background" align="left">Export</th>
        <th colspan="" style="padding-left: 6px;" class="theme-5-background" align="left">Action</th>
      </tr>
      <c:forEach items="${featureCounts}" var="fc" varStatus="status">
        <c:if test='${fc.key != "Chromosome"}'>
          <tr>
            <td width="15%">
              ${fc.key}

                <c:forEach items="${expFeatDescription}" var="fdes" varStatus="fdes_status">
                    <c:if test="${fn:substringBefore(fdes.key, '+') == object.experiment.name && fn:substringAfter(fdes.key, '+') == fc.key}">
                      <img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?" title="${fdes.value }">
                    </c:if>
                </c:forEach>
            </td>

            <td align="middle" width="8%">

                <%-- TMP PATCH until data is corrected. it should be (otherwise)
                <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">${fc.value} </html:link>
              --%>
                 <c:set var="sub" value="${object}"></c:set>
                 <c:choose>
                 <c:when test="${sub.dCCid == '2753'}">
                    <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">4230</html:link>
                 </c:when>
                 <c:when test="${sub.dCCid == '2754'}">
                    <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">4477</html:link>
                 </c:when>
                 <c:when test="${sub.dCCid == '2755'}">
                    <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">5159</html:link>
                 </c:when>
                 <c:when test="${sub.dCCid == '2783'}">
                    <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">7029</html:link>
                 </c:when>
                 <c:when test="${sub.dCCid == '2979'}">
                    <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">5726</html:link>
                 </c:when>
                  <c:when test="${sub.dCCid == '3247'}">
                     <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">540</html:link>
                  </c:when>
                  <c:when test="${sub.dCCid == '3251'}">
                    <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">4366</html:link>
                  </c:when>
                  <c:when test="${sub.dCCid == '3253'}">
                   <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">6587</html:link>
                  </c:when>

                 <c:otherwise>
                    <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">${fc.value} </html:link>
                 </c:otherwise>
                 </c:choose>
                 <%-- END patch --%>

            </td>
            <td align="left" width="5%">
              <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&format=tab&submission=${object.dCCid}&feature=${fc.key}" title="Tab-delimited values">TAB</html:link>
            </td>
            <td align="left" width="5%">
              <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&format=csv&submission=${object.dCCid}&feature=${fc.key}" title="Comma-separated values">CSV</html:link>
            </td>
            <td align="left" width="5%">
              <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&format=gff3&submission=${object.dCCid}&feature=${fc.key}" title="GFF3">GFF3</html:link>
            </td>
            <td align="left" width="10%">
              <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&format=sequence&submission=${object.dCCid}&feature=${fc.key}" title="FASTA">SEQUENCE</html:link>
            </td>
            <td align="left">
              <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=whatever&action=list&format=sequence&submission=${object.dCCid}&feature=${fc.key}" title="Create a list of ${fc.key}">create LIST</html:link>
            </td>
          </tr>
        </c:if>
      </c:forEach>
      <!-- end submission loop -->
  </table>
</div>
<br>
<div>
  <html:form action="/submissionOverlapsAction" method="post">
    <html:hidden property="submissionTitle" value="${object.title}" />
    <html:hidden property="submissionId" value="${object.id}" />

    <div  style="padding-bottom:10px;">
        <h4 style="padding-bottom:8px;">Find overlapping features:</h4>

        Find
            <html:select styleId="typeSelector" property="overlapFindType">
                      <html:option value="Gene">Genes</html:option>
                      <html:option value="Exon">Exons</html:option>
                      <html:option value="Intron">Introns</html:option>
                      <html:option value="IntergenicRegion">IntergenicRegions</html:option>
                    </html:select>
        which overlap the
            <html:select styleId="typeSelector" property="overlapFeatureType">
               <c:forEach items="${featureCounts}" var="fc" varStatus="status">
                 <c:if test='${fc.key != "Chromosome"}'>
                    <html:option value="${fc.key}">${fc.key}</html:option>
                 </c:if>
               </c:forEach>
            </html:select>
        features generated by this submission.

        <html:submit property="overlaps" styleClass="query">Show Results</html:submit>
    </div>
    <div style="padding: 10px 0 15px 0;">
        <h4 style="padding-bottom:8px;">Find nearby genes:</h4>

        Find Genes that have
               <html:select styleId="typeSelector" property="flankingFeatureType">
                 <c:forEach items="${featureCounts}" var="fc" varStatus="status">
                   <c:if test='${fc.key != "Chromosome"}'>
                     <html:option value="${fc.key}">${fc.key}</html:option>
                   </c:if>
                 </c:forEach>
               </html:select>
        features generated by this submission located within
               <html:select styleId="typeSelector" property="distance">
                  <html:option value="0.5kb">.5kb</html:option>
                  <html:option value="1.0kb">1kb</html:option>
                  <html:option value="2.0kb">2kb</html:option>
                  <html:option value="5.0kb">5kb</html:option>
                  <html:option value="10.0kb">10kb</html:option>
               </html:select>

                <html:select styleId="typeSelector" property="direction">
                  <html:option value="upstream">upstream</html:option>
                  <html:option value="downstream">downstream</html:option>
                  <html:option value="bothways">both ways</html:option>
                </html:select>

            <html:submit property="flanking" styleClass="query">Show Results</html:submit>
    </div>
  </html:form>
</div>

  </c:when>
  <c:otherwise>
     <h4>This submission has no generated features.</h4>
    <br/>
  </c:otherwise>
</c:choose>

<!-- /submissionGeneratedFeaturesDisplayer.jsp -->