(function() {
var o = {};

var JST = {};
JST["error.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<div class="alert alert-block">\n    <h4 class="alert-heading">'),b.push(d(this.title)),b.push("</h4>\n    <p>"),b.push(this.text),b.push("</p>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.matches.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f;b.push('<div class="popover" style="position:absolute;top:5px;right:0;z-index:1;display:block">\n    <div class="popover-inner" style="width:300px;margin-left:-300px">\n        <a style="cursor:pointer;margin:2px 5px 0 0" class="close">×</a>\n        <h3 class="popover-title"></h3>\n        <div class="popover-content">\n            '),f=this.matches;for(c=0,e=f.length;c<e;c++)a=f[c],b.push('\n                <a href="#" class="match">'),b.push(d(a.displayed)),b.push("</a>\n            ");b.push("\n        </div>\n    </div>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.actions.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<a class="btn btn-mini '),this.disabled&&b.push("disabled"),b.push(' view">View</a>\n<a class="btn btn-mini '),this.disabled&&b.push("disabled"),b.push(' export">Export</a>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.row.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<td class="check"><input type="checkbox" '),b.push(d(this.row.selected?b.push('checked="checked"'):void 0)),b.push(' /></td>\n<td class="description">'),b.push(d(this.row.description)),b.push('</td>\n<td class="pValue">'),b.push(d(this.row["p-value"].toFixed(7))),b.push('</td>\n<td class="matches">\n    <span class="count label label-success" style="cursor:pointer">'),b.push(d(this.row.matches.length)),b.push("</span>\n</td>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["noresults.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<div class="alert alert-info">\n    <p>The Widget has no results.</p>\n</div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.table.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<!-- actual fixed head -->\n<div class="head" style="display:table">\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;"><input type="checkbox" class="check" /></div>\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;">'),b.push(d(this.label)),b.push('</div>\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;">p-Value</div>\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;">Matches</div>\n    <div style="clear:both"></div>\n</div>\n<div class="wrapper" style="overflow:auto;overflow-x:hidden">\n    <table class="table table-striped">\n        <!-- head for proper cell width -->\n        <thead style="visibility:hidden">\n            <tr>\n                <th></th>\n                <th>'),b.push(d(this.label)),b.push("</th>\n                <th>p-Value</th>\n                <th>Matches</th>\n            </tr>\n        </thead>\n        <tbody>\n            <!-- loop enrichment.row.eco -->\n        </tbody>\n    </table>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.form.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f,g,h,i,j;b.push('<form style="margin:0">\n    <div class="group" style="display:inline-block;margin-right:5px">\n        <label>Test Correction</label>\n        <select name="errorCorrection" class="span2">\n            '),i=this.errorCorrections;for(e=0,g=i.length;e<g;e++)a=i[e],b.push('\n                <option value="'),b.push(d(a)),b.push('" '),this.options.errorCorrection===a&&b.push(d('selected="selected"')),b.push(">\n                    "),b.push(d(a)),b.push("\n            </option>\n            ");b.push('\n        </select>\n    </div>\n\n    <div class="group" style="display:inline-block;margin-right:5px">\n        <label>Max p-value</label>\n        <select name="pValue" class="span2">\n            '),j=this.pValues;for(f=0,h=j.length;f<h;f++)c=j[f],b.push('\n                <option value="'),b.push(d(c)),b.push('" '),this.options.pValue===c&&b.push(d('selected="selected"')),b.push(">\n                    "),b.push(d(c)),b.push("\n                </option>\n            ");b.push("\n        </select>\n    </div>\n</form>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.normal.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push("<header>\n    <h3>"),this.title&&b.push(d(this.title)),b.push("</h3>\n    <p>"),this.description&&b.push(this.description),b.push("</p>\n    "),this.notAnalysed&&(b.push('\n        <p>Number of Genes in this list not analysed in this widget: <span class="label label-info">'),b.push(d(this.notAnalysed)),b.push("</span></p>\n    ")),b.push('\n\n    <div class="form"></div>\n\n    <div class="actions" style="padding:10px 0">\n        <!-- enrichment.actions.eco -->\n    </div>\n</header>\n<div class="content">\n    <!-- enrichment.table.eco -->\n</div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["chart.normal.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push("<header>\n    <h3>"),this.title&&b.push(d(this.title)),b.push("</h3>\n    <p>"),this.description&&b.push(this.description),b.push("</p>\n    "),this.notAnalysed&&(b.push('\n        <p>Number of Genes in this list not analysed in this widget: <span class="label label-info">'),b.push(d(this.notAnalysed)),b.push("</span></p>\n    ")),b.push('\n</header>\n<div class="content"></div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.extra.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f;b.push('<div class="group" style="display:inline-block;margin-right:5px">\n    <label>'),b.push(d(this.label)),b.push('</label>\n    <select name="dataSet" class="span2">\n        '),f=this.possible;for(c=0,e=f.length;c<e;c++)a=f[c],b.push('\n            <option value="'),b.push(d(a)),b.push('" '),this.selected===a&&b.push(d('selected="selected"')),b.push(">\n                "),b.push(d(a)),b.push("\n            </option>\n        ");b.push("\n    </select>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["invalidjsonkey.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<li style="vertical-align:bottom">\n    <span style="display:inline-block" class="label label-inverse">'),b.push(d(this.key)),b.push("</span> is "),b.push(d(this.actual)),b.push("; was expecting "),b.push(d(this.expected)),b.push("\n</li>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
/* Types in JS.
*/
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
    return "" + this.expected + " but got " + this.actual;
  };

  return Root;

})();

type.isString = (function(_super) {

  __extends(isString, _super);

  isString.prototype.expected = "String";

  function isString(actual) {
    this.actual = actual;
    this.result = typeof actual === 'string';
  }

  return isString;

})(type.Root);

type.isInteger = (function(_super) {

  __extends(isInteger, _super);

  isInteger.prototype.expected = "Integer";

  function isInteger(actual) {
    this.actual = actual;
    this.result = typeof actual === 'number';
  }

  return isInteger;

})(type.Root);

type.isBoolean = (function(_super) {

  __extends(isBoolean, _super);

  isBoolean.prototype.expected = "Boolean true";

  function isBoolean(actual) {
    this.actual = actual;
    this.result = typeof actual === 'boolean';
  }

  return isBoolean;

})(type.Root);

type.isNull = (function(_super) {

  __extends(isNull, _super);

  isNull.prototype.expected = "Null";

  function isNull(actual) {
    this.actual = actual;
    this.result = actual === null;
  }

  return isNull;

})(type.Root);

type.isArray = (function(_super) {

  __extends(isArray, _super);

  isArray.prototype.expected = "Array";

  function isArray(actual) {
    this.actual = actual;
    this.result = actual instanceof Array;
  }

  return isArray;

})(type.Root);

type.isHTTPSuccess = (function(_super) {

  __extends(isHTTPSuccess, _super);

  isHTTPSuccess.prototype.expected = "HTTP code 200";

  function isHTTPSuccess(actual) {
    this.actual = actual;
    this.result = actual === 200;
  }

  return isHTTPSuccess;

})(type.Root);

type.isJSON = (function(_super) {

  __extends(isJSON, _super);

  isJSON.prototype.expected = "JSON Object";

  function isJSON(actual) {
    this.actual = actual;
    this.result = true;
    try {
      if (typeof JSON !== "undefined" && JSON !== null) JSON.parse(actual);
    } catch (e) {
      this.result = false;
    }
  }

  return isJSON;

})(type.Root);

type.isUndefined = (function(_super) {

  __extends(isUndefined, _super);

  function isUndefined() {
    isUndefined.__super__.constructor.apply(this, arguments);
  }

  isUndefined.prototype.expected = "it to be undefined";

  return isUndefined;

})(type.Root);

/* Merge properties of 2 dictionaries.
*/
var merge;

merge = function(child, parent) {
  var key;
  for (key in parent) {
    if (!(child[key] != null)) {
      if (Object.prototype.hasOwnProperty.call(parent, key)) {
        child[key] = parent[key];
      }
    }
  }
  return child;
};

/* Pure JS based JS script, CSS loader.
*/
var CSSLoader, JSLoader, Load, Loader,
  __hasProp = Object.prototype.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; },
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };

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

Load = (function() {

  Load.prototype.wait = false;

  function Load(resources, callback) {
    this.callback = callback;
    this.done = __bind(this.done, this);
    this.load = __bind(this.load, this);
    this.count = resources.length;
    this.load(resources.reverse());
  }

  Load.prototype.load = function(resources) {
    var resource,
      _this = this;
    if (this.wait) {
      return window.setTimeout((function() {
        return _this.load(resources);
      }), 0);
    } else {
      if (resources.length) {
        resource = resources.pop();
        if (resource.wait != null) this.wait = true;
        switch (resource.type) {
          case "js":
            if (resource.name != null) {
              if ((window[resource.name] != null) && (typeof window[resource.name] === "function" || "object")) {
                this.done(resource);
              } else {
                new JSLoader(resource.path, function() {
                  return _this.done(resource);
                });
              }
            } else {
              new JSLoader(resource.path, function() {
                return _this.done(resource);
              });
            }
        }
      }
      if (this.count || this.wait) {
        return window.setTimeout((function() {
          return _this.load(resources);
        }), 0);
      } else {
        return this.callback();
      }
    }
  };

  Load.prototype.done = function(resource) {
    if (resource.wait != null) this.wait = false;
    return this.count -= 1;
  };

  return Load;

})();

/* Create file download with custom content.
*/
var Exporter, PlainExporter,
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
    builder.append(data);
    a.attr('download', filename);
    (this.href = this.url.createObjectURL(builder.getBlob("" + this.mime + ";charset=" + this.charset))) && (a.attr('href', this.href));
    a.attr('data-downloadurl', [this.mime, filename, this.href].join(':'));
  }

  Exporter.prototype.destroy = function() {
    return this.url.revokeObjectURL(this.href);
  };

  return Exporter;

})();

