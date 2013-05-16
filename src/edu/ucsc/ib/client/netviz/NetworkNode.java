/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.netviz;

import edu.ucsc.ib.client.BiodeInfo;

/**
 * A node in the network. This is a node that gets its attributes from a
 * BiodeInfo object. It is a simple node that represents one entity. It does not
 * need any link aggregation. This is a child class of BasicNode.
 */
public class NetworkNode extends BasicNode {

	public NetworkNode(BiodeInfo bi, NetworkVisualization netViz) {
		super(bi.getSystematicName(), bi.getCommonName(), bi.getCommonName(),
				bi.getSystemSpace(), bi.getSystemSpace(), netViz,
				NetworkVisualization.basicNodeMouseEventsHandler);
	}

	public void redraw(BiodeInfo bi) {
		super.redraw(bi.getSystematicName(), bi.getCommonName(),
				bi.getCommonName(), bi.getSystemSpace(), bi.getSystemSpace());
	}
}
