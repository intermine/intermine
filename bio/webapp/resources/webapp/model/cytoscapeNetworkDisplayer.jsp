<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- cytoscapeNetworkDisplayer.jsp -->

<html:xhtml/>

<style type="text/css">
    /* The Cytoscape Web container must have its dimensions set. */
    html, body { height: 100%; width: 100%; padding: 0; margin: 0; }
    /* use absolute value */
    #cwcontent { width: 600px; height: 485px; border: 2px solid #CCCCCC; padding: 0 1em 1em 0;
        -moz-border-radius: 5px 5px 5px 5px; border-radius: 5px 5px 5px 5px; float:left; }
    #cwtabsbyside { float: right; width: 300px; }
    #cwinlinetable { display: none; padding: 5px 0 0 0;}
    label { vertical-align: middle; }
    #powerby { padding: 5px; text-align: center; }
    #powerby a { color: rgb(136, 136, 136); text-decoration: none; background-color: white; }
    #powerby img { vertical-align: middle; }

    #legendall { width:100%; height:50px; }

    #interactions-wrap { overflow-x:auto; }
    #interactions-wrap div.inside { min-width:1040px; }

    <%-- sidebar toolbar bar --%>
    #cwtabsbyside ul { border-bottom-width:1px; border-bottom-style:solid; font-size:14px; margin:0; }
    #cwtabsbyside ul li { display:inline; border-right-width:1px; border-right-style:solid; }
    #cwtabsbyside ul li a { padding:4px; }
    #cwtabsbyside ul li a.active { font-weight:bold; }
    #cwtabsbyside ul li.last { border:none; }
    #cwtabsbyside fieldset, #cwtabsbyside #legend { border:none; border-bottom-width:1px; border-bottom-style:solid; padding:8px; }
</style>

<script type="text/javascript">

    var project_title = "${WEB_PROPERTIES['project.title']}";
    var project_baseurl = "${WEB_PROPERTIES['webapp.baseurl']}";
    var project_path = "${WEB_PROPERTIES['webapp.path']}";

</script>

<div id="cwhead">
    <h3>Interaction Network</h3>
</div>

<div id="interactions-wrap">
  <div class="inside">
  <div id="cwtabsbyside">
    <ul class="theme-3-border theme-6-background">
      <li class="theme-3-border"><a class="active" href="#tabs-controls">Controls</a></li>
      <%--<li class="theme-3-border"><a href="#tabs-data">Data</a></li>--%>
      <li class="theme-3-border last"><a href="#tabs-help">Help</a></li>
    </ul>
    <div id="tabs-controls">
      <div>
        <fieldset class="theme-3-border">
              <label>Show:</label><br>
              <input type="radio" name="showInteractions" onclick="vis.filter('edges', function(edge) { return edge.color; });" checked><label>All Interactions</label><br>
              <input type="radio" name="showInteractions" onclick="vis.filter('edges', function(edge) { return edge.color >= '#FF0000'; });"><label>Physical Interactions</label><br>
              <input type="radio" name="showInteractions" onclick="vis.filter('edges', function(edge) { return edge.color <= '#FF0000'; });"><label>Genetic Interactions</label>
        </fieldset>

        <fieldset class="theme-3-border theme-6-background">
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

        <fieldset class="theme-3-border">
          <label class="fakelink" onclick="window.open(project_baseurl+ '/' + project_path + '/saveFromIdsToBag.do?type=Gene&ids=' + fullInteractingGeneSet + '&source=objectDetails&newBagName=interacting_gene_list');">Create a gene list...</label>
        </fieldset>

        <fieldset class="theme-3-border theme-6-background">
          <label>View interaction data in a table</lable>
          <input type="button" id="toggleTable" value="Toggle">
        </fieldset>
      </div>
    </div>
    <div id="tabs-data"></div>
    <div id="tabs-help">
      <div id="legend" class="theme-3-border">
        <p>Interaction Type</p>
        <div id="legendall"></div>
      </div>
      <div id="formats">
        <p>export format: </p>
        <ul>
            <li><a target="_blank" href="http://www.cs.rpi.edu/research/groups/pb/punin/public_html/XGMML/index.html">XGMML</a>: the eXtensible Graph Markup and Modeling Language
                is an XML format which is used for graph description. Cytoscape desktop loads and saves networks and node/edge attributes in XGMML as well.
            </li>
            <li><a target="_blank" href="http://cytoscape.wodaklab.org/wiki/Cytoscape_User_Manual/Network_Formats/">SIF</a>: A simpler text format
                 that can be very useful if you do not need to set custom nodes/edges attributes.
            </li>
            <li><a target="_blank" href="http://www.w3.org/Graphics/SVG/">SVG</a>: Scalable Vector Graphics defines the graphics in XML format and does not lose any quality if they are zoomed or resized.
            </li>
            <li><a target="_blank" href="http://www.w3.org/Graphics/SVG/">PNG</a>: Portable Network Graphics, a popular graphics format over the web.
            </li>
            <li>CSV/TSB: comma or tab separated values, suitable for import into Excel.
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
    <%-- sidebar toolbar bar --%>
    var sidebarPages = new Array();
    jQuery('#cwtabsbyside ul').first().find('li a').each(function(index) {
      <%-- push targets --%>
      sidebarPages.push(jQuery(this).attr('href'));
      <%-- attach� onclick behavior --%>
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
      } else {
        jQuery('#cwinlinetable').hide();
      }
    });
  </script>
  <div id="cwcontent"></div>
  </div>
