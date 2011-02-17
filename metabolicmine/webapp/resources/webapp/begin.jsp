<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml/>

<!-- metabolicMine CSS -->
<!--[if lt IE 8]><link rel="stylesheet" href="/metabolicmine/model/public/css/ie.css" type="text/css" media="screen, projection"/><![endif]-->

<div id="corner">&nbsp;</div>

<script type="text/javascript">
  // specify what happens to element in a small browser window (better add class than modify from here)
  if ($(window).width() < '1205') {
    // cite etc
    $('ul#topnav').addClass('smallScreen');
    // corners
    $('#corner').addClass('smallScreen');
    if ($(window).width() < '1125') {
      $('#help').addClass('smallScreen');
    }
  }
  // placeholder value for search boxes
  var placeholder = '<c:out value="${WEB_PROPERTIES['homeSearch.identifiers']}" />';
  // class used when toggling placeholder
  var inputToggleClass = 'eg';

  /**
   * A function that will save a cookie under a key-value pair
   * @key Key under which to save the cookie
   * @value Value to associate with the key
   * @days (optional) The number of days from now when to expire the cookie
   */
    jQuery.setCookie = function(key, value, days) {
      if (days == null) {
        document.cookie = key + "=" + escape(value);
      } else {
        // form date
        var expires = new Date();
        expires.setDate(expires.getDate() + days);
        // form cookie
        document.cookie = encodeURIComponent(key) + "=" + encodeURIComponent(value) + "; expires=" + expires.toUTCString();
      }
    };

  /**
   * A function that will get a cookie's value based on a provided key
   * @key Key under which the cookie is saved
   */
    jQuery.getCookie = function(key) {
      return (r = new RegExp('(?:^|; )' + encodeURIComponent(key) + '=([^;]*)').exec(document.cookie)) ?
      decodeURIComponent(r[1]) : null;
    };

</script>

<!-- preview div -->
<div id="ctxHelpDiv" class="preview">
  <div class="topBar info">
    <div id="ctxHelpTxt" class="preview">You are looking at our latest preview site. Enjoy and <a href="#" onclick="showContactForm();return false;">
    let us know</a> if events do not turn out as expected. Thank you!</div>
  </div>
</div>

<!-- faux context help -->
<div id="ctxHelpDiv" class="welcome" style="display:none;">
  <div class="topBar info">
    <div id="ctxHelpTxt" class="welcome"></div>
    <a href="#" onclick="toggleWelcome();return false">Show more</a>
  </div>
</div>

