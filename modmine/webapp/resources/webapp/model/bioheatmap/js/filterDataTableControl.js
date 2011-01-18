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
org.systemsbiology.visualization.FilterDataTableControl = Class.create(
        new org.systemsbiology.visualization.MappedSelectEventPropagation(),
        new org.systemsbiology.visualization.VisualizationDrawDataPropagation(),
        new org.systemsbiology.visualization.util.EventListenerAdapter(),
{
    initialize: function(container) {
        this.containerElement = container;
        this.globalAnd = true;
        this.columnIndexesToFilter = new Array();
        this.openFilterContainer = true;
        this.labelForAnd = "All Must Pass";
        this.labelForOr = "At Least One Must Pass";
        this.labelForTitle = "Filters";
        this.filterColumnControlByColumnIndex = new Hash();
    },

    draw : function(data, options) {
        this.data = data;

        var selectedColumnIndexes = new Array();

        if (options) {
            this.setControlledDrawVisualizations(options.controlledVisualizations);

            var controlled_event = "filtered-select";
            if (options.controlledEvent) controlled_event = options.controlledEvent;
            this.setControlledEventVisualizations(options.controlledVisualizations, controlled_event);

            if (options.labelForAnd) this.labelForAnd = options.labelForAnd;
            if (options.labelForOr) this.labelForOr = options.labelForOr;
            if (options.labelForTitle) this.labelForTitle = options.labelForTitle;
            if (options.columnIndexesToFilter) {
                for (var c = 0; c < options.columnIndexesToFilter.length; c++) {
                    selectedColumnIndexes[selectedColumnIndexes.length] = options.columnIndexesToFilter[c];
                }
            }
        }

        if (!selectedColumnIndexes.length) {
            for (var i = 0; i < this.data.getNumberOfColumns(); i++) {
                selectedColumnIndexes[selectedColumnIndexes.length] = i;
            }
        }

        this.containerElement.innerHTML = this.getHtml(selectedColumnIndexes);
        this.drawFilterColumnControls(selectedColumnIndexes, options);

        this.registerEvents(selectedColumnIndexes);
        this.resetFilter();

        if (options.hideFilterContainerOnOpen) this.toggleContainer();
    },

    getHtml: function(selectedColumnIndexes) {
        var html = "";
        html += "<div id='org-systemsbiology-visualization-filtercontainer-header-title'>";
        html += "<a id='org-systemsbiology-visualization-filtercontainer-header-titlelink' href='#'>" + this.getTitleHtml() + "</a>";
        html += "</div>";
        html += "<div id='org-systemsbiology-visualization-filtercontainer'>";
        html += "<ul id='org-systemsbiology-visualization-filtercontainer-header'>";
        html += "<li>"
        html += "<ul id='org-systemsbiology-visualization-filtercontainer-options'>";
        html += "<li id='org-systemsbiology-visualization-globalAnd-active'>";
        html += "<a id='org-systemsbiology-visualization-globalAnd' href='#'>" + this.labelForAnd + "</a>";
        html += "</li>";
        html += "<li>";
        html += "<a id='org-systemsbiology-visualization-globalOr' href='#'>" + this.labelForOr + "</a>";
        html += "</li>";
        html += "</ul>";
        html += "</li>";
        html += "<li>";
        html += "<ul id='org-systemsbiology-visualization-filtercontainer-buttons'>";
        html += "<li><a id='org-systemsbiology-visualization-apply' href='#'>Apply</a></li>";
        html += "<li><a id='org-systemsbiology-visualization-clear' href='#'>Clear All</a></li>";
        html += "</ul>";
        html += "</li>";
        html += "</ul>";
        html += "</div>";
        html += "<div id='org-systemsbiology-visualization-filtercontainer-menu'>";
        html += "<ul id='org-systemsbiology-visualization-filtercontainer-filterlist'>";

        // draw selectors for column filter controls
        for (var i = 0; i < selectedColumnIndexes.length; i++) {
            if (i == 0) {
                html += "<li id='org-systemsbiology-visualization-filtercontainer-filteritem-active'>";
            } else {
                html += "<li>";
            }

            var selectedColumnIndex = selectedColumnIndexes[i];
            var selectorLabel = this.data.getColumnLabel(selectedColumnIndex);
            html += "<a href='#' id='org-systemsbiology-visualization-filteritem-selector-" + selectedColumnIndex + "'>" + selectorLabel + "</a>";
            html += "</li><li>"
            var resetterId = "org-systemsbiology-visualization-filteritem-resetter-" + selectedColumnIndex;
            if (i == 0) {
                html += "<button id='" + resetterId + "'>clear</button>";
                this.resetFilterItemButton = resetterId;
            } else {
                html += "<button id='" + resetterId + "' style='display:none'>clear</button>";
            }
            html += "</li>";
        }
        html += "</ul>"
        html += "</div>";

        // draw containers for column filter controls
        html += "<div id='org-systemsbiology-visualization-filtercontainer-filteritemcontainers'>";
        for (var j = 0; j < selectedColumnIndexes.length; j++) {
            var containerId = "org-systemsbiology-visualization-filteritem-container-" + selectedColumnIndexes[j];
            if (j == 0) {
                this.openFilterItemContainer = containerId;
                html += "<div id='" + containerId + "'></div>";
            } else {
                html += "<div id='" + containerId + "' style='display:none;'></div>";
            }
        }
        html += "</div>";
        return html;
    },

    getTitleHtml: function() {
        if (this.openFilterContainer) {
            return "<img alt='-' src='http://systemsbiology-visualizations.googlecode.com/svn/trunk/src/main/images/collapse.jpg'>" + this.labelForTitle;
        }
        return "<img alt='+' src='http://systemsbiology-visualizations.googlecode.com/svn/trunk/src/main/images/expand.jpg'>" + this.labelForTitle;
    },

    drawFilterColumnControls: function(selectedColumnIndexes, options) {
        for (var i = 0; i < selectedColumnIndexes.length; i++) {
            var columnIndex = selectedColumnIndexes[i];
            var columnType = this.data.getColumnType(columnIndex);
            var fccContainer = $("org-systemsbiology-visualization-filteritem-container-" + columnIndex);
            var fcc = this.getFilterColumnControlInstance(columnIndex, columnType, fccContainer, options);

            var fccOptions = {
                assignedColumnIndex: columnIndex,
                inheritedOptions: options
            };

            this.filterColumnControlByColumnIndex.set(columnIndex, fcc);
            fcc.draw(this.data, fccOptions);
        }
    },

    getFilterColumnControlInstance: function(columnIndex, columnType, container, options) {
        // TODO: Load impls from options and columnIndex
        switch (columnType) {
            case "string":
                return new org.systemsbiology.visualization.SelectDistinctValuesStringFilterColumnControl(container);
            case "number":
                return new org.systemsbiology.visualization.SimpleOperatorNumberFilterColumnControl(container);
            case "boolean":
                return new org.systemsbiology.visualization.SimpleChoiceBooleanFilterColumnControl(container);
        }
        return null;
    },

    registerEvents: function(selectedColumnIndexes) {
        var control = this;
        var listenerOptions = {stopEventAfterCallback: true};
        this.addOnClickEventListener($("org-systemsbiology-visualization-globalAnd"), function() {
            control.togglePassButtons(true);
            control.applyFilter();
        }, listenerOptions);
        this.addOnClickEventListener($("org-systemsbiology-visualization-globalOr"), function() {
            control.togglePassButtons(false);
            control.applyFilter();
        }, listenerOptions);
        this.addOnClickEventListener($("org-systemsbiology-visualization-apply"), function() {
            control.applyFilter();
        }, listenerOptions);
        this.addOnClickEventListener($("org-systemsbiology-visualization-clear"), function() {
            control.resetFilter();
        }, listenerOptions);
        this.addOnClickEventListener($("org-systemsbiology-visualization-filtercontainer-header-titlelink"), function() {
            control.toggleContainer();
        }, listenerOptions);

        var activeSelectorClick = function() {
            // take care of highlighting selector
            var activeSelector = $("org-systemsbiology-visualization-filtercontainer-filteritem-active");
            if (activeSelector) {
                if (this.parentNode && activeSelector != this.parentNode) {
                    activeSelector.id = null;
                    this.parentNode.id = "org-systemsbiology-visualization-filtercontainer-filteritem-active";
                } else if (this.parent && activeSelector != this.parent) {
                    activeSelector.id = null;
                    this.parent.id = "org-systemsbiology-visualization-filtercontainer-filteritem-active";
                }
            }
        }

        var getOnSelectorClick = function(columnIndex) {
            return function() {
                // take care of visualizing container
                var currentSelection = $("org-systemsbiology-visualization-filteritem-container-" + columnIndex);
                var previousSelection = $(control.openFilterItemContainer);
                if (currentSelection) {
                    if (previousSelection) {
                        if (previousSelection != currentSelection) {
                            previousSelection.style.display = "none";
                            currentSelection.style.display = "";
                        }
                    } else {
                        currentSelection.style.display = "";
                    }
                    control.openFilterItemContainer = currentSelection.id;
                }
            }
        }

        var getOnSelectorClickChangeResetter = function(columnIndex) {
            return function() {
                // take care of visualizing container
                var currentResetter = $("org-systemsbiology-visualization-filteritem-resetter-" + columnIndex);
                var previousResetter = $(control.resetFilterItemButton);
                if (currentResetter) {
                    if (previousResetter) {
                        if (previousResetter != currentResetter) {
                            previousResetter.style.display = "none";
                            currentResetter.style.display = "";
                        }
                    } else {
                        currentResetter.style.display = "";
                    }
                    control.resetFilterItemButton = currentResetter.id;
                }
            }
        }

        var getOnResetterClick = function(columnIndex) {
            return function() {
                var fcc = control.filterColumnControlByColumnIndex.get(columnIndex);
                fcc.resetFilter();
                control.applyFilter();
            }
        }

        for (var i = 0; i < selectedColumnIndexes.length; i++) {
            var selectedColumnIndex = selectedColumnIndexes[i];
            var selectorId = "org-systemsbiology-visualization-filteritem-selector-" + selectedColumnIndex;
            var resetterId = "org-systemsbiology-visualization-filteritem-resetter-" + selectedColumnIndex;

            this.addOnClickEventListener($(selectorId), activeSelectorClick, listenerOptions);
            this.addOnClickEventListener($(selectorId), getOnSelectorClick(selectedColumnIndex), listenerOptions);
            this.addOnClickEventListener($(selectorId), getOnSelectorClickChangeResetter(selectedColumnIndex), listenerOptions);
            this.addOnClickEventListener($(resetterId), getOnResetterClick(selectedColumnIndex), listenerOptions);
        }
    },

    applyFilter : function() {
        this.clearMappings();

        var control = this;
        var activeFilters = new Array();
        this.filterColumnControlByColumnIndex.keys().each(function(columnIndex) {
            var fcc = control.filterColumnControlByColumnIndex.get(columnIndex);
            if (fcc.isActive()) {
                activeFilters[activeFilters.length] = columnIndex;
            }
        });

        if (activeFilters.length == 0) {
            this.resetFilter();
            return;
        }

        var numberOfPasses = 1;
        if (this.globalAnd) {
            numberOfPasses = activeFilters.length;
        }

        var passedRows = new Array();
        for (var i = 0; i < this.data.getNumberOfRows(); i++) {
            var passingValues = new Array();
            activeFilters.each(function(columnIndex) {
                var value = control.data.getValue(i, parseInt(columnIndex));
                var fcc = control.filterColumnControlByColumnIndex.get(columnIndex);
                if (fcc.passes(value)) {
                    passingValues[passingValues.length] = columnIndex;
                }
            });
            if (passingValues.length >= numberOfPasses) {
                passedRows[passedRows.length] = i;
            }
        }

        // create table with filters
        var filteredData = new google.visualization.DataTable();
        for (var c = 0; c < this.data.getNumberOfColumns(); c++) {
            filteredData.addColumn(this.data.getColumnType(c), this.data.getColumnLabel(c));
        }

        // populate filterData with rows passing filters
        if (passedRows.length) {
            filteredData.addRows(passedRows.length);
            var rowIndex = 0;
            passedRows.uniq().sort().each(function(passedRowIndex) {
                for (var i = 0; i < control.data.getNumberOfColumns(); i++) {
                    filteredData.setCell(rowIndex, i, control.data.getValue(passedRowIndex, i));
                }
                control.addMapping({row:rowIndex++}, {row:passedRowIndex});
            });
        }
        this.propagate(filteredData);
    },

    resetFilter : function() {
        this.clearMappings();

        this.filterColumnControlByColumnIndex.values().each(function(filterColumnControl) {
            filterColumnControl.resetFilter();
        });

        this.propagate(this.data);
    },

    toggleContainer: function() {
        this.openFilterContainer = !this.openFilterContainer;
        if (this.openFilterContainer) {
            Effect.BlindDown($("org-systemsbiology-visualization-filtercontainer-header"));
            Effect.BlindDown($("org-systemsbiology-visualization-filtercontainer-menu"));
            Effect.BlindDown($("org-systemsbiology-visualization-filtercontainer-filteritemcontainers"));
        } else {
            Effect.BlindUp($("org-systemsbiology-visualization-filtercontainer-header"));
            Effect.BlindUp($("org-systemsbiology-visualization-filtercontainer-menu"));
            Effect.BlindUp($("org-systemsbiology-visualization-filtercontainer-filteritemcontainers"));
        }
        $("org-systemsbiology-visualization-filtercontainer-header-titlelink").innerHTML = this.getTitleHtml();
    },

    togglePassButtons: function(flag) {
        this.globalAnd = flag;

        var globalAnd = $("org-systemsbiology-visualization-globalAnd");
        var globalOr = $("org-systemsbiology-visualization-globalOr");
        if (this.globalAnd) {
            globalAnd.parentNode.id = "org-systemsbiology-visualization-globalAnd-active";
            globalOr.parentNode.id = null;
        } else {
            globalAnd.parentNode.id = null;
            globalOr.parentNode.id = "org-systemsbiology-visualization-globalAnd-active";
        }
    }
});

