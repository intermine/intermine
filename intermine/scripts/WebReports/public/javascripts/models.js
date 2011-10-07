function wireUpRadios() {
    $('input[name=model-display]').change(function() {
        if ($(this).val() == "per-mine") {
            $('#model-data').show();
            $('#model-comparison').hide();
        } else {
            $('#model-data').hide();
            $('#model-comparison').show();
        }
    });
}

var subpath = "/service/model?format=jsonp&callback=?";

function __(x) {
    return _(x).chain();
}

function add(a, b) {
    return a + b;
}

var fieldGroups = ['attributes', 'references', 'collections'];

function countFields(model) {
    return __(model.classes)
                .map(function(c) { 
                    return __(fieldGroups)
                        .map(function(fs) { return _(c[fs]).size() })
                        .reduce(add, 0).value();
                })
                .reduce(add, 0)
                .value();
}

function getRowFinder(wanted) {return function() {
    return $(this).children("td").first().text() == wanted;
}}


function insertRowIntoTable($table, col1, colCount) {
    var $row = $table.find("tr").filter(getRowFinder(col1));
    if ($row.length == 0) {
        $row = $('<tr>').append('<td>' + col1 + '</td>');
        _(colCount).times(function() {$row.append("<td>")});
        var rowInserted = false;
        $table.find("tr").each(function() {
            var $tds = $(this).children("td");
            if ($tds.length == 0) {
                return true;
            }
            if (col1 < $tds.first().text()) {
                $(this).before($row);
                rowInserted = true;
                return false;
            }
        });
        if (!rowInserted) {
            $table.append($row);
        }
        $row.find("td").first().append('<a name="comparison' + col1 + '"/>');
    }
    return $row;
}

function updateComparisonTable($table, c, allFields, $spans) {
    var $row = insertRowIntoTable($table, c.name, _(mines).size());
    var cells = $row.children("td");
    var cell = cells[$spans.length + 1];
    $(cell).text(allFields.length);

    cells.unbind("click").click(function() {
        var $fieldRow = $row.next('.field-row');
        if ($fieldRow.length) {
            $fieldRow.remove();
            return false;
        }
        var $subtable = $('<table>').addClass("subtable");
        var $newRow = $('<tr class="field-row">').insertAfter($row);
        var $td = $('<td colspan="' + _(mines).size() + 2 + '">');
        $td.appendTo($newRow).append($subtable);
        var $headerRow = $('<tr>').appendTo($subtable).append("<th>Field Name</th>");
        var index = 0;
        _(models).each(function(model, mineName) {
            index++;
            $('<th>').text(mineName).appendTo($headerRow);
            var cd = model.classes[c.name];
            if (!cd) return true;
            var allCdFields = getAllFields(cd);
            _(allCdFields).each(function(f) {
                var $subrow = insertRowIntoTable($subtable, f.name, _(mines).size());
                var subCells = $subrow.children("td");
                var subCell = subCells[index];
                var rr;
                if (rr = f["referencedType"]) {
                    $('<a href="#comparison' + rr + '">').text(rr).appendTo(subCell);
                } else {
                    $(subCell).text(f["type"].split(/\./).pop());
                }
            });
        });
    });
}

function getAllFields(c) {
    return __(fieldGroups).map(function(fg) {return _(c[fg]).toArray()})
                            .flatten()
                            .sortBy(function(f) {return f.name})
                            .value();
}

function addHeaderCell($table, mineName) {
    $table.find("tr").first().append("<th>" + mineName + "</th>");
}

function addSummaryLine($container, mineName, noOfClasses, noOfFields) {
    $container.prepend('<div class="one-line-summary">' 
            + mineName + " - " + noOfClasses + " classes, " + noOfFields + " fields" + '</span>');
}