<!-- BluePrint CSS container -->
<div class="container">

  <script type="text/javascript">
    // minimize big welcome box into an info message
    function toggleWelcome(speed) {
      // default speed
      if (speed == null) speed = "slow";

      // minimizing?
      if ($("#welcome").is(':visible')) {
        // hide the big box
        if (speed == "fast") {
          $('#welcome').toggle();
        } else {
          $('#welcome').slideUp();
        }
        // do we have words to say?
        var welcomeText = $("#welcome-content.current").text();
        if (welcomeText.length > 0) {
          $("#ctxHelpDiv.welcome").slideDown(speed, function() {
            // ...display a notification with an appropriate text
            if (welcomeText.length > 150) {
              // ... substr
              $("#ctxHelpTxt.welcome").html(welcomeText.substring(0, 150) + '&hellip;');
            } else {
              $("#ctxHelpTxt.welcome").html(welcomeText);
            }
            });
        }
        // set the cookie
        jQuery.setCookie("welcome-visibility", "minimized", 365);
      } else {
        $("#ctxHelpDiv.welcome").slideUp(function() {
          $("#welcome").slideDown(speed);
          // set the cookie
          jQuery.setCookie("welcome-visibility", "maximized", 365);
        });
      }
    }
  </script>
  <div id="welcome" class="span-12 last wide-blue">
    <a class="close" href="#" title="Close" onclick="toggleWelcome();return false;">&nbsp;</a>
        <div class="top"></div>
        <div class="center span-12 last">
          <div class="bochs" id="bochs-1">
              <div id="thumb" class="span-4">
                  <img src="themes/metabolic/thumbs/thumb-image.png" alt="metabolicMine interface" />
              </div>
              <div id="welcome-content" class="span-8 last current">
              <h2>First time here?</h2>
              <p>Welcome to <strong>metabolicMine</strong>, an integrated web resource of Data &amp; Tools to support the Metabolic
              Disease research community.</p>

              <p>If you are short of time, just navigate through our set of <a class="nice" href="#" onclick="switchBochs(2);return false;">Feature Hints</a>. For a basic overview of
              the site and its features try the <a class="nice" href="<c:url value="http://www.metabolicmine.org/tour/start.html" />"
                  onclick="javascript:window.open('<c:url value="http://www.metabolicmine.org/tour/start.html" />','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=1000,height=800');return false">Quick Tour</a>, it takes about ten minutes.</p>
              <a class="button blue" href="<c:url value="http://www.metabolicmine.org/tour/start.html" />"
                  onclick="javascript:window.open('<c:url value="http://www.metabolicmine.org/tour/start.html" />','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=1000,height=800');return false">
                    <div><span>Take a tour</span></div>
              </a>
             </div>
            </div>

            <div class="bochs" id="bochs-2" style="display: none;">
              <div id="thumb" class="span-4">
              <a title="Try Search" href="/${WEB_PROPERTIES['webapp.path']}/keywordSearchResults.do?searchBag="><img
                src="themes/metabolic/thumbs/feature-search.jpg"
                alt="metabolicMine Search" /></a></div>
              <div id="welcome-content" class="span-8 last">
                <h2>Search</h2>
                <p>Our search engine operates across many data fields giving you the
                highest chance of getting a result. Just type your search words in the
                box.</p>
                <p>You can search by:</p>
                <ul>
                  <li>Identifiers (eg. Gene symbols, accession codes, SNP identifiers, PubMed IDs etc.)</li>
                  <li>Keywords (eg. Diabetes)</li>
                  <li>Authors (eg. Sanger F)</li>
                </ul>
                <p>Search supports AND, OR, NOT and wildcard*. You can access Search from
                the home page or use QuickSearch, located top right on every page.</p>
                <a class="button gray" href="#" onclick="switchBochs(3);return false;"><div><span>Next Hint: Facets</span></div></a>
              </div>
            </div>

            <div class="bochs" id="bochs-3" style="display: none;">
              <div id="thumb" class="span-4">
              <a title="Try Faceted Search" href="/${WEB_PROPERTIES['webapp.path']}/keywordSearchResults.do?searchBag="><img
                src="themes/metabolic/thumbs/feature-facets.jpg"
                alt="metabolicMine Facets" /></a></div>
              <div id="welcome-content" class="span-8 last">
                <h2>Facets</h2>
                <p><strong>Facets</strong> show you the different places where your search words were found (eg. within Gene, Protein, Go Term, Template, Publication etc).
                You can use the facets to filter for the type of results that are most important to you. When you've filtered by facets, you can even save the results
                straight to a List.</p>
                <a class="button gray" href="#" onclick="switchBochs(4);return false;"><div><span>Next Hint: Lists</span></div></a>
              </div>
            </div>

            <div class="bochs" id="bochs-4" style="display: none;">
              <div id="thumb" class="span-4">
              <a title="Try Lists" href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view"><img
                src="themes/metabolic/thumbs/feature-lists.jpg"
                alt="metabolicMine Lists" /></a></div>
              <div id="welcome-content" class="span-8 last">
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
                <a class="button gray" href="#" onclick="switchBochs(5);return false;"><div><span>Next Hint: Templates</span></div></a>
            </div>
           </div>

            <div class="bochs" id="bochs-5" style="display: none;">
              <div id="thumb" class="span-4">
              <a title="Try Templates" href="/${WEB_PROPERTIES['webapp.path']}/templates.do"><img
                src="themes/metabolic/thumbs/feature-templates.jpg"
                alt="metabolicMine Templates" /></a></div>
              <div id="welcome-content" class="span-8 last">
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
                <a class="button gray" href="#" onclick="switchBochs(6);return false;"><div><span>Next Hint: MyMine</span></div></a>
            </div>
           </div>

            <div class="bochs" id="bochs-6" style="display: none;">
              <div id="thumb" class="span-4">
              <a title="Try MyMine" href="/${WEB_PROPERTIES['webapp.path']}/mymine.do"><img
                src="themes/metabolic/thumbs/feature-mymine.jpg"
                alt="metabolicMine MyMine" /></a></div>
              <div id="welcome-content" class="span-8 last">
                <h2>MyMine</h2>
                <p><strong>MyMine</strong> is your <u>personal space</u> on metabolicMine. Creating an account is easy. Just provide an e-mail and a password. You're ready to go.</p>
                <p>Your account allows you to:</p>
                <ul>
                  <li>Save Queries and Lists</li>
                  <li> Modify and save Templates for later use</li>
                  <li>Mark Public Templates as favourites so they're easier to find</li>
                </ul>
                <p>You can access mMyMine from the Tab bar, located at the top of every page.</p>
                <p>Note: Your data and e-mail address are confidential and we won't send you unsolicited mail.</p>
                <a class="button gray" href="#" onclick="switchBochs(7);return false;"><div><span>Next Hint: QueryBuilder</span></div></a>
            </div>
           </div>

            <div class="bochs" id="bochs-7" style="display: none;">
              <div id="thumb" class="span-4">
              <a title="Try QueryBuilder" href="/${WEB_PROPERTIES['webapp.path']}/customQuery.do"><img
                src="themes/metabolic/thumbs/feature-querybuilder.jpg"
                alt="metabolicMine QueryBuilder" /></a></div>
              <div id="welcome-content" class="span-8 last">
                <h2>QueryBuilder</h2>
                <p><strong>QueryBuilder (QB)</strong> is the Powerhouse of metabolicMine.</p>
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

        <div class="span-12 last">
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
        <div class="bottom span-12 last"></div>
    </div>

    <script type="text/javascript">
    // are we showing a minimized welcome box?
    if (jQuery.getCookie("welcome-visibility") == "minimized") toggleWelcome("fast");

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
  </script>

    <div id="actions" class="span-12 last wide-gray">

      <form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
          <!--
          <a class="more" title="Want to see more?"
             onclick="javascript:jQuery('#more-actions').slideToggle(); return false;">&nbsp;</a>
          -->
          <div class="top"></div>
          <div class="center span-12 last">
              <div class="span-4 search">
                <script type="text/javascript">
                  /* pre-fill search input with a term */
                  function preFillInput(term) {
                    var e = $("input#actionsInput");
                    e.val(term);
              if (e.hasClass(inputToggleClass)) e.toggleClass(inputToggleClass);
              e.focus();
                  }
                </script>
                <div class="image">
                    <img src="images/icons/search-64.png" alt="Search" />
                  </div>
                  <h3><a href="/${WEB_PROPERTIES['webapp.path']}/keywordSearchResults.do?searchBag=">Search</a></h3>
                  <div style="clear:both;"> </div>
                  <p>Enter a gene, protein, SNP or other identifier [eg.
                  <a onclick="preFillInput('PPARG');return false;" title="Search for PPARG"
                    href="keywordSearchResults.do?searchTerm=PPARG"><strong>PPARG</strong></a>,
                  <a onclick="preFillInput('Insulin');return false;" title="Search for Insulin"
                    href="keywordSearchResults.do?searchTerm=Insulin"><strong>Insulin</strong></a>,
                  <a onclick="preFillInput('rs876498');return false;" title="Search for rs876498"
                    href="keywordSearchResults.do?searchTerm=rs876498"><strong>rs876498</strong></a>].
                  <br />Alternatively, search for disease, keywords or publications [eg.
                  <a onclick="preFillInput('Diabetes');return false;" title="Search for Diabetes"
                    href="keywordSearchResults.do?searchTerm=Diabetes"><strong>Diabetes</strong></a>,
                  <a onclick="preFillInput('GWAS');return false;" title="Search for GWAS"
                    href="keywordSearchResults.do?searchTerm=GWAS"><strong>GWAS</strong></a>,
                  <a onclick="preFillInput('13658959');return false;" title="Search for PMID"
                    href="keywordSearchResults.do?searchTerm=13658959"><strong>PMID</strong></a>,
                 <a onclick="preFillInput('Sanger F');return false;" title="Search for Author"
                    href="keywordSearchResults.do?searchTerm=Sanger+F"><strong>Author</strong></a>
                  ]</p>
                  <p>[Supports AND, OR, NOT and wildcard*]</p>
            <div class="input">
              <input id="actionsInput" class="input" type="text" name="searchTerm" value="e.g. PPARG, Insulin, rs876498" />
            </div>
              </div>
              <div class="span-4 lists">
                <div class="image">
                    <img src="images/icons/lists-64.png" alt="Lists" />
                  </div>
                  <h3><a href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view">Analyse Lists of Data</a></h3>
                  <div style="clear:both;"> </div>
          <p>
          <img src="themes/metabolic/thumbs/widget-charts-5.png" alt="widget charts" style="float:right;padding-left:5px;margin-right:4px;" />
          <strong>Explore</strong> and <strong>Analyse</strong>. Upload your own data or browse our Public
          sets. Covering Pathways to Publications, discover hidden relationships with our analysis widgets.</p>
              </div>
              <div class="span-4 last templates">
                  <div class="image">
                    <img src="images/icons/templates-64.png" alt="Templates" />
                  </div>
                  <h3><a href="/${WEB_PROPERTIES['webapp.path']}/templates.do">Use Template Queries</a></h3>
                  <div style="clear:both;"> </div>
                  <p>Get started with <strong>powerful queries</strong> using our predefined searches. These customizable templates have been
                  designed around common tasks performed by our biologist community.</p>
                  <p>To see how they work, why not try a template from our <strong>examples page</strong>?</p>
              </div>
              <div class="span-12 last">
                  <div class="span-4 search">
                      <input id="mainSearchButton" class="button orange-gray" type="submit" value="Search" />
                  </div>
                  <div class="span-4 lists">
                      <a href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view" class="button green">
                        <div><span>Lists</span></div>
                      </a>
                  </div>
                  <div class="span-4 last templates">
                      <a href="/${WEB_PROPERTIES['webapp.path']}/templates.do" class="button violet">
                        <div><span>Templates</span></div>
                      </a>
                  </div>
              </div>
          </div>
          <div class="bottom"></div>
        </form>
    </div>

    <div id="api" class="span-6 white-half">
        <div class="top"></div>
        <div class="center span-6 last">
            <h4>Perl<span>&nbsp;&amp;&nbsp;</span>Java API</h4>
            <img src="themes/metabolic/icons/perl-java-ico.gif" alt="perl java" />
            <p>You can fetch data from metabolicMine directly from your own programs via a REST web service.  More information:</p>
            <ul>
                <li><a href="/${WEB_PROPERTIES['webapp.path']}/api.do?subtab=perl">Perl API</a></li>
                <li><a href="/${WEB_PROPERTIES['webapp.path']}/api.do?subtab=java">Java API</a></li>
            </ul>
        </div>
        <div class="bottom span-6 last"></div>
    </div>

    <div id="rss" class="span-6 last white-half" style="display:none;">
      <script type="text/javascript">
      // feed URL
      var feedURL = "${WEB_PROPERTIES['project.rss']}";
      // limit number of entries displayed
      var maxEntries = 2;
      // where are we appending entries? (jQuery syntax)
      var target = 'table#articles';

      var months = new Array(12); months[0]="Jan"; months[1]="Feb"; months[2]="Mar"; months[3]="Apr"; months[4]="May"; months[5]="Jun";
      months[6]="Jul"; months[7]="Aug"; months[8]="Sep"; months[9]="Oct"; months[10]="Nov"; months[11]="Dec";

      $(document).ready(function () {
          // DWR fetch, see AjaxServices.java
          AjaxServices.getNewsPreview(feedURL, function (data) {
              if (data) {
                  // show us
                  $('#rss').slideToggle('slow');

                  // declare
                  var feedTitle, feedDescription, feedDate, feedLink, row, feed;

                  // convert to XML, jQuery manky...
                  try {
                      // Internet Explorer
                      feed = new ActiveXObject("Microsoft.XMLDOM");
                      feed.async = "false";
                      feed.loadXML(data);
                  } catch (e) {
                      try {
                          // ...the good browsers
                          feed = new DOMParser().parseFromString(data, "text/xml");
                      } catch (e) {
                          // ... BFF
                          alert(e.message);
                          return;
                      }
                  }

                  var items = feed.getElementsByTagName("item"); // ATOM!!!
                  for (var i = 0; i < items.length; i++) {
                      // early bath
                      if (i == maxEntries) return;

                      feedTitle = trimmer(items[i].getElementsByTagName("title")[0].firstChild.nodeValue, 70);
                      feedDescription = trimmer(items[i].getElementsByTagName("description")[0].firstChild.nodeValue, 70);
                      // we have a feed date
                      if (items[i].getElementsByTagName("pubDate")[0]) {
                          feedDate = new Date(items[i].getElementsByTagName("pubDate")[0].firstChild.nodeValue);
                          feedLink = items[i].getElementsByTagName("link")[0].firstChild.nodeValue

                          // build table row
                          row = '<tr>' + '<td class="date">' + '<a target="new" href="' + feedLink + '">' + feedDate.getDate()
                          + '<br /><span>' + months[feedDate.getMonth()] + '</span></a></td>'
                          + '<td><a target="new" href="' + feedLink + '">' + feedTitle + '</a><br/>' + feedDescription + '</td>'
                          + '</tr>';
                      } else {
                          feedLink = items[i].getElementsByTagName("link")[0].firstChild.nodeValue

                          // build table row
                          row = '<tr>'
                          + '<td><a target="new" href="' + feedLink + '">' + feedTitle + '</a><br/>' + feedDescription + '</td>'
                          + '</tr>';
                      }

                      // append, done
                      $(target).append(row);
                  }
              }
          });
      });

          // trim text to a specified length
      function trimmer(grass, length) {
        if (!grass) return;
        grass = stripHTML(grass);
        if (grass.length > length) return grass.substring(0, length) + '...';
        return grass;
      }

      // strip HTML
          function stripHTML(html) {
             var tmp = document.createElement("DIV"); tmp.innerHTML = html; return tmp.textContent || tmp.innerText;
          }
      </script>
        <div class="top"></div>
        <div class="center span-6 last">
            <h4>News<span>&nbsp;&amp;&nbsp;</span>Updates</h4>
            <table id="articles">
              <!-- append babies here -->
            </table>
        </div>
        <div class="more span-6 last"><a target="new" href="http://blog.metabolicmine.org/" class="more">&nbsp;</a></div>
    </div>

  <!--
    <div id="testimonials" class="span-4 last blue">
        <div class="top"></div>
        <div class="center span-4 last">
            <h4>User Feedback</h4>
            <img src="/metabolicmine/themes/metabolic/img/stockphoto.jpg" alt="Stock Photo" />
            <p>It's not how fat you are, it's what you do with it that counts, and that is why we feel that
                metabolicMine is a valid addition to the data mining lorem.</p>
            <q>- E. Novak-Brown, University of Cambridge</q>
        </div>
        <div class="bottom span-4 last"></div>
    </div>
    -->

    <div id="footer" class="span-12 last">
        <div class="span-6">
            <a href="#" onclick="showContactForm();return false;">Contact Us</a>
            <span>|</span>
            <a href="http://blog.metabolicmine.org/faq">FAQ</a>
            <span>|</span>
            <a href="http://blog.metabolicmine.org/about">About</a>
      <span>|</span>
            <!-- <a href="#">Cite</a>
      <span>|</span> -->
            <br />
            <a href="http://www.intermine.org">InterMine</a>
            <span>|</span>
            <a href="http://www.flymine.org">FlyMine</a>
            <span>|</span>
            <a href="http://www.modmine.org">modMine</a>
            <span>|</span>
            <a href="http://ratmine.mcw.edu/ratmine">RatMine</a>

            <p>&copy; 2010 Department of Genetics, University of Cambridge, Downing Street, Cambridge CB2 3EH, United Kingdom</p>
        </div>
        <div class="span-6 last">
            <a href="http://wellcome.ac.uk/" title="Wellcome Trust">
              <img src="themes/metabolic/icons/wellcome-ico.png" alt="Wellcome Trust" />
             </a>
             <!--
             <a href="http://www.gen.cam.ac.uk/" title="Department of Genetics">
               <img src="/metabolicmine/themes/metabolic/icons/genetics-ico.gif" alt="Department of Genetics" />
             </a>
             -->
             <a href="http://www.cam.ac.uk/" title="University of Cambridge">
              <img src="themes/metabolic/icons/cam-text-ico.gif" alt="University of Cambridge" />
            </a>
        </div>
    </div>
 </div>

 <script type="text/javascript">
   // e.g. values only available when JavaScript is on
  $('input#actionsInput').toggleClass(inputToggleClass);

  // register input elements with blur & focus
  $('input#actionsInput').blur(function() {
    if ($(this).val() == '') {
      $(this).toggleClass(inputToggleClass);
      $(this).val(placeholder);
    }
  });
  $('input#actionsInput').focus(function() {
    if ($(this).hasClass(inputToggleClass)) {
      $(this).toggleClass(inputToggleClass);
      $(this).val('');
    }
  });

  // associate functions with search that redir to a keyword objects listing instead of search results
  $('#mainSearchButton').click(function() {
    // if placeholder text in place, take us elsewhere
    if ($("#actionsInput").val() == placeholder) {
      $(location).attr('href', "/${WEB_PROPERTIES['webapp.path']}/keywordSearchResults.do?searchBag=");
      return false;
    }
  });

</script>