var subpath = "/service/model?format=jsonp&callback=?";

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

function getTableRowClickHandler(className, $row) {
    var getUnit = function(model, className) {return model.classes[className]};
    var addSubCellContents = function(subCell, f) {
        var rr;
        if (rr = f["referencedType"]) {
            $('<a href="#comparison' + rr + '">').text(rr).appendTo(subCell);
        } else {
            $(subCell).text(f["type"].split(/\./).pop());
        }
    }
    return getGenericTableRowHandler(className, $row, "Field Name", getUnit, models, getAllFields, addSubCellContents, function(f) {return f.name});
}

function getAllFields(c) {
    return __(fieldGroups).map(function(fg) {return _(c[fg]).toArray()})
                            .flatten()
                            .sortBy(function(f) {return f.name})
                            .value();
}

function addSummaryLine($container, mineName, noOfClasses, noOfFields) {
    $('<div>').addClass("one-line-summary")
              .append(mineName)
              .append(" - ")
              .append(noOfClasses + " classes, ")
              .append(noOfFields + " fields")
              .prependTo($container)
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


function getUnitAdder($modelSummary, mineName, $table, $spans) {return function(c) {
    var allFields = getAllFields(c);
    var $div = $('<div>').addClass("unit-summary").appendTo($modelSummary);
    addClassHeaderInfo($div, mineName, c, allFields);
    addExtendsInfo($div, c, mineName);
    addFieldLines($div, allFields, mineName);
    addUnitClickBehaviour($div);
    updateComparisonTable($table, c, allFields, $spans);
}}

var models = {};

function getThing(res, mineName) {
    models[mineName] = res["model"] || res;
    return models[mineName];
}

function getMainNo(model) {
    return _(model.classes).size();
}

function getSecondaryNo(model) {
    return countFields(model);
}

function getUnits(model) {
    return _(model.classes).sortBy(function(c) {return c.name});
}
