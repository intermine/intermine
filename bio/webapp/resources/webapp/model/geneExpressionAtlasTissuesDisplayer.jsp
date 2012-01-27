<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<style>
  #gene-expression-atlas-tissues div.chart { float:left; min-width:500px; }
  #gene-expression-atlas-tissues div.chart div.loading { background:url('images/icons/ajax-loader.gif') no-repeat top left; padding-left:28px;
    margin:0 auto; width:200px; margin-top:50px; font-weight:bold; letter-spacing:0.5px; }
  #gene-expression-atlas-tissues h3 { background-image:url("images/icons/ebi.gif"); background-position:6px 2px; background-repeat:no-repeat;
    line-height:20px; padding-left:28px; }
  #gene-expression-atlas-tissues div.wrap { overflow-x:auto; }
  #gene-expression-atlas-tissues div.inside { min-width:1000px; }
  #gene-expression-atlas-tissues div.sidebar { display:inline; float:left; margin-left:10px; }
  #gene-expression-atlas-tissues div.sidebar h4,
  #gene-expression-atlas-tissues div.sidebar p { margin:5px 0 16px 0; width:430px; }
  #gene-expression-atlas-tissues div.sidebar p.small { font-size:11px; }
  #gene-expression-atlas-tissues div.sidebar a.ext { background:url('images/icons/external_link.png') no-repeat top right; padding-right:10px; }
  #gene-expression-atlas-tissues div.sidebar h4 { margin:0; }
  #gene-expression-atlas-tissues div.sidebar div.description { width:100%; }
  #gene-expression-atlas-tissues div.sidebar div.description a.more { display:none; }
  #gene-expression-atlas-tissues div.sidebar div.description div.content { position:relative; }
  #gene-expression-atlas-tissues div.sidebar div.description.preview div.wrap { text-align:center; }
  #gene-expression-atlas-tissues div.sidebar div.description.preview a.more { display:inline-block; padding:1px 2px 1px 14px; border-radius:2px 2px 2px 2px;
    box-shadow:0 1px 2px #EFEFEF; background:url("images/report/arrow_expand.gif") no-repeat scroll 2px 50% transparent; color:#1F7492;
    cursor:pointer; margin:2px 0; }
  #gene-expression-atlas-tissues div.sidebar div.description.preview div.content { overflow:hidden; height:50px; cursor:pointer; }
  #gene-expression-atlas-tissues div.sidebar div.description.preview div.content div.overlay { display:block; width:430px; height:20px;
    background:url('model/images/white-to-transparent-gradient-20px.png') repeat-x top left; position:absolute; top:30px; left:0; }
  #gene-expression-atlas-tissues div.sidebar div.description.preview { display:block; }
  #gene-expression-atlas-tissues div.sidebar div.legend ul { margin-top:4px; }
  #gene-expression-atlas-tissues div.sidebar div.legend span { border:1px solid #000; display:inline-block; height:15px; width:20px; }
  #gene-expression-atlas-tissues div.sidebar div.legend span.up { background:#59BB14; }
  #gene-expression-atlas-tissues div.sidebar div.legend span.down { background:#0000FF; }
  #gene-expression-atlas-tissues div.sidebar div.legend span.confidence { background:url('model/images/low-confidence.png') no-repeat center;
    margin-top:10px; }
  #gene-expression-atlas-tissues div.sidebar input.update { font-weight:bold; }
  #gene-expression-atlas-tissues div.sidebar input.update.inactive { font-weight:normal; }
  #gene-expression-atlas-tissues div.sidebar div.settings { margin-bottom:20px; }
  #gene-expression-atlas-tissues div.sidebar div.collection-of-collections { min-width:430px; }
  #gene-expression-atlas-tissues div.sidebar table { text-align:left; }
  #gene-expression-atlas-tissues div.sidebar div.pane { padding:5px; }
  #gene-expression-atlas-tissues div.settings ul.sort { margin-bottom:10px; }
  #gene-expression-atlas-tissues div.settings ul.sort li { margin-left:10px !important; background:url('images/icons/sort-up.gif') no-repeat center left;
    padding-left:16px; cursor:pointer; }
  #gene-expression-atlas-tissues div.settings ul.sort li.active { background:url('images/icons/sort.gif') no-repeat center left; font-weight:bold; }
  #gene-expression-atlas-tissues fieldset { border:0; width:300px; }
  #gene-expression-atlas-tissues fieldset input[type="checkbox"] { margin-right:10px; vertical-align:bottom }
  #gene-expression-atlas-tissues div.data-table { display:none; margin-top:20px; }
  #gene-expression-atlas-tissues input.toggle-table { margin-bottom:20px; }

  #gene-expression-atlas-tissues-chart span { text-align:center; display:block; margin-left:55%; color:#1F7492; font-size:11px;
    font-style:italic; margin-bottom:20px; }
  #gene-expression-atlas-tissues-chart iframe { display:block; clear:both; }