</div>
<br />
<div id="cwinlinetable" class="table">
  <h3>Interactions</h3>
  <div style="overflow-x:auto;">
    <tiles:insert name="resultsTable.tile">
         <tiles:put name="pagedResults" beanName="cytoscapeNetworkPagedResults" />
         <tiles:put name="currentPage" value="objectDetails" />
         <tiles:put name="inlineTable" value="true" />
    </tiles:insert>
  </div>
  <p class="toggle" style="display:none;">
    <a class="collapser" style="float:right; display:none; margin-left:20px;" href="#"><span>Collapse</span></a>
    <a class="toggler" style="float:right;" href="#"><span>Show more rows</span></a>
  </p>
  <p class="in_table">
    <html:link styleClass="theme-1-color" action="/collectionDetails?id=${object.id}&amp;field=interactions&amp;trail=${param.trail}">
      Show all in a table �
    </html:link>
  </p>
</div>
<script type="text/javascript">
  <%-- hide more than 10 rows --%>
  var interactionsTableLength = jQuery("#cwinlinetable table.results tr.bodyRow").length;
  if (interactionsTableLength > 10) {
    jQuery("#cwinlinetable table.results tr.bodyRow").each(function(i) {
      if (i > 9) {
        jQuery(this).hide();
      }
    });
    <%-- 'provide' toggler --%>
    jQuery("#cwinlinetable p.toggle").show();
    <%-- attach toggler event --%>
    jQuery('#cwinlinetable p.toggle a.toggler').click(function(e) {
      jQuery("#cwinlinetable table.results tr.bodyRow:hidden").each(function(i) {
        if (i < 10) {
          jQuery(this).show();
        }
      });
      jQuery("#cwinlinetable p.toggle a.collapser").show();
      if (jQuery("#cwinlinetable table.results tr.bodyRow:hidden").length == 0) {
        jQuery('#cwinlinetable p.toggle a.toggler').hide();
      }

      e.preventDefault();
    });
    <%-- attach collapser event --%>
    jQuery('#cwinlinetable p.toggle a.collapser').click(function(e) {
      var that = this;
      jQuery("#cwinlinetable table.results tr.bodyRow").each(function(i) {
        if (i > 9) {
          jQuery(this).hide();
          jQuery(that).hide();
        }
      });
      jQuery('#cwinlinetable p.toggle a.toggler').show();

      jQuery("#cwinlinetable").scrollTo('fast', 'swing', -20);

      e.preventDefault();
    });
  }
</script>

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
    // from controller
    var fullInteractingGeneSet = '${fullInteractingGeneSet}'; // a string arrray of gene object store ids
    var dataNotIncludedMessage = '${dataNotIncludedMessage}'; // case: interaction data is not integrated
    var orgWithNoDataMessage = '${orgWithNoDataMessage}'; // case: no interaction data for the whole species

    var target = "#cwcontent";

    if (dataNotIncludedMessage != "") {
        jQuery(target).html(dataNotIncludedMessage)
                           .css({'font-style': 'italic', 'border': 'none'})
                           .height(0)
                           .width(600);
    }
    else if (orgWithNoDataMessage != "") {
      jQuery(target).html(orgWithNoDataMessage)
                               .css({'font-style': 'italic', 'border': 'none'})
                               .height(0)
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
                                             .css({'font-style': 'italic', 'border': 'none'})
                                             .height(0)
                                             .width(600);
                } else {
                    networkdata = response;
                    displayNetwork(networkdata, fullInteractingGeneSet, project_title, project_baseurl, project_path);
                    jQuery("#cwtabsbyside").tabs();
                    jQuery("#cwtabsbyside").css('display', 'inline');
                }
            },
            error:function (xhr, ajaxOptions, thrownError) {
                jQuery(target).html("There was a problem retrieving the result.");
            }
        });
    }

    jQuery('#legendall').svg();
    var legendall = jQuery('#legendall').svg('get');
    legendall.line(0, 10, 45, 10, {stroke: "red", strokeWidth: 4});
    legendall.polygon([[45, 5], [45, 15], [65, 10]], {fill: "red"});
    legendall.text(70, 15, "Physical");
    legendall.line(140, 10, 185, 10, {stroke: "blue", strokeWidth: 4});
    legendall.polygon([[185, 5], [185, 15], [205, 10]], {fill: "blue"});
    legendall.text(210, 15, "Genetic");

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
</script>

<!-- /cytoscapeNetworkDisplayer.jsp -->
