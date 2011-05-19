// Functions for network display

function displayNetwork(networkdata, fullInteractingGeneSet, projectTitle, projectBaseUrl, projectPath) {

    jQuery('#menu').html("&nbsp;");

    // initialization options
    var options = { swfPath: "model/cytoscape/swf/CytoscapeWeb",    // where you have the Cytoscape Web SWF
                    flashInstallerPath: "model/cytoscape/swf/playerProductInstall"  // where you have the Flash installer SWF
    };

    // init
    vis = new org.cytoscapeweb.Visualization("cwcontent", options);   //global, once this function is called

    // after init
    vis.ready(function() {

    // add new fields to edges
    var field_datasource = { name: "data_source", type: "string", defValue: "" };
    vis.addDataField("edges", field_datasource);

    // Customize edge tooltips
    // === 1. First, create a function and add it to the Visualization object.
    vis["customTooltip"] = function (data) {
        return 'Data Source:\n<font color="#660099" face="Verdana" size="12"><b>' + data["data_source"] + '</b></font>';
    };

    // === 2. Now create a new visual style (or get the current one) and register the custom mapper to one or more visual properties:
    var style = vis.visualStyle();
    style.edges.tooltipText = { customMapper: { functionName: "customTooltip" } },

    // === 3. Finally set the visual style again:
    vis.visualStyle(style);

    var menu = '<span id="physical" class="fakelink">Show Physical Interactions</span>&nbsp;|&nbsp;<span id="genetic" class="fakelink">Show Genetic Interactions</span>&nbsp;|&nbsp;<span id="all" class="fakelink">Show All Interactions</span>';
    jQuery('#menu').html(menu);

    var legends = '<span>Interaction Type:</span>&nbsp;&nbsp;&nbsp;&nbsp;<span class="physical">legends</span>&nbsp;&nbsp;<span>Physical</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span class="genetic">legends</span>&nbsp;&nbsp;<span>Genetic</span>';
    jQuery('#legends').html(legends);

    vis.panZoomControlVisible(true);
    vis.zoomToFit();

    vis.addContextMenuItem("View " + projectTitle + " gene report...", "nodes", function(evt) {
        var data = evt.target.data;
        url = projectBaseUrl + "/" + projectPath + "/report.do?id=" + data.id;
        window.open(url);
    })

    /* for future reference: find any node's neighbours
    vis.addContextMenuItem("Find neighbours...", "nodes", function(evt) {
        var id = evt.target.data.id; // symbol
        $.get(...) // ajax to fetch the data
        vis.draw(...) // redraw
    })
    */

     /* will be enabled in the next release
    .addContextMenuItem("View interaction report...", "edges", function(evt) {
        var data = evt.target.data;
        var url = data.url;
        if (url == null) { url = webapp_baseurl + "/" + webapp_path + "/portal.do?externalid="+shortname+"&class=Interaction";}
        window.open(url);
    })
    */

    .addListener("mouseover", "edges", function(evt) {

        var a_edge = evt.target;
        var edge_id = [a_edge.data.id];

        // update visual style of egdes
        var bypass = { edges: {} };
        bypass.edges[edge_id] = { width: 3 };
        vis.visualStyleBypass(bypass);

        var datasources = "";

        var sources = a_edge.data.dataSources;
        for (var i=0; i < sources.length; i++) {
            datasources = datasources + sources[i].dataSource + ",";
        }

        datasources = datasources.substring(0, datasources.lastIndexOf(","));

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
     });

    /* qTip will be enabled in the next release
    .addListener("click", "edges", function(evt) {

        // get the offset of cytoscape div
        var offset = jQuery("#cwcontent").offset();
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

            api: {
                onHide: function(){
                    alert('hiding');
                    jQuery('.qtip').remove(); }
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
    */

    /*
    .addListener("click", "none", function(evt) {
        jQuery('#cwcontent').height(600)
                               .width(1000);
        vis.panZoomControlVisible(true);
        jQuery('#caption').html("[Right click a node or edge for more options]");   // Change the caption
        vis.removeListener("click", "none");    // mimic of jQuery.one('click', func)
        vis.zoomToFit();  // Change the scale of the network until it fits the screen, vis.zoom(1.5) is over-sized
    }); */

  });

    // draw options
    var draw_options = {

        network: networkdata,   // your data goes here

        edgeLabelsVisible: false,
        nodeLabelsVisible: true,

        nodeTooltipsEnabled: false,
        edgeTooltipsEnabled: true,

        panZoomControlVisible: false,   // hide pan zoom

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

    vis.draw(draw_options); // draw
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