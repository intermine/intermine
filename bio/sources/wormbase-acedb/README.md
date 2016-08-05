Sample source invocation:

	<source name="wb-acedb-gene" type="wormbase-acedb" dump="true">
		<property name="src.data.dir" location="&datadir;/wormbase-acedb/gene/XML" />
		<!--
			This file maps intermine data classes to an XPath query.
		-->
		<property name="mapping.file"	
			value="&datadir;/wormbase-acedb/gene/mapping/wormbase-acedb-gene.properties"/> 
		<!-- 
		File specifying the primary keys to use for this source
		Usually this source's keys.properties file.  Keys must end in ".key"
		-->
		<property name="key.file" 
			value="../../bio/sources/wormbase-acedb/resources/wormbase-acedb_keys.properties"/>
		
		<!-- 
		This property specifies the intermine class type this source loads.
		Must use proper CamelCase
		-->
		<property name="source.class" value="Gene"/>
		
		<!--
		Optional.
		This specifies where the XML rejects file should go.  This file stores all XML records
		which could not be parsed
		-->
		<property name="rejects.file" 
			value="&datadir;/wormbase-acedb/gene/wormbase-acedb-gene-rejects.xml"/>
			
		
		<property name="data.set" value="AceDB XML (Gene)"/>
	</source> 


===Mapping file format===

Sample: 
primaryIdentifier		= /Variation/text()[1]
symbol					= /Variation/Name[1]/Public_name[1]/Variation_name[1]/text()[1]
if.naturalVariant		= /XPATH/... 

gene.primaryIdentifier 	= /Variation/Affects[1]/Gene[1]/Gene[1]/text()[1]

phenotypesObserved.primaryIdentifier 	= /Variation/Description[1]/Phenotype[1]/Phenotype

