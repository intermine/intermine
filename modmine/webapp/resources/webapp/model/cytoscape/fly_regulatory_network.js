// Functions for fly regulatory network display

function showNetwork(flyRegulatoryNetwork, webapp_baseurl, webapp_path, project_title) {

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
    vis = new org.cytoscapeweb.Visualization(div_id, options);

    // post init
    vis.ready(function() {

        var menu = '<span id="miRNA" class="fakelink">Show miRNA-TF Interactions</span>&nbsp;|&nbsp;<span id="TF" class="fakelink">Show TF-TF Interactions</span>&nbsp;|&nbsp;<span id="all" class="fakelink">Show All Interactions</span>';
        jQuery('#menu').html(menu);

        // legend
        var legends = '<table id="legend_table">' +
                         '<tr>' +
                           '<td align="right">Interaction Type:</td>' +
                           '<td id="miRNA_edge_svg" style="width:50px;"></td>' +
                           '<td>miRNA regulatory connection</td>' +
                           '<td id="TF_edge_svg" style="width:50px;"></td>' +
                           '<td>TF regulatory connection</td>' +
                         '</tr>' +
                         '<tr>' +
                           '<td align="right">Node Type:</td>' +
                           '<td id="miRNA_node_svg" style="width:50px;"></td>' +
                           '<td>miRNA</td>' +
                           '<td id="TF_node_svg" style="width:50px;""></td>' +
                           '<td>Transcription factor</td>' +
                         '</tr>' +
                       '</table>'

        jQuery('#legends').html(legends);
        jQuery('td', '#legend_table').css("font-size","90%");
        jQuery('tr', '#legend_table').css("height","30px");

        jQuery('#miRNA_edge_svg').svg();
        var svg_miRNA_edge = jQuery('#miRNA_edge_svg').svg('get');
        svg_miRNA_edge.line(10, 16, 42, 16, {stroke: "#ff0000", strokeWidth: 4});

        jQuery('#TF_edge_svg').svg();
        var svg_TF_edge = jQuery('#TF_edge_svg').svg('get');
        svg_TF_edge.line(10, 16, 42, 16, {stroke: "#33cc33", strokeWidth: 4});

        jQuery('#miRNA_node_svg').svg();
        var svg_miRNA_node = jQuery('#miRNA_node_svg').svg('get');
        svg_miRNA_node.circle(27, 15, 10, {fill: "#ff0000"});

        jQuery('#TF_node_svg').svg();
        var svg_TF_node = jQuery('#TF_node_svg').svg('get');
        svg_TF_node.circle(27, 15, 10, {fill: "#33cc33"});

        // corner the description div
        var description = '<div style="padding:8px 8px 8px 8px;">' +
                          '<span>This network is a hierarchical view of mixed ChIP-based/miRNA physical ' +
                          'regulatory network that combines transcrip-tional regulation by 76 TFs ' +
                          '(green) from ChIP experiments and posttranscriptional regulation by 52 ' +
                          'miRNAs (red). TFs are organized in a five-level hierarchy on the basis ' +
                          'of their relative proportion of TF targets versus TF regulators. miRNAs ' +
                          'are separated into two groups: the ones that are regulated by TFs (left) ' +
                          'and the ones that only regulate TFs (right). For more information, please ' +
                          'refer to <a href="http://www.ncbi.nlm.nih.gov/pubmed/21177974" target="_blank">modENCODE ' +
                          'fly integration paper</a> on <i>Science</i>.</span></div>';

        jQuery('.description').html(description).css('background','silver').corner(); // or light blue (#B9E6F0)?

        // Filter to show miRNA-TF interactions
        jQuery('#miRNA').click(function(){
             vis.filter("edges", function(edge) {
                 return edge.color >= "#FF0000";
             });
         });

        // Filter to show TF-TF interactions
        jQuery('#TF').click(function(){
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

        vis.addContextMenuItem("View " + project_title + " gene report...", "nodes", function(evt) {
            var data = evt.target.data;

            url = webapp_baseurl + "/" + webapp_path + "/portal.do?externalid=" + data.id + "&class=Gene";
            // url = webapp_baseurl + "/" + webapp_path + "/report.do?id=" + data.id;
            window.open(url);
        })

        /*
        .addContextMenuItem("Export network as SIF...", "none", function(evt) {
          vis.exportNetwork('sif', 'cytoscapeNetworkExport.do?type=sif');
        })

        .addContextMenuItem("Export network as XGMML...", "none", function(evt) {
          vis.exportNetwork('xgmml', 'cytoscapeNetworkExport.do?type=xgmml');
        })

        .addContextMenuItem("Export network as SVG...", "none", function(evt) {
          vis.exportNetwork('svg', 'cytoscapeNetworkExport.do?type=svg');
        })
        */

        .addListener("mouseover", "nodes", function(evt) {
            _mouseOverNode = evt.target; //global, once this function is called
            highlightFirstNeighbors(evt.target);
        })

        .addListener("click", "nodes", function(evt) {
            _mouseOverNode = evt.target; //global, once this function is called
            highlightFirstNeighbors(evt.target);
        })

        .addListener("mouseout", "nodes", function(evt) {
            _mouseOverNode = null;
            clearFirstNeighborsHighligh();
        })

        .addListener("dblclick", "nodes", function(evt) {
             selectFirstNeighbors(evt.target);
         })

         .addListener("click", "edges", function(evt) {
             // showQTip(env);
         })

        .addListener("mouseout", "edges", function(evt) {
            setTimeout(function() {
                vis.visualStyleBypass({});
              }, 400);
        })

        .zoomToFit();

  });

    // draw options
    var draw_options = {

        network: flyRegulatoryNetwork,

        layout: 'Preset', // for xgmml format

        // show labels
        edgeLabelsVisible: false,
        nodeLabelsVisible: true,

        // hide pan zoom
        panZoomControlVisible: true,

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
                selectionBorderWidth: 5,
                selectionBorderColor: "#ffff00",
                selectionGlowColor: "#ffff00"
            },

            edges: {
                opacity: 0.3
            }
         }
    };

    // draw
    vis.draw(draw_options);
}

function highlightFirstNeighbors(target) {
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
                  bypass.edges[e.data.id] = { opacity: 1, width: 2 };
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

function showQTip(env) {
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
        content: evt.target.data.interactionType,

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
}