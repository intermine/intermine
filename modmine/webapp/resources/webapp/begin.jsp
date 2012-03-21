<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml />

<script src="http://code.jquery.com/jquery-latest.js"></script>

<script>
// get the categories from the sam web service that feeds modencode home page
$.getJSON("${WEB_PROPERTIES['webapp.url']}/${WEB_PROPERTIES['webapp.path']}/service/query/metadatacache/catexp",
        function(data) {
    var url = "${WEB_PROPERTIES['webapp.path']}/categorySummary.do?category=";
    var items = [];
    items.push('<tr>');
    $.each(data, function(key, val) {
        $.each(val, function (k,v){
            // alert("AA" + k + " ++ " + v.organisms[0].experiments[0].experiment_name);
            items.push('<td id="' + k + '"><a href="/' + url + v.category + '">' + v.category
                    +  '<img src="images/right-arrow.gif" /></a></td>');
        });
    });

    $('<table/>', {
        'class': 'projects',
        html: items.join('')
    }).appendTo('#catnavigation');
});
</script>




<div id="ctxHelpDiv" class="welcome" style="display:none;">
  <div class="topBar info">
    <div id="ctxHelpTxt" class="welcome"></div>
    <a href="#" onclick="toggleWelcome();return false">Show more</a>
  </div>
</div>

<!-- BluePrint CSS container -->
<div class="container">
<div id="wrapper" class="span-42 last">

  <div id="welcome" class="span-42 last">
    <!-- <a class="close" href="#" title="Close" onclick="toggleWelcome();return false;">&nbsp;</a> -->
      <div class="top"></div>
      <div class="center span-42 last">
        <div class="bochs" id="bochs-1">
            <div id="welcome-content" class="span-42 last current">
              <div style="padding:0 20px;">
              <h2>Welcome to modMine</h2>
<p>The <strong><a href="http://www.modencode.org/">modENCODE</a></strong> project aims to identify all sequence-based functional elements
in the <i><strong>C. elegans</strong></i> and <i><strong>D. melanogaster</strong></i> genomes.
modENCODE labs submit data to the Data Coordination Center (DCC) where we organize and present the results.</p>
<p><strong>modMine</strong> is an integrated web resource of data &amp; tools to <strong>browse</strong>
and <strong>search</strong> modENCODE data and experimental details, <strong>download</strong>
results and access the GBrowse <strong>genome browser</strong>. Explore some of the
<a href="#" onclick="switchBochs(2);return false;">tools</a> provided below and
check out our <b><a href="http://www.modencode.org/quickstart/">quick start guide</a></b>!

</p>
<br />
<p><strong>modMine</strong> release <strong>${WEB_PROPERTIES['project.releaseVersion']}</strong>
uses genome annotations <strong>${WEB_PROPERTIES['genomeVersion.fly']}</strong> for fly and
<strong>${WEB_PROPERTIES['genomeVersion.worm']}</strong> for worm.</p>
<br />



</div>

<div style="padding:0 20px;">
<h3><a href="/${WEB_PROPERTIES['webapp.path']}/projectsSummary.do">Browse all modENCODE data</a></h3>
        <div class="span-42 last">

        <div id="catnavigation" style="padding: 0px 15px;">

        </div>
