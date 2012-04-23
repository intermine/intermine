<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="javax.servlet.jsp.jstl.core.LoopTagStatus" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<!-- drosophilaHomologueDisplayer.jsp -->
<c:if test="${willBeDisplayed}">

<c:set var="backslash" value="\\"/>
<c:set var="dblBackslash" value="\\\\"/>

<div>

<h3 class="goog">Drosophila 12 genomes homology</h3>
<c:if test="${isRecentred}">
The 12 genomes homology only records homologues of <i>D. melanogaster</i> genes. So this tree
displays the homologues of the D. melanogaster homologue of ${origSymbol}
</c:if>
<div id="phylotree">Loading &hellip;</div>
<p>
    Data from the 12 genomes project
</p>
<script type="text/javascript">
var baseUrl = "/${WEB_PROPERTIES['webapp.path']}/report.do?id=";
var homologues = {
<c:forEach items="${homologues}" var="entry" varStatus="homologuesLoop">
${entry.key}: [
    <c:set var="genes" value="${entry.value}" />
    <c:forEach items="${genes}" var="resultElement" varStatus="genesLoop">
    {
        uri: "report.do?id=${resultElement.id}",
        text: '${fn:replace(resultElement.field, backslash, dblBackslash)}'
    }${not genesLoop.last ? ',' : ''}
    </c:forEach>
]${not homologuesLoop.last ? ',' : ''}
</c:forEach>
};
window.onload = function() {

  var phyloJson = {
phylogeny: [
    {
        render: [
            {
                charts: [
                    {
                        group: [{type: "binary", thickness: 120}]
                    }
                ],
                styles: [
                    {
                        "present": [{fill: '#dcc8e2', stroke: 'white', fg: '#8931bc' }],
                        "absent": [{fill: 'white', stroke: 'white'}],
                        "species": [{"font-style": 'italic'}]
                    }
                ]
            }
        ],
        clade: [
            {
                branch_length: [{Text: 0.02}],
                clade: [
                    {
                        branch_length: [{Text: 0.05}],
                        name: [{Text: "Sophophora"}],
                        clade: [
                            {
                                branch_length: [{Text: 0.2}],
                                clade: [
                                    {
                                        branch_length: [{Text: 0.1}],
                                        clade: [
                                            {
                                                branch_length: [{Text: 0.1}],
                                                clade: [
                                                    {
                                                        branch_length: [{Text: 0.1}],
                                                        clade: [
                                                            {
                                                                branch_length: [{Text: 0.2}],
                                                                name: [{Text: "D. melanogaster", style: "species"}],
                                                                annotation: ("${organismIds['melanogaster']}") ? [{
                                                                    desc: [{ Text: "See organism details" }],
                                                                    uri: [{ Text: baseUrl + "${organismIds['melanogaster']}" }]
                                                                }] : null,
                                                                chart: [
                                                                    {
                                                                        group: [{Text: (homologues.melanogaster[0] ? "present" : "absent")}],
                                                                        text: [{Text: (homologues.melanogaster[0] ? homologues.melanogaster[0].text : '')}],
                                                                        uri: [{Text: (homologues.melanogaster[0] ? homologues.melanogaster[0].uri : '')}]
                                                                    }
                                                                ]
                                                            }, // end of melanogaster
                                                            {
                                                                branch_length: [{Text: 0.1}],
                                                                clade:[
                                                                    {
                                                                        name: [{Text: "D. simulans", style: "species"}],
                                                                        branch_length: [{Text: 0.1}],
                                                                        annotation: ("${organismIds['simulans']}") ? [{
                                                                            desc: [{ Text: "See organism details" }],
                                                                            uri: [{ Text: baseUrl + "${organismIds['simulans']}" }]
                                                                        }] : null,
                                                                        chart: [
                                                                            {
                                                                                group: [{Text: (homologues.simulans[0] ? "present" : "absent")}],
                                                                                text: [{Text: (homologues.simulans[0] ? homologues.simulans[0].text : '')}],
                                                                                uri: [{Text: (homologues.simulans[0] ? homologues.simulans[0].uri : '')}]
                                                                            }
                                                                        ]
                                                                    },
                                                                    {
                                                                        name: [{Text: "D. sechellia", style: "species"}],
                                                                        branch_length: [{Text:0.1}],
                                                                        annotation: ("${organismIds['sechellia']}") ? [{
                                                                            desc: [{ Text: "See organism details" }],
                                                                            uri: [{ Text: baseUrl + "${organismIds['sechellia']}" }]
                                                                        }] : null,
                                                                        chart: [
                                                                            {
                                                                                group: [{Text: (homologues.sechellia[0] ? "present" : "absent")}],
                                                                                text: [{Text: (homologues.sechellia[0] ? homologues.sechellia[0].text : '')}],
                                                                                uri: [{Text: (homologues.sechellia[0] ? homologues.sechellia[0].uri : '')}]
                                                                            }
                                                                        ]
                                                                    }
                                                                ]
                                                            }
                                                        ]
                                                    },
                                                    {
                                                        branch_length: [{Text: 0.1}],
                                                        clade: [
                                                            {
                                                                branch_length: [{Text: 0.2}],
                                                                name: [{Text: "D. yakuba", style: "species"}],
                                                                annotation: ("${organismIds['yakuba']}") ? [{
                                                                    desc: [{ Text: "See organism details" }],
                                                                    uri: [{ Text: baseUrl + "${organismIds['yakuba']}" }]
                                                                }] : null,
                                                                chart: [
                                                                    {
                                                                        group: [{Text: (homologues.yakuba[0] ? "present" : "absent")}],
                                                                        text: [{Text: (homologues.yakuba[0] ? homologues.yakuba[0].text : '')}],
                                                                        uri: [{Text: (homologues.yakuba[0] ? homologues.yakuba[0].uri : '')}]
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                branch_length: [{Text: 0.2}],
                                                                name: [{Text: "D. erecta", style: "species"}],
                                                                annotation: ("${organismIds['erecta']}") ? [{
                                                                    desc: [{ Text: "See organism details" }],
                                                                    uri: [{ Text: baseUrl + "${organismIds['erecta']}" }]
                                                                }] : null,
                                                                chart: [
                                                                    {
                                                                        group: [{Text: (homologues.erecta[0] ? "present" : "absent")}],
                                                                        text: [{Text: (homologues.erecta[0] ? homologues.erecta[0].text : '')}],
                                                                        uri: [{Text: (homologues.erecta[0] ? homologues.erecta[0].uri : '')}]
                                                                    }
                                                                ]
                                                            }
                                                        ]
                                                    }
                                                ]
                                            },
                                            {
                                                name: [{Text: "D. ananassae", style: "species"}],
                                                branch_length: [{Text: "0.4"}],
                                                annotation: ("${organismIds['ananassae']}") ? [{
                                                    desc: [{ Text: "See organism details" }],
                                                    uri: [{ Text: baseUrl + "${organismIds['ananassae']}" }]
                                                }] : null,
                                                chart: [
                                                    {
                                                        group: [{Text: (homologues.ananassae[0] ? "present" : "absent")}],
                                                        text: [{Text: (homologues.ananassae[0] ? homologues.ananassae[0].text : '')}],
                                                        uri: [{Text: (homologues.ananassae[0] ? homologues.ananassae[0].uri : '')}]
                                                    }
                                                ]
                                            }
                                        ]
                                    },
                                    {
                                        branch_length: [{Text: 0.4}],
                                        clade: [
                                            {
                                                branch_length: [{Text: 0.1}],
                                                name: [{Text: "D. pseudoobscura", style: "species"}],
                                                annotation: ("${organismIds['pseudoobscura']}") ? [{
                                                    desc: [{ Text: "See organism details" }],
                                                    uri: [{ Text: baseUrl + "${organismIds['pseudoobscura']}" }]
                                                }] : null,
                                                chart: [
                                                    {
                                                        group: [{Text: (homologues.pseudoobscura[0] ? "present" : "absent")}],
                                                        text: [{Text: (homologues.pseudoobscura[0] ? homologues.pseudoobscura[0].text : '')}],
                                                        uri: [{Text: (homologues.pseudoobscura[0] ? homologues.pseudoobscura[0].uri : '')}]
                                                    }
                                                ]
                                            },
                                            {
                                                branch_length: [{Text: 0.1}],
                                                name: [{Text: "D. persimilis", style: "species"}],
                                                annotation: ("${organismIds['persimilis']}") ? [{
                                                    desc: [{ Text: "See organism details" }],
                                                    uri: [{ Text: baseUrl + "${organismIds['persimilis']}" }]
                                                }] : null,
                                                chart: [
                                                    {
                                                        group: [{Text: (homologues.persimilis[0] ? "present" : "absent")}],
                                                        text: [{Text: (homologues.persimilis[0] ? homologues.persimilis[0].text : '')}],
                                                        uri: [{Text: (homologues.persimilis[0] ? homologues.persimilis[0].uri : '')}]
                                                    }
                                                ]
                                            }
                                        ]
                                    },
                                ]
                            },
                            {
                                branch_length: [{Text: 0.7}],
                                annotation: ("${organismIds['willistoni']}") ? [{
                                    desc: [{ Text: "See organism details" }],
                                    uri: [{ Text: baseUrl + "${organismIds['willistoni']}" }]
                                }] : null,
                                chart: [
                                    {
                                        group: [{Text: (homologues.willistoni[0] ? "present" : "absent")}],
                                        text: [{Text: (homologues.willistoni[0] ? homologues.willistoni[0].text : '')}],
                                        uri: [{Text: (homologues.willistoni[0] ? homologues.willistoni[0].uri : '')}]
                                    }
                                ],
                                name: [{Text: "D willistoni", style: "species"}]
                            }
                        ]
                },
                {
                    branch_length: [{Text: 0.1}],
                    name: [{Text: "Drosophila"}],
                    clade: [
                        {
                            branch_length: [{Text: 0.2}],
                            clade: [
                                {
                                    branch_length: [{Text: 0.45}],
                                    name: [{Text: "D. mojavensis", style: "species"}],
                                    annotation: ("${organismIds['mojavensis']}") ? [{
                                        desc: [{ Text: "See organism details" }],
                                        uri: [{ Text: baseUrl + "${organismIds['mojavensis']}" }]
                                    }] : null,
                                    chart: [
                                        {
                                            group: [{Text: (homologues.mojavensis[0] ? "present" : "absent")}],
                                            text: [{Text: (homologues.mojavensis[0] ? homologues.mojavensis[0].text : '')}],
                                            uri: [{Text: (homologues.mojavensis[0] ? homologues.mojavensis[0].uri : '')}]
                                        }
                                    ]
                                },
                                {
                                    branch_length: [{Text: 0.45}],
                                    name: [{Text: "D. virilis", style: "species"}],
                                    annotation: ("${organismIds['virilis']}") ? [{
                                        desc: [{ Text: "See organism details" }],
                                        uri: [{ Text: baseUrl + "${organismIds['virilis']}" }]
                                    }] : null,
                                    chart: [
                                        {
                                            group: [{Text: (homologues.virilis[0] ? "present" : "absent")}],
                                            text: [{Text: (homologues.virilis[0] ? homologues.virilis[0].text : '')}],
                                            uri: [{Text: (homologues.virilis[0] ? homologues.virilis[0].uri : '')}]
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            branch_length: [{Text: 0.65}],
                            name: [{Text: "D. grimshawi", style: "species"}],
                            annotation: ("${organismIds['grimshawi']}") ? [{
                                desc: [{ Text: "See organism details" }],
                                uri: [{ Text: baseUrl + "${organismIds['grimshawi']}" }]
                            }] : null,
                            chart: [
                                {
                                    group: [{Text: (homologues.grimshawi[0] ? "present" : "absent")}],
                                    text: [{Text: (homologues.grimshawi[0] ? homologues.grimshawi[0].text : '')}],
                                    uri: [{Text: (homologues.grimshawi[0] ? homologues.grimshawi[0].uri : '')}]
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ]
    }]};
  jQuery('#phylotree').html('');
  var data = { newick: '(((((((D. melanogaster:6,(D. simulans:5,D. schellia:5)),(D. yakuba:6,D. erecta:6))melanogaster subgroup,D. ananassae:8)melanogaster group,(D. pseudoobscura:8,D. persimilis:8)obscura group),(D.willistoni:9)willistoni group))Sophophora,(((D.mojavensis:9)repleta group,(D.virilis:9)virilis group),(D.grimshawi:10)Hawaiian Drosophila)Drosophila)Drosophilidae;'};
  Smits.PhyloCanvas.Render.Style.line.stroke = '#8931bc';
  Smits.PhyloCanvas.Render.Style.highlightedEdgeCircle.fill = '#8931bc';
  Smits.PhyloCanvas.Render.Style.line["stroke-width"] = 3;
  Smits.PhyloCanvas.Render.Style.line["stroke-linecap"] = "round";
  Smits.PhyloCanvas.Render.Style.line["stroke-linejoin"] = "round";
  Smits.PhyloCanvas.Render.Parameters.Rectangular.bufferX = "270";
  var pylocanvas = new Smits.PhyloCanvas({json: phyloJson}, 'phylotree', 600, 300);
};
</script>

</div>

</c:if>
<!-- /drosophilaHomologueDisplayer.jsp -->
