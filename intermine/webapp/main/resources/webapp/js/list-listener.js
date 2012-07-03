(function($, Backbone) {
    var ListNotification = Backbone.View.extend( {
        tagName: 'div',
        className: 'im-list-event-notification topBar',
        events: {
            'click a.closer': 'close'
        },
        close: function() {
            var self = this;
            this.$el.hide('slow', function() {self.remove()});
        },
        render: function() {
            this.$el.append('<a class="closer" href="#">Hide</a>');
            this.$el.append('<p><span><b>' + this.title + '</b></span></p>');
            
            this.appendContent();
            return this;
        }
    } );

    var SuccessNotification = ListNotification.extend( {
        className: "im-list-event-notification topBar messages",
        title: 'Success:',
        appendContent: function() {
            if (this.options.change == null) {
                this.notifyOfCreation();
            } else {
                this.notifyOfUpdate();
            }
        },
        addLinkToList: function() {
            var a = $('<a>');
            a.text(this.options.list.name);
            a.attr('href', this.options.list.service.root.replace('service/', 'bagDetails.do?bagName=' + this.options.list.name));
            this.$('p').append(a);
        },
        notifyOfCreation: function() {
            this.$('p').append('<span>Created a new list</span>');
            this.addLinkToList();
            this.$('p').append('<span>( ' + this.options.list.size + ' ' + this.options.list.type + 's)</span>');
        },
        notifyOfUpdate: function() {
            this.addLinkToList();
            this.$('p').append(' <span>successfully updated.</span> ');
            this.$('p').append(((this.options.change > 0) ? 'Added' : 'Removed') + ' ');
            this.$('p').append(Math.abs(this.options.change) + ' items.');
        }
    } );

    var FailureNotification = ListNotification.extend( {
        className: "im-list-event-notification topBar errors",
        title: 'Oops!',
        appendContent: function() {
            this.$el.append(this.options.message);
        }
    } );

    var failuriser = function(msg) {
        var notification = new FailureNotification({message: msg});
        notification.render().$el.prependTo('#pagecontentmax');
    };
    var successifier = function(list, change) {
        var notification = new SuccessNotification({list: list, change: change});
        notification.render().$el.prependTo('#pagecontentmax');
    };

    this.LIST_EVENTS = {
        "list-creation:success": successifier,
        "list-creation:failure": failuriser,
        "list-update:success": successifier,
        "list-update:failure": failuriser
    };
}).call(window, jQuery, Backbone);