</div>

            </div>
          </div>
        </div>

        <div class="bochs" id="bochs-2" style="display: none;">
          <div id="thumb">
          <img
            src="themes/metabolic/thumbs/feature-search.jpg"
            alt="modMine Search" /></div>
          <div id="welcome-content" class="span-27 last">
            <h2>Search</h2>
            <p>Our search engine operates across many data fields and understands logical operations
            (AND, OR, NOT) and approximate searches (use *).</p>
            <p>Just type your search words in the box!</p>
          </div>
        </div>

        <div class="bochs" id="bochs-3" style="display: none;">
          <div id="thumb">
          <img
            src="themes/metabolic/thumbs/feature-facets.jpg"
            alt="modMine Facets" /></div>
          <div id="welcome-content" class="span-27 last">
            <h2>Facets</h2>
            <p><strong>Facets</strong> show you the different places where your search words were
            found (eg. within Gene, Protein, Go Term, Publication etc).</p><p>
            You can use the facets to filter for the type of results that are most important to you.
            </p><p>When you've filtered by facets, you can save the results straight to a List.</p>
          </div>
        </div>

            <div class="bochs" id="bochs-4" style="display: none;">
              <div id="thumb" >
              <a title="Try Lists" href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view"><img
                src="themes/metabolic/thumbs/feature-lists.jpg"
                alt="modMine Lists" /></a></div>
              <div id="welcome-content" class="span-27 last">
                <h2>Lists</h2>
                <p>The <strong>Lists</strong> area lets you operate on whole sets of data at once. You can
                upload your own Lists (favourite Genes, Submissions etc) or save them from results tables.
                Various analysis tools for a first data exploration are provided when you are vieving the list.
                There are also useful pre-defined Public Lists for everyone to use.
                <p>Here are just some of the things you can do:</p>
                <ul>
                  <li>Ask questions about the data using our pre-defined Templates</li>
                  <li>Combine or subtract the content of other Lists</li>
                  <li>Uncover hidden relationships with our analysis <strong>Widgets</strong></li>
                </ul>
                <p>Select Lists from the Tab bar, located at the top of every page.</p>
            </div>
           </div>

            <div class="bochs" id="bochs-5" style="display: none;">
              <div id="thumb" >
              <a title="Try Templates" href="/${WEB_PROPERTIES['webapp.path']}/templates.do"><img
                src="themes/metabolic/thumbs/feature-templates.jpg"
                alt="modMine Templates" /></a></div>
              <div id="welcome-content" class="span-27 last">
                <h2>Templates</h2>
                <p><strong>Template queries</strong> are 'pre-defined' queries designed around the
                common tasks performed by our community of biologists.
                Templates provide you with a simple form that lets you set your starting point and
                optional filters to help focus your search.</p>
                <p>There are plenty of templates available, a search tools is available in the template page.
                If you don't find what are you looking for please contact us.</p>
                <p>Select Templates from the Tab bar, located at the top of every page.</p>
            </div>
           </div>

            <div class="bochs" id="bochs-6" style="display: none;">
              <div id="thumb" >
              <a title="Try MyMine" href="/${WEB_PROPERTIES['webapp.path']}/mymine.do"><img
                src="themes/metabolic/thumbs/feature-mymine.jpg"
                alt="modMine MyMine" /></a></div>
              <div id="welcome-content" class="span-27 last">
                <h2>MyMine</h2>
                <p><strong>MyMine</strong> is your <u>personal space</u> on modMine. Creating an account is easy. Just provide an e-mail and a password. You're ready to go.</p>
                <p>Your account allows you to:</p>
                <ul>
                  <li>Save Queries and Lists</li>
                  <li>Modify or create Templates, and save them for later use</li>
                  <li>Mark as favourites Public Templates, so they're easier for you to find</li>
                </ul>
                <p>You can access mMyMine from the Tab bar, located at the top of every page.</p>
                <p>Your data and e-mail address are confidential.</p>
            </div>
           </div>

            <div class="bochs" id="bochs-7" style="display: none;">
              <div id="thumb" >
              <a title="Try QueryBuilder" href="/${WEB_PROPERTIES['webapp.path']}/customQuery.do"><img
                src="themes/metabolic/thumbs/feature-querybuilder.jpg"
                alt="modMine QueryBuilder" /></a></div>
              <div id="welcome-content" class="span-27 last">
                <h2>QueryBuilder</h2>
                <p><strong>QueryBuilder</strong> allows you to construct your own queries and
                is the most powerful and flexible method to mine data in modMine.</p>
                <p>The easiest way to get started is by editing one of our pre-existing Template queries.
                Follow the simple tutorial in the QueryBuilder section of the <strong>Tour</strong>
                to see how to change a Template output or add a filter.</p>

                <p>You can access QueryBuilder from the Tab bar, located at the top of every page.</p>
            </div>
           </div>

      <div class="span-27 last">
            <ul id="switcher">
                <li id="switcher-1" class="switcher current"><a onclick="switchBochs(1);return false;" href="#">Exit hints</a></li>
                <li id="switcher-2" class="switcher"><a onclick="switchBochs(2);return false;" href="#">1</a></li>
                <li id="switcher-3" class="switcher"><a onclick="switchBochs(3);return false;" href="#">2</a></li>
                <li id="switcher-4" class="switcher"><a onclick="switchBochs(4);return false;" href="#">3</a></li>
                <li id="switcher-5" class="switcher"><a onclick="switchBochs(5);return false;" href="#">4</a></li>
                <li id="switcher-6" class="switcher"><a onclick="switchBochs(6);return false;" href="#">5</a></li>
                <li id="switcher-7" class="switcher"><a onclick="switchBochs(7);return false;" href="#">6</a></li>
            </ul>
          </div>
      </div>
      <div class="bottom span-27 last"></div>
  </div>

   <div class="span-21">
   <div id="search-bochs">
     <img title="search" src="themes/purple/homepage/search-ico-right.png" class="title">
     <h3><a href="/${WEB_PROPERTIES['webapp.path']}/keywordSearchResults.do?searchBag=">Search</a></h3>
     <div class="text">
       <span style="width:76px; float:left;">&nbsp;</span>
        <p>Enter names, identifiers or keywords for genes, proteins, pathways, ontology terms, etc. (e.g.
        <a onclick="preFillInput('zen', 'input#dataSearch');return false;" title="Search for zen"
          href="#"><strong>zen</strong></a>,
        <a onclick="preFillInput('pha-4', 'input#dataSearch');return false;" title="Search for pha-4"
          href="#"><strong>pha-4</strong></a>,
        <a onclick="preFillInput('DNA-binding', 'input#dataSearch');return false;" title="Search for DNA-binding"
          href="#"><strong>DNA-binding</strong></a>).
        <form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
          <input id="dataSearch" class="input" type="text" name="searchTerm" value="e.g. zen, pha-4" />
          <input type="submit" value="Search" />
        </form>

        <br />Or search <strong>modENCODE experiments</strong> by type, lab name, antibody, etc. (e.g.
        <a onclick="preFillInput('ChIP-seq', 'input#exptSearch');return false;" title="Search for ChIP-seq"
         href="#"><strong>ChIP-seq</strong></a>,
        <a onclick="preFillInput('Snyder', 'input#exptSearch');return false;" title="Search for Snyder"
          href="#"><strong>Snyder</strong></a>,
        <a onclick="preFillInput('CP190', 'input#exptSearch');return false;" title="Search for CP190"
          href="#"><strong>CP190</strong></a>).
        </p>
        <!-- <p>[Supports AND, OR, NOT and wildcard*]</p> -->
        <br />
         <html:form action="/modMineSearchAction">
            <input id="exptSearch" name="searchTerm" type="text" class="input" value="e.g. ChIP-seq, CP190">
            <html:submit>Experiment Search</html:submit>
        </html:form>

     </div>
   </div>
   </div>

   <div class="span-21 last">
   <div id="genomic-bochs">
     <img title="lists" src="images/icons/genomic-search-64.png" class="title">
     <h3><a href="/${WEB_PROPERTIES['webapp.path']}/spanUploadOptions.do">Genomic Region Search</a></h3>
     <div class="text">
       <span style="width:76px; float:left;"></span>
       <p>
         <a href="/${WEB_PROPERTIES['webapp.path']}/spanUploadOptions.do">
         <img src="themes/modmine/genome_region.jpg" alt="Genome Region Search" style="float:right;padding-left:5px;margin-right:4px;"/>
         </a>
         <strong>Explore</strong> a genomic region for features found by the <strong>modENCODE</strong> project.
         <a href="/${WEB_PROPERTIES['webapp.path']}/spanUploadOptions.do">Genomic Region Search</a>
       </p>
       <br />
     </div>
   </div>
   </div>

   <div style="clear:both;"></div>


   <div class="span-14">
   <div id="bochs">
     <h3>Fly Gene Expression</h3>
     <center><a href="/${WEB_PROPERTIES['webapp.path']}/bagDetails.do?scope=global&bagName=example"><div class="heatmap"><img src="themes/modmine/fly_heatmap.jpg" alt="Fly expression heatmap"/></div></a></center>
     <div class="text">
       <p>View an expression score heatmap for any list of fly genes.  See an <a href="/${WEB_PROPERTIES['webapp.path']}/bagDetails.do?scope=global&bagName=example">example</a>.</p>
       <p></p>
     <p>To upload your own list of genes, use the form above or go to the 'Lists' tab and click on
     <a class="heatmap" href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=upload">'Upload'</a> to create and name
