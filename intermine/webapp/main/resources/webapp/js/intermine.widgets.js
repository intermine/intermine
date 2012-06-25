(function() {
var o = {};

var JST = {};
JST["actions.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<a class="btn btn-small '),this.disabled&&t.push("disabled"),t.push(' view">View</a>\n<a class="btn btn-small '),this.disabled&&t.push("disabled"),t.push(' export">Download</a>')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["error.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<div class="alert alert-block">\n    <h4 class="alert-heading">'),t.push(r(this.title)),t.push(" for "),t.push(r(this.name)),t.push("</h4>\n    <p>"),t.push(this.text),t.push("</p>\n</div>")}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["extra.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){var e,n,i,s;t.push('<div class="group" style="display:inline-block;margin-right:5px;float:left">\n    <label>'),t.push(r(this.label)),t.push("</label>\n    ");if(this.possible.length>1){t.push('\n        <select name="'),t.push(r(this.label)),t.push('" class="span2">\n            '),s=this.possible;for(n=0,i=s.length;n<i;n++)e=s[n],t.push('\n                <option value="'),t.push(r(e)),t.push('" '),this.selected===e&&t.push(r('selected="selected"')),t.push(">\n                    "),t.push(r(e)),t.push("\n                </option>\n            ");t.push("\n        </select>\n    ")}else t.push("\n        "),t.push(r(this.possible[0])),t.push("\n    ");t.push('\n</div>\n<div style="clear:both"></div>')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["noresults.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<div class="alert alert-info">\n    <p>'),t.push(r(this.text||"The Widget has no results.")),t.push("</p>\n</div>")}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["loading.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<div class="loading" style="background:rgba(255,255,255,0.9);position:absolute;top:0;left:0;height:100%;width:100%;text-align:center;">\n    <p style="padding-top:50%;font-weight:bold;">Loading &hellip;</p>\n</div>')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["invalidjsonkey.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<li style="vertical-align:bottom">\n    <span style="display:inline-block" class="label label-important">'),t.push(r(this.key)),t.push("</span> is "),t.push(r(this.actual)),t.push("; was expecting "),t.push(r(this.expected)),t.push("\n</li>")}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["table.table.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){var e,n,i,s,o,u,a;t.push('<!-- actual fixed head -->\n<div class="head" style="display:table">\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;"><input type="checkbox" class="check" /></div>\n    '),u=this.columns;for(n=0,s=u.length;n<s;n++)e=u[n],t.push('\n        <div style="font-weight:bold;display:table-cell;padding:0 8px;">'),t.push(r(e)),t.push("</div>\n    ");t.push('\n    <div style="clear:both"></div>\n</div>\n<div class="wrapper" style="overflow:auto;overflow-x:hidden">\n    <table class="table table-striped">\n        <!-- head for proper cell width -->\n        <thead style="visibility:hidden">\n            <tr>\n                <th></th>\n                '),a=this.columns;for(i=0,o=a.length;i<o;i++)e=a[i],t.push("\n                    <th>"),t.push(r(e)),t.push("</th>\n                ");t.push("\n            </tr>\n        </thead>\n        <tbody>\n            <!-- loop table.row.eco -->\n        </tbody>\n    </table>\n</div>")}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["table.row.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){var e,n,i,s;t.push('<td class="check"><input type="checkbox" '),this.row.selected&&t.push('checked="checked"'),t.push(" /></td>\n"),s=this.row.descriptions;for(n=0,i=s.length;n<i;n++)e=s[n],t.push("\n    <td>"),t.push(r(e)),t.push("</td>\n");t.push('\n<td class="matches">\n    <a class="count" style="cursor:pointer">'),t.push(r(this.row.matches)),t.push("</a>\n</td>")}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["table.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<div class="header">\n    <h3>'),this.title&&t.push(r(this.title)),t.push("</h3>\n    <p>"),this.description&&t.push(this.description),t.push("</p>\n    "),this.notAnalysed&&(t.push("\n        <p>Number of Genes in this list not analysed in this widget: <a>"),t.push(r(this.notAnalysed)),t.push("</a></p>\n    ")),t.push('\n\n    <div class="actions" style="padding:10px 0">\n        <!-- actions.eco -->\n    </div>\n</div>\n<div class="content">\n    <!-- table.table.eco -->\n</div>')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["popover.values.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){var e,n,i,s;t.push("<h4>"),t.push(r(this.values.length)),t.push(" "),t.push(r(this.type)),this.values.length!==1&&t.push(r("s")),t.push(":</h4>\n\n"),s=this.values.slice(0,this.valuesLimit-1+1||9e9);for(n=0,i=s.length;n<i;n++)e=s[n],t.push('\n    <a href="#" class="match">'),t.push(r(e)),t.push("</a>\n");t.push("\n"),this.values.length>this.valuesLimit&&t.push("&hellip;")}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["popover.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<div class="popover" style="position:absolute;top:5px;right:0;z-index:1;display:block">\n    <div class="popover-inner" style="'),t.push(r(this.style)),t.push('">\n        <a style="cursor:pointer;margin:2px 5px 0 0" class="close">×</a>\n        <h3 class="popover-title">\n            '),t.push(r(this.description.slice(0,this.descriptionLimit-1+1||9e9))),t.push("\n            "),this.description.length>this.descriptionLimit&&t.push("&hellip;"),t.push('\n        </h3>\n        <div class="popover-content">\n            <div class="values">\n                <!-- popover.values.eco -->\n            </div>\n            <div style="margin-top:10px">\n                <a class="btn btn-small btn-primary results">View results</a>\n                <a class="btn btn-small list disabled">Create list</a>\n            </div>\n        </div>\n    </div>\n</div>')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["enrichment.form.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){var e,n,i,s,o,u,a,f;t.push('<form style="margin:0">\n    <div class="group" style="display:inline-block;margin-right:5px;float:left">\n        <label>Test Correction</label>\n        <select name="errorCorrection" class="span2">\n            '),a=this.errorCorrections;for(i=0,o=a.length;i<o;i++)e=a[i],t.push('\n                <option value="'),t.push(r(e)),t.push('" '),this.options.errorCorrection===e&&t.push(r('selected="selected"')),t.push(">\n                    "),t.push(r(e)),t.push("\n            </option>\n            ");t.push('\n        </select>\n    </div>\n\n    <div class="group" style="display:inline-block;margin-right:5px;float:left">\n        <label>Max p-value</label>\n        <select name="pValue" class="span2">\n            '),f=this.pValues;for(s=0,u=f.length;s<u;s++)n=f[s],t.push('\n                <option value="'),t.push(r(n)),t.push('" '),this.options.pValue===n&&t.push(r('selected="selected"')),t.push(">\n                    "),t.push(r(n)),t.push("\n                </option>\n            ");t.push('\n        </select>\n    </div>\n</form>\n<div style="clear:both"></div>')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["enrichment.row.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<td class="check"><input type="checkbox" '),this.row.selected&&t.push('checked="checked"'),t.push(' /></td>\n<td class="description">\n    '),t.push(r(this.row.description)),t.push("\n    "),this.row.externalLink&&(t.push('\n        [<a href="'),t.push(this.row.externalLink),t.push('" target="_blank">Link</a>]\n    ')),t.push('\n</td>\n<td class="pValue" style="white-space:nowrap">'),t.push(r(this.row["p-value"])),t.push('</td>\n<td class="matches">\n    <a class="count" style="cursor:pointer">'),t.push(r(this.row.matches)),t.push("</a>\n</td>")}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["enrichment.table.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<!-- actual fixed head -->\n<div class="head" style="display:table">\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;"><input type="checkbox" class="check" /></div>\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;">'),t.push(r(this.label)),t.push('</div>\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;">p-Value</div>\n    <div style="font-weight:bold;display:table-cell;padding:0 8px;">Matches</div>\n    <div style="clear:both"></div>\n</div>\n<div class="wrapper" style="overflow:auto;overflow-x:hidden">\n    <table class="table table-striped">\n        <!-- head for proper cell width -->\n        <thead style="visibility:hidden">\n            <tr>\n                <th></th>\n                <th>'),t.push(r(this.label)),t.push("</th>\n                <th>p-Value</th>\n                <th>Matches</th>\n            </tr>\n        </thead>\n        <tbody>\n            <!-- loop enrichment.row.eco -->\n        </tbody>\n    </table>\n</div>")}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["enrichment.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<div class="header">\n    <h3>'),this.title&&t.push(r(this.title)),t.push("</h3>\n    <p>"),this.description&&t.push(this.description),t.push("</p>\n    "),this.notAnalysed&&(t.push("\n        <p>Number of Genes in this list not analysed in this widget: <a>"),t.push(r(this.notAnalysed)),t.push("</a></p>\n    ")),t.push('\n\n    <div class="form">\n        <!-- enrichment.form.eco -->\n    </div>\n\n    <div class="actions" style="padding:10px 0">\n        <!-- actions.eco -->\n    </div>\n</div>\n<div class="content">\n    <!-- enrichment.table.eco -->\n</div>')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["chart.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<div class="header">\n    <h3>'),this.title&&t.push(r(this.title)),t.push("</h3>\n    <p>"),this.description&&t.push(this.description),t.push("</p>\n    "),this.notAnalysed&&(t.push("\n        <p>Number of Genes in this list not analysed in this widget: <a>"),t.push(r(this.notAnalysed)),t.push("</a></p>\n    ")),t.push('\n\n    <div class="form">\n        <form style="margin:0">\n            <!-- extra.eco -->\n        </form>\n    </div>\n\n    <div class="actions" style="padding:10px 0">\n        <!-- chart.actions.eco -->\n    </div>\n</div>\n<div class="content"></div>')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
JST["chart.actions.eco"]=function(e){e||(e={});var t=[],n=function(e){var n=t,r;return t=[],e.call(this),r=t.join(""),t=n,i(r)},r=function(e){return e&&e.ecoSafe?e:typeof e!="undefined"&&e!=null?o(e):""},i,s=e.safe,o=e.escape;return i=e.safe=function(e){if(e&&e.ecoSafe)return e;if(typeof e=="undefined"||e==null)e="";var t=new String(e);return t.ecoSafe=!0,t},o||(o=e.escape=function(e){return(""+e).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;")}),function(){(function(){t.push('<a class="btn btn-small view-all">View all</a>')}).call(this)}.call(e),e.safe=s,e.escape=o,t.join("")}
/* Types in JS.
*/

var type,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

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
      JSON.parse(actual);
    } catch (e) {
      this.result = false;
    }
  }

  return isJSON;

})(type.Root);

type.isUndefined = (function(_super) {

  __extends(isUndefined, _super);

  function isUndefined() {
    return isUndefined.__super__.constructor.apply(this, arguments);
  }

  isUndefined.prototype.expected = "it to be undefined";

  return isUndefined;

})(type.Root);

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
    if (filename == null) {
      filename = 'widget.tsv';
    }
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

  function PlainExporter(a, data) {
    var w;
    w = window.open();
    if (!(w != null) || typeof w === "undefined") {
      a.after(this.msg = $('<span/>', {
        'style': 'margin-left:5px',
        'class': 'label label-inverse',
        'text': 'Please enable popups'
      }));
    } else {
      w.document.open();
      w.document.write("<pre>" + data + "</pre>");
      w.document.close();
    }
  }

  PlainExporter.prototype.destroy = function() {
    var _ref;
    return (_ref = this.msg) != null ? _ref.fadeOut() : void 0;
  };

  return PlainExporter;

})();

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

