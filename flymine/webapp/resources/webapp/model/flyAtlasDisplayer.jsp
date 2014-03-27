<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>


<!-- flyAtlasDisplayer.jsp -->

<c:if test="${signals != '[]'}">
<div>

<h3>FlyAtlas adult tissue expression</h3>
<div id="flyatlas-viz"></div>

<div id="flyatlas-viz2"></div>

<div class="table">
<form id="fly-atlas-options">
  <table cellspacing="0">
    <tbody>
    <tr>
      <td><input type="radio" name="scale" value="log" checked="checked"/></td><td>Logarithmic Scale</td>
      <td class="border-left"><input type="radio" name="orderExpr" value="tissue.name" checked="checked"/></td><td>Order by Name</td>
      <td class="border-left"><input type="radio" name="dataPoint" value="enrichment" checked="checked" /></td><td>Show enrichment</td>
    </tr>
    <tr class="even">
      <td><input type="radio" name="scale" value="linear"/></td>
      <td>Linear Scale</td>
      <td class="border-left"><input type="radio" name="orderExpr" value="signal"/></td>
      <td>Order by Signal</td>
      <td class="border-left"><input type="radio" name="dataPoint" value="signal" /></td>
      <td>Show signal strength</td>
    </tr>
    </tbody>
  </table>
</form>
</div>
</div>

<script type="text/javascript">
function drawFlyAtlasChart(event, sortBySignal, useLinearScale, showSignal) {
  var sortByName = !sortBySignal;
  var showEnrichment = !showSignal;
  var useLog = !useLinearScale;
  var signal_data = new google.visualization.DataTable();
  var enrichment_data = new google.visualization.DataTable();
  var signals = <c:out value="${signals}"/>;
  var enrichments = <c:out value="${enrichments}"/>;
  var presentCalls = <c:out value="${presentCalls}"/>;
  var affyCalls = <% out = pageContext.getOut();
    out.write(request.getAttribute("affyCalls").toString());
  %>
  var names = <% out = pageContext.getOut();
     out.write(request.getAttribute("names").toString());
  %>;
  var objectIds = <% out = pageContext.getOut();
     out.write(request.getAttribute("objectIds").toString());
  %>;

    signal_data.addColumn('string', 'Tissue Name', 'tissue');
    signal_data.addColumn('number', 'Down Regulated', 'downreg');
    signal_data.addColumn('number', 'Same as Whole Fly', 'nonereg');
    signal_data.addColumn('number', 'Up Regulated', 'upreg');

    enrichment_data.addColumn('string', 'Tissue Name', 'tissue');
    enrichment_data.addColumn('number', 'Down Regulated', 'downreg');
    enrichment_data.addColumn('number', 'Same as Whole Fly', 'nonereg');
    enrichment_data.addColumn('number', 'Up Regulated', 'upreg');

    var baseUrl = "/${WEB_PROPERTIES['webapp.path']}/report.do?id=";

  for (var i = 0; i < names.length; i++) {
    var signal_row = [names[i], 0, 0, 0];
    var enrichment_row = [names[i], 0, 0, 0];

    var index;
      if (affyCalls[i] == 'Down') {
        index = 1;
      } else if (affyCalls[i] == 'Up') {
        index = 3;
      } else {
        index = 2;
      }
      signal_row[index] = signals[i];
      enrichment_row[index] = (useLog) ? (Math.log(enrichments[i])/Math.log(2)) : enrichments[i];

    var formattedValue = "(signal: " + signals[i]
            + (isNaN(enrichments[i]) ? '' : ", enrichment: " + enrichments[i])
            + ", present in " + presentCalls[i] + " of 4)";
    signal_data.addRow(signal_row);
    enrichment_data.addRow(enrichment_row);
    signal_data.setFormattedValue(i, index, formattedValue);
    enrichment_data.setFormattedValue(i, index, formattedValue);
  }

  var data = (showEnrichment) ? enrichment_data : signal_data;

  if (sortByName) {
    data.sort([{column: 0}]);
  }

  var useLogScaleOption = (useLog && !showEnrichment) ? true : false;

  var haxis = (showEnrichment) ? "Enrichment" : "Signal Intensity";
  if (useLog && showEnrichment) {
    haxis += " (log²)";
  }

  var maxH = (showEnrichment) ? null : 10000;
  var baseLine = (showEnrichment && useLinearScale) ? 1 : null;

  var height = 80 + (18 * signals.length);
  var viz = new google.visualization.BarChart(document.getElementById('flyatlas-viz'));
  viz.draw(
        data,
      {isStacked: true,
       colors: ['#314bbc','#8931bc','#bc3162'],
       title: "Fly Atlas Expression By Tissue",
       width: 920, height: height,
       legendTextStyle: {fontSize: 10},
       vAxis: {title: "Tissue Name", textStyle: {fontSize: 11}},
       hAxis: {title: haxis, logScale: useLogScaleOption, maxValue: maxH, baseline: baseLine, baselineColor: '#8931bc'},
      }
    );

  google.visualization.events.addListener(viz, 'select', function() {
         var selection = viz.getSelection();
         for (var i = 0; i < selection.length; i++) {
           var item = selection[i];
           if (item.row != null && item.column != null) {
             // it is a cell
             var objectId = objectIds[item.row];
             window.location.assign(baseUrl + objectId);
           }
         }
       });
};

jQuery("input[name='scale']").change(function() {
  var useLinearScale = ('linear' == jQuery(this).val());
  var orderBySignal = ('signal' == jQuery("input[name='orderExpr']:checked").val());
  var showSignal = ('signal' == jQuery("input[name='dataPoint']:checked").val());
  drawFlyAtlasChart(null, orderBySignal, useLinearScale, showSignal);
});

jQuery("input[name='orderExpr']").change(function() {
  var orderBySignal = ('signal' == jQuery(this).val());
  var useLinearScale = ('linear' == jQuery("input[name='scale']:checked").val());
  var showSignal = ('signal' == jQuery("input[name='dataPoint']:checked").val());
  drawFlyAtlasChart(null, orderBySignal, useLinearScale, showSignal);
});

jQuery("input[name='dataPoint']").change(function() {
  var showSignal = ('signal' == jQuery(this).val());
  var useLinearScale = ('linear' == jQuery("input[name='scale']:checked").val());
  var orderBySignal = ('signal' == jQuery("input[name='orderExpr']:checked").val());
  drawFlyAtlasChart(null, orderBySignal, useLinearScale, showSignal);
});

google.load("visualization", "1", {"packages": ["corechart"], "callback": drawFlyAtlasChart});

</script>

</c:if>

<!-- /flyAtlasDisplayer.jsp -->
