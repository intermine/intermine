<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- export.jsp -->

<c:set var="queryTrailLink" value="|query"/>
<c:if test="${fn:startsWith(tableName, 'bag.')}">
  <c:set var="queryTrailLink" value=""/>
</c:if>
<html:xhtml/>

<c:choose>
  <c:when test="${results_page != null}">
    <span class="csv">
      <html:link action="/exportOptions?table=${tableName}&amp;type=csv&amp;trail=${queryTrailLink}|${tableName}" title="Export results as comma or tab separated values (suitable for import into Excel)"></html:link>
    </span>
  </c:when>
  <c:otherwise>
    <html:link action="/exportOptions?table=${tableName}&amp;type=csv&amp;trail=${queryTrailLink}|${tableName}">
      <fmt:message key="exporter.csv.description"/>
    </html:link>
    <br/>
  </c:otherwise>
</c:choose>

<c:if test="${WEB_PROPERTIES['galaxy.display'] != false}">
    <c:choose>
      <c:when test="${results_page != null}">
        <span class="galaxy">
          <html:link action="/galaxyExportOptions?table=${tableName}&amp;trail=${queryTrailLink}|${tableName}" title="Export to Galaxy"></html:link>
        </span>
      </c:when>
      <c:otherwise>
        <html:link action="/galaxyExportOptions?table=${tableName}&amp;trail=${queryTrailLink}|${tableName}">
            <fmt:message key="exporter.galaxy.description"/>
        </html:link>
        <br/>
      </c:otherwise>
    </c:choose>
</c:if>

<c:forEach var="entry" items="${exporters}" varStatus="status">
  <c:set var="exporterId" value="${entry.key}"/>
  <c:choose>
    <c:when test="${results_page != null}">
      <c:if test="${!empty entry.value}">
        <span class="${exporterId}">
          <html:link action="/exportOptions?table=${tableName}&amp;type=${exporterId}&amp;trail=${queryTrailLink}|${tableName}" title="Export in ${exporterId} format"></html:link>
        </span>
      </c:if>
    </c:when>
    <c:otherwise>
      <c:choose>
        <c:when test="${empty entry.value}">
          <span class="nullStrike"><fmt:message key="exporter.${exporterId}.description"/></span><br>
        </c:when>
        <c:otherwise>
          <html:link action="/exportOptions?table=${tableName}&amp;type=${exporterId}&amp;trail=${queryTrailLink}|${tableName}">
            <fmt:message key="exporter.${exporterId}.description"/>
          </html:link><br>
        </c:otherwise>
      </c:choose>
    </c:otherwise>
  </c:choose>
</c:forEach>

<%-- <div id="clippy-table">Copy to clipboard </div> --%>

<script type="text/javascript">
// parse the table to provide a tabified string, optionally specify a column to exclusively include
function tableToString(column) {
  var text = "";

  // head
  jQuery('table.results thead tr th').each(function(i) { // no comment...
    if (i > 0 && column == null) text += "\t"; // tab
    if (column == null || column == i) {
      text += jQuery.trim(jQuery(this).find('table tr td span').text()).replace(/\s+/g, '');
    }
  });
  // replace html arrow chars with makeshift line...
  text = text.replace(new RegExp(">", "g"), " - ");

  // body
  jQuery('table.results tbody tr.bodyRow').each(function(i) { // rows
    text += "\n"; // newline
    jQuery(this).find('td').each(function(j) { // columns
      if (j > 0 && column == null) text += "\t"; // tab
      if (column == null || column == j) {
        text += jQuery.trim(jQuery(this).text()); // actual text
      }
    });
  });

  return text;
}

// create the Flash copy to clipboard object
function createClippy(text, target) {
  if (jQuery(target).length > 0) {
    // fetch the id from our id attribute
    var identifier = jQuery(target).attr("id");
    // create the element with text
    jQuery(target).append('<span style="display:none;" id="'+identifier+'-text">'+text+'</span>');
    // append the object
    var object = '<object style="vertical-align:bottom;" width="14" height="14" classid="clsid:d27cdb6e-ae6d-11cf-96b8-444553540000">'
            + '<param value="swf/clippy.swf" name="movie">'
            + '<param value="always" name="allowScriptAccess">'
            + '<param value="high" name="quality">'
            + '<param value="noscale" name="scale">'
            + '<param value="id='+identifier+'-text" name="FlashVars">'
            + '<param value="#FFFFFF" name="bgcolor">'
            + '<param value="opaque" name="wmode">'
            + '<embed width="14" height="14" wmode="opaque" bgcolor="#FFFFFF" flashvars="id='+identifier+'-text" pluginspage="http://www.macromedia.com/go/getflashplayer" type="application/x-shockwave-flash" allowscriptaccess="always" quality="high" name="clippy" src="swf/clippy.swf">'
            + '</object>';
    jQuery(target).append(object);
  }
}

//jQuery(document).ready(function() {
//  // on table load create table-wide clippy
//  createClippy(tableToString(), "div.results-page #tool_bar_item_export #clippy-table");
//  createClippy(tableToString(), "div.bagDetails-page #download #clippy-table");
//  createClippy(tableToString(), "div.bagDetails-page #tool_bar_item_export #clippy-table"); // old list analysis
//  // and one clippy per column
//  jQuery('table.results thead tr th.columnHeader').each(function(i) { // no comment...
//    // add the target
//    jQuery(this).prepend('<div class="summary_link" id="clippy-column-'+i+'"></div>');
//    // create clippy thingie
//    createClippy(tableToString(i), '#clippy-column-'+i);
//  });
//});
</script>

<!-- /export.jsp -->