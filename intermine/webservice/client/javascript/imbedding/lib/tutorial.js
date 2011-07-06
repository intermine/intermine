availableTemplates = null;
var model = {};
var baseUrl = "http://squirrel.flymine.org/intermine-test";
var flyMineBase = "http://squirrel.flymine.org/flymine";
$(function() {
    IMBedding.setBaseUrl(baseUrl);
    Syntax.root = "http://squirrel.flymine.org/imbedding/lib/jquery-syntax/";
    $('.js-snippet').syntax({
        brush: 'javascript', 
        layout: 'list', 
        replace: true,
        tabWidth: 4,
        root: "lib/jquery-syntax/"
    });
    $('.html-snippet').syntax({
        brush: 'html', 
        layout: 'list', 
        replace: true,
        tabWidth: 4,
        root: "lib/jquery-syntax/"
    });
    $('#showGraphAreaContainer').hide();
    loadTable3a();
    loadTable3b();
    loadTable3c();
    loadTable3d();
    loadTable4a();
    loadTable4b();
    loadTable4c();
    loadTable4d();
    loadTable4e();
    loadTable4();
    loadTable5();
    $('#faq').accordion({collapsible: true, autoHeight: false, active: false});
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
              name: "AnatomyTerm_Alleles",
              constraint1: "Gene.alleles.alleleClass",
              op1: "=",
              value1: "*loss of function*",
              constraint2: "Gene.alleles.phenotypeAnnotations.description",
              op2: "=",
              value2: "*eye disc*"

        },
        "#placeholderEx2",
        {openOnLoad: true, baseUrl: flyMineBase}
    );
}
function loadTable2() {
    IMBedding.loadTemplate(
        {
            name:           "Gene_upstreamRegulatoryRegions",

            constraint1:    "Gene",
            op1:            "LOOKUP",
            value1:         "eve",
            code1:          "A"
        },
        "#positionExample",
        {
            baseUrl: flyMineBase,
        }
    );
}
function loadTable3a() {
    IMBedding.loadTemplate(
        {
            name:           "Gene_allGOTerms2",

            constraint1:    "Gene",
            op1:            "LOOKUP",
            value1:         "CG11348",
            code1:          "A"
        },
        "#placeholderEx3a",
        {onTitleClick: "mine", baseUrl: flyMineBase}
    );
}

function loadTable3b() {
    IMBedding.loadTemplate(
        {
            name:           "Gene_allGOTerms2",
            size: 10,

            constraint1:    "Gene",
            op1:            "LOOKUP",
            value1:         "CG11348",
            code1:          "A"
        },
        "#placeholderEx3b",
        {onTitleClick: "none", baseUrl: flyMineBase}
    );
}

function loadTable3c() {
    var callback = function() {
        alert( "You clicked on:\n" + $(this).text());
    };
    IMBedding.loadTemplate(
        {
            name:           "Gene_allGOTerms2",
            size: 10,

            constraint1:    "Gene",
            op1:            "LOOKUP",
            value1:         "CG11348",
            code1:          "A"
        },
        "#placeholderEx3c",
        {onTitleClick: callback, titleHoverCursor: "help", baseUrl: flyMineBase}
    );
}

function loadTable3d() {
    IMBedding.loadTemplate(
        {
            name:           "Gene_allGOTerms2",
            size: 10,

            constraint1:    "Gene",
            op1:            "LOOKUP",
            value1:         "CG11348",
            code1:          "A"
        },
        "#placeholderEx3d",
        {baseUrl: flyMineBase}
    );
}

function loadTable4a() {
    IMBedding.loadTemplate(
        {
            name:           "Organism_GeneDomain_new",
            size: 10,

            constraint1:    "ProteinDomain.proteins.genes.organism.name",
            op1:            "=",
            value1:         "Drosophila melanogaster",
            code1:          "A",

            constraint2:    "ProteinDomain.shortName",
            op2:            "=",
            value2:         "*homeo*",
            code2:          "B"
        },
        "#placeholderEx4a",
        {showExportLinks: false, baseUrl: flyMineBase}
    );
}

