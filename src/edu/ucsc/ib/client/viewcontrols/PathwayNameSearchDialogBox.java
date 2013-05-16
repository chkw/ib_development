package edu.ucsc.ib.client.viewcontrols;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.SourceSystemSpaceListener;

/**
 * Child class of IbDialogBox for searching pathway names.
 * 
 * @author cw
 * 
 */
public class PathwayNameSearchDialogBox extends IbDialogBox implements
		SourceSystemSpaceListener {

	private static final String URL_FOR_GET_PATHWAY_DATA = "data/pathway/getPathway";

	private static final String URL_FOR_SEARCH_PATHWAYS = "data/pathway/searchPathways";

	private static final String DEFAULT_SEARCH_TEXT = "filter pathway names";

	/**
	 * Hold pathways returned from server query
	 */
	protected static JSONArray searchPathwayResultsJA = new JSONArray();

	private static Request currentRequest;

	private static NetworkVisualization nv;

	/**
	 * Contains controls for selecting pathways.
	 */
	private final static FlexTable pathwaySelectionFlexTable = new FlexTable();

	/**
	 * ScrollPanel for pathway selection controls
	 */
	private final static ScrollPanel pathwaySelectionScrollPanel = new ScrollPanel(
			pathwaySelectionFlexTable);
	static {
		pathwaySelectionScrollPanel.setSize("500px", "300px");
	}

	/**
	 * Visually indicate running request.
	 */
	private final static Image throbber = new Image();
	static {
		throbberStop();
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

				buildPathwaySelectionButtons(searchPathwayResultsJA, value);
			}

		});
	}

	// TODO ///////////////////////////////////////////////////////////////

	public PathwayNameSearchDialogBox(final NetworkVisualization netviz) {
		super("Search Pathway Name");

		nv = netviz;

		final VerticalPanel outermostPanel = new VerticalPanel();
		outermostPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
		outermostPanel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);

		final HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
		buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
		buttonPanel.add(filterTextBox);
		buttonPanel.add(throbber);

		outermostPanel.add(buttonPanel);
		outermostPanel.add(pathwaySelectionScrollPanel);

		setWidget(outermostPanel);

		SearchSpaceControl.addSystemSpaceListener(this);

		// TODO set focus on textbox
		// filterTextBox.setFocus(true);
	}

	/**
	 * Wrapper for {@link #searchPathways(String, String , Request)
	 * searchPathways(String, String , Request)}. Uses this object's Request
	 * object.
	 * 
	 * @param searchString
	 * @param ncbi_tax
	 */
	private static void searchPathways(final String searchString,
			final String ncbi_tax) {
		searchPathways(searchString, ncbi_tax, currentRequest);
	}

	/**
	 * Perform pathway name search and update the pathway selection controls
	 * with the results.
	 * 
	 * @param searchString
	 *            search String to use. If null, then all available pathways
	 *            returned
	 * @param ncbi_tax
	 *            should be NCBI tax ID
	 * @param request
	 */
	public static void searchPathways(final String searchString,
			final String ncbi_tax, Request request) {

		throbberStart();

		// cancel any running request
		if (request != null) {
			request.cancel();
			request = null;
		}

		RequestCallback requestCallback = new RequestCallback() {
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("searchPathways failed");
				LoggingDialogBox.log("=====================================");
				LoggingDialogBox.log(exception.toString());
				LoggingDialogBox.log("=====================================");
				throbberStop();
			}

			public void onResponseReceived(Request request, Response response) {
				// get the results
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						response.getText());

				// check for error message in JSON-RPC response
				if (jsonRpcResp.hasError()) {
					LoggingDialogBox.log("got an error: "
							+ jsonRpcResp.getError().toString());
					throbberStop();
					return;
				}

				LoggingDialogBox
						.log("no error in JSON-RPC result object, continue");

				JSONObject resultJO = jsonRpcResp.getResult();

				searchPathwayResultsJA = resultJO.get("pathways").isArray();

				buildPathwaySelectionButtons(searchPathwayResultsJA, "");

				throbberStop();
			}
		};

		String urlString = URL_FOR_SEARCH_PATHWAYS + "?organism=" + ncbi_tax
				+ "&search=" + searchString;

		LoggingDialogBox.log(urlString);

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlString);
		try {
			request = rb.sendRequest(null, requestCallback);
		} catch (Exception e) {
			LoggingDialogBox.log("exception sending request " + urlString
					+ ": " + e.toString());
		}
		return;
	}

	/**
	 * Build pathway selection controls for the specified group of pathways.
	 * 
	 * @param pathwayNamesJA
	 *            JSONArray of JSONObjects with keys: pathway_id, source, and
	 *            name.
	 * @param filter
	 *            TODO
	 */
	private static void buildPathwaySelectionButtons(
			final JSONArray pathwayNamesJA, String filter) {
		throbberStart();

		// clear current controls from FlexTable
		pathwaySelectionFlexTable.removeAllRows();

		// create new controls
		for (int i = 0; i < pathwayNamesJA.size(); i++) {
			JSONObject pathwayJO = pathwayNamesJA.get(i).isObject();
			final String id = pathwayJO.get("pathway_id").isString()
					.stringValue();
			String source = pathwayJO.get("source").isString().stringValue();
			String name = pathwayJO.get("name").isString().stringValue();
			double num_concepts = pathwayJO.get("num_concepts").isNumber()
					.doubleValue();
			double num_relations = pathwayJO.get("num_relations").isNumber()
					.doubleValue();

			String labelString = name + " from " + source + " (" + num_concepts
					+ " concepts with " + num_relations + " relations)";

			// filter
			boolean display = true;

			for (String word : filter.toLowerCase().split("\\s+")) {
				if (labelString.toLowerCase().indexOf(word) == -1) {
					display = false;
				}
			}

			if (display == true) {
				// create pathway selection control
				Button pathwayButton = new Button("+", new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						getPathwayData(id);
					}
				});

				Label pathwayLabel = new Label(labelString);

				// add control to FlexTable
				int insertRow = pathwaySelectionFlexTable.getRowCount();
				pathwaySelectionFlexTable
						.setWidget(insertRow, 0, pathwayButton);
				pathwaySelectionFlexTable.setWidget(insertRow, 1, pathwayLabel);
			} else {
				// don't add it
			}

		}

		// reset scrollPanel's scrolls
		pathwaySelectionScrollPanel.scrollToTop();

		throbberStop();
	}

	/**
	 * Wrapper for {@link #getPathwayData(String, Request)
	 * getPathwayData(String, Request)}. Uses this object's Request object.
	 * 
	 * @param pathway_id
	 */
	private static void getPathwayData(final String pathway_id) {
		getPathwayData(pathway_id, currentRequest);
	}

	/**
	 * Get the relations and concepts for the specified pathway and then display
	 * it.
	 * 
	 * @param pathway_id
	 * @param request
	 */
	public static void getPathwayData(final String pathway_id, Request request) {
		throbberStart();

		// cancel any running request
		if (request != null) {
			request.cancel();
			request = null;
		}

		RequestCallback requestCallback = new RequestCallback() {
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("getPathwayData failed");
				LoggingDialogBox.log("=====================================");
				LoggingDialogBox.log(exception.toString());
				LoggingDialogBox.log("=====================================");
				throbberStop();
			}

			public void onResponseReceived(Request request, Response response) {
				// get the results
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						response.getText());

				// check for error message in JSON-RPC response
				if (jsonRpcResp.hasError()) {
					LoggingDialogBox.log("got an error: "
							+ jsonRpcResp.getError().toString());
					throbberStop();
					return;
				}

				LoggingDialogBox
						.log("no error in JSON-RPC result object, continue");

				JSONObject resultJO = jsonRpcResp.getResult();

				// handle pathway data
				if (PathwayData.resultJoIsValid(resultJO)) {
					// get PathwayData object
					PathwayData pathwayData = new PathwayData(resultJO);

					// visualize pathway
					pathwayData.visualize(nv);
				}

				throbberStop();
			}
		};

		String urlString = URL_FOR_GET_PATHWAY_DATA + "?pathway=" + pathway_id;

		LoggingDialogBox.log(urlString);

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlString);
		try {
			request = rb.sendRequest(null, requestCallback);
		} catch (Exception e) {
			LoggingDialogBox.log("exception sending request " + urlString
					+ ": " + e.toString());
		}
		return;
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
		searchPathways("", systemSpace);
	}

	@Override
	public void viewSystemSpaceChanged(String viewSystemSpace) {
		// TODO Auto-generated method stub

	}
}
