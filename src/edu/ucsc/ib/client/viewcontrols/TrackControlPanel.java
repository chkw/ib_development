/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.viewcontrols;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.datapanels.DataPanel;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.Track;

/**
 * A DialogBox for viewing the current tracks on a network visualization, adding
 * tracks, and removing tracks.
 */
public class TrackControlPanel extends IbDialogBox implements DataPanel {

	private final FlexTable outermostFlexTable = new FlexTable();

	private static final String STOPPED_THROBBER = "notloading_16.png";

	private static final String LOADING_THROBBER = "loading_16.gif";

	/*
	 * organism
	 */
	private static String trackSpace;

	public static final String CSS_CLASS = "ib-biodeSetControl";

	public static final String CURRENTLY_DISPLAED_HEADER_CSS_CLASS = "ib-currentTracksHeaderCell";

	// public static final String[] CUSTOM_COLOR_LIST = { "red", "orange",
	// "green", "blue", "purple", "brown", "black", "aqua" };

	public static final String[] CUSTOM_COLOR_LIST = { "aqua", "black", "blue",
			"fuchsia", "gray", "grey", "green", "lime", "maroon", "navy",
			"olive", "purple", "red", "silver", "teal", "brown" };

	private final Image throbber;

	private Request currentRequest;

	private Request currentCustomRequest;

	/**
	 * This is a Tree widget that is used to display selectable tracks.
	 */
	private Tree trackTree;

	/**
	 * trackTree will be contained in this panel.
	 */
	private final VerticalPanel trackTreePanel = new VerticalPanel();

	/**
	 * ScrollPanel for the TreePanel of controls.
	 */
	private final ScrollPanel trackTreeScrollPanel = new ScrollPanel();
	{
		this.trackTreeScrollPanel.setHeight("25em");
		this.trackTreeScrollPanel.add(this.trackTreePanel);
	}

	private final NetworkVisualization nv;

	/**
	 * Panel to hold submitTrackForm.
	 */
	private final VerticalPanel customTrackFormPanel = new VerticalPanel();

	/**
	 * This is a TreeItem for customTrackFormPanel and custom TrackControl
	 * objects.
	 */
	private final TreeItem customTreeItem = new TreeItem("Custom");

	/**
	 * This is where track controls for custom tracks should go
	 */
	private final TreeItem customTracksTreeItem = new TreeItem(
			"Custom Networks");

	/**
	 * This is a TreeItem for Track Recommender TrackLinkControl objects.
	 */
	private final TreeItem trackRecommenderTreeItem = new TreeItem(
			"Network Recommender");

	/**
	 * Mapping from String trackName to TrackControls in the listing. Intended
	 * for active and inactive tracks.
	 */
	private final static Map<String, TrackControl> mapNameToTrackControl = new HashMap<String, TrackControl>();

	/**
	 * Mapping from String track name to Track objects. Intended for active
	 * tracks. Sort of like a track info center.
	 */
	private final Map<String, Track> mapTrackNameToTrack = new HashMap<String, Track>();

	private boolean developerMode;

	// TODO ////////////////////////////////////////////////////////////

	/**
	 * Constructor.
	 * 
	 * @param nv
	 *            the NetworkVisualization for this panel
	 * @param developerMode
	 *            TODO
	 * @param trackSpace
	 *            which set of tracks to browse for addition
	 * @param trackService
	 *            the TrackServiceAsync for creating new Track objects (so they
	 *            can access new data from the server)
	 */
	public TrackControlPanel(NetworkVisualization nv, boolean developerMode) {
		super("Network Controls");

		this.developerMode = developerMode;

		// stuff for custom tracks
		this.customTrackFormPanel.add(new TrackSubmitPanel(this));

		this.customTreeItem.addItem(this.customTrackFormPanel);

		this.requestCustomTrackUpdate();
		this.customTreeItem.addItem(this.customTracksTreeItem);
		// ////////////////////////

		// stuff for network recommender
		HorizontalPanel trPanel = new HorizontalPanel();
		trPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		trPanel.add(this.buildTrackRecommenderButton());
		trPanel.add(this.buildResetTracksButton());
		this.throbber = new Image();
		this.throbber.setUrl(STOPPED_THROBBER);
		trPanel.add(this.throbber);
		this.trackRecommenderTreeItem.addItem(trPanel);
		// ////////////////////////

		this.nv = nv;

		this.nv.addTrackSetListener(this);

		List<Track> currentTracks = nv.getTracks();
		for (int i = 0; i < currentTracks.size(); ++i) {
			trackAdded(currentTracks.get(i));
		}

		int row = 0;
		int col = 0;
		outermostFlexTable.setWidget(row, col, trackTreeScrollPanel);
		outermostFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);

