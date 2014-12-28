<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.net.URLEncoder" language="java" %>

<!-- eigenGene.jsp -->

<html:xhtml />

<tiles:importAttribute />

<!--[if IE]><script type="text/javascript" src="model/canvasXpress/js/excanvas.js"></script><![endif]-->
    <script type="text/javascript" src="model/canvasXpress/js/canvasXpress.min.js"></script>

<div class="body" id="eigengene_div">

<script type="text/javascript" charset="utf-8">
jQuery(document).ready(function () {
    var feature_count = parseInt(${FeatureCount});
    var organism_count = parseInt(${OrganismCount});
    if (organism_count > 1) {
        jQuery("#eigengeneGraph").hide();
    } else {
        jQuery("#eigengeneGraph").show();
    }

    jQuery("#bro").click(function () {
       if(jQuery("#eigengeneGraph").is(":hidden")) {
         jQuery("#oc").attr("src", "images/disclosed.gif");
       } else {
         jQuery("#oc").attr("src", "images/undisclosed.gif");
       }
       jQuery("#eigengeneGraph").toggle("slow");
    });
})
</script>

    <div id="eigengene_div">
        <p>
          <h2>
              <c:choose>
                <c:when test="${ExpressionType == 'gene'}">
                  ${WEB_PROPERTIES['eigengene.geneExpressionScoreTitle']}
                </c:when>
                <c:otherwise>
                  ${ExpressionType}
                </c:otherwise>
              </c:choose>
          </h2>
        </p>
        <p>
          <i>
            ${WEB_PROPERTIES['eigengene.expressionScoreSummary']}
            The black line is the first component in the SVD of the normalized expression. 
          </i>
        </p>
        <br/>

        <div id="eigengeneGraph" style="display: block">

        <div id="eigengeneContainer">
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
                  ${WEB_PROPERTIES['eigengene.geneExpressionScoreDescription']}
                </c:when>
                <c:when test="${ExpressionType == 'mrna'}">
                  Expression type is mrna
                  ${WEB_PROPERTIES['eigengene.mrnaExpressionScoreDescription']}
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

         if (organism_count > 1) {
             jQuery('#eigengene_div').remove();
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

</script>

<!-- /eigenGene.jsp -->
