<%--
  - Author: Fengyuan Hu
  - Date: 20-12-2010
  - Copyright Notice:
  - @(#)  Copyright (C) 2002-2010 FlyMine

          This code may be freely distributed and modified under the
          terms of the GNU Lesser General Public Licence.  This should
          be distributed with the code.  See the LICENSE file for more
          information or http://www.gnu.org/copyleft/lesser.html.

  - Description: In this page, the worm regulatory network will be
                 displayed in Cytoscape web.
  --%>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!--  wormRegulatoryNetwork.jsp -->


<html:xhtml />

<style type="text/css">
    /* The Cytoscape Web container must have its dimensions set. */
    html, body { height: 100%; width: 100%; padding: 0; margin: 0; }
    /* use absolute value */
    #cytoscapeweb { width: 1500px; height: 800px; }

    /* Optional styles */
    .sidecontentpullout {
            background-color: Black;
            color: White;
            padding: 4px 3px;
            -moz-border-radius-bottomleft: 1em;
            -moz-border-radius-topleft: 1em;
            -webkit-border-bottom-left-radius: 1em;
            -webkit-border-top-left-radius: 1em;
            border-bottom-left-radius: 1em;
            border-top-left-radius: 1em;
    }

    .sidecontentpullout:hover {
            background-color: #444444;
            color: White;
    }

    .sidecontent {
            background-color: Black;
            color: White;
            -moz-border-radius-bottomleft: 1em;
            -webkit-border-bottom-left-radius: 1em;
            border-bottom-left-radius: 1em;
    }

    .sidecontent > div > div {
            padding-left: 10px;
            padding-right: 40px;
    }

    p.title {
        font-family: Cambria, Georgia, Times, “Times New Roman”, serif;
        font-size: 2em;
        font-weight: normal;
        color: white;
        padding: 20px 0 0 5px;
    }

    p.youcan {
        padding:10px 10px 10px 10px;
    }

    li {
        padding: 5px 5px 5px 5px;
    }

    .description {
        height:30%;
        width:50%;
        text-align:justify;
        font-size:85%;
    }

    #export-div { padding: 5px 20px 0 0; }
    .export { margin-right: 5px; }
    .export a { display: inline-block; background-position: top left; vertical-align: middle; background-repeat: no-repeat; }
    .xgmml a { background-image:url('../../../images/formats/xgmml.gif'); width:59px; height:17px; }
    .sif a { background-image:url('../../../images/formats/sif.gif'); width:35px; height:17px; }
    .svg a { background-image:url('../../../images/formats/svg.gif'); width:35px; height:17px; }
    .png a { background-image:url('../../../images/formats/png.gif'); width:52px; height:17px; }

</style>

<div align="center" style="padding-top: 20px;">
    <h3 class="interactions">Integrated C. elegans miRNA-TF Regulatory Network</h3>
    <div id="export-div" align="left"></div>
    <br/>
    <div id="cytoscapeweb" style="text-align: center; width: 100%;">Please wait while the network data loads</div>
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
    <br/>
    <br/>
    <div class="description"></div>
</div>

<div class="sidecontent" title="About the network...">
    <br/>
    <p class="title">How to interact with the network?</p>
    <br/>
    <p class="youcan">You can...</p>

    <ul>
        <li>Zoom in/out by using the controller pan at the bottom right of the network view.</li>
        <li>Drag and move a node around.</li>
        <li>Click or mouseover a node to highlight its first neighbours.</li>
        <li>Double click a node to select and move its first neighbours.</li>
        <li>Right click a node for more options, e.g. view the modMine gene report page, export the network in different formats, etc.</li>
    </ul>
</div>


<!-- Flash embedding utility (needed to embed Cytoscape Web) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/AC_OETags.min.js'/>"></script>
<!-- JSON support for IE (needed to use JS API) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/json2.min.js'/>"></script>
<!-- Cytoscape Web JS API (needed to reference org.cytoscapeweb.Visualization) -->
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/cytoscapeweb.min.js'/>"></script>
<!-- qTip -->
<script type="text/javascript" src="<html:rewrite page='/model/jquery_qtip/jquery.qtip-1.0.js'/>"></script>
<!-- SVG -->
<link href="model/jquery_svg/jquery.svg.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="<html:rewrite page='/model/jquery_svg/jquery.svg.js'/>"></script>
<!-- Sidecontent -->
<script type="text/javascript" src="<html:rewrite page='/model/jquery_sidecontent/jquery.sidecontent.js'/>"></script>
<!-- Corner -->
<script type="text/javascript" src="<html:rewrite page='/model/jquery_corner/jquery.corner.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/model/cytoscape/worm_regulatory_network.js'/>"></script>
<script type="text/javascript">

    var classMissingMessage = '${classMissingMessage}';
    var wormRegulatoryNetwork = '${wormRegulatoryNetwork}';

    var webapp_baseurl = "${WEB_PROPERTIES['webapp.baseurl']}";
    var webapp_path = "${WEB_PROPERTIES['webapp.path']}";
    var project_title = "${WEB_PROPERTIES['project.title']}";

    if (classMissingMessage != "") {
        jQuery('#cytoscapeweb').html(classMissingMessage)
                               .css('font-style','italic');
    }
    else if (wormRegulatoryNetwork == "") {
        jQuery('#cytoscapeweb').html("Fly regulartory network data is not availiable...")
                               .css('font-style','italic');
    }
    else {
        showNetwork(wormRegulatoryNetwork, webapp_baseurl, webapp_path, project_title);

        // sidecontent settings
        jQuery('.sidecontent').sidecontent({
            classmodifier: "sidecontent",
            attachto: "rightside",
            width: "600px",
            opacity: "0.8",
            pulloutpadding: "170",
            textdirection: "horizontal",
            clickawayclose: true
         });
    }

    jQuery(document).ready(function() {
        jQuery("#export-div").html("<span class=\"export\">Select a file type to export the network</span>" +
                                   "<span class=\"export xgmml\"><a href=\"javascript:;\" onclick=\"exportNet('xgmml')\">XGMML</a></span>" +
                                   "<span class=\"export sif\"><a href=\"javascript:;\" onclick=\"exportNet('sif')\">SIF</a></span>" +
                                   "<span class=\"export svg\"><a href=\"javascript:;\" onclick=\"exportNet('svg')\">SVG</a></span>" +
                                   "<span class=\"export png\"><a href=\"javascript:;\" onclick=\"exportNet('png')\">PNG</a></span>"
                                   );
      });

    function exportNet(type) {
        if (type == "sif") {
            vis.exportNetwork(type, 'cytoscapeNetworkExport.do?type='+type, { interactionAttr: "label", nodeAttr: "label" });
        } else {
            vis.exportNetwork(type, 'cytoscapeNetworkExport.do?type='+type);
        }
    }

</script>

<!--  /wormRegulatoryNetwork.jsp -->
