(function() {
var root;
root = this;

var JST = {};
JST["error.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<div class="alert alert-block">\n    <h4 class="alert-heading">'),b.push(d(this.title)),b.push("</h4>\n    <p>"),b.push(this.text),b.push("</p>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.matches.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f;b.push('<div class="popover" style="position:absolute;top:22px;right:0;z-index:1;display:block">\n    <div class="popover-inner" style="width:300px;margin-left:-300px">\n        <a style="cursor:pointer;margin:2px 5px 0 0" class="close">×</a>\n        <h3 class="popover-title"></h3>\n        <div class="popover-content">\n            '),f=this.matches;for(c=0,e=f.length;c<e;c++)a=f[c],b.push('\n                <a href="#">'),b.push(d(a.displayed)),b.push("</a>\n            ");b.push("\n        </div>\n    </div>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.row.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<tr>\n    <td class="check"><input type="checkbox" /></td>\n    <td class="description">'),b.push(d(this.row.description)),b.push('</td>\n    <td class="pValue">'),b.push(d(this.row["p-value"].toFixed(7))),b.push('</td>\n    <td class="matches" style="position:relative">\n        <span class="count label label-success" style="cursor:pointer">'),b.push(d(this.row.matches.length)),b.push("</span>\n    </td>\n</tr>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["noresults.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<div class="alert alert-info">\n    <p>The Widget has no results.</p>\n</div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.table.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<!-- actual fixed head -->\n<div class="head">\n    <div style="font-weight:bold;display:block;float:left;padding:0 8px"><input type="checkbox" class="check" /></div>\n    <div style="font-weight:bold;display:block;float:left;padding:0 8px">'),b.push(d(this.label)),b.push('</div>\n    <div style="font-weight:bold;display:block;float:left;padding:0 8px">p-Value</div>\n    <div style="font-weight:bold;display:block;float:left;padding:0 8px">Matches</div>\n</div>\n<div style="clear:both"></div>\n<div class="wrapper" style="overflow:auto;overflow-x:hidden">\n    <table class="table table-striped" style="margin-top:-34px">\n        <!-- head for proper cell width -->\n        <thead style="visibility:hidden">\n            <tr>\n                <th></th>\n                <th>'),b.push(d(this.label)),b.push("</th>\n                <th>p-Value</th>\n                <th>Matches</th>\n            </tr>\n        </thead>\n        <tbody></tbody>\n    </table>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.form.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f,g,h,i,j;b.push('<form>\n    <div class="group" style="display:inline-block;margin-right:5px">\n        <label>Test Correction</label>\n        <select name="errorCorrection" class="span2">\n            '),i=this.errorCorrections;for(e=0,g=i.length;e<g;e++)a=i[e],b.push('\n                <option value="'),b.push(d(a)),b.push('" '),this.options.errorCorrection===a&&b.push(d('selected="selected"')),b.push(">\n                    "),b.push(d(a)),b.push("\n            </option>\n            ");b.push('\n        </select>\n    </div>\n\n    <div class="group" style="display:inline-block;margin-right:5px">\n        <label>Max p-value</label>\n        <select name="pValue" class="span2">\n            '),j=this.pValues;for(f=0,h=j.length;f<h;f++)c=j[f],b.push('\n                <option value="'),b.push(d(c)),b.push('" '),this.options.pValue===c&&b.push(d('selected="selected"')),b.push(">\n                    "),b.push(d(c)),b.push("\n                </option>\n            ");b.push("\n        </select>\n    </div>\n</form>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.normal.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push("<header>\n    <h3>"),this.title&&b.push(d(this.title)),b.push("</h3>\n    <p>"),this.description&&b.push(this.description),b.push("</p>\n    "),this.notAnalysed&&(b.push('\n        <p>Number of Genes in this list not analysed in this widget: <span class="label label-info">'),b.push(d(this.notAnalysed)),b.push("</span></p>\n    ")),b.push('\n\n    <div class="form"></div>\n\n    <div class="actions" style="margin-bottom:10px">\n        <a class="view btn btn-mini disabled">View</a>\n        <a class="export btn btn-mini disabled">Export</a>\n    </div>\n</header>\n<div class="content"></div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["chart.normal.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push("<header>\n    <h3>"),this.title&&b.push(d(this.title)),b.push("</h3>\n    <p>"),this.description&&b.push(this.description),b.push("</p>\n    "),this.notAnalysed&&(b.push('\n        <p>Number of Genes in this list not analysed in this widget: <span class="label label-info">'),b.push(d(this.notAnalysed)),b.push("</span></p>\n    ")),b.push('\n</header>\n<div class="content"></div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.extra.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f;b.push('<div class="group" style="display:inline-block;margin-right:5px">\n    <label>'),b.push(d(this.label)),b.push('</label>\n    <select name="dataSet" class="span2">\n        '),f=this.possible;for(c=0,e=f.length;c<e;c++)a=f[c],b.push('\n            <option value="'),b.push(d(a)),b.push('" '),this.selected===a&&b.push(d('selected="selected"')),b.push(">\n                "),b.push(d(a)),b.push("\n            </option>\n        ");b.push("\n    </select>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["invalidjsonkey.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<li style="vertical-align:bottom">\n    <span style="display:inline-block" class="label label-inverse">'),b.push(d(this.key)),b.push("</span> is "),b.push(d(this.actual)),b.push("; was expecting "),b.push(d(this.expected)),b.push("\n</li>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
var type,
  __hasProp = Object.prototype.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

type = {};

type.Root = (function() {

  function Root() {}

  Root.prototype.result = false;

  Root.prototype.is = function() {
    return this.result;
  };

  Root.prototype.toString = function() {
    return this.expected;
  };

  return Root;

})();

type.isString = (function(_super) {

  __extends(isString, _super);

  isString.prototype.expected = "String";

  function isString(key) {
    this.result = typeof key === 'string';
  }

  return isString;

})(type.Root);

type.isInteger = (function(_super) {

  __extends(isInteger, _super);

  isInteger.prototype.expected = "Integer";

  function isInteger(key) {
    this.result = typeof key === 'number';
  }

  return isInteger;

})(type.Root);

type.isBoolean = (function(_super) {

  __extends(isBoolean, _super);

  isBoolean.prototype.expected = "Boolean true";

  function isBoolean(key) {
    this.result = typeof key === 'boolean';
  }

  return isBoolean;

})(type.Root);

type.isNull = (function(_super) {

  __extends(isNull, _super);

  isNull.prototype.expected = "Null";

  function isNull(key) {
    this.result = key === null;
  }

  return isNull;

})(type.Root);

type.isArray = (function(_super) {

  __extends(isArray, _super);

  isArray.prototype.expected = "Array";

  function isArray(key) {
    this.result = key instanceof Array;
  }

  return isArray;

})(type.Root);

type.isHTTPSuccess = (function(_super) {

  __extends(isHTTPSuccess, _super);

  isHTTPSuccess.prototype.expected = "HTTP code 200";

  function isHTTPSuccess(key) {
    this.result = key === 200;
  }

  return isHTTPSuccess;

})(type.Root);

type.isUndefined = (function(_super) {

  __extends(isUndefined, _super);

  function isUndefined() {
    isUndefined.__super__.constructor.apply(this, arguments);
  }

  isUndefined.prototype.expected = "it to be undefined";

  return isUndefined;

})(type.Root);

var Exporter,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };

Exporter = (function() {

  Exporter.prototype.mime = 'text/plain';

  Exporter.prototype.charset = 'UTF-8';

  Exporter.prototype.url = window.webkitURL || window.URL;

  function Exporter(a, data, filename) {
    var builder;
    if (filename == null) filename = 'widget.tsv';
    this.destroy = __bind(this.destroy, this);
    builder = new (window.WebKitBlobBuilder || window.MozBlobBuilder || window.BlobBuilder)();
    builder.append(data(s));
    a.attr('download', filename);
    (this.href = this.url.createObjectURL(builder.getBlob("" + this.mime + ";charset=" + this.charset))) && (a.attr('href', this.href));
    a.attr('data-downloadurl', [this.mime, filename, this.href].join(':'));
  }

  Exporter.prototype.destroy = function() {
    return this.url.revokeObjectURL(this.href);
  };

  return Exporter;

})();

var CSSLoader, JSLoader, Loader,
  __hasProp = Object.prototype.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

Loader = (function() {

  function Loader() {}

  Loader.prototype.getHead = function() {
    return document.getElementsByTagName('head')[0];
  };

  Loader.prototype.setCallback = function(tag, callback) {
    tag.onload = callback;
    return tag.onreadystatechange = function() {
      var state;
      state = tag.readyState;
      if (state === "complete" || state === "loaded") {
        tag.onreadystatechange = null;
        return window.setTimeout(callback, 0);
      }
    };
  };

  return Loader;

})();

JSLoader = (function(_super) {

  __extends(JSLoader, _super);

  function JSLoader(path, callback) {
    var script;
    script = document.createElement("script");
    script.src = path;
    script.type = "text/javascript";
    if (callback) this.setCallback(script, callback);
    this.getHead().appendChild(script);
  }

  return JSLoader;

})(Loader);

CSSLoader = (function(_super) {

  __extends(CSSLoader, _super);

  function CSSLoader(path, callback) {
    var sheet;
    sheet = document.createElement("link");
    sheet.rel = "stylesheet";
    sheet.type = "text/css";
    sheet.href = path;
    if (callback) this.setCallback(sheet, callback);
    this.getHead().appendChild(sheet);
  }

  return CSSLoader;

})(Loader);

var factory;
factory = function(Backbone) {

  var InterMineWidget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  InterMineWidget = (function(_super) {
  
    __extends(InterMineWidget, _super);
  
    function InterMineWidget() {
      this.error = __bind(this.error, this);
      this.validateType = __bind(this.validateType, this);    $(this.el).html($('<div/>', {
        "class": "inner",
        style: "height:572px;overflow:hidden",
        html: "Loading &hellip;"
      }));
      this.el = "" + this.el + " div.inner";
    }
  
    InterMineWidget.prototype.template = function(name, context) {
      var _name;
      if (context == null) context = {};
      return typeof JST[_name = "" + name + ".eco"] === "function" ? JST[_name](context) : void 0;
    };
  
    InterMineWidget.prototype.validateType = function(object, spec) {
      var fails, key, r, value;
      fails = [];
      for (key in object) {
        value = object[key];
        if ((r = (typeof spec[key] === "function" ? new spec[key](value) : void 0) || (r = new type.isUndefined())) && !r.is()) {
          fails.push(this.template("invalidjsonkey", {
            key: key,
            actual: r.is(),
            expected: new String(r)
          }));
        }
      }
      if (fails.length) return this.error("JSONObjectType", fails);
    };
  
    InterMineWidget.prototype.error = function(type, data) {
      var opts;
      opts = {
        title: "Error",
        text: "Generic error"
      };
      switch (type) {
        case "AJAXTransport":
          opts.title = data.statusText;
          opts.text = data.responseText;
          break;
        case "JSONObjectType":
          opts.title = "Invalid JSON";
          opts.text = "<ol>" + (data.join('')) + "</ol>";
      }
      return $(this.el).html(this.template("error", opts));
    };
  
    return InterMineWidget;
  
  })(Backbone.View);
  

  var ChartWidget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  ChartWidget = (function(_super) {
  
    __extends(ChartWidget, _super);
  
    ChartWidget.prototype.chartOptions = {
      fontName: "Sans-Serif",
      fontSize: 11,
      width: 400,
      height: 450,
      legend: "bottom",
      colors: ["#2F72FF", "#9FC0FF"],
      chartArea: {
        top: 30
      },
      hAxis: {
        titleTextStyle: {
          fontName: "Sans-Serif"
        }
      },
      vAxis: {
        titleTextStyle: {
          fontName: "Sans-Serif"
        }
      }
    };
  
    ChartWidget.prototype.spec = {
      response: {
        "chartType": type.isString,
        "description": type.isString,
        "error": type.isNull,
        "list": type.isString,
        "notAnalysed": type.isInteger,
        "pathQuery": type.isString,
        "requestedAt": type.isString,
        "results": type.isArray,
        "seriesLabels": type.isString,
        "seriesValues": type.isString,
        "statusCode": type.isHTTPSuccess,
        "title": type.isString,
        "type": type.isString,
        "wasSuccessful": type.isBoolean
      }
    };
  
    function ChartWidget(service, token, id, bagName, el, widgetOptions) {
      var _this = this;
      this.service = service;
      this.token = token;
      this.id = id;
      this.bagName = bagName;
      this.el = el;
      this.widgetOptions = widgetOptions != null ? widgetOptions : {
        "title": true,
        "description": true,
        selectCb: function(pq) {
          return window.open("" + _this.service + "query/results?query=" + (encodeURIComponent(pq)) + "&format=html");
        }
      };
      this.render = __bind(this.render, this);
      ChartWidget.__super__.constructor.call(this);
      this.render();
    }
  
    ChartWidget.prototype.render = function() {
      var _this = this;
      return $.ajax({
        url: "" + this.service + "list/chart",
        dataType: "json",
        data: {
          widget: this.id,
          list: this.bagName,
          token: this.token
        },
        success: function(response) {
          var chart;
          _this.validateType(response, _this.spec.response);
          $(_this.el).html(_this.template("chart.normal", {
            "title": _this.widgetOptions.title ? response.title : "",
            "description": _this.widgetOptions.description ? response.description : "",
            "notAnalysed": response.notAnalysed
          }));
          if (response.results.length > 1) {
            if (response.chartType in google.visualization) {
              chart = new google.visualization[response.chartType]($(_this.el).find("div.content")[0]);
              chart.draw(google.visualization.arrayToDataTable(response.results, false), _this.chartOptions);
              if (response.pathQuery != null) {
                return google.visualization.events.addListener(chart, "select", function() {
                  var item, pq, _i, _len, _ref, _results;
                  pq = response.pathQuery;
                  _ref = chart.getSelection();
                  _results = [];
                  for (_i = 0, _len = _ref.length; _i < _len; _i++) {
                    item = _ref[_i];
                    if (item.row != null) {
                      pq = pq.replace("%category", response.results[item.row + 1][0]);
                      if (item.column != null) {
                        pq = pq.replace("%series", _this._translateSeries(response, response.results[0][item.column]));
                      }
                      _results.push(_this.widgetOptions.selectCb(pq));
                    } else {
                      _results.push(void 0);
                    }
                  }
                  return _results;
                });
              }
            } else {
              return $(_this.el).html(_this.template("error", {
                title: response.chartType,
                text: "This chart type does not exist in Google Visualization API"
              }));
            }
          } else {
            return $(_this.el).find("div.content").html($(_this.template("noresults")));
          }
        },
        error: function(err) {
          return _this.error("AJAXTransport", err);
        }
      });
    };
  
    ChartWidget.prototype._translateSeries = function(response, series) {
      return response.seriesValues.split(',')[response.seriesLabels.split(',').indexOf(series)];
    };
  
    return ChartWidget;
  
  })(InterMineWidget);
  

  var EnrichmentWidget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  EnrichmentWidget = (function(_super) {
  
    __extends(EnrichmentWidget, _super);
  
    EnrichmentWidget.prototype.formOptions = {
      errorCorrection: "Holm-Bonferroni",
      pValue: 0.05
    };
  
    EnrichmentWidget.prototype.errorCorrections = ["Holm-Bonferroni", "Benjamini Hochberg", "Bonferroni", "None"];
  
    EnrichmentWidget.prototype.pValues = [0.05, 0.10, 1.00];
  
    EnrichmentWidget.prototype.spec = {
      response: {
        "title": type.isString,
        "description": type.isString,
        "error": type.isNull,
        "list": type.isString,
        "notAnalysed": type.isInteger,
        "requestedAt": type.isString,
        "results": type.isArray,
        "label": type.isString,
        "statusCode": type.isHTTPSuccess,
        "title": type.isString,
        "type": type.isString,
        "wasSuccessful": type.isBoolean
      },
      resultRow: {
        "description": type.isString,
        "item": type.isString,
        "matches": type.isArray,
        "p-value": type.isInteger
      }
    };
  
    function EnrichmentWidget(service, token, id, bagName, el, widgetOptions) {
      var _this = this;
      this.service = service;
      this.token = token;
      this.id = id;
      this.bagName = bagName;
      this.el = el;
      this.widgetOptions = widgetOptions != null ? widgetOptions : {
        "title": true,
        "description": true,
        matchCb: function(id) {
          return typeof console !== "undefined" && console !== null ? console.log(id) : void 0;
        }
      };
      this.exportClick = __bind(this.exportClick, this);
      this.matchesClick = __bind(this.matchesClick, this);
      this.selectAllClick = __bind(this.selectAllClick, this);
      this.checkboxClick = __bind(this.checkboxClick, this);
      this.formClick = __bind(this.formClick, this);
      this.render = __bind(this.render, this);
      EnrichmentWidget.__super__.constructor.call(this);
      this.render();
    }
  
    EnrichmentWidget.prototype.render = function() {
      var _this = this;
      $.ajax({
        url: "" + this.service + "list/enrichment",
        dataType: "json",
        data: {
          widget: this.id,
          list: this.bagName,
          correction: this.formOptions.errorCorrection,
          maxp: this.formOptions.pValue,
          token: this.token
        },
        success: function(response) {
          var height, i, table, _fn, _ref;
          _this.validateType(response, _this.spec.response);
          if (response.wasSuccessful) {
            $(_this.el).html(_this.template("enrichment.normal", {
              "title": _this.widgetOptions.title ? response.title : "",
              "description": _this.widgetOptions.description ? response.description : "",
              "notAnalysed": response.notAnalysed
            }));
            $(_this.el).find("div.actions a.export").click(function(e) {
              return _this.exportClick(e);
            });
            $(_this.el).find("div.form").html(_this.template("enrichment.form", {
              "options": _this.formOptions,
              "errorCorrections": _this.errorCorrections,
              "pValues": _this.pValues
            }));
            if (response.extraAttributeLabel != null) {
              $(_this.el).find('div.form form').append(_this.template("enrichment.extra", {
                "label": response.extraAttributeLabel,
                "possible": response.extraAttributePossibleValues,
                "selected": response.extraAttributeSelectedValue
              }));
            }
            if (response.results.length > 0) {
              height = $(_this.el).height() - $(_this.el).find('header').height() - 30 - 18;
              $(_this.el).find("div.content").html($(_this.template("enrichment.table", {
                "label": response.label
              }))).find('div.wrapper').css('height', "" + height + "px");
              $(_this.el).find('div.content div.head input.check').click(function(e) {
                return _this.selectAllClick(e);
              });
              table = $(_this.el).find("div.content table");
              _fn = function(i) {
                var row, td, tr;
                row = response.results[i];
                _this.validateType(row, _this.spec.resultRow);
                table.append(tr = $(_this.template("enrichment.row", {
                  "row": row
                })));
                td = tr.find("td.matches .count").click(function() {
                  return _this.matchesClick(td, row["matches"], _this.widgetOptions.matchCb);
                });
                return tr.find("td.check input").click(function() {
                  return _this.checkboxClick(i, row);
                });
              };
              for (i = 0, _ref = response.results.length; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
                _fn(i);
              }
              table.find('thead th').each(function(i, th) {
                return $(_this.el).find("div.content div.head div:eq(" + i + ")").width($(th).width());
              });
            } else {
              $(_this.el).find("div.content").html($(_this.template("noresults")));
            }
            return $(_this.el).find("form select").change(_this.formClick);
          }
        },
        error: function(err) {
          return _this.error("AJAXTransport", err);
        }
      });
      return this;
    };
  
    EnrichmentWidget.prototype.formClick = function(e) {
      this.formOptions[$(e.target).attr("name")] = $(e.target[e.target.selectedIndex]).attr("value");
      return this.render();
    };
  
    EnrichmentWidget.prototype.checkboxClick = function(key, row) {
      var value, _ref;
      if (!(this.selected != null)) this.selected = {};
      if (this.selected[key] != null) {
        delete this.selected[key];
      } else {
        this.selected[key] = row;
      }
      _ref = this.selected;
      for (key in _ref) {
        value = _ref[key];
        $(this.el).find('div.actions a.btn.disabled').removeClass('disabled');
        return;
      }
      return $(this.el).find('div.actions a.btn').addClass('disabled');
    };
  
    EnrichmentWidget.prototype.selectAllClick = function(e) {
      var _this = this;
      if (!(this.selected != null)) this.selected = {};
      if ($(e.target).is(':checked')) {
        $(this.el).find('div.content table tbody tr').each(function(i, row) {
          $(row).find('td.check input:not(:checked)').attr('checked', true);
          return _this.selected[i] = row;
        });
        return $(this.el).find('div.actions a.btn').removeClass('disabled');
      } else {
        this.selected = {};
        $(this.el).find('div.content table tbody tr td.check input:checked').each(function(i, input) {
          return $(input).attr('checked', false);
        });
        return $(this.el).find('div.actions a.btn').addClass('disabled');
      }
    };
  
    EnrichmentWidget.prototype.matchesClick = function(target, matches, matchCb) {
      var modal;
      target.after(modal = $(this.template("enrichment.matches", {
        "matches": matches
      })));
      modal.find("a.close").click(function() {
        return modal.remove();
      });
      return modal.find("div.popover-content a").click(function(e) {
        matchCb($(this).text());
        return e.preventDefault();
      });
    };
  
    EnrichmentWidget.prototype.viewAction = function() {
      return console.log('viewAction triggered thanks to Backbone');
    };
  
    EnrichmentWidget.prototype.exportClick = function(e) {
      var ex, key, match, result, value, _ref;
      result = [];
      _ref = this.selected;
      for (key in _ref) {
        value = _ref[key];
        result.push([value.item, value['p-value']].join("\t") + "\t" + [
          (function() {
            var _i, _len, _ref2, _results;
            _ref2 = value.matches;
            _results = [];
            for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
              match = _ref2[_i];
              _results.push(match.displayed);
            }
            return _results;
          })()
        ].join(','));
      }
      if (result.length) {
        ex = new Exporter($(e.target), result.join("\n"), "" + this.bagName + " " + this.id + ".tsv");
        return window.setTimeout((function() {
          return ex.destroy();
        }), 5000);
      }
    };
  
    return EnrichmentWidget;
  
  })(InterMineWidget);
  

  return {

    "InterMineWidget": InterMineWidget,
    "ChartWidget": ChartWidget,
    "EnrichmentWidget": EnrichmentWidget,

  };
};
var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = Object.prototype.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; },
  __slice = Array.prototype.slice,
  __indexOf = Array.prototype.indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

root.Widgets = (function() {

  Widgets.prototype.resources = {
    js: {
      jQuery: "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js",
      _: "http://documentcloud.github.com/underscore/underscore.js",
      google: "https://www.google.com/jsapi",
      Backbone: "http://documentcloud.github.com/backbone/backbone-min.js"
    }
  };

  function Widgets(service, token) {
    var library, path, _fn, _ref,
      _this = this;
    this.service = service;
    this.token = token != null ? token : "";
    this.all = __bind(this.all, this);
    this.enrichment = __bind(this.enrichment, this);
    this.chart = __bind(this.chart, this);
    _ref = this.resources.js;
    _fn = function(library, path) {
      var _ref2;
      if (!(window[library] != null)) {
        _this.wait = ((_ref2 = _this.wait) != null ? _ref2 : 0) + 1;
        return new JSLoader(path, function() {
          if (library === 'jQuery') root.$ = window.jQuery;
          if (library === 'Backbone') __extends(root, factory(window.Backbone));
          return _this.wait -= 1;
        });
      } else {
        if (library === 'jQuery') root.$ = window.jQuery;
        if (library === 'Backbone') {
          return __extends(root, factory(window.Backbone));
        }
      }
    };
    for (library in _ref) {
      path = _ref[library];
      _fn(library, path);
    }
  }

  Widgets.prototype.chart = function() {
    var opts,
      _this = this;
    opts = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
    if (this.wait) {
      return window.setTimeout((function() {
        return _this.chart.apply(_this, opts);
      }), 0);
    } else {
      return google.load("visualization", "1.0", {
        packages: ["corechart"],
        callback: function() {
          return (function(func, args, ctor) {
            ctor.prototype = func.prototype;
            var child = new ctor, result = func.apply(child, args);
            return typeof result === "object" ? result : child;
          })(ChartWidget, [_this.service, _this.token].concat(__slice.call(opts)), function() {});
        }
      });
    }
  };

  Widgets.prototype.enrichment = function() {
    var opts,
      _this = this;
    opts = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
    if (this.wait) {
      return window.setTimeout((function() {
        return _this.enrichment.apply(_this, opts);
      }), 0);
    } else {
      return (function(func, args, ctor) {
        ctor.prototype = func.prototype;
        var child = new ctor, result = func.apply(child, args);
        return typeof result === "object" ? result : child;
      })(EnrichmentWidget, [this.service, this.token].concat(__slice.call(opts)), function() {});
    }
  };

  Widgets.prototype.all = function(type, bagName, el, widgetOptions) {
    var _this = this;
    if (type == null) type = "Gene";
    if (this.wait) {
      return window.setTimeout((function() {
        return _this.all(type, bagName, el, widgetOptions);
      }), 0);
    } else {
      return $.ajax({
        url: "" + this.service + "widgets",
        dataType: "json",
        success: function(response) {
          var widget, widgetEl, _i, _len, _ref, _results;
          if (response.widgets) {
            _ref = response.widgets;
            _results = [];
            for (_i = 0, _len = _ref.length; _i < _len; _i++) {
              widget = _ref[_i];
              if (!(__indexOf.call(widget.targets, type) >= 0)) continue;
              widgetEl = widget.name.replace(/[^-a-zA-Z0-9,&\s]+/ig, '').replace(/-/gi, "_").replace(/\s/gi, "-").toLowerCase();
              $(el).append($('<div/>', {
                id: widgetEl,
                "class": "widget span6"
              }));
              switch (widget.widgetType) {
                case "chart":
                  _results.push(_this.chart(widget.name, bagName, "" + el + " #" + widgetEl, widgetOptions));
                  break;
                case "enrichment":
                  _results.push(_this.enrichment(widget.name, bagName, "" + el + " #" + widgetEl, widgetOptions));
                  break;
                default:
                  _results.push(void 0);
              }
            }
            return _results;
          }
        },
        error: function(err) {
          return $(el).html($('<div/>', {
            "class": "alert alert-error",
            text: "An unspecified error has happened, server timeout?"
          }));
        }
      });
    }
  };

  return Widgets;

})();

}).call(this);