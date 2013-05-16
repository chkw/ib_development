package edu.ucsc.ib.client.netviz;

import edu.ucsc.ib.client.BiodeSet;

/**
 * Interface for getting updates about the NetworkVisualization's selected set of
 * biodes.
 */
public interface SelectedSetListener {
    /**
     * Called by NetworkVisualization in response to additions to its selected set.
     * 
     * @param b the BiodeSet of additions
     */
    public void selectedBiodes(BiodeSet b);

    /**
     * Called by NetworkVisualization in response to removals from its selected
     * set.
     * 
     * @param b the BiodeSet of nodes to remove
     */
    public void deselectedBiodes(BiodeSet b);
}