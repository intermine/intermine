// icons in for this tool comes from http://prothemedesign.com/circular-icons/
// license says free for any use but no re-distribution, so please download it
// directly from that site yourself or make/choose your own icons
Ext.canvasXpress = Ext.extend(Ext.Panel, {
  data: false,
  options: false,
  events: false,
  contextMenu: true,
  menuTitle: 'Customize',
  showPrint: true,
  changedNodes: [],
  changedEdges: [],
  removedNodes: [], // should be handled for removenode and saveallchanges events by child class that does not commit delete changes immediately to DB (which causes nodeIndice issue etc.)
  removedEdges: [], // same as above (removeedge instead of removenode of course)
  initComponent: function() {
    Ext.canvasXpress.superclass.initComponent.apply(this, arguments);
    this.resetChanges();
  },
  onRender:function() {
    Ext.canvasXpress.superclass.onRender.apply(this, arguments);
    if (this.contextMenu) {
      this.el.on({contextmenu:{scope:this, fn:this.onContextMenu, stopEvent:true}});
    }
  },
  afterRender: function() {
    Ext.canvasXpress.superclass.afterRender.apply(this, arguments);
    this.canvasId = this.id + 'canvas';
    // To cope with the remote services divs we resize the canvas
    var dw = this.options.decreaseWidth ? this.options.decreaseWidth : 0;
    var dh = this.options.decreaseHeight ? this.options.decreaseHeight : 0;
    var pw = this.el.dom.parentNode ? this.el.dom.parentNode.clientWidth - dw : 500;
    var ph = this.el.dom.parentNode ? this.el.dom.parentNode.clientHeight - dh : 500;
    // Add the canvas tag
    Ext.DomHelper.append(this.body, {
      tag: 'canvas',
      id: this.canvasId,
      width: dw && pw ? pw : this.width || pw,
      height: dh && ph ? ph : this.height || ph
    });
    // first set up default events
    var events = {};
    switch(this.options.graphType) {
      case 'Network':
        this.addEvents(
          'endnodedrag','removenode','removeedge',
          'expandgroup','togglenode','togglechildren','saveallchanges',
          'updatenode','updateedge','updatenodes','updateedges',
          'updatelegend','updateorder','leftclick','importdata');
        events.enddragnode = this.endDrag.createDelegate(this);
        if(this.hasListener('leftclick'))
          events.click = function(o, e) {
            this.fireEvent('leftclick', o, e)
          }.createDelegate(this);
        if(!this.hasListener('removenode'))
          this.on('removenode', function(n, f) { f() });
        if(!this.hasListener('removeedge'))
          this.on('removeedge', function(n, f) { f() });
    }
    Ext.applyIf(this.events, events);
    // Now get the canvasXpress object
    this.canvas = new CanvasXpress(this.canvasId, this.data, this.options, this.events);
    this.canvas.Ext = this;
    this.canvas.highlightNode = []; // clear up the initial highlight
    var ss = this.canvas.shapes.sort();
    if(ss[0] != 'image') ss.unshift('image');
    if(ss[0] != 'custom') ss.unshift('custom');

    if (this.canvas.version < 2) {
      var msg = 'Please download a newer version of canvasXpress at:<br>';
      msg += '<a target="blank" src="http://www.canvasXpress.org">http://www.canvasXpress.org</a><br>';
      msg += 'You are using an older version that dO NOT support all the functionality of this panel';
      Ext.MessageBox.alert('Warning', msg);
    }
    this.on('destroy', function(p) {
      if(p.snapshotCtrl) p.snapshotCtrl.close();
    });
    this.on('beforedestroy', function(p) {
      p.canvas.destroy();
    });
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
  networkMenu: function(o, type, it) {
    var xy = this.canvas.adjustedCoordinates(this.clickXY);
    var items = [];
    if(type == 'cs') // right click menu only
    {
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
        if(o.nodes.length > 1)
        {
          var n = o.nodes;
          items.push({
            icon: this.imgDir + 'edit.png',
            text: 'Edit the Selected Nodes',
            handler: this.editNode.createDelegate(this, [n], true)
          }, {
            icon: this.imgDir + 'delete.png',
            text: 'Delete the Selected Nodes',
            scope: this,
            handler: this.deleteNode.createDelegate(this, [n])
          }, {
            icon: this.imgDir + 'cancel.png',
            text: 'Hide the Selected Nodes',
            handler: this.toggleNode.createDelegate(this, [n, true])
          });
          Ext.canvasXpress.utils.addMenu(items, n, this.nodeMenu, this);
        }
        else
        {
          var n = o.nodes[0], label = n.label || n.name || n.id;
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
                var n1 = e.id1 == id? en[e.id2] : en[e.id1], label1 = n1.label || n1.name || n1.id;
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
            s = (n1.label || n1.name || n1.id) + ' - ' + (n2.label || n2.name || n2.id);
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
        var hidden = this.canvas.getHiddenNodes(), hn = [];
        for(var i = 0; i < hidden.length; i++)
        {
          var sn = hidden[i];
          hn.push({
            icon: this.imgDir + 'eye.png',
            text: sn.label || sn.name || sn.id,
            handler: this.toggleNode.createDelegate(this, [sn, false])
          });
        }
        if(hn.length)
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
        if(this.showNetworkEditItems)
        {
          items.push('-', {
            text: 'Edit Network',
            icon: this.imgDir + 'edit.png',
            scope: this,
            handler: this.editNetwork
          });
          if(this.hasUnsavedChanges() && this.hasListener('saveallchanges'))
            items.push({
              text: 'Save the Network Now',
              icon: this.imgDir + 'save.png',
              scope: this,
              handler: this.saveMap
            });
        }
        items.push('-',
          {
            icon: this.imgDir + 'magnify.png',
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
          }, '-',
          {
            icon: this.snapshotCtrl && !this.snapshotCtrl.hidden? this.imgDir + 'power_off.png' : this.imgDir + 'power_on.png',
            text: this.snapshotCtrl && !this.snapshotCtrl.hidden? 'Hide Snapshot Control':'Show Snapshot Control',
            scope: this,
            handler: this.toggleSnapshotCtrl
          }
        );
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
          items.push({
            text: 'Advanced Test',
            menu: ti
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
  hasUnsavedChanges: function() {
    return this.changedNodes.length || this.changedEdges.length || this.removedNodes.length ||
           this.removedEdges.length || this.legendChanged || this.orderChanged || this.networkChanged;
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
    return this.buildGroupMenu('fontName', this.canvas.fonts);
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
      this.fireEvent('endnodedrag', n.nodes, function(s) {
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
    this.fireEvent('expandgroup', n, this.removeNode.createDelegate(this, [n]));
    this.canvas.draw();
  },
  toggleNode: function(n, hide) {
    var ns = [];
    if(n.length && !n.id)
      for(var i = 0; i < n.length; i++)
        ns.push(n[i].id);
    else ns.push(n.id);
    this.canvas.hideUnhideNodes(ns, hide);
    this.canvas.selectNode = [];
    this.addNode(n);
    this.fireEvent('togglenode', n, hide, this.removeNode.createDelegate(this, [n]));
    this.canvas.draw();
  },
  changeNodeOrder: function(n, dir) {
    this.canvas[dir](n);
    this.updateOrder();
    this.canvas.draw();
  },
  copyObj: function(b, e, type, o) {
    var w;
    o = this.canvas.cloneObject(o);
    delete o.data;
    if(type == 'node')
    {
      delete o.id;
      w = new Ext.canvasXpress.nodeDialog(o, this);
    }
    else if(type == 'edge')
    {
      delete o.id1;
      delete o.id2;
      w = new Ext.canvasXpress.edgeDialog(o, null, this);
    }
    w.show();
  },
  editNode: function(b, e, o) {
    var w = new Ext.canvasXpress.nodeDialog(o, this);
    w.show();
  },
  editEdge: function(b, e, o, dir) {
    var w = new Ext.canvasXpress.edgeDialog(o, dir, this);
    w.show();
  },
  showSearchWin: function(allLabels, allNames, sc, es, scall, supported, sf) {
    for(var i in this.canvas.nodes)
    {
      var l = this.canvas.nodes[i], label = l.label || l.id;
      allLabels.push([label, label]); // val, displayVal (if setting id as val, more changes is needed to ensure id instead of label is searched)
      allNames.push(l.name);
    }
    allLabels.sort(function(a,b){return a[1]>b[1]?1:a[1]<b[1]?-1:0});
    allNames.sort();

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
        ['Node Name','node.name','string',allNames],
        ['Node Parent','node.parentNode','string',allLabels,{exactMatch:true}],
        ['Node Size','node.size','number'],
        ['Node Width','node.width','number'],
        ['Node Height','node.height','number'],
        ['Node Rotate','node.rotate','number'],
        ['Node Shape','node.shape','string',this.canvas.shapes.sort(),{exactMatch:true}],
        ['Node Color','node.color','string'],
        ['Node Outline','node.outline','string'],
        ['Node Pattern','node.pattern','string'],
        ['Node From','node.from','custom',[this.nodeConnectEdge.createDelegate(this, 'from', true)]],
        ['Node To','node.to','custom',[this.nodeConnectEdge.createDelegate(this, 'to', true)]],
        ['Edge From','edge.id1','string',allLabels,{takeInput:true}],
        ['Edge To','edge.id2','string',allLabels,{takeInput:true}],
        ['Edge Width','edge.width','number'],
        ['Edge Linetype','edge.type','string',this.canvas.lines.sort(),{exactMatch:true}],
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
        ['Name','name','string',allNames],
        ['Parent','parentNode','string',allLabels,{exactMatch:true}],
        ['Size','size','number'],
        ['Width','width','number'],
        ['Height','height','number'],
        ['Rotate','rotate','number'],
        ['Shape','shape','string',this.canvas.shapes.sort(),{exactMatch:true}],
        ['Color','color','string'],
        ['Outline','outline','string'],
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
        ['Width','width','number'],
        ['Linetype','type','string',this.canvas.lines.sort(),{exactMatch:true}],
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
    this.fireEvent('updatelegend', function(s) {
        if(s) this.legendChanged = false;
      }.createDelegate(this));
  },
  updateOrder: function() {
    this.orderChanged = true;
    this.fireEvent('updateorder', function(s) {
        if(s) this.orderChanged = false;
      }.createDelegate(this));
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
        this.fireEvent('removenode', n, function() {
          for(var i = 0; i < n.length; i++)
            this.canvas.removeNode(n[i]);
          this.canvas.draw();
          this.updateOrder(); // change of # of node causes nodeIndices to change, it has to be consistent!!
        }.createDelegate(this));
      }
    }, this)
  },
  deleteEdge: function(e) {
    Ext.Msg.confirm('Warning', 'Are you sure you want to delete this edge?', function(b) {
      if(b == 'yes')
      {
        this.fireEvent('removeedge', e, function() {
          this.canvas.removeEdge(e);
          this.canvas.draw();
        }.createDelegate(this));
      }
    }, this)
  },
  editNetwork: function(b, e) {
    var config = {extCanvas:this};
    var d = new Ext.canvasXpress.networkDialog(this.networkInfo, this);
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
  },
  addNode: function(n) {
    if(typeof(n) != 'object' || !n.length) n = [n];
    var all = {};
    for(var i = 0; i < this.changedNodes.length; i++)
      all[this.changedNodes[i].id] = 1;
    for(var i = 0; i < n.length; i++)
      if(!all[n[i].id])
        this.changedNodes.push(n[i]);
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
  },
  addEdge: function(ae) {
    for(var i = 0; i < this.changedEdges.length; i++)
    {
      var e = this.changedEdges[i];
      if(e.id1 == ae.id1 && e.id2 == ae.id2)
        return;
    }
    this.changedEdges.push(ae);
  },
  resetChanges: function() {
    this.changedNodes = [];
    this.changedEdges = [];
    this.removedNodes = [];
    this.removedEdges = [];
    this.legendChanged = false;
    this.orderChanged = false;
    this.networkChanged = false;
  },
  saveMap: function(force) {
    var d = this.canvas.data, obj = {nodes:force?d.nodes:this.changedNodes, edges:force?d.edges:this.changedEdges};
    if(this.legendChanged || force) obj.legend = d.legend;
    if(this.orderChanged || force) obj.nodeIndices = d.nodeIndices;
    if(this.networkChanged || force) obj.info = this.networkInfo;
    obj.force = force;
    // handler should redraw entire canvas if nodes and/or edges were added
    this.fireEvent('saveallchanges', obj, this.resetChanges.createDelegate(this));
  },
  exchangeData: function(type) {
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
            { xtype: 'textarea', hideLabel: true, width: 473, height: type == 'Import'? 230 : 258,
              value: type.match(/Import/)? '' : JSON && JSON.stringify? JSON.stringify(a, null, 2) : Ext.encode(a) }
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
                this.canvas.updateConfig(tmp[1]);
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
                else
                {
                  this.canvas.updateData(tmp[0]);
                  this.fireEvent('importdata', tmp[0], tmp[1]); // data and userOptions
                  this.canvas.draw();
                  win.close();
                }
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
  storeSnapshot: function(xy) {
    if(this.movieURL)
    {
      this.canvas.stopSnapshotPlay(true);
      var win = new Ext.Window({
        title: 'Store Current Snapshots to Database',
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
              { fieldLabel: 'Name', value: this.snapshotsInfo? this.snapshotsInfo.name : '' },
              { fieldLabel: 'Description', value: (this.snapshotsInfo? this.snapshotsInfo.desc : ''), width: 170 },
              { xtype: 'checkbox', hideLabel: true, boxLabel: 'Shared with everyone?', checked: (this.snapshotsInfo? this.snapshotsInfo.shared : false) }
            ]
          }
        },
        bbar: {
          items: [
            '->',
            {
              text: 'Save to DB Now',
              scope: this,
              handler: function(b, e) {
                var f = b.ownerCt.ownerCt.get(0).get(0), name = f.get(0).getValue(),
                    desc = f.get(1).getValue(), shared = f.get(2).getValue();
                if(!name)
                {
                  Ext.Msg.alert('Error', 'Name must be filled out!');
                  return;
                }
                var id = (this.snapshotsInfo && this.snapshotsInfo.id)? this.snapshotsInfo.id : null;
                Ext.Ajax.request({
                  url: this.movieURL,
                  params: { action:'saveMovie', name: name, id: id,
                            pid:this.networkInfo.id,
                            data:Ext.encode(this.canvas.getSnapshotsData()),
                            desc: desc, shared: shared? '1':'0' },
                  scope: this,
                  callback: function(o, s, r) {
                    if(!s) Ext.Msg.alert('Error', 'Could not save movie to server!');
                    else
                    {
                      var res = Ext.decode(r.responseText);
                      this.snapshotsInfo.id = res.id;
                      win.close();
                      this.showSnapshotResultTip('Snapshots saved to DB', xy);
                    }
                  }
                });
                this.snapshotsInfo = {name:name, desc:desc, shared:shared};
              }
            }
          ]
        }
      });
      win.show();
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
      }, {
        icon: this.imgDir + 'power_off.png',
        itemId: 'hide',
        tooltip: 'Hide This Toolbar',
        handler: this.toggleSnapshotCtrl
      });
      var xy = e.xy? e.xy : e;
      this.snapshotCtrl = new Ext.Window({
        height: 16,
        width: 315 + (this.movieURL? 42 : 0),
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
      this.canvas.playSnapshot(time);
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
            var win = new Ext.Window({
              title: 'List of Available Snapshots',
              width: 500,
              height: 300,
              layout: 'fit',
              items: {
                xtype: 'grid',
                border: false,
                autoExpandColumn: 'ssdesc',
                store: new Ext.data.ArrayStore({
                    autoDestroy: true,
                    data: list,
                    fields: [ 'username', 'name', 'desc', 'id', 'shared' ]
                  }),
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
                    text: 'Delete Selected Snapshots',
                    scope: this,
                    handler: function(b, e) {
                      Ext.Msg.confirm('Warning', 'Are you sure you want to delete this movie?', function(t) {
                        if(t == 'yes')
                        {
                          var g = b.ownerCt.ownerCt.get(0), r = g.getSelectionModel().getSelected();
                          if(r && r.data)
                          {
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
                  },
                  {
                    text: 'Load Selected Snapshots',
                    scope: this,
                    handler: function(b, e) {
                      var g = b.ownerCt.ownerCt.get(0), r = g.getSelectionModel().getSelected();
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
                              this.showSnapshotResultTip('Snapshots loaded from DB', xy);
                            }
                          }
                        });
                      }
                    }
                  }
                ]
              }
            });
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
  }
});
Ext.canvasXpress.utils = {
  removeNode: function(all, id) {
    for(var i = 0; i < this.changedNodes.length; i++)
    {
      if(this.changedNodes[i].id == n.id)
      {
        this.changedNodes[i].splice(i, 1);
        return;
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
    var xy = this.extCanvas.canvas.adjustedCoordinates(this.extCanvas.clickXY);
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
                      this.editorDoc.designMode = "on";
                    }.createDelegate(this), 100);
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
                    this.mask.show();
                    var shared = vals.shared == 'on'? '1':'0', n = vals.name, id = panel.dbid;
                    Ext.Ajax.request({
                      url: this.extCanvas.customShapesURL,
                      params: { shared: shared, trans: vals.trans == 'on'? 1:0, name: n, id:id,
                        image: images && images.length? images[0].src : '', url: text.match(/^(http:\/\/.+?)(?:[\n\r]|$)/)? RegExp.$1 : '',
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
  constructor: function(config, extCanvas) {
    if(!config) config = {};
    this.nodeInfo = config;
    var isMulti = !!config.length;
    this.extCanvas = extCanvas;
    var allNodes = [], data = config.data || {}, parent = null;
    for(var i in extCanvas.canvas.nodes)
    {
      var l = extCanvas.canvas.nodes[i], n = l.data, id = l.label || l.name || l.id;
      if(config.id != i) allNodes.push([l.id, id]);
    }
    allNodes.sort(function(aa,bb){var a=(aa[1]+'').toLowerCase(),b=(bb[1]+'').toLowerCase();return a>b?1:a<b?-1:0});
    var h = {}, nd = this.extCanvas.nodeDialog? this.extCanvas.nodeDialog(config) : {};
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
          { xtype: 'textfield', value: isMulti? null : config.label || config.name || config.id || '',
            allowBlank: isMulti, name:'label', width: 80 },
          { xtype: 'displayfield', value: 'Size:', width: 24, style:'margin-top:3px' },
          { xtype: 'numberfield', value: isMulti? null : config.labelSize || 1,
            allowBlank: isMulti, name:'labelSize', width: 30 },
          {
            xtype: 'checkbox',
            checked: isMulti? false : config.hideName,
            boxLabel: 'Hide label?',
            name: 'hideName',
            flex: 1
          }
        ]
      },
      { fieldLabel: 'Name', value: isMulti? null : config.name || '',
        hideLabel: h.name, hidden: h.name,
        allowBlank: isMulti, name:'name' },
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
        value: isMulti? null : config.parentNode
      }
    ];
    var item2 = [
      { xtype: 'colorfield', fieldLabel: 'Color', allowBlank: isMulti,
        name:'color', value: isMulti? null : config.color || 'rgb(255,0,0)',
        hideLabel: h.color, hidden: h.color },
      { xtype: 'colorfield', fieldLabel: 'Outline', allowBlank: isMulti,
        name:'outline', value: isMulti? null : config.outline || 'rgb(255,0,0)',
        hideLabel: h.outline, hidden: h.outline },
      {
        xtype: 'compositefield',
        fieldLabel: 'Pattern',
        items: [
          { xtype:'combo', name:'pattern', hideLabel: h.pattern, hidden: h.pattern,
            triggerAction: 'all', store: ['open', 'closed'],
            width: 80, value: isMulti? null : config.pattern || 'closed' },
          { xtype: 'displayfield', value: 'Rotate:', width: 52, style:'margin-top:3px;margin-left:5px;' },
          { xtype:'numberfield', name:'rotate', hideLabel: h.rotate, hidden: h.rotate,
            width: 30, value: isMulti? null : config.rotate || '' }
        ]
      },
      {
        xtype: 'compositefield',
        fieldLabel: 'Size',
        items: [
          { xtype:'numberfield', name:'size', hideLabel: h.size, hidden: h.size,
            width: 30, value: isMulti? null : config.size || '1.0' },
          { xtype: 'displayfield', value: 'Width:', width: 35, style:'margin-top:3px' },
          { xtype:'numberfield', name:'width', hideLabel: h.width, hidden: h.width,
            width: 30, value: isMulti? null : config.width || '' },
          { xtype: 'displayfield', value: 'Height:', width: 40, style:'margin-top:3px' },
          { xtype:'numberfield', name:'height', hideLabel: h.height, hidden: h.height,
            width: 30, value: isMulti? null : config.height || '' }
        ]
      },
      {
        xtype: 'combo',
        fieldLabel: 'Shape',
        allowBlank: isMulti,
        hideLabel: h.shape, hidden: h.shape,
        emptyText: 'Selection Required',
        triggerAction: 'all',
        store: extCanvas.canvas.shapes.sort(),
        name: 'shape',
        value: isMulti? null : config.shape || 'square',
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
      { fieldLabel: 'Image Path', value: isMulti? null : config.imagePath || '',
        hideLabel: h.imagePath, hidden: h.imagePath, width: 200,
        allowBlank: true, name:'imagePath', disabled: config.shape != 'image' }
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
      var p = pf.getValues(), p1 = pf.getFieldValues(), g = gf? gf.getValues() : {};
      for(var i in g)
        p[i] = g[i];
      // assembl parameters
      p.parent = p1.parentLabel;
      if(config.id) p.id = config.id;
      if(this.nd.getParams) this.nd.getParams.call(this, config, p, tab);
      var dup = !config.id && (config.x || config.y);
      if(dup)
      {
        var dupMove = (config.x>config.y?config.y:config.x)/10;
        config.x -= dupMove;
        config.y -= dupMove;
        config.labelX -= dupMove;
        config.labelY -= dupMove;
      }
      var c = config.id || dup? {x:config.x,y:config.y} : this.extCanvas.canvas.adjustedCoordinates(this.extCanvas.clickXY); // make sure there's an X,Y that user can save to DB in callback
      p.x = c.x; p.y = c.y;
      if(config.labelX)
      {
        p.labelX = config.labelX;
        p.labelY = config.labelY;
      }

      var f = function(n, p, res) {
        n.label = p.label;
        n.hideName = p.hideName == "on";
        n.labelSize = p.labelSize-0;
        // if user sets them hidden, delegate these properties to user callbacks
        if(!h.name) n.name = p.name;
        if(!h.color) n.color = p.color;
        if(!h.shape) n.shape = p.shape;
        if(!h.imagePath) n.imagePath = p.imagePath;
        if(!h.size) n.size = p.size-0;
        if(!h.width) n.width = p.width-0;
        if(!h.height) n.height = p.height-0;
        if(!h.rotate) n.rotate = p.rotate-0;
        if(!h.outline) n.outline = p.outline;
        if(!h.pattern) n.pattern = p.pattern;
        if(!h.parent) n.parentNode = p.parent;
        if(!n.id)
        {
          if(res && res.id) n.id = res.id;
          this.extCanvas.canvas.addNode(n, dup? null : this.extCanvas.clickXY); // use existing x,y when duplicating
          this.extCanvas.updateOrder(); // change of # of node causes nodeIndices to change, it has to be consistent!!
        }
        this.extCanvas.canvas.draw();
        if(!res) // not saved to server
          this.extCanvas.addNode(n);
        else
          this.extCanvas.removeNode(n.id);
        if(!noClose) this.close();
      }.createDelegate(this);

      if(this.extCanvas.hasListener('updatenode'))
        this.extCanvas.fireEvent('updatenode', this.nodeInfo, p, f);
      else f(this.nodeInfo, p);
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
      width: nd.width || 350,
      height: nd.height || 330,
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
  }
});
Ext.canvasXpress.edgeDialog = Ext.extend(Ext.Window, {
  title: 'Network Edge Editor',
  constructor: function(config, dir, extCanvas) {
    if(dir == 'to') config = {id2:config.id};
    else if(dir == 'from') config = {id1:config.id};
    else if(!config) config = {};
    var isMulti = !!config.length;
    this.edgeInfo = config;
    this.extCanvas = extCanvas;
    var allNodes = [], data = config.data || {};
    for(var i in extCanvas.canvas.nodes)
    {
      var l = extCanvas.canvas.nodes[i], n = l.data, id = l.label || l.name || l.id;
      allNodes.push([l.id, id]);
    }
    allNodes.sort(function(aa,bb){var a=(aa[1]+'').toLowerCase(),b=(bb[1]+'').toLowerCase();return a>b?1:a<b?-1:0});
    var h = {}, ed = this.extCanvas.edgeDialog? this.extCanvas.edgeDialog(config) : {};
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
          fieldLabel: 'Start Node',
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
          value: v1? v1.id: null// v1? v1.label || v1.name || v1.id : null
        },
        {
          fieldLabel: 'End Node',
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
          value: v2? v2.id: null// v2? v2.label || v2.name || v2.id : null
        },
        { name: 'width', xtype: 'numberfield', fieldLabel: 'Width',
          hideLabel: h.width, hidden: h.width,
          value: config.width || '2.0', allowBlank: false },
        {
          fieldLabel: 'Line Type',
          hideLabel: h.linetype, hidden: h.linetype,
          emptyText: 'Selection Required',
          triggerAction: 'all',
          store: extCanvas.canvas.lines.sort(),
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
      var p = bf.getValues(), p1 = bf.getFieldValues();
      // assembl parameters
      p.id1 = p1.id1;
      p.id2 = p1.id2;
      p.entryid1 = p.id1;
      p.entryid2 = p.id2;
      if(this.ed.getParams) this.ed.getParams.call(this, config, p, tab);

      var f = function(e, p, res) {
        var add = !config.id1 || !config.id2;
        // if user sets them hidden, delegate these properties to user callbacks
        if(!h.width) e.width = p.width;
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

      if(this.extCanvas.hasListener('updateedge'))
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
        text: (config.id1 && config.id2? 'Change' : 'Add') + ' Edge',
        handler: bbf
      });
    Ext.apply(this, {
      width: ed.width || 350,
      height: ed.height || 250,
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
  constructor: function(config, extCanvas) { // config should be the network obj (with (optional) id,name,(optional) description), with a (optional) key extCanvas being the panel
    if(!config) config = {};
    var ps = [{
      title: 'Customize',
      items: [
        {
          fieldLabel: 'Name',
          allowBlank: false,
          width: 220,
          name: 'name',
          value: config.name || ''
        },
        {
          fieldLabel: 'Description',
          width: 220,
          name: 'description',
          value: config.description || ''
        },
        {
          fieldLabel: 'Display Options',
          xtype: 'textarea',
          width: 220,
          height: 150,
          name: 'options',
          value: config.options? config.options.replace(/\\n/g, '\n').replace(/^"|"$/g, '') || '{}' : '{}'
        }
      ]
    }];
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
          var p = bf.getValues(), f = function() {
            this.networkChanged = false;
          };
          var opts;
          try {
            opts = Ext.decode(p.options);
          } catch(e) {
            Ext.Msg.alert('Error', 'Invalid JSON in Display Options:' + e);
            return;
          }
          this.networkChanged = true; // unfinished. add in user panel might be needed, combine with code in tree.js

          config.name = p.name;
          config.description = p.description;
          if(extCanvas)
          {
            for(var i in opts)
              extCanvas.canvas[i] = opts[i];
            extCanvas.canvas.draw();
          }
          config.options = p.options;

          if(extCanvas)
            extCanvas.fireEvent('updatenetwork', config, f);
          else if(config.callback) config.callback(config, f);
          this.close();
        }
      });
    Ext.apply(this, {
      width: 350,
      height: 300,
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
              store: extCanvas.canvas.shapes.sort() })
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
              store: extCanvas.canvas.lines.sort() })
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

    var cm = new Ext.grid.ColumnModel({
      defaults: { sortable: true },
      columns: [
        {
          header: 'Has Border?',
          dataIndex: 'boxed',
          width: 70,
          editor: new Ext.form.Checkbox({ allowBlank: false })
        },
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
        store.insert(0, new Text({ font: 1, margin: 10, color: 'rgb(255,255,255)', boxed: true }));
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
Ext.reg('canvasxpress', Ext.canvasXpress);
