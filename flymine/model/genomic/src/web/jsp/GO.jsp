<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td>
      <div class="heading2">
        GO annotation in FlyMine
      </div>
      <div class="body">
        <DL>
          <DT><I>D. melanogaster</I></DT>
          <DD>
            GO annotations for <I>D. melanogaster</I> gene products assigned by FlyBase.<br/>
            The gene association file is downloaded from <A HREF="http://www.geneontology.org/GO.current.annotations.shtml">www.geneontology.org</A>.<BR/>
          </DD>
          <DT><I>C. elegans</I></DT>
          <DD>
            GO annotations for <I>C. elegans</I> gene products assigned by WormBase.<BR/>
            The gene association file is downloaded from <A HREF="http://www.geneontology.org/GO.current.annotations.shtml">www.geneontology.org</A>.<BR/>
          </DD>
          <DT><I>A. gambiae</I></DT>
          <DD>
            GO annotations for <I>A. gambiae</I> gene products assigned by the GOA (GO annotation@EBI) project.<BR/>
            The gene association file (gene_association.goa_uniprot.gz) is downloaded from the <A HREF="ftp://ftp.ebi.ac.uk/pub/databases/GO/goa/UNIPROT">EBI ftp site</A>.
          </DD>
        </DL>
      </div>
    </td>
    <td width="40%" valign="top">
      <div class="heading2">
        Explore data sets
      </div>
      <div class="body">
        <ul>
          <li>
            <im:querylink text="All gene / GO annotation pairs from <i>Drosophila</i> (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Gene Gene.allGoAnnotation">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All gene identifiers and GO terms from <i>Drosophila</i> (for export/download)"
                          skipBuilder="true">
              <query name="" model="genomic" view="Gene.identifier Gene.name Gene.organismDbId Gene.symbol Gene.allGoAnnotation.identifier">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
        </ul>
        <ul>
          <li>
            <im:querylink text="All gene / GO annotation pairs from <i>Anopheles</i> (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Gene Gene.proteins.annotations">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.proteins" type="Protein">
                </node>
                <node path="Gene.proteins.annotations" type="GOAnnotation">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.species" type="String">
                </node>
                <node path="Gene.organism.name" type="String">
                </node>
                <node path="Gene.organism.genus" type="String">
                  <constraint op="=" value="Anopheles" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All gene identifiers and GO terms from <i>Anopheles</i> (for export/download)"
                          skipBuilder="true">
              <query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.proteins.annotations.name Gene.proteins.annotations.identifier Gene.proteins.annotations.evidenceCode">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.proteins" type="Protein">
                </node>
                <node path="Gene.proteins.annotations" type="GOAnnotation">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.species" type="String">
                </node>
                <node path="Gene.organism.name" type="String">
                </node>
                <node path="Gene.organism.genus" type="String">
                  <constraint op="=" value="Anopheles" description="" identifier="" code="A">
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
