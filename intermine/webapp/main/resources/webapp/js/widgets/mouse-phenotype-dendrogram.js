new Error('This widget cannot be called directly');

/**
 *      _/_/_/  _/      _/   
 *       _/    _/_/  _/_/     InterMine Report Widget
 *      _/    _/  _/  _/      (C) 2012 InterMine, University of Cambridge.
 *     _/    _/      _/       http://intermine.org
 *  _/_/_/  _/      _/
 *
 *  Name: #@+TITLE
 *  Author: #@+AUTHOR
 *  Description: #@+DESCRIPTION
 *  Version: #@+VERSION
 *  Generated: Mon, 01 Oct 2012 11:59:27 GMT
 */

(function() {
var root = this;

  /**#@+ the presenter */
  var AssertException, RadialDendrogram, TreeDendrogram, Widget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  
  AssertException = (function() {
  
    function AssertException(message) {
      this.message = message;
    }
  
    AssertException.prototype.toString = function() {
      return "AssertException: " + this.message;
    };
  
    return AssertException;
  
  })();
  
  /*
  Set the assertion on the window object.
  @param {boolean} exp Expression to be truthy
  @param {string} message Exception text to show if `exp` is not truthy fruthy
  */
  
  
  this.assert = function(exp, message) {
    if (!exp) {
      throw new AssertException(message);
    }
  };
  
  Widget = (function() {
  
    Widget.prototype.pq = {
      alleleTerms: {
        "select": ["Gene.symbol", "Gene.alleles.id", "Gene.alleles.genotypes.id", "Gene.alleles.genotypes.phenotypeTerms.id", "Gene.alleles.genotypes.phenotypeTerms.name"],
        "constraints": []
      },
      highLevelTerms: {
        "select": ["Allele.highLevelPhenotypeTerms.name", "Allele.highLevelPhenotypeTerms.relations.childTerm.name"],
        "constraints": []
      }
    };
  
    Widget.prototype.alleleTerms = function(cb) {
      var pq,
        _this = this;
      assert(this.config.symbol != null, '`symbol` of the gene in question not provided');
      pq = this.pq.alleleTerms;
      pq.constraints.push({
        "path": "Gene",
        "op": "LOOKUP",
        "value": this.config.symbol
      });
      return this.service.query(pq, function(q) {
        return q.records(function(records) {
          var allele, genotype, term, terms, _i, _j, _k, _len, _len1, _len2, _ref, _ref1, _ref2;
          _this.max = 1;
          terms = {};
          _ref = records[0]['alleles'];
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            allele = _ref[_i];
            _ref1 = allele.genotypes;
            for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
              genotype = _ref1[_j];
              _ref2 = genotype.phenotypeTerms;
              for (_k = 0, _len2 = _ref2.length; _k < _len2; _k++) {
                term = _ref2[_k];
                if (terms[term.name] != null) {
                  terms[term.name].count += 1;
                  if (terms[term.name].count > _this.max) {
                    _this.max = terms[term.name].count;
                  }
                } else {
                  terms[term.name] = {
                    'count': 1
                  };
                }
              }
            }
          }
          _this.band = _this.max / 4;
          return cb(terms);
        });
      });
    };
  
    Widget.prototype.highLevelTerms = function(children, cb) {
      var k, pq, v,
        _this = this;
      assert(cb != null, 'callback `cb` needs to be provided, we use async data loading');
      assert(this.config.symbol != null, '`symbol` of the gene in question not provided');
      assert(this.band != null, '`band` of allele counts not provided');
      pq = this.pq.highLevelTerms;
      pq.constraints.push({
        "path": "Allele.highLevelPhenotypeTerms.relations.childTerm.name",
        "op": "ONE OF",
        "values": (function() {
          var _results;
          _results = [];
          for (k in children) {
            v = children[k];
            _results.push(k);
          }
          return _results;
        })()
      });
      return this.service.query(pq, function(q) {
        return q.rows(function(rows) {
          var child, parent, t, terms, _i, _len, _ref;
          terms = {};
          for (_i = 0, _len = rows.length; _i < _len; _i++) {
            _ref = rows[_i], parent = _ref[0], child = _ref[1];
            if (terms[parent] != null) {
              terms[parent].children.push({
                'name': child,
                'count': children[child].count,
                'band': Math.floor(children[child].count / _this.band),
                'depth': 2
              });
            } else {
              terms[parent] = {
                'name': parent,
                'children': [],
                'depth': 1
              };
            }
          }
          _this.hlts = [];
          terms = (function() {
            var _j, _len1, _ref1, _results,
              _this = this;
            _ref1 = _(terms).toArray();
            _results = [];
            for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
              t = _ref1[_j];
              if (t.children.length !== 0) {
                _results.push((function() {
                  _this.hlts.push(t.name);
                  return t;
                })());
              }
            }
            return _results;
          }).call(_this);
          return cb({
            'name': _this.config.symbol,
            'children': terms,
            'depth': 0
          });
        });
      });
    };
  
    function Widget(config, templates) {
      this.config = config;
      this.templates = templates;
      this.dendrogram = __bind(this.dendrogram, this);
  
      this.renderGraph = __bind(this.renderGraph, this);
  
      this.service = new intermine.Service({
        'root': 'http://metabolicmine.org/beta/service/'
      });
    }
  
    Widget.prototype.render = function(target) {
      var _this = this;
      this.target = target;
      $(this.target).html(this.templates.widget({
        'title': "Alleles phenotype terms for " + this.config.symbol
      }));
      return this.alleleTerms(function(children) {
        return _this.highLevelTerms(children, _this.renderGraph);
      });
    };
  
    /*
        Once data are loaded or updated, render the dendrogram and init config for it.
        @param {object} data A root to children object to render.
    */
  
  
    Widget.prototype.renderGraph = function(data) {
      var l, tangle, term, widget, _i, _len, _ref,
        _this = this;
      assert(typeof data === 'object' && (data.children != null), '`data` needs to be an Object with `children`');
      assert(this.target != null, 'need to have a target for rendering defined');
      assert(typeof Tangle !== "undefined" && Tangle !== null, 'Tangle lib does not seem to be loaded');
      assert(this.band != null, '`band` of allele counts not provided');
      assert(this.max != null, '`max` top allele count not provided');
      assert((this.hlts != null) && this.hlts instanceof Array, '`hlts` needs to be populated by an Array of High Level Terms');
      $(this.target).find('.config').html(this.templates.config());
      l = $(this.target).find('.config .terms ul');
      _ref = this.hlts;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        term = _ref[_i];
        l.append($('<li/>', {
          'class': 'option',
          'text': term
        }));
      }
      $(this.target).find('.config .terms .option').click(function(e) {
        assert(typeof tangle !== "undefined" && tangle !== null, 'wow a bit too fast there fella');
        $(_this.target).find('.config .terms .option.selected').removeClass('selected');
        $(e.target).addClass('selected');
        return tangle.setValue('showCategory', $(e.target).text());
      });
      $(this.target).find('.config .types .option').click(function(e) {
        assert(typeof tangle !== "undefined" && tangle !== null, 'wow a bit too fast there fella');
        $(_this.target).find('.config .types .option.selected').removeClass('selected');
        $(e.target).addClass('selected');
        return tangle.setValue('type', $(e.target).text());
      });
      widget = this;
      return tangle = new Tangle($(this.target).find('.config')[0], {
        initialize: function() {
          this.termTextBand = 3;
          this.hideTermsBand = 2;
          this.showCategory = 'all';
          return this.type = 'radial';
        },
        update: function() {
          this.termTextCount = (this.termTextBand - 1) * widget.band;
          this.hideTermsCount = (this.hideTermsBand - 1) * widget.band;
          return widget.dendrogram(data, this);
        }
      });
    };
  
    Widget.prototype.dendrogram = function(data, opts) {
      var filterChildren, params, target;
      assert(this.target != null, 'need to have a target for rendering defined');
      assert(this.config.width != null, 'need to provide a `width` for the chart');
      assert(this.config.height != null, 'need to provide a `height` for the chart');
      assert(typeof opts === 'object', '`opts` of the graph are not provided by Tangle, go untangle');
      data = (filterChildren = function(node, bandCutoff, category) {
        var ch, children, _i, _len, _ref;
        assert(node != null, '`node` not provided');
        assert(bandCutoff != null, '`bandCutoff` not provided');
        assert(category != null, '`category` not provided');
        if (node.depth === 1) {
          if (!(node.children != null) || node.children.length === 0) {
            return;
          }
          if (category !== 'all' && node.name !== category) {
            return;
          }
        }
        if (!((node.children != null) && node.children.length > 0)) {
          return node;
        }
        assert(node.children != null, 'need children at this point, knock me up!');
        children = [];
        _ref = node.children;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          ch = _ref[_i];
          if (!((ch.band != null) && ch.band < (bandCutoff - 1))) {
            ch = filterChildren(ch, bandCutoff, category);
            if (ch != null) {
              children.push(ch);
            }
          }
        }
        if (children.length !== 0) {
          return {
            'name': node.name,
            'count': node.count,
            'band': node.band,
            'children': children
          };
        }
      })(data, opts.hideTermsBand, opts.showCategory);
      target = $(this.target).find('.graph');
      target.empty();
      if (data == null) {
        return target.html($('<div/>', {
          'class': 'alert-box',
          'text': 'Nothing to show. Adjust the filters above to display the graph.'
        }));
      }
      params = {
        'termTextBand': opts.termTextBand,
        'data': data,
        'width': this.config.width,
        'height': this.config.height,
        'el': target[0]
      };
      switch (opts.type) {
        case 'radial':
          return new RadialDendrogram(params);
        case 'tree':
          return new TreeDendrogram(params);
      }
    };
  
    return Widget;
  
  })();
  
  RadialDendrogram = (function() {
  
    function RadialDendrogram(opts) {
      var arc, cluster, d, depths, diagonal, key, link, links, n, node, nodes, rx, ry, sort, value, vis, _i, _j, _len, _len1, _ref;
      assert((opts.width != null) && typeof opts.width === 'number', '`width` is missing and needs to be a number');
      assert((opts.height != null) && typeof opts.height === 'number', '`height` is missing and needs to be a number');
      assert((opts.el != null) && typeof opts.el === 'object', '`el` is missing and needs to be an HTMLDivElement');
      assert(typeof opts.data === 'object', '`data` need to be provided in an Object form, read up on D3.js');
      assert(opts.termTextBand != null, "`termTextBand` representing the node text cutoff not present");
      for (key in opts) {
        value = opts[key];
        this[key] = value;
      }
      rx = this.width / 2;
      ry = this.height / 2;
      sort = function(a, b) {
        switch (a.depth - b.depth) {
          case -1:
            return -1;
          case 1:
            return 1;
          default:
            if ((a.count != null) && (b.count != null)) {
              return a.count - b.count;
            }
        }
        return 0;
      };
      cluster = d3.layout.cluster().size([360, ry - 50]).sort(sort);
      diagonal = d3.svg.diagonal.radial().projection(function(d) {
        return [d.y, d.x / 180 * Math.PI];
      });
      vis = d3.select(this.el).append("svg:svg").attr("width", this.width).attr("height", this.height).append("svg:g").attr("transform", "translate(" + rx + "," + ry + ")");
      arc = vis.append("svg:path").attr("class", "arc").attr("d", d3.svg.arc().innerRadius(ry - 50).outerRadius(ry - 20).startAngle(0).endAngle(2 * Math.PI));
      nodes = cluster.nodes(this.data);
      links = vis.append("svg:g").attr("class", "links");
      _ref = cluster.links(nodes);
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        link = _ref[_i];
        links.append("svg:path").attr("class", link.target.band != null ? "link band-" + link.target.band : 'link').attr("d", diagonal(link));
      }
      n = vis.append("svg:g").attr("class", "nodes");
      depths = [n.append("svg:g").attr("class", "tier depth-2"), n.append("svg:g").attr("class", "tier depth-1"), n.append("svg:g").attr("class", "tier depth-0")];
      for (_j = 0, _len1 = nodes.length; _j < _len1; _j++) {
        d = nodes[_j];
        node = depths[Math.abs(d.depth - 2)].append("svg:g").attr("class", d.count != null ? "node depth-" + d.depth + " count-" + d.count : "node depth-" + d.depth).attr("transform", "rotate(" + (d.x - 90) + ")translate(" + d.y + ")");
        node.append("svg:circle").attr("r", Math.abs(d.depth - 6)).attr("class", d.band ? "band-" + d.band : void 0);
        node.append("svg:title").text(d.name);
        if (!(d.band != null) || d.band > (this.termTextBand - 2)) {
          node.append("svg:text").attr("dx", d.x < 180 ? 8 : -8).attr("dy", ".31em").attr("text-anchor", d.x < 180 ? "start" : "end").attr("transform", d.x < 180 ? null : "rotate(180)").text(d.name.length > 50 ? d.name.slice(0, 50) + '...' : d.name);
        }
      }
    }
  
    return RadialDendrogram;
  
  })();
  
  TreeDendrogram = (function() {
  
    function TreeDendrogram(opts) {
      var cluster, d, depths, diagonal, key, link, links, n, node, nodes, sort, value, vis, _i, _j, _len, _len1, _ref;
      assert((opts.width != null) && typeof opts.width === 'number', '`width` is missing and needs to be a number');
      assert((opts.height != null) && typeof opts.height === 'number', '`height` is missing and needs to be a number');
      assert((opts.el != null) && typeof opts.el === 'object', '`el` is missing and needs to be an HTMLDivElement');
      assert(typeof opts.data === 'object', '`data` need to be provided in an Object form, read up on D3.js');
      assert(opts.termTextBand != null, "`termTextBand` representing the node text cutoff not present");
      for (key in opts) {
        value = opts[key];
        this[key] = value;
      }
      sort = function(a, b) {
        switch (b.depth - a.depth) {
          case -1:
            return -1;
          case 1:
            return 1;
          default:
            if ((a.count != null) && (b.count != null)) {
              return b.count - a.count;
            }
        }
        return 0;
      };
      cluster = d3.layout.cluster().size([this.height, this.width / 2]).sort(sort);
      diagonal = d3.svg.diagonal().projection(function(d) {
        return [d.y, d.x];
      });
      vis = d3.select(this.el).append("svg").attr("width", this.width).attr("height", this.height).append("g").attr("transform", "translate(120, 0)");
      nodes = cluster.nodes(this.data);
      links = vis.append("svg:g").attr("class", "links");
      _ref = cluster.links(nodes);
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        link = _ref[_i];
        links.append("svg:path").attr("class", link.target.band != null ? "link band-" + link.target.band : 'link').attr("d", diagonal(link));
      }
      n = vis.append("svg:g").attr("class", "nodes");
      depths = [n.append("svg:g").attr("class", "tier depth-2"), n.append("svg:g").attr("class", "tier depth-1"), n.append("svg:g").attr("class", "tier depth-0")];
      for (_j = 0, _len1 = nodes.length; _j < _len1; _j++) {
        d = nodes[_j];
        node = depths[Math.abs(d.depth - 2)].append("svg:g").attr("class", d.count != null ? "node depth-" + d.depth + " count-" + d.count : "node depth-" + d.depth).attr("transform", "translate(" + d.y + "," + d.x + ")");
        node.append("svg:circle").attr("r", Math.abs(d.depth - 6)).attr("class", d.band ? "band-" + d.band : void 0);
        node.append("svg:title").text(d.name);
        if (!(d.band != null) || d.band > (this.termTextBand - 2)) {
          node.append("svg:text").attr("dx", d.children ? -8 : 8).attr("dy", "3").attr("text-anchor", d.children ? "end" : "start").text(d.name.length > 50 ? d.name.slice(0, 50) + '...' : d.name);
        }
      }
    }
  
    return TreeDendrogram;
  
  })();
  
  /**#@+ the config */
  var config = #@+CONFIG;

  /**#@+ the templates */
  var templates = {};
  templates.config=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<p><strong>Show term text</strong> when band is <span data-var="termTextBand" class="TKAdjustableNumber" data-min="1" data-max="5"></span> or higher corresponding to allele count of <span data-var="termTextCount"></span> or higher.</p>\n\n<div><strong>Hide terms</strong> when band is <span data-var="hideTermsBand" class="TKAdjustableNumber" data-min="1" data-max="5"></span> or higher corresponding to allele count of <span data-var="hideTermsCount"></span> or higher.</div>\n\n<div class="terms">Show <span class="option selected">all</span> or only <ul></ul> terms.</div>\n\n<div class="types">Use a <a class="option selected">radial</a> or <a class="option">tree</a> <strong>dendrogram</strong>.</div>')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")};
  templates.widget=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push("<h4>"),t.push(r(this.title)),t.push('</h4>\n<div class="config"></div>\n<div class="graph"></div>')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")};
  
  /**#@+ css */
  var style = document.createElement('style');
  style.type = 'text/css';
  style.innerHTML = 'div#w#@+CALLBACK path.arc{fill:#FFF}div#w#@+CALLBACK .node.depth-0{font-size:18px}div#w#@+CALLBACK .node.depth-1{font-size:14px}div#w#@+CALLBACK .node.depth-2{font-size:10px}div#w#@+CALLBACK .node circle{fill:#FFF;stroke:#CCC;stroke-width:1.5px}div#w#@+CALLBACK .node.depth-2 circle{stroke:#FEE5D9;fill:#FEE5D9}div#w#@+CALLBACK .node circle.band-1{stroke:#FCAE91;fill:#FCAE91}div#w#@+CALLBACK .node circle.band-2{stroke:#FB6A4A;fill:#FB6A4A}div#w#@+CALLBACK .node circle.band-3{stroke:#DE2D26;fill:#DE2D26}div#w#@+CALLBACK .node circle.band-4{stroke:#A50F15;fill:#A50F15}div#w#@+CALLBACK .link{fill:none;stroke:#CCC;stroke-width:1px}div#w#@+CALLBACK .link.band-0{stroke:#FEE5D9}div#w#@+CALLBACK .link.band-1{stroke:#FCAE91}div#w#@+CALLBACK .link.band-2{stroke:#FB6A4A}div#w#@+CALLBACK .link.band-3{stroke:#DE2D26}div#w#@+CALLBACK .link.band-4{stroke:#A50F15}div#w#@+CALLBACK .config{background:#FFF;padding:20px;box-shadow:0 0 10px #CCC}div#w#@+CALLBACK .config>div{margin:0}div#w#@+CALLBACK .config>div:not(:last-child){margin-bottom:10px}div#w#@+CALLBACK .config ul{display:inline;margin:0;list-style-type:none}div#w#@+CALLBACK .config ul li{display:inline-block;white-space:nowrap;margin-top:6px}div#w#@+CALLBACK .config ul li:not(:last-child){margin-right:5px}div#w#@+CALLBACK .config ul li:not(:last-child):after{content:\',\';color:#222}div#w#@+CALLBACK .config .option{color:#46f;border-bottom:1px dashed #46f;cursor:pointer;position:relative}div#w#@+CALLBACK .config .option.selected{color:#00c}div#w#@+CALLBACK .config .option:not(.selected):hover:before{content:\'select\';color:#00f;position:absolute;top:-6px;left:0;font:9px "Helvetica-Neue","Arial",sans-serif}div#w#@+CALLBACK .alert-box{margin-top:10px}';
  document.head.appendChild(style);
  
  /**#@+ callback */
  (function() {
    var parent, part, _i, _len, _ref;
    parent = this;
    _ref = 'intermine.temp.widgets'.split('.');
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      part = _ref[_i];
      parent = parent[part] = parent[part] || {};
    }
  }).call(root);
  root.intermine.temp.widgets['#@+CALLBACK'] = new Widget(config, templates);

}).call(this);