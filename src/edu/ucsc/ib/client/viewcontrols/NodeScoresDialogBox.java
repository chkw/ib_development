package edu.ucsc.ib.client.viewcontrols;

import java.util.HashMap;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.EntityScoreAgent;
import edu.ucsc.ib.client.netviz.NetworkVisualization;

/**
 * A dialog box for working with user-submitted node scores. Only one active
 * score set at any time.
 * 
 * @author cw
 * 
 */
public class NodeScoresDialogBox extends IbDialogBox {
	private static final String SAVE_SCORES_FORM_ACTION = "data/conceptScores/saveScores";

	private static NetworkVisualization nv;

	private final static HashMap<String, EntityScoreAgent> esaHashMap = new HashMap<String, EntityScoreAgent>();

	private final Image throbber = new Image();
	{
		throbberStop();
	}

	/**
	 * currently selected visualization mode
	 */
	private int vizMode = 0;

	/**
	 * Update the selected visualization mode.
	 */
	private final ValueChangeHandler<Boolean> vizModeVCH = new ValueChangeHandler<Boolean>() {
		@Override
		public void onValueChange(ValueChangeEvent<Boolean> event) {
			RadioButton source = (RadioButton) event.getSource();
			String sourceLabel = source.getText();

			if (sourceLabel.equalsIgnoreCase("size and fill")) {
				vizMode = EntityScoreAgent.SIZE_AND_FILL_MODE;
			} else if (sourceLabel.equalsIgnoreCase("fill")) {
				vizMode = EntityScoreAgent.FILL_MODE;
			} else if (sourceLabel.equalsIgnoreCase("stroke")) {
				vizMode = EntityScoreAgent.STROKE_MODE;
			} else {
				vizMode = EntityScoreAgent.INACTIVE_MODE;
			}
		}
	};

	/**
	 * select "size and fill mode"
	 */
	private final RadioButton sizeAndFillRB = new RadioButton("vizModeGroup",
			"size and fill");

	/**
	 * Select "fill" mode
	 */
	private final RadioButton fillRB = new RadioButton("vizModeGroup", "fill");

	/**
	 * Select "stroke" mode
	 */
	private final RadioButton strokeRB = new RadioButton("vizModeGroup",
			"stroke");

	{
		sizeAndFillRB.addValueChangeHandler(vizModeVCH);
		fillRB.addValueChangeHandler(vizModeVCH);
		strokeRB.addValueChangeHandler(vizModeVCH);

		// set initially selected radio button
		fillRB.setValue(true, true);
	}

	/**
	 * Panel contains controls to select visualization mode.
	 */
	private final VerticalPanel vizModeControlPanel = new VerticalPanel();
	{
		vizModeControlPanel.add(sizeAndFillRB);
		vizModeControlPanel.add(fillRB);
		vizModeControlPanel.add(strokeRB);
	}

