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

var Remover, ListOperations;

(function () {
  if (Backbone && _) {

    Remover = Backbone.View.extend({

      initialize: function (options) {
        this.model = new Backbone.Model({active: false});
        this.bag = options.bag;
        this.tableModel = options.tableModel;
        this.selection = options.selection;
        this.listenTo(this.model, 'change', this.render);
        this.listenTo(this.selection, 'add remove reset', this.render);
      },

      template: _.template(
        '<h3 class="goog" style="cursor: pointer">' +
          '<i class="fa fa-caret-<%= active ? "down" : "right" %>"></i> ' +
          'Remove Items' +
        '</h3>' +
        '<div class="grid medium-grid-full grid-fit imtables" ' +
             '<% if (!active) { %><%= hidden %><% } %>>' +
          '<div class="grid-cell">' +
          ' <button class="btn btn-default stop-removing">Cancel</button>' +
          '</div><div class="grid-cell">' +
          ' <button class="<% if (nothingSelected) { %>disabled <% } %>' +
                          'btn btn-default remove-items">' +
            'Remove selected' +
           '</button>' +
          '</div>' +
        '</div>'
      ),

      data: function () {
        return _.extend({
          nothingSelected: this.selection.isEmpty(),
          hidden: 'style="display:none"'
        }, this.model.toJSON());
      },

      events: function () {
        return {
          'click h3': this.startRemoving,
          'click .stop-removing': this.stopRemoving,
          'click .remove-items': this.removeItems
        };
      },

      removeItems: function (e) {
        var ids = this.selection.pluck('id');
        var bagName = this.bag.name;
        if (ids.length) {
          AjaxServices.removeIdsFromBag(bagName, ids, {
            callback: function (delta) {
              if (delta > 0) {
                // This is terrible for performance, but it ensures correctness.
                location.reload();
              } else {
                LIST_EVENTS.failure(new Error("No items were removed"));
              }
            },
            errorHandler: LIST_EVENTS.failure
          });
        }
        this.stopRemoving(e);
      },

      startRemoving: function (e) {
        this.model.set({active: true});
        this.tableModel.set({selecting: true});
        this.selection.state.set({node: this.bag.type});
      },

      stopRemoving: function (e) {
        this.model.set({active: false});
        this.tableModel.set({selecting: false});
        this.selection.state.set({node: null});
        this.selection.reset();
      },

      render: function () {
        this.$el.html(this.template(this.data()));
        return this;
      }

    });

    var OperationsModel = Backbone.Model.extend({
      defaults: function () {
        return {combineWith: null, operation: 'copy', newName: null, open: false};
      }
    });

    ListOperations = Backbone.View.extend({

      initialize: function (options) {
        var bag = this.bag = options.bag;
        var m = this.model = new OperationsModel();
        var coll = this.compatibleBags = new Backbone.Collection();

        this.listenTo(coll, 'add remove reset', this.render);
        this.listenTo(this.model, 'change', this.render);

        Q.all([$SERVICE.fetchModel(), $SERVICE.fetchLists()])
         .then(function (resolutions) {
          var model = resolutions[0], lists = resolutions[1];
          lists.forEach(function (list) {
            if ((list.name !== bag.name) &&
              (model.makePath(list.type).isa(bag.type) ||
               model.makePath(bag.type).isa(list.type))) {
              coll.add(_.pick(list, 'name', 'type'));
            }
          });
          if (coll.length) {
            m.set({combineWith: coll.first().get('name')});
          }
        }, function (error) {
          console.error("Could not fetch lists", error);
        });
      },

      template: _.template(
        '<h3 class="goog" style="cursor: pointer">' +
          '<i class="fa fa-caret-<%= state.open ? "down" : "right" %>"></i> ' +
          'List Operations' +
        '</h3>' +
        '<div class="imtables" style="display: <%= state.open ? "block" : "none" %>">' +
        '<% if ("copy" !== state.operation && "delete" !== state.operation) { %>' +
          '<% if (compatibleBags.length) { %>' +
            '<div class="form-group">' +
              '<label>' +
                '<%- state.operation %> ' +
                '<%= (state.operation === "subtract") ? "from" : "with" %>:' +
              '</label>' +
              '<select class="form-control compatible-bags">' +
              ' <% _.each(compatibleBags, function (bag) { %>' +
              '   <option <%= bag.name === state.combineWith ? "selected" : void 0 %> ' +
                          'value="<%- bag.name %>"><%- bag.name %></option>' +
              ' <% }); %>' +
              '</select>' +
            '</div>' +
          '<% } else { %>' +
            '<div class="alert alert-warning"><p>No compatible lists</p></div>' +
          '<% } %>' +
        '<% } %>' +
        '<div class="form-group">' +
          '<label>New list name:</label>' +
          '<input class="form-control new-name" placeholder="enter a name" type="text" value="<%- state.newName %>">' +
        '</div>' +
        '<div class="btn-group">' +
        ' <button class="btn btn-default act"><%- state.operation %></button>' +
        ' <button class="btn btn-default dropdown-toggle" data-toggle="dropdown">' +
        '   <span class="caret"></span>' +
        ' </button>' +
        ' <ul class="dropdown-menu" role="menu">' +
          '<% _.each(operations, function (op) { %>' +
            '<% if (op !== state.operation) { %>' +
              '<li><a class="<%- op %>"><%- op %></a></li>' +
            '<% } %>' +
          '<% }); %>' +
        '</div>'
      ),

      events: function () {
        var es = {
          'change .new-name': this.updateName,
          'change .compatible-bags': this.updateCombineWith,
          'click .act': this.act,
          'click h3': function () { this.model.set({open: !this.model.get('open')}); }
        };
        ListOperations.operations.forEach(function (op) {
          es['click .' + op] = function () { this.model.set({operation: op}); };
        });
        return es;
      },

      updateCombineWith: function (e) {
        this.model.set({combineWith: this.$('.compatible-bags').val()});
      },

      act: function (e) {
        var op = this.model.get('operation');
        if (op === 'delete') {
          return this.deleteList();
        }
        if (!this.model.get('newName')) {
          return LIST_EVENTS.failure(new Error('Name is required'));
        }
        if (op === 'copy') {
          return this.copyList();
        } else if (op === 'intersect') {
          return this.combineLists('intersect');
        } else if (op === 'combine') {
          return this.combineLists('merge');
        } else if (op === 'subtract') {
          return this.subtractList();
        } else {
          throw new Error("Unknown operation: " + op);
        }
      },

      subtractList: function () {
        if (!this.model.get('combineWith')) {
          return LIST_EVENTS.failure(new Error('Must choose a list to subtract from'));
        }
        var fromList = this.model.get('combineWith');
        var listDetails = this.getListDetails();
        listDetails.from = [fromList];
        listDetails.exclude = [this.bag.name];
        listDetails.description = 'Contents of ' + fromList + ' excluding ' +
          this.bag.type + 's in ' + this.bag.name;
        $SERVICE.complement(listDetails).then(LIST_EVENTS.success, LIST_EVENTS.failure);
      },

      deleteList: function () {
        var name = this.bag.name;
        var warning = "Do you really want to delete " + name + "? This cannot be undone.";
        if (window.confirm(warning)) {
          this.bag.del().then(
              function () {window.location.href = './bag.do?subtab=view';},
              LIST_EVENTS.failure
          );
        }
      },

      combineLists: function (meth) { 
        if (!this.model.get('combineWith')) {
          return LIST_EVENTS.failure(new Error('Must choose a list to combine with'));
        }
        var listDetails = this.getListDetails();
        listDetails.lists = [this.bag.name, this.model.get('combineWith')];
        $SERVICE[meth](listDetails).then(LIST_EVENTS.success, LIST_EVENTS.failure);
      },

      copyList: function () {
        var q = {
          select: ['id'],
          from: this.bag.type,
          where: [[this.bag.type, 'IN', this.bag.name]]
        };
        var listDetails = this.getListDetails();
        listDetails.tags.push('copy-of:' + this.bag.name);
        $SERVICE.query(q)
                .then(function (query) {return query.saveAsList(listDetails);})
                .then(LIST_EVENTS.success, LIST_EVENTS.failure);
      },

      getListDetails: function () {
        return {
          name: this.model.get('newName'),
          description: this.bag.description,
          tags: []
        };
      },

      updateName: function (e) {
        this.model.set({newName: e.target.value});
      },

      data: function () {
        var canDelete = this.bag.authorized;
        return {
          operations: ListOperations.operations.filter(function (op) {
            return canDelete || (op !== 'delete');
          }),
          compatibleBags: this.compatibleBags.toJSON(),
          state: this.model.toJSON()
        };
      },

      render: function () {
        this.$el.html(this.template(this.data()));
        return this;
      }
    });

    ListOperations.operations = ['copy', 'delete', 'intersect', 'combine', 'subtract'];

  } else {
    console.error("Backbone and underscore are not both available!");
    Remover = ListOperations = function () {
      throw new Error("Backbone or _ was not available");
    };
  }
})();

function addBagItemRemover (table) {
  var bagName = table.history.getCurrentQuery().constraints[0].value;
  $SERVICE.fetchList(bagName).then(function (bag) {
    var remover = new Remover({
      el: document.querySelector('#item-remover'),
      bag: bag,
      tableModel: table.model,
      selection: table.selectedObjects
    });
    remover.render();
    var operations = new ListOperations({
      el: document.querySelector('#list-operations'),
      bag: bag
    });
    operations.render();
  }, function (error) {
    console.error("Could not find bag" + bagName, error);
  }).then(null, console.error.bind(console));
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
