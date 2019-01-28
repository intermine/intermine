The classes are automatically generated from the following json schemas: 
https://github.com/ISA-tools/isa-api/tree/master/isatools/schemas/isa_model_version_1_0_schemas/core

All the classes are obtain in 3 steps.

(1) "_schema" suffix is removed from the schema names e.g. "assay_schema.json" is renamed to "assay.json"
The re-factoring is done simultaneously for the all objects using:
	CodeUtils.clearJsonFileSuffix(new File("input_path.../"), new File("output_path.../"), "_schema");
	This step is needed since the schema names are used for the java classes names as well.
	
	
	
(2) Java classes are generated from the schemas using jsonschema2pojo project tools
	 (http://www.jsonschema2pojo.org/)
	 
	jsonschema2pojo -s source_dir -t target_dir -p ambit2.export.isa.v1_0.objects -E -S -da -R 
	 
	 
(3) Rename some classes which has inappropriate names:
		
		"ProcessSequence" is renamed to "Process"
		"ExecutesProtocol" is renamed to "Protocol"
		"Characteristic" is renamed to "MaterialAttribute"

	The re-factoring is done using:
	
		CodeUtils.renameJavaClass(null, new File(targetDir),"ProcessSequence", "Process");
		CodeUtils.renameJavaClass(null, new File(targetDir),"ExecutesProtocol", "Protocol");
		CodeUtils.renameJavaClass(null, new File(targetDir),"Characteristic", "MaterialAttribute");

	This re-factoring is needed since the jsonschema2pojo cannot guess all classes names to be logical as we expect.
	
	 