package edu.ucsc.ib.client.rpc;


/**
 * AnnotationSearchResults is used to store results of an annotation search in
 * the form of a matrix, String[][].
 */
public class AnnotationSearchResults {
    /**
     * The total number of results that could have been returned
     */
    protected int totalCount;

    protected String[][] result;

    public AnnotationSearchResults() {
	totalCount = 0;
	result = null;
    }

    public AnnotationSearchResults(int totalCount, String[][] resultMatrix) {
	this.totalCount = totalCount;
	this.result = resultMatrix;
    }

    public int getTotalCount() {
	return totalCount;
    }

    /**
     * A matrix of results, one result per row, and in each row the first
     * element is the biode.
     * 
     * @return String[][] of results, as described above
     */
    public String[][] getResultMatrix() {
	return result;
    }
}
