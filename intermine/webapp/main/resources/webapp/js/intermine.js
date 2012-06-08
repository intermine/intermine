// jQuery reqs
if (typeof jQuery == 'undefined') {
  throw new Error("ƒ missing jQuery!");
} else {
  if (jQuery().jquery < '1.4.1') {
    throw new Error("ƒ jQuery >= 1.4.1 required");
  }
}
// the prefix
var im;
if (!im) {
    im = {};
    if (window.console && window.console.log) { // 'attach' Firebug
      console.log("ƒ InterMine JavaScript Library loaded");
      im.firebug = true;
      jQuery.error = console.error;
    }
} else if (typeof org != 'object') throw new Error("ƒ InterMine JavaScript Library cannot be loaded, 'var im' is already in use");

//check if element exists
im.exists = function(e) {
  return (jQuery(e).length > 0);
};

// (on all IE only), will apply .odd/.even classes to "known" tables (see Wiki for those)
im.alternatingColors = function() {
  if (jQuery.browser.msie) {
    jQuery.each(['collection-table'], function(index, imTable) {
      jQuery('div.' + imTable + ' table tbody tr:nth-child(2n)').addClass('odd');
      jQuery('div.' + imTable + '.column-border table tbody tr td:nth-child(n+2)').addClass('left-column-border');
      jQuery('div.' + imTable + '.column-border-by-2 table tbody tr td:nth-child(2n+3)').addClass('left-column-border');
    });
  }
};

// will turn thead.persistent into persistent header
im.persistentTableHeaders = function(e) {

  function makeEm(collection) {
    var collection = jQuery(collection),
      table 	   = collection.find('table'),
      head  	   = table.find('thead tr');
    // continue only if we have exactly one <tr> element in the head
    // ... and at least two elements in the actual table
    if (head.length == 1 && table.find('tbody tr').length > 1) {
      // duplicate the row and apply different classes to static/fixed positioned elements
      fixed = head.addClass('static-header').clone().attr('class', 'fixed-header').appendTo(head.parent());
      // apply fixed positioning
      fixed.css({'position':'fixed', 'top':'21px', 'z-index':2, 'width':"100%"}).hide();

      // now we need to fix the width of the columns much like in the original head
      head.find('th').each(function(i) {
        jQuery(fixed.find('th')[i]).css('width', jQuery(this).width());
      });
    }
  }

  function monitorEm(collection) {
    var collection = jQuery(collection);
    collection.find('div.toggle').click(function() {
      // ... and resize the head as we have different number of rows now
      var head = collection.find('table thead');

      head.find('tr.static-header th').each(function(i) {
        jQuery(head.find('tr.fixed-header th')[i]).css('width', jQuery(this).width());
      });
    });
  }

  // undefined, grab them all
  if (e == undefined) {
    // traverse all 'collection' tables that have a .persistent class
    // monitor the showing/hiding of table rows throw more/collapse
    jQuery('div.collection-table.persistent').each(function(i) {
      makeEm(this);
      monitorEm(this);
    });
  } else {
    // make them and monitor them e
    makeEm(e);
    monitorEm(e);
  }

  // monitor scroll events and see if we no longer see the static one
  if (jQuery('div.collection-table.persistent').length > 0) {
    jQuery(window).scroll(function() {
      jQuery('div.collection-table.persistent').each(function(i) {
        var table  = jQuery(this).find('table'),
          offset = table.offset(),
          top    = jQuery(window).scrollTop() + 21,
          fixed  = table.find('thead tr.fixed-header');

        // then swap the 'visibility' of the fixed head
        if (table.length > 0) {
          if ((top > offset.top) && (top < offset.top + table.height() - fixed.height())) {
            if (!fixed.is(':visible')) {
              fixed.show();

              // recalculate their width for good measure
              table.find('thead tr.static-header th').each(function(i) {
                jQuery(table.find('thead tr.fixed-header th')[i]).css('width', jQuery(this).width());
              });
            }
          } else {
            fixed.hide();
          }
        }
      });
    }).trigger("scroll");
  }
}

