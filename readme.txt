This code contains several classes to achieve a number of functionalities. These functionalities have the objective of creating links between know-how as linked data and DBpedia, and to find decomposition links within the know-how dataset.
The application is assuming there is a SPARQL endpoint accessible supporting SPARQL 1.1 
This endpoint can be changed in the configuration file. Some functionalities have additional requirements.

(1) Lucene indexing functionality
This functionality is used to create lucene indexes. An index contains information about entities, in particular uri, label, category, parsed label according to NLP algorithm, uri of requirements, etc.
There are 4 types of indexes:
 - The complex entities (main entities) (used as SOURCE for the query functionality)
 - The primitive entities (bottom level entities) (used as TARGET for the query functionality)
 - The context index, which contains information specific to each page URL (and not for each entity), it is used not to recompute expensive calculations for multiple entities within the same process
 - The application index, which is used for the Web application, it contains information about each entity related to its visualization. For example information if an entity should be highlighted as it contains a link

(2) Lucene querying functionality
* REQUIRES the index files to use must be in the right directories. These directories can be configured in the configuration file.
It works in conjunction with the machine learning component to output decomposition links.
It requires not only the index files of SOURCE and TARGET, but also the context index, the manual annotations to create the training set of the classifier, and one of the computed features for the classifier is also based on the manual generated links by the wikiHow community.

(3) Evaluation functionality
This functionality provides a simple text based interface for the annotation of links as right or wrong.
The directory where to find the links to annotate is expected to be the same where the links have been created using the 'Lucene querying functionality' or the 'DBpedia links generation functionality'
Plus you need to specify the directories where to store the positive and the negative links already annotated.
This evaluation could be either manual (to annotate new links as right or wrong) or just automatic (in this last case, it just computes precision and recall of the links based on the positive and negative links found)
It can be done either for the Decomposition links, or for the DBpedia links

(4) Activity recognition functionality (ONLY EXPERIMENTAL)
This functionality provides a simple text interface. The text submitted to the system is parsed, divided into sentences and matched against the existing model of the processes. 
It returns the top level activity which matched the most steps with the submitted sentences. The idea being that it would provide a general explanation for the sequence of steps taken.

(5) DBpedia links generation functionality
* REQUIRES connection to a DBpedia lookup service
** This application is not launched through the main file, but it is run by class StartDBpediaIntegration.java
This functionality iterates through all the requirements and tries to match them with the DBpedia entities using the DBpedia lookup service.
It also tries to break down complex requirements into their sub-components.
Finally it identifies outputs by checking if there is a creation verb in the title of a process.
