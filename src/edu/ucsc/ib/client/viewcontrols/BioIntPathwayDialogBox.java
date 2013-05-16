package edu.ucsc.ib.client.viewcontrols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;

/**
 * A dialog box for working with BioInt pathways.
 * 
 * @author cw
 * 
 */
public class BioIntPathwayDialogBox extends IbDialogBox {
	private Request pathwayListCurrentRequest = null;
	private Request featuresAndInteractionCurrentRequest = null;

	private final NetworkVisualization nv;

	/**
	 * The outermost panel, the object's widget.
	 */
	private final SimplePanel outermostPanel = new SimplePanel();

	/**
	 * pathway_id of selected BioInt pathway
	 */
	private String pathwayID = "";

	/**
	 * Handler for identifying selected RadioButton. Sets the pathwayID global
	 * variable.
	 */
	private final ValueChangeHandler<Boolean> pathwayRadioButtonValueChangeHandler = new ValueChangeHandler<Boolean>() {
		@Override
		public void onValueChange(ValueChangeEvent<Boolean> event) {
			RadioButton sourceRB = (RadioButton) event.getSource();
			if (sourceRB.getValue()) {
				pathwayID = sourceRB.getFormValue();
				String pathwayName = sourceRB.getText();
				LoggingDialogBox.log(pathwayID + " was selected");

				// TODO query for the pathway data and update UI
				getPathwayFeaturesAndInteractions(pathwayID, pathwayName,
						featuresAndInteractionCurrentRequest);
			} else {
				// not checked. do nothing
			}
		}
	};

	/**
	 * Tree for selecting pathway
	 */
	private final Tree pathwaysTree = new Tree();
	{
		this.pathwaysTree.setTitle("select pathway");
	}

	/**
	 * Tree for displaying pathway features
	 */
	private final Tree featuresTree = new Tree();
	{
		this.featuresTree.setTitle("pathway features");
	}

	/**
	 * Tree for displaying pathway interactions
	 */
	private final Tree interactionsTree = new Tree();
	{
		this.interactionsTree.setTitle("pathway interactions");
	}

	// ///////////////////////////////////////////////////////////

	/**
	 * Construct a dialog box for working with BioInt pathways.
	 * 
	 * @param nv
	 */
	public BioIntPathwayDialogBox(NetworkVisualization nv) {
		super("BioInt pathway controls");

		this.nv = nv;

		this.setWidget(this.outermostPanel);

		FlexTable panelsFlexTable = new FlexTable();
		panelsFlexTable.setBorderWidth(1);

		/**
		 * Contains the datasetsTree for selecting datasets.
		 */
		ScrollPanel datasetsScrollPanel = new ScrollPanel(this.pathwaysTree);
		datasetsScrollPanel.setHeight("200px");
		datasetsScrollPanel.setWidth("200px");

		panelsFlexTable.setText(0, 0, "select pathway from below");
		panelsFlexTable.setWidget(1, 0, datasetsScrollPanel);

		/**
		 * Contains the featuresTree for displaying pathway features.
		 */
		ScrollPanel featuresScrollPanel = new ScrollPanel(this.featuresTree);
		featuresScrollPanel.setHeight("200px");
		featuresScrollPanel.setWidth("200px");

		panelsFlexTable.setText(0, 1, "pathway features");
		panelsFlexTable.setWidget(1, 1, featuresScrollPanel);

		/**
		 * Contains the interactionsTree for displaying pathway interactions.
		 */
		ScrollPanel interactionsScrollPanel = new ScrollPanel(
				this.interactionsTree);
		interactionsScrollPanel.setHeight("200px");
		interactionsScrollPanel.setWidth("400px");

		panelsFlexTable.setText(0, 2, "pathway interactions");
		panelsFlexTable.setWidget(1, 2, interactionsScrollPanel);

		this.outermostPanel.add(panelsFlexTable);

		// initial loading of available BioInt pathways
		this.setupAvailablePathways(pathwayListCurrentRequest);
	}

