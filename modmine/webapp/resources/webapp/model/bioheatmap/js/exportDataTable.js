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
org.systemsbiology.visualization.ExportDataTable = Class.create({
    initialize: function(container) {
        this.containerElement = container;
    },

    draw : function(data, options) {
        this.data = data;

        this.columnSeparator = ",";
        this.rowSeparator = "\n";
        this.undefinedValue = "null";
        if (options) {
            if (options.columnSeparator) this.columnSeparator = options.columnSeparator;
            if (options.rowSeparator) this.rowSeparator = options.rowSeparator;
            if (options.undefinedValue) this.undefinedValue = options.undefinedValue;
        }

        var html = "";
        html += "<textArea class='org-systemsbiology-visualization-exportdatatable-textArea'>"
        html += this.getDataString();
        html += "</textArea>"

        this.containerElement.innerHTML = html;
    },

    getDataString : function() {
        var txt = "";
        for (var i = 0; i < this.data.getNumberOfRows(); i++) {
            if (i != 0) txt += this.rowSeparator;
            for (var j = 0; j < this.data.getNumberOfColumns(); j++) {
                if (j != 0) txt += this.columnSeparator;
                var val = this.data.getValue(i, j);
                if (val != null) {
                	txt += val;
                } else {
                    txt += this.undefinedValue;
                }
            }
        }
        return txt;
    }
});