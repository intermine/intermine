Plug-in LIKE

The calculations of like consist of two steps. The first one is the precalculation, the second one the runTime. 

Precalculation is used to calculate matrices in advance. There are two types of matrices. The commonMats contain the items genes share. The similarityMats contain the similarity ratings, where a 0 means the genes have nothing in common and a 100 means they share all their items). For each aspect (e.g. pathways) these two matrices are precalculated. They are stored in the db.

The runTime is run, when there is a users request. The corresponding matrices to the aspects the user wants to have a look at are loaded from the db. With this matrices the calculations for a total rating of common genes are done. The result is a list of genes common to the users genes with a rating. Also, all common items between the result genes and the users genes are shown.

To call the precalculations, run the method Precalculate.precalculate().
To call the runTime, run the class TestClass.java.

