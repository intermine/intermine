// Functions for worm regulatory network display

function showNetwork(wormRegulatoryNetwork, webapp_baseurl, webapp_path, project_title) {

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

        var menu = '<span id="miRNAtoTF" class="fakelink">Show miRNA<span style="font-family:\'Comic Sans MS\';">-></span>TF Interactions</span>&nbsp;|&nbsp;<span id="TFtoMiRNA" class="fakelink">Show TF<span style="font-family:\'Comic Sans MS\';">-></span>miRNA Interactions</span>&nbsp;|&nbsp;<span id="TFtoTF" class="fakelink">Show TF<span style="font-family:\'Comic Sans MS\';">-</span>TF Interactions</span>&nbsp;|&nbsp;<span id="all" class="fakelink">Show All Interactions</span>';
        jQuery('#menu').html(menu);

        // legend
        var legends = '<table id="legend_node">' +
                     '<tr>' +
                       '<td id="miRNA" style="width:45px;"></td>' +
                       '<td style="width:80px;">miRNA</td>' +
                       '<td id="TF" style="width:40px;"></td>' +
                       '<td style="width:60px;">TF</td>' +
                       '<td id="Essential_TF" style="width:40px;"></td>' +
                       '<td style="width:100px;">Essential TF</td>' +
                       '<td id="HOX_TF" style="width:40px;""></td>' +
                       '<td style="width:80px;">HOX TF</td>' +
                     '</tr>' +
                   '</table>' +
                   '<table id="legend_edge">' +
                     '<tr>' +
                       '<td id="miRNA_target" style="width:150px;"></td>' +
                       '<td id="TF_target" style="width:150px;"></td>' +
                       '<td id="TF_TF" style="width:150px;"></td>' +
                     '</tr>' +
                   '</table>'

        jQuery('#legends').html(legends);
        jQuery('td', '#legend_node').css("font-size","90%");
        jQuery('tr', '#legend_node').css("height","30px");
        jQuery('td', '#legend_edge').css("font-size","80%");
        jQuery('td', '#legend_edge').css("font-family","sans-serif");
        jQuery('tr', '#legend_edge').css("height","30px");

        jQuery('#miRNA').svg();
        jQuery('#miRNA').svg('get').circle(27, 15, 10, {fill: "#ff00ff"});

        jQuery('#TF').svg();
        jQuery('#TF').svg('get').polygon([[10, 25], [30, 25], [20, 5]], {fill: "#4169e1"});

        jQuery('#Essential_TF').svg();
        jQuery('#Essential_TF').svg('get').polygon([[10, 25], [30, 25], [20, 5]], {fill: "red"});

        jQuery('#HOX_TF').svg();
        jQuery('#HOX_TF').svg('get').polygon([[10, 25], [30, 25], [20, 5]], {fill: "yellow"});

        jQuery('#miRNA_target').svg();
        var miRNA_target = jQuery('#miRNA_target').svg('get');
        miRNA_target.line(20, 5, 80, 5, {stroke: "red", strokeWidth: 4});
        miRNA_target.polygon([[80, 0], [80, 10], [100, 5]], {fill: "black"});
        miRNA_target.text(5, 27, "miRNA -> target");

        jQuery('#TF_target').svg();
        var TF_target = jQuery('#TF_target').svg('get');
        TF_target.line(20, 5, 80, 5, {stroke: "#00ff7f", strokeWidth: 4});
        TF_target.polygon([[80, 0], [80, 10], [100, 5]], {fill: "black"});
        TF_target.text(20, 27, "TF -> target");

        jQuery('#TF_TF').svg();
        var TF_TF = jQuery('#TF_TF').svg('get');
        TF_TF.line(20, 5, 80, 5, {stroke: "black", strokeWidth: 4});
        TF_TF.polygon([[80, 0], [80, 10], [100, 5]], {fill: "black"});
        TF_TF.text(10, 27, "TF-TF interaction");

        // corner the description div
        var description = '<div style="padding:8px 8px 8px 8px;">' +
                          '<span>Integrated miRNA-TF regulatory network. ' +
                          'The Transcription Factors (TFs) are organized in three layers. The top layer has ' +
                          'TFs that are not regulated by any other TFs. The middle layer has TFs that both ' +
                          'regulate and are regulated by other TFs, and the third and lowest row has TFs that ' +
                          'do not regulate any other TF. miRNAs either regulating or being regulated by the TFs ' +
                          'are shown. The miRNAs to the left are the ones that regulate at least one of the TFs ' +
                          'in the network. They are further separated into three rows, based on the highest layer ' +
                          'of TFs that they regulate. So if a miRNA regulates a TF in the top row it will be placed ' +
                          'on the top row to the left, and so forth <span id="note_1" class="fakelink">(1)</span>. The miRNAs on the ' +
                          'right are the ones that do not regulate a TF. Their placement within their three layers ' +
                          'is based on the lowest layer TF that regulates it <span id="note_2" class="fakelink">(2)</span>. ' +
                          'All larval TF-TF interactions in HOT regions ' +
                          'were removed. Tissue specificity and number of protein-protein interactions are ' +
                          'shown for each of the hierarchical levles. For further information, please ' +
                          'refer to <a href="http://www.ncbi.nlm.nih.gov/pubmed/221177976" target="_blank">modENCODE ' +
                          'worm integration paper</a> on <i>Science</i>.</span></div>';

        jQuery('.description').html(description).css('background','silver').corner(); // or light blue (#B9E6F0)?


         // qtip configuration
         jQuery("#note_1").qtip({
           content: 'note that if a miRNA regulates two TFs, one in the top layer and one in the second layer it will still be placed in the top layer on the left.',
           style: {
             border: {
               width: 3,
               radius: 8,
               color: 'black'
             },
             tip: 'bottomLeft',
             name: 'light',
             width: 350,
             height:85
           },
            position: {
              corner: {
                 target: 'topMiddle',
                 tooltip: 'bottomLeft'
              }
            },
           show: 'click',
           hide: 'mouseout'
         });

         jQuery("#note_2").qtip({
           content: 'so if a miRNA is regulated by a TF in the second and third layers the miRNA will be placed in the right bottom layer.',
           style: {
             border: {
               width: 3,
               radius: 8,
               color: 'black'
             },
             tip: 'topLeft',
             name: 'light',
             width: 350,
             height:65
           },
           show: 'click',
           hide: 'mouseout'
         });

        // Filter to show miRNA-TF interactions
        jQuery('#miRNAtoTF').click(function(){
             vis.filter("edges", function(edge) {
                 return edge.color == "#ff0000";
             });
         });

        // Filter to show TF-miRNA interactions
        jQuery('#TFtoMiRNA').click(function(){
             vis.filter("edges", function(edge) {
                 return edge.color == "#00ff7f";
             });
         });

        // Filter to show TF-TF interactions
        jQuery('#TFtoTF').click(function(){
            vis.filter("edges", function(edge) {
                return edge.color == "#000000";
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

            url = webapp_baseurl + "/" + webapp_path + "/report.do?id=" + data.id;
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
            evt.target.labelFontSize = 11;
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

        network: wormRegulatoryNetwork,

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
                if (n.data.label.substr(0,3) == "miR"){
                    bypass.nodes[n.data.id] = {opacity: 1, labelFontSize: 11, labelVerticalAnchor: "bottom", label: n.data.label.substr(0,3)+"\n"+n.data.label.substr(3,n.data.label.length)};
                } else {
                    bypass.nodes[n.data.id] = { opacity: 1 };
                }

            });

            var allEdges = vis.edges();
            jQuery.each(allEdges, function(i, e) {
                  bypass.edges[e.data.id] = { opacity: 0.1 };
            });
            var edges = fn.edges;
            jQuery.each(edges, function(i, e) {
                  bypass.edges[e.data.id] = { opacity: 1, width: 2 };
            });

            if (target.data.label.substr(0,3) == "miR"){
                bypass.nodes[target.data.id] = {labelFontSize: 11, labelVerticalAnchor: "bottom"};
            }

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