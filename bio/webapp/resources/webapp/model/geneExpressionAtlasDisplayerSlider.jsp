<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<tiles:importAttribute name="defaultPValue" ignore="false" />

<script type="text/javascript" src="js/dragdealer.js"></script>

<style>
.slider { float:right; padding:6px 2px; margin-top:1px; }
.slider-wrap input.value { border:1px solid #CCC; width:60px; float:left; padding:2px; font-size:11px; }
.dragdealer { width:360px; position:relative; height:3px; background:url('model/images/scale.png') repeat-y top left; position:relative; }
.dragdealer .handle { position:absolute; top:-9px; cursor:pointer; width:12px; height:23px; background-repeat:no-repeat; background-position:top left; }
.dragdealer .handle.blue { background-image:url('model/images/slider-blue.gif'); }
.dragdealer .handle.green { background-image:url('model/images/slider-green.gif'); }
</style>

<div class="slider-wrap">
  <div class="slider">
    <div id="slider" class="dragdealer">
      <div class="handle"></div>
    </div>
  </div>
  <input type="text" class="value" value="${defaultPValue}" autocomplete="off">
  <div style="clear:both;"></div>
</div>

<script type="text/javascript">
(function() {
  <%-- init the slider --%>
  new Dragdealer('slider');

  adjustSliderPosition();
  <%-- adjust position of handle based on input --%>
  function adjustSliderPosition() {
    jQuery("#slider div.handle").css('left', function() {
      var pValue = jQuery("div.slider-wrap input.value").val();
      if (!isNaN(parseFloat(pValue)) && isFinite(pValue) && pValue >= 0 && pValue <= 1) {
        updateSliderColor();

        var width = jQuery("#slider div.handle").css('width').replace(/[^0-9]/g, '');
        var total = jQuery("#slider").css('width').replace(/[^0-9]/g, '');

        // linear slider
        return (pValue * (total - width)) + "px";
      } else {
        alert('The p value needs to be between 0 and 1');
      }
    });
  }

  <%-- switch between handle colors --%>
  function updateSliderColor() {
    if (jQuery("div.slider-wrap input.value").val() < 0.625) {
      jQuery("#slider div.handle").addClass('green').removeClass('blue');
    } else {
      jQuery("#slider div.handle").addClass('blue').removeClass('green');
    }
  }

  <%-- update the slider on input manual change --%>
  jQuery("div.slider-wrap input.value")
  .focusout(adjustSliderPosition)
  .bind('keypress', function(e) {
    if (e.keyCode == 13) adjustSliderPosition();
  });

  <%-- update input on handle mouse --%>
  jQuery("#slider div.handle").mousemove(function(event) {
    var handle = jQuery(this);
    jQuery("div.slider-wrap input.value").val(function() {
      var distance = handle.css('left').replace(/[^0-9.]/g, '');
      var width = handle.css('width').replace(/[^0-9.]/g, '');
      var total = handle.parent().css('width').replace(/[^0-9.]/g, '');

      updateSliderColor();

      var value = 1 - ((total - distance - width) / (total - width));

      // linear
      // return new Number(value).toFixed(parseInt(2)); // rounded value to 2 decimal places

      // non linear
      return new Number((1/Math.pow(10, value * 10)).toPrecision(25)).toExponential(2);
    });
  });
})();
</script>