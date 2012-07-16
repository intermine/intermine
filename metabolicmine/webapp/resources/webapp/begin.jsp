<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- newBegin.jsp -->
<html:xhtml/>

<div id="corner">&nbsp;</div>

<!-- preview div -->
<div id="ctxHelpDiv" class="preview">
  <div class="topBar info">
<!--    <div id="ctxHelpTxt" class="preview">You are looking at our latest preview site. Enjoy and <a href="#" onclick="showContactForm();return false;">
    let us know</a> if events do not turn out as expected. Thank you!</div> -->
    <div id="ctxHelpTxt" class="preview">Unfortunately, we are experiencing technical difficulties with one of our servers. Please bear with us as we work to fix these issues.</div>
  </div>
</div>

<!-- faux context help -->
<div id="ctxHelpDiv" class="welcome" style="display:none;">
  <div class="topBar info">
    <div id="ctxHelpTxt" class="welcome"></div>
    <a href="#" onclick="toggleWelcome();return false">Show more</a>
  </div>
</div>

<div id="content-wrap">

  <script type="text/javascript">

  // specify what happens to element in a small browser window (better add class than modify from here)
  if (jQuery(window).width() < '1205') {
    // cite etc
    jQuery('ul#topnav').addClass('smallScreen');
    // corners
    jQuery('#corner').addClass('smallScreen');
    if (jQuery(window).width() < '1125') {
      jQuery('#help').addClass('smallScreen');
    }
  }

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

    // minimize big welcome box into an info message
    function toggleWelcome(speed) {
      // default speed
      if (speed == null) speed = "slow";

      // minimizing?
      if (jQuery("#welcome").is(':visible')) {
        // hide the big box
        if (speed == "fast") {
          jQuery('#welcome').toggle();
        } else {
          jQuery('#welcome').slideUp();
        }
        // do we have words to say?
        var welcomeText = jQuery("#welcome-content.current").text();
        if (welcomeText.length > 0) {
          jQuery("#ctxHelpDiv.welcome").slideDown(speed, function() {
            // ...display a notification with an appropriate text
            if (welcomeText.length > 150) {
              // ... substr
              jQuery("#ctxHelpTxt.welcome").html(welcomeText.substring(0, 150) + '&hellip;');
            } else {
              jQuery("#ctxHelpTxt.welcome").html(welcomeText);
            }
          });
        }
        // set the cookie
        jQuery.setCookie("welcome-visibility", "minimized", 365);
      } else {
        jQuery("#ctxHelpDiv.welcome").slideUp(function() {
          jQuery("#welcome").slideDown(speed);
          // set the cookie
          jQuery.setCookie("welcome-visibility", "maximized", 365);
        });
      }
    }
  </script>
  <div id="welcome">
        <div class="center">
          <a class="close" title="Close" onclick="toggleWelcome();">Close</a>
          <div class="bochs" id="bochs-1">
              <div id="thumb">
                  <img src="themes/metabolic/thumbs/thumb-image.png" alt="metabolicMine interface" />
              </div>
              <div id="welcome-content" class="current">
              <h2>First time here?</h2>
              <p>Welcome to <strong>metabolicMine</strong>, an integrated web resource of Data &amp; Tools to support the Metabolic
              Disease research community.</p>

              <p>If you are short of time, just navigate through our set of <a class="nice" href="#" onclick="switchBochs(2);return false;">Feature Hints</a>. For a basic overview of
              the site and its features try the <a class="nice" href="<c:url value="http://www.metabolicmine.org/tour/start.html" />"
                  onclick="javascript:window.open('<c:url value="http://www.metabolicmine.org/tour/start.html" />','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=1000,height=800');return false">Quick Tour</a>, it takes about ten minutes.</p>

              <br />
              <a class="button blue" href="<c:url value="http://www.metabolicmine.org/tour/start.html" />"
                  onclick="javascript:window.open('<c:url value="http://www.metabolicmine.org/tour/start.html" />','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=1000,height=800');return false">
                    <div><span>Take a tour</span></div>
              </a>
             </div>
            </div>

            <div class="bochs" id="bochs-2" style="display: none;">
              <div id="thumb">
              <a title="Try Search" href="/${WEB_PROPERTIES['webapp.path']}/keywordSearchResults.do?searchBag="><img
                src="themes/metabolic/thumbs/feature-search.jpg"
                alt="metabolicMine Search" /></a></div>
              <div id="welcome-content">
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
                <br />
                <a class="button gray" onclick="switchBochs(3);"><div><span>Next Hint: Facets</span></div></a>
              </div>
            </div>

            <div class="bochs" id="bochs-3" style="display: none;">
              <div id="thumb">
              <a title="Try Faceted Search" href="/${WEB_PROPERTIES['webapp.path']}/keywordSearchResults.do?searchBag="><img
                src="themes/metabolic/thumbs/feature-facets.jpg"
                alt="metabolicMine Facets" /></a></div>
              <div id="welcome-content">
                <h2>Facets</h2>
                <p><strong>Facets</strong> show you the different places where your search words were found (eg. within Gene, Protein, Go Term, Template, Publication etc).
                You can use the facets to filter for the type of results that are most important to you. When you've filtered by facets, you can even save the results
                straight to a List.</p>
                <br />
                <a class="button gray" onclick="switchBochs(4);"><div><span>Next Hint: Lists</span></div></a>
              </div>
            </div>

            <div class="bochs" id="bochs-4" style="display: none;">
              <div id="thumb">
              <a title="Try Lists" href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view"><img
                src="themes/metabolic/thumbs/feature-lists.jpg"
                alt="metabolicMine Lists" /></a></div>
              <div id="welcome-content">
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
                <br />
                <a class="button gray" onclick="switchBochs(5);"><div><span>Next Hint: Templates</span></div></a>
            </div>
           </div>

            <div class="bochs" id="bochs-5" style="display: none;">
              <div id="thumb">
              <a title="Try Templates" href="/${WEB_PROPERTIES['webapp.path']}/templates.do"><img
                src="themes/metabolic/thumbs/feature-templates.jpg"
                alt="metabolicMine Templates" /></a></div>
              <div id="welcome-content">
                <h2>Templates</h2>
                <p>Our predefined <strong>template searches</strong> are designed around the common tasks performed by our Biologist Community.
                Templates provide you with a simple form that lets you define your starting point and optional filters to help focus your search.</p>
                <p>Templates cover common questions like:</p>
                <ul>
                    <li>I have a List of SNPs - do any of them affect Genes?</li>
                    <li>This Gene came up in my results - what can I find out about it?</li>
                    <li>I'm interested in this chromosome region - what's in there that could be linked with this disease?</li>
                </ul>
                <p>You can work with Templates from the Home page or select Templates from the Tab bar, located at the top of every page.</p>
                <br />
                <a class="button gray" onclick="switchBochs(6);"><div><span>Next Hint: MyMine</span></div></a>
            </div>
           </div>

            <div class="bochs" id="bochs-6" style="display: none;">
              <div id="thumb">
              <a title="Try MyMine" href="/${WEB_PROPERTIES['webapp.path']}/mymine.do"><img
                src="themes/metabolic/thumbs/feature-mymine.jpg"
                alt="metabolicMine MyMine" /></a></div>
              <div id="welcome-content">
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
                <br />
                <a class="button gray" onclick="switchBochs(7);"><div><span>Next Hint: QueryBuilder</span></div></a>
            </div>
           </div>

            <div class="bochs" id="bochs-7" style="display: none;">
              <div id="thumb">
              <a title="Try QueryBuilder" href="/${WEB_PROPERTIES['webapp.path']}/customQuery.do"><img
                src="themes/metabolic/thumbs/feature-querybuilder.jpg"
                alt="metabolicMine QueryBuilder" /></a></div>
              <div id="welcome-content">
                <h2>QueryBuilder</h2>
                <p><strong>QueryBuilder (QB)</strong> is the Powerhouse of metabolicMine.</p>
                <p>Its advanced interface lets you:</p>
                <ul>
                  <li>Construct your own custom searches
                  <li>Modify your previous searches
                  <li>You can even edit our predefined Templates.
                </ul>
                <p>The easiest way to get started with QB is by editing one of our pre-existing Template searches.
                Follow the simple tutorial in the QueryBuilder section of the <strong>Tour</strong> to see how to change a Template output or add a filter.</p>
                <p>You can access QueryBuilder from the Tab bar, located at the top of every page.</p>
                <br/>
                <br/>
            </div>
           </div>

           <div style="clear:both;"></div>

              <ul id="switcher">
                <li id="switcher-1" class="switcher current"><a onclick="switchBochs(1);">Start</a></li>
                <li id="switcher-2" class="switcher"><a onclick="switchBochs(2);">1</a></li>
                <li id="switcher-3" class="switcher"><a onclick="switchBochs(3);">2</a></li>
                <li id="switcher-4" class="switcher"><a onclick="switchBochs(4);">3</a></li>
                <li id="switcher-5" class="switcher"><a onclick="switchBochs(5);">4</a></li>
                <li id="switcher-6" class="switcher"><a onclick="switchBochs(6);">5</a></li>
                <li id="switcher-7" class="switcher"><a onclick="switchBochs(7);">6</a></li>
              </ul>
        </div>
    </div>

    <script type="text/javascript">
    // are we showing a minimized welcome box?
    if (jQuery.getCookie("welcome-visibility") == "minimized") toggleWelcome("fast");

    /* hide switcher of we are on first time here */
    if (jQuery("#switcher-1").hasClass('current')) {
      jQuery("#switcher").hide();
    }

    /* div switcher for welcome bochs using jQuery */
    function switchBochs(newDivId) {
      // no current
      jQuery(".switcher").each (function() { jQuery(this).removeClass('current'); });
      // apply current
      jQuery('#switcher-'+newDivId).addClass('current');
      // hide them all bochs
      jQuery(".bochs").each (function() { jQuery(this).hide(); });
      // then show our baby
      jQuery('#bochs-'+newDivId).fadeIn();

      // apply active class
      jQuery("#welcome-content").each (function() { jQuery(this).removeClass('current'); });
      jQuery('#bochs-'+newDivId+' > #welcome-content').addClass('current');

      // show/hide switcher?
      if (jQuery("#switcher-1").hasClass('current')) {
        jQuery("#switcher").hide();
      } else {
        jQuery("#switcher").show();
      }
    }
  </script>

    <div id="boxes">
        <div id="search-bochs">
            <img class="title" src="themes/purple/homepage/search-ico-right.png" title="search"/>
            <div class="inner">
                <h3>Search</h3>
                <span class="ugly-hack">&nbsp;</span>

                <script type="text/javascript">
                  /* pre-fill search input with a term */
                  function preFillInput(term) {
                    var e = jQuery("input#actionsInput");
                    e.val(term);
                    if (e.hasClass(inputToggleClass)) e.toggleClass(inputToggleClass);
                    e.focus();
                  }
                </script>

                <p>Enter a gene, protein, SNP or other identifier [eg.
                <a onclick="preFillInput('PPARG');return false;" title="Search for PPARG"><strong>PPARG</strong></a>,
                <a onclick="preFillInput('Insulin');return false;" title="Search for Insulin"><strong>Insulin</strong></a>,
                <a onclick="preFillInput('rs876498');return false;" title="Search for rs876498"><strong>rs876498</strong></a>].
                <br />Alternatively, search for disease, keywords or publications [eg.
                <a onclick="preFillInput('Diabetes');return false;" title="Search for Diabetes"><strong>Diabetes</strong></a>,
                <a onclick="preFillInput('GWAS');return false;" title="Search for GWAS"><strong>GWAS</strong></a>,
                <a onclick="preFillInput('13658959');return false;" title="Search for PMID"><strong>PMID</strong></a>,
                <a onclick="preFillInput('Sanger F');return false;" title="Search for Author"><strong>Author</strong></a>]</p>

                <form id="mainSearchForm" action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
                    <div class="input"><input id="actionsInput" name="searchTerm" class="input" type="text" value="${WEB_PROPERTIES['begin.searchBox.example']}"></div>
                    <div class="bottom">
                        <center>
                            <a class="button orange">
                              <div><span>Search</span></div>
                            </a>
                        </center>
                    </div>
               </form>
               
				<script type="text/javascript">
				(function() {
				    var index = function(value) {
				        switch (value) {
				          case "${ids}":
				          case "${WEB_PROPERTIES['begin.searchBox.example']}":
				          case "":
				            // if placeholder text or no text in place, take us to the index
				            jQuery(location).attr('href', "/${WEB_PROPERTIES['webapp.path']}/keywordSearchResults.do?searchBag=");
				            return false;
				        }
				        return true;
				    }
					
					var button = jQuery('#mainSearchForm a'),
					    input  = jQuery("input#actionsInput");
					
					button.click(function(e){
			        	if( index(input.val()) ) {
			        		document.getElementById("mainSearchForm").submit();
			        	}
					});
				    input.keypress(function(e){
				        if(e.which == 13){
				        	if( index(input.val()) ) {
				        		document.getElementById("mainSearchForm").submit();
				        	}
				        }
				      });
				})()
				</script>

                <div style="clear:both;"></div>
            </div>
        </div>
        <div id="lists-bochs">
            <img class="title" src="images/icons/lists-64.png" title="lists"/>
            <div class="inner">
                <h3>Analyse Lists of Data</h3>
                <div class="right">
                  <p>
                  <img style="float: right; padding-left: 5px; margin-right: 4px;" alt="widget charts" src="themes/metabolic/thumbs/widget-charts-5.png">
                  <strong>Explore</strong> and <strong>Analyse</strong>. Upload your own data or browse our Public sets.
                  Covering Pathways to Publications, discover hidden relationships with our analysis widgets.</p>
                </div>

                <div class="left">
                  <span class="ugly-hack">&nbsp;</span>
                  <form name="buildBagForm" method="post" action="<c:url value="/buildBag.do" />">
                      <select name="type">
                        <option value="Gene">Gene</option>
                        <option value="Protein">Protein</option>
                        <option value="SNP">SNP</option>
                      </select>
                      <select name="extraFieldValue">
                        <option value="H. sapiens">H. sapiens</option>
                        <option value="M. musculus">M. musculus</option>
                        <option value="R. norvegicus">R. norvegicus</option>
                      </select>
                      <div class="textarea">
                        <textarea autocomplete="off" id="listInput" name="text">e.g. <c:out value="${WEB_PROPERTIES['bag.example.identifiers']}" /></textarea>
                      </div>
                  </form>
                </div>
                <div style="clear:both;"></div>

                <div class="bottom arrowtip">
                  <a class="advanced" class="adv" href="bag.do?subtab=upload">file upload</a>
                  <a class="button green">
                    <div><span>Analyse list</span></div>
                  </a>
                  <script type="text/javascript">
                    jQuery("#lists-bochs textarea").click(function() {
                      jQuery("#lists-bochs div.bottom").removeClass('arrowtip');
                    });
                    jQuery('#lists-bochs a.button').click(function() {
                      jQuery("textarea#listInput").val(jQuery("textarea#listInput").val().replace("e.g.", "").replace(/^\s+|\s+$/g, ""));
                      document.buildBagForm.submit();
                    });
                  </script>
                </div>
            </div>
        </div>
    </div>

    <div style="clear:both"></div>

    <div id="bottom-wrap">
      <div id="templates-bochs">
        <img class="title" src="images/icons/templates-64.png" title="templates"/>
          <div class="inner">
            <h3>Use Template Searches</h3>
            <p><span class="ugly-hack"></span>Get started with <strong>powerful searches</strong> using our predefined Templates. These
              customizable templates have been designed around common tasks performed by our biologist community.</p>
          </div>

          <c:if test="${!empty tabs}">
            <div id="templates">
                <table id="menu" border="0" cellspacing="0">
                    <tr>
                      <!-- templates tabs -->
                      <c:forEach var="item" items="${tabs}">
                        <td><div class="container"><span id="tab${item.key}">
                          <c:forEach var="row" items="${item.value}">
                            <c:choose>
                              <c:when test="${row.key == 'name'}">
                                <c:out value="${row.value}" />
                              </c:when>
                            </c:choose>
                          </c:forEach>
                        </span></div></td>
                      </c:forEach>
                    </tr>
                </table>

                <div id="tab-content">
                    <div id="ribbon"></div>

                    <!-- templates content -->
                    <c:forEach var="item" items="${tabs}">
                      <div id="content${item.key}" class="content" style="display:none;">
                        <c:forEach var="row" items="${item.value}">
                          <c:choose>
                            <c:when test="${row.key == 'identifier'}">
                              <c:set var="aspectTitle" value="${row.value}"/>
                            </c:when>
                            <c:when test="${row.key == 'description'}">
                              <p><c:out value="${row.value}" />&nbsp;<a href="dataCategories.do">Read more</a></p><br/>
                            </c:when>
                            <c:when test="${row.key == 'name'}">
                              <p>Search for <c:out value="${row.value}" />:</p>
                            </c:when>
                            <c:when test="${row.key == 'templates'}">
                              <table>
                                <c:forEach var="template" items="${row.value}" varStatus="status">
                                  <c:if test="${status.count %2 == 1}">
                                    <c:choose>
                                      <c:when test="${status.first}">
                                        <tr>
                                      </c:when>
                                      <c:otherwise>
                                        </tr><tr>
                                      </c:otherwise>
                                    </c:choose>
                                  </c:if>
                                  <td>
                                    <a href="template.do?name=${template.name}&scope=global"><c:out value="${fn:replace(template.title,'-->','&nbsp;<img src=\"themes/metabolic/homepage/arrow-green-ico.png\" style=\"vertical-align:bottom\">&nbsp;')}" escapeXml="false" /></a>
                                  </td>
                                </c:forEach>
                              </table>
                              <p class="more"><a href="templates.do?filter=${aspectTitle}">More queries</a></p>
                            </c:when>
                          </c:choose>
                        </c:forEach>
                      </div>
                    </c:forEach>
                </div>
            </div>
          </c:if>
      </div>
      <div style="clear: both;"></div>

        <div id="low">
            <div id="rss" style="display:none;">
                <h4>News<span>&nbsp;&amp;&nbsp;</span>Updates</h4>
                <table id="articles"></table>
                <c:if test="${!empty WEB_PROPERTIES['links.blog']}">
                  <p class="more"><a target="new" href="${WEB_PROPERTIES['links.blog']}">More news</a></p>
                </c:if>
            </div>

            <div id="api">
                <h4>Perl, Python<span>&nbsp;&amp;&nbsp;</span>Java API</h4>
                <img src="images/begin/perl-java-python-ico.png" alt="perl java python" />
                <p>
                    You can fetch data from <c:out value="${WEB_PROPERTIES['project.title']}"/>
                    directly from your own programs via a REST web service. More information:
                </p>
                <ul>
                    <li><a href="<c:out value="${WEB_PROPERTIES['path']}" />api.do?subtab=perl">Perl</a>
                    <li><a href="<c:out value="${WEB_PROPERTIES['path']}" />api.do?subtab=python">Python</a>
                    <li><a href="<c:out value="${WEB_PROPERTIES['path']}" />api.do?subtab=java">Java</a>
                </ul>
            </div>

            <div style="clear:both;"></div>
        </div>
    </div>