	/**
	 * for specifying score set. Should have an option to use default
	 * visualization (no score).
	 */
	private final static ListBox scoreSetNamesListBox = new ListBox();
	{
		scoreSetNamesListBox.setTitle("select score set to visualize");
		scoreSetNamesListBox.addItem("(none loaded)", "");
		scoreSetNamesListBox.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				ListBox source = (ListBox) event.getSource();

				int selectedIndex = source.getSelectedIndex();
				String selectedValue = source.getValue(selectedIndex);

				LoggingDialogBox.log("selected listbox value: " + selectedValue
						+ " at " + selectedIndex);

				EntityScoreAgent esa = getEntityScoreAgent(selectedValue);

				LoggingDialogBox.log("got esa for " + esa.getName());

				// TODO ? need to check if selected one has changed

				// TODO ? need to reset previous ESA's ?

				esa.addSelfToNetViz();

				// esa.setMode(EntityScoreAgent.SIZE_AND_FILL_MODE);
				esa.setMode(vizMode);

				synchronizeSelectedScoreSet(selectedValue);
			}
		});
	}

	/**
	 * For save a file of score sets on the server.
	 */
	private final FormPanel uploadScoreFileFormPanel = new FormPanel();
	{
		uploadScoreFileFormPanel.setAction(SAVE_SCORES_FORM_ACTION);
		uploadScoreFileFormPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadScoreFileFormPanel.setMethod(FormPanel.METHOD_POST);
		final FileUpload fileUploadWidget = new FileUpload();
		fileUploadWidget.setName("uploadFormElement");

		uploadScoreFileFormPanel.setWidget(fileUploadWidget);

		uploadScoreFileFormPanel.addSubmitHandler(new SubmitHandler() {
			@Override
			public void onSubmit(SubmitEvent event) {
				LoggingDialogBox
						.log("NodeScoresDialogBox.uploadScoreFileFormPanel: begin onSubmit");
				throbberStart();
				LoggingDialogBox
						.log("NodeScoresDialogBox.uploadScoreFileFormPanel: end onSubmit");
			}
		});

		uploadScoreFileFormPanel
				.addSubmitCompleteHandler(new SubmitCompleteHandler() {
					@Override
					public void onSubmitComplete(SubmitCompleteEvent event) {
						LoggingDialogBox
								.log("NodeScoresDialogBox.uploadScoreFileFormPanel: begin onSubmitComplete");
						handleUploadScoreFileSubmitComplete(event);
						throbberStop();
						LoggingDialogBox
								.log("NodeScoresDialogBox.uploadScoreFileFormPanel: end onSubmitComplete");
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
	 * Timer for cycling score set.
	 */
	private final Timer scoreSetTimer = new Timer() {
		@Override
		public void run() {
			selectNextScoreSet();
		}
	};

	/**
	 * control for cycling through score sets.
	 */
	private final CheckBox cycleScoresCheckBox = new CheckBox("cycle sets");
	{
		cycleScoresCheckBox.setTitle("cycle through score sets");
		cycleScoresCheckBox
				.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
					@Override
					public void onValueChange(ValueChangeEvent<Boolean> event) {
						if (event.getValue()) {
							scoreSetTimer.scheduleRepeating(1000);
						} else {
							scoreSetTimer.cancel();
						}
					}
				});
	}

	// TODO ///////////////////////////////////////////////////////////////

	/**
	 * Construct a dialog box for working with uploaded node score sets.
	 * 
	 * @param nv
	 */
	public NodeScoresDialogBox(NetworkVisualization netviz) {
		super("upload node scores");

		nv = netviz;

		final FlexTable outermostPanel = new FlexTable();
		setWidget(outermostPanel);

		int row = 0;
		int col = 0;

		Label instructionsLabel = new Label();
		{
			StringBuffer sb = new StringBuffer();

			sb.append("Upload a tab-delimited file of scores.");
			sb.append("  The first line in the file should be column names.");
			sb.append("  The remaining lines should begin with a concept ID followed by score values.");
			sb.append("  After a file of scores has been uploaded to the server, a column in the score file may be selected from the pick list at the right.");

			instructionsLabel.setText(sb.toString());
			instructionsLabel.setWidth("450px");
		}

		outermostPanel.setWidget(row, col, instructionsLabel);
		FlexCellFormatter cellFormatter = outermostPanel.getFlexCellFormatter();
		cellFormatter.setWordWrap(row, col, true);
		cellFormatter.setColSpan(row, col, 4);
		cellFormatter.setHorizontalAlignment(row, col,
				HasHorizontalAlignment.ALIGN_JUSTIFY);

		col = 0;

		outermostPanel.setWidget(++row, col, uploadScoreFileFormPanel);
		outermostPanel.setWidget(row, ++col, uploadScoreFileButton);
		outermostPanel.setWidget(row, ++col, throbber);
		outermostPanel.setWidget(row, ++col, vizModeControlPanel);
		outermostPanel.setWidget(row, ++col, scoreSetNamesListBox);
		// outermostPanel.setWidget(row, ++col, cycleScoresCheckBox);
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
	}

	/**
	 * update available score sets
	 * 
	 * @param jsonRpcResp
	 */
	private void setAvailableScoreSets(final JsonRpcResponse jsonRpcResp) {
		// check for error message in JSON-RPC response
		if (jsonRpcResp.hasError()) {
			LoggingDialogBox
					.log("NodeScoresDialogBox.setAvailableScoreSets got an error: "
							+ jsonRpcResp.getError().toString());
			return;
		}

		JSONObject resultJO = jsonRpcResp.getResult();

		JSONArray availableScoreSetsJA = resultJO.get("savedScoreSets")
				.isArray();

		// update control with available score sets
		setAvailableScoreSets(availableScoreSetsJA);

		// update scoreNodeFilterDialogBox
		ScoredNodeFilterDialogBox.setAvailableScoreSets(availableScoreSetsJA);
	}

	/**
	 * update available score sets
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

			addScoreSet(name, avg, std);
		}
	}

	/**
	 * Clear esaHashMap and scoreSetNamesListBox.
	 */
	private static void clearScoreSets() {
		for (String key : esaHashMap.keySet()) {
			EntityScoreAgent esa = esaHashMap.get(key);
			esa.removeSelfFromNetViz();
		}
		esaHashMap.clear();
		scoreSetNamesListBox.clear();
	}

	/**
	 * add a score set name to the control
	 * 
	 * @param display
	 * @param avg
	 * @param std
	 */
	private static void addScoreSet(final String display, final double avg,
			final double std) {
		// create/add esa
		esaHashMap.put(display, new EntityScoreAgent(display, avg, std, nv));

		// create/add control
		scoreSetNamesListBox.addItem(display, display);
	}

	/**
	 * Get a RadioButton for the score set. RadioButton has a ValueChangeHandler
	 * for turning on the score set.
	 * 
	 * @param display
	 * @return
	 */
	private RadioButton createScoreSetRadioButton(final String display) {
		RadioButton rb = new RadioButton("scoreSetSelection", display);

		rb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				// TODO Auto-generated method stub
				RadioButton sourceRB = (RadioButton) event.getSource();
				boolean value = sourceRB.getValue();
				String name = sourceRB.getText();

				LoggingDialogBox.log("RadioButton for " + name + " is now "
						+ value);

				EntityScoreAgent esa = getEntityScoreAgent(name);

				LoggingDialogBox.log("got esa for " + esa.getName());

				if (value) {
					// TODO turn on score set
				} else {
					// TODO turn off score set
				}
			}
		});

		return rb;
	}

	/**
	 * Get the EntityScoreAgent for the named score set from this object's
	 * HashMap of ESA's.
	 * 
	 * @param display
	 * @return
	 */
	private EntityScoreAgent getEntityScoreAgent(final String display) {
		return esaHashMap.get(display);
	}

	/**
	 * Select the RadioButton in the next row of the score sets FlexTable. If
	 * the last row is selected, go to the top of the FlexTable.
	 * 
	 * @return
	 */
	private void selectNextScoreSet() {
		int next = scoreSetNamesListBox.getSelectedIndex() + 1;

		// at the end of the table
		if (next >= scoreSetNamesListBox.getItemCount()) {
			next = 0;
		}

		scoreSetNamesListBox.setSelectedIndex(next);
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
	 * Synchronize the selected score set with ScoredNodeFilterDialogBox
	 * control.
	 * 
	 * @param scoreSetName
	 */
	private void synchronizeSelectedScoreSet(final String scoreSetName) {
		ScoredNodeFilterDialogBox.setSelectedScoreSet_no_event(scoreSetName);
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
