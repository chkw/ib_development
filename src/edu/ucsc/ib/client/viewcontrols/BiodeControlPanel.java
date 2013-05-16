package edu.ucsc.ib.client.viewcontrols;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.StackPanel;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.datapanels.DataPanel;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.BasicNode;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.Track;

//extends DialogBox implements DataPanel {
public class BiodeControlPanel extends IbDialogBox implements DataPanel {

	private final FlexTable outermostFlexTable = new FlexTable();

	private static final String CSS_CLASS = "ib-biodeControlPanel";

	private final StackPanel stackPanel = new StackPanel();

	private final NetworkVisualization netviz;

	private final BiodeUserInputPanel buip;

	private final SearchSpaceControl ssc;

	private final BiodeGOPanel bgp;

	private Request currentRequest;

	// ////////////////////////////////////////////////////////////

	public BiodeControlPanel(final SearchSpaceControl ssc,
			final NetworkVisualization nv, boolean developerMode) {
		super("Concept Controls");

		this.ssc = ssc;
		this.netviz = nv;

		this.currentRequest = null;

		this.bgp = new BiodeGOPanel(this, this.netviz);

		this.stackPanel.add(new BiodeSearchPanel(this, this.netviz),
				"Add from Search");
		if (developerMode) {
			this.stackPanel.add(new BiodeRecommenderPanel(this, this.netviz),
					"Add Neighbors");
		}
		this.stackPanel.add(this.bgp, "Add a Set");

		this.buip = new BiodeUserInputPanel(this.ssc, this.netviz);
		this.stackPanel.add(this.buip, "User Input (clipboard)");

		int row = 0;
		int col = 0;
		outermostFlexTable.setWidget(row, col, this.stackPanel);
		outermostFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);

