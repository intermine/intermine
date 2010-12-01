<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml/>

<div id="content-wrap">
    <div id="boxes">
        <div id="search-bochs">
            <img class="title" src="themes/purple/homepage/search-ico-right.png" title="search"/>
            <div class="inner">
                <h3>Search</h3>
                <span class="ugly-hack">&nbsp;</span>
                <p>Search FlyMine. Enter <strong>names</strong>, <strong>identifiers</strong> or <strong>keywords</strong> for genes,
                proteins, pathways, ontology terms, authors, etc. (e.g. <em>eve</em>, HIPPO_DROME, glycolysis, <em>hb</em> allele).</p>

                <form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
                    <div class="input"><input id="actionsInput" name="searchTerm" class="input" type="text" value="eg. zen"></div>
                    <div class="bottom">
                        <center>
                            <input name="searchSubmit" class="button violet" type="submit" value="search"/>
                        </center>
                    </div>
                </form>

                <div style="clear:both;"></div>
            </div>
        </div>
        <div id="lists-bochs">
            <img class="title" src="images/icons/lists-64.png" title="lists"/>
            <div class="inner">
                <h3>Analyse</h3>
                <p>Enter a <strong>list</strong> of identifiers.</p>

                <form name="buildBagForm" method="post" action="<c:url value="/buildBag.do" />">
                    <select name="type">
                        <option value="Gene">Gene</option>
                        <option value="Protein">Protein</option>
                    </select>
                    <div class="textarea"><textarea name="text">eg. zen, adh, CG2328, FBgn0000099</textarea></div>
                    <div class="bottom">
                        <center>
                            <a class="advanced" href="bag.do?subtab=upload">advanced</a>
                            <br /><br />
                            <input class="button plush" type="submit" value="analyse"/>
                        </center>
                    </div>
                </form>
            </div>
        </div>
        <div id="welcome-bochs">
            <div class="inner">
                <h3>First Time Here?</h3>
                <br />
                <p>FlyMine integrates many types of data for <em>Drosophila</em>, <em>Anopheles</em> and other organisms. You can run flexible queries, export results and analyse lists of data.</p>
                <div class="bottom">
                    <center>
                        <a class="button gray" href="http://www.flymine.org/help/tour/start.html"
                        onclick="javascript:window.open('http://www.flymine.org/help/tour/start.html','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">
                        take a tour
                        </a>
                    </center>
                </div>
            </div>
        </div>
    </div>

    <div style="clear:both"></div>

    <div id="bottom-wrap">
        <div id="templates">
            <table id="menu" border="0" cellspacing="0">
                <tr>
                    <td><div class="container"><span id="tab1">Genes</span></div></td>
                    <td><div class="container"><span id="tab2">Proteins</span></div></td>
                    <td><div class="container"><span id="tab3">Interactions</span></div></td>
                    <td><div class="container"><span id="tab4">Pathways</span></div></td>
                    <td><div class="container"><span id="tab5">Homologues</span></div></td>
                    <td><div class="container"><span id="tab6">Gene Ontology</span></div></td>
                    <td><div class="container"><span id="tab7">Gene Expression</span></div></td>
                </tr>
            </table>

            <div id="tab-content">
                <div id="ribbon"></div>
                <div id="try"></div>
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

        <div id="low">
            <div id="rss" style="display:none;">
                <h4>News<span>&nbsp;&amp;&nbsp;</span>Updates</h4>
                <table id="articles"></table>
                <p class="more"><a target="new" href="http://blog.flymine.org/">More news</a></p>
            </div>

            <div id="api">
                <h4>Perl<span>&nbsp;&amp;&nbsp;</span>Java API</h4>
                <img src="themes/metabolic/icons/perl-java-ico.gif" alt="perl java" />
                <p>We support programatic access to our data through Application Programming Interface too! Choose from options below:</p>
                <ul>
                    <li><a href="<c:out value="${WEB_PROPERTIES['path']}" />api.do">Perl API</a>
                    <li><a href="<c:out value="${WEB_PROPERTIES['path']}" />api.do?subtab=java">Java API</a>
                </ul>
            </div>

            <div style="clear:both;"></div>
        </div>
    </div>
