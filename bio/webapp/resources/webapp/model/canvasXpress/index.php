<html>

  <head>

    <title>CanvasXpress</title>

    <meta http-equiv="CACHE-CONTROL" CONTENT="NO-CACHE">
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="keywords" content="canvasxpress, canvas, html5, graph, chart, plot, javascript, javascript library, genomic, scientific, android, animation, bar graph, line graph, dotplot, boxplot, heatmap, newick, scatter, 3d, pie, correlation, venn, network, market, candlestick, genome browser, isaac neuhaus"/>
    <meta name="description" content=""/>
    <meta http-equiv="Content-Language" content="en-us" />
    <meta name="Rating" content="general" />
    <meta name="googlebot" content="index,follow" />
    <meta name="robots" content="index,follow" />
    <meta name="author" content="Isaac Neuhaus" />
    <meta name="google-site-verification" content="UmQp3wIY7r9cDwr5LRdUDeDkpxAxg0Lv5tUc3tW9-90" />

    <link rel="stylesheet" href="./css/global.css" type="text/css"/>
    <link rel="stylesheet" href="./css/canvasXpress.css" type="text/css"/>

    <!--[if lt IE 9]><script type="text/javascript" src="./js/flashcanvas.js"></script><![endif]-->
    <script type="text/javascript" src="./js/canvasXpress.min.js"></script>

  </head>

  <body>

    <div class="page pageSP">

      <div class="header">

        <a href="./" class="logo">canvasXpress | Javascript Canvas Graphing Library</a>

        <div class="share">
          <a class="addthis_button" href="http://addthis.com/bookmark.php?v=250&amp;username=xa-4c58a3ba2ff9a164">
            <img src="http://s7.addthis.com/static/btn/v2/lg-share-en.gif" width="125" height="16" alt="Bookmark and Share" style="border:0"/>
          </a>
          <script type="text/javascript" src="http://s7.addthis.com/js/250/addthis_widget.js#username=xa-4c58a3ba2ff9a164"></script>
        </div>


        <div class="donate">
          <form action="https://www.paypal.com/cgi-bin/webscr" method="post">
            <input type="hidden" name="cmd" value="_s-xclick">
            <input type="hidden" name="hosted_button_id" value="EVP6CWVNJN7SY">
            <input type="image" src="https://www.paypal.com/en_US/i/btn/btn_donate_SM.gif" border="0" name="submit" alt="PayPal - The safer, easier way to pay online!">
            <img alt="" border="0" src="https://www.paypal.com/en_US/i/scr/pixel.gif" width="1" height="1">
          </form>
        </div>


        <div class="tweet">
          <a href="https://twitter.com/CanvasXpress" class="twitter-follow-button" data-show-count="false">Follow @CanvasXpress</a>
          <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0];if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src="//platform.twitter.com/widgets.js";fjs.parentNode.insertBefore(js,fjs);}}(document,"script","twitter-wjs");</script>
        </div>


        <ul class="nav">
          <li class="current"><a href="./">Home</a></li>
          <li ><a href="./documentation.html">Documentation</a></li>
          <li ><a href="./bar.html">Examples</a></li>
          <li ><a href="./general.html">API Reference</a></li>
          <li ><a href="./download.html">Download</a></li>
          <li ><a href="./about.html">About</a></li>
        </ul>

      </div>

      <div class="wrap">

        <div class="sp">

          <h3>Supported Graphs</h3>

          <form action="./search.html" id="cse-search-box" class="searchBox">
            <div>
             <input type="hidden" name="cx" value="015306267999521516642:feu1uzs43cg" />
             <input type="hidden" name="cof" value="FORID:9" />
             <input type="hidden" name="ie" value="UTF-8" />
             <input type="text" name="q" size="20" />
             <input type="submit" name="sa" value="Search" class="button okay med" />
            </div>
          </form>

          <script type="text/javascript" src="http://www.google.com/cse/brand?form=cse-search-box&lang=en"></script>


          <ul class="graphTypes">
            <li><a href="./area.html">Area</a></li>
            <li><a href="./arealine.html">AreaLine</a></li>
            <li><a href="./bar.html">Bar</a></li>
            <li><a href="./barline.html">BarLine</a></li>
            <li><a href="./boxplot.html">Boxplot</a></li>
            <li><a href="./candlestick.html">Candlestick</a></li>
            <li><a href="./circular.html">Circular</a></li>
            <li><a href="./correlation.html">Correlation</a></li>
            <li><a href="./dotline.html">DotLine</a></li>
            <li><a href="./dotplot.html">Dotplot</a></li>
            <li><a href="./genome.html">Genome</a></li>
            <li><a href="./heatmap.html">Heatmap</a></li>
            <li><a href="./histogram.html">Histogram</a></li>
            <li><a href="./kaplan-meyer.html">Kaplan-Meyer</a></li>
            <li><a href="./layout.html">Layout</a></li>
            <li><a href="./line.html">Line</a></li>
            <li><a href="./network.html">Network</a></li>
            <li><a href="./nonlinear-fit.html">NonLinear-Fit</a></li>
            <li><a href="./oncoprint.html">Oncoprint</a></li>
            <li><a href="./pie.html">Pie</a></li>
            <li><a href="./scatter2d.html">Scatter2D</a></li>
            <li><a href="./scatter3d.html">Scatter3D</a></li>
            <li><a href="./scatterbubble2d.html">ScatterBubble2D</a></li>
            <li><a href="./stacked.html">Stacked</a></li>
            <li><a href="./stackedline.html">StackedLine</a></li>
            <li><a href="./stackedpercent.html">StackedPercent</a></li>
            <li><a href="./stackedpercentline.html">StackedPercentLine</a></li>
            <li><a href="./tagcloud.html">TagCloud</a></li>
            <li><a href="./treemap.html">Treemap</a></li>
            <li><a href="./venn.html">Venn</a></li>
            <li><a href="./video.html">Video</a></li>
          </ul>



          <div class="share">
            <h3>Share canvasXpress</h3>
            <a class="addthis_button" href="http://addthis.com/bookmark.php?v=250&amp;username=xa-4c58a3ba2ff9a164">
              <img src="http://s7.addthis.com/static/btn/v2/lg-share-en.gif" width="125" height="16" alt="Bookmark and Share" style="border:0"/>
            </a>
            <script type="text/javascript" src="http://s7.addthis.com/js/250/addthis_widget.js#username=xa-4c58a3ba2ff9a164"></script>
          </div>


          <br />          <div class="donate">
            <h3>Donate to canvasXpress</h3>
            <form action="https://www.paypal.com/cgi-bin/webscr" method="post">
              <input type="hidden" name="cmd" value="_s-xclick">
              <input type="hidden" name="hosted_button_id" value="EVP6CWVNJN7SY">
              <input type="image" src="https://www.paypal.com/en_US/i/btn/btn_donate_SM.gif" border="0" name="submit" alt="PayPal - The safer, easier way to pay online!">
              <img alt="" border="0" src="https://www.paypal.com/en_US/i/scr/pixel.gif" width="1" height="1">
            </form>
          </div>


        </div>

        <div class="content">

          <br /><br />

          <h1>CanvasXpress Version 8.1</h1>

          <p>
            CanvasXpress is a standalone HTML5 graphing library
            written in Javascript that includes a simple and
            unobtrusive user interfase to explore complex data sets.
            CanvasXpress is supported in all major browsers in
            computers and mobile devices.
            <div class="read"><a href="./documentation.html"><img src="./images/readmore.png" /></a></div>
          </p>

          <div class="clear">&nbsp;</div>

          <div class="mod">
            <a href="./bar.html"><img src="./images/cx-ex-app.gif"/></a>
          </div>

          <div style="margin-top:0px; class="mod nm">
            <a href="./download.html"><img style="margin-left:20px;" src="./images/download1.png" /></a>
            <p>
              CanvasXpress is released under GPL3. The use of this
              library is permited for non-comercial use as long as its
              copyright notice is included without any modification.
              <div class="read"><a href="./download.html"><img src="./images/readmore.png" /></a></div>
            </p>
          </div>

          <div class="clear">&nbsp;</div>

          <h1>About CanvasXpress</h1>

          <p>
            CanvasXpress was developed as the core visualization
            component for bioinformatics and systems biology analysis
            at Bristol-Myers Squibb. It supports a large number of
            visualizations to display scientific and non-scientific
            data. CanvasXpress also includes a standalone unobtrusive
            data table and a filtering widget to allow data
            exploration similar to those only seen in other high-end
            commercial applications. Data can be easily sorted,
            grouped, transposed, transformed or clustered
            dynamically. The fully customizable mouse events as well
            as the zooming, panning and drag'n drop capabilities are
            features that make this library unique in its class.
            <div class="read"><a href="./about.html"><img src="./images/readmore.png" /></a></div>
          </p>

          <div class="clear">&nbsp;</div>

        </div>

        <div class="clear">&nbsp;</div>

      </div>

      <div class="footer">
        Copyright &copy; 2009-2015 canvasXpress.org |
        <a href="mailto:imnphd@gmail.com">Isaac&nbsp;Neuhaus</a> |
        Site Powered by <a href="http://www.artician.net">Artician</a>
      </div>

    </div>

    <div style="text-indent:-9999px;">
      &nbsp;
    </div>

    <script type="text/javascript">
      var _gaq = _gaq || [];
      _gaq.push(['_setAccount', 'UA-601687-37']);
      _gaq.push(['_trackPageview']);
      (function() {
        var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
        ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
        var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
      })();
    </script>

  </body>

</html>
