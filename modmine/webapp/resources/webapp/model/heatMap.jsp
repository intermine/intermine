<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="mm"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
    prefix="str"%>

<!-- heatMap.jsp -->

<html:xhtml />

<tiles:importAttribute />

<script type="text/javascript" src="http://www.google.com/jsapi"></script>
<script type="text/javascript">
    google.load("visualization", "1", {});
    google.load("prototype", "1.6");
</script>
<script type="text/javascript" src="model/bioheatmap/js/bioheatmap.js"></script>
<script type="text/javascript">
  function drawHeatMap() {
         //============= Heatmap body =============
         var heatmap = new org.systemsbiology.visualization.BioHeatMap(document.getElementById('heatmapContainer'));

         var data_body = new google.visualization.DataTable();

         data_body.addColumn('string', 'Gene Name');

         <c:forEach items="${expressionScoreMap}" var="ges" varStatus="ges_status">
             <c:if test="${ges_status.first}">
                 <c:forEach items="${ges.value}" var="geScores" varStatus="geScores_status" >
                    data_body.addColumn('number', "${geScores.condition}");
                 </c:forEach>
             </c:if>
         </c:forEach>

         <c:forEach items="${expressionScoreMap}" var="ges" varStatus="ges_status">
             data_body.addRows(1);
             data_body.setCell(${ges_status.count - 1}, 0, "${ges.key}");

             <c:forEach items="${ges.value}" var="geScores" varStatus="geScores_status" >
                data_body.setCell(${ges_status.count - 1}, ${geScores_status.count }, ${geScores.logScore});
             </c:forEach>
         </c:forEach>

          heatmap.draw(data_body, ${maxExpressionScore}, ${minExpressionScore}, {startColor: {r:0, g:0, b:255, a:1},
                                                                                 endColor: {r:255, g:255, b:0, a:1},
                                                                                 passThroughBlack: false,
                                                                                 numberOfColors: 256,
                                                                                 cellHeight: 10,
                                                                                 cellWidth: 10,
                                                                                 fontHeight: 7,
                                                                                 drawBorder: false});

          google.visualization.events.addListener(heatmap, 'select', function() {
                    var rowNo = heatmap.getSelection()[0].row;
                    var colNo = heatmap.getSelection()[0].column;
                    alert('Expression score: ' + data_body.getValue(rowNo, colNo));
                });

        //============= Heatmap legend =============
        var heatmap_legend = new org.systemsbiology.visualization.BioHeatMap(document.getElementById('heatmapLegendContainer'));

        var data_legend = new google.visualization.DataTable();
        var nCols = 100;
        var nRows = 1;
        for (col = 0; col < nCols; col++) {
            data_legend.addColumn('number', 1);
        }

        var max = ${maxExpressionScore};
        var min = ${minExpressionScore};
        var dataStep = (Math.abs(min) + Math.abs(max))/((nCols)*nRows);
        var value = min;
        for (var i = 0; i < nRows; i++) {
            data_legend.addRows(1);
            for (col = 0; col < nCols; col++) {
                data_legend.setCell(i, col, value);
                value += dataStep;
            }
        }

        heatmap_legend.draw(data_legend, ${maxExpressionScore}, ${minExpressionScore}, {startColor: {r:0, g:0, b:255, a:1},
                                                                                        endColor: {r:255, g:255, b:0, a:1},
                                                                                        passThroughBlack: false,
                                                                                        numberOfColors: 256,
                                                                                        cellWidth: 2,
                                                                                        cellHeight: 15,
                                                                                        useRowLabels: false,
                                                                                        drawBorder: false });
  }
</script>

<div class="body" id="expression_div">
    <div id="heatmap_div">
        <p><h2>${ExpressionScoreTitle}</h2></p>
        <p><i>${ExpressionScoreSummary}<html:link href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=Drosophila Cell Line and Developmental Stage Gene and Exon Scores">
            the Celniker group</html:link>.</i></p>
        <br/>
        <div id="heatmapContainer"></div>
        <div id="heatmapLegend_div">
            <table id="heatmapLegend_table" border="0" style="padding-left:30px;">
                <tr>
                    <td style="vertical-align:middle;">${minExpressionScore}</td>
                    <td id="heatmapLegendContainer"></td>
                    <td style="vertical-align:middle;">${maxExpressionScoreCeiling}</td>
                </tr>
            </table>
        </div>
        <div id="description_div">
            <table border="0">
                <tr>
                    <td ><h3 style="font-weight: bold; background: black; color: white;">More Information</h3></td>
                    <td ><h3 style="background: white;"><img src="images/disclosed.gif" id="co"></h3></td>
                </tr>
            </table>
        </div>
        <div id="description" style="padding: 5px">
            <i>${ExpressionScoreDescription}</i>To see <html:link href="/${WEB_PROPERTIES['webapp.path']}/portal.do?class=Submission&externalids=${expressionScoreDCCid}">
            further information about the submission</html:link> and <a href="http://www.modencode.org/docs/flyscores/" target="_blank">original score tables</a>.
        </div>
    </div>
</div>

<script type="text/javascript">
    if ('${expressionScoreDCCid}'=='') {
        jQuery('#heatmap_div').remove();
        jQuery('#expression_div').html('<i>Expression scores are not available</i>');
     } else {
         jQuery("#description").hide();

         drawHeatMap();

         jQuery("#description_div").click(function () {
               if(jQuery("#description").is(":hidden")) {
                 jQuery("#co").attr("src", "images/disclosed.gif");
               } else {
                 jQuery("#co").attr("src", "images/undisclosed.gif");
               }
               jQuery("#description").toggle("slow");
            });
     }
</script>

<!-- /heatMap.jsp -->

<%--
// methods that return example datatables and display options
var BioHeatMapExampleData = {

    // ---------------------------------------------------------------
    // Example 3 DATA: large random matrix (can use for stress testing)
    // ---------------------------------------------------------------
    example3 : function() {
        var data = new google.visualization.DataTable();
        data.addColumn('string', 'Gene Name');

       var nCols = 50;
       var nRows = 20;
       for (col = 1; col < nCols; col++) {
           data.addColumn('number', 'Col' + col);

       }
       var max = 10;
       var min = -10;
       for (var i = 0; i < nRows; i++) {
           data.addRows(1);
           data.setCell(i, 0, 'Row' + i);
           for (col = 1; col < nCols; col++) {
               var value = (Math.random() * (max - min + 1)) + min;
               data.setCell(i, col, value);
           }
       }

        return data;
    }
}
--%>