function addFieldLines($div, allFields, mineName) {
    $div.append('<ul class="field-summaries">');
    _(allFields).each(function(f) {
        var $field = $('<li>').appendTo($div.children("ul"));
        var t, rt, rr;
        $field.append(f.name + " (")
        if (t = f["type"]) {
            $field.append($('<span>').addClass("attribute-type").text(_(f["type"].split(/\./)).last()));
        } else {
            $field.append('<a href="#' + mineName + f.referencedType + '">' + f.referencedType);
        }
        $field.append(")");
        if (rr = f["reverseReference"]) {
            $field.append(" &larr; " + f["referencedType"] + "." + rr);
        }
    });
}

function addExtendsInfo($div, c, mineName) {
    if (c["extends"].length) {
        var $ext = $('<span>').append("extends: ").appendTo($div);

        _(c["extends"]).each(function(e) { 
            $ext.append("<a href='#" + mineName + e + "'>" + e + "</a>");
        });
    }
}

function addClassHeaderInfo($div, mineName, c, allFields) {
    $div.append('<a name="' + mineName + c.name + '"/>');
    $div.append($('<h4>').text(c.name).addClass("field-shower"));
    $('<span>').addClass("field-shower").text(allFields.length + " fields").appendTo($div);
}

function addClassClickBehaviour($div) {
    $div.children('.field-shower').click(function() {
        $div.children('ul').slideToggle();
    });
}

function addMineClickBehaviour($container, $modelSummary) {
    $container.children('.one-line-summary').click(function() {
        $modelSummary.slideToggle();
        $container.toggleClass("open closed");
    });
}

function getClassAdder($modelSummary, mineName, $table, $spans) {return function(c) {
    var allFields = getAllFields(c);
    var $div = $('<div>').addClass("class-summary").appendTo($modelSummary);
    addClassHeaderInfo($div, mineName, c, allFields);
    addExtendsInfo($div, c, mineName);
    addFieldLines($div, allFields, mineName);
    addClassClickBehaviour($div);
    updateComparisonTable($table, c, allFields, $spans);
}}

function addDifferenceColumn($table) {
    $rows = $table.find("tr");
    $rows.first().append("<th>Similarity</th>");
    $rows.each(function() {
        var $row = $(this);
        var $cells = $row.find("td");
        if ($cells.length < 1) return true;
        var values = $cells.map(function() {return parseInt($(this).text()) || 0}).get();
        var sum = _(values).reduce(add, 0);
        var max = _(values).max();
        var count = values.length - 1;
        var avg = sum / count;
        var diff = avg / max * 100;
        var $simCell = $('<td>').text(diff.toFixed(1) + "%");
        if (diff > 70) {
            $simCell.addClass("good-similarity");
        } else if (diff > 50) {
            $simCell.addClass("med-similarity");
        } else if (diff > 25) {
            $simCell.addClass("low-similarity");
        } else {
            $simCell.addClass("no-similarity");
        }

        $row.append($simCell);
    });
}

var models = {};

function getModelHandler(mineName) {
    return function(res) {
        var $container = $('<li>').addClass("mine-summary closed");
        var $modelSummary = $('<div>').addClass('model-summary').appendTo($container);
        var $otherContainers = $('#model-data .mine-summary');
        var $table = $('#model-comparison');
        var model = res["model"] || res;
        var noOfClasses = _(model.classes).size();
        var inserted = false;

        models[mineName] = model;

        addHeaderCell($table, mineName);
        addSummaryLine($container, mineName, noOfClasses, countFields(model));
        addMineClickBehaviour($container, $modelSummary);
        $container.attr('noofclasses', noOfClasses);

        var classAdder = getClassAdder($modelSummary, mineName, $table, $otherContainers);
        __(model.classes).sortBy(function(c) {return c.name}).each(classAdder);
        $otherContainers.each(function(e) {
            var no = parseInt($(this).attr('noofclasses'));
            if (no < noOfClasses) {
                $(this).before($container);
                inserted = true;
                return false;
            }
        });
        if (!inserted) {
            $('#model-data').append($container);
        }
        if ($otherContainers.length + 1 == _(mines).size()) {
            $('.throbbing').remove();
            addDifferenceColumn($table);
        }
    }
}

$(function() {
    wireUpRadios();
    _(mines).each(function(v, k) {
        $.getJSON(v + subpath, getModelHandler(k));
    });
});
