# bio-sources/obo
A generic OBO data source which stores terms as OntologyTerm.

Set properties in project.xml as follows:
```
<source name="example-ontology" type="obo">
   <property name="src.data.file" location="/data/example-ontology/example.obo"/>
   <property name="ontology.prefix" value="EX"/>
   <property name="obo.ontology.name" value="Example Ontology"/>
</source>
```
