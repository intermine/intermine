

function showInteractions(data, webapp_baseurl, webapp_path) {

    $("#menu").html("&nbsp;");

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
    vis = new org.cytoscapeweb.Visualization(div_id, options);

    // after init
    vis.ready(function() {

    var caption = "[Click the network to zoom]";
    $("#caption").html(caption);

    var menu = '<span id="phsical" class="fakelink">Show Physical Interactions</span>&nbsp;|&nbsp;<span id="genetic" class="fakelink">Show Genetic Interactions</span>&nbsp;|&nbsp;<span id="all" class="fakelink">Show All Interactions</span>';
    $("#menu").html(menu);

    var legends = '<span>Interaction Type:</span>&nbsp;&nbsp;&nbsp;&nbsp;<span class="physical">legends</span>&nbsp;&nbsp;<span>Physical</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span class="genetic">legends</span>&nbsp;&nbsp;<span>Genetic</span>';
    $("#legends").html(legends);

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

     // Filter to show phsical interactions
     $("#phsical").click(function(){
         vis.filter("edges", function(edge) {
             return edge.color >= "#FF0000";
         });
     });

    // Filter to show genetic interactions
    $("#genetic").click(function(){
        vis.filter("edges", function(edge) {
            return edge.color <= "#FF0000";
        });
    });

    // Show all interactions
    $("#all").click(function(){
        vis.filter("edges", function(edge) {
            return edge.color;
        });
    });

    vis.addContextMenuItem("View FlyMine report...", "nodes", function(evt) {
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

    vis.addContextMenuItem("Export as SIF...", "none", function(evt) {
      vis.exportNetwork('sif', 'cytoscapeNetworkExport.do?type=sif');
    })

    vis.addContextMenuItem("Export as XGMML...", "none", function(evt) {
      vis.exportNetwork('xgmml', 'cytoscapeNetworkExport.do?type=xgmml');
    })

    .addListener("mouseover", "nodes", function(evt) {
        _mouseOverNode = evt.target;
        highlighFirstNeighbors(evt.target);
    })

    .addListener("mouseout", "nodes", function(evt) {
        _mouseOverNode = null;
        clearFirstNeighborsHighligh();
    })

    .addListener("dblclick", "nodes", function(evt) {
         selectFirstNeighbors(evt.target);
     });
    });

    // draw options
    var draw_options = {
        // your data goes here
        network: data,

        // show edge labels too
        edgeLabelsVisible: false,
        nodeLabelsVisible: true,

        // hide pan zoom
        panZoomControlVisible: false,

        visualStyle: {
            global: {
               backgroundColor: "#FFFFFF"
            },

            nodes: {
                borderWidth: 1,
                label: { passthroughMapper: { attrName: "id" } },
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
              color: {
                  discreteMapper: { attrName: "interaction",
                                    entries: [
                                              { attrValue: "physical", value: "#FF0000" },
                                              { attrValue: "genetic", value: "#0000FF" }
                                             ]
                                   }
                      },

              selectionGlowOpacity: 0
                    }
             }

        };

    // draw
    vis.draw(draw_options);

    $("#cytoscapeweb").one('click', function () {
       $(this).height(600)
              .width(1000);
       // Resize effect from jQuery UI, but not working properly on flash
       // $(this).effect("size", { to: {width: 1000,height: 600} }, 1000);
       // Add zoom control panel
       vis.panZoomControlVisible(true);
       // Change the scale of the network until it fits the screen.
       vis.zoom(1);
       vis.nodeTooltipsEnabled(false);
       vis.edgeTooltipsEnabled(false);
       // Change the caption
       $("#caption").html("[Right click a node for more options]");
    });


}

    function highlighFirstNeighbors(target) {
        setTimeout(function() {
            if (_mouseOverNode != null && _mouseOverNode.data.id === target.data.id) {
                var fn = vis.firstNeighbors([target]);
                var bypass = { nodes: {}, edges: {} };

                var allNodes = vis.nodes();
                $.each(allNodes, function(i, n) {
                      bypass.nodes[n.data.id] = { opacity: 0.2 };
                });
                var neighbors = fn.neighbors;
                neighbors = neighbors.concat(fn.rootNodes);
                $.each(neighbors, function(i, n) {
                      bypass.nodes[n.data.id] = { opacity: 1 };
                });

                var allEdges = vis.edges();
                $.each(allEdges, function(i, e) {
                      bypass.edges[e.data.id] = { opacity: 0.1 };
                });
                var edges = fn.edges;
                $.each(edges, function(i, e) {
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