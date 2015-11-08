<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top" rowspan="2">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <dl>
          <dt>mRNA Expression Data</dt>
          <dd>
            <p class="body">
            Phytozome hosts  a collection of RNA-seq expression studies acquired from several internal and external sources
            Next-generation sequencing reads are
            aligned to reference genomes, and gene- and transcript-level expression values are determined using Cufflinks.
            The currently available set covers:
            <ul>
              <li><em>Amaranthus hypochondriacus</em> (8 experiments from the lab of P. Jeff Maughan, BYU)</li>
              <li><em>Chlamydomonas reinhardtii</em> (133 experiments)</li>
              <li><em>Eucalyptus grandis</em> (6 experiments performed on a <em>E.grandis</em> X <em>E.urophylla</em> hybrid.)</li>
              <li><em>Glycine max</em> (9 experiments)</li>
              <li><em>Phaseolus vulgaris</em> (11 experiments)</li>
              <li><em>Physcomitrella patens</em> (33 experiments)</li>
              <li><em>Populus trichocarpa</em> (7 experiments)</li>
            </ul>
            </p>
            <p class="body">
            Please note this includes some data from the initial release of JGI Plant Gene Atlas Project:
            <ul>
              <li><em>Chlamydomonas reinhardtii</em> (26 unpublished experiments from Sabeeha Merchant, email: merchant AT chem DOT ucla DOT edu)</li>
              <li><em>Physcomitrella patens</em> (unpublished data from Stefan Rensing, email: stefan DOT rensing AT biologie DOT uni-marburg DOT de)</li>
              <li><em>Populus trichocarpa</em> (unpublished data from Gerald Tuskan, email: gtk AT ornl DOT gov )</li>
            </ul>
            </p>

            <p class="body">
            This project is a multi-laboratory collaboration that seeks to produce a standardized
            expression atlas across diverse tissues and time courses from JGI Plant
            Flagship organisms.  More detail about the Plant Flagship Genomes can be
            found
     <a href="http://jgi.doe.gov/our-science/science-programs/plant-genomics/plant-flagship-genomes">
            here.</a>
            JGI users can submit proposals to have tissues and conditions of
            interest included the Plant Atlas through the 
            <a href="http://jgi.doe.gov/collaborate-with-jgi/community-science-program/">
            JGI Community Science Program
            </a>.
            </p>

            <p class="body">
            This release V0.1 provides normalized expression values for diverse
            growth conditions of Chlamydomonas, Populus, and Physcomitrella.
            <p class="body">
            This data is unpublished.  As a public service, the
            Department of Energy's Joint Genome Institute (JGI) is
            making the RNA-seq expression data
            available before scientific publication according to the
            Ft. Lauderdale Accord. This balances the imperative of the
            DOE and the JGI that the data from its sequencing projects
            be made available as soon and as completely as possible
            with the desire of contributing scientists and the JGI to
            reserve a reasonable period of time to publish on analysis without concerns about preemption
            by other groups.
            </p>
	        <p class="body">
            At the present time, we request that anyone intending to download Gene Atlas data for use in any analysis to <b>please contact the overall project lead</b>,
	    Jeremy Schmutz at JGI/HudsonAlpha, email: jschmutz AT hudsonalpha DOT org.   To be kept informed on the progress of the JGI Gene Atlas project, changes in 
	   the data usage policy, or to ask questions, please subscribe to the project mailing list by sending a blank email with the Subject line "subscribe jgi-gene_atlas FirstName LastName"
	   to sympa@lists.lbl.gov.
            </p>

          </dd>
        </dl>
      </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
      Bulk data files for gene expression data for <i><b>non-Gene Atlas</b></i> experiments are available for download from individual organisms folders at the 
      <a href="http://genome.jgi.doe.gov/pages/dynamicOrganismDownload.jsf?organism=PhytozomeV10">
      JGI Genome Portal </a>.
      </div>
    </td>
  </tr>
</table>