</style>

<div id="gene-expression-atlas-tissues">

<c:choose>
<c:when test="${empty(expressions.byName)}">
<h3 class="goog gray">Tissue Expression (ArrayExpress)</h3>
<p>No expression data available for this gene.</p>
</c:when>
<c:otherwise>
<h3 class="goog">Tissue Expression (ArrayExpress)</h3>

<div class="wrap">
<div class="inside">
<div class="chart" id="gene-expression-atlas-tissues-chart">
  <div class="loading">Loading the chart...</div>
</div>

  <script type="text/javascript">
    <%-- stuff this goodie bag --%>
    var geneExpressionAtlasTissuesDisplayer = {};
    <%-- call me to tell me settings have updated --%>
    geneExpressionAtlasTissuesDisplayer.settingsUpdated = function() {
      jQuery("#gene-expression-atlas-tissues div.settings input.update").removeClass('inactive');
    };

     <%-- load Goog, create the initial bag from Java, determine max t-stat peak --%>
     (function() {

      <%-- Java to JavaScript --%>
      geneExpressionAtlasTissuesDisplayer.originalList =
          {"byName": new Array(), "byTStatistic": new Array(), "byPValue": new Array()};
      geneExpressionAtlasTissuesDisplayer.peaks = {"up": 0, "down": 0};

      <%-- ordered by organ part --%>
      <c:forEach var="cellType" items="${expressions.byName}">
        var expressions = new Array();
        <c:set var="cell" value="${cellType.value}"/>
        <c:forEach var="expression" items="${cell.values}">
          var tStatistic = ${expression.tStatistic};
          var expression = {
            'pValue': ${expression.pValue},
            'tStatistic': tStatistic
          };
          expressions.push(expression);

          <%-- figure out min/max scale --%>
          if (tStatistic > 0) {
            if (tStatistic > geneExpressionAtlasTissuesDisplayer.peaks.up) {
                geneExpressionAtlasTissuesDisplayer.peaks.up = tStatistic;
            }
          } else {
            if (tStatistic < geneExpressionAtlasTissuesDisplayer.peaks.down) {
                geneExpressionAtlasTissuesDisplayer.peaks.down = tStatistic;
            }
          }
        </c:forEach>

        var expression = {
          'tissue': '${cellType.key}',
          'expressions': expressions
        };
        geneExpressionAtlasTissuesDisplayer.originalList.byName.push(expression);
      </c:forEach>

      <%-- ordered by t-statistic --%>
      <c:forEach var="cellType" items="${expressions.byTStatistic}">
        var expressions = new Array();
        <c:set var="cell" value="${cellType.value}"/>
        <c:forEach var="expression" items="${cell.values}">
          var expression = {
            'pValue': ${expression.pValue},
            'tStatistic': ${expression.tStatistic}
          };
          expressions.push(expression);
        </c:forEach>

        var expression = {
          'tissue': '${cellType.key}',
          'expressions': expressions
        };
        geneExpressionAtlasTissuesDisplayer.originalList.byTStatistic.push(expression);
      </c:forEach>

      <%-- ordered by p-value --%>
      <c:forEach var="cellType" items="${expressions.byPValue}">
        var expressions = new Array();
        <c:set var="cell" value="${cellType.value}"/>
        <c:forEach var="expression" items="${cell.values}">
          var expression = {
            'pValue': ${expression.pValue},
            'tStatistic': ${expression.tStatistic}
          };
          expressions.push(expression);
        </c:forEach>

        var expression = {
          'tissue': '${cellType.key}',
          'expressions': expressions
        };
        geneExpressionAtlasTissuesDisplayer.originalList.byPValue.push(expression);
      </c:forEach>

      <%-- set global t-stat peak for slider --%>
      geneExpressionAtlasTissuesDisplayer.peaks.global =
      (geneExpressionAtlasTissuesDisplayer.peaks.up > Math.abs(geneExpressionAtlasTissuesDisplayer.peaks.down)) ?
      geneExpressionAtlasTissuesDisplayer.peaks.up : Math.abs(geneExpressionAtlasTissuesDisplayer.peaks.down);
     })();
  </script>

  <%-- sidebar --%>
  <div class="sidebar">
  <div class="collection-of-collections">
    <div class="header">
      <div class="switchers">
        <a href="#" title="key" class="active">Key</a> <a href="#" title="controls">Controls</a> <a href="#" title="halp">Help</a>
      </div>
    </div>
    <div class="pane key">
    <div class="legend">
       <strong>Expression</strong>
       <ul class="expression">
         <li><span class="up"></span> Upregulation</li>
         <li><span class="down"></span> Downregulation</li>
         <li><span class="confidence"></span> Low confidence</li>
       </ul>
     </div>
    </div>
    <div class="pane controls" style="display:none;">
      <div class="settings">
        <strong>Sort</strong>
        <ul class="sort">
          <li title="byName">By tissue name</li>
          <li class="active" title="byTStatistic">By t-statistic</li>
          <li title="byPValue">By p-value</li>
        </ul>

        <strong>1) Show regulation type</strong>
        <fieldset class="regulation-type">
          <label for="upregulation-check">Upregulation:</label>
          <input type="checkbox" id="upregulation-check" title="UP" checked="checked" autocomplete="off" />
          <label for="downregulation-check">Downregulation:</label>
          <input type="checkbox" id="downregulation-check" title="DOWN" checked="checked" autocomplete="off" />
        </fieldset>

      <script type="text/javascript">
        geneExpressionAtlasTissuesDisplayer.dragdealers = {};
      </script>
        <strong>2) Adjust the p-value**</strong>
        <fieldset class="p-value">
          <tiles:insert name="geneExpressionAtlasTissuesDisplayerNonLinearSlider.jsp">
            <tiles:put name="sliderIdentifier" value="tissuesPValue" />
            <tiles:put name="defaultValue" value="${defaultPValue}" />
          </tiles:insert>
        </fieldset>

        <strong>3) Adjust the t-statistic*</strong>
        <fieldset class="t-statistic">
          <tiles:insert name="geneExpressionAtlasTissuesDisplayerLinearSlider.jsp">
            <tiles:put name="sliderIdentifier" value="tissuesTStatistic" />
            <tiles:put name="defaultValue" value="${defaultTValue}" />
          </tiles:insert>
        </fieldset>

        <strong>4)</strong>
        <input class="update inactive" type="button" value="Update" title="Update the chart"></input>
      </div>

      <input class="toggle-table" type="button" value="Toggle table">
    </div>
    <div class="pane halp" style="display:none;">
      <div class="description">
        <%--<div class="content">--%>
          <%--<div class="overlay"></div>--%>
          <h4>* Moderated t-statistic</h4>
          <p class="small">The basic statistic used for significance analysis is the moderated
        t-statistic, which is computed for each probe and for each contrast.
        This has the same interpretation as an ordinary t-statistic except
        that the standard errors have been moderated across genes, i.e.,
        shrunk towards a common value, using a simple Bayesian model. This has
        the effect of borrowing information from the ensemble of genes to aid
        with inference about each individual gene.<br />
        The moderated t-statistic (t) is the ratio of the log fold change to
        its standard error.</p>

        <h4>** p-value</h4>
        <p class="small">The p-value (p-value) is obtained from the moderated t-statistic,
        usually after adjustment for multiple testing: "fdr" which is
        Benjamini and Hochberg's method to control the false discovery rate.</p>

        <h3>About &amp; Source</h3>
        <p>This chart is taken from <a class="ext" target="new" href="http://www.ebi.ac.uk/gxa/experiment/E-MTAB-62">EMTAB-62 experiment</a>.</p>
      <%--</div>--%>
      <%--<div class="wrap"><a class="more">Read more</a></div>--%>
    </div>
    </div>
  </div>
  </div>

  <script type="text/javascript">
    <%-- call me to draw me --%>
    function geneExpressionTissuesDrawChart(liszt, redraw) {
      if (liszt.length > 0) {
        googleChart();
      } else {
        geneExpressionTissuesNotify('Nothing to show, adjust the p-value and/or t-stat to see upto ' + geneExpressionAtlasTissuesDisplayer.originalList.byName.length + ' results', true);
      }

      <%-- the Goog draws here --%>
      function googleChart() {
        var data = new google.visualization.DataTable();
        data.addColumn('string', 'Cell type');

        data.addColumn('number', 'Downregulation, high p-value');
        data.addColumn('number', 'Downregulation');
        data.addColumn('number', 'Upregulation');
        data.addColumn('number', 'Upregulation, high p-value');

        var chartDirections = {'up': false, 'down':false};

        var n = 0;
        <%-- for each cell type --%>
        for (x in liszt) {
          var cellType = liszt[x];
          data.addRows(cellType.expressions.length);

          <%-- for each expression (one bar) in this cell type --%>
          for (y in cellType.expressions) {
            var expression = cellType.expressions[y];
            var tStatistic = expression.tStatistic;
            data.setValue(n, 0, cellType.tissue);

            var formattedString = '\n' + tStatistic + ' (t-statistic)\n' + expression.pValue + ' (p-value)';

            if (tStatistic > 0) { <%-- UP --%>
              <%-- low confidence? --%>
              if (geneExpressionAtlasTissuesDisplayer.currentFilter.pValue < expression.pValue) {
                data.setValue(n, 1, 0);
                data.setValue(n, 2, 0);
                data.setValue(n, 3, 0);
                data.setValue(n, 4, tStatistic);

                data.setFormattedValue(n, 4, formattedString);
              } else {
                data.setValue(n, 1, 0);
                data.setValue(n, 2, 0);
                data.setValue(n, 3, tStatistic);
                data.setValue(n, 4, 0);

                data.setFormattedValue(n, 3, formattedString);
              }

              chartDirections.up = true;
            } else {  <%-- DOWN --%>
              <%-- low confidence? --%>
              if (geneExpressionAtlasTissuesDisplayer.currentFilter.pValue < expression.pValue) {
                  data.setValue(n, 1, tStatistic);
                  data.setValue(n, 2, 0);
                  data.setValue(n, 3, 0);
                  data.setValue(n, 4, 0);

                  data.setFormattedValue(n, 1, formattedString);
              } else {
                data.setValue(n, 1, 0);
                  data.setValue(n, 2, tStatistic);
                  data.setValue(n, 3, 0);
                  data.setValue(n, 4, 0);

                  data.setFormattedValue(n, 2, formattedString);
              }

              chartDirections.down = true;
            }

            n++;
          }
        }

        <%-- modify the chart properties --%>
        var options = {
          isStacked:		true,
          width:			windowSize()/2.2,
          height:			(9 * n) + 50,
          chartArea:		{left: windowSize()/4, top: 0, height: 9 * n},
          backgroundColor: 	["0", "CCCCCC", "0.2", "FFFFFF", "0.2"],
          colors: 			['#C9C9FF', '#0000FF', '#59BB14', '#B5E196'],
          fontName: 		"Lucida Grande,Verdana,Geneva,Lucida,Helvetica,Arial,sans-serif",
          fontSize: 		11,
          vAxis: 			{title: 'Tissue', titleTextStyle: {color: '#1F7492'}},
          hAxis:			'none',
          legend: 			'none',
          hAxis:			{minValue: geneExpressionAtlasTissuesDisplayer.peaks.down - 2, maxValue: geneExpressionAtlasTissuesDisplayer.peaks.up + 2}
        };

        // TODO: switch off any loading messages

        var chart = new google.visualization.BarChart(document.getElementById("gene-expression-atlas-tissues-chart"));
        chart.draw(data, options);

        // attach the hAxis as it does not work natively
        jQuery('<span/>', {
          text: 'expression (t-statistic)'
        }).appendTo('#gene-expression-atlas-tissues-chart');
      }
    }

    <%-- filter expressions in the chart given a variety of filters --%>
    function geneExpressionTissuesFilterAndDrawChart(redraw) {
      // TODO: chart loading msg

      var filters = geneExpressionAtlasTissuesDisplayer.currentFilter;

      <%-- should the expression be included? --%>
      function iCanIncludeExpression(expression, filters) {
        <%-- regulation type (UP/DOWN/NONE) --%>
        if ("regulationType" in filters) {
          if (expression.tStatistic > 0) {
            if (filters.regulationType.indexOf("UP") < 0) {
              return false;
            }
          } else {
            if (expression.tStatistic < 0) {
              if (filters.regulationType.indexOf("DOWN") < 0) {
                return false;
              }
            } else {
              if (filters.regulationType.indexOf("NONE") < 0) return false;
            }
          }
        }

        <%-- t-statistic --%>
        if ("tStatistic" in filters) {
          if (Math.abs(expression.tStatistic) < filters.tStatistic) {
            return false;
          }
        }

        <%-- all fine --%>
        return true;
      }

      <%-- go through the original list here (based on sort order) --%>
      var originalList = geneExpressionAtlasTissuesDisplayer.originalList[geneExpressionTissuesGetSortOrder()];
      var liszt = new Array();
      if (filters) {
        for (x in originalList) {
          var oldCellType = originalList[x]
          var newExpressions = new Array();
          <%-- traverse the unfiltered expressions --%>
          for (y in oldCellType.expressions) {
            var expression = oldCellType.expressions[y];
            <%-- I can not haz dis 1? --%>
            if (iCanIncludeExpression(expression, filters)) {
              newExpressions.push(expression);
            }
          }

          if (newExpressions.length > 0) { <%-- one/some expression(s) met the bar --%>
            var newCellType = {
              'tissue': oldCellType.tissue,
              'expressions': newExpressions
            };
            liszt.push(newCellType);
          }
        }
      } else {
        liszt = geneExpressionAtlasTissuesDisplayer.originalList[geneExpressionTissuesGetSortOrder()];
      }

      geneExpressionAtlasTissuesDisplayer.newList = liszt;

      <%-- re-/draw the chart --%>
      geneExpressionTissuesDrawChart(liszt, redraw);
    }

    function geneExpressionTissuesInitFilter() {
      <%-- regulation type (UP/DOWN/NONE) --%>
      geneExpressionAtlasTissuesDisplayer.currentFilter.regulationType = new Array('UP', 'DOWN');

      <%-- p-value --%>
      geneExpressionAtlasTissuesDisplayer.currentFilter.pValue = ${defaultPValue};

      <%-- t-statistic --%>
      geneExpressionAtlasTissuesDisplayer.currentFilter.tStatistic = ${defaultTValue};
    }

    function geneExpressionTissuesUpdateCurrentFilter() {
      <%-- regulation type (UP/DOWN/NONE) --%>
      geneExpressionAtlasTissuesDisplayer.currentFilter.regulationType = new Array();
      jQuery("#gene-expression-atlas-tissues div.settings fieldset.regulation-type input:checked").each(function() {
        geneExpressionAtlasTissuesDisplayer.currentFilter.regulationType.push(jQuery(this).attr('title'));
      });

      <%-- p-value --%>
      geneExpressionAtlasTissuesDisplayer.currentFilter.pValue = jQuery("#gene-expression-atlas-tissues div.settings fieldset.p-value input.value").val();

      <%-- t-statistic --%>
      geneExpressionAtlasTissuesDisplayer.currentFilter.tStatistic = jQuery("#gene-expression-atlas-tissues div.settings fieldset.t-statistic input.value").val();

      jQuery("#gene-expression-atlas-tissues-chart-organism_part.chart").empty();
    }

    <%-- initial call on page load --%>
    (function() {
      <%-- create an initial filter --%>
      geneExpressionAtlasTissuesDisplayer.currentFilter = {};
      geneExpressionTissuesInitFilter();

      <%-- lets rumble --%>
      google.load("visualization", "1", {"packages":["corechart"], "callback":geneExpressionTissuesFilterAndDrawChart});
    })();

    <%-- get the browser window size --%>
    function windowSize() {
        return jQuery(window).width();
    }

    <%-- what is the current sort order --%>
    function geneExpressionTissuesGetSortOrder() {
        if (!jQuery('#gene-expression-atlas-tissues div.settings ul.sort').exists()) {
            return 'byTStatistic'; <%-- settings do not exist yet --%>
        } else {
            return jQuery("#gene-expression-atlas-tissues div.settings ul.sort li.active").attr('title');
        }
    }

    <%-- show message in place of the chart --%>
    function geneExpressionTissuesNotify(message, clear) {
      if (clear) jQuery('#gene-expression-atlas-tissues-chart').empty();
      jQuery('<p/>', {
        text: message,
        style: "border:1px solid #ED9D12; color:#ED9D12; background:#FEF9F1; padding:4px;"
      }).appendTo('#gene-expression-atlas-tissues-chart');
    }

    <%-- resize chart on browser window resize --%>
    jQuery(window).resize(function() {
      if (this.resz) clearTimeout(this.resz);
      this.resz = setTimeout(function() {
        geneExpressionTissuesFilterAndDrawChart(true);
      }, 500);
    });

    <%-- attache events to the sidebar settings, set as filters and redraw --%>
    jQuery("#gene-expression-atlas-tissues div.settings input.update").click(function() {
      geneExpressionTissuesUpdateCurrentFilter();
      <%-- redraw --%>
      geneExpressionTissuesFilterAndDrawChart(true);
      <%-- update button not highlighted --%>
      jQuery(this).addClass('inactive');
    });

    <%-- attache switcher for sort order --%>
    jQuery("#gene-expression-atlas-tissues div.settings ul.sort li").click(function() {
      jQuery("#gene-expression-atlas-tissues div.settings ul.sort li.active").removeClass('active');
      jQuery(this).addClass('active');
      geneExpressionTissuesUpdateCurrentFilter();
      geneExpressionTissuesFilterAndDrawChart(true);
    });

    <%-- attache monitoring for regulation type checkbox change --%>
    jQuery("#gene-expression-atlas-tissues div.settings fieldset.regulation-type input").click(function() {
      if (typeof geneExpressionAtlasTissuesDisplayer == 'object') {
        geneExpressionAtlasTissuesDisplayer.settingsUpdated();
      }
    });

    geneExpressionAtlasTissuesDisplayer.dragdealers.initialized = false;
    // switcher between tables this displayer haz
    jQuery("#gene-expression-atlas-tissues div.sidebar div.collection-of-collections div.switchers a").each(function(i) {
      jQuery(this).bind(
        "click",
        function(e) {
            // hide anyone (!) that is shown
            jQuery("#gene-expression-atlas-tissues div.sidebar div.collection-of-collections div.pane:visible").each(function(j) {
              jQuery(this).hide();
            });

            // show the one we want
            jQuery("#gene-expression-atlas-tissues div.sidebar div.collection-of-collections div." + jQuery(this).attr('title') + ".pane").show();

            // switchers all off
            jQuery("#gene-expression-atlas-tissues div.sidebar div.collection-of-collections div.switchers a.active").each(function(j) {
              jQuery(this).toggleClass('active');
            });

           // init Dragdealers?
            if (jQuery(this).attr('title') == 'controls' && !geneExpressionAtlasTissuesDisplayer.dragdealers.initialized) {
              geneExpressionAtlasTissuesDisplayer.dragdealers.tissuesPValue.init();
              geneExpressionAtlasTissuesDisplayer.dragdealers.tissuesTStatistic.init();
              geneExpressionAtlasTissuesDisplayer.dragdealers.initialized = true;
            }

            // we are active
            jQuery(this).toggleClass('active');

            // no linking on my turf
            e.preventDefault();
        }
      );
    });
  </script>

