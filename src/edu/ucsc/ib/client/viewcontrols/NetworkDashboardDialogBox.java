/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.viewcontrols;

import java.util.HashMap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.datapanels.DataPanel;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkEdgeGroup;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.Track;
import edu.ucsc.ib.drawpanel.client.TrackLine;

/**
 * A legend for showing active tracks.
 */
public class NetworkDashboardDialogBox extends IbDialogBox implements DataPanel {

	public static final String CSS_CLASS = "ib-trackLegend";

	private final NetworkVisualization netViz;

	/**
	 * map CheckBox to Track object
	 */
	private static final HashMap<CheckBox, Track> checkBoxToTrackHashMap = new HashMap<CheckBox, Track>();

	/**
	 * This is where activated tracks are displayed. It also serves a legend for
	 * the track colors.
	 */
	private static final FlexTable trackFlexTable = new FlexTable();

	/**
	 * Contains trackFlexTable.
	 */
	private final ScrollPanel tracksTableScrollPanel = new ScrollPanel();
	{
		tracksTableScrollPanel.setWidth("40em");
		tracksTableScrollPanel.setHeight("10em");
		tracksTableScrollPanel.add(trackFlexTable);
	}

	/**
	 * Turns on all networks in the legend.
	 */
	private final Button allOnButton = new Button("Turn On All Networks",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					turnOnAllTracks();
				}
			});

	/**
	 * Turns off all networks in the legend, without removal from the legend.
	 */
	private final Button allOffButton = new Button("Turn Off All Networks",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					turnOffAllTracks();
				}
			});

	/**
	 * Remove inactive networks from the legend.
	 */
	private final Button removeInactiveButton = new Button(
			"Remove Inactive Networks", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					removeInactiveTracks();
				}
			});

	/**
	 * set the opacity of edges
	 */
	private final CheckBox faintEdgesCheckBox = new CheckBox("use faint edges");

	/**
	 * Contains buttons
	 */
	private final VerticalPanel buttonPanel = new VerticalPanel();
	{
		buttonPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		buttonPanel.add(faintEdgesCheckBox);
		buttonPanel.add(this.allOnButton);
		buttonPanel.add(this.allOffButton);
		buttonPanel.add(this.removeInactiveButton);
	}

	/**
	 * Outermost panel to be set as the DialogBox object's widget.
	 */
	private final FlexTable outermostFlexTable = new FlexTable();

	// ///////////////////////////////////////////////////////

	/**
	 * A legend for active tracks. Also has button to remove the track.
	 * 
	 * @param nv
	 */
	public NetworkDashboardDialogBox(NetworkVisualization nv) {
		super("Networks Dashboard");

		this.netViz = nv;
		this.netViz.addTrackSetListener(this);

		int row = 0;
		int col = 0;

		row++;

		outermostFlexTable.setWidget(row, col, tracksTableScrollPanel);
		outermostFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);

		row++;

		outermostFlexTable.setWidget(row, col, buttonPanel);
		outermostFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);

		faintEdgesCheckBox
				.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
					@Override
					public void onValueChange(ValueChangeEvent<Boolean> event) {
						boolean value = ((CheckBox) event.getSource())
								.getValue();
						int newOpacity;
						if (value) {
							newOpacity = (int) Math
									.floor(NetworkEdgeGroup.DEFAULT_GROUP_OPACITY / 4);
						} else {
							newOpacity = NetworkEdgeGroup.DEFAULT_GROUP_OPACITY;
						}
						netViz.setNetworkEdgeOpacity(newOpacity);
					}
				});

		this.setWidget(outermostFlexTable);
	}

	/**
	 * Get the row number in the activeTrackFlexTable for a track. If not found,
	 * return -1.
	 * 
	 * @param t
	 * @return
	 */
	private int getTrackLegendRowNumber(final Track t) {
		int result = -1;

		int rows = trackFlexTable.getRowCount();
		for (int i = rows - 1; i >= 0; i--) {
			CheckBox cb = (CheckBox) trackFlexTable.getWidget(i, 0);
			if (cb.getName().equalsIgnoreCase(t.getDisplayName())) {
				return i;
			}
		}

		return result;
	}

	/**
	 * Implements the response to track addition events.
	 */
	public void trackAdded(final Track t) {

		// If the track already has a row in the legend, don't create another
		// row for it. Instead, set the checkbox.
		int rowNumber = this.getTrackLegendRowNumber(t);
		if (rowNumber >= 0) {
			CheckBox cb = (CheckBox) trackFlexTable.getWidget(rowNumber, 0);
			cb.setValue(true, false);
			return;
		}

		int row = checkBoxToTrackHashMap.keySet().size();

		// Each row in the currentlyDisplayed FlexTable has 3 columns.
		// 0 - color swatch
		// 1 - track name
		// 2 - remove button

		// Set up the color legend
		AbsolutePanel swatch = new AbsolutePanel();
		DOM.setStyleAttribute(swatch.getElement(), "background", t.getColor());
		swatch.setPixelSize(32, (int) (TrackLine.DEFAULT_STROKE_WIDTH + 1));
		trackFlexTable.setWidget(row, 1, swatch);

		// Add the name
		trackFlexTable.setWidget(row, 2, new Label(t.getDisplayName()));

		// use checkbox for turning on/off
		CheckBox cb = new CheckBox();
		cb.setValue(true, false);
		cb.setName(t.getDisplayName());
		cb.setTitle("toggle " + t.getDisplayName() + " on/off");
		cb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {

				int rowNumber = getTrackLegendRowNumber(t);
				LoggingDialogBox.log(t.getDisplayName() + " is on row "
						+ rowNumber);

				if (rowNumber < 0) {
					return;
				}

				CheckBox source = (CheckBox) event.getSource();
				boolean value = source.getValue();
				if (!value) {
					netViz.removeTrack(t);
				} else {
					netViz.addTrack(t);
				}
			}
		});

		checkBoxToTrackHashMap.put(cb, t);

		trackFlexTable.setWidget(row, 0, cb);
	}

	/**
	 * Turn off all tracks.
	 * 
	 */
	protected void turnOffAllTracks() {
		int rows = trackFlexTable.getRowCount();
		for (int i = rows - 1; i >= 0; i--) {
			CheckBox cb = (CheckBox) trackFlexTable.getWidget(i, 0);
			cb.setValue(false, true);
		}
	}

	/**
	 * Turn on all tracks.
	 * 
	 */
	protected void turnOnAllTracks() {
		int rows = trackFlexTable.getRowCount();
		for (int i = rows - 1; i >= 0; i--) {
			CheckBox cb = (CheckBox) trackFlexTable.getWidget(i, 0);
			cb.setValue(true, true);
		}
	}

	/**
	 * Implements the response to track removal events.
	 */
	public void trackRemoved(Track t) {
		int rowNumber = this.getTrackLegendRowNumber(t);
		if (rowNumber >= 0) {
			CheckBox cb = (CheckBox) trackFlexTable.getWidget(rowNumber, 0);
			cb.setValue(false, false);
			return;
		}
	}

	/**
	 * Remove rows from the track legend where the CheckBox is unchecked.
	 */
	private void removeInactiveTracks() {
		int rows = trackFlexTable.getRowCount();
		for (int i = rows - 1; i >= 0; i--) {
			CheckBox cb = (CheckBox) trackFlexTable.getWidget(i, 0);
			if (cb.getValue()) {
				// it's checked, do nothing
			} else {
				// it's unchecked, remove it
				trackFlexTable.removeRow(i);
				checkBoxToTrackHashMap.remove(cb);
			}
		}
	}

	/**
	 * Get a HashMap of Track objects and whether they are switched on or not.
	 * 
	 * @return
	 */
	public static HashMap<Track, Boolean> getTrackStatus() {
		HashMap<Track, Boolean> result = new HashMap<Track, Boolean>();

		int rows = trackFlexTable.getRowCount();
		for (int i = rows - 1; i >= 0; i--) {
			CheckBox cb = (CheckBox) trackFlexTable.getWidget(i, 0);
			boolean isOn;
			if (cb.getValue()) {
				// it's checked
				isOn = true;
			} else {
				// it's unchecked
				isOn = false;
			}
			Track t = checkBoxToTrackHashMap.get(cb);
			result.put(t, isOn);
		}
		return result;
	}

	@Override
	public void addedBiodes(BiodeSet b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removedBiodes(BiodeSet b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectedBiodes(BiodeSet b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deselectedBiodes(BiodeSet b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setVisibility(boolean isVisible) {
		if (isVisible) {
			this.show();
		} else {
			this.hide();
		}
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