function loadTable4b() {
    IMBedding.loadTemplate(
        {
            name:           "Organism_GeneDomain_new",
            size: 10,

            constraint1:    "ProteinDomain.proteins.genes.organism.name",
            op1:            "=",
            value1:         "Drosophila melanogaster",
            code1:          "A",

            constraint2:    "ProteinDomain.shortName",
            op2:            "=",
            value2:         "*homeo*",
            code2:          "B"
        },
        "#placeholderEx4b",
        {showCount: false, baseUrl: flyMineBase}
    );
}
function loadTable4c() {
    IMBedding.loadTemplate(
        {
            name:           "Organism_GeneDomain_new",
            size: 10,

            constraint1:    "ProteinDomain.proteins.genes.organism.name",
            op1:            "=",
            value1:         "Drosophila melanogaster",
            code1:          "A",

            constraint2:    "ProteinDomain.shortName",
            op2:            "=",
            value2:         "*homeo*",
            code2:          "B"
        },
        "#placeholderEx4c",
        {showMineLink: false, baseUrl: flyMineBase}
    );
}
function loadTable4d() {
    IMBedding.loadTemplate(
        {
            name:           "Organism_GeneDomain_new",
            size: 15,

            constraint1:    "ProteinDomain.proteins.genes.organism.name",
            op1:            "=",
            value1:         "Drosophila melanogaster",
            code1:          "A",

            constraint2:    "ProteinDomain.shortName",
            op2:            "=",
            value2:         "*homeo*",
            code2:          "B"
        },
        "#placeholderEx4d",
        {baseUrl: flyMineBase}
    );
}
function loadTable4e() {
    var ops = {
        baseUrl: flyMineBase,
        nextText: "avanti", 
        previousText: "indietro",
        additionText: "carica altre [x] righe",
        allRowsText: "carica tutte righe",
        emptyCellText: "[NIENTE]",
        collapseHelpText: "chiudi tabella",
        expandHelpText: "mostra tabella",
        exportCSVText: "Esporta il risultato in formato CSV", 
        exportTSVText: "Esporta il risultato in formato TSV", 
        mineLinkText: "Guarda in Mine",
        resultsDescriptionText: "il risultato della loro interrogazione",
        queryTitleText: "Organismo --> Tutti genia con un dominio specifico",
        countText: "[x] righe",
        thousandsSeparator: "."
    };
    IMBedding.loadTemplate(
        {
            name:           "Organism_GeneDomain_new",
            size: 15,

            constraint1:    "ProteinDomain.proteins.genes.organism.name",
            op1:            "=",
            value1:         "Drosophila melanogaster",
            code1:          "A",

            constraint2:    "ProteinDomain.shortName",
            op2:            "=",
            value2:         "*homeo*",
            code2:          "B"
        },
        "#placeholderEx4e", ops
    );
}
function loadTable4() {
    IMBedding.loadTemplate(
        {
            name:           "Chromosome_Gene",
        
            constraint1:    "Gene.chromosome.primaryIdentifier",
            op1:            "=",
            value1:         "2L",
            code1:          "A",
        
            constraint2:    "Gene.organism.name",
            op2:            "=",
            value2:         "Drosophila melanogaster",
            code2:          "B"
        },
        '#placeholder1',
        {baseUrl: flyMineBase}
    );
}

function loadTable5() {
    IMBedding.loadTemplate(
        {
            name:           "Chromosome_Gene",
        
            constraint1:    "Gene.chromosome.primaryIdentifier",
            op1:            "=",
            value1:         "2L",
            code1:          "A",
        
            constraint2:    "Gene.organism.name",
            op2:            "=",
            value2:         "Drosophila melanogaster",
            code2:          "B"
        },
        '#styleDemo',
        {baseUrl: flyMineBase}
    );
}

