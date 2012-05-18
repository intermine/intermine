<%--
  - Author: Fengyuan Hu
  - Created: 30-Mar-2012
  - Description: In this page, users have different options to constrain
                 their query for overlapping located sequence features with
                 the genomic regions they upload.
  --%>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!--  genomicRegionSearchOptionsBase.jsp -->

<html:xhtml />

<c:choose>
    <c:when test='${webData == "Chromosome location information is missing"}'>
        <div  class="altmessage" align="center" >
           <br>Chromsome location information is not available, region search is disabled.</br>
        </div>
    </c:when>
    <c:otherwise>

        <link type="text/css" rel="stylesheet" href="model/jquery_ui/css/smoothness/jquery-ui-1.8.13.custom.css"/>


        <script type="text/javascript">
            //liftOver url, set it before loading "genomic_region_search_options_metabolicmine.js"
            var liftOverUrl = "${WEB_PROPERTIES['genomicRegionSearch.liftOver.url']}";
            liftOverUrl = /\/$/.test(liftOverUrl)? liftOverUrl : liftOverUrl + "/";

            // webData must be defined in base jsp first, and customized page can make use of it.
            var webDataJSON = jQuery.parseJSON('${webData}');

            // genomic region examples read from web.properties
            var exampleSpans = "${WEB_PROPERTIES['genomicRegionSearch.defaultSpans']}";

            // Set value to textarea#pasteInput
            jQuery(document).ready(function () {
                if ('${galaxyIntervalData}') {
                    jQuery("#pasteInput").val('${galaxyIntervalData}');
                    switchInputs('paste','file');
                    jQuery('#isInterBaseCoordinate').attr('checked', true);
                    // Add galaxy imported data information on top
                    jQuery('#grs-options-body').before('<div id="grs-options-info" class="topBar info" style="padding-left:34px;"><a href="#" onclick="javascript:jQuery(\'#grs-options-info\').hide(\'slow\');return false">Hide</a>${galaxyFetchDataSuccess}<br></div>');
                } else {
                    if ('${galaxyFetchDataError}') {
                        // Add galaxy imported data error on top
                        jQuery('#grs-options-body').before('<div id="grs-options-error" class="topBar errors" style="padding-left:34px;"><a href="#" onclick="javascript:jQuery(\'#grs-options-error\').hide(\'slow\');return false">Hide</a>${galaxyFetchDataError}<br></div>');
                    }
                }
            });
        </script>
        <script type="text/javascript" src="model/jquery_qtip/jquery.qtip-1.0.js"></script>
        <script type="text/javascript" src="model/genomic_region_search/genomic_region_search_options_base.js"></script>
        <script type="text/javascript" src="model/genomic_region_search/${optionsJavascript}.js"></script>
        <script type="text/javascript" src="model/jquery_ui/jquery-ui-1.8.13.custom.min.js"></script>

        <div id="grs-options-body" align="center" style="padding-top: 20px;">
            <im:boxarea titleKey="genomicRegionSearch.title" stylename="plainbox" fixedWidth="85%" titleStyle="font-size: 1.2em; text-align: center;">
              <div class="body">
                <html:form action="/genomicRegionSearchAction" method="POST" enctype="multipart/form-data">

                  <p>${WEB_PROPERTIES['genomicRegionSearch.caption']}</p>

                  <br/>
                  <a id="region-help-link" href="#">Genome coordinates help</a>
                  <script type="text/javascript">
                    jQuery('#region-help-link').click(function(e) {
                        jQuery('#region-help').slideToggle('slow');
                        e.preventDefault();
                        });
                  </script>

                  <div id="region-help" style="display:none">
                     ${WEB_PROPERTIES['genomicRegionSearch.howTo']}
                  </div>
                  <br/>
                  <br/>
                  <ol id="optionlist">

                    <li id="genomicRegionInput">
                       <%-- textarea --%>
                       <span>Type/Paste in genomic regions in</span>
                       <span id="baseCorRadioSpan">
                           <html:radio property="dataFormat" styleId="isNotInterBaseCoordinate" value="isNotInterBaseCoordinate">&nbsp;base coordinate
                               <a title="e.g. BLAST, GFF/GFF3" onclick="document.getElementById('ctxHelpTxt').innerHTML='base coordinate: e.g. BLAST, GFF/GFF3';document.getElementById('ctxHelpDiv').style.display=''; window.scrollTo(0, 0);return false">
                                   <img style="padding: 4px 3px" alt="?" src="images/icons/information-small-blue.png" class="tinyQuestionMark">
                               </a>
                           </html:radio>
                       </span>
                       <span id="interBaseCorRadioSpan">
                           <html:radio property="dataFormat" styleId="isInterBaseCoordinate" value="isInterBaseCoordinate">&nbsp;interbase coordinate
                               <a title="e.g. UCSC BED, Chado" onclick="document.getElementById('ctxHelpTxt').innerHTML='interbase coordinate: e.g. UCSC BED, Chado';document.getElementById('ctxHelpDiv').style.display=''; window.scrollTo(0, 0);return false">
                                   <img style="padding: 4px 3px" alt="?" src="images/icons/information-small-blue.png" class="tinyQuestionMark">
                               </a>
                           </html:radio>
                       </span>

                       <%-- example span --%>
                       <div style="text-align:left;">
                           <html:link href="" onclick="javascript:loadExample(exampleSpans);return false;">
                             (click to see an example)<img src="images/disclosed.gif" title="Click to Show example"/>
                           </html:link>
                       </div>
                       <html:textarea styleId="pasteInput" property="pasteInput" rows="10" cols="60" onclick="if(this.value != ''){switchInputs('paste','file');}else{openInputs();}" onkeyup="if(this.value != ''){switchInputs('paste','file');}else{openInputs();}" />
                       <br>

                       <%-- file input --%>
                       <span>or Upload genomic regions from a .txt file...</span>
                       <br>
                       <html:file styleId="fileInput" property="fileInput" onchange="switchInputs('file','paste');" onkeydown="switchInputs('file','paste');" size="28" />
                       <html:hidden styleId="whichInput" property="whichInput" />
                    </li>
                    <br>

                    <li id="genomicRegionFlanking">
                       <span>Extend your regions at both sides: <i><b id="extendLength"></b></i></span>
                       <html:hidden styleId="extendedRegionSize" property="extendedRegionSize" value="0" />

                       <tiles:insert name="genomicRegionSearchOptionsExtentionNonLinearSlider.jsp">
                          <tiles:put name="sliderIdentifier" value="regionExtention" />
                          <tiles:put name="defaultValue" value="0" />
                       </tiles:insert>
                    </li>

                  </ol>

                  <div align="right">
                     <%-- reset button --%>
                     <input type="button" onclick="resetInputs()" value="Reset" />
                     <%-- <html:submit onclick="javascript: return validateBeforeSubmit();">Search</html:submit> --%>
                     <input type="button" onclick="javascript: if(validateBeforeSubmit()) jQuery('form#genomicRegionSearchForm').submit();" value="Search" />
                  </div>

                </html:form>
              </div>
            </im:boxarea>
        </div>

    </c:otherwise>
</c:choose>


<!--  /genomicRegionSearchOptionsBase.jsp -->