// return a path to the current element
im.elementPath = function(e) {
  e = jQuery(e);
  switch (e.length) {
    case 0:
      jQuery.error("ƒ element does not exist");
      im.trace(1);
      break;
    case 1:
      break;
    default:
      return e.each().elementPath(); // call us for each matched element
  }

  var node = e;
  var path = Array();
  while (node.length) {
    var realNode = node[0], name = realNode.localName;
      if (!name) break;
      name = name.toLowerCase();
      var parent = node.parent();
      var siblings = parent.children(name);
      if (siblings.length > 1) {
        attrId = jQuery(node).attr('id');
          attrClass = jQuery(node).attr('class');
          if (attrId) name += '#' + attrId;
          if (attrClass) name += '.' + attrClass.replace(/ /g, "."); // using dot notation
      }
      path.push(name);
      node = parent;
  }
  return im.log(path.join(' < '));
};

// log message in Firebug if enabled
im.log = function(message) {
  if (im.firebug) {
    console.log('ƒ ' + message);
  }
};

// return a stack trace (Firefox only)
im.trace = function(level) {
  if (jQuery.browser.mozilla) {
    try {
      kaboodakabooda++; // booda?
    } catch (e) {
      level = (level === undefined) ? 0 : level;
        var stack = ['trace:'];
        jQuery.each(e.stack.split("\n"), function(index, value) {
          if (index > level) {
            if (value.toLowerCase().indexOf('jquery') < 0) {
              stack.push(value.substring(value.indexOf('@')));
            }
          }
        });
        return im.log(stack.join('\n'));
    }
  }
};

// scroll to an element
im.scrollTo = function(e, speed, easing, offset, onComplete) {
    return jQuery('html,body').animate({scrollTop: jQuery(e).offset().top + offset}, speed, easing, onComplete);
};

// is element in the viewport?
im.isInView = function(e, visibility) {
    var pageTop       = jQuery(window).scrollTop(),
      pageBottom    = pageTop + jQuery(window).height(),
      elementTop    = jQuery(e).offset().top,
      elementBottom = elementTop + jQuery(e).height();

    return (visibility == 'partial') ?  ((elementBottom >= pageTop) && (elementTop <= pageBottom)) : ((elementBottom < pageBottom) && (elementTop > pageTop));
};

// provide a lightweight jQuery.animate("highlight") alternative
im.highlight = function(e) {
  var startRGB 			 = [255, 243, 211],
    endRGB	 			 = [255, 255, 255],
    finalBackgroundValue = 'transparent',
    totalSteps			 = 75,
    currentStep			 = 0,
    speed				 = 20,
    power				 = 4;

  e.highlight = window.setInterval(function() {
    e.css("background", newRGB(startRGB, endRGB, totalSteps, currentStep++, power));
      if (currentStep > totalSteps) {
        e.css("background", finalBackgroundValue);
          window.clearInterval(e.highlight);
      }
  }, speed);

  function arrayToRGB(array) {
    return "rgb(" + array[0] + "," + array[1] + "," + array[2] + ")";
  }

  // determine new RGB value in a range
  function newRGB(startRGB, endRGB, totalSteps, currentStep, power) {
    var newRGB = Array();
    for (var i = 0; i < 3; i++) {
      newRGB[i] = Math.ceil(startRGB[i] + (Math.pow(((1 / totalSteps) * currentStep), power) * (endRGB[i] - startRGB[i])));
    }
    return arrayToRGB(newRGB);
  }
};

// a queue object for delayed execution of long running code
im.queue = {
  'timer': null,
  'q': [],
  put: function(fn, context, time) {
    var setTimer = function(time) {
      im.queue.timer = setTimeout(function() {
        time = im.queue.put();
              if (im.queue.q.length) setTimer(time);
          }, time || 2);
      }

    if (fn) {
      im.queue.q.push([fn, context, time]);
          if (im.queue.q.length == 1) setTimer(time);
          return;
      }

    var next = im.queue.q.shift();
      if (!next) return 0;
      next[0].call(next[1] || window);
      return next[2];
  }
};

