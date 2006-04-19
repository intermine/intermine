<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<TABLE width="100%">
  <TR>
    <TD valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <p>
          Orthologue and paralogue relationships calculated by <A
          href="http://inparanoid.cgb.ki.se/index.html">InParanoid</A> (latest
          calculated 16th April 2005) for the following organism pairs of
          predictions have been loaded:
        </p>
        <ul>
          <li><I>D. melanogaster</I> : <I>A. gambiae</I></li>
          <li><I>D. melanogaster</I> : <I>C. elegans</I></li>
          <li><I>A. gambiae</I> : <I>C. elegans</I></li>
        </ul>
      </div>
    </TD>

    <TD width="45%" valign="top">
      <div class="heading2">
       Bulk download
      </div>
      <div class="body">
        <ul>
          <li>
            <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>A. gambiae</i> (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Orthologue.object Orthologue.subjectTranslation.gene Orthologue">
                <node path="Orthologue" type="Orthologue">
                </node>
                <node path="Orthologue.object" type="Gene">
                </node>
                <node path="Orthologue.object.organism" type="Organism">
                </node>
                <node path="Orthologue.object.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster">
                  </constraint>
                </node>
                <node path="Orthologue.subjectTranslation" type="Translation">
                </node>
                <node path="Orthologue.subjectTranslation.organism" type="Organism">
                </node>
                <node path="Orthologue.subjectTranslation.organism.name" type="String">
                  <constraint op="=" value="Anopheles gambiae str. PEST">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li> 
          <li>
            <im:querylink text="<i>D. melanogaster</i> gene identifiers and the
                                identifiers of predicted orthologues in
                                <i>A. gambiae</i> (for export/download)" skipBuilder="true">
              <query name="" model="genomic"
                     view="Orthologue.object.identifier Orthologue.object.organismDbId Orthologue.object.symbol Orthologue.subjectTranslation.gene.identifier Orthologue.subjectTranslation.gene.symbol">
                <node path="Orthologue" type="Orthologue">
                </node>
                <node path="Orthologue.object" type="Gene">
                </node>
                <node path="Orthologue.object.organism" type="Organism">
                </node>
                <node path="Orthologue.object.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster">
                  </constraint>
                </node>
                <node path="Orthologue.subjectTranslation" type="Translation">
                </node>
                <node path="Orthologue.subjectTranslation.organism" type="Organism">
                </node>
                <node path="Orthologue.subjectTranslation.organism.name" type="String">
                  <constraint op="=" value="Anopheles gambiae str. PEST">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li> 
        </ul>
        <ul>
          <li>
            <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>C. elegans</i> (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Orthologue.object Orthologue.subject Orthologue">
                <node path="Orthologue" type="Orthologue">
                </node>
                <node path="Orthologue.object" type="Gene">
                </node>
                <node path="Orthologue.object.organism" type="Organism">
                </node>
                <node path="Orthologue.object.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster">
                  </constraint>
                </node>
                <node path="Orthologue.subject" type="Gene">
                </node>
                <node path="Orthologue.subject.organism" type="Organism">
                </node>
                <node path="Orthologue.subject.organism.name" type="String">
                  <constraint op="=" value="Caenorhabditis elegans">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="<i>D. melanogaster</i> gene identifiers and the
                                identifiers of predicted orthologues in
                                <i>C. elegans</i> (for export/download)" skipBuilder="true">
              <query name="" model="genomic" 
                     view="Orthologue.object.identifier Orthologue.object.organismDbId Orthologue.object.symbol Orthologue.subject.identifier Orthologue.subject.organismDbId Orthologue.subject.symbol">
                <node path="Orthologue" type="Orthologue">
                </node>
                <node path="Orthologue.object" type="Gene">
                </node>
                <node path="Orthologue.object.organism" type="Organism">
                </node>
                <node path="Orthologue.object.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster">
                  </constraint>
                </node>
                <node path="Orthologue.subject" type="Gene">
                </node>
                <node path="Orthologue.subject.organism" type="Organism">
                </node>
                <node path="Orthologue.subject.organism.name" type="String">
                  <constraint op="=" value="Caenorhabditis elegans">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
        </ul>
        <ul>
          <li>
            <im:querylink text="Orthologues: <i>A. gambiae</i> vs <i>C. elegans</i> (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Orthologue.object Orthologue.subjectTranslation.gene Orthologue">
                <node path="Orthologue" type="Orthologue">
                </node>
                <node path="Orthologue.object" type="Gene">
                </node>
                <node path="Orthologue.object.organism" type="Organism">
                </node>
                <node path="Orthologue.object.organism.name" type="String">
                  <constraint op="=" value="Caenorhabditis elegans">
                  </constraint>
                </node>
                <node path="Orthologue.subjectTranslation" type="Translation">
                </node>
                <node path="Orthologue.subjectTranslation.organism" type="Organism">
                </node>
                <node path="Orthologue.subjectTranslation.organism.name" type="String">
                  <constraint op="=" value="Anopheles gambiae str. PEST">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="<i>A. gambiae</i> gene identifiers and the
                                identifiers of predicted orthologues in
                                <i>C. elegans</i> (for export/download)" skipBuilder="true">
              <query name="" model="genomic" 
                     view="Orthologue.object.identifier Orthologue.object.organismDbId Orthologue.object.symbol Orthologue.subjectTranslation.gene.identifier Orthologue.subjectTranslation.gene.symbol">
                <node path="Orthologue" type="Orthologue">
                </node>
                <node path="Orthologue.object" type="Gene">
                </node>
                <node path="Orthologue.object.organism" type="Organism">
                </node>
                <node path="Orthologue.object.organism.name" type="String">
                  <constraint op="=" value="Caenorhabditis elegans">
                  </constraint>
                </node>
                <node path="Orthologue.subjectTranslation" type="Translation">
                </node>
                <node path="Orthologue.subjectTranslation.organism" type="Organism">
                </node>
                <node path="Orthologue.subjectTranslation.organism.name" type="String">
                  <constraint op="=" value="Anopheles gambiae str. PEST">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
        </ul>
      </div>
    </TD>
  </TR>
</TABLE>
 

