(function($, Backbone) {
    'use strict';
	
    if (typeof this.console === 'undefined') {
        this.console = {log: function() {}};
    }
    if (typeof this.console.error === 'undefined') {
        this.console.error = this.console.log;
    }

  var canShow = true;

  window.addEventListener('beforeunload', function () {
    canShow = false;
    return;
  });

	var Notification = Backbone.View.extend( {
        tagName: 'div',
        className: 'im-event-notification topBar messages',
        title: 'Success:',
        events: {
            'click a.closer': 'close'
        },
        initialize: function (options) {
          this.options = (options || {});
          _.bindAll(this);
        },
        close: function() {
            var self = this;
            this.$el.hide('slow', function() {self.remove()});
        },
        render: function() {
            if (!canShow) return;
            var self = this
              , remAfter = self.options.autoRemove;
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

    /**
     * Static factory method for notifying of messages.
     * @param {string} message The message to show to the user.
     */
    Notification.notify = function(message) {
        new Notification({message: message}).render();
    }

    var lastError = 0;
    /**
     * Static factory method for handling errors.
     * In addition to showing the user a notification, the message
     * will also be logged to the console if one is available.
     * @param {?string} error The message to show to the user.
     */
    FailureNotification.notify = function(error) {
        if (console) {
            (console.error || console.log).apply(console, arguments);
        }
        if (error && error.status === 0) return; // Aborted.
        var now = new Date().getTime();
        var sinceLast = now - lastError;
        lastError = now;
        if (sinceLast < 1000) return; // Too many
        if (error == null) {
            error = "Unknown error";
        }
        new FailureNotification({message: error, autoRemove: true}).render();
    };

	this.Notification = Notification;
	this.FailureNotification = FailureNotification;
	
}).call(window, jQuery, Backbone);
