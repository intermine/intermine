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
#${sliderIdentifier}.slider-wrap input.value { border:1px solid #CCC; width:60px; float:left; padding:2px; font-size:11px; margin-top:20px; }
#${sliderIdentifier}.slider-wrap .dragdealer { width:340px; position:relative; height:3px; background:url('model/images/linear-scale.png') repeat-y top left; position:relative; }
#${sliderIdentifier}.slider-wrap .dragdealer .handle { position:absolute; top:-9px; cursor:pointer; width:12px; height:23px; background-repeat:no-repeat; background-position:top left; }
#${sliderIdentifier}.slider-wrap .dragdealer .handle.blue { background-image:url('model/images/slider-blue.gif'); }
#${sliderIdentifier}.slider-wrap .dragdealer .handle.green { background-image:url('model/images/slider-green.gif'); }
</style>

<div id="${sliderIdentifier}" class="slider-wrap">
  <div class="slider">
    <a style="margin-left:3px;" title="100"><span>100</span>|</a>
    <a style="margin-left:11px;" title="80"><span>80</span>|</a>
    <a style="margin-left:11px;" title="60"><span>60</span>|</a>
    <a style="margin-left:10px;" title="40"><span>40</span>|</a>
    <a style="margin-left:11px;" title="20"><span>20</span>|</a>
    <a style="margin-left:11px;" title="0"><span>0</span>|</a>
    <a style="margin-left:11px;" title="-20"><span>-20</span>|</a>
    <a style="margin-left:11px;" title="-40"><span>-40</span>|</a>
    <a style="margin-left:10px;" title="-60"><span>-60</span>|</a>
    <a style="margin-left:11px;" title="-80"><span>-80</span>|</a>
    <div style="clear:both;"></div>
    <div id="${sliderIdentifier}-slider" class="dragdealer">
      <div class="handle green"></div>
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
      var distance = handle.css('left').replace(/[^0-9.]/g, '');
      var width = handle.css('width').replace(/[^0-9.]/g, '');
      var total = handle.parent().css('width').replace(/[^0-9.]/g, '');

      var value = (((total - distance - width) / (total - width)) * 200) - 100;

      if (value < 0) {
          jQuery("#${sliderIdentifier}-slider div.handle").removeClass('green').addClass('blue');
        } else {
          jQuery("#${sliderIdentifier}-slider div.handle").removeClass('blue').addClass('green');
      }

      // linear
      return new Number(value).toFixed(parseInt(2)); // rounded value to 2 decimal places
    });
  }
  });

  adjustSliderPosition();
  <%-- derive slider position from value --%>
  function adjustSliderPosition() {
    jQuery("#${sliderIdentifier}-slider div.handle").css('left', function() {
      var pValue = jQuery("#${sliderIdentifier}.slider-wrap input.value").val();
      if (!isNaN(parseFloat(pValue)) && isFinite(pValue)) {

        var width = jQuery("#${sliderIdentifier}-slider div.handle").css('width').replace(/[^0-9]/g, '');
        var total = jQuery("#${sliderIdentifier}-slider").css('width').replace(/[^0-9]/g, '');

        pValue = parseFloat(pValue) + 100;
        if (pValue < 0)  {
            pValue = 0;
        } else {
          if (pValue > 200)  {
              pValue = 200;
          }
        }
        pValue = pValue / 200;

        if (pValue < 0.5) {
          jQuery(this).removeClass('green').addClass('blue');
        } else {
          jQuery(this).removeClass('blue').addClass('green');
        }

        // linear
        return ((1 - pValue) * (total - width)) + "px";
      } else {
        alert('The ${sliderIdentifier} needs to be between -100 and 100');
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