<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<TABLE width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <p>
          Orthologue and paralogue relationships calculated by <A
          href="http://inparanoid.cgb.ki.se/index.html">InParanoid</A> (latest
          calculated 16th April 2005) between the following organisms:
        </p>
        <ul>
          <li><I>D. melanogaster</I></li>
          <li><I>D. pseudoobscura</I></li>
          <li><I>A. gambiae</I></li>
          <li><I>A. mellifera</I></li>
          <li><I>C. elegans</I></li>
        </ul>
        <p>
          In addition, orthologues from these five species to several others:
        </p>
        <p>
          <i>C. familiaris , D. rerio, G. gallus, H. sapiens, M. musculus, P. troglodytes, P. falciparum 3D7, R. norvegicus, S. cerevisiae, S. pombe, T. nigroviridis, T. rubripes</I>
        <p>
          <im:querylink text="Show all pairs of organisms linked by orthologues" skipBuilder="true">
            <query name="" model="genomic" view="Orthologue.object.organism.shortName Orthologue.subject.organism.shortName"><node path="Orthologue" type="Orthologue"></node></query>
          </im:querylink>
      </div>
    </td>

    <td width="40%" valign="top">
      <div class="heading2">
       Bulk download
      </div>
      <div class="body">
        <ul>
          <li>
            <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>A. gambiae</i> " skipBuilder="true">
              <query name="" model="genomic" view="Orthologue.object.identifier Orthologue.object.organismDbId Orthologue.object.symbol Orthologue.subject.identifier Orthologue.subject.organismDbId Orthologue.subject.symbol" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.object" type="Gene">
  </node>
  <node path="Orthologue.object.organism" type="Organism">
  </node>
  <node path="Orthologue.object.organism.name" type="String">
    <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Orthologue.subject" type="Gene">
  </node>
  <node path="Orthologue.subject.organism" type="Organism">
  </node>
  <node path="Orthologue.subject.organism.name" type="String">
    <constraint op="=" value="Anopheles gambiae str. PEST" description="" identifier="" code="B">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li> 

          <li>            
            <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>C. elegans</i> " skipBuilder="true">
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

          <li>
           <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>H. sapiens</i> " skipBuilder="true">
            <query name="" model="genomic" view="Orthologue.object.identifier Orthologue.object.organismDbId Orthologue.object.symbol Orthologue.subject.identifier Orthologue.subject.organismDbId Orthologue.subject.symbol" constraintLogic="A and B">
             <node path="Orthologue" type="Orthologue">
              </node>
             <node path="Orthologue.object" type="Gene">
              </node>
             <node path="Orthologue.object.organism" type="Organism">
              </node>
             <node path="Orthologue.object.organism.name" type="String">
                <constraint op="=" value="Drosophila melanogaster" code="A">
                </constraint>
              </node>
             <node path="Orthologue.subject" type="Gene">
              </node>
             <node path="Orthologue.subject.organism" type="Organism">
              </node>
             <node path="Orthologue.subject.organism.name" type="String">
                 <constraint op="=" value="Homo sapiens" code="B">
                 </constraint>
              </node>
             </query>
            </im:querylink>
           </li>

          <li>            
           <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>M. musculus</i> " skipBuilder="true">
            <query name="" model="genomic" view="Orthologue.object.identifier Orthologue.object.organismDbId Orthologue.object.symbol Orthologue.subject.identifier Orthologue.subject.organismDbId Orthologue.subject.symbol" constraintLogic="A and B">
             <node path="Orthologue" type="Orthologue">
              </node>
             <node path="Orthologue.object" type="Gene">
              </node>
             <node path="Orthologue.object.organism" type="Organism">
              </node>
             <node path="Orthologue.object.organism.name" type="String">
               <constraint op="=" value="Drosophila melanogaster" code="A">
               </constraint>
              </node>
             <node path="Orthologue.subject" type="Gene">
              </node>
             <node path="Orthologue.subject.organism" type="Organism">
              </node>
             <node path="Orthologue.subject.organism.name" type="String">
              <constraint op="=" value="Mus musculus" code="B">
              </constraint>
             </node>
            </query>
           </im:querylink>
          </li>

          <li>
          <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>D. pseudoobscura</i> " skipBuilder="true">
           <query name="" model="genomic" view="Orthologue.object.identifier Orthologue.object.organismDbId Orthologue.object.symbol Orthologue.subject.identifier Orthologue.subject.organismDbId Orthologue.subject.symbol" constraintLogic="A and B">
            <node path="Orthologue" type="Orthologue">
             </node>
            <node path="Orthologue.object" type="Gene">
             </node>
            <node path="Orthologue.object.organism" type="Organism">
             </node>
            <node path="Orthologue.object.organism.name" type="String">
             <constraint op="=" value="Drosophila melanogaster" code="A">
              </constraint>
            </node>
            <node path="Orthologue.subject" type="Gene">
            </node>
            <node path="Orthologue.subject.organism" type="Organism">
            </node>
            <node path="Orthologue.subject.organism.name" type="String">
             <constraint op="=" value="Drosophila pseudoobscura" code="B">
             </constraint>
            </node>
           </query>
          </im:querylink>
         </li>


          <li>
            <im:querylink text="Orthologues: <i>A. gambiae</i> vs <i>C. elegans</i> " skipBuilder="true">
              <query name="" model="genomic" view="Orthologue.object.identifier Orthologue.object.organismDbId Orthologue.object.symbol Orthologue.subject.identifier Orthologue.subject.organismDbId Orthologue.subject.symbol" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.object" type="Gene">
  </node>
  <node path="Orthologue.object.organism" type="Organism">
  </node>
  <node path="Orthologue.object.organism.name" type="String">
    <constraint op="=" value="Anopheles gambiae str. PEST" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Orthologue.subject" type="Gene">
  </node>
  <node path="Orthologue.subject.organism" type="Organism">
  </node>
  <node path="Orthologue.subject.organism.name" type="String">
    <constraint op="=" value="Caenorhabditis elegans" description="" identifier="" code="B">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>

        </ul>
      </div>
    </td>
  </tr>
</TABLE>
 

