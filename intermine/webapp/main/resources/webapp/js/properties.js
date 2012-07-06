(function($) {
	$(function() {
		$('.current-value').click(function(e) {
			$(this).next().toggle();
		});
	});
}).call(window, jQuery);