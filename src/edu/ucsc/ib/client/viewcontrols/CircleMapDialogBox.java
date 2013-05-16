package edu.ucsc.ib.client.viewcontrols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LegendDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.BasicNode;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.SourceSystemSpaceListener;
import edu.ucsc.ib.client.netviz.WorkingSetListener;

/**
 * A dialog box for working with circle images.
 * 
 * @author cw
 * 
 */
public class CircleMapDialogBox extends IbDialogBox implements
		WorkingSetListener, SourceSystemSpaceListener {
	private static final String CIRCLE_IMAGE_URL_BASE = "circleOutput/";
	private static final String CIRCLE_MAP_DATASETS_URL = "data/circlePlot/getMatrixList";
	private static final String CIRCLE_MAP_GET_IMAGES_URL = "data/circlePlot/getImages";
	private static final String UPLOAD_MATRIX_FORM_ACTION = "data/circlePlot/uploadMatrix";
	private static final String DEFAULT_SEARCH_TEXT = "filter selectable data rings";
	// private static final String CIRCLE_MAP_DATASETS_URL =
	// "cgi-bin/cgi_circlePlot.py?method=list";
	private static Request circleImageCurrentRequest = null;
	private Request availabeDatasetCurrentRequest = null;

	private static NetworkVisualization nv;

	/**
	 * If null or "sample score", then show score datasets. If "sample group",
	 * then show group datasets.
	 */
	private String ringTypeSelection = null;

	/**
	 * Handle selection of ring type to display in ring selection controls.
	 */
	private final ValueChangeHandler<Boolean> ringTypeSelectValueChangeHandler = new ValueChangeHandler<Boolean>() {
		@Override
		public void onValueChange(ValueChangeEvent<Boolean> event) {
			RadioButton rb = (RadioButton) event.getSource();

			setRingTypeSelection(rb.getFormValue());
		}
	};

	/**
	 * RadioButton for selecting to display available sample score datasets.
	 */
	private final RadioButton sampleScoreRadioButton = new RadioButton(
			"ringType", "sample score");
	{
		sampleScoreRadioButton.setFormValue("score");
		sampleScoreRadioButton
				.addValueChangeHandler(ringTypeSelectValueChangeHandler);
	}

	/**
	 * RadioButton for selecting to display available sample group datasets.
	 */
	private final RadioButton sampleGroupRadioButton = new RadioButton(
			"ringType", "sample group");
	{
		sampleGroupRadioButton.setFormValue("group");
		sampleGroupRadioButton
				.addValueChangeHandler(ringTypeSelectValueChangeHandler);
	}

	/**
	 * Contain sampleScoreRadioButton and sampleGroupRadioButton
	 */
	private final VerticalPanel ringTypeSelectionPanel = new VerticalPanel();
	{
		ringTypeSelectionPanel.add(sampleScoreRadioButton);
		ringTypeSelectionPanel.add(sampleGroupRadioButton);
	}

	/**
	 * TextBox for live filtering loaded concepts.
	 */
	private final TextBox filterTextBox = new TextBox();
	{
		filterTextBox.setTitle(DEFAULT_SEARCH_TEXT);
		filterTextBox.setText(DEFAULT_SEARCH_TEXT);

		filterTextBox.addKeyUpHandler(new KeyUpHandler() {

			@Override
			public void onKeyUp(KeyUpEvent event) {
				event.stopPropagation();

				TextBox source = (TextBox) event.getSource();
				String value = source.getValue();

				// only filter if enter was pressed
				int keyCode = event.getNativeKeyCode();
				if (keyCode != KeyCodes.KEY_ENTER) {
					return;
				}

				// perform filtering based on value
				rebuildRingSelectionControls(getRingTypeSelection(), value);
			}

		});
	}

	/**
	 * Contain ringTypeSelectionPanel and filterTextBox for
	 * restricting/filtering available datasets
	 */
	private final HorizontalPanel ringTypeFilteringPanel = new HorizontalPanel();
	{
		ringTypeFilteringPanel
				.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		ringTypeFilteringPanel.add(ringTypeSelectionPanel);
		ringTypeFilteringPanel.add(filterTextBox);
	}

	/**
	 * contains controls for ring selection
	 */
	private final FlexTable ringSelectionFlexTable = new FlexTable();

	/**
	 * ScrollPanel for ring selection controls
	 */
	private final ScrollPanel ringSelectionScrollPanel = new ScrollPanel(
			ringSelectionFlexTable);
	{
		ringSelectionScrollPanel.setSize("500px", "300px");
	}

	private final static Image throbber = new Image();
	{
		throbberStop();
	}

	/**
	 * The outermost panel, the object's widget.
	 */
	private final SimplePanel dialogBoxPanel = new SimplePanel();

	/**
	 * Panel for buttons
	 */
	private final HorizontalPanel buttonPanel = new HorizontalPanel();

	/**
	 * Button for switching nodes to use image for visualization.
	 */
	private final Button useImageButton = new Button("use stored images",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					BiodeSet bs = nv.getSelectedNodeIds();

					// if none selected, use all
					if (bs.size() == 0) {
						bs = nv.getCurrentNodeIds();
					}

					switchToImageNodes(bs);
				}
			});

	/**
	 * Button for switching nodes to use SVG shape for visualization.
	 */
	private final Button useShapeButton = new Button("view shapes",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					BiodeSet bs = nv.getSelectedNodeIds();

					// if none selected, use all
					if (bs.size() == 0) {
						bs = nv.getCurrentNodeIds();
					}

					switchToShapeNodes(bs);
				}
			});

	/**
	 * Button for clearing selected dataset CheckBox objects.
	 */
	private final Button clearDatasetsButton = new Button("clear datasets",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					clearSelectedDatasets();
				}
			});

	/**
	 * Button for testing request for image data.
	 */
	private final Button getImageDataButton = new Button("view CircleMaps",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {

					BiodeSet bs = nv.getCurrentNodeIds(SearchSpaceControl
							.getSystemspace());

					// check for valid parameters
					if ((selectedRingDatasetsArrayList.size() == 0)
							|| (bs.isEmpty())) {
						LoggingDialogBox.log("no dataset selected or no genes");
						return;
					}

					// pick the first gene... no matter what it is
					if (getOrderFeature() == null) {
						setOrderFeature(bs.getArray()[0]);
					}

					getImageUrlsAndDisplayImageNodes(bs,
							selectedRingDatasetsArrayList, null,
							getOrderFeature(),
							minMaxOverAllDatasetFeaturesCheckBoxValue(),
							getSampleGroupSummaryCheckBoxValue(),
							getRingMergeCheckBoxValue(),
							getIgnoreMissingSamplesCheckBoxValue(),
							circleImageCurrentRequest);
				}
			});

	/**
	 * the currently selected score datasets by displayName. By default, the
	 * first one is used for sorting.
	 */
	private static final ArrayList<String> selectedRingDatasetsArrayList = new ArrayList<String>();

	/**
	 * Current display order of score rings
	 */
	private static ArrayList<String> ringDisplayOrder = new ArrayList<String>();

	/**
	 * The dataset to use for sorting.
	 */
	private static String sortingDataset = null;

	/**
	 * feature to use for ordering samples
	 */
	private static String orderFeature = null;

	/**
	 * Map a selectable ring display name to internal name.
	 */
	private static HashMap<String, String> ringDisplayNameToInternalNameMapping = new HashMap<String, String>();

	/**
	 * Tree to explain rings.
	 */
	private final static Tree ringExplanationTree = new Tree();
	{
		// ringExplanationTree.setAnimationEnabled(true);
		ringExplanationTree.setTitle("explanation of loaded rings");
	}

	/**
	 * Tree to explain selected sample score data.
	 */
	private final static Tree selectedRingsTree = new Tree();
	{
		// selectedMatrixDataTree.setAnimationEnabled(true);
		selectedRingsTree.setTitle("order of selected sample score rings");
	}

	/**
	 * Tree to explain selected sample group data.
	 */
	private final static Tree selectedGroupRingsTree = new Tree();
	{
		selectedGroupRingsTree.setTitle("order of selected sample group rings");
	}

	/**
	 * select how min/max sample values are calculated for each dataset
	 */
	private final static CheckBox minMaxOverAllDatasetFeaturesCheckBox = new CheckBox(
			"min/max over all of datasets' concepts");
	{
		minMaxOverAllDatasetFeaturesCheckBox
				.setTitle("use min/max values over all of each datasets' concepts instead of just the selected ones");
		minMaxOverAllDatasetFeaturesCheckBox.setValue(true, false);
	}

	/**
	 * select if sample subtype summary should be used
	 */
	private final static CheckBox sampleSubtypeSummaryCheckBox = new CheckBox(
			"use sample group summary");
	{
		sampleSubtypeSummaryCheckBox
				.setTitle("draw a ring that shows sample group summaries");
		sampleSubtypeSummaryCheckBox
				.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

					@Override
					public void onValueChange(ValueChangeEvent<Boolean> event) {
						// currently, merge and summary are mutually exclusive

						boolean eventValue = ((CheckBox) event.getSource())
								.getValue();

						if (eventValue == true
								&& ringMergeCheckBox.getValue() == true) {
							ringMergeCheckBox.setValue(false, false);
						}
					}

				});
	}

	/**
	 * Select whether or not to attempt ring merging
	 */
	private final static CheckBox ringMergeCheckBox = new CheckBox("ring merge");
	{
		ringMergeCheckBox
				.setTitle("attempt to merge rings of similar datatype");
		ringMergeCheckBox
				.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

					@Override
					public void onValueChange(ValueChangeEvent<Boolean> event) {
						// currently, merge and summary are mutually exclusive

						boolean eventValue = ((CheckBox) event.getSource())
								.getValue();

						if (eventValue == true
								&& sampleSubtypeSummaryCheckBox.getValue() == true) {
							sampleSubtypeSummaryCheckBox.setValue(false, false);
						}
					}

				});
	}

	private final static CheckBox ignoreMissingSamplesCheckBox = new CheckBox(
			"ignore missing samples");
	{
		ignoreMissingSamplesCheckBox
				.setTitle("include only samples present in all selected datasets");
		ignoreMissingSamplesCheckBox
				.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

					@Override
					public void onValueChange(ValueChangeEvent<Boolean> event) {
						// TODO Auto-generated method stub
					}

				});

	}

	/**
	 * additional form submission part
	 */
	private final TextBox uploadMatrixNameTextBox = new TextBox();
	{
		uploadMatrixNameTextBox.setName("name");
		uploadMatrixNameTextBox.setTitle("name of uploaded data");
		uploadMatrixNameTextBox.setVisible(false);
	}

	private final FileUpload fileUploadWidget = new FileUpload();
	{
		fileUploadWidget.setName("uploadFormElement");
		// fileUploadWidget.setWidth("250px");
	}
	/**
	 * for uploading a matrix file.
	 */
	private final FormPanel uploadMatrixFileFormPanel = new FormPanel();
	{
		uploadMatrixFileFormPanel.setAction(UPLOAD_MATRIX_FORM_ACTION);
		uploadMatrixFileFormPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadMatrixFileFormPanel.setMethod(FormPanel.METHOD_POST);

		VerticalPanel containerPanel = new VerticalPanel();
		uploadMatrixFileFormPanel.setWidget(containerPanel);

		uploadMatrixFileFormPanel.addSubmitHandler(new SubmitHandler() {
			@Override
			public void onSubmit(SubmitEvent event) {
				LoggingDialogBox
						.log("CircleMapDialogBox.uploadScoreFileFormPanel: begin onSubmit");
				throbberStart();
				setUploadMatrixName(fileUploadWidget.getFilename());
				LoggingDialogBox
						.log("CircleMapDialogBox.uploadScoreFileFormPanel: end onSubmit");
			}
		});

		uploadMatrixFileFormPanel
				.addSubmitCompleteHandler(new SubmitCompleteHandler() {
					@Override
					public void onSubmitComplete(SubmitCompleteEvent event) {
						LoggingDialogBox
								.log("CircleMapDialogBox.uploadScoreFileFormPanel: begin onSubmitComplete");
						handleUploadScoreFileSubmitComplete(event);
						throbberStop();
						LoggingDialogBox
								.log("CircleMapDialogBox.uploadScoreFileFormPanel: end onSubmitComplete");
					}
				});

		// controls for form submission parts
		containerPanel.add(fileUploadWidget);
		containerPanel.add(uploadMatrixNameTextBox);
	}

	/**
	 * submits file for uploading to server
	 */
	final Button uploadMatrixFileButton = new Button("upload matrix",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					uploadMatrixFileFormPanel.submit();
				}
			});

	/**
	 * put file upload controls here
	 */
	final HorizontalPanel uploadFilePanel = new HorizontalPanel();
	{
		uploadFilePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
		uploadFilePanel.add(uploadMatrixFileFormPanel);
		uploadFilePanel.add(uploadMatrixFileButton);
	}

	/**
	 * contain the following controls: 1- matrix upload 2- ring selection 3-
	 * filter TextBox for ring selection controls
	 */
	private VerticalPanel ringSelectionOuterPanel = new VerticalPanel();
	{
		ringSelectionOuterPanel.add(ringTypeFilteringPanel);
		ringSelectionOuterPanel.add(ringSelectionScrollPanel);
		ringSelectionOuterPanel.add(uploadFilePanel);
	}

	/**
	 * data to create color key for group rings
	 */
	private static JSONObject groupRingsColorKeysJO;

	// TODO ///////////////////////////////////////////////////////////

	/**
	 * Construct a dialog box for working with metanodes.
	 * 
	 * @param nv
	 */
	public CircleMapDialogBox(NetworkVisualization netViz) {
		super("CircleMap controls");

		nv = netViz;

		this.setWidget(this.dialogBoxPanel);

		// this.buttonPanel.add(this.useImageButton);
		this.buttonPanel.add(this.useShapeButton);
		this.buttonPanel.add(this.clearDatasetsButton);
		this.buttonPanel.add(this.getImageDataButton);
		this.buttonPanel.add(throbber);

		FlexTable controlsFlexTable = new FlexTable();

		VerticalPanel optionButtonsPanel = new VerticalPanel();
		optionButtonsPanel.add(sampleSubtypeSummaryCheckBox);
		optionButtonsPanel.add(ringMergeCheckBox);
		optionButtonsPanel.add(ignoreMissingSamplesCheckBox);

		optionButtonsPanel.add(selectedRingsTree);
		optionButtonsPanel.add(selectedGroupRingsTree);

		controlsFlexTable.setText(0, 0, "select rings from below");
		controlsFlexTable.setWidget(1, 0, ringSelectionOuterPanel);

		// this cell spans 2 rows
		controlsFlexTable.getFlexCellFormatter().setRowSpan(0, 1, 2);
		controlsFlexTable.setWidget(0, 1, optionButtonsPanel);
		controlsFlexTable.getFlexCellFormatter().setVerticalAlignment(0, 1,
				HasVerticalAlignment.ALIGN_TOP);

		controlsFlexTable.setWidget(2, 0, this.buttonPanel);
		// ...and set it's column span so that it takes up the whole row.
		controlsFlexTable.getFlexCellFormatter().setColSpan(2, 0, 2);

		// controlsFlexTable.setWidget(3, 0, selectedMatrixDataTree);
		// controlsFlexTable.getFlexCellFormatter().setColSpan(3, 0, 2);

		controlsFlexTable.setWidget(4, 0, ringExplanationTree);
		controlsFlexTable.getFlexCellFormatter().setColSpan(4, 0, 2);

		this.dialogBoxPanel.add(controlsFlexTable);

		nv.addWorkingSetListener(this);
		SearchSpaceControl.addSystemSpaceListener(this);

		sampleScoreRadioButton.setValue(true, true);
	}

	/**
	 * Get the ringTypeSelection for restricting the ring type selection
	 * controls.
	 * 
	 * @return
	 */
	protected String getRingTypeSelection() {
		return ringTypeSelection;
	}

	/**
	 * Set the ringTypeSelection for restricting the ring type selection
	 * controls.
	 * 
	 * @param ringType
	 */
	protected void setRingTypeSelection(String ringType) {
		ringTypeSelection = ringType;

		// switch ring selection controls
		rebuildRingSelectionControls(ringTypeSelection, getFilterTextBoxValue());
	}

	/**
	 * value of cohortMinMaxCheckBox
	 * 
	 * @return If false, min/max values are calculated for each ring for each
	 *         circle map image. If true, min/max values are calculated over the
	 *         whole set of the datasets' features.
	 */
	public static boolean minMaxOverAllDatasetFeaturesCheckBoxValue() {
		return minMaxOverAllDatasetFeaturesCheckBox.getValue();
	}

	/**
	 * value of ringMergeCheckBox
	 * 
	 * @return
	 */
	public static boolean getRingMergeCheckBoxValue() {
		return ringMergeCheckBox.getValue();
	}

	/**
	 * value of sampleSubtypeSummaryCheckBox
	 * 
	 * @return If true, draw a ring to show sample subtype summarization.
	 */
	public static boolean getSampleGroupSummaryCheckBoxValue() {
		return sampleSubtypeSummaryCheckBox.getValue();
	}

	/**
	 * value of ignoreMissingSamplesCheckBox
	 * 
	 * @return
	 */
	public static boolean getIgnoreMissingSamplesCheckBoxValue() {
		return ignoreMissingSamplesCheckBox.getValue();
	}

	/**
	 * Set all of the CheckBox objects to false.
	 */
	private void clearSelectedDatasets() {
		selectedRingDatasetsArrayList.clear();

		rebuildRingSelectionControls(getRingTypeSelection(), "");

		updateSelectedRingsDisplay();
	}

	/**
	 * Modify the Tree widget of CheckBox objects for selecting datasets for
	 * circle image rings.
	 * 
	 * @param systemSpace
	 */
	private void setupAvailableDatasets(String systemSpace) {

		// check for running request
		if (availabeDatasetCurrentRequest != null) {
			availabeDatasetCurrentRequest.cancel();
			availabeDatasetCurrentRequest = null;
			LoggingDialogBox
					.log("cancelled a running request for availabeDatasetCurrentRequest");
		}

		RequestCallback availableDatasetRequestCallback = new RequestCallback() {

			@Override
			public void onResponseReceived(Request request, Response response) {
				// check for OK status code
				if (response.getStatusCode() != 200) {
					LoggingDialogBox
							.log("RequestCallback in CircleMapDialogBox.setupAvailableDatasets() did not get OK status(200).  Status code: "
									+ response.getStatusCode());
					throbberStop();
					return;
				}

				// DataVizPanel.log(response.getText());

				// check for error message in JSON-RPC response
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						response.getText());

				if (jsonRpcResp.hasError()) {
					LoggingDialogBox
							.log("CircleMapDialogBox.setupAvailableDatasets got an error: "
									+ jsonRpcResp.getError().toString());
					throbberStop();
					return;
				}

				JSONObject resultJO = jsonRpcResp.getResult();

				// get the datasets from JSON-RPC result object and store in
				// a hash

				JSONArray matricesJA = resultJO.get("matrices").isArray();
				JSONArray clinicalMatricesJA = resultJO.get("clinical")
						.isArray();

				setDatasetDisplayToInternalNameMapping(matricesJA,
						clinicalMatricesJA);

				rebuildRingSelectionControls(getRingTypeSelection(), "");

				throbberStop();
			}

			@Override
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox
						.log("error with RequestCallback in CircleMapDialogBox.setupAvailableDatasets(): "
								+ exception.toString());
				throbberStop();
			}

		};

		// setup request
		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
				CIRCLE_MAP_DATASETS_URL + "?organism=" + systemSpace);

		// send request and specify the RequestCallback object
		try {
			throbberStart();
			availabeDatasetCurrentRequest = rb.sendRequest(null,
					availableDatasetRequestCallback);
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
			throbberStop();
		}
	}

	/**
	 * Get a mapping for UI display name to internal naming for available data.
	 * The "internal name" for clinical features will end with take the form:
	 * table__feature_clinical. The "internal name" for uploaded data will take
	 * the form: filename_uploaded.
	 * 
	 * @return
	 */
	protected static HashMap<String, String> getDatasetDisplayToInternalNameMapping() {
		return ringDisplayNameToInternalNameMapping;
	}

	/**
	 * Set a mapping for UI display name to internal naming for available data.
	 * The "internal name" for clinical features will end with take the form:
	 * table__feature_clinical. The "internal name" for uploaded data will take
	 * the form: filename_uploaded.
	 * 
	 * @param matricesJA
	 * @param clinicalMatricesJA
	 */
	protected static void setDatasetDisplayToInternalNameMapping(
			JSONArray matricesJA, JSONArray clinicalMatricesJA) {
		HashMap<String, String> resultHashMap = new HashMap<String, String>();

		JSONObject jo;

		for (int i = 0; i < matricesJA.size(); i++) {
			jo = matricesJA.get(i).isObject();
			String displayName = jo.get("name").isString().stringValue();

			String internalName = displayName;

			if (jo.get("category").isString().stringValue()
					.equalsIgnoreCase("uploaded")) {
				displayName = "*uploaded: "
						+ jo.get("name").isString().stringValue();
				internalName = internalName + "_uploaded";
			}

			resultHashMap.put(displayName, internalName);
		}

		for (int i = 0; i < clinicalMatricesJA.size(); i++) {
			jo = clinicalMatricesJA.get(i).isObject();
			String tableName = jo.get("tableName").isString().stringValue();

			JSONArray featuresJA = jo.get("features").isArray();
			for (int j = 0; j < featuresJA.size(); j++) {
				String feature = featuresJA.get(j).isString().stringValue();

				if (feature.equals("_PATIENT")) {
					continue;
				}

				String displayName = tableName.replaceFirst("clinicalMatrix",
						"") + "clinical feature: " + feature;

				String internalName = tableName + "__" + feature + "_clinical";

				resultHashMap.put(displayName, internalName);
				resultHashMap.put(internalName, internalName);
			}
		}

		ringDisplayNameToInternalNameMapping = resultHashMap;
	}

	/**
	 * Switch the visualization of nodes to use shapes.
	 * 
	 * @param bs
	 */
	private void switchToShapeNodes(BiodeSet bs) {
		if (bs.size() == 0) {
			return;
		}
		nv.switchNodeToImage(false, bs);

		nv.updateEdgePositions(nv.getEdgeGroups(bs));
	}

	/**
	 * The method has several parts. First, it uses HUGO gene symbols to query
	 * server for circle image data. (This query only understands HUGO symbols)
	 * Second, the circle image URLs are used to modify BiodeInfo objects.
	 * Finally, the image nodes are displayed.
	 * 
	 * @param biodeSet
	 *            set of biodes to get images for. Empty set shouldn't break the
	 *            request.
	 * @param ringDisplayOrder
	 *            datasets to display. Empty list may break the request.
	 * @param sortingDataset
	 *            dataset to use for sorting
	 * @param geneToSortBy
	 *            gene to use for sorting
	 * @param minMaxOverAllDatasetFeaturesSwitch
	 *            If true, use min/max of selected genes. If false, use min/max
	 *            over all values in dataset.
	 * @param sampleGroupSummarySwitch
	 *            compute and display summary rings
	 * @param ringMergeSwitch
	 *            If true, attempt to merge similar rings together
	 * @param ignoreMissingSamples
	 *            If true, only include samples that have data in all selected
	 *            datasets.
	 * @param request
	 */
	private static void getImageUrlsAndDisplayImageNodes(BiodeSet biodeSet,
			ArrayList<String> ringDisplayOrder, String sortingDataset,
			String geneToSortBy, boolean minMaxOverAllDatasetFeaturesSwitch,
			boolean sampleGroupSummarySwitch, boolean ringMergeSwitch,
			boolean ignoreMissingSamples, Request request) {

		// cancel any running request
		if (request != null) {
			request.cancel();
			request = null;
		}

		// simple parameter check
		if (ringDisplayOrder.isEmpty()) {
			throbberStop();
			LoggingDialogBox
					.log("CircleMapDialogBox.getImageUrlsAndDisplayImageNodes: no datasets");
			return;
		}

		/**
		 * RequestCallback for getting circle images and setting nodes to
		 * display the images.
		 */
		RequestCallback getCircleImageRC = new RequestCallback() {

			@Override
			public void onResponseReceived(Request request, Response response) {
				// check for OK status code
				if (response.getStatusCode() != 200) {
					LoggingDialogBox
							.log("RequestCallback for getting circle images did not get OK status(200).  Status code: "
									+ response.getStatusCode());
					throbberStop();
					return;
				}

				// DataVizPanel.log(response.getText());

				// check for error message in JSON-RPC response
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						response.getText());

				if (jsonRpcResp.hasError()) {
					LoggingDialogBox
							.log("CircleMapDialogBox.setupAvailableDatasets got an error: "
									+ jsonRpcResp.getError().toString());
					throbberStop();
					return;
				}

				JSONObject resultJO = jsonRpcResp.getResult();

				// get heatmaps from JSON-RPC result object
				JSONObject urlsJO = resultJO.get("circleImageURLs").isObject();

				// set the image URL for the BiodeInfo objects
				BiodeSet bs = new BiodeSet();

				for (String biode : urlsJO.keySet()) {
					String imageURL = CIRCLE_IMAGE_URL_BASE
							+ urlsJO.get(biode).isString().stringValue();

					// restrict systemspace
					String organism = SearchSpaceControl.getSystemspace();

					BiodeInfo bi = nv.BIC.getBiodeInfo(biode);

					if (bi.getSystemSpace().equalsIgnoreCase(organism)) {
						BasicNode nn = nv.getNode(bi.getSystematicName());
						nn.setImageURL(imageURL);

						bs.add(bi.getSystematicName());
					}
				}

				// set display of images for nodes
				if (bs.size() > 0) {
					nv.switchNodeToImage(true, bs);

					nv.updateEdgePositions(nv.getEdgeGroups(bs));
				}

				// set the explanation of rings

				setRingDisplayOrder(resultJO.get("matrixDisplayOrder")
						.isArray());

				setSortingDataset(resultJO.get("sortingDataset").isString()
						.stringValue());

				setOrderFeature(resultJO.get("orderFeature").isString()
						.stringValue());

				groupRingsColorKeysJO = resultJO.get("groupRingColorKeys")
						.isObject();

				setRingExplanation(getRingDisplayOrder(), getOrderFeature());

				throbberStop();
			}

			@Override
			public void onError(Request request, Throwable exception) {
				throbberStop();
				LoggingDialogBox
						.log("error with RequestCallback while getting circle images: "
								+ exception.toString());
			}
		};

		// build up a StringBuffer to use in query string
		StringBuffer datasetSB = new StringBuffer();
		for (String dataset : ringDisplayOrder) {
			String internalName;
			if (dataset.endsWith("_clinical")) {
				// TODO hacky way to deal with internal vs display name here.
				internalName = dataset;
			} else if (dataset.endsWith("_uploaded")) {
				internalName = dataset;
			} else {
				internalName = getDatasetInternalName(dataset);
			}

			datasetSB.append(internalName + ",");
		}
		datasetSB.deleteCharAt(datasetSB.lastIndexOf(","));

		// build up a StringBuffer to use in query string
		StringBuffer idSB = new StringBuffer();
		for (String id : biodeSet) {
			idSB.append(id + ",");
		}
		idSB.deleteCharAt(idSB.lastIndexOf(","));

		// setup request
		StringBuffer urlSB = new StringBuffer(CIRCLE_MAP_GET_IMAGES_URL);
		urlSB.append("?ringsList=" + datasetSB.toString());
		urlSB.append("&features=" + idSB.toString());
		urlSB.append("&orderFeature=" + geneToSortBy);
		urlSB.append("&minMaxOverAllDatasetFeaturesSwitch="
				+ minMaxOverAllDatasetFeaturesSwitch);
		urlSB.append("&sampleGroupSummarySwitch=" + sampleGroupSummarySwitch);
		urlSB.append("&ringMergeSwitch=" + ringMergeSwitch);

		urlSB.append("&sortingRing=" + sortingDataset);

		urlSB.append("&ignoreMissingSamples=" + ignoreMissingSamples);

		// LoggingDialogBox.log(urlSB.toString());

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
				urlSB.toString());

		// send request and specify the RequestCallback object
		try {
			throbberStart();
			request = rb.sendRequest(null, getCircleImageRC);
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
			throbberStop();
		}

	}

	/**
	 * Set the order feature.
	 * 
	 * @param feature
	 */
	public static void setOrderFeature(String feature) {
		orderFeature = feature;
	}

	/**
	 * Set the current display order of rings in CircleMap images.
	 * 
	 * @param inputJA
	 */
	public static void setRingDisplayOrder(JSONArray inputJA) {
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < inputJA.size(); i++) {
			result.add(inputJA.get(i).isString().stringValue());
		}

		ringDisplayOrder = result;
	}

	/**
	 * Get the current display order of rings in CircleMap images.
	 * 
	 * @return
	 */
	public static ArrayList<String> getRingDisplayOrder() {
		return ringDisplayOrder;
	}

	/**
	 * Get the order feature.
	 * 
	 * @return
	 */
	protected static String getOrderFeature() {
		return orderFeature;
	}

	/**
	 * Create a color key panel.
	 * 
	 * @param groupRingsColorKeysJO
	 *            JSONOBject that has RGBA information for each subtype
	 * @return
	 */
	public static VerticalPanel getGroupRingColorKeyPanel(String ringName) {

		VerticalPanel colorKeyPanel = new VerticalPanel();

		if (!groupRingsColorKeysJO.keySet().contains(ringName)) {
			return colorKeyPanel;
		}

		JSONArray colorsJA = groupRingsColorKeysJO.get(ringName).isArray();

		for (int i = 0; i < colorsJA.size(); i++) {

			JSONObject rgbJO = colorsJA.get(i).isObject();

			String groupingVal = rgbJO.get("groupName").isString()
					.stringValue();

			double r = rgbJO.get("r").isNumber().doubleValue();
			double g = rgbJO.get("g").isNumber().doubleValue();
			double b = rgbJO.get("b").isNumber().doubleValue();
			double a = rgbJO.get("a").isNumber().doubleValue() / 255;

			String rgbColor = "rgba(" + r + "," + g + "," + b + "," + a + ")";

			HorizontalPanel legendRow = new HorizontalPanel();

			AbsolutePanel swatch = new AbsolutePanel();
			swatch.setPixelSize(9, 9);
			swatch.setTitle(groupingVal);

			DOM.setStyleAttribute(swatch.getElement(), "background", rgbColor);

			legendRow.add(swatch);
			legendRow.add(new Label(groupingVal));

			colorKeyPanel.add(legendRow);
		}

		return colorKeyPanel;
	}

	/**
	 * Set explanation for rings.
	 * 
	 * @param matrixDisplayOrder
	 * @param orderFeature
	 */
	private static void setRingExplanation(
			ArrayList<String> matrixDisplayOrder, String orderFeature) {
		ringExplanationTree.removeItems();

		TreeItem explanationTI = new TreeItem("current displayed rings");

		// dataset names
		int index = 1;
		for (String name : matrixDisplayOrder) {

			String text = "";
			if (index == 1) {
				text = (index++) + " (innermost): " + name;
			} else {
				text = (index++) + ": " + name;
			}

			HorizontalPanel hp = new HorizontalPanel();
			hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
			hp.add(new Label(text));

			explanationTI.addItem(hp);
		}

		String str = "Each CircleMap sorted by the ordering of dataset scores for "
				+ orderFeature;

		ringExplanationTree.addTextItem(str);

		ringExplanationTree.addItem(explanationTI);

		explanationTI.setState(true);

		// TODO reset the legend dialog box
		LegendDialogBox.resetCircleLegend();
	}

	/**
	 * Switch the visualization of nodes to use images.
	 * 
	 * @param bs
	 */
	private void switchToImageNodes(final BiodeSet bs) {
		// get relevant biodes
		BiodeSet relevantSet = new BiodeSet();
		for (String biode : bs) {
			BiodeInfo bi = nv.BIC.getBiodeInfo(biode);
			if (bi.getSystemSpace().equalsIgnoreCase("9606")) {
				relevantSet.add(biode);
			}
		}

		// relevant set is empty
		if (relevantSet.size() == 0) {
			LoggingDialogBox.log("no human nodes");
			return;
		}

		// switch visualization to use image
		nv.switchNodeToImage(true, relevantSet);

		nv.updateEdgePositions(nv.getEdgeGroups(bs));
	}

	/**
	 * stop throbber
	 */
	private static void throbberStop() {
		throbber.setUrl(BiodeSearchPanel.STOPPED_THROBBER);
	}

	/**
	 * active throbber
	 */
	private static void throbberStart() {
		throbber.setUrl(BiodeSearchPanel.LOADING_THROBBER);
	}

	@Override
	public void sourceSystemSpaceChanged(String systemSpace) {
		// update available biodes for ordering
		addedBiodes(new BiodeSet());

		// update available score matrices for rings
		setupAvailableDatasets(systemSpace);
	}

	@Override
	public void viewSystemSpaceChanged(String viewSystemSpace) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addedBiodes(BiodeSet b) {

		BiodeSet bs = nv.getCurrentNodeIds();
		bs.addAll(b);

		BiodeSet acceptedBiodes = new BiodeSet();

		for (String biode : bs) {
			BiodeInfo bi = nv.BIC.getBiodeInfo(biode);
			if ((bi == null)
					|| (bi != null && bi.getSystemSpace().equalsIgnoreCase(
							SearchSpaceControl.getSystemspace()))) {
				acceptedBiodes.add(biode);
			}
		}

		// request CircleMap images
		getImageUrlsAndDisplayImageNodes(acceptedBiodes, getRingDisplayOrder(),
				getSortingDataset(), getOrderFeature(),
				minMaxOverAllDatasetFeaturesCheckBoxValue(),
				getSampleGroupSummaryCheckBoxValue(),
				getRingMergeCheckBoxValue(),
				getIgnoreMissingSamplesCheckBoxValue(),
				circleImageCurrentRequest);
	}

	/**
	 * Set the dataset to use for sorting.
	 * 
	 * @param dataset
	 */
	private static void setSortingDataset(String dataset) {
		sortingDataset = dataset;
	}

	/**
	 * Get the dataset to use for sorting. If the dataset is not a currently
	 * displayed ring, then return null.
	 * 
	 * @return
	 */
	private static String getSortingDataset() {

		if (ringDisplayOrder.contains(sortingDataset)) {
			return sortingDataset;
		} else {
			return null;
		}

	}

	@Override
	public void removedBiodes(BiodeSet b) {
		BiodeSet bs = nv.getCurrentNodeIds();
		bs.removeAll(b);

		BiodeSet acceptedBiodes = new BiodeSet();

		for (String biode : bs) {
			BiodeInfo bi = nv.BIC.getBiodeInfo(biode);
			if (bi != null
					&& bi.getSystemSpace().equalsIgnoreCase(
							SearchSpaceControl.getSystemspace())) {
				acceptedBiodes.add(biode);
			}
		}
	}

	/**
	 * Clear the display of selected rings.
	 */
	private static void clearSelectedRingsDisplay() {
		selectedRingsTree.removeItems();
		selectedGroupRingsTree.removeItems();
	}

	/**
	 * Update display of selected rings (both score rings and group rings)
	 */
	private void updateSelectedRingsDisplay() {
		clearSelectedRingsDisplay();

		// score rings
		TreeItem selectedScoreRingsTreeItem = new TreeItem(
				"selected data for score rings");

		int index = 1;
		for (String displayName : selectedRingDatasetsArrayList) {
			String text = "";
			if (index == 1) {
				text = (index++) + " (innermost): " + displayName;
			} else {
				text = (index++) + ": " + displayName;
			}
			selectedScoreRingsTreeItem.addTextItem(text);
		}

		if (selectedScoreRingsTreeItem.getChildCount() > 0) {
			selectedRingsTree.addItem(selectedScoreRingsTreeItem);
			selectedScoreRingsTreeItem.setState(true);
		}

	}

	/**
	 * Get a list of CircleMap ring radii. Includes radius for the center and
	 * group ring, if used.
	 * 
	 * @param centerThickness
	 *            unit thickness of center "ring"
	 * @param totalNumRings
	 *            includes the group ring, but not the center
	 * @param fullRadius
	 *            full radius of CircleMap
	 * @return
	 */
	public static ArrayList<Double> getListOfRingRadii(double centerThickness,
			int totalNumRings, double fullRadius) {
		ArrayList<Double> radiiList = new ArrayList<Double>();

		// sum of unit thicknesses
		double unitSum = 0;
		unitSum += centerThickness;

		for (int i = 1; i <= totalNumRings; i++) {
			unitSum += 1;
		}

		double cumulativeUnitRadius = 0;

		double fraction;

		// center
		fraction = centerThickness / unitSum;
		cumulativeUnitRadius += fraction;
		radiiList.add(cumulativeUnitRadius * fullRadius);

		// data rings
		for (int i = 1; i <= totalNumRings; i++) {
			fraction = new Double(1) / unitSum;

			cumulativeUnitRadius += fraction;
			radiiList.add(cumulativeUnitRadius * fullRadius);
		}

		return radiiList;
	}

	/**
	 * Get the number of displayed rings.
	 * 
	 * @return
	 */
	public static int getNumDisplayedRings() {

		return ringDisplayOrder.size();
	}

	/**
	 * Wrapper method for getting re-sorted circle map images.
	 * 
	 * @param sortingFeatureID
	 * @param clickedRingNumber
	 *            one-based counting
	 * @param request
	 */
	public static void reSortCircleMaps(String sortingFeatureID,
			int clickedRingNumber, Request request) {

		// get parameters and options for CircleMap image request

		// convert to zero-based counting
		clickedRingNumber--;

		// TODO need to do some index out of bounds checking
		String clickedDataset = getRingDisplayOrder().get(clickedRingNumber);
		LoggingDialogBox.log("clicked ring: " + clickedRingNumber + " is for "
				+ clickedDataset);

		BiodeSet bs = nv.getCurrentNodeIds(nv.BIC
				.getBiodeInfo(sortingFeatureID).getSystemSpace());

		setOrderFeature(sortingFeatureID);

		// make CircleMap image request
		getImageUrlsAndDisplayImageNodes(bs, getRingDisplayOrder(),
				clickedDataset, getOrderFeature(),
				minMaxOverAllDatasetFeaturesCheckBoxValue(),
				getSampleGroupSummaryCheckBoxValue(),
				getRingMergeCheckBoxValue(),
				getIgnoreMissingSamplesCheckBoxValue(), request);
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
		// JsonRpcResponse jsonRpcResp = new
		// JsonRpcResponse(event.getResults());

		// update controls
		setupAvailableDatasets(SearchSpaceControl.getSystemspace());
	}

	/**
	 * Set the name of the matrix.
	 * 
	 * @param name
	 */
	public void setUploadMatrixName(String name) {
		uploadMatrixNameTextBox.setText(name);
	}

	/**
	 * rebuild the ring selection controls.
	 * 
	 * @param ringTypeFilter
	 * @param nameFilter
	 */
	private void rebuildRingSelectionControls(String ringTypeFilter,
			String nameFilter) {

		// do not apply filter with DEFAULT_SEARCH_TEXT
		if (nameFilter.equalsIgnoreCase(DEFAULT_SEARCH_TEXT)) {
			nameFilter = "";
		}

		// clear out previous controls
		ringSelectionFlexTable.removeAllRows();

		ArrayList<String> displayNameArrayList = new ArrayList<String>(
				getDatasetDisplayToInternalNameMapping().keySet());

		Collections.sort(displayNameArrayList);

		// Handler for keeping track of dataset selection
		final ValueChangeHandler<Boolean> checkBoxValueChangeHandler = new ValueChangeHandler<Boolean>() {

			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				CheckBox source = (CheckBox) event.getSource();

				String displayName = source.getFormValue();

				String ringType = getRingType(displayName);

				if (source.getValue()) {
					selectedRingDatasetsArrayList.add(displayName);
				} else {
					selectedRingDatasetsArrayList.remove(displayName);
				}

				// update display of selected datasets
				updateSelectedRingsDisplay();
			}
		};

		String nameFilterLC = nameFilter.toLowerCase();

		// create control for each selectable ring
		for (final String displayName : displayNameArrayList) {

			boolean display = true;

			// TODO HACK!
			if (displayName.endsWith("_clinical")
					&& getDatasetInternalName(displayName).equalsIgnoreCase(
							displayName)) {
				display = false;
			}

			// enforce ringType filter
			String ringType = getRingType(displayName);
			if (!ringType.equalsIgnoreCase(ringTypeFilter)) {
				display = false;
			}

			// enforce name filter - require ALL substrings
			for (String value : nameFilterLC.split("\\s+")) {
				if (displayName.toLowerCase().indexOf(value) == -1) {
					display = false;
					break;
				}
			}

			if (display == false) {
				// skip this displayName
				continue;
			}

			// make CheckBox for dataset
			CheckBox cb = new CheckBox(displayName);
			cb.setTitle("");
			cb.setFormValue(displayName);
			cb.addValueChangeHandler(checkBoxValueChangeHandler);

			// check if already chosen ring
			if (isDatasetSelected(displayName)) {
				cb.setValue(true, false);
			}

			// add new control
			int row = ringSelectionFlexTable.getRowCount();

			ringSelectionFlexTable.setWidget(row, 0, cb);
		}

		ringSelectionScrollPanel.scrollToTop();
	}

	/**
	 * Get the ring type.
	 * 
	 * @param displayName
	 * @return "group" or "score" (uploaded data is assumed "score" for now)
	 */
	private String getRingType(String displayName) {
		String internalName = getDatasetInternalName(displayName);

		if (internalName.endsWith("_upload")) {
			return "score";
		} else if (internalName.endsWith("_clinical")) {
			return "group";
		} else {
			return "score";
		}
	}

	/**
	 * Get the internal name of a dataset
	 * 
	 * @param displayName
	 * @return
	 */
	public static String getDatasetInternalName(String displayName) {
		return ringDisplayNameToInternalNameMapping.get(displayName);
	}

	/**
	 * Check if the dataset has been selected for display.
	 * 
	 * @param displayName
	 * @return
	 */
	public static boolean isDatasetSelected(String displayName) {

		if (selectedRingDatasetsArrayList.contains(displayName)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get the value in the filterTextBox. This is the String to use to filter
	 * down the displayed controls for ring selection.
	 * 
	 * @return
	 */
	private String getFilterTextBoxValue() {
		return filterTextBox.getValue();
	}
}
