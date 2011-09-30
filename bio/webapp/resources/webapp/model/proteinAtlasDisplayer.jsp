<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

  <style>
    #proteinAtlasDisplayer h3 { background-image:url("images/icons/protein-atlas.gif"); background-repeat:no-repeat; background-position:6px 2px;
      padding-left:28px; line-height:20px; }

    #proteinAtlasDisplayer table { float:left; border-spacing:0; border-collapse:collapse; }
    #proteinAtlasDisplayer table td, #proteinAtlasDisplayer table th { padding:2px 10px 2px 4px; }
    #proteinAtlasDisplayer table th { font-size:11px; }
    #proteinAtlasDisplayer table th.sortable { padding-left:8px; cursor:pointer; }
    #proteinAtlasDisplayer div.table table th.sortable { background:url("images/icons/sort-up.gif") no-repeat left; }
    #proteinAtlasDisplayer div.table.byLevel table th.sortable[title="byLevel"],
    #proteinAtlasDisplayer div.table.byOrgan table th.sortable[title="byOrgan"],
    #proteinAtlasDisplayer div.table.byCells table th.sortable[title="byCells"] { background:url("images/icons/sort.gif") no-repeat left; }
    #proteinAtlasDisplayer tr.alt td { background:#F6FCFE; border-top:1px solid #E6F7FE; border-bottom:1px solid #E6F7FE; }

    #proteinAtlasDisplayer table div.expressions { border:1px solid #000; border-right:0; display:inline-block; }
    #proteinAtlasDisplayer table div.expression { display:inline-block; width:20px; height:15px; cursor:pointer; float:left; border-right:1px solid #000; }
    #proteinAtlasDisplayer table div.expression span.tissue { display:none; }
    #proteinAtlasDisplayer table div.expression span.tooltip { z-index:5; background:#FFF; border:1px solid #CCC; whitespace:no-wrap; display:inline-block;
      position:absolute; font-size:11px; padding:1px 2px; -moz-box-shadow:1px 1px 2px #DDD; -webkit-box-shadow:1px 1px 2px #DDD; box-shadow:1px 1px 2px #DDD;
      line-height:15px; color:#000; }

    #proteinAtlasDisplayer.staining .high, #proteinAtlasDisplayer.staining .strong { background:#CD3A32; }
    #proteinAtlasDisplayer.ape .high, #proteinAtlasDisplayer.ape .strong { background:#2B4A7B; }

    #proteinAtlasDisplayer.staining .medium, #proteinAtlasDisplayer.staining .moderate { background:#FD9B34; }
    #proteinAtlasDisplayer.ape .medium, #proteinAtlasDisplayer.ape .moderate { background:#4B97CE; }

    #proteinAtlasDisplayer.staining .weak, #proteinAtlasDisplayer.staining .low { background:#FFED8D; }
    #proteinAtlasDisplayer.ape .weak, #proteinAtlasDisplayer.ape .low { background:#BFF1FF; }

    #proteinAtlasDisplayer .none, #proteinAtlasDisplayer .negative { background:#FFF; }

    #proteinAtlasDisplayer table span.level span.value { display:none; }

    #proteinAtlasDisplayer div.sidebar { float:right; width:40%; }
    #proteinAtlasDisplayer div.sidebar p strong.uncertain,
    #proteinAtlasDisplayer div.sidebar p strong.low { background:#FFD92C; border:1px solid #DDD; }
    #proteinAtlasDisplayer div.sidebar p strong.supportive,
    #proteinAtlasDisplayer div.sidebar p strong.high,
    #proteinAtlasDisplayer div.sidebar p strong.medium { background:#A9CC30; border:1px solid #DDD; }
    #proteinAtlasDisplayer div.sidebar p.small { font-size:11px; margin-bottom:16px; }
    #proteinAtlasDisplayer div.sidebar p.small a { background:url('images/icons/external_link.png') no-repeat top right; padding-right:10px; }

    #proteinAtlasDisplayer div.legend { margin-top:10px; }
    #proteinAtlasDisplayer div.legend ul { margin:4px 0 0 0; }
    #proteinAtlasDisplayer div.legend ul li span, #proteinAtlasDisplayer table span.level { display:inline-block; width:20px; height:15px;
      border:1px solid #000; }

    #proteinAtlasDisplayer div.graph ul.header li { display:inline; font-size:11px; }

    #proteinAtlasDisplayer div.inactive { display:none; }

    /* resizing */
    #proteinAtlasDisplayer.scale-9 table.graph td,
    #proteinAtlasDisplayer.scale-8 table.graph td,
    #proteinAtlasDisplayer.scale-7 table.graph td,
    #proteinAtlasDisplayer.scale-6 table.graph td { padding:0 !important; }

    #proteinAtlasDisplayer.scale-9 table.graph td span.name,
    #proteinAtlasDisplayer.scale-8 table.graph td span.name,
    #proteinAtlasDisplayer.scale-7 table.graph td span.name,
    #proteinAtlasDisplayer.scale-6 table.graph td span.name { font-size:11px; }

    #proteinAtlasDisplayer.scale-8 table.graph div.expression,
    #proteinAtlasDisplayer.scale-7 table.graph div.expression,
    #proteinAtlasDisplayer.scale-6 table.graph div.expression { width:11px; }

    #proteinAtlasDisplayer.scale-7 div.sidebar { width:30%; }

    #proteinAtlasDisplayer.scale-6 div.sidebar { width:20%; }
  </style>

