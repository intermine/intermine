<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- cytoscapeInteractionsDisplayer.jsp -->

<html:xhtml/>
        
        <script type="text/javascript" src="<html:rewrite page='/js/cytoscape/AC_OETags.min.js'/>"></script>
        <script type="text/javascript" src="<html:rewrite page='/js/cytoscape/json2.min.js'/>"></script>
        <script type="text/javascript" src="<html:rewrite page='/js/cytoscape/cytoscapeweb.min.js'/>"></script>
        <script type="text/javascript">

            var div_id = "cytoscapeweb";

            var vis;
            
            // Draw options:
            var options = {
                nodeTooltipsEnabled: true,
                edgeTooltipsEnabled: true,
                nodeLabelsVisible: true,
                edgeLabelsVisible: false,
                edgesMerged: false,
                visualStyle: {
                    global: {
                        backgroundColor: "#fefefe",
                        tooltipDelay: 1000
                    },
                    nodes: {
                        shape: "ELLIPSE",
                        color: "#cccccc",
                        opacity: 0.9,
                        size: { defaultValue: 30, continuousMapper: { attrName: "weight",  minValue: 30, maxValue: 60 } },
                        borderWidth: 1,
                        borderColor: "#707070"
                    },
                    edges: {
                        color: "#0b94b1",
                        width: 2,
                        mergeWidth: 2,
                        opacity: 0.8,
                        labelFontSize: 10,
                        labelFontWeight: "bold"
                     }
                }   
            };
        
         jQuery(document).ready(function(){
         
                // init and draw
                vis = new org.cytoscapeweb.Visualization(div_id);

                vis.ready(function() {
                    // interact with cytoscape web here to avoid errors...
				 });
                
                	// Get SIF data with AJAX request:
					//var data = "tba-2 Co-localization ebp-2\n";
					var url = "model/data/interactions.sif";
                	
    	            jQuery.get(url, function(data) {
        	        	options.network = data;
            	        vis.draw(options);
                	});               
            });
            
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
            Cytoscape Web will replace the contents of this div with your network.
        </div>
        <p>
            <a href="http://www.cytoscape.org" rel="external" target="_blank">Cytoscape</a> |
            <a href="http://tdccbr.med.utoronto.ca" rel="external" target="_blank">Donnelly CCBR</a>

            , &copy; 2009
        </p>

    <!-- /cytoscapeInteractionsDisplayer.jsp -->
