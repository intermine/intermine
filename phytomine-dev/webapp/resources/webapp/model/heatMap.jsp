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
    if (feature_count > 100 || $organism_count > 1) {
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


<!-- hr>
${cufflinksScoreJSONFpkm}
<hr>
${cufflinksScoreJSONCount}
-->

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
                     <span>Count Expression Clustering - Hierarchical:</span>
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
var max_cluster = parseInt(${MAX_CLUSTER});
var max_map = parseInt(${MAX_MAP});

    if ('${fn:length(cufflinksScoreJSONFpkm)}' < 10) {
        jQuery('#heatmap_div').remove();
        jQuery('#expression_div').html('<i>Expression scores are not available</i>');
     } else {

         if (feature_count > max_map) {
             jQuery('#heatmap_div').remove();
             jQuery('#expression_div').html('<i>Too many elements. Please select a subset to see the rna-seq expression heat maps.</i>');
         }
         if (organism_count > 1) {
             jQuery('#heatmap_div').remove();
             jQuery('#expression_div').html('<i>Too many organisms. Please select a subset of genes or mRNAs from only one organism to see the rna-seq expression heat maps.</i>');
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

                                                   if ("${ExpressionType}" == "gene") {
                                                       var query = '<query name="" model="genomic" view="Gene.primaryIdentifier Gene.organism.shortName Gene.cufflinksscores.experiment.name Gene.cufflinksscores.fpkm Gene.cufflinksscores.count Gene.cufflinksscores.conflo Gene.cufflinksscores.confhi Gene.cufflinksscores.countdispersionvar Gene.cufflinksscores.countuncertaintyvar Gene.cufflinksscores.countvariance" sortOrder="Gene.primaryIdentifier asc" constraintLogic="A and B"><constraint path="Gene.primaryIdentifier" code="B" op="=" value="' + featureId + '" /><constraint path="Gene.cufflinksscores.experiment.name" code="A" op="=" value="' + condition + '"/></query>';
                                                       var encodedQuery = encodeURIComponent(query);
                                                       encodedQuery = encodedQuery.replace("%20", "+");
                                                       window.open("/${WEB_PROPERTIES['webapp.path']}/loadQuery.do?skipBuilder=true&query=" + encodedQuery + "%0A++++++++++++&trail=|query&method=xml");

                                                   } else if ("${ExpressionType}" == "mrna") {

                                                       var query = '<query name="" model="genomic" view="MRNA.primaryIdentifier MRNA.organism.shortName MRNA.cufflinksscores.experiment.name MRNA.cufflinksscores.fpkm MRNA.cufflinksscores.count MRNA.cufflinksscores.conflo MRNA.cufflinksscores.confhi MRNA.cufflinksscores.countdispersionvar MRNA.cufflinksscores.countuncertaintyvar MRNA.cufflinksscores.countvariance" sortOrder="MRNA.primaryIdentifier asc" constraintLogic="A and B"><constraint path="MRNA.primaryIdentifier" code="A" op="=" value="' + featureId + '" /><constraint path="MRNA.cufflinksscores.experiment.name" code="B" op="=" value="' + condition + '" /></query>';
                                                       var encodedQuery = encodeURIComponent(query);
                                                       encodedQuery = encodedQuery.replace("%20", "+");
                                                       window.open("/${WEB_PROPERTIES['webapp.path']}/loadQuery.do?skipBuilder=true&query=" + encodedQuery + "%0A++++++++++++&trail=|query&method=xml");

                                                   } else {
                                                      alert("${ExpressionType}");
                                                   }
                                                   // window.open('/${WEB_PROPERTIES['webapp.path']}/portal.do?class=Gene&externalids=' + o.y.smps);
                                                  }}
                                         );
            // cluster on gene/mrnas
            if (feature_count > max_cluster) {
                jQuery("#cl-hc").attr('disabled', true);
            }
            if (organism_count > 1) {
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
                                         ${cufflinksScoreJSONCount},
                                         {graphType: 'Heatmap',
                                          title: 'Read Count',
                                          // heatmapType: 'yellow-purple',
                                          dendrogramSpace: 6,
                                          smpDendrogramPosition: 'right',
                                          setMin: ${minCountCufflinksScore},
                                          setMax: ${maxCountCufflinksScore},
                                          varLabelRotate: 45,
                                          centerData: false,
                                          autoExtend: true},
                                          {click: function(o) {
                                                   var featureId = o.y.vars;
                                                   var condition = o.y.smps;

                                                   if ("${ExpressionType}" == "gene") {

                                                       var query = '<query name="" model="genomic" view="Gene.primaryIdentifier Gene.organism.shortName Gene.cufflinksscores.experiment.name Gene.cufflinksscores.fpkm Gene.cufflinksscores.count Gene.cufflinksscores.conflo Gene.cufflinksscores.confhi Gene.cufflinksscores.countdispersionvar Gene.cufflinksscores.countuncertaintyvar Gene.cufflinksscores.countvariance" sortOrder="Gene.primaryIdentifier asc" constraintLogic="A and B"><constraint path="Gene.primaryIdentifier" code="B" op="=" value="' + featureId + '" /><constraint path="Gene.cufflinksscores.experiment.name" code="A" op="=" value="' + condition + '"/></query>';
                                                       var encodedQuery = encodeURIComponent(query);
                                                       encodedQuery = encodedQuery.replace("%20", "+");
                                                       window.open("/${WEB_PROPERTIES['webapp.path']}/loadQuery.do?skipBuilder=true&query=" + encodedQuery + "%0A++++++++++++&trail=|query&method=xml");

                                                   } else if ("${ExpressionType}" == "mrna") {

                                                       var query = '<query name="" model="genomic" view="MRNA.primaryIdentifier MRNA.organism.shortName MRNA.cufflinksscores.experiment.name MRNA.cufflinksscores.fpkm MRNA.cufflinksscores.count MRNA.cufflinksscores.conflo MRNA.cufflinksscores.confhi MRNA.cufflinksscores.countdispersionvar MRNA.cufflinksscores.countuncertaintyvar MRNA.cufflinksscores.countvariance" sortOrder="MRNA.primaryIdentifier asc" constraintLogic="A and B"><constraint path="MRNA.primaryIdentifier" code="A" op="=" value="' + featureId + '" /><constraint path="MRNA.cufflinksscores.experiment.name" code="B" op="=" value="' + condition + '" /></query>';
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
            if (organism_count > 1) {
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
