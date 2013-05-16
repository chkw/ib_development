package edu.ucsc.ib.client.viewcontrols;

import java.util.HashMap;
import java.util.HashSet;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.Track;

/**
 * A dialog box for working with BioInt pathways.
 * 
 * @author cw
 * 
 */
public class IterativeBayesPathwayExpansionDialogBox extends IbDialogBox {
	private Request iterativeBayesCurrentRequest = null;

	private final NetworkVisualization nv;

	/**
	 * indicate running process
	 */
	private final Image throbber = new Image(BiodeSearchPanel.STOPPED_THROBBER);

	private JSONObject expansionResultsJO;

	/**
	 * for display of expansion results
	 */
	private final FlexTable expansionResultsFlexTable = new FlexTable();
	{
		expansionResultsFlexTable.setTitle("expansion results");
		expansionResultsFlexTable.setWidth("30em");
		expansionResultsFlexTable.setBorderWidth(1);
	}

	/**
	 * container panel for expansionResultsFlexTable
	 */
	private final ScrollPanel expansionResultsScrollPanel = new ScrollPanel(
			expansionResultsFlexTable);
	{
		expansionResultsScrollPanel.setHeight("150px");
		expansionResultsScrollPanel.setWidth("300px");
	}

	/**
	 * cutoff for server-side use
	 */
	private static final double REQUEST_SCORE_CUTOFF = 0.5;

	/**
	 * cutoff for client-side use
	 */
	private double results_score_cutoff = REQUEST_SCORE_CUTOFF;

	/**
	 * Change the displayed results by enforcing a cutoff value
	 */
	private final ValueChangeHandler<Boolean> cutoffRadioButtonChangeHandler = new ValueChangeHandler<Boolean>() {
		@Override
		public void onValueChange(ValueChangeEvent<Boolean> event) {
			RadioButton rb = (RadioButton) event.getSource();
			String text = rb.getText();

			if (event.getValue()) {
				if (text.equalsIgnoreCase("least")) {
					results_score_cutoff = REQUEST_SCORE_CUTOFF + 0.4;
				} else if (text.equalsIgnoreCase("fewer")) {
					results_score_cutoff = REQUEST_SCORE_CUTOFF + 0.2;
				} else {
					results_score_cutoff = REQUEST_SCORE_CUTOFF;
				}
			} else {
				// do nothing
			}
			LoggingDialogBox.log("cutoff set to: " + results_score_cutoff);
			displayExpansionResults(results_score_cutoff);
		}
	};

	// Make some radio buttons, all in one group.
	private final RadioButton leastResultsRadioButton = new RadioButton(
			"resultsCutoffRadioGroup", "least");
	private final RadioButton fewerResultsRadioButton = new RadioButton(
			"resultsCutoffRadioGroup", "fewer");
	private final RadioButton allResultsRadioButton = new RadioButton(
			"resultsCutoffRadioGroup", "all");
	{
		leastResultsRadioButton
				.addValueChangeHandler(cutoffRadioButtonChangeHandler);
		fewerResultsRadioButton
				.addValueChangeHandler(cutoffRadioButtonChangeHandler);
		allResultsRadioButton
				.addValueChangeHandler(cutoffRadioButtonChangeHandler);
	}

	/**
	 * containing panel for RadioButton objects used for setting result cutoff
	 */
	private final HorizontalPanel resultsCutoffRadioButtonHP = new HorizontalPanel();
	{
		resultsCutoffRadioButtonHP.add(leastResultsRadioButton);
		resultsCutoffRadioButtonHP.add(fewerResultsRadioButton);
		resultsCutoffRadioButtonHP.add(allResultsRadioButton);
	}

	/**
	 * contains network CheckBoxes
	 */
	private final VerticalPanel networkCheckBoxVP = new VerticalPanel();

