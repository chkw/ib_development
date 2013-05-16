package edu.ucsc.ib.client.netviz;

/**
 * Interface to notify changes in the source system space.
 * 
 * @author wongc
 * 
 */
public interface SourceSystemSpaceListener {

	/**
	 * Method for responding to source systemSpace changes.
	 * 
	 * @param systemSpace
	 *            This should be the NCBI tax ID of the new systemSpace.
	 */
	public void sourceSystemSpaceChanged(String systemSpace);

	/**
	 * Method for responding to view systemSpace changes.
	 * 
	 * @param viewSystemSpace
	 *            This should be the NCBI tax ID of the new systemSpace.
	 */
	public void viewSystemSpaceChanged(String viewSystemSpace);
}
