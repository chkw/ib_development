package edu.ucsc.ib.client.viewcontrols;

import java.util.HashMap;
import java.util.HashSet;

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
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.EntityScoreAgent;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.StaticTrack;

/**
 * A dialog box for filtering connected network nodes by uploaded node scores.
 * 
 * @author cw
 * 
 */
public class ScoredNodeFilterDialogBox extends IbDialogBox {
	public static final String EDGE_SCORE_METHOD_2 = "absNode_larger";
	public static final String EDGE_SCORE_METHOD_1 = "absNode_smaller";
	private static final String SAVE_SCORES_FORM_ACTION = "data/conceptScores/saveScores";
	private static final String GRAPH_FILTER_URL = "data/conceptScores/graphFilter2";
	private static final String EDGE_FILTER_URL = "data/conceptScores/edgeFilter";

	public static final String UPLOAD_NAME_PREFIX = "uploaded_";
	private static final int MAX_RESULTS_LIMIT = 200;

	/**
	 * store stats for available score sets
	 */
	private final static HashMap<String, HashMap<String, Double>> statsHashMap = new HashMap<String, HashMap<String, Double>>();

	private Request currentRequest;

	private NetworkVisualization nv;

	private EntityScoreAgent esa = null;

	private StaticTrack filterTrack = null;

	private final Image throbber = new Image();
	{
		throbberStop();
	}

	/**
	 * provide some instructions for user for node scores file
	 */
	private final Label instructionsLabel = new Label();
	{
		StringBuffer sb = new StringBuffer();

		sb.append("Upload a tab-delimited file of scores.");
		sb.append("  The first line in the file should be column names.");
		sb.append("  The remaining lines should begin with a concept ID followed by score values.");
		sb.append("  After a file of scores has been uploaded to the server, a column in the score file may be selected from the pick list at the right.");

		instructionsLabel.setText(sb.toString());
	}

	/**
	 * For save a file of score sets on the server.
	 */
	private final FormPanel uploadScoreFileFormPanel = new FormPanel();
	{
		uploadScoreFileFormPanel.setAction(SAVE_SCORES_FORM_ACTION);
		uploadScoreFileFormPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadScoreFileFormPanel.setMethod(FormPanel.METHOD_POST);

		// panel to contain form widgets
		VerticalPanel formWidgetsVP = new VerticalPanel();
		uploadScoreFileFormPanel.setWidget(formWidgetsVP);

		final FileUpload fileUploadWidget = new FileUpload();
		fileUploadWidget.setName("uploadFormElement");

		final Hidden speciesIdHidden = new Hidden("speciesID");
		speciesIdHidden.setVisible(false);

		formWidgetsVP.add(fileUploadWidget);
		formWidgetsVP.add(speciesIdHidden);

		uploadScoreFileFormPanel.addSubmitHandler(new SubmitHandler() {
			@Override
			public void onSubmit(SubmitEvent event) {
				LoggingDialogBox
						.log("ScoreNodeFilterDB.uploadScoreFileFormPanel: begin onSubmit");
				throbberStart();
				// filterRequestButton.setEnabled(false);
				speciesIdHidden.setValue(SearchSpaceControl.getSystemspace());
				LoggingDialogBox
						.log("ScoreNodeFilterDB.uploadScoreFileFormPanel: end onSubmit");
			}
		});

		uploadScoreFileFormPanel
				.addSubmitCompleteHandler(new SubmitCompleteHandler() {
					@Override
					public void onSubmitComplete(SubmitCompleteEvent event) {
						LoggingDialogBox
								.log("ScoreNodeFilterDB.uploadScoreFileFormPanel: begin onSubmitComplete");
						handleUploadScoreFileSubmitComplete(event);
						throbberStop();
						LoggingDialogBox
								.log("ScoreNodeFilterDB.uploadScoreFileFormPanel: end onSubmitComplete");
					}
				});
	}

