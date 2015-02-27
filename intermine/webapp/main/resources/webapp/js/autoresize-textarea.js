jQuery(function () {
  // textarea resizer
  jQuery('#textarea').autoResize({
    // on resize:
    onResize : function() {
      jQuery(this).css({opacity:0.8});
    },
    // after resize:
    animateCallback : function() {
      jQuery(this).css({opacity:1});
    },
    // quite slow animation:
    animateDuration : 300,
    // more extra space:
    extraSpace : 10
  });
});