<div style="clear:both;"></div>
</div>
</div>

<%-- collection table --%>
<div class="data-table collection-table">
  <h3>Table</h3>
  <c:set var="inlineResultsTable" value="${collection}" />
  <tiles:insert page="/reportCollectionTable.jsp">
  <tiles:put name="inlineResultsTable" beanName="inlineResultsTable" />
  <tiles:put name="object" beanName="reportObject.object" />
  <tiles:put name="fieldName" value="atlasExpression" />
  </tiles:insert>
  <div class="toggle">
      <a class="less" style="float:right; display:none; margin-left:20px;"><span>Collapse</span></a>
      <a class="more" style="float:right;"><span>Show more rows</span></a>
  </div>
  <div class="show-in-table">
    <html:link action="/collectionDetails?id=${reportObject.object.id}&amp;field=atlasExpression&amp;trail=${param.trail}">
      Show all in a table &raquo;
    </html:link>
  </div>
</div>

<script type="text/javascript">
(function() {
    <%-- hide more than 10 rows --%>
    var bodyRows = jQuery("#gene-expression-atlas-tissues div.collection-table table tbody tr");
    if (bodyRows.length > 10) {
      bodyRows.each(function(i) {
        if (i > 9) {
          jQuery(this).hide();
        }
      });
      <%-- 'provide' toggler --%>
      jQuery("#gene-expression-atlas-tissues div.collection-table div.toggle").show();
      <%-- attach toggler event --%>
      jQuery('#gene-expression-atlas-tissues div.collection-table div.toggle a.more').click(function(e) {
        jQuery("#gene-expression-atlas-tissues div.collection-table table tbody tr:hidden").each(function(i) {
          if (i < 10) {
            jQuery(this).show();
          }
        });
        jQuery("#gene-expression-atlas-tissues div.collection-table div.toggle a.less").show();
        if (jQuery("#gene-expression-atlas-tissues div.collection-table table tbody tr:hidden").length == 0) {
          jQuery('#gene-expression-atlas-tissues div.collection-table div.toggle a.more').hide();
        }
      });
      <%-- attach collapser event --%>
      jQuery('#gene-expression-atlas-tissues div.collection-table div.toggle a.less').click(function(e) {
        var that = this;
        bodyRows.each(function(i) {
          if (i > 9) {
            jQuery(this).hide();
            jQuery(that).hide();
          }
        });
        jQuery('#gene-expression-atlas-tissues div.collection-table div.toggle a.more').show();
        jQuery('#gene-expression-atlas-tissues div.collection-table').hide();
        jQuery('input.toggle-table').show();
      });
    }

    jQuery('#gene-expression-atlas-tissues input.toggle-table').click(function() {
      jQuery('#gene-expression-atlas-tissues div.collection-table').toggle();
      if (jQuery('#gene-expression-atlas-tissues div.collection-table:visible')) {
        jQuery("#gene-expression-atlas-tissues div.collection-table").scrollTo('fast', 'swing', -20);
        jQuery(this).hide();
      }
    });
})();
</script>
</c:otherwise>
</c:choose>
</div>