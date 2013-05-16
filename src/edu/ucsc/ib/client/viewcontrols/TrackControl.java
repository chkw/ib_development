/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.viewcontrols;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

import edu.ucsc.ib.client.netviz.Track;

/**
 * A control for selecting a track, or deselecting it.
 */
public class TrackControl extends Composite implements ClickHandler {
	private HorizontalPanel hp;

	private JSONTrackListing jsonTrack;

	private CheckBox trackControlCheckBox;

	private TrackControlPanel tcp;

	private Track track;

	/**
	 * Constructor for TrackControl. Each track should have a TrackContol so
	 * that it can be turned on/off with the associated checkbox.
	 * 
	 * @param jtl
	 *            JSONTrackListing
	 * @param tcp
	 *            TrackControlPanel
	 */
	public TrackControl(JSONTrackListing jtl, TrackControlPanel tcp) {
		hp = new HorizontalPanel();
		initWidget(hp);

		jsonTrack = jtl;

		trackControlCheckBox = new CheckBox(jsonTrack.getDisplayName());
		trackControlCheckBox.setTitle(jsonTrack.getDescription());
		trackControlCheckBox.addClickHandler(this);

		hp.add(trackControlCheckBox);

		/*
		 * double trScore = this.jsonTrack.getTrScore(); if
		 * (Double.toString(trScore).equalsIgnoreCase("NaN")) { // do nothing }
		 * else { this.hp.add(new Label("[trScore: " + trScore + "]")); }
		 */

		double linkCount = jsonTrack.getLinkCount();
		if (Double.toString(linkCount).equalsIgnoreCase("NaN")) {
			// do nothing
		} else {
			hp.add(new Label("[links: " + linkCount + "]"));
		}

		this.tcp = tcp;
		track = tcp.addTrackControlToMapping(jsonTrack.getTrackName(), this);

		// track has turned on
		if (track != null) {
			trackControlCheckBox.setValue(true);
		}
	}

	/**
	 * Get the JSONTrackListing for this TrackControl.
	 * 
	 * @return
	 */
	public JSONTrackListing getTrackListing() {
		return jsonTrack;
	}

	/**
	 * Get the Track object for this TrackControl. If the Track object doesn't
	 * exist yet, create it.
	 * 
	 * @return Track
	 */
	public Track getTrack() {
		// regulatory ones?
		if (track == null) {

			track = new Track(jsonTrack, tcp.getNetworkVisualization());

			// if (jsonTrack.isDirected()) {
			// track = new DirectedTrack(jsonTrack,
			// tcp.getNetworkVisualization());
			// } else {
			// track = new UndirectedTrack(jsonTrack,
			// tcp.getNetworkVisualization());
			// }
		}
		return track;
	}

	/**
	 * Turn on the track.
	 */
	public void turnOnTrack() {
		Track t = getTrack();
		tcp.getNetworkVisualization().addTrack(t);
	}

	/**
	 * Turn off the track.
	 */
	public void turnOffTrack() {
		tcp.getNetworkVisualization().removeTrack(getTrack());
	}

	/**
	 * Is this TrackControl's CheckBox checked ?
	 * 
	 * @return
	 */
	public boolean isChecked() {
		return trackControlCheckBox.getValue();
	}

	/**
	 * Set the "checked" status of this TrackControl's checkbox.
	 * 
	 * @param checked
	 *            boolean
	 */
	public void setChecked(boolean checked) {
		trackControlCheckBox.setValue(checked);
	}

	/**
	 * Get the tool-tip for this TrackControl.
	 * 
	 * @return
	 */
	public String getToolTip() {
		return trackControlCheckBox.getTitle();
	}

	/**
	 * Get the Name from this TrackControl's JSONTrackListing. The name is used
	 * as display name, not the trackName which is the name of the track table
	 * in the database.
	 * 
	 * @return
	 */
	public String getName() {
		return jsonTrack.getName();
	}

	/**
	 * Get the trScore from this TrackControl's JSONTrackListing.
	 * 
	 * @return
	 */
	public double getTrScore() {
		return jsonTrack.getTrScore();
	}

	@Override
	public void onClick(ClickEvent event) {
		if (isChecked()) {
			turnOnTrack();
			MainMenuBar.showNetsDash();
		} else {
			turnOffTrack();
		}
	}
}
