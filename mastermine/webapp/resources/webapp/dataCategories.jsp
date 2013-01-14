<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- dataCategories -->


<html:xhtml/>

<div class="body">
<im:boxarea title="Data" stylename="plainbox"><p>masterMine integrates orthologue data from a large number of sources into a single data warehouse.  This page lists the data that are included in the current release.  Many more data sets will be added in future releases, please contact us if there are any particular data you would like to see included.</p></im:boxarea>


<div style="padding: 10px 40px">
<h3>The following data are loaded in the mastermine BETA:</h3>

<br/>

<table>
       <tr>
               <th>Type</th>
               <th>Source</th>
               <th>Organisms</th>
               <th>Version</th>
       </tr>
       
       <tr>
               <td>Gene names and symbol</td>
               <td><a href="http://www.genenames.org/">HGNC</a>, <a href="http://www.ncbi.nlm.nih.gov/gene/">NCBI</a>, <a href="http://www.informatics.jax.org/">MGI</a>, <a href="http://rgd.mcw.edu/">RGD</a></td>
               <td>Human</td>
               <td>August 2012</td>
       </tr>
 <tr>
               <td>Gene names and symbol</td>
               <td><a href="http://www.informatics.jax.org/">MGI</a></td>
               <td>Mouse</td>
               <td>August 2012</td>
       </tr>
 <tr>
               <td>Gene names and symbol</td>
               <td><a href="http://rgd.mcw.edu/">RGD</a></td>
               <td>Rat</td>
               <td>August 2012</td>
       </tr>
 <tr>
               <td>Gene names and symbol</td>
               <td><a href="http://www.flybase.org/">FlyBase</a></td>
               <td>Fly</td>
               <td>August 2012</td>
       </tr>
<tr>
               <td>Gene names and symbol</td>
               <td><a href="http://http://www.yeastgenome.org//">SGD</a></td>
               <td>Yeast</td>
               <td>August 2012</td>
       </tr>
 <tr>
               <td>Gene names and symbol</td>
               <td><a href="http://www.wormbase.org/">Wormbase</a></td>
               <td>Worm</td>
               <td>August 2012</td>
       </tr>
 <tr>
               <td>Gene names and symbol</td>
               <td><a href="http://www.zfin.org//">ZFIN</a></td>
               <td>Zebrafish</td>
               <td>August 2012</td>
       </tr>                                     
       <tr>
               <td>Orthologues</td>
               <td><a href="http://http://www.ncbi.nlm.nih.gov/homologene">Homologene</a></td>
               <td>Fly, Mouse, Rat, Yeast, Worm, Zebrafish, Human</td>
               <td>Homologene</td>
       </tr>
<tr>
               <td>Orthologues</td>
               <td><a href="http://cegg.unige.ch/orthodb6">OrthoDB</a></td>
               <td>Fly, Mouse, Rat, Yeast, Worm, Zebrafish, Human</td>
               <td>OrthoDB</td>
       </tr>
<tr>
               <td>Orthologues</td>
               <td><a href="http://http://www.pantherdb.org/">Panther</a></td>
               <td>Fly, Mouse, Rat, Yeast, Worm, Zebrafish, Human</td>
               <td>Panther</td>
       </tr>
       
</table>

</div>
</div>
<!-- /dataCategories -->
