package edu.ucsc.ib.client.viewcontrols;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;

public class BiodeGOPanel extends Composite implements ViewControl {

	private static final String CSS_CLASS = "ib-biodeControlPanel";

	private static final String INNER_PANEL_HEIGHT = "20em";

	private static final String INNER_PANEL_WIDTH = "15em";

	private static final String STOPPED_THROBBER = "notloading_16.png";

	private static final String LOADING_THROBBER = "loading_16.gif";

	private static final int MIN_INPUT = 3;

	private final TextBox input;

	private final Image throbber;

	private final Button enrichButton;

	private final Button searchButton;

	private final VerticalPanel panel;

	/**
	 * List of data sets available for the current SourceSystemSpace
	 */
	private final ListBox dataSetListBox;

	/**
	 * ListBox to select the gene set from.
	 */
	private final ListBox setSelectionListBox;

	private final BiodeControlPanel bcp;

	/**
	 * Key=setName; value=ArrayList of setMembers
	 */
	private HashMap<String, ArrayList<String>> setsMap;

	private Request currentRequest;

	// private String biodeSpace;

	private final NetworkVisualization netviz;

	public BiodeGOPanel(BiodeControlPanel bcp, NetworkVisualization nv) {

		this.bcp = bcp;
		this.netviz = nv;
		this.setsMap = new HashMap<String, ArrayList<String>>();

		this.input = new TextBox();
		this.input.setTitle("enter some text to search for in a GO category");
		this.throbber = new Image();
		this.throbber.setTitle("throbber");
		this.throbber.setUrl(STOPPED_THROBBER);

		this.enrichButton = makeEnrichButton();
		this.searchButton = makeSetSearchButton();
		this.dataSetListBox = new ListBox();
		this.setSelectionListBox = initSetsList();

		this.panel = buildPanel();

		initWidget(this.panel);
		setStyleName(CSS_CLASS);
	}

	/**
	 * Build up the main panel.
	 * 
	 * @return
	 */
	private VerticalPanel buildPanel() {
		VerticalPanel vPanel = new VerticalPanel();
		vPanel.add(makeInputPanel());
		vPanel.add(makeListPanel());
		vPanel.setSize(INNER_PANEL_WIDTH, INNER_PANEL_HEIGHT);
		return vPanel;
	}

	/**
	 * Build up panel for ListBox objects.
	 * 
	 * @return
	 */
	private VerticalPanel makeListPanel() {
		VerticalPanel p = new VerticalPanel();
		p.add(this.dataSetListBox);
		p.add(this.setSelectionListBox);
		p.add(makeAddSetButton());
		return p;
	}

