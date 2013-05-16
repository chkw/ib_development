package edu.ucsc.ib.client.viewcontrols;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.EntityInfo;
import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.SelectedSetListener;

/**
 * checkout <a href=
 * "https://code.google.com/webtoolkit/doc/latest/DevGuideUiCellWidgets.html"
 * >DevGuideUiCellWidgets</a> for more info
 * 
 * @author cw
 * 
 */
public class ShortestPathDialogBox extends IbDialogBox implements
		SelectedSetListener {

	private static final String SHORTEST_PATH_SERVICE_URL = "data/trackdb/shortestPath";

	private static Request currentRequest;

	private static NetworkVisualization nv;

	/**
	 * Contains biodes to be considered as origin nodes in shortest path
	 * analysis.
	 */
	private static final BiodeSet originSet = new BiodeSet();

	/**
	 * Contains biodes to be considered as destination nodes in shortest path
	 * analysis.
	 */
	private static final BiodeSet destinationSet = new BiodeSet();

	/**
	 * ValueChangeHandler for CheckBox that adds/removes biodes to/from
	 * originSet.
	 */
	private static final ValueChangeHandler<Boolean> originCheckBoxValueChangeHandler = new ValueChangeHandler<Boolean>() {
		@Override
		public void onValueChange(ValueChangeEvent<Boolean> event) {
			Object source = event.getSource();
			if (!(source instanceof CheckBox)) {
				return;
			}
			boolean isChecked = ((CheckBox) source).getValue();
			String biode = ((CheckBox) source).getName();

			if (isChecked) {
				originSet.add(biode);
			} else {
				originSet.remove(biode);
			}

//			LoggingDialogBox.log("originSet: " + originSet.toString());
		}
	};

	/**
	 * ValueChangeHandler for CheckBox that adds/removes biodes to/from
	 * destinationSet.
	 */
	private static final ValueChangeHandler<Boolean> destinationCheckBoxValueChangeHandler = new ValueChangeHandler<Boolean>() {
		@Override
		public void onValueChange(ValueChangeEvent<Boolean> event) {
			Object source = event.getSource();
			if (!(source instanceof CheckBox)) {
				return;
			}
			boolean isChecked = ((CheckBox) source).getValue();
			String biode = ((CheckBox) source).getName();

			if (isChecked) {
				destinationSet.add(biode);
			} else {
				destinationSet.remove(biode);
			}

			// LoggingDialogBox
			// .log("destinationSet: " + destinationSet.toString());
		}
	};

	/**
	 * Button to begin shortest path analysis.
	 */
	private static final Button getShortestPathButton = new Button(
			"get shortest path", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					// LoggingDialogBox.log("originSet: " + getOriginSet());
					// LoggingDialogBox.log("destinationSet: "
					// + getDestinationSet());

					if (getOriginSet().size() < 1) {
						// no origin
						Window.alert("At least one origin node is required to find the shortest path.");
					} else if (getDestinationSet().size() < 1) {
						// no destination
						Window.alert("At least one destination node is required to find the shortest path.");
					} else if (nv.getTrackNamesHash(false).size() < 1) {
						// no network
						if (TrackControlPanel.getTrackSpace().equalsIgnoreCase(
								"9606")) {
							TrackControlPanel.turnOnTrack("UCSC_Superpathway");
							Window.alert("At least one network is required to find the shortest path. UCSC_Superpathway will be used.");
							getShortestPath();
						} else {
							Window.alert("At least one network is required to find the shortest path.");
						}

					} else {
						// origin, destination, and network have been specified
						getShortestPath();
					}
				}
			});

	/**
	 * Button to clear origin selections
	 */
	private static final Button clearOriginSetButton = new Button(
			"clear origin", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					clearOriginSet();
				}
			});

	/**
	 * Button to clear destination selections
	 */
	private static final Button clearDestinationSetButton = new Button(
			"clear destination", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					clearDestinationSet();
				}
			});

	/**
	 * Contains controls for including biode in originSet or destinationSet.
	 */
	private final static FlexTable controlsFlexTable = new FlexTable();

	private final static Image throbber = new Image();
	{
		throbberStop();
	}

	// TODO ///////////////////////////////////////////////////////////////

	public ShortestPathDialogBox(final NetworkVisualization netviz) {
		super("ShortestPathDialogBox");

		nv = netviz;
		nv.addSelectedSetListener(this);

		final VerticalPanel outermostPanel = new VerticalPanel();
		outermostPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
		outermostPanel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);

		setupBiodeControls(nv.getCurrentNodeIds());
		outermostPanel.add(controlsFlexTable);

		final HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
		buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
		buttonPanel.add(clearOriginSetButton);
		buttonPanel.add(clearDestinationSetButton);
		buttonPanel.add(getShortestPathButton);
		buttonPanel.add(throbber);

		outermostPanel.add(buttonPanel);

		setWidget(outermostPanel);
	}

	/**
	 * Wrapper for {@link #getShortestPath(Set,Set,Set,Request)}.
	 */
	protected static void getShortestPath() {
		throbberStart();
		getShortestPath(getOriginSet(), getDestinationSet(),
				nv.getTrackNamesHash(false), currentRequest);
	}

	/**
	 * Submit request to shortest path service.
	 * 
	 * @param origins
	 * @param destinations
	 * @param trackNames
	 * @param request
	 */
	public static void getShortestPath(final Set<String> origins,
			final Set<String> destinations, final Set<String> trackNames,
			Request request) {

		// cancel any running request
		if (request != null) {
			request.cancel();
			request = null;
		}

		RequestCallback requestCallback = new RequestCallback() {
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("getShortestPath failed");
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

				// figure out which one of returned paths is shortest
				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();

				JSONArray shortPathsJA = resultJO.get("shortestPaths")
						.isArray();
				for (int i = 0; i < shortPathsJA.size(); i++) {
					JSONObject shortPathJO = shortPathsJA.get(i).isObject();
					if (shortPathJO.get("distance").isNumber().doubleValue() != 0) {
						// don't consider single node paths (origin == dest)
						joList.add(shortPathJO);
					}
				}

				// sort the results by score
				Collections.sort(joList, new JSONNumberComparator("distance"));

				// get JSONObject with shortest path
				JSONObject shortestPathJO = joList.get(0).isObject();

				// get JSONarray of biodes
				JSONArray ja = shortestPathJO.get("path").isArray();

				// get array of biodes
				String[] biodes = new String[ja.size()];
				for (int i = 0; i < ja.size(); i++) {
					biodes[i] = ja.get(i).isString().stringValue();
				}

				// find out which biodes are new biodes
				final BiodeSet resultBiodes = nv.getNewBiodes(new BiodeSet(
						Arrays.asList(biodes)));

				// using DialogBox instead of PopupPanel
				final DialogBox resultsDialogBox = new DialogBox();
				resultsDialogBox.setText("shortest path");

				HorizontalPanel buttonPanel = new HorizontalPanel();

				Button createNodesButton = new Button("create nodes in graph",
						new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								// add biodes to netviz
								// this step should involve lookup service to
								// get all biodeinfo for the items to add

								BiodeUserInputPanel
										.processSubmissionWithLookupService_single_sp(
												SearchSpaceControl
														.getSystemspace(),
												resultBiodes.getArray(), true);

								resultsDialogBox.hide();
							}

						});

				Button cancelButton = new Button("cancel", new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						resultsDialogBox.hide();
					}

				});

				buttonPanel.add(createNodesButton);
				buttonPanel.add(cancelButton);

				VerticalPanel vp = new VerticalPanel();

				int numResults = resultBiodes.size();

				double distance = shortestPathJO.get("distance").isNumber()
						.doubleValue();
				if ((shortPathsJA.size() > 0 && joList.size() == 0)
						|| distance >= Double.MAX_VALUE) {
					// shortest path was either 0 or infinite distance
					// inf indicates no path that includes a destination
					// 0 dist indicates path with origin = dest
					vp.add(new Label("No path was found."));
				}
				if (numResults < 1) {
					createNodesButton.setEnabled(false);
					vp.add(new Label("No nodes to add to graph."));
				} else {
					vp.add(new Label("The shortest path has " + numResults
							+ " nodes not currently displayed."));
				}

				vp.add(buttonPanel);

				resultsDialogBox.add(vp);

				throbberStop();

				MainMenuBar.showDialogBox(resultsDialogBox, true);

				createNodesButton.setFocus(true);
			}
		};

		// get a result object
		String trackListString = BiodeControlPanel
				.arrayToCommaSeparatedString(trackNames.toArray(new String[0]));

		String originsListString = BiodeControlPanel
				.arrayToCommaSeparatedString(origins.toArray(new String[0]));

		String destinationsListString = BiodeControlPanel
				.arrayToCommaSeparatedString(destinations
						.toArray(new String[0]));

		String urlString = SHORTEST_PATH_SERVICE_URL + "?trackList="
				+ trackListString + "&origins=" + originsListString
				+ "&destinations=" + destinationsListString;

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
	 * Set up controls for current biodes and add them to controlsFlexTable.
	 * Includes Checkbox objects for inclusion in originSet and destinationSet.
	 * 
	 * @param bs
	 */
	private void setupBiodeControls(final BiodeSet bs) {
		// get current biodes
		List<EntityInfo> entityInfoList = new ArrayList<EntityInfo>();

		bs.addAll(originSet);
		bs.addAll(destinationSet);

		for (String biode : bs) {
			EntityInfo ei = nv.BIC.getBiodeInfo(biode);
			if (ei != null) {
				entityInfoList.add(ei);
			} else {
				originSet.remove(biode);
				destinationSet.remove(biode);
			}
		}

		// sort by common name
		Collections.sort(entityInfoList, EntityInfo.commonNameComparator);

		// clear originSet and destinationSet
		// originSet.clear();
		// destinationSet.clear();

		// create controls
		controlsFlexTable.clear();
		int row = 0;
		int column = 0;

		// column headings
		CellFormatter cellFormatter = controlsFlexTable.getCellFormatter();
		controlsFlexTable.setText(row, column++, "origin");
		controlsFlexTable.setText(row, column, "node");
		cellFormatter.setHorizontalAlignment(row, column++,
				HasHorizontalAlignment.ALIGN_CENTER);
		controlsFlexTable.setText(row, column++, "destination");

		// controls
		for (EntityInfo ei : entityInfoList) {
			String biode = ei.getSystematicName();

			CheckBox originCheckBox = new CheckBox();
			originCheckBox.setName(biode);
			originCheckBox.setTitle("origin set");
			originCheckBox
					.addValueChangeHandler(originCheckBoxValueChangeHandler);
			if (originSet.contains(biode)) {
				originCheckBox.setValue(true, true);
			}

			CheckBox destinationCheckBox = new CheckBox();
			destinationCheckBox.setName(biode);
			destinationCheckBox.setTitle("destination set");
			destinationCheckBox
					.addValueChangeHandler(destinationCheckBoxValueChangeHandler);
			if (destinationSet.contains(biode)) {
				destinationCheckBox.setValue(true, true);
			}

			column = 0;
			controlsFlexTable.setWidget(++row, column++, originCheckBox);
			controlsFlexTable.setWidget(row, column++,
					new Label(ei.getCommonName()));
			controlsFlexTable.setWidget(row, column++, destinationCheckBox);
		}
	}

	/**
	 * uncheck all originCheckBox widgets to empty the originSet
	 */
	private static void clearOriginSet() {
		int rowCount = controlsFlexTable.getRowCount();

		for (int row = 1; row < rowCount; row++) {
			CheckBox cb = (CheckBox) controlsFlexTable.getWidget(row, 0);
			cb.setValue(false, true);
		}
	}

	/**
	 * uncheck all destinationCheckBox widgets to empty the destinationSet
	 */
	private static void clearDestinationSet() {
		int rowCount = controlsFlexTable.getRowCount();

		for (int row = 1; row < rowCount; row++) {
			CheckBox cb = (CheckBox) controlsFlexTable.getWidget(row, 2);
			cb.setValue(false, true);
		}
	}

	/**
	 * Get the originSet.
	 * 
	 * @return
	 */
	private static Set<String> getOriginSet() {
		return originSet;
	}

	/**
	 * Get the destinationSet.
	 * 
	 * @return
	 */
	private static Set<String> getDestinationSet() {
		return destinationSet;
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

	/**
	 * Number Comparator for JSONObject. The field for comparing is specified.
	 * 
	 * @author cw
	 * 
	 */
	public static class JSONNumberComparator implements Comparator<JSONObject> {

		/**
		 * name of JSON field to compare
		 */
		private String fieldName;

		/**
		 * Number Comparator for JSONObject. The field for comparing is
		 * specified.
		 * 
		 * @param fieldName
		 */
		public JSONNumberComparator(final String fieldName) {
			this.fieldName = fieldName;
		}

		@Override
		public int compare(JSONObject o1, JSONObject o2) {
			double val0, val1 = 0;
			int result = 0;

			val0 = o1.get(this.fieldName).isNumber().doubleValue();
			val1 = o2.get(this.fieldName).isNumber().doubleValue();

			if (val0 > val1) {
				result = 1;
			} else if (val0 < val1) {
				result = -1;
			} else {
				result = 0;
			}

			return result;
		}
	}

	@Override
	public void selectedBiodes(BiodeSet b) {
		BiodeSet bs = nv.getSelectedNodeIds();
		bs.addAll(b);
		setupBiodeControls(bs);
	}

	@Override
	public void deselectedBiodes(BiodeSet b) {
		BiodeSet bs = nv.getSelectedNodeIds();
		bs.removeAll(b);
		setupBiodeControls(bs);
	}
}
