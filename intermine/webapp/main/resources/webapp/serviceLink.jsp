<!-- serviceLink.jsp -->
<div align="center">
<div class="plainbox" style="width:600px; font-size:14px;">

<h1>Resource link</h1>

<form action="">

    <div  style="margin-top: 10px;">
       <span style="float: left;">HTML code to embed in your website</span> 
       <span style="float: right;"><a href="javascript:openPopWindow('${link}&amp;output=html', 500, 500)" style="1em;">preview</a></span>
    </div>
    <%-- Don't split following line --%>
    <textarea style="width:100%;height:100px;"><iframe width=&quot;500&quot; height=&quot;500&quot; frameborder=&quot;1&quot; scrolling=&quot;yes&quot; marginheight=&quot;0&quot; marginwidth=&quot;0&quot; src=&quot;${link}&amp;output=html&quot;></iframe></textarea>
    
    <div style="margin-top: 10px;">
       <span style="float:left;">Web service link to the resource</span> 
       <span style="float:right;"><a href="javascript:openPopWindow('${link}&amp;output=tab', 500, 500)">preview</a></span>
    </div>
    <%-- Don't split following line --%>
    <textarea style="width:100%;height:100px;">${link}&amp;output=tab</textarea>
    
</form>

<p>For more information and information how to generate web service links  <a href="http://intermine.org/wiki/TemplateWebService">click here</a>.</p> 

</div>
</div>
<!-- /serviceLink.jsp -->

