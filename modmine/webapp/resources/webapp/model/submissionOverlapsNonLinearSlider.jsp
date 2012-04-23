<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<tiles:importAttribute name="sliderIdentifier" ignore="false" />
<tiles:importAttribute name="defaultValue" ignore="false" />

<script type="text/javascript" src="js/dragdealer.js"></script>

<style>
#${sliderIdentifier}.slider-wrap .slider { display:inline-block; padding:6px 2px; margin-top:1px; }
#${sliderIdentifier}.slider-wrap .slider a { font-size:10px; float:left; cursor:pointer; width:30px; }
#${sliderIdentifier}.slider-wrap .slider a span { display:block; }
#${sliderIdentifier}.slider-wrap input.value { border:1px solid #CCC; width:56px; float:left; padding:2px; font-size:11px; margin-top:18px;
  vertical-align:top; }
#${sliderIdentifier}.slider-wrap .dragdealer { width:340px; position:relative; height:3px; background:url('model/images/non-linear-scale.png') repeat-y top left;
  position:relative; -webkit-border-radius: 3px; -moz-border-radius: 3px; border-radius: 3px; border:1px solid #CCC; }
#${sliderIdentifier}.slider-wrap .dragdealer .handle { position:absolute; top:-9px; cursor:pointer; width:9px; height:20px; background: rgb(237,237,237);
  background: -moz-linear-gradient(top, rgba(237,237,237,1) 0%, rgba(241,241,241,1) 100%);
  background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,rgba(237,237,237,1)), color-stop(100%,rgba(241,241,241,1)));
  background: -webkit-linear-gradient(top, rgba(237,237,237,1) 0%,rgba(241,241,241,1) 100%);
  background: -o-linear-gradient(top, rgba(237,237,237,1) 0%,rgba(241,241,241,1) 100%);
  background: -ms-linear-gradient(top, rgba(237,237,237,1) 0%,rgba(241,241,241,1) 100%);
  filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#ededed', endColorstr='#f1f1f1',GradientType=0 );
  background: linear-gradient(top, rgba(237,237,237,1) 0%,rgba(241,241,241,1) 100%);
  border: 1px solid #BFBFBF; -webkit-border-radius: 8px; -webkit-border-bottom-right-radius: 4px; -webkit-border-bottom-left-radius: 4px;
  -moz-border-radius: 8px; -moz-border-radius-bottomright: 4px; -moz-border-radius-bottomleft: 4px; border-radius: 8px; border-bottom-right-radius: 4px;
  border-bottom-left-radius: 4px; }
#${sliderIdentifier}.slider-wrap .dragdealer .handle:hover { border-color:#999; background: rgb(228,228,228);
  background: -moz-linear-gradient(top, rgba(228,228,228,1) 0%, rgba(234,234,234,1) 100%);
  background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,rgba(228,228,228,1)), color-stop(100%,rgba(234,234,234,1)));
  background: -webkit-linear-gradient(top, rgba(228,228,228,1) 0%,rgba(234,234,234,1) 100%);
  background: -o-linear-gradient(top, rgba(228,228,228,1) 0%,rgba(234,234,234,1) 100%);
  background: -ms-linear-gradient(top, rgba(228,228,228,1) 0%,rgba(234,234,234,1) 100%);
  filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#e4e4e4', endColorstr='#eaeaea',GradientType=0 );
  background: linear-gradient(top, rgba(228,228,228,1) 0%,rgba(234,234,234,1) 100%); }
</style>

<div id="${sliderIdentifier}" class="slider-wrap">
  <input type="text" class="value" value="${defaultValue}" autocomplete="off">
  <div class="slider">
    <!-- <a style="margin-left:16px;" title="500bp"><span>500</span>|</a> -->
    <a style="margin-left:64px;" title="1kbp"><span>1k</span>|</a>
    <!-- <a style="margin-left:12px;" title="5kbp"><span>5k</span>|</a> -->
    <a style="margin-left:36px;" title="10kbp"><span>10k</span>|</a>
    <!-- <a style="margin-left:40px;" title="50kbp"><span>50k</span>|</a> -->
    <a style="margin-left:36px;" title="100kbp"><span>100k</span>|</a>
    <!-- <a style="margin-left:45px;" title="500kbp"><span>500k</span>|</a> -->
    <a style="margin-left:37px;" title="1Mbp"><span>1M</span>|</a>
    <!-- <a style="margin-left:45px;" title="5Mbp"><span>5M</span>|</a> -->
    <a style="margin-left:39px;" title="10Mbp"><span>10M</span>|</a>
    <div style="clear:both;"></div>
    <div id="${sliderIdentifier}-slider" class="dragdealer">
      <div class="handle gray"></div>
    </div>
  </div>
  <div style="clear:both;"></div>
