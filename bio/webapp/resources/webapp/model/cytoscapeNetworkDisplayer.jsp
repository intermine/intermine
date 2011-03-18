<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- cytoscapeNetworkDisplayer.jsp -->

<html:xhtml/>

<link type="text/css" rel="stylesheet" href="model/jquery_ui/css/smoothness/jquery-ui-1.8.10.custom.css"/>
<style type="text/css">
    /* The Cytoscape Web container must have its dimensions set. */
    html, body { height: 100%; width: 100%; padding: 0; margin: 0; }
    /* use absolute value */
    #cwcontent { width: 600px; height: 485px; border: 2px solid #CCCCCC; padding: 0 1em 1em 0; -moz-border-radius: 5px 5px 5px 5px;}
    #cwtabsbyside { display: none; clear: right; float: right; overflow: auto; width: 460px; }
    #cwinlinetable { display: none; padding: 5px 0 0 0;}
    fieldset { border: 1px solid #CCCCCC; margin-bottom: 0.5em; padding: 0.8em 1em; }
    label { vertical-align: middle; }
    #legend h3 { -moz-border-radius: 5px 5px 0 0; background: none repeat scroll 0 0 #CCCCCC; margin: 0; padding: 4px 5px; color: black; border-top-style: none; }
    #legend p {
    border-color: -moz-use-text-color #BBBBBB #BBBBBB;
    border-right: 1px solid #BBBBBB;
    border-style: none solid solid;
    border-width: medium 1px 1px;
    margin: 0;
    padding: 5px;
}
    #legend table {
    border-color: -moz-use-text-color #BBBBBB #BBBBBB;
    border-right: 1px solid #BBBBBB;
    border-style: none solid solid;
    border-width: medium 1px 1px;
    margin: 0;
    padding: 5px;
}
    #legend { padding: 0.2em 0.4em 0.4em; }
    #powerby { padding: 5px; text-align: center; }
    #powerby a { color: rgb(136, 136, 136); text-decoration: none; background-color: white; }
    #powerby img { vertical-align: middle; }

    #svgtable {
    border: 2px solid #CCCCCC;
    border-collapse: separate;
    border-spacing: 1px;
    clear: both;
    width: 100%;
    }

    #legends { height: 50px; }

</style>
<div id="cwhead">
    <h3>Interaction Network</h3>
</div>

    <!--
    <div id="menu">
    </div>
    <div id="legends">
    </div>

    -->
<div id="cwtabsbyside">
	<ul>
		<li><a href="#tabs-controls">Controls</a></li>
		<li><a href="#tabs-data">Data</a></li>
		<li><a href="#tabs-help">Help</a></li>
	</ul>
	<div id="tabs-controls">
		<div>
		  <fieldset>
            <label>Show:</label><br>
            <input type="radio" name="showInteractions" onclick="vis.filter('edges', function(edge) { return edge.color; });" checked><label>All Interactions</label><br>
            <input type="radio" name="showInteractions" onclick="vis.filter('edges', function(edge) { return edge.color >= '#FF0000'; });"><label>Physical Interactions</label><br>
            <input type="radio" name="showInteractions" onclick="vis.filter('edges', function(edge) { return edge.color <= '#FF0000'; });"><label>Genetic Interactions</label>
          </fieldset>
		  <fieldset>
            <label>Export network as:</label>
            <select id="exportoptions">
                <option value="xgmml" selected>XGMML</option>
                <option value="sif">SIF</option>
                <option value="svg">SVG</option>
                <option value="tab">TSV</option>
                <option value="csv">CSV</option>
            </select>
            <input type="button" id="exportbutton" value="Export">
		  </fieldset>
		  <fieldset>
		    <label class="fakelink" onclick="url = webapp_baseurl + '/' + webapp_path + '/saveFromIdsToBag.do?type=Gene&ids='+fullInteractingGeneSet+'&source=objectDetails&newBagName=interacting_gene_list'; window.open(url);">Create a gene list...</lable>
		  </fieldset>
		  <fieldset>
		    <label>View interaction data in a table</lable>
		    <input type="button" value="Toggle" onclick="jQuery('#cwinlinetable').toggle('blind', {}, 1000);">
		  </fieldset>
		</div>
	</div>
	<div id="tabs-data">
		<div>to be implemented...</div>
	</div>
	<div id="tabs-help">
	  <div id="legend">
	    <h3>Interaction Type</h3>
        <table id="svgtable">
          <tr>
            <td id="legends">
            </td>
          </tr>
        </table>
	  </div>
	  <div id="powerby">
	      <a onmouseout="this.style.backgroundColor='white';" onmouseover="this.style.backgroundColor='#f1f1d1';" title="Cytoscape Web" target="_blank" href="http://cytoscapeweb.cytoscape.org">
	        Powered by <img border="0/" src="model/images/cytoscape_logo_small.png" height="15" width="15"> Cytoscape Web
	      </a>
      </div>
    </div>
