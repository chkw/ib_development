package edu.ucsc.ib.client.viewcontrols;

import com.google.gwt.http.client.Request;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;

/**
 * previous functionality of this UI is now in ScoredNodeFilterDialogBox.
 * Keeping this class file for future UI.
 * 
 * @author cw
 * 
 */
public class GraphFilterDialogBox extends IbDialogBox {
	private Request currentRequest = null;

	private final NetworkVisualization nv;

	/**
	 * The outermost panel, the object's widget.
	 */
	private final VerticalPanel outermostPanel = new VerticalPanel();

	// TODO ///////////////////////////////////////////////////////////

	/**
	 * Construct a dialog box for expanding the graph via filtering of
	 * server-side network.
	 * 
	 * @param nv
	 */
	public GraphFilterDialogBox(NetworkVisualization nv) {
		super("Graph Filtering DialogBox");

		this.nv = nv;

		setWidget(outermostPanel);
	}
}