<c:set var="expressionType" value="${expressions.type}" />
<div id="proteinAtlasDisplayer" class="${expressionType.clazz}">

<c:choose>
<c:when test="${expressions.reliability != null}">
<h3 class="goog">Protein Atlas Tissue Expression</h3>

  <div class="sidebar">
    <p>Reliability: <strong class="${fn:toLowerCase(expressions.reliability)}">${expressions.reliability}</strong> (${expressionType.text})</p>
    <div class="legend">
      <strong>Level of antibody staining</strong>*
      <ul class="level">
        <li><span class="high"></span> High</li>
        <li><span class="medium"></span> Medium</li>
        <li><span class="low"></span> Low</li>
        <li><span class="none"></span> None</li>
      </ul>

      <p class="small">* A validation score for immunohistochemistry is assigned for all antibodies and reflects the results of immunostaining.</p>

      <strong>About &amp; Source</strong>
      <p class="small">This chart represents a normal tissue &amp; organ summary of the antibody staining or the protein expression in a number of
      human tissues and organs. A description of the assay and annotation can be found <a target="new" href="http://www.proteinatlas.org/about/assays+annotation#ih">here</a>.</p>
      <a target="new" href="http://www.proteinatlas.org/"><img src="model/images/protein-atlas.gif" alt="Human Protein Atlas" /></a>
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
    jQuery("#proteinAtlasDisplayer div.expression").each(function() {
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
    jQuery("#proteinAtlasDisplayer table th.sortable").click(function() {
      var order = jQuery(this).attr('title');
      jQuery("#proteinAtlasDisplayer div.table.active").removeClass('active').addClass('inactive');
      jQuery("#proteinAtlasDisplayer div.table."+order).removeClass('inactive').addClass('active');
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
      if (jQuery("#proteinAtlasDisplayer").hasClass('ape')) {
          jQuery("#proteinAtlasDisplayer").attr('class', 'ape scale-' + ratio);
      } else {
          jQuery("#proteinAtlasDisplayer").attr('class', 'staining scale-' + ratio);
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
  })();
  </script>
</c:when>
<c:otherwise>
<h3 class="goog gray">Protein Atlas Tissue Expression</h3>
<p>No expression data available for this gene.</p>
</c:otherwise>
</c:choose>
</div>