org.systemsbiology.visualization.SelectDistinctValuesStringFilterColumnControl = Class.create({
    initialize: function(container) {
        this.containerElement = container;
    },

    draw : function(data, options) {
        var columnIndex = options.assignedColumnIndex;
        var columnValues = new Array();
        for (var rowId = 0; rowId < data.getNumberOfRows(); rowId++) {
            columnValues[rowId] = data.getValue(rowId, columnIndex);
        }

        var uniqueValues = columnValues.uniq().sort();
        if (uniqueValues.length > 1) {
            var multiSize = 4;
            if (multiSize > uniqueValues.length) {
                multiSize = uniqueValues.length;
            }

            this.selectionElement = "filter_item_" + columnIndex;
            var html = "Select Any";
            html += "<select id='" + this.selectionElement + "' class='org-systemsbiology-visualization-filteritem-select'";
            html += " multiple='multiple' size='" + multiSize + "'>";
            uniqueValues.each(function(uniqueValue) {
                html += "<option value='" + uniqueValue + "'>" + uniqueValue + "</option>";
            });
            html += "</select>";
        }
        this.containerElement.innerHTML = html;
    },

    isActive: function() {
        var selectBox = $(this.selectionElement);
        for (var i = 0; i < selectBox.options.length; i++) {
            if (selectBox.options[i].selected) return true;
        }
        return false;
    },

    passes: function(value) {
        if (this.isActive()) {
            var selectedValues = new Array();
            var selectBox = $(this.selectionElement);
            for (var i = 0; i < selectBox.options.length; i++) {
                var option = selectBox.options[i];
                if (option.selected) {
                    selectedValues[selectedValues.length] = option.value;
                }
            }

            if (selectedValues.length) {
                var without = selectedValues.without(value);
                return (without.length < selectedValues.length);
            }
        }
        return true;
    },

    resetFilter: function() {
        var selectBox = $(this.selectionElement);
        for (var i = 0; i < selectBox.options.length; i++) {
            selectBox.options[i].selected = false;
        }
    }
});

