AGDISTIS
========

AGDISTIS - Agnostic Named Entity Disambiguation. This projects aimes at delivering a framework for disambiguating a priori annotated named entities. To the best of our knowledge based upon HITS and any knowledge base we outperform the state-of-the-art algorithm AIDA.

Requirements 
========
 
General
========

Temporary you need the following files to work with the german DBpedia for example:
 * de_surface_forms.tsv   
 * mappingbased_properties_de.nt
 * disambiguations_de.nt  
 * redirects_transitive_de.nt
 * instance_types_de.nt   
 * specific_mappingbased_properties_de.nt
 * labels_de.nt

Those have to be in a directory that is encoded in the Java variable "dataDirectory".

Judgement Agreement
========

src/main/resources/jdbc.properties contains information about a MySQL database, you need to set this to your mysql database

We also deliver the dump called "ned.sql" 

This is used for judgement agreement

AIDA comparision
========
For running the AIDA comparision follow the instruction of installation on: https://github.com/yago-naga/aida

Adapt the following: settings/database_aida.properties accordingly.
