/*
 * Interaction Browser
 * 
 * License to be determined.
 */
package edu.ucsc.ib.client.datapanels;

import edu.ucsc.ib.client.netviz.SelectedSetListener;
import edu.ucsc.ib.client.netviz.SourceSystemSpaceListener;
import edu.ucsc.ib.client.netviz.TrackSetListener;
import edu.ucsc.ib.client.netviz.WorkingSetListener;

/**
 * This is the interface for Panels that display information about the current
 * working set, or other data that has been loaded by the user.
 * 
 */
public interface DataPanel extends WorkingSetListener, SelectedSetListener,
		TrackSetListener, SourceSystemSpaceListener {
	/**
	 * Show/hide DataPanel.
	 * 
	 * @param isVisible
	 */
	public void setVisibility(boolean isVisible);
}
