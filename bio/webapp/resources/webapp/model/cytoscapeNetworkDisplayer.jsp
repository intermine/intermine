<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- cytoscapeNetworkDisplayer.jsp -->

<html:xhtml/>

<script type="text/javascript">
(function() {
    project_title = "${WEB_PROPERTIES['project.title']}";
    project_baseurl = "${WEB_PROPERTIES['webapp.baseurl']}";
    project_path = "${WEB_PROPERTIES['webapp.path']}";

    // from controller
    fullInteractingGeneSet = '${fullInteractingGeneSet}'; // a string arrray of gene object store ids
    dataNotIncludedMessage = '${dataNotIncludedMessage}'; // case: interaction data is not integrated
    orgWithNoDataMessage = '${orgWithNoDataMessage}'; // case: no interaction data for the whole species
})();
</script>

<style type="text/css">
    /* The Cytoscape Web container must have its dimensions set. */
    html, body { height: 100%; width: 100%; padding: 0; margin: 0; }
    /* use absolute value */
    #cwcontent { width: 600px; height: 485px; border: 2px solid #CCCCCC; padding: 0 1em 1em 0;
        -moz-border-radius: 5px 5px 5px 5px; border-radius: 5px 5px 5px 5px; float:left; }
    #cwtabsbyside { float: right; width: 300px; display:none; }
    #cwinlinetable { display: none; padding: 5px 0 0 0;}
    label { vertical-align: middle; }
    #powerby { padding: 5px; text-align: center; }
    #powerby a { color: rgb(136, 136, 136); text-decoration: none; background-color: white; }
    #powerby img { vertical-align: middle; }

    #legendall { width:100%; height:50px; }

    #interactions-wrap div.inside { min-width:1040px; }

    <%-- sidebar toolbar bar --%>
    #cwtabsbyside ul { border-bottom-width:1px; border-bottom-style:solid; font-size:14px; margin:0; }
    #cwtabsbyside ul li { display:inline; border-right-width:1px; border-right-style:solid; }
    #cwtabsbyside ul li a { padding:4px; }
    #cwtabsbyside ul li a.active { font-weight:bold; }
    #cwtabsbyside ul li.last { border:none; }
    #cwtabsbyside fieldset, #cwtabsbyside #legend { border:none; border-bottom-width:1px; border-bottom-style:solid; padding:8px; }

    #cwtabsbyside #formats ul li { display:block; font-size:12px; margin-bottom:4px; }
</style>

<div id="cwhead">
    <h3 class="goog">Interaction Network</h3>
</div>

