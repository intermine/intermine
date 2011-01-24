availableTemplates = null;
var model = {};
var baseUrl = "http://squirrel.flymine.org/intermine-test";
$(function() {
    IMBedding.setBaseUrl(baseUrl);
    Syntax.root = "http://squirrel.flymine.org/imbedding/lib/jquery-syntax/";
    $('#showMeArea').syntax({
        brush: 'javascript', 
        layout: 'list', 
        replace: true,
        tabWidth: 4,
        root: "lib/jquery-syntax/"
    });
    $('#showGraphArea').syntax({
        brush: 'javascript', 
        layout: 'list', 
        replace: true,
        tabWidth: 4,
        root: "lib/jquery-syntax/"
    });
    $('#showGraphAreaContainer').hide();
    loadTable4();
});

function setActiveStyleSheet(title) {
    var i, a, main;
    for(i=0; (a = document.getElementsByTagName("link")[i]); i++) {
        if(a.getAttribute("rel").indexOf("style") != -1
                && a.getAttribute("title")) {
            a.disabled = true;
            if(a.getAttribute("title") == title) a.disabled = false;
        }
    }
}

function loadTable1() {
    IMBedding.loadTemplate(
        {
            name: "employeesOverACertainAgeFromDepartmentA",
            size: 10,

            constraint1: "Employee.age",
            op1: ">=",
            value1: 25,

            constraint2: "Employee.department.name",
            op2: "=",
            value2: "DepartmentB1"
        },
        "#placeholder1"
    );
}
function loadTable2() {
    IMBedding.loadTemplate(
        {
            name: "employeesFromCompanyAndDepartment",
            size: 10,

            constraint1: "Employee.department.company.name",
            op1: "LIKE",
            value1: "Company*",

            constraint2: "Employee.department.name",
            op2: "LIKE",
            value2: "Department*"
        },
        "#placeholder1"
    );
}
function loadTable3() {
    IMBedding.loadTemplate(
        {
            name: "employeesOfACertainAge",
            size: 10,

            constraint1: "Employee.age",
            code1: "A",
            op1: ">",
            value1: 30,

            constraint2: "Employee.age",
            code2: "B",
            op2: "<=",
            value2: "60"
        },
        "#placeholder1"
    );
}
function loadTable4() {
    IMBedding.loadTemplate(
        {
            name:           "ManagerLookup",
        
            constraint1:    "Manager",
            op1:            "LOOKUP",
            value1:         "M.",
            code1:          "A",
        },
        '#placeholder1'
    );
}

function loadGraph1() {
    IMBedding.loadQuery(
        {
            select: ["Employee.age"],
            from: "testmodel"
        },
        {size: 1000, format: "jsonprows"},
        function(resultSet) {
            var graphData = [];
            var ageDist = {
                label: "Age Distribution",
                hoverable: true,
                bars: {show: true},
                data: []
            };
            var countAtAge = {};
            for (i in resultSet.results) {
                var age = resultSet.results[i][0].value;
                if (! countAtAge[age]) {
                    countAtAge[age] = 1;
                } else {
                    countAtAge[age] = countAtAge[age] + 1;
                }
            }
            for (age in countAtAge) {
                ageDist.data.push([age, countAtAge[age]]);
            }
            graphData.push(ageDist);
            var plot = $.plot("#graph", graphData);
            $('<span></span>').html("Age").addClass("axis-label").appendTo('#graph');
        }
    );
}

function loadGraph2() {
    IMBedding.loadQuery(
        {
            select: ["Manager.seniority", "Manager.age"],
            from: "testmodel"
        },
        {size: 1000, format: "jsonprows"},
        function(resultSet) {
            var graphData = [];
            var ageVsSen = {
                label: "Seniority age Age",
                points: {show: true},
                data: []
            };
            for (i in resultSet.results) {
                var seniority = resultSet.results[i][0].value;
                var age = resultSet.results[i][1].value;
                ageVsSen.data.push([age, seniority]);
            }
            graphData.push(ageVsSen);
            var plot = $.plot("#graph", graphData);
            $('<span></span>').html("Age").addClass("axis-label").appendTo('#graph');
        }
    );
}


