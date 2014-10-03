<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

  <style>
    #protein-atlas-displayer h3 { background-image:url("images/icons/protein-atlas.gif"); background-repeat:no-repeat; background-position:6px 2px;
      padding-left:28px; line-height:20px; }

    #protein-atlas-displayer table { float:left; border-spacing:0; border-collapse:collapse; }
    #protein-atlas-displayer table td, #protein-atlas-displayer table th { padding:2px 10px 2px 4px; }
    #protein-atlas-displayer table th { font-size:11px; }
    #protein-atlas-displayer table th.sortable { padding-left:8px; cursor:pointer; }
    #protein-atlas-displayer div.table table th.sortable { background:url("images/icons/sort-up.gif") no-repeat left; }
    #protein-atlas-displayer div.table.byLevel table th.sortable[title="byLevel"],
    #protein-atlas-displayer div.table.byOrgan table th.sortable[title="byOrgan"],
    #protein-atlas-displayer div.table.byCells table th.sortable[title="byCells"] { background:url("images/icons/sort.gif") no-repeat left; }
    #protein-atlas-displayer tr.alt td { background:#F6FCFE; border-top:1px solid #E6F7FE; border-bottom:1px solid #E6F7FE; }

    #protein-atlas-displayer table div.expressions { border:1px solid #000; border-right:0; display:inline-block; }
    #protein-atlas-displayer table div.expression { display:inline-block; width:20px; height:15px; cursor:pointer; float:left; border-right:1px solid #000; }
    #protein-atlas-displayer table div.expression span.tissue { display:none; }
    #protein-atlas-displayer table div.expression span.tooltip { z-index:5; background:#FFF; border:1px solid #CCC; whitespace:no-wrap; display:inline-block;
      position:absolute; font-size:11px; padding:1px 2px; -moz-box-shadow:1px 1px 2px #DDD; -webkit-box-shadow:1px 1px 2px #DDD; box-shadow:1px 1px 2px #DDD;
      line-height:15px; color:#000; }

    #protein-atlas-displayer.staining .high, #protein-atlas-displayer.staining .strong { background:#CD3A32; }
    #protein-atlas-displayer.ape .high, #protein-atlas-displayer.ape .strong { background:#2B4A7B; }

    #protein-atlas-displayer.staining .medium, #protein-atlas-displayer.staining .moderate { background:#FD9B34; }
    #protein-atlas-displayer.ape .medium, #protein-atlas-displayer.ape .moderate { background:#4B97CE; }

    #protein-atlas-displayer.staining .weak, #protein-atlas-displayer.staining .low { background:#FFED8D; }
    #protein-atlas-displayer.ape .weak, #protein-atlas-displayer.ape .low { background:#BFF1FF; }

    #protein-atlas-displayer .none, #protein-atlas-displayer .negative { background:#FFF; }

    #protein-atlas-displayer table span.level span.value { display:none; }

    #protein-atlas-displayer div.sidebar { float:right; width:40%; }
    #protein-atlas-displayer div.sidebar p strong.uncertain,
    #protein-atlas-displayer div.sidebar p strong.low { background:#FFD92C; border:1px solid #DDD; }
    #protein-atlas-displayer div.sidebar p strong.supportive,
    #protein-atlas-displayer div.sidebar p strong.high,
    #protein-atlas-displayer div.sidebar p strong.medium { background:#A9CC30; border:1px solid #DDD; }
    #protein-atlas-displayer div.sidebar p.small { font-size:11px; margin-bottom:16px; }
    #protein-atlas-displayer div.sidebar p.small a { background:url('images/icons/external_link.png') no-repeat top right; padding-right:10px; }
    
    #protein-atlas-displayer div.sidebar div.pane { padding:5px; }

    #protein-atlas-displayer div.legend { margin-top:10px; }
    #protein-atlas-displayer div.legend ul { margin:4px 0 0 0; }
    #protein-atlas-displayer div.legend ul li span, #protein-atlas-displayer table span.level { display:inline-block; width:20px; height:15px;
      border:1px solid #000; }

    #protein-atlas-displayer div.graph ul.header li { display:inline; font-size:11px; }

    #protein-atlas-displayer div.inactive { display:none; }

    /* resizing */
    #protein-atlas-displayer.scale-9 table.graph td,
    #protein-atlas-displayer.scale-8 table.graph td,
    #protein-atlas-displayer.scale-7 table.graph td,
    #protein-atlas-displayer.scale-6 table.graph td { padding:0 !important; }

    #protein-atlas-displayer.scale-9 table.graph td span.name,
    #protein-atlas-displayer.scale-8 table.graph td span.name,
    #protein-atlas-displayer.scale-7 table.graph td span.name,
    #protein-atlas-displayer.scale-6 table.graph td span.name { font-size:11px; }

    #protein-atlas-displayer.scale-8 table.graph div.expression,
    #protein-atlas-displayer.scale-7 table.graph div.expression,
    #protein-atlas-displayer.scale-6 table.graph div.expression { width:11px; }

    #protein-atlas-displayer.scale-7 div.sidebar { width:30%; }

    #protein-atlas-displayer.scale-6 div.sidebar { width:20%; }
  </style>

<c:set var="expressionType" value="${expressions.type}" />
<div id="protein-atlas-displayer" class="${expressionType.clazz}">

