<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<tiles:importAttribute name="sliderIdentifier" ignore="false" />
<tiles:importAttribute name="defaultValue" ignore="false" />

<script type="text/javascript" src="js/dragdealer.js"></script>

<style>
#${sliderIdentifier}.slider-wrap { width:410px; }
#${sliderIdentifier}.slider-wrap .slider { display:inline-block; padding:6px 2px; margin-top:1px; width:344px; }
#${sliderIdentifier}.slider-wrap .slider a { font-size:10px; float:left; cursor:pointer; width:22px; }
#${sliderIdentifier}.slider-wrap .slider a span { display:block; }
#${sliderIdentifier}.slider-wrap input.value { border:1px solid #CCC; width:56px; float:left; padding:2px; font-size:11px; margin-top:16px;
  vertical-align:top; }
#${sliderIdentifier}.slider-wrap .dragdealer { width:340px; position:relative; height:3px; background:url('model/images/linear-scale.png') repeat-y top left;
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
    <a style="margin-left:4px;" title=""><span></span>|</a>
    <a style="margin-left:11px;" title=""><span></span>|</a>
    <a style="margin-left:11px;" title=""><span></span>|</a>
    <a style="margin-left:11px;" title=""><span></span>|</a>
    <a style="margin-left:11px;" title=""><span></span>|</a>
    <a style="margin-left:11px;" title=""><span></span>|</a>
    <a style="margin-left:11px;" title=""><span></span>|</a>
    <a style="margin-left:12px;" title=""><span></span>|</a>
    <a style="margin-left:11px;" title=""><span></span>|</a>
    <a style="margin-left:11px;" title=""><span></span>|</a>
    <a style="margin-left:10px; width:10px;" title=""><span></span>|</a>
    <div style="clear:both;"></div>
    <div id="${sliderIdentifier}-slider" class="dragdealer">
      <div class="handle gray"></div>
    </div>
  </div>
  <div style="clear:both;"></div>
</div>

<script type="text/javascript">
geneExpressionAtlasDiseasesDisplayer.dragdealers.${sliderIdentifier} = {};
geneExpressionAtlasDiseasesDisplayer.dragdealers.${sliderIdentifier}.init = function() {
    <%-- fill in the t-stat values based on the absolute maximum of the source expressions --%>
    var maxValue = geneExpressionAtlasDiseasesDisplayer.peaks.global += 10 - (geneExpressionAtlasDiseasesDisplayer.peaks.global % 10),
        piece = maxValue / 10;
    jQuery('#${sliderIdentifier} div.slider a').each(function() {
      jQuery(this).attr('title', maxValue).find('span').text(maxValue);
      maxValue -= piece;
    });
    maxValue = 10 * piece;

    <%-- init the slider --%>
    new Dragdealer('${sliderIdentifier}-slider', {callback: function() {
      <%-- derive value from slider --%>
      var handle = jQuery("#${sliderIdentifier}.slider-wrap #${sliderIdentifier}-slider div.handle");
      jQuery("#${sliderIdentifier}.slider-wrap input.value").val(function() {
        <%-- call a log that something has updated --%>
        if (typeof geneExpressionAtlasDiseasesDisplayer == 'object') {
          geneExpressionAtlasDiseasesDisplayer.settingsUpdated();
        }

        var distance = handle.css('left').replace(/[^0-9.]/g, '');
        var width = handle.css('width').replace(/[^0-9.]/g, '');
        var total = handle.parent().css('width').replace(/[^0-9.]/g, '');

        var value = (total - distance - width) / (total - width) * maxValue;

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
        if (!isNaN(parseFloat(pValue)) && isFinite(pValue) && pValue <= maxValue && pValue >= 0) {
          <%-- call a log that something has updated --%>
          if (typeof geneExpressionAtlasDiseasesDisplayer == 'object') {
            geneExpressionAtlasDiseasesDisplayer.settingsUpdated();
          }

          var width = jQuery("#${sliderIdentifier}-slider div.handle").css('width').replace(/[^0-9]/g, '');
          var total = jQuery("#${sliderIdentifier}-slider").css('width').replace(/[^0-9]/g, '');

          // linear
          return ((1 - (parseFloat(pValue) / maxValue)) * (total - width) + "px");
        } else {
          alert('The ${sliderIdentifier} needs to be between 0 and ' + maxValue);
        }
      });
    }

    <%-- update the slider on input manual change --%>
    jQuery("#${sliderIdentifier}.slider-wrap input.value")
    .focusout(adjustSliderPosition)
    .bind('keypress', function(e) {
      if (e.keyCode == 13) {
        adjustSliderPosition();
        jQuery("#gene-expression-atlas-diseases div.settings input.update").click();
      }
    });

    <%-- key points on the scale --%>
    jQuery("#${sliderIdentifier}.slider-wrap div.slider a").click(function() {
      jQuery("#${sliderIdentifier}.slider-wrap input.value").val(jQuery(this).attr('title'));
      adjustSliderPosition();
    });
};
</script>