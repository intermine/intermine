<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <dt>

         The pathway data in the KEGG database have been manually entered from published materials. Current KEGG data in FlyMine are from Release41 and only include KEGG pathway names with their IDs and the genes involved.  
        </dt>

        <dd>Kanehisa et al (2006) Nucleic Acids Res. 34, D354-357  (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16381885">PubMed: 16381885</a>) - From genomics to chemical genomics: new developments in KEGG. 
        </dt>
       </div>
    </td>

    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
         <ul>
      
          <li>
            <im:querylink text="All KEGG pathways with associated genes " skipBuilder="true">

            <query name="" model="genomic" view="Pathway.identifier Pathway.name Pathway.genes.identifier Pathway.genes.symbol"></query>
            </im:querylink>

          </li>
         </ul>
      </div>
    </td>
  </tr>
</table>