</div>
<div id="cwcontent"></div>
<div id="cwinlinetable" class="box table">
	<h3>Interactions</h3>
	<tiles:insert name="resultsTable.tile">
	     <tiles:put name="pagedResults" beanName="cytoscapeNetworkPagedResults" />
	     <tiles:put name="currentPage" value="objectDetails" />
	     <tiles:put name="inlineTable" value="true" />
	</tiles:insert>
</div>

<!-- Flash embedding utility (needed to embed Cytoscape Web) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/AC_OETags.min.js'/>"></script>

<!-- JSON support for IE (needed to use JS API) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/json2.min.js'/>"></script>

<!-- Cytoscape Web JS API (needed to reference org.cytoscapeweb.Visualization) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/cytoscapeweb.min.js'/>"></script>

<script type="text/javascript" src="<html:rewrite page='/model/jquery_qtip/jquery.qtip-1.0.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/model/jquery_ui/jquery-ui-1.8.10.custom.min.js'/>"></script>
<link href="model/jquery_svg/jquery.svg.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="<html:rewrite page='/model/jquery_svg/jquery.svg.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/displaynetwork.js'/>"></script>
<script type="text/javascript">

    // from controller
    var fullInteractingGeneSet = '${fullInteractingGeneSet}'; // a string arrray of gene object store ids
    var dataNotIncludedMessage = '${dataNotIncludedMessage}'; // case: interaction data is not integrated
    var orgWithNoDataMessage = '${orgWithNoDataMessage}'; // case: no interaction data for the whole species

    var project_title = "${WEB_PROPERTIES['project.title']}";

    var target = "#cwcontent";

    if (dataNotIncludedMessage != "") {
        jQuery(target).html(dataNotIncludedMessage)
                           .css('font-style','italic');
    }
    else if (orgWithNoDataMessage != "") {
      jQuery(target).html(orgWithNoDataMessage)
                               .css('font-style','italic')
                               .height(20)
                               .width(600);
    }
    else {

        // AJAX POST
        jQuery(target).html("Please wait while the network data loads...");
        jQuery.ajax({
            type: "POST",
            url: "cytoscapeNetworkAjax.do",
            dataType: "text",
            data: "fullInteractingGeneSet=" + encodeURIComponent('${fullInteractingGeneSet}'),
            success: function(response) {
                if (response.match("^"+"No interaction data found from data sources:")) {
                    geneWithNoDatasourceMessage = response; // case: no interaction data found from the data sources
                    jQuery(target).html(geneWithNoDatasourceMessage)
                                             .css('font-style','italic')
                                             .height(20)
                                             .width(60);
                } else {
                    networkdata = response;
                    displayNetwork(networkdata, fullInteractingGeneSet);
                    jQuery("#cwtabsbyside").tabs();
                    jQuery("#cwtabsbyside").show();
                }
            },
            error:function (xhr, ajaxOptions, thrownError) {
                jQuery(target).html("There was a problem retrieving the result.");
            }
        });
    }

    jQuery('#legends').svg();
    var legends = jQuery('#legends').svg('get');
    legends.line(20, 5, 80, 5, {stroke: "red", strokeWidth: 4});
    legends.polygon([[80, 0], [80, 10], [100, 5]], {fill: "red"});
    legends.text(5, 27, "Physical");

/*
    jQuery('#legend_genetic').svg();
    var legend_genetic = jQuery('#legend_genetic').svg('get');
    legend_genetic.line(20, 5, 80, 5, {stroke: "blue", strokeWidth: 4});
    legend_genetic.polygon([[80, 0], [80, 10], [100, 5]], {fill: "blue"});
    legend_genetic.text(5, 27, "Genetic");
*/
    jQuery("#exportoptions").change(function () {
        export(jQuery("#exportoptions option:selected").val());
    });

    jQuery("#exportbutton").click(function () {
		export(jQuery("#exportoptions option:selected").val());
    });

    function export(type) {
        if (type=="tab" || type=="csv") {
			vis.exportNetwork(type, 'cytoscapeNetworkExport.do?type=' + type + '&fullInteractingGeneSet='+fullInteractingGeneSet); }
        else {
        	vis.exportNetwork(type, 'cytoscapeNetworkExport.do?type='+type); }
    }

</script>

<!-- /cytoscapeNetworkDisplayer.jsp -->
