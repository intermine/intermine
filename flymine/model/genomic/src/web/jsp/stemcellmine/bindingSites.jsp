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
        <DL>
          <DT>TFbinding sites: <I>H. sapiens</I></DT>
          <DD>
            TFbinding sites for <I>H. sapiens</I> annotated by 
            <A HREF="http://genome.ucsc.edu/cgi-bin/hgTables?command=start">UCSC</A>.
          </DD>
        </DL>
        <DL>
          <DT>TFBSCluster: <I>H. sapiens</I></DT>
          <DD>
            TFbinding sites for <I>H. sapiens</I> annotated by 
            <A HREF="http://hscl.cimr.cam.ac.uk/">HSCL</A>.
          </DD>
        </DL>
        <DL>
          <DT>TFBS Library: <I>H. sapiens</I> and <I>M. musculus</I></DT>
          <DD>
            TFBS Library containing above TFBSClusters in <I>H. sapiens</I> that are conserved in <I>M. musculus</I> annotated by 
            <A HREF="http://hscl.cimr.cam.ac.uk/">HSCL</A>.
          </DD>
        </DL>
      </div>
    </TD> 
 <TD width="30%" valign="top">
      <div class="heading2">
        Datasets
      </div>
      <div class="body">
        
        <ul>
          <li>
            <im:querylink text="TFbinding sites" skipBuilder="true">
              <query name="" model="genomic"
                     view="TFBindingSite TFBindingSite.gene TFBindingSite.factor TFBindingSite.chromosomeLocation"/>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="TFBSCluster" skipBuilder="true">
              <query name="" model="genomic"
                     view="TFBSCluster TFBSCluster.gene TFBindingSite.gene TFBindingSite.chromosomeLocation"/>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="TFBSLibrary" skipBuilder="true">
              <query name="" model="genomic"
                     view="NcConservedRegion NcConservedRegion.chromosomeLocation"/>
            </im:querylink>
          </li>
        </ul>
      </div>
   </TD> 
  </TR>
</TABLE>
