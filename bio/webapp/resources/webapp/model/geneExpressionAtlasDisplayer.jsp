<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<style>
  #gene-expression-atlas div.chart { float:left; }
  #gene-expression-atlas h3 { background-image:url("images/icons/ebi.gif"); background-position:6px 2px; background-repeat:no-repeat;
    line-height:20px; padding-left:28px; }
  #gene-expression-atlas div.wrap { overflow-x:auto; }
  #gene-expression-atlas div.inside { min-width:1000px; }
  #gene-expression-atlas div.sidebar { display:inline; float:left; margin-left:10px; }
  #gene-expression-atlas div.sidebar h4,
  #gene-expression-atlas div.sidebar p.small { font-size:11px; margin:5px 0 16px 0; width:430px; }
  #gene-expression-atlas div.sidebar h4 { margin:0; }
  #gene-expression-atlas div.sidebar div.description { width:100%; }
  #gene-expression-atlas div.sidebar div.description a.more { display:none; }
  #gene-expression-atlas div.sidebar div.description div.content { position:relative; }
  #gene-expression-atlas div.sidebar div.description.preview div.wrap { text-align:center; }
  #gene-expression-atlas div.sidebar div.description.preview a.more { display:inline-block; padding:1px 2px 1px 14px; border-radius:2px 2px 2px 2px;
  	box-shadow:0 1px 2px #EFEFEF; background:url("images/report/arrow_expand.gif") no-repeat scroll 2px 50% transparent; color:#1F7492;
  	cursor:pointer; margin:2px 0; }
  #gene-expression-atlas div.sidebar div.description.preview div.content { overflow:hidden; height:50px; cursor:pointer; }
  #gene-expression-atlas div.sidebar div.description.preview div.content div.overlay { display:block; width:430px; height:20px;
  	background:url('model/images/white-to-transparent-gradient-20px.png') repeat-x top left; position:absolute; top:30px; left:0; }
  #gene-expression-atlas div.sidebar div.description.preview { display:block; }
  #gene-expression-atlas div.sidebar div.legend ul { margin-top:4px; }
  #gene-expression-atlas div.sidebar div.legend span { border:1px solid #000; display:inline-block; height:15px; width:20px; }
  #gene-expression-atlas div.sidebar div.legend span.up { background:#59BB14; }
  #gene-expression-atlas div.sidebar div.legend span.down { background:#0000FF; }
  #gene-expression-atlas div.sidebar input.update { font-weight:bold; }
  #gene-expression-atlas div.sidebar input.update.inactive { font-weight:normal; }
  #gene-expression-atlas div.sidebar div.settings { margin-bottom:20px; }
  #gene-expression-atlas div.settings ul.sort { margin-bottom:10px; }
  #gene-expression-atlas div.settings ul.sort li { margin-left:10px !important; background:url('images/icons/sort-up.gif') no-repeat center left;
    padding-left:16px; cursor:pointer; }
  #gene-expression-atlas div.settings ul.sort li.active { background:url('images/icons/sort.gif') no-repeat center left; font-weight:bold; }
  #gene-expression-atlas fieldset { border:0; width:300px; }
  #gene-expression-atlas fieldset input[type="checkbox"] { margin-right:10px; vertical-align:bottom }
  #gene-expression-atlas div.collection-table { display:none; }
  #gene-expression-atlas input.toggle-table { margin-bottom:20px; }
  
  #gene-expression-atlas-chart span { text-align:center; display:block; margin-left:50%; color:#1F7492; font-size:11px;
  	font-style:italic; margin-bottom:20px; }
  #gene-expression-atlas-chart iframe { display:block; clear:both; }
</style>

<div id="gene-expression-atlas">

<c:choose>
<c:when test="${empty(expressions.byName)}">
<h3 class="goog gray">ArrayExpress Atlas Gene Expression</h3>
<p>No expression data available for this gene.</p>
</c:when>
<c:otherwise>
<h3 class="goog">ArrayExpress Atlas Gene Expression</h3>

