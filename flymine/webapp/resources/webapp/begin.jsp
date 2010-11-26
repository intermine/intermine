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
				pathways, authors, ontology terms, etc. (e.g. <em>eve</em>, <em>embryo</em>, <em>zen allele</em>)</p>
				
				<form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
					<div class="input"><input id="actionsInput" name="searchTerm" class="input" type="text" value=""></div>
					<div class="bottom">
						<input name="searchSubmit" class="button violet" type="submit" value="search"/>
					</div>
				</form>
				
				<div style="clear:both;"></div>
			</div>
		</div>
		<div id="lists-bochs">
			<img class="title" src="images/icons/lists-64.png" title="lists"/>
			<div class="inner">
				<h3>Analyse Lists</h3>
				<p>Enter a comma separated <strong>list</strong> of identifiers.</p>
				
				<html:form action="/buildBag" focus="pasteInput">
	                <html:select property="type">
	                    <html:option value="Gene">Gene</html:option>
	                	<html:option value="Protein">Protein</html:option>
	                </html:select>
					<html:textarea property="text" />
					<div class="bottom">
						<input class="button plush" type="submit" value="analyse"/>
						<span>-or-</span>
						<a href="bag.do?subtab=upload">Upload a list</a>
					</div>
				</html:form>
			</div>
		</div>
		<div id="welcome-bochs">
			<div class="inner">
				<h3>First Time Here?</h3>
				<p>FlyMine integrates many types of data for <em>Drosophila</em> and other organisms. You can run flexible queries, export
				results and work with lists of data.</p>
				<div class="bottom">			
					<a class="button gray" href="http://www.flymine.org/help/tour/start.html"
					onclick="javascript:window.open('http://www.flymine.org/help/tour/start.html','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">
					<div><span>take a tour</span></div></a>
				</div>
			</div>
		</div>
	</div>
	
	<div style="clear:both"></div>
	
	<div id="bottom-wrap">
		<div id="templates">
			<table id="menu" border="0" cellspacing="0">
                <tr>
                    <td><a href="#tab1">Genes</a></td>
                    <td><a href="#tab2">Proteins</a></td>
                    <td><a href="#tab3">Interactions</a></td>
                    <td><a href="#tab4">Pathways</a></td>
                    <td><a href="#tab5">Homologues</a></td>
                    <td><a href="#tab6">Gene Ontology</a></td>
                    <td><a href="#tab7">Gene Expression</a></td>
                </tr>
			</table>
			
			<div id="tab-content">
			    <div id="tab1" class="content">
			        <p>The gene structure and other genome annotation in FlyMine are provided by a variety of source databases including:
			        FlyBase, UniProt, Ensembl and over 30 other data sources.  <a href="dataCategories.do">Read more...</a></p>
			        <p>Query for genes:</p>
				    <tiles:insert name="aspectTemplates.jsp">
				    	<tiles:put name="aspectQueries" beanName="aspectQueries" />
				      	<tiles:put name="aspectTitle" value="Genomics" />
				    </tiles:insert>
				    
				    <ul>
						<li><a href="#">EST clone [A. gambiae] <img src="themes/purple/homepage/arrow-gray-ico.png" /> EST clusters.</a></li>
    					<li><a href="#">Protein <img src="themes/purple/homepage/arrow-gray-ico.png" /> Gene.</a></li>
    					<li><a href="#">Chromosome [D. melanogaster] <img src="themes/purple/homepage/arrow-gray-ico.png" /> All genes with insertions.</a></li>
    					<li><a href="#">Chromosomal location <img src="themes/purple/homepage/arrow-gray-ico.png" /> All genes + Transcripts + Exons.</a></li>
    					<li><a href="#">Chromosome <img src="themes/purple/homepage/arrow-gray-ico.png" /> All genes.</a></li>
    					<li><a href="#">Pathway <img src="themes/purple/homepage/arrow-gray-ico.png" /> genes.</a></li>
    					<li><a href="#">Organism <img src="themes/purple/homepage/arrow-gray-ico.png" /> All intergenic regions.</a></li>
    					<li><a href="#">Chromosome <img src="themes/purple/homepage/arrow-gray-ico.png" /> All intergenic regions.</a></li>
    					<li><a href="#">Organism <img src="themes/purple/homepage/arrow-gray-ico.png" /> All 5' UTRs.</a></li>
    					<li><a href="#">Chromosomal location <img src="themes/purple/homepage/arrow-gray-ico.png" /> All genes.</a></li>
				    </ul>
				    
			        <p class="more"><a href="templates.do">More queries</a></p>
			    </div>
			    <div id="tab2" class="content">
			        <p>FlyMine loads proteins from UniProt and protein domains from InterPro.  <a href="aspect.do?name=Proteins">Read
			        more...</a></p>
			        <p>Query for proteins:</p>
			    	<tiles:insert name="aspectTemplates.jsp">
			      		<tiles:put name="aspectQueries" beanName="aspectQueries" />
			      		<tiles:put name="aspectTitle" value="Proteins" />
			    	</tiles:insert>
			        <p class="more"><a href="templates.do">More queries</a></p>
			    </div>
			    <div id="tab3" class="content">
			        <p>FlyMine loads interactions from IntAct and BioGRID.  <a href="aspect.do?name=Interactions">Read more</a></p>
			        <p>Query for interactions:</p>
			    	<tiles:insert name="aspectTemplates.jsp">
			      		<tiles:put name="aspectQueries" beanName="aspectQueries" />
			      		<tiles:put name="aspectTitle" value="Interactions" />
			    	</tiles:insert>
			        <p class="more"><a href="templates.do">More queries</a></p>
			    </div>
			    <div id="tab4" class="content">
			        <p>FlyMine loads pathway data.  <a href="aspect.do?name=Pathways">Read more..</a></p>
			        <p>Popular queries:</p>
			    	<tiles:insert name="aspectTemplates.jsp">
			      		<tiles:put name="aspectQueries" beanName="aspectQueries" />
			      		<tiles:put name="aspectTitle" value="Pathways" />
			    	</tiles:insert>
			        <p class="more"><a href="templates.do">More queries</a></p>
			    </div>
			    <div id="tab5" class="content">
			        <p>FlyMine loads homologues from InParanoid, KEGG and TreeFam.  <a href="aspect.do?name=Comparative+Genomics">
			        Read more</a></p>
			        <p>Query for homologues:</p>
			    	<tiles:insert name="aspectTemplates.jsp">
			      		<tiles:put name="aspectQueries" beanName="aspectQueries" />
			      		<tiles:put name="aspectTitle" value="Comparative Genomics" />
			    	</tiles:insert>
			        <p class="more"><a href="templates.do">More queries</a></p>
			    </div>
			    <div id="tab6" class="content">
			        <p>FlyMine loads gene ontology from MGI, FlyBase, WormBase, UniProt, SGD, and InterPro.
			        <a href="aspect.do?name=Gene+Ontology">Read more</a></p>
			        <p>Query for GO:</p>
			    	<tiles:insert name="aspectTemplates.jsp">
			      		<tiles:put name="aspectQueries" beanName="aspectQueries" />
			      		<tiles:put name="aspectTitle" value="Gene Ontology" />
			    	</tiles:insert>
			        <p class="more"><a href="templates.do">More queries</a></p>
			    </div>
			    <div id="tab7" class="content">
			        <p>FlyMine loads gene expression data for Drosophila melanogaster and Anopheles gambiae from FlyAtlas, BDGP,
			        ArrayExpress and Fly-FISH.  <a href="aspect.do?name=Gene+Expression">Read more...</a></p>
			        <p>Query for gene expression:</p>
			    	<tiles:insert name="aspectTemplates.jsp">
			      		<tiles:put name="aspectQueries" beanName="aspectQueries" />
			      		<tiles:put name="aspectTitle" value="Gene Expression" />
			    	</tiles:insert>
			        <p class="more"><a href="templates.do">More queries</a></p>
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
	            <img src="/metabolicmine/themes/metabolic/icons/perl-java-ico.gif" alt="perl java" />
	            <p>We support programatic access to our data through Application Programming Interface too! Choose from options below:</p>
	            <ul>
	                <li><a href="/api.do">Perl API</a>
	                <li><a href="/api.do?subtab=java">Java API</a>
	            </ul>
			</div>
			
			<div style="clear:both;"></div>
		</div>
	</div>
</div>

<script type="text/javascript">
	$(document).ready(function() {
	    $("#tab-content div.content").hide();
	    $("#templates table#menu td:first").addClass("active").append('<div class="right"></div><div class="left"></div>').show();
	    $("#tab-content div.content:first").show();
	
	    $("#templates table#menu td").click(function() {
	        $("#templates table#menu td").removeClass("active");
	        $("#templates table#menu td div").remove('.right').remove('.left')
	        
	        $(this).addClass("active").append('<div class="right"></div><div class="left"></div>');
	        $("#tab-content .content").hide();
	
	        var activeTab = $(this).find("a").attr("href");
	        $(activeTab).fadeIn();
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

	// placeholder value for search boxes
	var placeholder = 'e.g. PPARG, eve, Schmidt 2009';
	// class used when toggling placeholder
	var inputToggleClass = 'eg';

	function preFillInput(term) {
		var e = $("input#actionsInput");
		e.val(term);
		if (e.hasClass(inputToggleClass)) e.toggleClass(inputToggleClass);
		e.focus();
	}

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
</script>

<!-- /begin.jsp -->
