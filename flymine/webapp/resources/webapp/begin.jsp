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
				<p>Search FlyMine. Enter <strong>names</strong>, <strong>identifiers</strong> or <strong>keywords</strong> for genes, pathways, authors,
				ontology terms, etc. (e.g. <em>eve</em>, <em>embryo</em>, <em>zen allele</em>)</p>
				<div class="input"><input class="input" type="text" value="e.g. eve"></div>
				<input class="button violet" type="submit" value="search"/>
				<div style="clear:both;"></div>
			</div>
		</div>
		<div id="lists-bochs">
			<img class="title" src="images/icons/lists-64.png" title="lists"/>
			<div class="inner">
				<h3>Analyse Lists</h3>
				<p>Enter a comma separated <strong>list</strong> of identifiers.</p>
				<select>
                    <option>Gene</option>
				</select>
				<textarea></textarea>
				<input class="button plush" type="submit" value="analyse"/>
				<span>-or-</span>
				<a href="#">Upload a list</a>
			</div>
		</div>
		<div id="welcome-bochs">
			<div class="inner">
				<h3>First Time Here?</h3>
				<p>FlyMine integrates many types of data for <em>Drosophila</em> and other organisms.
				You can run flexible queries, export results and work with lists of data.
				</p>
				<a class="button gray" href="#">
					<div><span>take a tour</span></div>
				</a>
			</div>
		</div>
	</div>

	<div style="clear:both"></div>

	<div id="bottom-wrap">
		<div id="templates">

		</div>
		<div id="low">
			<div id="api">
	            <h4>Perl<span>&nbsp;&amp;&nbsp;</span>Java API</h4>
	            <img src="/metabolicmine/themes/metabolic/icons/perl-java-ico.gif" alt="perl java" />
	            <p>We support programatic access to our data through Application Programming Interface too! Choose from options below:</p>
	            <ul>
	                <li><a href="#">Perl API</a></li>
	                <li><a href="#">Java API</a></li>
	            </ul>
			</div>
			<div id="rss" style="display:none;">
				<h4>News<span>&nbsp;&amp;&nbsp;</span>Updates</h4>
				<table id="articles"></table>
			</div>
		</div>
	</div>
</div>

