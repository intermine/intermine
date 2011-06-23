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


<!--  genomicRegionSearchOptionsBase.jsp -->

<html:xhtml />

<script type="text/javascript" src="model/jquery_qtip/jquery.qtip-1.0.js"></script>
<script type="text/javascript" src="model/genomic_region_search/genomic_region_search_options_base.js"></script>
<script type="text/javascript" src="model/genomic_region_search/${optionsJavascript}.js"></script>
<script type="text/javascript">

    // webData must be defined in base jsp first, and customized page can make use of it.
    var webDataJSON = jQuery.parseJSON('${webData}');

    // genomic region examples read from web.properties
    var exampleSpans = "${WEB_PROPERTIES['genomicRegionSearch.defaultSpans']}";

</script>

<div align="center" style="padding-top: 20px;">
<im:boxarea titleKey="genomicRegionSearch.title" stylename="plainbox" fixedWidth="85%" titleStyle="font-size: 1.2em; text-align: center;">
  <div class="body">
    <html:form action="/genomicRegionSearchAction" method="POST" enctype="multipart/form-data">

      <p>${WEB_PROPERTIES['genomicRegionSearch.caption']}</p>
      <br/>
      ${WEB_PROPERTIES['genomicRegionSearch.howTo']}
      <br/>

      <ol id="optionlist">

        <li id="genomicRegionInput">
           <%-- textarea --%>
           <span>Type/Paste in genomic regions in</span>
           <span id="baseCorRadioSpan"><html:radio property="dataFormat" value="isNotInterBaseCoordinate">&nbsp;base coordinate</html:radio></span>
           <span id="interBaseCorRadioSpan"><html:radio property="dataFormat" value="isInterBaseCoordinate">&nbsp;interbase coordinate</html:radio></span>

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

      </ol>

      <div align="right">
         <%-- reset button --%>
         <input type="button" onclick="resetInputs()" value="Reset" />
         <html:submit onclick="javascript: return validateBeforeSubmit();">Search</html:submit>
      </div>

    </html:form>
  </div>
</im:boxarea>
</div>

<!--  /genomicRegionSearchOptionsBase.jsp -->