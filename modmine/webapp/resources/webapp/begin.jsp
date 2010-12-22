<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml />

<div id="ctxHelpDiv" class="welcome" style="display:none;">
  <div class="topBar info">
    <div id="ctxHelpTxt" class="welcome"></div>
    <a href="#" onclick="toggleWelcome();return false">Show more</a>
  </div>
</div>

<!-- BluePrint CSS container -->
<div class="container">
<div id="wrapper" class="span-12 last">

  <div id="welcome" class="span-11 last">
    <!-- <a class="close" href="#" title="Close" onclick="toggleWelcome();return false;">&nbsp;</a> -->
      <div class="top"></div>
      <div class="center span-11 last">
        <div class="bochs" id="bochs-1">
            <div id="welcome-content" class="span-11 last current">
              <div style="padding: 0px 20px;">
              <h2>Welcome to modMine</h2>
<p>The <strong>modENCODE</strong> project aims to identify all sequence-based functional elements in the <i><strong>C. elegans</strong></i> and <i><strong>D. melanogaster</strong></i> genomes. modENCODE labs submit data to the Data Coordination Center (DCC) where we organize and present the results.</p>
<br />
<p><strong>modMine</strong> is an integrated web resource of data &amp; tools to <strong>browse</strong> and <strong>search</strong> modENCODE data and experimental details, <strong>download</strong> results and access the GBrowse <strong>genome browser</strong>.  Explore some of the tools provided below.</p>
<br />
<h3><a href="/${WEB_PROPERTIES['webapp.path']}/projectsSummary.do"">Browse all modENCODE data</a></h3>
        <div class="span-11 last">
              <img src="model/images/data_preview.png" alt="experiments View"/>
            </div>
              <!-- <p>If you are short of time, just navigate through our set of <a href="#" onclick="switchBochs(2);return false;">Feature Hints</a>.-->
            </div>
          </div>
        </div>

        <div class="bochs" id="bochs-2" style="display: none;">
          <div id="thumb" class="span-4">
          <img
            src="themes/metabolic/thumbs/feature-search.jpg"
            alt="modMine Search" /></div>
          <div id="welcome-content" class="span-7 last">
            <h2>Search</h2>
            <p>Our search engine operates across many data fields giving you the
            highest chance of getting a result. Just type your search words in the
            box.</p>
          </div>
        </div>

        <div class="bochs" id="bochs-3" style="display: none;">
          <div id="thumb" class="span-4">
          <img
            src="themes/metabolic/thumbs/feature-facets.jpg"
            alt="modMine Facets" /></div>
          <div id="welcome-content" class="span-7 last">
            <h2>Facets</h2>
            <p><strong>Facets</strong> show you the different places where your search words were found (eg. within Gene, Protein, Go Term, Template, Publication etc).
            You can use the facets to filter for the type of results that are most important to you. When you've filtered by facets, you can even save the results
            straight to a List.</p>
          </div>
        </div>

            <div class="bochs" id="bochs-4" style="display: none;">
              <div id="thumb" class="span-4">
              <a title="Try Lists" href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view"><img
                src="themes/metabolic/thumbs/feature-lists.jpg"
                alt="modMine Lists" /></a></div>
              <div id="welcome-content" class="span-7 last">
                <h2>Lists</h2>
                <p>The <strong>Lists</strong> area lets you operate on whole sets of data at once. You can
                upload your own Lists (favourite Genes, SNPs etc) or save them from results tables.
                We also create useful <strong>Public Lists</strong> for everyone to use. Explore
                your data on the List Analysis Page</p>
                <p>Here are just some of the things you can do:</p>
                <ul>
                  <li>Ask questions about the data using our predefined Templates</li>
                  <li>Combine or subtract the content of other Lists</li>
                  <li>Uncover hidden relationships with our analysis <strong>Widgets</strong></li>
                </ul>
                <p>You can work with Lists from the Home page or select Lists from the Tab bar, located at the top of every page.</p>
            </div>
           </div>

            <div class="bochs" id="bochs-5" style="display: none;">
              <div id="thumb" class="span-4">
              <a title="Try Templates" href="/${WEB_PROPERTIES['webapp.path']}/templates.do"><img
                src="themes/metabolic/thumbs/feature-templates.jpg"
                alt="modMine Templates" /></a></div>
              <div id="welcome-content" class="span-7 last">
                <h2>Templates</h2>
                <p><strong>Template queries</strong> are 'predefined' queries designed around the common tasks performed by our Biologist Community. Templates
                provide you with a simple form that lets you define your starting point and optional filters to help focus your search.</p>
                <p>Templates cover common questions like:</p>
                <ul>
                    <li>I have a List of SNPs - do any of them affect Genes?</li>
                    <li>This Gene came up in my results - what can I find out about it?</li>
                    <li>I'm interested in this chromosome region - what's in there that could be linked with this disease?</li>
                </ul>
                <p>You can work with Templates from the Home page or select Templates from the Tab bar, located at the top of every page.</p>
            </div>
           </div>

            <div class="bochs" id="bochs-6" style="display: none;">
              <div id="thumb" class="span-4">
              <a title="Try MyMine" href="/${WEB_PROPERTIES['webapp.path']}/mymine.do"><img
                src="themes/metabolic/thumbs/feature-mymine.jpg"
                alt="modMine MyMine" /></a></div>
              <div id="welcome-content" class="span-7 last">
                <h2>MyMine</h2>
                <p><strong>MyMine</strong> is your <u>personal space</u> on modMine. Creating an account is easy. Just provide an e-mail and a password. You're ready to go.</p>
                <p>Your account allows you to:</p>
                <ul>
                  <li>Save Queries and Lists</li>
                  <li> Modify and save Templates for later use</li>
                  <li>Mark Public Templates as favourites so they're easier to find</li>
                </ul>
                <p>You can access mMyMine from the Tab bar, located at the top of every page.</p>
                <p>Note: Your data and e-mail address are confidential and we wont send you unsolicited mail.</p>
            </div>
           </div>

            <div class="bochs" id="bochs-7" style="display: none;">
              <div id="thumb" class="span-4">
              <a title="Try QueryBuilder" href="/${WEB_PROPERTIES['webapp.path']}/customQuery.do"><img
                src="themes/metabolic/thumbs/feature-querybuilder.jpg"
                alt="modMine QueryBuilder" /></a></div>
              <div id="welcome-content" class="span-7 last">
                <h2>QueryBuilder</h2>
                <p><strong>QueryBuilder (QB)</strong> is the Powerhouse of modMine.</p>
                <p>Its advanced interface lets you:</p>
                <ul>
                  <li>Construct your own custom queries
                  <li>Modify your previous queries
                  <li>You can even edit our predefined Templates.
                </ul>
                <p>The easiest way to get started with QB is by editing one of our pre-existing Template queries.
                Follow the simple tutorial in the QueryBuilder section of the <strong>Tour</strong> to see how to change a Template output or add a filter.</p>

                <p>You can access QueryBuilder from the Tab bar, located at the top of every page.</p>
            </div>
           </div>

      <div class="span-11 last">
            <ul id="switcher">
                <li id="switcher-1" class="switcher current"><a onclick="switchBochs(1);return false;" href="#">Start</a></li>
                <li id="switcher-2" class="switcher"><a onclick="switchBochs(2);return false;" href="#">1</a></li>
                <li id="switcher-3" class="switcher"><a onclick="switchBochs(3);return false;" href="#">2</a></li>
                <li id="switcher-4" class="switcher"><a onclick="switchBochs(4);return false;" href="#">3</a></li>
                <li id="switcher-5" class="switcher"><a onclick="switchBochs(5);return false;" href="#">4</a></li>
                <li id="switcher-6" class="switcher"><a onclick="switchBochs(6);return false;" href="#">5</a></li>
                <li id="switcher-7" class="switcher"><a onclick="switchBochs(7);return false;" href="#">6</a></li>
            </ul>
          </div>
      </div>
      <div class="bottom span-11 last"></div>
  </div>

   <div id="search-bochs" class="span-4">
     <img title="search" src="themes/purple/homepage/search-ico-right.png" class="title">
     <h3><a href="/${WEB_PROPERTIES['webapp.path']}/keywordSearchResults.do?searchBag=">Search</a></h3>
     <div class="text">
       <span style="width:71px; float:left;">&nbsp;</span>
        <p>Enter names, identifiers or keywords for genes, proteins, pathways, ontology terms, etc. (e.g.
        <a onclick="preFillInput('zen', 'input#dataSearch');return false;" title="Search for zen"
          href="#"><strong>zen</strong></a>,
        <a onclick="preFillInput('pha-4', 'input#dataSearch');return false;" title="Search for pha-4"
          href="#"><strong>pha-4</strong></a>,
        <a onclick="preFillInput('DNA binding', 'input#dataSearch');return false;" title="Search for DNA binding"
          href="#"><strong>DNA binding</strong></a>).
        <form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
          <input id="dataSearch" class="input" type="text" name="searchTerm" value="e.g. zen, pha-4" />
          <input type="submit" value="Search" />
        </form>

        <br />Or search <strong>modENCODE experiments</strong> by type, lab name, antibody, etc. (e.g.
        <a onclick="preFillInput('RNA seq', 'input#exptSearch');return false;" title="Search for RNA-seq"
         href="#"><strong>RNA-seq</strong></a>,
        <a onclick="preFillInput('Snyder', 'input#exptSearch');return false;" title="Search for Snyder"
          href="#"><strong>Snyder</strong></a>,
        <a onclick="preFillInput('CP190', 'input#exptSearch');return false;" title="Search for CP190"
          href="#"><strong>CP190</strong></a>).
        </p>
        <!-- <p>[Supports AND, OR, NOT and wildcard*]</p> -->
        <br />
         <html:form action="/modMineSearchAction">
            <input id="exptSearch" name="searchTerm" type="text" class="input" value="e.g. RNA-seq, CP190">
            <html:submit>Experiment Search</html:submit>
        </html:form>

     </div>
   </div>

   <div id="lists-bochs" class="span-4">
     <img title="lists" src="images/icons/lists-64.png" class="title">
     <h3><a href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view">Analyze Lists of Data</a></h3>
     <div class="text">
       <span style="height: 25px; float: left; width:100px;">&nbsp;</span>
       <p>
         <img src="themes/metabolic/thumbs/widget-charts-5.png" alt="widget charts" style="float:right;padding-left:5px;margin-right:4px;" />
         <strong>Explore</strong> and <strong>Analyze</strong>. Upload lists of identifiers to use in queries and discover relationshops in our analysis widgets.
       </p>
       <br />
       <a href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view" class="button green">Lists</a>
     </div>
   </div>

   <div id="templates-bochs" class="span-4 last">
     <img title="templates" src="images/icons/templates-64.png" class="title">
     <h3><a href="/${WEB_PROPERTIES['webapp.path']}/templates.do">Use Template Queries</a></h3>
     <div class="text">
        <span style="width: 72px; float: left;">&nbsp;</span>
        <p>Get started with <strong>powerful queries</strong> using our predefined searches. These customizable templates have been
           designed around common tasks performed by our biologist community.</p>
        <p>To see how they work, why not try a template from our <strong>examples page</strong>?</p>
        <br />
        <a href="/${WEB_PROPERTIES['webapp.path']}/templates.do" class="button violet">Templates</a>
     </div>
   </div>

   <div id="bochs" class="span-4">
     <h3>Fly Gene Expression</h3>
     <a href="/${WEB_PROPERTIES['webapp.path']}/projectsSummary.do"><div class="heatmap"><img src="themes/modmine/icons/exView.png" alt="experiments View"/></div></a>
     <div class="text">
       <p>View an expression score heatmap for any list of fly genes.</p>
     </div>
   </div>

   <div id="bochs" class="span-4">
     <h3>Fly Chromatin states</h3>
     <a href="/${WEB_PROPERTIES['webapp.path']}/chromatinStates.do"><div class="heatmap"><img src="themes/modmine/icons/flyscore-text.png" alt="heatmap"/></div></a>
