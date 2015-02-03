<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top" rowspan="2">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <DL>
          <DT>Ortholog/Paralog Data</DT>
          <DD>
            Phytozome has compiled a collection orthologs and paralogs
            for the genes in our database. 

            <p class="body">
Ortholog calls were generated using
<a href="http://software.sbc.su.se/">inParanoid 4.1</a>.

InParanoid was run on proteome sequences
for all possible organism-organism pairs in Phytozome using the default
two-pass blast strategy. Genes from different species that fell in the same
InParanoid ortholog cluster were considered orthologs.
<p class="body">
Automatic clustering of orthologs and in-paralogs from pairwise species
comparisons. Remm M, Storm CE and Sonnhammer EL. J. Mol Biol. 2001 Dec 14.

          </DD>
        </DL>
      </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
      Bulk data files for all organisms in Phytozome are available for download from the 
      <a href="http://genome.jgi.doe.gov/pages/dynamicOrganismDownload.jsf?organism=PhytozomeV10">
      JGI Download Portal </a>.
      </div>
    </td>
  </tr>
</table>
