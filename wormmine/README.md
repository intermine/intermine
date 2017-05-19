Sample gff3 source:

     <source name="celegans-gff3-gene" type="wormbase-gff3-core" dump="true">
         <property name="gff3.taxonId"           value="&c_elegans_taxon_id;"/>
         <property name="gff3.seqDataSourceName" value="WormBase"/>
         <property name="gff3.dataSourceName"    value="WormBase"/>
         <property name="gff3.seqClsName"        value="Chromosome"/>
         <property name="gff3.dataSetTitle"      value="WormBase C. elegans genomic annotations"/>
         <!--<property name="src.data.dir"           location="&datadir;/c_elegans_gff3"/>-->
	 <property name="src.data.dir"           location="&datadir;/test_gff3"/> 
        
	 <!-- 
	 Only GFF3 records with a third column matching this value 
	 are processed.  Case insensitive.
	 gene, mrna, cds, exon
	 -->
	 <property name="gff3.allowedClasses" 	 value="gene"/>
	 
	 <!-- 
	 This file maps IDs from GFF3 into a type compatible with other 
	 data sources
	 Format: key \t value. All other columns ignored 
	 -->
	 <property name="gff3.IDMappingFile"	 value="&datadir;/test_gff3/c_elegans.WS235.geneIDs.wb-gff3.tab"/> 
	 
	 <!-- 
	 Maps type names from GFF3 into recognized InterMine class names
	 Format: key \t value. All other columns ignored 
	 -->
	 <property name="gff3.typeMappingFile"	 value="&datadir;/test_gff3/typeMapping.tab"/>

    </source>