// set a key value cookie for a period of days
im.setCookie = function(key, value, days) {
  var cookie = key + "=" + escape(value);
    days   = (days) ? parseInt(days): 9999,
    expires = new Date();

  expires.setDate(expires.getDate() + days);
  cookie += ";expires=" + expires.toGMTString() + ";";
  document.cookie = cookie;
};

// retrieve a cookie by its key
im.getCookie = function(key) {
  var cookies = document.cookie.split(";"),
    i 		 = 0,
    l 		 = cookies.length;

  for (i; i < l; i++) {
      var cookie     = cookies[i],
        p       	 = cookie.indexOf("="),
        currentKey = cookie.substr(0, p).replace(/^\s+|\s+$/g,"");

    if (currentKey == key) {
      return unescape(cookie.substr(p+1));
    }
  }
};

// format system timestamps from the client's perspective, add 1 to the month and format the time like on bagUploadConfirm
// sadly, so that sorters work, we can show nice dates (for now) as a 'title' only
im.timestampFormat = function() {
  var pad = function(value) { return (value < 10) ? "0" + value : value; };

  // locales
  Date.locale = {
      'en': {
         month_name_short: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
      }
  };

  Date.prototype.getMonthNameShort = function(lang) {
      lang = lang && (lang in Date.locale) ? lang : 'en';
      return Date.locale[lang].month_name_short[this.getMonth()];
  };

  jQuery(".intermine.timestamp").each(function() {
    var timestamp = parseInt(jQuery(this).text());
    // convert to Date and shift from current timezone in minutes
    if (timestamp) { // parse was successful.
      var d = new Date(timestamp);
      // format
      var formattedTime = d.getFullYear() + "-" +
        pad(d.getMonth() + 1) + "-" +
        pad(d.getDate()) + " " +
        pad(d.getHours()) + ":" +
        pad(d.getMinutes());

      var title = pad(d.getDate()) + " " + d.getMonthNameShort() + " " +
         d.getFullYear() + " " + pad(d.getHours()) + ":" + pad(d.getMinutes());

      jQuery(this).text(formattedTime).removeClass('intermine').attr('title', title);
    } else {
      jQuery(this).text("Unknown date").removeClass('intermine').addClass("unknown");
    }
  });
};

im.slugify = function(str) {
  str = str.replace(/^\s+|\s+$/g, '').toLowerCase();

  var from = "àáäâèéëêìíïîòóöôùúüûñç·/_,:;",
     to   = "aaaaeeeeiiiioooouuuunc------";
  for (var i=0, l=from.length; i<l; i++) {
    str = str.replace(new RegExp(from.charAt(i), 'g'), to.charAt(i));
  }

  return str.replace(/[^a-z0-9 -]/g, '').replace(/\s+/g, '-').replace(/-+/g, '-');
};

// jQuery extensions
jQuery.fn.extend({
  exists: function() {
    return im.exists(this);
  },
  elementPath: function() {
    return im.elementPath(this);
  },
  log: function(message) {
    im.log(message);
  },
  scrollTo: function(speed, easing, offset, onComplete) {
    return im.scrollTo(this, speed, easing, offset, onComplete);
  },
  isInView: function(visibility) {
    return im.isInView(this, visibility);
  },
  persistentTableHeaders: function() {
    return im.persistentTableHeaders(this);
  },
  highlight: function() {
    return im.highlight(this);
  },
  setCookie: function(key, value, days) {
    im.setCookie(key, value, days);
  },
  getCookie: function(key) {
    return im.getCookie(key);
  },
  slugify: function(str) {
    return im.slugify(str);
  }
});

// on load methods
jQuery(document).ready(function() {
  // fix n-th child in IE and apply .odd/.even classes to tables
  im.alternatingColors();
  // persistent table headers
  im.persistentTableHeaders();
  // timestamp formatting
  im.timestampFormat();
});