<div id="interactions-wrap">
  <div class="inside">
  <div id="cwtabsbyside">
    <ul>
      <li><a class="active" href="#tabs-controls">Controls</a></li>
      <li class="last"><a href="#tabs-help">Help</a></li>
    </ul>
    <div id="tabs-controls">
      <div>
        <fieldset>
              <label>Show:</label><br>
              <input type="radio" name="showInteractions" onclick="vis.filter('edges', function(edge) { return edge.color; });" checked><label>All Interactions</label><br>
              <input type="radio" name="showInteractions" onclick="vis.filter('edges', function(edge) { return edge.color >= '#FF0000'; });"><label>Physical Interactions</label><br>
              <input type="radio" name="showInteractions" onclick="vis.filter('edges', function(edge) { return edge.color <= '#FF0000'; });"><label>Genetic Interactions</label>
        </fieldset>

        <fieldset class="alt">
              <label>Export network as:</label>
              <select id="exportoptions">
                  <option value="xgmml" selected>XGMML</option>
                  <option value="sif">SIF</option>
                  <option value="png">PNG</option>
                  <option value="svg">SVG</option>
                  <option value="tab">TSV</option>
                  <option value="csv">CSV</option>
              </select>
              <input type="button" id="exportbutton" value="Export">
        </fieldset>

        <fieldset>
          <label class="fakelink" onclick="window.open(project_baseurl+ '/' + project_path + '/saveFromIdsToBag.do?type=Gene&ids=' + fullInteractingGeneSet + '&source=objectDetails&newBagName=interacting_gene_list');">Create a gene list...</label>
        </fieldset>

        <fieldset class="alt">
          <label>View interaction data in a table</lable>
          <input type="button" id="toggleTable" value="Toggle">
        </fieldset>
      </div>
    </div>
    <div id="tabs-data"></div>
    <div id="tabs-help">
      <div id="legend">
        <h4>Interaction Type</h4>
        <div id="legendall"></div>
      </div>
      <div id="formats" class="theme-3-border theme-6-background">
        <h4>Export formats</h4>
        <ul class="formats">
            <li class="external"><a target="_blank" href="http://www.cs.rpi.edu/research/groups/pb/punin/public_html/XGMML/index.html">XGMML</a>: the eXtensible Graph Markup and Modeling Language
                is an XML format which is used for graph description. Cytoscape desktop loads and saves networks and node/edge attributes in XGMML as well.
            </li>
            <li class="external"><a target="_blank" href="http://cytoscape.wodaklab.org/wiki/Cytoscape_User_Manual/Network_Formats/">SIF</a>: A simpler text format
                 that can be very useful if you do not need to set custom nodes/edges attributes.
            </li>
            <li class="external"><a target="_blank" href="http://www.w3.org/Graphics/SVG/">SVG</a>: Scalable Vector Graphics defines the graphics in XML format and does not lose any quality if they are zoomed or resized.
            </li>
            <li class="external"><a target="_blank" href="http://www.w3.org/Graphics/SVG/">PNG</a>: Portable Network Graphics, a popular graphics format over the web.
            </li>
            <li class="external">CSV/TSB: comma or tab separated values, suitable for import into Excel.
            </li>
        </ul>
      </div>
      <div id="powerby">
          <a onmouseout="this.style.backgroundColor='white';" onmouseover="this.style.backgroundColor='#f1f1d1';" title="Cytoscape Web" target="_blank" href="http://cytoscapeweb.cytoscape.org">
            Powered by <img border="0/" src="model/images/cytoscape_logo_small.png" height="15" width="15"> Cytoscape Web
          </a>
        </div>
      </div>
  </div>
  <script type="text/javascript">
    (function() {
      <%-- sidebar toolbar bar --%>
      var sidebarPages = new Array();
      jQuery('#cwtabsbyside ul').first().find('li a').each(function(index) {
        <%-- push targets --%>
        sidebarPages.push(jQuery(this).attr('href'));
        <%-- attach onclick behavior --%>
        jQuery(this).click(function(e) {
          var that = this;
          jQuery.each(sidebarPages, function(index, target) {
            if (target == jQuery(that).attr('href')) {
              jQuery("#cwtabsbyside "+target).show();
            } else {
              jQuery("#cwtabsbyside "+target).hide();
            }
          });
          jQuery('#cwtabsbyside ul').first().find('li a').each(function(index) {
              jQuery(this).removeClass('active');
          });
          jQuery(that).addClass('active');
          e.preventDefault();
        });
      });
      <%-- show only first tab --%>
      jQuery.each(sidebarPages, function(index, target) {
        if (index > 0) {
          jQuery("#cwtabsbyside "+target).hide();
        }
      });
      <%-- toggle table btn --%>
      jQuery('#cwtabsbyside #tabs-controls #toggleTable').click(function(e) {
        if (jQuery('#cwinlinetable').is(":hidden")) {
          jQuery('#cwinlinetable').show().scrollTo('slow', 'swing', -20);

          if (jQuery('#cytoscape-network-results-table-div').is(':empty') ) {
              var view = new intermine.query.results.CompactView($SERVICE, ${cytoscapeNetworkQueryJson}, LIST_EVENTS, {pageSize: 25});
              view.$el.appendTo('#cytoscape-network-results-table-div');
              view.render();
          }
        } else {
          jQuery('#cwinlinetable').hide();
        }
      });
    })();
  </script>
  <div id="cwcontent"></div>
  </div>
</div>
<br />
<div id="cwinlinetable" class="collection-table nowrap nomargin">
  <h3>Interactions</h3>
  <div id="cytoscape-network-results-table-div" style="overflow-x:auto;">
  </div>
</div>

<!-- Flash embedding utility (needed to embed Cytoscape Web) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/AC_OETags.min.js'/>"></script>

<!-- JSON support for IE (needed to use JS API) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/json2.min.js'/>"></script>

<!-- Cytoscape Web JS API (needed to reference org.cytoscapeweb.Visualization) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/cytoscapeweb.min.js'/>"></script>

