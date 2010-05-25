<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- cytoscapeInteractionsDisplayer.jsp -->

<html:xhtml/>
        <script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/AC_OETags.min.js'/>"></script>
        <script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/json2.min.js'/>"></script>
        <script type="text/javascript" src="<html:rewrite page='/model/cytoscape/js/cytoscapeweb.min.js'/>"></script>
        <script type="text/javascript">

        window.onload=function() {
            // id of Cytoscape Web container div
            var div_id = "cytoscapeweb";

            // network data could alternatively be grabbed via ajax (jQuery)
            // TODO : Performance
            <c:set var="data" value="${networkdata}"/>
            var data = "${data}";
            // alert(data);

            // initialization options
            var options = {
                // where you have the Cytoscape Web SWF
                swfPath: "model/cytoscape/swf/CytoscapeWeb",
                // where you have the Flash installer SWF
                flashInstallerPath: "model/cytoscape/swf/playerProductInstall"
            };

            // init and draw
            var vis = new org.cytoscapeweb.Visualization(div_id, options);
            vis.draw({ network: data });
        };

        </script>

        <style>
            /* The Cytoscape Web container must have its dimensions set. */
            html, body { padding: 0; margin: 0; text-align: center; font-family: sans-serif; }
            h1, h2 { margin-top: 8px; }
            #header { width: 100%; height: 40px; padding: 4px 0; color: #0b94b1; }
            #cytoscapeweb { height: 600px; width: 800px; margin: 0 auto; border: 2px solid #0b94b1; }
        </style>



        <div id="header">
            <h2>Cytoscape Web</h2>
        </div>
        <div id="cytoscapeweb" width="*">
            Please wait while the network data loads
        </div>
        <p>
            <a href="http://www.cytoscape.org" rel="external" target="_blank">Cytoscape</a> |
            <a href="http://tdccbr.med.utoronto.ca" rel="external" target="_blank">Donnelly CCBR</a>

            , &copy; 2009
        </p>

    <!-- /cytoscapeInteractionsDisplayer.jsp -->