PlainExporter = (function() {

  function PlainExporter(data) {
    var w;
    w = window.open();
    w.document.open();
    w.document.write(data);
    w.document.close();
  }

  PlainExporter.prototype.destroy = function() {};

  return PlainExporter;

})();

var factory;
factory = function(Backbone) {

  /* Parent for both Widgets, handling templating, validation and errors.
  */
  var InterMineWidget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  
  InterMineWidget = (function() {
  
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
      if (fails.length) return this.error(fails, "JSONResponse");
    };
  
    InterMineWidget.prototype.error = function(opts, type) {
      if (opts == null) {
        opts = {
          'title': 'Error',
          'text': 'Generic error'
        };
      }
      switch (type) {
        case "AJAXTransport":
          opts.title = opts.statusText;
          opts.text = opts.responseText;
          break;
        case "JSONResponse":
          opts.title = "Invalid JSON Response";
          opts.text = "<ol>" + (opts.join('')) + "</ol>";
      }
      $(this.el).html(this.template("error", opts));
      throw new Error(type);
    };
  
    return InterMineWidget;
  
  })();
  

  /* Enrichment Widget table row.
  */
  var EnrichmentRowView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  EnrichmentRowView = (function(_super) {
  
    __extends(EnrichmentRowView, _super);
  
    function EnrichmentRowView() {
      this.matchesAction = __bind(this.matchesAction, this);
      this.selectAction = __bind(this.selectAction, this);
      this.render = __bind(this.render, this);
      EnrichmentRowView.__super__.constructor.apply(this, arguments);
    }
  
    EnrichmentRowView.prototype.tagName = "tr";
  
    EnrichmentRowView.prototype.events = {
      "click td.check input": "selectAction",
      "click td.matches span": "matchesAction"
    };
  
    EnrichmentRowView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      this.model.bind('change', this.render);
      return this.render();
    };
  
    EnrichmentRowView.prototype.render = function() {
      $(this.el).html(this.template("enrichment.row", {
        "row": this.model.toJSON()
      }));
      return this;
    };
  
    EnrichmentRowView.prototype.selectAction = function() {
      return this.model.toggleSelected();
    };
  
    EnrichmentRowView.prototype.matchesAction = function() {
      if (this.matches != null) {
        return $(this.matches).show();
      } else {
        this.matches = new EnrichmentMatchesView({
          "matches": this.model.get("matches"),
          "template": this.template,
          "type": this.type,
          "callback": this.matchCb
        }).el;
        return $(this.el).find('td.matches span').after(this.matches);
      }
    };
  
    return EnrichmentRowView;
  
  })(Backbone.View);
  

  /* Enrichment Widget table row matches box.
  */
  var EnrichmentMatchesView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  EnrichmentMatchesView = (function(_super) {
  
    __extends(EnrichmentMatchesView, _super);
  
    function EnrichmentMatchesView() {
      this.hide = __bind(this.hide, this);
      this.matchAction = __bind(this.matchAction, this);
      this.render = __bind(this.render, this);
      EnrichmentMatchesView.__super__.constructor.apply(this, arguments);
    }
  
    EnrichmentMatchesView.prototype.events = {
      "click a.match": "matchAction",
      "click a.close": "hide"
    };
  
    EnrichmentMatchesView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      this.collection = new EnrichmentMatches(this.matches);
      return this.render();
    };
  
    EnrichmentMatchesView.prototype.render = function() {
      $(this.el).css({
        'position': 'relative'
      });
      $(this.el).html(this.template("enrichment.matches", {
        "matches": this.collection.toJSON()
      }));
      return this;
    };
  
    EnrichmentMatchesView.prototype.matchAction = function(e) {
      this.callback($(e.target).text(), this.type);
      return e.preventDefault();
    };
  
    EnrichmentMatchesView.prototype.hide = function() {
      return $(this.el).hide();
    };
  
    return EnrichmentMatchesView;
  
  })(Backbone.View);
  

  /* View maintaining Chart Widget.
  */
  var ChartView,
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  ChartView = (function(_super) {
  
    __extends(ChartView, _super);
  
    function ChartView() {
      ChartView.__super__.constructor.apply(this, arguments);
    }
  
    ChartView.prototype.chartOptions = {
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
  
    ChartView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      return this.render();
    };
  
    ChartView.prototype.render = function() {
      var chart,
        _this = this;
      $(this.el).html(this.template("chart.normal", {
        "title": this.options.title ? this.response.title : "",
        "description": this.options.description ? this.response.description : "",
        "notAnalysed": this.response.notAnalysed
      }));
      if (this.response.results.length > 1) {
        if (this.response.chartType in google.visualization) {
          chart = new google.visualization[this.response.chartType]($(this.el).find("div.content")[0]);
          chart.draw(google.visualization.arrayToDataTable(this.response.results, false), this.chartOptions);
          if (this.response.pathQuery != null) {
            return google.visualization.events.addListener(chart, "select", function() {
              var item, pq, translate, _i, _len, _ref, _results;
              translate = function(response, series) {
                return response.seriesValues.split(',')[response.seriesLabels.split(',').indexOf(series)];
              };
              pq = _this.response.pathQuery;
              _ref = chart.getSelection();
              _results = [];
              for (_i = 0, _len = _ref.length; _i < _len; _i++) {
                item = _ref[_i];
                if (item.row != null) {
                  pq = pq.replace("%category", _this.response.results[item.row + 1][0]);
                  if (item.column != null) {
                    pq = pq.replace("%series", translate(_this.response, _this.response.results[0][item.column]));
                  }
                  pq = typeof JSON !== "undefined" && JSON !== null ? JSON.parse(pq) : void 0;
                  _results.push(_this.options.selectCb(pq));
                } else {
                  _results.push(void 0);
                }
              }
              return _results;
            });
          }
        } else {
          return this.error({
            'title': this.response.chartType,
            'text': "This chart type does not exist in Google Visualization API"
          });
        }
      } else {
        return $(this.el).find("div.content").html($(this.template("noresults")));
      }
    };
  
    return ChartView;
  
  })(Backbone.View);
  

  /* Chart Widget main class.
  */
  var ChartWidget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  ChartWidget = (function(_super) {
  
    __extends(ChartWidget, _super);
  
    ChartWidget.prototype.widgetOptions = {
      "title": true,
      "description": true,
      selectCb: function(pq) {
        return typeof console !== "undefined" && console !== null ? console.log(pq) : void 0;
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
      this.service = service;
      this.token = token;
      this.id = id;
      this.bagName = bagName;
      this.el = el;
      if (widgetOptions == null) widgetOptions = {};
      this.render = __bind(this.render, this);
      this.widgetOptions = merge(widgetOptions, this.widgetOptions);
      ChartWidget.__super__.constructor.call(this);
      this.render();
    }
  
    ChartWidget.prototype.render = function() {
      var _this = this;
      return $.ajax({
        url: "" + this.service + "list/chart",
        dataType: "jsonp",
        data: {
          widget: this.id,
          list: this.bagName,
          token: this.token
        },
        success: function(response) {
          _this.validateType(response, _this.spec.response);
          if (response.wasSuccessful) {
            return new ChartView({
              "widget": _this,
              "el": _this.el,
              "template": _this.template,
              "response": response,
              "options": _this.widgetOptions
            });
          }
        },
        error: function(err) {
          return _this.error(err, "AJAXTransport");
        }
      });
    };
  
    return ChartWidget;
  
  })(InterMineWidget);
  

  /* Models underpinning Enrichment Widget results.
  */
  var EnrichmentMatch, EnrichmentMatches, EnrichmentResults, EnrichmentRow,
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; },
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  
  EnrichmentMatch = (function(_super) {
  
    __extends(EnrichmentMatch, _super);
  
    function EnrichmentMatch() {
      EnrichmentMatch.__super__.constructor.apply(this, arguments);
    }
  
    return EnrichmentMatch;
  
  })(Backbone.Model);
  
  EnrichmentMatches = (function(_super) {
  
    __extends(EnrichmentMatches, _super);
  
    function EnrichmentMatches() {
      EnrichmentMatches.__super__.constructor.apply(this, arguments);
    }
  
    EnrichmentMatches.prototype.model = EnrichmentMatch;
  
    return EnrichmentMatches;
  
  })(Backbone.Collection);
  
  EnrichmentRow = (function(_super) {
  
    __extends(EnrichmentRow, _super);
  
    function EnrichmentRow() {
      this.toggleSelected = __bind(this.toggleSelected, this);
      this.validate = __bind(this.validate, this);
      EnrichmentRow.__super__.constructor.apply(this, arguments);
    }
  
    EnrichmentRow.prototype.defaults = {
      "selected": false
    };
  
    EnrichmentRow.prototype.spec = {
      "description": type.isString,
      "identifier": type.isString,
      "matches": type.isArray,
      "p-value": type.isInteger,
      "selected": type.isBoolean
    };
  
    EnrichmentRow.prototype.initialize = function(row, widget) {
      this.widget = widget;
      return this.validate(row);
    };
  
    EnrichmentRow.prototype.validate = function(row) {
      return this.widget.validateType(row, this.spec);
    };
  
    EnrichmentRow.prototype.toggleSelected = function() {
      return this.set({
        selected: !this.get("selected")
      });
    };
  
    return EnrichmentRow;
  
  })(Backbone.Model);
  
  EnrichmentResults = (function(_super) {
  
    __extends(EnrichmentResults, _super);
  
    function EnrichmentResults() {
      EnrichmentResults.__super__.constructor.apply(this, arguments);
    }
  
    EnrichmentResults.prototype.model = EnrichmentRow;
  
    EnrichmentResults.prototype.selected = function() {
      return this.filter(function(row) {
        return row.get("selected");
      });
    };
  
    EnrichmentResults.prototype.toggleSelected = function() {
      var model, _i, _j, _len, _len2, _ref, _ref2, _results, _results2;
      if (this.models.length - this.selected().length) {
        _ref = this.models;
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          model = _ref[_i];
          _results.push(model.set({
            "selected": true
          }, {
            'silent': true
          }));
        }
        return _results;
      } else {
        _ref2 = this.models;
        _results2 = [];
        for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
          model = _ref2[_j];
          _results2.push(model.set({
            "selected": false
          }, {
            'silent': true
          }));
        }
        return _results2;
      }
    };
  
    return EnrichmentResults;
  
  })(Backbone.Collection);
  

  /* View maintaining Enrichment Widget.
  */
  var EnrichmentView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  EnrichmentView = (function(_super) {
  
    __extends(EnrichmentView, _super);
  
    function EnrichmentView() {
      this.viewAction = __bind(this.viewAction, this);
      this.exportAction = __bind(this.exportAction, this);
      this.selectAllAction = __bind(this.selectAllAction, this);
      this.formAction = __bind(this.formAction, this);
      this.renderTableBody = __bind(this.renderTableBody, this);
      this.renderTable = __bind(this.renderTable, this);
      this.renderToolbar = __bind(this.renderToolbar, this);
      EnrichmentView.__super__.constructor.apply(this, arguments);
    }
  
    EnrichmentView.prototype.events = {
      "click div.actions a.view": "viewAction",
      "click div.actions a.export": "exportAction",
      "change div.form select": "formAction",
      "click div.content input.check": "selectAllAction"
    };
  
    EnrichmentView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      this.collection = new EnrichmentResults;
      this.collection.bind('change', this.renderToolbar);
      return this.render();
    };
  
    EnrichmentView.prototype.render = function() {
      $(this.el).html(this.template("enrichment.normal", {
        "title": this.options.title ? this.response.title : "",
        "description": this.options.description ? this.response.description : "",
        "notAnalysed": this.response.notAnalysed
      }));
      $(this.el).find("div.form").html(this.template("enrichment.form", {
        "options": this.form.options,
        "pValues": this.form.pValues,
        "errorCorrections": this.form.errorCorrections
      }));
      if (this.response.extraAttributeLabel != null) {
        $(this.el).find('div.form form').append(this.template("enrichment.extra", {
          "label": this.response.extraAttributeLabel,
          "possible": this.response.extraAttributePossibleValues,
          "selected": this.response.extraAttributeSelectedValue
        }));
      }
      if (this.response.results.length > 0) {
        this.renderToolbar();
        this.renderTable();
      } else {
        $(this.el).find("div.content").html($(this.template("noresults")));
      }
      return this;
    };
  
    EnrichmentView.prototype.renderToolbar = function() {
      return $(this.el).find("div.actions").html($(this.template("enrichment.actions", {
        "disabled": this.collection.selected().length === 0
      })));
    };
  
    EnrichmentView.prototype.renderTable = function() {
      var height, i, table, _fn, _ref,
        _this = this;
      $(this.el).find("div.content").html($(this.template("enrichment.table", {
        "label": this.response.label
      })));
      table = $(this.el).find("div.content table");
      _fn = function(i) {
        var row;
        row = new EnrichmentRow(_this.response.results[i], _this.widget);
        return _this.collection.add(row);
      };
      for (i = 0, _ref = this.response.results.length; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
        _fn(i);
      }
      this.renderTableBody(table);
      height = $(this.el).height() - $(this.el).find('header').height() - $(this.el).find('div.content div.head').height();
      $(this.el).find("div.content div.wrapper").css('height', "" + height + "px");
      $(this.el).find("div.content div.head").css("width", $(this.el).find("div.content table").width() + "px");
      table.find('thead th').each(function(i, th) {
        return $(_this.el).find("div.content div.head div:eq(" + i + ")").width($(th).width());
      });
      return table.css({
        'margin-top': '-' + table.find('thead').height() + 'px'
      });
    };
  
    EnrichmentView.prototype.renderTableBody = function(table) {
      var fragment, row, _i, _len, _ref;
      fragment = document.createDocumentFragment();
      _ref = this.collection.models;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        row = _ref[_i];
        fragment.appendChild(new EnrichmentRowView({
          "model": row,
          "template": this.template,
          "type": this.response.type,
          "matchCb": this.options.matchCb
        }).el);
      }
      return table.find('tbody').html(fragment);
    };
  
    EnrichmentView.prototype.formAction = function(e) {
      this.widget.formOptions[$(e.target).attr("name")] = $(e.target[e.target.selectedIndex]).attr("value");
      return this.widget.render();
    };
  
    EnrichmentView.prototype.selectAllAction = function() {
      this.collection.toggleSelected();
      this.renderToolbar();
      return this.renderTableBody($(this.el).find("div.content table"));
    };
  
    EnrichmentView.prototype.exportAction = function(e) {
      var ex, match, model, result, _i, _len, _ref;
      result = [];
      _ref = this.collection.selected();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        model = _ref[_i];
        result.push([model.get('description'), model.get('p-value')].join("\t") + "\t" + ((function() {
          var _j, _len2, _ref2, _results;
          _ref2 = model.get('matches');
          _results = [];
          for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
            match = _ref2[_j];
            _results.push(match.displayed);
          }
          return _results;
        })()).join());
      }
      if (result.length) {
        try {
          ex = new Exporter($(e.target), result.join("\n"), "" + this.widget.bagName + " " + this.widget.id + ".tsv");
        } catch (TypeError) {
          ex = new PlainExporter(result.join("\n"));
        }
        return window.setTimeout((function() {
          return ex.destroy();
        }), 5000);
      }
    };
  
    EnrichmentView.prototype.viewAction = function() {
      var match, model, pq, result, _i, _len, _ref;
      result = [];
      _ref = this.collection.selected();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        model = _ref[_i];
        Array.prototype.push.apply(result, (function() {
          var _j, _len2, _ref2, _results;
          _ref2 = model.get('matches');
          _results = [];
          for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
            match = _ref2[_j];
            _results.push(match.displayed);
          }
          return _results;
        })());
      }
      if (result.length) {
        pq = this.response.pathQuery;
        pq = JSON.parse(pq);
        pq.where.push({
          "path": this.response.pathConstraint,
          "op": "ONE OF",
          "values": result
        });
        return this.options.viewCb(pq);
      }
    };
  
    return EnrichmentView;
  
  })(Backbone.View);
  

  /* Enrichment Widget main class.
  */
  var EnrichmentWidget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  EnrichmentWidget = (function(_super) {
  
    __extends(EnrichmentWidget, _super);
  
    EnrichmentWidget.prototype.widgetOptions = {
      "title": true,
      "description": true,
      matchCb: function(id, type) {
        return typeof console !== "undefined" && console !== null ? console.log(id, type) : void 0;
      },
      viewCb: function(pq) {
        return typeof console !== "undefined" && console !== null ? console.log(pq) : void 0;
      }
    };
  
    EnrichmentWidget.prototype.formOptions = {
      errorCorrection: "Holm-Bonferroni",
      pValue: "0.05"
    };
  
    EnrichmentWidget.prototype.errorCorrections = ["Holm-Bonferroni", "Benjamini Hochberg", "Bonferroni", "None"];
  
    EnrichmentWidget.prototype.pValues = ["0.05", "0.10", "1.00"];
  
    EnrichmentWidget.prototype.spec = {
      response: {
        "title": type.isString,
        "description": type.isString,
        "pathQuery": type.isJSON,
        "pathConstraint": type.isString,
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
      }
    };
  
    function EnrichmentWidget(service, token, id, bagName, el, widgetOptions) {
      this.service = service;
      this.token = token;
      this.id = id;
      this.bagName = bagName;
      this.el = el;
      if (widgetOptions == null) widgetOptions = {};
      this.render = __bind(this.render, this);
      this.widgetOptions = merge(widgetOptions, this.widgetOptions);
      EnrichmentWidget.__super__.constructor.call(this);
      this.render();
    }
  
    EnrichmentWidget.prototype.render = function() {
      var _ref,
        _this = this;
      if ((_ref = this.view) != null) _ref.undelegateEvents();
      return $.ajax({
        url: "" + this.service + "list/enrichment",
        dataType: "jsonp",
        data: {
          widget: this.id,
          list: this.bagName,
          correction: this.formOptions.errorCorrection,
          maxp: this.formOptions.pValue,
          token: this.token
        },
        success: function(response) {
          _this.validateType(response, _this.spec.response);
          if (response.wasSuccessful) {
            return _this.view = new EnrichmentView({
              "widget": _this,
              "el": _this.el,
              "template": _this.template,
              "response": response,
              "form": {
                "options": _this.formOptions,
                "pValues": _this.pValues,
                "errorCorrections": _this.errorCorrections
              },
              "options": _this.widgetOptions
            });
          }
        },
        error: function(err) {
          return _this.error(err, "AJAXTransport");
        }
      });
    };
  
    return EnrichmentWidget;
  
  })(InterMineWidget);
  

  return {

    "InterMineWidget": InterMineWidget,
    "EnrichmentRowView": EnrichmentRowView,
    "EnrichmentMatchesView": EnrichmentMatchesView,
    "ChartView": ChartView,
    "ChartWidget": ChartWidget,
    "EnrichmentResults": EnrichmentResults,
    "EnrichmentView": EnrichmentView,
    "EnrichmentWidget": EnrichmentWidget,

  };
};
/* Interface to InterMine Widgets.
*/
var $,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = Object.prototype.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; },
  __slice = Array.prototype.slice,
  __indexOf = Array.prototype.indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

