<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="javax.servlet.jsp.jstl.core.LoopTagStatus" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<!-- drosophilaHomologueDisplayer.jsp -->

<c:set var="backslash" value="\\"/>
<c:set var="dblBackslash" value="\\\\"/>

<div>

<h3>Drosophila 12 genomes homology</h3>
<div id="phylotree"></div>
<script type="text/javascript">
window.onload = function() {

  var homologues = {
  <c:forEach items="${homologues}" var="entry" varStatus="homologuesLoop">
    ${entry.key}: [
        <c:set var="genes" value="${entry.value}" />
        <c:forEach items="${genes}" var="resultElement" varStatus="genesLoop">
        { 
          uri: "objectDetails.do?id=${resultElement.id}",
          text: '${fn:replace(resultElement.field, backslash, dblBackslash)}'
        }${not genesLoop.last ? ',' : ''} 
        </c:forEach>
    ]${not homologuesLoop.last ? ',' : ''}
  </c:forEach>
  };

  console.log(homologues);

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
                        "absent": [{fill: 'orange', stroke: 'white'}]
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
                                                                name: [{Text: "D. melanogaster"}],
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
                                                                        name: [{Text: "D. simulans"}],
                                                                        branch_length: [{Text: 0.1}],
                                                                        annotation: [{
                                                                            desc: [{Text: "Base of Many Coffees"}], 
                                                                            uri: [{Text: "http://en.wikipedia.org/wiki/Espresso"}]
                                                                        }],
                                                                        chart: [
                                                                            { 
                                                                                group: [{Text: (homologues.simulans[0] ? "present" : "absent")}],
                                                                                text: [{Text: (homologues.simulans[0] ? homologues.simulans[0].text : '')}], 
                                                                                uri: [{Text: (homologues.simulans[0] ? homologues.simulans[0].uri : '')}] 
                                                                            }
                                                                        ]
                                                                    },
                                                                    {
                                                                        name: [{Text: "D. sechellia"}],
                                                                        branch_length: [{Text:0.1}],
                                                                        annotation: [{
                                                                            desc: [{Text: "Base of Many Coffees"}], 
                                                                            uri: [{Text: "http://en.wikipedia.org/wiki/Espresso"}]
                                                                        }],
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
                                                                name: [{Text: "D. yakuba"}],
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
                                                                name: [{Text: "D. erecta"}],
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
                                                name: [{Text: "D. ananassae"}], 
                                                branch_length: [{Text: "0.4"}],
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
                                                name: [{Text: "D. pseudoobscura"}],
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
                                                name: [{Text: "D. persimilis"}],
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
                                chart: [
                                    { 
                                        group: [{Text: (homologues.willistoni[0] ? "present" : "absent")}],
                                        text: [{Text: (homologues.willistoni[0] ? homologues.willistoni[0].text : '')}], 
                                        uri: [{Text: (homologues.willistoni[0] ? homologues.willistoni[0].uri : '')}] 
                                    }
                                ],
                                name: [{Text: "D willistoni"}]
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
                                    name: [{Text: "D. mojavensis"}],
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
                                    name: [{Text: "D. virilis"}],
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
                            name: [{Text: "D. grimshawi"}],
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

  var data = { newick: '(((((((D. melanogaster:6,(D. simulans:5,D. schellia:5)),(D. yakuba:6,D. erecta:6))melanogaster subgroup,D. ananassae:8)melanogaster group,(D. pseudoobscura:8,D. persimilis:8)obscura group),(D.willistoni:9)willistoni group))Sophophora,(((D.mojavensis:9)repleta group,(D.virilis:9)virilis group),(D.grimshawi:10)Hawaiian Drosophila)Drosophila)Drosophilidae;'};
  Smits.PhyloCanvas.Render.Style.line.stroke = '#8931bc';
  Smits.PhyloCanvas.Render.Style.highlightedEdgeCircle.fill = '#8931bc';
  Smits.PhyloCanvas.Render.Style.line["stroke-width"] = 3;
  Smits.PhyloCanvas.Render.Style.line["stroke-linecap"] = "round";
  Smits.PhyloCanvas.Render.Style.line["stroke-linejoin"] = "round";
  Smits.PhyloCanvas.Render.Parameters.Rectangular.bufferX = "270";
  var pylocanvas = new Smits.PhyloCanvas({json: phyloJson}, 'phylotree', 670, 500);
};
</script>

</div>

<!-- /drosophilaHomologueDisplayer.jsp -->
