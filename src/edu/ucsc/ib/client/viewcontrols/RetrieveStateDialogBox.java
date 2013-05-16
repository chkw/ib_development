package edu.ucsc.ib.client.viewcontrols;

import java.util.HashMap;
import java.util.HashSet;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.Track;

/**
 * Controls for getting a saved state from the IB servlet.
 * 
 * @author cw
 * 
 */
public class RetrieveStateDialogBox extends IbDialogBox {

	public static final String URL_TO_RETRIEVE_SAVED_STATE = "data/savedStateDB/retrieve";

	/**
	 * Request for retrieving a saved state
	 */
	private Request saveStateCurrentRequest;

	private final NetworkVisualization nv;

	/**
	 * Provide a quick instruction for the dialogBox.
	 */
	private final Label instruction = new Label();
	{
		instruction
				.setText("Retrieve a previously-saved network by entering the saved state ID.");
	}

	/**
	 * Control for specifying the ID to retrieve.
	 */
	private final TextBox idTextBox = new TextBox();
	{
		this.idTextBox.setTitle("Enter an ID here.");
	}

	/**
	 * Submit request to IB's SavedStateDBService.
	 */
	private final Button submitButton = new Button("submit",
			new savedStateButtonClickHandler());

	// /////////////////////////////////////////////////////////

	public RetrieveStateDialogBox(NetworkVisualization netViz) {
		super("retrieve a saved state");

		this.nv = netViz;

		/**
		 * The outermost panel of the DialogBox.. this widget
		 */
		VerticalPanel outermostPanel = new VerticalPanel();
		this.setWidget(outermostPanel);

		outermostPanel.add(this.instruction);
		outermostPanel.add(this.idTextBox);
		outermostPanel.add(this.submitButton);
	}

	/**
	 * Submit an HTTP POST request to IB servlet to retrieve a saved state.
	 * 
	 * @param id
	 * @param nv
	 * @param req
	 */
	public static void getSavedState(final String id,
			final NetworkVisualization nv, Request currentRequest) {
		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		RequestCallback retrieveStateRequestCallback = new RequestCallback() {

			public void onError(Request request, Throwable exception) {
				LoggingDialogBox
						.log("RetrieveStateDialogBox: attempt to retrieve saved state has FAILED");
				LoggingDialogBox.log(exception.toString());
			}

			public void onResponseReceived(Request request, Response response) {
				// get JSONObject from response text
				LoggingDialogBox.log("got a response");

				JSONObject jo = JSONParser.parseStrict(response.getText())
						.isObject();

				reconstituteState(jo.get("savedJSON").isObject(), nv);
			}
		};

		String url = URL_TO_RETRIEVE_SAVED_STATE;
		RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, url);

		// required for posting form data
		// http://dev-answers.blogspot.com/2007/01/gwt-post-request-doesnt-include.html
		rb.setHeader("Content-type", "application/x-www-form-urlencoded");

		StringBuffer postData = new StringBuffer();
		postData.append(URL.encode("saveStateID")).append(URL.encode("="))
				.append(URL.encode(id));

		try {
			LoggingDialogBox.log("Beginning request:\t" + url);
			currentRequest = rb.sendRequest(postData.toString(),
					retrieveStateRequestCallback);
			LoggingDialogBox.log("request sent");
		} catch (RequestException re) {
			LoggingDialogBox.log("Unable to make request: " + re.getMessage());
		} catch (Exception e) {
			LoggingDialogBox.log("Unknown exeception: " + e.toString());
		}
	}

	/**
	 * Implementation of ClickHandler for submitting request to retrieve a saved
	 * state and reconstituting the response.
	 * 
	 * @author cw
	 * 
	 */
	private class savedStateButtonClickHandler implements ClickHandler {
		@Override
		public void onClick(ClickEvent event) {
			String id = idTextBox.getText();

			getSavedState(id, nv, saveStateCurrentRequest);
		}
	}

	/**
	 * Read and display the nodes and tracks contained within the JSON object
	 * retrieved from SavedStateService.
	 * 
	 * @param jo
	 * @param nv
	 */
	public static void reconstituteState(final JSONObject jo,
			final NetworkVisualization nv) {

		// get BiodeInfo from JSONObject & display
		JSONArray biodeJA = (JSONArray) jo.get("biodeInfo");
		LoggingDialogBox.log(biodeJA.size() + " BiodeInfo's in save state.");

		HashMap<String, BiodeInfo> biodeInfoHashMap = new HashMap<String, BiodeInfo>();

		for (int i = 0; i < biodeJA.size(); i++) {
			JSONObject biodeInfoJO = biodeJA.get(i).isObject();
			BiodeInfo bi = new BiodeInfo(biodeInfoJO);

			biodeInfoHashMap.put(bi.getSystematicName(), bi);
		}

		nv.addBiodeInfoToBIC(biodeInfoHashMap);
		nv.addBiode(new BiodeSet(biodeInfoHashMap.keySet()));

		// get tracks from JSONOBject & display
		JSONArray trackJA = (JSONArray) jo.get("tracks");
		LoggingDialogBox.log(trackJA.size() + " tracks in save state.");

		HashSet<String> trackSet = nv.getTrackNamesHash();
		for (int i = 0; i < trackJA.size(); i++) {
			Track newTrack = new Track(new JSONTrackListing(
					(JSONObject) trackJA.get(i)), nv);
			if (!trackSet.contains(newTrack.getName())) {
				nv.addTrack(newTrack);
			}
		}
	}

}