		this.setWidget(outermostFlexTable);
	}

	/**
	 * Refresh the trackSpace by reloading tracks available for the selected
	 * organism.
	 */
	public void refreshTrackSpace() {
		this.setTrackSpace(trackSpace, this.currentRequest);
	}

	/**
	 * Set the currently displayed track space. This is usually just which
	 * organism to browse.
	 * 
	 * @param ncbiTaxId
	 * @param currentRequest
	 *            so a running request can be cancelled before sending new one
	 */
	public void setTrackSpace(String ncbiTaxId, Request currentRequest) {
		trackSpace = ncbiTaxId;

		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		RequestCallback rc = new RequestCallback() {
			public void onError(Request request, Throwable exception) {
				trackTreePanel.clear();
				trackTreePanel.add(new Label("Unable to complete request: "
						+ exception.getMessage()));
				throbber.setUrl(STOPPED_THROBBER);
			}

			public void onResponseReceived(Request request, Response response) {
				// DataVizPanel.log("got a response!");
				updateTrackControls(response.getText());
				throbber.setUrl(STOPPED_THROBBER);
			}
		};
		String urlStr = "data/trackdb/trackList?organism=" + trackSpace;

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlStr);
		try {
			// DataVizPanel.log("TrackControlPanel ... Beginning request:\t"
			// + urlStr);
			currentRequest = rb.sendRequest(null, rc);
			throbber.setUrl(LOADING_THROBBER);
			// DataVizPanel.log("request sent");
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
		}
	}

	/**
	 * Update the track controls using a stringified JSON object.
	 * 
	 * @param jsonResultString
	 */
	protected void updateTrackControls(String jsonString) {
		// trackTree is tree widget for navigating through a collection of
		// tracks.
		this.trackTree = new Tree();
		if (this.developerMode) {
			this.trackTree.addItem(this.trackRecommenderTreeItem);
		}
		this.trackTree.addItem(this.customTreeItem);

		// listing is a VerticalPanel where we'll put trackTree
		this.trackTreePanel.clear();
		this.trackTreePanel.add(trackTree);

		JSONObject mainJO = JSONParser.parseStrict(jsonString).isObject();

		JSONArray ja = mainJO.get("tracks").isArray();
		if (ja.size() > 0) {
			for (int i = 0; i < ja.size(); i++) {
				JSONObject joCategory = ja.get(i).isObject();

				String catName = joCategory.get("datatype").isString()
						.stringValue();

				JSONArray jaTracks = joCategory.get("categoryArray").isArray();
				TreeItem ti = new TreeItem(catName + "(" + jaTracks.size()
						+ ")");

				// TODO maybe use com.google.gwt.user.client.ui.Grid here
				// each category of tracks has one grid
				// each row in the grid is for one track
				// each column can have various attributes about the track

				FlexTable trackCatFlexTable = new FlexTable();

				trackCatFlexTable.setWidth("50em");
				trackCatFlexTable.setBorderWidth(1);

				for (int j = 0; j < jaTracks.size(); j++) {
					JSONObject joTrack = jaTracks.get(j).isObject();
					JSONTrackListing JSTL = new JSONTrackListing(joTrack);
					TrackControl tc = new TrackControl(JSTL, this);

					int column_number = 0;
					trackCatFlexTable.setWidget(j, column_number++, tc);

					String possible_pmid = JSTL.getPMID();
					if (!possible_pmid.equalsIgnoreCase("not found")) {
						trackCatFlexTable.setWidget(j, column_number++,
								this.createPubMedButton(possible_pmid));
					} else {
						// just increment the column_number
						column_number++;
					}

					trackCatFlexTable.setWidget(j, column_number++, new Label(
							JSTL.getDescription()));

					trackCatFlexTable.setWidget(j, column_number++, new Label(
							JSTL.getCategory()));

					trackCatFlexTable.setWidget(j, column_number++, new Label(
							JSTL.getNumLinks() + " total links"));

				}

				ti.addItem(trackCatFlexTable);
				this.trackTree.addItem(ti);
			}
		}
	}

	/**
	 * Create a button for going to PubMed website in a new browser window.
	 * 
	 * @param pmid
	 * @return
	 */
	private Button createPubMedButton(final String pmid) {
		Button b = new Button("PubMed", new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Window.open("http://www.ncbi.nlm.nih.gov/pubmed/" + pmid,
						"pubmed_window", null);
			}

		});

		b.setTitle("go to PubMed website");

		return b;
	}

	/**
	 * Gets the name of the current track space.
	 * 
	 * @return a String name of the track space
	 */
	public static String getTrackSpace() {
		return trackSpace;
	}

	/**
	 * Gets the observed NetworkVisualization
	 * 
	 * @return the NetworkVizualization that this object controls
	 */
	public NetworkVisualization getNetworkVisualization() {
		return this.nv;
	}

	/**
	 * Implements the response to track addition events from the Network
	 * Visualization. Required by TrackSetListener interface.
	 */
	public void trackAdded(Track t) {
		this.mapTrackNameToTrack.put(t.getName(), t);

		TrackControl tc = mapNameToTrackControl.get(t.getName());
		if (tc != null) {
			tc.setChecked(true);
		}
	}

	/**
	 * Implements the response to track removal events from the Network
	 * Visualization. Required by TrackSetListener interface.
	 */
	public void trackRemoved(Track t) {
		this.mapTrackNameToTrack.remove(t.getName());

		TrackControl tc = mapNameToTrackControl.get(t.getName());
		if (tc != null) {
			tc.setChecked(false);
		}
	}

	/**
	 * Attempt to turn on the named track. In order to be successful, the track
	 * should be available in the current trackspace.
	 * 
	 * @param trackName
	 */
	public static void turnOnTrack(final String trackName) {
		TrackControl tc = getTrackControl(trackName);
		if (tc != null) {
			tc.turnOnTrack();
		}
	}

	/**
	 * Get the Track object that is mapped from the trackName.
	 * 
	 * @param trackName
	 * @return 'null' if there is no mapping
	 */
	public Track getTrack(String trackName) {
		return this.mapTrackNameToTrack.get(trackName);
	}

	/**
	 * Get the TrackControl that has the trackName.
	 * 
	 * @param trackName
	 *            This is the name that looks like a path.
	 * @return
	 */
	public static TrackControl getTrackControl(String trackName) {
		return mapNameToTrackControl.get(trackName);
	}

	/**
	 * Add a TrackControl TrackControlPanel's mapping from track name to
	 * TrackControl.
	 * 
	 * @param trackName
	 *            String
	 * @param tc
	 *            TrackControl
	 * @return Track 'null' if track has not been turned on
	 */
	protected Track addTrackControlToMapping(String trackName, TrackControl tc) {
		mapNameToTrackControl.put(trackName, tc);
		return this.mapTrackNameToTrack.get(trackName);
	}

	protected void cancelPendingRequest() {
		if (this.currentRequest != null) {
			this.currentRequest.cancel();
			this.currentRequest = null;
		}
	}

	/**
	 * Changes the selectable tracks to match the appropriate system space.
	 * Required for implementations of SystemSpaceListener interface.
	 */
	public void sourceSystemSpaceChanged(String systemSpace) {
		this.setTrackSpace(systemSpace, currentRequest);
	}

	/**
	 * Reset the available tracks for the selected organism.
	 * 
	 * @return
	 */
	public Button buildResetTracksButton() {
		Button b = new Button("reset nets", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				refreshTrackSpace();
			}

		});
		return b;
	}

	/**
	 * Build a button that triggers the track recommender
	 * 
	 * @return
	 */
	public Button buildTrackRecommenderButton() {
		Button b = new Button("Network Recommender", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				// getTrackLinkCounts();
				BiodeSet selectedBiodes = new BiodeSet();

				// get selected biodes
				selectedBiodes = nv.getSelectedNodeIds();
				if (selectedBiodes.isEmpty()) {
					// no selected nodes
					boolean useAllBiodes = Window
							.confirm("You'll need to select some nodes to use as the query set.  Do you want to find "
									+ getTrackSpace()
									+ " networks for all nodes?");
					if (!useAllBiodes) {
						// do nothing
					} else {
						getNetworkRecommendations(nv.getCurrentNodeIds());
					}
				} else {
					getNetworkRecommendations(selectedBiodes);
				}
			}
		});
		return b;
	}

	private void getNetworkRecommendations(final BiodeSet selectedBiodes) {
		// some nodes selected
		BiodeSet submitBiodes = new BiodeSet();
		for (Iterator<String> iter = selectedBiodes.iterator(); iter.hasNext();) {
			String biodeID = iter.next();
			if (nv.BIC.getBiodeInfo(biodeID).getSystemSpace()
					.equalsIgnoreCase(getTrackSpace())) {
				submitBiodes.add(biodeID);
			}
		}
		if (submitBiodes.size() > 0) {
			queryTrackRecommenderService(submitBiodes, "modes", currentRequest);
		} else {
			Window.alert("No nodes to recommend networks for.  Perhaps try selecting different nodes or changing the organism.");
		}
	}

	/**
	 * Make request to TrackRecommenderDBService and pass off the response to be
	 * handled.
	 * 
	 * @param biodes
	 *            BiodeSet of biodeID's to use in query
	 * @param queryOption
	 *            use this to pick what version of TrackRecommender the service
	 *            should use
	 * @param currentRequest
	 */
	protected void queryTrackRecommenderService(BiodeSet biodes,
			String queryOption, Request currentRequest) {
		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		String csvBiodes = BiodeControlPanel.arrayToCommaSeparatedString(biodes
				.getArray());

		// DataVizPanel.log("cvsBiodes is: " + csvBiodes);

		RequestCallback rc = new RequestCallback() {
			public void onError(Request request, Throwable exception) {
				trackTreePanel.clear();
				trackTreePanel.add(new Label("Unable to complete request: "
						+ exception.getMessage()));
				throbber.setUrl(STOPPED_THROBBER);
			}

			public void onResponseReceived(Request request, Response response) {
				// DataVizPanel.log("got response: " + response.getText());
				// updateTrackControls(response.getText());
				handleTrackRecommenderResults(response.getText());
				throbber.setUrl(STOPPED_THROBBER);
			}
		};

		String urlStr;
		if (queryOption.equalsIgnoreCase("modes")) {
			urlStr = "data/trdb/tr6?organism=" + trackSpace + "&biodes="
					+ csvBiodes;
		} else {
			urlStr = null;
		}

		if (urlStr != null) {
			RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlStr);
			try {
				currentRequest = rb.sendRequest(null, rc);
				throbber.setUrl(LOADING_THROBBER);
				// DataVizPanel.log("request sent");
			} catch (Exception e) {
				LoggingDialogBox.log(e.toString());
			}
		} else {
			// do nothing
		}
	}

	/**
	 * Handle the response from Track Recommender service.
	 * 
	 * @param stringifiedJSONObject
	 */
	protected void handleTrackRecommenderResults(String stringifiedJSONObject) {
		// trackTree is tree widget for navigating through a collection of
		// tracks.
		this.trackTree = new Tree();
		if (this.developerMode) {
			this.trackTree.addItem(this.trackRecommenderTreeItem);
		}
		this.trackTree.addItem(this.customTreeItem);

		// listing is a VerticalPanel where we'll put trackTree
		this.trackTreePanel.clear();
		this.trackTreePanel.add(this.trackTree);

		JSONObject mainJO = JSONParser.parseStrict(stringifiedJSONObject)
				.isObject();

		JSONValue tracksJV = mainJO.get("tracks");
		if (tracksJV != null) {
			JSONArray tracksJA = tracksJV.isArray();
			if ((tracksJA.size() > 0)) {
				TreeItem ti = new TreeItem("Recommended Networks("
						+ tracksJA.size() + ")");

				// display additional information about networks
				FlexTable trackCatFlexTable = new FlexTable();

				trackCatFlexTable.setWidth("50em");
				trackCatFlexTable.setBorderWidth(1);

				for (int i = 0; i < tracksJA.size(); i++) {
					JSONObject trackJO = tracksJA.get(i).isObject();
					JSONTrackListing JSTL = new JSONTrackListing(trackJO);
					TrackControl tc = new TrackControl(JSTL, this);

					int column_number = 0;
					trackCatFlexTable.setWidget(i, column_number++, tc);

					String possible_pmid = JSTL.getPMID();
					if (!possible_pmid.equalsIgnoreCase("not found")) {
						trackCatFlexTable.setWidget(i, column_number++,
								this.createPubMedButton(possible_pmid));
					} else {
						// just increment the column_number
						column_number++;
					}

					trackCatFlexTable.setWidget(i, column_number++, new Label(
							JSTL.getDescription()));

					trackCatFlexTable.setWidget(i, column_number++, new Label(
							JSTL.getCategory()));

					trackCatFlexTable.setWidget(i, column_number++, new Label(
							JSTL.getNumLinks() + " total links"));
				}
				ti.addItem(trackCatFlexTable);
				this.trackTree.addItem(ti);
			}
		} else {
			this.trackTree.addItem(new Label("no recommendations"));
		}
	}

	/**
	 * Make request to get custom tracks for this session
	 * 
	 */
	protected void requestCustomTrackUpdate() {
		// cancel any running request
		if (currentCustomRequest != null) {
			currentCustomRequest.cancel();
			currentCustomRequest = null;
		}

		RequestCallback rc = new RequestCallback() {

			public void onError(Request request, Throwable exception) {
				LoggingDialogBox
						.log("Unable to complete request for custom tracks");
			}

			public void onResponseReceived(Request request, Response response) {
				String jsonString = response.getText();
				handleCustomTrackResults(jsonString);
			}

		};

		String urlStr = "data/customTrackDB/trackList";
		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlStr);
		try {
			currentCustomRequest = rb.sendRequest(null, rc);
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
		}
	}

	/**
	 * Update the customTracksTreeItem with the current custom track controls
	 * 
	 * @param stringifiedJSONObject
	 */
	private void handleCustomTrackResults(final String stringifiedJSONObject) {
		customTracksTreeItem.remove();
		customTracksTreeItem.removeItems();
		customTreeItem.addItem(customTracksTreeItem);

		JSONObject mainJO = (JSONObject) JSONParser
				.parseStrict(stringifiedJSONObject);

		JSONArray tracksJA = mainJO.get("tracks").isArray();
		if (tracksJA.size() > 0) {
			JSONObject trackJO;
			for (int i = 0; i < tracksJA.size(); i++) {
				trackJO = tracksJA.get(i).isObject();
				customTracksTreeItem.addItem(new TrackControl(
						new JSONTrackListing(trackJO), this));
			}
		} else {
			customTracksTreeItem.addItem("No custom networks were found.");
		}
	}

	public void viewSystemSpaceChanged(String viewSystemSpace) {
		// TODO Auto-generated method stub

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
}