	/**
	 * Build up button for adding the members of the selected gene set to
	 * netViz.
	 * 
	 * @return
	 */
	private Button makeAddSetButton() {
		Button b = new Button("add set", new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				String chosenSetName = setSelectionListBox
						.getValue(setSelectionListBox.getSelectedIndex());

				ArrayList<String> memberList = setsMap.get(chosenSetName);

				BiodeUserInputPanel
						.processSubmissionWithLookupService_single_sp(
								SearchSpaceControl.getSystemspace(),
								(String[]) memberList.toArray(new String[0]), true);
			}

		});
		b.setTitle("add the selected GO set");

		return b;
	}

	/**
	 * Initialize the
	 * 
	 * @return
	 */
	private ListBox initSetsList() {
		ListBox lb = new ListBox();
		lb.addItem("sets appear in this list.");
		// lb.setVisibleItemCount(10);
		lb.setTitle("sets appear in this list.");
		return lb;
	}

	/**
	 * Update the setSelectionListBox with the current contents of setsMap.
	 */
	public void updateSetsList() {
		this.setSelectionListBox.clear();
		Set<String> setNames = new HashSet<String>(this.setsMap.keySet());
		for (Iterator<String> iter = setNames.iterator(); iter.hasNext();) {
			String s = iter.next();
			this.setSelectionListBox.addItem((this.setsMap.get(s)).size() + ":"
					+ s, s);
		}
	}

	/**
	 * Build up the user's input interfaces.
	 * 
	 * @return
	 */
	private HorizontalPanel makeInputPanel() {
		HorizontalPanel hp = new HorizontalPanel();
		VerticalPanel vp = new VerticalPanel();
		vp.add(this.dataSetListBox);
		vp.add(this.input);
		hp.add(vp);
		hp.add(this.searchButton);
		hp.add(this.enrichButton);
		hp.add(this.throbber);

		// add the KeyboardListener to listen for enter key.
		// also, can make dynamic search - search on keystroke

		this.input.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(KeyPressEvent event) {
				// when enter is pressed (13), click searchButton.

				if (event.getCharCode() == KeyCodes.KEY_ENTER) {
					searchButton.click();
				}
			}
		});
		return hp;
	}

	/**
	 * Build up the button for looking up gene sets by name.
	 * 
	 * @return
	 */
	private Button makeSetSearchButton() {
		Button b = new Button("search");
		b.setTitle("search for GO categories");

		b.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				searchSetNames(currentRequest);
			}

		});
		return b;
	}

	/**
	 * Build up the button for looking up gene set enrichment.
	 * 
	 * @return
	 */
	private Button makeEnrichButton() {
		Button b = new Button("enrichment");
		b.setTitle("check selected entitities for enriched sets");

		b.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				// Only search if there is a minimum number of entities.
				String[] queryIds = netviz.getSelectedNodeIds().getArray();
				if (queryIds.length < 2) {
					// turn off throbber
					boolean useAllBiodes = Window
							.confirm("At least 2 nodes need to be selected to perform this analysis.  Do you want to select all nodes?  (You'll have to click the enrichment button to try again.)");
					if (!useAllBiodes) {
						throbber.setUrl(STOPPED_THROBBER);
						return;
					} else {
						netviz.selectAllBiodes();
						throbber.setUrl(STOPPED_THROBBER);
						return;
					}
				}

				StringBuffer urlSB = new StringBuffer("data/setsdb/"
						+ "setEnrichment" + "?");
				urlSB.append("organism=" + getSourceSystemSpace());
				urlSB.append("&setDataName=" + getSetDataName());
				urlSB.append("&queryIds=");

				for (int i = 0; i < queryIds.length; i++) {
					urlSB.append(queryIds[i] + ",");
				}
				// remove the extra comma
				urlSB.deleteCharAt(urlSB.length() - 1);

				Window.open(urlSB.toString(), "set_enrichment_results", "");
			}
		});
		return b;
	}

	/**
	 * Set a new setsMap.
	 * 
	 * @param newOne
	 */
	public void setSetsMap(HashMap<String, ArrayList<String>> newOne) {
		this.setsMap = newOne;
	}

	/**
	 * Get the setsMap.
	 * 
	 * @return
	 */
	public HashMap<String, ArrayList<String>> getSetsMap() {
		return this.setsMap;
	}

	/**
	 * Get the selected sourceSystemSpace from the BiodeControlPanel.
	 * 
	 * @return
	 */
	public String getSourceSystemSpace() {
		return this.bcp.getSystemspace();
	}

	/**
	 * Get the selected set data name from the dataSetListBox.
	 * 
	 * @return
	 */
	public String getSetDataName() {
		return this.dataSetListBox.getValue(this.dataSetListBox
				.getSelectedIndex());
	}

	/**
	 * Reset the dataSetListBox with the specified options.
	 * 
	 * @param dataSets
	 */
	public void resetDataSetListBox(ArrayList<String> dataSets) {
		this.dataSetListBox.clear();
		for (Iterator<String> iter = dataSets.iterator(); iter.hasNext();) {
			this.dataSetListBox.addItem(iter.next());
		}
	}

	/**
	 * Build up and submit a GET request in an httpRequest object to search set
	 * names.
	 * 
	 * @param currentRequest
	 *            request to use. This is used so that running request can be
	 *            cancelled before submitting the new one.
	 */
	private void searchSetNames(Request currentRequest) {
		// turn on throbber
		throbber.setUrl(LOADING_THROBBER);

		// Only search if there is a minimum number of characters in the
		// input text.
		String inputString = input.getText();
		if (inputString.length() < MIN_INPUT) {
			// turn off throbber
			throbber.setUrl(STOPPED_THROBBER);
			return;
		}

		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		RequestCallback rc = new RequestCallback() {

			public void onResponseReceived(Request request, Response response) {
				LoggingDialogBox.log("got a response");
				// result is a stringified JSONObject
				JSONObject resultJO = (JSONObject) JSONParser
						.parseStrict(response.getText());

				JSONArray resultJA = resultJO.get("sets").isArray();

				LoggingDialogBox.log("number of set names: " + resultJA.size());

				HashMap<String, ArrayList<String>> setsMap = new HashMap<String, ArrayList<String>>();
				ArrayList<String> membersList;
				JSONObject jo;
				JSONArray ja;
				for (int i = 0; i < resultJA.size(); i++) {
					jo = resultJA.get(i).isObject();
					ja = jo.get("members").isArray();
					membersList = new ArrayList<String>();
					for (int j = 0; j < ja.size(); j++) {
						membersList.add(ja.get(j).isString().stringValue());
					}
					setsMap.put(jo.get("name").isString().stringValue(),
							membersList);
				}

				setSetsMap(setsMap);

				updateSetsList();

				// turn off throbber
				throbber.setUrl(STOPPED_THROBBER);
			}

			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("BiodeGOPanel: searchSetNames has FAILED");
				LoggingDialogBox.log(exception.toString());

				// turn off throbber
				throbber.setUrl(STOPPED_THROBBER);
			}
		};

		String urlString = "data/setsdb/setsName?setType=" + getSetDataName()
				+ "&searchString=" + inputString;

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlString);
		try {
			LoggingDialogBox.log("Beginning request:\t" + urlString);
			currentRequest = rb.sendRequest(null, rc);
			LoggingDialogBox.log("request sent");
		} catch (RequestException re) {
			LoggingDialogBox.log("Unable to make request: " + re.getMessage());
		} catch (Exception e) {
			LoggingDialogBox.log("Unknown exeception: " + e.toString());
		}
	}
}