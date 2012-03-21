<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<style>
  #flybase-expression div.data-table { margin-top:20px; }
</style>

<div id="flybase-expression">

<c:choose>
  <c:when test="${empty(flybaseResults)}">
    <h3 class="goog gray">Expression by Stage (modENCODE)</h3>
    <p>No expression data available for this gene.</p>
  </c:when>
  <c:otherwise>
    <h3 class="goog">Expression by Stage (modENCODE)</h3>
    <div class="chart" id="flybase-expression-chart"></div>

    <input class="toggle" type="button" value="Toggle table" />

    <%-- collection table --%>
    <div class="data-table collection-table" style="display:none;">
      <h3>Table</h3>
      <c:set var="inlineResultsTable" value="${flybaseCollection}" />
      <tiles:insert page="/reportCollectionTable.jsp">
        <tiles:put name="inlineResultsTable" beanName="inlineResultsTable" />
        <tiles:put name="object" beanName="reportObject.object" />
        <tiles:put name="fieldName" value="flybaseExpression" />
      </tiles:insert>
    </div>

    <script type="text/javascript">
      function drawChart() {

          var range = [
                       {"text": "No expression", "range": "0"},
                       {"text": "Extremely low expression", "range": "1 to 10"},
                       {"text": "Very low expression", "range": "11 to 100"},
                       {"text": "Low expression", "range": "101 to 400"},
                       {"text": "Moderate expression", "range": "401 to 1400"},
                       {"text": "Moderately high expression", "range": "1401 to 4000"},
                       {"text": "High expression", "range": "4001 to 10000"},
                       {"text": "Very high expression", "range": "10001 to 100000"},
                       {"text": "Extremely high expression", "range": "100001 and up"},
          ];

          <%-- init columns for each expression range --%>
          function initDataColumns(range, data) {
              var _i, _len;
              for (_i = 0, _len = range.length; _i < _len; _i++) {
                data.addColumn("number", range[_i]["text"]);
              }
              return data;
          }

          <%-- turns a string description of the amount of expression into chart form --%>
          function howExpressed(text, range, data, n) {
              var _i, _len;
              for (_i = 0, _len = range.length; _i < _len; _i++) {
                data.setValue(n, _i + 1, (text == range[_i]["text"]) ? _i : 0);
                data.setFormattedValue(n, _i + 1, range[_i]["range"]);
              }
              return data;
          }

          <%-- setup Google DataTable with the different columns --%>
          var data = new google.visualization.DataTable();
          data.addColumn('string', 'Stage');
          data = initDataColumns(range, data);

          <%-- take the Java result and make into a DataTable rows --%>
          var n = 0;
          <c:forEach var="stage" items="${flybaseResults}">
            data.addRows(1);
            data.setValue(n, 0, "${stage.key}");
            data = howExpressed("${stage.value}", range, data, n);
            n++;
          </c:forEach>

          <%-- modify the chart properties --%>
          var windowSize = jQuery(window).width();
          var options = {
            isStacked:		 true,
            width:			 windowSize/1.5,
            height:			 (10 * n) + 50,
            chartArea:		 {left: windowSize/4, top: 0, height: 10 * n},
            backgroundColor: ["0", "CCCCCC", "0.2", "FFFFFF", "0.2"],
            colors: 		 ["#f8f3fb", "#ecdef4", "#dec4ec", "#cea9e3", "#bc8bd9", "#ac6ed0", "#9e55c8", "#9240c1", '#8931BC'],
            fontName: 		 "Lucida Grande,Verdana,Geneva,Lucida,Helvetica,Arial,sans-serif",
            fontSize: 		 11,
            vAxis: 			 {title: 'Stage'},
            legend: 		 'none',
            hAxis:			 {title: "Expression", textStyle: {color: '#FFF'}}
          };

          <%-- aim & shoot --%>
          var chart = new google.visualization.BarChart(document.getElementById("flybase-expression-chart"));
          chart.draw(data, options);
      }

      google.load("visualization", "1", {"packages":["corechart"], "callback":drawChart});

      <%-- toggle table --%>
      jQuery("#flybase-expression input.toggle").click(function() {
        jQuery("#flybase-expression div.collection-table").toggle();
      });
    </script>

  </c:otherwise>
</c:choose>

</div>