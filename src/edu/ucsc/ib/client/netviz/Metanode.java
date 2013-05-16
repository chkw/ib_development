/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.netviz;

import edu.ucsc.ib.client.MetanodeInfo;

/**
 * A metanode in the network. It may represent multiple entities. It may also
 * require link aggregation.
 */
public class Metanode extends BasicNode {

	/**
	 * This SIZE of this object is derived from the SIZE of the parent class.
	 */
	private final int SIZE = super.SIZE * 2;

	public Metanode(MetanodeInfo mi, NetworkVisualization netViz) {
		super(mi.getSystematicName(), mi.getCommonName(), mi
				.getSystematicName(), mi.getSystemSpace(), mi.getBiodeSpace(),
				netViz, NetworkVisualization.basicNodeMouseEventsHandler);
		
		super.setSize(this.SIZE);
	}
}