<script type="text/javascript">
		// feed URL
		var feedURL = "${WEB_PROPERTIES['project.rss']}";
		// limit number of entries displayed
		var maxEntries = 2;
		// where are we appending entries? (jQuery syntax)
		var target = 'table#articles';

		var months = new Array(12); months[0]="Jan"; months[1]="Feb"; months[2]="Mar"; months[3]="Apr"; months[4]="May"; months[5]="Jun";
		months[6]="Jul"; months[7]="Aug"; months[8]="Sep"; months[9]="Oct"; months[10]="Nov"; months[11]="Dec";

		$(document).ready(function() {
			// DWR fetch, see AjaxServices.java
			AjaxServices.getNewsPreview(feedURL, function(data) {
				if (data) {
					// show us
					$('#rss').slideToggle('slow');

					// declare
					var feedTitle, feedDescription, feedDate, feedLink, row;

					// convert to XML, jQuery manky...
		            var feed = new DOMParser().parseFromString(data, "text/xml");
		            var items = feed.getElementsByTagName("item"); // ATOM!!!
		            for (var i = 0; i < items.length; ++i) {
						// early bath
						if (i > maxEntries) return;

			            feedTitle = trimmer(items[i].getElementsByTagName("title")[0].firstChild.nodeValue, 70);
			            feedDescription = trimmer(items[i].getElementsByTagName("description")[0].firstChild.nodeValue, 70);
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
    					$(target).append(row);
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
</script>




















<div style="background:red">

<div class="body">
<div id="actions">


     <im:boxarea title="Search" stylename="search plainbox">
        <form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
            <input type="text" id="keywordSearch" name="searchTerm" value="" />
            <input type="submit" name="searchSubmit" value="Search" />
        </form>
<br/>
      <em><p>Search FlyMine. Enter <strong>identifiers</strong>, <strong>names</strong> or <strong>keywords</strong> for
                genes, pathways, authors, ontology terms, etc.  (e.g. <i>eve</i>, <i>embryo</i>,
                <i>zen</i>, <i>allele</i>)
     </p></em>

    </im:boxarea>

     <im:boxarea title="Java/Perl API" stylename="api plainbox">
     <br/>
We support programatic access to our data through Application Programming Interface too.  Choose from options below:
<br/>
<ul>
<li><a href="/api.do?subtab=java">Java</a>
<li><a href="/api.do">Perl</a>
</ul>
    </im:boxarea>

     <im:boxarea title="Analyse" stylename="analyse plainbox last">
     <em><p>Enter a list of identifiers to be forwarded to the list analysis page.</p></em>
        <html:form action="/buildBag" focus="pasteInput">

                <html:select styleId="typeSelector" property="type">
                        <html:option value="Gene">Gene</html:option>
                        <html:option value="Protein">Protein</html:option>
                </html:select>
            <html:textarea styleId="pasteInput" property="text" rows="2" cols="30" />
            <a href="bag.do?subtab=upload">Upload a file</a>.
            <html:submit styleId="submitBag">Analyse</html:submit>
        </html:form>
    </im:boxarea>
</div>

<div style="clear:both;"></div>


<div>
<ul class="tabs">
    <li class="tab"><a href="#tab1">Genes</a></li>
    <li class="tab"><a href="#tab2">Proteins</a></li>
    <li class="tab"><a href="#tab3">Interactions</a></li>
    <li class="tab"><a href="#tab4">Pathways</a></li>
    <li class="tab"><a href="#tab5">Homologues</a></li>
    <li class="tab"><a href="#tab6">Gene Ontology</a></li>
    <li class="tab"><a href="#tab7">Gene Expression</a></li>
    <li class="link"><a href="dataCategories.do">more ...</a></li>
</ul>
</div>

<div class="tab_container">
    <div id="tab1" class="tab_content">
        The gene structure and other genome annotation in FlyMine are provided by a variety of source databases including: FlyBase, UniProt, Ensembl and over 30 other data sources.  <a href="dataCategories.do">Read more...</a>
        <br/><br/>
        Query for genes:
        <br/>
    <tiles:insert name="aspectTemplates.jsp">
      <tiles:put name="aspectQueries" beanName="aspectQueries" />
      <tiles:put name="aspectTitle" value="Genomics" />
    </tiles:insert>
        <br/><br/>
        <small><a href="templates.do">Click here</a> for more queries.</small>
    </div>
    <div id="tab2" class="tab_content">
        FlyMine loads proteins from UniProt and protein domains from InterPro.  <a href="aspect.do?name=Proteins">Read more...</a>
        <br/><br/>
        Query for proteins:
        <br/>
    <tiles:insert name="aspectTemplates.jsp">
      <tiles:put name="aspectQueries" beanName="aspectQueries" />
      <tiles:put name="aspectTitle" value="Proteins" />
    </tiles:insert>
        <br/><br/>
        <small><a href="templates.do">Click here</a> for more queries.</small>
    </div>
    <div id="tab3" class="tab_content">
        FlyMine loads interactions from IntAct and BioGRID.  <a href="aspect.do?name=Interactions">Read more</a>
        <br/><br/>
        Query for interactions:
        <br/>
    <tiles:insert name="aspectTemplates.jsp">
      <tiles:put name="aspectQueries" beanName="aspectQueries" />
      <tiles:put name="aspectTitle" value="Interactions" />
    </tiles:insert>
        <br/><br/>
        <small><a href="templates.do">Click here</a> for more queries.</small>
    </div>
    <div id="tab4" class="tab_content">
        FlyMine loads pathway data.  <a href="aspect.do?name=Pathways">Read more..</a>
        <br/><br/>
        Popular queries:
        <br/>
    <tiles:insert name="aspectTemplates.jsp">
      <tiles:put name="aspectQueries" beanName="aspectQueries" />
      <tiles:put name="aspectTitle" value="Pathways" />
    </tiles:insert>
        <br/><br/>
        <small><a href="templates.do">Click here</a> for more queries.</small>
    </div>
    <div id="tab5" class="tab_content">
        FlyMine loads homologues from InParanoid, KEGG and TreeFam.  <a href="aspect.do?name=Comparative+Genomics">Read more</a>
        <br/><br/>
        Query for homologues:
        <br/>
    <tiles:insert name="aspectTemplates.jsp">
      <tiles:put name="aspectQueries" beanName="aspectQueries" />
      <tiles:put name="aspectTitle" value="Comparative Genomics" />
    </tiles:insert>
        <br/><br/>
        <small><a href="templates.do">Click here</a> for more queries.</small>
    </div>
    <div id="tab6" class="tab_content">
        FlyMine loads gene ontology from MGI, FlyBase, WormBase, UniProt, SGD, and InterPro.  <a href="aspect.do?name=Gene+Ontology">Read more</a>
        <br/><br/>
        Query for GO:
        <br/>
    <tiles:insert name="aspectTemplates.jsp">
      <tiles:put name="aspectQueries" beanName="aspectQueries" />
      <tiles:put name="aspectTitle" value="Gene Ontology" />
    </tiles:insert>
        <br/><br/>
        <small><a href="templates.do">Click here</a> for more queries.</small>
    </div>
    <div id="tab7" class="tab_content">
        FlyMine loads gene expression data for Drosophila melanogaster and Anopheles gambiae from FlyAtlas, BDGP, ArrayExpress and Fly-FISH.  <a href="aspect.do?name=Gene+Expression">Read more...</a>
        <br/><br/>
        Query for gene expression:
        <br/>
    <tiles:insert name="aspectTemplates.jsp">
      <tiles:put name="aspectQueries" beanName="aspectQueries" />
      <tiles:put name="aspectTitle" value="Gene Expression" />
    </tiles:insert>
        <br/><br/>
        <small><a href="templates.do">Click here</a> for more queries.</small>
    </div>
</div>

 <br style="clear: left;" />
 <br style="clear: both;" />

       <div id="rss">
        <c:if test="${!empty WEB_PROPERTIES['project.rss']}">
        </c:if>
      </div>

</div>
<script language="javascript">
<!--//<![CDATA[
    document.getElementById("takeATourLink").style.display="block";

    $(document).ready(function() {

    //When page loads...
    $(".tab_content").hide(); //Hide all content
    $("ul.tabs li:first").addClass("active").show(); //Activate first tab
    $(".tab_content:first").show(); //Show first tab content

    //On Click Event
    $("ul.tabs li.tab").click(function() {
        $("ul.tabs li").removeClass("active"); //Remove any "active" class
        $(this).addClass("active"); //Add "active" class to selected tab
        $(".tab_content").hide(); //Hide all tab content

        var activeTab = $(this).find("a").attr("href"); //Find the href attribute value to identify the active tab + content
        $(activeTab).fadeIn(); //Fade in the active ID content
        return false;
    });

});


//]]>-->
</script>

</div>
<!-- /begin.jsp -->
