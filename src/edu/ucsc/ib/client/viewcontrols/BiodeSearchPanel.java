package edu.ucsc.ib.client.viewcontrols;

import java.util.HashMap;
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
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;

public class BiodeSearchPanel extends Composite implements ViewControl {

	protected static final String STOPPED_THROBBER = "notloading_16.png";

	protected static final String LOADING_THROBBER = "loading_16.gif";

	private static final String CSS_CLASS = "ib-biodeControlPanel";

	private static final int MIN_INPUT = 3;

	private final VerticalPanel panel;

	private final TextBox input;

	private final Button searchButton;

	private final Image throbber;

	private final ListBox list;

	private final Label label;

	private final Button addButton;

	private final BiodeControlPanel bcp;

	private final NetworkVisualization netviz;

	private HashMap<String, BiodeInfo> searchResults;

	private Request currentRequest;

	public BiodeSearchPanel(BiodeControlPanel bcp, NetworkVisualization nv) {

		this.bcp = bcp;
		this.netviz = nv;

		this.input = new TextBox();
		this.input.setStyleName(CSS_CLASS + "-input");
		this.searchButton = new Button();
		this.searchButton.setStyleName(CSS_CLASS + "-button");
		this.throbber = new Image();
		this.throbber.setStyleName(CSS_CLASS + "-throbber");
		this.list = new ListBox();
		this.list.setStyleName(CSS_CLASS + "-list");
		this.label = new Label();
		this.label.setStyleName(CSS_CLASS + "-label");
		this.addButton = new Button();
		this.addButton.setStyleName(CSS_CLASS + "-button");

		this.panel = buildPanel();

		initWidget(this.panel);
		setStyleName(CSS_CLASS);
	}

	/**
	 * Submit a request to the server to search annotation table. Request has
	 * the form: data/annodb/annot?organism=9606&searchString=ribosome . The
	 * list of search results is updated.
	 * 
	 * @param currentRequest
	 */
	void getAnnotationResults(Request currentRequest) {
		// turn on throbber
		this.throbber.setUrl(BiodeSearchPanel.LOADING_THROBBER);

		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		// Only search if there is a minimum number of characters in the
		// input text.
		String inputString = this.input.getText();
		if (inputString.length() < BiodeSearchPanel.MIN_INPUT) {
			// turn off throbber
			this.throbber.setUrl(BiodeSearchPanel.STOPPED_THROBBER);
			return;
		}

		RequestCallback rc = new RequestCallback() {
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("BiodeSearchPanel: search has FAILED");

				// turn off throbber
				throbber.setUrl(BiodeSearchPanel.STOPPED_THROBBER);
			}

			public void onResponseReceived(Request request, Response response) {

				// get the results
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						response.getText());

				// check for error message in JSON-RPC response
				if (jsonRpcResp.hasError()) {
					LoggingDialogBox.log("got an error: "
							+ jsonRpcResp.getError().toString());
					// turn off throbber
					throbber.setUrl(BiodeSearchPanel.STOPPED_THROBBER);
					return;
				}

				JSONObject resultJO = jsonRpcResp.getResult();

				list.clear();

				searchResults = BiodeControlPanel.jsonArrayToBiodeHash(resultJO
						.get("annotations").isArray());

				for (String key : searchResults.keySet()) {
					String commonName = searchResults.get(key).getCommonName();

					if (commonName.equalsIgnoreCase(key)) {
						list.addItem(key, key);
					} else {
						list.addItem(commonName + " : " + key, key);
					}

				}

				// turn off throbber
				throbber.setUrl(BiodeSearchPanel.STOPPED_THROBBER);
			}
		};
		String urlStr = "data/annodb/annot?organism="
				+ this.bcp.getSystemspace() + "&searchString=" + inputString;

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlStr);
		try {
			LoggingDialogBox.log("TrackControlPanel ... Beginning request:\t"
					+ urlStr);
			currentRequest = rb.sendRequest(null, rc);
			LoggingDialogBox.log("request sent");
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
		}
	}

	private VerticalPanel buildPanel() {

		VerticalPanel aPanel = new VerticalPanel();

		this.input.setTitle("TextBox for entering search string.");

		this.searchButton.setTitle("search");
		this.searchButton.setText("search");
		this.searchButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				getAnnotationResults(currentRequest);
			}
		});

		this.throbber.setTitle("throbber");
		this.throbber.setUrl(BiodeSearchPanel.STOPPED_THROBBER);

		HorizontalPanel searchspacePanel = new HorizontalPanel();
		searchspacePanel.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
		searchspacePanel.setStyleName(BiodeSearchPanel.CSS_CLASS
				+ "-searchspacePanel");
		aPanel.add(searchspacePanel);

		HorizontalPanel searchInputPanel = new HorizontalPanel();
		searchInputPanel.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
		searchInputPanel.add(this.input);
		searchInputPanel.add(this.searchButton);
		searchInputPanel.add(this.throbber);
		searchInputPanel.setStyleName(BiodeSearchPanel.CSS_CLASS
				+ "-searchInputPanel");
		aPanel.add(searchInputPanel);

		this.addButton.setText("Add selected item.");
		this.addButton.setTitle("add");
		this.addButton.setStyleName(BiodeSearchPanel.CSS_CLASS + "-button");
		this.addButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				String selection = list.getValue(list.getSelectedIndex());
				LoggingDialogBox.log(selection + " was picked from the list.");

				BiodeInfo BI = (BiodeInfo) searchResults.get(selection);
				bcp.setBiodeInfoSpaces(BI);
				netviz.addBiodeInfoToBIC(BI);
				netviz.addBiode(new BiodeSet(BI.getSystematicName()));
			}
		});

		aPanel.add(this.addButton);

		// add the KeyboardListener to listen for enter key.
		// also, can make dynamic search - search on keystroke
		this.input.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(KeyPressEvent event) {
				if (event.getCharCode() == KeyCodes.KEY_ENTER) {
					searchButton.click();
				}
			}
		});

		this.list.addItem("Search results appear in this list.");
		this.list.setVisibleItemCount(10);
		this.list.setTitle("Search results appear in this list.");
		this.list.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				String selectedSystematicName = list.getValue(list
						.getSelectedIndex());
				BiodeInfo BI = (BiodeInfo) searchResults
						.get(selectedSystematicName);

				StringBuffer text = new StringBuffer();
				text.append(BI.getDescription());

				label.setText(text.toString());
			}
		});
		aPanel.add(this.list);

		this.label
				.setText("Info about current selection appears in this label.");
		this.label
				.setTitle("Info about current selection appears in this label.");
		this.label.setStyleName(BiodeSearchPanel.CSS_CLASS + "-label");
		aPanel.add(this.label);

		return aPanel;
	}
}
