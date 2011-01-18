/*
**    Copyright (C) 2003-2008 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/
org.systemsbiology.visualization.MappedSelectEventPropagation = Class.create({
    initialize: function() {
        this.row_hash = new org.systemsbiology.visualization.util.KeyToKeyMapping();
        this.col_hash = new org.systemsbiology.visualization.util.KeyToKeyMapping();
        this.controlled_events = new Array();
    },

    setControlledEventVisualizations: function(visualizations, controlled_event) {
        if (visualizations && visualizations.length) {
            this.controlled_events[this.controlled_events.length] = controlled_event;

            for (var i = 0; i < visualizations.length; i++) {
                var visualization = visualizations[i].visualization;
                this.registerListenerOnSelectEvent(visualization);
                this.registerAsControlledEventListener(visualization, controlled_event);
            }
        }
    },

    registerListenerOnSelectEvent: function(controlled_visualization) {
        if (!controlled_visualization.getSelection) return;

        var control = this;
        google.visualization.events.addListener(controlled_visualization, "select", function() {
            control.selection = null;

            var controlled_selections = controlled_visualization.getSelection();
            if (control.hasMappings() && controlled_selections && controlled_selections.length) {
                var uncontrolled_selections = [];
                for (var r = 0; r < controlled_selections.length; r++) {
                    var controlled_row = controlled_selections[r].row;
                    var controlled_col = controlled_selections[r].column;
                    uncontrolled_selections[uncontrolled_selections.length] = {
                        row: control.row_hash.getLeft(controlled_row),
                        column: control.col_hash.getLeft(controlled_col)
                    }
                }
                control.selection = uncontrolled_selections;
            } else {
                control.selection = controlled_selections;
            }
            
            // propagate controlled selection event to external visualizations using uncontrolled data
            if (control.selection) {
                google.visualization.events.trigger(control, "select", control.selection);
            }
        });
    },

    registerAsControlledEventListener: function(controlled_visualization, controlled_event) {
        if (controlled_visualization.setSelection) {
            var control = this;
            // register controlled visualization as controlled event listener
            google.visualization.events.addListener(control, controlled_event, function() {
                if (control.hasMappings()) {
                    var controlled_selections = [];
                    var uncontrolled_selections = control.selection;
                    for (var r = 0; r < uncontrolled_selections.length; r++) {
                        controlled_selections[r] = {
                            row: control.row_hash.getRight(uncontrolled_selections[r].row),
                            column: control.col_hash.getRight(uncontrolled_selections[r].column)
                        }
                    }
                    controlled_visualization.setSelection(controlled_selections);
                } else {
                    controlled_visualization.setSelection(control.selection);
                }
            });
        }
    },

    getSelection : function() {
        return this.selection;
    },

    setSelection : function(selection) {
        this.selection = selection;

        var control = this;
        this.controlled_events.uniq().each(function(controlled_event) {
            google.visualization.events.trigger(control, controlled_event, null);
        });
    },

    addMapping : function(controlled_selection, source_selection) {
        this.col_hash.addMapping(controlled_selection.column, source_selection.column);
        this.row_hash.addMapping(controlled_selection.row, source_selection.row);
    },

    hasMappings : function() {
        return (this.row_hash.hasMappings() || this.col_hash.hasMappings());
    },

    clearMappings : function() {
        this.row_hash.clearMappings();
        this.col_hash.clearMappings();
    }
});

org.systemsbiology.visualization.util.KeyToKeyMapping = Class.create({
    initialize: function() {
        this.clearMappings();        
    },

    addMapping: function(leftKey, rightKey) {
        if (leftKey != null && rightKey != null) {
            this.left.set(leftKey, rightKey);
            this.right.set(rightKey, leftKey);
        }
    },

    hasMappings: function() {
        return (this.left.keys().length && this.right.keys().length);
    },

    clearMappings: function() {
        this.left = new Hash();
        this.right = new Hash();
    },

    getLeft: function(key) {
        return this.left.get(key);
    },

    getRight: function(key) {
        return this.right.get(key);
    }
});