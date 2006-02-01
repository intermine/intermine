<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table>
  <tr>
    <td valign="top">
      <div class="heading2">
        Current Data
      </div>
      <div class="body">
        <DL>
          <DT><A href="http://www.ebi.uniprot.org/index.shtml">Uniprot
          Knowledgebase (UniProtKB)</A></DT>
          <DD>
            All proteins from the <A
            href="http://www.ebi.uniprot.org/index.shtml">UniProt
            Knowledgebase</A> (version 5.6) for the following organisms have
            been loaded:
            <UL>
              <LI><I>D. melanogaster</I></LI>
              <LI><I>A. gambiae</I></LI>
              <LI><I>C. elegans</I></LI>
            </UL>
            For each protein record in Uniprot for each species the following
            information is extracted and loaded into FlyMine:
            <UL>
              <LI>Entry name</LI>
              <LI>Primary accession number</LI>
              <LI>Secondary accession number</LI>
              <LI>Protein name</LI>
              <LI>Comments</LI>
              <LI>Publications</LI>
              <LI>Sequence</LI>
              <LI>Gene ORF name</LI>
            </UL>
          </DD>
          <DT><A href="http://www.ebi.ac.uk/interpro">Interpro</A> protein
          families and domains.</DT>
          <DD>
            Protein family and domain assignments to proteins in Uniprot have
            been loaded from <A
            href="http://www.ebi.ac.uk/interpro">Interpro</A> (version 10.0)
            for the following organisms:
            <UL>
              <LI><I>D. melanogaster</I></LI>
              <LI><I>A. gambiae</I></LI>
              <LI><I>C. elegans</I></LI>
            </UL>
          </DD>
        </DL>
      </div>
    </td>
    <td width="30%" valign="top">
      <div class="heading2">
        Datasets
      </div>
      <div class="body">
        <ul>
          <li>
            <im:querylink text="All <i>Drosophila melanogaster</i> proteins
                                and corresponding genes (browse)"
                          skipBuilder="true">
              <query name="" model="genomic" view="Protein Protein.genes">
                <node path="Protein" type="Protein">
                </node>
                <node path="Protein.organism" type="Organism">
                </node>
                <node path="Protein.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster"
                              description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All <i>Drosophila melanogaster</i> protein
                                identifiers with gene identifier and location (for export/download)"
                          skipBuilder="true">
              <query name="" model="genomic" view="Protein.identifier Protein.name Protein.primaryAccession Protein.genes.identifier Protein.genes.chromosomeLocation.start Protein.genes.chromosomeLocation.end">
                <node path="Protein" type="Protein">
                </node>
                <node path="Protein.organism" type="Organism">
                </node>
                <node path="Protein.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All <i>Anopheles gambiae str. PEST</i> proteins
                                and corresponding genes (browse)"
                          skipBuilder="true">
              <query name="" model="genomic" view="Protein Protein.genes">
                <node path="Protein" type="Protein">
                </node>
                <node path="Protein.organism" type="Organism">
                </node>
                <node path="Protein.organism.name" type="String">
                  <constraint op="=" value="Anopheles gambiae str. PEST"
                              description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All <i>Anopheles gambiae str. PEST</i> protein
                                identifiers with gene identifier and location (for export/download)"
                          skipBuilder="true">
              <query name="" model="genomic" view="Protein.identifier Protein.name Protein.primaryAccession Protein.genes.identifier Protein.genes.chromosomeLocation.start Protein.genes.chromosomeLocation.end">
                <node path="Protein" type="Protein">
                </node>
                <node path="Protein.organism" type="Organism">
                </node>
                <node path="Protein.organism.name" type="String">
                  <constraint op="=" value="Anopheles gambiae str. PEST"
                              description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
        </ul>
      </div>
    </td>
  </tr>
</table>
