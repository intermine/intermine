<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- bedExportOptions.jsp -->

<script type="text/javascript">
    function stopRKey(evt) {
      var evt = (evt) ? evt : ((event) ? event : null);
      var node = (evt.target) ? evt.target : ((evt.srcElement) ? evt.srcElement : null);
      if ((evt.keyCode == 13) && (node.type=="text"))  {return false;}
    }

    document.onkeypress = stopRKey;

    function saveTrackDescription(){
        var trackText = jQuery('#trackText').val();
        if (trackText == "") {
            jQuery('#descDiv').html("(empty description)");
        } else {
            jQuery('#descDiv').html(trackText);
        }
        jQuery('#trackDescriptionText').toggle();
        jQuery('#trackDescriptionDiv').toggle();
    }

    jQuery(document).ready(function() {
        jQuery('#makeUcscCompatibleCheckbox').click(function() {
            if ( jQuery('#makeUcscCompatibleCheckbox').attr('checked')) {
                jQuery('#ucscCompatibleCheck').val('yes');
            } else {
                jQuery('#ucscCompatibleCheck').val('no');
            }
        });
    });


</script>

    <html:form action="/${type}ExportAction">
        <fieldset>
            <legend>
                <fmt:message key="exporter.bed.description"/>
            </legend>
        <ol>
            <li class="columnHeaderOption">
                <html:checkbox property="doGzip"/>
                <label>Compress data using gzip</label>
            <li>
            <li class="columnHeaderOption">
                <html:checkbox styleId="makeUcscCompatibleCheckbox" property="makeUcscCompatible" value="true"/>
                <label>Prefix "chr" to chromosome name to be compatible with UCSC convention, e.g. <i>chrII:14,646,344-14,667,746</label>
            <li>
            <li class="columnHeaderOption">
              <b>UCSC Track Description:</b>
                <div id="trackDescriptionDiv" onclick="jQuery('#trackDescriptionDiv').toggle();jQuery('#trackDescriptionText').toggle();jQuery('#trackText').focus()">
                    <div id="descDiv"><i style="background-color:#D4D4D4;">To display your personal data as an annotation track in UCSC Genome Browser, click here to enter a description</i></div>
               </div>
               <div id="trackDescriptionText" style="display:none; padding-top:5px;">
                    <html:text styleId="trackText" property="trackDescription" style="border:none; background-color:#D4D4D4; width: 50%;"></html:text>
                      <input type="button" style="margin:0 3px 5px 10px;" onclick="jQuery('#trackDescriptionText').toggle();
                          jQuery('#trackDescriptionDiv').toggle(); return false;" value='Cancel' />
                      <input type="button" onclick="saveTrackDescription(); return false;" value='Save' />
               </div>
            </li>
        </ol>
        </fieldset>

        <html:hidden property="pathsString" styleId="pathsString" value="${pathsString}"/>
        <html:hidden property="table" value="${table}"/>
        <html:hidden property="type" value="${type}"/>
        <html:hidden styleId="ucscCompatibleCheck" property="ucscCompatibleCheck" value="yes"/>

        <fieldset class="submit"><input name="submit" type="submit" value="Export" /></fieldset>
    </html:form>

<!-- /bedExportOptions.jsp -->