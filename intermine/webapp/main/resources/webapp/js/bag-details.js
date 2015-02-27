// toggleToolBarMenu comes from toolbar.js

function toggleDescription () {
  jQuery('#bagDescriptionDiv').toggle();
  jQuery('#bagDescriptionTextarea').toggle();
  jQuery('#textarea').focus();
}

function cancelEditBagDescription () {
  jQuery('#bagDescriptionTextarea').toggle();
  jQuery('#bagDescriptionDiv').toggle();
  return false;
}

jQuery(function() {
  jQuery(".tb_button").click(function () {
    toggleToolBarMenu(this);
  });
  AjaxServices.getToggledElements(function(elements){
    var prefix = '#widgetcontainer';
    console.log(elements);
    elements.filter(function (e) { return !e.open })
            .forEach(function (e) { jQuery(prefix + e.id).hide(); });
  });
  jQuery('body').on('click', '#toggle-widgets a.widget-toggler', function (e) {
    e.preventDefault();

    // Toggle us.
    var link = jQuery(e.target);
    link.toggleClass('inactive');

    // Toggle widget.
    var widgetId = link.attr('data-widget');
    var w = jQuery('#' + widgetId + '-widget');
    w.toggle();

    // Save.
    AjaxServices.saveToggleState(widgetId, w.is(":visible"));
  });
});
