/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client;

import java.util.HashMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabPanel;

import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.Track;
import edu.ucsc.ib.client.viewcontrols.BiodeControlPanel;
import edu.ucsc.ib.client.viewcontrols.JSONTrackListing;
import edu.ucsc.ib.client.viewcontrols.MainMenuBar;
import edu.ucsc.ib.client.viewcontrols.PathwayData;
import edu.ucsc.ib.client.viewcontrols.PathwayNameSearchDialogBox;
import edu.ucsc.ib.client.viewcontrols.RetrieveStateDialogBox;
import edu.ucsc.ib.client.viewcontrols.SearchSpaceControl;
import edu.ucsc.ib.client.viewcontrols.TrackControlPanel;

/**
 * Simple beginnings of NetworkVisualisation testing.
 * 
 */
public class NetVizTest implements EntryPoint {

	private Request savedStateCurrentRequest = null;
	private Request tracksCurrentRequest = null;
	private Request pathwayCurrentRequest = null;

	private static final String NETVIZ_WIDTH = "800px";
	private static final String NETVIZ_HEIGHT = "600px";

	private static final NetworkVisualization nv = new NetworkVisualization(
			NETVIZ_WIDTH, NETVIZ_HEIGHT);

	private static final SearchSpaceControl SSC = new SearchSpaceControl();

	private static BiodeControlPanel BCP;

	private static TrackControlPanel TCP;

	private static final TabPanel controlTabPanel = new TabPanel();

	private static MainMenuBar mainMenuBar;

	public void onModuleLoad() {

		// UI construction

		// fix FF3.6 incompatibility
		// firefox3compatibility();

		String queryString = getQueryString().substring(1);
		// get all parameter mappings
		HashMap<String, String> paramMap = new HashMap<String, String>();
		if (queryString != null) {
			// LoggingDialogBox.log("found query string: " + queryString);
			paramMap = getQueryStringParameterMapping(queryString);
		}

		// developer mode has access to additional menu items
		boolean developer_mode = false;
		if (paramMap.containsKey("logger")
				&& paramMap.get("logger").equalsIgnoreCase("on")) {
			developer_mode = true;
		}

		BCP = new BiodeControlPanel(SSC, nv, developer_mode);
		TCP = new TrackControlPanel(nv, developer_mode);
		mainMenuBar = new MainMenuBar(nv, BCP, TCP, developer_mode);

		HorizontalPanel topHP = new HorizontalPanel();
		topHP.add(mainMenuBar);
		// TODO hide it for now
		// topHP.add(SSC);

		FlexTable outermostFlexTable = new FlexTable();

		int row = 0;
		int col = 0;
		outermostFlexTable.setWidget(row, col, topHP);
		outermostFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_LEFT,
				HasVerticalAlignment.ALIGN_TOP);

		row++;