function loadGraph1() {
    IMBedding.loadQuery(
        {
            select: ["Employee.age", "Employee.department.company.name"],
            from: "testmodel",
            where: [
                {path: "Employee.department.company.name", op: "!=", value: "Diffic*"},
                {path: "Employee.department.company.name", op: "!=", value: "Company*"}
            ]
        },
        {size: 1000, format: "jsonpobjects"},
        function(resultSet) {
            var graphData = [];
            var ageSum = 0;
            var countAtAgeAtCompany = {};
            for (i in resultSet.results) {
                var employee = resultSet.results[i];
                var age = employee.age;
                var company = employee.department.company.name
                ageSum += age;
                if (! countAtAgeAtCompany[company]) {
                    countAtAgeAtCompany[company] = {};
                }
                if (! countAtAgeAtCompany[company][age]) {
                    countAtAgeAtCompany[company][age] = 1;
                } else {
                    countAtAgeAtCompany[company][age] 
                        = countAtAgeAtCompany[company][age] + 1;
                }
            }
            for (name in countAtAgeAtCompany) {
                var company = countAtAgeAtCompany[name];
                var count = 0;
                var sum = 0;
                var companyData = {
                    data: []
                }
                for (age in company) {
                    count += company[age];
                    sum += age * company[age];
                    companyData.data.push([age, company[age]]);
                }
                var companyAverage = parseInt(sum / count);
                companyData.label = 
                    name + " [av: " + companyAverage + "]";
                graphData.push(companyData);
            }
            var options = {
                series: {
                    stack: true,
                    lines: {show: false},
                    bars: {show: true, barWidth: 0.9}
                },
                legend: {position: "nw"}
            };
            var plot = $.plot("#graph", graphData, options);
            var averageAge = parseInt(ageSum / resultSet.results.length);
            $('<span></span>').html("Age (average:" + averageAge + ")")
                              .addClass("axis-label").appendTo('#graph');
        }
    );
}

function showTooltip(x, y, content) {
     $('<div id="tooltip"></div>').html(content)
                                  .addClass("tooltip")
                                  .css( {
        position:             'absolute',
        display:              'none',
        top:                  y + 10,
        left:                 x + 10,
        border:               '2px solid #FF0084',
        padding:              '5px',
        'border-radius':      '5px',
        '-moz-border-radius': '5px',
        'background-color':   '#FE45A5',
        color:                "white",
        opacity:              0.70
      }).appendTo("body").fadeIn(200);
}

function findLineByLeastSquares(datapoints) {
    var sum_x = 0;
    var sum_y = 0;
    var sum_xy = 0;
    var sum_xx = 0;
    var count = 0;
    
    /*
     * We'll use those variables for faster read/write access.
     */
    var x = 0;
    var y = 0;
    var values_length = datapoints.length;
    
    /*
     * Nothing to do.
     */
    if (values_length === 0) {
        return [ ];
    }
    
    /*
     * Calculate the sum for each of the parts necessary.
     */
    for (var i = 0; i < values_length; i++) {
        x = datapoints[i][0];
        y = datapoints[i][1];
        sum_x += x;
        sum_y += y;
        sum_xx += x*x;
        sum_xy += x*y;
        count++;
    }
    
    /*
     * Calculate m and b for the formular:
     * y = x * m + b
     */
    var m = (count*sum_xy - sum_x*sum_y) / (count*sum_xx - sum_x*sum_x);
    var b = (sum_y/count) - (m*sum_x)/count;
    
    /*
     * We will make the x and y result line now
     */
    var data = [];
    
    for (var i = 0; i < values_length; i++) {
        x = datapoints[i][0];
        y = x * m + b;
        data.push([x, y]);
    }
    
    return data;
}