</div>

<div id="footer">
    <div class="column">
        <a href="#" onclick="showContactForm();return false;">Contact Us</a>
        <span>|</span>
        <a href="http://blog.metabolicmine.org/faq">FAQ</a>
        <span>|</span>
        <a href="http://blog.metabolicmine.org/about">About</a>
        <span>|</span>
        <a href="http://intermine.org/wiki/PrivacyPolicy">Privacy Policy</a>
        <br />
        <a href="http://www.intermine.org">InterMine</a>
        <span>|</span>
        <a href="http://www.flymine.org">FlyMine</a>
        <span>|</span>
        <a href="http://www.modmine.org">modMine</a>
        <span>|</span>
        <a href="http://ratmine.mcw.edu/ratmine">RatMine</a>
        <span>|</span>
        <a href="http://yeastmine.yeastgenome.org/yeastmine">YeastMine</a>

        <p>&copy; 2012 Department of Genetics, University of Cambridge, Downing Street, Cambridge CB2 3EH, United Kingdom</p>
    </div>
    <div class="column last">
         <a href="http://www.cam.ac.uk/" title="University of Cambridge">
          <img src="themes/metabolic/icons/cam-text-ico.gif" alt="University of Cambridge" />
        </a>
    </div>

    <div style="clear:both;"></div>
