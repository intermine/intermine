<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.net.URLEncoder" language="java" %>

<!-- heatMap.jsp -->

<html:xhtml />

<tiles:importAttribute />

<!--[if IE]><script type="text/javascript" src="model/canvasXpress/js/excanvas.js"></script><![endif]-->
    <script type="text/javascript" src="model/canvasXpress/js/canvasXpress.min.js"></script>

<div class="body" id="expression_div">

<script type="text/javascript" charset="utf-8">
jQuery(document).ready(function () {
    var feature_count = parseInt(${FeatureCount});
    var organism_count = parseInt(${OrganismCount});
    if (feature_count > 100 || organism_count > 1) {
        jQuery("#heatmapGraph").hide();
    } else {
        jQuery("#heatmapGraph").show();
    }

    jQuery("#bro").click(function () {
       if(jQuery("#heatmapGraph").is(":hidden")) {
         jQuery("#oc").attr("src", "images/disclosed.gif");
       } else {
         jQuery("#oc").attr("src", "images/undisclosed.gif");
       }
       jQuery("#heatmapGraph").toggle("slow");
    });
})
</script>

<c:set var="MAX_CLUSTER" value="250" />
<c:set var="MAX_MAP" value="600" />
<c:set var="MAX_DEFAULT_OPEN" value="100" />


    <div id="heatmap_div">
        <p>
          <h2>
              <c:choose>
                <c:when test="${ExpressionType == 'gene'}">
                  ${WEB_PROPERTIES['heatmap.geneExpressionScoreTitle']}
                </c:when>
                <c:when test="${ExpressionType == 'mrna'}">
                  ${WEB_PROPERTIES['heatmap.mrnaExpressionScoreTitle']}
                </c:when>
                <c:otherwise>
                  ${ExpressionType}
                </c:otherwise>
              </c:choose>
          </h2>
        </p>
        <p>
          <i>
            ${WEB_PROPERTIES['heatmap.expressionScoreSummary']}
            The plotted values and are log2 of the actual value.
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

        <div id="heatmapGraph" style="display: block">

        <c:if test="${FeatureCount > MAX_CLUSTER}">
        Please note that clustering functions are not available for lists with more than ${MAX_CLUSTER} elements.
        <br>
        </c:if>

        <div id="heatmapContainer">
            <table>
              <tr>
                <td>
                    <div style="padding: 0px 0px 5px 30px;">
                     <span>FPKM Expression Clustering - Hierarchical:</span>
                     <select id="fpkmHierarchical">
                         <option value="single" selected="selected">Single</option>
                         <option value="complete">Complete</option>
                         <option value="average">Average</option>
                     </select>
                     <span> and K-means:</span>
                     <select id="fpkmKMeans">
                         <option value="3" selected="selected">3</option>
                     </select>
                    </div>
                    <canvas id="canvas_fp" width="825" height="550"></canvas>
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
                  ${WEB_PROPERTIES['heatmap.geneExpressionScoreDescription']}
                </c:when>
                <c:when test="${ExpressionType == 'mrna'}">
                  Expression type is mrna
                  ${WEB_PROPERTIES['heatmap.mrnaExpressionScoreDescription']}
                </c:when>
                <c:otherwise>
                  Expression type is not mrna or gene
                  ${ExpressionType}
                </c:otherwise>
              </c:choose>
            <br>FPKM values derived from a <em>cufflinks</em> analysis of aligned RNA-seq data.
            </i>
        </div>
    </div>
</div>
</div>


