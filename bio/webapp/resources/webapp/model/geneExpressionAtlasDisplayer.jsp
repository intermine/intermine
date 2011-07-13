<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<style>
  #gene-expression-atlas div.chart { float:left; }
  #gene-expression-atlas div.sidebar { float:right; width:35%; }
  #gene-expression-atlas div.sidebar p.small { font-size:11px; margin:5px 0 16px 0; }
  #gene-expression-atlas div.sidebar div.legend ul { margin-top:4px; }
  #gene-expression-atlas div.sidebar div.legend span { border:1px solid #000; display:inline-block; height:15px; width:20px; }
  #gene-expression-atlas div.sidebar div.legend span.up { background:#59BB14; }
  #gene-expression-atlas div.sidebar div.legend span.down { background:#0000FF; }
</style>

<div id="gene-expression-atlas">
<h3 class="goog">Gene Expression Atlas Expressions</h3>

<div class="sidebar">
  <div class="legend">
    <strong>Expression</strong>*
    <ul class="expression">
      <li><span class="up"></span> Upregulation</li>
      <li><span class="down"></span> Downregulation</li>
    </ul>
    <p class="small">* Color intensity denotes the reliability of the regulation if more that one probe set is present and their results differ.</p>
  </div>
</div>

<%-- for each category/type --%>
<c:forEach var="category" items="${expressions.map}">
  <div class="chart" id="gene-expression-atlas-chart-${category.key}"></div>

  <script type="text/javascript">
    (function() {
      <%-- Java to JavaScript --%>
      var liszt = new Array();

      <c:forEach var="cellType" items="${category.value}">
        var expressions = new Array();
        <c:forEach var="expression" items="${cellType.value}">
          var expression = {
            'pValue': ${expression.pValue},
            'tStatistic': ${expression.tStatistic}
          };
          expressions.push(expression);
        </c:forEach>

        var expression = {
          'condition': '${cellType.key}',
          'expressions': expressions
        };
        liszt.push(expression);
        im.log(expression.condition);
      </c:forEach>;

      google.load("visualization", "1", {packages:["corechart"]});
      google.setOnLoadCallback(drawChart);
      function drawChart() {
        var data = new google.visualization.DataTable();
        data.addColumn('string', 'Cell type');

        data.addColumn('number', 'High DOWN');
        data.addColumn('number', 'Low DOWN');
        data.addColumn('number', 'Low UP');
        data.addColumn('number', 'High UP');

        var n = 0;
        <%-- for each cell type --%>
        for (x in liszt) {
          var cellType = liszt[x];
          data.addRows(cellType.expressions.length);

          <%-- for each expression (one bar) in this cell type --%>
          for (y in cellType.expressions) {
            var expression = cellType.expressions[y];

            var tStatistic = expression.tStatistic;
            data.setValue(n, 0, cellType.condition);
            if (tStatistic > 0) { <%-- UP --%>
              data.setValue(n, 1, 0);
              data.setValue(n, 2, 0);
              data.setValue(n, 3, 0);
              data.setValue(n, 4, tStatistic);
            } else {  <%-- DOWN --%>
              data.setValue(n, 1, tStatistic);
              data.setValue(n, 2, 0);
              data.setValue(n, 3, 0);
              data.setValue(n, 4, 0);
            }

            n++;
          }
        }

        var options = {
          isStacked: true,
          width: 800,
          chartArea: {left: 400, top: 0, height: 9 * n},
          height: (9 * n) + 20,
          backgroundColor: ["0", "CCCCCC", "0.2", "FFFFFF", "0.2"],
          colors: ['#0000FF', '#D2D2F8', '#C2EAB8', '#59BB14'],
          fontName: "Lucida Grande,Verdana,Geneva,Lucida,Helvetica,Arial,sans-serif",
          fontSize: 11,
          vAxis: {title: 'Condition', titleTextStyle: {color: '#1F7492'}},
          hAxis: {title: 'Expression Value', titleTextStyle: {color: '#1F7492'}},
          legend: 'none'
        };

        var chart = new google.visualization.BarChart(document.getElementById("gene-expression-atlas-chart-${category.key}"));
        chart.draw(data, options);
      }
    })();
  </script>
</c:forEach>

<div style="clear:both;"></div>
</div>