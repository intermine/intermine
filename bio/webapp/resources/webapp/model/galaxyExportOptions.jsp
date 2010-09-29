<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- galaxyExportOptions.jsp -->

<script type="text/javascript">

  jQuery(document).ready(function() {

     //>>>>>
     // Hide the export feature function for now till the webservice refactory finish in the near future
     jQuery("#exportFeature").hide();

     // IMPORTANT: BED format exporting is disabled due to chromosome name and the difference of start of sequence feature between GFF3(in database) and BED
     jQuery("input[name='exportOptions']").filter("[value='feature']").attr('disabled', 'disabled');
     //<<<<<

     initForm();
     jQuery("#URL").val("${viewURL}");

     if ("${exportAsBED}" == "false") {
       jQuery("input[name='exportOptions']").filter("[value='feature']").attr('disabled', 'disabled');
     }

     jQuery("input[name='exportOptions']").filter("[value='view']").attr('checked', true);

     if (jQuery("input[name='sequencePath']").length>1) {

        jQuery("#firstPathRadio").attr('checked', true);
        jQuery("input[name='sequencePath']").each(function(i) {
                jQuery(this).attr('disabled', 'disabled');
        });
     }

    jQuery("#ajaxLoading").hide()
            .ajaxStart(function(){
                jQuery("input[name='submit']").attr("disabled", "disabled");
                jQuery(this).fadeIn();
            })
            .ajaxStop(function(){
                jQuery(this).fadeOut();
                jQuery("input[name='submit']").removeAttr('disabled');
             });

      jQuery("input[name='exportOptions']").bind("click", exportRadioClicks);
      jQuery("input[name='sequencePath']").bind("click", pathRadioClicks);

  });

  function exportRadioClicks()
  {
    initForm();

     // if sequencePath radio exsits by jQuery("input[name='sequencePath']").length
     if (jQuery(this).val() == "view" && jQuery("input[name='sequencePath']").length==1) {

        jQuery("#URL").val("${viewURL}");

     } else
     if (jQuery(this).val() == "view" && jQuery("input[name='sequencePath']").length>1) {

        jQuery("input[name='sequencePath']").each(function(i) {
                    jQuery(this).attr('disabled', 'disabled');
        });

        jQuery("#URL").val("${viewURL}");

     } else
     if (jQuery(this).val() == "feature" && jQuery("input[name='sequencePath']").length>1) {

        var pathVal = jQuery("input[name='sequencePath']:checked").val();
        var idx;
        <c:forEach var="pathIndexMap" items="${pathIndexMap}">
            if (pathVal == "${pathIndexMap.key}") { idx = "${pathIndexMap.value}" }; // the index of the column
        </c:forEach>

        // Use ajax to get the GALAXY_URL, data_type, genome build and extra info back from galaxyExportAction
        jQuery.get("${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/galaxyExportAction.do", { tableName: "${table}", index: idx, prefix: pathVal}, function(data){
          jQuery("#data_type").val("bed");
          jQuery("#URL").val(jQuery.trim(data));
          <c:forEach var="orgInfo" items="${orgInfoList}">
             if(idx == "${orgInfo.idx}") {
               jQuery("#info").val("Organisms:${orgInfo.orgList}");
               jQuery("#dbkey").val("${orgInfo.genomeBuild}");
             }
           </c:forEach>
        });

        jQuery("input[name='sequencePath']").each(function(i) {
                    jQuery(this).removeAttr('disabled');
        });

     } else
     if (jQuery(this).val() == "feature" && jQuery("input[name='sequencePath']").length==1) {

        var pathVal = jQuery("input[name='sequencePath']").val();
        var idx;
        <c:forEach var="pathIndexMap" items="${pathIndexMap}">
            if (pathVal == "${pathIndexMap.key}") { idx = "${pathIndexMap.value}" };
        </c:forEach>

        jQuery.get("${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/galaxyExportAction.do", { tableName: "${table}", index: idx, prefix: pathVal}, function(data){
          jQuery("#data_type").val("bed");
          jQuery("#URL").val(jQuery.trim(data));
          <c:forEach var="orgInfo" items="${orgInfoList}">
             if(idx == "${orgInfo.idx}") {
               jQuery("#info").val("Organisms:${orgInfo.orgList}");
               jQuery("#dbkey").val("${orgInfo.genomeBuild}");
             }
           </c:forEach>
        });
     }
  }

  function pathRadioClicks()
  {
    initForm();

    if (jQuery("input[name='sequencePath']").length>1) {

        // Before ajax call, disable the unchecked radio buttons, to prevent quick click on radio buttons thus the form gets the right ajax response for user's selection
        // Option 2 - Stop the current ajax call:
        // If you use $.ajax for your remote calls it returns the XHR object...:
        // var xhr = $.ajax({...});
        // You can then stop it using the given function:
        // xhr.abort();

        // Disable the unchecked
        jQuery("input[name='sequencePath']:not(:checked)").each(function(i) {
                jQuery(this).attr('disabled', 'disabled');
        });

        var pathVal = jQuery("input[name='sequencePath']:checked").val();
        var idx;
        <c:forEach var="pathIndexMap" items="${pathIndexMap}">
            if (pathVal == "${pathIndexMap.key}") { idx = "${pathIndexMap.value}" };
        </c:forEach>

        jQuery.get("${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/galaxyExportAction.do", { tableName: "${table}", index: idx, prefix: pathVal}, function(data){
          jQuery("#data_type").val("bed");
          jQuery("#URL").val(jQuery.trim(data));
          <c:forEach var="orgInfo" items="${orgInfoList}">
             if(idx == "${orgInfo.idx}") {
               jQuery("#info").val("Organisms:${orgInfo.orgList}");
               jQuery("#dbkey").val("${orgInfo.genomeBuild}");
             }
           </c:forEach>
        });

        // After ajax call, enable the unchecked
        jQuery("input[name='sequencePath']:not(:checked)").each(function(i) {
                jQuery(this).removeAttr('disabled');
        });
    }
  }

  function initForm()
  {
    jQuery("#data_type").val("txt");
    jQuery("#URL").val("");
    jQuery("#info").val("");
    jQuery("#dbkey").val("");
  }

