var subpath = "/service/templates?format=jsonp&callback=?";
var templates = {};

function getThing(res, mineName) {
    templates[mineName] = res["templates"] || res;
    return templates[mineName];
}

function getMainNo(templates) {
    return _(templates).size();
}

function getSecondaryNo() { return null }

function getUnits(templates) {
    return _(templates).sortBy(function(t) {return t.name});
}

function addSummaryLine($container, mineName, noOfTemplates) {
    $('<div>').addClass("one-line-summary")
              .append(mineName)
              .append(" - ")
              .append(noOfTemplates + " templates")
              .prependTo($container)
}

function addTemplateHeaderInfo($div, t, cons) {
    var text = t.title || t.name;

    $('<h4>').html(text.replace("-->", "&rarr;"))
             .addClass("field-shower")
             .appendTo($div);
}

function addViews($div, t) {
    if (t.view) {
        var root = t.view[0].split(/\./)[0];
        var views = _(t.view).map(function(v) {
            return v.split(/\./).slice(1).join(".");
        });
        $('<span>').text("Root: " + root).appendTo($div);
        $('<span>').addClass("field-shower").text(views.length + " output columns").appendTo($div);
        var $ul = $('<ul>').addClass("view-summary hidden")
                       .appendTo($div);
        _(views).each(function(v) {$('<li>').text(v).appendTo($ul)});
    }
}

function addConstraintLines($div, cons) {
    $('<span>').text(cons.length + " constraints")
               .addClass("field-shower")
               .appendTo($div);
    var $ul = $('<ul>').addClass("con-summaries hidden")
                       .appendTo($div);
    _(cons).each(function(c) {
        var $li = $('<li>').appendTo($ul);
        $li.text(stringifyConstraint(c));
    });
}

function getUnitAdder($summary, mineName, $table, $others) {
    return function(t) {
        var $div = $('<div>').addClass("unit-summary")
                             .appendTo($summary);
        var allConstraints = t.constraints;
        addTemplateHeaderInfo($div, t, allConstraints);
        addViews($div, t);
        addConstraintLines($div, allConstraints);
        addUnitClickBehaviour($div);
        updateComparisonTable($table, t, allConstraints, $others);
    };
}

function stringifyConstraint(c) {
    var str = c.path + " " + c.op + " x";
    if (c.extraValue) {
        str += "(in " + c.extraValue + ")";
    }
    return str;
}

function getTableRowClickHandler(templateName, $row) {
    return getGenericTableRowHandler(
            templateName, $row, "Constraint", 
            function(ts, name) {return ts[name]}, 
            templates, 
            function(t) {return t.constraints}, 
            function(x, c) {$(x).text(c.value)}, 
            stringifyConstraint);
}