	/**
	 * containing panel for networkCheckBoxVP
	 */
	private final ScrollPanel networkCheckBoxScrollPanel = new ScrollPanel(
			networkCheckBoxVP);
	{
		networkCheckBoxScrollPanel.setHeight("150px");
		networkCheckBoxScrollPanel.setWidth("200px");
	}

	/**
	 * For selecting organism
	 */
	private ListBox organismListBox = new ListBox();
	{
		String selectedOrganismID = SearchSpaceControl.getSystemspace();
		// fill with options
		for (String name : SearchSpaceControl.SOURCE_OPTIONS.keySet()) {
			String organismID = SearchSpaceControl.SOURCE_OPTIONS.get(name);
			organismListBox.addItem(name, organismID);
			if (organismID.equalsIgnoreCase(selectedOrganismID)) {
				organismListBox
						.setSelectedIndex(organismListBox.getItemCount() - 1);
			}
		}
	}

	/**
	 * Display IDs of selected nodes
	 */
	private final FlexTable queryIdsFlexTable = new FlexTable();
	{
		queryIdsFlexTable.setTitle("selected nodes");
		queryIdsFlexTable.setWidth("30em");
		queryIdsFlexTable.setBorderWidth(1);
	}

	/**
	 * container panel for queryIdsFlexTable
	 */
	private final ScrollPanel queryIdsScrollPanel = new ScrollPanel(
			queryIdsFlexTable);
	{
		queryIdsScrollPanel.setHeight("150px");
		queryIdsScrollPanel.setWidth("300px");
	}

	/**
	 * container panel for organism ListBox and query IDs ScrollPanel
	 */
	private final VerticalPanel querySetControlsVP = new VerticalPanel();
	{
		querySetControlsVP.add(organismListBox);
		querySetControlsVP.add(queryIdsScrollPanel);
	}

	/**
	 * select number of iterations
	 */
	private final ListBox iterationsListBox = new ListBox();
	{
		iterationsListBox.setTitle("select number of iterations");
		for (int i = 1; i <= 10; i++) {
			iterationsListBox.addItem(i + "", i + "");
		}
		iterationsListBox.setItemSelected(2, true);
	}

	/**
	 * contains query IDs
	 */
	private BiodeSet queryIdsBS = new BiodeSet();

	/**
	 * contains selected network names
	 */
	private HashSet<String> selectedNetworksHashSet = new HashSet<String>();

	/**
	 * The outermost panel, the object's widget.
	 */
	private final SimplePanel outermostPanel = new SimplePanel();

	/**
	 * Panel for buttons
	 */
	private final HorizontalPanel buttonPanel = new HorizontalPanel();