the new list. You can also use any of modMine's queries to create a list.
     </p>
     </div>

   </div>
   </div>

   <div class="span-14">
   <div id="bochs">
     <h3>Fly Chromatin states</h3>
     <div class="text" >
       <left>
       <a class="heatmap" href="/${WEB_PROPERTIES['webapp.path']}/chromatinStates.do"><img src="themes/modmine/flyscore.jpg" alt="flyscore"/><span>GBrowse Ideograms</span></a>
       </left>
       <br />
<p></p>
       <left>
       <a class="heatmap" target ="new" href="http://compbio.med.harvard.edu/flychromatin/"><img src="themes/modmine/parklab.jpg" alt="parklabviewer"/><span>Park Lab Viewer</span></a>
       </left>
       <p>Includes folded view and also data about DHS, TSS, replication, etc.</p>
     </div>
   </div>
   </div>

   <div class="span-14 last">
   <div id="bochs">
     <h3>Regulatory Network</h3>

     <center><a href="/${WEB_PROPERTIES['webapp.path']}/wormRegulatoryNetwork.do">Worm <div class="heatmap"><img src="themes/modmine/worm-network-detail2.jpg" alt="Worm Regulatory Network"/></div></a></center>
     <center><a href="/${WEB_PROPERTIES['webapp.path']}/flyRegulatoryNetwork.do">Fly <div class="heatmap"><img src="themes/modmine/fly-network-detail2.jpg" alt="Fly Regulatory Network"/></div></a></center>

     <div class="text">
       <p><strong>Explore</strong> an hierarchical view of the physical regulatory networks.
       </p>
     </div>
   </div>
   </div>

   <div style="clear:both;"></div>

      <div class="span-14">
   <div id="upload-bochs">
     <img title="lists" src="images/icons/upload-64.png" class="title">
     <h3><a href="/${WEB_PROPERTIES['webapp.path']}/bag.do">Upload Lists</a></h3>
     <div class="text">
       <span style="width: 72px; float: left;">&nbsp;</span>
        <p>Enter a <strong>list</strong> of identifiers.</p>
        <br />
           <form name="buildBagForm" method="post" action="<c:url value="/buildBag.do" />">
               <select name="type">
                 <c:forEach var="bag" items="${preferredBags}">
                    <c:choose>
                      <c:when test="${bag == 'Gene'}">
                        <option value="Gene" selected="selected">Gene</option>
                      </c:when>
                      <c:otherwise>
                        <option value="<c:out value="${bag}" />"><c:out value="${bag}" /></option>
                      </c:otherwise>
                    </c:choose>
                 </c:forEach>
               </select>
               <textarea id="listInput" name="text"><c:out
               value="${WEB_PROPERTIES['bag.example.identifiers']}"
                    default="zen, eve, CG4807, FBgn0000099" /></textarea>
               <br /><br />
                <center>
                  <a href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=upload">advanced</a>
                  <br />
                  <input type="submit" value="upload"/>
                </center>
           </form>
     </div>
   </div>
   </div>

   <div class="span-14">
   <div id="templates-bochs">
     <img title="templates" src="images/icons/templates-64.png" class="title">
     <h3><a href="/${WEB_PROPERTIES['webapp.path']}/templates.do">Use Template Queries</a></h3>
     <div class="text">
        <span style="width: 77px; float: left;">&nbsp;</span>
        <p>Get started with <strong>powerful queries</strong> using our predefined searches. These customizable templates have been
           designed around common tasks performed by our biologist community.</p>
        <p>To see how they work, why not try a template from our <strong>examples page</strong>?</p>
        <br />
        <a href="/${WEB_PROPERTIES['webapp.path']}/templates.do" class="button violet">Templates</a>
     </div>
   </div>
   </div>

   <div class="span-14 last">
   <div id="lists-bochs">
     <img title="lists" src="images/icons/lists-64.png" class="title">
     <h3><a href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view">Analyze Lists of Data</a></h3>
     <div class="text">
       <span style="height: 25px; float: left; width:100px;">&nbsp;</span>
       <p>
         <img src="themes/modmine/widget-charts.jpg" alt="widget charts" style="float:right;padding-left:5px;margin-right:4px;" />
         <strong>Explore</strong> and <strong>Analyze</strong>. Upload lists of identifiers to use in queries and discover relationshops in our analysis widgets.
         See an <a href="/${WEB_PROPERTIES['webapp.path']}/bagDetails.do?scope=global&bagName=example">example</a>.
       </p>
       <br />
       <a href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view" class="button green">Lists</a>
     </div>
   </div>
   </div>

   <div style="clear:both;"></div>



