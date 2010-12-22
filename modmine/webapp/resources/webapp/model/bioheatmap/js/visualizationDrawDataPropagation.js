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
org.systemsbiology.visualization.VisualizationDrawDataPropagation = Class.create({
    controlled : new Array(),

    setControlledDrawVisualizations: function(visualizations) {
        if (visualizations && visualizations.length) {
            for (var i = 0; i < visualizations.length; i++) {
                this.controlled[this.controlled.length] = visualizations[i];
            }
        }
    },

    registerVisualization: function(controlled_visualization) {
        if (controlled_visualization) {
            this.controlled[this.controlled.length] = controlled_visualization;
        }
    },

    propagate: function(data) {
        this.controlled.each(function(item) {
            item.visualization.draw(data, item.options);
        });
    }
});