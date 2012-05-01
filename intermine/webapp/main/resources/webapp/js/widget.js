(function() {
var o = {};

var JST = {};
JST["table.normal.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push("<header>\n    <h3>"),this.title&&b.push(d(this.title)),b.push("</h3>\n    <p>"),this.description&&b.push(this.description),b.push("</p>\n    "),this.notAnalysed&&(b.push("\n        <p>Number of Genes in this list not analysed in this widget: <a>"),b.push(d(this.notAnalysed)),b.push("</a></p>\n    ")),b.push('\n\n    <div class="actions" style="padding:10px 0">\n        <!-- actions.eco -->\n    </div>\n</header>\n<div class="content">\n    <!-- table.table.eco -->\n</div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["chart.extra.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f;b.push('<div class="group" style="display:inline-block;margin-right:5px">\n    <label>'),b.push(d(this.label)),b.push('</label>\n    <select name="'),b.push(d(this.label)),b.push('" class="span3">\n        '),f=this.possible;for(c=0,e=f.length;c<e;c++)a=f[c],b.push('\n            <option value="'),b.push(d(a)),b.push('" '),this.selected===a&&b.push(d('selected="selected"')),b.push(">\n                "),b.push(d(a)),b.push("\n            </option>\n        ");b.push("\n    </select>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["error.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<div class="alert alert-block">\n    <h4 class="alert-heading">'),b.push(d(this.title)),b.push(" for "),b.push(d(this.name)),b.push("</h4>\n    <p>"),b.push(this.text),b.push("</p>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["table.row.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f;b.push('<td class="check"><input type="checkbox" '),this.row.selected&&b.push('checked="checked"'),b.push(" /></td>\n"),f=this.row.descriptions;for(c=0,e=f.length;c<e;c++)a=f[c],b.push("\n    <td>"),b.push(d(a)),b.push("</td>\n");b.push("\n<td>"),b.push(d(this.row.matches)),b.push("</td>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["actions.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<a class="btn btn-small '),this.disabled&&b.push("disabled"),b.push(' view">View</a>\n<a class="btn btn-small '),this.disabled&&b.push("disabled"),b.push(' export">Download</a>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.row.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<td class="check"><input type="checkbox" '),this.row.selected&&b.push('checked="checked"'),b.push(' /></td>\n<td class="description">\n    '),b.push(d(this.row.description)),b.push("\n    "),this.row.externalLink&&(b.push('\n        [<a href="'),b.push(this.row.externalLink),b.push('" target="_blank">Link</a>]\n    ')),b.push('\n</td>\n<td class="pValue">'),b.push(d(this.row["p-value"].toPrecision(5))),b.push('</td>\n<td class="matches">\n    <a class="count" style="cursor:pointer">'),b.push(d(this.row.matches.length)),b.push("</a>\n</td>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["noresults.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<div class="alert alert-info">\n    <p>The Widget has no results.</p>\n</div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.table.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<!-- actual fixed head -->\n<div class="head" style="display:table">\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;"><input type="checkbox" class="check" /></div>\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;">'),b.push(d(this.label)),b.push('</div>\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;">p-Value</div>\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;">Matches</div>\n    <div style="clear:both"></div>\n</div>\n<div class="wrapper" style="overflow:auto;overflow-x:hidden">\n    <table class="table table-striped">\n        <!-- head for proper cell width -->\n        <thead style="visibility:hidden">\n            <tr>\n                <th></th>\n                <th>'),b.push(d(this.label)),b.push("</th>\n                <th>p-Value</th>\n                <th>Matches</th>\n            </tr>\n        </thead>\n        <tbody>\n            <!-- loop enrichment.row.eco -->\n        </tbody>\n    </table>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["popover.values.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f;b.push("<h4>"),b.push(d(this.values.length)),b.push(" "),b.push(d(this.type)),this.values.length!==1&&b.push(d("s")),b.push(":</h4>\n\n"),f=this.values.slice(0,this.valuesLimit-1+1||9e9);for(c=0,e=f.length;c<e;c++)a=f[c],b.push('\n    <a href="#" class="match">'),b.push(d(a)),b.push("</a>\n");b.push("\n"),this.values.length>this.valuesLimit&&b.push("&hellip;")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["table.table.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f,g,h,i;b.push('<!-- actual fixed head -->\n<div class="head" style="display:table">\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;"><input type="checkbox" class="check" /></div>\n    '),h=this.columns;for(c=0,f=h.length;c<f;c++)a=h[c],b.push('\n        <div style="font-weight:bold;display:table-cell;padding:0 8px;">'),b.push(d(a)),b.push("</div>\n    ");b.push('\n    <div style="clear:both"></div>\n</div>\n<div class="wrapper" style="overflow:auto;overflow-x:hidden">\n    <table class="table table-striped">\n        <!-- head for proper cell width -->\n        <thead style="visibility:hidden">\n            <tr>\n                <th></th>\n                '),i=this.columns;for(e=0,g=i.length;e<g;e++)a=i[e],b.push("\n                    <th>"),b.push(d(a)),b.push("</th>\n                ");b.push("\n            </tr>\n        </thead>\n        <tbody>\n            <!-- loop table.row.eco -->\n        </tbody>\n    </table>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.form.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f,g,h,i,j;b.push('<form style="margin:0">\n    <div class="group" style="display:inline-block;margin-right:5px">\n        <label>Test Correction</label>\n        <select name="errorCorrection" class="span2">\n            '),i=this.errorCorrections;for(e=0,g=i.length;e<g;e++)a=i[e],b.push('\n                <option value="'),b.push(d(a)),b.push('" '),this.options.errorCorrection===a&&b.push(d('selected="selected"')),b.push(">\n                    "),b.push(d(a)),b.push("\n            </option>\n            ");b.push('\n        </select>\n    </div>\n\n    <div class="group" style="display:inline-block;margin-right:5px">\n        <label>Max p-value</label>\n        <select name="pValue" class="span2">\n            '),j=this.pValues;for(f=0,h=j.length;f<h;f++)c=j[f],b.push('\n                <option value="'),b.push(d(c)),b.push('" '),this.options.pValue===c&&b.push(d('selected="selected"')),b.push(">\n                    "),b.push(d(c)),b.push("\n                </option>\n            ");b.push("\n        </select>\n    </div>\n</form>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.normal.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push("<header>\n    <h3>"),this.title&&b.push(d(this.title)),b.push("</h3>\n    <p>"),this.description&&b.push(this.description),b.push("</p>\n    "),this.notAnalysed&&(b.push("\n        <p>Number of Genes in this list not analysed in this widget: <a>"),b.push(d(this.notAnalysed)),b.push("</a></p>\n    ")),b.push('\n\n    <div class="form">\n        <!-- enrichment.form.eco -->\n    </div>\n\n    <div class="actions" style="padding:10px 0">\n        <!-- actions.eco -->\n    </div>\n</header>\n<div class="content">\n    <!-- enrichment.table.eco -->\n</div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["loading.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<div class="loading" style="background:rgba(255,255,255,0.9);position:absolute;top:0;left:0;height:100%;width:100%;text-align:center;">\n    <p style="padding-top:50%;font-weight:bold;">Loading &hellip;</p>\n</div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["chart.normal.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push("<header>\n    <h3>"),this.title&&b.push(d(this.title)),b.push("</h3>\n    <p>"),this.description&&b.push(this.description),b.push("</p>\n    "),this.notAnalysed&&(b.push("\n        <p>Number of Genes in this list not analysed in this widget: <a>"),b.push(d(this.notAnalysed)),b.push("</a></p>\n    ")),b.push('\n\n    <div class="form">\n        <form style="margin:0">\n            <!-- chart.extra.eco -->\n        </form>\n    </div>\n</header>\n<div class="content"></div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["enrichment.extra.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){var a,c,e,f;b.push('<div class="group" style="display:inline-block;margin-right:5px">\n    <label>'),b.push(d(this.label)),b.push('</label>\n    <select name="'),b.push(d(this.label)),b.push('" class="span2">\n        '),f=this.possible;for(c=0,e=f.length;c<e;c++)a=f[c],b.push('\n            <option value="'),b.push(d(a)),b.push('" '),this.selected===a&&b.push(d('selected="selected"')),b.push(">\n                "),b.push(d(a)),b.push("\n            </option>\n        ");b.push("\n    </select>\n</div>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["popover.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<div class="popover" style="position:absolute;top:5px;right:0;z-index:1;display:block">\n    <div class="popover-inner" style="'),b.push(d(this.style)),b.push('">\n        <a style="cursor:pointer;margin:2px 5px 0 0" class="close">×</a>\n        <h3 class="popover-title">\n            '),b.push(d(this.description.slice(0,this.descriptionLimit-1+1||9e9))),b.push("\n            "),this.description.length>this.descriptionLimit&&b.push("&hellip;"),b.push('\n        </h3>\n        <div class="popover-content">\n            <div class="values">\n                <!-- popover.values.eco -->\n            </div>\n            <div style="margin-top:10px">\n                <a class="btn btn-small btn-primary results">View results</a>\n                <a class="btn btn-small list disabled">Create list</a>\n            </div>\n        </div>\n    </div>\n</div>')})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
JST["invalidjsonkey.eco"]=function(a){a||(a={});var b=[],c=function(a){var c=b,d;return b=[],a.call(this),d=b.join(""),b=c,e(d)},d=function(a){return a&&a.ecoSafe?a:typeof a!="undefined"&&a!=null?g(a):""},e,f=a.safe,g=a.escape;return e=a.safe=function(a){if(a&&a.ecoSafe)return a;if(typeof a=="undefined"||a==null)a="";var b=new String(a);return b.ecoSafe=!0,b},g||(g=a.escape=function(a){return(""+a).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){((function(){b.push('<li style="vertical-align:bottom">\n    <span style="display:inline-block" class="label label-important">'),b.push(d(this.key)),b.push("</span> is "),b.push(d(this.actual)),b.push("; was expecting "),b.push(d(this.expected)),b.push("\n</li>")})).call(this)}.call(a),a.safe=f,a.escape=g,b.join("")}
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
            break;
          case "css":
            new CSSLoader(resource.path);
            this.done(resource);
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
        style: "height:572px;overflow:hidden;position:relative"
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
      opts.name = this.name || this.id;
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
  

  /* Models underpinning Table Widget results.
  */
  var TableResults, TableRow,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  TableRow = (function(_super) {
  
    __extends(TableRow, _super);
  
    function TableRow() {
      this.toggleSelected = __bind(this.toggleSelected, this);
      this.validate = __bind(this.validate, this);
      TableRow.__super__.constructor.apply(this, arguments);
    }
  
    TableRow.prototype.defaults = {
      "selected": false
    };
  
    TableRow.prototype.spec = {
      "matches": type.isInteger,
      "identifier": type.isInteger,
      "descriptions": type.isArray,
      "selected": type.isBoolean
    };
  
    TableRow.prototype.initialize = function(row, widget) {
      this.widget = widget;
      return this.validate(row);
    };
  
    TableRow.prototype.validate = function(row) {
      return this.widget.validateType(row, this.spec);
    };
  
    TableRow.prototype.toggleSelected = function() {
      return this.set({
        selected: !this.get("selected")
      });
    };
  
    return TableRow;
  
  })(Backbone.Model);
  
  TableResults = (function(_super) {
  
    __extends(TableResults, _super);
  
    function TableResults() {
      TableResults.__super__.constructor.apply(this, arguments);
    }
  
    TableResults.prototype.model = TableRow;
  
    TableResults.prototype.selected = function() {
      return this.filter(function(row) {
        return row.get("selected");
      });
    };
  
    TableResults.prototype.toggleSelected = function() {
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
  
    return TableResults;
  
  })(Backbone.Collection);
  

  /* Table Widget table row matches box.
  */
  var TableMatchesView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  TableMatchesView = (function(_super) {
  
    __extends(TableMatchesView, _super);
  
    function TableMatchesView() {
      this.close = __bind(this.close, this);
      this.listAction = __bind(this.listAction, this);
      this.resultsAction = __bind(this.resultsAction, this);
      this.matchAction = __bind(this.matchAction, this);
      this.renderValues = __bind(this.renderValues, this);
      this.render = __bind(this.render, this);
      TableMatchesView.__super__.constructor.apply(this, arguments);
    }
  
    TableMatchesView.prototype.descriptionLimit = 50;
  
    TableMatchesView.prototype.valuesLimit = 5;
  
    TableMatchesView.prototype.events = {
      "click a.match": "matchAction",
      "click a.results": "resultsAction",
      "click a.list": "listAction",
      "click a.close": "close"
    };
  
    TableMatchesView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      return this.render();
    };
  
    TableMatchesView.prototype.render = function() {
      var values,
        _this = this;
      $(this.el).css({
        'position': 'relative'
      });
      $(this.el).html(this.template("popover", {
        "description": this.description,
        "descriptionLimit": this.descriptionLimit,
        "style": 'width:300px'
      }));
      this.pathQuery = JSON.parse(this.pathQuery);
      this.pathQuery.where.push({
        "path": this.pathConstraint,
        "op": "ONE OF",
        "values": this.identifiers
      });
      values = [];
      this.imjs.query(this.pathQuery, function(q) {
        console.log(q.toXML());
        return q.rows(function(response) {
          var object, _i, _len;
          for (_i = 0, _len = response.length; _i < _len; _i++) {
            object = response[_i];
            values.push((function(object) {
              var column, _j, _len2;
              for (_j = 0, _len2 = object.length; _j < _len2; _j++) {
                column = object[_j];
                if (column.length > 0) return column;
              }
            })(object));
          }
          return _this.renderValues(values);
        });
      });
      return this;
    };
  
    TableMatchesView.prototype.renderValues = function(values) {
      return $(this.el).find('div.values').html(this.template("popover.values", {
        'values': values,
        'type': this.type,
        'valuesLimit': this.valuesLimit
      }));
    };
  
    TableMatchesView.prototype.matchAction = function(e) {
      this.matchCb($(e.target).text(), this.type);
      return e.preventDefault();
    };
  
    TableMatchesView.prototype.resultsAction = function() {
      return this.resultsCb(this.pathQuery);
    };
  
    TableMatchesView.prototype.listAction = function() {
      return this.listCb(this.pathQuery);
    };
  
    TableMatchesView.prototype.close = function() {
      return $(this.el).remove();
    };
  
    return TableMatchesView;
  
  })(Backbone.View);
  

  /* Enrichment Widget table row.
  */
  var EnrichmentRowView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  EnrichmentRowView = (function(_super) {
  
    __extends(EnrichmentRowView, _super);
  
    function EnrichmentRowView() {
      this.toggleMatchesAction = __bind(this.toggleMatchesAction, this);
      this.selectAction = __bind(this.selectAction, this);
      this.render = __bind(this.render, this);
      EnrichmentRowView.__super__.constructor.apply(this, arguments);
    }
  
    EnrichmentRowView.prototype.tagName = "tr";
  
    EnrichmentRowView.prototype.events = {
      "click td.check input": "selectAction",
      "click td.matches a.count": "toggleMatchesAction"
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
  
    EnrichmentRowView.prototype.toggleMatchesAction = function() {
      if (!(this.matchesView != null)) {
        return $(this.el).find('td.matches a.count').after((this.matchesView = new EnrichmentMatchesView({
          "matches": this.model.get("matches"),
          "identifiers": [this.model.get("identifier")],
          "description": this.model.get("description"),
          "template": this.template,
          "matchCb": this.callbacks.matchCb,
          "resultsCb": this.callbacks.resultsCb,
          "listCb": this.callbacks.listCb,
          "response": this.response
        })).el);
      } else {
        return this.matchesView.toggle();
      }
    };
  
    return EnrichmentRowView;
  
  })(Backbone.View);
  

  /* Table Widget table row.
  */
  var TableRowView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  TableRowView = (function(_super) {
  
    __extends(TableRowView, _super);
  
    function TableRowView() {
      this.selectAction = __bind(this.selectAction, this);
      this.render = __bind(this.render, this);
      TableRowView.__super__.constructor.apply(this, arguments);
    }
  
    TableRowView.prototype.tagName = "tr";
  
    TableRowView.prototype.events = {
      "click td.check input": "selectAction"
    };
  
    TableRowView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      this.model.bind('change', this.render);
      return this.render();
    };
  
    TableRowView.prototype.render = function() {
      $(this.el).html(this.template("table.row", {
        "row": this.model.toJSON()
      }));
      return this;
    };
  
    TableRowView.prototype.selectAction = function() {
      return this.model.toggleSelected();
    };
  
    return TableRowView;
  
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
      this.listAction = __bind(this.listAction, this);
      this.resultsAction = __bind(this.resultsAction, this);
      this.matchAction = __bind(this.matchAction, this);
      this.getPq = __bind(this.getPq, this);
      this.toggle = __bind(this.toggle, this);
      this.render = __bind(this.render, this);
      EnrichmentMatchesView.__super__.constructor.apply(this, arguments);
    }
  
    EnrichmentMatchesView.prototype.descriptionLimit = 50;
  
    EnrichmentMatchesView.prototype.valuesLimit = 5;
  
    EnrichmentMatchesView.prototype.events = {
      "click a.match": "matchAction",
      "click a.results": "resultsAction",
      "click a.list": "listAction",
      "click a.close": "toggle"
    };
  
    EnrichmentMatchesView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      return this.render();
    };
  
    EnrichmentMatchesView.prototype.render = function() {
      var x;
      $(this.el).css({
        'position': 'relative'
      });
      $(this.el).html(this.template("popover", {
        "description": this.description,
        "descriptionLimit": this.descriptionLimit,
        "style": this.style || "width:300px;margin-left:-300px"
      }));
      $(this.el).find('div.values').html(this.template("popover.values", {
        'values': (function() {
          var _i, _len, _ref, _results;
          _ref = this.matches;
          _results = [];
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            x = _ref[_i];
            _results.push(x['displayed']);
          }
          return _results;
        }).call(this),
        'type': this.response.type,
        'valuesLimit': this.valuesLimit
      }));
      return this;
    };
  
    EnrichmentMatchesView.prototype.toggle = function() {
      return $(this.el).toggle();
    };
  
    EnrichmentMatchesView.prototype.getPq = function() {
      var pq;
      pq = this.response.pathQuery;
      this.pq = JSON.parse(pq);
      return this.pq.where.push({
        "path": this.response.pathConstraint,
        "op": "ONE OF",
        "values": this.identifiers
      });
    };
  
    EnrichmentMatchesView.prototype.matchAction = function(e) {
      this.matchCb($(e.target).text(), this.response.type);
      return e.preventDefault();
    };
  
    EnrichmentMatchesView.prototype.resultsAction = function() {
      if (this.pq == null) this.getPq();
      return this.resultsCb(this.pq);
    };
  
    EnrichmentMatchesView.prototype.listAction = function() {
      if (this.pq == null) this.getPq();
      return this.listCb(this.pq);
    };
  
    return EnrichmentMatchesView;
  
  })(Backbone.View);
  

  /* View maintaining Table Widget.
  */
  var TableView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  TableView = (function(_super) {
  
    __extends(TableView, _super);
  
    function TableView() {
      this.viewAction = __bind(this.viewAction, this);
      this.exportAction = __bind(this.exportAction, this);
      this.selectAllAction = __bind(this.selectAllAction, this);
      this.renderTableBody = __bind(this.renderTableBody, this);
      this.renderTable = __bind(this.renderTable, this);
      this.renderToolbar = __bind(this.renderToolbar, this);
      TableView.__super__.constructor.apply(this, arguments);
    }
  
    TableView.prototype.events = {
      "click div.actions a.view": "viewAction",
      "click div.actions a.export": "exportAction",
      "click div.content input.check": "selectAllAction"
    };
  
    TableView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      this.collection = new TableResults();
      this.collection.bind('change', this.renderToolbar);
      return this.render();
    };
  
    TableView.prototype.render = function() {
      $(this.el).html(this.template("table.normal", {
        "title": this.options.title ? this.response.title : "",
        "description": this.options.description ? this.response.description : "",
        "notAnalysed": this.response.notAnalysed
      }));
      if (this.response.results.length > 0) {
        this.renderToolbar();
        this.renderTable();
      } else {
        $(this.el).find("div.content").html($(this.template("noresults")));
      }
      return this;
    };
  
    TableView.prototype.renderToolbar = function() {
      return $(this.el).find("div.actions").html($(this.template("actions", {
        "disabled": this.collection.selected().length === 0
      })));
    };
  
    TableView.prototype.renderTable = function() {
      var height, i, table, _fn, _ref,
        _this = this;
      $(this.el).find("div.content").html($(this.template("table.table", {
        "columns": this.response.columns.split(',')
      })));
      table = $(this.el).find("div.content table");
      _fn = function(i) {
        var row;
        row = new TableRow(_this.response.results[i], _this.widget);
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
  
    TableView.prototype.renderTableBody = function(table) {
      var fragment, row, _i, _len, _ref;
      fragment = document.createDocumentFragment();
      _ref = this.collection.models;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        row = _ref[_i];
        fragment.appendChild(new TableRowView({
          "model": row,
          "template": this.template,
          "response": this.response
        }).el);
      }
      return table.find('tbody').html(fragment);
    };
  
    TableView.prototype.selectAllAction = function() {
      this.collection.toggleSelected();
      this.renderToolbar();
      return this.renderTableBody($(this.el).find("div.content table"));
    };
  
    TableView.prototype.exportAction = function(e) {
      var ex, model, result, _i, _len, _ref;
      result = [this.response.columns.replace(/,/g, "\t")];
      _ref = this.collection.selected();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        model = _ref[_i];
        result.push(model.get('descriptions').join("\t") + "\t" + model.get('matches'));
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
  
    TableView.prototype.viewAction = function() {
      var descriptions, model, rowIdentifiers, _i, _len, _ref, _ref2;
      descriptions = [];
      rowIdentifiers = [];
      _ref = this.collection.selected();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        model = _ref[_i];
        descriptions.push(model.get('descriptions')[0]);
        rowIdentifiers.push(model.get('identifier'));
      }
      if (rowIdentifiers.length) {
        if ((_ref2 = this.matchesView) != null) _ref2.remove();
        return $(this.el).find('div.actions').after((this.matchesView = new TableMatchesView({
          "identifiers": rowIdentifiers,
          "description": descriptions.join(', '),
          "template": this.template,
          "matchCb": this.options.matchCb,
          "resultsCb": this.options.resultsCb,
          "listCb": this.options.listCb,
          "pathQuery": this.response.pathQuery,
          "pathConstraint": this.response.pathConstraint,
          "imjs": new intermine.Service({
            'root': this.widget.service,
            'token': this.widget.token
          }),
          "type": this.response.type
        })).el);
      }
    };
  
    return TableView;
  
  })(Backbone.View);
  

  /* View maintaining Chart Widget.
  */
  var ChartView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  ChartView = (function(_super) {
  
    __extends(ChartView, _super);
  
    function ChartView() {
      this.formAction = __bind(this.formAction, this);
      ChartView.__super__.constructor.apply(this, arguments);
    }
  
    ChartView.prototype.chartOptions = {
      fontName: "Sans-Serif",
      fontSize: 11,
      width: 460,
      height: 450,
      colors: ["#2F72FF", "#9FC0FF"],
      legend: {
        position: "top"
      },
      chartArea: {
        top: 30,
        left: 50,
        width: 400,
        height: 305
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
  
    ChartView.prototype.events = {
      "change div.form select": "formAction"
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
      if (this.response.filterLabel != null) {
        $(this.el).find('div.form form').append(this.template("chart.extra", {
          "label": this.response.filterLabel,
          "possible": this.response.filters.split(','),
          "selected": this.response.filterSelectedValue
        }));
      }
      if (this.response.results.length > 1) {
        if (this.response.chartType in google.visualization) {
          chart = new google.visualization[this.response.chartType]($(this.el).find("div.content")[0]);
          chart.draw(google.visualization.arrayToDataTable(this.response.results, false), this.chartOptions);
          if (this.response.pathQuery != null) {
            return google.visualization.events.addListener(chart, "select", function() {
              var column, description, item, quickPq, resultsPq, row, translate, _i, _len, _ref;
              translate = function(response, series) {
                if (response.seriesValues != null) {
                  return response.seriesValues.split(',')[response.seriesLabels.split(',').indexOf(series)];
                }
              };
              description = '';
              resultsPq = _this.response.pathQuery;
              quickPq = _this.response.simplePathQuery;
              _ref = chart.getSelection();
              for (_i = 0, _len = _ref.length; _i < _len; _i++) {
                item = _ref[_i];
                if (item.row != null) {
                  row = _this.response.results[item.row + 1][0];
                  description += row;
                  resultsPq = resultsPq.replace("%category", row);
                  quickPq = quickPq.replace("%category");
                  if (item.column != null) {
                    column = _this.response.results[0][item.column];
                    description += ' ' + column;
                    resultsPq = resultsPq.replace("%series", translate(_this.response, column));
                    quickPq = resultsPq.replace("%series", translate(_this.response, column));
                  }
                }
              }
              resultsPq = typeof JSON !== "undefined" && JSON !== null ? JSON.parse(resultsPq) : void 0;
              quickPq = typeof JSON !== "undefined" && JSON !== null ? JSON.parse(quickPq) : void 0;
              if (_this.barView != null) _this.barView.close();
              if (description) {
                return $(_this.el).find('div.content').append((_this.barView = new ChartBarView({
                  "description": description,
                  "template": _this.template,
                  "resultsPq": resultsPq,
                  "resultsCb": _this.options.resultsCb,
                  "listCb": _this.options.listCb,
                  "matchCb": _this.options.matchCb,
                  "quickPq": quickPq,
                  "imjs": new intermine.Service({
                    'root': _this.widget.service,
                    'token': _this.widget.token
                  }),
                  "type": _this.response.type
                })).el);
              }
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
  
    ChartView.prototype.formAction = function(e) {
      this.widget.formOptions[$(e.target).attr("name")] = $(e.target[e.target.selectedIndex]).attr("value");
      return this.widget.render();
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
      matchCb: function(id, type) {
        return typeof console !== "undefined" && console !== null ? console.log(id, type) : void 0;
      },
      resultsCb: function(pq) {
        return typeof console !== "undefined" && console !== null ? console.log(pq) : void 0;
      },
      listCb: function(pq) {
        return typeof console !== "undefined" && console !== null ? console.log(pq) : void 0;
      }
    };
  
    ChartWidget.prototype.formOptions = {};
  
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
        "wasSuccessful": type.isBoolean,
        "filters": type.isString,
        "filterLabel": type.isString,
        "filterSelectedValue": type.isString,
        "simplePathQuery": type.isString
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
      var data, key, timeout, value, _ref, _ref2,
        _this = this;
      timeout = window.setTimeout((function() {
        return $(_this.el).append(_this.loading = $(_this.template('loading')));
      }), 400);
      if ((_ref = this.view) != null) _ref.undelegateEvents();
      data = {
        'widget': this.id,
        'list': this.bagName,
        'token': this.token
      };
      _ref2 = this.formOptions;
      for (key in _ref2) {
        value = _ref2[key];
        if (key !== 'errorCorrection' && key !== 'pValue') data['filter'] = value;
      }
      return $.ajax({
        url: "" + this.service + "list/chart",
        dataType: "jsonp",
        data: data,
        success: function(response) {
          var _ref3;
          window.clearTimeout(timeout);
          if ((_ref3 = _this.loading) != null) _ref3.remove();
          _this.validateType(response, _this.spec.response);
          if (response.wasSuccessful) {
            _this.name = response.title;
            return _this.view = new ChartView({
              "widget": _this,
              "el": _this.el,
              "template": _this.template,
              "response": response,
              "form": {
                "options": _this.formOptions
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
  
    return ChartWidget;
  
  })(InterMineWidget);
  

  /* Models underpinning Enrichment Widget results.
  */
  var EnrichmentResults, EnrichmentRow,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
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
      "selected": type.isBoolean,
      "externalLink": type.isString
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
      this.collection = new EnrichmentResults();
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
      if (this.response.filterLabel != null) {
        $(this.el).find('div.form form').append(this.template("enrichment.extra", {
          "label": this.response.filterLabel,
          "possible": this.response.filters.split(','),
          "selected": this.response.filterSelectedValue
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
      return $(this.el).find("div.actions").html($(this.template("actions", {
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
        var data, row;
        data = _this.response.results[i];
        if (_this.response.externalLink) {
          data.externalLink = _this.response.externalLink + data.identifier;
        }
        row = new EnrichmentRow(data, _this.widget);
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
          "callbacks": {
            "matchCb": this.options.matchCb,
            "resultsCb": this.options.resultsCb,
            "listCb": this.options.listCb
          },
          "response": this.response
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
      var descriptions, match, matches, model, rowIdentifiers, _i, _j, _len, _len2, _ref, _ref2, _ref3;
      matches = [];
      descriptions = [];
      rowIdentifiers = [];
      _ref = this.collection.selected();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        model = _ref[_i];
        descriptions.push(model.get('description'));
        rowIdentifiers.push(model.get('identifier'));
        _ref2 = model.get('matches');
        for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
          match = _ref2[_j];
          matches.push(match);
        }
      }
      if (matches.length) {
        if ((_ref3 = this.matchesView) != null) _ref3.remove();
        return $(this.el).find('div.actions').after((this.matchesView = new EnrichmentMatchesView({
          "matches": matches,
          "identifiers": rowIdentifiers,
          "description": descriptions.join(', '),
          "template": this.template,
          "style": "width:300px",
          "matchCb": this.options.matchCb,
          "resultsCb": this.options.resultsCb,
          "listCb": this.options.listCb,
          "response": this.response
        })).el);
      }
    };
  
    return EnrichmentView;
  
  })(Backbone.View);
  

  /* Chart Widget bar onclick box.
  */
  var ChartBarView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  ChartBarView = (function(_super) {
  
    __extends(ChartBarView, _super);
  
    function ChartBarView() {
      this.close = __bind(this.close, this);
      this.listAction = __bind(this.listAction, this);
      this.resultsAction = __bind(this.resultsAction, this);
      this.matchAction = __bind(this.matchAction, this);
      this.renderValues = __bind(this.renderValues, this);
      this.render = __bind(this.render, this);
      ChartBarView.__super__.constructor.apply(this, arguments);
    }
  
    ChartBarView.prototype.descriptionLimit = 50;
  
    ChartBarView.prototype.valuesLimit = 5;
  
    ChartBarView.prototype.events = {
      "click a.match": "matchAction",
      "click a.results": "resultsAction",
      "click a.list": "listAction",
      "click a.close": "close"
    };
  
    ChartBarView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      return this.render();
    };
  
    ChartBarView.prototype.render = function() {
      var values,
        _this = this;
      $(this.el).html(this.template("popover", {
        "description": this.description,
        "descriptionLimit": this.descriptionLimit,
        "style": 'width:300px'
      }));
      values = [];
      this.imjs.query(this.quickPq, function(q) {
        return q.rows(function(response) {
          var object, _i, _len;
          for (_i = 0, _len = response.length; _i < _len; _i++) {
            object = response[_i];
            values.push((function(object) {
              var column, _j, _len2;
              for (_j = 0, _len2 = object.length; _j < _len2; _j++) {
                column = object[_j];
                if (column.length > 0) return column;
              }
            })(object));
          }
          return _this.renderValues(values);
        });
      });
      return this;
    };
  
    ChartBarView.prototype.renderValues = function(values) {
      return $(this.el).find('div.values').html(this.template("popover.values", {
        'values': values,
        'type': this.type,
        'valuesLimit': this.valuesLimit
      }));
    };
  
    ChartBarView.prototype.matchAction = function(e) {
      this.matchCb($(e.target).text(), this.type);
      return e.preventDefault();
    };
  
    ChartBarView.prototype.resultsAction = function() {
      return this.resultsCb(this.resultsPq);
    };
  
    ChartBarView.prototype.listAction = function() {
      return this.listCb(this.resultsPq);
    };
  
    ChartBarView.prototype.close = function() {
      return $(this.el).remove();
    };
  
    return ChartBarView;
  
  })(Backbone.View);
  

  /* Table Widget main class.
  */
  var TableWidget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };
  
  TableWidget = (function(_super) {
  
    __extends(TableWidget, _super);
  
    TableWidget.prototype.widgetOptions = {
      "title": true,
      "description": true,
      matchCb: function(id, type) {
        return typeof console !== "undefined" && console !== null ? console.log(id, type) : void 0;
      },
      resultsCb: function(pq) {
        return typeof console !== "undefined" && console !== null ? console.log(pq) : void 0;
      },
      listCb: function(pq) {
        return typeof console !== "undefined" && console !== null ? console.log(pq) : void 0;
      }
    };
  
    TableWidget.prototype.spec = {
      response: {
        "columnTitle": type.isString,
        "title": type.isString,
        "description": type.isString,
        "pathQuery": type.isString,
        "columns": type.isString,
        "pathConstraint": type.isString,
        "requestedAt": type.isString,
        "list": type.isString,
        "type": type.isString,
        "notAnalysed": type.isInteger,
        "results": type.isArray,
        "wasSuccessful": type.isBoolean,
        "error": type.isNull,
        "statusCode": type.isHTTPSuccess
      }
    };
  
    function TableWidget(service, token, id, bagName, el, widgetOptions) {
      this.service = service;
      this.token = token;
      this.id = id;
      this.bagName = bagName;
      this.el = el;
      if (widgetOptions == null) widgetOptions = {};
      this.render = __bind(this.render, this);
      this.widgetOptions = merge(widgetOptions, this.widgetOptions);
      TableWidget.__super__.constructor.call(this);
      this.render();
    }
  
    TableWidget.prototype.render = function() {
      var data, timeout, _ref,
        _this = this;
      timeout = window.setTimeout((function() {
        return $(_this.el).append(_this.loading = $(_this.template('loading')));
      }), 400);
      if ((_ref = this.view) != null) _ref.undelegateEvents();
      data = {
        'widget': this.id,
        'list': this.bagName,
        'token': this.token
      };
      return $.ajax({
        url: "" + this.service + "list/table",
        dataType: "jsonp",
        data: data,
        success: function(response) {
          var _ref2;
          window.clearTimeout(timeout);
          if ((_ref2 = _this.loading) != null) _ref2.remove();
          _this.validateType(response, _this.spec.response);
          if (response.wasSuccessful) {
            _this.name = response.title;
            return _this.view = new TableView({
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
  
    return TableWidget;
  
  })(InterMineWidget);
  

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
      resultsCb: function(pq) {
        return typeof console !== "undefined" && console !== null ? console.log(pq) : void 0;
      },
      listCb: function(pq) {
        return typeof console !== "undefined" && console !== null ? console.log(pq) : void 0;
      }
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
        "wasSuccessful": type.isBoolean,
        "filters": type.isString,
        "filterLabel": type.isString,
        "filterSelectedValue": type.isString,
        "externalLink": type.isString
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
      this.formOptions = {
        errorCorrection: "Holm-Bonferroni",
        pValue: "0.05"
      };
      EnrichmentWidget.__super__.constructor.call(this);
      this.render();
    }
  
    EnrichmentWidget.prototype.render = function() {
      var data, key, timeout, value, _ref, _ref2,
        _this = this;
      timeout = window.setTimeout((function() {
        return $(_this.el).append(_this.loading = $(_this.template('loading')));
      }), 400);
      if ((_ref = this.view) != null) _ref.undelegateEvents();
      data = {
        'widget': this.id,
        'list': this.bagName,
        'correction': this.formOptions.errorCorrection,
        'maxp': this.formOptions.pValue,
        'token': this.token
      };
      _ref2 = this.formOptions;
      for (key in _ref2) {
        value = _ref2[key];
        if (key !== 'errorCorrection' && key !== 'pValue') data['filter'] = value;
      }
      return $.ajax({
        'url': "" + this.service + "list/enrichment",
        'dataType': "jsonp",
        'data': data,
        success: function(response) {
          var _ref3;
          window.clearTimeout(timeout);
          if ((_ref3 = _this.loading) != null) _ref3.remove();
          _this.validateType(response, _this.spec.response);
          if (response.wasSuccessful) {
            _this.name = response.title;
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
    "TableResults": TableResults,
    "TableMatchesView": TableMatchesView,
    "EnrichmentRowView": EnrichmentRowView,
    "TableRowView": TableRowView,
    "EnrichmentMatchesView": EnrichmentMatchesView,
    "TableView": TableView,
    "ChartView": ChartView,
    "ChartWidget": ChartWidget,
    "EnrichmentResults": EnrichmentResults,
    "EnrichmentView": EnrichmentView,
    "ChartBarView": ChartBarView,
    "TableWidget": TableWidget,
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
      path: "http://cdnjs.cloudflare.com/ajax/libs/jquery/1.7.2/jquery.min.js",
      type: "js",
      wait: true
    }, {
      name: "_",
      path: "http://cdnjs.cloudflare.com/ajax/libs/underscore.js/1.3.3/underscore-min.js",
      type: "js",
      wait: true
    }, {
      name: "Backbone",
      path: "http://cdnjs.cloudflare.com/ajax/libs/backbone.js/0.9.2/backbone-min.js",
      type: "js",
      wait: true
    }, {
      name: "google",
      path: "https://www.google.com/jsapi",
      type: "js"
    }, {
      path: "https://raw.github.com/alexkalderimis/imjs/master/src/model.js",
      type: "js"
    }, {
      path: "https://raw.github.com/alexkalderimis/imjs/master/src/query.js",
      type: "js"
    }, {
      path: "https://raw.github.com/alexkalderimis/imjs/master/src/service.js",
      type: "js"
    }
  ];

  function Widgets(service, token) {
    var _this = this;
    this.service = service;
    this.token = token != null ? token : "";
    this.all = __bind(this.all, this);
    this.table = __bind(this.table, this);
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

  Widgets.prototype.table = function() {
    var opts,
      _this = this;
    opts = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
    if (this.wait) {
      return window.setTimeout((function() {
        return _this.table.apply(_this, opts);
      }), 0);
    } else {
      return (function(func, args, ctor) {
        ctor.prototype = func.prototype;
        var child = new ctor, result = func.apply(child, args);
        return typeof result === "object" ? result : child;
      })(o.TableWidget, [this.service, this.token].concat(__slice.call(opts)), function() {});
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
                case "table":
                  _results.push(_this.table(widget.name, bagName, "" + el + " #" + widgetEl, widgetOptions));
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