<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<tiles:importAttribute name="sliderIdentifier" ignore="false" />
<tiles:importAttribute name="defaultValue" ignore="false" />

<script type="text/javascript" src="js/dragdealer.js"></script>

<style>
#${sliderIdentifier}.slider-wrap .slider { float:right; padding:6px 2px; margin-top:1px; }
#${sliderIdentifier}.slider-wrap .slider a { font-size:10px; float:left; cursor:pointer; width:22px; }
#${sliderIdentifier}.slider-wrap .slider a span { display:block; }
#${sliderIdentifier}.slider-wrap input.value { border:1px solid #CCC; width:42px; float:left; padding:2px; font-size:11px; margin-top:20px; }
#${sliderIdentifier}.slider-wrap .dragdealer { width:340px; position:relative; height:3px; background:url('model/images/non-linear-scale.png') repeat-y top left; position:relative; }
#${sliderIdentifier}.slider-wrap .dragdealer .handle { position:absolute; top:-9px; cursor:pointer; width:12px; height:23px; background-repeat:no-repeat; background-position:top left; }
#${sliderIdentifier}.slider-wrap .dragdealer .handle.gray { background-image:url('model/images/slider-gray.gif'); }
</style>

<div id="${sliderIdentifier}" class="slider-wrap">
  <div class="slider">
    <a style="margin-left:3px;" title="1"><span>1<sup>&nbsp;</sup></span>|</a>
    <a style="margin-left:43px;" title="1e-2"><span>10<sup>-2</sup></span>|</a>
    <a style="margin-left:12px;" title="1e-3"><span>10<sup>-3</sup></span>|</a>
    <a style="margin-left:11px;" title="1e-4"><span>10<sup>-4</sup></span>|</a>
    <a style="margin-left:11px;" title="1e-5"><span>10<sup>-5</sup></span>|</a>
    <a style="margin-left:10px;" title="1e-6"><span>10<sup>-6</sup></span>|</a>
    <a style="margin-left:11px;" title="1e-7"><span>10<sup>-7</sup></span>|</a>
    <a style="margin-left:11px;" title="1e-8"><span>10<sup>-8</sup></span>|</a>
    <a style="margin-left:11px;" title="1e-9"><span>10<sup>-9</sup></span>|</a>
    <div style="clear:both;"></div>
    <div id="${sliderIdentifier}-slider" class="dragdealer">
      <div class="handle gray"></div>
    </div>
  </div>
  <input type="text" class="value" value="${defaultValue}" autocomplete="off">
  <div style="clear:both;"></div>
</div>

<script type="text/javascript">
(function() {
  <%-- init the slider --%>
  new Dragdealer('${sliderIdentifier}-slider', {callback: function() {
    <%-- derive value from slider --%>
    var handle = jQuery("#${sliderIdentifier}.slider-wrap #${sliderIdentifier}-slider div.handle");
    jQuery("#${sliderIdentifier}.slider-wrap input.value").val(function() {
      <%-- call a log that something has updated --%>
      if (typeof geneExpressionAtlasDisplayer == 'object') {
        geneExpressionAtlasDisplayer.settingsUpdated();
      }

      var distance = handle.css('left').replace(/[^0-9.]/g, '');
      var width = handle.css('width').replace(/[^0-9.]/g, '');
      var total = handle.parent().css('width').replace(/[^0-9.]/g, '');

      var value = 1 - ((total - distance - width) / (total - width));

      // non linear
      var p = new Number((1/Math.pow(10, value * 10)).toPrecision(21)).toExponential(2);
      return p;
    });
  }
  });

  adjustSliderPosition();
  <%-- derive slider position from value --%>
  function adjustSliderPosition() {
    Math.log10 = function(arg) {
      return Math.log(arg) / Math.LN10;
    };

    jQuery("#${sliderIdentifier}-slider div.handle").css('left', function() {
      var pValue = jQuery("#${sliderIdentifier}.slider-wrap input.value").val();
      if (!isNaN(parseFloat(pValue)) && isFinite(pValue) && pValue >= 0 && pValue <= 1) {
        <%-- call a log that something has updated --%>
        if (typeof geneExpressionAtlasDisplayer == 'object') {
          geneExpressionAtlasDisplayer.settingsUpdated();
        }

        var width = jQuery("#${sliderIdentifier}-slider div.handle").css('width').replace(/[^0-9]/g, '');
        var total = jQuery("#${sliderIdentifier}-slider").css('width').replace(/[^0-9]/g, '');

        // non linear (http://www.wolframalpha.com/input/?i=p+%3D+10^%28-10x%29)
        var value = new Number(pValue).toExponential().toString();
        var x = - (Math.log(pValue)) / (10 * Math.log(10));

        if (x > 1) x = 1;

        return (x * (total - width)) + "px";
      } else {
        alert('The ${sliderIdentifier} needs to be between 0 and 1');
      }
    });
  }

  <%-- update the slider on input manual change --%>
  jQuery("#${sliderIdentifier}.slider-wrap input.value")
  .focusout(adjustSliderPosition)
  .bind('keypress', function(e) {
    if (e.keyCode == 13) adjustSliderPosition();
  });

  <%-- key points on the scale --%>
  jQuery("#${sliderIdentifier}.slider-wrap div.slider a").click(function() {
    jQuery("#${sliderIdentifier}.slider-wrap input.value").val(jQuery(this).attr('title'));
    adjustSliderPosition();
  });
})();
</script>