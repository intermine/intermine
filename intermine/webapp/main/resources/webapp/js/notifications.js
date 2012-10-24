(function($, Backbone) {
	
	var Notification = Backbone.View.extend( {
        tagName: 'div',
        className: 'im-event-notification topBar messages',
        events: {
            'click a.closer': 'close'
        },
        title: 'Success:',
        close: function() {
            var self = this;
            this.$el.hide('slow', function() {self.remove()});
        },
        render: function() {
            var self = this, remAfter = self.options.autoRemove;
            this.$el.append('<a class="closer" href="#">Hide</a>');
            this.$el.append('<p><span><b>' + this.title + '</b></span></p>');
            
            this.appendContent();
            this.$el.prependTo('#pagecontentcontainer');
            
            if (remAfter != null) {
                _.delay(function() {self.close();}, (remAfter === true) ? 3000 : remAfter);
            }
            return this;
        },
        appendContent: function() {
        	 this.$el.append(this.options.message);
        }
    } );
	
	var FailureNotification = Notification.extend( {
        className: "im-event-notification topBar errors",
        title: 'Oops!'
    } );
	
	this.Notification = Notification;
	this.FailureNotification = FailureNotification;
	
}).call(window, jQuery, Backbone);