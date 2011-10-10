function wireUpRadios() {
    $('input[name=things-display]').change(function() {
        if ($(this).val() == "per-mine") {
            $('#thing-data').show();
            $('#thing-comparison').hide();
        } else {
            $('#thing-data').hide();
            $('#thing-comparison').show();
        }
    });
}

function add(a, b) {
    return a + b;
}


function __(x) {
    return _(x).chain();
}

function addHeaderCell($table, mineName) {
    $table.find("tr").first().append("<th>" + mineName + "</th>");
}

function addMineClickBehaviour($container, $summary) {
    $container.children('.one-line-summary').click(function() {
        $summary.slideToggle();
        $container.toggleClass("open closed");
    });
}

function addUnitClickBehaviour($div) {
    $div.children('.field-shower').click(function() {
        $div.children('ul').slideToggle();
    });
}

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

function getResultsHandler(mineName) {
    return function(res) {
        var $container = $('<li>').addClass("mine-summary closed");
        var $thingSummary = $('<div>').addClass('thing-summary').appendTo($container);
        var $otherContainers = $('#thing-data .mine-summary');
        var $table = $('#thing-comparison');
        var thing = getThing(res, mineName);
        var mainNo = getMainNo(thing);        
        var inserted = false;

        addHeaderCell($table, mineName);
        addSummaryLine($container, mineName, mainNo, getSecondaryNo(thing));
        addMineClickBehaviour($container, $thingSummary);
        $container.attr('mainno', mainNo);

        var addUnits = getUnitAdder($thingSummary, mineName, $table, $otherContainers);
        _(getUnits(thing)).each(addUnits);

        $otherContainers.each(function(e) {
            var no = parseInt($(this).attr('mainno'));
            if (no < mainNo) {
                $(this).before($container);
                inserted = true;
                return false;
            }
        });
        if (!inserted) {
            $('#thing-data').append($container);
        }
        if ($otherContainers.length + 1 == _(mines).size()) {
            $('.throbbing').remove();
            addDifferenceColumn($table);
        }
    }
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

function updateComparisonTable($table, x, things, $others) {
    var $row = insertRowIntoTable($table, x.name, _(mines).size());
    var cells = $row.children("td");
    var cell = cells[$others.length + 1];
    $(cell).text(things.length);

    cells.unbind("click").click(getTableRowClickHandler(x.name, $row));
}

function getGenericTableRowHandler(unitName, $row, topLeft, getUnit, things, getSubThings, addSubCellContents, getCol1) {  
    var width = _(mines).size();
    return function() {
        var $fieldRow = $row.next('.field-row');
        if ($fieldRow.length) {
            $fieldRow.remove();
            return false;
        }
        var $subtable = $('<table>').addClass("subtable");
        var $newRow = $('<tr class="field-row">').insertAfter($row);
        var $td = $('<td colspan="' + (_(mines).size() + 2) + '">');
        $td.appendTo($newRow).append($subtable);
        var $headerRow = $('<tr>').appendTo($subtable)
                             .append("<th>" + topLeft + "</th>");
        var index = 0;
        _(things).each(function(thing, mineName) {
            index++;
            $('<th>').text(mineName).appendTo($headerRow);
            var unit = getUnit(thing, unitName);
            if (!unit) return true;
            var subThings = getSubThings(unit);
            _(subThings).each(function(x) {
                var col1 = getCol1(x);
                var $subrow = insertRowIntoTable($subtable, col1, width);
                var subCells = $subrow.children("td");
                var subCell = subCells[index];
                addSubCellContents(subCell, x);
            });
        });
    }
}

$(function() {
    wireUpRadios();
    _(mines).each(function(v, k) {
        $.getJSON(v + subpath, getResultsHandler(k));
    });
});