<div class="wrap">
<div class="inside">
<div class="chart" id="gene-expression-atlas-chart"></div>

  <script type="text/javascript">
    <%-- stuff this goodie bag --%>
    var geneExpressionAtlasDisplayer = {};
    <%-- call me to tell me settings have updated --%>
    geneExpressionAtlasDisplayer.settingsUpdated = function() {
      jQuery("#gene-expression-atlas div.settings input.update").removeClass('inactive');
    };

     <%-- load Goog, create the initial bag from Java, determine max t-stat peak --%>
    (function() {
      google.load("visualization", "1", {packages:["corechart"]});

      <%-- Java to JavaScript --%>
      geneExpressionAtlasDisplayer.originalList =
          {"byName": new Array(), "byTStatistic": new Array(), "byPValue": new Array()};
      geneExpressionAtlasDisplayer.peaks = {"up": 0, "down": 0};

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
            if (tStatistic > geneExpressionAtlasDisplayer.peaks.up) {
                geneExpressionAtlasDisplayer.peaks.up = tStatistic;
            }
          } else {
            if (tStatistic < geneExpressionAtlasDisplayer.peaks.down) {
                geneExpressionAtlasDisplayer.peaks.down = tStatistic;
            }
          }
        </c:forEach>

        var expression = {
          'condition': '${cellType.key}',
          'expressions': expressions
        };
        geneExpressionAtlasDisplayer.originalList.byName.push(expression);
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
          'condition': '${cellType.key}',
          'expressions': expressions
        };
        geneExpressionAtlasDisplayer.originalList.byTStatistic.push(expression);
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
          'condition': '${cellType.key}',
          'expressions': expressions
        };
        geneExpressionAtlasDisplayer.originalList.byPValue.push(expression);
      </c:forEach>

      <%-- set global t-stat peak for slider --%>
      geneExpressionAtlasDisplayer.peaks.global =
      (geneExpressionAtlasDisplayer.peaks.up > Math.abs(geneExpressionAtlasDisplayer.peaks.down)) ?
      geneExpressionAtlasDisplayer.peaks.up : Math.abs(geneExpressionAtlasDisplayer.peaks.down);
    })();
  </script>

  <%-- sidebar --%>
  <div class="sidebar">
    <div class="legend">
      <strong>Expression</strong>
      <ul class="expression">
        <li><span class="up"></span> Upregulation</li>
        <li><span class="down"></span> Downregulation</li>
      </ul>
    </div>

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

      <strong>2) Adjust the p-value**</strong>
      <fieldset class="p-value">
        <tiles:insert name="geneExpressionAtlasDisplayerNonLinearSlider.jsp">
          <tiles:put name="sliderIdentifier" value="p-value" />
          <tiles:put name="defaultValue" value="${defaultPValue}" />
        </tiles:insert>
      </fieldset>

      <strong>3) Adjust the t-statistic*</strong>
      <fieldset class="t-statistic">
        <tiles:insert name="geneExpressionAtlasDisplayerLinearSlider.jsp">
          <tiles:put name="sliderIdentifier" value="t-statistic" />
          <tiles:put name="defaultValue" value="${defaultTValue}" />
        </tiles:insert>
      </fieldset>

      <strong>4)</strong>
      <input class="update inactive" type="button" value="Update" title="Update the chart"></input>
    </div>
    
    <input class="toggle-table" type="button" value="Toggle table">
    
    <div class="description preview">
	    <div class="content">
		    <div class="overlay"></div>
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
		</div>
		<div class="wrap"><a class="more">Read more</a></div>
	</div>
  </div>

  <script type="text/javascript">
  	<%-- toggler for the sidebar description --%>
  	jQuery('#gene-expression-atlas div.sidebar div.description.preview').click(function() {
  		jQuery(this).removeClass('preview');
  	});
  
    <%-- call me to draw me --%>
    function drawChart(liszt, redraw) {
      if (liszt.length > 0) {
        if (redraw) {
          googleChart();
        } else {
          google.setOnLoadCallback(googleChart);
        }
      } else {
        notify('Nothing to show, adjust the p-value and/or t-stat to see upto ' + geneExpressionAtlasDisplayer.originalList.byName.length + ' results', true);
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
            data.setValue(n, 0, cellType.condition);

            var formattedString = tStatistic + ' (t-statistic), ' + expression.pValue + ' (p-value)';

            if (tStatistic > 0) { <%-- UP --%>
              <%-- low confidence? --%>
              if (geneExpressionAtlasDisplayer.currentFilter.pValue < expression.pValue) {
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
              if (geneExpressionAtlasDisplayer.currentFilter.pValue < expression.pValue) {
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
          vAxis: 			{title: 'Condition', titleTextStyle: {color: '#1F7492'}},
          hAxis:			'none',
          legend: 			'none',
          hAxis:			{minValue: geneExpressionAtlasDisplayer.peaks.down - 2, maxValue: geneExpressionAtlasDisplayer.peaks.up + 2}
        };

        // TODO: switch off any loading messages

        var chart = new google.visualization.BarChart(document.getElementById("gene-expression-atlas-chart"));
        chart.draw(data, options);
        
        // attach the hAxis as it does not work natively
        jQuery('<span/>', {
        	text: 't-statistic'
        }).appendTo('#gene-expression-atlas-chart');
      }
    }

    <%-- filter expressions in the chart given a variety of filters --%>
    function filterAndDrawChart(redraw) {
      // TODO: chart loading msg

      var filters = geneExpressionAtlasDisplayer.currentFilter;

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
      var originalList = geneExpressionAtlasDisplayer.originalList[getSortOrder()];
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
              'condition': oldCellType.condition,
              'expressions': newExpressions
            };
            liszt.push(newCellType);
          }
        }
      } else {
        liszt = geneExpressionAtlasDisplayer.originalList[getSortOrder()];
      }

      geneExpressionAtlasDisplayer.newList = liszt;

      <%-- re-/draw the chart --%>
      drawChart(liszt, redraw);
    }

    function initFilter() {
      <%-- regulation type (UP/DOWN/NONE) --%>
      geneExpressionAtlasDisplayer.currentFilter.regulationType = new Array('UP', 'DOWN');

      <%-- p-value --%>
      geneExpressionAtlasDisplayer.currentFilter.pValue = ${defaultPValue};

      <%-- t-statistic --%>
      geneExpressionAtlasDisplayer.currentFilter.tStatistic = ${defaultTValue};
    }

    function updateCurrentFilter() {
      <%-- regulation type (UP/DOWN/NONE) --%>
      geneExpressionAtlasDisplayer.currentFilter.regulationType = new Array();
      jQuery("#gene-expression-atlas div.settings fieldset.regulation-type input:checked").each(function() {
        geneExpressionAtlasDisplayer.currentFilter.regulationType.push(jQuery(this).attr('title'));
      });

      <%-- p-value --%>
      geneExpressionAtlasDisplayer.currentFilter.pValue = jQuery("#gene-expression-atlas div.settings fieldset.p-value input.value").val();

      <%-- t-statistic --%>
      geneExpressionAtlasDisplayer.currentFilter.tStatistic = jQuery("#gene-expression-atlas div.settings fieldset.t-statistic input.value").val();

      jQuery("#gene-expression-atlas-chart-organism_part.chart").empty();
    }

    <%-- initial call on page load --%>
    (function() {
      <%-- create an initial filter --%>
      geneExpressionAtlasDisplayer.currentFilter = {};
      initFilter();

      <%-- lets rumble --%>
      filterAndDrawChart();
    })();

    <%-- get the browser window size --%>
    function windowSize() {
        return jQuery(window).width();
    }

    <%-- what is the current sort order --%>
    function getSortOrder() {
        if (!jQuery('#gene-expression-atlas div.settings ul.sort').exists()) {
            return 'byTStatistic'; <%-- settings do not exist yet --%>
        } else {
            return jQuery("#gene-expression-atlas div.settings ul.sort li.active").attr('title');
        }
    }

    <%-- show message in place of the chart --%>
    function notify(message, clear) {
      if (clear) jQuery('#gene-expression-atlas-chart').empty();
      jQuery('<p/>', {
        text: message,
        style: "border:1px solid #ED9D12; color:#ED9D12; background:#FEF9F1; padding:4px;"
      }).appendTo('#gene-expression-atlas-chart');
    }

    <%-- resize chart on browser window resize --%>
    jQuery(window).resize(function() {
      if (this.resz) clearTimeout(this.resz);
      this.resz = setTimeout(function() {
        filterAndDrawChart(true);
      }, 500);
    });

    <%-- attache events to the sidebar settings, set as filters and redraw --%>
    jQuery("#gene-expression-atlas div.settings input.update").click(function() {
      updateCurrentFilter();
      <%-- redraw --%>
      filterAndDrawChart(true);
      <%-- update button not highlighted --%>
      jQuery(this).addClass('inactive');
    });

    <%-- attache switcher for sort order --%>
    jQuery("#gene-expression-atlas div.settings ul.sort li").click(function() {
      jQuery("#gene-expression-atlas div.settings ul.sort li.active").removeClass('active');
      jQuery(this).addClass('active');
      updateCurrentFilter();
      filterAndDrawChart(true);
    });

    <%-- attache monitoring for regulation type checkbox change --%>
    jQuery("#gene-expression-atlas div.settings fieldset.regulation-type input").click(function() {
      if (typeof geneExpressionAtlasDisplayer == 'object') {
        geneExpressionAtlasDisplayer.settingsUpdated();
      }
    });
  </script>

