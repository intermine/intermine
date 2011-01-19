jQuery.beautyOfCode = {
  initialized: false,

  settings: {

    // hide line numbers?
    noGutter: false,

    // show copy, plain, ... links
    addControls: true,

    // collapse to control bar. cant be used
    // with addControls set to false
    collapse: false,

    // show column numbers
    showColumns: false,

    // start with another line number?
    firstLine: 1
  },

  brushByAlias: {},

  init: function (clipboardSwf, settings) {
    SyntaxHighlighter.ClipboardSwf = clipboardSwf;

    if (settings)
      jQuery.extend(jQuery.beautyOfCode.settings, settings);

    if (jQuery.beautyOfCode.isInitialized)
      return;

    // creates a map of each registered brush by alias
    jQuery.each(SyntaxHighlighter.brushes, function (i, brush) {
      var aliases = brush.Aliases;

      if(aliases == null)
       return;

      jQuery.each(aliases, function (ii, alias) {
        jQuery.beautyOfCode.brushByAlias[alias] = brush;
      });
    });

    jQuery.beautyOfCode.isInitialized = true;
  },

  addCssForBrush: function (brush, highlighter) {
    if (brush.isCssInitialized)
      return;

    var headNode = $("head")[0];
    if(highlighter.Style && headNode)
    {
      var styleNode = document.createElement('style');
      styleNode.setAttribute('type', 'text/css');

      if(styleNode.styleSheet) // for IE
        styleNode.styleSheet.cssText = highlighter.Style;
      else // for everyone else
        $(styleNode).text(highlighter.Style);

      headNode.appendChild(styleNode);
    }

    brush.isCssInitialized = true;
  },

  beautifyAll: function() {
    jQuery("pre.code:has(code[class])").each(function (i, item) {

      function getOptionValue(name, list)
      {
        var regex = new RegExp('^' + name + '\\[(\\w+)\\]$', 'gi');
        var matches = null;

        for(var i = 0; i < list.length; i++)
         if((matches = regex.exec(list[i])) != null)
          return matches[1];

        return null;
      }

      var $item = jQuery(item);
      var $code = $item.children("code");
      var code = $code[0];

      var options = code.className.split(" ");
      var language = options[0];

      var settings = {};

      if ($code.hasClass("boc-nogutter"))
        settings.noGutter = true;

      if ($code.hasClass("boc-nocontrols"))
        settings.addControls = false;

      if ($code.hasClass("boc-showcolumns"))
        settings.showColumns = true;

      if ($code.hasClass("boc-collapse"))
        settings.collapse = true;        

      var firstLine = getOptionValue("boc-firstline", options, 1);
      if (firstLine)
        settings.firstLine = firstLine;

      $item.beautifyCode(language, settings);
    });
  }
};

jQuery.fn.beautifyCode = function (language, settings) {

  var saveLanguage = language;
  var saveSettings = settings;

  // iterate all elements
  this.each( function (i, item) {
    var $item = jQuery(item);

    var settings = jQuery.extend({}, jQuery.beautyOfCode.settings, saveSettings);

    var brush = jQuery.beautyOfCode.brushByAlias[saveLanguage];

    if (!brush)
      return;

    // instantiate brush
    highlighter = new brush();

    // set brush options
    jQuery.extend(highlighter, settings);

    jQuery.beautyOfCode.addCssForBrush(brush, highlighter);

    // IE Bug?: code in pre has to be skipped
    // in order to preserver line breaks.
    if ($item.is("pre") && ($code = $item.children("code")))
      $item.text($code.text());

    highlighter.Highlight($item.html());
    highlighter.source = item;

    $item.replaceWith(highlighter.div);
  });
}