function loadGraph2() {
    IMBedding.loadQuery(
        {
            select: ["Manager.age", "Manager.seniority", "Manager.name"],
            where: [
                {path: "Manager.age", op: ">", value: 30},
                {path: "Manager.department.company.name", op: "!=", value: "Diffic*"},
                {path: "Manager.department.company.name", op: "!=", value: "Company*"}
                ],
            from: "testmodel"
        },
        {size: 1000, format: "jsonpobjects"},
        function(resultSet) {
            var graphData = [];
            var managers = {};
            var ageVsSen = {
                label: "Seniority by Age",
                color: "rgb(0, 115, 234)",
                points: {show: true},
                data: []
            };
            var trendLine = {
                label: "Trend Line",
                color: "rgb(255, 0, 132)",
                lines: {show: true}
            };
            for (i in resultSet.results) {
                var manager = resultSet.results[i];
                var seniority =manager.seniority;
                var age = manager.age;
                ageVsSen.data.push([age, seniority]);
                managers[age + "-" + seniority] = manager;
            }
            trendLine.data = findLineByLeastSquares(ageVsSen.data);
            graphData.push(ageVsSen);
            graphData.push(trendLine);
            var options = {
                grid: {hoverable: true, clickable: true},
                legend: {position: "nw"}
            };
            var plot = $.plot("#graph", graphData, options);
            $('#graph').bind("plothover", function(event, pos, item) {
                if (item) {
                    $("#tooltip").remove();
                    var x = item.datapoint[0].toFixed(2),
                        y = item.datapoint[1].toFixed(2);
                    var key = parseInt(x) + "-" + parseInt(y);
                    var manager = managers[key];
                    if (manager) {
                        showTooltip(item.pageX, item.pageY, manager.name);
                    }
                }
                else {
                    $("#tooltip").remove();
                    previousPoint = null;            
                }
            }).bind("plotclick", function(event, pos, item) {
                if (item) {
                    var x = item.datapoint[0].toFixed(2),
                        y = item.datapoint[1].toFixed(2);
                    var key = parseInt(x) + "-" + parseInt(y);
                    var manager = managers[key];
                    var url = baseUrl + "/objectDetails.do?id=" 
                                + manager.objectId;
                    window.location.replace(url);
                }
            });
            $('<span></span>').html("Age").addClass("axis-label").appendTo('#graph');
        }
    );
}

function loadDepartmentSummary(departmentName) {
    departmentName = departmentName || "Sales";
    IMBedding.loadQuery(
        {
            select: [
                "Department.name", "Department.company.name", 
                "Department.employees.name", 
                "Department.employees.age", 
                "Department.employees.address.address", 
                "Department.manager.name",
                "Department.company.departments.name"
            ],
            where: [
                {
                    path: "Department.name", 
                    op: "=", value: departmentName
                },
                {
                    path: "Department.company.name", 
                    op: "=", value: "Wernham-Hogg"
                }
            ],
            from: "testmodel"
        },
        {size: 1000, format: "jsonpobjects"},
        function(resultSet) {
            var department = resultSet.results[0];
            var summary = $('<div></div>').addClass("department-summary");
            var title = document.createElement("h2");
            title.appendChild(document.createTextNode(
                        department.company.name + ": "));
            var depSelect = document.createElement("select");
            $(depSelect).button();
            var noOfDeps = department.company.departments.length;
            for (var i = 0; i < noOfDeps; i++) {
                var dep = department.company.departments[i];
                var depOption = document.createElement("option");
                if (dep.objectId == department.objectId) {
                    depOption.selected = true;
                }
                depOption.innerHTML = dep.name;
                depSelect.appendChild(depOption);
            }
            title.appendChild(depSelect);
            $(depSelect).change(function() {
                loadDepartmentSummary($(depSelect).val());
            });
            summary.append(title);
            var subtitle = document.createElement("h3");
            subtitle.innerHTML = "Manager: " + department.manager.name;
            summary.append(subtitle);
            var employeeCount = department.employees.length;
            var list = document.createElement("ul");
            for (var i = 0; i < employeeCount; i++) {
                var employee = department.employees[i];
                if (employee.objectId == department.manager.objectId) {
                    continue;
                }
                var item = document.createElement("li");
                var link = document.createElement("a");
                link.href = baseUrl + "/objectDetails.do?id=" 
                                    + employee.objectId;
                link.innerHTML = employee.name;
                item.appendChild(link);
                $(link).hover(
                    getEmployeeTooltipper(employee),
                    function(event) {
                        $('.tooltip').remove();
                    }
                );
                list.appendChild(item);
            }
            summary.append(list);
            $('#graph').empty().append(summary);
        }
    );
};