<div style="clear:both;"></div>
</div>
</div>

<%-- collection table --%>
<div class="collection-table">
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
	  <html:link action="/collectionDetails?id=${object.id}&amp;field=atlasExpression&amp;trail=${param.trail}">
			Show all in a table &raquo;
	  </html:link>
	</div>
</div>

<script type="text/javascript">
(function() {
    <%-- hide more than 10 rows --%>
    var bodyRows = jQuery("#gene-expression-atlas div.collection-table table tbody tr");
    if (bodyRows.length > 10) {
      bodyRows.each(function(i) {
        if (i > 9) {
          jQuery(this).hide();
        }
      });
      <%-- 'provide' toggler --%>
      jQuery("#gene-expression-atlas div.collection-table div.toggle").show();
      <%-- attach toggler event --%>
      jQuery('#gene-expression-atlas div.collection-table div.toggle a.more').click(function(e) {
        jQuery("#gene-expression-atlas div.collection-table table tbody tr:hidden").each(function(i) {
          if (i < 10) {
            jQuery(this).show();
          }
        });
        jQuery("#gene-expression-atlas div.collection-table div.toggle a.less").show();
        if (jQuery("#gene-expression-atlas div.collection-table table tbody tr:hidden").length == 0) {
          jQuery('#gene-expression-atlas div.collection-table div.toggle a.more').hide();
        }
      });
      <%-- attach collapser event --%>
      jQuery('#gene-expression-atlas div.collection-table div.toggle a.less').click(function(e) {
        var that = this;
        bodyRows.each(function(i) {
          if (i > 9) {
            jQuery(this).hide();
            jQuery(that).hide();
          }
        });
        jQuery('#gene-expression-atlas div.collection-table div.toggle a.more').show();
        jQuery('#gene-expression-atlas div.collection-table').hide();
        jQuery('input.toggle-table').show();
      });
    }
    
    jQuery('input.toggle-table').click(function() {
    	jQuery('#gene-expression-atlas div.collection-table').toggle();
    	if (jQuery('#gene-expression-atlas div.collection-table:visible')) {
    		jQuery("#gene-expression-atlas div.collection-table").scrollTo('fast', 'swing', -20);
        jQuery(this).hide();
    	}
    });
})();
</script>
</c:otherwise>
</c:choose>
</div>