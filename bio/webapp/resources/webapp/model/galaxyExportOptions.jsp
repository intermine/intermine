<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- galaxyExportOptions.jsp -->

<script type="text/javascript">

  jQuery(document).ready(function() {

    initForm();
    jQuery("input[name='exportOptions']").bind("change", exportRadioClicks);

  });

  function exportRadioClicks()
  {
    if (jQuery(this).val() == "view") {
        jQuery("#data_type").val("tabular");
        jQuery("#info").val("");
        jQuery("#dbkey").val("");
        jQuery("#URL").val("${tableURL}");
        jQuery("#size").val("${size}");
        jQuery("input[name='exportOptions']").filter("[value='view']").attr('checked', true);
    }

    if (jQuery(this).val() == "feature") {
        jQuery("#data_type").val("bed");
        jQuery("#info").val("${org}");
        jQuery("#dbkey").val("${dbkey}");
        jQuery("#URL").val("${bedURL}");
        jQuery("#size").val("");
        jQuery("input[name='exportOptions']").filter("[value='feature']").attr('checked', true);
    }
  }

  function initForm()
  {
    jQuery("#data_type").val("tabular");
    jQuery("#URL").val("${tableURL}");
    jQuery("#queryXML").val('${query}');
    jQuery("#size").val("${size}");
    jQuery("#info").val("");
    jQuery("#dbkey").val("");

    if ("${canExportAsBED}" == "false") {
        jQuery("input[name='exportOptions']").filter("[value='feature']").attr('disabled', 'disabled');
    }

    jQuery("input[name='exportOptions']").filter("[value='view']").attr('checked', true);
  }

</script>

<style type="text/css">

ol { margin-left:1.5em; padding-left:0px; }
li { margin-bottom:0.5em; }

</style>

<html:xhtml />
<link rel="stylesheet" href="css/exportOptions.css" type="text/css" />

<div align="center">
  <div style="clear:both;width:60%" class="body" align="left">

    <form id="galaxyform" action="${GALAXY_URL}" name="galaxyform" method="POST" target="_blank">
    <fieldset>

      <legend>
        <fmt:message key="exporter.galaxy.description"/>
      </legend>

    <ol>

      <ol>Export results of this query to the Galaxy tool:</ol>

      <li id="exportTableView">
          <fieldset>
             <input type="radio" name="exportOptions" checked="checked" value="view"/><label>Send results as tab separated values</label>
          </fieldset>
      </li>

      <li id="exportFeature">
          <fieldset>
            <input type="radio" name="exportOptions" value="feature"/><label>Send results in UCSC BED format (Sequence Feature only)</label>
          </fieldset>
      </li>

    </ol>

    </fieldset>

    <fieldset class="submit"><input name="submit" type="submit" value="Send to Galaxy" /></fieldset>

    <input id="URL" type="hidden" name="URL">
    <input id="queryXML" type="hidden" name="query">
    <input id="size" type="hidden" name="size">
    <input type="hidden" name="URL_method" value="post">
    <input id="data_type" type="hidden" name="data_type">
    <input id="dbkey" type="hidden" name="dbkey">
    <input id="info" type="hidden" name="info">

    </form>

  </div>
</div>
<!-- /galaxyExportOptions.jsp -->