<c:choose>
<c:when test="${expressions.reliability != null}">
<h3 class="goog">Protein Atlas Tissue Expression</h3>

  <div class="sidebar">
  	<div class="collection-of-collections">
  	  <div class="header">
	    <div class="switchers">
	      <a href="#" title="key" class="active">Key</a> <a href="#" title="halp">Help</a>
	    </div>
	  </div>
	  
	  <div class="pane key">
		<p>Reliability: <strong class="${fn:toLowerCase(expressions.reliability)}">${expressions.reliability}</strong> (${expressionType.text})</p>
		
	    <div class="legend">
	      <strong>Level of antibody staining</strong>*
	      <ul class="level">
	        <li><span class="high"></span> High</li>
	        <li><span class="medium"></span> Medium</li>
	        <li><span class="low"></span> Low</li>
	        <li><span class="none"></span> None</li>
	      </ul>
	    </div>
	  </div>
	  
	  <div class="pane halp" style="display:none;">
	      <p class="small">* A validation score for immunohistochemistry is assigned for all antibodies and reflects the results of immunostaining.</p>
	
	      <strong>About &amp; Source</strong>
	      <p class="small">This chart represents a normal tissue &amp; organ summary of the antibody staining or the protein expression in a number of
	      human tissues and organs. A description of the assay and annotation can be found <a target="new" href="http://www.proteinatlas.org/about/assays+annotation#ih">here</a>.</p>
	      <a target="new" href="http://www.proteinatlas.org/"><img src="model/images/protein-atlas.gif" alt="Human Protein Atlas" /></a>
	  </div>
    </div>
  </div>

  <div class="table byOrgan active">
    <c:set var="tableRows" value="${expressions.byOrgan}" />
    <tiles:insert page="proteinAtlasDisplayerTable.jsp">
      <tiles:put name="rows" beanName="tableRows" />
    </tiles:insert>
  </div>

  <div class="table byCells inactive ${expressionType.clazz}">
    <c:set var="tableRows" value="${expressions.byCells}" />
    <tiles:insert page="proteinAtlasDisplayerTable.jsp">
      <tiles:put name="rows" beanName="tableRows" />
    </tiles:insert>
  </div>

  <div class="table byLevel inactive ${expressionType.clazz}">
    <c:set var="tableRows" value="${expressions.byLevel}" />
    <tiles:insert page="proteinAtlasDisplayerTable.jsp">
      <tiles:put name="rows" beanName="tableRows" />
    </tiles:insert>
  </div>

  <div style="clear:both;"></div>

  <script type="text/javascript">
  (function() {
    <%-- hovering on an organ tissue level will merge the contained tissue expressions of the same level and show a tooltip --%>
    jQuery("#protein-atlas-displayer div.expression").each(function() {
      var level = jQuery(this).attr('class').replace('expression', '').trim();
      var that = this;
      jQuery(this).hover(
            function() {
                jQuery('<span/>', {
                    'class': 'tooltip',
                    'html': function() {
                      var tooltip = new Array();
                      jQuery(that).parent().find("div.expression."+level).each(function() {
                          tooltip.push(jQuery(this).find('span.tissue').text());
                      });
                      return tooltip.join('<br/>');
                    },
                    'click': function() {
                      jQuery(that).parent().click();
                    }
                }).appendTo(that);
            },
            function() {
                jQuery(this).find('span.tooltip').remove();
            }
      );
    });

    <%-- table "sorting" --%>
    jQuery("#protein-atlas-displayer table th.sortable").click(function() {
      var order = jQuery(this).attr('title');
      jQuery("#protein-atlas-displayer div.table.active").removeClass('active').addClass('inactive');
      jQuery("#protein-atlas-displayer div.table."+order).removeClass('inactive').addClass('active');
    });

    <%-- determine the viewport size and 'resize' the chart --%>
    function sizeChart() {
      var width = jQuery(window).width();
      var ratio = Math.round(width / 160);
      if (ratio < 5) {
          ratio = 5;
      } else if (ratio > 10) {
          ratio = 10;
      }
      if (jQuery("#protein-atlas-displayer").hasClass('ape')) {
          jQuery("#protein-atlas-displayer").attr('class', 'ape scale-' + ratio);
      } else {
          jQuery("#protein-atlas-displayer").attr('class', 'staining scale-' + ratio);
      }
    };
    sizeChart();

    <%-- resize chart on browser window resize --%>
    jQuery(window).resize(function() {
      if (this.resz) clearTimeout(this.resz);
      this.resz = setTimeout(function() {
        sizeChart();
      }, 500);
    });
    
    // switcher between tables this displayer haz
    jQuery("#protein-atlas-displayer div.sidebar div.collection-of-collections div.switchers a").each(function(i) {
      jQuery(this).bind(
        "click",
        function(e) {
            // hide anyone (!) that is shown
            jQuery("#protein-atlas-displayer div.sidebar div.collection-of-collections div.pane:visible").each(function(j) {
              jQuery(this).hide();
            });

            // show the one we want
            jQuery("#protein-atlas-displayer div.sidebar div.collection-of-collections div." + jQuery(this).attr('title') + ".pane").show();

            // switchers all off
            jQuery("#protein-atlas-displayer div.sidebar div.collection-of-collections div.switchers a.active").each(function(j) {
              jQuery(this).toggleClass('active');
            });
            
            // we are active
            jQuery(this).toggleClass('active');

            // no linking on my turf
            e.preventDefault();
        }
      );
    });    
  })();
  </script>
</c:when>
<c:otherwise>
<h3 class="goog gray">Protein Atlas Tissue Expression</h3>
<p>No expression data available for this gene.</p>
</c:otherwise>
</c:choose>
</div>