<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/imutil.tld" prefix="imutil" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- galaxyExportOptions.jsp -->

<script type="text/javascript" src="js/exportoptions.js"></script>
<script type="text/javascript" src="model/jquery_ui/jquery-ui-1.8.13.custom.min.js"></script>
<script type="text/javascript">

  jQuery(document).ready(function() {

    initForm();
    jQuery("input[name='exportOptions']").bind("change", exportRadioClicks);

    jQuery('#pathsList').sortable({
        revert: true
    });

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

  function updatePathQueryView()
  {
      if (jQuery("input[name='exportOptions']:checked").val() == "view") { // export as TSV
          var sorted = jQuery('#pathsList').sortable("serialize");
          var newViewPattern = /\[\]=\d&*/g;
          var updatedSorted = sorted.replace(newViewPattern, " ").trim();

          if (updatedSorted != "") {
              var viewPattern = /view="[^=<]*"/;
              var queryXML = jQuery("#queryXML").val();
              var updatedQueryXML = queryXML.replace(viewPattern, 'view="' + updatedSorted + '"');

              jQuery("#queryXML").val(updatedQueryXML);
          }

      } else { // export as BED
        jQuery("#queryXML").val('${query}'); //reset query if updated in tsv options
      }
  }

</script>

<style type="text/css">

ol { margin-left:1.5em; padding-left:0px; }
li { margin-bottom:0.5em; }

    #pathsList {
        list-style-position: inside;
        cursor: hand;
        cursor: pointer;
    }
    #pathsList li{
        float:left;
        list-style: none;
        border:1px solid #bbbbbb;
        background:#FFF;
        padding:5px;
        margin:5px 1px 10px 1px;
    }

</style>

<html:xhtml />
<link rel="stylesheet" href="css/exportOptions.css" type="text/css" />

<div align="center">
  <div style="clear:both;width:60%" class="body" align="left">

    <form id="galaxyform" action="${GALAXY_URL}" name="galaxyform" method="POST" target="_blank" onsubmit="updatePathQueryView();">
    <fieldset>

      <legend>
        <fmt:message key="exporter.galaxy.description"/>
      </legend>

    <ol>

      <ol>Export results of this query to the Galaxy tool:</ol>

      <li id="exportTableView">
          <fieldset>
             <input type="radio" name="exportOptions" checked="checked" value="view"/><label>Send results as tab separated values</label>
             <p onclick="javascript: jQuery('#tsv-options').slideToggle('slow');">Advanced Options</p>
             <%-- options for pathquery views --%>
             <div id="tsv-options" style="display: none;">
                <html:hidden property="pathsString" styleId="pathsString" value="${pathsString}"/>
                <ol>
                  <li><label>Add column:</label> &nbsp;
                  <tiles:insert name="availableColumns.tile">
                     <tiles:put name="table" value="${table}" />
                     <tiles:put name="table" value='${query}' />
                  </tiles:insert>
                  &nbsp;
                  <button type="button" onclick="javascript:addSelectedPath()" id="columnAddButton">Add</button></li>

                  <li>
                    <label>Drag and drop the fields to reorder the output:</label>
                  <ul id="pathsList">
                  </ul>

                  <script type="text/javascript">
                      pathIndex = 1;

                     <c:forEach var="path" items="${pathsMap}">
                         <c:choose>
                             <c:when test="${empty QUERY}">
                                 <im:debug message="QUERY is empty"/>
                                 <c:set var="displayPath" value="${imf:formatPathStr(path.key, INTERMINE_API, WEBCONFIG)}"/>
                             </c:when>
                             <c:otherwise>
                                 <c:set var="displayPath" value="${imf:formatViewElementStr(path.key, QUERY, WEBCONFIG)}"/>
                             </c:otherwise>
                         </c:choose>
                         addPathElement("${path.key}", "${displayPath}");
                     </c:forEach>

                    if (document.getElementById('columnToAdd')[0].value == '') {
                        document.getElementById("columnAddButton").disabled = true;
                        document.getElementById("columnToAdd").disabled = true;
                    }
                  </script>
                  </li>
                </ol>
             </div>
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
