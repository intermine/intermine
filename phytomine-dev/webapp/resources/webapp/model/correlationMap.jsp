<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.net.URLEncoder" language="java" %>

<!-- correlationMap.jsp -->

<html:xhtml />

<tiles:importAttribute />

<!--[if IE]><script type="text/javascript" src="model/canvasXpress/js/excanvas.js"></script><![endif]-->
    <script type="text/javascript" src="model/canvasXpress/js/canvasXpress.min.js"></script>

<div class="body" id="expression_div">

<html> mincorrelation "${minCorrelation}" </html>
<html> maxcorrelation "${maxCorrelation}" </html>
<html> geneCount "${bioentityCount}" </html>
<html> json "${correlationJSON}" </html>
<html> organism count "${organismCount}" </html>

<script type="text/javascript" charset="utf-8">
jQuery(document).ready(function () {
    var bioentity_count = parseInt(${bioentiyyCount});
    var organism_count = parseInt(${organismCount});
    if (bioentity_count > 100 || organism_count > 1) {
        jQuery("#correlationGraph").hide();
    } else {
        jQuery("#correlationGraph").show();
    }

    jQuery("#bro").click(function () {
       if(jQuery("#correlationGraph").is(":hidden")) {
         jQuery("#oc").attr("src", "images/disclosed.gif");
       } else {
         jQuery("#oc").attr("src", "images/undisclosed.gif");
       }
       jQuery("#correlationGraph").toggle("slow");
    });
})
</script>

<c:set var="MAX_CLUSTER" value="250" />
<c:set var="MAX_MAP" value="600" />
<c:set var="MAX_DEFAULT_OPEN" value="100" />


    <div id="correlationMap_div">
        <p>
          <h2>
              <c:choose>
                <c:when test="${ExpressionType == 'gene'}">
                  ${WEB_PROPERTIES['correlationMap.geneExpressionScoreTitle']}
                </c:when>
                <c:when test="${ExpressionType == 'mrna'}">
                  ${WEB_PROPERTIES['correlationMap.mrnaExpressionScoreTitle']}
                </c:when>
                <c:otherwise>
                  ${ExpressionType}
                </c:otherwise>
              </c:choose>
          </h2>
        </p>
        <p>
          <i>
            ${WEB_PROPERTIES['correlationMap.expressionScoreSummary']}
            <br>Heatmap visualization powered by
            <a href="http://www.canvasxpress.org">canvasXpress</a>, learn more about the <a href="http://www.canvasxpress.org/heatmap.html">display options</a>.
          </i>
        </p>
        <br/>

        <html:link linkName="#" styleId="bro" style="cursor:pointer">
        <h3>
        <c:if test="${FeatureCount > MAX_DEFAULT_OPEN}">
        Your list is big and there could be issues with the display:
        </c:if>
        <b>Click to see/hide</b> the expression maps<img src="images/undisclosed.gif" id="oc"></h3>
        </html:link>

        <div id="correlationGraph" style="display: block">

        <c:if test="${FeatureCount > MAX_CLUSTER}">
        Please note that clustering functions are not available for lists with more than ${MAX_CLUSTER} elements.
        <br>
        </c:if>

        <div id="correlationContainer">
            <table>
              <tr>
                <td>
                    <div style="padding: 0px 0px 5px 30px;">
                     <span>Coexpression Clustering - Hierarchical:</span>
                     <select id="coexpressionHierarchical">
                         <option value="single" selected="selected">Single</option>
                         <option value="complete">Complete</option>
                         <option value="average">Average</option>
                     </select>
                     <span> and K-means:</span>
                     <select id="coexpressionKMeans">
                         <option value="3" selected="selected">3</option>
                     </select>
                    </div>
                    <canvas id="canvas_coexp" width="825" height="550"></canvas>
                </td>
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
            <i>
              <c:choose>
                <c:when test="${ExpressionType == 'gene'}">
                  Expression type is gene
                  ${WEB_PROPERTIES['correlationMap.geneExpressionScoreDescription']}
                </c:when>
                <c:when test="${ExpressionType == 'mrna'}">
                  Expression type is mrna
                  ${WEB_PROPERTIES['correlationMap.mrnaExpressionScoreDescription']}
                </c:when>
                <c:otherwise>
                  Expression type is not mrna or gene
                  ${ExpressionType}
                </c:otherwise>
              </c:choose>
            <br>Coexpression values derived from Pearson correlation of <em>cufflinks</em> FPKMs.
            </i>
        </div>
    </div>
