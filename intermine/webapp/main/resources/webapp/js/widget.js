(function() {
  var CSSLoader, ChartWidget, EnrichmentWidget, InterMineWidget, JSLoader, Loader, root,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; },
    __slice = Array.prototype.slice,
    __indexOf = Array.prototype.indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  root = this;

  InterMineWidget = (function() {

    function InterMineWidget() {
      this.error = __bind(this.error, this);      $(this.el).html($('<div/>', {
        "class": "inner",
        style: "height:572px;overflow:hidden"
      }));
      this.el = "" + this.el + " div.inner";
    }

    InterMineWidget.prototype.error = function(err, template) {
      return $(this.el).html(_.template(template, {
        "title": err.statusText,
        "text": err.responseText
      }));
    };

    return InterMineWidget;

  })();

  ChartWidget = (function(_super) {

    __extends(ChartWidget, _super);

    ChartWidget.prototype.chartOptions = {
      fontName: "Sans-Serif",
      fontSize: 9,
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

    ChartWidget.prototype.templates = {
      normal: "<header>\n    <% if (title) { %>\n        <h3><%= title %></h3>\n    <% } %>\n    <% if (description) { %>\n        <p><%= description %></p>\n    <% } %>\n    <% if (notAnalysed > 0) { %>\n        <p>Number of Genes in this list not analysed in this widget: <span class=\"label label-info\"><%= notAnalysed %></span></p>\n    <% } %>\n</header>\n<div class=\"content\"></div>",
      error: "<div class=\"alert alert-block\">\n    <h4 class=\"alert-heading\"><%= title %></h4>\n    <p><%= text %></p>\n</div>"
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
        "selectCb": function(pq) {
          return console.log(pq);
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
          filter: "",
          token: this.token
        },
        success: function(response) {
          var chart;
          if (response.wasSuccessful) {
            $(_this.el).html(_.template(_this.templates.normal, {
              "title": _this.widgetOptions.title ? response.title : "",
              "description": _this.widgetOptions.description ? response.description : "",
              "notAnalysed": response.notAnalysed
            }));
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
                      pq = pq.replace("%series", response.results[0][item.column]);
                    }
                    _results.push(_this.widgetOptions.selectCb(pq));
                  } else {
                    _results.push(void 0);
                  }
                }
                return _results;
              });
            }
          }
        },
        error: function(err) {
          return _this.error(err, _this.templates.error);
        }
      });
    };

    return ChartWidget;

  })(InterMineWidget);

  EnrichmentWidget = (function(_super) {

    __extends(EnrichmentWidget, _super);

    EnrichmentWidget.prototype.formOptions = {
      errorCorrection: "Holm-Bonferroni",
      pValue: 0.05,
      dataSet: "All datasets"
    };

    EnrichmentWidget.prototype.errorCorrections = ["Holm-Bonferroni", "Benjamini Hochberg", "Bonferroni", "None"];

    EnrichmentWidget.prototype.pValues = [0.05, 0.10, 1.00];

    EnrichmentWidget.prototype.templates = {
      normal: "<header>\n    <% if (title) { %>\n        <h3><%= title %></h3>\n    <% } %>\n    <% if (description) { %>\n        <p><%= description %></p>\n    <% } %>\n    <% if (notAnalysed > 0) { %>\n        <p>Number of Genes in this list not analysed in this widget: <span class=\"label label-info\"><%= notAnalysed %></span></p>\n    <% } %>\n    <div class=\"form\"></div>\n</header>\n<div class=\"content\" style=\"overflow:auto;overflow-x:hidden;height:400px\"></div>",
      form: "<form>\n    <div class=\"group\" style=\"display:inline-block;margin-right:5px\">\n        <label>Test Correction</label>\n        <select name=\"errorCorrection\" class=\"span2\">\n            <% for (var i = 0; i < errorCorrections.length; i++) { %>\n                <% var correction = errorCorrections[i] %>\n                <option value=\"<%= correction %>\" <%= (options.errorCorrection == correction) ? 'selected=\"selected\"' : \"\" %>><%= correction %></option>\n            <% } %>\n        </select>\n    </div>\n\n    <div class=\"group\" style=\"display:inline-block;margin-right:5px\">\n        <label>Max p-value</label>\n        <select name=\"pValue\" class=\"span2\">\n            <% for (var i = 0; i < pValues.length; i++) { %>\n                <% var p = pValues[i] %>\n                <option value=\"<%= p %>\" <%= (options.pValue == p) ? 'selected=\"selected\"' : \"\" %>><%= p %></option>\n            <% } %>\n        </select>\n    </div>\n</form>",
      extra: "<div class=\"group\" style=\"display:inline-block;margin-right:5px\">\n    <label><%= label %></label>\n    <select name=\"dataSet\" class=\"span2\">\n        <% for (var i = 0; i < possible.length; i++) { %>\n            <% var v = possible[i] %>\n            <option value=\"<%= v %>\" <%= (selected == v) ? 'selected=\"selected\"' : \"\" %>><%= v %></option>\n        <% } %>\n    </select>\n</div>",
      table: "<table class=\"table table-striped\">\n    <thead>\n        <tr>\n            <th><%= label %></th>\n            <th>p-Value</th>\n            <th>Matches</th>\n        </tr>\n    </thead>\n    <tbody></tbody>\n</table>",
      row: "<tr>\n    <td class=\"description\"><%= row[\"description\"] %></td>\n    <td class=\"pValue\"><%= row[\"p-value\"].toFixed(7) %></td>\n    <td class=\"matches\" style=\"position:relative\">\n        <span class=\"count label label-success\" style=\"cursor:pointer\"><%= row[\"matches\"].length %></span>\n    </td>\n</tr>",
      matches: "<div class=\"popover\" style=\"position:absolute;top:22px;right:0;z-index:1;display:block\">\n    <div class=\"popover-inner\" style=\"width:300px;margin-left:-300px\">\n        <a style=\"cursor:pointer;margin:2px 5px 0 0\" class=\"close\">×</a>\n        <h3 class=\"popover-title\"></h3>\n        <div class=\"popover-content\">\n            <% for (var i = 0; i < matches.length; i++) { %>\n                <a href=\"#\"><%= matches[i] %></a><%= (i < matches.length -1) ? \",\" : \"\" %>\n            <% } %>\n        </div>\n    </div>\n</div>",
      noresults: "<div class=\"alert alert-info\">\n    <p>The Widget has no results.</p>\n</div>",
      error: "<div class=\"alert alert-block\">\n    <h4 class=\"alert-heading\"><%= title %></h4>\n    <p><%= text %></p>\n</div>"
    };

    function EnrichmentWidget(service, token, id, bagName, el, widgetOptions) {
      this.service = service;
      this.token = token;
      this.id = id;
      this.bagName = bagName;
      this.el = el;
      this.widgetOptions = widgetOptions != null ? widgetOptions : {
        "title": true,
        "description": true
      };
      this.matchesClick = __bind(this.matchesClick, this);
      this.formClick = __bind(this.formClick, this);
      this.render = __bind(this.render, this);
      EnrichmentWidget.__super__.constructor.call(this);
      this.render();
    }

    EnrichmentWidget.prototype.render = function() {
      var _this = this;
      return $.ajax({
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
          var height, row, table, _fn, _i, _len, _ref;
          if (response.wasSuccessful) {
            $(_this.el).html(_.template(_this.templates.normal, {
              "title": _this.widgetOptions.title ? response.title : "",
              "description": _this.widgetOptions.description ? response.description : "",
              "notAnalysed": response.notAnalysed
            }));
            $(_this.el).find("div.form").html(_.template(_this.templates.form, {
              "options": _this.formOptions,
              "errorCorrections": _this.errorCorrections,
              "pValues": _this.pValues
            }));
            if (response.extraAttributeLabel != null) {
              $(_this.l).find('div.form form').append(_.template(_this.templates.extra, {
                "label": response.extraAttributeLabel,
                "possible": response.extraAttributePossibleValues,
                "selected": response.extraAttributeSelectedValue
              }));
            }
            if (response.results.length > 0) {
              height = $(_this.el).height() - $(_this.el).find('header').height() - 18;
              $(_this.el).find("div.content").html($(_.template(_this.templates.table, {
                "label": response.label
              }))).css("height", "" + height + "px");
              table = $(_this.el).find("div.content table");
              _ref = response.results;
              _fn = function(row) {
                var td, tr;
                table.append(tr = $(_.template(_this.templates.row, {
                  "row": row
                })));
                return td = tr.find("td.matches .count").click(function() {
                  return _this.matchesClick(td, row["matches"]);
                });
              };
              for (_i = 0, _len = _ref.length; _i < _len; _i++) {
                row = _ref[_i];
                _fn(row);
              }
            } else {
              $(_this.el).find("div.content").html($(_.template(_this.templates.noresults)));
            }
            return $(_this.el).find("form select").change(_this.formClick);
          }
        },
        error: function(err) {
          return _this.error(err, _this.templates.error);
        }
      });
    };

    EnrichmentWidget.prototype.formClick = function(e) {
      this.formOptions[$(e.target).attr("name")] = $(e.target[e.target.selectedIndex]).attr("value");
      return this.render();
    };

    EnrichmentWidget.prototype.matchesClick = function(target, matches) {
      var modal;
      target.after(modal = $(_.template(this.templates.matches, {
        "matches": matches
      })));
      return modal.find("a.close").click(function() {
        return modal.remove();
      });
    };

    return EnrichmentWidget;

  })(InterMineWidget);

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

  window.Widgets = (function() {

    Widgets.prototype.resources = {
      js: {
        jQuery: "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js",
        _: "http://documentcloud.github.com/underscore/underscore.js",
        google: "https://www.google.com/jsapi"
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
            if (library === "jQuery") root.$ = window.jQuery;
            return _this.wait -= 1;
          });
        } else {
          if (library === "jQuery") return root.$ = window.jQuery;
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
        }), 1000);
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
        }), 1000);
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
        }), 1000);
      } else {
        return $.getJSON("" + this.service + "widgets", function(response) {
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
                  _results.push(new ChartWidget(_this.service, _this.token, widget.name, bagName, "#" + el + " #" + widgetEl, widgetOptions));
                  break;
                case "enrichment":
                  _results.push(new EnrichmentWidget(_this.service, _this.token, widget.name, bagName, "#" + el + " #" + widgetEl, widgetOptions));
                  break;
                default:
                  _results.push(void 0);
              }
            }
            return _results;
          }
        });
      }
    };

    return Widgets;

  })();

}).call(this);
