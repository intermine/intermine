// icons in for this tool comes from http://prothemedesign.com/circular-icons/
// license says free for any use but no re-distribution, so please download it
// directly from that site yourself or make/choose your own icons

// the text legned feature requires the Ext.ux.CheckColumn.js (CheckColumn.js in Extjs package)
function notImplemented() { Ext.Msg.alert('Error', 'Not implemented') }
Ext.canvasXpress = Ext.extend(Ext.Panel, {
  data: false,
  options: false,
  cxpevents: false,
  //bodyStyle: 'overflow: auto;',
  layout: 'fit',
  contextMenu: true,
  menuTitle: 'Customize',
  showPrint: true,
  showToolbar: true,
  changedNodes: [],
  changedEdges: [],
  removedNodes: [], // should be handled for removenode and saveallchanges events by child class that does not commit delete changes immediately to DB (which causes nodeIndice issue etc.)
  removedEdges: [], // same as above (removeedge instead of removenode of course)
  initComponent: function() {
    this.canvasId = (this.id || Ext.id()) + 'canvas';
    if(!this.stackViewObj) this.stackViewObj = {};
    if(!this.listeners) this.listeners = { scope: this };
    if(!this.listeners.resize)
      this.listeners.resize = this.resizeCanvas.createDelegate(this);
    if(this.options && this.options.graphType == 'Network')
    {
      var saveNewView = {
          icon: this.imgDir + 'picture_add.png',
          text: 'Save as A New View',
          scope: this,
          handler: this.storeView
        }, items = [
          {
            icon: this.imgDir + 'add.png',
            tooltip: 'Add Node/Edge/Legend/Network',
            menu : {
              items: [{
                icon: this.imgDir + 'bullet_red.png',
                text: 'Add Node',
                handler: this.editNode.createDelegate(this, [null, null, true], true)
              }, {
                icon: this.imgDir + 'arrow.png',
                text: 'Add Edge',
                scope: this,
                handler: this.editEdge
              }, {
                icon: this.imgDir + 'network.png',
                text: 'Save as A New Network',
                scope: this,
                handler: this.editNetwork.createDelegate(this, [true])
              }],
              listeners: {
                scope: this,
                show: function(m) {
                  for(var i = m.items.items.length; i > 2; i--)
                    m.remove(m.items.items[i]);
                  var al = [], l = this.canvas.data.legend;
                  if(!l || !l.nodes || !l.nodes.length)
                    m.add({
                      icon: this.imgDir + 'add.png',
                      text: 'Node Legend',
                      handler: this.editLegend.createDelegate(this, ['node'], true)
                    });
                  if(!l || !l.edges || !l.edges.length)
                    m.add({
                      icon: this.imgDir + 'add.png',
                      text: 'Edge Legend',
                      handler: this.editLegend.createDelegate(this, ['edge'], true)
                    });
                  if(!l || !l.text || !l.text.length)
                    m.add({
                      icon: this.imgDir + 'add.png',
                      text: 'Text Legend',
                      handler: this.editLegend.createDelegate(this, ['text'], true)
                    });
                }
              }
            }
          },
          {
            icon: this.imgDir + 'edit.png',
            tooltip: 'Edit Legend/Network',
            itemId: 'editmenu',
            menu : {
              items: [{
                text: 'Edit Network',
                itemId: 'editnet',
                icon: this.imgDir + 'network.png',
                scope: this,
                handler: this.editNetwork,
                disabled: !(this.networkInfo && this.networkInfo.id)
              }],
              listeners: {
                scope: this,
                show: function(m) {
                  for(var i = m.items.items.length; i > 0; i--)
                    m.remove(m.items.items[i]);
                  var al = [], l = this.canvas.data.legend;
                  if(!(!l || !l.nodes || !l.nodes.length))
                    m.add({
                      icon: this.imgDir + 'edit.png',
                      text: 'Node Legend',
                      handler: this.editLegend.createDelegate(this, ['node'], true)
                    });
                  if(!(!l || !l.edges || !l.edges.length))
                    m.add({
                      icon: this.imgDir + 'edit.png',
                      text: 'Edge Legend',
                      handler: this.editLegend.createDelegate(this, ['edge'], true)
                    });
                  if(!(!l || !l.text || !l.text.length))
                    m.add({
                      icon: this.imgDir + 'edit.png',
                      text: 'Text Legend',
                      handler: this.editLegend.createDelegate(this, ['text'], true)
                    });
                }
              }
            }
          },
          {
            icon: this.imgDir + 'save.png',
            tooltip: 'Save the Entire Network and Reload It (could be slow!)',
            disabled: true,
            scope: this,
            handler: this.saveMap
          },
          {
            icon: this.imgDir + 'turn_left.png',
            tooltip: 'Undo',
            disabled: true,
            scope: this,
            handler: function(){this.canvas.undoNetworkOp()}
          },
          {
            icon: this.imgDir + 'turn_right.png',
            tooltip: 'Redo',
            disabled: true,
            scope: this,
            handler: function(){this.canvas.redoNetworkOp()}
          },
          {
            icon: this.imgDir + 'power_on.png',
            tooltip: 'Show Snapshot Control',
            scope: this,
            handler: function(b, e) {
              this.toggleSnapshotCtrl(b, [e.xy[0] + 15, e.xy[1] + 15]);
              this.toggleToolbarBtn(/power_on/.test(b.icon)?
                {text:'Snapshot Control',icon:this.imgDir + 'power_off.png',tip:'Hide Snapshot Control'} :
                {text:'Snapshot Control',icon:this.imgDir + 'power_on.png',tip:'Show Snapshot Control'});
            }
          },
          {
            tooltip: 'View Menu (currently in original view)',
            icon: this.imgDir + 'picture.png',
            itemId: 'views',
            menu: {
              items: [saveNewView],
              listeners: {
                scope: this,
                show: function(m) {
                  m.removeAll();
                  if(this.pViews && this.pViews.length) // has existing pathway views
                  {
                    if(this.viewInfo) m.add({text:'Current view: <b>'+this.viewInfo.name+'</b>'}, '-');
                    m.add(saveNewView, {
                      icon: this.imgDir + 'pictures.png',
                      text: 'Manage View(s) for This Network',
                      scope: this,
                      handler: function() {
                        var win = new Ext.canvasXpress.ViewManager({list:this.pViews}, this);
                        win.show();
                      }
                    });
                    if(this.viewInfo) // current network is a view
                    {
                      m.add({
                        icon: this.imgDir + 'picture_save.png',
                        text: 'Save the Current View',
                        handler: this.storeView.createDelegate(this, [true])
                      }, {
                        icon: this.imgDir + 'picture_delete.png',
                        text: 'Delete the Current View',
                        scope: this,
                        handler: function() {
                          Ext.Msg.confirm('Warning', 'Are you sure you would like to delete the view? This operation is NOT reversible!', function(btn) {
                            if(btn == 'yes') this.deleteView(this.viewInfo.id);
                          }, this);
                        }
                      }, {
                        icon: this.imgDir + 'picture_edit.png',
                        text: 'Edit the Current View',
                        handler: this.editView.createDelegate(this, [this.viewInfo])
                      }, '-', {
                        icon: this.imgDir + 'turn_left.png',
                        text: 'Revert to the Original View',
                        scope: this,
                        handler: this.revertView
                      });
                    }
                    else m.add('-');
                    for(var i = 0; i < this.pViews.length; i++)
                    {
                      if(!(this.viewInfo && this.viewInfo.id == this.pViews[i].id))
                        m.add({
                          text: 'View "' + this.pViews[i].name + '"',
                          handler: this.loadView.createDelegate(this, [this.pViews[i]])
                        });
                    }
                  }
                  else m.add(saveNewView);
                }
              }
            }
          },
          {
            tooltip: 'Simple Search',
            icon: this.imgDir + 'simple_find.png',
            scope: this,
            handler: this.simpleSearch
          },
          {
            tooltip: 'Advanced Search',
            icon: this.imgDir + 'advanced_find.png',
            scope: this,
            menu: [
              {
                text: 'Nodes',
                icon: this.imgDir + 'bullet_red.png',
                scope: this,
                handler: this.showSearchNodes
              },
              {
                text: 'Edges',
                icon: this.imgDir + 'arrow.png',
                scope: this,
                handler: this.showSearchEdges
              },
              {
                text: 'Mixed',
                icon: this.imgDir + 'network.png',
                scope: this,
                handler: this.showSearchAll
              }
            ]
          },
          {
            tooltip: 'Zoom Out',
            icon: this.imgDir + 'magnifier_zoom_out.png',
            scope: this,
            handler: function() { this.canvas.zoom *= 0.85; this.canvas.draw(); }
          },
          {
            tooltip: 'Zoom In',
            icon: this.imgDir + 'magnifier_zoom_in.png',
            scope: this,
            handler: function() { this.canvas.zoom *= 1.15; this.canvas.draw(); }
          },
          {
            tooltip: 'Show Hidden Nodes',
            icon: this.imgDir + 'eye.png',
            itemId: 'hidenode',
            menu: [{
              icon: this.imgDir + 'eye.png',
              text: 'All Hidden Nodes',
              handler: this.toggleNode.createDelegate(this, [null, false])
            }],
            disabled: true
          },
          {
            tooltip: 'Export Network as An Image for Printing/Importing Elsewhere',
            icon: this.imgDir + 'print.png',
        		canvasId: this.canvasId,
        		handler: this.onPrintGraph
          },
          {
            tooltip: 'Tiled Views',
            icon: this.imgDir + 'application_view_tile.png',
            menu: {
              items:[{
                icon: this.imgDir + 'application_tile_horizontal.png',
                text: 'Add Current View to Tile Stack',
                scope: this,
                handler: function() {
                  var f = function(b, t) {
                    if(!this.stackViewObj.stackViews) this.stackViewObj.stackViews = [[]];
                    this.stackViewObj.stackViews[0][0] = t;
                    Ext.Msg.prompt('Caption for the current tile', 'Please enter the caption for the current tile', function(btn, txt) {
                      if(btn == 'ok')
                        this.stackViewObj.stackViews.push([txt, this.canvas.canvas.toDataURL("image/png")]);
                    }, this);
                  }.createDelegate(this);
                  if(this.stackViewObj.stackViews && this.stackViewObj.stackViews.length)
                    f('ok', this.stackViewObj.stackViews[0][0]);
                  else
                    Ext.Msg.prompt('Main Caption', 'Please enter the title caption for all the tiles you will be adding:', f, this);
                }
              }],
              listeners: {
                scope: this,
                show: function(m) {
                  for(var i = m.items.items.length; i; i--)
                    m.remove(m.items.items[i]);
                  if(this.stackViewObj.stackViews && this.stackViewObj.stackViews.length)
                  {
                    m.add({
                      icon: this.imgDir + 'application_view_tile.png',
                      text: 'View the Tiles Now',
                      scope: this,
                      handler: this.showTiles
                    }, {
                      icon: this.imgDir + 'refresh.png',
                      text: 'Clear the Tiles!',
                      scope: this,
                      handler: function() {
                        Ext.Msg.confirm('Are you sure?', 'Resetting all tiled views is not reversible, are you sure you would like to remove all tiled views now?', function(b) {
                          if(b == 'yes') delete this.stackViewObj.stackViews;
                        }, this);
                      }
                    }, {
                      icon: this.imgDir + 'pencil.png',
                      text: 'Edit the Tile Titles',
                      scope: this,
                      handler: function() {
                        var win = new Ext.Window({
                          width: 400,
                          height: 300,
                          layout: 'fit',
                          items: {
                            xtype: 'editorgrid',
                            title: 'Just click to edit any title',
                            store: new Ext.data.ArrayStore({
                              autoDestroy: true,
                              fields: ['text', 'img'],
                              data: this.stackViewObj.stackViews
                            }),
                            cm: new Ext.grid.ColumnModel({
                              columns: [
                                {
                                  header: 'Title',
                                  dataIndex: 'text',
                                  editor: new Ext.form.TextField({
                                    listeners: {
                                      scope: this,
                                      change: function(c, nv, ov) {
                                        var r = c.gridEditor.record, s = r.store, idx = s.indexOf(r);
                                        this.stackViewObj.stackViews[idx][0] = nv;
                                      }
                                    }
                                  })
                                }
                              ]
                            }),
                            width: 390,
                            height: 290,
                            autoExpandColumn: 0,
                            frame: false,
                            clicksToEdit: 1
                          }
                        });
                        win.show();
                      }
                    }, {
                      text: 'Set the Number of Tiles Per Row',
                      scope: this,
                      handler: function() {
                        Ext.Msg.prompt('Tile per Row', 'Please enter how many tiles should be put in each row:', function(btn, txt) {
                          if(btn == 'ok')
                          {
                            if(!isNaN(txt) && txt >= 1 && txt <= 10)
                              this.stackViewObj.perRow = Math.floor(txt);
                            else
                              Ext.Msg.alert('Error', txt + ' is an invalid number! Only numbers between 1 and 10 are accepted');
                          }
                        }, this, false, this.stackViewObj.perRow || 2);
                      }
                    });
                  }
                }
              }
            }
          },
          {
            icon: this.imgDir + 'refresh.png',
            tooltip: 'Reset View/Selection (unselect all nodes and view entire network)',
            scope: this,
            handler: function() { this.canvas.redraw() }
          }
        ];
      if(this.toolButtons)
      {
        for(var i = 0; i < this.toolButtons.length; i++)
          items.push(this.toolButtons[i]);
      }
      this.tbar = { items: items };
    }
    // initToolbarState will change toolbar buttons based on the network info when it finishes loading
    Ext.canvasXpress.superclass.initComponent.apply(this, arguments);
    this.resetChanges();
  },
  resizeCanvas: function(p, aw, ah, rw, rh) {
    if(this.noResize)
    {
      delete this.noResize;
      return;
    }
    if(!this.canvas) return;
    if(this.options.graphType == 'Network')
    {
      var tmp = this.canvas.networkFreeze;
      this.canvas.networkFreeze = false;
      this.canvas.setDimensions(aw - 7 * 2, ah - 7 - 35); // these are hard-coded middle-container and north-container etc w/h
      this.canvas.networkFreeze = tmp;
    }
    else
      this.canvas.setDimensions(aw - 7 * 2, ah - 7 - 35); // these are hard-coded middle-container and north-container etc w/h
  },
  showTiles: function() {
    var perrow = this.stackViewObj.perRow || 2, html = ['<html><head><style>td { text-align:center; }</style></head><body><h3>' + this.stackViewObj.stackViews[0][0] + '</h3>\n<table>'], w = Math.floor((window.innerWidth-15) / perrow) - 10;
    for(var i = 1; i < this.stackViewObj.stackViews.length; i++)
    {
      var img = this.stackViewObj.stackViews[i];
      if(!((i-1) % perrow))
      {
        if(html.length > 1) html.push('</tr>');
        html.push('<tr>');
      }
      html.push('<td><img src="', img[1], '" style="width:' + w + 'px;" /><br><b>' + img[0] + '</b></td>');
    }
    window.open().document.write(html.join('\n') + '</tr>\n</table>\n</body></html>');
  },
  toggleSaveNetworkBtn: function() {
    this.toggleToolbarBtn({text:'Save All Changes and Reload the Network',
      toggle: !this.viewInfo && this.hasUnsavedChanges() && this.hasListener('saveallchanges')? 1 : 2});
  },
  toggleToolbarBtn: function(obj) {
    var tb = this.getTopToolbar();
    if (tb && tb.items && tb.items.items) {
      var it = tb.items.items, b, re = new RegExp(obj.text);
      for(var i = 0; i < it.length; i++)
      if(re.test(it[i].tooltip))
      {
        b = it[i];
        break;
      }
      if(b)
      {
        if(obj.icon) b.setIcon(obj.icon);
        if(obj.tip) b.setTooltip(obj.tip);
        if(obj.toggle)
        {
          if(obj.toggle == 1) b.enable();
          else b.disable();
        }
      }
    }
  },
  onRender:function() {
    Ext.canvasXpress.superclass.onRender.apply(this, arguments);
    if (this.contextMenu) {
      this.el.on({contextmenu:{scope:this, fn:this.onContextMenu, stopEvent:true}});
    }
  },
  afterRender: function() {
    Ext.canvasXpress.superclass.afterRender.apply(this, arguments);
    // To cope with the remote services divs we resize the canvas
    var dw = this.options.decreaseWidth || 0;
    var dh = this.options.decreaseHeight || 0;
    var pw = this.el.dom.parentNode ? this.el.dom.parentNode.clientWidth - dw : 500;
    var ph = this.el.dom.parentNode ? this.el.dom.parentNode.clientHeight - dh: 500;
    // Add the canvas tag
    Ext.DomHelper.append(this.body, {
      tag: 'canvas',
      id: this.canvasId,
      width: dw && pw ? pw : this.width || pw,
      height: dh && ph ? ph : this.height || ph
    });
    // first set up default events
    var cxpevents = {};
    switch(this.options.graphType) {
      case 'Network':
        this.addEvents(
          'endnodedrag','removenode','removeedge',
          'expandgroup','togglenode','togglechildren','saveallchanges',
          'updatenode','updateedge','updatenodes','updateedges',
          'updatelegend','updateorder','leftclick','importdata',
          'loadview','saveview','deleteview','doubleclick','updatetag');
        cxpevents.enddragnode = this.endDrag.createDelegate(this);
        cxpevents.click = function(o, e) {
          if(this.selectCallback)
            this.selectCallback(o, e);
          else if(this.hasListener('leftclick'))
            this.fireEvent('leftclick', o, e)
        }.createDelegate(this);
        cxpevents.dblclick = function(o, e) {
          if(this.hasListener('doubleclick'))
            this.fireEvent('doubleclick', o, e)
        }.createDelegate(this);
        cxpevents.stackchange = function(idx, len) {
          this.toggleToolbarBtn({text:'^Redo$',toggle:idx<len-1?1:2});
          this.toggleToolbarBtn({text:'^Undo$',toggle:idx>=1?1:2});
        }.createDelegate(this);
        if(!this.hasListener('removenode'))
          this.on('removenode', function(n, f) { f() });
        if(!this.hasListener('removeedge'))
          this.on('removeedge', function(n, f) { f() });
    }
    if(!this.cxpevents) this.cxpevents = {};
    Ext.applyIf(this.cxpevents, cxpevents);
    // Now get the canvasXpress object
    this.canvas = new CanvasXpress(this.canvasId, this.data, this.options, this.cxpevents);
    this.canvas.Ext = this;
    this.canvas.highlightNode = []; // clear up the initial highlight
    var ss = this.canvas.shapes, found = false;
    for(var i = 0; i < ss.length; i++)
      if(ss[i] == 'custom')
      {
        found = true;
        break;
      }
    if(!found) ss.unshift('custom');

    if (this.canvas.version < 2) {
      var msg = 'Please download a newer version of canvasXpress at:<br>';
      msg += '<a target="blank" src="http://www.canvasXpress.org">http://www.canvasXpress.org</a><br>';
      msg += 'You are using an older version that dO NOT support all the functionality of this panel';
      Ext.MessageBox.alert('Warning', msg);
    }
    this.on('destroy', function(p) { // cleanup the associated toolbar, windows etc.
      if(p.snapshotCtrl) p.snapshotCtrl.close();
      if(this.searchWin) this.searchWin.close();
      if(this.simpleSearchWin) this.simpleSearchWin.close();
    });
    this.on('beforedestroy', function(p) {
      p.canvas.destroy(p.canvas.target);
    });
    this.on('beforeclose', function(p) {
      p.canvas.destroy(p.canvas.target);
    });
  },
  initToolbarState: function() { // call it only after the network data is loaded and better, drawn
    var hn = Ext.canvasXpress.utils.getHiddenNodes(this), tb = this.getTopToolbar();
    if(hn.length > 1)
    {
      var btn = tb.getComponent('hidenode');
      btn.menu = new Ext.menu.Menu(hn);
      btn.enable();
    }
  },
  onContextMenu: function (e, o, r) {
    if (e.browserEvent) {
      if (typeof(this.contextMenu) == 'object') {
        this.contextMenu.destroy();
      }
      this.contextMenu = new Ext.menu.Menu(this.createContextMenu(e));
      this.contextMenu.showAt(e.getXY());
      e.stopEvent();
    }
  },
  selectNode: function(callback) {
//     Ext.Msg.alert('Select the node', 'Next please click on the node you want to select it');
    this.selectCallback = function(o) {
      if(o.nodes && o.nodes.length)
      {
        var node = o.nodes[0];
//         this.canvas.flashNode(node.id);
//         Ext.Msg.confirm('This Node?', 'Is the flashing node the correct one?', function(btn) {
//           if(btn == 'yes')
//           {

            callback(node);
            this.selectCallback = null;

//           }
//           else
//             Ext.Msg.alert('Please select', 'Please select the node again');
//         }, this);
      }
    }.createDelegate(this);
  },
  networkMenu: function(o, type, it) {
//     var xy = this.canvas.adjustedCoordinates(this.clickXY);
    var xy = this.canvas.findXYCoordinates(this.clickXY);
    var items = [];
    if(type == 'cs') // right click menu only
    {
      var func = function(nodes) {
        this.clipboard.type = 'nodes';
        // find the edges
        var n1 = nodes, es = [], all = this.canvas.data.edges;
        if(n1.length > 1)
        {
          var idmap = {};
          for(var i = 0; i < n1.length; i++)
            idmap[n1[i].id] = 1;
          for(var i = 0; i < all.length; i++)
          {
            var e = all[i];
            if(idmap[e.id1] && idmap[e.id2])
              es.push(e);
          }
        }
        this.clipboard.obj = {
          bounds: {minX:this.canvas.minX, minY:this.canvas.minY, maxX:this.canvas.maxX, maxY:this.canvas.maxY},
          nodes: this.canvas.cloneObject(n1),
          edges: this.canvas.cloneObject(es),
          extra: this.copyExtra? this.copyExtra(n1, es) : null
        };
      }, func1 = function(nodes) {
        this.clipboard.type = 'nodestyle';
        this.clipboard.obj = { node: this.canvas.cloneObject(nodes[0]) };
      };

      var tags = [], tagn = this.canvas.data.taggedNodes;
      if(tagn)
        for(var i in tagn)
          tags.push(i);
      tags.sort(Ext.canvasXpress.utils.ciSort);

      if(o && o.nodes && o.nodes.length) // canvasXpress only sends 1 node over
      {
        if(this.canvas.selectNode)
        {
          var cnt = 0, found = false;
          for(var i in this.canvas.selectNode)
          {
            cnt++;
            if(i == o.nodes[0].id)
              found = true;
          }
          if(found && cnt > 1)
          {
            for(var i in this.canvas.selectNode)
              if(i != o.nodes[0].id)
                o.nodes.push(this.canvas.nodes[i]);
          }
        }
        if(this.clipboard)
        {
          var n1 = o.nodes, label = this.getName(n1[0]);
          items.push({
            icon: this.imgDir + 'copy.png',
            text: n1.length > 1? 'Copy the Selected Node(s and Edges)' : 'Copy ' + label,
            handler: func.createDelegate(this, [n1])
          });
          if(n1.length == 1)
            items.push({
              icon: this.imgDir + 'copy.png',
              text: 'Copy the Style of ' + label,
              handler: func1.createDelegate(this, [n1])
            });
          if(this.clipboard.type == 'nodestyle' && this.clipboard.obj && this.clipboard.obj.node)
            items.push({
              icon: this.imgDir + 'paste.png',
              text: 'Apply the Copied Style (' + this.getName(this.clipboard.obj.node) + ') to ' + (n1.length > 1? 'Selected Nodes' : label),
              scope: this,
              handler: function() {
                var ns = this.clipboard.obj.node, copy = {width:1,size:1,height:1,labelSize:1,imagePath:1,shape:1,color:1,hideLabel:1,rotate:1,outline:1,outlineWidth:1,pattern:1,eventless:1,hideTooltip:1,hidden:1,anchor:1,fixed:1};
                for(var i = 0; i < o.nodes.length; i++)
                {
                  var nt = o.nodes[i];
                  for(var j in copy)
                    nt[j] = ns[j];
                  this.addNode(nt);
                }
                this.canvas.draw();
                this.canvas.addToNetworkStack();
              }
            });
        }
        if(o.nodes.length > 1)
        {
          var n = o.nodes;
          items.push({
            icon: this.imgDir + 'edit.png',
            text: 'Edit the Selected Nodes',
            handler: this.editNode.createDelegate(this, [n], true)
          }, {
            icon: this.imgDir + 'shape_group.png',
            text: 'Group the Selected Nodes',
            scope: this,
            handler: function () {
              // first find all the parents of the nodes
              var conflict = false, childids = {}, allids = {}, setParent = [];
              var seen = {}, found, found1, changed = [], f = function(nid) { // called when node added successfully
                if(typeof(nid) == 'object') nid = nid.id;
                for(var i = 0; i < n.length; i++)
                  if(n[i].parentNode != nid && n[i].id != nid)
                  {
                    changed.push(n[i]);
                    n[i].parentNode = nid;
                  }
                this.addNode(changed); // there might be a lot of nodes, saveMap makes the most sense
                this.saveMap(null, {respectNoPost:true});
              }.createDelegate(this);
              for(var i = 0; i < n.length; i++)
                allids[n[i].id] = 1;
              for(var i = 0; i < n.length; i++)
              {
                var cn = n[i], bad = false;
                if(childids[cn.id]) continue;
                while(cn.parentNode && allids[cn.parentNode])
                {
                  bad = true;
                  childids[cn.id] = 1;
                  cn = cn.parentNode;
                }
                if(!bad) setParent.push(cn);
              }
              for(var i = 0; i < setParent.length; i++)
              {
                if(setParent[i].parentNode)
                {
                  if(found && !seen[setParent[i].parentNode])
                  {
                    found1 = setParent[i];
                    conflict = true;
                  }
                  else if(found === undefined) found = setParent[i];
                  seen[setParent[i].parentNode] = 1;
                }
              }
              var f1 = function() {
                if(!found || conflict) // gotta make a new node, tricky
                {
                  Ext.Msg.show({
                    title:'Important',
                    msg: 'You must either choose a group parent node or create one first. Click "Yes" then click on the node you want to choose it as group parent, "No" to create a new group parent node, and "Cancel" to abort grouping the nodes.".',
                    buttons: Ext.Msg.YESNOCANCEL,
                    scope: this,
                    fn: function(btn) {
                      if(btn == 'yes') this.selectNode(f);
                      else if(btn == 'no')
                      {
                        this.editNode(null, null, null, function(node) { // node is the added node
                          if(node.id) f(node.id);
                          else
                            Ext.Msg.alert('Error', 'Adding the new group node failed, so the grouping of the selected node was aborted. Please have the admin fix the add node issue before trying to group them again.');
                        }.createDelegate(this));
                      }
                    },
                    icon: Ext.MessageBox.QUESTION
                  });
                }
                else f(found.parentNode);
              }.createDelegate(this);
              if(conflict)
              {
                Ext.Msg.confirm('Error', 'The nodes you selected belong to different immediate groups - e.g., '+this.getName(found1)+'\'s parent is '+this.getName(this.canvas.nodes[found1.parentNode])+', different than '+this.getName(found)+'\'s parent '+this.getName(this.canvas.nodes[found.parentNode])+'. If you press "Yes" button below, then you will be prompted to choose a new commond parent node from existing nodes or create a new node as group parent. Do you want to proceed?', function(btn) {
                  if(btn == 'yes') f1();
                });
              }
              else if(setParent.length == 1)
              {
                Ext.Msg.alert('Info', 'All nodes that you selected already belong to the same group (group parent is:'+this.getName(setParent[0])+')! No need for grouping again.');
                return;
              }
              else f1();
            }
          }, {
            icon: this.imgDir + 'shape_ungroup.png',
            text: 'Ungroup the Selected Nodes',
            scope: this,
            handler: function () {
              // first find all the parents of the nodes
              var seen = {}, found, all = [], multi = false;
              for(var i = 0; i < n.length; i++)
                if(n[i].parentNode)
                {
                  if(found && !seen[n[i].parentNode]) multi = true;
                  found = n[i];
                  seen[n[i].parentNode] = 1;
                  all.push(n[i]);
                }
              Ext.Msg.confirm(multi? 'Warning!!':'Confirm the operation', multi?'The nodes you selected belong to different groups. Ungrouping multiple groups at once is frequently a user mistake, are you sure you still want to proceed? This operation cannot be reverted!':'This operation cannot be reverted. Are you sure you would like to ungroup the whole group (group parent: '+this.getName(this.canvas.nodes[found.parentNode])+', '+all.length+' child node'+(all.length>1?'s':'')+')?', function(btn) {
                if(btn == 'yes') {
                  var changed = [];
                  for(var i = 0; i < all.length; i++)
                    if(all[i].parentNode)
                    {
                      delete all[i].parentNode;
                      changed.push(all[i]);
                    }
                  this.addNode(changed); // there might be a lot of nodes, saveMap makes the most sense
                  this.saveMap(null, {respectNoPost:true});
                }
              }, this);
            }
          }, {
            icon: this.imgDir + 'datatable.png',
            text: 'Show Data of the Selected Nodes',
            scope: this,
            handler: function() {
              this.canvas.updateDataTable(o);
            }
          }, {
            icon: this.imgDir + 'lightning.png',
            text: 'Flash the Selected Nodes',
            scope: this,
            handler: function() {
              var all = [];
              for(var i = 0; i < n.length; i++)
                all.push(n[i].id);
              this.canvas.flashNode(all);
            }
          }, {
            icon: this.imgDir + 'connect.png',
            text: 'Show Nodes Connected to These Nodes',
            handler: this.canvas.showHideSelectedDataPoint.createDelegate(this.canvas, [false, 36])
          });
          this.tagging(n, items, tagn);
          items.push({
            text: 'Align the Selected Nodes',
            icon: this.imgDir + 'text_padding_left.png',
            menu: [
              {
                icon: this.imgDir + 'text_padding_top.png',
                text: 'Align to Top',
                handler: this.alignNodes.createDelegate(this, [n, 84])
              },
              {
                icon: this.imgDir + 'text_padding_bottom.png',
                text: 'Align to Bottom',
                handler: this.alignNodes.createDelegate(this, [n, 66])
              },
              {
                icon: this.imgDir + 'text_padding_left.png',
                text: 'Align to Left',
                handler: this.alignNodes.createDelegate(this, [n, 76])
              },
              {
                icon: this.imgDir + 'text_padding_right.png',
                text: 'Align to Right',
                handler: this.alignNodes.createDelegate(this, [n, 82])
              },
              {
                icon: this.imgDir + 'text_distribute_h.png',
                text: 'Distribute Horizontally',
                handler: this.alignNodes.createDelegate(this, [n, 72])
              },
              {
                icon: this.imgDir + 'text_distribute_v.png',
                text: 'Distribute Vertically',
                handler: this.alignNodes.createDelegate(this, [n, 86])
              }
            ]
          }, '-', {
            icon: this.imgDir + 'delete.png',
            text: 'Script the Selected Nodes (Advanced)',
            scope: this,
            handler: this.scriptNodes.createDelegate(this, [n])
          }, '-', {
            icon: this.imgDir + 'delete.png',
            text: 'Delete the Selected Nodes',
            scope: this,
            handler: this.deleteNode.createDelegate(this, [n])
          }, {
            icon: this.imgDir + 'cancel.png',
            text: 'Hide the Selected Nodes',
            handler: this.toggleNode.createDelegate(this, [n, true])
          }, {
            icon: this.imgDir + 'cancel.png',
            text: 'Hide All Other Nodes',
            handler: this.toggleNode.createDelegate(this, [n, true, true])
          });
          if(o.nodes.length == 2)
          {
            var n1 = o.nodes[0], label1 = this.getName(n1),
                n2 = o.nodes[1], label2 = this.getName(n2);
            items.push({
              icon: this.imgDir + 'add.png',
              text: 'Add Edge from ' + label1 + ' to ' + label2,
              handler: this.editEdge.createDelegate(this, [n1, 'from', n2], true)
            }, {
              icon: this.imgDir + 'add.png',
              text: 'Add Edge from ' + label2 + ' to ' + label1,
              handler: this.editEdge.createDelegate(this, [n1, 'to', n2], true)
            });
          }
          Ext.canvasXpress.utils.addMenu(items, n, this.nodeMenu, this);
        }
        else
        {
          var n = o.nodes[0], label = this.getName(n);
          items.push({
            icon: this.imgDir + 'edit.png',
            text: 'Edit ' + label,
            handler: this.editNode.createDelegate(this, [n], true)
          }, {
              icon: this.imgDir + 'copy.png',
              text: 'Duplicate ' + label,
              handler: this.copyObj.createDelegate(this, ['node', n], true)
          }, {
            icon: this.imgDir + 'delete.png',
            text: 'Delete ' + label,
            scope: this,
            handler: this.deleteNode.createDelegate(this, [n])
          }, {
            icon: this.imgDir + 'cancel.png',
            text: 'Hide ' + label,
            handler: this.toggleNode.createDelegate(this, [n, true])
          }, {
            icon: this.imgDir + 'lightning.png',
            text: 'Flash ' + label,
            handler: this.canvas.flashNode.createDelegate(this.canvas, [n.id])
          }, {
            icon: this.imgDir + 'connect.png',
            text: 'Show Nodes Connected to ' + label,
            scope: this,
            handler: function() {
              this.canvas.selectNode = {};
              this.canvas.isSelectedNodes = 0;
              this.canvas.addRemoveToSelectedDataPoints([this.canvas.data.nodeIndices[n.id]]);
              this.canvas.showHideSelectedDataPoint(false, 36);
            }
          });
          this.tagging([n], items, tagn, label);
          if(this.showAdvancedTest)
          {
            var nitem = [{
              text: 'View/edit JSON for node only',
              handler: this.editJSON.createDelegate(this, [n, 'node'])
            }, {
              text: 'View/edit JSON for data only',
              handler: this.editJSON.createDelegate(this, [n, 'data'])
            }];
            if(this.hasListener('updatenode') && !this.viewInfo)
              nitem.push({
                text: 'Save this node to DB (if available)',
                scope: this,
                handler: function() {
                  var nd = this.nodeDialog? this.nodeDialog(n, this) : null,
                      p = Ext.canvasXpress.utils.clone(n);
                  if(nd && nd.getParams) nd.getParams.call(this, n, p);
                  this.fireEvent('updatenode', n, p, function(){}, true);
                }
              });
            items.push({
              text: 'JSON for ' + label,
              menu: nitem
            });
          }
          if(n.oldEventless)
            items.push({
              icon: this.imgDir + 'arrow_left.png',
              text: 'Revert ' + label + ' To Eventless',
              handler: this.eventlessNode.createDelegate(this, [[n], true])
            });
          if(n.parentNode)
          {
            var pn = this.canvas.nodes[n.parentNode], pl = this.getName(pn);
            items.push({
            text: 'Grouping',
            menu: [
              {
                icon: this.imgDir + 'edit.png',
                text: 'Edit the Group/Parent',
                handler: this.editNode.createDelegate(this, [this.canvas.nodes[n.parentNode]], true)
              },
              {
                icon: this.imgDir + 'shape_ungroup.png',
                text: 'Ungroup ' + label,
                scope: this,
                handler: function() {
                  Ext.Msg.confirm('Confirm the operation', 'This operation cannot be reverted. Are you sure you would like to ungroup this node from the group parent '+pl+'?', function(btn) {
                    if(btn == 'yes') delete n.parentNode;
                  }, this);
                }
              },
              {
                icon: this.imgDir + 'shape_ungroup.png',
                text: 'Ungroup the Whole Group',
                scope: this,
                handler: function() {
                  var p = n.parentNode, nodes = this.canvas.data.nodes, all = [];
                  for(var i = 0; i < nodes.length; i++)
                    if(nodes[i].parentNode == p)
                      all.push(nodes[i]);
                  Ext.Msg.confirm('Confirm the operation', 'This operation cannot be reverted. Are you sure you would like to ungroup the whole group (group parent: '+pl+', '+all.length+' child node'+(all.length>1?'s':'')+')?', function(btn) {
                    if(btn == 'yes') {
                      for(var i = 0; i < all.length; i++)
                        delete all[i].parentNode;
                    }
                  }, this);
                }
              },
              {
                icon: this.imgDir + 'lightning.png',
                text: 'Flash the Group/Parent',
                handler:  this.canvas.flashNode.createDelegate(this.canvas, [n.parentNode])
              },
              {
                icon: this.imgDir + 'lightning.png',
                text: 'Flash the Whole Group',
                scope: this,
                handler: function() {
                  var p = n.parentNode, nodes = this.canvas.data.nodes, all = [];
                  for(var i = 0; i < nodes.length; i++)
                    if(nodes[i].parentNode == p)
                      all.push(nodes[i].id);
                  this.canvas.flashNode(p, 'rgb(0,255,0)', 1);
                  this.canvas.flashNode.defer(800, this.canvas, [all]);
                }
              }
            ]});
          }
          items.push({
            text: 'Order of ' + label,
            menu: [
              {
                icon: this.imgDir + 'bring_front.png',
                text: 'Bring to Front',
                handler: this.changeNodeOrder.createDelegate(this, [n, 'bringNodeToFront'])
              },
              {
                icon: this.imgDir + 'send_back.png',
                text: 'Send to Back',
                handler: this.changeNodeOrder.createDelegate(this, [n, 'sendNodeToBack'])
              },
              {
                icon: this.imgDir + 'bring_forward.png',
                text: 'Bring Forward',
                handler: this.changeNodeOrder.createDelegate(this, [n, 'bringNodeForward'])
              },
              {
                icon: this.imgDir + 'send_backwards.png',
                text: 'Send Backward',
                handler: this.changeNodeOrder.createDelegate(this, [n, 'sendNodeBackward'])
              }
            ]
          },
          '-', {
            icon: this.imgDir + 'add.png',
            text: 'Add Edge to ' + label,
            handler: this.editEdge.createDelegate(this, [n, 'to'], true)
          }, {
            icon: this.imgDir + 'add.png',
            text: 'Add Edge from ' + label,
            handler: this.editEdge.createDelegate(this, [n, 'from'], true)
          });
          // find the edges coming from this
          if(this.canvas.data && this.canvas.data.edges)
          {
            var eMenu = [], eMenu1 = [], en = this.canvas.nodes,
                es = this.canvas.data.edges, id = n.id;
            for(var i = 0; i < es.length; i++)
            {
              var e = es[i];
              if(e.id1 == id || e.id2 == id)
              {
                var n1 = e.id1 == id? en[e.id2] : en[e.id1], label1 = this.getName(n1);
                eMenu.push({
                  icon: this.imgDir + 'edit.png',
                  text: label1,
                  handler: this.editEdge.createDelegate(this, [e], true)
                });
                eMenu1.push({
                  icon: this.imgDir + 'delete.png',
                  text: label1,
                  handler: this.deleteEdge.createDelegate(this, [e])
                });
              }
            }
            if(eMenu.length)
              items.push({
                text: 'Edit Edge'+(eMenu.length>1?'s':'')+ ' for ' + label,
                menu: eMenu
              }, {
                text: 'Delete Edge'+(eMenu.length>1?'s':'')+ ' for ' + label,
                menu: eMenu1
              });
          }
          if(this.canvas.hasChildren(n.id)) // add group menu
          {
            items.push('-', {
              text: (n.hideChildren? 'Expand':'Collapse') + ' Children of ' + label,
              icon: this.imgDir + 'folder_' + (n.hideChildren? 'open':'close') + '.png',
              handler: this.toggleChildren.createDelegate(this, [n], true)
            });
          }
          Ext.canvasXpress.utils.addMenu(items, n, this.nodeMenu, this);
        }
      }
      else if(o && o.edges && o.edges[0])
      {
        var e = o.edges[0], n1 = this.canvas.nodes[e.id1], n2 = this.canvas.nodes[e.id2],
            s = this.getName(n1) + ' - ' + this.getName(n2);
        var f = function(node) {
          var label = this.getName(node), item = {
            icon: this.imgDir + 'edit.png',
            text: 'Edit ' + label,
            handler: this.editNode.createDelegate(this, [node], true)
          }, menu = [item];
          if(node.eventless)
            menu.push({
              icon: this.imgDir + 'arrows_4_way.png',
              text: 'Make Node Draggable',
              handler: this.eventlessNode.createDelegate(this, [[node]])
            });
          else if(node.oldEventless)
            menu.push({
              icon: this.imgDir + 'arrow_left.png',
              text: 'Revert To Eventless',
              handler: this.eventlessNode.createDelegate(this, [[node], true])
            });
          else return item;
          return {
            text: label + (node.eventless? ' *' : ''),
            menu: menu
          }
        }.createDelegate(this);
        items.push({
          icon: this.imgDir + 'edit.png',
          text: 'Edit ' + s,
          handler: this.editEdge.createDelegate(this, [e], true)
        }, {
            icon: this.imgDir + 'copy.png',
            text: 'Duplicate ' + s,
            handler: this.copyObj.createDelegate(this, ['edge', e], true)
        }, {
          icon: this.imgDir + 'delete.png',
          text: 'Delete ' + s,
          handler: this.deleteEdge.createDelegate(this, [e])
        }, f(n1), f(n2));
        if(this.showAdvancedTest)
        {
          var eitem = [{
              text: 'View/edit JSON for edge only',
              handler: this.editJSON.createDelegate(this, [e, 'edge'])
            }, {
              text: 'View/edit JSON for data only',
              handler: this.editJSON.createDelegate(this, [e, 'data'])
            }];
          if(this.hasListener('updateedge') && !this.viewInfo)
            eitem.push({
              text: 'Save this edge to DB (if available)',
              scope: this,
              handler: function() {
                var ed = this.edgeDialog? this.edgeDialog(e, this) : null,
                    p = Ext.canvasXpress.utils.clone(e);
                if(ed && ed.getParams) ed.getParams.call(this, e, p);
                p.linetype = p.type;
                this.fireEvent('updateedge', e, p, function(){}, true);
              }
            });
          items.push({
            text: 'JSON for this edge',
            menu: eitem
          });
        }
        if(n1.eventless && n2.eventless)
          menu.push({
            icon: this.imgDir + 'arrows_4_way.png',
            text: 'Make Both Nodes Draggable',
            handler: this.eventlessNode.createDelegate(this, [[n1, n2]])
          });
        if(n1.oldEventless && n2.oldEventless)
          menu.push({
            icon: this.imgDir + 'arrow_left.png',
            text: 'Revert Both Nodes To Eventless',
            handler: this.eventlessNode.createDelegate(this, [[n1, n2], true])
          });
        Ext.canvasXpress.utils.addMenu(items, e, this.edgeMenu, this);
      }
      else
      {
        var type = (o && o.edgeLegend && o.edgeLegend[0])? 'edge' :
                   (o && o.nodeLegend && o.nodeLegend[0])? 'node' :
                   (o && o.textLegend)? 'text' : null;
        if(type)
        {
          items.push({
            icon: this.imgDir + 'edit.png',
            text: 'Edit This Legend',
            handler: this.editLegend.createDelegate(this, [type, o[type + 'Legend']], true)
          },{
            icon: this.imgDir + 'delete.png',
            text: 'Delete This Legend',
            handler: this.deleteLegend.createDelegate(this, [type, o[type + 'Legend']])
          });
        }
        items.push({
            icon: this.imgDir + 'add.png',
            text: 'Add Node',
            scope: this,
            handler: this.editNode
          }, {
            icon: this.imgDir + 'add.png',
            text: 'Add Edge',
            scope: this,
            handler: this.editEdge
          });
        var al = [], el = [], l = this.canvas.data.legend;
        if(!l || !l.nodes || !l.nodes.length)
          al.push({
            icon: this.imgDir + 'add.png',
            text: 'Node Legend',
            handler: this.editLegend.createDelegate(this, ['node'], true)
          });
        else
          el.push({
            icon: this.imgDir + 'edit.png',
            text: 'Node Legend',
            handler: this.editLegend.createDelegate(this, ['node'], true)
          });
        if(!l || !l.edges || !l.edges.length)
          al.push({
            icon: this.imgDir + 'add.png',
            text: 'Edge Legend',
            handler: this.editLegend.createDelegate(this, ['edge'], true)
          });
        else
          el.push({
            icon: this.imgDir + 'edit.png',
            text: 'Edge Legend',
            handler: this.editLegend.createDelegate(this, ['edge'], true)
          });
        if(!l || !l.text || !l.text.length)
          al.push({
            icon: this.imgDir + 'add.png',
            text: 'Text Legend',
            handler: this.editLegend.createDelegate(this, ['text'], true)
          });
        else
          el.push({
            icon: this.imgDir + 'edit.png',
            text: 'Text Legend',
            handler: this.editLegend.createDelegate(this, ['text'], true)
          });
        if(al.length)
          items.push({
            text: 'Add Legend',
            menu: al
          });
        if(el.length)
          items.push({
            text: 'Edit Legend',
            menu: el
          });
        if(this.clipboard)
        {
          items.push({
            icon: this.imgDir + 'copy.png',
            text: 'Copy All Nodes & Edges',
            handler: func.createDelegate(this, [this.data.nodes])
          });
          var f = function(clean) {
            var s = this.clipboard.obj.bounds, n = this.clipboard.obj.nodes,
                e = this.clipboard.obj.edges, newn = this.canvas.data.nodes,
                newe = this.canvas.data.edges, maxX = this.canvas.maxX || s.maxX,
                maxY = this.canvas.maxY || s.maxY, minX = this.canvas.minX || s.minX,
                minY = this.canvas.minY || s.minY, rx = (s.maxX - s.minX)/(maxX - minX),
                ry = (s.maxY - s.minY)/(maxY - minY), idmap = {}, bounds = {},
                rs = rx > ry ? ry : rx;
            // find paste nodes' boundary
            for(var i = 0; i < n.length; i++)
            {
              if(bounds.minX === undefined || n[i].x < bounds.minX) bounds.minX = n[i].x;
              if(bounds.minY === undefined || n[i].y < bounds.minY) bounds.minY = n[i].y;
              if(bounds.maxX === undefined || n[i].x > bounds.maxX) bounds.maxX = n[i].x;
              if(bounds.maxY === undefined || n[i].y > bounds.maxY) bounds.maxY = n[i].y;
            }
            if(isNaN(xy.x)) xy.x = (bounds.maxX + bounds.minX) / 2;
            if(isNaN(xy.y)) xy.y = (bounds.maxY + bounds.minY) / 2;
            var mx = xy.x - (bounds.maxX - bounds.minX) / (rx*2), my = xy.y - (bounds.maxY - bounds.minY) / (2*ry);
            for(var i = 0; i < n.length; i++)
            {
              // convert coordinates
              n[i].x = (n[i].x - bounds.minX) / rx + mx;
              n[i].labelX = (n[i].labelX - bounds.minX) / rx + mx;
              n[i].y = (n[i].y - bounds.minY) / ry + my;
              n[i].labelY = (n[i].labelY - bounds.minY) / ry + my;
              if(n[i].width) n[i].width /= rx;
              if(n[i].height) n[i].height /= ry;
              if(n[i].size) n[i].size /= rs;
              if(n[i].labelSize) n[i].labelSize /= rs;
              var id = n[i].id;
              delete n[i].id;
              if(this.cleanNode) this.cleanNode(n[i], clean);
              else delete n[i].data;
              this.canvas.addNode(n[i]);
              idmap[id] = n[i].id;
              this.addNode(n[i]);
            }
            for(var i = 0; i < n.length; i++)
              if(n[i].parentNode !== undefined) // set parentNode as needed
                n[i].parentNode = idmap[n[i].parentNode];
            for(var i = 0; i < e.length; i++)
            {
              e[i].id1 = idmap[e[i].id1];
              e[i].id2 = idmap[e[i].id2];
              if(e[i].width) e[i].width /= rs;
              if(this.cleanEdge) this.cleanEdge(e[i], clean);
              else delete e[i].data;
              this.canvas.addEdge(e[i]);
              this.addEdge(e[i]);
            }
            if(this.pasteExtra) this.pasteExtra();
            this.canvas.draw();
            this.canvas.addToNetworkStack();
          }.createDelegate(this);
          if(this.clipboard.type == 'nodes' && this.clipboard.obj && this.clipboard.obj.bounds)
            items.push({
              icon: this.imgDir + 'paste.png',
              text: 'Paste the Copied Node(s and Edges)',
              scope: this,
              handler: function() {
                if((this.hasNote(this.clipboard.obj.nodes) || this.hasNote(this.clipboard.obj.edges)) && this.warnClean) 
                  this.warnClean(f);
                else f(true);
              }
            });
        }
        var hn = Ext.canvasXpress.utils.getHiddenNodes(this);
        if(hn.length > 1)
          items.push({
            text: 'Show Hidden Nodes',
            menu: hn
          });
        if(this.canvas.canNetworkUndoOp())
          items.push({
            text: 'Undo Last Operation',
            icon: this.imgDir + 'arrow_left.png',
            scope: this.canvas,
            handler: this.canvas.undoNetworkOp
          });
        if(this.canvas.canNetworkRedoOp())
          items.push({
            text: 'Redo Last Operation',
            icon: this.imgDir + 'arrow_right.png',
            scope: this.canvas,
            handler: this.canvas.redoNetworkOp
          });
        if(!this.canvas.data.legend) this.canvas.data.legend = {};
        if(!this.canvas.data.legend.nodes) this.canvas.data.legend.nodes = [];
        if(!this.canvas.data.legend.edges) this.canvas.data.legend.edges = [];
        if(!this.canvas.data.legend.text) this.canvas.data.legend.text = [];
        items.push({
          text: 'Make Anchor Nodes ' + (this.canvas.overrideAnchorNodes? 'Hidden':'Visible'),
          scope: this,
          handler: function() {
            // for compatibility reasons, try converting user-hidden nodes to anchor nodes
            var ns = this.canvas.data.nodes;
            for(var i = 0; i < ns.length; i++)
            {
              var n = ns[i];
              if(/rgba\(.*,\s*0\)/i.test(n.color) && /rgba\(.*,\s*0\)/i.test(n.outline) && n.eventless)
              {
                n.width = 10;
                n.height = 10;
                n.shape = 'square';
                n.color = 'rgb(255,255,0)';
                n.outline = 'rgb(255,0,0)';
                n.anchor = true;
                n.eventless = false;
                n.fixed = false;
                this.addNode(n);
              }
            }
            this.canvas.overrideAnchorNodes = !this.canvas.overrideAnchorNodes;
            this.canvas.draw();
          }
        }, {
          text: 'Make Eventless Nodes ' + (this.canvas.overrideEventlessNodes? 'Unmovable':'Movable'),
          scope: this.canvas,
          handler: function() {
            this.overrideEventlessNodes = !this.overrideEventlessNodes;
            this.draw();
          }
        });
        if(tags.length)
        {
          var showtag = function(hide) {
            var ns = [];
            for(var name in tagn)
            {
              var tag = tagn[name], n = tag.nodes;
              tag.show = !hide;
              for(var i in n) ns.push(i);
            }
            this.canvas.hideUnhideNodes(ns, hide);
            this.canvas.draw();
          }, shm = [{
            text: 'Show All',
            handler: showtag.createDelegate(this, [false])
          }, {
            text: 'Hide All',
            handler: showtag.createDelegate(this, [true])
          }], tnm = [], anm = [], fnm = [], tmenu = [];
          for(var i = 0; i < tags.length; i++)
          {
            var j = tags[i];
            shm.push({
              xtype: 'menucheckitem',
              text: j,
              checked: !!tagn[j].show,
              scope: this,
              handler: function(b) {
                var tag = tagn[b.text], n = tag.nodes, ns = [];
                tag.show = !b.checked;
                for(var i in n)
                {
                  var notshown = true;
                  if(b.checked)
                  {
                    for(var j in tagn)
                    {
                      if(j == b.text) continue;
                      if(tagn[j].nodes[i] && tagn[j].show) // check if this node belongs to other tags that are currently showing
                      {
                        notshown = false;
                        break;
                      }
                    }
                  }
                  if(notshown)
                    ns.push(i);
                }
                this.canvas.hideUnhideNodes(ns, b.checked);
                this.canvas.draw();
//                 this.tagChanged = true; // right now we always show all tagged nodes at network first load, so don't set tagChanged to true here for show/hide operations
                b.setChecked(tag.show);
                return false;
              }
            });
            tnm.push({
              xtype: 'menucheckitem',
              text: j,
              scope: this,
              group: 'tnm',
              handler: function(b) {
                if(b.checked) return false;
                var n = tagn[b.text].nodes, ns = [];
                for(var i in n) ns.push(i);
                this.canvas.setSelectNodes(ns);
                this.canvas.draw();
                b.setChecked(true);
                return false;
              }
            });
            anm.push({
              xtype: 'menucheckitem',
              text: j,
              scope: this,
              handler: function(b) {
                if(b.checked) return false;
                var n = tagn[b.text].nodes, ns = [];
                for(var i in n) ns.push(i);
                for(var i in this.canvas.selectNode) ns.push(i);
                this.canvas.setSelectNodes(ns);
                this.canvas.draw();
                b.setChecked(true);
                return false;
              }
            });
            fnm.push({
              xtype: 'menucheckitem',
              text: j,
              scope: this,
              group: 'fnm',
              handler: function(b) {
                var n = tagn[b.text].nodes, ns = [];
                for(var i in n) ns.push(i);
                this.canvas.flashNode(ns);
                b.setChecked(true);
                return false;
              }
            });
          }
          tmenu.push({
              text: 'Flash those tagged',
              menu: fnm
            }, {
              text: 'Select those tagged',
              menu: tnm
            }, {
              text: 'Add to selection those tagged',
              menu: anm
            }, {
            text: 'Show/hide the nodes tagged',
            scope: this,
            menu: shm
          });
          if(this.hasListener('updatetag') && !this.viewInfo && this.tagChanged)
            tmenu.push({
              text: 'Save the tags to DB (if available)',
              scope: this,
              handler: this.updateTag
            });
          items.push({
            text: 'Manage Tags',
            icon: this.imgDir + 'tag_blue.png',
            menu: tmenu
          });
        }
        if(this.showNetworkEditItems)
        {
          items.push('-', {
            text: 'Save as A New Network',
            icon: this.imgDir + 'add.png',
            handler: this.editNetwork.createDelegate(this, [true])
          });
          if(this.networkInfo.id && !this.viewInfo)
            items.push({
              text: 'Edit Network',
              icon: this.imgDir + 'edit.png',
              scope: this,
              handler: this.editNetwork
            });
          if(!this.viewInfo && this.hasUnsavedChanges() && this.hasListener('saveallchanges'))
            items.push({
              text: 'Save the Network Now',
              icon: this.imgDir + 'save.png',
              scope: this,
              handler: this.saveMap
            });
        }
        items.push('-',
          {
            text: 'Simple Search',
            icon: this.imgDir + 'simple_find.png',
            scope: this,
            handler: this.simpleSearch
          },
          {
            icon: this.imgDir + 'advanced_find.png',
            text: 'Search for',
            menu: [
              {
                text: 'Nodes',
                scope: this,
                handler: this.showSearchNodes
              },
              {
                text: 'Edges',
                scope: this,
                handler: this.showSearchEdges
              },
              {
                text: 'Mixed',
                scope: this,
                handler: this.showSearchAll
              }
            ]
          }, '-'
        );
        items.push({
          icon: this.showToolbar? this.imgDir + 'power_off.png' : this.imgDir + 'power_on.png',
          text: this.showToolbar? 'Hide Toolbar':'Show Toolbar',
          scope: this,
          handler: function() {
            var t = this.getTopToolbar();
            if(this.showToolbar) t.hide();
            else t.show();
            this.showToolbar = !this.showToolbar;
          }
        });
        if(!this.showToolbar)
          items.push(
            {
              icon: this.snapshotCtrl && !this.snapshotCtrl.hidden? this.imgDir + 'power_off.png' : this.imgDir + 'power_on.png',
              text: this.snapshotCtrl && !this.snapshotCtrl.hidden? 'Hide Snapshot Control':'Show Snapshot Control',
              scope: this,
              handler: this.toggleSnapshotCtrl
            });
        if(this.showAdvancedTest)
        {
          var ti = [
            {
              text: 'Import JSON Data',
              handler: this.exchangeData.createDelegate(this, ['Import'])
            }, {
              text: 'Export JSON Data',
              handler: this.exchangeData.createDelegate(this, ['Export'])
            }, {
              text: 'Export JSON for CXP Test',
              handler: this.exchangeData.createDelegate(this, ['Export', true])
            }, {
              text: 'Import JSON Movie',
              handler: this.exchangeData.createDelegate(this, ['ImportMovie'])
            }, {
              text: 'Export JSON Movie',
              handler: this.exchangeData.createDelegate(this, ['ExportMovie'])
            }
          ];
          if(this.hasListener('saveallchanges'))
            ti.push({
              text: 'Force Save the Entire Network',
              icon: this.imgDir + 'save.png',
              scope: this,
              handler: this.saveMap.createDelegate(this, [1], false)
            });
          var eitem = [{
            text: 'View/edit all legends',
            handler: this.editJSON.createDelegate(this, [this.canvas.data.legend, 'legend'])
          }, {
            text: 'View/edit node legend',
            handler: this.editJSON.createDelegate(this, [this.canvas.data.legend.nodes, 'nodelegend'])
          }, {
            text: 'View/edit edge legend',
            handler: this.editJSON.createDelegate(this, [this.canvas.data.legend.nodes, 'edgelegend'])
          }, {
            text: 'View/edit edge legend',
            handler: this.editJSON.createDelegate(this, [this.canvas.data.legend.text, 'textlegend'])
          }, {
            text: 'View/edit network config',
            handler: this.editJSON.createDelegate(this, [this.canvas.getUserConfig(), 'config'])
          }];
          if(this.hasListener('updatelegend') && !this.viewInfo)
            eitem.push({
              text: 'Save the legends to DB (if available)',
              scope: this,
              handler: function() {
                this.fireEvent('updatelegend', function(){}, true);
              }
            });
          items.push({
            text: 'Advanced Test',
            menu: ti
          }, {
          text: 'JSON for legend/config',
          menu: eitem
        });
        }
        Ext.canvasXpress.utils.addMenu(items, null, this.nullMenu, this);
      }
    }
    if(items.length)
    {
      for(var i = 0; i < items.length; i++)
        it.push(items[i]);
      it.push('-');
//       it.push('-', {
//           icon: this.imgDir + 'information.png',
//           text: 'X:' + xy.x + ', Y:' + xy.y
//         }, '-');
    }
  },
  tagging: function(nodes, items, tagn, label) {
    var belong = [], notb = [], belongm = [], notbm = [];
    if(tagn)
    {
      var bseen = {}, nseen = {};
      for(var i in tagn)
      {
        for(var j = 0; j < nodes.length; j++)
        {
          var n = nodes[j];
          if(bseen[i] && nseen[i]) break;
          if(!bseen[i] && tagn[i].nodes[n.id])
          {
            belong.push(i);
            bseen[i] = 1;
          }
          else if(!nseen[i] && !tagn[i].nodes[n.id])
          {
            notb.push(i);
            nseen[i] = 1;
          }
        }
      }
      belong.sort(Ext.canvasXpress.utils.ciSort);
      notb.sort(Ext.canvasXpress.utils.ciSort);
    }
    var setTag = function(txt) {
      for(var j = 0; j < nodes.length; j++)
      {
        var n = nodes[j];
        tagn[txt].nodes[n.id] = 1;
      }
      this.tagChanged = true;
    }.createDelegate(this), tmenu = [
      {
        text: 'Add to a new tag',
        scope: this,
        handler: function() {
          Ext.Msg.prompt('Create Tag', 'Please enter tag name:', function(btn, txt) {
            if(btn == 'ok')
            {
              if(!tagn)
              {
                this.canvas.data.taggedNodes = {};
                tagn = this.canvas.data.taggedNodes;
              }
              if(tagn[txt])
              {
                Ext.Msg.confirm('Tag exists', 'This tag "' + txt + '" already exists. Do you want to add the node to the existing tag?', function(btn) {
                  if(btn == 'yes') setTag(txt);
                });
                return;
              }
              else
              {
                tagn[txt] = {show:true, nodes:{}};
                setTag(txt);
              }
            }
          }, this);
        }
      }
    ];
    if(notb.length)
    {
      for(var i = 0; i < notb.length; i++)
        notbm.push({
          text: notb[i],
          scope: this,
          handler: function(b) {
            setTag(b.text);
          }
        });
      tmenu.push({
        text: 'Add to the tag',
        scope: this,
        menu: notbm
      });
    }
    if(belong.length)
    {
      for(var i = 0; i < belong.length; i++)
        belongm.push({
          text: belong[i],
          scope: this,
          handler: function(b) {
            for(var j = 0; j < nodes.length; j++)
            {
              var n = nodes[j];
              delete tagn[b.text].nodes[n.id];
            }
            var found = false;
            for(var i in tagn[b.text].nodes)
            {
              found = true;
              break;
            }
            if(!found) delete tagn[b.text];
            this.tagChanged = true;
          }
        });
      tmenu.push({
        text: 'Remove from the tag',
        scope: this,
        menu: belongm
      });
    }
    items.push({
      icon: this.imgDir + 'tag_blue.png',
      text: 'Tagging ' + (nodes.length > 1? 'the Selected Nodes' : label),
      scope: this,
      menu: tmenu
    });
  },
  editJSON: function(obj, type) {
    var data;
    if(type == 'data')
    {
      data = this.encode(obj.data || '');
    }
    else
    {
      var tmp = obj.data;
      delete obj.data;
      data = this.encode(obj);
      obj.data = tmp;
    }
    var win = new Ext.Window({
      title: 'JSON Data',
      width: 500,
      height: 300,
      layout: 'fit',
      items: {
        xtype: 'form',
        border: false,
        width: 480,
        style:'margin:5px',
        items: [
          {
            xtype: 'textarea', hideLabel: true, width: 473, height: 230,
            value: data
          }
        ]
      },
      bbar: {
        items: [
          '->',
          {
            text: 'Apply!',
            scope: this,
            handler: function(b, e) {
              var json = b.ownerCt.ownerCt.get(0).get(0).getValue();
              try
              {
                var tmp = Ext.decode(json);
                switch(type) {
                  case 'data':
                    obj.data = tmp;
                    break;
                  case 'config':
                    this.canvas.updateConfig(tmp);
                    break;
                  default:
                    var td = obj.data;
                    for(var i in obj)
                      obj[i] = undefined;
                    for(var i in tmp)
                      obj[i] = tmp[i];
                    if(td != undefined)
                      obj.data = td;
                }
                this.canvas.draw();
              }
              catch(e)
              {
                Ext.Msg.alert('Error', e);
              }
            }
          }
        ]
      }
    });
    win.show();
  },
  getName: function(n) {
    return n? n.label || n.name || n.tooltip || n.id || '' : '';
  },
  alignNodes: function(nodes, o) {
    this.canvas.alignDistributeSelectedNodes(false, o);
    this.addNode(nodes);
  },
  scriptNodes: function(nodes) {
    var win = new Ext.Window({
      title: 'Modify selected nodes with Javascript',
      width: 300,
      height: 200,
      layout: 'fit',
      items: [
        {
          border: false,
          html: 'Enter Javascript script below in the form of a loop that work on an array name "nodes", like "for(var i = 0; i < nodes.length; i++) { /* modify nodes[i] */ }"'
        },
        {
          xtype: 'textarea',
          width: 280,
          labelWidth: 80,
          emptyText: 'for(var i = 0; i < nodes.length; i++)\n{\n  var n = nodes[i];\n  n.label = "test label";\n  n.shape = "oval";\n  ...\n}'
        }
      ],
      bbar: {
        items: [
          '->',
          {
            text: 'Run Script',
            scope: this,
            handler: function(b, e) {
              var val = b.ownerCt.ownerCt.get(0).get(1).getValue();
              try {
                eval('function tmpJS4NodeMod(nodes) {' + val + '}');
                tmpJS4NodeMod(nodes);
                for(var i = 0; i < nodes.length; i++)
                  this.addNode(nodes[i]);
                this.canvas.draw();
                this.canvas.addToNetworkStack();
              }
              catch(e) {
                alert('Problem running your script - ' + e + '\nPlease correct it before trying again');
                return;
              }
            }
          }
        ]
      }
    });
    win.show();
  },
  eventlessNode: function(nodes, toEventless) {
    if(nodes && nodes.length)
    {
      var process = function(node, o) {
        node.width = o.width;
        node.height = o.height;
        node.size = o.size;
        node.color = o.color;
        node.shape = o.shape;
      }
      for(var i = 0; i < nodes.length; i++)
      {
        var node = nodes[i];
        if(toEventless)
        {
          process(node, node.oldEventless);
          node.eventless = true;
          delete node.oldEventless;
        }
        else
        {
          node.oldEventless = {};
          process(node.oldEventless, node);
          process(node, {
            size:1, width:10, height:10, shape: 'square', color: 'rgb(255,0,0)'
          });
          node.eventless = false;
        }
      }
      this.canvas.draw();
    }
  },
  hasUnsavedChanges: function() {
    return this.changedNodes.length || this.changedEdges.length || this.removedNodes.length ||
           this.removedEdges.length || this.legendChanged || this.orderChanged || this.networkChanged || this.tagChanged;
  },
  createContextMenu: function (e) {
    var o = this.canvas.getEventAreaData(e.browserEvent), items = [];
    this.clickXY = e.xy;
    if(this.menuTitle)
    {
      items.push({
        text: this.menuTitle,
        style:'font-weight:bold;margin:0px 4px 0px 27px;line-height:18px'
      });
      items.push('-');
    }
    switch(this.canvas.graphType) {
      case 'Network': this.networkMenu(o, 'cs', items);
    }
    this.addItemToMenu(items, 'General', false, this.generalMenus());
    this.addItemToMenu(items, 'Axes', false, this.axesMenus());
    this.addItemToMenu(items, 'Labels', false, this.labelMenus());
    this.addItemToMenu(items, 'Legend', false, this.legendMenus());
    this.addItemToMenu(items, 'Indicators', false, this.indicatorMenus());
    this.addItemToMenu(items, 'Decorations', false, this.decorationMenus());
    this.addItemToMenu(items, 'Data', false, this.dataMenus());
    items.push({
      icon: this.imgDir + 'refresh.png',
      text: 'Reset ' + this.canvas.graphType,
      scope: this.canvas,
      handler: this.canvas.redraw
    });
    if(this.showPrint)
    {
      items.push('-');
      items.push({text: 'Print',
        icon: this.imgDir + 'print.png',
  		canvasId: this.canvasId,
  		handler: this.onPrintGraph});
    }
    return items;
  },
  // Utilities
  // Add to Menu
  addItemToMenu: function (container, text, icon, items){
    if (!icon) {
      icon = '';
    }
    if (items && items.length > 0) {
      container.push({
	text: text,
	iconCls: icon,
	menu: items
      });
    }
  },
  // Form
  addFormParameter: function (p, isText) {
    var f;
    if (isText) {
      f = new Ext.form.TextField({
	width: 80,
	iconCls: 'no-icon',
	canvasId: this.canvasId,
	name: p,
	value: this.canvas[p] ? this.canvas[p] : '',
	enableKeyEvents: true
      });
    } else {
      f = new Ext.form.NumberField({
	width: 80,
	iconCls: 'no-icon',
	canvasId: this.canvasId,
	name: p,
	value: this.canvas[p] ? this.canvas[p] : '',
	enableKeyEvents: true
      });
    }
    f.on('change', this.addFormEvent, this);
    f.on('specialkey', this.enterFormEvent, this);
    return [f];
  },
  enterFormEvent: function(f, e) {
    if (e.getKey() == e.ENTER) {
      this.addFormEvent(f, f.getValue());
      this.contextMenu.hide();
    }
  },
  addFormEvent: function(f, n, o) {
    var p = f.name;
    var w = p == 'width' ? n : false;
    var h = p == 'height' ? n : false;
    if (f.canvasId) {
      var t = Ext.getCmp(Ext.get(f.canvasId).parent('div.x-panel').id);
      if (t.canvas && t.canvas[p] != n) {
	t.canvas[p] = n;
	t.canvas.draw(w, h);
      }
    }
  },
  // Color Menu
  colorMenu: function (par) {
    var handler = this.buildColorHandler(par);
    return new Ext.menu.ColorMenu({
      width: 155,
      canvasId: this.canvasId,
      handler: handler
    });
  },
  buildColorHandler: function(par) {
    return function (cm, color) {
      var t = Ext.getCmp(Ext.get(cm.canvasId).parent('div.x-panel').id);
      if (t.canvas) {
        t.canvas[par] = t.hexToRgb(color);
        t.canvas.draw();
      }
    }
  },
  hexToRgb: function (color) {
    var r = parseInt(color.substring(0,2),16);
    var g = parseInt(color.substring(2,4),16);
    var b = parseInt(color.substring(4,6),16);
    return 'rgb('+ r + ',' + g + ',' + b + ')';
  },
  // Radio Group
  buildGroupMenu: function(par, vals, callback) {
    var m = [];
    for (var i = 0; i < vals.length; i++) {
      m.push({text: vals[i] ? vals[i] : 'false',
	      group: par,
	      checked: this.canvas[par] == vals[i] ? true : false,
	      canvasId: this.canvasId,
	      checkHandler: callback ? callback : this.clickedGroupMenu
	     });
    }
    return m;
  },
  clickedGroupMenu: function(item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      var p = item.text == 'false' ? false : item.text;
      if (t.canvas && t.canvas[item.group] != p) {
	t.canvas[item.group] = p;
	t.canvas.draw();
      }
    }
  },
  // Print
  onPrintGraph: function(item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      if (t.canvas) {
	t.canvas.print();
      }
    }
  },
  // Indicator
  indicatorMenus: function () {
    if (this.canvas.hasIndicator()) {
      var items = [{
	text: 'Show',
	iconCls: '',
	menu: this.buildGroupMenu('showIndicators', [true, false])
      }];
      if (this.canvas.showIndicators) {
	items.push({
	  text: 'Position',
	  iconCls: '',
	  menu: this.buildGroupMenu('indicatorsPosition', ['bottom', 'right'])
	});
      }
      return items;
    }
  },
  // Decoration
  decorationMenus: function () {
    if ((this.canvas.graphType == 'Scatter2D' || this.canvas.graphType == 'ScatterBubble2D') && this.canvas.hasDecorations()) {
      var items = [{
	text: 'Show',
	iconCls: '',
	menu: this.buildGroupMenu('showDecorations', [true, false])
      }];
      if (this.canvas.showDecorations) {
	items.push({
	  text: 'Position',
	  iconCls: '',
	  menu: this.buildGroupMenu('decorationsPosition', ['bottom', 'right'])
	});
	items.push({
	  text: 'Size',
	  iconCls: '',
	  menu: this.buildGroupMenu('decorationFontSize', [8, 9, 10, 11, 12, 13, 14, 15, 16])
	});
	items.push({
	  text: 'Color',
	  iconCls: '',
	  menu: this.colorMenu('decorationsColor')
	});
	items.push({
	  text: 'Scale Factor',
	  iconCls: '',
	  menu: this.buildGroupMenu('decorationScaleFontFactor', [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2])
	});
      }
      return items;
    } else {
      return false;
    }
  },
  // Legend
  legendMenus: function () {
    if (this.canvas.hasLegend()) {
      var items = [{
	text: 'Show',
	iconCls: '',
	menu: this.buildGroupMenu('showLegend', [true, false])
      }];
      if (this.canvas.hasLegendProperties() && this.canvas.showLegend) {
	items.push({
	  text: 'Position',
	  iconCls: '',
	  menu: this.buildGroupMenu('legendPosition', ['bottom', 'right'])
	});
	items.push({
	  text: 'Size',
	  iconCls: '',
	  menu: this.buildGroupMenu('legendFontSize', [8, 9, 10, 11, 12, 13, 14, 15, 16])
	});
	items.push({
	  text: 'Color',
	  iconCls: '',
	  menu: this.colorMenu('legendColor')
	});
	items.push({
	  text: 'Scale Factor',
	  iconCls: '',
	  menu: this.buildGroupMenu('legendScaleFontFactor', [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2])
	});
	items.push({
	  text: 'Boxed',
	  iconCls: '',
	  menu: this.buildGroupMenu('legendBox', [true, false])
	});
      }
      return items;
    } else {
      return false;
    }
  },
  // Data
  dataMenus: function () {
    if (this.canvas.hasData()) {
      var items = [];
      items.push({
	text: 'Transpose',
	iconCls: '',
        canvasId: this.canvasId,
	handler: this.onTranspose			
      });
      if (this.canvas.hasDataSamples()) {
	this.addItemToMenu(items, 'Samples', false, this.dataSamples());
      }
      if (this.canvas.hasDataVariables()) {
	this.addItemToMenu(items, 'Variables', false, this.dataVariables());
      }
      if (this.canvas.hasDataGroups() && this.canvas.isGroupedData) {
 	this.addItemToMenu(items, 'Groups', false, this.dataGroups());
      }
      this.addItemToMenu(items, 'Series', false, this.dataSeries());
      this.addItemToMenu(items, 'Clustering', false, this.cluster());
      if (this.canvas.hasDataProperties()) {
	this.addItemToMenu(items, 'Range', false, this.dataRange());
	this.addItemToMenu(items, 'Transformation', false, this.transformations());
	items.push({
	  text: 'Error Bars',
	  iconCls: '',
	  menu: this.buildGroupMenu('showErrorBars', [true, false])
	})
      }
      return items;
    }
  },
  cluster: function () {
    if (this.canvas.hasDendrograms) {
      var items = [{
	text: 'Samples',
	iconCls: '',
        menu: this.clusterSamples()
      }];
      if (this.canvas.graphType == 'Heatmap') {
        items.push({
	  text: 'Variables',
	  iconCls: '',
          menu: this.clusterVariables()
	});
      }
      items.push({
	text: 'Center Data',
	iconCls: '',
	canvasId: this.canvasId,
	menu: this.buildGroupMenu('autoScaleFont', [true, false])
      });
      items.push({
	text: 'Distance',
	iconCls: '',
	canvasId: this.canvasId,
	menu: this.buildGroupMenu('distance', ['euclidian', 'manhatan', 'max'])
      });
      items.push({
	text: 'Linkage',
	iconCls: '',
	canvasId: this.canvasId,
	menu: this.buildGroupMenu('linkage', ['single', 'complete', 'average'])
      });
      items.push({
	text: 'Impute Method',
	iconCls: '',
	canvasId: this.canvasId,
	menu: this.buildGroupMenu('imputeMethod', ['mean', 'median'])
      });
      items.push({
	text: 'K-Means Cluster',
	iconCls: '',
	canvasId: this.canvasId,
	menu: this.addFormParameter('kmeansClusters')
      });
      items.push({
	text: 'K-Means Max Iterations',
	iconCls: '',
	canvasId: this.canvasId,
	menu: this.addFormParameter('maxIterations')
      });
      return items;
    }
  },
  clusterSamples: function () {
    var items = [{
      text: 'Hierarchical',
      iconCls: '',
      canvasId: this.canvasId,
      handler: this.onClusterSamplesH
    }, {
      text: 'K-Means',
      iconCls: '',
      canvasId: this.canvasId,
      handler: this.onClusterSamplesKM
    }];
    return items;
  },
  clusterVariables: function () {
    var items = [{
      text: 'Hierarchical',
      iconCls: '',
      canvasId: this.canvasId,
      handler: this.onClusterVariablesH
    }, {
      text: 'K-Means',
      iconCls: '',
      canvasId: this.canvasId,
      handler: this.onClusterVariablesKM
    }];
    return items;
  },
  onClusterSamplesH: function (item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      if (t.canvas) {
        t.canvas.clusterSamples();
	t.canvas.draw();
      }
    }	
  },
  onClusterVariablesH: function (item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      if (t.canvas) {
        t.canvas.clusterVariables();
	t.canvas.draw();
      }
    }	
  },
  onClusterSamplesKM: function (item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      if (t.canvas) {
        t.canvas.kmeansSamples();
	t.canvas.draw();
      }
    }	
  },
  onClusterVariablesKM: function (item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      if (t.canvas) {
        t.canvas.kmeansVariables();
	t.canvas.draw();
      }
    }
  },
  transformations: function () {
    var s = [];
    var smps = this.canvas.getSamples();
    for (var i = 0; i < smps.length; i++) {
      var check = smps[i].index == this.canvas.ratioReference ? true : false;
      s.push({
	group: 'ratioReference',
	text: smps[i].name,
	index: smps[i].index,
	checked: check,
	canvasId: this.canvasId,
	checkHandler: this.onDataTransform
      });
    }
    return [{
      text: 'Type',
      iconCls: '',
      menu: this.buildGroupMenu('transformType', ['log2', 'log10', 'exp2', 'exp10', 'percentile', 'zscore', 'ratio', false, 'reset', 'save'], this.onDataTransform)
    }, {
      text: 'Ratio Reference',
      iconCls: '',
      menu: s
    }, {
      text: 'Z-Score Axis',
      iconCls: '',
      menu: this.buildGroupMenu('zscoreAxis', ['samples', 'variable'], this.onDataTransform)
    }];
  },
  onTranspose: function(item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      if (t.canvas) {
        t.canvas.transpose();
	t.canvas.draw();
      }
    }	
  },
  onDataTransform: function (item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      var p = item.group == 'ratioReference' ? item.index : item.text == 'false' ? false : item.text;
      if (t.canvas && t.canvas[item.group] != p) {
	t.canvas[item.group] = p;
	var tr = t.canvas.transformType;
	var ax = t.canvas.zscoreAxis;
	var id = t.canvas.ratioReference;
	if (tr) {
	  t.canvas.transform(tr, ax, id);
	}
	t.canvas.draw();
      }
    }
  },
  dataSamples: function () {
    var items = [];
    var s = [];
    var smps = this.canvas.getSamples();
    for (var i = 0; i < smps.length; i++) {
      var check = smps[i].hidden ? false : true;
      s.push({
	text: smps[i].name,
	checked: check,
	canvasId: this.canvasId,
	handler: this.onSamples
      });
    }
    var g = [];
    var check;
    var annt = this.canvas.getAnnotations();
    var grp = this.canvas.getGroupingFactors();
    for (var i = 0; i < annt.length; i++) {
      check = grp.hasOwnProperty(annt[i]) ? true : false;
      g.push({
	text: annt[i],
	checked: check,
	canvasId: this.canvasId,
	handler: this.onGroup
      });
    }
    items.push({
      text: 'Show',
      iconCls: '',
      menu: s
    });
    if (annt && this.canvas.isSegregable()) {
      var c = [];
      annt.push(false);
      for (var i = 0; i < annt.length; i++) {
	c.push({
	  group: 'segregateSamplesBy',
	  text: annt[i] == false ? 'false' : annt[i],
	  category: true,
	  checked: this.canvas.segregateSamplesBy == annt[i] ? true : false,
	  canvasId: this.canvasId,
	  handler: this.onSegregateSamples
	});
      }
      items.push({
	text: 'Segregate',
	iconCls: '',
	menu: c
      })
      annt.pop();
    }
    items.push({
      text: 'Group',
      iconCls: '',
      menu: g
    });
    items.push({
      text: 'Sort',
      iconCls: '',
      menu: this.dataSamplesSort()
    });
    return items;
  },
  onSamples: function (item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      t.canvas.hideUnhideSmps(item.text);
      t.canvas.draw();
    }
  },
  onGroup: function (item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      t.canvas.modifyGroupingFactors(item.text, item.checked);
      t.canvas.groupSamples(t.canvas.getGroupingFactors(true));
      t.canvas.draw();
    }
  },
  onSegregateSamples: function(item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      if (item.text == 'false') {
	t.canvas.desegregateSamples();
      } else {
	t.canvas.segregateSamples(item.text);
      }
      t.canvas.draw();
    }
  },
  dataSamplesSort: function () {
    var v = [];
    var vars = this.canvas.getVariables();
    for (var i = 0; i < vars.length; i++) {
      v.push({
	text: vars[i].name,
	isVar: true,
	index: i + 1,
	checked: this.canvas.varSort == i ? true : false,
	canvasId: this.canvasId,
	handler: this.onSamplesSort
      });
    }
    var c = [];
    var annt = this.canvas.getAnnotations();
    for (var i = 0; i < annt.length; i++) {
      c.push({
	group: 'catSort',
	text: annt[i],
	category: true,
	checked: this.canvas.smpSort == annt[i] ? true : false,
	canvasId: this.canvasId,
	handler: this.onSamplesSort
      });
    }
    return [{
      text: 'By Value of Variable',
      iconCls: '',
      menu: v
    }, {
      text: 'By Annotation',
      iconCls: '',
      menu: c
    }, {
      text: 'By Name',
      iconCls: '',
      canvasId: this.canvasId,
      handler: this.onSamplesSort
    }, {
      text: 'Direction',
      iconCls: '',
      menu: this.buildGroupMenu('sortDir', ['ascending', 'descending'])
    }];
  },
  onSamplesSort: function (item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    var p = item.text == 'false' ? false : item.text;
    if (t.canvas && t.canvas[item.group] != p) {
      if (item.group == 'sortDir') {
	t.canvas.sortDir = item.text;
      }
      if (item.text == 'By Name') {
	t.canvas.sortIndices('smps', t.canvas.sortDir);
      } else {
	if (item.category) {
	  t.canvas.sortIndices('smps', t.canvas.sortDir, item.text);
	} else if (item.isVar) {
	  t.canvas.sortIndices('smps', t.canvas.sortDir, false, false, item.index);
	} else {
	  t.canvas.sortIndices('smps', t.canvas.sortDir);
	}
      }
      t.canvas.draw();
    }
  },
  dataGroups: function () {
    var g = [];
    var grps = this.canvas.getSamples();
    for (var i = 0; i < grps.length; i++) {
      var check = grps[i].hidden ? false : true;
      g.push({
	text: grps[i].name,
	checked: check,
	isVar: false,
	canvasId: this.canvasId,
	handler: this.onSamples
      });
    }
    return [{
      text: 'Show',
      iconCls: '',
      menu: g
    }];
  },
  dataVariables: function () {
    var items = [];
    var v = [];
    var vars = this.canvas.getVariables();
    var annt = this.canvas.getAnnotations(true);
    for (var i = 0; i < vars.length; i++) {
      var check = vars[i].hidden ? false : true;
      v.push({
	text: vars[i].name,
	checked: check,
	canvasId: this.canvasId,
	handler: this.onVariables
      });
    }
    items.push({
      text: 'Show',
      iconCls: '',
      menu: v
    })
    if (annt && this.canvas.isSegregable()) {
      var c = [];
      annt.push(false);
      for (var i = 0; i < annt.length; i++) {
	c.push({
	  group: 'segregateVariablesBy',
	  text: annt[i] == false ? 'false' : annt[i],
	  category: true,
	  checked: this.canvas.segregateVariablesBy == annt[i] ? true : false,
	  canvasId: this.canvasId,
	  handler: this.onSegregateVariables
	});
      }
      items.push({
	text: 'Segregate',
	iconCls: '',
	menu: c
      })
      annt.pop();
    }
    items.push({
      text: 'Sort',
      iconCls: '',
      menu: this.dataVariablesSort(annt)
    })
    return items;
  },
  onVariables: function (item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      t.canvas.hideUnhideVars(item.text);
      t.canvas.draw();
    }
  },
  onSegregateVariables: function(item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      if (item.text == 'false') {
	t.canvas.desegregateVariables();
      } else {
	t.canvas.segregateVariables(item.text);
      }
      t.canvas.draw();
    }
  },
  dataVariablesSort: function (annt) {
    var items = [];
    var s = [];
    var smps = this.canvas.getSamples();
    for (var i = 0; i < smps.length; i++) {
      s.push({
	group: 'sampleSort',
	text: smps[i].name,
	index: i + 1,
	isSmp: true,
	category: false,
	checked: this.canvas.smpSort == i ? true : false,
	canvasId: this.canvasId,
	handler: this.onVariablesSort
      });
    }
    var c = [];
    if (annt) {
      for (var i = 0; i < annt.length; i++) {
	c.push({
	  group: 'catSort',
	  text: annt[i],
	  category: true,
	  checked: this.canvas.varSort == annt[i] ? true : false,
	  canvasId: this.canvasId,
	  handler: this.onVariablesSort
	});
      }
    }
    items.push({
      text: 'By Value in Samples',
      iconCls: '',
      menu: s
    })
    if (c.length > 0) {
      items.push({
	text: 'By Annotation',
	iconCls: '',
	menu: c
      })
    }
    items.push({
      text: 'By Name',
      iconCls: '',
      canvasId: this.canvasId,
      handler: this.onVariablesSort
    })
    items.push({
      text: 'Direction',
      iconCls: '',
      menu: this.buildGroupMenu('sortDir', ['ascending', 'descending'])
    });
    return items;
  },
  onVariablesSort: function (item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    var p = item.text == 'false' ? false : item.text;
    if (t.canvas && t.canvas[item.group] != p) {
      if (item.group == 'sortDir') {
	t.canvas.sortDir = item.text;
      }
      if (item.text == 'By Name') {
	t.canvas.sortIndices('vars', t.canvas.sortDir);
      } else {
	if (item.category) {
	  t.canvas.sortIndices('vars', t.canvas.sortDir, item.text);
	} else if (item.isSmp) {
	  t.canvas.sortIndices('vars', t.canvas.sortDir, false, item.index);
	} else {
	  t.canvas.sortIndices('vars', t.canvas.sortDir);
	}
      }
      t.canvas.draw();
    }
  },
  dataRange: function () {
    var m = [];
    if (!this.canvas.getValidAxes) {
      this.canvas.initAxes(false, true);
    }
    var axes = this.canvas.getValidAxes();
    for (var i = 0; i < axes.length; i++) {
      var l = axes[i] == 'xAxis2' ? 'X2' : axes[i].substring(0,1).toUpperCase();
      var p = this.canvas.graphType.match(/Scatter/) ? l : axes[i] == 'xAxis2' ? '2' : '';
      m.push({text: axes[i].replace('Axis', '-Axis'),
	      iconCls: '',
	      menu: [{text: 'Min',
		      menu: this.addFormParameter('setMin' + p)},
		     {text: 'Max',
		      menu: this.addFormParameter('setMin' + p)}]});
    }
    return m;
  },
  dataSeries: function () {
    var m = [];
    if (this.canvas.graphType == 'BarLine' || this.canvas.graphType.match(/Scatter/) || this.canvas.graphType == 'Pie') {
      if (!this.canvas.getValidAxes) {
	this.canvas.initAxes(false, true);
      }
      var axes = this.canvas.getValidAxes(true);
      var objs = this.canvas.graphType == 'BarLine' ? this.canvas.getVariables() : this.canvas.getSamples();
      for (var i = 0; i < axes.length; i++) {
	var oba = this.canvas.graphType == 'BarLine' ? this.canvas.getVariablesByAxis(axes[i]) : this.canvas.getSamplesByAxis(axes[i]);
	var o = [];
	for (var j = 0; j < objs.length; j++) {
	  var check = false;
	  for (var k = 0; k < oba.length; k++) {
	    if (objs[j].name == oba[k]) {
	      check = true;
	      break;
	    }
	  }
	  if (this.canvas.graphType == 'Scatter2D' || this.canvas.graphType == 'ScatterBubble2D') {
	    o.push({text: objs[j].name,
		    checked: check,
		    canvasId: this.canvasId,
		    isVar: false,
		    handler: this.onDataSeries});
	  } else if (this.canvas.graphType == 'Scatter3D' || this.canvas.graphType == 'Pie') {
	    o.push({text: objs[j].name,
		    group: 'axis',
		    checked: check,
		    canvasId: this.canvasId,
		    isVar: false,
		    checkHandler: this.onDataSeries});
	  } else if (this.canvas.graphType == 'BarLine') {
	    o.push({text: objs[j].name,
		    checked: check,
		    canvasId: this.canvasId,
		    isVar: true,
		    handler: this.onDataSeries});
	  }
	}
	var str = axes[i].replace('Axis', '-Axis');
	if (o.length > 0) {
	  m.push({text: str,
		  iconCls: '',
		  menu: o});
	}
      }
    }
    return m;
  },
  onDataSeries: function (item) {
    var axis = item.getBubbleTarget().ownerCt.text;
    var check = item.checked;
    var isVar = item.isVar;
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      axis = axis.replace('-Axis', 'Axis');
      var res;
      if (isVar) {
	res = t.canvas.addRemoveVariablesInAxis(item.text, axis, check);
      } else {
	res = t.canvas.addRemoveSamplesInAxis(item.text, axis, check);
      }
      if (res) {
	Ext.MessageBox.alert('Status', res);
      } else {
	t.canvas.draw();
      }
    }
  },
  // Labels
  labelMenus: function () {
    var items = [];
    var lab = this.canvas.isGroupedData ? 'Groups' : 'Samples';
    if (this.canvas.hasDataSamples()) {
      this.addItemToMenu(items, lab, false, this.labelSamples());
    }
    if (this.canvas.hasDataVariables()) {
      this.addItemToMenu(items, 'Variables', false, this.labelVariables());
    }
    return items;
  },
  labelSamples: function () {
    var h = [];
    var smps = this.canvas.getSamples();
    var hls = this.canvas.getHighlights();
    for (var i = 0; i < smps.length; i++) {
      var check = hls.hasOwnProperty(smps[i].name) ? true : false;
      h.push({
	text: smps[i].name,
	checked: check,
	isVar: false,
	canvasId: this.canvasId,
	handler: this.onHighlight
      });
    }
    return [{
      text: 'Size',
      iconCls: '',
      canvasId: this.canvasId,
      menu: this.buildGroupMenu('smpLabelFontSize', [8, 9, 10, 11, 12, 13, 14, 15, 16])
    }, {
      text: 'Scale Factor',
      iconCls: '',
      menu: this.buildGroupMenu('smpLabelScaleFontFactor', [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2])			
		}, {
      text: 'Max String Length',
      iconCls: '',
      canvasId: this.canvasId,
      menu: this.addFormParameter('maxSmpStringLen')
    }, {
      text: 'Color',
      iconCls: '',
      menu: this.colorMenu('smpLabelColor')
    }, {
      text: 'Highlight',
      iconCls: '',
      menu: h
    }, {
      text: 'Highlight Color',
      iconCls: '',
      menu: this.colorMenu('smpHighlightColor')
    }];
  },
  labelVariables: function () {
    var h = [];
    var vars = this.canvas.getVariables();
    var hls = this.canvas.getHighlights(true);
    for (var i = 0; i < vars.length; i++) {
      var check = hls.hasOwnProperty(vars[i].name) ? true : false;
      h.push({
	text: vars[i].name,
	checked: check,
	isVar: true,
	canvasId: this.canvasId,
	handler: this.onHighlight
      });
    }
    return [{
      text: 'Size',
      iconCls: '',
      canvasId: this.canvasId,
      menu: this.buildGroupMenu('varLabelFontSize', [8, 9, 10, 11, 12, 13, 14, 15, 16])
    }, {
      text: 'Scale Factor',
      iconCls: '',
      menu: this.buildGroupMenu('varLabelScaleFontFactor', [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2])     
    }, {
      text: 'Max String Length',
      iconCls: '',
      menu: this.addFormParameter('maxVarStringLen')
    }, {
      text: 'Color',
      iconCls: '',
      menu: this.colorMenu('varLabelColor')
    }, {
      text: 'Highlight',
      iconCls: '',
      menu: h
    }, {
      text: 'Highlight Color',
      iconCls: '',
      menu: this.colorMenu('varHighlightColor')
    }];
  },
  onHighlight: function (item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      t.canvas.modifyHighlights(item.text, item.checked, item.isVar);
      t.canvas.draw();
    }
  },
  // Axes
  axesMenus: function () {
    var items = [];
    if (!this.canvas.getValidAxes) {
      this.canvas.initAxes(false, true);
    }
    var axes = this.canvas.getValidAxes();
    if (axes) {
      this.addItemToMenu(items, 'Font Sizes', false, this.axesFontSizes());
      this.addItemToMenu(items, 'Colors', false, this.axesColors(axes));
      this.addItemToMenu(items, 'Scale Factors', false, this.axesFontScaleFactors());
      this.addItemToMenu(items, 'Show', false, this.axesShow(axes));
      this.addItemToMenu(items, 'Properties', false, this.axesProperties(axes));
      this.addItemToMenu(items, 'Titles', false, this.axesTitles(axes));
      this.addItemToMenu(items, 'Ticks', false, this.axesTicks(axes));
      this.addItemToMenu(items, 'Transform', false, this.axesTransforms(axes));
    }
    return items;
  },
  axesFontSizes: function () {
    var items = [{
      text: 'Ticks',
      iconCls: '',
      menu: this.buildGroupMenu('axisTickFontSize', [8, 9, 10, 11, 12, 13, 14, 15, 16])
    }, {
      text: 'Titles',
      iconCls: '',
      menu: this.buildGroupMenu('axisTitleFontSize', [8, 9, 10, 11, 12, 13, 14, 15, 16])
    }];
    if (this.canvas.hasDataSamples()) {
			items.push({
        text: 'Samples',
        iconCls: '',
        menu: this.buildGroupMenu('smpTitleFontSize', [8, 9, 10, 11, 12, 13, 14, 15, 16])
      });
		}
    return items;
  },
  axesFontScaleFactors: function () {
    var items = [{
      text: 'Ticks',
      iconCls: '',
      menu: this.buildGroupMenu('tickScaleFontFactor', [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2])
    }, {
      text: 'Titles',
      iconCls: '',
      menu: this.buildGroupMenu('titleScaleFontFactor', [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2])
    }];
    if (this.canvas.hasDataSamples()) {
      items.push({
        text: 'Samples',
        iconCls: '',
        menu: this.buildGroupMenu('smpTitleScaleFontFactor', [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2])
      });
    }
    return items;
  },
  axesColors: function (axes) {
    var m = [];
    var ax = [];
    for (var i = 0; i < axes.length; i++) {
      var s = axes[i].replace('Axis', '-Axis');
      ax.push({text: s,
	       iconCls: '',
	       menu: this.colorMenu(axes[i] + 'TickColor')});
    }
    if (ax.length > 0) {
      m.push({text: 'Mesh',
	      iconCls: '',
	      menu: ax});
    }
    m.push({
			text: 'Ticks',
	    iconCls: '',
	    menu: this.colorMenu('axisTickColor')
		});
    m.push({
			text: 'Titles',
	    iconCls: '',
	    menu: this.colorMenu('axisTitleColor')
		});
    if (this.canvas.hasDataSamples()) {
      m.push({
				text: 'Samples',
        iconCls: '',
        menu: this.colorMenu('smpTitleColor')
			});
		}
    return m;
  },
  axesShow: function (axes) {
    var m = [];
    for (var i = 0; i < axes.length; i++) {
      m.push({text: axes[i].replace('Axis', '-Axis'),
	      iconCls: '',
	      menu: this.buildGroupMenu(axes[i] + 'Show', [true, false])});
    }
    return m;
  },
  axesProperties: function (axes) {
    var m = [];
    var a = [];
    m.push({text: 'Over-Extension',
	    iconCls: '',
	    menu: this.addFormParameter('axisExtension')});
    for (var i = 0; i < axes.length; i++) {
      a.push({text: axes[i].replace('Axis', '-Axis'),
	      iconCls: '',
	      menu: this.buildGroupMenu(axes[i] + 'Exact', [true, false])});
    }
    if (a.length > 0) {
      m.push({text: 'Exact',
	      iconCls: '',
	      menu: a});
    }
    return m;
  },
  axesTitles: function (axes) {
    var m = [];
    if (this.canvas.graphType.match(/Scatter/)) {
      for (var i = 0; i < axes.length; i++) {
	var s = axes[i].replace('Axis', '-Axis');
	var n = axes[i] + 'Title';
	m.push({text: s,
		iconCls: '',
		menu: this.addFormParameter(n, true)});
      }
    }
    if (this.canvas.hasDataSamples()) {
      m.push({
				text: 'Samples',
        iconCls: '',
        menu: this.addFormParameter('smpTitle', true)
      })		
		}		
    return m;
  },
  axesTicks: function (axes) {
    var m = [];
    var n = this.axesTicksNumber(axes);
    var s = this.axesTickStyles(axes);
    if (n.length > 0) {
      m.push({text: 'Number',
              iconCls: '',
              menu: n});
    }
    if (s.length > 0) {
      m.push({text: 'Style',
	      iconCls: '',
	      menu: s});
    }
    return m;
  },
  axesTicksNumber: function (axes) {
    var m = [];
    for (var i = 0; i < axes.length; i++) {
      var s = axes[i].replace('Axis', '-Axis');
      var n = axes[i] + 'Ticks';
      m.push({text: s,
	      iconCls: '',
	      menu: this.addFormParameter(n)});
    }
    return m;
  },
  axesTickStyles: function (axes) {
    var m = [];
    if (this.canvas.graphType.match(/Scatter/)) {
      for (var i = 0; i < axes.length; i++) {
	var s = axes[i].replace('Axis', '-Axis');
	var n = axes[i] + 'TickStyle';
	m.push({text: s,
		iconCls: '',
		menu: this.buildGroupMenu(n, ['solid', 'dotted'])});
      }
    }
    return m;
  },
  axesTransforms: function (axes) {
    var m = [];
    if (this.canvas.graphType.match(/Scatter/)) {
      for (var i = 0; i < axes.length; i++) {
	m.push({text: axes[i].replace('Axis', '-Axis'),
		iconCls: '',
		menu: this.buildGroupMenu(axes[i] + 'Transform', ['log2', 'log10', 'exp2', 'exp10',
								  'percentile', false])});
      }
    }
    return m;
  },
  // General
  generalMenus: function () {
    var items = [];
    this.addItemToMenu(items, 'Colors', false, this.generalColors());
    this.addItemToMenu(items, 'Dimensions', false, this.dimensions());
    this.addItemToMenu(items, 'Fonts', false, this.font());
    this.addItemToMenu(items, 'Layout', false, this.graphLayout());
    this.addItemToMenu(items, 'Main Title', false, this.titles());
    this.addItemToMenu(items, 'Orientation', false, this.graphOrientation());
    this.addItemToMenu(items, 'Overlays', false, this.overlays());
    this.addItemToMenu(items, 'Plot', false, this.plot());
    this.addItemToMenu(items, 'Shadows', false, this.shadows());
    this.addItemToMenu(items, 'Types', false, this.graphTypes());
    return items;
  },
  generalColors: function() {
    return [{
      text: 'Background',
      iconCls: '',
      menu: this.background()
    }, {
      text: 'Foreground',
      iconCls: '',
      menu: this.foreground()
    }];
  },
  foreground: function () {
    return this.colorMenu('foreground');
  },
  background: function () {
    var m = [];
    m.push({text: 'Type',
	    iconCls: '',
	    menu: this.backgroundType()});
    if (this.canvas.backgroundType == 'gradient') {
      m.push({text: 'Colors',
	      iconCls: '',
	      menu: this.backgroundGradients()});
    } else {
      m.push({text: 'Color',
	      iconCls: '',
	      menu: this.colorMenu('background')});
    }
    return m;
  },
  backgroundType: function () {
    return this.buildGroupMenu('backgroundType', ['solid', 'gradient']);
  },
  backgroundGradients: function () {
    return [{
      text: 'Color1',
      iconCls: '',
      menu: this.colorMenu('backgroundGradient1Color')
    }, {
      text: 'Color2',
      iconCls: '',
      menu: this.colorMenu('backgroundGradient2Color')
    }];
  },
  dimensions: function () {
    return [{
      text: 'Width',
      iconCls: '',
      menu: this.addFormParameter('width')
    }, {
      text: 'Height',
      iconCls: '',
      menu: this.addFormParameter('height')
    }, {
      text: 'Margins',
      iconCls: '',
      menu: this.addFormParameter('margin')
    }];
  },
  font: function() {
    return [
      {text: 'Name',
       iconCls: '',
       menu: this.fontName()},
      {text: 'Max Size',
       iconCls: '',
       menu: this.fontSize()},
      {text: 'Auto Scale',
       iconCls: '',
       menu: this.fontAutoScale()}
    ];
  },
  fontName: function() {
    return this.buildGroupMenu('fontName', this.canvas.fonts || []);
  },
  fontSize: function() {
    return this.buildGroupMenu('maxTextSize', [8, 9, 10, 11, 12, 13, 14, 15, 16]);
  },
  fontAutoScale: function() {
    return this.buildGroupMenu('autoScaleFont', [true, false]);
  },
  graphLayout: function() {
    var items = [];
    if (this.canvas.layoutComb) {
      var c = 0;
      for (var i = 0; i < this.canvas.layoutRows; i++) {
	for (var j = 0; j < this.canvas.layoutCols; j++) {
	  var lab = 'Graph ' + (c + 1) + ' Weight';
	  items.push({
	    text: lab,
	    iconCls: '',
	    menu: this.addFormParameter('subGraphWeight' + c)
	  });
	  c++;
	}
      }
    }
    return items;
  },
  titles: function () {
    var items = [{
      text: 'Title',
      iconCls: '',
      menu: this.titlesTitle()
    }];
    if (this.canvas.title) {
      items.push({
	text: 'Subtitle',
	iconCls: '',
	menu: this.titlesSubTitle()
      });
    }
    return items;
  },
  titlesTitle: function () {
    var items = [{
      text: 'Text',
      iconCls: '',
      menu: this.addFormParameter('title', true)
    }];
    if (this.canvas.title) {
      items.push({
	text: 'Height',
	iconCls: '',
	menu: this.addFormParameter('titleHeight')
      });
    }
    return items;
  },
  titlesSubTitle: function () {
    var items = [{
      text: 'Text',
      iconCls: '',
      menu: this.addFormParameter('subtitle', true)
    }];
    if (this.canvas.subtitle) {
      items.push({
	text: 'Height',
	iconCls: '',
	menu: this.addFormParameter('subtitleHeight')
      });
    }
    return items;
  },
  graphOrientation: function () {
    if (this.canvas.hasOrientation()) {
      return this.buildGroupMenu('graphOrientation', ['vertical', 'horizontal']);
    } else {
      return false;
    }
  },
  overlays: function () {
    if (this.canvas.hasOrientation()) {
      var items = [];
      var os = [];
      var ov = [];
      var annt = this.canvas.getAnnotations();
      var anntv = this.canvas.getAnnotations(true);
      var ovls = this.canvas.getSmpOverlays();
      var ovlv = this.canvas.getVarOverlays();
      var isOvl = false;
      for (var i = 0; i < annt.length; i++) {
	var check = ovls.hasOwnProperty(annt[i]) ? true : false;
	if (check) {
	  isOvl = true;
	}
	os.push({
	  text: annt[i],
	  checked: check,
	  canvasId: this.canvasId,
	  handler: this.onSmpOverlays
	});
      }
      if (annt && annt.length > 0) {
	items.push({
	  text: 'Sample Annotations',
	  iconCls: '',
	  menu: os
	});
      }
      if (this.canvas.graphType == 'Heatmap') {
        for (var i = 0; i < anntv.length; i++) {
          var check = ovlv.hasOwnProperty(anntv[i]) ? true : false;
          if (check) {
            isOvl = true;
          }
          ov.push({
            text: anntv[i],
            checked: check,
            canvasId: this.canvasId,
            handler: this.onVarOverlays
          });
        }
        if (anntv && anntv.length > 0) {
          items.push({
            text: 'Variable Annotations',
            iconCls: '',
            menu: ov
          });
        }
      }
      if (isOvl) {
	items.push({
	  text: 'Width',
	  iconCls: '',
	  menu: this.addFormParameter('overlaysWidth')
	});
      }
      return items;
    } else {
      return false;
    }

  },
  onSmpOverlays: function (item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      t.canvas.modifySmpOverlays(item.text, item.checked);
      t.canvas.draw();
    }
  },
  onVarOverlays: function (item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      t.canvas.modifyVarOverlays(item.text, item.checked);
      t.canvas.draw();
    }
  },
  // Plot specific
  plot: function () {
    switch (this.canvas.graphType) {
      case 'Bar':
      case 'Area':
      case 'Dotplot':
      case 'Stacked':
      case 'StackedPercent':
      case 'Boxplot':
        return this.oneDGraphs();
      case 'Line':
      case 'BarLine':
        return [{
	  text: 'Configuration',
	  iconCls: '',
	  menu: this.oneDGraphs()
	}, {
	  text: 'Lines',
	  iconCls: '',
	  menu: this.lines()
	}];
      case 'Heatmap':
        var items = [{
	  text: 'Configuration',
	  iconCls: '',
	  menu: this.oneDGraphs()
	}, {
	  text: 'Heatmaps',
	  iconCls: '',
	  menu: this.heatmaps()
	}];
        this.addItemToMenu(items, 'Trees', false, this.dendrograms())
        return items;
      case 'Scatter2D':
        return this.scatter2D();
      case 'ScatterBubble2D':
        return false;
      case 'Scatter3D':
        return this.scatter3D();
      case 'Correlation':
        return this.correlation();
      case 'Venn':
        return this.venn();
      case 'Pie':
        return this.pie();
      case 'Network':
        return this.networks();
      case 'Genome':
      return this.genomeBrowser();
    }
  },
  randomData: function () {
    return [{
      text: 'Restore',
      iconCls: '',
      canvasId: this.canvasId,
      handler: this.onRestoreData			
    }, {
      text: 'Generate',
      iconCls: '',
      canvasId: this.canvasId,
      handler: this.onRandomData			
    }, {
      text: 'Data Mean',
      iconCls: '',
      menu: this.addFormParameter('randomDataMean')
    }, {
      text: 'Data Sigma',
      iconCls: '',
      menu: this.addFormParameter('randomDataSigma')
    }, {
      text: 'Number of Variables',
      iconCls: '',
      menu: this.addFormParameter('randomDataVariables')
    }, {
      text: 'Number of Samples',
      iconCls: '',
      menu: this.addFormParameter('randomDataSamples')
    }, {
      text: 'Number of Variable Annotations',
      iconCls: '',
      menu: this.addFormParameter('randomDataVariableAnnotations')
    }, {
      text: 'Number of Sample Annotations',
      iconCls: '',
      menu: this.addFormParameter('randomDataSampleAnnotations')
    }, {
      text: 'Ratio of Variables per Factor',
      iconCls: '',
      menu: this.addFormParameter('randomDataVariableAnnotationRatio')
    }, {
      text: 'Ratio of Samples per Factor',
      iconCls: '',
      menu: this.addFormParameter('randomDataSampleAnnotationRatio')
    }, {
      text: 'Percentage of Missing Data',
      iconCls: '',
      menu: this.addFormParameter('randomMissingDataPercentage')
    }];
  },
  onRestoreData: function (item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      t.canvas.restoreRandomData();
      t.canvas.draw();
    }
  },
  onRandomData: function (item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      t.canvas.createRandomData();
      t.canvas.draw();
    }
  },
  shadows: function () {
    if (!this.canvas.isIE) {
      return [{
	text: 'Show',
	iconCls: '',
	menu: this.buildGroupMenu('showShadow', [true, false])
      }];
    } else {
      return false;
    }
  },
  // 1D graaphs
  oneDGraphs: function () {
    var items = [];
    if (this.canvas.graphType == 'Heatmap') {
      this.addItemToMenu(items, 'Sample Blocks', false, this.blocks());
      this.addItemToMenu(items, 'Random Data', false, this.randomData());
    } else {
      this.addItemToMenu(items, 'Sample Hairline', false, this.hairline());
      this.addItemToMenu(items, 'Sample Blocks', false, this.blocks());
      this.addItemToMenu(items, 'Trees', false, this.dendrograms(true));
      this.addItemToMenu(items, 'Parameters', false, this.colorBy());
      this.addItemToMenu(items, 'Random Data', false, this.randomData());
    }
    return items;
  },
  colorBy: function () {
    var annt = this.canvas.getAnnotations();
    annt.push(false);
    return [{
      text: 'Color By',
      iconCls: '',
      menu: this.buildGroupMenu('colorBy', annt)
    }];
  },
  hairline: function () {
    return [{
      text: 'Type',
      iconCls: '',
      menu: this.buildGroupMenu('smpHairline', ['solid', 'dotted', false])
    }, {
      text: 'Color',
      iconCls: '',
      menu: this.colorMenu('smpHairlineColor')
    }];
  },
  blocks: function() {
    var items = [];
    if (this.canvas.graphType != 'Heatmap') {
      items.push({
	text: 'Contrast',
	iconCls: '',
	menu: this.buildGroupMenu('blockContrast', [true, false])
      });
      if (this.canvas.blockContrast) {
	items.push({
	  text: 'Contrast Odd Colors',
	  iconCls: '',
	  menu: this.colorMenu('blockContrastOddColor')
	});
	items.push({
	  text: 'Contrast Even Colors',
	  iconCls: '',
	  menu: this.colorMenu('blockContrastEvenColor')
	});
      }
    }
    if (! this.canvas.layoutComb) {
      items.push({
	text: 'Autoextend',
	iconCls: '',
	menu: this.buildGroupMenu('autoExtend', [true, false])
      });
      if (this.canvas.autoExtend) {
	if (this.canvas.graphType != 'Heatmap') {
	  items.push({
	    text: 'Separation Factor',
	    iconCls: '',
	    menu: this.buildGroupMenu('blockSeparationFactor', [0, 0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 3, 4, 5])
	  });
	}
	items.push({
	  text: 'Width / Height Factor',
	  iconCls: '',
	  menu: this.buildGroupMenu('blockFactor', [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 3, 4, 5])
	});
      }
    }
    return items;
  },
  // Lines / BarLines
  lines: function () {
    var items = [{
      text: 'Decorations',
      iconCls: '',
      menu: this.buildGroupMenu('lineDecoration', ['dot', 'symbol', false])
    }]
    if (this.canvas.graphType != 'Barline') {
      items.push({
	text: 'Coordinate Colors in Bar / Lines',
	iconCls: '',
	menu: this.buildGroupMenu('coordinateLineColor', [true, false])
      })
    }
    return items;
  },
  // 2D Scatter
  scatter2D: function () {
    var items = [{
      text: 'Configuration',
      iconCls: '',
      menu: this.scatterParameters()
    }, {
      text: 'Layout',
      iconCls: '',
      menu: this.allVsAll()
    }, {
      text: 'Scatter Line Plot',
      iconCls: '',
      menu: this.buildGroupMenu('isScatterLine', [true, false])
    }, {
      text: 'Histogram',
      iconCls: '',
      menu: this.histograms()
    }, {
      text: 'Functions',
      iconCls: '',
      menu: this.functions()
    }];
    items.push({
      text: 'Random Data',
      iconCls: '',
      menu: this.randomData()
    });
    return items;
  },
  functions: function () {
    var items = [{
      text: 'Add regression line',
      iconCls: '',
      canvasId: this.canvasId,
      handler: this.onRegressionLine
    }, {
      text: 'Add normal distribution line',
      iconCls: '',
      canvasId: this.canvasId,
      handler: this.onNormalDistributionLine
    }];
    return items;    
  },
  onRegressionLine: function (item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      if (t.canvas) {
        t.canvas.addRegressionLine();
	t.canvas.draw();
      }
    }	
  },
  onNormalDistributionLine: function (item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      if (t.canvas) {
        t.canvas.addNormalDistributionLine();
	t.canvas.draw();
      }
    }	
  },
  histograms: function () {
    var items = [{
      text: 'Plot as Histogram',
      iconCls: '',
      menu: this.buildGroupMenu('isHistogram', [true, false])
    }];
    if (this.canvas.isCreateHistogram) {
      items.push({
	text: 'Remove Histogram',
	iconCls: '',
	canvasId: this.canvasId,
	handler: this.onRemoveHistogram
      });
    } else {
      items.push({
	text: 'Create Histogram',
	iconCls: '',
	canvasId: this.canvasId,
	handler: this.onCreateHistogram
      });
    }
    if (this.canvas.isHistogram) {
      items.push({
	text: 'Number of Bins',
	iconCls: '',
	menu: this.addFormParameter('histogramBins')
      });
      items.push({
	text: 'Histogram Bar width',
	iconCls: '',
	menu: this.addFormParameter('histogramBarWidth')
      });
    }
    return items;
  },
  onCreateHistogram: function (item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      if (t.canvas) {
        t.canvas.createHistogram();
	t.canvas.draw();
      }
    }	
  },
  onRemoveHistogram: function (item) {
    if (item.canvasId) {
      var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
      if (t.canvas) {
        t.canvas.removeHistogram();
	t.canvas.draw();
      }
    }	
  },
  allVsAll: function () {
    var o = [{
      text: 'true',
      checked: this.canvas.allVsAll,
      canvasId: this.canvasId,
      handler: this.onAllVsAll
    }, {
      text: 'false',
      checked: this.canvas.allVsAll ? false : true,
      canvasId: this.canvasId,
      handler: this.onAllVsAll
    }
   ]
    var items = [{
      text: 'All vs All Series',
      iconCls: '',
      menu: o
    }];
    if (this.canvas.allVsAll) {
      items.push({
	text: 'All vs All Type',
	iconCls: '',
	menu: this.buildGroupMenu('allVsAllType', ['upper', 'lower', 'both'])
      });
    }
    return items;
  },
  onAllVsAll: function (item) {
    var t = Ext.getCmp(Ext.get(item.canvasId).parent('div.x-panel').id);
    if (t.canvas) {
      if (item.text == 'false') {
	t.canvas.unsetAllVsAll()
      } else {
	t.canvas.allVsAll = true;
      }
      t.canvas.draw();
    }
  },
  // 3D Scatter
  scatter3D: function () {
    return [{
      text: 'Rotation',
      iconCls: '',
      menu: this.scatter3DRotate()
    }, {
      text: 'Configuration',
      iconCls: '',
      menu: this.scatterParameters()
    }, {
      text: 'Plot as Scatter Line',
      iconCls: '',
      menu: this.buildGroupMenu('isScatterLine', [true, false])
    }, {
      text: 'Random Data',
      iconCls: '',
      menu: this.randomData()
    }];
  },
  scatterParameters: function () {
    var annt = this.canvas.getAnnotations(true);
    var smps = this.canvas.getSamples();
    for (var i = 0; i < smps.length; i++) {
      annt.push(smps[i].name);
    }    
    annt.push('variable');
    annt.push(false);
    return [{
      text: 'Color By',
      iconCls: '',
      menu: this.buildGroupMenu('colorBy', annt)
    }, {
      text: 'Shape By',
      iconCls: '',
      menu: this.buildGroupMenu('shapeBy', annt)
    }, {
      text: 'Size By',
      iconCls: '',
      menu: this.buildGroupMenu('sizeBy', annt)
    }];
  },
  scatter3DRotate: function () {
    return [{
      text: 'X-Axis',
      iconCls: '',
      menu: this.addFormParameter('xRotate')
    }, {
      text: 'Y-Axis',
      iconCls: '',
      menu: this.addFormParameter('yRotate')
    }, {
      text: 'Z-Axis',
      iconCls: '',
      menu: this.addFormParameter('zRotate')
    }];
  },
  // Correlation
  correlation: function () {
    return [{
      text: 'Anchor',
      iconCls: '',
      menu: this.correlationAnchor()
    }, {
      text: 'Axis',
      iconCls: '',
      menu: this.buildGroupMenu('correlationAxis', ['samples', 'variables'])
    }, {
      text: 'Heatmap',
      iconCls: '',
      menu: this.heatmaps()
    }, {
      text: 'Random Data',
      iconCls: '',
      menu: this.randomData()
    }];
  },
  correlationAnchor: function () {
    return [{
      text: 'Show',
      iconCls: '',
      menu: this.buildGroupMenu('correlationAnchorLegend', [true, false])
    }, {
      text: 'Width',
      iconCls: '',
      menu: this.addFormParameter('correlationAnchorLegendAlignWidth')
    }];
  },
  // Heatmap
  heatmaps: function () {
    return [{
      text: 'Indicator Height',
      iconCls: '',
      menu: this.addFormParameter('indicatorHeight')
    }, {
      text: 'Indicator Center',
      iconCls: '',
      menu: this.buildGroupMenu('indicatorCenter', ['black', 'white'])
    }, {
      text: 'Scheme',
      iconCls: '',
      menu: this.buildGroupMenu('heatmapType', ['blue', 'blue-green', 'blue-red', 'green', 'green-blue', 'green-red', 'red', 'red-blue', 'red-green'])
    }];
  },
  // Trees
  dendrograms: function (justSamples) {
    if (this.canvas.hasDendrograms) {
      var items = [];
      items.push({
	text: 'Samples',
	iconCls: '',
	menu: this.dendrogramSamples()
      });
      if (!justSamples) {
	items.push({
	  text: 'Variables',
	  iconCls: '',
	  menu: this.dendrogramVariables()
	});
      }
      if (this.canvas.showSmpDendrogram || this.canvas.showVarDendrogram) {
	items.push({
	  text: 'Height',
	  iconCls: '',
	  menu: this.addFormParameter('dendrogramSpace')
	});
	items.push({
	  text: 'Hang',
	  iconCls: '',
	  menu: this.buildGroupMenu('dendrogramHang', [true, false])
	});
      }
      return items;
    } else {
      return false;
    }
  },
  dendrogramSamples: function () {
    var items = [{
      text: 'Show',
      iconCls: '',
      menu: this.buildGroupMenu('showSmpDendrogram', [true, false])
    }];
    if (this.canvas.showSmpDendrogram) {
      var o = this.canvas.graphOrientation == 'vertical' ? ['top', 'bottom'] : ['left', 'right'];
      items.push({
	text: 'Position',
	iconCls: '',
	menu: this.buildGroupMenu('smpDendrogramPosition', o)
      });
    }
    return items;
  },
  dendrogramVariables: function () {
    var items = [{
      text: 'Show',
      iconCls: '',
      menu: this.buildGroupMenu('showVarDendrogram', [true, false])
    }];
    if (this.canvas.showVarDendrogram) {
      items.push({
	text: 'Position',
	iconCls: '',
	menu: this.buildGroupMenu('varDendrogramPosition', ['top', 'bottom'])
      });
    }
    return items;
  },
  // Venn Diagram
  venn: function () {
    return [{
      text: 'Groups',
      iconCls: '',
      menu: this.buildGroupMenu('vennGroups', [1, 2, 3, 4])
    }]
  },
  // Pie graphs
  pie: function () {
    return [{
      text: 'Style',
      iconCls: '',
      menu: this.buildGroupMenu('pieType', ['solid', 'separated'])
    }, {
      text: 'Precision',
      iconCls: '',
      menu: this.addFormParameter('pieSegmentPrecision')
    }, {
      text: 'Separation',
      iconCls: '',
      menu: this.addFormParameter('pieSegmentSeparation')
    }, {
      text: 'Labels',
      iconCls: '',
      menu: this.buildGroupMenu('pieSegmentLabels', ['inside', 'outside'])
    }];
  },
  // Networks
  networks: function() {
    return [{
      text: 'Animation',
      iconCls: '',
      menu: this.networkAnimation()
    }, {
      text: 'Pre-Scale',
      iconCls: '',
      menu: this.buildGroupMenu('preScaleNetwork', [true, false])
    }, {
      text: 'Nodes',
      iconCls: '',
      menu: this.networkNodes()
    }, {
      text: 'Random Networks',
      iconCls: '',
      menu: this.networkRandom()
    }];
  },
  networkAnimation: function () {
    return [{
      text: 'Show',
      iconCls: '',
      menu: this.buildGroupMenu('showAnimation', [true, false])
    }, {
      text: 'Font',
      iconCls: '',
      menu: this.networkAnimationFont()
    }];
  },
  networkAnimationFont: function () {
    return [{
      text: 'Size',
      iconCls: '',
      menu: this.addFormParameter('showAnimationFontSize')
    }, {
      text: 'Color',
      iconCls: '',
      menu: this.colorMenu('showAnimationFontColor')
    }];
  },
  networkNodes: function () {
    return [{
      text: 'Threshold Number',
      iconCls: '',
      menu: this.addFormParameter('showNodeNameThreshold')
    }, {
      text: 'Font',
      iconCls: '',
      menu: this.networkNodesFont()
    }];
  },
  networkNodesFont: function () {
    return [{
      text: 'Size',
      iconCls: '',
      menu: this.addFormParameter('nodeFontSize')
    }, {
      text: 'Color',
      iconCls: '',
      menu: this.colorMenu('nodeFontColor')
    }];
  },
  networkRandom: function () {
    return [{
      text: 'Generate',
      iconCls: '',
      menu: this.buildGroupMenu('randomNetwork', [true, false])
    }, {
      text: 'Node Number',
      iconCls: '',
      menu: this.addFormParameter('randomNetworkNodes')
    }, {
      text: 'Max Edges / Node',
      iconCls: '',
      menu: this.addFormParameter('randomNetworkNodeEdgesMax')
    }];
  },
  // Genome Browser
  genomeBrowser: function() {
    return [{
      text: 'Tracks',
      iconCls: '',
      menu: this.genomeBrowserTracks()
    }, {
      text: 'Features',
      iconCls: '',
      menu: this.genomeBrowserFeatures()
    }, {
      text: 'Sequence',
      iconCls: '',
      menu: this.genomeBrowserSequence()
    }, {
      text: 'Wire Color',
      iconCls: '',
      menu: this.colorMenu('wireColor')
    }, {
      text: 'Ticks',
      iconCls: '',
      menu: this.genomeBrowserTicks()
    }];
  },
  genomeBrowserTracks: function() {
    return [{
      text: 'Font',
      iconCls: '',
      menu: this.genomeBrowserTracksFont()
    }];
  },
  genomeBrowserTracksFont: function() {
    return [{
      text: 'Size',
      iconCls: '',
      menu: this.addFormParameter('trackNameFontSize')
    }, {
      text: 'Color',
      iconCls: '',
      menu: this.colorMenu('trackNameFontColor')
    }];
  },
  genomeBrowserFeatures: function() {
    return [{
      text: 'Threshold Number',
      iconCls: '',
      menu: this.addFormParameter('showFeatureNameThereshold')
    }, {
      text: 'Font',
      iconCls: '',
      menu: this.genomeBrowserFeaturesFont()
    }, {
      text: 'Defaults',
      iconCls: '',
      menu: this.genomeBrowserFeaturesDefaults()
    }];
  },
  genomeBrowserFeaturesFont: function() {
    return [{
      text: 'Size',
      iconCls: '',
      menu: this.addFormParameter('featureNameFontSize')
    }, {
      text: 'Color',
      iconCls: '',
      menu: this.colorMenu('featureNameFontColor')
    }];
  },
  genomeBrowserFeaturesDefaults: function() {
    return [{
      text: 'Width',
      iconCls: '',
      menu: this.addFormParameter('featureWidthDefault')
    }, {
      text: 'Height',
      iconCls: '',
      menu: this.addFormParameter('featureHeightDefault')
    }, {
      text: 'Type',
      iconCls: '',
      menu: this.addFormParameter('featureTypeDefault', true)
    }];
  },
  genomeBrowserSequence: function() {
    return [{
      text: 'Font Size',
      iconCls: '',
      menu: this.addFormParameter('sequenceFontSize')
    }, {
      text: 'A Color',
      iconCls: '',
      menu: this.colorMenu('sequenceAColor')
    }, {
      text: 'C Color',
      iconCls: '',
      menu: this.colorMenu('sequenceCColor')
    }, {
      text: 'G Color',
      iconCls: '',
      menu: this.colorMenu('sequenceGColor')
    }, {
      text: 'T Color',
      iconCls: '',
      menu: this.colorMenu('sequenceTColor')
    }, {
      text: 'Multiple Color',
      iconCls: '',
      menu: this.colorMenu('sequenceMColor')
    }];
  },
  genomeBrowserTicks: function() {
    return [{
      text: 'Number',
      iconCls: '',
      menu: this.addFormParameter('ticks')
    }, {
      text: 'Label Periodicity',
      iconCls: '',
      menu: this.addFormParameter('periodTicksLabels')
    }];
  },
  graphTypes: function () {
    if (this.canvas.layoutComb) {
      var items = [];
      var c = 0;
      for (var i = 0; i < this.canvas.layoutRows; i++) {
	for (var j = 0; j < this.canvas.layoutCols; j++) {
	  var lab = 'Graph ' + (c + 1);
	  items.push({
	    text: lab,
	    iconCls: '',
	    menu: this.buildGroupMenu('subGraphType' + c, this.canvas.getValidGraphTypes())
	  });
	  c++;
	}
      }
      return items;
    } else {
      return this.buildGroupMenu('graphType', this.canvas.getValidGraphTypes());
    }
  },
  endDrag: function(n) {
    if(n && n.nodes && n.nodes.length)
    {
      this.addNode(n.nodes);
      if(!this.viewInfo) this.fireEvent('endnodedrag', n.nodes, function(s) {
        if(s) this.removeNode(n.nodes);
      }.createDelegate(this));
    }
    if(n && n.legend)
      this.updateLegend();
  },
  toggleChildren: function(b, e, n) {
    if(b.text.match(/expand/i)) n.hideChildren = false;
    else n.hideChildren = true;
    this.addNode(n);
    if(!this.viewInfo) this.fireEvent('expandgroup', n, this.removeNode.createDelegate(this, [n]));
    this.canvas.draw();
  },
  toggleNode: function(n, hide, other) {
    var ns = [];
    if(!n) // unhide all
    {
      n = [];
      for(var i in this.canvas.nodes)
      {
        var node = this.canvas.nodes[i];
        if(node.hide)
        {
          ns.push(node.id);
          n.push(node);
        }
      }
    }
    else if(other) // hide all other
    {
      var tmp = {};
      if(!Ext.isArray(n)) n = [n];
      for(var i = 0; i < n.length; i++)
        tmp[n[i].id] = n[i];
      n = [];
      for(var i in this.canvas.nodes)
      {
        var node = this.canvas.nodes[i];
        if(!tmp[node.id])
        {
          ns.push(node.id);
          n.push(node);
        }
      }
    }
    else if(n.length && !n.id)
      for(var i = 0; i < n.length; i++)
        ns.push(n[i].id);
    else ns.push(n.id);
    this.canvas.hideUnhideNodes(ns, hide);
    this.canvas.selectNode = {};
    this.addNode(n);
    if(!this.viewInfo) this.fireEvent('togglenode', n, hide, this.removeNode.createDelegate(this, [n]));
    this.canvas.draw();
  },
  changeNodeOrder: function(n, dir) {
    this.canvas[dir](n);
    this.updateOrder();
    this.canvas.draw();
  },
  hasNote: function(o) {
    if(!o) return false;
    if(o.length)
    {
      for(var i = 0; i < o.length; i++)
      {
        var o1 = o[i];
        if(o1.data && o1.data.note)
          return true;
      }
      return false;
    }
    return o.data && o.data.note;
  },
  copyObj: function(b, e, type, o) {
    o = this.canvas.cloneObject(o);
    var hasNote = this.hasNote(o);
    if(type == 'node')
    {
      delete o.id;
      var f = function(clean) {
        if(this.cleanNode) this.cleanNode(o, clean);
        else delete o.data;
        var w = new Ext.canvasXpress.nodeDialog(o, this);
        w.show();
      }.createDelegate(this);
      if(hasNote && this.warnClean) this.warnClean(f);
      else f(true);
    }
    else if(type == 'edge')
    {
      delete o.id1;
      delete o.id2;
      var f = function(clean) {
        if(this.cleanEdge) this.cleanEdge(o, clean);
        else delete o.data;
        var w = new Ext.canvasXpress.edgeDialog(o, null, this);
        w.show();
      }.createDelegate(this);
      if(hasNote && this.warnClean) this.warnClean(f);
      else f(true);
    }
  },
  editNode: function(b, e, o, callback, needXY) {
    if(needXY) this.clickXY = [e.x + 50, e.y + 50];
    var w = new Ext.canvasXpress.nodeDialog(o, this, callback);
    w.show();
  },
  editEdge: function(b, e, o, dir, o1) {
    var w = new Ext.canvasXpress.edgeDialog(o, dir, this, o1);
    w.show();
  },
  makeOENode: function(o) {
    // confirm first
    Ext.Msg.confirm('Warning', 'Once a node is turned into a Special Anchor Node, it loses its original styles (size, color, image ...) forever. Are you sure you would like to proceed?', function(btn) {
      if(btn == 'yes')
      {
        o.width = 10;
        o.height = 10;
        o.color = 'rgba(0,0,0,0)';
        o.outline = 'rgba(0,0,0,0)';
        o.eventless = true;
        this.addNode(o);
        this.canvas.draw();
//         this.canvas.addToNetworkStack(); // didn't seem to work?
      }
    }, this);
  },
  addOEEdge: function(b, e, o, dir, o1) {
  },
  toggleOEEdge: function(show) {
  },
  simpleSearch: function() {
    var win = new Ext.Window({
      title: 'Search for Nodes and Edges',
      width: 550,
      height: 300,
      stateId: 'extcxp-simplesearch',
      stateful: true,
      stateEvents: ['move','resize'],
      constrainHeader: true,
      x: 50,
      layout: 'anchor',
      items: [{
          xtype: 'form',
          border: false,
//           height: 35,
          items: [{
            xtype: 'compositefield',
            fieldLabel: 'Search Value',
            labelStyle: 'margin-left:5px;margin-top:5px',
            style: 'margin:5px 5px 0px 5px;',
            items: [{
              xtype: 'textfield',
              emptyText: 'RegEx is accepted',
              enableKeyEvents: true,
              width: 200,
              listeners: {
                scope: this,
                keyup: function(f, e) {
                  if(e.getKey() == 13)
                  {
                    var b = f.nextSibling();
                    b.initialConfig.handler.call(this, b);
                  }
                }
              }
            }, {
              xtype: 'button',
              text: 'Search',
              scope: this,
              handler: function(b) {
                if(!this.canvas.data) return;
                var text = b.previousSibling().getValue(), once = b.nextSibling().getValue();
                var re = new RegExp(text, 'i'), re1 = new RegExp('('+text+')', 'i');
                // now do the search
                var nodes = this.canvas.data.nodes || [], edges = this.canvas.data.edges || [],
                    results = [];
                var addRes = function(n, type, key, val) {
                  results.push([n, type, key, val.replace(re1, '<span style="color:black;background-color:yellow;">$1</span>')]);
                }, recurseSearch = function(obj, type, results, key, val) {
                  switch(typeof(val))
                  {
                    case 'string':
                      if(re.test(val))
                      {
                        addRes(obj, type, key, val);
                        if(once) return true;
                      }
                      break;
                    case 'object':
                      if(Ext.isArray(val))
                      {
                        for(var i = 0; i < val.length; i++)
                          if(val[i] && recurseSearch(obj, type, results, key, val[i]) && once)
                            return true;
                      }
                      else
                        for(var i in val)
                          if(val[i] && recurseSearch(obj, type, results, i + ' < ' + key, val[i]) && once)
                            return true;
                      break;
                    default: return;
                  }
                };
                for(var i = 0; i < nodes.length; i++)
                {
                  var n = nodes[i], found = false;
                  for(var j in {'name':1,'label':1})
                    if(re.test(n[j]))
                    {
                      found = true;
                      addRes(n, 'Node', j, n[j] || '');
                      if(once) break;
                    }
                  if(found && once) continue;
                  if(n.data) recurseSearch(n, 'Node', results, 'data', n.data);
                }
                for(var i = 0; i < edges.length; i++)
                {
                  var n = edges[i];
                  if(n.data) recurseSearch(n, 'Edge', results, 'data', n.data);
                }
                var grid = win.items.items[1], store = grid.getStore();
                store.loadData(results);
              }
            }, {
              xtype: 'checkbox',
              boxLabel: 'Advanced Search Options',
              checked: false,
              listeners: {
                scope: this,
                check: function(c, checked) {
                  var com = win.items.items[0].items.items[1];
                  if(checked)
                  {
                    com.show();
                    com.hideLabel = !checked;
                  }
                  else
                  {
                    com.hide();
                    com.hideLabel = !checked;
                  }
                }
              }
            }]
          }, {
            xtype: 'compositefield',
            hidden: true,
            hideLabel: true,
            labelStyle: 'margin-left:5px;margin-top:5px',
            style: 'margin:0px 5px 5px 5px;',
            items: [{
              xtype: 'label',
              text: 'Search Key:',
              width: 100
            }, {
              xtype: 'textfield',
              emptyText: 'RegEx is accepted',
              enableKeyEvents: true,
              width: 200,
              listeners: {
                scope: this,
                keyup: function(f, e) {
                  if(e.getKey() == 13)
                  {
                    var b = f.nextSibling();
                    b.initialConfig.handler.call(this, b);
                  }
                }
              }
            }, {
              xtype: 'checkbox',
              boxLabel: 'One row per object',
              checked: true
            }]
          }]
        }, {
          xtype: 'grid',
          anchor: '100% -40',
          autoExpandColumn: 'detail',
          store: new Ext.data.ArrayStore({
            autoDestroy: true,
            data: [],
            fields: [ 'obj', 'type', 'name', 'detail' ]
          }),
          sm: new Ext.grid.RowSelectionModel({
            singleSelect:true,
            listeners: {
              scope: this,
              rowselect: function(sm, idx, r) {
                var d = r.data;
                if(d.type == 'Node')
                {
                  this.canvas.highlightNode = [d.obj.id];
                  this.canvas.draw();
                }
              }
            }
          }),
          cm: new Ext.grid.ColumnModel({
              defaults: { sortable: true, width: 60 },
              columns: [
                { header: "Type", dataIndex:'type' },
                { header: "Matched Field", dataIndex:'name', width: 120 },
                { header: "Matched Value", dataIndex:'detail', id: 'detail' }
            ]}),
          listeners: {
            scope: this,
            rowdblclick: function(g, idx, e) {
              rows = g.getSelectionModel().getSelections();
              if(rows && rows.length)
                this.canvas.flashNode(rows[0].data.obj.id);
            }
          }
        }],
      bbar: {
        items: [
          {
            text: 'Clear Highlight',
            scope: this,
            handler : function(b) {
              this.canvas.highlightNode = [];
              this.canvas.draw();
            }
          },
          '->',
          {
            text: 'Flash the Selected to Highlight',
            scope: this,
            handler : function(b) {
              var grid = win.items.items[1], rows = grid.getSelectionModel().getSelections();
              if(rows && rows.length)
                this.canvas.flashNode(rows[0].data.obj.id);
            }
          },
          {
            text: 'Edit Selected Object',
            scope: this,
            handler : function(b) {
              var grid = win.items.items[1], rows = grid.getSelectionModel().getSelections();
              if(rows && rows.length)
              {
                var d = rows[0].data, o = d.obj, w;
                switch(d.type)
                {
                  case 'Node':
                    w = new Ext.canvasXpress.nodeDialog(o, this);
                    break;
                  case 'Edge':
                    w = new Ext.canvasXpress.edgeDialog(o, null, this);
                    break;
                }
                if(w) w.show();
              }
            }
          }
        ]
      }
    });
    win.show();
    this.simpleSearchWin = win;
  },
  showSearchWin: function(allLabels, allNames, sc, es, scall, supported, sf) {
    for(var i in this.canvas.nodes)
    {
      var l = this.canvas.nodes[i], label = this.getName(l);
      allLabels.push([label, label]); // val, displayVal (if setting id as val, more changes is needed to ensure id instead of label is searched)
      allNames.push(l.tooltip || l.name);
    }
    allLabels.sort(Ext.canvasXpress.utils.ciSort);
    allNames.sort(Ext.canvasXpress.utils.ciSort);

    var h = [];
    if(es.hide)
      for(var i = 0; i < es.hide.length; i++)
        h[es.hide[i]] = 1;

    for(var i = 0; i < scall.length; i++)
      if(!h[scall[i][1]])
      {
        supported[scall[i][1]] = 1;
        sc.push(scall[i]);
      }
    if(es.searchItems)
      for(var i = 0; i < es.searchItems.length; i++)
        sc.push(es.searchItems[i]);

    var ls = {
      scope: this,
      search: sf,
      highlight: function(res) {
        var ids = [], node = false;
        for(var i = 0; i < res.length; i++)
        {
          ids.push(res[i].id);
          if(node || (res[i].id && !res[i].id1 && !res[i].id2))
            node = true;
        }
        if(!node) return;
        this.canvas.setSelectNodes(ids);
        this.canvas.draw();
      },
      clear: function() {
        this.canvas.setSelectNodes([]);
        this.canvas.draw();
      }
    };
    if(es.listeners && es.listeners.list)
      ls.list = es.listeners.list;

    var w = new Ext.ux.SearchWindow({
      listeners: ls,
      searchCriteria: sc,
      stateful: true,
      stateEvents: ['move', 'resize'],
      stateId: 'extcxp-advancedsearch',
      notOpFn: function(res) {
        var type, res1 = [];
        for(var i in res)
        {
          type = (res[i].id1 || res[i].id2)? 'edges' : 'nodes';
          break;
        }
        var arr = this.canvas.data[type];
        for(var i = 0; i < arr.length; i++)
        {
          for(var j = 0; j < res.length; j++)
            if(res[j] == arr[i]) break;
          if(j == res.length) res1.push(arr[i]);
        }
        return res1;
      }.createDelegate(this)
    });
    w.show();
    this.searchWin = w;
  },
  nodeConnectEdge: function(inpset, set, direct) {
    var nodes = inpset? inpset : this.canvas.nodes, ids = {}, res = [];
    for(var i in set)
    {
      var l = set[i];
      if(typeof(l) == 'function') continue;
      ids[direct == 'to'? l.id1 : l.id2] = 1;
    }
    for(var i in nodes)
    {
      var l = nodes[i];
      if(typeof(l) == 'function') continue;
      if(ids[l.id]) res.push(l);
    }
    return res;
  },
  showSearchAll: function() {
    var allLabels = [], allNames = [], sc = [], es = this.allSearch || {};
    var scall = [
        ['Node Label','node.label','string',allLabels],
        ['Node Tooltip','node.tooltip','string',allNames],
        ['Node Parent','node.parentNode','string',allLabels,{exactMatch:true}],
        ['Node Size','node.size','number'],
        ['Node Width','node.width','number'],
        ['Node Height','node.height','number'],
        ['Node Rotate','node.rotate','number'],
        ['Node Shape','node.shape','string',this.canvas.shapes.sort(Ext.canvasXpress.utils.ciSort),{exactMatch:true}],
        ['Node Color','node.color','string'],
        ['Node Outline','node.outline','string'],
        ['Node OutlineWidth','node.outlineWidth','number'],
        ['Node ImagePath','node.imagePath','string'],
        ['Node Pattern','node.pattern','string'],
        ['Node From','node.from','custom',[this.nodeConnectEdge.createDelegate(this, 'from', true)]],
        ['Node To','node.to','custom',[this.nodeConnectEdge.createDelegate(this, 'to', true)]],
        ['Edge From','edge.id1','id'],
        ['Edge To','edge.id2','id'],
        ['Edge Width','edge.width','number'],
        ['Edge Linetype','edge.type','string',this.canvas.lines.sort(Ext.canvasXpress.utils.ciSort),{exactMatch:true}],
        ['Edge Color','edge.color','string']
      ], supported = {};
    var sf = function(attr, f, cb, inpset, set) {
      var res = [], attr1, type;
      if(attr.match(/^(node|edge)\.(.+)/))
      {
        type = RegExp.$1 + 's';
        attr1 = RegExp.$2;
      }
      if(!f)
      {
        var set1 = {};
        for(var i in set)
        {
          var l = set[i];
          if(typeof(l) == 'function') continue;
          set1[l.id] = 1;
        }
        f = function(val) { return set1[val] };
      }
      var objs = inpset? inpset : type == 'nodes'? this.canvas[type] : this.canvas.data[type];
      for(var i in objs)
      {
        var l = objs[i];
        if(typeof(l) == 'function') continue;
        var val = supported[attr]? l[attr1] : es.getVal? es.getVal(l, attr) : null;
        if(val && f(val)) res.push(l);
      }
      if(cb) cb(res);
    };

    this.showSearchWin(allLabels, allNames, sc, es, scall, supported, sf);
  },
  showSearchNodes: function() {
    var allLabels = [], allNames = [], sc = [], es = this.nodeSearch || {};
    var scall = [
        ['Label','label','string',allLabels],
        ['Tooltip','tooltip','string',allNames],
        ['Parent','parentNode','string',allLabels,{exactMatch:true}],
        ['Size','size','number'],
        ['Width','width','number'],
        ['Height','height','number'],
        ['Rotate','rotate','number'],
        ['Shape','shape','string',this.canvas.shapes.sort(Ext.canvasXpress.utils.ciSort),{exactMatch:true}],
        ['Color','color','string'],
        ['Outline','outline','string'],
        ['OutlineWidth','outlineWidth','string'],
        ['ImagePath','imagePath','string'],
        ['Pattern','pattern','string']
      ], supported = {};
    var sf = function(attr, f, cb, inpset) {
      var res = [], nodes = inpset? inpset : this.canvas.nodes;
      for(var i in nodes)
      {
        var l = nodes[i], val = supported[attr]? l[attr] :
                                es.getVal? es.getVal(l, attr) : null;
        if(val && f(val)) res.push(l);
      }
      if(cb) cb(res);
    };
    this.showSearchWin(allLabels, allNames, sc, es, scall, supported, sf);
  },
  showSearchEdges: function() {
    var allLabels = [], allNames = [], sc = [], es = this.edgeSearch || {};
    var scall = [
        ['From','id1','string',allLabels],
        ['To','id2','string',allLabels],
        ['Tooltip','tooltip','string',allNames],
        ['Width','width','number'],
        ['Linetype','type','string',this.canvas.lines.sort(Ext.canvasXpress.utils.ciSort),{exactMatch:true}],
        ['Color','color','string']
      ], supported = {};
    var sf = function(attr, f, cb, inpset) {
      var res = [], edges = inpset? inpset : this.canvas.data.edges;
      for(var i = 0; i < edges.length; i++)
      {
        var l = edges[i], val = supported[attr]? l[attr] :
                                es.getVal? es.getVal(l, attr) : null;
        if(val && f(val)) res.push(l);
      }
      if(cb) cb(res);
    };
    this.showSearchWin(allLabels, allNames, sc, es, scall, supported, sf);
  },
  editLegend: function(b, e, type, o) {
    var w = type == 'node'? new Ext.canvasXpress.nodeLegendDialog(o, this) :
            type == 'edge'? new Ext.canvasXpress.edgeLegendDialog(o, this) :
            new Ext.canvasXpress.textLegendDialog(o, this);
    w.show();
  },
  updateLegend: function() {
    this.legendChanged = true;
    if(!this.viewInfo) this.fireEvent('updatelegend', function(s) {
        if(s) this.legendChanged = false;
      }.createDelegate(this));
    this.toggleSaveNetworkBtn();
  },
  updateTag: function() {
    if(!this.viewInfo) this.fireEvent('updatetag', function(s) {
        if(s) this.tagChanged = false;
      }.createDelegate(this), true);
    this.toggleSaveNetworkBtn();
  },
  updateOrder: function() {
    this.orderChanged = true;
    if(!this.viewInfo) this.fireEvent('updateorder', function(s) {
        if(s) this.orderChanged = false;
      }.createDelegate(this));
    this.toggleSaveNetworkBtn();
  },
  deleteLegend: function(type, o) {
    if(type == 'node' || type == 'edge')
      this.canvas.data.legend[type + 's'] = [];
    else
    {
      var t = this.canvas.data.legend.text;
      for(var i = 0; i < t.length; i++)
        if(t[i] == o)
        {
          this.canvas.data.legend.text.splice(i, 1);
          break;
        }
    }
    this.updateLegend();
    this.canvas.draw();
  },
  deleteNode: function(n) {
    if(typeof(n) != 'object' || !n.length) n = [n];
    var str1 = n.length > 1? 'the selected nodes' : 'this node',
        str2 = n.length > 1? 'The selected nodes and all of their' : 'This node and all of its';
    Ext.Msg.confirm('Warning', 'Are you sure you want to delete '+str1+'? '+str2+' edges will be deleted!', function(b) {
      if(b == 'yes')
      {
        var f = function() {
          for(var i = 0; i < n.length; i++)
            this.canvas.removeNode(n[i]);
          this.canvas.draw();
          this.canvas.addToNetworkStack();
          this.updateOrder(); // change of # of node causes nodeIndices to change, it has to be consistent!!
        }.createDelegate(this);
        if(this.viewInfo) f();
        else
          this.fireEvent('removenode', n, f);
      }
    }, this)
  },
  deleteEdge: function(e) {
    Ext.Msg.confirm('Warning', 'Are you sure you want to delete this edge?', function(b) {
      if(b == 'yes')
      {
        var f = function() {
          this.canvas.removeEdge(e);
          this.canvas.draw();
        }.createDelegate(this);
        if(this.viewInfo) f();
        else this.fireEvent('removeedge', e, f);
      }
    }, this)
  },
  editNetwork: function(isSaveAs) {
    var d = new Ext.canvasXpress.networkDialog(this.networkInfo, this, isSaveAs === true);
    d.show();
  },
  removeNode: function(n) {
    if(typeof(n) != 'object' || !n.length) n = [n];
    var all = {};
    for(var i = 0; i < n.length; i++)
      all[n[i].id] = 1;
    for(var i = this.changedNodes.length-1; i >= 0; i--)
      if(all[this.changedNodes[i].id])
      {
        this.changedNodes.splice(i, 1);
        return;
      }
    this.toggleSaveNetworkBtn();
  },
  addNode: function(n) {
    if(typeof(n) != 'object' || !n.length) n = [n];
    var all = {};
    for(var i = 0; i < this.changedNodes.length; i++)
      all[this.changedNodes[i].id] = 1;
    for(var i = 0; i < n.length; i++)
      if(!all[n[i].id])
        this.changedNodes.push(n[i]);
    this.toggleSaveNetworkBtn();
  },
  removeEdge: function(id1, id2) {
    for(var i = 0; i < this.changedEdges.length; i++)
    {
      var e = this.changedEdges[i];
      if(e.id1 == id1 && e.id2 == id2)
      {
        this.changedEdges.splice(i, 1);
        return;
      }
    }
    this.toggleSaveNetworkBtn();
  },
  addEdge: function(ae) {
    for(var i = 0; i < this.changedEdges.length; i++)
    {
      var e = this.changedEdges[i];
      if(e.id1 == ae.id1 && e.id2 == ae.id2)
        return;
    }
    this.changedEdges.push(ae);
    this.toggleSaveNetworkBtn();
  },
  resetChanges: function() {
    this.changedNodes = [];
    this.changedEdges = [];
    this.removedNodes = [];
    this.removedEdges = [];
    this.legendChanged = false;
    this.orderChanged = false;
    this.networkChanged = false;
    this.tagChanged = false;
    this.toggleSaveNetworkBtn();
  },
  saveNow: function(force, opts) {
    var d = this.canvas.data, obj = {nodes:force?d.nodes:this.changedNodes, edges:force?d.edges:this.changedEdges};
    if(this.linksChanged || force) obj.links = d.links;
    if(this.legendChanged || force) obj.legend = d.legend;
    if(this.tagChanged || force) obj.taggedNodes = d.taggedNodes;
    if(this.orderChanged || force) obj.nodeIndices = d.nodeIndices;
    if(this.networkChanged || force) obj.info = this.networkInfo;
    obj.force = force;
    // handler should redraw entire canvas if nodes and/or edges were added
    this.fireEvent('saveallchanges', obj, this.resetChanges.createDelegate(this), opts);
  },
  saveMap: function(force, opts, warn) {
    if(this.viewInfo) return;
    if(warn) 
      Ext.Msg.confirm('Warning', 'Saving network now could cause a reload of network, which MAY result in any data overlay or other external features get lost. Do you still want to proceed to save the network?', function(b) {
        if(b == 'yes') this.saveNow(force, opts);
      }, this);
    else this.saveNow(force, opts);
  },
  encode: function(a) {
    return JSON && JSON.stringify? JSON.stringify(a, null, 2) : Ext.encode(a)
  },
  exchangeData: function(type, cxptest) {
    var win, a;
    if(type == 'ExportMovie' && !this.canvas.snapshots.length)
    {
      alert('No movie to export! Create/load a movie first before trying again!');
      return;
    }
    a = [type.match(/Movie/)? {base:this.canvas.snapshotsBase, slides:this.canvas.snapshots} : this.canvas.data,
         this.canvas.getUserConfig()];
    var config = {
      title: type + ' JSON Data',
      width: 500,
      height: 300,
      layout: 'fit',
      items: {
        items: {
          xtype: 'form',
          border: false,
          width: 480,
          style:'margin:5px',
          items: [
            {
              xtype: 'textarea', hideLabel: true, width: 473, height: type == 'Import'? 230 : 258,
              value: cxptest? 'new CanvasXpress( \'canvas\', ' + this.encode(a[0]) + ', ' + this.encode(a[1]) + ');' : (type.match(/Import/)? '' : this.encode(a))
            }
          ]
        }
      }
    };
    if(type.match(/Import/))
    {
      config.bbar = {
        items: [
          '->',
          {
            text: 'Submit',
            scope: this,
            handler: function(b, e) {
              var json = b.ownerCt.ownerCt.get(0).get(0).get(0).getValue();
              try
              {
                var tmp = Ext.decode(json);
                if(type.match(/Movie/))
                {
                  this.canvas.snapshots = tmp[0].slides;
                  this.canvas.snapshotsBase = tmp[0].base;
                  win.close();
                  alert('Imported! You can play the movie now.');
                  this.toggleSnapshotCtrl(b, e);
                  if(!this.snapshotCtrl.isVisible())
                    this.snapshotCtrl.show(null, this.setCtrlMode, this);
                }
                else this.updateNetwork(tmp, win);
              }
              catch(e)
              {
                Ext.Msg.alert('Error', e);
              }
            }
          }
        ]
      }
    }
    win = new Ext.Window(config);
    win.show();
  },
  updateNetwork: function(tmp, win) { // tmp is [data, options]
    this.canvas.updateConfig(tmp[1]);
    this.canvas.updateData(tmp[0]);
    this.fireEvent('importdata', tmp[0], tmp[1]); // data and userOptions
    if(win) win.close();
    Ext.Msg.show({
      title:'Very Important!!',
      msg: 'You <b>MUST</b> "force save the network" BEFORE anything else if you:<br><br>1. cannot see your imported network or only see a portion of it, OR,<br>2. need to add notes to any Pathway nodes/edges.',
      buttons: Ext.Msg.OK,
      icon: Ext.MessageBox.WARNING
    });
  },
  showEditSnapshot: function(snap, opts) {
    if(!opts) opts = {};
    var win, p = { action: opts.action || 'saveMovie', name: snap.name, type:'movie',
              id: snap && snap.id && (opts.action || opts.overwrite)? snap.id : null,
              pid:this.networkInfo.id, desc: snap.desc, shared: snap.shared? '1':'0',
              data: opts.action? null : Ext.encode(this.canvas.getSnapshotsData()) },
        f1 = function() {
          Ext.Ajax.request({
            url: this.movieURL,
            params: p,
            scope: this,
            callback: function(o, s, r) {
              if(!s) Ext.Msg.alert('Error', 'Could not save movie to server!');
              else
              {
                var res = Ext.decode(r.responseText);
                if(res.error)
                {
                  Ext.Msg.alert('Error', res.error);
                  return;
                }
                snap.id = res.id;
                if(win) win.close();
                if(opts.win) opts.win.close();
                if(opts.xy) this.showSnapshotResultTip('Snapshots saved to DB', opts.xy);
              }
            }
          });
          this.snapshotsInfo = {name:p.name, desc:p.desc, shared:p.shared}; // this.snapshotsInfo
        }.createDelegate(this);
    if(opts.overwrite)
    {
      f1();
      return;
    }
    win = new Ext.Window({
      title: opts.title || 'Store Current Snapshots to Database',
      width: 300,
      height: 200,
      layout: 'fit',
      items: {
        items: {
          xtype: 'form',
          border: false,
          defaultType: 'textfield',
          width: 280,
          labelWidth: 80,
          style:'margin:5px',
          items: [
            { fieldLabel: 'Name', value: snap? snap.name : '' },
            { fieldLabel: 'Description', value: (snap? snap.desc : ''), width: 170 },
            { xtype: 'checkbox', hideLabel: true, boxLabel: 'Shared with everyone?', checked: (snap? snap.shared : false) }
          ]
        }
      },
      bbar: {
        items: [
          '->',
          {
            text: opts.bText || 'Save to DB Now',
            scope: this,
            handler: function(b, e) {
              var f = b.ownerCt.ownerCt.get(0).get(0), name = f.get(0).getValue(),
                  desc = f.get(1).getValue(), shared = f.get(2).getValue();
              if(!name)
              {
                Ext.Msg.alert('Error', 'Name must be filled out!');
                return;
              }
              p.name = name;
              p.desc = desc;
              p.shared = shared? '1':'0';
              f1();
            }
          }
        ]
      }
    });
    win.show();
  },
  storeSnapshot: function(xy) {
    if(this.movieURL)
    {
      this.canvas.stopSnapshotPlay(true);
      var f = function(o) {
        this.showEditSnapshot(this.snapshotsInfo, {xy:xy,overwrite:o});
      }.createDelegate(this);
      if(this.snapshotsInfo.id)
      {
        Ext.Msg.show({
          title:'Warning',
          msg: 'Are you sure you want to save the movie now and overwrite the version stored on server? Press "No" to save as a new movie instead, or press "Cancel" to not save at this time.',
          icon: Ext.MessageBox.QUESTION,
          buttons: Ext.Msg.YESNOCANCEL,
          scope: this,
          fn: function(t) {
            if(t == 'yes') f(true)
            else if(t == 'no') f();
          }
        });
      }
      else f();
    }
  },
  warnViewSave: function() {
    return 'Press "Yes" to <b>discard</b> the unsaved changes, OR press "No" below, then ' + (this.viewInfo? 'use the View Menu\'s "Save Current View" to save.' : 'either enter superuser mode or press save button to save.');
  },
  revertView: function(force) {
    if(this.mainView)
    {
      var f = function() {
        this.canvas.updateConfig(this.mainView.opts);
        this.canvas.updateData(this.mainView.data);
        delete this.viewInfo;
        delete this.mainView;
        var btn = this.getTopToolbar().getComponent('views');
        btn.setIcon(this.imgDir + 'picture.png');
        btn.setTooltip('View Menu (currently in original view)');
        this.resetChanges();
        this.viewStateChange();
      }.createDelegate(this);
      if(force !== 1 && this.hasUnsavedChanges() && this.hasListener('saveallchanges'))
        Ext.Msg.confirm('Warning!', this.warnViewSave(), function(b) {
          if(b == 'yes') f()
        }, this);
      else f();
    }
  },
  viewStateChange: function(inView) {
    var btns = this.getTopToolbar().getComponent('editmenu').menu.find('itemId', 'editnet'), btn = btns && btns.length? btns[0] : null;
    if(btn)
    {
      if(inView) btn.disable();
      else btn.enable();
    }
    if(this.viewStateItems)
      for(var i = 0; i < this.viewStateItems.length; i++)
      {
        btn = this.getTopToolbar().getComponent(this.viewStateItems[i]);
        if(btn)
        {
          if(inView) btn.disable();
          else btn.enable();
        }
      }
  },
  loadView: function(view) {
    var f = function() {
      this.showMask("Loading view from server...");
      Ext.Ajax.request({
        url: this.movieURL,
        params: {pid:this.networkInfo.id, id:view.id, action:'loadMovie', type:'view'},
        scope: this,
        callback: function(o, s, re) {
          this.hideMask();
          var res = (s && re && re.responseText)? Ext.decode(re.responseText) : null;
          if(!res || res.error) Ext.Msg.alert('Error', res && res.error? res.error : 'Could not load from server!');
          else
          {
            if(!this.viewInfo)
              this.mainView = {data:this.canvas.data,opts:this.canvas.getUserConfig()};
            var d = Ext.decode(res.data);
            this.canvas.updateConfig(d.opts);
            this.canvas.updateData(d.data);
            this.viewInfo = view;
            var btn = this.getTopToolbar().getComponent('views');
            btn.setIcon(this.imgDir + 'picture_red.png');
            btn.setTooltip('View Menu (currently in view "'+view.name+'")');
            this.resetChanges();
            this.viewStateChange(true);
          }
        }
      });
    }.createDelegate(this);
    if(this.hasUnsavedChanges() && this.hasListener('saveallchanges'))
      Ext.Msg.confirm('Warning!', this.warnViewSave(), function(b) {
        if(b == 'yes') f();
      }, this);
    else f();
  },
  deleteView: function(id) {
    this.showMask("Removing view from server...");
    Ext.Ajax.request({
      url: this.movieURL,
      params: {pid:this.networkInfo.id, id:id, action:'deleteMovie'},
      scope: this,
      callback: function(o, s, re) {
        this.hideMask();
        var res = (s && re && re.responseText)? Ext.decode(re.responseText) : null;
        if(!res || res.error) Ext.Msg.alert('Error', res && res.error? res.error : 'Could not load from server!');
        else
        {
          for(var i = 0; i < this.pViews.length; i++)
            if(this.pViews[i].id == id)
              this.pViews.splice(i, 1);
          delete this.viewInfo;
          this.revertView(1);
        }
      }
    });
  },
  editView: function(view, win) {
    if(this.networkInfo && this.networkInfo.user && this.networkInfo.user != view.user)
    {
      Ext.Msg.alert('Error', 'You are only allowed to edit your own views!');
      return;
    }
    this.showEditView({id:view.id,name:view.name,shared:view.shared,desc:view.desc},
      {action:'editMovie', title: 'Change View Info', bText: 'Save New Info to DB Now', win: win});
  },
  showEditView: function(view, opts) {
    if(!opts) opts = {};
    if(!view) view = {};
    var win, p = { action: opts.action || 'saveMovie', name: view.name, type:'view',
              id: view && view.id && (opts.action || opts.overwrite)? view.id : null,
              pid:this.networkInfo.id, desc: view.desc, shared: view.shared? '1':'0',
              data: opts.action? null : Ext.encode({data:this.canvas.data,opts:this.canvas.getUserConfig()}) },
        f1 = function() {
          Ext.Ajax.request({
            url: this.movieURL,
            params: p,
            scope: this,
            callback: function(o, s, r) {
              if(!s) Ext.Msg.alert('Error', 'Could not save view to server!');
              else
              {
                var res = Ext.decode(r.responseText);
                if(res.error)
                {
                  Ext.Msg.alert('Error', res.error);
                  return;
                }
                if(!this.viewInfo)
                  this.mainView = {data:this.canvas.data,opts:this.canvas.getUserConfig()};
                this.viewInfo = {name:p.name, desc:p.desc, shared:p.shared, id:res.id, user:this.networkInfo && this.networkInfo.user? this.networkInfo.user : null};
                // now add the view to the list
                if(!this.pViews) this.pViews = [];
                var found = false;
                for(var i = 0; i < this.pViews.length; i++)
                  if(this.pViews[i].id == this.viewInfo.id)
                  {
                    this.pViews.splice(i, 1, this.viewInfo);
                    found = true;
                  }
                if(!found) this.pViews.push(this.viewInfo);
                if(win) win.close();
                if(opts.win) opts.win.close();
                var btn = this.getTopToolbar().getComponent('views');
                btn.setIcon(this.imgDir + 'picture_red.png');
                btn.setTooltip('View Menu (currently in view "'+p.name+'")');
                if(opts.overwrite) Ext.Msg.alert('Done', 'View saved to server!');
                else if(!p.id) Ext.Msg.alert('Done', 'View saved to server. Note that now you are viewing/editing the view you just saved.');
                this.resetChanges();
                this.viewStateChange(true);
              }
            }
          });
        }.createDelegate(this);
    if(opts.overwrite)
    {
      f1();
      return;
    }
    win = new Ext.Window({
      title: opts.title || 'Store Current View to Database',
      width: 300,
      height: 200,
      layout: 'fit',
      items: {
        items: {
          xtype: 'form',
          border: false,
          defaultType: 'textfield',
          width: 280,
          labelWidth: 80,
          style:'margin:5px',
          items: [
            { fieldLabel: 'Name', value: view? view.name : '' },
            { fieldLabel: 'Description', value: (view? view.desc : ''), width: 170 },
            { xtype: 'checkbox', hideLabel: true, boxLabel: 'Shared with everyone?', checked: (view? view.shared : false) }
          ]
        }
      },
      bbar: {
        items: [
          '->',
          {
            text: opts.bText || 'Save to DB Now',
            scope: this,
            handler: function(b, e) {
              var f = b.ownerCt.ownerCt.get(0).get(0), name = f.get(0).getValue(),
                  desc = f.get(1).getValue(), shared = f.get(2).getValue();
              if(!name)
              {
                Ext.Msg.alert('Error', 'Name must be filled out!');
                return;
              }
              p.name = name;
              p.desc = desc;
              p.shared = shared? '1':'0';
              f1();
            }
          }
        ]
      }
    });
    win.show();
  },
  storeView: function(force) {
    if(this.movieURL)
    {
      var f = function(o) {
        this.showEditView(this.viewInfo, {overwrite:o});
      }.createDelegate(this);
//       if(this.viewInfo && force !== true)
//       {
//         Ext.Msg.show({
//           title:'Warning',
//           msg: 'Are you sure you want to save the view now and overwrite the version stored on server? Press "No" to save as a new view instead, or press "Cancel" to not save at this time.',
//           icon: Ext.MessageBox.QUESTION,
//           buttons: Ext.Msg.YESNOCANCEL,
//           scope: this,
//           fn: function(t) {
//             if(t == 'yes') f(true)
//             else if(t == 'no') f();
//           }
//         });
//       }
//       else
      if(this.viewInfo && force === true) f(true);
      else f();
    }
  },
  toggleSnapshotCtrl: function(b, e) {
    if(!this.snapshotCtrl)
    {
      var items = [
        {
          icon: this.imgDir + 'eye.png',
          itemId: 'snap',
          tooltip: 'Take A Snapshot'
        },
        {
          itemId: 'play',
          menu: [
            {
              text: 'Custom or Default',
              handler: this.playSnapshot.createDelegate(this, [0], true)
            },
            {
              text: 'Every 50ms',
              handler: this.playSnapshot.createDelegate(this, [50], true)
            },
            {
              text: 'Every 200ms',
              handler: this.playSnapshot.createDelegate(this, [200], true)
            },
            {
              text: 'Every 500ms',
              handler: this.playSnapshot.createDelegate(this, [500], true)
            },
            {
              text: 'Every 1s',
              handler: this.playSnapshot.createDelegate(this, [1000], true)
            },
            {
              text: 'Every 2s',
              handler: this.playSnapshot.createDelegate(this, [2000], true)
            },
            {
              text: 'Every 5s',
              handler: this.playSnapshot.createDelegate(this, [5000], true)
            }
          ]
        },
        {
          icon: this.imgDir + 'stop.png',
          itemId: 'stop',
          tooltip: 'Stop Playing Snapshots'
        },
        {
          icon: this.imgDir + 'rewind.png',
          itemId: 'prev',
          tooltip: 'Previous Snapshot'
        },
        {
          icon: this.imgDir + 'fast_forward.png',
          itemId: 'next',
          tooltip: 'Next Snapshot'
        },
        {
          icon: this.imgDir + 'copy.png',
          itemId: 'copy',
          tooltip: 'Duplicate Current Snapshot'
        },
        {
          icon: this.imgDir + 'arrow_left.png',
          itemId: 'left',
          tooltip: 'Shift This Snapshot Left'
        },
        {
          icon: this.imgDir + 'arrow_right.png',
          itemId: 'right',
          tooltip: 'Shift This Snapshot Right'
        },
        {
          icon: this.imgDir + 'clock.png',
          itemId: 'clock',
          tooltip: 'Change Display Length of This Snapshot'
        },
        {
          icon: this.imgDir + 'edit.png',
          itemId: 'edit',
          tooltip: 'Save Edits for This Snapshot'
        },
        {
          icon: this.imgDir + 'delete.png',
          itemId: 'delete',
          tooltip: 'Delete Current Snapshots'
        }
      ];
      if(this.movieURL)
        items.push(
          {
            icon: this.imgDir + 'turn_left.png',
            tooltip: 'Load Snapshots from DB',
            itemId: 'load'
          },
          {
            icon: this.imgDir + 'save.png',
            tooltip: 'Store Snapshots to DB',
            itemId: 'store'
          });
      items.push({
        icon: this.imgDir + 'help.png',
        itemId: 'help',
        handler: null
      });
      var xy = e.xy? e.xy : e;
      this.snapshotCtrl = new Ext.Window({
        height: 16,
        width: 297 + (this.movieURL? 42 : 0),
        x: xy[0],
        y: xy[1],
        border: false,
        closable: false,
        resizable: false,
        constrainHeader: true,
        bbar: new Ext.Toolbar({
          defaults: {
            xtype: 'button',
            scope: this,
            handler: this.playSnapshot
          },
          items: items
        }),
        listeners: {
          scope: this,
          render: function() {
            this.snapshotCtrl.header.on('dblclick', function(e) {
              var f = function() {
                this.setCtrlMode();
                this.showSnapshotResultTip('Demo Animation Loaded, Press the play button above to play', [xy[0] + 25, xy[1] + 15]);
              }.createDelegate(this);
              Ext.Msg.prompt('Demo Animation', 'Enter 1 for Ball Demo, 2 for Stars Demo:',
                function(b, t) {
                  if(b == 'ok')
                  {
                    if(t == 1)
                    {
                      this.canvas.createDemoNetworkAnimation1(); // falling ball only works nicely for small canvas
                      f();
                    }
                    else
                    {
                      Ext.Msg.prompt('Stars Demo Animation', 'Proceed to create a demo animation of following number of nodes?<br>\nImportant: demo animation will replace the current snapshots!',
                        function(b, t) {
                          if(b == 'ok')
                            if(t.match(/^\s*\d+\s*$/))
                            {
                              this.canvas.createDemoNetworkAnimation(t-0);
                              f();
                            }
                            else Ext.Msg.alert('Error', 'Node number must be natural number!');
                        }, this, false, '100');
                    }
                  }
                }, this, false, '1');
            }.createDelegate(this));
          }
        }
      });
    }
    if(this.snapshotCtrl.isVisible()) this.snapshotCtrl.hide();
    else this.snapshotCtrl.show(null, this.setCtrlMode, this);
  },
  enableCtrlButton: function(name, enabled) {
    var button = this.snapshotCtrl.getBottomToolbar().getComponent(name);
    if(button) button.setDisabled(!enabled);
  },
  changePlayIcon: function(name) {
    var button = this.snapshotCtrl.getBottomToolbar().getComponent('play');
    if(name == 'play')
    {
      button.setIcon(this.imgDir + 'play.png');
      button.setTooltip('Play Current Snapshots');
      button.setHandler(null);
    }
    else
    {
      button.setIcon(this.imgDir + 'pause.png');
      button.setTooltip('Pause Snapshot Play');
      button.setHandler(this.playSnapshot.createDelegate(this));
    }
  },
  setCtrlMode: function(mode) {
    this.enableCtrlButton('next', this.canvas.hasNextSnapshot());
    this.enableCtrlButton('prev', this.canvas.hasPrevSnapshot());
    this.enableCtrlButton('left', this.canvas.snapshotPaused && this.canvas.hasPrevSnapshot());
    this.enableCtrlButton('right', this.canvas.hasNextSnapshot());
    this.enableCtrlButton('copy', this.canvas.snapshots && this.canvas.snapshots.length);
    this.changePlayIcon((this.canvas.snapshotPaused || !this.canvas.snapshotPlay)? 'play' : 'pause');
    this.enableCtrlButton('play', this.canvas.snapshots && this.canvas.snapshots.length > 1);
    this.enableCtrlButton('stop', this.canvas.snapshotPlay);
    this.enableCtrlButton('clock', this.canvas.snapshotPlay);
    this.enableCtrlButton('edit', this.canvas.snapshotPlay);
    this.enableCtrlButton('store', this.canvas.snapshots && this.canvas.snapshots.length > 1);
    this.enableCtrlButton('delete', this.canvas.snapshots && this.canvas.snapshots.length);
    this.enableCtrlButton('snap', !this.canvas.snapshotPlay && !this.canvas.snapshotPaused);
    var button = this.snapshotCtrl.getBottomToolbar().getComponent('help'), msg;
    if(this.canvas.snapshotPlay)
    {
      if(this.canvas.snapshotPaused)
        msg = 'Playing paused on snapshot #' + this.canvas.snapshotPlay.idx;
      else
        msg = 'Currently playing snapshots at ' + (this.canvas.snapshotPlay.time || 'default') + ' ms/snapshot';
    }
    else if(this.canvas.snapshots && this.canvas.snapshots.length > 1)
      msg = 'Has '+this.canvas.snapshots.length+' snapshots. Press the play button to view animation.';
    else if(this.canvas.snapshots && this.canvas.snapshots.length)
      msg = 'Has only one snapshot, one more needed for animation.';
    else
      msg = 'Has no snapshot yet. Animation requires at least 2 snapshots.';
    button.setTooltip(msg);
  },
  showSnapshotResultTip: function(msg, xy) {
    var tip = new Ext.QuickTip({ title: msg, dismissDelay: 3000 });
    tip.showAt([xy[0], xy[1] + 20]);
  },
  playSnapshot : function(b, e, time) {
    if(b.text && b.text.match(/Every \d+|Custom or Default/))
    {
      if(!time)
      {
        Ext.Msg.prompt('Please enter the speed', 'Please enter the speed at which you would like the animation played (in milliseconds!)', function(btn, txt) {
          if(btn == 'ok')
          {
            if(!isNaN(txt) && txt > 0)
              this.canvas.playSnapshot(txt - 0);
            else
              Ext.Msg.alert('Error', txt + ' is not a positive number!');
          }
        }, this, false, 20);
      }
      else this.canvas.playSnapshot(time);
    }
    else
    {
      var msg;
      switch (b.itemId)
      {
        case 'stop': this.canvas.stopSnapshotPlay(true); break;
        case 'play':
          if(this.canvas.snapshotPlay) // paused
          {
            if(b.hasVisibleMenu()) b.hideMenu();
            this.canvas.pauseSnapshot();
          }
          break;
        case 'delete':
          this.canvas.clearSnapshot();
          delete this.snapshotsInfo;
          break;
        case 'edit':
          this.canvas.updateSnapshot();
          msg = 'Edits saved';
          break;
        case 'clock':
          Ext.Msg.prompt('Change snapshot timing', "Please set how long (in milliseconds) you'd like this snapshot displayed during playing mode",
            function(b, t) {
              if(b == 'ok')
              {
                if(t.match(/^\s*\d+\s*$/)) this.canvas.setSnapshotTime(t - 0);
                else Ext.Msg.alert('Error!', 'Snapshot display time must be natural numbers!');
                msg = 'Changed, but you still need to save the edit!';
              }
            }, this, false, this.canvas.getSnapshotTime())
          break;
        case 'snap': this.canvas.saveSnapshot(); break;
        case 'next': this.canvas.nextSnapshotOnce(); break;
        case 'prev': this.canvas.prevSnapshotOnce(); break;
        case 'load': this.loadSnapshotList(e.xy); break;
        case 'store': this.storeSnapshot(e.xy); break;
        case 'copy': this.canvas.duplicateSnapshot(); break;
        case 'left': this.canvas.moveSnapshot(-1); break;
        case 'right': this.canvas.moveSnapshot(1); break;
      }
      if(msg) this.showSnapshotResultTip(msg, e.xy);
    }
    this.setCtrlMode();
  },
  loadSnapshotList: function(xy) {
    if(!this.movieURL) return;
    this.showMask();
    Ext.Ajax.request({
        url: this.movieURL,
        params: { action:'listMovie', id:this.networkInfo.id },
        scope: this,
        callback: function(o, s, r) {
          this.hideMask();
          if(!s) Ext.Msg.alert('Error', 'Could not load snapshot list server!');
          else
          {
            var list = Ext.decode(r.responseText);
            // array of username, name, description, movieid, shared
            var win = new Ext.canvasXpress.SnapshotManager({list:list, xy:xy}, this);
            win.show();
          }
        }
      });
  },
  showMask: function(msg) // pass in msg to override default msg
  {
    var defaultMsg = 'Loading ...';
    if(!this.mask) this.mask = new Ext.LoadMask(this.id, {msg:msg || defaultMsg});
    else if(!msg) this.mask.msg = defaultMsg;
    else if(msg) this.mask.msg = msg;
    this.mask.show();
  },
  hideMask: function()
  {
    if(this.mask) this.mask.hide();
  },
  setComboMouse: function(c) {
    var self = this;
    if(!c.mouseoverAdded)
    {
      c.list.on('mouseover', function(e, item, value) {
        var idstr = item.getAttribute('ext:qtip');
        if(!idstr) return;
        var s = idstr.split(':'), id = s[1] - 0, cv = self.canvas;
        cv.highlightNodes([id]);
      });
      c.list.on('mouseout', function(e, item, value) {
        var idstr = item.getAttribute('ext:qtip');
        if(!idstr) return;
        var s = idstr.split(':'), id = s[1] - 0, cv = self.canvas;
        cv.unHighlightNodes([id]);
      });
      c.mouseoverAdded = true;
    }
  }
});
Ext.canvasXpress.utils = {
  ciSort: function(a,b) {
    var aa = (a == null? '' : a+'').toLowerCase(), bb = (b == null? '' : b+'').toLowerCase();
    return aa>bb?1:aa<bb?-1:0
  },
  clone: function(obj) { // from CXP's own cloneObj
    if(obj == null || typeof (obj) != 'object') {
      return obj;
    }
    var temp = new obj.constructor();
    for(var key in obj) {
      temp[key] = Ext.canvasXpress.utils.clone(obj[key]);
    }
    return temp;
  },
  getHiddenNodes: function(extCanvas) {
    var hidden = extCanvas.canvas.getHiddenNodes(), hn = [{
      icon: extCanvas.imgDir + 'eye.png',
      text: 'All Hidden Nodes',
      handler: extCanvas.toggleNode.createDelegate(extCanvas, [null, false])
    }];
    for(var i = 0; i < hidden.length; i++)
    {
      var sn = hidden[i];
      hn.push({
        icon: extCanvas.imgDir + 'eye.png',
        text: extCanvas.getName(sn),
        handler: extCanvas.toggleNode.createDelegate(extCanvas, [sn, false])
      });
    }
    return hn;
  },
  removeNode: function(all, id) {
    for(var i = 0; i < this.changedNodes.length; i++)
    {
      if(this.changedNodes[i].id == n.id)
      {
        this.changedNodes[i].splice(i, 1);
        return;
      }
    }
    this.toggleSaveNetworkBtn();
  },
  selectLabel: function(extCanvas, key) {
    return {
      xtype: 'label',
      text: 'Click to select',
      style: 'text-decoration:underline;color:blue;cursor:hand;cursor:pointer;',
      listeners: {
        render: function(l) {
          l.el.dom.title = 'First click here, then click the node you want. To change selection, repeat this process.';
          l.el.on('click', function() {
            extCanvas.selectNode(function(node) {
              l.previousSibling().setValue(key && typeof(key) == 'function'? key(node) : node[key && typeof(key) == 'string'? key : 'id']);
            });
          });
        }
      }
    }
  },
  customPanel: function(ps, ed, isMulti) {
    // it's nicer to remove the item instead of hiding them (due to sending data AND the looks due to margin)
    for(var i = 0; i < ps.length; i++)
    {
      var a = ps[i].items;
      for(var j = a.length - 1; j >= 0; j--)
        if(a[j].hidden) a.splice(j, 1);
        else if(isMulti) a[j].allowBlank = true;
    }
    if(ed.panels)
    {
      for(var i = 0; i < ed.panels.length; i++)
      {
        var p = ed.panels[i];
        if(isMulti)
        {
          var a = p.items;
          for(var j = a.length - 1; j >= 0; j--)
            a[j].allowBlank = true;
        }
        ps.push(p);
      }
    }
    if(ed.addToPanels)
    {
      for(var i = 0; i < ed.addToPanels.length; i++)
      {
        var o = ed.addToPanels[i];
        if(isMulti) o.allowBlank = true;
        ps[o.pidx || 0].items.push(o);
      }
    }
  },
  renderPanels: function(tab) { // make sure all tabs are rendered
    var active = tab.getActiveTab();
    for(var i = 0; i < tab.items.getCount(); i++)
    {
      var p = tab.get(i);
      if(!p.rendered) // we need to render it otherwise af.getValues() will break
        tab.activate(p); // render() should not and does not work
    }
    tab.activate(active);
  },
  resetForms: function(tab) {
    for(var i = 0; i < tab.items.getCount(); i++)
    {
      var p = tab.get(i);
      if(p.isXType('form'))
        p.getForm().reset();
    }
  },
  callback: function(cb, args) {
    if(cb.fn) cb.fn.apply(cb.scope || this, args);
    else cb.apply(this, args);
  },
  addMenu: function(items, o, f, scope) {
    if(f)
    {
      var mi = f.call(scope, o);
      if(mi)
        for(var i = 0; i < mi.length; i++)
          items.push(mi[i]);
    }
  },
  gridToLegend: function(extCanvas, grid, type) {
    extCanvas.canvas.data.legend[type] = [];
    var t = grid.getStore().data.items, l = extCanvas.canvas.data.legend[type];
    for(var i = 0; i < t.length; i++)
      l.push(t[i].data);
  },
  initLegend: function(type) {
//     var xy = this.extCanvas.canvas.adjustedCoordinates(this.extCanvas.clickXY);
    var xy = this.extCanvas.canvas.findXYCoordinates(this.extCanvas.clickXY);
    if(!this.extCanvas.canvas.data.legend)
      this.extCanvas.canvas.data.legend = {nodes:[],edges:[]};
    if(!this.extCanvas.canvas.data.legend[type])
      this.extCanvas.canvas.data.legend[type] = [];
    if(!this.extCanvas.canvas.data.legend.pos)
      this.extCanvas.canvas.data.legend.pos = {type:xy};
    else if(!this.extCanvas.canvas.data.legend.pos[type] || this.extCanvas.canvas.data.legend.pos[type].x === false)
      this.extCanvas.canvas.data.legend.pos[type] = xy;
  },
  updateLegend: function(extCanvas, type) {
    if(!extCanvas.canvas.data.legend[type].length)
      extCanvas.canvas.data.legend.pos[type] = {x:false,y:false};
    extCanvas.updateLegend();
    extCanvas.canvas.draw();
  }
};
Ext.canvasXpress.customShapeDialog = Ext.extend(Ext.Window, {
  // config MUST have a valid service url!!!!
  // it also should have a valField so when user selects an image, the image url is input in there
  constructor: function(config) {
    this.allShapes = {};
    Ext.apply(this, config, {
      width: 625,
      height: 450,
      layout: 'fit',
      title: 'Custom Shape Dialog',
      items: {
        xtype: 'tabpanel',
        activeItem: 0,
        items: [
          {
            title: 'Create Shape',
            xtype: 'form',
            labelWidth: 80,
            items: [
              { xtype: 'textfield', labelStyle: 'margin-top:5px;margin-left:5px', style:'margin-top:5px;', fieldLabel: 'Name', allowBlank: false, name:'name', width: 200 },
              { xtype: 'checkbox', style: 'margin-left:5px', hideLabel: true, boxLabel: 'Convert white background to transparent? (recommended for Powerpoint objects!)', name:'trans', checked:true },
              { xtype: 'checkbox', style: 'margin-left:5px', hideLabel: true, boxLabel: 'Share this shape with others?', name:'shared', checked:true },
              { xtype: 'label', text:'Please paste one image URL or image itself or PowerPoint object below:', style:'font-size:12px;margin-top:5px;margin-left:5px;' },
              {
                width: 573,
                height: 260,
                style: 'margin-left:5px;margin-top:5px',
                html: '<iframe width=565 height=275 frameborder=0></iframe>',
                listeners: {
                  scope: this,
                  afterlayout: function(p) {
                    setTimeout(function() {
                      this.editorDoc = this.getIFrameDoc();
                      this.editorDoc.designMode = "On"; // Google Chrome has a bug that disabled ALL image/obj pasting in designMode or contenteditable in Chrome  18 and 19 at least. They haven't fixed it in months. Hope they fix it soon as there's no easy workaround - even in Google mail they fake it by sending data to server to create URL instead of properly do a base64 encode! Unbelievable.
                    }.createDelegate(this), 500);
                  }
                }
              }
            ],
            bbar: [
              '->',
              {
                text: 'Switch Back to Create Mode',
                scope: this,
                disabled: true,
                handler: function(b, e) {
                  var panel = this.items.items[0].items.items[0], items = panel.items.items;
                  items[0].setValue('');
                  items[1].setValue(true);
                  items[2].setValue(true);
                  panel.dbid = undefined;
                  panel.setTitle('Create Shape');
                  var doc = this.getIFrameDoc(), cs = this.allShapes[id], bar = Ext.getCmp(panel.bbar.dom.children[0].id);
                  b.nextSibling().setText('Create Now');
                  doc.body.textContent = '';
                  b.disable();
                }
              },
              {
                text: 'Create Now',
                scope: this,
                handler: function(b, e) {
                  if(this.extCanvas.customShapesURL)
                  {
                    var tabs = this.items.items[0], panel = tabs.items.items[0], vals = panel.getForm().getValues();
                    var doc = this.getIFrameDoc(), images = doc.images, text = doc.body.textContent;
                    if(!vals.name || !((images && images.length) || (text.match(/^http:\/\//))))
                    {
                      Ext.Msg.alert('Error', 'Both image (or valid URL) and name are required!');
                      return;
                    }
                    else if(images.length > 1)
                    {
                      Ext.Msg.alert('Error', 'Only one image can be saved at a time. Please remove the extra image(s) before proceeding again.');
                      return;
                    }
                    // check name of the shape (needs to be unique)
                    var alls = [], dup, shared = vals.shared == 'on'? '1':'0', n = vals.name, id = panel.dbid;
                    if(config.myShapes)
                      for(var i = 0; i < config.myShapes.length; i++)
                        alls.push(config.myShapes[i]);
                    if(config.sharedShapes)
                      for(var i = 0; i < config.sharedShapes.length; i++)
                        alls.push(config.sharedShapes[i]);
                    if(alls)
                      for(var i = 0; i < alls.length; i++)
                      {
                        var s = alls[i];
                        if(s.name == n)
                        {
                          dup = s;
                          break;
                        }
                      }
                    if(dup)
                    {
                      Ext.Msg.alert('Error - Cannot Save Image!!', 'The name of your new image conflict with the name of this image:<br><img style="width:150px;height:100px;" src="'+dup.url+'">');
                      return;
                    }
                    if(!text)
                    {
                      try {
                        var tmp = images[0].src;
                        if(/^(https?|ftp):/.test(tmp))
                        {
                          Ext.Msg.alert('Error - Cannot Save Image!!', 'You probably pasted an image you obtained from the Internet using "Copy Image" option in Firefox context menu. This is not supported.<br><br>Instead, you should use the "Copy Image", then paste into PowerPoint or any image editor like IrfanView, GIMP, Photoshop, then copy the image out again and paste into the shape area.  This one extra copy/paste will get around multiple technical issues.');
                          return;
                        }
                      } catch(e) {}
                    }
                    this.mask.show();
                    Ext.Ajax.request({
                      url: this.extCanvas.customShapesURL,
                      params: { shared: shared, trans: vals.trans == 'on'? 1:0, name: n, id:id,
                        image: images && images.length? images[0].src : '', url: text.match(/^((?:https?|ftp):\/\/.+?)(?:[\n\r]|$)/)? RegExp.$1 : '',
                        action: 'createShape' },
                      scope: this,
                      callback: function(o, s, r) {
                        this.mask.hide();
                        if(!s) Ext.Msg.alert('Error', 'Could not save the image to server!');
                        else
                        {
                          var res = Ext.decode(r.responseText);
                          if(res.url) // just created
                          {
                            this.allShapes[res.id] = {shared:shared, name:n, id:res.id, url:res.url, editable:true, user:res.user};
                            if(!id)
                            {
                              tabs.setActiveTab(1);
                              var table = tabs.items.items[1].el.child('table').dom, rows = table.rows;
                              // create and add the element
                              var tr = rows[rows.length-1];
                              if(tr.cells.length >= 3)
                              {
                                tr = document.createElement('tr');
                                table.tBodies[0].appendChild(tr);
                              }
                              var td = document.createElement('td');
                              tr.appendChild(td);
                              td.innerHTML = this.getImgHtml(res.id, res.url, vals.name, true);
                            }
                            else
                            {
                              var table = tabs.items.items[1].el.child('table').dom, rows = table.rows, found = false;
                              for(var i = 0; i < rows.length; i++)
                              {
                                for(var j = 0; j < rows[i].cells.length; j++)
                                {
                                  var td = rows[i].cells[j];
                                  if(td.innerHTML && td.childNodes[0].childNodes[1].childNodes[1].childNodes[0].getAttribute('dbid') == id)
                                  {
                                    td.childNodes[0].childNodes[1].childNodes[0].textContent = n;
                                    found = true;
                                    break;
                                  }
                                }
                                if(found) break;
                              }
                            }
                          }
                          else
                            Ext.Msg.alert('Error', res.error)
                        }
                      }
                    });
                  }
                }
              }
            ]
          },
          {
            title: 'My Shapes',
            autoScroll: true,
            html: this.assembleHtml(config.myShapes, true),
            bbar: [
              '->',
              {
                text: 'Reset Search',
                scope: this,
                handler: this.resetSearch
              },
              {
                text: 'Search',
                scope: this,
                handler: this.search
              }
            ]
          },
          {
            title: 'Shared Shapes',
            autoScroll: true,
            html: this.assembleHtml(config.sharedShapes, false),
            bbar: [
              '->',
              {
                text: 'Reset Search',
                scope: this,
                handler: this.resetSearch
              },
              {
                text: 'Search',
                scope: this,
                handler: this.search
              }
            ]
          }
        ]
      }
    });
    Ext.canvasXpress.customShapeDialog.superclass.constructor.apply(this);
  },
  resetSearch: function(b, e) {
    var names = b.ownerCt.ownerCt.el.select('div.shapenamefound').elements;
    for(var i = 0; i < names.length; i++)
      names[i].className = 'shapename';
  },
  search: function(b, e) {
    Ext.Msg.prompt('Enter Search Term', 'Please enter the search term. RegExp is supported!', function(cb, text) {
      if(cb == 'ok' && text)
      {
        var re = new RegExp(text, 'i'), ele;
        var panel = b.ownerCt.ownerCt, names = panel.el.select('div.shapename').elements;
        for(var i = 0; i < names.length; i++)
        {
          if(names[i].textContent.match(re))
          {
            if(!ele) ele = names[i];
            if(!names[i].className.match(/shapenamefound/))
              names[i].className += ' shapenamefound';
          }
          else names[i].className = 'shapename';
        }
        if(ele) Ext.get(ele.parentNode.parentNode).scrollIntoView(panel.body);
      }
    }, this);
  },
  getImgHtml: function(id, url, name, editable) {
    var idstr = ' dbid="' + id + '"';
    return '<div style="width:180px;height:150px;text-alignment:center;padding-left:15px;"><img width=150 height=100 src="' +
            url + '" /><div style="text-align:center"><div class="shapename">'+name+'</div>' +
            '<div class="buttonwrapper">' +
            '<a '+ (editable?'':'style="margin-left:55px" ')+'class="squarebutton"'+idstr+'><span>Use</span></a>' +
            (editable?'<a style="margin-left:6px" class="squarebutton"'+idstr+'><span>Edit</span></a><a style="margin-left:6px" class="squarebutton"'+idstr+'><span>Remove</span></a>':'') +
            '</div></div></div>';
  },
  getIFrameDoc: function() {
    try {
      return this.items.items[0].items.items[0].el.child('iframe').dom.contentDocument;
    } catch(e) {
      Ext.Msg.alert('Wrong "Create Shape" Panel format! Cannot find content document!');
      return {}
    }
  },
  afterRender: function() {
    Ext.canvasXpress.customShapeDialog.superclass.afterRender.apply(this, arguments);
    this.body.on('click', function(e, t) {
      var p = t.parentNode;
      if(t.tagName == 'IMG' && p && p.tagName == 'DIV')
      {
        var w = new Ext.Window({
          autoScroll: true,
          width: t.naturalWidth > 880? 900 : t.naturalWidth + 20,
          height: t.naturalWidth > 566? 600 : t.naturalHeight + 34,
          title: t.nextSibling.childNodes[0].textContent,
          html: '<img src="'+t.src+'" />'
        });
        w.show();
      }
      if(t.tagName == 'SPAN' && p && p.tagName == 'A' && p.className == 'squarebutton')
      {
        var id = p.getAttribute('dbid');
        switch(t.textContent) {
          case 'Use':
            this.valField.setValue(this.allShapes[id].url);
            this.dropdown.setValue('image');
            this.close();
            break;
          case 'Edit':
            var tabs = this.items.items[0], panel = tabs.items.items[0], items = panel.items.items;
            tabs.setActiveTab(0);
            var doc = this.getIFrameDoc(), cs = this.allShapes[id], bar = Ext.getCmp(panel.bbar.dom.children[0].id);
            items[0].setValue(cs.name);
            items[1].setValue(true);
            items[2].setValue(cs.shared == 1);
            doc.body.textContent = cs.url;
            panel.setTitle('Edit Shape');
            panel.dbid = id;
            bar.items.items[2].setText('Save Edits Now');
            bar.items.items[1].enable();
            break;
          case 'Remove':
            Ext.Msg.confirm('Warning', 'Are you sure you want to do that? This is not reversible!!', function(b) {
              if(b == 'yes')
              {
                if(this.extCanvas.customShapesURL)
                {
                  this.mask.show();
                  Ext.Ajax.request({
                    url: this.extCanvas.customShapesURL,
                    scope: this,
                    params: { id:id, action: 'deleteShape' },
                    callback: function(o, s, r) {
                      this.mask.hide();
                      if(!s) Ext.Msg.alert('Error', 'Could not delete the image from server!');
                      else
                      {
                        Ext.get(p).findParent('td').innerHTML = '';
                        delete this.allShapes[id];
                      }
                    }
                  });
                }
              }
            }, this);
            break;
        };
      }
    }, this);
  },
  assembleHtml: function(shapes) {
    if(shapes && shapes.length)
    {
      var str = [];
      for(var i = 0; i < shapes.length;)
      {
        var str1 = [];
        for(var j = 0; j < 3 && i + j < shapes.length; j++)
        {
          this.allShapes[shapes[i+j].id] = shapes[i+j];
          var idstr = ' dbid="' + shapes[i+j].id + '"';
          str1.push(this.getImgHtml(shapes[i+j].id, shapes[i+j].url, shapes[i+j].name, shapes[i+j].editable));
        }
        i += j;
        str.push('<tr><td>' + str1.join('</td><td>') + '</td></tr>');
      }
      return '<table>' + str.join('\n') + '</table>';
    }
    return '<table><tr></tr></table>';
  }
});
Ext.canvasXpress.nodeDialog = Ext.extend(Ext.Window, {
  constructor: function(config, extCanvas, callback) {
    if(!config) config = {};
    var isMulti = !!config.length, multiconf = {}, bad = {};
    if(isMulti)
    {
      for(var i = 0; i < config.length; i++)
      {
        var n = config[i];
        for(var j in n)
        {
          if(j == 'data' || bad[j]) continue;
          if(multiconf[j] == undefined || multiconf[j] === n[j])
            multiconf[j] = n[j];
          else
          {
            delete multiconf[j];
            bad[j] = 1;
          }
        }
      }
    }
    this.extCanvas = extCanvas;
    var allNodes = [], data = config.data || {}, parent = null, ns = extCanvas.canvas.data.nodes;
    for(var i = 0; i < ns.length; i++)
    {
      var l = ns[i], n = l.data, label = extCanvas.getName(l);
      if(config.id != l.id) allNodes.push([l.id, label]);
    }
    allNodes.sort(function(aa,bb){var a=(aa[1]+'').toLowerCase(),b=(bb[1]+'').toLowerCase();return a>b?1:a<b?-1:0});
    var h = {}, nd = this.extCanvas.nodeDialog? this.extCanvas.nodeDialog(config, this) : {};
    this.nd = nd;
    if(nd.hide)
      for(var i = 0; i < nd.hide.length; i++)
        h[nd.hide[i]] = 1;
    var item1 = [
      // label is required, parent, name, color, size, shape can all be hidden if user wants
      {
        xtype: 'compositefield',
        fieldLabel: 'Label',
        items: [
          { xtype: 'textfield', value: isMulti? multiconf.label : this.extCanvas.getName(config),
            allowBlank: isMulti, name:'label', width: 80 },
          { xtype: 'displayfield', value: 'Size:', width: 24, style:'margin-top:3px' },
          { xtype: 'numberfield', value: isMulti? multiconf.labelSize : config.labelSize || 1,
            allowBlank: isMulti, name:'labelSize', width: 30 },
          {
            xtype: 'checkbox',
            checked: isMulti? multiconf.hideLabel : config.hideLabel || config.hideName,
            boxLabel: 'Hide label?',
            name: 'hideLabel',
            flex: 1
          }
        ]
      },
      {
        xtype: 'compositefield',
        fieldLabel: 'Tooltip',
        items: [
          { xtype: 'textfield', value: isMulti? multiconf.tooltip : config.tooltip || config.name || '',
            hideLabel: h.tooltip, hidden: h.tooltip, name:'tooltip', width:80 },
          {
            xtype: 'checkbox',
            checked: isMulti? multiconf.hideTooltip : config.hideTooltip,
            boxLabel: 'Hide Tooltip?',
            name: 'hideTooltip',
            flex: 1
          }
        ]
      },
      {
        xtype: 'compositefield',
        fieldLabel: 'Parent',
        itemId: 'parent',
        items: [
          {
            xtype: 'combo',
            fieldLabel: 'Parent',
            hideLabel: h.parent, hidden: h.parent,
            emptyText: 'Selection NOT Required',
            triggerAction: 'all',
            name: 'parentLabel',
            mode: 'local',
            store: new Ext.data.ArrayStore({
              fields: [ 'id', 'label' ],
              data: allNodes }),
            valueField: 'id',
            displayField: 'label',
            tpl: '<tpl for="."><div ext:qtip="Id:{id}" class="x-combo-list-item">{label}</div></tpl>',
            value: isMulti? multiconf.parentNode : config.parentNode,
            listeners: { scope: extCanvas, expand: extCanvas.setComboMouse }
          },
          Ext.canvasXpress.utils.selectLabel(extCanvas)
        ]
      }
    ];
    if(isMulti && !nd.noMultiDesc)
      this.getMultiDesc(item1, config, extCanvas);
    var item2 = [
      { xtype: 'colorfield', fieldLabel: 'Color', allowBlank: isMulti,
        name:'color', value: isMulti? multiconf.color : config.color || 'rgb(255,0,0)',
        hideLabel: h.color, hidden: h.color },
      {
        xtype: 'compositefield',
        fieldLabel: 'Outline',
        items: [
          { xtype: 'colorfield', allowBlank: isMulti,
            name:'outline', value: isMulti? multiconf.outline : config.outline || 'rgb(255,0,0)',
            hideLabel: h.outline, hidden: h.outline },
          { xtype: 'displayfield', value: 'Width:', width: 34, style:'margin-top:3px' },
          { xtype:'numberfield', name:'outlineWidth', hideLabel: h.outlineWidth, hidden: h.outlineWidth,
            width: 30, value: isMulti? multiconf.outlineWidth : config.outlineWidth || '' }
        ]
      },
      {
        xtype: 'compositefield',
        fieldLabel: 'Pattern',
        items: [
          { xtype:'combo', name:'pattern', hideLabel: h.pattern, hidden: h.pattern,
            triggerAction: 'all', store: ['open', 'closed'],
            width: 80, value: isMulti? multiconf.pattern : config.pattern || 'closed' },
          { xtype: 'displayfield', value: 'Rotate:', width: 52, style:'margin-top:3px;margin-left:5px;' },
          { xtype:'numberfield', name:'rotate', hideLabel: h.rotate, hidden: h.rotate,
            width: 30, value: isMulti? multiconf.rotate : config.rotate || '' }
        ]
      },
      {
        xtype: 'compositefield',
        fieldLabel: 'Size',
        items: [
          { xtype:'numberfield', name:'size', hideLabel: h.size, hidden: h.size,
            width: 30, value: isMulti? multiconf.size : config.size || '1.0' },
          { xtype: 'displayfield', value: 'Width:', width: 35, style:'margin-top:3px' },
          { xtype:'numberfield', name:'width', hideLabel: h.width, hidden: h.width,
            width: 30, value: isMulti? multiconf.width : config.width || '' },
          { xtype: 'displayfield', value: 'Height:', width: 40, style:'margin-top:3px' },
          { xtype:'numberfield', name:'height', hideLabel: h.height, hidden: h.height,
            width: 30, value: isMulti? multiconf.height : config.height || '' }
        ]
      },
      {
        xtype: 'combo',
        fieldLabel: 'Shape',
        allowBlank: isMulti,
        hideLabel: h.shape, hidden: h.shape,
        emptyText: isMulti? 'Selection NOT Required' : 'Selection Required',
        triggerAction: 'all',
        store: extCanvas.canvas.shapes.sort(Ext.canvasXpress.utils.ciSort),
        name: 'shape',
        value: isMulti? multiconf.shape : config.shape || 'square',
        listeners: {
          scope: this,
          select: function(c, r, i) {
            var f0 = c.nextSibling();
            if(r.json[0] == 'image' || r.json[0] == 'custom')
            {
              if(!f0 || f0.name != 'imagePath')
                Ext.Msg.alert('Error','No image path textbox available!');
              else
              {
                f0.enable();
                if(r.json[0] == 'custom')
                {
                  this.value = 'image';
                  if(extCanvas.customShapesURL)
                  {
                    var m = new Ext.LoadMask(Ext.getBody(), {msg:"Contacting server..."});
                    m.show();
                    Ext.Ajax.request({
                      url: extCanvas.customShapesURL,
                      params: { action: 'getShapes' },
                      callback: function(o, s, r) {
                        m.hide();
                        if(!s) Ext.Msg.alert('Error', 'Could not get shapes from server!');
                        else
                        {
                          var data = Ext.decode(r.responseText);
                          var w = new Ext.canvasXpress.customShapeDialog({valField:f0, dropdown:c, mask:m,
                            extCanvas:extCanvas, myShapes:data.myShapes, sharedShapes:data.sharedShapes});
                          w.show();
                        }
                      }
                    });
                  }
                  else
                  {
                    Ext.Msg.alert('Error', 'No customShapesURL found! Cannot create custom shapes.');
                  }
                }
              }
            }
            else if(f0) f0.disable();
          }
        }
      },
      { fieldLabel: 'Image Path', value: isMulti? multiconf.imagePath : config.imagePath || '',
        hideLabel: h.imagePath, hidden: h.imagePath, width: 220,
        allowBlank: true, name:'imagePath', disabled: config.shape != 'image' },
      {
        xtype: 'compositefield',
        hideLabel: true,
        items: [{
          xtype: 'checkbox',
          checked: isMulti? multiconf.anchor : config.anchor,
          boxLabel: 'Anchor',
          width: 65,
          name: 'anchor'
        }, {
          xtype: 'checkbox',
          checked: isMulti? multiconf.eventless : config.eventless,
          boxLabel: 'Eventless',
          width: 80,
          name: 'eventless'
        }, {
          xtype: 'checkbox',
          checked: isMulti? multiconf.fixed : config.fixed,
          boxLabel: 'Fixed',
          width: 55,
          name: 'fixed'
        }, {
          xtype: 'checkbox',
          checked: false,
          boxLabel: 'Center Label',
          name: 'center',
          width: 100,
          flex: 1
        }]
      }
    ];
    var ps = [{
      title: 'Properties',
      items: item1
    }];
    if(!extCanvas.separateGraphics)
      for(var i = 0; i < item2.length; i++)
        ps[0].items.push(item2[i]);
    else
      ps.push({
        title: 'Graphics',
        items: item2
      });
    Ext.canvasXpress.utils.customPanel(ps, nd, isMulti);
    var bf = function(b, e, noClose) {
      var tab = b.ownerCt.ownerCt.get(0), prop = tab.get(0), graph = extCanvas.separateGraphics? tab.get(1) : null,
          pf = prop.getForm(), gf = graph? graph.getForm() : null;
      Ext.canvasXpress.utils.renderPanels(tab);
      // validate data
      if(!pf.isValid()) tab.activate(prop);
      if(gf && !gf.isValid()) tab.activate(gf);
      if(!pf.isValid() || (gf && !pf.isValid()) || (this.nd.isValid && !this.nd.isValid(tab)))
      {
        Ext.Msg.alert('Error', 'Required item not filled out or in wrong format!');
        return;
      }
      var p = pf.getValues(), p1 = prop.getComponent('parent').items.items[0].getValue(), g = gf? gf.getValues() : {}, changed = {}, allchanged = [];
      for(var i in g)
        p[i] = g[i];
      if(isMulti)
      {
        for(var i in p)
          if(p[i] && !/selection not required|please select/i.test(p[i]))
            changed[i] = p[i];
        for(var i = 0; i < config.length; i++)
        {
          var node = this.extCanvas.canvas.cloneObject(config[i]);
          for(var j in changed)
            node[j] = changed[j];
          allchanged.push(node);
        }
      }
      else allchanged = [config];
      var conf = config, oldNoClose = noClose;
      // now process every node as needed
      for(var i = 0; i < allchanged.length; i++)
      {
        p = isMulti? allchanged[i] : p;
        if(isMulti)
        {
          config = p;
          if(i < allchanged.length - 1) noClose = true;
          else noClose = oldNoClose;
        }
        // assembl parameters
        if(p1) p.parent = p1; // p1 is parent node id
        if(config.id) p.id = config.id;
        if(this.nd.getParams) this.nd.getParams.call(this, config, p, tab);
        var dup = !isMulti && !config.id && (config.x || config.y);
        if(dup)
        {
          var dupMove = (config.x>config.y?config.y:config.x)/10;
          config.x -= dupMove;
          config.y -= dupMove;
          if(config.labelX)
          {
            config.labelX -= dupMove;
            config.labelY -= dupMove;
          }
        }
//         var c = config.id || dup? {x:config.x,y:config.y} : this.extCanvas.canvas.adjustedCoordinates(this.extCanvas.clickXY); // make sure there's an X,Y that user can save to DB in callback
        var c = config.id || dup? {x:config.x,y:config.y} : this.extCanvas.canvas.findXYCoordinates(this.extCanvas.clickXY); // make sure there's an X,Y that user can save to DB in callback
        p.x = c.x; p.y = c.y;
        if(p.center)
        {
          p.labelX = config.x;
          p.labelY = config.y;
        }
        else if(config.labelX)
        {
          p.labelX = config.labelX;
          p.labelY = config.labelY;
        }

        var f = function(n, p, res) {
          n.label = p.label;
          n.hideLabel = p.hideLabel == "on";
          n.hideTooltip = p.hideTooltip == "on";
          n.eventless = p.eventless == "on";
          n.anchor = p.anchor == "on";
          n.fixed = p.fixed == "on";
          n.labelSize = p.labelSize-0;
          // if user sets them hidden, delegate these properties to user callbacks
          if(!h.tooltip) n.tooltip = p.tooltip;
          if(!h.color) n.color = p.color;
          if(!h.shape) n.shape = p.shape;
          if(!h.imagePath) n.imagePath = p.imagePath;
          if(!h.size) n.size = p.size-0;
          if(!h.width) n.width = p.width-0;
          if(!h.height) n.height = p.height-0;
          if(!h.rotate) n.rotate = p.rotate-0;
          if(!h.outline) n.outline = p.outline;
          if(!h.outlineWidth) n.outlineWidth = p.outlineWidth;
          if(!h.pattern) n.pattern = p.pattern;
          if(!h.parent) n.parentNode = p.parent;
          if(!n.id)
          {
            if(res && res.id) n.id = res.id;
            this.extCanvas.canvas.addNode(n, dup? null : this.extCanvas.clickXY); // use existing x,y when duplicating
            this.extCanvas.updateOrder(); // change of # of node causes nodeIndices to change, it has to be consistent!!
          }
          if(p.center)
          {
            n.labelX = p.x;
            n.labelY = p.y;
          }

          this.extCanvas.canvas.draw();
          if(!res) // not saved to server
            this.extCanvas.addNode(n);
          else
            this.extCanvas.removeNode(n.id);
          if(!noClose) this.close();
          if(callback) callback(n);
        }.createDelegate(this);

        if(isMulti && this.extCanvas.hasListener('saveallchanges'))
        {
          f(conf[i], p);
          if(i == allchanged.length - 1)
          {
            this.extCanvas.addNode(allchanged);
            this.extCanvas.saveMap(null, {respectNoPost:true});
          }
        }
        else
        {
          if(this.extCanvas.hasListener('updatenode') && !this.viewInfo)
            this.extCanvas.fireEvent('updatenode', isMulti? conf[i] : conf, p, f);
          else f(isMulti? conf[i] : conf, p);
        }
      }
      config = conf;
    }.createDelegate(this);
    var bitems = ['->'];
    if(config.id)
      bitems.push({
        text: 'Reset',
        scope: this,
        handler: function(b, e) { Ext.canvasXpress.utils.resetForms(b.ownerCt.ownerCt.get(0)) }
      },
      '|');
    bitems.push({
        text: 'Apply',
        handler: function(b, e) { bf(b, e, true) }
      },
      '|',
      {
        text: isMulti? 'Change Nodes' : (config.id? 'Change' : 'Add') + ' Node',
        handler: bf
      });
    Ext.apply(this, {
      width: nd.width || 370,
      height: nd.height || 350,
      title: 'Network Node Editor' + (config.id? ' (Node Id:' + config.id + ')':''),
      items: {
        xtype: 'tabpanel',
        activeItem: 0,
        defaultType: 'form',
        defaults: { style:'margin:5px',defaultType: 'textfield',height:210,labelWidth:80 },
        items: ps
      },
      bbar: bitems
    });
    Ext.canvasXpress.nodeDialog.superclass.constructor.apply(this);
  },
  getMultiDesc: function(items, config) {
    var str = '';
    for(var i = 0; i < config.length; i++)
    {
      var name = this.extCanvas.getName(config[i]);
      if(str.length + name.length > 45)
      {
        str += '...';
        break;
      }
      else str += (str.length? ', ':'') + name;
    }
    items.push({xtype: 'label', text: 'Edit the ' + config.length + ' node' + (config.length>1?'s':'') + ' selected: ' + str, style:'color:red'});
  }
});
Ext.canvasXpress.edgeDialog = Ext.extend(Ext.Window, {
  title: 'Network Edge Editor',
  constructor: function(config, dir, extCanvas, config1) {
    if(dir == 'to') config = {id2:config.id};
    else if(dir == 'from') config = {id1:config.id};
    else if(!config) config = {};
    if(config1)
    {
      if(dir == 'to') config.id1 = config1.id;
      else if(dir == 'from') config.id2 = config1.id;
    }
    var isMulti = !!config.length;
    this.edgeInfo = config;
    this.extCanvas = extCanvas;
    var allNodes = [], data = config.data || {}, ns = extCanvas.canvas.data.nodes;
    for(var i = 0; i < ns.length; i++)
    {
      var l = ns[i], n = l.data, label = extCanvas.getName(l);
      allNodes.push([l.id, label]);
    }
    allNodes.sort(function(aa,bb){var a=(aa[1]+'').toLowerCase(),b=(bb[1]+'').toLowerCase();return a>b?1:a<b?-1:0});
    var h = {}, ed = this.extCanvas.edgeDialog? this.extCanvas.edgeDialog(config) : {},
        isChange = config.id1 && config.id2 && !config1;
    this.ed = ed;
    if(ed.hide)
      for(var i = 0; i < ed.hide.length; i++)
        h[ed.hide[i]] = 1;
    var v1 = config.id1? extCanvas.canvas.nodes[config.id1] : null,
        v2 = config.id2? extCanvas.canvas.nodes[config.id2] : null;
    var ps = [{
      title: 'Customize',
      items: [
        {
          xtype: 'compositefield',
          fieldLabel: 'Start Node',
          itemId: 'startnode',
          items: [
            {
              width: 140,
              xtype: 'combo',
              emptyText: 'Selection Required',
              triggerAction: 'all',
              allowBlank: false,
              name: 'id1',
              mode: 'local',
              store: new Ext.data.ArrayStore({
                fields: [ 'id', 'label' ],
                data: allNodes }),
              valueField: 'id',
              displayField: 'label',
              tpl: '<tpl for="."><div ext:qtip="Id:{id}" class="x-combo-list-item">{label}</div></tpl>',
              value: v1? v1.id: null, // v1? extCanvas.getName(v1) : null
              listeners: { scope: extCanvas, expand: extCanvas.setComboMouse }
            },
            Ext.canvasXpress.utils.selectLabel(extCanvas)
          ]
        },
        {
          xtype: 'compositefield',
          fieldLabel: 'End Node',
          itemId: 'endnode',
          items: [
            {
              xtype: 'combo',
              width: 140,
              emptyText: 'Selection Required',
              triggerAction: 'all',
              store: new Ext.data.ArrayStore({
                fields: [ 'id', 'label' ],
                data: allNodes }),
              valueField: 'id',
              displayField: 'label',
              allowBlank: false,
              name: 'id2',
              mode: 'local',
              tpl: '<tpl for="."><div ext:qtip="Id:{id}" class="x-combo-list-item">{label}</div></tpl>',
              value: v2? v2.id: null,// v2? extCanvas.getName(v2) : null
              listeners: { scope: extCanvas, expand: extCanvas.setComboMouse }
            },
            Ext.canvasXpress.utils.selectLabel(extCanvas)
          ]
        },
        {
          xtype: 'compositefield',
          fieldLabel: 'Tooltip',
          items: [
            { xtype: 'textfield', value: isMulti? null : config.tooltip || '',
              hideLabel: h.tooltip, hidden: h.tooltip, name:'tooltip', width:80 },
            {
              xtype: 'checkbox',
              checked: isMulti? false : config.hideTooltip,
              boxLabel: 'Hide Tooltip?',
              name: 'hideTooltip',
              flex: 1
            }
          ]
        },
        { name: 'width', xtype: 'numberfield', fieldLabel: 'Width',
          hideLabel: h.width, hidden: h.width,
          value: config.width || '2.0', allowBlank: false },
        {
          fieldLabel: 'Line Type',
          hideLabel: h.linetype, hidden: h.linetype,
          emptyText: 'Selection Required',
          triggerAction: 'all',
          store: extCanvas.canvas.lines.sort(Ext.canvasXpress.utils.ciSort),
          allowBlank: false,
          name: 'linetype',
          value: config.type || 'arrowHeadLine'
        },
        { xtype: 'colorfield', fieldLabel: 'Color', allowBlank: false,
          name:'color', value: config.color || 'rgb(255,0,0)',
          hideLabel: h.color, hidden: h.color }
      ]
    }];
    Ext.canvasXpress.utils.customPanel(ps, ed, isMulti);
    var bbf = function(b, e, noClose) {
      var tab = b.ownerCt.ownerCt.get(0), fp = tab.get(0), bf = fp.getForm();
      Ext.canvasXpress.utils.renderPanels(tab);
      if(!bf.isValid() || (this.ed.isValid && !this.ed.isValid(tab)))
      {
        Ext.Msg.alert('Error', 'Required item not filled out or in wrong format!');
        return;
      }
      var p = bf.getValues(), p1 = fp.getComponent('startnode').items.items[0].getValue(),
          p2 = fp.getComponent('endnode').items.items[0].getValue();
      // assembl parameters
      p.id1 = p1;
      p.id2 = p2;
      p.entryid1 = p.id1;
      p.entryid2 = p.id2;
      if(this.ed.getParams) this.ed.getParams.call(this, config, p, tab);

      var f = function(e, p, res) {
        var add = !isChange;
        // if user sets them hidden, delegate these properties to user callbacks
        e.hideTooltip = p.hideTooltip == "on";
        if(!h.width) e.width = p.width;
        if(!h.tooltip) e.tooltip = p.tooltip;
        if(!h.linetype) e.type = p.linetype;
        if(!h.color) e.color = p.color;
        e.id1 = p.entryid1;
        e.id2 = p.entryid2;
        if(add) this.extCanvas.canvas.addEdge(e);
        this.extCanvas.canvas.draw();
        if(!res) // not saved to server
          this.extCanvas.addEdge(e);
        else
          this.extCanvas.removeEdge(e.id1, e.id2);
        if(!noClose) this.close();
      }.createDelegate(this);

      if(this.extCanvas.hasListener('updateedge') && !this.viewInfo)
        this.extCanvas.fireEvent('updateedge', this.edgeInfo, p, f);
      else f(this.edgeInfo, p);
    }.createDelegate(this);
    var bitems = ['->'];
    if(config.id)
      bitems.push({
        text: 'Reset',
        scope: this,
        handler: function(b, e) { Ext.canvasXpress.utils.resetForms(b.ownerCt.ownerCt.get(0)) }
      },
      '|');
    bitems.push({
        text: 'Apply',
        handler: function(b, e) { bbf(b, e, true) }
      },
      '|',
      {
        text: (isChange? 'Change' : 'Add') + ' Edge',
        handler: bbf
      });
    Ext.apply(this, {
      width: ed.width || 350,
      height: ed.height || 300,
      items: {
        xtype: 'tabpanel',
        activeItem: 0,
        defaultType: 'form',
        defaults: { style:'margin:5px',defaultType: 'combo',height:210,labelWidth:80 },
        items: ps
      },
      bbar: bitems
    });
    Ext.canvasXpress.edgeDialog.superclass.constructor.apply(this);
  }
});
Ext.canvasXpress.networkDialog = Ext.extend(Ext.Window, {
  title: 'Network Editor',
  constructor: function(config, extCanvas, isSaveAs) { // config should be the network obj (with (optional) id,name,(optional) description), with a (optional) key extCanvas being the panel
    if(!config) config = {};
    if(isSaveAs)
    {
      config = { category: config.category, description: config.description,
        options: Ext.canvasXpress.utils.clone(config.options) };
    }
    this.nd = config.networkDialog? config.networkDialog(config) : extCanvas && extCanvas.networkDialog? extCanvas.networkDialog(config) : {};
    var ps = [{
      title: 'Customize',
      defaults: { width: 220 },
      items: [
        {
          fieldLabel: 'Name',
          allowBlank: false,
          name: 'name',
          value: config.name || ''
        },
        {
          xtype: 'combo',
          fieldLabel: 'Category',
          name: 'category',
          allowBlank: false,
          width: 220,
          triggerAction: 'all',
          typeahead: true,
          emptyText: 'Select one or type a new one',
          mode: 'local',
          store: (extCanvas? extCanvas.categories : config.categories) || [],
          value: config.category || ''
        },
        {
          fieldLabel: 'Description',
          name: 'description',
          value: config.description || ''
        },
        {
          fieldLabel: 'Display Options',
          xtype: 'textarea',
          height: 150,
          name: 'options',
          value: config.options? config.options.replace(/\\n/g, '\n').replace(/^"|"$/g, '') || '{}' : '{}'
        }
      ]
    }];
    Ext.canvasXpress.utils.customPanel(ps, this.nd, false);
    var bitems = ['->'];
    if(config.id)
      bitems.push({
        text: 'Reset',
        scope: this,
        handler: function(b, e) { Ext.canvasXpress.utils.resetForms(b.ownerCt.ownerCt.get(0)) }
      },
      '|');
    bitems.push({
        text: (config.id? 'Change' : 'Add') + ' Network',
        scope: this,
        handler: function(b, e) {
          var tab = b.ownerCt.ownerCt.get(0), fp = tab.get(0), bf = fp.getForm();
          if(!bf.isValid())
          {
            Ext.Msg.alert('Error', 'Required item not filled out or in wrong format!');
            return;
          }
          // add in delete this legend.
          var p = bf.getValues(), f = function(pid) {
            if(!this.toggleSaveNetworkBtn) return;
            this.networkChanged = false;
            if(isSaveAs) // force save the network now
            {
              this.networkInfo.id = pid; // new id
              this.saveMap(1);
            }
            this.toggleSaveNetworkBtn();
          }.createDelegate(extCanvas);
          var opts;
          try {
            opts = Ext.decode(p.options);
          } catch(e) {
            Ext.Msg.alert('Error', 'Invalid JSON in Display Options:' + e);
            return;
          }

          var namechanged = config.name != p.name;
          config.name = p.name;
          config.category = p.category;
          config.description = p.description;
          if(extCanvas)
          {
            extCanvas.networkChanged = true; // unfinished. add in user panel might be needed, combine with code in tree.js
            extCanvas.toggleSaveNetworkBtn();
            for(var i in opts)
              extCanvas.canvas[i] = opts[i];
            extCanvas.canvas.draw();
            if(namechanged) extCanvas.setTitle(config.name);
          }
          config.options = p.options;

          if(extCanvas)
          {
            if(!this.viewInfo) extCanvas.fireEvent('updatenetwork', config, f);
          }
          else if(config.callback) config.callback(config, f);
          this.close();
        }
      });
    Ext.apply(this, {
      width: 350,
      height: 350,
      items: {
        xtype: 'tabpanel',
        activeItem: 0,
        defaultType: 'form',
        defaults: { style:'margin:5px',defaultType: 'textfield',height:210,labelWidth:90 },
        items: ps
      },
      bbar: bitems
    });
    Ext.canvasXpress.networkDialog.superclass.constructor.apply(this);
  }
});
Ext.canvasXpress.nodeLegendDialog = Ext.extend(Ext.Window, {
  title: 'Network NodeLegend Editor',
  constructor: function(config, extCanvas) {
    if(!config) config = {};
    this.confInfo = config;
    this.extCanvas = extCanvas;

    var cm = new Ext.grid.ColumnModel({
      defaults: { sortable: true },
      columns: [
        {
          header: 'Shape',
          dataIndex: 'shape',
          width: 70,
          editor: new Ext.form.ComboBox({ allowBlank: false,
              emptyText: 'Selection Required',
              triggerAction: 'all',
              store: extCanvas.canvas.shapes.sort(Ext.canvasXpress.utils.ciSort) })
        },
        {
          header: 'Node Size',
          dataIndex: 'size',
          width: 70,
          editor: new Ext.form.NumberField({ allowBlank: false })
        },
        {
          header: 'Font Size',
          dataIndex: 'font',
          width: 70,
          editor: new Ext.form.NumberField({ allowBlank: false })
        },
        {
          header: 'Color',
          dataIndex: 'color',
          width: 150,
          editor: new Ext.ux.ColorField({ allowBlank: false })
        },
        {
          header: 'Text',
          dataIndex: 'text',
          editor: new Ext.form.TextField({ allowBlank: false })
        }
      ]
    });

    var store = new Ext.data.JsonStore({
      autoDestroy: true,
      root: 'nodes',
      fields: ['shape','text','color',{name:'size', type: 'float'},{name:'font', type: 'float'}]
    });
    Ext.canvasXpress.utils.initLegend.call(this, 'nodes');
    store.loadData(this.extCanvas.canvas.data.legend);

    // create the editor grid
    var width = 610, height = 300, grid;
    var ai = {
      text: 'Add Row',
      icon: this.extCanvas.imgDir + 'add.png',
      scope: this,
      handler: function(){
        var Node = grid.getStore().recordType;
        var n = new Node({
          color: 'rgb(255,0,0)',
          shape: 'square',
          font: 1,
          size: 1
        });
        grid.stopEditing();
        store.insert(0, n);
        grid.startEditing(0, 0);
        Ext.canvasXpress.utils.gridToLegend(extCanvas, grid, 'nodes');
      }
    }, ri = {
      text: 'Update Network',
      icon: this.extCanvas.imgDir + 'refresh.png',
      scope: this,
      handler: function(){
        Ext.canvasXpress.utils.gridToLegend(extCanvas, grid, 'nodes');
        Ext.canvasXpress.utils.updateLegend(extCanvas, 'nodes');
      }
    }, di = {
      text: 'Delete Selected Row',
      icon: this.extCanvas.imgDir + 'delete.png',
      scope: this,
      handler: function(){
        var c = grid.getSelectionModel().getSelectedCell();
        if(c)
        {
          grid.stopEditing();
          grid.getStore().removeAt(c[0]);
          Ext.canvasXpress.utils.gridToLegend(extCanvas, grid, 'nodes');
        }
      }
    };
    grid = new Ext.grid.EditorGridPanel({
      store: store,
      cm: cm,
      width: width - 10,
      height: height - 10,
      autoExpandColumn: 4, // 5th column, 'text'
      frame: false,
      clicksToEdit: 1,
      listeners: {
        scope: this,
        containercontextmenu: function(g, e) {
          e.stopEvent(); // here it's required, in TreePanel it's not.
          var m = new Ext.menu.Menu({ items: [ai, ri] });
          m.showAt(e.getXY());
        },
        contextmenu: function(e) {
          var c = grid.getSelectionModel().getSelectedCell();
          if(c)
          {
            e.stopEvent();
            var m = new Ext.menu.Menu({ items: di });
            m.showAt(e.getXY());
          }
        }
      },
      tbar: [ ai, di, ri ]
    });
    Ext.apply(this, {
      width: width,
      layout: 'fit',
      height: height,
      items: grid
    });
    Ext.canvasXpress.nodeLegendDialog.superclass.constructor.apply(this);
  }
});
Ext.canvasXpress.edgeLegendDialog = Ext.extend(Ext.Window, {
  title: 'Network EdgeLegend Editor',
  constructor: function(config, extCanvas) {
    if(!config) config = {};
    this.confInfo = config;
    this.extCanvas = extCanvas;

    var cm = new Ext.grid.ColumnModel({
      defaults: { sortable: true },
      columns: [
        {
          header: 'Line Type',
          dataIndex: 'type',
          width: 140,
          editor: new Ext.form.ComboBox({ allowBlank: false,
              emptyText: 'Selection Required',
              triggerAction: 'all',
              store: extCanvas.canvas.lines.sort(Ext.canvasXpress.utils.ciSort) })
        },
        {
          header: 'Line Width',
          dataIndex: 'width',
          width: 70,
          editor: new Ext.form.NumberField({ allowBlank: false })
        },
        {
          header: 'Line Length',
          dataIndex: 'size',
          width: 70,
          editor: new Ext.form.NumberField({ allowBlank: false })
        },
        {
          header: 'Font Size',
          dataIndex: 'font',
          width: 70,
          editor: new Ext.form.NumberField({ allowBlank: false })
        },
        {
          header: 'Color',
          dataIndex: 'color',
          width: 150,
          editor: new Ext.ux.ColorField({ allowBlank: false })
        },
        {
          header: 'Text',
          dataIndex: 'text',
          editor: new Ext.form.TextField({ allowBlank: false })
        }
      ]
    });

    var store = new Ext.data.JsonStore({
      autoDestroy: true,
      root: 'edges',
      fields: ['type','text','color',{name:'size', type: 'float'},{name:'font', type: 'float'},{name:'width', type: 'float'}]
    });
    Ext.canvasXpress.utils.initLegend.call(this, 'edges');
    store.loadData(this.extCanvas.canvas.data.legend);

    // create the editor grid
    var width = 710, height = 300, grid;
    var ai = {
      text: 'Add Row',
      icon: this.extCanvas.imgDir + 'add.png',
      scope: this,
      handler: function(){
        var Edge = grid.getStore().recordType;
        var n = new Edge({
          type: 'arrowHeadLine',
          color: 'rgb(255,0,0)',
          font: 1,
          width: 1.5,
          size: 2
        });
        grid.stopEditing();
        store.insert(0, n);
        grid.startEditing(0, 0);
        Ext.canvasXpress.utils.gridToLegend(extCanvas, grid, 'edges');
      }
    }, ri = {
      text: 'Update Network',
      icon: this.extCanvas.imgDir + 'refresh.png',
      scope: this,
      handler: function(){
        Ext.canvasXpress.utils.gridToLegend(extCanvas, grid, 'edges');
        Ext.canvasXpress.utils.updateLegend(extCanvas, 'edges');
      }
    }, di = {
      text: 'Delete Selected Row',
      icon: this.extCanvas.imgDir + 'delete.png',
      scope: this,
      handler: function(){
        var c = grid.getSelectionModel().getSelectedCell();
        if(c)
        {
          grid.stopEditing();
          grid.getStore().removeAt(c[0]);
          Ext.canvasXpress.utils.gridToLegend(extCanvas, grid, 'edges');
        }
      }
    };
    grid = new Ext.grid.EditorGridPanel({
      store: store,
      cm: cm,
      width: width - 10,
      height: height - 10,
      autoExpandColumn: 5, // 6th column, 'text'
      frame: false,
      clicksToEdit: 1,
      listeners: {
        scope: this,
        containercontextmenu: function(g, e) {
          e.stopEvent(); // here it's required, in TreePanel it's not.
          var m = new Ext.menu.Menu({ items: [ai, ri] });
          m.showAt(e.getXY());
        },
        contextmenu: function(e) {
          var c = grid.getSelectionModel().getSelectedCell();
          if(c)
          {
            e.stopEvent();
            var m = new Ext.menu.Menu({ items: di });
            m.showAt(e.getXY());
          }
        }
      },
      tbar: [ ai, di, ri ]
    });
    Ext.apply(this, {
      width: width,
      layout: 'fit',
      height: height,
      items: grid
    });
    Ext.canvasXpress.edgeLegendDialog.superclass.constructor.apply(this);
  }
});
Ext.canvasXpress.textLegendDialog = Ext.extend(Ext.Window, {
  title: 'Network TextLegend Editor',
  constructor: function(config, extCanvas) {
    if(!config) config = {};
    this.confInfo = config;
    this.extCanvas = extCanvas;

    var checkColumn = new Ext.grid.CheckColumn({
      header: 'Has Border?',
      dataIndex: 'boxed',
      width: 55
    });
    var cm = new Ext.grid.ColumnModel({
      defaults: { sortable: true },
      columns: [
        checkColumn,
        {
          header: 'Font Size',
          dataIndex: 'font',
          width: 70,
          editor: new Ext.form.NumberField({ allowBlank: false })
        },
        {
          header: 'Margin',
          dataIndex: 'margin',
          width: 100,
//           editor: new Ext.form.TextField({ allowBlank: false})
          editor: new Ext.form.TextField({ allowBlank: false,
                  invalidText: 'Allowed format: 10 or 10 20 or 15 20 40 or 10 15 40 30 (like CSS margin)',
                  regex: /^[0-9]+$|^[0-9]+[,\s]+[0-9]+$|^[0-9]+[,\s]+[0-9]+[,\s]+[0-9]+$|^[0-9]+[,\s]+[0-9]+[,\s]+[0-9]+[,\s]+[0-9]+$/ })
        },
        {
          header: 'Color',
          dataIndex: 'color',
          width: 150,
          editor: new Ext.ux.ColorField({ allowBlank: false })
        },
        {
          header: 'Text',
          dataIndex: 'text',
          editor: new Ext.form.TextField({ allowBlank: false })
        }
      ]
    });

    var store = new Ext.data.JsonStore({
      autoDestroy: true,
      root: 'text',
//       fields: ['text','id','color',{name:'boxed', type: 'boolean'},{name:'margin', type: 'float'},{name:'font', type: 'float'},{name:'x', type: 'float'},{name:'y', type: 'float'}]
      fields: ['text','id','color','margin',{name:'boxed', type: 'boolean'},{name:'font', type: 'float'},{name:'x', type: 'float'},{name:'y', type: 'float'}]
    });
    Ext.canvasXpress.utils.initLegend.call(this, 'text');
    store.loadData(this.extCanvas.canvas.data.legend);

    // create the editor grid
    var width = 610, height = 300, grid;
    var ai = {
      text: 'Add Row',
      icon: this.extCanvas.imgDir + 'add.png',
      scope: this,
      handler: function(){
        var Text = grid.getStore().recordType;
        grid.stopEditing();
        store.insert(0, new Text({ font: 1, margin: 10, color: 'rgb(0,0,0)', boxed: true }));
        grid.startEditing(0, 0);
        Ext.canvasXpress.utils.gridToLegend(extCanvas, grid, 'text');
      }
    }, ri = {
      text: 'Update Network',
      icon: this.extCanvas.imgDir + 'refresh.png',
      scope: this,
      handler: function(){
        Ext.canvasXpress.utils.gridToLegend(extCanvas, grid, 'text');
        Ext.canvasXpress.utils.updateLegend(extCanvas, 'text');
      }
    }, di = {
      text: 'Delete Selected Row',
      icon: this.extCanvas.imgDir + 'delete.png',
      scope: this,
      handler: function(){
        var c = grid.getSelectionModel().getSelectedCell();
        if(c)
        {
          grid.stopEditing();
          grid.getStore().removeAt(c[0]);
          Ext.canvasXpress.utils.gridToLegend(extCanvas, grid, 'text');
        }
      }
    };
    grid = new Ext.grid.EditorGridPanel({
      store: store,
      cm: cm,
      width: width - 10,
      height: height - 10,
      autoExpandColumn: 4, // 5th column, 'text'
      frame: false,
      clicksToEdit: 1,
      plugins: checkColumn,
      listeners: {
        scope: this,
        viewready: function() {
          if(this.confInfo && this.confInfo.id)
          {
            var row = grid.getStore().find('id', this.confInfo.id);
            grid.view.focusRow(row);
            grid.getSelectionModel().select(row, 4); // select text column
          }
        },
        containercontextmenu: function(g, e) {
          e.stopEvent(); // here it's required, in TreePanel it's not.
          var m = new Ext.menu.Menu({ items: [ai, ri] });
          m.showAt(e.getXY());
        },
        contextmenu: function(e) {
          var c = grid.getSelectionModel().getSelectedCell();
          if(c)
          {
            e.stopEvent();
            var m = new Ext.menu.Menu({ items: di });
            m.showAt(e.getXY());
          }
        }
      },
      tbar: [ ai, di, ri ]
    });
    Ext.apply(this, {
      width: width,
      layout: 'fit',
      height: height,
      items: grid
    });
    Ext.canvasXpress.textLegendDialog.superclass.constructor.apply(this);
  }
});
Ext.canvasXpress.SnapshotManager = Ext.extend(Ext.Window, {
  constructor: function(config, extCanvas) {
    if(!config || !extCanvas || !config.list)
      Ext.Msg.alert('Error', 'SnapshotManager must be called with list and extcanvas parameters!');
    Ext.apply(this, {
      title: 'Manage the Available Snapshots',
      width: 500,
      height: 300,
      layout: 'fit',
      items: {
        xtype: 'grid',
        border: false,
        autoExpandColumn: 'ssdesc',
        store: new Ext.data.ArrayStore({
          autoDestroy: true,
          data: config.list,
          fields: [ 'username', 'name', 'desc', 'id', 'shared' ]
        }),
        stripeRows: true,
        sm: new Ext.grid.RowSelectionModel({singleSelect:true}),
        cm: new Ext.grid.ColumnModel({
            defaults: { sortable: true, width: 60 },
            columns: [
              { header: "User", dataIndex:'username' },
              { header: "Name", dataIndex:'name', width: 90 },
              { header: "Shared", dataIndex:'shared' },
              { header: "Description", dataIndex:'desc', id: 'ssdesc' }
          ]})
      },
      bbar: {
        items: [
          '->',
          {
            text: 'Edit Selected',
            scope: extCanvas,
            handler: function(b, e) {
              var g = b.ownerCt.ownerCt.get(0), r = g.getSelectionModel().getSelected();
              if(r && r.data)
              {
                if(this.networkInfo && this.networkInfo.user && this.networkInfo.user != r.data.username)
                {
                  Ext.Msg.alert('Error', 'You are only allowed to edit your own snapshots!');
                  return;
                }
                this.showEditSnapshot({id:r.data.id,name:r.data.name,shared:r.data.shared,desc:r.data.desc},
                  {action:'editMovie', title: 'Change Snapshots Info', bText: 'Save New Info to DB Now',
                  xy:config.xy, win: g.ownerCt});
              }
            }
          },
          {
            text: 'Load Selected',
            scope: extCanvas,
            handler: function(b, e) {
              var g = b.ownerCt.ownerCt.get(0), r = g.getSelectionModel().getSelected(),
                  win = g.ownerCt;
              if(r && r.data && this.movieURL)
              {
                this.showMask('Loading snapshots ...');
                this.snapshotsInfo = r.data;
                Ext.Ajax.request({
                  url: this.movieURL,
                  params: { action:'loadMovie', id: r.data.id },
                  scope: this,
                  callback: function(o, s, r) {
                    this.hideMask();
                    if(!s) Ext.Msg.alert('Error', 'Could not load movie from server!');
                    else
                    {
                      var res = Ext.decode(r.responseText);
                      this.canvas.setSnapshotsData(Ext.decode(res.data));
                      win.close();
                      this.setCtrlMode();
                      if(config.xy) this.showSnapshotResultTip('Snapshots loaded from DB', config.xy);
                    }
                  }
                });
              }
            }
          },
          {
            text: 'Delete Selected',
            scope: extCanvas,
            handler: function(b, e) {
              Ext.Msg.confirm('Warning', 'Are you sure you want to delete this movie?', function(t) {
                if(t == 'yes')
                {
                  var g = b.ownerCt.ownerCt.get(0), r = g.getSelectionModel().getSelected();
                  if(r && r.data)
                  {
                    if(extCanvas.networkInfo && extCanvas.networkInfo.user && extCanvas.networkInfo.user != r.data.username)
                    {
                      Ext.Msg.alert('Error', 'You are only allowed to delete your own snapshots!');
                      return;
                    }
                    this.showMask('Deleting snapshots ...');
                    Ext.Ajax.request({
                      url: this.movieURL,
                      params: { action:'deleteMovie', id: r.data.id },
                      scope: this,
                      callback: function(o, s, re) {
                        this.hideMask();
                        var res = (s && re && re.responseText)? Ext.decode(re.responseText) : null;
                        if(!res || res.error) Ext.Msg.alert('Error', res && res.error? res.error : 'Could not delete snapshots from server!');
                        else
                        {
                          g.store.remove(r);
                          Ext.Msg.alert('Done!', 'Snapshots deleted from the server!');
                        }
                      }
                    });
                  }
                }
              }, this);
            }
          }
        ]
      }
    });
    Ext.canvasXpress.SnapshotManager.superclass.constructor.apply(this);
  }
});
Ext.canvasXpress.ViewManager = Ext.extend(Ext.Window, {
  constructor: function(config, extCanvas) {
    if(!config || !extCanvas || !config.list)
      Ext.Msg.alert('Error', 'ViewManager must be called with list and extcanvas parameters!');
    Ext.apply(this, {
      title: 'Manage the Available Views',
      width: 500,
      height: 300,
      layout: 'fit',
      items: {
        xtype: 'grid',
        border: false,
        autoExpandColumn: 'vdesc',
        store: new Ext.data.JsonStore({
          autoDestroy: true,
          data: {rows:config.list},
          root: 'rows',
          idProperty: 'id',
          fields: [ 'name', 'user', 'desc', 'id', 'shared' ]
        }),
        stripeRows: true,
        sm: new Ext.grid.RowSelectionModel({singleSelect:true}),
        cm: new Ext.grid.ColumnModel({
            defaults: { sortable: true, width: 60 },
            columns: [
              { header: "User", dataIndex:'user' },
              { header: "Name", dataIndex:'name', width: 90 },
              { header: "Shared", dataIndex:'shared' },
              { header: "Description", dataIndex:'desc', id: 'vdesc' }
          ]})
      },
      bbar: {
        items: [
          '->',
          {
            text: 'Edit Selected',
            scope: extCanvas,
            handler: function(b, e) {
              var g = b.ownerCt.ownerCt.get(0), r = g.getSelectionModel().getSelected();
              if(r && r.data)
                this.editView(r.data, g.ownerCt);
            }
          },
          {
            text: 'Load Selected',
            scope: extCanvas,
            handler: function(b, e) {
              var g = b.ownerCt.ownerCt.get(0), r = g.getSelectionModel().getSelected(),
                  win = g.ownerCt;
              if(r && r.data && this.movieURL)
                this.loadView({id:r.data.id,name:r.data.name,shared:r.data.shared,desc:r.data.desc});
            }
          },
          {
            text: 'Delete Selected',
            scope: extCanvas,
            handler: function(b, e) {
              Ext.Msg.confirm('Warning', 'Are you sure you want to delete this view?', function(t) {
                if(t == 'yes')
                {
                  var g = b.ownerCt.ownerCt.get(0), r = g.getSelectionModel().getSelected();
                  if(r && r.data)
                  {
                    if(extCanvas.networkInfo && extCanvas.networkInfo.user && extCanvas.networkInfo.user != r.data.user)
                    {
                      Ext.Msg.alert('Error', 'You are only allowed to delete your own view!');
                      return;
                    }
                    this.showMask('Deleting view ...');
                    Ext.Ajax.request({
                      url: this.movieURL,
                      params: { action:'deleteMovie', id: r.data.id },
                      scope: this,
                      callback: function(o, s, re) {
                        this.hideMask();
                        var res = (s && re && re.responseText)? Ext.decode(re.responseText) : null;
                        if(!res || res.error) Ext.Msg.alert('Error', res && res.error? res.error : 'Could not delete view from server!');
                        else
                        {
                          if(this.viewInfo && this.viewInfo.id == r.data.id)
                            this.revertView(1);
                          for(var i = 0; i < this.pViews.length; i++)
                            if(this.pViews[i].id == r.data.id)
                              this.pViews.splice(i, 1);
                          g.store.remove(r);
                          Ext.Msg.alert('Done!', 'View deleted from the server!');
                        }
                      }
                    });
                  }
                }
              }, this);
            }
          }
        ]
      }
    });
    Ext.canvasXpress.ViewManager.superclass.constructor.apply(this);
  }
});
Ext.canvasXpress.ResultList = Ext.extend(Ext.Window, {
  constructor: function(config, extCanvas) {
    if(!config || !extCanvas || !config.list)
      Ext.Msg.alert('Error', 'ResultList must be called with config.list and extcanvas parameters!');
    // assemble the store
    var results = [], en = extCanvas.canvas.nodes;
    for(var i = 0; i < config.list.length; i++)
    {
      var o = config.list[i];
      if(o.id && !(o.id1 && o.id2))
        results.push([o, (o.anchor? 'Anchor ' : '') + 'Node', extCanvas.getName(o), o.tooltip]);
      else
        results.push([o, (o.anchor? 'Anchor ' : '') + 'Edge', extCanvas.getName(en[o.id1]) + ' - ' + extCanvas.getName(en[o.id2]), o.tooltip]);
    }
    var win = this;
    Ext.apply(this, {
      title: 'Search Result List',
      width: 400,
      height: 300,
      stateId: 'extcxp-resultlist',
      stateful: true,
      stateEvents: ['move','resize'],
      constrainHeader: true,
      layout: 'anchor',
      items: [{
          xtype: 'grid',
          anchor: '100% -2',
          autoExpandColumn: 'tooltip',
          store: new Ext.data.ArrayStore({
            autoDestroy: true,
            data: results,
            fields: [ 'obj', 'type', 'label', 'tooltip' ]
          }),
          sm: new Ext.grid.RowSelectionModel({
            listeners: {
              scope: extCanvas,
              rowselect: function(sm, idx, r) {
                var d = r.data;
                if(d.type == 'Node')
                {
                  this.canvas.highlightNode = [d.obj.id];
                  this.canvas.draw();
                }
              }
            }
          }),
          cm: new Ext.grid.ColumnModel({
              defaults: { sortable: true, width: 100 },
              columns: [
                { header: "Type", dataIndex:'type' },
                { header: "Label", dataIndex:'label', width: 160 },
                { header: "Tooltip", dataIndex:'tooltip', id: 'tooltip' }
            ]}),
          listeners: {
            scope: extCanvas,
            rowdblclick: function(g, idx, e) {
              rows = g.getSelectionModel().getSelections();
              if(rows && rows.length)
              {
                var o = rows[0].data.obj, isNode = o.id && !(o.id1 && o.id2), w;
                if(isNode)
                {
                  this.canvas.flashNode(o.id);
                  w = new Ext.canvasXpress.nodeDialog(o, this);
                }
                else
                  w = new Ext.canvasXpress.edgeDialog(o, null, this);
                w.show();
              }
            }
          }
        }],
      bbar: {
        items: [
          {
            text: 'Clear Highlight',
            scope: extCanvas,
            handler : function(b) {
              this.canvas.highlightNode = [];
              this.canvas.draw();
            }
          },
          '->',
          {
            text: 'Flash the Selected Node(s)',
            scope: extCanvas,
            handler : function(b) {
              var grid = win.items.items[0], rows = grid.getSelectionModel().getSelections();
              if(rows && rows.length)
              {
                var nids = [];
                for(var i = 0; i < rows.length; i++)
                {
                  var o = rows[i].data.obj;
                  if(o.id && !(o.id1 && o.id2))
                    nids.push(o.id);
                }
                this.canvas.flashNode(nids);
              }
            }
          },
          {
            text: 'Select the Selected Node(s) on the Pathway',
            scope: extCanvas,
            handler : function(b) {
              var grid = win.items.items[0], rows = grid.getSelectionModel().getSelections();
              if(rows && rows.length)
              {
                var nids = [];
                for(var i = 0; i < rows.length; i++)
                {
                  var o = rows[i].data.obj;
                  if(o.id && !(o.id1 && o.id2))
                    nids.push(o.id);
                }
                this.canvas.setSelectNodes(nids);
                this.canvas.draw();
              }
            }
          },
          {
            text: 'Edit the Selected Nodes',
            scope: extCanvas,
            handler : function(b) {
              var grid = win.items.items[0], rows = grid.getSelectionModel().getSelections();
              if(rows && rows.length)
              {
                var ns = [];
                for(var i = 0; i < rows.length; i++)
                {
                  var o = rows[0].data.obj;
                  if(o.id && !(o.id1 && o.id2))
                    ns.push(o);
                }
                if(ns.length)
                {
                  w = new Ext.canvasXpress.nodeDialog(o, this);
                  w.show();
                }
                else Ext.Msg.alert('Warning', 'You have not selected any node!');
              }
            }
          }
        ]
      }
    });
    Ext.canvasXpress.ResultList.superclass.constructor.apply(this);
  }
});
Ext.reg('canvasxpress', Ext.canvasXpress);