</div>
</div>

<script type="text/javascript">
   // minimize big welcome box into an info message
   function toggleWelcome() {
     // minimizing?
     if ($("#welcome").is(':visible')) {
       // hide the big box
       $('#welcome').slideUp();
       // do we have words to say?
       var welcomeText = $("#welcome-content.current").text();
       if (welcomeText.length > 0) {
         $("#ctxHelpDiv.welcome").slideDown("slow", function() {
           // ...display a notification with an appropriate text
           if (welcomeText.length > 150) {
             // ... substr
             $("#ctxHelpTxt.welcome").html(welcomeText.substring(0, 150) + '&hellip;');
           } else {
             $("#ctxHelpTxt.welcome").html(welcomeText);
           }
           });
       }
     } else {
       $("#ctxHelpDiv.welcome").slideUp(function() {
         $("#welcome").slideDown("slow");
       });
     }
   }

   /* hide switcher of we are on first time here */
   if ($("#switcher-1").hasClass('current')) {
     $("#switcher").hide();
   }

   /* div switcher for welcome bochs using jQuery */
   function switchBochs(newDivId) {
     // no current
     javascript:jQuery(".switcher").each (function() { javascript:jQuery(this).removeClass('current'); });
     // apply current
     javascript:jQuery('#switcher-'+newDivId).addClass('current');
     // hide them all bochs
     javascript:jQuery(".bochs").each (function() { javascript:jQuery(this).hide(); });
     // then show our baby
     javascript:jQuery('#bochs-'+newDivId).fadeIn();

     // apply active class
     $("#welcome-content").each (function() { javascript:jQuery(this).removeClass('current'); });
     $('#bochs-'+newDivId+' > #welcome-content').addClass('current');

     // show/hide switcher?
     if ($("#switcher-1").hasClass('current')) {
       $("#switcher").hide();
     } else {
       $("#switcher").show();
     }
   }

   // placeholder value for search boxes
   var dataPlaceholder = 'e.g. zen, pha-4';
   var exptPlaceholder = 'e.g. ChIP-seq, CP190';
   var placeholderTextarea = '<c:out value="${WEB_PROPERTIES['textarea.identifiers']}" />';
   // class used when toggling placeholder
   var inputToggleClass = 'eg';

   /* pre-fill search input with a term */
   function preFillInput(term, input) {
     var e = $(input);
     e.val(term);
      if (e.hasClass(inputToggleClass)) e.toggleClass(inputToggleClass);
    e.focus();
   }

   // e.g. values only available when JavaScript is on
   jQuery('input#dataSearch').toggleClass(inputToggleClass);
   jQuery('input#exptSearch').toggleClass(inputToggleClass);
   jQuery('textarea#listInput').toggleClass(inputToggleClass);

   // register input elements with blur & focus
   $('input#dataSearch').blur(function() {
     if ($(this).val() == '') {
       $(this).toggleClass(inputToggleClass);
       $(this).val(dataPlaceholder);
     }
   });
   // register input elements with blur & focus
   jQuery('input#exptSearch').blur(function() {
     if ($(this).val() == '') {
       $(this).toggleClass(inputToggleClass);
       $(this).val(exptPlaceholder);
     }
   });
   jQuery('input#dataSearch').focus(function() {
     if ($(this).hasClass(inputToggleClass)) {
       $(this).toggleClass(inputToggleClass);
       $(this).val('');
     }
   });
   jQuery('input#exptSearch').focus(function() {
       if ($(this).hasClass(inputToggleClass)) {
         $(this).toggleClass(inputToggleClass);
         $(this).val('');
       }
     });
   jQuery('textarea#listInput').blur(function() {
       if (jQuery(this).val() == '') {
           jQuery(this).toggleClass(inputToggleClass);
           jQuery(this).val(placeholderTextarea);
       }
   });
   jQuery('textarea#listInput').focus(function() {
       if (jQuery(this).hasClass(inputToggleClass)) {
           jQuery(this).toggleClass(inputToggleClass);
           jQuery(this).val('');
       }
   });
</script>
