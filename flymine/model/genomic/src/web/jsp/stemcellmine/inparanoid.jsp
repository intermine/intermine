<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<TABLE>
  <TR>
    <TD valign="top">
      <div class="heading2">
        Current Data
      </div>
      <div class="body">
        <p>
          Orthologue and paralogue relationships calculated by <A href="http://inparanoid.cgb.ki.se/index.html">inparanoid</A> (latest calculated 16th April 2005) for the following organism pairs have been loaded:
        </p>
        <ul>
          <li><I>H. sapiens</I> : <I>M. musculus</I></li>
        </ul>
        <p>
          Mouse Chain annotated by <A href="http://genome.ucsc.edu/">UCSC</A> 
        </p>
         <p>
          Opossum Chain annotated by <A href="http://genome.ucsc.edu/">UCSC</A> 
        </p>
      </div>
    </TD>

    <TD width="30%" valign="top">
      <div class="heading2">
        Datasets
      </div>
      <div class="body">
        <ul>
          <li>
            <im:querylink text="D. melanogaster : A. gambiae" skipBuilder="true">
              <query name="" model="genomic" view="Orthologue Orthologue.object Orthologue.subjectTranslation">
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
                  <constraint op="=" value="Anopheles gambiae sensu stricto">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="D. melanogaster : C. elegans" skipBuilder="true">
              <query name="" model="genomic" view="Orthologue Orthologue.object Orthologue.subject">
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
            <im:querylink text="A. gambiae : C. elegans" skipBuilder="true">
              <query name="" model="genomic" view="Orthologue Orthologue.object Orthologue.subjectTranslation">
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
                  <constraint op="=" value="Anopheles gambiae sensu stricto">
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
 