org.systemsbiology.visualization.SimpleOperatorNumberFilterColumnControl = Class.create({
    initialize: function(container) {
        this.containerElement = container;
    },

    draw : function(data, options) {
        var simpleOperatorLabels = { gt: ">", ge: ">=", eq: "==", ne: "!=", lt: "<", le: "<=" };
        if (options.inheritedOptions && options.inheritedOptions.simpleOperatorLabels) {
            simpleOperatorLabels = options.inheritedOptions.simpleOperatorLabels;
        }

        var columnIndex = options.assignedColumnIndex;
        this.selectionElement = "filter_item_" + columnIndex;
        this.textInputElement = "filter_item_value_" + columnIndex;

        var html = "Operator<br/>"
        html += "<select id='" + this.selectionElement + "'>";
        html += "<option></option>";
        html += "<option value='GT'>" + simpleOperatorLabels.gt + "</option>";
        html += "<option value='GE'>" + simpleOperatorLabels.ge + "</option>";
        html += "<option value='EQ'>" + simpleOperatorLabels.eq + "</option>";
        html += "<option value='NE'>" + simpleOperatorLabels.ne + "</option>";
        html += "<option value='LT'>" + simpleOperatorLabels.lt + "</option>";
        html += "<option value='LE'>" + simpleOperatorLabels.le + "</option>";
        html += "</select>";
        html += "<br/>Compare Value<br/><input type='text' id='" + this.textInputElement + "'>";

        this.containerElement.innerHTML = html;
    },

    isActive: function() {
        return ($(this.textInputElement).value && $(this.selectionElement).selectedIndex);
    },

    passes: function(value) {
        if (this.isActive()) {
            var compareInput = $(this.textInputElement);
            var selectOperation = $(this.selectionElement);
            if (compareInput.value && selectOperation.selectedIndex) {
                var selectedOption = selectOperation.options[selectOperation.selectedIndex];
                if (selectedOption && selectedOption.value) {
                    return this.compare(parseFloat(value), selectedOption.value, parseFloat(compareInput.value));
                }
            }
        }
        return true;
    },

    resetFilter: function() {
        var selectOperation = $(this.selectionElement);
        for (var i = 0; i < selectOperation.options.length; i++) {
            selectOperation.options[i].selected = false;
        }
        $(this.textInputElement).value = "";
    },

    compare: function(value, operator, filterValue) {
        switch (operator) {
            case "GT" : return value > filterValue;
            case "GE": return value >= filterValue;
            case "EQ": return value == filterValue;
            case "NE": return value != filterValue;
            case "LT" : return value < filterValue;
            case "LE": return value <= filterValue;
        }
        return true;
    }
});

