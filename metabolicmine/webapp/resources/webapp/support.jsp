<%--
	=========================================================================================
	This JSP controls the way we provide support to the users and receive feedback from them.
	Author: Radek, Nov 2010
	=========================================================================================
--%>

<style type="text/css">
	<%-- TODO: move to external ---%>
	#survey { margin-top:40px; width:1000px; display:block; background:#FFF; }
		#survey #header { height:auto; }
			#survey #header h2 { width:960px; display:block; background:#FFEADB; border-top:1px solid #F08B4A; padding:4px 20px;
			font-size:17px; color:#000; margin-bottom:6px; }
		#survey #wrapper { padding:20px; }
		#survey h3 { font-size:16.5px; color:#000; margin-bottom:10px; }
		#survey a { font-size:14px; }
		#survey input[type="text"] { width:400px; }
	#survey #search-faq, #survey #recommended-faq div.two-columns ul.column { margin-bottom:30px; }
	#survey ul { list-style-type:none; padding:0; margin:0; }
	#survey #learn-more h3 { background:url("themes/metabolic/icons/learn-24.png") no-repeat left; padding-left:32px; }
		#survey #recommended-faq ul.column { width:50%; }
		#survey #learn-more ul { ; }
			#survey #learn-more ul li.shift-1 { padding-left:20px; }
			#survey #learn-more ul li.shift-2 { padding-left:40px; }
				#survey #learn-more ul li a { font-weight:bold; }
</style>


<div id="survey">
	
	<div id="header">
		<h2>metabolicMine Help & Support</h2>
	</div>
	
	<div id="wrapper">
		<%-- search FAQ --%>
		<div id="search-faq">
			<%-- TODO: make this search something ---%>
			<h3>What can we help you with?</h3>
			<input type="text" value=""/>
			<input type="button" value="Search FAQ"/>
		</div>
		
		<%-- recommended articles --%>
		<div id="recommended-faq">
			<%-- TODO: set from external file/db based on page we are on ---%>
			<h3>Recommended articles</h3>
			<div class="two-columns">
				<ul class="column">
					<li><a href="#">How do I use the search form?</a></li>
					<li><a href="#">Can I trust you won't track my searches?</a></li>
					<li><a href="#">Is your database curated?</a></li>
				</ul>
				<ul class="column">
					<li><a href="#">Can I buy you a beer when I see you next?</a></li>
					<li><a href="#">How frequently do you update the site and its data?</a></li>
					<li><a href="#">Why is it so cold outside? Weather Control 101.</a></li>
				</ul>
				<div style="clear:both;"></div>
			</div>
		</div>
	
		<%-- learn more from our walkthrough --%>
		<div id="learn-more">
			<%-- TODO: "Mike" this walkthrough ---%>
			<h3>Learn to use metabolicMine</h3>
			<ul>
				<li><a href="#">1.0 Homepage</a></li>
					<li class="shift-1"><a href="#">1.1 Homepage search</a></li>
					<li class="shift-1"><a href="#">1.2 Homepage list upload</a></li>
						<li class="shift-2"><a href="#">1.2.1 List upload functionality</a></li>
				<li><a href="#">2.0 Anotherpage</a></li>
			</ul>
		</div>
	</div>
</div>