</div>

<script type="text/javascript">
  // dragdeal and ajax issue?
  timer();
  function timer()
  {
      var t=setTimeout("initDragDealer()", 1);
  }

  function initDragDealer() {
    <%-- init the slider --%>
    new Dragdealer('${sliderIdentifier}-slider', {callback: function() {
        <%-- derive value from slider --%>
        var handle = jQuery("#${sliderIdentifier}.slider-wrap #${sliderIdentifier}-slider div.handle");
        jQuery("#${sliderIdentifier}.slider-wrap input.value").val(function() {
            var distance = handle.css('left').replace(/[^0-9.]/g, '');
            var width = handle.css('width').replace(/[^0-9.]/g, '');
            var total = handle.parent().css('width').replace(/[^0-9.]/g, '');

            var value = 1 - ((total - distance - width) / (total - width));

            var e = 0;
            if (value != 0) {
                // region extension, the function is the solution of the function derived by curve fitting
                e = Math.round(Math.pow(Math.E, (value + 0.445)/0.089) - 143.57);
            }

            jQuery( "#distance" ).val(e);

            if (e >= 1000 && e < 1000000) {
                e = roundWithPrecision(e/1000, 2) + "k";
            } else if (e >= 1000000) {
                e = roundWithPrecision(e/1000000, 2) + "M"
            }

            return e;
        });
      }
    });
  }

  adjustSliderPosition();
  <%-- derive slider position from value --%>
  function adjustSliderPosition() {
    jQuery("#${sliderIdentifier}-slider div.handle").css('left', function() {
      var eValue = jQuery.trim(jQuery("#${sliderIdentifier}.slider-wrap input.value").val());

      // regular expression - ^((\d+)|(\d*[0-9](\.\d*[0-9])?(k|K|m|M|g|G)))(b|bp)?$
      var reg = new RegExp("^((\\d+)|(\\d*[0-9](\\.\\d*[0-9])?(k|K|m|M|g|G)))(b|bp)?$");
      if (reg.test(eValue)) {
         var number = new Number(eValue.match(/^\d*[0-9](\.\d*[0-9])?/g));

         if (eValue.search(/k/gi) != -1) {
             number = number * 1000;
         } else if (eValue.search(/m/gi) != -1) {
             number = number * 1000000;
         } else if (eValue.search(/g/gi) != -1) {
             number = number * 1000000000;
         }
      } else {
          alert('Please input a value such as 100, 1.2k or 2M');
          jQuery("input#${sliderIdentifier}-input-box").val(0);
          adjustSliderPosition();
      }

      eValue = number;
      jQuery( "#distance" ).val(eValue);
      if (!isNaN(parseFloat(eValue)) && isFinite(eValue) && eValue >= 0) {
        var width = jQuery("#${sliderIdentifier}-slider div.handle").css('width').replace(/[^0-9]/g, '');
        var total = jQuery("#${sliderIdentifier}-slider").css('width').replace(/[^0-9]/g, '');

        // non linear function (y = 0.089* log(x+143.57)-0.445) to find the postion of handle for
        // fixed values, the function was found by curve fitting on zunzun.com, data: {(0, 0),
        // (500, 0.1), (1000, 0.2), (5000, 0.3), (10000, 0.4), (50000, 0.5), (100000, 0.6),
        // (500000, 0.7), (1000000, 0.8), (5000000, 0.9), (10000000, 1.0)}, the function is
        // y = a*ln(x+b) + Offset, Fitting target of lowest sum of squared absolute error =
        // 4.4265964131639966E-03, a =  8.8815733291768229E-02, b =  1.4356968547713012E+02
        // Offset = -4.4519524991486498E-01
        var x = 0.089 * Math.log(new Number(eValue) + 143.57) - 0.445;
        x = roundWithPrecision(x, 2); // round to the second place after decimal point
        if (x > 1) x = 1;
        return (x * (total - width)) + "px";
      }
    });
  }

  // Round to a given place (precision) after decimal point
  function roundWithPrecision(value, precision)
  {
     value  =  Math.round(value*Math.pow(10, precision))/Math.pow(10, precision);
     return value;
  }

  <%-- update the slider on input manual change --%>
  jQuery("#${sliderIdentifier}.slider-wrap input.value")
  .focusout(adjustSliderPosition)
  .bind('keypress', function(e) {
    if (e.keyCode == 13) {
      adjustSliderPosition();
      return false;
    }
  });

  <%-- key points on the scale --%>
  jQuery("#${sliderIdentifier}.slider-wrap div.slider a").click(function() {
    jQuery("#${sliderIdentifier}.slider-wrap input.value").val(jQuery(this).attr('title'));
    adjustSliderPosition();
  });
</script>