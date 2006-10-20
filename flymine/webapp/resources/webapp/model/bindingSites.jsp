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
        <dl>
         <h4>   
           <a href="javascript:toggleDiv('hiddenDiv1');">
             <img id='hiddenDiv1Toggle' src="images/undisclosed.gif"/>
              <i>D. melanogaster</i>  - Regulatory elements ...
           </a>
         </h4>

<div id="hiddenDiv1" style="display:none;">

          <dt>
            Transcriptional  <i>cis</i>-regulatory modules (CRMs) for <i>D. melanogaster</i>
            from the <a href="http://redfly.ccr.buffalo.edu"> REDfly database </a>.
          </dt>
          <dd>Gallo et al (2006) Bioinformatics 22:381-383 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16303794">PubMed: 16303794 </a>) - REDfly: a Regulatory Element Database for <i>Drosophila</i>.
           </dd>


          <dt>
            Regulatory elements for <i>D. melanogaster</i> annotated by 
            <a href="http://flybase.bio.indiana.edu">FlyBase</a>.
           </dt>

<br/>

   </div>
          <h4>           
           <a href="javascript:toggleDiv('hiddenDiv2');">
             <img id='hiddenDiv2Toggle' src="images/undisclosed.gif"/>
              <i>D. melanogaster</i>  - Binding sites ...
           </a>
          </h4>

  <div id="hiddenDiv2" style="display:none;">

          <dt>
            DNase I footprints for <i>D. melanogaster</i> from the 
            <a href="http://www.flyreg.org"> DNase I footprint database (V2.0)</a>.
          </dt>
          <dd>
          Bergman et al (2005) Bioinformatics 21:1747-1749 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15572468">PubMed: 15572468</a>) - <i>Drosophila</i> DNase I footprint database: a systematic genome annotation of transcription factor binding sites in the fruitfly, <i>Drosophila melanogaster</i>.
          </dd>
          <dt>
            Protein binding sites for <i>D. melanogaster</i> annotated by 
            <a href="http://flybase.bio.indiana.edu">FlyBase</a>.
          </dt>
        </dl>
      </div>
     </div>
    </td>

    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">        
        <ul>
          <li>
            <im:querylink text="FlyReg binding site data" skipBuilder="true">
              <query name="" model="genomic"
                     view="TFBindingSite TFBindingSite.gene TFBindingSite.factor TFBindingSite.chromosomeLocation">
                <node path="TFBindingSite.evidence" type="DataSet">
                </node>
                <node path="TFBindingSite.evidence.title" type="String">
                  <constraint op="=" value="FlyReg data set">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="FlyReg binding site data for export" skipBuilder="true">
              <query name="" model="genomic" view="TFBindingSite.identifier TFBindingSite.length TFBindingSite.gene.identifier TFBindingSite.factor.identifier TFBindingSite.chromosome.identifier TFBindingSite.chromosomeLocation.start TFBindingSite.chromosomeLocation.end">
                <node path="TFBindingSite" type="TFBindingSite">
                </node>
                <node path="TFBindingSite.evidence" type="DataSet">
                </node>
                <node path="TFBindingSite.evidence.title" type="String">
                  <constraint op="=" value="FlyReg data set">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="FlyBase binding site data" skipBuilder="true">
              <query name="" model="genomic" view="BindingSite">
                <node path="BindingSite" type="BindingSite">
                </node>
                <node path="BindingSite.evidence" type="DataSet">
                </node>
                <node path="BindingSite.evidence.title" type="String">
                  <constraint op="=" value="FlyBase Drosophila melanogaster data set">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="FlyBase binding site data for export" 
                          skipBuilder="true">
              <query name="" model="genomic" view="BindingSite.identifier BindingSite.length BindingSite.chromosome.identifier BindingSite.chromosomeLocation.start BindingSite.chromosomeLocation.end">
                <node path="BindingSite" type="BindingSite">
                </node>
                <node path="BindingSite.evidence" type="DataSet">
                </node>
                <node path="BindingSite.evidence.title" type="String">
                  <constraint op="=" value="FlyBase Drosophila melanogaster data set">
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