	/**
	 * Button for submitting this request to server.
	 */
	private final Button submitButton = new Button("submit",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					getIterativeBayesPathwayExpansionResults(
							getSelectedOrganismID(), queryIdsBS,
							selectedNetworksHashSet, getSelectedIterations(),
							REQUEST_SCORE_CUTOFF, iterativeBayesCurrentRequest);
				}
			});

	/**
	 * for creating nodes in the netviz
	 */
	private final Button createNodesButton = new Button("create nodes",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					// TODO Auto-generated method stub
					createNodes();
					dismiss();
				}
			});

	// TODO ///////////////////////////////////////////////////////////

	/**
	 * Construct a dialog box for working with BioInt pathways.
	 * 
	 * @param nv
	 */
	public IterativeBayesPathwayExpansionDialogBox(NetworkVisualization nv) {
		super("Iterative Bayes Pathway Expansion DialogBox");

		this.nv = nv;

		this.setWidget(outermostPanel);

		setupNetworkCheckBoxes(networkCheckBoxVP);

		this.buttonPanel.add(throbber);
		this.buttonPanel.add(submitButton);

		// add ChangeHandler to update query list based on selection
		organismListBox.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				String selectedOrganismID = getSelectedOrganismID();

				setQueryIds(selectedOrganismID);
			}
		});

		setQueryIds(getSelectedOrganismID());

		FlexTable panelsFlexTable = new FlexTable();
		panelsFlexTable.setBorderWidth(1);

		int row = 0;
		int col = 0;

		panelsFlexTable.setWidget(row, col, querySetControlsVP);
		panelsFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_LEFT,
				HasVerticalAlignment.ALIGN_TOP);

		col++;
		panelsFlexTable.setWidget(row, col, networkCheckBoxScrollPanel);
		panelsFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_LEFT,
				HasVerticalAlignment.ALIGN_TOP);

		col++;
		panelsFlexTable.setWidget(row, col, iterationsListBox);
		panelsFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_LEFT,
				HasVerticalAlignment.ALIGN_TOP);

		row++;

		panelsFlexTable.setWidget(row, 0, buttonPanel);
		// ...and set it's column span so that it takes up the whole row.
		panelsFlexTable.getFlexCellFormatter().setColSpan(row, 0, 3);
		panelsFlexTable.getCellFormatter().setAlignment(row, 0,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);

		row++;

		panelsFlexTable.setWidget(row, 0, resultsCutoffRadioButtonHP);
		panelsFlexTable.getFlexCellFormatter().setColSpan(row, 0, 3);
		panelsFlexTable.getCellFormatter().setAlignment(row, 0,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);

		row++;

		panelsFlexTable.setWidget(row, 0, expansionResultsScrollPanel);
		panelsFlexTable.getFlexCellFormatter().setColSpan(row, 0, 3);
		panelsFlexTable.getCellFormatter().setAlignment(row, 0,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);

		// expansionResultsScrollPanel

		this.outermostPanel.add(panelsFlexTable);
	}

	private void createNodes() {
		// TODO Auto-generated method stub
		BiodeSet bs = new BiodeSet();

		JSONArray scoresJA = this.getExpansionResultsJO()
				.get("expansionScores").isArray();

		String organismID = this.getExpansionResultsJO().get("query")
				.isObject().get("organism").isString().stringValue();

		for (int i = 0; i < scoresJA.size(); i++) {
			JSONObject jo = scoresJA.get(i).isObject();

			JSONValue jv = jo.get("query");
			if (jv.isNumber().doubleValue() == 1) {
				// skip query gene
				continue;
			}

			jv = jo.get("ID");
			String id = jv.isString().stringValue();

			jv = jo.get("score");
			double score = jv.isNumber().doubleValue();

			if (score < results_score_cutoff) {
				continue;
			}

			bs.add(id);
		}
		LoggingDialogBox.log(organismID);
		for (String biode : bs) {
			LoggingDialogBox.log(biode);
		}

		BiodeControlPanel.lookupAndAddBiodes(organismID, bs.getArray());
	}

	/**
	 * Display the expansion results.
	 * 
	 * @param cutoff
	 */
	private void displayExpansionResults(final double cutoff) {
		if (this.expansionResultsJO == null) {
			LoggingDialogBox.log("expansion results JSON is null");
			return;
		}

		LoggingDialogBox.log("cutoff: " + cutoff);

		// clear expansionResultsFlexTable
		expansionResultsFlexTable.removeAllRows();

		int row = 0;
		int col = 0;

		expansionResultsFlexTable.setText(row, col++, "ID");
		expansionResultsFlexTable.setText(row, col++, "score");

		JSONArray scoresJA = this.expansionResultsJO.get("expansionScores")
				.isArray();

		for (int i = 0; i < scoresJA.size(); i++) {
			JSONObject jo = scoresJA.get(i).isObject();

			JSONValue jv = jo.get("query");
			if (jv.isNumber().doubleValue() == 1) {
				// skip query gene
				continue;
			}

			jv = jo.get("ID");
			String id = jv.isString().stringValue();

			jv = jo.get("score");
			double score = jv.isNumber().doubleValue();

			if (score < cutoff) {
				continue;
			}

			row = expansionResultsFlexTable.getRowCount();

			col = 0;

			expansionResultsFlexTable.setText(row, col++, id);
			expansionResultsFlexTable.setText(row, col++, "" + score);
		}
	}

	/**
	 * Set the query ID controls by grabbing IDs in the system space.
	 * 
	 * @param selectedOrganismID
	 */
	private void setQueryIds(final String selectedOrganismID) {
		LoggingDialogBox.log("selected organism ID: " + selectedOrganismID);

		// get the query IDs
		queryIdsBS = nv.getNodeIdsInOrganism(selectedOrganismID, true);

		if (queryIdsBS.size() < 1) {
			queryIdsBS = nv.getNodeIdsInOrganism(selectedOrganismID, false);
		}

		// clear selectedNodesFT
		queryIdsFlexTable.removeAllRows();

		// fill selectedNodesFT
		int count = 0;
		if (queryIdsBS.size() > 0) {
			for (String biode : queryIdsBS) {
				int row = count / 5;
				int column = count % 5;

				String displayName = nv.BIC.getBiodeInfo(biode).getCommonName();
				if (displayName.equalsIgnoreCase("no alias")) {
					queryIdsFlexTable.setText(row, column, biode);
				} else {
					queryIdsFlexTable.setText(row, column, displayName);
				}
				count++;
			}
		} else {
			queryIdsFlexTable.setText(0, 0, "no nodes");
		}
	}

	/**
	 * Get the selected iterations, which is the value of the selected item in
	 * the ListBox.
	 * 
	 * @return
	 */
	private int getSelectedIterations() {
		int selectedIndex = iterationsListBox.getSelectedIndex();

		String selectedValue = iterationsListBox.getValue(selectedIndex);

		return Integer.parseInt(selectedValue);
	}

	/**
	 * Get the selected organism ID, which is the value of the selected item in
	 * the ListBox.
	 * 
	 * @return
	 */
	private String getSelectedOrganismID() {
		int selectedIndex = organismListBox.getSelectedIndex();

		String selectedValue = organismListBox.getValue(selectedIndex);

		return selectedValue;
	}

	/**
	 * Get pathway expansion results from iterative Bayes service. Saves the
	 * result object from the RPC-JSON response.
	 * 
	 * @param organism
	 * @param queryIds
	 * @param networks
	 * @param iterations
	 * @param cutoff
	 * @param request
	 */
	private void getIterativeBayesPathwayExpansionResults(
			final String organism, final HashSet<String> queryIds,
			final HashSet<String> networks, final int iterations,
			final double cutoff, Request request) {

		// cancel any running request
		if (request != null) {
			stopThrobber();
			request.cancel();
			request = null;
		}

		RequestCallback iterBayesRequestCallback = new RequestCallback() {
			@Override
			public void onResponseReceived(Request request, Response response) {
				stopThrobber();
				JSONObject jsonRpcResponseJO = JSONParser.parseStrict(
						response.getText()).isObject();

				// check for JSON-RPC error
				JSONValue jv = jsonRpcResponseJO.get("error");
				if (!jv.toString().equalsIgnoreCase("null")) {
					LoggingDialogBox
							.log("got an error from iterative bayes service");
					return;
				}

				// check for non-null result
				jv = jsonRpcResponseJO.get("result");
				if (jv.toString().equalsIgnoreCase("null")) {
					LoggingDialogBox
							.log("got a null result from iterative bayes service");
					return;
				}

				// set result
				setExpansionResultsJO(jv.isObject());

				// force value change, which triggers the display of results
				allResultsRadioButton.setValue(false, false);
				allResultsRadioButton.setValue(true, true);

				buttonPanel.add(createNodesButton);
			}

			@Override
			public void onError(Request request, Throwable exception) {
				stopThrobber();
				LoggingDialogBox
						.log("error with RequestCallback in IterativeBayesPathwayExpansionDialogBox.getIterativeBayesPathwayExpansionResults(): "
								+ exception.toString());
			}
		};

		StringBuffer querySB = new StringBuffer();
		querySB.append("organism=" + organism);
		querySB.append("&iterations=" + iterations);
		querySB.append("&cutoff=" + cutoff);

		if (queryIds == null || networks == null) {
			LoggingDialogBox.log("must specify query set and networks");
			return;
		}

		querySB.append("&queryset=");
		for (String id : queryIds) {
			querySB.append(id + ",");
		}
		querySB.deleteCharAt(querySB.lastIndexOf(","));

		querySB.append("&networks=");
		for (String network : networks) {
			querySB.append(network + ",");
		}
		querySB.deleteCharAt(querySB.lastIndexOf(","));

		LoggingDialogBox.log(querySB.toString());

		// setup request
		// http://localhost:8080/ib/data/iterativeBayesPathwayExpander/expand?organism=9606&iterations=5&cutoff=0.5&queryset=1017,1019,1029,1058,1059,1063,11200,1163,1387,2033,2099,2119,2305,2353,255626,2619,2931,3175,3304,332,3910,4313,4609,4751,4775,5347,5604,5925,595,6502,6667,675,7039,7515,891,898,9133,9212,994&networks=Ma05_15994924,Rieger04_15356296,Frasor04_14973112,human_biogrid_Affinity_Capture-Western
		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
				"data/iterativeBayesPathwayExpander/expand?"
						+ querySB.toString());

		// send request and specify the RequestCallback object
		try {
			request = rb.sendRequest(null, iterBayesRequestCallback);
			startThrobber();
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
		}
	}

	/**
	 * Create CheckBox objects for each of the networks displayed in the
	 * TrackLegend. Also, set the selected status to match the TrackLegend. That
	 * is, if it is selected in the legend, then it will be selected here.
	 * 
	 * @param vp
	 */
	private void setupNetworkCheckBoxes(VerticalPanel vp) {
		// clear out previous CheckBoxes
		vp.clear();

		final ValueChangeHandler<Boolean> vch = new ValueChangeHandler<Boolean>() {
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				String networkName = ((CheckBox) event.getSource()).getName();

				boolean isOn = event.getValue();

				if (isOn) {
					selectedNetworksHashSet.add(networkName);
				} else {
					selectedNetworksHashSet.remove(networkName);
				}
			}
		};

		// get tracks from the track legend
		final HashMap<Track, Boolean> trackStatusHashMap = NetworkDashboardDialogBox
				.getTrackStatus();

		// create controls
		for (final Track t : trackStatusHashMap.keySet()) {
			String trackName = t.getName();
			String trackDisplayName = t.getDisplayName();
			boolean isOn = trackStatusHashMap.get(t);

			CheckBox cb = new CheckBox();

			cb.setName(trackName);
			cb.setText(trackDisplayName);
			cb.setTitle("toggle " + trackDisplayName + " for query");
			cb.addValueChangeHandler(vch);
			cb.setValue(isOn, true);

			vp.add(cb);
		}
	}

	/**
	 * Set expansionResultsJO
	 * 
	 * @param jo
	 */
	private void setExpansionResultsJO(final JSONObject jo) {
		this.expansionResultsJO = jo;
	}

	/**
	 * get expansionResultsJO
	 * 
	 * @return
	 */
	private JSONObject getExpansionResultsJO() {
		return this.expansionResultsJO;
	}

	/**
	 * set the running throbber
	 */
	private void startThrobber() {
		this.throbber.setUrl(BiodeSearchPanel.LOADING_THROBBER);
	}

	/**
	 * set the stopped throbber
	 */
	private void stopThrobber() {
		this.throbber.setUrl(BiodeSearchPanel.STOPPED_THROBBER);
	}

	/**
	 * Calls DialogBox.hide().
	 */
	private void dismiss() {
		this.hide();
	}
}
