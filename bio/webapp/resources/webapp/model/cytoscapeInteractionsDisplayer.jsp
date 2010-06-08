<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- cytoscapeInteractionsDisplayer.jsp -->

<html:xhtml/>

        <style type="text/css">
            /* The Cytoscape Web container must have its dimensions set. */
            html, body { height: 100%; width: 100%; padding: 0; margin: 0; }
            /* use absolute value */
            #cytoscapeweb { width: 315px; height: 270px; }

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
               BACKGROUND-COLOR: #0000FF;
               COLOR: #0000FF;
               FONT-SIZE: 2px;
               padding-top:0px;
               padding-bottom:0px;
               padding-right:18px;
               padding-left:18px;
               position:relative;
               top:-4px;
            }
        </style>

        <!-- Flash embedding utility (needed to embed Cytoscape Web) -->
        <script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/AC_OETags.min.js'/>"></script>

        <!-- JSON support for IE (needed to use JS API) -->
        <script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/json2.min.js'/>"></script>

        <!-- Cytoscape Web JS API (needed to reference org.cytoscapeweb.Visualization) -->
        <script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/cytoscapeweb.min.js'/>"></script>

        <script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/interactions.js'/>"></script>

        <script type="text/javascript">

        // network data could alternatively be fetched via ajax (jQuery)
        <c:set var="data" value="${networkdata}"/>
        var data = "${data}";

        if (data == "") {
            jQuery(document).ready(function() {
              $("#cytoscapeweb").html("no interactions found")
                                .css('font-style','italic')
                                .height(50)
                                .width(150);
            });
        }
        else {
            var webapp_baseurl = "${WEB_PROPERTIES['webapp.baseurl']}";
            var webapp_path = "${WEB_PROPERTIES['webapp.path']}";

            jQuery(document).ready(function() {
                showInteractions(data, webapp_baseurl, webapp_path);
            });
        }
        </script>

        <div style="font-size:16px; font-weight:bold">Interaction Network</div>
        <div id="caption" style="font-size:12px; font-style:italic">
        <!-- jQuery will add stuff here -->
        </div>
        <div id="cytoscapeweb" width="*">Please wait while the network data loads</div>
        <div id="menu">
        <!-- jQuery will add stuff here -->
        </div>
        <div id="legends">
        <!-- jQuery will add stuff here -->
        </div>
        <p>
            <a href="http://cytoscapeweb.cytoscape.org" rel="external" target="_blank">Cytoscape Web</a> |
            <a href="http://tdccbr.med.utoronto.ca" rel="external" target="_blank">Donnelly CCBR</a>
            , &copy; 2009-2010 Cytoscape Consortium
        </p>

    <!-- /cytoscapeInteractionsDisplayer.jsp -->
