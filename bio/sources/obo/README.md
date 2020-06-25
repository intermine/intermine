# bio-sources/obo
A generic OBO data source which stores terms as OntologyTerm.

Set properties in project.xml as in the following example:
```
<source name="plant-trait-ontology" type="obo">
    <property name="src.data.file" location="/data/plant-trait-ontology/to.obo"/>
    <property name="ontology.prefix" value="TO"/>
    <property name="obo.ontology.name" value="Plant Trait Ontology"/>
    <property name="obo.ontology.url" value="http://www.obofoundry.org/ontology/to.html"/>
    <property name="obo.ontology.licence" value="https://creativecommons.org/licenses/by/4.0/"/>
</source>
```