</script>

<style type="text/css">

ol { margin-left:1.5em; padding-left:0px; }
li { margin-bottom:0.5em; }

</style>

<html:xhtml />

    <div id="ajaxLoading" align="center" style="position:absolute;clear:both;width:60%;height:30%;">
            <img src="images/wait18.gif"/><br>Loading...
    </div>

    <form id="galaxyform" action="${GALAXY_URL}" name="galaxyform" method="POST" target="_blank">
    <fieldset>

      <legend>
        <fmt:message key="exporter.galaxy.description"/>
      </legend>

    <ol>

      <ol>Export results of this query to the Galaxy tool:</ol>

      <li id="exportTableView">
      <fieldset>
      <input type="radio" name="exportOptions" checked="checked" value="view"/><label>Send the results as a table</label>
      </fieldset>
      </li>

      <li id="exportFeature">
      <fieldset>
      <input type="radio" name="exportOptions" value="feature"/><label>Send feature data in BED format which will be recognised by Galaxy</label>
      </fieldset>


        <ol>

        <fieldset>
            <c:forEach items="${exportClassPaths}" var="entry" varStatus="status">
              <c:set var="path" value="${entry.key}" />
              <c:choose>
                <c:when test="${fn:length(exportClassPaths) == 1}">
                  <li><html:hidden property="sequencePath" value="${path}" />
                        <label>${entry.value}</label></li>
                </c:when>
                <c:otherwise>
                   <c:choose>
                            <c:when test="${status.first}">
                                <li><input id = "firstPathRadio" type="radio" name="sequencePath" value="${path}" checked="checked" /><label>${entry.value}</label></li>
                            </c:when>
                            <c:otherwise>
                                 <li><input type="radio" name="sequencePath" value="${path}" /><label>${entry.value}</label></li>
                            </c:otherwise>
                        </c:choose>
                </c:otherwise>
              </c:choose>
            </c:forEach>
        </fieldset>

        </ol>

     </li>

    </ol>

    </fieldset>
      <fieldset class="submit"><input name="submit" type="submit" value="Send to Galaxy" /></fieldset>

    <input id="URL" type="hidden" name="URL">
    <input id="data_type" type="hidden" name="data_type">
    <input id="dbkey" type="hidden" name="dbkey">
    <input id="info" type="hidden" name="info">

    </form>

<!-- /galaxyExportOptions.jsp -->