/* <IE9 does not have Array::indexOf, use MDC implementation.
*/

if (!Array.prototype.indexOf) {
  Array.prototype.indexOf = function(elt) {
    var from, len;
    len = this.length >>> 0;
    from = Number(arguments[1]) || 0;
    from = (from < 0 ? Math.ceil(from) : Math.floor(from));
    if (from < 0) {
      from += len;
    }
    while (from < len) {
      if (from in this && this[from] === elt) {
        return from;
      }
      from++;
    }
    return -1;
  };
}

var factory;
factory = function(Backbone) {

  /* Parent for all Widgets, handling templating, validation and errors.
  */
  
  var InterMineWidget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  
  InterMineWidget = (function() {
  
    function InterMineWidget() {
      this.error = __bind(this.error, this);
  
      this.validateType = __bind(this.validateType, this);
      $(this.el).html($('<div/>', {
        "class": "inner",
        style: "height:572px;overflow:hidden;position:relative"
      }));
      this.el = "" + this.el + " div.inner";
      this.imService = new intermine.Service({
        'root': this.service,
        'token': this.token
      });
    }
  
    InterMineWidget.prototype.template = function(name, context) {
      var _name;
      if (context == null) {
        context = {};
      }
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
      if (fails.length) {
        return this.error(fails, "JSONResponse");
      }
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
          opts.title = "AJAX Request Failed";
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
  

  /* Chart Widget main class.
  */
  
  var ChartWidget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };
  
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
        "simplePathQuery": type.isString,
        "domainLabel": type.isString,
        "rangeLabel": type.isString
      }
    };
  
    function ChartWidget(service, token, id, bagName, el, widgetOptions) {
      this.service = service;
      this.token = token;
      this.id = id;
      this.bagName = bagName;
      this.el = el;
      if (widgetOptions == null) {
        widgetOptions = {};
      }
      this.render = __bind(this.render, this);
  
      this.widgetOptions = merge(widgetOptions, this.widgetOptions);
      ChartWidget.__super__.constructor.call(this);
      this.render();
    }
  
    ChartWidget.prototype.render = function() {
      var data, key, timeout, value, _ref, _ref1,
        _this = this;
      timeout = window.setTimeout((function() {
        return $(_this.el).append(_this.loading = $(_this.template('loading')));
      }), 400);
      if ((_ref = this.view) != null) {
        _ref.undelegateEvents();
      }
      data = {
        'widget': this.id,
        'list': this.bagName,
        'token': this.token
      };
      _ref1 = this.formOptions;
      for (key in _ref1) {
        value = _ref1[key];
        if (key !== 'errorCorrection' && key !== 'pValue') {
          data['filter'] = value;
        }
      }
      return $.ajax({
        url: "" + this.service + "list/chart",
        dataType: "jsonp",
        data: data,
        success: function(response) {
          var _ref2;
          window.clearTimeout(timeout);
          if ((_ref2 = _this.loading) != null) {
            _ref2.remove();
          }
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
        error: function(request, status, error) {
          clearTimeout(timeout);
          return _this.error({
            'text': "" + _this.service + "list/chart"
          }, "AJAXTransport");
        }
      });
    };
  
    return ChartWidget;
  
  })(InterMineWidget);
  

  /* Table Widget main class.
  */
  
  var TableWidget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };
  
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
      if (widgetOptions == null) {
        widgetOptions = {};
      }
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
      if ((_ref = this.view) != null) {
        _ref.undelegateEvents();
      }
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
          var _ref1;
          window.clearTimeout(timeout);
          if ((_ref1 = _this.loading) != null) {
            _ref1.remove();
          }
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
        error: function(request, status, error) {
          clearTimeout(timeout);
          return _this.error({
            'text': "" + _this.service + "list/table"
          }, "AJAXTransport");
        }
      });
    };
  
    return TableWidget;
  
  })(InterMineWidget);
  

  /* Enrichment Widget main class.
  */
  
  var EnrichmentWidget,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };
  
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
        "type": type.isString,
        "wasSuccessful": type.isBoolean,
        "filters": type.isString,
        "filterLabel": type.isString,
        "filterSelectedValue": type.isString,
        "externalLink": type.isString,
        "pathQueryForMatches": type.isString
      }
    };
  
    function EnrichmentWidget(service, token, id, bagName, el, widgetOptions) {
      this.service = service;
      this.token = token;
      this.id = id;
      this.bagName = bagName;
      this.el = el;
      if (widgetOptions == null) {
        widgetOptions = {};
      }
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
      var data, key, timeout, value, _ref, _ref1,
        _this = this;
      timeout = window.setTimeout((function() {
        return $(_this.el).append(_this.loading = $(_this.template('loading')));
      }), 400);
      if ((_ref = this.view) != null) {
        _ref.undelegateEvents();
      }
      data = {
        'widget': this.id,
        'list': this.bagName,
        'correction': this.formOptions.errorCorrection,
        'maxp': this.formOptions.pValue,
        'token': this.token
      };
      _ref1 = this.formOptions;
      for (key in _ref1) {
        value = _ref1[key];
        if (key !== 'errorCorrection' && key !== 'pValue') {
          data['filter'] = value;
        }
      }
      return $.ajax({
        'url': "" + this.service + "list/enrichment",
        'dataType': "jsonp",
        'data': data,
        success: function(response) {
          var _ref2;
          window.clearTimeout(timeout);
          if ((_ref2 = _this.loading) != null) {
            _ref2.remove();
          }
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
        error: function(request, status, error) {
          clearTimeout(timeout);
          return _this.error({
            'text': "" + _this.service + "list/enrichment"
          }, "AJAXTransport");
        }
      });
    };
  
    return EnrichmentWidget;
  
  })(InterMineWidget);
  

  /* Core Model for Enrichment and Table Models.
  */
  
  var CoreCollection, CoreModel, EnrichmentResults, EnrichmentRow, TableResults, TableRow,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };
  
  CoreModel = (function(_super) {
  
    __extends(CoreModel, _super);
  
    function CoreModel() {
      this.toggleSelected = __bind(this.toggleSelected, this);
  
      this.validate = __bind(this.validate, this);
      return CoreModel.__super__.constructor.apply(this, arguments);
    }
  
    CoreModel.prototype.defaults = {
      "selected": false
    };
  
    CoreModel.prototype.initialize = function(row, widget) {
      this.widget = widget;
      return this.validate(row);
    };
  
    CoreModel.prototype.validate = function(row) {
      return this.widget.validateType(row, this.spec);
    };
  
    CoreModel.prototype.toggleSelected = function() {
      return this.set({
        selected: !this.get("selected")
      });
    };
  
    return CoreModel;
  
  })(Backbone.Model);
  
  CoreCollection = (function(_super) {
  
    __extends(CoreCollection, _super);
  
    function CoreCollection() {
      return CoreCollection.__super__.constructor.apply(this, arguments);
    }
  
    CoreCollection.prototype.model = CoreModel;
  
    CoreCollection.prototype.selected = function() {
      return this.filter(function(row) {
        return row.get("selected");
      });
    };
  
    CoreCollection.prototype.toggleSelected = function() {
      var model, _i, _j, _len, _len1, _ref, _ref1, _results, _results1;
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
        _ref1 = this.models;
        _results1 = [];
        for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
          model = _ref1[_j];
          _results1.push(model.set({
            "selected": false
          }, {
            'silent': true
          }));
        }
        return _results1;
      }
    };
  
    return CoreCollection;
  
  })(Backbone.Collection);
  
  /* Models underpinning Enrichment Widget results.
  */
  
  
  EnrichmentRow = (function(_super) {
  
    __extends(EnrichmentRow, _super);
  
    function EnrichmentRow() {
      return EnrichmentRow.__super__.constructor.apply(this, arguments);
    }
  
    EnrichmentRow.prototype.spec = {
      "description": type.isString,
      "identifier": type.isString,
      "matches": type.isInteger,
      "p-value": type.isInteger,
      "selected": type.isBoolean,
      "externalLink": type.isString
    };
  
    EnrichmentRow.prototype.toJSON = function() {
      var attributes;
      attributes = _.clone(this.attributes);
      if (attributes['p-value'] < 0.001) {
        attributes['p-value'] = attributes['p-value'].toExponential(6);
      } else {
        attributes['p-value'] = attributes['p-value'].toFixed(6);
      }
      return attributes;
    };
  
    return EnrichmentRow;
  
  })(CoreModel);
  
  EnrichmentResults = (function(_super) {
  
    __extends(EnrichmentResults, _super);
  
    function EnrichmentResults() {
      return EnrichmentResults.__super__.constructor.apply(this, arguments);
    }
  
    EnrichmentResults.prototype.model = EnrichmentRow;
  
    return EnrichmentResults;
  
  })(CoreCollection);
  
  /* Models underpinning Table Widget results.
  */
  
  
  TableRow = (function(_super) {
  
    __extends(TableRow, _super);
  
    function TableRow() {
      return TableRow.__super__.constructor.apply(this, arguments);
    }
  
    TableRow.prototype.spec = {
      "matches": type.isInteger,
      "identifier": type.isInteger,
      "descriptions": type.isArray,
      "selected": type.isBoolean
    };
  
    return TableRow;
  
  })(CoreModel);
  
  TableResults = (function(_super) {
  
    __extends(TableResults, _super);
  
    function TableResults() {
      return TableResults.__super__.constructor.apply(this, arguments);
    }
  
    TableResults.prototype.model = TableRow;
  
    return TableResults;
  
  })(CoreCollection);
  

  /* Table Widget table row matches box.
  */
  
  var TablePopoverView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };
  
  TablePopoverView = (function(_super) {
  
    __extends(TablePopoverView, _super);
  
    function TablePopoverView() {
      this.toggle = __bind(this.toggle, this);
  
      this.adjustPopover = __bind(this.adjustPopover, this);
  
      this.listAction = __bind(this.listAction, this);
  
      this.resultsAction = __bind(this.resultsAction, this);
  
      this.matchAction = __bind(this.matchAction, this);
  
      this.renderValues = __bind(this.renderValues, this);
  
      this.render = __bind(this.render, this);
      return TablePopoverView.__super__.constructor.apply(this, arguments);
    }
  
    TablePopoverView.prototype.descriptionLimit = 50;
  
    TablePopoverView.prototype.valuesLimit = 5;
  
    TablePopoverView.prototype.events = {
      "click a.match": "matchAction",
      "click a.results": "resultsAction",
      "click a.list": "listAction",
      "click a.close": "toggle"
    };
  
    TablePopoverView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      return this.render();
    };
  
    TablePopoverView.prototype.render = function() {
      var values,
        _this = this;
      $(this.el).css({
        'position': 'relative'
      });
      $(this.el).html(this.template("popover", {
        "description": this.description,
        "descriptionLimit": this.descriptionLimit,
        "style": this.style || "width:300px;margin-left:-300px"
      }));
      this.pathQuery = JSON.parse(this.pathQuery);
      this.pathQuery.where.push({
        "path": this.pathConstraint,
        "op": "ONE OF",
        "values": this.identifiers
      });
      values = [];
      this.imService.query(this.pathQuery, function(q) {
        return q.rows(function(response) {
          var object, _i, _len;
          for (_i = 0, _len = response.length; _i < _len; _i++) {
            object = response[_i];
            values.push((function(object) {
              var column, _j, _len1;
              for (_j = 0, _len1 = object.length; _j < _len1; _j++) {
                column = object[_j];
                if (column.length > 0) {
                  return column;
                }
              }
            })(object));
          }
          _this.renderValues(values);
          return _this.adjustPopover();
        });
      });
      return this;
    };
  
    TablePopoverView.prototype.renderValues = function(values) {
      return $(this.el).find('div.values').html(this.template("popover.values", {
        'values': values,
        'type': this.type,
        'valuesLimit': this.valuesLimit
      }));
    };
  
    TablePopoverView.prototype.matchAction = function(e) {
      this.matchCb($(e.target).text(), this.type);
      return e.preventDefault();
    };
  
    TablePopoverView.prototype.resultsAction = function() {
      return this.resultsCb(this.pathQuery);
    };
  
    TablePopoverView.prototype.listAction = function() {
      return this.listCb(this.pathQuery);
    };
  
    TablePopoverView.prototype.adjustPopover = function() {
      var _this = this;
      return window.setTimeout((function() {
        var diff, head, header, parent, popover, table, widget;
        table = $(_this.el).closest('div.wrapper');
        popover = $(_this.el).find('.popover');
        parent = popover.closest('td.matches');
        if (!parent.length) {
          return;
        }
        widget = parent.closest('div.inner');
        header = widget.find('div.header');
        head = widget.find('div.content div.head');
        diff = ((parent.position().top - header.height() + head.height()) + 30 + popover.outerHeight()) - table.height();
        if (diff > 0) {
          return popover.css('top', -diff);
        }
      }), 0);
    };
  
    TablePopoverView.prototype.toggle = function() {
      $(this.el).toggle();
      return this.adjustPopover();
    };
  
    return TablePopoverView;
  
  })(Backbone.View);
  

  /* Chart Widget bar onclick box.
  */
  
  var ChartPopoverView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };
  
  ChartPopoverView = (function(_super) {
  
    __extends(ChartPopoverView, _super);
  
    function ChartPopoverView() {
      this.close = __bind(this.close, this);
  
      this.listAction = __bind(this.listAction, this);
  
      this.resultsAction = __bind(this.resultsAction, this);
  
      this.matchAction = __bind(this.matchAction, this);
  
      this.renderValues = __bind(this.renderValues, this);
  
      this.render = __bind(this.render, this);
      return ChartPopoverView.__super__.constructor.apply(this, arguments);
    }
  
    ChartPopoverView.prototype.descriptionLimit = 50;
  
    ChartPopoverView.prototype.valuesLimit = 5;
  
    ChartPopoverView.prototype.events = {
      "click a.match": "matchAction",
      "click a.results": "resultsAction",
      "click a.list": "listAction",
      "click a.close": "close"
    };
  
    ChartPopoverView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      return this.render();
    };
  
    ChartPopoverView.prototype.render = function() {
      var values,
        _this = this;
      $(this.el).html(this.template("popover", {
        "description": this.description,
        "descriptionLimit": this.descriptionLimit,
        "style": 'width:300px'
      }));
      values = [];
      this.imService.query(this.quickPq, function(q) {
        return q.rows(function(response) {
          var object, _i, _len;
          for (_i = 0, _len = response.length; _i < _len; _i++) {
            object = response[_i];
            values.push((function(object) {
              var column, _j, _len1;
              for (_j = 0, _len1 = object.length; _j < _len1; _j++) {
                column = object[_j];
                if (column.length > 0) {
                  return column;
                }
              }
            })(object));
          }
          return _this.renderValues(values);
        });
      });
      return this;
    };
  
    ChartPopoverView.prototype.renderValues = function(values) {
      return $(this.el).find('div.values').html(this.template("popover.values", {
        'values': values,
        'type': this.type,
        'valuesLimit': this.valuesLimit
      }));
    };
  
    ChartPopoverView.prototype.matchAction = function(e) {
      this.matchCb($(e.target).text(), this.type);
      return e.preventDefault();
    };
  
    ChartPopoverView.prototype.resultsAction = function() {
      return this.resultsCb(this.resultsPq);
    };
  
    ChartPopoverView.prototype.listAction = function() {
      return this.listCb(this.resultsPq);
    };
  
    ChartPopoverView.prototype.close = function() {
      return $(this.el).remove();
    };
  
    return ChartPopoverView;
  
  })(Backbone.View);
  

  /* Enrichment Widget table row.
  */
  
  var EnrichmentRowView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };
  
  EnrichmentRowView = (function(_super) {
  
    __extends(EnrichmentRowView, _super);
  
    function EnrichmentRowView() {
      this.toggleMatchesAction = __bind(this.toggleMatchesAction, this);
  
      this.selectAction = __bind(this.selectAction, this);
  
      this.render = __bind(this.render, this);
      return EnrichmentRowView.__super__.constructor.apply(this, arguments);
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
      this.model.toggleSelected();
      if (this.popoverView != null) {
        $(this.el).find('td.matches a.count').after(this.popoverView.el);
        return this.popoverView.delegateEvents();
      }
    };
  
    EnrichmentRowView.prototype.toggleMatchesAction = function() {
      if (!(this.popoverView != null)) {
        return $(this.el).find('td.matches a.count').after((this.popoverView = new EnrichmentPopoverView({
          "matches": this.model.get("matches"),
          "identifiers": [this.model.get("identifier")],
          "description": this.model.get("description"),
          "template": this.template,
          "matchCb": this.callbacks.matchCb,
          "resultsCb": this.callbacks.resultsCb,
          "listCb": this.callbacks.listCb,
          "response": this.response,
          "imService": this.imService
        })).el);
      } else {
        return this.popoverView.toggle();
      }
    };
  
    return EnrichmentRowView;
  
  })(Backbone.View);
  

  /* View maintaining Enrichment Widget.
  */
  
  var EnrichmentView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };
  
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
      return EnrichmentView.__super__.constructor.apply(this, arguments);
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
      $(this.el).html(this.template("enrichment", {
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
        $(this.el).find('div.form form').append(this.template("extra", {
          "label": this.response.filterLabel,
          "possible": this.response.filters.split(','),
          "selected": this.response.filterSelectedValue
        }));
      }
      if (this.response.results.length > 0) {
        this.renderToolbar();
        this.renderTable();
      } else {
        $(this.el).find("div.content").html($(this.template("noresults", {
          'text': 'No enrichment found.'
        })));
      }
      return this;
    };
  
    EnrichmentView.prototype.renderToolbar = function() {
      return $(this.el).find("div.actions").html($(this.template("actions", {
        "disabled": this.collection.selected().length === 0
      })));
    };
  
    EnrichmentView.prototype.renderTable = function() {
      var height, i, table, _fn, _i, _ref,
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
      for (i = _i = 0, _ref = this.response.results.length; 0 <= _ref ? _i < _ref : _i > _ref; i = 0 <= _ref ? ++_i : --_i) {
        _fn(i);
      }
      this.renderTableBody(table);
      height = $(this.el).height() - $(this.el).find('div.header').height() - $(this.el).find('div.content div.head').height();
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
          "response": this.response,
          "imService": this.widget.imService
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
      var model, pq, rowIdentifiers, _i, _len, _ref,
        _this = this;
      rowIdentifiers = [];
      _ref = this.collection.selected();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        model = _ref[_i];
        rowIdentifiers.push(model.get('identifier'));
      }
      pq = JSON.parse(this.response['pathQueryForMatches']);
      pq.where.push({
        "path": this.response.pathConstraint,
        "op": "ONE OF",
        "values": rowIdentifiers
      });
      return this.widget.imService.query(pq, function(q) {
        return q.rows(function(response) {
          var dict, ex, object, result, _j, _k, _len1, _len2, _ref1;
          dict = {};
          for (_j = 0, _len1 = response.length; _j < _len1; _j++) {
            object = response[_j];
            if (!(dict[object[0]] != null)) {
              dict[object[0]] = [];
            }
            dict[object[0]].push(object[1]);
          }
          result = [];
          _ref1 = _this.collection.selected();
          for (_k = 0, _len2 = _ref1.length; _k < _len2; _k++) {
            model = _ref1[_k];
            result.push([model.get('description'), model.get('p-value')].join("\t") + "\t" + dict[model.get('identifier')].join(','));
          }
          if (result.length) {
            ex = new PlainExporter($(e.target), result.join("\n"));
            return window.setTimeout((function() {
              return ex.destroy();
            }), 5000);
          }
        });
      });
    };
  
    EnrichmentView.prototype.viewAction = function() {
      var descriptions, model, rowIdentifiers, _i, _len, _ref, _ref1;
      descriptions = [];
      rowIdentifiers = [];
      _ref = this.collection.selected();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        model = _ref[_i];
        descriptions.push(model.get('description'));
        rowIdentifiers.push(model.get('identifier'));
      }
      if (rowIdentifiers.length) {
        if ((_ref1 = this.popoverView) != null) {
          _ref1.remove();
        }
        return $(this.el).find('div.actions').after((this.popoverView = new EnrichmentPopoverView({
          "identifiers": rowIdentifiers,
          "description": descriptions.join(', '),
          "template": this.template,
          "style": "width:300px",
          "matchCb": this.options.matchCb,
          "resultsCb": this.options.resultsCb,
          "listCb": this.options.listCb,
          "response": this.response,
          "imService": this.widget.imService
        })).el);
      }
    };
  
    return EnrichmentView;
  
  })(Backbone.View);
  

  /* View maintaining Table Widget.
  */
  
  var TableView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };
  
  TableView = (function(_super) {
  
    __extends(TableView, _super);
  
    function TableView() {
      this.viewAction = __bind(this.viewAction, this);
  
      this.exportAction = __bind(this.exportAction, this);
  
      this.selectAllAction = __bind(this.selectAllAction, this);
  
      this.renderTableBody = __bind(this.renderTableBody, this);
  
      this.renderTable = __bind(this.renderTable, this);
  
      this.renderToolbar = __bind(this.renderToolbar, this);
      return TableView.__super__.constructor.apply(this, arguments);
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
      $(this.el).html(this.template("table", {
        "title": this.options.title ? this.response.title : "",
        "description": this.options.description ? this.response.description : "",
        "notAnalysed": this.response.notAnalysed
      }));
      if (this.response.results.length > 0) {
        this.renderToolbar();
        this.renderTable();
      } else {
        $(this.el).find("div.content").html($(this.template("noresults", {
          'text': "No \"" + this.response.title + "\" with your list."
        })));
      }
      return this;
    };
  
    TableView.prototype.renderToolbar = function() {
      return $(this.el).find("div.actions").html($(this.template("actions", {
        "disabled": this.collection.selected().length === 0
      })));
    };
  
    TableView.prototype.renderTable = function() {
      var height, i, table, _fn, _i, _ref,
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
      for (i = _i = 0, _ref = this.response.results.length; 0 <= _ref ? _i < _ref : _i > _ref; i = 0 <= _ref ? ++_i : --_i) {
        _fn(i);
      }
      this.renderTableBody(table);
      height = $(this.el).height() - $(this.el).find('div.header').height() - $(this.el).find('div.content div.head').height();
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
          "response": this.response,
          "matchCb": this.options.matchCb,
          "resultsCb": this.options.resultsCb,
          "listCb": this.options.listCb,
          "imService": this.widget.imService
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
        ex = new PlainExporter($(e.target), result.join("\n"));
        return window.setTimeout((function() {
          return ex.destroy();
        }), 5000);
      }
    };
  
    TableView.prototype.viewAction = function() {
      var descriptions, model, rowIdentifiers, _i, _len, _ref, _ref1;
      descriptions = [];
      rowIdentifiers = [];
      _ref = this.collection.selected();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        model = _ref[_i];
        descriptions.push(model.get('descriptions')[0]);
        rowIdentifiers.push(model.get('identifier'));
      }
      if (rowIdentifiers.length) {
        if ((_ref1 = this.popoverView) != null) {
          _ref1.remove();
        }
        return $(this.el).find('div.actions').after((this.popoverView = new TablePopoverView({
          "identifiers": rowIdentifiers,
          "description": descriptions.join(', '),
          "template": this.template,
          "matchCb": this.options.matchCb,
          "resultsCb": this.options.resultsCb,
          "listCb": this.options.listCb,
          "pathQuery": this.response.pathQuery,
          "pathConstraint": this.response.pathConstraint,
          "imService": this.widget.imService,
          "type": this.response.type,
          "style": 'width:300px'
        })).el);
      }
    };
  
    return TableView;
  
  })(Backbone.View);
  

  /* Table Widget table row.
  */
  
  var TableRowView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };
  
  TableRowView = (function(_super) {
  
    __extends(TableRowView, _super);
  
    function TableRowView() {
      this.toggleMatchesAction = __bind(this.toggleMatchesAction, this);
  
      this.selectAction = __bind(this.selectAction, this);
  
      this.render = __bind(this.render, this);
      return TableRowView.__super__.constructor.apply(this, arguments);
    }
  
    TableRowView.prototype.tagName = "tr";
  
    TableRowView.prototype.events = {
      "click td.check input": "selectAction",
      "click td.matches a.count": "toggleMatchesAction"
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
      this.model.toggleSelected();
      if (this.popoverView != null) {
        $(this.el).find('td.matches a.count').after(this.popoverView.el);
        return this.popoverView.delegateEvents();
      }
    };
  
    TableRowView.prototype.toggleMatchesAction = function() {
      if (!(this.popoverView != null)) {
        return $(this.el).find('td.matches a.count').after((this.popoverView = new TablePopoverView({
          "identifiers": [this.model.get("identifier")],
          "description": this.model.get("descriptions").join(', '),
          "template": this.template,
          "matchCb": this.matchCb,
          "resultsCb": this.resultsCb,
          "listCb": this.listCb,
          "pathQuery": this.response.pathQuery,
          "pathConstraint": this.response.pathConstraint,
          "imService": this.imService,
          "type": this.response.type
        })).el);
      } else {
        return this.popoverView.toggle();
      }
    };
  
    return TableRowView;
  
  })(Backbone.View);
  

  /* View maintaining Chart Widget.
  */
  
  var ChartView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };
  
  ChartView = (function(_super) {
  
    __extends(ChartView, _super);
  
    function ChartView() {
      this.formAction = __bind(this.formAction, this);
  
      this.viewSeriesAction = __bind(this.viewSeriesAction, this);
  
      this.viewAllAction = __bind(this.viewAllAction, this);
  
      this.renderToolbar = __bind(this.renderToolbar, this);
      return ChartView.__super__.constructor.apply(this, arguments);
    }
  
    ChartView.prototype.chartOptions = {
      fontName: "Sans-Serif",
      fontSize: 11,
      colors: ["#2F72FF", "#9FC0FF"],
      legend: {
        position: "top"
      },
      chartArea: {
        top: 30,
        bottom: 80,
        left: 50
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
      "change div.form select": "formAction",
      "click div.actions a.view-all": "viewAllAction"
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
      var chart, height, width,
        _this = this;
      $(this.el).html(this.template("chart", {
        "title": this.options.title ? this.response.title : "",
        "description": this.options.description ? this.response.description : "",
        "notAnalysed": this.response.notAnalysed
      }));
      if (this.response.filterLabel != null) {
        $(this.el).find('div.form form').append(this.template("extra", {
          "label": this.response.filterLabel,
          "possible": this.response.filters.split(','),
          "selected": this.response.filterSelectedValue
        }));
      }
      if (this.response.results.length > 1) {
        if (this.response.chartType in google.visualization) {
          this.renderToolbar();
          width = $(this.el).width();
          height = $(this.el).height() - $(this.el).find('div.header').height();
          this.chartOptions.width = width;
          this.chartOptions.chartArea.width = width - this.chartOptions.chartArea.left;
          this.chartOptions.height = height;
          this.chartOptions.chartArea.height = height - this.chartOptions.chartArea.top - this.chartOptions.chartArea.bottom;
          this.chartOptions.hAxis = {
            'title': this.response.chartType === 'BarChart' ? this.response.rangeLabel : this.response.domainLabel
          };
          this.chartOptions.vAxis = {
            'title': this.response.chartType === 'BarChart' ? this.response.domainLabel : this.response.rangeLabel
          };
          chart = new google.visualization[this.response.chartType]($(this.el).find("div.content")[0]);
          chart.draw(google.visualization.arrayToDataTable(this.response.results, false), this.chartOptions);
          if (this.response.pathQuery != null) {
            return google.visualization.events.addListener(chart, "select", function() {
              var column, description, quickPq, resultsPq, row, selection, translate;
              translate = function(response, series) {
                if (response.seriesValues != null) {
                  return response.seriesValues.split(',')[response.seriesLabels.split(',').indexOf(series)];
                }
              };
              selection = chart.getSelection()[0];
              description = '';
              resultsPq = _this.response.pathQuery;
              quickPq = _this.response.simplePathQuery;
              if (selection.row != null) {
                row = _this.response.results[selection.row + 1][0];
                description += row;
                resultsPq = resultsPq.replace("%category", row);
                quickPq = quickPq.replace("%category");
                if (selection.column != null) {
                  column = _this.response.results[0][selection.column];
                  description += ' ' + column;
                  resultsPq = resultsPq.replace("%series", translate(_this.response, column));
                  quickPq = resultsPq.replace("%series", translate(_this.response, column));
                }
              } else {
                if (selection.column != null) {
                  return _this.viewSeriesAction(resultsPq.replace("%series", translate(_this.response, _this.response.results[0][selection.column])));
                }
              }
              resultsPq = JSON.parse(resultsPq);
              quickPq = JSON.parse(quickPq);
              if (_this.barView != null) {
                _this.barView.close();
              }
              if (description) {
                return $(_this.el).find('div.content').append((_this.barView = new ChartPopoverView({
                  "description": description,
                  "template": _this.template,
                  "resultsPq": resultsPq,
                  "resultsCb": _this.options.resultsCb,
                  "listCb": _this.options.listCb,
                  "matchCb": _this.options.matchCb,
                  "quickPq": quickPq,
                  "imService": _this.widget.imService,
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
        return $(this.el).find("div.content").html($(this.template("noresults", {
          'text': "No \"" + this.response.title + "\" with your list."
        })));
      }
    };
  
    ChartView.prototype.renderToolbar = function() {
      return $(this.el).find("div.actions").html($(this.template("chart.actions")));
    };
  
    ChartView.prototype.viewAllAction = function() {
      var field, i, pq, rem, _i, _len, _ref, _ref1;
      pq = JSON.parse(this.response.pathQuery);
      _ref = ['%category', '%series'];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        rem = _ref[_i];
        _ref1 = pq.where;
        for (i in _ref1) {
          field = _ref1[i];
          if ((field != null ? field.value : void 0) === rem) {
            pq.where.splice(i, 1);
            break;
          }
        }
      }
      return this.options.resultsCb(pq);
    };
  
    ChartView.prototype.viewSeriesAction = function(pathQuery) {
      var field, i, pq, _ref;
      pq = JSON.parse(pathQuery);
      _ref = pq.where;
      for (i in _ref) {
        field = _ref[i];
        if ((field != null ? field.value : void 0) === '%category') {
          pq.where.splice(i, 1);
          break;
        }
      }
      return this.options.resultsCb(pq);
    };
  
    ChartView.prototype.formAction = function(e) {
      this.widget.formOptions[$(e.target).attr("name")] = $(e.target[e.target.selectedIndex]).attr("value");
      return this.widget.render();
    };
  
    return ChartView;
  
  })(Backbone.View);
  

  /* Enrichment Widget table row matches box.
  */
  
  var EnrichmentPopoverView,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    __indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };
  
  EnrichmentPopoverView = (function(_super) {
  
    __extends(EnrichmentPopoverView, _super);
  
    function EnrichmentPopoverView() {
      this.listAction = __bind(this.listAction, this);
  
      this.resultsAction = __bind(this.resultsAction, this);
  
      this.matchAction = __bind(this.matchAction, this);
  
      this.getPq = __bind(this.getPq, this);
  
      this.toggle = __bind(this.toggle, this);
  
      this.adjustPopover = __bind(this.adjustPopover, this);
  
      this.renderValues = __bind(this.renderValues, this);
  
      this.render = __bind(this.render, this);
      return EnrichmentPopoverView.__super__.constructor.apply(this, arguments);
    }
  
    EnrichmentPopoverView.prototype.descriptionLimit = 50;
  
    EnrichmentPopoverView.prototype.valuesLimit = 5;
  
    EnrichmentPopoverView.prototype.events = {
      "click a.match": "matchAction",
      "click a.results": "resultsAction",
      "click a.list": "listAction",
      "click a.close": "toggle"
    };
  
    EnrichmentPopoverView.prototype.initialize = function(o) {
      var k, v;
      for (k in o) {
        v = o[k];
        this[k] = v;
      }
      return this.render();
    };
  
    EnrichmentPopoverView.prototype.render = function() {
      var pq, values,
        _this = this;
      $(this.el).css({
        'position': 'relative'
      });
      $(this.el).html(this.template("popover", {
        "description": this.description,
        "descriptionLimit": this.descriptionLimit,
        "style": this.style || "width:300px;margin-left:-300px"
      }));
      pq = JSON.parse(this.response['pathQueryForMatches']);
      pq.where.push({
        "path": this.response.pathConstraint,
        "op": "ONE OF",
        "values": this.identifiers
      });
      values = [];
      this.imService.query(pq, function(q) {
        return q.rows(function(response) {
          var object, value, _i, _len;
          for (_i = 0, _len = response.length; _i < _len; _i++) {
            object = response[_i];
            value = (function(object) {
              var column, _j, _len1, _ref;
              _ref = object.reverse();
              for (_j = 0, _len1 = _ref.length; _j < _len1; _j++) {
                column = _ref[_j];
                if (column.length > 0) {
                  return column;
                }
              }
            })(object);
            if (__indexOf.call(values, value) < 0) {
              values.push(value);
            }
          }
          _this.renderValues(values);
          return _this.adjustPopover();
        });
      });
      return this;
    };
  
    EnrichmentPopoverView.prototype.renderValues = function(values) {
      return $(this.el).find('div.values').html(this.template("popover.values", {
        'values': values,
        'type': this.response.type,
        'valuesLimit': this.valuesLimit
      }));
    };
  
    EnrichmentPopoverView.prototype.adjustPopover = function() {
      var _this = this;
      return window.setTimeout((function() {
        var diff, head, header, parent, popover, table, widget;
        table = $(_this.el).closest('div.wrapper');
        popover = $(_this.el).find('.popover');
        parent = popover.closest('td.matches');
        if (!parent.length) {
          return;
        }
        widget = parent.closest('div.inner');
        header = widget.find('div.header');
        head = widget.find('div.content div.head');
        diff = ((parent.position().top - header.height() + head.height()) + 30 + popover.outerHeight()) - table.height();
        if (diff > 0) {
          return popover.css('top', -diff);
        }
      }), 0);
    };
  
    EnrichmentPopoverView.prototype.toggle = function() {
      $(this.el).toggle();
      return this.adjustPopover();
    };
  
    EnrichmentPopoverView.prototype.getPq = function() {
      var pq;
      pq = this.response.pathQuery;
      this.pq = JSON.parse(pq);
      return this.pq.where.push({
        "path": this.response.pathConstraint,
        "op": "ONE OF",
        "values": this.identifiers
      });
    };
  
    EnrichmentPopoverView.prototype.matchAction = function(e) {
      this.matchCb($(e.target).text(), this.response.type);
      return e.preventDefault();
    };
  
    EnrichmentPopoverView.prototype.resultsAction = function() {
      if (this.pq == null) {
        this.getPq();
      }
      return this.resultsCb(this.pq);
    };
  
    EnrichmentPopoverView.prototype.listAction = function() {
      if (this.pq == null) {
        this.getPq();
      }
      return this.listCb(this.pq);
    };
  
    return EnrichmentPopoverView;
  
  })(Backbone.View);
  

  return {

    "InterMineWidget": InterMineWidget,
    "ChartWidget": ChartWidget,
    "TableWidget": TableWidget,
    "EnrichmentWidget": EnrichmentWidget,
    "CoreModel": CoreModel,
    "TablePopoverView": TablePopoverView,
    "ChartPopoverView": ChartPopoverView,
    "EnrichmentRowView": EnrichmentRowView,
    "EnrichmentView": EnrichmentView,
    "TableView": TableView,
    "TableRowView": TableRowView,
    "ChartView": ChartView,
    "EnrichmentPopoverView": EnrichmentPopoverView
  };
};
/* Interface to InterMine Widgets.
*/

var $, Widgets,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
  __slice = [].slice,
  __indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

$ = window.jQuery || window.Zepto;

Widgets = (function() {

  Widgets.prototype.VERSION = '1.3.0';

  Widgets.prototype.wait = true;

  Widgets.prototype.resources = [
    {
      name: 'JSON',
      path: 'http://cdn.intermine.org/js/json3/3.2.2/json3.min.js',
      type: 'js'
    }, {
      name: "jQuery",
      path: "http://cdn.intermine.org/js/jquery/1.7.2/jquery.min.js",
      type: "js",
      wait: true
    }, {
      name: "_",
      path: "http://cdn.intermine.org/js/underscore.js/1.3.3/underscore-min.js",
      type: "js",
      wait: true
    }, {
      name: "Backbone",
      path: "http://cdn.intermine.org/js/backbone.js/0.9.2/backbone-min.js",
      type: "js",
      wait: true
    }, {
      name: "google",
      path: "https://www.google.com/jsapi",
      type: "js"
    }, {
      path: "http://cdn.intermine.org/js/intermine/imjs/latest/imjs.js",
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

    intermine.load(this.resources, function() {
      $ = window.jQuery;
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
            var child = new ctor, result = func.apply(child, args), t = typeof result;
            return t == "object" || t == "function" ? result || child : child;
          })(o.ChartWidget, [_this.service, _this.token].concat(__slice.call(opts)), function(){});
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
        var child = new ctor, result = func.apply(child, args), t = typeof result;
        return t == "object" || t == "function" ? result || child : child;
      })(o.EnrichmentWidget, [this.service, this.token].concat(__slice.call(opts)), function(){});
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
        var child = new ctor, result = func.apply(child, args), t = typeof result;
        return t == "object" || t == "function" ? result || child : child;
      })(o.TableWidget, [this.service, this.token].concat(__slice.call(opts)), function(){});
    }
  };

  Widgets.prototype.all = function(type, bagName, el, widgetOptions) {
    var _this = this;
    if (type == null) {
      type = "Gene";
    }
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
              if (!(__indexOf.call(widget.targets, type) >= 0)) {
                continue;
              }
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

if (!window.intermine) {
  throw 'You need to include the InterMine API Loader first!';
} else {
  window.intermine.widgets = Widgets;
}

}).call(this);