</div>

<script type="text/javascript">

    // 'open up' the first tab
    jQuery("table#menu td:first").addClass("active").find("div").append('<span class="right"></span><span class="left"></span>').show();
    jQuery("div.content:first").show();

    // onclick behavior for tabs
    jQuery("table#menu td").click(function() {
        jQuery("table#menu td.active").find("div").find('.left').remove();
        jQuery("table#menu td.active").find("div").find('.right').remove();
        jQuery("table#menu td").removeClass("active");

        jQuery(this).addClass("active").find("div").append('<span class="right"></span><span class="left"></span>');
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

    // feed URL
    var feedURL = "${WEB_PROPERTIES['project.rss']}";
    // limit number of entries displayed
    var maxEntries = 2;
    // where are we appending entries? (jQuery syntax)
    var target = 'table#articles';

    var months = new Array(12); months[0]="Jan"; months[1]="Feb"; months[2]="Mar"; months[3]="Apr"; months[4]="May"; months[5]="Jun";
    months[6]="Jul"; months[7]="Aug"; months[8]="Sep"; months[9]="Oct"; months[10]="Nov"; months[11]="Dec";

    jQuery(document).ready(function () {
        // DWR fetch, see AjaxServices.java
        AjaxServices.getNewsPreview(feedURL, function (data) {
            if (data) {
                // show us
                jQuery('#rss').slideToggle('slow');

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
                    jQuery(target).append(row);
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

    var placeholder = '<c:out value="${WEB_PROPERTIES['begin.searchBox.example']}" />';
    var placeholderTextarea = '<c:out value="${WEB_PROPERTIES['textarea.identifiers']}" />';
    var inputToggleClass = 'eg';

    /*
    function preFillInput(target, term) {
        var e = jQuery("input#actionsInput");
        e.val(term);
        if (e.hasClass(inputToggleClass)) e.toggleClass(inputToggleClass);
        e.focus();
    }
    */

    // e.g. values only available when JavaScript is on
    jQuery('input#actionsInput').toggleClass(inputToggleClass);
    jQuery('textarea#listInput').toggleClass(inputToggleClass);

    // register input elements with blur & focus
    jQuery('input#actionsInput').blur(function() {
        if (jQuery(this).val() == '') {
            jQuery(this).toggleClass(inputToggleClass);
            jQuery(this).val(placeholder);
        }
    });
    jQuery('textarea#listInput').blur(function() {
        if (jQuery(this).val() == '') {
            jQuery(this).toggleClass(inputToggleClass);
            jQuery(this).val(placeholderTextarea);
        }
    });
    jQuery('input#actionsInput').focus(function() {
        if (jQuery(this).hasClass(inputToggleClass)) {
            jQuery(this).toggleClass(inputToggleClass);
            jQuery(this).val('');
        }
    });
    jQuery('textarea#listInput').focus(function() {
        if (jQuery(this).hasClass(inputToggleClass)) {
            jQuery(this).toggleClass(inputToggleClass);
            jQuery(this).val('');
        }
    });

    // associate functions with search that redir to a keyword objects listing instead of search results
    jQuery('#mainSearchButton').click(function() {
      // if placeholder text in place, take us elsewhere
      if (jQuery("#actionsInput").val() == placeholder) {
        jQuery(location).attr('href', "/${WEB_PROPERTIES['webapp.path']}/keywordSearchResults.do?searchBag=");
        return false;
      }
    });
</script>

<!-- /newBegin.jsp -->
