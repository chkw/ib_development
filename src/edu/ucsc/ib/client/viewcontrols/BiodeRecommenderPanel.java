package edu.ucsc.ib.client.viewcontrols;

import java.util.HashMap;
import java.util.Iterator;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;

public class BiodeRecommenderPanel extends Composite implements ViewControl {

	/**
	 * species for use with ClueGene
	 */
	// private static final String[] species = { "worm", "fly", "human",
	// "mouse",
	// "yeast" };

	/**
	 * for maxResults option in ClueGene
	 */
	private static final String[] maxResults = { "5", "10", "20", "30" };

	private static final String STOPPED_THROBBER = "notloading_16.png";

	private static final String LOADING_THROBBER = "loading_16.gif";

	private static final String CSS_CLASS = "ib-biodeControlPanel";

	private static final String INNER_PANEL_HEIGHT = "20em";

	private static final String INNER_PANEL_WIDTH = "15em";

	private final TextArea inputArea;

	private final RadioButton cluegeneRadioButton;

	private final RadioButton generecommenderRadioButton;

	private final FlexTable resultsTable;

	// private final ListBox speciesListBox;

	private final ListBox maxResultsListBox;

	private final Image throbber;

	private final VerticalPanel buttonPanel;

	private final VerticalPanel panel;

	private final BiodeControlPanel bcp;

	private final NetworkVisualization netviz;

	private final SearchSpaceControl ssc;

	private Request currentRequest;

	public BiodeRecommenderPanel(BiodeControlPanel bcp, NetworkVisualization nv) {

		this.bcp = bcp;
		this.netviz = nv;
		this.ssc = this.bcp.getSearchSpaceControl();

		this.maxResultsListBox = buildMaxResultsListBox(maxResults);

		HorizontalPanel hp = new HorizontalPanel();
		hp.add(this.maxResultsListBox);

		this.inputArea = buildInputArea();

		this.cluegeneRadioButton = new RadioButton("recommender", "ClueGene");
		this.generecommenderRadioButton = new RadioButton("recommender",
				"GeneRecommender");

		// Note: Don't need to do setFormValue, as we aren't using FormPanel
		this.cluegeneRadioButton.setValue(true);

		this.resultsTable = new FlexTable();

		this.throbber = new Image();
		this.throbber.setTitle("throbber");
		this.throbber.setUrl(STOPPED_THROBBER);

		this.buttonPanel = buildButtonPanel();

		this.panel = new VerticalPanel();
		this.panel.add(hp);
		this.panel.add(this.inputArea);
		this.panel.add(this.cluegeneRadioButton);
		this.panel.add(this.generecommenderRadioButton);
		this.panel.add(this.buttonPanel);
		this.panel.add(this.resultsTable);
		this.panel.setSize(INNER_PANEL_WIDTH, INNER_PANEL_HEIGHT);

		initWidget(this.panel);
		setStyleName(CSS_CLASS);
	}

	/**
	 * 
	 * @return the selected species value as String
	 */
	private String getSpecies() {
		return this.bcp.getSystemspace();
	}

	/**
	 * 
	 * @return the selected maxResults value as a String rather than int because
	 *         it will be used as part of a command in a ShellProcess.
	 */
	private String getMaxResults() {
		return this.maxResultsListBox.getValue(this.maxResultsListBox
				.getSelectedIndex());
	}

	/**
	 * 
	 * @param results
	 *            selectable values in String[]
	 * @return ListBox for selecting maxresults for ClueGene
	 */
	private ListBox buildMaxResultsListBox(String[] results) {
		ListBox lb = new ListBox();
		lb.setTitle("select maximum number of results");
		lb.setVisibleItemCount(1);
		for (int i = 0; i < results.length; i++) {
			lb.addItem(results[i], results[i]);
		}
		lb.setSelectedIndex(2);
		return lb;
	}

	/**
	 * 
	 * @return TextArea for entering query genes
	 */
	private TextArea buildInputArea() {
		TextArea input = new TextArea();
		input.setWidth("100%");
		input.setVisibleLines(5);
		input.setTitle("enter query genes for recommender");

		// input
		// .setText("YLR264W YBR191W YMR142C YOR063W YLR439W YOR369C YNL067W YOL039W YLR061W YLR388W");
		return input;
	}

	/**
	 * 
	 * @return VerticalPanel containing buttons and throbber
	 */
	private VerticalPanel buildButtonPanel() {
		VerticalPanel vp = new VerticalPanel();
		vp.add(makeClearButton());
		vp.add(makeGetSelectedSetButton());
		vp.add(makeSubmitButton());
		vp.add(makeAddAllResultsButton());
		vp.add(this.throbber);
		return vp;
	}

