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
  var Publication, Publications, Table, Widget,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  
  Publication = (function(_super) {
  
    __extends(Publication, _super);
  
    function Publication() {
      return Publication.__super__.constructor.apply(this, arguments);
    }
  
    return Publication;
  
  })(Backbone.Model);
  
  Publications = (function(_super) {
  
    __extends(Publications, _super);
  
    function Publications() {
      return Publications.__super__.constructor.apply(this, arguments);
    }
  
    Publications.prototype.model = Publication;
  
    return Publications;
  
  })(Backbone.Collection);
  
  Table = (function(_super) {
  
    __extends(Table, _super);
  
    function Table() {
      return Table.__super__.constructor.apply(this, arguments);
    }
  
    Table.prototype.page = 0;
  
    Table.prototype.size = 10;
  
    Table.prototype.events = {
      'click ul.pages a': 'changePage',
      'keyup input.symbol': 'changeSymbol'
    };
  
    Table.prototype.initialize = function(opts) {
      var k, v, _results;
      _results = [];
      for (k in opts) {
        v = opts[k];
        _results.push(this[k] = v);
      }
      return _results;
    };
  
    Table.prototype.render = function() {
      $(this.el).html(this.template({
        'rows': this.collection.length !== 0 ? this.collection.toJSON().splice(this.size * this.page, this.size) : [],
        'symbol': this.symbol,
        'pages': Math.ceil(this.collection.length / this.size),
        'current': this.page,
        'count': this.collection.length
      }));
      return this;
    };
  
    Table.prototype.changePage = function(e) {
      this.page = parseInt($(e.target).text()) - 1;
      return this.render();
    };
  
    Table.prototype.changeSymbol = function(e) {
      var done,
        _this = this;
      done = function() {
        var symbol;
        symbol = $(e.target).val();
        if (symbol !== '' && symbol !== _this.symbol) {
          return _this.data(symbol, function(records) {
            _this.symbol = symbol;
            _this.collection = new Publications(records);
            return _this.render();
          });
        }
      };
      if (this.timeout != null) {
        clearTimeout(this.timeout);
      }
      return this.timeout = root.setTimeout(done, 500);
    };
  
    return Table;
  
  })(Backbone.View);
  
  Widget = (function() {
  
    function Widget(config, templates) {
      this.config = config;
      this.templates = templates;
      this.data = __bind(this.data, this);
  
      this.service = new intermine.Service({
        'root': this.config.mine
      });
    }
  
    Widget.prototype.data = function(symbol, done) {
      var loading, pq, _ref,
        _this = this;
      $((_ref = this.view) != null ? _ref.el : void 0).hide();
      $(this.target).prepend(loading = $('<div class="alert-box">Loading &hellip;</div>'));
      pq = this.config.pathQueries.pubsForGene;
      pq.where = {
        'symbol': {
          '=': symbol
        }
      };
      return this.service.query(pq, function(q) {
        return q.records(function(records) {
          var _ref1;
          $((_ref1 = _this.view) != null ? _ref1.el : void 0).show();
          loading.remove();
          if (records.length === 1 && (records[0].publications != null)) {
            return done(records.pop().publications);
          } else {
            return done([]);
          }
        });
      });
    };
  
    Widget.prototype.render = function(target) {
      var _this = this;
      this.target = target;
      return this.data(this.config.symbol, function(records) {
        _this.view = new Table({
          'collection': new Publications(records),
          'template': _this.templates.table,
          'symbol': _this.config.symbol,
          'data': _this.data
        });
        return $(_this.target).html(_this.view.render().el);
      });
    };
  
    return Widget;
  
  })();
  
  /**#@+ the config */
  var config = #@+CONFIG;

  /**#@+ the templates */
  var templates = {};
  templates.table=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){var e,n,i,s,o,u,a,f,l,c,h,p;t.push("<header>\n    <!-- pagination -->\n    ");if(this.pages>1){t.push('\n        <ul class="pages">\n            <li>Page:</li>\n        ');for(n=s=0,c=this.pages;0<=c?s<c:s>c;n=0<=c?++s:--s)t.push("\n            <li><a "),this.current===n&&t.push('class="current"'),t.push(">"),t.push(r(n+1)),t.push("</a></li>\n        ");t.push("\n        </ul>\n    ")}t.push("\n    \n    <h4>"),t.push(r(this.count)),t.push(' Publications for</h4> <input type="text" placeholder="zen" class="symbol three columns" value="'),t.push(r(this.symbol)),t.push('" />\n</header>\n\n');if(this.rows.length!==0){t.push("\n    <table>\n        <thead>\n            <tr>\n                <th>Title</th>\n                <th>Author</th>\n            </tr>\n        </thead>\n        <tbody>\n            "),h=this.rows;for(o=0,f=h.length;o<f;o++){i=h[o],t.push("\n                <tr>\n                    <td>"),t.push(r(i.title)),t.push("</td>\n                    <td>\n                        ");if(i.authors.length>5){t.push("\n                            ");for(n=u=0;u<5;n=++u)t.push('\n                                <span class="author">'),t.push(r(i.authors[n].name)),t.push("</span>\n                            ");t.push("\n                            &hellip;\n                        ")}else{t.push("\n                            "),p=i.authors;for(a=0,l=p.length;a<l;a++)e=p[a],t.push('\n                                <span class="author">'),t.push(r(e.name)),t.push("</span>\n                            ");t.push("\n                        ")}t.push("\n                    </td>\n                </tr>\n            ")}t.push("\n        </tbody>\n    </table>\n")}else t.push('\n    <div class="alert-box alert">No results</div>\n')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")};
  
  /**#@+ css */
  var style = document.createElement('style');
  style.type = 'text/css';
  style.innerHTML = 'div#w#@+CALLBACK h4{float:left}div#w#@+CALLBACK ul.pages{display:inline;list-style-type:none;float:right;margin:15px 0;max-width;max-width:500px}div#w#@+CALLBACK ul.pages li{display:inline-block}div#w#@+CALLBACK ul.pages a.current{font-weight:700}div#w#@+CALLBACK header:after{content:" ";display:block;clear:both}div#w#@+CALLBACK table{width:100%}div#w#@+CALLBACK span.author:not(:last-child):after{content:",";display:inline-block}div#w#@+CALLBACK input.symbol{color:#8E0022;background:0;border:0;-webkit-box-shadow:none;-moz-box-shadow:none;box-shadow:none;font-family:\'Droid Serif\',serif;font-size:23px;font-weight:700;padding:0;margin:10px 0;margin-left:4px}';
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