/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.netviz;

import edu.ucsc.ib.client.BiodeSet;

/**
 * Interface for getting updates about the NetworkVisualization's working set of
 * biodes.
 */
public interface WorkingSetListener {
	/**
	 * Called by NetworkVisualization in response to additions to its working
	 * set.
	 * 
	 * @param b
	 *            the BiodeSet of additions
	 */
	public void addedBiodes(final BiodeSet b);

	/**
	 * Called by NetworkVisualization in response to removals from its working
	 * set.
	 * 
	 * @param b
	 *            the BiodeSet of nodes to remove
	 */
	public void removedBiodes(final BiodeSet b);
}
