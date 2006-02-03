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
        Dataset links
      </div>
      <div class="body">
        <ul>
          <li>
            <im:querylink text="All gene / GO annotation pairs from Drosophila (browse)" skipBuilder="true">
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
            <im:querylink text="All gene identifiers and GO terms from Drosophila (for export/download)"
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
      </div>
    </td>
  </tr>
</table>