<script type="text/javascript">
    var feature_count = parseInt(${FeatureCount});
    var organism_count = parseInt(${OrganismCount});
    var experiment_count = parseInt(${ExperimentCount});
    var max_cluster = parseInt(${MAX_CLUSTER});
    var max_map = parseInt(${MAX_MAP});

    if ('${fn:length(cufflinksScoreJSONFpkm)}' < 10) {
        // if the JSON string is short, no data.
        jQuery('#heatmap_div').remove();
        jQuery('#expression_div').html('<i>Expression scores are not available</i>');
    } else if (feature_count > max_map) {
        jQuery('#heatmap_div').remove();
        jQuery('#expression_div').html('<i>Too many elements. Please select a subset to see the rna-seq expression heat maps.</i>');
    } else if (organism_count > 1) {
        jQuery('#heatmap_div').remove();
        jQuery('#expression_div').html('<i>Too many organisms. Please select a subset of genes or mRNAs from only one organism to see the rna-seq expression heat maps.</i>');
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


        // the FPKM heat map
        var fpkmHeatMap = new CanvasXpress('canvas_fp',
                                     ${cufflinksScoreJSONFpkm},
                                     {graphType: 'Heatmap',
                                      title: 'FPKM',
                                      // heatmapType: 'yellow-purple',
                                      dendrogramSpace: 6,
                                      smpDendrogramPosition: 'right',
                                      varDendrogramPosition: 'bottom',
                                      setMin: ${minFpkmCufflinksScore},
                                      setMax: ${maxFpkmCufflinksScore},
                                      varLabelRotate: 45,
                                      centerData: false,
                                      autoExtend: true},
                                      {click: function(o) {
                                               var featureId = o.y.vars;
                                               var condition = o.y.smps;
                                               // take the feature and experiment list and nest them
                                               // into <value>..</value> tags
                                               var featureConstraint = featureId.map(function(a)
                                                              { return "<value>"+a+"</value>" }).join("");
                                               var conditionConstraint = condition.map(function(a)
                                                              { return "<value>"+a+"</value>" }).join("");

                                               if ("${ExpressionType}" == "gene") {
                                                   var query = '<query name="" model="genomic" view="Gene.primaryIdentifier Gene.organism.shortName Gene.cufflinksscores.experiment.name Gene.cufflinksscores.experiment.experimentGroup Gene.cufflinksscores.fpkm Gene.cufflinksscores.conflo Gene.cufflinksscores.confhi" sortOrder="Gene.primaryIdentifier asc" constraintLogic="A and B">'+
'<constraint path="Gene.primaryIdentifier" code="A" op="ONE OF">' + featureConstraint+ '</constraint>'+
'<constraint path="Gene.cufflinksscores.experiment.name" code="B" op="ONE OF">'+conditionConstraint+'</constraint></query>';
                                                   var encodedQuery = encodeURIComponent(query);
                                                   encodedQuery = encodedQuery.replace("%20", "+");
                                                   window.open("/${WEB_PROPERTIES['webapp.path']}/loadQuery.do?skipBuilder=true&query=" + encodedQuery  + "&trail=|query&method=xml");

                                               } else if ("${ExpressionType}" == "mrna") {
                                                   var query = '<query name="" model="genomic" view="MRNA.primaryIdentifier MRNA.organism.shortName MRNA.cufflinksscores.experiment.name MRNA.cufflinksscores.experiment.experimentGroup MRNA.cufflinksscores.fpkm MRNA.cufflinksscores.conflo MRNA.cufflinksscores.confhi" sortOrder="MRNA.primaryIdentifier asc" constraintLogic="A and B">'+
'<constraint path="MRNA.primaryIdentifier" code="A" op="ONE OF">'+featureConstraint+'</constraint>'+
'<constraint path="Gene.cufflinksscores.experiment.name" code="B" op="ONE OF">'+conditionConstraint+'</constraint></query>';
                                                   var encodedQuery = encodeURIComponent(query);
                                                   encodedQuery = encodedQuery.replace("%20", "+");
                                                   window.open("/${WEB_PROPERTIES['webapp.path']}/loadQuery.do?skipBuilder=true&query=" + encodedQuery + "&method=xml");

                                               } else {
                                                  alert("Unexpected expression type: ${ExpressionType}");
                                               }
                                              }});
            // cluster on gene/mrnas
            if (feature_count > max_cluster) {
                jQuery("#fpkmHierarchical").attr('disabled', true);
            }
            if (organism_count > 1) {
                jQuery("#fpkmHierarchical").attr('disabled', true);
            }

            if (experiment_count > 3 && experiment_count <= max_cluster) {
                fpkmHeatMap.clusterSamples();
                fpkmHeatMap.kmeansSamples();

                for (var i=4; i < experiment_count; ++i) {
                    jQuery('#fpkmKMeans').
                              append(jQuery("<option></option>").
                              attr("value",i).
                              text(i));
                }
            } else {
                jQuery("#fpkmKMeans").attr('disabled', true);
            }

            // cluster on conditions
            if (experiment_count <= max_cluster) {
                fpkmHeatMap.clusterVariables(); // clustering method will call draw action within it.
                fpkmHeatMap.draw();
            }
            // cx_cellline.kmeansVariables();
            // fpkmHeatMap.draw();

           jQuery('#fpkmHierarchical').change(function() {
                fpkmHeatMap.linkage = this.value;
                if (feature_count >= 3) { fpkmHeatMap.clusterSamples(); }
                fpkmHeatMap.clusterVariables(); 
                fpkmHeatMap.draw();
           });

           jQuery('#fpkmKMeans').change(function() {
                fpkmHeatMap.kmeansClusters = parseInt(this.value);
                fpkmHeatMap.kmeansSamples();
                // fpkmHeatMapcl.kmeansVariables();
                fpkmHeatMap.draw();
           });

     }

</script>

<!-- /heatMap.jsp -->