	/**
	 * 
	 * @return Button to submit query to server
	 */
	private Button makeSubmitButton() {
		Button b = new Button();
		b.setText("submit query");
		b.setTitle("submit query genes to server");

		b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				// turn on throbber
				throbber.setUrl(LOADING_THROBBER);

				// cancel any running request
				if (currentRequest != null) {
					currentRequest.cancel();
					currentRequest = null;
				}

				LoggingDialogBox.log("BiodeRecommenderPanel: selected "
						+ getSelectedRecommender());

				RequestCallback rc = new RequestCallback() {

					public void onError(Request request, Throwable exception) {
						LoggingDialogBox
								.log("BiodeRecommenderPanel: search has FAILED");
						LoggingDialogBox.log(exception.toString());

						// turn off throbber
						throbber.setUrl(STOPPED_THROBBER);
					}

					public void onResponseReceived(Request request,
							Response response) {
						// Auto-generated method stub

						LoggingDialogBox.log("got recommender results: "
								+ response.getText());

						// result is a stringified JSONObject
						JSONObject resultJO = (JSONObject) JSONParser
								.parseStrict(response.getText());

						JSONArray resultsJA = resultJO.get("results").isArray();

						if ((resultsJA != null) && (resultsJA.size() > 0)) {
							String[] resultArray = new String[resultsJA.size()];
							for (int i = 0; i < resultsJA.size(); i++) {
								resultArray[i] = resultsJA.get(i).isString()
										.stringValue();
							}

							addToResultsTable(resultArray);
						} else {
							// do nothing
						}

						// turn off throbber
						throbber.setUrl(STOPPED_THROBBER);
					}
				};

				String recommenderType = "clueGene";
				if (getSelectedRecommender().equalsIgnoreCase("gr")) {
					recommenderType = "geneRecommender";
				}

				StringBuffer urlSB = new StringBuffer(
						"data/biodeRecommender/recommender?");
				urlSB.append("recommenderType=" + recommenderType);
				urlSB.append("&organism=" + getSpecies());
				urlSB.append("&queryList=" + getInput());
				urlSB.append("&maxResults=" + getMaxResults());

				RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
						urlSB.toString());
				try {
					if (getInput().length() > 0) {
						LoggingDialogBox.log("Beginning request:\t"
								+ urlSB.toString());
						currentRequest = rb.sendRequest(null, rc);
						LoggingDialogBox.log("request sent");
					} else {
						// do nothing
					}
				} catch (Exception e) {
					LoggingDialogBox.log("Unknown exeception: " + e.toString());
				}
			}
		});
		return b;
	}

	/**
	 * 
	 * @return Button to clear inputArea
	 */
	private Button makeClearButton() {
		Button b = new Button();
		b.setText("clear results");
		b.setTitle("clear results");

		b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				clearResultsTable();
			}
		});
		return b;
	}

	/**
	 * 
	 * @return Button to add all recommendation results
	 */
	private Button makeAddAllResultsButton() {
		Button b = new Button();
		b.setText("add all results");
		b.setTitle("add all recommendations");

		b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				addAllResults();
			}
		});
		return b;
	}

	/**
	 * 
	 * @return Button to add selected nodes to the inputArea
	 */
	private Button makeGetSelectedSetButton() {
		Button b = new Button();
		b.setText("add selected");
		b.setTitle("adds selected nodes to the input area");

		b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				BiodeSet selected = netviz.getSelectedNodeIds();
				addToInputArea(selected.getArray());
			}
		});
		return b;
	}

	/**
	 * Gets the text from inputArea, then creates a String where each individual
	 * entry separated by a space.
	 * 
	 * @return individual entries in a space-separated String
	 */
	private String getInput() {
		String[] entries = this.inputArea.getText().split("\\s+");
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < entries.length; i++) {
			sb.append(entries[i] + " ");
		}
		return sb.toString().trim();
	}

	/**
	 * Clear inputArea
	 * 
	 */
	private void clearInputArea() {
		this.inputArea.setText(null);
	}

	/**
	 * Fill the resultsTable FlexTable with rows, where each row is a biode
	 * recommendation.
	 * 
	 * @param results
	 *            recommender's results in String[]
	 */
	private void addToResultsTable(String[] results) {
		int nextRow = this.resultsTable.getRowCount();
		for (int i = 0; i < results.length; i++) {
			// TODO check to see if recommendation is already in NetViz
			this.resultsTable.setText(nextRow, 0, results[i]);
			this.resultsTable.setWidget(nextRow, 1, makeAddButton(results[i]));
			nextRow++;
		}
	}

	private Button makeAddButton(String biode) {
		Button b = new Button("add");
		b.setTitle(biode);

		b.addClickHandler(new ClickHandler() {

			String queryBiode;

			@Override
			public void onClick(ClickEvent event) {
				// turn on throbber
				throbber.setUrl(LOADING_THROBBER);

				queryBiode = ((Widget) event.getSource()).getTitle();

				RequestCallback rc = new RequestCallback() {

					public void onError(Request request, Throwable exception) {
						LoggingDialogBox
								.log("BiodeRecommenderPanel: recommender has FAILED");
						LoggingDialogBox.log(exception.toString());

						// turn off throbber
						throbber.setUrl(STOPPED_THROBBER);
					}

					public void onResponseReceived(Request request,
							Response response) {
						String jsonResultString = response.getText();

						if (jsonResultString == null) {
							LoggingDialogBox
									.log("BiodeRecommenderPanel: ID search returned null object");
							// turn off throbber
							throbber.setUrl(STOPPED_THROBBER);
							return;
						}

						// get the results
						LoggingDialogBox
								.log("BiodeRecommenderPanel: ID search got non-null object");
						// DataVizPanel.log(jsonResultString);

						// clear the input area
						clearInputArea();

						// process the JSON result
						HashMap<String, BiodeInfo> resultHash = BiodeControlPanel
								.jsonArrayToBiodeHash(JSONParser
										.parseStrict(jsonResultString)
										.isObject().get("annotations")
										.isArray());
						LoggingDialogBox
								.log("BiodeRecommenderPanel: number results "
										+ resultHash.size());
						String key;
						BiodeInfo bi;
						for (Iterator<String> iter = resultHash.keySet()
								.iterator(); iter.hasNext();) {
							key = iter.next();
							bi = resultHash.get(key);
							ssc.setBiodeInfoSpaces(bi);
							if (bi != null) {
								// DataVizPanel.log(key + " : " +
								// bi.getCommonName());
								netviz.addBiodeInfoToBIC(bi);
								netviz.addBiode(new BiodeSet(bi
										.getSystematicName()));

								// remove the row from resultsTable
								removeFromResultsTable(queryBiode);
							} else {
								LoggingDialogBox.log(key + " : " + "null");
							}
						}
						// turn off throbber
						throbber.setUrl(STOPPED_THROBBER);
					}
				};

				String urlStr = "data/annodb/aliasToAnnot2?organism="
						+ bcp.getSystemspace() + "&list=" + queryBiode;

				RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
						urlStr);
				try {
					LoggingDialogBox
							.log("BiodeRecommenderPanel ... Beginning request:\t"
									+ urlStr);
					currentRequest = rb.sendRequest(null, rc);
					LoggingDialogBox.log("request sent");
				} catch (Exception e) {
					LoggingDialogBox.log(e.toString());
				}
			}
		});
		return b;
	}

	/**
	 * Find the index for row in the resultsTable FlexTable corresponding to a
	 * biode.
	 * 
	 * @param biode
	 * @return index of the row or -1 if not found
	 */
	private int findRow(String biode) {
		int result = -1;
		for (int i = 0; i < this.resultsTable.getRowCount(); i++) {
			if (this.resultsTable.getWidget(i, 1).getTitle()
					.equalsIgnoreCase(biode)) {
				result = i;
				break;
			}
		}
		return result;
	}

	/**
	 * Clear the resultsTable FlexTable.
	 * 
	 */
	private void clearResultsTable() {
		// There is a bug with FlexTable.removeRow() that can be avoided by
		// removing nodes from the botton of the table.
		int rows = this.resultsTable.getRowCount();
		for (int i = rows - 1; i >= 0; i--) {
			this.resultsTable.removeRow(i);
		}
	}

	/**
	 * TODO I think this is a hack. We should do it a better way.
	 * 
	 */
	private void addAllResults() {
		for (int i = this.resultsTable.getRowCount() - 1; i >= 0; i--) {
			((Button) this.resultsTable.getWidget(i, 1)).click();
		}
	}

	/**
	 * Remove a row the resultsTable FlexTable.
	 * 
	 * @param key
	 *            String
	 */
	private void removeFromResultsTable(String key) {
		int row = findRow(key);
		resultsTable.removeRow(row);
	}

	/**
	 * Appends additional String to the current String in the inputArea
	 * TextArea.
	 * 
	 * @param newInput
	 *            String
	 */
	private void addToInputArea(String[] newInput) {
		String[] entries = this.inputArea.getText().split("\\s+");
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < entries.length; i++) {
			sb.append(entries[i] + " ");
		}
		for (int j = 0; j < newInput.length; j++) {
			sb.append(newInput[j] + " ");
		}
		this.inputArea.setText(sb.toString());
	}

	/**
	 * Finds out which recommender has been selected.
	 * 
	 * @return String that indicates which recommender to use. cg -> ClueGene,
	 *         gr -> GeneRecommender
	 */
	private String getSelectedRecommender() {
		String result = "cg";
		if (this.generecommenderRadioButton.getValue()) {
			result = "gr";
		}
		return result;
	}
}
