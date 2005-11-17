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
            <im:querylink text="H. sapiens : M. musculus" skipBuilder="true">
              <query name="" model="genomic" view="Orthologue Orthologue.objectTranslation Orthologue.subjectTranslation"/>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="CrossGenomeMatch for Mouse" skipBuilder="true">
              <query name="" model="genomic" view="CrossGenomeMatch CrossGenomeMatch.chromosomeLocation CrossGenomeMatch.targetLocatedSequenceFeatureLocation">
                <node path="CrossGenomeMatch" type="CrossGenomeMatch">
                </node>
                <node path="CrossGenomeMatch.targetOrganism" type="Organism">
                </node>
                <node path="CrossGenomeMatch.targetOrganism.abbreviation" type="String">
                 <constraint op="=" value="MM" description="" identifier="" code="A">
                 </constraint>
               </node>
             </query>         
            </im:querylink>
          </li>
          <li>
            <im:querylink text="CrossGenomeMatch for Opposum" skipBuilder="true">
              <query name="" model="genomic" view="CrossGenomeMatch CrossGenomeMatch.chromosomeLocation CrossGenomeMatch.targetLocatedSequenceFeatureLocation">
                <node path="CrossGenomeMatch" type="CrossGenomeMatch">
                </node>
                <node path="CrossGenomeMatch.targetOrganism" type="Organism">
                </node>
                <node path="CrossGenomeMatch.targetOrganism.abbreviation" type="String">
                 <constraint op="=" value="MD" description="" identifier="" code="A">
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
 