		this.setWidget(outermostFlexTable);
	}

	/**
	 * Get the SearchSpaceControl.
	 * 
	 * @return
	 */
	public SearchSpaceControl getSearchSpaceControl() {
		return this.ssc;
	}

	/**
	 * Get the NCBI tax ID for the selected organism system from the
	 * systemspaceList ListBox.
	 * 
	 * @return selected String value
	 */
	public String getSystemspace() {
		return SearchSpaceControl.getSystemspace();
	}

	/**
	 * Get the selected biode type from the systemspaceList ListBox.
	 * 
	 * @return selected String value
	 */
	public String getBiodespace() {
		return SearchSpaceControl.getBiodespace();
	}

	/**
	 * Append to the query string in the user input panel.
	 * 
	 * @param b
	 *            BiodeSet
	 */
	public void appendUserInputString(BiodeSet b) {
		this.buip.appendInputArea(b);
	}

	/**
	 * Set the biodespace and systemspace for a BiodeInfo object to match the
	 * selected options. Forces drugs to be chemicals.
	 * 
	 * @param bi
	 */
	public void setBiodeInfoSpaces(BiodeInfo bi) {
		this.ssc.setBiodeInfoSpaces(bi);
		if (bi.getSystemSpace().equalsIgnoreCase("drug")) {
			bi.setBiodeSpace("chemical");
		}
	}

	/**
	 * Set the biodespace and systemspace for the BiodeInfo objects in a HashMap
	 * of BiodeInfo objects to match the selected options. Forces drugs to be
	 * chemicals.
	 * 
	 * @param biodeHashMap
	 */
	public void setBiodeInfoSpaces(HashMap<String, BiodeInfo> biodeHashMap) {
		if (biodeHashMap.isEmpty()) {
			return;
		}

		for (String Biode : biodeHashMap.keySet()) {
			BiodeInfo bi = biodeHashMap.get(Biode);
			this.ssc.setBiodeInfoSpaces(bi);
			if (bi.getSystemSpace().equalsIgnoreCase("drug")) {
				bi.setBiodeSpace("chemical");
			}
		}
	}

	/**
	 * Required by SourceSystemSpaceListener.
	 */
	public void sourceSystemSpaceChanged(String systemSpace) {

		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		RequestCallback rc = new RequestCallback() {

			public void onResponseReceived(Request request, Response response) {
				if (response == null) {
					return;
				}

				// get the results
				LoggingDialogBox
						.log("BCP: non-null object from entitySetService");

				// result Object is actually a stringified JSONObject
				JSONObject resultJO = (JSONObject) JSONParser
						.parseStrict(response.getText());

				JSONArray ja = resultJO.get("sets").isArray();

				JSONObject jo;
				ArrayList<String> namesList = new ArrayList<String>();
				for (int i = 0; i < ja.size(); i++) {
					jo = ja.get(i).isObject();
					namesList.add(jo.get("name").isString().stringValue());
				}

				LoggingDialogBox.log("num names:\t" + namesList.size());
				for (Iterator<String> iter = namesList.iterator(); iter
						.hasNext();) {
					LoggingDialogBox.log(iter.next());
				}

				resetBGPDataListBox(namesList);
			}

			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("BCP: sourceSystemSpaceChanged failed");
				LoggingDialogBox.log("=====================================");
				LoggingDialogBox.log(exception.toString());
				LoggingDialogBox.log("=====================================");
			}
		};

		String urlString = "data/setsdb/setsList?organism="
				+ this.getSystemspace();

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
				urlString.toString());
		try {
			LoggingDialogBox.log("Beginning request:\t" + urlString);
			currentRequest = rb.sendRequest(null, rc);
			LoggingDialogBox.log("request sent");
		} catch (Exception e) {
			LoggingDialogBox.log("Unknown exeception: " + e.toString());
		}
	}

	/**
	 * Resets the BiodeGOPanel's dataListBox with the specified options.
	 * 
	 * @param dataSets
	 */
	public void resetBGPDataListBox(ArrayList<String> dataSets) {
		this.bgp.resetDataSetListBox(dataSets);
	}

	/**
	 * Get a HashMap of BiodeInfo objects from a JSONArray. The keys are biode.
	 * The values are BiodeInfo.
	 * 
	 * @param annotationsJA
	 *            JSONArray of JSONObjects that have BiodeInfo data
	 * @return
	 */
	static HashMap<String, BiodeInfo> jsonArrayToBiodeHash(
			JSONArray annotationsJA) {

		HashMap<String, BiodeInfo> results = new HashMap<String, BiodeInfo>();

		if (annotationsJA.size() > 0) {
			for (int i = 0; i < annotationsJA.size(); i++) {
				JSONObject jo = annotationsJA.get(i).isObject();

				BiodeInfo bi = new BiodeInfo(jo.get("ID").isString()
						.stringValue());

				bi.setCommonName(jo.get("common").isString().stringValue());

				bi.setDescription(jo.get("desc").isString().stringValue());

				results.put(bi.getSystematicName(), bi);
			}
		}
		return results;
	}

	/**
	 * Get a String of comma-separated values from an array of String
	 * 
	 * @param strArray
	 * @return
	 */
	public static String arrayToCommaSeparatedString(String[] strArray) {
		StringBuffer strBuf = new StringBuffer();
		for (int i = 0; i < strArray.length; i++) {
			if (strArray[i].length() > 0) {
				strBuf.append(strArray[i] + ",");
			}
		}
		return strBuf.toString();
	}

	/**
	 * Set the source organism.
	 * 
	 * @param organism
	 */
	public void setSourceOrganism(String organism) {
		this.ssc.setSourceSearchSpace(organism);
	}

	/**
	 * Look up and add entities.
	 * 
	 * @param organismID
	 *            may be something like "9606" for human.
	 * @param entityArray
	 */
	public static void lookupAndAddBiodes(String organismID,
			String[] entityArray) {

		BiodeUserInputPanel.processSubmissionWithLookupService_single_sp(
				organismID, entityArray, true);
	}

	/**
	 * Called when the view space has been changed.
	 */
	public void viewSystemSpaceChanged(String viewSystemSpace) {
		Collection<BasicNode> nodesList = this.netviz.getBasicNodes();

		for (BasicNode node : nodesList) {
			if (this.netviz.BIC.getBiodeInfo(node.getID()).getSystemSpace()
					.equalsIgnoreCase("drug")) {
				nodesList.remove(node);
			}
		}

		if (nodesList.size() <= 0) {
			LoggingDialogBox.log("no nodes!");
			return;
		}
		LoggingDialogBox.log("got collection of NetworkNodes: "
				+ nodesList.size());
		if (viewSystemSpace.equalsIgnoreCase("normal")) {
			// change back to original biodeID's and system space
			LoggingDialogBox.log("query for normal view");
			this.setNormalView();
		} else {
			// query for new biodeID's in the new view
			LoggingDialogBox.log("query for new view: " + viewSystemSpace);

			// get list of biodeID's for each system
			BiodeInfo bi;
			HashMap<String, ArrayList<String>> biodeHash = new HashMap<String, ArrayList<String>>();
			for (BasicNode node : nodesList) {
				bi = this.netviz.BIC.getBiodeInfo(node.getID());
				if (biodeHash.containsKey(bi.getSystemSpace())) {
					(biodeHash.get(bi.getSystemSpace())).add(bi
							.getSystematicName());
				} else {
					biodeHash.put(bi.getSystemSpace(), new ArrayList<String>());
					(biodeHash.get(bi.getSystemSpace())).add(bi
							.getSystematicName());
				}
			}

			// build up query string parameters
			JSONObject jo = new JSONObject();
			for (String systemSpace : biodeHash.keySet()) {
				// DataVizPanel.log("got systemSpace: " + systemSpace);
				JSONArray orgJA = new JSONArray();
				for (String biode : biodeHash.get(biodeHash)) {
					// DataVizPanel.log(systemSpace + ":" + biode);
					orgJA.set(orgJA.size(), new JSONString(biode));
				}
				jo.put(systemSpace, orgJA);
			}

			// cancel any running request
			if (currentRequest != null) {
				currentRequest.cancel();
				currentRequest = null;
			}

			// query the IB server
			RequestCallback rc = new RequestCallback() {

				public void onError(Request request, Throwable exception) {
					Window.alert("Request failed with exception: "
							+ exception.toString());
				}

				public void onResponseReceived(Request request,
						Response response) {
					Window.alert("Changing view systemspace.");
					if (response == null) {
						return;
					}
					JSONObject jsonResponse = (JSONObject) JSONParser
							.parseStrict(response.getText());
					changeView(jsonResponse);
				}
			};

			StringBuffer sb = new StringBuffer(
					"data/annodb/bestBlastProt?matchOrg=" + viewSystemSpace);
			sb.append("&biodeJSON=" + jo.toString());

			RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
					sb.toString());
			try {
				LoggingDialogBox.log("Beginning request:\t" + sb.toString());
				currentRequest = rb.sendRequest(null, rc);
				LoggingDialogBox.log("request sent");
			} catch (Exception e) {
				LoggingDialogBox.log("Unknown exeception: " + e.toString());
			}
		}
	}

	/**
	 * Set normal view
	 */
	private void setNormalView() {
		Collection<BasicNode> nodesList = this.netviz.getBasicNodes();
		if (nodesList.size() <= 0) {
			LoggingDialogBox.log("no nodes!");
			return;
		}
		// TODO set to normal view
	}

	/**
	 * Change the view.
	 * 
	 * @param jsonResponse
	 */
	protected void changeView(JSONObject jsonResponse) {
		// process JSON response
		String newSystemSpace = jsonResponse.get("matchOrg").isString()
				.stringValue();
		LoggingDialogBox.log("newSystemSpace: " + newSystemSpace);

		JSONArray mainResultsJA = jsonResponse.get("results").isArray();
		LoggingDialogBox.log("mainResultsJA size: " + mainResultsJA.size());

		// get nodes in a searchable structure .. by organism then biode
		Collection<BasicNode> nodesList = this.netviz.getBasicNodes();
		if (nodesList.size() <= 0) {
			LoggingDialogBox.log("no nodes!");
			return;
		}

		HashMap<String, HashMap<String, BasicNode>> biodeHash = new HashMap<String, HashMap<String, BasicNode>>();
		for (BasicNode node : nodesList) {
			BiodeInfo bi = this.netviz.BIC.getBiodeInfo(node.getID());
			if (biodeHash.containsKey(bi.getSystemSpace())) {
				(biodeHash.get(bi.getSystemSpace())).put(
						bi.getSystematicName(), node);

			} else {
				biodeHash.put(bi.getSystemSpace(),
						new HashMap<String, BasicNode>());

				(biodeHash.get(bi.getSystemSpace())).put(
						bi.getSystematicName(), node);
			}
			if (bi.getSystemSpace().equalsIgnoreCase(newSystemSpace)) {
				// TODO this is for case of already matching systemSpaces
			}
		}

		// iterate through results for each organism
		for (int organismIndex = 0; organismIndex < mainResultsJA.size(); organismIndex++) {
			JSONObject resultJO = mainResultsJA.get(organismIndex).isObject();

			String queryOrgID = resultJO.get("queryOrgID").isString()
					.stringValue();
			LoggingDialogBox.log("queryOrgID: " + queryOrgID);

			JSONArray orgResultsArray = resultJO.get("orgResultsArray")
					.isArray();
			LoggingDialogBox.log("orgResultsArray size: "
					+ orgResultsArray.size());

			HashMap<String, BasicNode> nodesHash = biodeHash.get(queryOrgID);

			// iterate through each result for this organism
			for (int biodeIndex = 0; biodeIndex < orgResultsArray.size(); biodeIndex++) {
				JSONObject biodeResultJO = orgResultsArray.get(biodeIndex)
						.isObject();
				// DataVizPanel.log("qqq got biodeResultJO " + j);

				String queryBiode = biodeResultJO.get("query").isString()
						.stringValue();
				// DataVizPanel.log("got queryBiode " + queryBiode);

				BasicNode node = nodesHash.get(queryBiode);
				// DataVizPanel.log("got node " + node.getBiode());

				BiodeInfo bi = this.netviz.BIC.getBiodeInfo(node.getID());
				// DataVizPanel.log("got biodeInfo " + bi.getSystematicName());

				// TODO update the view with new biode for the node

				LoggingDialogBox.log("node with redraw instructions: "
						+ queryBiode);
			}
		}

		int delay = 1000;
		this.netviz.selectAllBiodes();
		Timer t = new Timer() {
			public void run() {
				netviz.deselectAllBiodes();
			}
		};
		// Schedule the timer to run once in ? ms.
		t.schedule(delay);
	}

	@Override
	public void setVisibility(boolean isVisible) {
		if (isVisible) {
			show();
		} else {
			hide();
		}
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
	public void trackAdded(Track T) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trackRemoved(Track T) {
		// TODO Auto-generated method stub

	}

	/**
	 * Check if String can be parsed as a double.
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isNumericString(final String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
		// return s.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+");
	}
}