$ = window.jQuery || window.Zepto;

window.Widgets = (function() {

  Widgets.prototype.wait = true;

  Widgets.prototype.resources = [
    {
      name: "jQuery",
      path: "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js",
      type: "js",
      wait: true
    }, {
      name: "_",
      path: "http://documentcloud.github.com/underscore/underscore.js",
      type: "js",
      wait: true
    }, {
      name: "Backbone",
      path: "http://documentcloud.github.com/backbone/backbone-min.js",
      type: "js"
    }, {
      name: "google",
      path: "https://www.google.com/jsapi",
      type: "js"
    }
  ];

  function Widgets(service, token) {
    var _this = this;
    this.service = service;
    this.token = token != null ? token : "";
    this.all = __bind(this.all, this);
    this.enrichment = __bind(this.enrichment, this);
    this.chart = __bind(this.chart, this);
    new Load(this.resources, function() {
      $ = window.jQuery;
      $.support.cors = true;
      __extends(o, factory(window.Backbone));
      return _this.wait = false;
    });
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
          })(o.ChartWidget, [_this.service, _this.token].concat(__slice.call(opts)), function() {});
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
      })(o.EnrichmentWidget, [this.service, this.token].concat(__slice.call(opts)), function() {});
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
        dataType: "jsonp",
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
        error: function(xhr, opts, err) {
          return $(el).html($('<div/>', {
            "class": "alert alert-error",
            html: "" + xhr.statusText + " for <a href='" + _this.service + "widgets'>" + _this.service + "widgets</a>"
          }));
        }
      });
    }
  };

  return Widgets;

})();

}).call(this);