var firstline = "/* Below is the Javascript to load this table using ajax:\n"
+ "You can cut and paste this into a page to get started.\n"
+ "Don't forget you need to include the imbedding.js library"
+ " in your head */\n\n"
+ '/* You only need to do the following once */' + "\n"
+ "IMBedding.setBaseUrl('" + baseUrl + "');\n\n";

var JSON = JSON || {};
// implement JSON.stringify serialization
JSON.stringify = JSON.stringify || function (obj) {
    var t = typeof (obj);
    if (t != "object" || obj === null) {
        // simple data type
        if (t == "string") obj = '"'+obj+'"';
        return String(obj);
    }
    else {
        // recurse array or object
        var n, v, json = [], arr = (obj && obj.constructor == Array);
        for (n in obj) {
            v = obj[n]; t = typeof(v);
            if (t == "string") v = '"'+v+'"';
            else if (t == "object" && v !== null) v = JSON.stringify(v);
            json.push((arr ? "" : '"' + n + '":') + String(v));
        }
        return (arr ? "[" : "{") + String(json) + (arr ? "]" : "}");
    }
};

function loadUserQuery() {
    var rootClass = $("#root-class").val();
    var views = [];
    var viewsFormValues = $("#views-form").serializeArray();
    for (var i = 0; i < viewsFormValues.length; i++) {
        var nameValuePair = viewsFormValues[i];
        views.push(rootClass + "." + nameValuePair.value);
    }
    var constraints = {};
    var consFormValues = $("#constraints-form").serializeArray();
    var typeConstraints = {};
    for (var i = 0; i < consFormValues.length; i++) {
        var nameValuePair = consFormValues[i];
        var consNo = nameValuePair.name.match(/\d+/)[0];
        if (! constraints[consNo]) {
            constraints[consNo] = {
                code: letters.charAt(parseInt(consNo) - 1)
            };
        }
        var consProp = nameValuePair.name.match(/[a-z]+/)[0];
        if (consProp == "constraint") {
            constraints[consNo].path
                = rootClass + "." + nameValuePair.value;
        } else if (consProp == "op") {
            if (nameValuePair.value == "ISA") {
                typeConstraints[consNo] = true;
            } else {
                constraints[consNo].op = nameValuePair.value;
            }
        } else if (consProp == "value") {
            if (typeConstraints[consNo]) {
                constraints[consNo].type = nameValuePair.value;
            } else {
                constraints[consNo].value = nameValuePair.value;
            }
        } else {
            throw("Unexpected constraint property: " + consProp);
        }
    }
    var constraintsList = [];
    for (key in constraints) {
        constraintsList.push(constraints[key]);
    }
    var joinsFormsValues = $('#joins-form').serializeArray();
    var joins = {};
    for (var i = 0; i < joinsFormsValues.length; i++) {
        var nameValuePair = joinsFormsValues[i];
        var joinNo = nameValuePair.name.match(/\d+/)[0];
        if (! joins[joinNo]) {
            joins[joinNo] = {};
        }
        var joinProp = nameValuePair.name.match(/[a-z]+/)[0];
        if (joinProp == "join") {
            joins[joinNo].path
                = rootClass + "." + nameValuePair.value;
        } else if (joinProp == "style") {
            joins[joinNo].style = nameValuePair.value;
        } else {
            throw("Unexpected join property: " + joinProp);
        }
    }
    var joinList = [];
    for (key in joins) {
        joinList.push(joins[key]);
    }

    var sortOrder = $('#sortOrderSelector').val() + " "
       + $('input:radio[name=sortDirection]:checked').val();

    var logic = $('#logic').val();

    var query = {
        select: views,
        where: constraintsList,
        joins: joinList,
        sortOrder: sortOrder,
        from: model.name
    };

    if (logic.length > 0) {
        query.constraintLogic = logic;
    }

    // Make the displayed json string
    // It needs munging to prettify it as well
    var queryDisplayString = JSON.stringify(query);
    var keys = queryDisplayString.match(/"\w+":/g);
    if (keys) {
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            var newKey = key.replace(/"/g, "").replace(":", ": ");
            queryDisplayString = queryDisplayString.replace(key, newKey);
        }
    }
    queryDisplayString = queryDisplayString.replace(/\}\],/g, "}\n\t],")
                                          .replace(/\},\{/g, "},\n\t{")
                                          .replace(/\],/g, "],\n\t")
                                          .replace(/^\{/, "{\n\t")
                                          .replace(/\[\{/g, "[\n\t{")
                                          .replace(/\}$/, "\n}")
                                          .replace(/",/g, "\", ")
                                          .replace(/\t\{/g, "\t\t{");
    var xmlString = IMBedding.makeQueryXML(query).replace(/&gt;/g, "&amp;gt;")
                                                 .replace(/&lt;/g, "&amp;lt;"); 
    var data = {size: 10};
    var newValue = firstline
     + "/*You can define the query as a regular javascript object*/\n"
     +  "var query = " + queryDisplayString + ";\n"
     + "\n/*Or as xml*/\n"
     + "var query = '" + xmlString + "';\n\n"
     + "/*Execution is the same in either case*/\n"
     + "IMBedding.loadQuery(query, {size: 10}, '#placeholder');"

    var codeContainer = $("#codeContainer");
    codeContainer.empty();

    var codeDisplayer = document.createElement("textarea");
    codeDisplayer.id = "codeDisplay";
    codeDisplayer.className = "coder";
    codeDisplayer.cols = 80;

    codeDisplayer.innerHTML = newValue;
    codeContainer.append(codeDisplayer);
    Syntax.root = "http://squirrel.flymine.org/imbedding/lib/jquery-syntax/";
    $(codeDisplayer).syntax({
        brush: 'javascript', 
        layout: 'list', 
        replace: true,
        tabWidth: 4,
        root: "lib/jquery-syntax/"
    });
    try {
        IMBedding.loadQuery(query, data, "#placeholder2");
    } catch(err) {
        alert("There was something wrong with your query:\n" + err);
    }
}

function complainAboutName(problem) {
    tempSpec = $('#templateSpecific');
    var error = document.createElement("p");
    error.className = "ui-state-error ui-corner-all";
    var icon = document.createElement("span");
    icon.className = "ui-icon ui-icon-alert";
    icon.style.float = "left";
    icon.style["margin-right"] = ".3em";
    error.appendChild(icon);
    var strong = document.createElement("strong");
    strong.innerHTML = "Um, excuse me:";
    error.appendChild(strong);
    error.appendChild(
            document.createTextNode(" " + problem));
    tempSpec.prepend(error);
    return;
}

function loadUserTemplate() {
    var formValues = $('#template-form').serializeArray();
    if (! formValues[0].value) {
        complainAboutName("Please enter a template name first");
        return;
    } else if (! (formValues[0].value in window.availableTemplates)) {
        complainAboutName("Check the name - " + 
                formValues[0].value + " is not one of the available templates");
        return;
    }

    var data = {size: 10};
    var newValue = firstline + "IMBedding.loadTemplate(\n\t{\n\t";
    for (x in formValues) {
        name = formValues[x].name;
        value = formValues[x].value;
        data[name] = value;
        if (name.match("constraint")) {
            newValue += "\n\t";
        }
        newValue += "\t" +name + ":\t";
        if (! name.match(/constraint/)) {
            newValue += "\t\t";
        }
        newValue += "\"" + value + "\",\n\t";
    }
    newValue += "},\n\t'#placeholder'\n);";
    var codeContainer = $("#templateCodeContainer");
    codeContainer.empty();

    var codeDisplayer = document.createElement("textarea");
    codeDisplayer.id = "templateCodeDisplay";
    codeDisplayer.className = "coder";
    codeDisplayer.cols = 80;

    codeDisplayer.innerHTML = newValue;
    codeContainer.append(codeDisplayer);
    Syntax.root = "http://squirrel.flymine.org/imbedding/lib/jquery-syntax/";
    $(codeDisplayer).syntax({
        brush: 'javascript', 
        layout: 'list', 
        replace: true,
        tabWidth: 4,
        root: "lib/jquery-syntax/"
    });

    IMBedding.loadTemplate(data, '#placeholder3');
};

$(function() {
        $('#tabs').tabs();
});
var oldRootClass = "Employee";
$(function() {
    $("input:button").button();
    $("button").button();
    $("#query-loader").click(function() {
        loadUserQuery();
    });
    $("#view-adder").click(function() {
        var rootClass = $("#root-class").val();
        addViewLine(rootClass);
    });
    $("#constraint-adder").click(function() {
        var rootClass = $("#root-class").val();
        addConstraintLine(rootClass, true);
    });
    $("#join-adder").click(function() {
        var rootClass = $("#root-class").val();
        addJoinLine(rootClass);
    });
    $("#root-class").change(function() {
        $( "#dialog-confirm" ).dialog({
            modal: true,
            width: 400,
            position: 'center',
            buttons: {
                "Change root class": function() {
                    $("#querybuilder-views").empty();
                    $("#querybuilder-constraints").empty();
                    $('#querybuilder-joins').empty();
                    $('#sortOrderDiv').hide();
                    $('#logic-box').hide();
                    oldRootClass = $("#root-class").val();
                    $( this ).dialog( "close" );
                },
                Cancel: function() {
                    $("#root-class").val(oldRootClass);
                    $( this ).dialog( "close" );
                }
            }
        });
    });

    $('#sortOrderSelector').button();
    $('#sortDirectionDiv').buttonset();
    $("input:checkbox").button();
    $('#radio').buttonset();
    $('#graph-radios').buttonset();
    $('#styles').buttonset();
    $('#showMe').click(function() {
        $('#showMeAreaContainer').slideToggle('fast', function() {});
    });
    $('#showMeGraphCode').click(function() {
        $('#showGraphAreaContainer').slideToggle('fast', function() {});
    });
    $('#showTemplateCode').click(function() {
        $('#templateCodeContainer').slideToggle('fast', function() {});
    });
    $('#showQueryCode').click(function() {
        $('#codeContainer').slideToggle('fast', function() {});
    });
    $('input.styler').click(function() {
        setActiveStyleSheet(this.id);
    });
    $('input.templater').click(function() {
        if (this.id.match(/1/)) {
            loadTable1();
        } else if (this.id.match(/2/)) {
            loadTable2();
        } else if (this.id.match(/3/)) {
            loadTable3();
        }
    });
    $('input.grapher').click(function() {
        if (this.id.match(/1/)) {
            loadGraph1();
        } else if (this.id.match(/2/)) {
            loadGraph2();
        }
    });
});

var operators = {
    "=": "is",
    "!=": "isn't", 
    ">": "is greater than/sorts after",
    "<": "is less than/sorts before",
    ">=": "is greater than or equal to",
    "<=": "is less than or equal to",
    "LIKE": "matches", 
    "LOOKUP": "any field matches",
    "ISA": "is a",
    "IS NOT NULL": "exists",
    "IS NULL": "doesn't exist"
};

var isAttributeOp = {
    "=": true,
    "!=": true,
    ">": true,
    "<": true,
    ">=": true,
    "<=": true,
    "LIKE": true,
    "LOOKUP": false,
    "ISA": false,
    "IS NOT NULL": true,
    "IS NULL": true
};

var getViewCount = function() {
    return $("#querybuilder-views").children().length;
}

var getConstraintCount = function() {
    return $("#querybuilder-constraints > tbody").children('tr').length;
}

var getJoinCount = function() {
    return $("#querybuilder-joins").children().length;
}

var getViewPathSuggester = function(rootClass) {
    return function(request, callback) {
        var term = request.term;
        var paths = getPossibleViewPathsFor(rootClass);
        var suggestions = [];
        for (i in paths) {
            if (paths[i].value.substring(0, term.length) == term) {
                suggestions.push(paths[i]);
            }
        }
        callback(suggestions);
    };
};

var getConsPathSuggester = function(rootClass) {
    return function(request, callback) {
        var term = request.term;
        var paths = getPossibleConsPathsFor(rootClass);
        var suggestions = [];
        for (i in paths) {
            if (paths[i].value.substring(0, term.length) == term) {
                suggestions.push(paths[i]);
            }
        }
        callback(suggestions);
    };
};

var getJoinPathSuggester = function(rootClass) {
    return function(request, callback) {
        var term = request.term;
        var paths = getPossibleConsPathsFor(rootClass);
        var suggestions = [];
        for (i in paths) {
            if (paths[i].type == "reference" 
                    && paths[i].value.substring(0, term.length) == term) {
                suggestions.push(paths[i]);
            }
        }
        callback(suggestions);
    };
};

var addToSortOrders = function(path) {
    $('#sortOrderDiv').show();
    var option = document.createElement("option");
    option.value = path;
    option.innerHTML = path;
    $('#sortOrderSelector').append(option);
};

var removeSortOrder = function(path) {
    $('#sortOrderSelector').children('option').each(function() {
        if (this.value == path) {
            $(this).detach();
        }
    });
    if ($('#sortOrderSelector').children('option').length < 1) {
        $('#sortOrderDiv').hide();
    }
};

var addViewLine = function(rootClass) {
    var row = document.createElement("tr");
    var pathCell = document.createElement("td");
    var rootLabel = document.createElement("code");
    rootLabel.innerHTML = rootClass + ".";
    pathCell.appendChild(rootLabel);
    var textBox = document.createElement("input");
    textBox.className = "text-input";
    textBox.type = "text";
    textBox.size = 80;
    textBox.name = "view" + (getViewCount() + 1);
    $(textBox).autocomplete({
        source: getViewPathSuggester(rootClass),
        minLength: 2,
        change: function(event, ui) {
            addToSortOrders(rootClass + "." + $(textBox).val());
        }
    });
    pathCell.appendChild(textBox);
    row.appendChild(pathCell);
    var deleteCell = document.createElement("td");
    var deleteButton = document.createElement("button");
    deleteButton.appendChild(document.createTextNode("delete"));
    $(deleteButton).click(function() {
        $(row).detach();
        removeSortOrder(rootClass + "." + $(textBox).val());
    });
    $(deleteButton).button();
    deleteCell.appendChild(deleteButton);
    row.appendChild(deleteCell);
    $("#querybuilder-views").append(row);
};

var viewPathsFor = {};
var getPossibleViewPathsFor = function(rootClass) {
    if (viewPathsFor[rootClass]) {
        return viewPathsFor[rootClass];
    }
    var paths = getPathsFor(rootClass);
    viewPathsFor[rootClass] = paths;
    return paths;
}

var consPathsFor = {};
var getPossibleConsPathsFor = function(rootClass) {
    if (consPathsFor[rootClass]) {
        return consPathsFor[rootClass];
    }
    var paths = getPathsFor(rootClass, null, 0, true);
    consPathsFor[rootClass] = paths;
    return paths;
}

var getPathsFor = function(rootClass, excludedAttr, level, includeClass) {
    level = level || 0;
    if (level > 4) {
        return [];
    }
    level++;
    var paths = [];
    var cld = model.classes[rootClass];
    var attrs = cld.attributes;
    for (var i = 0; i < attrs.length; i++) {
        if (attrs[i].name == excludedAttr) {
            continue;
        }
        paths.push({
            value: attrs[i].name,
            type: "attribute"
        });
    }
    var refs = cld.references;
    for (var i = 0; i < refs.length; i++) {
        if (includeClass) {
            paths.push({
                value: refs[i].name,
                type: "reference"
            });
        }
        var refPaths = getPathsFor(
                refs[i].referencedType, refs[i].reverseReference, 
                level, includeClass);
        for (var j = 0; j < refPaths.length; j++) {
            paths.push({
                value: refs[i].name + "." + refPaths[j].value,
                type: refPaths[j].type
            });
        }
    }
    var cols = cld.collections;
    for (var i = 0; i < cols.length; i++) {
        if (includeClass) {
            paths.push({
                value: cols[i].name,
                type: "reference"
            });
        }
        var colPaths = getPathsFor(
                cols[i].referencedType, cols[i].reverseReference, 
                level, includeClass);
        for (var j = 0; j < colPaths.length; j++) {
            paths.push({
                value: cols[i].name + "." + colPaths[j].value,
                type: colPaths[j].type
            });
        }
    }
    return paths;
}

var letters = "ABCDEFGHIJKMNOPQRSTUVWXYZ";

var addConstraintLine = function(rootClass, includeISA) {
    var counter = getConstraintCount() + 1;
    if (counter > 1) {
        $('#logic-box').show();
    }
    var row = document.createElement("tr");
    var pathCell = document.createElement("td");
    pathCell.appendChild(document.createTextNode(rootClass + "."));
    var textBox = document.createElement("input");
    textBox.className = "text-input";
    textBox.type = "text";
    textBox.size = 40;
    textBox.name = "constraint" + counter;
    pathCell.appendChild(textBox);
    row.appendChild(pathCell);
    var opCell = document.createElement("td");
    var opInput = getOpInput(counter, null, includeISA);
    opCell.appendChild(opInput);
    row.appendChild(opCell);
    var valueCell = document.createElement("td");
    var valueInput = document.createElement("input");
    valueInput.type = "text";
    valueInput.name = "value" + counter;
    valueCell.appendChild(valueInput);
    row.appendChild(valueCell);
    $(opInput).change(function() {
        if ($(this).val().match(/NULL/)) {
            valueInput.disabled = true;
        } else {
            valueInput.disabled = false;
        }
    });
    $(textBox).autocomplete({
        source: getConsPathSuggester(rootClass),
        minLength: 2,
        select: function(event, ui) {
            var selectedType = ui.item.type;
            if (selectedType == "attribute") {
                enableAttributeOptions(opInput);
            } else {
                enableClassOptions(opInput);
            }
        }
    });
    var deleteCell = document.createElement("td");
    var deleteButton = document.createElement("button");
    deleteButton.innerHTML = "delete";
    $(deleteButton).click(function() {
        $(row).detach();
        if ($('#querybuilder-constraints > tbody').children().length < 2) {
            $('#logic-box').hide();
        };
    });
    $(deleteButton).button();
    deleteCell.appendChild(deleteButton);
    row.appendChild(deleteCell);
    $("#querybuilder-constraints").append(row);
};

var addJoinLine = function(rootClass) {
    var counter = getJoinCount() + 1;
    var row = document.createElement("tr");
    var pathCell = document.createElement("td");
    pathCell.appendChild(document.createTextNode(rootClass + "."));
    var textBox = document.createElement("input");
    textBox.className = "text-input";
    textBox.type = "text";
    textBox.size = 40;
    textBox.name = "join" + counter;
    pathCell.appendChild(textBox);
    row.appendChild(pathCell);
    var styleCell = document.createElement("td");
    var styleDiv = document.createElement("div");
    var innerButton = document.createElement("input");
    innerButton.type = "radio";
    innerButton.id = "inner" + counter;
    innerButton.value = "INNER";
    innerButton.name = "style" + counter;
    innerButton.checked = true;
    styleDiv.appendChild(innerButton);
    var innerLabel = document.createElement("label");
    innerLabel.setAttribute("for", "inner" + counter);
    innerLabel.innerHTML = "INNER";
    styleDiv.appendChild(innerLabel);
    var outerButton = document.createElement("input");
    outerButton.type = "radio";
    outerButton.id = "outer" + counter;
    outerButton.value = "OUTER";
    outerButton.name = "style" + counter;
    styleDiv.appendChild(outerButton);
    var outerLabel = document.createElement("label");
    outerLabel.setAttribute("for", "outer" + counter);
    outerLabel.innerHTML = "OUTER";
    styleDiv.appendChild(outerLabel);
    styleCell.appendChild(styleDiv);
    row.appendChild(styleCell);
    $(styleDiv).buttonset();
    $(textBox).autocomplete({
        source: getJoinPathSuggester(rootClass),
        minLength: 2,
    });
    var deleteCell = document.createElement("td");
    var deleteButton = document.createElement("button");
    deleteButton.innerHTML = "delete";
    $(deleteButton).click(function() {$(row).detach()});
    $(deleteButton).button();
    deleteCell.appendChild(deleteButton);
    row.appendChild(deleteCell);
    $("#querybuilder-joins").append(row);
};

function enableAttributeOptions(opInput) {
    var options = $(opInput).children("option");
    options.each(function() {
        this.disabled = ! isAttributeOp[this.value];
        if (this.selected && this.disabled) {
            this.selected = false;
        }
    });
}

function enableClassOptions(opInput) {
    var options = $(opInput).children("option");
    options.each(function() {
        this.disabled = isAttributeOp[this.value];
        if (this.selected && this.disabled) {
            this.selected = false;
        }
    });
}
    
function getOpInput(counter, cons, includeISA) {
    var opInput = document.createElement("select");
    opInput.name = "op" + counter;
    for (op in operators) {
        if (op == "ISA" && ! includeISA) {
            continue;
        }
        var option = document.createElement("option");
        option.value = op;
        option.innerHTML = operators[op];
        if (cons && op == cons.op) {
            option.selected = "selected";
        }
        opInput.appendChild(option);
    }
    return opInput;
}

getSelectHandler = function(templates) {
    return function(event, ui) {
        if (ui.item) {
            var selectedTemplate = ui.item.value;
            tempSpec = $('#templateSpecific');
            tempSpec.empty();
            var template = templates[selectedTemplate];
            var title = document.createElement("h3");
            title.innerHTML = template.title;
            tempSpec.append(title);
            var table = document.createElement("table");
            for (var i = 0; i < template.constraints.length; i++) {
                var row = document.createElement("tr");
                table.appendChild(row);
                cons = template.constraints[i];
                counter = i + 1;

                var cell1 = document.createElement("td");
                var nameInput = document.createElement("input");
                nameInput.type = "hidden";
                nameInput.name = "constraint" + counter;
                nameInput.value = cons.path;
                cell1.appendChild(nameInput);
                var label = document.createElement("label");
                label.className = "text-input";
                label["for"] = "op" + counter;
                label.innerHTML = cons.path + ": ";
                cell1.appendChild(label);
                row.appendChild(cell1);

                var cell2 = document.createElement("td");
                var opInput = getOpInput(counter, cons, false);
                cell2.appendChild(opInput);
                row.appendChild(cell2);

                var cell3 = document.createElement("td");
                var valueInput = document.createElement("input");
                valueInput.type = "text";
                valueInput.className = "text-input";
                valueInput.name = "value" + counter;
                valueInput.value = cons.value;
                cell3.appendChild(valueInput);
                $(opInput).change(function() {
                    if ($(this).val().match(/NULL/)) {
                        valueInput.disabled = true;
                    } else {
                        valueInput.disabled = false;
                    }
                });
                var codeInput = document.createElement("input");
                codeInput.type = "hidden";
                codeInput.name = "code" + counter;
                codeInput.value = cons.code;
                cell3.appendChild(codeInput);
                row.appendChild(cell3);

            }
            tempSpec.append(table);
        }
    };
};

$(function() {
    $.jsonp({
        url: "http://squirrel.flymine.org/intermine-test/service/templates",
        callbackParameter: "callback",
        data: {
            format: "jsonp"
        },
        success: function( data ) {
            window.availableTemplates = data;
            var names = [];
            for (name in data) {
                names.push({
                    value: name,
                    label: data[name].title
                });
            }
            $('#templateName').autocomplete({
                source: names,
                minLength: 0,
                delay: 0,
                focus: function(event, ui) {
                    $('#templateName').val(ui.item.value);
                    return false;
                },
                select: getSelectHandler(availableTemplates)
            }).focus(function() {
                $(this).autocomplete("search", "");
            }).data( "autocomplete" )._renderItem = function(ul, item) {
                return $("<li></li>").data( "item.autocomplete", item )
                                     .append("<a><strong>" + item.value + "</strong><br/><em>" + item.label + "</em></a>")
                                     .appendTo(ul);
            };
        }
    });
    $.jsonp({
        url: "http://squirrel.flymine.org/intermine-test/service/model",
        callbackParameter: "callback",
        data: {format: "jsonp"},
        success: function( data ) {
            model = data;
            for (name in data.classes) {
                if (data.classes[name].isInterface) {
                    continue;
                }
                var option = document.createElement("option");
                option.value = name;
                var displayName = name;
                if (displayName.match(/s$/)) {
                    displayName += "e";
                }
                displayName += "s";
                option.innerHTML = displayName;
                $("#root-class").append(option);
            }
        }
    });
});