<link href="model/jquery_svg/jquery.svg.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="<html:rewrite page='/model/jquery_svg/jquery.svg.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/displaynetwork.js'/>"></script>
<script type="text/javascript">
(function() {

    var target = "#cwcontent";

    if (dataNotIncludedMessage != "") {
        jQuery("#interactions-wrap").html(dataNotIncludedMessage)
                           .css({'font-style': 'italic', 'border': 'none'})
                           .width(600);
    }
    else if (orgWithNoDataMessage != "") {
      jQuery("#interactions-wrap").html(orgWithNoDataMessage)
                               .css({'font-style': 'italic', 'border': 'none'})
                               .width(600);
    }
    else {
        // AJAX POST
        jQuery("#cwcontent").html("Please wait while the network data loads...");
        jQuery.ajax({
            type: "POST",
            url: "cytoscapeNetworkAjax.do",
            dataType: "text",
            data: "fullInteractingGeneSet=" + encodeURIComponent('${fullInteractingGeneSet}'),
            success: function(response) {
                if (response.match("^"+"No interaction data found from data sources:") || response.match("^"+"No interaction data found")) {
                    geneWithNoDatasourceMessage = response; // case: no interaction data found from the data sources
                    jQuery("#interactions-wrap").html(geneWithNoDatasourceMessage)
                                             .css({'font-style': 'italic', 'border': 'none'})
                                             .width(600);
                }
                else if (response.match("^"+"large_network")) {
                    var html = '<form action="cytoscapeNetworkExport.do" id="export-large-network-form" method="post" style="display: none;">' +
                               '<input type="hidden" name="type" value="large_network" />' +
                               '<input type="hidden" name="fullInteractingGeneSet" value="' + fullInteractingGeneSet + '" />' +
                               '</form>' +
                               '<form action="loadQuery.do" id="network-inline-query-form" method="post" style="display: none;">' +
                               '<input type="hidden" name="skipBuilder" value="true" />' +
                               '<input type="hidden" name="query" id="cytoscape-network-query-xml" />' +
                               '<input type="hidden" name="trail" value="|query" />' +
                               '<input type="hidden" name="method" value="xml" />' +
                               '</form>' +
                               '<span>The network contains more than 2000 elements, the interaction can become sluggish in the displayer. Please ' +
                               '<a href="javascript:;"onclick="javascript: jQuery(\'#export-large-network-form\').submit();return false;">Download the network data</a>' +
                               ' and import it to <a href="http://www.cytoscape.org/" target="_blank">Cytoscape desktop</a> and use <b>Force-Directed Layout</b> to view or <a href="javascript:;"onclick="javascript: jQuery(\'#network-inline-query-form\').submit();">view interaction data in a table.</a></span>';

                    jQuery("#interactions-wrap").html(html)
                                  .css({'font-style': 'italic', 'border': 'none'})
                                  .width(550);

                    jQuery("#cytoscape-network-query-xml").val('${cytoscapeNetworkQueryXML}');
                }
                else {
                    networkdata = response;
                    <%-- attach overflow --%>
                    jQuery("#interactions-wrap").css('overflow-x', 'auto');
                    <%-- show network --%>
                    displayNetwork(networkdata, fullInteractingGeneSet, project_title, project_baseurl, project_path);
                    <%-- show toolbox --%>
                    jQuery("#cwtabsbyside").css('display', 'inline');
                }
            },
            error:function (xhr, ajaxOptions, thrownError) {
                jQuery("#interactions-wrap").html("There was a problem retrieving the network data.");
            }
        });
    }

    jQuery('#legendall').svg();
    var legendall = jQuery('#legendall').svg('get');
    if (legendall) {
      legendall.line(0, 10, 45, 10, {stroke: "red", strokeWidth: 4});
      legendall.polygon([[45, 5], [45, 15], [65, 10]], {fill: "red"});
      legendall.text(70, 15, "Physical");
      legendall.line(140, 10, 185, 10, {stroke: "blue", strokeWidth: 4});
      legendall.polygon([[185, 5], [185, 15], [205, 10]], {fill: "blue"});
      legendall.text(210, 15, "Genetic");
    }

    jQuery("svg").height("100%").width("100%");

    // 'export' is reserved! (http://www.quackit.com/javascript/javascript_reserved_words.cfm)
    function exportNet(type) {
        if (type=="tab" || type=="csv") {
            vis.exportNetwork(type, 'cytoscapeNetworkExport.do?type=' + type + '&fullInteractingGeneSet='+fullInteractingGeneSet);
        } else {
            vis.exportNetwork(type, 'cytoscapeNetworkExport.do?type='+type);
        }
    }

    jQuery("#exportoptions").change(function () {
        exportNet(jQuery("#exportoptions option:selected").val());
    });

    jQuery("#exportbutton").click(function () {
        exportNet(jQuery("#exportoptions option:selected").val());
    });
})();
</script>

<!-- /cytoscapeNetworkDisplayer.jsp -->