	/**
	 * submits score file for uploading to server
	 */
	final Button uploadScoreFileButton = new Button("upload score file",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					uploadScoreFileFormPanel.submit();
				}
			});

	/**
	 * controls for step 1 of this widget
	 */
	final VerticalPanel step1panel = new VerticalPanel();
	{
		step1panel.add(instructionsLabel);
		step1panel.add(uploadScoreFileFormPanel);
		step1panel.add(uploadScoreFileButton);

		instructionsLabel.setWidth("250px");
	}

	/**
	 * for specifying score to filter on
	 */
	private final static ListBox scoreSetNamesListBox = new ListBox();
	{
		scoreSetNamesListBox.setTitle("select score to filter on");
		scoreSetNamesListBox.addItem("(none loaded)", "");
	}

	/**
	 * for specifying edge scoring method
	 */
	private final ListBox edgeScoringMethodListBox = new ListBox();
	{
		edgeScoringMethodListBox
				.setTitle("select edge score assignment method");
		edgeScoringMethodListBox.addItem(
				"of the 2 nodes, the score that is smaller magnitude",
				EDGE_SCORE_METHOD_1);
		edgeScoringMethodListBox.addItem(
				"of the 2 nodes, the score that is larger magnitude",
				EDGE_SCORE_METHOD_2);
		edgeScoringMethodListBox.setSelectedIndex(0);
	}

	/**
	 * for specifying filtering method
	 */
	private final ListBox filterMethodListBox = new ListBox();
	{
		filterMethodListBox.setTitle("select filtering method");
		int value = 0;
		filterMethodListBox.addItem("edges with top-scoring concepts", ""
				+ value++);
		filterMethodListBox.addItem(
				"edges with top-scoring concepts and scored neighbors", ""
						+ value++);
		filterMethodListBox
				.addItem(
						"edges with top-scoring concepts and neighbors with at least average score",
						"" + value++);
		filterMethodListBox.setSelectedIndex(value - 1);
	}

	/**
	 * for specifying max results
	 */
	private final TextBox maxResultsTextBox = new TextBox();
	{
		maxResultsTextBox.setValue("100", false);
		maxResultsTextBox.setWidth("2em");
		maxResultsTextBox.setTitle("set maximum number of results - up to "
				+ MAX_RESULTS_LIMIT);
		maxResultsTextBox
				.addValueChangeHandler(new ValueChangeHandler<String>() {
					@Override
					public void onValueChange(ValueChangeEvent<String> event) {
						// check for numeric values only
						TextBox tb = (TextBox) event.getSource();
						if (!BiodeControlPanel.isNumericString(tb.getValue())) {
							// not valid !
							tb.setValue("0", false);
						} else if (Double.parseDouble(tb.getValue()) < 0
								|| Double.parseDouble(tb.getValue()) > MAX_RESULTS_LIMIT) {
							tb.setValue("0", false);
						}
					}
				});
	}

	/**
	 * for specifying value of threshold
	 */
	private final TextBox thresholdValueTextBox = new TextBox();
	{
		thresholdValueTextBox.setValue("1", false);
		thresholdValueTextBox.setWidth("2em");
		thresholdValueTextBox
				.setTitle("set value of threshold (absolute value will be used)");
		thresholdValueTextBox
				.addValueChangeHandler(new ValueChangeHandler<String>() {
					@Override
					public void onValueChange(ValueChangeEvent<String> event) {
						// check for numeric values only
						TextBox tb = (TextBox) event.getSource();
						if (!BiodeControlPanel.isNumericString(tb.getValue())) {
							// not valid !
							tb.setValue("1", false);
						} else if (Double.parseDouble(tb.getValue()) < 0
								|| Double.parseDouble(tb.getValue()) > MAX_RESULTS_LIMIT) {
							tb.setValue("1", false);
						}
					}
				});
	}

	/**
	 * for selecting how to compare edge score to threshold value
	 */
	private final ListBox thresholdComparisonListBox = new ListBox();
	{
		thresholdComparisonListBox
				.setTitle("select whether to replace or append current set of nodes");
		thresholdComparisonListBox.addItem(
				"keep edges with scores greater than threshold value", "gt");
		thresholdComparisonListBox.addItem(
				"keep edges with scores less than threshold value", "lte");
		thresholdComparisonListBox.setSelectedIndex(0);
	}

	/**
	 * for selecting whether or not to clear away current nodes before adding
	 * new ones
	 */
	private final ListBox clearConceptsListBox = new ListBox();
	{
		clearConceptsListBox
				.setTitle("select whether to replace or append current set of nodes");
		clearConceptsListBox.addItem(
				"clear away current nodes before adding new ones", "replace");
		clearConceptsListBox.addItem(
				"append new nodes to current set of nodes", "append");
		clearConceptsListBox.setSelectedIndex(1);
	}

	/**
	 * submit request for filtering results
	 */
	final Button filterRequestButton = new Button("get filtering results",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					String scoreSet = getSelectedScoreSet();
					String filterMethod = getFilteringMethod();
					String edgeScoringMethod = getEdgeScoringMethod();
					double thresholdValue = getThresholdValue();
					String thresholdComparisonMethod = getThresholdComparisonMethod();
					HashSet<String> trackNamesHash = nv
							.getTrackNamesHash(false);

					getEdgeFilterResults(trackNamesHash, scoreSet,
							edgeScoringMethod, filterMethod, thresholdValue,
							thresholdComparisonMethod, currentRequest);
				}
			});
	{
		// inactive button until score set is uploaded
		// filterRequestButton.setEnabled(false);
	}

	/**
	 * controls for step 2 of this widget
	 */
	final FlexTable step2flexTable = new FlexTable();
	{

		int row = 0;
		int column = 0;
		step2flexTable.setBorderWidth(1);

		step2flexTable.setText(row, column++, "edge score assigned by...");
		step2flexTable.setWidget(row, column++, edgeScoringMethodListBox);

		row++;
		column = 0;

		// step2flexTable.setText(row, column++, "filter method");
		// step2flexTable.setWidget(row, column++, filterMethodListBox);
		//
		// row++;
		// column = 0;

		step2flexTable.setText(row, column++, "threshold value");
		step2flexTable.setWidget(row, column++, thresholdValueTextBox);

		row++;
		column = 0;

		step2flexTable.setText(row, column++, "threshold comparison");
		step2flexTable.setWidget(row, column++, thresholdComparisonListBox);

		row++;
		column = 0;

		// step2flexTable.setText(row, column++, "number of concepts");
		// step2flexTable.setWidget(row, column++, maxResultsTextBox);
		//
		// row++;
		// column = 0;

		step2flexTable.setText(row, column++, "scores to use");
		step2flexTable.setWidget(row, column++, scoreSetNamesListBox);

		row++;
		column = 0;

		step2flexTable.setText(row, column++, "append current graph?");
		step2flexTable.setWidget(row, column++, clearConceptsListBox);

		row++;
		column = 0;

		step2flexTable.setText(row, column++, "submit request");
		step2flexTable.setWidget(row, column++, filterRequestButton);
	}

	// TODO /////////////////////////////////////////////////////////////////

	/**
	 * Construct a dialog box for working with metanodes.
	 * 
	 * @param nv
	 */
	public ScoredNodeFilterDialogBox(NetworkVisualization netviz) {
		super("upload node scores");

		nv = netviz;

		FlexTable outermostPanel = new FlexTable();
		int row = 0;
		int column = 0;
		outermostPanel.setBorderWidth(1);
		setWidget(outermostPanel);

		outermostPanel.setWidget(row, column, step1panel);

		outermostPanel.setWidget(row, ++column, step2flexTable);

		outermostPanel.setWidget(++row, 0, throbber);
		outermostPanel.getFlexCellFormatter().setColSpan(row, 0, (column + 1));
		outermostPanel.getFlexCellFormatter().setVerticalAlignment(row, 0,
				HasVerticalAlignment.ALIGN_MIDDLE);
	}

	/**
	 * Submit HTTP request for filter results and handle the results.
	 * 
	 * @param trackNamesHash
	 * @param scoreSetName
	 * @param filteringMethod
	 * @param request
	 */
	protected void getEdgeFilterResults(final HashSet<String> trackNamesHash,
			final String scoreSetName, final String edgeScoringMethod,
			final String filteringMethod, final double thresholdValue,
			final String thresholdComparisonMethod, Request request) {

		throbberStart();

		// check for running request
		if (request != null) {
			request.cancel();
			request = null;
			LoggingDialogBox
					.log("cancelled a running request in getFilterResults");
			throbberStop();
		}

		RequestCallback rc = new RequestCallback() {

			@Override
			public void onResponseReceived(Request request, Response response) {
				// check for OK status code
				if (response.getStatusCode() != 200) {
					LoggingDialogBox
							.log("RequestCallback in GraphFilterDialogBox.getGraphFilterResults() did not get OK status(200).  Status code: "
									+ response.getStatusCode());
					throbberStop();
					return;
				}

				// expect a JSON-RPC compliant result
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						response.getText());

				// check for error message in JSON-RPC response
				if (jsonRpcResp.hasError()) {
					LoggingDialogBox.log("getGraphFilterResults got an error: "
							+ jsonRpcResp.getError().toString());
					throbberStop();
					return;
				}

				JSONObject resultJO = jsonRpcResp.getResult();

				// do something with results

				JSONArray edgesJA = resultJO.get("edges").isArray();

				String scoreSetName = resultJO.get("scoreSetName").isString()
						.stringValue();

				// TODO confirm number of returned edges with user
				GraphFilterResultsDialogBox confirmationDialogBox = new GraphFilterResultsDialogBox(
						edgesJA, scoreSetName);

				confirmationDialogBox.center();
				confirmationDialogBox.show();

				// processFilterResults(edgesJA, scoreSetName);

				throbberStop();
			}

			@Override
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox
						.log("getFilterResults: got error in request callback");
				throbberStop();
			}
		};

		if (trackNamesHash.size() == 0) {
			LoggingDialogBox.log("ScoredNodeFilterDialogBox: no networks!");
			throbberStop();
			return;
		}

		if (scoreSetName.isEmpty()) {
			LoggingDialogBox.log("ScoredNodeFilterDialogBox: no score set!");
			throbberStop();
			return;
		}

		StringBuffer querySB = new StringBuffer();
		querySB.append("scoreSetName=" + scoreSetName);
		querySB.append("&edgeScoringMethod=" + edgeScoringMethod);
		querySB.append("&method=" + filteringMethod);
		querySB.append("&thresholdValue=" + thresholdValue);
		querySB.append("&thresholdComparisonMethod="
				+ thresholdComparisonMethod);
		querySB.append("&networks=");
		for (String network : trackNamesHash) {
			querySB.append(network + ",");
		}
		querySB.deleteCharAt(querySB.lastIndexOf(","));

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
				EDGE_FILTER_URL + "?" + querySB.toString());

		// send request and specify the RequestCallback object
		try {
			request = rb.sendRequest(null, rc);
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
			throbberStop();
		}
	}

	public class GraphFilterResultsDialogBox extends DialogBox {

		final JSONArray edgesJA;
		final String scoreSetName;

		final Button cancelButton = new Button("cancel", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				dismiss();
			}
		});

		/**
		 * The text in this button will show the number of returned results.
		 */
		final Button continueButton = new Button("continue",
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						processFilterResults(edgesJA, scoreSetName);
						synchronizeSelectedScoreSet(scoreSetName);
						dismiss();
					}
				});

		final HorizontalPanel buttonPanel = new HorizontalPanel();
		{
			buttonPanel.add(continueButton);
			buttonPanel.add(cancelButton);
		}

		public GraphFilterResultsDialogBox(final JSONArray edgesJA,
				final String scoreSetName) {
			super(false, true);
			setText("Filtering Results");

			this.edgesJA = edgesJA;
			this.scoreSetName = scoreSetName;

			continueButton.setText(this.edgesJA.size()
					+ " edges returned.  Click to continue.");

			setWidget(buttonPanel);
		}

		public void dismiss() {
			hide();
		}
	}

	/**
	 * Synchronize the selected score set with NodeScoresDialogBox's control.
	 * 
	 * @param scoreSetName
	 */
	private void synchronizeSelectedScoreSet(final String scoreSetName) {
		NodeScoresDialogBox.setSelectedScoreSet_no_event(scoreSetName);
	}

	/**
	 * Get the JSON-RPC response from submitting score file (step 1). Create
	 * controls for selecting score set to use in step 2.
	 * 
	 * @param event
	 */
	protected void handleUploadScoreFileSubmitComplete(SubmitCompleteEvent event) {
		if (event.getResults().isEmpty()) {
			LoggingDialogBox
					.log("handleScoreDataSubmitComplete got a null result");
			return;
		}

		// expect a JSON-RPC compliant result
		JsonRpcResponse jsonRpcResp = new JsonRpcResponse(event.getResults());

		setAvailableScoreSets(jsonRpcResp);

		if (nv.getTracks().size() < 1) {
			// select superpathway if no networks selected and organism is human
			if (SearchSpaceControl.getSystemspace().equalsIgnoreCase("9606")) {
				TrackControlPanel.turnOnTrack("UCSC_Superpathway");
			} else {
				// show networks control panel if there are no active networks
				MainMenuBar.showNetsBrowse();
			}
		}
	}

	/**
	 * Set the available score sets.
	 * 
	 * @param jsonRpcResp
	 */
	public static void setAvailableScoreSets(final JsonRpcResponse jsonRpcResp) {
		// check for error message in JSON-RPC response
		if (jsonRpcResp.hasError()) {
			LoggingDialogBox
					.log("ScoreNodesFilterDialogBox.setAvailableScoreSets got an error: "
							+ jsonRpcResp.getError().toString());
			return;
		}

		JSONObject resultJO = jsonRpcResp.getResult();

		JSONArray availableScoreSetsJA = resultJO.get("savedScoreSets")
				.isArray();

		setAvailableScoreSets(availableScoreSetsJA);

		// update NodeScoresDialogBox
		NodeScoresDialogBox.setAvailableScoreSets(availableScoreSetsJA);
	}

	/**
	 * Clear score set selection control and set available score sets.
	 * 
	 * @param availableScoreSetsJA
	 */
	public static void setAvailableScoreSets(
			final JSONArray availableScoreSetsJA) {
		clearScoreSets();

		for (int i = 0; i < availableScoreSetsJA.size(); i++) {
			JSONObject jo = availableScoreSetsJA.get(i).isObject();

			String name = jo.get("name").isString().stringValue();
			double avg = jo.get("avg").isNumber().doubleValue();
			double std = jo.get("std").isNumber().doubleValue();

			// addScoreSet(name + " (avg:" + avg + " std:" + std + ")",
			// UPLOAD_NAME_PREFIX + name, avg, std);

			addScoreSet(name + " (avg:" + avg + " std:" + std + ")", name, avg,
					std);
		}
	}

	/**
	 * stop throbber
	 */
	private void throbberStop() {
		throbber.setUrl(BiodeSearchPanel.STOPPED_THROBBER);
	}

	/**
	 * active throbber
	 */
	private void throbberStart() {
		throbber.setUrl(BiodeSearchPanel.LOADING_THROBBER);
	}

	/**
	 * Get the selected score set name from the user-control. Returns null if
	 * nothing selected.
	 * 
	 * @return
	 */
	private String getSelectedScoreSet() {
		try {
			return scoreSetNamesListBox.getValue(scoreSetNamesListBox
					.getSelectedIndex());
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * clear out all the selectable score sets from the control
	 */
	private static void clearScoreSets() {
		scoreSetNamesListBox.clear();

		statsHashMap.clear();
	}

	/**
	 * add a score set name to the control
	 * 
	 * @param display
	 * @param controlValue
	 * @param avg
	 * @param std
	 */
	private static void addScoreSet(final String display,
			final String controlValue, final double avg, final double std) {
		scoreSetNamesListBox.addItem(display, controlValue);

		HashMap<String, Double> stats = new HashMap<String, Double>();
		statsHashMap.put(controlValue, stats);
		stats.put("avg", avg);
		stats.put("std", std);
	}

	/**
	 * Get the stats for a score set
	 * 
	 * @param name
	 * @return
	 */
	private HashMap<String, Double> getStats(String name) {
		return statsHashMap.get(name);
	}

	/**
	 * Get the selected filtering method from the user-control.
	 * 
	 * @return
	 */
	private String getFilteringMethod() {
		return filterMethodListBox.getValue(filterMethodListBox
				.getSelectedIndex());
	}

	/**
	 * Get the selected edge scoring method from the user-control.
	 * 
	 * @return
	 */
	private String getEdgeScoringMethod() {
		return edgeScoringMethodListBox.getValue(edgeScoringMethodListBox
				.getSelectedIndex());
	}

	/**
	 * Get the increment from the user-control.
	 * 
	 * @return
	 */
	private int getMaxResults() {
		return (int) Math
				.floor(Double.parseDouble(maxResultsTextBox.getValue()));
	}

	/**
	 * Get the threshold value from the user-control.
	 * 
	 * @return
	 */
	private Double getThresholdValue() {
		return Double.parseDouble(thresholdValueTextBox.getValue());
	}

	/**
	 * Get action to "replace" or "append" current set of nodes
	 * 
	 * @return
	 */
	private String getReplaceAppendNodes() {
		return clearConceptsListBox.getValue(clearConceptsListBox
				.getSelectedIndex());
	}

	/**
	 * Get whether to keep edges with scores "gt" or "lte" threshold value
	 * 
	 * @return
	 */
	private String getThresholdComparisonMethod() {
		return thresholdComparisonListBox.getValue(thresholdComparisonListBox
				.getSelectedIndex());
	}

	/**
	 * @param edgesJA
	 * @param scoreSetName
	 */
	public void processFilterResults(JSONArray edgesJA, String scoreSetName) {
		// EntityScoreAgent to keep netviz updated

		// TODO this should be synchronized with other ESA that might already
		// exist for this score set
		if (esa != null) {
			esa.removeSelfFromNetViz();
		}

		// TODO turn on new ESA
		esa = new EntityScoreAgent(scoreSetName, statsHashMap.get(scoreSetName)
				.get("avg"), statsHashMap.get(scoreSetName).get("std"), nv);

		esa.addSelfToNetViz();

		esa.setMode(EntityScoreAgent.SIZE_AND_FILL_MODE);

		// TODO prepare the staticTrack for edge data
		if (filterTrack != null) {
			nv.removeTrack(filterTrack);
		}

		JSONObject jo = new JSONObject();
		jo.put("name", new JSONString("testFilterTrack"));
		jo.put("color", new JSONString("lime"));
		JSONTrackListing jtl = new JSONTrackListing(jo);

		// create StaticTrack
		filterTrack = new StaticTrack(jtl, edgesJA, nv);

		// TODO add StaticTrack to nv
		nv.addTrack(filterTrack);

		// add concepts
		String[] conceptsArray = getConceptsFromJsonArray(edgesJA).toArray(
				new String[0]);

		LoggingDialogBox.log("num concepts from edgesJA: "
				+ conceptsArray.length);

		if (conceptsArray.length < 1) {
			throbberStop();
			return;
		}

		if (getReplaceAppendNodes().equalsIgnoreCase("replace")) {
			nv.removeBiodeSet(nv.getCurrentNodeIds());
		}

		LoggingDialogBox.log("send concepts to BUIP for lookup");

		BiodeUserInputPanel.processSubmissionWithLookupService_single_sp(
				SearchSpaceControl.getSystemspace(), conceptsArray, true);
	}

	/**
	 * Get a HashSet of concepts from a JSONArray of edges. Concepts are keyed
	 * "1" and "2" in JSONObjects.
	 * 
	 * @param edgesJA
	 * @return
	 */
	public static HashSet<String> getConceptsFromJsonArray(
			final JSONArray edgesJA) {
		HashSet<String> conceptsHashSet = new HashSet<String>();

		for (int i = 0; i < edgesJA.size(); i++) {
			JSONObject edgeJO = edgesJA.get(i).isObject();
			JSONString jsonString = edgeJO.get("1").isString();
			if (!JsonRpcResponse.isNullValue(jsonString)) {
				conceptsHashSet.add(jsonString.stringValue());
			}
			jsonString = edgeJO.get("2").isString();
			if (!JsonRpcResponse.isNullValue(jsonString)) {
				conceptsHashSet.add(jsonString.stringValue());
			}
		}
		return conceptsHashSet;
	}

	/**
	 * Select the score set, but do not trigger an event.
	 * 
	 * @param scoreSetName
	 */
	public static void setSelectedScoreSet_no_event(final String scoreSetName) {
		for (int i = 0; i < scoreSetNamesListBox.getItemCount(); i++) {
			if (scoreSetNamesListBox.getValue(i).equalsIgnoreCase(scoreSetName)) {
				scoreSetNamesListBox.setItemSelected(i, true);
			}
		}
	}
}
