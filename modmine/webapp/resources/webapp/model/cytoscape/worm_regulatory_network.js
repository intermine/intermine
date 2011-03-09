// Functions for network display

function showInteractions(networkdata, webapp_baseurl, webapp_path, project_title, extNetworkDataArray) {

    jQuery('#menu').html("&nbsp;");

    // id of Cytoscape Web container div
    var div_id = "cytoscapeweb";

    // initialization options
    var options = {
        // where you have the Cytoscape Web SWF
        swfPath: "model/cytoscape/swf/CytoscapeWeb",
        // where you have the Flash installer SWF
        flashInstallerPath: "model/cytoscape/swf/playerProductInstall"
    };

    // init
    vis = new org.cytoscapeweb.Visualization(div_id, options); //global, once this function is called

    // after init
    vis.ready(function() {

    // add new fields to edges
    //var field_shortname = { name: "name", type: "string", defValue: "" };
    var field_datasource = { name: "data_source", type: "string", defValue: "" };
    //var field_publicationtitle = { name: "publication_title", type: "string", defValue: "" };
    //vis.addDataField("edges", field_shortname);
    vis.addDataField("edges", field_datasource);
    //vis.addDataField("edges", field_publicationtitle);

    // Customize edge tooltips
    // 1. First, create a function and add it to the Visualization object.
    vis["customTooltip"] = function (data) {
        return 'Data Source:\n<font color="#660099" face="Verdana" size="12"><b>' + data["data_source"] + '</b></font>';
    };

    // 2. Now create a new visual style (or get the current one) and register
    //    the custom mapper to one or more visual properties:
    var style = vis.visualStyle();
    style.edges.tooltipText = { customMapper: { functionName: "customTooltip" } },

    // 3. Finally set the visual style again:
    vis.visualStyle(style);

    var caption = "[Click the network to zoom]";
    jQuery('#caption').html(caption);

    var menu = '<span id="physical" class="fakelink">Show Physical Interactions</span>&nbsp;|&nbsp;<span id="genetic" class="fakelink">Show Genetic Interactions</span>&nbsp;|&nbsp;<span id="all" class="fakelink">Show All Interactions</span>';
    jQuery('#menu').html(menu);

    var legends = '<span>Interaction Type:</span>&nbsp;&nbsp;&nbsp;&nbsp;<span class="physical">legends</span>&nbsp;&nbsp;<span>Physical</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span class="genetic">legends</span>&nbsp;&nbsp;<span>Genetic</span>';
    jQuery('#legends').html(legends);

    // Show or Hide edge labels
    //$("#edgelabels").click(function(){
        //if (vis.edgeLabelsVisible() == true) {
             //vis.edgeLabelsVisible(false);
             //$("#edgelabels").text('Show Labels');
        //}
        //else
            //{ vis.edgeLabelsVisible(true);
              //$("#edgelabels").text('Hide Labels');}
     //});

     // Filter to show physical interactions
    jQuery('#physical').click(function(){
         vis.filter("edges", function(edge) {
             return edge.color >= "#FF0000";
         });
     });

    // Filter to show genetic interactions
    jQuery('#genetic').click(function(){
        vis.filter("edges", function(edge) {
            return edge.color <= "#FF0000";
        });
    });

    // Show all interactions
    jQuery('#all').click(function(){
        vis.filter("edges", function(edge) {
            return edge.color;
        });
    });

    vis.addContextMenuItem("View "+project_title+" report...", "nodes", function(evt) {
        var data = evt.target.data;
        var url = data.url;
        //if (url == null) { url = "${WEB_PROPERTIES['webapp.baseurl']}"+"/"+"${WEB_PROPERTIES['webapp.path']}"+"/portal.do?externalid="+data.id+"&class=Gene"; }
        if (url == null) { url = webapp_baseurl+"/"+webapp_path+"/portal.do?externalid="+data.id+"&class=Gene"; }
        window.open(url);
    })

    // for future reference: find any node's neighbours
    // vis.addContextMenuItem("Find neighbours...", "nodes", function(evt) {
        // var id = evt.target.data.id; // symbol
        // $.get(...) // ajax to fetch the data
        // vis.draw(...) // redraw
    // })

    vis.addContextMenuItem("Export network as SIF...", "none", function(evt) {
      vis.exportNetwork('sif', 'cytoscapeNetworkExport.do?type=sif');
    })

    .addContextMenuItem("Export network as XGMML...", "none", function(evt) {
      vis.exportNetwork('xgmml', 'cytoscapeNetworkExport.do?type=xgmml');
    })

    .addContextMenuItem("View interaction report...", "edges", function(evt) {

        // shortname_array = shortnames.split(/\r\n|\r|\n/);
        // a_shortname = shortname_array[0];

        var data = evt.target.data;
        var url = data.url;
        if (url == null) { url = webapp_baseurl+"/"+webapp_path+"/portal.do?externalid="+shortname+"&class=Interaction";}
        window.open(url);
    })

    .addListener("mouseover", "edges", function(evt) {

        var a_edge = evt.target;
        var edge_id = [a_edge.data.id];

        // update visual style of egdes
        var bypass = { edges: {} };
        bypass.edges[edge_id] = { width: 3 };
        vis.visualStyleBypass(bypass);

        // get interaction info via ajax
        // var datasources = queryInteractionDatasource(a_edge);
        // shortnames = queryInteractionShortname(a_edge);

        // splitRecords = interactionRecords.split(/\r\n|\r|\n/);
        // splitRecords.pop();

        // extNetworkDataArray
        var datasources = "";

        var sources = a_edge.data.dataSources;
        for (var i=0; i < sources.length; i++) {
            datasources = datasources + sources[i].dataSource + ",";
        }

        datasources = datasources.substring(0, datasources.lastIndexOf(","));

        //var interaction = a_edge.data.interaction;
        //var target = a_edge.data.target;
        //var interactionString = source + "-" + interaction + "-" + target;
        //var interactionRevString = target + "-" + interaction + "-" + source;

        //for (i=0; i<extNetworkDataArray.length; i++) {
          // var extRec = extNetworkDataArray[i].split(";");
           //if(interactionString == extRec[0] || interactionRevString == extRec[0]) {
             // datasources = extRec[1].split(",");
             // shortname =  extRec[2];
           //}
        //}

        var data_to_update = { data_source: datasources };
        vis.updateData("edges", edge_id, data_to_update);
    })

    .addListener("mouseout", "edges", function(evt) {
        setTimeout(function() {
            vis.visualStyleBypass({});
          }, 400);
    })

    .addListener("mouseover", "nodes", function(evt) {
        _mouseOverNode = evt.target; //global, once this function is called
        highlighFirstNeighbors(evt.target);
    })

    .addListener("mouseout", "nodes", function(evt) {
        _mouseOverNode = null;
        clearFirstNeighborsHighligh();
    })

    .addListener("dblclick", "nodes", function(evt) {
         selectFirstNeighbors(evt.target);
     })

     .addListener("click", "edges", function(evt) {

        // get the offset of cytoscape div
        var offset = jQuery("#cytoscapeweb").offset();
        var X = offset.left;
        var Y = offset.top;

        // get two nodes
        var source_node_id = evt.target.data.source;
        var target_node_id = evt.target.data.target;

        var source_node = vis.node(source_node_id);
        var target_node = vis.node(target_node_id);

        var edge_mid_x = (source_node.x + target_node.x)/2;
        var edge_mid_y = (source_node.y + target_node.y)/2;

        // working not well for curve lines
        var edge_abs_x = X + edge_mid_x;
        var edge_abs_y = Y + edge_mid_y;

        // showing where it clicks to
        var edge_click_x = evt.mouseX + X;
        var edge_click_y = evt.mouseY + Y;

        jQuery("body").qtip({
            content: evt.target.data.dataSources[0].dataSource,

            position: {
                target: false,
                type: "absolute",
                corner: {
                    tooltip: "rightTop",
                    target: "leftTop"
                },
                adjust: {
                    mouse: false,
                    x: edge_click_x,
                    y: edge_click_y,
                    scroll: false,
                    resize: false
                }
            },

            show: {
                delay: 0,
                when: false,
                effect: { type: "fade", length: 0 },
                ready: true // Show the tooltip when ready
            },

            hide: {

                delay: 0,
                effect: { type: "fade", length: 0 },
                when: { event: "unfocus" }, // Hide when clicking anywhere else
                fixed: true // Make it fixed so it can be hovered over
            },

            style: {
               border: { width: 1, radius: 8 },
               screen: true,
               textAlign: 'left',
               name: 'light', // Style it according to the preset 'cream' style,
               tip: true      // Give it a speech bubble tip with automatic corner detection
            }
        });

     })

    .addListener("click", "none", function(evt) {
        jQuery('#cytoscapeweb').height(600)
                               .width(1000);
        // Resize effect from jQuery UI, but not working properly on flash
        // $(this).effect("size", { to: {width: 1000,height: 600} }, 1000);
        // Add zoom control panel
        vis.panZoomControlVisible(true);
        // Change the scale of the network until it fits the screen.
        vis.zoom(1);
        vis.nodeTooltipsEnabled(false);
        vis.edgeTooltipsEnabled(true);
        // Change the caption
        jQuery('#caption').html("[Right click a node or edge for more options]");

        // mimic of jQuery.one('click', func)
        vis.removeListener("click", "none");
    });
  });

    // draw options
    var draw_options = {
        // your data goes here
        network: networkdata,

        // show edge labels too
        edgeLabelsVisible: false,
        nodeLabelsVisible: true,

        // hide pan zoom
        panZoomControlVisible: false,

        visualStyle: {
            global: {
               backgroundColor: "#FFFFFF",
               tooltipDelay: 1
            },

            nodes: {
                borderWidth: 1,
                label: { passthroughMapper: { attrName: "label" } },
                labelHorizontalAnchor: "center",
                labelVerticalAnchor: "middle",
                labelFontWeight: "bold",
                labelGlowOpacity: 0.4,
                labelGlowColor: "#ffffff",
                selectionBorderWidth: 2,
                selectionBorderColor: "#000000",
                selectionGlowColor: "#ffff00"
            },

            edges: {
                opacity: { defaultValue: 1,
                discreteMapper: { attrName: "interaction",
                                  entries: [
                                             { attrValue: "physical", value: 0.8 },
                                             { attrValue: "genetic", value: 0.8 }
                                           ]
                                }
                          },
              selectionGlowOpacity: 0
                    }
             }

        };

    // draw
    vis.draw(draw_options);
}

    function highlighFirstNeighbors(target) {
        setTimeout(function() {
            if (_mouseOverNode != null && _mouseOverNode.data.id === target.data.id) {
                var fn = vis.firstNeighbors([target]);
                var bypass = { nodes: {}, edges: {} };

                var allNodes = vis.nodes();
                jQuery.each(allNodes, function(i, n) {
                      bypass.nodes[n.data.id] = { opacity: 0.2 };
                });
                var neighbors = fn.neighbors;
                neighbors = neighbors.concat(fn.rootNodes);
                jQuery.each(neighbors, function(i, n) {
                      bypass.nodes[n.data.id] = { opacity: 1 };
                });

                var allEdges = vis.edges();
                jQuery.each(allEdges, function(i, e) {
                      bypass.edges[e.data.id] = { opacity: 0.1 };
                });
                var edges = fn.edges;
                jQuery.each(edges, function(i, e) {
                      bypass.edges[e.data.id] = { opacity: 1 };
                });

                vis.visualStyleBypass(bypass);
            }
      }, 400);
   }

    function clearFirstNeighborsHighligh() {
        setTimeout(function() {
            if (_mouseOverNode == null) {
                vis.visualStyleBypass({});
            }
         }, 400);
    }

    function selectFirstNeighbors(node) {
        var fn = vis.firstNeighbors([node]);
        var nodes = fn.neighbors.concat(fn.rootNodes);
        vis.deselect();
        vis.select(nodes);
    }

    function queryInteractionDatasource(edge) {

        //var ajax_url =  webapp_baseurl+"/"+webapp_path+"/service/query/results?query=%3Cquery+name%3D%22%22+model%3D%22genomic%22+view%3D%22Gene.interactions.shortName+Gene.interactions.dataSets.dataSource.name+Gene.interactions.experiment%3Apublication.title+Gene.interactions.experiment%3Apublication.pubMedId+Gene.interactions.interactionType%22+constraintLogic%3D%22A+and+B+and+C%22%3E%3Cnode+path%3D%22Gene%22+type%3D%22Gene%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions%22+type%3D%22Interaction%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions.interactingGenes%22+type%3D%22Gene%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions.interactingGenes.symbol%22+type%3D%22String%22%3E%3Cconstraint+op%3D%22%3D%22+value%3D%22"+edge.data.target+"%22+description%3D%22%22+identifier%3D%22%22+code%3D%22B%22%3E%3C%2Fconstraint%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions.interactionType%22+type%3D%22String%22%3E%3Cconstraint+op%3D%22%3D%22+value%3D%22"+edge.data.interaction+"%22+description%3D%22%22+identifier%3D%22%22+code%3D%22C%22%3E%3C%2Fconstraint%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions.experiment%22+type%3D%22InteractionExperiment%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions.experiment%3Apublication%22+type%3D%22Publication%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.symbol%22+type%3D%22String%22%3E%3Cconstraint+op%3D%22%3D%22+value%3D%22"+edge.data.source+"%22+description%3D%22%22+identifier%3D%22%22+code%3D%22A%22%3E%3C%2Fconstraint%3E%3C%2Fnode%3E%3C%2Fquery%3E&format=tab";
        var ajax_url = webapp_baseurl+"/"+webapp_path+"/service/query/results?query=%3Cquery+name%3D%22%22+model%3D%22genomic%22+view%3D%22Gene.interactions.dataSets.dataSource.name%22+sortOrder%3D%22Gene.interactions.dataSets.dataSource.name+asc%22+constraintLogic%3D%22A+and+B+and+C%22%3E%3Cnode+path%3D%22Gene%22+type%3D%22Gene%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.symbol%22+type%3D%22String%22%3E%3Cconstraint+op%3D%22%3D%22+value%3D%22"+edge.data.target+"%22+description%3D%22%22+identifier%3D%22%22+code%3D%22A%22%3E%3C%2Fconstraint%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions%22+type%3D%22Interaction%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions.interactingGenes%22+type%3D%22Gene%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions.interactingGenes.symbol%22+type%3D%22String%22%3E%3Cconstraint+op%3D%22%3D%22+value%3D%22"+edge.data.source+"%22+description%3D%22%22+identifier%3D%22%22+code%3D%22B%22%3E%3C%2Fconstraint%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions.interactionType%22+type%3D%22String%22%3E%3Cconstraint+op%3D%22%3D%22+value%3D%22"+edge.data.interaction+"%22+description%3D%22%22+identifier%3D%22%22+code%3D%22C%22%3E%3C%2Fconstraint%3E%3C%2Fnode%3E%3C%2Fquery%3E&format=tab";

        return jQuery.ajax({
            url: ajax_url,
            type: "GET",
            async:false,
            success: function(intact_info){
               // alert(edge.data.target+"\n"+edge.data.interaction+"\n"+edge.data.source+"\n"+intact_info);
            }
         }
      ).responseText;
    }

    function queryInteractionShortname(edge) {

        var ajax_url = webapp_baseurl+"/"+webapp_path+"/service/query/results?query=%3Cquery+name%3D%22%22+model%3D%22genomic%22+view%3D%22Gene.interactions.shortName%22+sortOrder%3D%22Gene.interactions.shortName+asc%22+constraintLogic%3D%22A+and+C+and+B%22%3E%3Cnode+path%3D%22Gene%22+type%3D%22Gene%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.symbol%22+type%3D%22String%22%3E%3Cconstraint+op%3D%22%3D%22+value%3D%22"+edge.data.target+"%22+description%3D%22%22+identifier%3D%22%22+code%3D%22A%22%3E%3C%2Fconstraint%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions%22+type%3D%22Interaction%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions.interactionType%22+type%3D%22String%22%3E%3Cconstraint+op%3D%22%3D%22+value%3D%22"+edge.data.interaction+"%22+description%3D%22%22+identifier%3D%22%22+code%3D%22C%22%3E%3C%2Fconstraint%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions.interactingGenes%22+type%3D%22Gene%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Gene.interactions.interactingGenes.symbol%22+type%3D%22String%22%3E%3Cconstraint+op%3D%22%3D%22+value%3D%22"+edge.data.source+"%22+description%3D%22%22+identifier%3D%22%22+code%3D%22B%22%3E%3C%2Fconstraint%3E%3C%2Fnode%3E%3C%2Fquery%3E&format=tab";

        return jQuery.ajax({
            url: ajax_url,
            type: "GET",
            async:false,
            success: function(intact_info){
               // alert(edge.data.target+"\n"+edge.data.interaction+"\n"+edge.data.source+"\n"+intact_info);
            }
         }
      ).responseText;
    }