var getEmployeeTooltipper = function(employee) {
    return function(event) {
        var tooltip = document.createElement("div");
        var ttTitle = document.createElement("h3");
        ttTitle.innerHTML = employee.name;
        tooltip.appendChild(ttTitle);
        var table = document.createElement("table");
        table.className = "employee-details";
        var rowA = document.createElement("tr");
        var cellA1 = document.createElement("td");
        cellA1.innerHTML = "Age:";
        cellA1.className = "summary-cell";
        var cellA2 = document.createElement("td");
        cellA2.innerHTML = employee.age;
        cellA2.align = "right";
        cellA2.className = "summary-cell";
        rowA.appendChild(cellA1);
        rowA.appendChild(cellA2);
        table.appendChild(rowA);
        var rowB = document.createElement("tr");
        var cellB1 = document.createElement("td");
        cellB1.innerHTML = "Address:";
        cellB1.className = "summary-cell";
        var cellB2 = document.createElement("td");
        cellB2.innerHTML = employee.address.address;
        cellB2.className = "summary-cell";
        rowB.appendChild(cellB1);
        rowB.appendChild(cellB2);
        table.appendChild(rowB);
        tooltip.appendChild(table);
        var x = event.pageX;
        var y = event.pageY;
        showTooltip(x, y, tooltip);
    };
};

var toggleExample = function() {
    $('#exampleCode').slideToggle('fast', function(){});
    $('#querybuilder').slideToggle('fast', function(){});
};

var firstline = "/* Below is the Javascript to load this table using ajax:\n"
+ "You can cut and paste this into a page to get started.\n"
+ "Don't forget you need to include the imbedding.js library"
+ " in your head */\n\n"
+ '/* You only need to do the following once */' + "\n";

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

var getQueryFromForm = function() {
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
    return query;
};

var getQueryFromBox = function() {
    var format = $('#boxselector').val();
    if (format == "xml") {
        return $('#xmlarea').val();
    } else if (format == "json") {
        var jsonString = $('#jsonarea').val();
        var parsedQuery;
        try {
            parsedQuery = eval('(' + jsonString + ')');
        } catch(e) {
            alert("Something is wrong with your json\n" + e);
        }
        return parsedQuery;
    } else {
        return null;
    }
};

