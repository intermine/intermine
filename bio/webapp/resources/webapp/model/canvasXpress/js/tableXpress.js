/*
* TableXpress 0.1 - JavaScript Table Library
*
* Copyright (c) 2013 Isaac Neuhaus
*
* imnphd@gmail.com
*
*
* Redistributions of this source code must retain this copyright
* notice and the following disclaimer.
*
* TableXpress is licensed under the terms of the Open Source
* LGPL 3.0 license.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
* Commercial use is permitted to the extent that this source code
* do NOT become part of any other Open Source or Commercially licensed
* development library or toolkit without explicit permission.
*
*
*/

/* Changes
*
*/

/// End Changes ///

if( typeof (TableXpress) == 'undefined') {
  TableXpress = {};
}

var TableXpress = function(target, data, config, events) {

  /*
   * Shortcut functions
   */
  this.$ = function(id) {
    return document.getElementById(id);
  }
  this.$cX = function(t, p, s) {
    var e = document.createElement(t);
    if(p) {
      for(var i in p) {
        e[i] = p[i];
      }
    }
    if(s) {
      for(var i in s) {
        e.style[i] = s[i];
      }
    }
    return e;
  }
  /*
   * Check if there is a target
   */
  if(!target) {
    target = this.createNewTarget();
  } else if( typeof (target) == 'object') {
    data = target.data || false;
    config = target.config || false;
    events = target.events || false;
    target = target.renderTo || this.createNewTarget();
  }
  /*
   * Some global variables
   */
  this.setInit = function() {
    this.version = 0.1;
    this.target = target;
    this.events = events;
    this.userId = 1;
    this.startTime = new Date().getTime();
  }
  /*
   * Validate the parameters
   */
  this.validateParameters = function() {
    this.validateData();
    this.validateConfig();
    this.validateEvents();
  }
  /*
   * Data
   */
  this.validateData = function() {
    if(data && !this.subBrowser) {
      try {
        JSON.stringify(data);
      } catch (e) {
        alert('Data object malformed:\n' + e);
      }
    }
  }
  /*
   * Config
   */
  this.validateConfig = function() {
    this.addConfigLocation();
    if(!config) {
      config = {};
    } else if(!this.subBrowser) {
      try {
        JSON.stringify(config);
      } catch (e) {
        alert('Config object malformed:\n' + e);
      }
    }
    this.userConfig = config;
  }
  /*
   * Events
   */
  this.validateEvents = function() {
    //if (events && !this.isIE) {
    //try {
    //  JSON.stringify(events);
    //} catch (e) {
    //  alert('Events object malformed:\n' + e);
    //}
    //}
  }
  /*
   * Parameters passed in the window.location.href
   */
  this.addConfigLocation = function() {
    var params = window.location.href.split(/&/);
    if(params && params.length > 0) {
      for(var i = 0; i < params.length; i++) {
        var json = params[i].match(/tableXpress=(\{.+\})/);
        if(json && json.length > 1) {
          var str = decodeURIComponent(JSON.stringify(json[1]).replace(/^\"/, '').replace(/\"$/, ''));
          var par;
          try {
            par = JSON.parse(str);
          } catch (e) {
            alert('Location parameters malformed:\n' + e);
          }
          if(par) {
            if(!config) {
              config = {};
            }
            for(var p in par) {
              if(!config[p]) {
                config[p] = par[p]
              }
            }
          }
        }
      }
    }
  }
  /*
   * New id
   */
  this.newId = function(t) {
    var n = 0;
    var i = this.target + t + n;
    var e = this.$(i);
    while(e) {
      n++;
      i = this.target + t + n;
      e = this.$(i);
    }
    return i;
  }
  /*
   * Create target in document
   */
  this.createNewTarget = function() {
    var c = this.$cX('div', {
      id : this.newId('tableXpress')
    });
    document.body.appendChild(c);
    return c.id;
  }
  /*
   * Remove target from document
   */
  this.removeTarget = function(t) {
    var n = this.$(t);
    if(n) {
      n.parentNode.removeChild(n);
    }
  }
  /*
   * Save the data object
   */
  this.save = function() {
    return {
      renderTo : this.target,
      data : this.data,
      config : this.getConfig(),
      events : this.events
    }
  }
  /*
   * Export 2D array to excel
   */
  this.exportToExcel = function(d) {
    var str = '';
    for(var i = 0; i < d.length; i++) {
      str += d[i].join('%09') + '%0D';
    }
    return window.open('data:text/tab-separeted-values,' + str);
  }
  /*
   * Export 2D array to excel
   */
  this.exporttoHTML = function(d) {
    var str = '<table>';
    for(var i = 0; i < d.length; i++) {
      str += '<tr><td>';
      str += d[i].join("</td><td>");
      str += '</td></tr>';
    }
    str += '</table>';
    return window.open().document.write(str);
  }
  /*
   * A function to produce pretty JSON
   */
  this.prettyJSON = function(o, str) {
    var realTypeOf = function(v) {
      if( typeof (v) == "object") {
        if(v === null) {
          return "null";
        }
        if(v.constructor == (new Array).constructor) {
          return "array";
        }
        if(v.constructor == (new Date).constructor) {
          return "date";
        }
        if(v.constructor == (new RegExp).constructor) {
          return "regex";
        }
        return "object";
      }
      return typeof (v);
    }
    if(!str) {
      str = "";
    }
    var json;
    var sty = "  ";
    var sdt = realTypeOf(o);
    var cnt = 0;
    if(sdt == "array") {
      if(o.length == 0) {
        return "[]";
      }
      json = "[";
    } else {
      for(var i in o) {
        cnt++;
        break;
      }
      if(cnt == 0) {
        return "{}";
      }
      json = "{";
    }
    cnt = 0;
    for(var k in o) {
      v = o[k];
      if(cnt > 0) {
        json += ",";
      }
      if(sdt == "array") {
        json += ("\n" + str + sty);
      } else {
        json += ("\n" + str + sty + "\"" + k + "\"" + ": ");
      }
      switch (realTypeOf(v)) {
        case "array":
        case "object":
          json += this.prettyJSON(v, (str + sty));
          break;
        case "boolean":
        case "number":
          json += v.toString();
          break;
        case "null":
          json += "null";
          break;
        case "string":
          json += ("\"" + v + "\"");
          break;
        default:
        //json += ("TYPEOF: " + typeof (v));
      }
      cnt++;
    }
    if(sdt == "array") {
      json += ("\n" + str + "]");
    } else {
      json += ("\n" + str + "}");
    }
    return json;
  }
  /*
   * Dump object to console
   */
  this.dumpToConsole = function(o) {
    console.log(this.target);
    console.log(this.prettyJSON(o));
  }
  /*
   * Initialize TableXpress
   * @object
   */
  this.initialize = function() {
    /*
     * Add some css
     */
    this.initCSS();
    /*
     * Initialize initialization parameters
     */
    this.setInit();
    /*
     * Initialize Browser related stuff
     */
    this.initCrossBrowser();
    /*
     * Validate parameters
     */
    this.validateParameters();
    /*
     * Initialize Configurations
     */
    this.initConfig(config);
    /*
     * Initialize the example data
     */
    this.initExample();
    /*
     * Initialize the data
     */
    this.initData(data);
    /*
     * Initialize Events
     */
    this.initEvents();
    /*
     * Initialize the graph type
     */
    this.initTable();
  }
  /*
   * Initialize
   */
  this.initialize();

  /*
   * Save a reference for this instance
   */
  TableXpress.references.push(this);

}

/*
 * An array with all tables to facilitate
 * communication within them
 * TO DO: Broadcast 'ala' Resolver
 */
TableXpress.references = [];

/*
 * Method to get the TableXpress object
 */
TableXpress.getObject = function(id) {
  for(var i = 0; i < TableXpress.references.length; i++) {
    if(TableXpress.references[i].target == id) {
      return TableXpress.references[i];
    }
  }
}

/*
 * Detect Browser and set Cross-Browser events
 */
TableXpress.prototype.initCrossBrowser = function() {

  //  =========================== 
  //  = Class related functions = 
  //  ===========================

  /*
   * Check for classes
   */
  this.hasClass = function(e, n) {
    return new RegExp('(\\s|^)' + n + '(\\s|$)').test(e.className);
  }

  /*
   * Add a class
   */
  this.addClass = function(e, n) {
    if(!hasClass(e, n)) {
      e.className += (e.className ? ' ' : '') + n;
    }
  }

  /*
   * Remove a class
   */
  this.removeClass = function(e, n) {
    if(hasClass(e, n)) {
      e.className = e.className.replace(new RegExp('(\\s|^)' + n + '(\\s|$)'), ' ').replace(/^\s+|\s+$/g, '');
    }
  }

  //  =========================== 
  //  = Event related functions = 
  //  ===========================

  /*
   * Cancel event (normalized across browsers)
   */
  this.cancelEvent = function(e) {
    if(!e) {
      e = window.event;
    }
    if(e.preventDefault) {
      e.preventDefault();
    } else {
      e.returnValue = false;
    }
  }
  
  /*
   * Stop event (normalized across browsers)
   */
  this.stopEvent = function(e) {
    if(!e) {
      e = window.event;
    }
    if(e.stopPropagation) {
      e.stopPropagation();
    } else {
      e.cancelBubble = true;
    }
  }
  
  /*
   * Normalize event name across browsers
   */
  this.normalizeEvtName = function(e) {
    return this.isIE ? 'on' + e : e;
  }
  
  /*
   * Add event (normalized across browsers)
   *
   * this.registeredEvents
   *
   */
  this.addEvtListener = function(o, e, c, f) {
    if(o && e && c) {
      if(!this.registeredEvents) {
        this.registeredEvents = {};
      }
      if(o.id) {
        if(!this.registeredEvents[o.id]) {
          this.registeredEvents[o.id] = {};
        }
        this.registeredEvents[o.id][e] = [c, f];
      } else {
        if(!this.registeredEvents[o]) {
          this.registeredEvents[o] = {};
        }
        this.registeredEvents[o][e] = [c, f];
      }
      if(this.isIE) {
        o.attachEvent(this.normalizeEvtName(e), c);
      } else {
        o.addEventListener(e, c, f);
        if(e == 'mousewheel') {
          o.addEventListener('DOMMouseScroll', c, f);
        }
      }
    }
  }
  
  /*
   * Remove event (normalized across browsers)
   */
  this.removeEvtListener = function(o, e, c, f) {
    if(o && e && c) {
      var k = this.registeredEvents[o.id || o];
      if(k && k.hasOwnProperty(e)) {
        delete k[e];
        if(this.isIE) {
          o.detachEvent(this.normalizeEvtName(e), c);
        } else {
          o.removeEventListener(e, c, f);
          if(e == 'mousewheel') {
            o.removeEventListener('DOMMouseScroll', c, f);
          }
        }
        var v = this.getKeys(this.registeredEvents[o.id || o]);
        if(v && v.length < 1) {
          delete this.registeredEvents[o.id || o];
        }
      }
    }
  }
  
  /*
   * Generic function to add or remove event listeners
   */
  this.addRemoveEvtListener = function(t, o, e, c, f) {
    if(t && o && e && c) {
      this[t](o, e, c, f);
    }
  }
  
  /*
   * Purge all events listeners
   */
  this.purgeEventListeners = function() {
    for(var i in this.registeredEvents) {
      var o = this.$(i) || i;
      for(var e in this.registeredEvents[i]) {
        this.removeEvtListener(o, e, this.registeredEvents[i][0], this.registeredEvents[i][1]);
      }
    }
  }
  
  /*
   * Prevent selection
   */
  this.preventSelection = function() {
    if(document.selection) {
      document.selection.empty();
    } else if(window.getSelection) {
      window.getSelection().removeAllRanges();
    }
  }
  
  /*
   * Get the target element on an events
   */
  this.getTargetEvent = function(e) {
    if(this.isIE && this.useFlashIE && this.browserVersion < 9) {
      return e.srcElement.parentNode;
    } else {
      return e.target || e.srcElement;
    }
  }

  //  ============================= 
  //  = Browser related functions = 
  //  =============================

  /*
   * Type of browser hash
   */
  this.dataBrowser = [{
    string : navigator.platform,
    subString : "iPhone",
    identity : "iPhone"
  }, {
    string : navigator.platform,
    subString : "iPod",
    identity : "iPod"
  }, {
    string : navigator.userAgent,
    subString : "iPad",
    identity : "iPad"
  }, {
    string : navigator.userAgent,
    subString : "Android",
    identity : "Android"
  }, {
    string : navigator.userAgent,
    subString : "BlackBerry",
    identity : "BlackBerry"
  }, {
    string : navigator.userAgent,
    subString : "Chrome",
    identity : "Chrome"
  }, {
    string : navigator.userAgent,
    subString : "OmniWeb",
    versionSearch : "OmniWeb/",
    identity : "OmniWeb"
  }, {
    string : navigator.vendor,
    subString : "Apple",
    identity : "Safari",
    versionSearch : "Version"
  }, {
    prop : window.opera,
    identity : "Opera"
  }, {
    string : navigator.vendor,
    subString : "iCab",
    identity : "iCab"
  }, {
    string : navigator.vendor,
    subString : "KDE",
    identity : "Konqueror"
  }, {
    string : navigator.userAgent,
    subString : "Firefox",
    identity : "Firefox"
  }, {
    string : navigator.vendor,
    subString : "Camino",
    identity : "Camino"
  }, {
    string : navigator.userAgent,
    subString : "Netscape",
    identity : "Netscape"
  }, {
    string : navigator.userAgent,
    subString : "MSIE",
    identity : "Explorer",
    versionSearch : "MSIE"
  }, {
    string : navigator.userAgent,
    subString : "Gecko",
    identity : "Mozilla",
    versionSearch : "rv"
  }, {
    string : navigator.userAgent,
    subString : "Mozilla",
    identity : "Netscape",
    versionSearch : "Mozilla"
  }];

  /*
   * Type of operating system hash
   */
  this.dataOS = [{
    string : navigator.platform,
    subString : "Win",
    identity : "Windows"
  }, {
    string : navigator.platform,
    subString : "Mac",
    identity : "Mac"
  }, {
    string : navigator.platform,
    subString : "iPhone",
    identity : "iPhone"
  }, {
    string : navigator.platform,
    subString : "iPod",
    identity : "iPod"
  }, {
    string : navigator.userAgent,
    subString : "iPad",
    identity : "iPad"
  }, {
    string : navigator.userAgent,
    subString : "Android",
    identity : "Android"
  }, {
    string : navigator.userAgent,
    subString : "BlackBerry",
    identity : "BlackBerry"
  }, {
    string : navigator.platform,
    subString : "Linux",
    identity : "Linux"
  }];

  /*
   * Mobile devices
   */
  this.isMobileApp = function() {
    if(this.browser.match(/iPhone|iPod|iPad|Android|BlackBerry/i)) {
      return true;
    } else {
      return false;
    }
  }

  /*
   * Search function
   */
  this.searchString = function(data) {
    for(var i = 0; i < data.length; i++) {
      var dataString = data[i].string;
      var dataProp = data[i].prop;
      this.versionSearchString = data[i].versionSearch || data[i].identity;
      if(dataString) {
        if(dataString.indexOf(data[i].subString) != -1) {
          return data[i].identity;
        }
      } else if(dataProp) {
        return data[i].identity;
      }
    }
  }

  /*
   * Version
   */
  this.searchVersion = function(dataString) {
    var index = dataString.indexOf(this.versionSearchString);
    if(index == -1) {
      return;
    }
    return parseFloat(dataString.substring(index + this.versionSearchString.length + 1));
  }

  /*
   * Initialization function
   */
  this.initializeBrowser = function() {
    this.browser = this.searchString(this.dataBrowser) || "An unknown browser";
    this.browserVersion = this.searchVersion(navigator.userAgent) || this.searchVersion(navigator.appVersion) || "an unknown version";
    this.browserOS = this.searchString(this.dataOS) || "an unknown OS";
    if(this.browser == 'Explorer') {
      this.isIE = true;
      this.mobileApp = false;
    } else {
      this.isIE = false;
      this.mobileApp = this.isMobileApp();
    }
  }

  /*
   * Initialize functions
   */
  this.initializeBrowser();

}

/*
 * Add CSS
 */

TableXpress.prototype.initCSS = function() {

  var s = ' ';
  s += ' div.CanvasXpressTooltip{border:1px solid rgb(113,139,183);border-radius:5px;background-color:rgb(240,240,240);cursor:move;color:rgb(34,34,34);font:normal 11px arial,tahoma,sans-serif;margin:2px 3px;overflow:hidden;padding:2px 3px;position:absolute;white-space:nowrap;}';
  s += ' img.CanvasXpressTooltip{cursor:default;float:right;height:13px;margin:2px 3px 0px 0px;width:13px;cursor:default;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' span.CanvasXpressTooltip{border:1px solid rgb(113,139,183);border-radius:5px;background-color:rgb(240,240,240);cursor:move;color:rgb(34,34,34);font:normal 11px arial,tahoma,sans-serif;margin:2px 3px;overflow:hidden;padding:2px 3px;position:absolute;white-space:nowrap;}';
  s += ' div.CanvasXpressDataTable{border:1px solid rgb(113,139,183);border-radius:5px;background-color:rgb(240,240,240);clear:both;color:rgb(34,34,34);font:normal 11px arial,tahoma,sans-serif;overflow:hidden;position:absolute;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' div.CanvasXpressDataTableToolbar{border:0px;border-radius:5px 5px 0px 0px;background-color:rgb(240,240,240);color:rgb(34,34,34);cursor:move;font:normal 11px arial,tahoma,sans-serif;height:18px;left:0px;line-height:100%;position:absolute;top:0px;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' img.CanvasXpressDataTableToolbarImage{cursor:default;float:right;height:13px;margin:2px 3px 0px 0px;width:13px;cursor:default;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' div.CanvasXpressDataTableContainer{background-color:rgb(240,240,240);border-radius:0px 0px 5px 5px;color:rgb(34,34,34);display:block;font:normal 11px arial,tahoma,sans-serif;left:0px;line-height:100%;position:absolute;top:20px;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' div.CanvasXpressDataTableVertical{background-color:rgb(240,240,240);overflow:auto;position:absolute;right:2px;top:2px;width:18px;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' div.CanvasXpressDataTableHorizontal{background-color:rgb(240,240,240);bottom:2px;height:18px;left:2px;overflow:auto;position:absolute;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' div.CanvasXpressDataTableResizer{background-color:rgb(240,240,240);background-image:url(http://www.canvasxpress.org/images/resize_w.png);background-repeat:no-repeat;cursor:se-resize;bottom:1px;height:15px;position:absolute;right:1px;width:15px;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' table.CanvasXpressDataTable{border:0px;border-spacing:1px;color:rgb(34,34,34);font:normal 11px arial,tahoma,sans-serif;left:1px;line-height:20px;position:absolute;table-layout:fixed;top:1px;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' div.CanvasXpressDataTableMask{border:0px;left:1px;position:absolute;top:1px;overflow:hidden;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' th.CanvasXpressTableCellHead{background-image:url(http://www.canvasxpress.org/images/accordion.png);background-repeat:repeat-x;border-left:1px solid #ffffff;border-top:1px solid #ffffff;color:rgb(34,34,34);font:normal 11px arial,tahoma,sans-serif;line-height:20px;padding:0px;text-align:left;vertical-align:middle;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' th.CanvasXpressTableCellHeadActive{background-image:url(http://www.canvasxpress.org/images/accordion_active.png);background-repeat:repeat-x;border-left:1px solid #ffffff;border-top:1px solid #ffffff;color:rgb(34,34,34);font:normal 11px arial,tahoma,sans-serif;line-height:20px;padding:0px;text-align:left;vertical-align:middle;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' td.CanvasXpressTableCell{background-color:#ffffff;border:0px;color:rgb(34,34,34);font:normal 11px arial,tahoma,sans-serif;line-height:20px;padding:0px;vertical-align:middle;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' td.CanvasXpressTableCellActive{background-color:#dfe8f9;border:0px;color:rgb(34,34,34);font:normal 11px arial,tahoma,sans-serif;line-height:20px;padding:0px;vertical-align:middle;-moz-user-select:none;-khtml-user-select:none;-webkit-user-select:none;-o-user-select:none;-user-select:none;}';
  s += ' div.CanvasXpressTableCell{font:normal 11px arial,tahoma,sans-serif;line-height:20px;margin:2px 2px 1px 5px;overflow:hidden;white-space:nowrap;}';
  s += ' div.CanvasXpressTableCellSortAsc{background:url(http://www.canvasxpress.org/images/sort_asc.gif) no-repeat right 6px;font:normal 11px arial,tahoma,sans-serif;line-height:20px;margin:2px 2px 1px 5px;overflow:hidden;white-space:nowrap;}';
  s += ' div.CanvasXpressTableCellSortDesc{background:url(http://www.canvasxpress.org/images/sort_desc.gif) no-repeat right 6px;font:normal 11px arial,tahoma,sans-serif;line-height:20px;margin:2px 2px 1px 5px;overflow:hidden;white-space:nowrap;}';
  var c = document.createElement("style");
  c.type = "text/css";
  if(c.styleSheet) {
    c.styleSheet.cssText = s;
  } else {
    c.appendChild(document.createTextNode(s));
  }
  document.getElementsByTagName("head")[0].appendChild(c);

}

TableXpress.prototype.initConfig = function() {

}

TableXpress.prototype.initExample = function() {

}

TableXpress.prototype.initData = function() {

}

TableXpress.prototype.initEvents = function() {

}

TableXpress.prototype.initTable = function() {
  /******************
   * Data Table HTML
   ******************/
  this.addDataTableDiv = function(o, n) {
    if (this.$(this.target + '-cX-DataTable')) {
      return;
    }
    var that = this;
    var addCell = function(r, i, j) {
      var c = that.$cX(i == 0 || j == 0 ? 'th' : 'td', {
        id : that.target + '-cX-DataTableCell.' + i + '.' + j,
        className : i == 0 || j == 0 ? 'CanvasXpressTableCellHead' : 'CanvasXpressTableCell'
      }, {
        width : that.colWidth + 'px',
        height : that.rowHeight + 'px'
      });
      r.appendChild(c);
    }
    if (!n) {
      this.setDataTableDimensions(o);
    }
    var aph = 20;
    var apv = 44;
    var lsx = this.dataTableLastX != null ? parseInt(this.dataTableLastX) : this.canvas.width * 0.1;
    var lsy = this.dataTableLastY != null ? parseInt(this.dataTableLastY) : -this.canvas.height * 0.9;
    var lsw = this.dataTableLastWidth || this.dataTableColsWidth + aph;
    var lsh = this.dataTableLastHeight || this.dataTableRowsHeight + apv;
    var mcw = ((this.colWidth + 8) * 3) + 6;
    var mrh = ((this.rowHeight + 4) * 3) + 8;
    // Main div
    var d = this.$cX('div', {
      id : this.target + '-cX-DataTable',
      className : 'CanvasXpressDataTable draggable-container'
    }, {
      top : lsy + 'px',
      left : lsx + 'px',
      width : lsw + 'px',
      height : lsh + 'px',
      minWidth : (mcw + aph) + 'px',
      minHeight : (mrh + apv + 20) + 'px',
      zIndex : 10000,
      display : 'none'
    });
    // Toolbar div
    var dtt = this.$cX('div', {
      id : this.target + '-cX-DataTableToolbar',
      className : 'CanvasXpressDataTableToolbar draggable'
    }, {
      width : lsw + 'px',
      minWidth : (mcw + aph) + 'px'
    });
    // Save image
    var is = this.$cX('img', {
      id : this.target + '-cX-DataTableSaveImage',
      className : 'CanvasXpressDataTableToolbarImage',
      src : this.imageDir + 'disk.png',
      alt : 'Save data',
      title : 'Save data'
    });
    // Transpose image
    var ix = this.$cX('img', {
      id : this.target + '-cX-DataTableTransposeImage',
      className : 'CanvasXpressDataTableToolbarImage',
      src : this.imageDir + 'transpose.png',
      alt : 'Transpose data',
      title : 'Transpose data'
    });
    // Network image
    var it = this.$cX('img', {
      id : this.target + '-cX-DataTableNetworkImage',
      className : 'CanvasXpressDataTableToolbarImage',
      src : this.networkShowDataTable == 'nodes' ? this.imageDir + 'edges.png' : this.imageDir + 'nodes.png',
      alt : this.networkShowDataTable == 'nodes' ? 'Show edge data' : 'Show node data',
      title : this.networkShowDataTable == 'nodes' ? 'Show edge data' : 'Show node data'
    }, {
      display : this.graphType == 'Network' ? 'block' : 'none'
    });
    // Dock image
    var ii = this.$cX('img', {
      id : this.target + '-cX-DataTableDockImage',
      className : 'CanvasXpressDataTableToolbarImage',
      src : this.dataTableLastState && this.dataTableLastState == 'docked' ? this.imageDir + 'unpin.png' : this.imageDir + 'pin.png',
      alt : this.dataTableLastState && this.dataTableLastState == 'docked' ? 'Undock' : 'Dock',
      title : this.dataTableLastState && this.dataTableLastState == 'docked' ? 'Undock' : 'Dock'
    });
    // Close image
    var ic = this.$cX('img', {
      id : this.target + '-cX-DataTableCloseImage',
      className : 'CanvasXpressDataTableToolbarImage',
      src : this.imageDir + 'cancel1.png',
      alt : 'Close table',
      title : 'Close table'
    });
    // Container for the rest of the data table
    var dtc = this.$cX('div', {
      id : this.target + '-cX-DataTableContainer',
      className : 'CanvasXpressDataTableContainer'
    }, {
      width : lsw + 'px',
      height : (lsh - 20) + 'px',
      minWidth : (mcw + aph) + 'px',
      minHeight : (mrh + apv) + 'px'
    });
    // Table Mask
    var tm = this.$cX('div', {
      id : this.target + '-cX-DataTableTableMask',
      className : 'CanvasXpressDataTableMask'
    }, {
      width : (lsw - aph) + 'px',
      height : (lsh - apv) + 'px',
      minWidth : mcw + 'px',
      minHeight : (mrh + 20) + 'px'
    });
    // Table
    var t = this.$cX('table', {
      id : this.target + '-cX-DataTableTable',
      className : 'CanvasXpressDataTable'
    });
    var b = this.$cX('tbody');
    for (var i = 0; i < this.maxRows; i++) {
      var r = this.$cX('tr');
      for (var j = 0; j < this.maxCols; j++) {
        addCell(r, i, j);
      }
      b.appendChild(r);
    }
    // Vertical Scroller
    var dv = this.$cX('div', {
      id : this.target + '-cX-DataTableVer',
      className : 'CanvasXpressDataTableVertical'
    }, {
      height : (lsh - apv) + 'px',
      minHeight : (mrh + 20) + 'px'
    });
    // Vertical image
    var iv = this.$cX('img', {
      id : this.target + '-cX-DataTableVerImage',
      src : this.getPixelImage(),
      width : 1,
      height : this.dataTableTotalHeight
    });
    dv.appendChild(iv);
    // Horizontal Scroller
    var dh = this.$cX('div', {
      id : this.target + '-cX-DataTableHor',
      className : 'CanvasXpressDataTableHorizontal'
    }, {
      width : (lsw - aph) + 'px',
      minWidth : mcw + 'px'
    });
    // Horizontal image
    var ih = this.$cX('img', {
      id : this.target + '-cX-DataTableHorImage',
      src : this.getPixelImage(),
      width : this.dataTableTotalWidth,
      height : 1
    });
    dh.appendChild(ih);
    // Resizer
    var dtr = this.$cX('div', {
      id : this.target + '-cX-DataTableResizer',
      className : 'CanvasXpressDataTableResizer resizable'
    });
    // Pack the toolbar with the images
    dtt.appendChild(ic);
    dtt.appendChild(ii);
    dtt.appendChild(ix);
    dtt.appendChild(is);
    dtt.appendChild(it);
    // Pack the table div with the tbody and the table
    t.appendChild(b);
    tm.appendChild(t);
    // Pack the container
    dtc.appendChild(tm);
    dtc.appendChild(dv);
    dtc.appendChild(dh);
    dtc.appendChild(dtr);
    // Pack the main div with the toolbar and the container
    d.appendChild(dtt);
    d.appendChild(dtc);
    var w = this.$('south-wrapper-' + this.target);
    if (w) {
      w.appendChild(d);
      this.addRemoveDataTableListeners('addEvtListener');
    }
  }
  /***********************
   * Data Table listeners
   ***********************/
  this.addRemoveDataTableListeners = function(ty, hd) {
    this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableHor'), "scroll", this.scrollTable, false);
    this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableVer'), "scroll", this.scrollTable, false);
    this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableToolbar'), 'mousedown', this.registerMousemove, false);
    this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableSaveImage'), "click", this.saveTable, false);
    this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableNetworkImage'), "click", this.networkUpdateTable, false);
    this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableTransposeImage'), "click", this.transposeDataTable, false);
    this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableDockImage'), "click", this.dockUndockTable, false);
    this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableCloseImage'), "click", this.hideTable, false);
    this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableResizer'), "mousedown", this.mousedownDataTableResizer, false);
    var r = hd ? hd[0] : this.maxRows;
    var c = hd ? hd[1] : this.maxCols;
    for (var i = 0; i < c; i++) {
      this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableCell.0.' + i), "click", this.clickDataTableHeader, false);
      this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableCell.0.' + i), "mousemove", this.mousemoveDataTableHeader, false);
      this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableCell.0.' + i), "mousedown", this.mousedownDataTableHeader, false);
      this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableCell.0.' + i), "mouseover", this.mouseoverDataTableHeader, false);
      this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableCell.0.' + i), "mouseout", this.mouseoutDataTableHeader, false);
    }
    for (var i = 0; i < r; i++) {
      this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableCell.' + i + '.0'), "mouseover", this.mouseoverDataTableHeader, false);
      this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableCell.' + i + '.0'), "mouseout", this.mouseoutDataTableHeader, false);
    }
    for (var i = 1; i < r; i++) {
      for (var j = 1; j < c; j++) {
        this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableCell.' + i + '.' + j), "mouseover", this.mouseoverDataTableCell, false);
        this.addRemoveEvtListener(ty, this.$(this.target + '-cX-DataTableCell.' + i + '.' + j), "mouseout", this.mouseoutDataTableCell, false);
      }
    }
  }
  /********************
   * Data Table Events
   ********************/
  /*
   * scroller event
   *
   * this.dataTableLastScrollLeft
   * this.dataTableLastScrollTop
   * this.dataTableLastScrollWidth
   * this.dataTableLastScrollHeight
   */
  this.scrollTable = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      var d = t.$(t.target + '-cX-DataTable');
      var v = t.$(t.target + '-cX-DataTableVer');
      var h = t.$(t.target + '-cX-DataTableHor');
      if (d && v && h && !t.resizingDataTableOn) {
        // Scroll Horizontally
        var sc = Math.ceil(h.scrollLeft / (h.scrollWidth / t.totalCols));
        // Scroll vertically
        var sr = Math.ceil(v.scrollTop / (v.scrollHeight / t.totalRows));
        // Keep track of the scrollers
        t.dataTableLastScrollLeft = h.scrollLeft;
        t.dataTableLastScrollTop = v.scrollTop;
        t.dataTableLastScrollWidth = h.scrollWidth;
        t.dataTableLastScrollHeight = v.scrollHeight;
        // Do the update
        if (sc != t.startCol || sr != t.startRow) {
          t.startCol = sc;
          t.startRow = sr;
          t.updateDataTable(false, false, false, true);
        }
      }
      return false;
    }
  }(this);
  /*
   * Save the table
   */
  this.saveTable = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      var mxc = t.maxCols;
      var mxr = t.maxRows;
      var stc = t.startCol;
      var str = t.startRow;
      t.maxCols = t.totalCols;
      t.maxRows = t.totalRows;
      t.startCol = 0;
      t.startRow = 0;
      var d = t.updateDataTable(false, false, true);
      t.exportToExcel(d);
      t.maxCols = mxc;
      t.maxRows = mxr;
      t.startCol = stc;
      t.startRow = str;
      return false;
    }
  }(this);
  /*
   * Dock the table
   */
  this.dockUndockTable = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      var s = t.dataTableLastState && t.dataTableLastState == 'docked' ? 'undock' : 'dock';
      var trg = t.getTargetEvent(e);
      if (s == 'dock') {
        trg.src = t.imageDir + 'unpin.png';
        trg.alt = 'Undock';
        trg.title = 'Undock';
      } else {
        trg.src = t.imageDir + 'pin.png';
        trg.alt = 'Dock';
        trg.title = 'Dock';
      }
      t.moveDataTableDiv(s);
      return false;
    }
  }(this);
  /*
   * Dock the table
   */
  this.transposeDataTable = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      t.dataTableTransposed = t.dataTableTransposed ? false : true;
      t.sortDataTableHead = false;
      t.updateDataTable(false, true, false, false, true);
      t.resizeDataTable();
      return false;
    }
  }(this);
  /*
   * minimize the data table
   */
  this.minimizeTable = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      t.moveDataTableDiv('min');
      return false;
    }
  }(this);
  /*
   * maximize the data table
   */
  this.maximizeTable = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      t.moveDataTableDiv('max');
      return false;
    }
  }(this);
  /*
   * hide the table
   */
  this.hideTable = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      var d = t.$(t.target + '-cX-DataTable');
      if (d) {
        t.showDataTable = false;
        t.moveDataTableDiv('hide');
        setTimeout(function() {
          t.hideUnhideDataTable(true);
          t.resetFade(d);
          t.resetDataTable(true);
        }, 500);
      }
      return false;
    }
  }(this);
  /*
   * change the type of network data in the table
   */
  this.networkUpdateTable = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      var trg = t.getTargetEvent(e);
      t.networkShowDataTable = t.networkShowDataTable == 'nodes' ? 'edges' : 'nodes';
      t.updateDataTable(false, true, false, true);
      return false;
    }
  }(this);
  /*
   * mousedown data table resizer
   *
   * this.resizingDataTableOn
   */
  this.mousedownDataTableResizer = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      var trg = e.target || e.srcElement;
      if (t.hasClass(trg, 'resizable')) {
        var c = t.$(t.target + '-cX-DataTable');
        if (c) {
          t.dataTableTarget = c;
          t.xMouseDown = e.clientX;
          t.yMouseDown = e.clientY;
          t.dataTableWidth = parseInt(t.dataTableTarget.style.width);
          t.dataTableHeight = parseInt(t.dataTableTarget.style.height);
          t.resizingDataTableOn = true;
        }
      }
      return false;
    }
  }(this);
  /*
   * mousemove data table header
   */
  this.mousemoveDataTableHeader = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      var trg = t.resizingDataTableColumnOn ? t.dataTableColumnTarget : t.getTargetEvent(e);
      var ac = t.adjustedCoordinates(e, trg);
      var hs = t.$(t.target + '-cX-DataTableHorImage');
      if (ac && hs) {
        if (t.resizingDataTableColumnOn) {
          // Get the real Row and Column from the div inside the th
          var ids = t.dataTableColumnTarget.id.split('.');
          var col = parseInt(ids[2]);
          var diff = ac.x - t.xMouseDown;
          var w = Math.max(t.colWidth, (t.dataTableColumnWidth[t.startCol + col] || t.colWidth) + diff);
          t.dataTableColumnTarget.firstChild.style.width = w + 'px';
          hs.width += diff;
          // Resize the rest of the rows
          for (var i = t.startRow; i < t.startRow + t.totalRows; i++) {
            var th = t.$(t.target + '-cX-DataTableCell.' + i + '.' + col);
            if (th) {
              th.style.width = w + 'px';
              th.firstChild.style.width = w + 'px';
            }
          }
          // Save the column width
          t.dataTableColumnWidth[t.startCol + col] = w;
          // Update the mouse down
          t.xMouseDown = ac.x;
          t.yMouseDown = ac.y;
          // Keep the cursor resizing
          document.body.style.cursor = 'ew-resize';
        } else {
          var off = (trg.offsetLeft + trg.clientWidth) - ac.x;
          if (off < 5 && trg.nodeName.toLowerCase() == 'th') {
            trg.style.cursor = 'ew-resize';
          } else {
            trg.style.cursor = 'default';
          }
        }
      }
      return false;
    }
  }(this);
  /*
   * mousedown data table header
   *
   * this.resizingDataTableColumnOn
   */
  this.mousedownDataTableHeader = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      var trg = e.target || e.srcElement;
      var ac = t.adjustedCoordinates(e, trg);
      if (ac) {
        var off = (trg.offsetLeft + trg.clientWidth) - ac.x;
        if (off < 5 && trg.nodeName.toLowerCase() == 'th') {
          t.dataTableColumnTarget = trg;
          t.xMouseDown = ac.x;
          t.yMouseDown = ac.y;
          t.resizingDataTableColumnOn = true;
          document.body.style.cursor = 'ew-resize';
        }
      }
      return false;
    }
  }(this);
  /*
   * Sort the column
   *
   * this.sortDataTableHead
   */
  this.clickDataTableHeader = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      var trg = t.getTargetEvent(e);
      var zro = t.target + '-cX-DataTableCell.0.0';
      if (trg.tagName.match(/th/i)) {
        trg = trg.firstChild;
      }
      if (trg.className == 'CanvasXpressTableCellSortDesc') {
        trg.className = 'CanvasXpressTableCellSortAsc';
      } else {
        trg.className = 'CanvasXpressTableCellSortDesc';
      }
      if (t.sortDataTableHead) {
        if (t.sortDataTableHead.id != trg.id) {
          var s = t.$(t.sortDataTableHead.id);
          if (s) {
            if (trg.parentNode.id == zro) {
              t.sortDataTableHead.id = t.target + '-cX-DataTableCellContent.0.0';
            }
            s.className = 'CanvasXpressTableCell';
          }
        }
      }
      t.sortDataTableHead = trg;
      t.sortDataTable();
      return false;
    }
  }(this);
  /*
   * Change the class name for the table header
   */
  this.mouseoverDataTableHeader = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      this.className = 'CanvasXpressTableCellHeadActive';
      return false;
    }
  }(this);
  /*
   * Change the class name for the table header
   */
  this.mouseoutDataTableHeader = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      this.className = 'CanvasXpressTableCellHead';
      return false;
    }
  }(this);
  /*
   * Change the class name for the table cell
   */
  this.mouseoverDataTableCell = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      this.className = 'CanvasXpressTableCellActive';
      return false;
    }
  }(this);
  /*
   * Change the class name for the table cell
   */
  this.mouseoutDataTableCell = function(t) {
    return function(e) {
      if (!e) {
        e = window.event;
      }
      this.className = 'CanvasXpressTableCell';
      return false;
    }
  }(this);
  /***********************
   * Data Table Functions
   ***********************/
  /*
   * Insert a table in the DOM
   * tree with the data
   *
   * this.totalRows
   * this.totalCols
   * this.initialMaxRows
   * this.initialMaxCols
   * this.dataTableRowsHeight
   * this.dataTableColsWidth
   * this.dataTableTotalHeight
   * this.dataTableTotalWidth
   * this.maxRows
   * this.maxCols
   *
   */
  this.setDataTableDimensions = function(o, f) {
    var r = 0;
    var c = 0;
    var n = 0;
    var reset = false;
    var g = function() {
      for (var i = 0; i < o.w.grps.length; i++) {
        n += o.w.grps[i].length;
      }
      return n;
    }
    if (!o) {
      o = this.data;
    }
    var x = 0;
    if (o.x) {
      for (var i in o.x) {
        x++;
      }
    }
    var z = 0;
    if (o.z) {
      for (var i in o.z) {
        z++;
      }
    }
    var t = this.getDataTableDimensions();
    if (t[0] != this.maxRows || t[1] != this.maxCols) {
      this.initialMaxRows = this.maxRows;
      this.initialMaxCols = this.maxCols;
    }
    if (this.graphType == 'Network') {
      var a = this.getAllNetworkAttributes(o);
      r = this.dataTableTransposed ? 1 + o[this.networkShowDataTable].length : 1 + a[1];
      c = this.dataTableTransposed ? 1 + a[1] : 1 + o[this.networkShowDataTable].length;
    } else if (this.graphType == 'Genome') {
      r = 1;
      c = 1;
    } else if (this.graphType == 'Venn') {
      var v = this.getVennCompartments(o);
      r = this.dataTableTransposed ? 1 + v[1] : 2;
      c = this.dataTableTransposed ? 2 : 1 + v[1];
    } else if (this.graphType == 'Correlation') {
      r = this.correlationAxis == 'samples' ? 1 + o.y.smps.length : 1 + o.y.vars.length;
      c = r;
    } else if (this.graphType == 'Candlestick') {
      if (o.y) {
        r = this.dataTableTransposed ? 1 + (o.y.smps.length * o.y.vars.length) : 6;
        c = this.dataTableTransposed ? 6 : 1 + (o.y.smps.length * o.y.vars.length);
      } else if (o.market) {
        r = this.dataTableTransposed ? 1 + (o.market.smps.length * o.market.vars.length) : 6;
        c = this.dataTableTransposed ? 6 : 1 + (o.market.smps.length * o.market.vars.length);
      }
    } else if (o.y && o.y.vars && o.y.smps) {
      r = this.dataTableTransposed ? 1 + z + o.y.smps.length : 1 + x + o.y.vars.length;
      c = this.dataTableTransposed ? 1 + x + o.y.vars.length : 1 + z + o.y.smps.length;
    } else if (o.w && o.w.vars && o.w.grps) {
      n = g();
      r = this.dataTableTransposed ? 1 + z + n : 1 + x + o.w.vars.length;
      c = this.dataTableTransposed ? 1 + x + o.w.vars.length : 1 + z + n;
    }
    if (this.maxRows > r) {
      this.initialMaxRows = this.maxRows;
      this.maxRows = r;
    }
    if (this.maxCols > c) {
      this.initialMaxCols = this.maxCols;
      this.maxCols = c;
    }
    if (this.totalRows == null || this.totalCols == null) {
      // First time
      this.totalRows = r;
      this.totalCols = c;
    } else {
      if (f || r != this.totalRows || c != this.totalCols || t[0] != this.maxRows || t[1] != this.maxCols || t[2] != this.rowHeight || t[3] != this.colWidth) {
        this.totalRows = r;
        this.totalCols = c;
        reset = true;
      } else {
        this.totalRows = r;
        this.totalCols = c;
        if (this.showDataTable) {
          this.hideUnhideDataTable();
        }
      }
    }
    // Assign the width and height for all the cells
    // in the table.
    // Total Height and Width used in scroller bars
    this.dataTableTotalHeight = 10;
    for (var i = 0; i < this.totalRows; i++) {
      this.dataTableTotalHeight += this.setDataTableRowHeight(i);
    }
    this.dataTableTotalHeight += this.totalRows * 4;
    this.dataTableTotalWidth = 10;
    for (var i = 0; i < this.totalCols; i++) {
      this.dataTableTotalWidth += this.setDataTableColumnWidth(i);
    }
    this.dataTableTotalWidth += this.totalCols * 8;
    // Width and Height of the displayed data in the table
    this.dataTableRowsHeight = 10;
    for (var i = this.startRow; i < this.startRow + this.maxRows; i++) {
      this.dataTableRowsHeight += this.setDataTableRowHeight(i);
    }
    this.dataTableRowsHeight += this.maxRows * 4;
    this.dataTableColsWidth = 10;
    for (var i = this.startCol; i < this.startCol + this.maxCols; i++) {
      this.dataTableColsWidth += this.setDataTableColumnWidth(i);
    }
    this.dataTableColsWidth += this.maxCols * 8;
    if (reset) {
      this.resetDataTable(true, t);
    }
  }
  /*
   * Set the data table column width
   */
  this.setDataTableColumnWidth = function(i, w) {
    if (!this.dataTableColumnWidth[i]) {
      this.dataTableColumnWidth[i] = this.colWidth;
    } else if (w) {
      this.dataTableColumnWidth[i] = Math.max(w, this.colWidth);
    }
    return this.dataTableColumnWidth[i];
  }
  /*
   * Set the row height
   */
  this.setDataTableRowHeight = function(i, h) {
    if (!this.dataTableRowHeight[i]) {
      this.dataTableRowHeight[i] = this.rowHeight;
    } else if (h) {
      this.dataTableRowHeight[i] = Math.max(h, this.rowHeight);
    }
    return this.dataTableRowHeight[i];
  }
  /*
   * Get all network attributes
   */
  this.getAllNetworkAttributes = function(o) {
    var a = this.getNetworkData(this.networkShowDataTable, true);
    var n = this.getKeys(a).length;
    return [a, n];
  }
  /*
   * Get Venn compartments
   */
  this.getVennCompartments = function(o) {
    var v = {};
    var n = 0;
    if (o.venn && o.venn.data) {
      for (var att in o.venn.data) {
        v[att] = 1;
        n++;
      }
    }
    return [v, n];
  }
  /*
   * Get the data table dimensions. the number of rows and columns
   * and the height and width of the first row and first column
   */
  this.getDataTableDimensions = function() {
    var r = 0;
    var c = 0;
    var h = 0;
    var w = 0;
    var t = this.$(this.target + '-cX-DataTableTable');
    var z = this.$(this.target + '-cX-DataTableCell.0.0');
    if (t) {
      var tb = t.childNodes[0].rows;
      if (tb) {
        r = tb.length;
        c = tb[0].cells.length;
        h = z && z.firstChild && z.firstChild.style ? parseInt(z.firstChild.style.height) : 0;
        w = z && z.firstChild && z.firstChild.style ? parseInt(z.firstChild.style.width) : 0;
      }
    }
    return [r, c, h, w];
  }
  /*
   * Remove the table in the DOM
   * tree with the data
   */
  this.resetDataTable = function(n, t) {
    this.addRemoveDataTableListeners('removeEvtListener', t);
    this.removeTarget(this.target + '-cX-DataTable');
    this.addDataTableDiv(false, n);
  }
  /*
   * Hide / Unhide the table in the DOM
   * tree with the data
   */
  this.hideUnhideDataTable = function(h) {
    if (this.initialMaxRows) {
      this.maxRows = this.initialMaxRows;
    }
    if (this.initialMaxCols) {
      this.maxCols = this.initialMaxCols;
    }
    var d = this.$(this.target + '-cX-DataTable');
    if (d) {
      if (h == true) {
        d.style.display = 'none';
      } else {
        d.style.display = 'block';
      }
    }
  }
  /*
   * Resize the data table
   */
  this.resizeDataTable = function() {
    var c = this.$(this.target + '-cX-DataTableContainer');
    var v = this.$(this.target + '-cX-DataTableVer');
    var h = this.$(this.target + '-cX-DataTableHor');
    if (c && v && h) {
      // Columns
      var cl = 0;
      this.maxCols = 0;
      for (var i = this.startCol; i < this.totalCols; i++) {
        cl += this.dataTableColumnWidth[i] + 8;
        if (parseInt(c.style.width) < cl) {
          this.maxCols++;
          break;
        } else {
          this.maxCols++;
        }
      }
      if (cl < parseInt(c.style.width) && this.startCol > 0) {
        for (var i = this.startCol; i >= 0; i--) {
          if (cl + this.dataTableColumnWidth[i] + 8 < parseInt(c.style.width)) {
            cl += this.dataTableColumnWidth[i] + 8;
            this.maxCols++;
            this.startCol--;
          } else {
            break;
          }
        }
      }
      if (parseInt(c.style.width) > this.dataTableTotalWidth) {
        this.maxCols = (this.totalCols - this.startCol);
      }
      this.maxCols = Math.max(3, Math.min(this.maxCols, (this.totalCols - this.startCol)));
      // Rows
      var rt = 0;
      this.maxRows = 0;
      for (var i = this.startRow; i < this.totalRows; i++) {
        rt += this.dataTableRowHeight[i] + 4;
        if (parseInt(c.style.height) < rt) {
          this.maxRows++;
          break;
        } else {
          this.maxRows++;
        }
      }
      if (rt < parseInt(c.style.height) && this.startRow > 0) {
        for (var i = this.startRow; i >= 0; i--) {
          if (rt + this.dataTableRowHeight[i] + 4 < parseInt(c.style.height)) {
            rt += this.dataTableRowHeight[i] + 4;
            this.maxRows++;
            this.startRow--;
          } else {
            break;
          }
        }
      }
      if (parseInt(c.style.height) > this.dataTableTotalHeight) {
        this.totalRows = (this.totalRows - this.startRow);
      }
      this.maxRows = Math.max(3, Math.min(this.maxRows, (this.totalRows - this.startRow)));
      // Update
      this.updateDataTable(false, true, false, true);
      // Get the divs again after the update
      v = this.$(this.target + '-cX-DataTableVer');
      h = this.$(this.target + '-cX-DataTableHor');
      // Update Scrollers
      if (v && h) {
        if (!this.isIE) {
          v.scrollHeight = this.dataTableLastScrollHeight;
        }
        v.scrollTop = this.dataTableLastScrollTop ? this.dataTableLastScrollTop : v.scrollTop;
        if (!this.isIE) {
          h.scrollWidth = this.dataTableLastScrollWidth;
        }
        h.scrollLeft = this.dataTableLastScrollLeft ? this.dataTableLastScrollLeft : h.scrollLeft;
      }
      if (this.dataTableLastState == 'docked') {
        // Update the south container
        this.resizeViewportSouth(true);
      }
    }
  }
  /*
   * Sort the data table
   */
  this.sortDataTable = function() {
    this.sortDir = this.sortDataTableHead.className == 'CanvasXpressTableCellSortDesc' ? 'descending' : 'ascending';
    if (this.graphType == 'Network') {
      if (this.dataTableTransposed) {
        if (this.sortDataTableHead.type == 'cxb') {
          this.sortDataTableHead.className = 'CanvasXpressTableCell';
          this.sortDataTableHead = false;
          return;
        } else {
          this.sortNetworkIndices(this.sortDataTableHead.innerHTML);
        }
      } else {
        this.sortDataTableHead.className = 'CanvasXpressTableCell';
        this.sortDataTableHead = false;
        return;
      }
    } else if (this.graphType == 'Genome') {
      this.sortDataTableHead.className = 'CanvasXpressTableCell';
      this.sortDataTableHead = false;
      return;
    } else if (this.graphType == 'Venn') {
      this.sortDataTableHead.className = 'CanvasXpressTableCell';
      this.sortDataTableHead = false;
      return;
    } else if (this.graphType == 'Correlation') {
      if (this.correlationAxis == 'samples') {
        if (this.sortDataTableHead.type == 'cxb') {
          this.sortSamples();
        } else {
          this.sortSamples(false, false, this.sortDataTableHead.innerHTML, 'cor', true);
          this.sortDataTableHead.className = 'CanvasXpressTableCell';
          this.sortDataTableHead = false;
        }
      } else {
        if (this.sortDataTableHead.type == 'cxb') {
          this.sortVariables();
        } else {
          this.sortVariables(false, false, this.sortDataTableHead.innerHTML, 'cor', true);
          this.sortDataTableHead.className = 'CanvasXpressTableCell';
          this.sortDataTableHead = false;
        }
      }
    } else if (this.graphType == 'Candlestick') {
      this.sortDataTableHead.className = 'CanvasXpressTableCell';
      this.sortDataTableHead = false;
      return;
    } else if (this.data.y.vars && this.data.y.smps) {
      if (this.dataTableTransposed) {
        if (this.sortDataTableHead.type == 'cxx') {
          this.sortSamplesByCategory([this.sortDataTableHead.innerHTML]);
        } else if (this.sortDataTableHead.type == 'cxv') {
          this.sortSamplesByVariable(this.sortDataTableHead.innerHTML);
        } else if (this.sortDataTableHead.type == 'cxb') {
          this.sortSamples();
        }
      } else {
        if (this.sortDataTableHead.type == 'cxz') {
          this.sortVariablesByCategory([this.sortDataTableHead.innerHTML]);
        } else if (this.sortDataTableHead.type == 'cxs') {
          this.sortVariablesBySample(this.sortDataTableHead.innerHTML);
        } else if (this.sortDataTableHead.type == 'cxb') {
          this.sortVariables();
        }
      }
    }
    this.updateDataTable(false, true, false, false, true);
  }
  /*
   * Update the data in the data table
   *
   */
  this.updateDataTable = function(o, f, e, n, q) {
    var row = 0;
    var col = 0;
    var trow = 0;
    var tcol = 0;
    var d = [];
    var c = [];
    var r = [];
    var s = [];
    var no = o ? false : true;
    var getValue = function(p, o, a, c) {
      if (o.hasOwnProperty(p)) {
        return o[p] != null ? o[p] : '';
      } else {
        if (a[0][p] && a[0][p].hasOwnProperty('r')) {
          var n = a[0][p]['r'];
          for (var i = 0; i < c[n].length; i++) {
            var j = c[n][i];
            if (o.hasOwnProperty(j)) {
              o = o[j];
            } else {
              return '';
            }
          }
          if (o.hasOwnProperty(p)) {
            return o[p] != null ? o[p] : '';
          }
        }
        return '';
      }
    }
    if (this.dataTableLastState && this.dataTableLastState == 'docked' && n) {
      this.dataTableLastX = this.configuringOn && this.configuringOn == 'docked' ? 0 : 7;
      this.dataTableLastY = 0;
    }
    if (!o) {
      o = this.data;
    } else if (this.isGroupedData) {
      var i = this.getVariableIndices(o.w.vars);
      var g = [];
      for (var j = 0; j < o.w.grps.length; j++) {
        g = g.concat(o.w.grps[j]);
      }
      this.isGroupedData = false;
      o = this.extractDataObject(g, i);
      this.isGroupedData = true;
    }
    if (!e) {
      this.setDataTableDimensions(o, f);
    }
    // Get the data
    if (this.graphType == 'Network') {
      var a = this.getAllNetworkAttributes(o);
      r.push('');
      s.push('cxb');
      if (this.dataTableTransposed) {
        // First Row
        for (var i in a[0]) {
          if (col < this.maxCols && tcol >= this.startCol) {
            r.push(i);
            s.push('cxx');
            col++;
          }
          tcol++;
        }
        d.push(r);
        c.push(s);
        // Rows with data
        for (var i = 0; i < o[this.networkShowDataTable].length; i++) {
          if (row < this.maxRows && trow >= this.startRow) {
            col = 0;
            tcol = 0;
            r = [i + 1];
            s = ['cxz'];
            for (var j in a[0]) {
              if (col < this.maxCols && tcol >= this.startCol) {
                var v = getValue(j, o[this.networkShowDataTable][i], a, this[this.networkShowDataTable + 'Properties']);
                r.push(v);
                s.push('cxy');
                col++;
              }
              tcol++;
            }
            d.push(r);
            c.push(s);
            row++;
          }
          trow++;
        }
      } else {
        // First Row
        for (var i = 0; i < o[this.networkShowDataTable].length; i++) {
          if (col < this.maxCols && tcol >= this.startCol) {
            r.push(i + 1);
            s.push('cxz');
            col++;
          }
          tcol++;
        }
        d.push(r);
        c.push(s);
        // Rows with data
        for (var i in a[0]) {
          if (row < this.maxRows && trow >= this.startRow) {
            col = 0;
            tcol = 0;
            r = [i];
            s = ['cxx'];
            for (var j = 0; j < o[this.networkShowDataTable].length; j++) {
              if (col < this.maxCols && tcol >= this.startCol) {
                var v = getValue(i, o[this.networkShowDataTable][j], a, this[this.networkShowDataTable + 'Properties']);
                r.push(v);
                s.push('cxy');
                col++;
              }
              tcol++;
            }
            d.push(r);
            c.push(s);
            row++;
          }
          trow++;
        }
      }
    } else if (this.graphType == 'Genome') {
      d = [['Not implemented']];
      c = [['cxb']];
    } else if (this.graphType == 'Venn') {
      var v = this.getVennCompartments(o);
      r.push('');
      s.push('cxb');
      if (this.dataTableTransposed) {
        // First Row
        r.push('No');
        s.push('cxz');
        d.push(r);
        c.push(s);
        // Row with data
        for (var i in v[0]) {
          if (row < this.maxRows && trow >= this.startRow) {
            col = 0;
            tcol = 0;
            r = [i];
            s = ['cxx'];
            if (col < this.maxCols && tcol >= this.startCol) {
              r.push(o.venn.data[i]);
              s.push('cxy');
              col++;
            }
            tcol++;
            d.push(r);
            c.push(s);
            row++
          }
          trow++;
        }
      } else {
        // First Row
        for (var i in v[0]) {
          if (col < this.maxCols && tcol >= this.startCol) {
            r.push(i);
            s.push('cxx');
            col++;
          }
          tcol++;
        }
        d.push(r);
        c.push(s);
        // Second Row
        if (row < this.maxRows && trow >= this.startRow) {
          col = 0;
          tcol = 0;
          r = ['No'];
          s = ['cxz'];
          for (var i in v[0]) {
            if (col < this.maxCols && tcol >= this.startCol) {
              r.push(o.venn.data[i]);
              s.push('cxy');
              col++;
            }
            tcol++;
          }
          d.push(r);
          c.push(s);
        }
      }
    } else if (this.graphType == 'Correlation') {
      var ax = this.correlationAxis == 'samples' ? o.y.smps : o.y.vars;
      var ix = this.correlationAxis == 'samples' ? this.smpIndices : this.varIndices;
      var cl = this.correlationAxis == 'samples' ? 'cxs' : 'cxv';
      r.push('');
      s.push('cxb');
      // First Row
      for (var i = 0; i < ax.length; i++) {
        if (col < this.maxCols && tcol >= this.startCol) {
          r.push( no ? ax[ix[i]] : ax[i]);
          s.push(cl);
          col++;
        }
        tcol++;
      }
      d.push(r);
      c.push(s);
      // Data
      for (var i = 0; i < ax.length; i++) {
        if (row < this.maxRows && trow >= this.startRow) {
          col = 0;
          tcol = 0;
          r = [ no ? ax[ix[i]] : ax[i]];
          s = [cl];
          for (var j = 0; j < ax.length; j++) {
            if (col < this.maxCols && tcol >= this.startCol) {
              if (o.y.cor) {
                r.push( no ? o.y.cor[ix[i]][ix[j]] : o.y.cor[i][j]);
              } else {
                r.push(i == j ? 1 : o.y.data);
              }
              s.push('cxy');
              col++;
            }
            tcol++;
          }
          d.push(r);
          c.push(s);
          row++;
        }
        trow++;
      }
    } else if (this.graphType == 'Candlestick') {
      var a = ['open', 'low', 'high', 'close', 'volume'];
      r.push('');
      s.push('cxb');
      if (this.dataTableTransposed) {
        // First Row
        for (var i = 0; i < o.y.vars.length; i++) {
          for (var j = 0; j < a.length; j++) {
            if (col < this.maxCols && tcol >= this.startCol) {
              var l = o.y.vars.length > 1 ? o.y.vars[i] + ':' + a[j] : a[j];
              r.push(l);
              s.push('cxv');
              col++;
            }
            tcol++;
          }
        }
        d.push(r);
        c.push(s);
        // Data
        for (var i = 0; i < o.y.smps.length; i++) {
          if (row < this.maxRows && trow >= this.startRow) {
            col = 0;
            tcol = 0;
            r = [o.y.smps[i]];
            s = ['cxs'];
            for (var j = 0; j < o.y.vars.length; j++) {
              for (var k = 0; k < a.length; k++) {
                if (col < this.maxCols && tcol >= this.startCol) {
                  r.push(o.y[a[k]][j][i] != null ? o.y[a[k]][j][i] : o.y[a[k]][i]);
                  s.push('cxy');
                  col++;
                }
                tcol++;
              }
            }
            d.push(r);
            c.push(s);
            row++;
          }
          trow++;
        }
      } else {
        // First Row
        for (var i = 0; i < o.y.smps.length; i++) {
          if (col < this.maxCols && tcol >= this.startCol) {
            r.push(o.y.smps[i]);
            s.push('cxs');
            col++;
          }
          tcol++;
        }
        d.push(r);
        c.push(s);
        // Data
        for (var i = 0; i < o.y.vars.length; i++) {
          for (var j = 0; j < a.length; j++) {
            if (row < this.maxRows && trow >= this.startRow) {
              col = 0;
              tcol = 0;
              var l = o.y.vars.length > 1 ? o.y.vars[i] + ':' + a[j] : a[j];
              r = [l];
              s = ['cxv'];
              for (var k = 0; k < o.y.smps.length; k++) {
                if (col < this.maxCols && tcol >= this.startCol) {
                  r.push(o.y[a[j]][i][k] != null ? o.y[a[j]][i][k] : o.y[a[j]][k]);
                  s.push('cxy');
                  col++;
                }
                tcol++;
              }
              d.push(r);
              c.push(s);
              row++;
            }
            trow++;
          }
        }
      }
    } else if (o.y.vars && o.y.smps) {
      var vl = no ? this.varIndices.length : o.y.vars.length;
      var sl = no ? this.smpIndices.length : o.y.smps.length;
      r.push('');
      s.push('cxb');
      if (this.dataTableTransposed) {
        // First Row
        if (o.x) {
          for (var i in o.x) {
            if (col < this.maxCols && tcol >= this.startCol) {
              r.push(i);
              s.push('cxx');
              col++;
            }
            tcol++;
          }
        }
        for (var i = 0; i < vl; i++) {
          if (col < this.maxCols && tcol >= this.startCol) {
            r.push( no ? o.y.vars[this.varIndices[i]] : o.y.vars[i]);
            s.push('cxv');
            col++;
          }
          tcol++;
        }
        d.push(r);
        c.push(s);
        // Rows with variable annotations
        if (o.z) {
          for (var i in o.z) {
            if (row < this.maxRows && trow >= this.startRow) {
              col = 0;
              tcol = 0;
              r = [i];
              s = ['cxz'];
              if (o.x) {
                for (var j in o.x) {
                  if (col < this.maxCols && tcol >= this.startCol) {
                    r.push('');
                    s.push('cxb');
                    col++;
                  }
                  tcol++;
                }
              }
              for (var j = 0; j < vl; j++) {
                if (col < this.maxCols && tcol >= this.startCol) {
                  r.push( no ? o.z[i][this.varIndices[j]] : o.z[i][j]);
                  s.push('cxz');
                  col++;
                }
                tcol++;
              }
              d.push(r);
              c.push(s);
              row++;
            }
            trow++;
          }
        }
        // Rows with data
        for (var i = 0; i < sl; i++) {
          if (row < this.maxRows && trow >= this.startRow) {
            col = 0;
            tcol = 0;
            r = [ no ? o.y.smps[this.smpIndices[i]] : o.y.smps[i]];
            s = ['cxs'];
            if (o.x) {
              for (var j in o.x) {
                if (col < this.maxCols && tcol >= this.startCol) {
                  r.push( no ? o.x[j][this.smpIndices[i]] : o.x[j][i]);
                  s.push('cxx');
                  col++;
                }
                tcol++;
              }
            }
            for (var j = 0; j < vl; j++) {
              if (col < this.maxCols && tcol >= this.startCol) {
                if (no) {
                  r.push(o.y.data[this.varIndices[j]][this.smpIndices[i]] != null ? o.y.data[this.varIndices[j]][this.smpIndices[i]] : o.y.data[this.smpIndices[i]]);
                } else {
                  r.push(o.y.data[j][i] != null ? o.y.data[j][i] : o.y.data[i]);
                }
                s.push('cxy');
                col++;
              }
              tcol++;
            }
            d.push(r);
            c.push(s);
            row++;
          }
          trow++;
        }
      } else {
        // First Row
        if (o.z) {
          for (var i in o.z) {
            if (col < this.maxCols && tcol >= this.startCol) {
              r.push(i);
              s.push('cxz');
              col++;
            }
            tcol++;
          }
        }
        for (var i = 0; i < sl; i++) {
          if (col < this.maxCols && tcol >= this.startCol) {
            r.push( no ? o.y.smps[this.smpIndices[i]] : o.y.smps[i]);
            s.push('cxs');
            col++;
          }
          tcol++;
        }
        d.push(r);
        c.push(s);
        // Rows with variable annotations
        if (o.x) {
          for (var i in o.x) {
            if (row < this.maxRows && trow >= this.startRow) {
              col = 0;
              tcol = 0;
              r = [i];
              s = ['cxx'];
              if (o.z) {
                for (var j in o.z) {
                  if (col < this.maxCols && tcol >= this.startCol) {
                    r.push('');
                    s.push('cxb');
                    col++;
                  }
                  tcol++;
                }
              }
              for (var j = 0; j < sl; j++) {
                if (col < this.maxCols && tcol >= this.startCol) {
                  r.push( no ? o.x[i][this.smpIndices[j]] : o.x[i][j]);
                  s.push('cxx');
                  col++;
                }
                tcol++;
              }
              d.push(r);
              c.push(s);
              row++;
            }
            trow++;
          }
        }
        // Rows with data
        for (var i = 0; i < vl; i++) {
          if (row < this.maxRows && trow >= this.startRow) {
            col = 0;
            tcol = 0;
            r = [ no ? o.y.vars[this.varIndices[i]] : o.y.vars[i]];
            s = ['cxv'];
            if (o.z) {
              for (var j in o.z) {
                if (col < this.maxCols && tcol >= this.startCol) {
                  r.push( no ? o.z[j][this.varIndices[i]] : o.z[j][i]);
                  s.push('cxz');
                  col++;
                }
                tcol++;
              }
            }
            for (var j = 0; j < sl; j++) {
              if (col < this.maxCols && tcol >= this.startCol) {
                if (no) {
                  r.push(o.y.data[this.varIndices[i]][this.smpIndices[j]] != null ? o.y.data[this.varIndices[i]][this.smpIndices[j]] : o.y.data[this.smpIndices[j]]);
                } else {
                  r.push(o.y.data[i][j] != null ? o.y.data[i][j] : o.y.data[j]);
                }
                s.push('cxy');
                col++;
              }
              tcol++;
            }
            d.push(r);
            c.push(s);
            row++;
          }
          trow++;
        }
      }
    }
    // Load the data in the table
    if (e) {
      return d;
    } else {
      this.loadDataTableValues(d, c, n, q);
    }
  }
  /*
   * Load the data table values
   */
  this.loadDataTableValues = function(d, c, n, q) {
    var t = this.$(this.target + '-cX-DataTable');
    if (t) {
      // Load the data
      var ii = this.startRow;
      for (var i = 0; i < d.length; i++) {
        var jj = this.startCol;
        for (var j = 0; j < d[i].length + this.startCol; j++) {
          var id = this.target + '-cX-DataTableCell.' + i + '.' + j;
          var did = this.target + '-cX-DataTableCellContent.' + ii + '.' + jj;
          var e = this.$(id);
          if (e) {
            // Remove the current value
            if (e.hasChildNodes()) {
              while (e.childNodes.length >= 1) {
                e.removeChild(e.firstChild);
              }
            }
            var val = d[i][j] != null ? d[i][j] : '';
            var cls = c[i][j] != null ? c[i][j] : '';
            var td = this.$cX('div', {
              id : did,
              className : 'CanvasXpressTableCell ',
              innerHTML : val,
              title : val,
              alt : val,
              type : cls
            });
            if (i != 0) {
              td.style.height = this.setDataTableRowHeight(ii) + 'px';
              e.style.height = this.setDataTableRowHeight(ii) + 'px';
            } else {
              td.style.height = this.setDataTableRowHeight(0) + 'px';
              e.style.height = this.setDataTableRowHeight(0) + 'px';
            }
            if (j != 0) {
              td.style.width = this.setDataTableColumnWidth(jj) + 'px';
              e.style.width = this.setDataTableColumnWidth(jj) + 'px';
            } else {
              td.style.width = this.setDataTableColumnWidth(0) + 'px';
              e.style.width = this.setDataTableColumnWidth(0) + 'px';
            }
            e.appendChild(td);
            jj++;
          }
        }
        ii++;
      }
      // Make the table appear
      if (this.activeTarget) {
        this.activeTarget.style.zIndex = 10000;
      }
      if (this.sortDataTableHead) {
        var s = this.$(this.sortDataTableHead.id);
        var z = this.$(this.target + '-cX-DataTableCell.0.0').firstChild;
        var w = this.target + '-cX-DataTableCellContent.0.0';
        if (s) {
          s.className = this.sortDataTableHead.className;
        }
        if (z) {
          z.className = this.sortDataTableHead.id == w ? this.sortDataTableHead.className : 'CanvasXpressTableCell';
        }
      }
      this.activeTarget = t;
      t.style.display = 'block';
      t.style.zIndex = 10001;
      if (this.dataTableLastState && this.dataTableLastState == 'docked' && !n && !q) {
        var sh = this.$('south-handler-' + this.target);
        this.clickViewport(false, sh);
        return;
      }
    }
  }
  /*
   * Update the postion of the data table
   *
   * this.dataTableLastHeight
   * this.dataTableLastX
   * this.dataTableLastY
   */
  this.moveDataTableDiv = function(l) {
    var d = this.$(this.target + '-cX-DataTable');
    if (d) {
      var z, w, h;
      var rw = this.$(this.remoteParentId + '-canvasXpressRemoteWindow');
      var wh = this.$('west-handler-' + this.target);
      var nc = this.$('north-container-' + this.target);
      var wc = this.$('west-container-' + this.target);
      var ec = this.$('east-container-' + this.target);
      var sc = this.$('south-container-' + this.target);
      var sh = this.$('south-handler-' + this.target);
      var sw = this.$('south-wrapper-' + this.target);
      var that = this;
      if (wh && wc && ec && sc && sh && sw) {
        if (l && l == 'dock') {
          this.dataTableLastState = 'docked';
          this.dataTableLastHeight = d.clientHeight;
          this.dataTableLastX = d.offsetLeft;
          this.dataTableLastY = d.offsetTop;
          sh.style.display = 'block';
          z = wh.style.display == 'none' ? 7 : 0;
          w = Math.max(d.clientWidth + z, parseInt(wc.style.width) + parseInt(ec.style.width) + this.canvas.width) + 2;
          h = parseInt(nc.style.height) + this.canvas.height + d.clientHeight + 7 + 2;
          if (rw) {
            this.resizeMove(rw, 0, 0, w + 48, h + 54);
          }
          this.resizeMove(sc, 0, 0, w, d.clientHeight + 7 + 2);
          this.resizeMove(sw, 0, 0, w, d.clientHeight + 2);
          this.resizeMove(d, z, 0, d.clientWidth, d.clientHeight);
        } else if (l && l == 'hide') {
          delete (this.dataTableLastState);
          delete (this.dataTableLastX);
          delete (this.dataTableLastY);
          sh.style.display = 'none';
          w = parseInt(wc.style.width) + parseInt(ec.style.width) + this.canvas.width;
          h = parseInt(nc.style.height) + this.canvas.height + 7;
          if (rw) {
            this.resizeMove(rw, 0, 0, w + 48, h + 54);
          }
          this.fade(d);
          this.resizeMove(sw, 0, 0, w, 0);
          this.resizeMove(sc, 0, 0, w, 7);
        } else if (l && (l == 'max' || l == 'release' || l == 'undock')) {
          this.dataTableLastState = 'free';
          sh.style.display = 'none';
          w = parseInt(wc.style.width) + parseInt(ec.style.width) + this.canvas.width;
          h = parseInt(nc.style.height) + this.canvas.height + 7;
          if (rw) {
            this.resizeMove(rw, 0, 0, w + 48, h + 54);
          }
          if (l == 'max' || l == 'undock') {
            this.resizeMove(d, parseInt(this.dataTableLastX), parseInt(this.dataTableLastY), d.clientWidth, parseInt(this.dataTableLastHeight) || (this.dataTableRowsHeight + 42));
          }
          this.resizeMove(sw, 0, 0, w, 0);
          this.resizeMove(sc, 0, 0, w, 7);
        } else {
          this.dataTableLastX = d.offsetLeft;
          this.dataTableLastY = d.offsetTop;
          return;
        }
        if (rw) {
          setTimeout(function() {
            that.resizeExtContainer(w + 48, h + 54);
          }, 500);
        }
      }
    }
  }
  /*
   * Update the dimensions of the data table after dragging the resizer
   *
   * this.dataTableLastWidth
   * this.dataTableLastHeight
   */
  this.updateDataTableResizerDiv = function(e, x, y) {
    if (!e) {
      e = window.event;
    }
    if (e && (x == null || y == null)) {
      x = Math.abs(this.dataTableWidth) - (this.xMouseDown - e.clientX);
      y = Math.abs(this.dataTableHeight) - (this.yMouseDown - e.clientY);
    }
    if (this.dataTableTarget && this.dataTableTarget.style) {
      var d = this.$(this.target + '-cX-DataTable');
      var c = this.$(this.target + '-cX-DataTableContainer');
      var m = this.$(this.target + '-cX-DataTableTableMask');
      var v = this.$(this.target + '-cX-DataTableVer');
      var h = this.$(this.target + '-cX-DataTableHor');
      var t = this.$(this.target + '-cX-DataTableToolbar');
      if (d && c && m && v && h && t) {
        this.dataTableTarget.style.width = this.isIE ? Math.max(0, x) + 'px' : x + 'px';
        c.style.width = this.isIE ? Math.max(0, x) + 'px' : x + 'px';
        m.style.width = this.isIE ? Math.max(0, (x - 20)) + 'px' : (x - 20) + 'px';
        h.style.width = this.isIE ? Math.max(0, (x - 20)) + 'px' : (x - 20) + 'px';
        t.style.width = this.isIE ? Math.max(0, x) + 'px' : x + 'px';
        this.dataTableTarget.style.height = this.isIE ? Math.max(0, y) + 'px' : y + 'px';
        c.style.height = this.isIE ? Math.max(0, (y - 20)) + 'px' : (y - 20) + 'px';
        m.style.height = this.isIE ? Math.max(0, (y - 44)) + 'px' : (y - 44) + 'px';
        v.style.height = this.isIE ? Math.max(0, (y - 44)) + 'px' : (y - 44) + 'px';
        // Keep track of the table dimensions
        this.dataTableLastWidth = x;
        this.dataTableLastHeight = y;
      }
    }
  }
  /*
   * Release the data table div
   */
  this.endDataTableResizerDiv = function(e) {
    if (this.dataTableTarget != null) {
      this.resizeDataTable();
      this.xMouseDown = null;
      this.yMouseDown = null;
      this.dataTableWidth = null;
      this.dataTableHeight = null;
      this.dataTableTarget = false;
      this.resizingDataTableOn = false;
    }
  }
  /*
   * End the column resizing
   */
  this.endDataTableColumnResize = function(e) {
    this.xMouseDown = null;
    this.yMouseDown = null;
    this.resizingDataTableColumnOn = false;
    this.dataTableColumnTarget = false;
    document.body.style.cursor = 'default';
  }
  /*************
   * Initialize
   *************/
  this.initializeDataTableEvents = function() {
    this.addDataTableDiv();
  }

  this.initializeDataTableEvents();
}

