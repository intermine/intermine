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
          <DT><A href="http://www.uniprot.org">UniProt Knowledgebase (UniProtKB)</A></DT>
          <DD>
            All proteins from the <A href="http://www.uniprot.org" target="_new">UniProt Knowledgebase</A> (version 14.9) for the following organisms have been loaded:
            <UL>
              <LI><I>D. rerio</I></LI>
            </UL>
            For each protein record in UniProt for each species the following information is extracted and loaded into FishMine:
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
        </DL>
      </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Bulk download <i>D. rerio</i> data
      </div>
      <div class="body">
        <ul>
          <li>
            <i>Danio rerio</i> proteins and corresponding genes:
            <span style="white-space:nowrap">
              <im:querylink text="[browse]" skipBuilder="true">
                <query name="" model="genomic" view="Protein Protein.genes">
                  <node path="Protein" type="Protein">
                  </node>
                  <node path="Protein.organism" type="Organism">
                  </node>
                  <node path="Protein.organism.name" type="String">
                    <constraint op="=" value="Danio rerio" description="" identifier="" code="A">
                    </constraint>
                  </node>
                </query>
              </im:querylink>
              or
              <im:querylink text="[export/download]" skipBuilder="true">
                <query name="" model="genomic" view="Protein.identifier Protein.name Protein.primaryAccession Protein.genes.identifier">
                  <node path="Protein" type="Protein">
                  </node>
                  <node path="Protein.organism" type="Organism">
                  </node>
                  <node path="Protein.organism.name" type="String">
                    <constraint op="=" value="Danio rerio" description="" identifier="" code="A">
                    </constraint>
                  </node>
                </query>
              </im:querylink>
            </span>
          </li>
        </ul>
      </div>
    </td>
  </tr>
</table>