	/**
	 * Get pathway features and interaction from BioInt database via IB's
	 * servlet.
	 * 
	 * @param biointID
	 * @param pathwayName
	 * @param request
	 */
	private void getPathwayFeaturesAndInteractions(final String biointID,
			final String pathwayName, Request request) {
		// clear the trees
		featuresTree.clear();
		featuresTree.setTitle("features for " + pathwayName);

		interactionsTree.clear();
		interactionsTree.setTitle("features for " + pathwayName);

		// cancel any running request
		if (request != null) {
			request.cancel();
			request = null;
		}

		RequestCallback featuresAndInteractionsRequestCallback = new RequestCallback() {
			@Override
			public void onResponseReceived(Request request, Response response) {
				// check for OK status code
				if (response.getStatusCode() != 200) {
					LoggingDialogBox
							.log("RequestCallback in BioIntPathwayDialogBox.getPathwayFeaturesAndInteractions() did not get OK status(200).  Status code: "
									+ response.getStatusCode());
					return;
				}

				// check for error message in JSON-RPC response
				JSONObject jsonRpcResultJO = JSONParser.parseStrict(
						response.getText()).isObject();

				if (!jsonRpcResultJO.get("error").toString()
						.equalsIgnoreCase("null")) {
					LoggingDialogBox.log("got an error: "
							+ jsonRpcResultJO.get("error").isObject()
									.get("message").isString().stringValue());
					return;
				}

				LoggingDialogBox
						.log("no error in JSON-RPC result object, continue");

				// TODO handle features
				JSONArray featuresJA = jsonRpcResultJO.get("result").isObject()
						.get("pathway_features").isArray();

				/**
				 * Map feature type to tree item
				 */
				HashMap<String, TreeItem> featureTypeToTreeItemHashMap = new HashMap<String, TreeItem>();

				// iterate through all the features
				for (int i = 0; i < featuresJA.size(); i++) {
					JSONObject featureJO = featuresJA.get(i).isObject();

					String type = featureJO.get("feature_type").isString()
							.stringValue();
					String name = featureJO.get("feature_name").isString()
							.stringValue();

					// add feature_name to type TreeItem
					if (featureTypeToTreeItemHashMap.containsKey(type)) {
						featureTypeToTreeItemHashMap.get(type).addItem(name);
					} else {
						TreeItem ti = new TreeItem(type);
						ti.addItem(name);
						featureTypeToTreeItemHashMap.put(type, ti);
					}
				}

				// add TreItem objects to Tree
				for (String type : featureTypeToTreeItemHashMap.keySet()) {
					featuresTree
							.addItem(featureTypeToTreeItemHashMap.get(type));
				}

				// TODO handle interactions
				JSONArray interactionsJA = jsonRpcResultJO.get("result")
						.isObject().get("pathway_interactions").isArray();

				/**
				 * Map link type to tree item
				 */
				HashMap<String, TreeItem> linkTypeToTreeItemHashMap = new HashMap<String, TreeItem>();

				// iterate through all the features
				for (int i = 0; i < interactionsJA.size(); i++) {
					JSONObject linkJO = interactionsJA.get(i).isObject();

					String link_type = linkJO.get("link_type").isString()
							.stringValue();
					String parent_type = linkJO.get("parent_type").isString()
							.stringValue();
					String parent_name = linkJO.get("parent_name").isString()
							.stringValue();
					String child_type = linkJO.get("child_type").isString()
							.stringValue();
					String child_name = linkJO.get("child_name").isString()
							.stringValue();

					String stringItem = parent_name + "(" + parent_type + ") "
							+ link_type + " " + child_name + "(" + child_type
							+ ")";

					// add feature_name to type TreeItem
					if (linkTypeToTreeItemHashMap.containsKey(link_type)) {
						linkTypeToTreeItemHashMap.get(link_type).addItem(
								stringItem);
					} else {
						TreeItem ti = new TreeItem(link_type);
						ti.addItem(stringItem);
						linkTypeToTreeItemHashMap.put(link_type, ti);
					}
				}

				// add TreItem objects to Tree
				for (String type : linkTypeToTreeItemHashMap.keySet()) {
					interactionsTree.addItem(linkTypeToTreeItemHashMap
							.get(type));
				}
			}

			@Override
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox
						.log("error with RequestCallback in BioIntPathwayDialogBox.getPathwayFeaturesAndInteractions(): "
								+ exception.toString());
			}
		};