		outermostFlexTable.setWidget(row, col, nv);
		outermostFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_LEFT,
				HasVerticalAlignment.ALIGN_TOP);

		SearchSpaceControl.addSystemSpaceListener(BCP);
		SearchSpaceControl.addSystemSpaceListener(TCP);

		LoggingDialogBox.log("size of nv is " + nv.getOffsetWidth() + " x "
				+ nv.getOffsetHeight());

		// Add it to the root panel.
		// RootPanel.get("controlSlot").add(legendTabPanel); // #controlSlot in
		// the css

		RootPanel.get("netVizSlot").add(outermostFlexTable);

		// RootPanel.get("netVizSlot").add(tp); // #netVizSlot in the css

		if (queryString != null) {
			loadFromQueryString(paramMap);
		}

		// I tried clearing the query string, but it seems this is not a good
		// idea. The page keeps reloading if the query string is modified.

		LoggingDialogBox.log("This IB loaded from: " + this.getLocation());
	}

	/**
	 * Get mapping of url query string parameters.
	 * 
	 * @param queryString
	 * @return
	 */
	private static HashMap<String, String> getQueryStringParameterMapping(
			String queryString) {
		HashMap<String, String> resultHashMap = new HashMap<String, String>();

		String[] params = queryString.split("&");
		for (int i = 0; i < params.length; i++) {
			String[] paramStr = params[i].split("=", 2);
			resultHashMap.put(paramStr[0], paramStr[1]);
		}

		return resultHashMap;
	}

	/**
	 * Load the biodes and tracks specified in the query string.
	 * 
	 * @param paramMap
	 *            map of key-value pairs parsed from the url query string.
	 */
	private void loadFromQueryString(final HashMap<String, String> paramMap) {
		// test with this:
		// http://localhost:8080/ib/NetVizTest.html?organism=worm&biodes=lin-23,lin-39,F29C12.4,Y48E1B.5,ZC395.10,mrp-5,F54C9.6,exo-3,T09A5.5,ogt-1,lin-2,C27F2.10,bar-1,lin-35,efl-1,B0432.3,lin-7,T20B12.7,ubc-18,T01E8.6,prx-5&tracks=Weirauch08
		// DataVizPanel.log("queryString:\t" + queryStr);

		String organism = paramMap.get("organism");
		String csvBiodes = paramMap.get("biodes");
		String csvTracks = paramMap.get("tracks");
		String logger = paramMap.get("logger");
		String savedState = paramMap.get("savedState");
		String pathway = paramMap.get("pathway");

		String pathwayDataJSON = paramMap.get("pathwayData");

		if ((logger != null) && (logger.equalsIgnoreCase("on"))) {
			// include logger in UI
			mainMenuBar.addDeveloperMenuItems();
		} else {
			// leave logger out of UI
		}

		// set organism
		if (organism != null) {
			BCP.setSourceOrganism(organism);
		}

		// load biodes
		if (csvBiodes != null) {
			BiodeControlPanel
					.lookupAndAddBiodes(organism, csvBiodes.split(","));
		}

		// load tracks
		if (csvTracks != null) {
			this.turnOnTracks(csvTracks, tracksCurrentRequest);
		}

		// load saved state
		if (savedState != null) {
			RetrieveStateDialogBox.getSavedState(savedState, nv,
					savedStateCurrentRequest);
		}

		// load pathway
		if (pathway != null) {
			PathwayNameSearchDialogBox.getPathwayData(pathway,
					pathwayCurrentRequest);
		}

		// TODO load pathwayData
		if (pathwayDataJSON != null) {

			// do some decoding
			String decodedString = URL.decodeQueryString(pathwayDataJSON);
			LoggingDialogBox.log("decodedString: " + decodedString);

			// get a JSON object
			JSONObject pathwayDataJO = null;
			try {
				pathwayDataJO = JSONParser.parseStrict(decodedString)
						.isObject();
			} catch (Exception e) {
				LoggingDialogBox.log(e.getMessage());
			}

			LoggingDialogBox.log("pathwayDataJO: " + pathwayDataJO.toString());

			// visualize pathway
			if (PathwayData.resultJoIsValid(pathwayDataJO)) {

				PathwayData pathwayData = new PathwayData(pathwayDataJO);

				pathwayData.visualize(nv);

			} else {
				LoggingDialogBox
						.log("invalid JSON for constructing a PathwayData object.");
			}

		}
	}

	/**
	 * Turn on tracks by first looking up track attributes from TrackDbService.
	 * 
	 * @param csvTrackList
	 * @param currentRequest
	 */
	private void turnOnTracks(String csvTrackList, Request currentRequest) {

		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		RequestCallback rc = new RequestCallback() {
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox
						.log("error turning on tracks from query string");
			}

			public void onResponseReceived(Request request, Response response) {
				// DataVizPanel.log("response: " + response.getText());

				JSONArray ja = ((JSONObject) JSONParser.parseStrict(response
						.getText())).get("trackAnnotations").isArray();
				if (ja.size() > 0) {
					for (int i = 0; i < ja.size(); i++) {
						JSONObject joTrackAnnot = ja.get(i).isObject();

						nv.addTrack(new Track(
								new JSONTrackListing(joTrackAnnot), nv));

						// if (joTrackAnnot.get("directional").isString()
						// .stringValue().equalsIgnoreCase("true")) {
						// nv.addTrack(new DirectedTrack(new JSONTrackListing(
						// joTrackAnnot), nv));
						// } else {
						// nv.addTrack(new UndirectedTrack(
						// new JSONTrackListing(joTrackAnnot), nv));
						// }
					}
				}
			}
		};
		String urlStr = "data/trackdb/trackAnnot?tracks=" + csvTrackList;

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlStr);
		try {
			// DataVizPanel.log("NetVizTest.turnOnTracks ... Beginning request:\t"
			// + urlStr);
			currentRequest = rb.sendRequest(null, rc);
			// DataVizPanel.log("request sent");
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
		}
	}

	/**
	 * Get the query string, if it exists. Uses "$doc.location.search".
	 * "window.location.search" does not work here.
	 * 
	 * @return
	 */
	private native String getQueryString() /*-{
											return $doc.location.search;
											}-*/;

	/**
	 * Get the URL. Uses "$doc.location". "window.location" does not work here.
	 * 
	 * @return
	 */
	private native String getLocation() /*-{
										return $doc.location;
										}-*/;

	/**
	 * Fix error <doc.getBoxObjectFor is not a function> in FF3.6.
	 * 
	 * http://stackoverflow.com/questions/1018997/gwt-javascript-exception-in-
	 * hosted-mode-result-of-expression-doc-getboxobjectfo
	 */
	private static native void firefox3compatibility() /*-{
														if (!$doc.getBoxObjectFor) {
														$doc.getBoxObjectFor = function (element) {
														var box = element.getBoundingClientRect();
														return { "x" : box.left, "y" : box.top, "width" : box.width, "height" : box.height, "screenX": box.left, "screenY":box.top };
														}
														}
														}-*/;
}
