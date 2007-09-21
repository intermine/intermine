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
        <DL>
          <DT>DNase I footprints: <I>D. melanogaster</I></DT>
          <DD>
            DNase I footprints for <I>D. melanogaster</I> from the 
            <A HREF="http://www.flyreg.org" target="_new"> DNase I footprint database (V2.0)</A>.
            This data has been reported in Bergman, Carlson and Celniker (2005)
            Bioinformatics 21:1747-1749 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15572468" target="_new">PubMed: 15572468</A>). 
          </DD>
          <DT>Protein binding sites: <I>D. melanogaster</I></DT>
          <DD>
            Protein binding sites for <I>D. melanogaster</I> annotated by 
            <A HREF="http://flybase.bio.indiana.edu" target="_new">FlyBase</A>.
          </DD>
        </DL>
      </div>
    </TD>
    <TD width="40%" valign="top">
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
