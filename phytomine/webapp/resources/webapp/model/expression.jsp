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
          <DT>mRNA Expression Data</DT>
          <DD>
            Phytozome has compiled a collection of RNA-seq data acquired from different
            organisms. Next-generation sequencing reads were
            aligned to reference genomes expression values for genes and transcripts
            were quantified using Cufflinks.
            The data available is:
            <UL>
              <LI><em>Chlamydomonas reinhardtii</em> (108 experiments)</LI>
              <LI><em>Phaseolus vulgaris</em> (11 experiments)</LI>
              <LI><em>Glycine max</em> (9 experiments)</LI>
            </UL>
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
