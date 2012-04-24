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
    if (feature_count > 100) {
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

<%--
<hr>
${expressionScoreJSONCellLine}
<hr>
--%>
    <div id="heatmap_div">
        <p>
          <h2>
              <c:choose>
                <c:when test="${ExpressionType == 'gene'}">
                  ${WEB_PROPERTIES['heatmap.geneExpressionScoreTitle']}
                </c:when>
                <c:when test="${ExpressionType == 'exon'}">
                  ${WEB_PROPERTIES['heatmap.exonExpressionScoreTitle']}
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
            <a href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=Drosophila Cell Line and Developmental Stage Gene and Exon Scores"> the Celniker group</a>
            and are log2 of the actual value.
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
                     <span>Cell Line Clustering - Hierarchical:</span>
                     <select id="cl-hc">
                         <option value="single" selected="selected">Single</option>
                         <option value="complete">Complete</option>
                         <option value="average">Average</option>
                     </select>
                     <span> and K-means:</span>
                     <select id="cl-km">
                         <option value="3" selected="selected">3</option>
                     </select>
                    </div>
                    <canvas id="canvas_cl" width="525" height="550"></canvas>
                </td>
                <td>
                     <div style="padding: 0px 0px 5px 30px;">
                     <span>Developmental Stage Clustering - Hierarchical:</span>
                     <select id="ds-hc">
                         <option value="single" selected="selected">Single</option>
                         <option value="complete">Complete</option>
                         <option value="average">Average</option>
                     </select>
                     <span> and K-means:</span>
                     <select id="ds-km">
                         <option value="3" selected="selected">3</option>
                     </select>
                    </div>
                     <canvas id="canvas_ds" width="550" height="550"></canvas>
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
                  ${WEB_PROPERTIES['heatmap.geneExpressionScoreDescription']}
                </c:when>
                <c:when test="${ExpressionType == 'exon'}">
                  ${WEB_PROPERTIES['heatmap.exonExpressionScoreDescription']}
                </c:when>
                <c:otherwise>
                  ${ExpressionType}
                </c:otherwise>
              </c:choose>
            <br>Further information: check the <a href="/${WEB_PROPERTIES['webapp.path']}/portal.do?class=Submission&externalids=modENCODE_3305">
modENCODE submission</a>, with links to the original score files for <a href="http://submit.modencode.org/submit/public/get_file/3305/extracted/Drosophila_Cell_Lines_and_Developmental_Stages_Gene_Scores.txt" target="_blank">genes</a>
            and <a href="http://submit.modencode.org/submit/public/get_file/3305/extracted/Drosophila_Cell_Lines_and_Developmental_Stages_Exon_Scores.txt" target="_blank">exons</a>.
            </i>
        </div>
    </div>
</div>
</div>


<script type="text/javascript">
var feature_count = parseInt(${FeatureCount});
var max_cluster = parseInt(${MAX_CLUSTER});
var max_map = parseInt(${MAX_MAP});

    if ('${fn:length(expressionScoreJSONCellLine)}' < 10) {
        jQuery('#heatmap_div').remove();
        jQuery('#expression_div').html('<i>Expression scores are not available</i>');
     } else {

         if (feature_count > max_map) {
             jQuery('#heatmap_div').remove();
             jQuery('#expression_div').html('<i>Too many elements, please select a subset to see the heat maps.</i>');
         }

         jQuery("#description").hide();

         jQuery("#description_div").click(function () {
               if(jQuery("#description").is(":hidden")) {
                 jQuery("#co").attr("src", "images/disclosed.gif");
               } else {
                 jQuery("#co").attr("src", "images/undisclosed.gif");
               }
               jQuery("#description").toggle("slow");
            });


           // hm - heatmap; cl - cellline; ds - developmentalstage; hc - hierarchical clustering; km - kmeans
            var hm_cl = new CanvasXpress('canvas_cl',
                                         ${expressionScoreJSONCellLine},
                                         {graphType: 'Heatmap',
                                          title: 'Cell Line',
                                          // heatmapType: 'yellow-purple',
                                          dendrogramSpace: 6,
                                          smpDendrogramPosition: 'right',
                                          varDendrogramPosition: 'bottom',
                                          setMin: ${minExpressionScore},
                                          setMax: ${maxExpressionScore},
                                          varLabelRotate: 45,
                                          centerData: false,
                                          autoExtend: true},
                                          {click: function(o) {
                                                   var featureId = o.y.smps;
                                                   var condition = o.y.vars;

                                                   if ("${ExpressionType}" == "gene") {

                                                       var query = '<query name="" model="genomic" view="GeneExpressionScore.score GeneExpressionScore.cellLine.name GeneExpressionScore.gene.primaryIdentifier GeneExpressionScore.gene.secondaryIdentifier GeneExpressionScore.gene.symbol GeneExpressionScore.gene.name GeneExpressionScore.gene.source GeneExpressionScore.organism.shortName GeneExpressionScore.submission.title GeneExpressionScore.submission.design GeneExpressionScore.submission.DCCid" sortOrder="GeneExpressionScore.score asc" constraintLogic="A and B"><constraint path="GeneExpressionScore.gene" code="B" op="LOOKUP" value="' + featureId + '" extraValue=""/><constraint path="GeneExpressionScore.cellLine" code="A" op="LOOKUP" value="' + condition + '"/></query>';
                                                       var encodedQuery = encodeURIComponent(query);
                                                       encodedQuery = encodedQuery.replace("%20", "+");
                                                       window.open("/${WEB_PROPERTIES['webapp.path']}/loadQuery.do?skipBuilder=true&query=" + encodedQuery + "%0A++++++++++++&trail=|query&method=xml");

                                                   } else if ("${ExpressionType}" == "exon") {

                                                       var query = '<query name="" model="genomic" view="ExonExpressionScore.score ExonExpressionScore.cellLine.name ExonExpressionScore.exon.primaryIdentifier ExonExpressionScore.exon.symbol  ExonExpressionScore.exon.gene.primaryIdentifier ExonExpressionScore.exon.gene.symbol ExonExpressionScore.organism.shortName ExonExpressionScore.submission.title ExonExpressionScore.submission.design ExonExpressionScore.submission.DCCid" sortOrder="ExonExpressionScore.exon.primaryIdentifier asc" constraintLogic="A and B"><constraint path="ExonExpressionScore.exon" code="A" op="LOOKUP" value="' + featureId + '" extraValue=""/><constraint path="ExonExpressionScore.cellLine" code="B" op="LOOKUP" value="' + condition + '" extraValue=""/></query>';
                                                       var encodedQuery = encodeURIComponent(query);
                                                       encodedQuery = encodedQuery.replace("%20", "+");
                                                       window.open("/${WEB_PROPERTIES['webapp.path']}/loadQuery.do?skipBuilder=true&query=" + encodedQuery + "%0A++++++++++++&trail=|query&method=xml");

                                                   } else {
                                                      alert("${ExpressionType}");
                                                   }
                                                   // window.open('/${WEB_PROPERTIES['webapp.path']}/portal.do?class=Gene&externalids=' + o.y.smps);
                                                  }}
                                         );
            // cluster on gene/exons
            if (feature_count > max_cluster) {
                jQuery("#cl-hc").attr('disabled', true);
            }

            if (feature_count > 3 && feature_count <= max_cluster) {
                hm_cl.clusterSamples();
                hm_cl.kmeansSamples();

                for (var i=4; i < feature_count; ++i) {
                    jQuery('#cl-km').
                              append(jQuery("<option></option>").
                              attr("value",i).
                              text(i));
                }

            } else {
                jQuery("#cl-km").attr('disabled', true);
            }

            // cluster on conditions
            if (feature_count <= max_cluster) {
                hm_cl.clusterVariables(); // clustering method will call draw action within it.
                hm_cl.draw();
            }
            // cx_cellline.kmeansVariables();
//            hm_cl.draw();

            var hm_ds = new CanvasXpress('canvas_ds',
                                         ${expressionScoreJSONDevelopmentalStage},
                                         {graphType: 'Heatmap',
                                          title: 'Developmental Stage',
                                          // heatmapType: 'yellow-purple',
                                          dendrogramSpace: 6,
                                          smpDendrogramPosition: 'right',
                                          setMin: ${minExpressionScore},
                                          setMax: ${maxExpressionScore},
                                          varLabelRotate: 45,
                                          centerData: false,
                                          autoExtend: true},
                                          {click: function(o) {
                                                   var featureId = o.y.smps;
                                                   var condition = o.y.vars;

                                                   if ("${ExpressionType}" == "gene") {

                                                       var query = '<query name="" model="genomic" view="GeneExpressionScore.score GeneExpressionScore.developmentalStage.name GeneExpressionScore.gene.primaryIdentifier GeneExpressionScore.gene.secondaryIdentifier GeneExpressionScore.gene.symbol GeneExpressionScore.gene.name GeneExpressionScore.gene.source GeneExpressionScore.organism.shortName GeneExpressionScore.submission.title GeneExpressionScore.submission.design GeneExpressionScore.submission.DCCid" sortOrder="GeneExpressionScore.score asc" constraintLogic="A and B"><constraint path="GeneExpressionScore.gene" code="B" op="LOOKUP" value="' + featureId + '" extraValue=""/><constraint path="GeneExpressionScore.developmentalStage" code="A" op="LOOKUP" value="' + condition + '"/></query>';
                                                       var encodedQuery = encodeURIComponent(query);
                                                       encodedQuery = encodedQuery.replace("%20", "+");
                                                       window.open("/${WEB_PROPERTIES['webapp.path']}/loadQuery.do?skipBuilder=true&query=" + encodedQuery + "%0A++++++++++++&trail=|query&method=xml");

                                                   } else if ("${ExpressionType}" == "exon") {

                                                       var query = '<query name="" model="genomic" view="ExonExpressionScore.score ExonExpressionScore.developmentalStage.name ExonExpressionScore.exon.primaryIdentifier ExonExpressionScore.exon.symbol  ExonExpressionScore.exon.gene.primaryIdentifier ExonExpressionScore.exon.gene.symbol ExonExpressionScore.organism.shortName ExonExpressionScore.submission.title ExonExpressionScore.submission.design ExonExpressionScore.submission.DCCid" sortOrder="ExonExpressionScore.exon.primaryIdentifier asc" constraintLogic="A and B"><constraint path="ExonExpressionScore.exon" code="A" op="LOOKUP" value="' + featureId + '" extraValue=""/><constraint path="ExonExpressionScore.developmentalStage" code="B" op="LOOKUP" value="' + condition + '" extraValue=""/></query>';
                                                       var encodedQuery = encodeURIComponent(query);
                                                       encodedQuery = encodedQuery.replace("%20", "+");
                                                       window.open("/${WEB_PROPERTIES['webapp.path']}/loadQuery.do?skipBuilder=true&query=" + encodedQuery + "%0A++++++++++++&trail=|query&method=xml");

                                                   } else {
                                                      alert("${ExpressionType}");
                                                   }
                                                   // window.open('/${WEB_PROPERTIES['webapp.path']}/portal.do?class=Gene&externalids=' + o.y.smps);
                                                  }}
                                         );

            if (feature_count > max_cluster) {
                jQuery("#ds-hc").attr('disabled', true);
            }
            if (feature_count > 3 && feature_count <= max_cluster) {
                hm_ds.clusterSamples();
                hm_ds.kmeansSamples();

                for (var i=4; i < feature_count; ++i) {
                    jQuery('#ds-km').
                              append(jQuery("<option></option>").
                              attr("value",i).
                              text(i));
                }

            } else {
                jQuery("#ds-km").attr('disabled', true);
            }

            hm_ds.draw();

           jQuery('#cl-hc').change(function() {
                hm_cl.linkage = this.value;
                if (feature_count >= 3) { hm_cl.clusterSamples(); }
                hm_cl.clusterVariables();
                hm_cl.draw();
           });

           jQuery('#cl-km').change(function() {
                hm_cl.kmeansClusters = parseInt(this.value);
                hm_cl.kmeansSamples();
                // hm_cl.kmeansVariables();
                hm_cl.draw();
           });

           jQuery('#ds-hc').change(function() {
                hm_ds.linkage = this.value;
                hm_ds.clusterSamples();
                hm_ds.draw();
            });

            jQuery('#ds-km').change(function() {
                hm_ds.kmeansClusters = parseInt(this.value);
                hm_ds.kmeansSamples();
                hm_ds.draw();
           });

     }

    </script>

<!-- /heatMap.jsp -->