</div>
</div>


<script type="text/javascript">
    var organism_count = parseInt(${organismCount});
    var bioentity_count = parseInt(${bioentityCount});
    var max_cluster = parseInt(${MAX_CLUSTER});
    var max_map = parseInt(${MAX_MAP});

    if ('${fn:length(correlationJSON)}' < 10) {
        // if the JSON string is short, no data.
        jQuery('#correlationMap_div').remove();
        jQuery('#expression_div').html('<i>Coexpression scores are not available</i>');
    } else if (bioentity_count > max_map) {
        jQuery('#correlationMap_div').remove();
        jQuery('#expression_div').html('<i>Too many elements. Please select a subset to see the coexpression heat maps.</i>');
    } else if (organism_count > 1) {
        jQuery('#correlationMap_div').remove();
        jQuery('#expression_div').html('<i>Too many organisms. Please select a subset of genes or mRNAs from only one organism to see the coexpression heat maps.</i>');
    } else {
        jQuery("#description").hide();

        jQuery("#description_div").click(function () {
               if(jQuery("#description").is(":hidden")) {
                 jQuery("#co").attr("src", "images/disclosed.gif");
               } else {
                 jQuery("#co").attr("src", "images/undisclosed.gif");
               }
               jQuery("#description").toggle("slow");
           });


        // the heat map
        var coexpressionHeatMap = new CanvasXpress('canvas_coexp',
                                     ${correlationJSON},
                                     {graphType: 'Heatmap',
                                      title: 'Coexpression',
                                      heatmapType: 'yellow-red',
                                      dendrogramSpace: 6,
                                      smpDendrogramPosition: 'right',
                                      varDendrogramPosition: 'bottom',
                                      setMin: ${minCorrelation},
                                      setMax: ${maxCorrelation},
                                      varLabelRotate: 45,
                                      centerData: false,
                                      autoExtend: true},
                                      {click: function(o) {
                                                  alert("Clicked on something.");
                                               }
                                              });
            // cluster on gene/mrnas
            if (bioentity_count > max_cluster) {
                jQuery("#coexpressionHierarchical").attr('disabled', true);
            }
            if (organism_count > 1) {
                jQuery("#coexpressionHierarchical").attr('disabled', true);
            }

            if (bioentity_count > 3 && bioentity_count <= max_cluster) {
                coexpressionHeatMap.clusterSamples();
                coexpressionHeatMap.kmeansSamples();
                coexpressionHeatMap.clusterVariables();
                coexpressionHeatMap.draw();

                for (var i=4; i < bioentity_count; ++i) {
                    jQuery('#coexpressionKMeans').
                              append(jQuery("<option></option>").
                              attr("value",i).
                              text(i));
                }
            } else {
                jQuery("#coexpressionKMeans").attr('disabled', true);
            }

           jQuery('#coexpressionHierarchical').change(function() {
                coexpressionHeatMap.linkage = this.value;
                if (bioentity_count >= 3) { coexpressionHeatMap.clusterSamples(); }
                coexpressionHeatMap.clusterVariables(); 
                coexpressionHeatMap.draw();
           });

           jQuery('#coexpressionKMeans').change(function() {
                coexpressionHeatMap.kmeansClusters = parseInt(this.value);
                coexpressionHeatMap.kmeansSamples();
                coexpressionHeatMap.draw();
           });

     }

</script>

<!-- /correlationMap.jsp -->