<br />

     <a target ="new" href="http://compbio.med.harvard.edu/flychromatin/"><div class="heatmap"><img src="themes/modmine/icons/bg3-text.png" alt="heatmap"/></div></a>

     <div class="text">
       <p></p>
     </div>
   </div>

   <div id="bochs" class="span-4 last">
     <h3>Genomic Region Search</h3>
     <a href="/${WEB_PROPERTIES['webapp.path']}/spanUploadOptions.do"><div class="heatmap"><img src="model/images/genome_region.png" alt="Genome Region Search"/></div></a>
     <div class="text">
       <p>Find modENCODE data in given regions of the genome.
       </p>
       <div style="text-align: center;"><a href="/${WEB_PROPERTIES['webapp.path']}/spanUploadOptions.do">Region Search</a></div>
     </div>
   </div>

   <div style="clear:both;"></div>

   <div id="templates-menu" class="span-11 last">
       <table id="menu" border="0" cellspacing="0">
           <tr>
               <td><div class="cont"><span id="tab1">Genes</span></div></td>
               <td><div class="cont"><span id="tab2">Proteins</span></div></td>
               <td><div class="cont"><span id="tab3">Interactions</span></div></td>
               <td><div class="cont"><span id="tab4">Pathways</span></div></td>
               <td><div class="cont"><span id="tab5">Homologues</span></div></td>
               <td><div class="cont"><span id="tab6">Gene Ontology</span></div></td>
               <td><div class="cont"><span id="tab7">Gene Expression</span></div></td>
           </tr>
       </table>

       <div id="tab-content">
           <div id="content1" class="content">
               <p>The gene models and other genome annotation in FlyMine are provided by a variety of source databases including: FlyBase, UniProt, Ensembl and over
               30 other data sources. <a href="dataCategories.do">Read more</a></p>
               <br/>
               <p>Query for genes:</p>
               <tiles:insert name="aspectTemplates.jsp">
                   <tiles:put name="aspectQueries" beanName="aspectQueries" />
                   <tiles:put name="aspectTitle" value="Genomics" />
               </tiles:insert>
           </div>
           <div id="content2" class="content">
               <p>FlyMine loads proteins from UniProt and FlyBase, and protein domains from InterPro. <a href="aspect.do?name=Proteins">Read
               more</a></p>
               <br/>
               <p>Query for proteins:</p>
               <tiles:insert name="aspectTemplates.jsp">
                   <tiles:put name="aspectQueries" beanName="aspectQueries" />
                   <tiles:put name="aspectTitle" value="Proteins" />
               </tiles:insert>
           </div>
           <div id="content3" class="content">
               <p>FlyMine loads physical interactions from IntAct and BioGRID, and genetic interaction from FlyBase. <a href="aspect.do?name=Interactions">Read more</a></p>
               <br/>
               <p>Query for interactions:</p>
               <tiles:insert name="aspectTemplates.jsp">
                   <tiles:put name="aspectQueries" beanName="aspectQueries" />
                   <tiles:put name="aspectTitle" value="Interactions" />
               </tiles:insert>
           </div>
           <div id="content4" class="content">
               <p>FlyMine loads pathway data from KEGG, Reactome and FlyReactome. <a href="aspect.do?name=Pathways">Read more..</a></p>
               <br/>
               <p>Query for pathways:</p>
               <tiles:insert name="aspectTemplates.jsp">
                   <tiles:put name="aspectQueries" beanName="aspectQueries" />
                   <tiles:put name="aspectTitle" value="Pathways" />
               </tiles:insert>
           </div>
           <div id="content5" class="content">
               <p>FlyMine loads homologue predictions from InParanoid, KEGG and TreeFam. <a href="aspect.do?name=Comparative+Genomics">
               Read more</a></p>
               <br/>
               <p>Query for homologues:</p>
               <tiles:insert name="aspectTemplates.jsp">
                   <tiles:put name="aspectQueries" beanName="aspectQueries" />
                   <tiles:put name="aspectTitle" value="Comparative Genomics" />
               </tiles:insert>
           </div>
           <div id="content6" class="content">
               <p>FlyMine loads Gene Ontology annotation from MGI, FlyBase, WormBase, UniProt, SGD, and InterPro.
               <a href="aspect.do?name=Gene+Ontology">Read more</a></p>
               <br/>
               <p>Query using gene ontology:</p>
               <tiles:insert name="aspectTemplates.jsp">
                   <tiles:put name="aspectQueries" beanName="aspectQueries" />
                   <tiles:put name="aspectTitle" value="Gene Ontology" />
               </tiles:insert>
           </div>
           <div id="content7" class="content">
               <p>FlyMine loads gene expression data for Drosophila melanogaster and Anopheles gambiae from FlyAtlas, BDGP, ArrayExpress and Fly-FISH.
               <a href="aspect.do?name=Gene+Expression">Read more</a></p>
               <br/>
               <p>Query for gene expression:</p>
               <tiles:insert name="aspectTemplates.jsp">
                   <tiles:put name="aspectQueries" beanName="aspectQueries" />
                   <tiles:put name="aspectTitle" value="Gene Expression" />
               </tiles:insert>
           </div>
       </div>
   </div>

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
   var exptPlaceholder = 'e.g. RNA-seq, CP190';
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
   $('input#dataSearch').toggleClass(inputToggleClass);
   $('input#exptSearch').toggleClass(inputToggleClass);

   // register input elements with blur & focus
   $('input#dataSearch').blur(function() {
     if ($(this).val() == '') {
       $(this).toggleClass(inputToggleClass);
       $(this).val(dataPlaceholder);
     }
   });
   // register input elements with blur & focus
   $('input#exptSearch').blur(function() {
     if ($(this).val() == '') {
       $(this).toggleClass(inputToggleClass);
       $(this).val(exptPlaceholder);
     }
   });
   $('input#dataSearch').focus(function() {
     if ($(this).hasClass(inputToggleClass)) {
       $(this).toggleClass(inputToggleClass);
       $(this).val('');
     }
   });
   $('input#exptSearch').focus(function() {
	     if ($(this).hasClass(inputToggleClass)) {
	       $(this).toggleClass(inputToggleClass);
	       $(this).val('');
	     }
	   });

   // templates switcher
   jQuery(document).ready(function() {
        jQuery("#tab-content .content").each(function() {
            jQuery(this).hide();
        });

        jQuery("table#menu td:first").addClass("active").show();
        jQuery("div.content:first").show();

        jQuery("table#menu td").click(function() {
            jQuery("table#menu td").removeClass("active");
            jQuery(this).addClass("active");
            jQuery("#tab-content .content").hide();

            if (jQuery(this).is('span')) {
                // span
                var activeTab = jQuery(this).attr("id").substring(3);
            } else {
                // td, div (IE)
                var activeTab = jQuery(this).find("span").attr("id").substring(3);
            }
            jQuery('#content' + activeTab).fadeIn();

            return false;
        });
    });
</script>
