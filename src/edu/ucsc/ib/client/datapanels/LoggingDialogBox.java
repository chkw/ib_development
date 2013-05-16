package edu.ucsc.ib.client.datapanels;

import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.MetanodeInfo;
import edu.ucsc.ib.client.netviz.Metanode;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.Track;
import edu.ucsc.ib.drawpanel.client.impl.DrawPanelImpl;

/**
 * This a dialog box for logging. It is intended to be used for debugging and
 * testing purposes.
 * 
 * @author cw
 * 
 */
public class LoggingDialogBox extends IbDialogBox implements DataPanel {
	private NetworkVisualization nv;

	private final FlexTable outermostFlexTable = new FlexTable();

	private static final VerticalPanel logvp = new VerticalPanel();

	private final ScrollPanel logScrollPanel = new ScrollPanel();
	{
		logScrollPanel.setWidth("40em");
		logScrollPanel.setHeight("25em");
		logScrollPanel.add(logvp);
	}

	/**
	 * Report various values
	 */
	private final Button reportButton = new Button("report",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					LoggingDialogBox.log("***** CLICKED REPORT BUTTON *****");
					// offsetHeight does not work on SVG stuff
					LoggingDialogBox.log("nv.getOffsetHeight(): "
							+ nv.getOffsetHeight());
					Widget nvParent = nv.getParent();
					LoggingDialogBox.log("nvParent.getOffsetHeight(): "
							+ nvParent.getOffsetHeight());
					LoggingDialogBox.log("size of BIC: "
							+ nv.BIC.getKeys().size());
					LoggingDialogBox.log("current nodes: "
							+ nv.getCurrentNodeIds().size());
					LoggingDialogBox.log("selected nodes: "
							+ nv.getSelectedNodeIds().size());

					LoggingDialogBox.log("size of MIC: "
							+ nv.MIC.getMetaNodeNames().size());
					for (String metanodeName : nv.MIC.getMetaNodeNames()) {
						MetanodeInfo mi = nv.MIC.getMetanodeInfo(metanodeName);
						Metanode mn = nv.getMetanode(metanodeName);
						LoggingDialogBox.log(mi.getSystematicName() + " : "
								+ mi.getBiodeSpace() + " : "
								+ mi.getSystemSpace() + " : "
								+ mn.getShapeType() + " : " + mn.getSize());
					}

					Set<String> edges = nv.getEdges();
					int i = 0;
					LoggingDialogBox.log("edge groups: " + edges.size());
					for (String edge : edges) {
						LoggingDialogBox.log(++i + " : " + edge);
					}

					LoggingDialogBox.log("tracks: " + nv.getTracks().size());
					int[] temp = nv.getDPdimensions();
					LoggingDialogBox.log("NetViz's DrawPanel dimensions: "
							+ temp[0] + " by " + temp[1]);
					LoggingDialogBox.log(nv.getDPsize());
					LoggingDialogBox.log("number unconnected nodes: "
							+ nv.getUnconnectedBiodes().size());
					LoggingDialogBox.log("agent is: "
							+ DrawPanelImpl.getUserAgent());
					LoggingDialogBox.log("*********************************");
				}
			});

	/**
	 * Clear the log
	 */
	private final Button clearButton = new Button("clear log",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					logvp.clear();
				}
			});

	private final HorizontalPanel buttonPanel = new HorizontalPanel();
	{
		buttonPanel.add(reportButton);
		buttonPanel.add(clearButton);
	}

	// ////////////////////////////////////////////////////////////

	public LoggingDialogBox(final NetworkVisualization nv) {
		super("message log");

		this.nv = nv;

		int row = 0;
		int col = 0;
		outermostFlexTable.setWidget(row, col, buttonPanel);
		outermostFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);

		row++;

		outermostFlexTable.setWidget(row, col, logScrollPanel);

		setWidget(outermostFlexTable);
	}

	private static String listBiodes(BiodeSet b) {
		StringBuffer sb = new StringBuffer();
		for (String biode : b) {
			sb.append(biode + " ");
		}
		return sb.toString().trim();
	}

	/**
	 * Log text to the logger.
	 * 
	 * @param text
	 *            String
	 */
	public static void log(String text) {
		logvp.add(new Label(text));
	}

	/**
	 * Add a widget to the log.
	 * 
	 * @param w
	 */
	public static void log(Widget w) {
		logvp.add(w);
	}

	@Override
	public void addedBiodes(BiodeSet b) {
		log("Logger: added " + b.size() + " biodes.");

		log(listBiodes(b));
	}

	@Override
	public void removedBiodes(BiodeSet b) {
		log("Logger: removed " + b.size() + " biodes");

		log(listBiodes(b));
	}

	@Override
	public void selectedBiodes(BiodeSet b) {
		log("Logger: selected " + b.size() + " biodes.");

		log(listBiodes(b));
	}

	@Override
	public void deselectedBiodes(BiodeSet b) {
		log("Logger: deselected " + b.size() + " biodes.");

		log(listBiodes(b));
	}

	@Override
	public void setVisibility(boolean isVisible) {
		if (isVisible) {
			show();
			logScrollPanel.scrollToBottom();
		} else {
			hide();
		}
	}

	@Override
	public void trackAdded(Track T) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trackRemoved(Track T) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sourceSystemSpaceChanged(String systemSpace) {
		// TODO Auto-generated method stub

	}

	@Override
	public void viewSystemSpaceChanged(String viewSystemSpace) {
		// TODO Auto-generated method stub

	}
}