		// setup request
		// http://localhost:8080/ib/data/queryBioInt/getPathwayFeaturesAndInteractions?bioInt_pathway_id=36934
		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
				"data/queryBioInt/getPathwayFeaturesAndInteractions?bioInt_pathway_id="
						+ biointID);

		// send request and specify the RequestCallback object
		try {
			request = rb.sendRequest(null,
					featuresAndInteractionsRequestCallback);
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
		}
	}

	/**
	 * Modify the Tree widget of RadioButton objects for selecting pathways.
	 * 
	 * @param request
	 */
	private void setupAvailablePathways(Request request) {
		// clear the Tree
		this.pathwaysTree.clear();

		// check for running request
		if (request != null) {
			request.cancel();
			request = null;
			LoggingDialogBox
					.log("cancelled a running request for pathwayListCurrentRequest");
		}

		RequestCallback pathwayListRequestCallback = new RequestCallback() {

			@Override
			public void onResponseReceived(Request request, Response response) {
				// check for OK status code
				if (response.getStatusCode() != 200) {
					LoggingDialogBox
							.log("RequestCallback in BioIntPathwayDialogBox.setupAvailablePathways() did not get OK status(200).  Status code: "
									+ response.getStatusCode());
					return;
				}

				// DataVizPanel.log(response.getText());

				// check for error message in JSON-RPC response
				JSONObject jsonRpcResultJO = JSONParser.parseStrict(
						response.getText()).isObject();

				if (!jsonRpcResultJO.get("error").toString()
						.equalsIgnoreCase("null")) {
					LoggingDialogBox.log("got an error: "
							+ jsonRpcResultJO.get("error").isObject()
									.get("message").isString().stringValue());
					return;
				}

				LoggingDialogBox
						.log("no error in JSON-RPC result object, continue");

				// get the pathways from JSON-RPC result object and store in
				// a hash
				JSONArray pathwaysJA = jsonRpcResultJO.get("result").isObject()
						.get("pathways").isArray();

				/**
				 * Map tissue to a TreeItem object.
				 */
				HashMap<String, TreeItem> sourceToTreeItemHashMap = new HashMap<String, TreeItem>();

				// iterate through all the pathways
				for (int i = 0; i < pathwaysJA.size(); i++) {
					JSONObject pathwayJO = pathwaysJA.get(i).isObject();
					double pathway_id = pathwayJO.get("pathway_id").isNumber()
							.doubleValue();
					String source = pathwayJO.get("source").isString()
							.stringValue();
					String name = pathwayJO.get("name").isString()
							.stringValue();

					// make RadioButton for pathway
					RadioButton rb = new RadioButton("pathway_id", name);
					rb.setFormValue(pathway_id + "");
					rb.addValueChangeHandler(pathwayRadioButtonValueChangeHandler);

					// add RadioButton to source TreeItem
					if (sourceToTreeItemHashMap.containsKey(source)) {
						sourceToTreeItemHashMap.get(source).addItem(rb);
					} else {
						TreeItem ti = new TreeItem(source);
						ti.addItem(rb);
						sourceToTreeItemHashMap.put(source, ti);
					}
				}

				// add TreItem objects to Tree
				ArrayList<String> sourceList = new ArrayList<String>(
						sourceToTreeItemHashMap.keySet());
				Collections.sort(sourceList);
				for (String source : sourceList) {
					pathwaysTree.addItem(sourceToTreeItemHashMap.get(source));
				}

			}

			@Override
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox
						.log("error with RequestCallback in BioIntPathwayDialogBox.setupAvailablePathways(): "
								+ exception.toString());
			}

		};

		// setup request
		// http://localhost:8080/ib/data/queryBioInt/getPathways
		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
				"data/queryBioInt/getPathways");

		// send request and specify the RequestCallback object
		try {
			request = rb.sendRequest(null, pathwayListRequestCallback);
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
		}
	}

	/**
	 * Calls DialogBox.hide().
	 */
	private void dismiss() {
		this.hide();
	}
}
