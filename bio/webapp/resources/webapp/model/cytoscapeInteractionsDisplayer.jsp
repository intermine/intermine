<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- cytoscapeInteractionsDisplayer.jsp -->

<html:xhtml/>

<style type="text/css">
    /* The Cytoscape Web container must have its dimensions set. */
    html, body { height: 100%; width: 100%; padding: 0; margin: 0; }
    /* use absolute value */
    #cytoWebContent { width: 315px; height: 270px; }

    SPAN.physical
    {
       BACKGROUND-COLOR: #FF0000;
       COLOR: #FF0000;
       FONT-SIZE: 2px;
       padding-top:0px;
       padding-bottom:0px;
       padding-right:18px;
       padding-left:18px;
       position:relative;
       top:-4px;
    }
    .genetic
    {
       BACKGROUND-COLOR: #6666FF;
       COLOR: #6666FF;
       FONT-SIZE: 2px;
       padding-top:0px;
       padding-bottom:0px;
       padding-right:18px;
       padding-left:18px;
       position:relative;
       top:-4px;
    }
</style>
<div id="cyto_div">
    <h3 class="interactions">Interaction Network</h3>
    <div id="caption" style="font-size:12px; font-style:italic">
    <!-- jQuery will add stuff here -->
    </div>
    <div id="cytoWebContent" width="*">Please wait while the network data loads</div>
    <div id="menu">
    <!-- jQuery will add stuff here -->
    </div>
    <div id="legends">
    <!-- jQuery will add stuff here -->
    </div>
    <p>
      <a style="color: rgb(136, 136, 136); text-decoration: none; background-color: white;" onmouseout="this.style.backgroundColor='white';" onmouseover="this.style.backgroundColor='#f1f1d1';" title="Cytoscape Web" target="_blank" href="http://cytoscapeweb.cytoscape.org">
        Powered by <img border="0/" style="vertical-align: middle;" src="model/images/cytoscape_logo_small.png" height="15" width="15"> Cytoscape Web
      </a>
    </p>
</div>

<!-- Flash embedding utility (needed to embed Cytoscape Web) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/AC_OETags.min.js'/>"></script>

<!-- JSON support for IE (needed to use JS API) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/json2.min.js'/>"></script>

<!-- Cytoscape Web JS API (needed to reference org.cytoscapeweb.Visualization) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/cytoscapeweb.min.js'/>"></script>

<!-- qTip -->
<script type="text/javascript" src="<html:rewrite page='/model/jquery_qtip/jquery.qtip-1.0.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/model/jquery-ui/jquery-ui-1.8.7.custom.min.js'/>"></script>
<script type="text/javascript">
    // TODO separate showNetwork() method from interactions.js
</script>
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/interactions.js'/>"></script>
<script type="text/javascript">

    var networkdata = '${networkdata}';
    var hubgene = '${hubGene}'; // could be a set of genes
    var geneOSIds = '${geneOSIds}'.split(","); // a string arrray of gene object store ids

    var dataNotIncludedMessage = '${dataNotIncludedMessage}'; // case: interaction data is not integrated
    var orgWithNoDataMessage = '${orgWithNoDataMessage}'; // case: no interaction data for the whole species
    var geneWithNoDatasourceMessage = '${geneWithNoDatasourceMessage}'; // case: no interaction data for the gene from the data sources


    var webapp_baseurl = "${WEB_PROPERTIES['webapp.baseurl']}";
    var webapp_path = "${WEB_PROPERTIES['webapp.path']}";
    var project_title = "${WEB_PROPERTIES['project.title']}";

    if (dataNotIncludedMessage != "") {
        jQuery('#cyto_div').html(dataNotIncludedMessage)
                           .css('font-style','italic');
    }
    else if (orgWithNoDataMessage != "") {
      jQuery('#cytoWebContent').html(orgWithNoDataMessage)
                               .css('font-style','italic')
                               .height(20)
                               .width(600);
    }
    else if (geneWithNoDatasourceMessage != "") {
        jQuery('#cytoWebContent').html(geneWithNoDatasourceMessage)
                                 .css('font-style','italic')
                                 .height(20)
                                 .width(1200);
    }
    else if (hubgene == "") {
      jQuery('#cytoWebContent').html("internal error")
                             .css('font-weight','bold')
                             .css('color','red')
                             .height(50)
                             .width(150);
    }
    else {
        showNetwork(networkdata, hubgene, geneOSIds, webapp_baseurl, webapp_path, project_title);
    }
</script>

<!-- /cytoscapeInteractionsDisplayer.jsp -->
