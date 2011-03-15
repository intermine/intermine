<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- cytoscapeNetworkDisplayer.jsp -->

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
    <div id="cytoWebContent" width="*"></div>
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
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/displaynetwork.js'/>"></script>
<script type="text/javascript">

    // from controller
    var fullInteractingGeneSet = '${fullInteractingGeneSet}'; // a string arrray of gene object store ids
    var dataNotIncludedMessage = '${dataNotIncludedMessage}'; // case: interaction data is not integrated
    var orgWithNoDataMessage = '${orgWithNoDataMessage}'; // case: no interaction data for the whole species

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
    else {

       jQuery("#cytoWebContent")
          .ajaxStart(function(){ jQuery(this).html("Please wait..."); })
          .ajaxStop(function(){})
          .ajaxError(function(){ jQuery(this).html("ajax error!"); return;});

        // use ajax to get network
        jQuery.post("cytoscapeNetworkAjax.do", { fullInteractingGeneSet: '${fullInteractingGeneSet}'}, function(data){
            if (data.match("^"+"No interaction data found from data sources:")) {
                geneWithNoDatasourceMessage = data; // case: no interaction data found from the data sources
                jQuery('#cytoWebContent').html(geneWithNoDatasourceMessage)
                                         .css('font-style','italic')
                                         .height(20)
                                         .width(1200);
            } else {
                networkdata = data;
                if (networkdata.trim() == "") {
                    jQuery("#cytoWebContent").html("Interactions not available")
                                       .css('font-style','italic');
                } else {
                    displayNetwork(networkdata, fullInteractingGeneSet);
                }
            }
        });
    }

</script>

<!-- /cytoscapeNetworkDisplayer.jsp -->