org.systemsbiology.visualization.SimpleChoiceBooleanFilterColumnControl = Class.create({
    initialize: function(container) {
        this.containerElement = container;
    },

    draw : function(data, options) {
        var columnIndex = options.assignedColumnIndex;
        var filterName = "filter_item_" + columnIndex;
        this.trueFilterId = filterName + "_T";
        this.falseFilterId = filterName + "_F";

        var labelForTrue = true;
        var labelForFalse = false;
        if (options.inheritedOptions && options.inheritedOptions.columnFilterControlConfigByColumnIndex) {
            var cfg = $H(options.inheritedOptions.columnFilterControlConfigByColumnIndex).get("column_" + columnIndex);
            if (cfg) {
                if (cfg.labelForTrue) labelForTrue = cfg.labelForTrue;
                if (cfg.labelForFalse) labelForFalse = cfg.labelForFalse;
            }
        }

        var html = "Select One<br/>";
        html += "<input id='" + this.trueFilterId + "' name='" + filterName + "' type='radio'>" + labelForTrue + "<br/>";
        html += "<input id='" + this.falseFilterId + "' name='" + filterName + "' type='radio'>" + labelForFalse + "<br/>";
        this.containerElement.innerHTML = html;
    },

    isActive: function() {
        return ($(this.trueFilterId).checked || $(this.falseFilterId).checked);
    },

    passes: function(value) {
        if (this.isActive()) {
            if ($(this.trueFilterId).checked) return value;
            if ($(this.falseFilterId).checked) return !value;
        }
        return true;
    },

    resetFilter: function() {
        $(this.trueFilterId).checked = "";
        $(this.falseFilterId).checked = "";
    }
});