var makeQueryDisplayStrings = function(query) {
    if (! jQuery.isPlainObject(query)) {
        // assume it is xml
        var xmlString = query
                            .replace(/&gt;/g, "&amp;gt;")
                            .replace(/&lt;/g, "&amp;lt;")
                            .replace(/</g, "&lt;")
                            .replace(/>/g, "&gt;");
        return [null, xmlString];
    }
    // Make the displayed json string
    // It needs munging to prettify it as well
    var jsonString = JSON.stringify(query);
    var keys = jsonString.match(/"\w+":/g);
    if (keys) {
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            var newKey = key.replace(/"/g, "").replace(":", ": ");
            jsonString = jsonString.replace(key, newKey);
        }
    }
    jsonString = jsonString.replace(/\}\],/g, "}\n\t],")
                            .replace(/\},\{/g, "},\n\t{")
                            .replace(/\],/g, "],\n\t")
                            .replace(/^\{/, "{\n\t")
                            .replace(/\[\{/g, "[\n\t{")
                            .replace(/\}$/, "\n}")
                            .replace(/",/g, "\", ")
                            .replace(/\t\{/g, "\t\t{");
    var xmlString = IMBedding.makeQueryXML(query)
                            .replace(/&gt;/g, "&amp;gt;")
                            .replace(/&lt;/g, "&amp;lt;")
                            .replace(/</g, "&lt;")
                            .replace(/>/g, "&gt;");
    return [jsonString, xmlString];
};

function loadUserQuery() {
    var source = $('input:radio[name=query]:checked').val();
    var query;
    if (source == "querybuilder") {
        query = getQueryFromForm();
    } else {
        query = getQueryFromBox();
    }
    if (! query) {
        return false;
    }
    var displayStrings = makeQueryDisplayStrings(query);
    if (! displayStrings) {
        return false;
    }

    var data = {size: 10};
    var newValue = firstline + "IMBedding.setBaseUrl('";

    var urlToQuery = flyMineBase;
    newValue += urlToQuery + "');\n\n";
    var opts = {baseUrl: urlToQuery};

    if (displayStrings[0]) {
        newValue 
            += "/*You can define the query as a regular javascript object*/\n"
            +  "var query = " + displayStrings[0] + ";\n";
    }
    if (displayStrings[1]) {
        newValue
            += "\n/*A query can be defined as XML*/\n"
            + "var query = '" + displayStrings[1] + "';\n\n";
    }
    newValue 
        += "/*Execution is the same in either case*/\n"
        + "IMBedding.loadQuery(query, {size: 10}, '#placeholder');";

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
        IMBedding.loadQuery(query, data, "#placeholder2", opts);
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
    icon.style["float"] = "left";
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
    var newValue = firstline + "IMBedding.setBaseUrl('";

    var urlToQuery;
    urlToQuery = flyMineBase;
    newValue += urlToQuery + "');\n\n";
    var opts = {baseUrl: urlToQuery};
    newValue += "IMBedding.loadTemplate(\n\t{\n\t";

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

    IMBedding.loadTemplate(data, '#placeholder3', opts);
};

$(function() {
        $('#tabs').tabs();
});
var oldRootClass = "Employee";
$(function() {
    $("input:button").button();
    $("button").button();
    $("#positionExButton").click(function() {
        loadTable2();
        $(this).hide();
    });
    $('#librHeadButton').click(function() {
        $('#librariesHead').slideToggle();
    });
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
                    $('#sortOrderSelector').empty();
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
    $('#source-radios-t').buttonset();
    $('.sourcer').click(function () {
        if (this.value == "testmodel") {
            loadTemplateInfo(
                "http://squirrel.flymine.org/intermine-test/service/templates");
        } else if (this.value == "flymine") {
            loadTemplateInfo(flyMineBase + "/service/templates");
        }
    });
    $('#source-radios-q').buttonset();
    $('.model-sourcer').click(function () {
        if (this.value == "testmodel") {
            loadModel(
                "http://squirrel.flymine.org/intermine-test/service/model");
        } else if (this.value == "flymine") {
            loadModel(flyMineBase + "/service/model");
        }
    });
    $('#sortOrderSelector').button();
    $('#sortDirectionDiv').buttonset();
    $('#boxselector').change(function() {
        if ($(this).val() == "xml") {
            $('#xmlarea').show();
            $('#jsonarea').hide();
        } else {
            $('#xmlarea').hide();
            $('#jsonarea').show();
        }
    });
    $("input:checkbox").button();
    $('#radio').buttonset();
    $('#graph-radios').buttonset();
    $('#query-radios').buttonset();
    $('.styles').buttonset();
    $('#querybuilder-opt').click(function() {
        $('#querybuilder').slideDown('fast', function() {});
        $('#cutandpaste').slideUp('fast', function() {});
    });
    $('#cutandpaste-opt').click(function() {
        $('#querybuilder').slideUp('fast', function() {});
        $('#cutandpaste').slideDown('fast', function() {});
    });
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
        setActiveStyleSheet(this.value);
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
        } else if (this.id.match(/3/)) {
            loadDepartmentSummary();
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
    if (level > 3) {
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
        minLength: 2
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

var loadTemplateInfo = function(url) {
    $.jsonp({
        url: url,
        callbackParameter: "callback",
        data: {
            format: "jsonp"
        },
        success: function( data ) {
            window.availableTemplates = data.templates;
            var names = [];
            for (name in data.templates) {
                names.push({
                    value: name,
                    label: data.templates[name].title
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
};

var loadModel = function(url) {
    $.jsonp({
        url: url,
        callbackParameter: "callback",
        data: {format: "jsonp"},
        success: function( data ) {
            model = data.model;
            $("#root-class").children('option').remove();
            var names = [];
            for (name in model.classes) {
                names.push(name);
            };
            names = names.sort();
            for (var i = 0; i < names.length; i++) {
                var name = names[i];
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
};


$(function() {
    loadTemplateInfo(flyMineBase + "/service/templates");
    loadModel(flyMineBase + "/service/model");
});