</div>

<script type="text/javascript">
    jQuery(document).ready(function() {
        jQuery("#tab-content .content").each(function() {
            jQuery(this).hide();
        });

        jQuery("table#menu td:first").addClass("active").find("div").append('<span class="right"></span><span class="left"></span>').show();
        jQuery("div.content:first").show();

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
    });


    // feed URL
    var feedURL = "${WEB_PROPERTIES['project.rss']}";
    // limit number of entries displayed
    var maxEntries = 2
    // where are we appending entries? (jQuery syntax)
    var target = 'table#articles';

    var months = new Array(12); months[0]="Jan"; months[1]="Feb"; months[2]="Mar"; months[3]="Apr"; months[4]="May"; months[5]="Jun";
    months[6]="Jul"; months[7]="Aug"; months[8]="Sep"; months[9]="Oct"; months[10]="Nov"; months[11]="Dec";

    jQuery(document).ready(function() {
        // DWR fetch, see AjaxServices.java
        AjaxServices.getNewsPreview(feedURL, function(data) {
            if (data) {
                // show us
                jQuery('#rss').slideToggle('slow');

                // declare
                var feedTitle, feedDescription, feedDate, feedLink, row, feed;

                // convert to XML, jQuery manky...
                try {
                    // Internet Explorer
                    feed = new ActiveXObject("Microsoft.XMLDOM");
                    feed.async="false";
                    feed.loadXML(data);
                } catch(e) {
                    try {
                        // ...the good browsers
                        feed = new DOMParser().parseFromString(data, "text/xml");
                    } catch(e) {
                        // ... BFF
                        alert(e.message);
                        return;
                    }
                }

                var items = feed.getElementsByTagName("item"); // ATOM!!!
                for (var i = 0; i < items.length; ++i) {
                    // early bath
                    if (i > maxEntries) return;

                    feedTitle = trimmer(items[i].getElementsByTagName("title")[0].firstChild.nodeValue, 80);
                    feedDescription = trimmer(items[i].getElementsByTagName("description")[0].firstChild.nodeValue, 80);
                    feedDate = new Date(items[i].getElementsByTagName("pubDate")[0].firstChild.nodeValue);
                    feedLink = items[i].getElementsByTagName("link")[0].firstChild.nodeValue

                    // build table row
                    row = '<tr>'
                            + '<td class="date">'
                                + '<a target="new" href="' + feedLink + '">' + feedDate.getDate()
                                + '<br /><span>' + months[feedDate.getMonth()] + '</span></a></td>'
                            + '<td><a target="new" href="' + feedLink + '">' + feedTitle + '</a><br/>' + feedDescription + '</td>'
                        + '</tr>';
                    // append, done
                    jQuery(target).append(row);
                    i++;
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

    // placeholder value for search boxes
    var placeholder = 'e.g. zen, Q9V4E1';
    // class used when toggling placeholder
    var inputToggleClass = 'eg';

    function preFillInput(term) {
        var e = jQuery("input#actionsInput");
        e.val(term);
        if (e.hasClass(inputToggleClass)) e.toggleClass(inputToggleClass);
        e.focus();
    }

    // e.g. values only available when JavaScript is on
    jQuery('input#actionsInput').toggleClass(inputToggleClass);

    // register input elements with blur & focus
    jQuery('input#actionsInput').blur(function() {
        if (jQuery(this).val() == '') {
            jQuery(this).toggleClass(inputToggleClass);
            jQuery(this).val(placeholder);
        }
    });
    jQuery('input#actionsInput').focus(function() {
        if (jQuery(this).hasClass(inputToggleClass)) {
            jQuery(this).toggleClass(inputToggleClass);
            jQuery(this).val('');
        }
    });
</script>

<!-- /begin.jsp -->
