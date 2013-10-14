/**
 * 
 */
package edu.ucsc.ib.client.viewcontrols;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.SourceSystemSpaceListener;

/**
 * @author wongc
 * 
 */
public class BiodeUserInputPanel extends Composite implements ViewControl,
		SourceSystemSpaceListener, FormPanel.SubmitHandler,
		FormPanel.SubmitCompleteHandler {
	private static final String MULTI_SPECIES_LOOKUP_SERVICE_URL = "data/annodb/aliasToAnnot_multiSpace";

	private static final String SINGLE_SPECIES_LOOKUP_SERVICE_URL = "data/annodb/aliasToAnnot2";

	/**
	 * URL for the Grinder servlet
	 */
	private static final String GRINDER_SERVICE_URL = "http://disco.cse.ucsc.edu:8089/Grinder/data/GrinderServlet";

	private static final String CSS_CLASS = "ib-biodeUserInputPanel";

	private final VerticalPanel panel;

	private static SearchSpaceControl ssc;

	private static NetworkVisualization netviz;

	private static final TextArea input = new TextArea();

	private final VerticalPanel buttonPanel;

	private static Request currentRequest;

	private final ListBox grinderSourceListBox;

	private String grinderTargetKeyspace;

	/**
	 * Grinder keyspaces gotten from:
	 * http://disco.cse.ucsc.edu:8089/Grinder/data
	 * /GrinderServlet?request=keyspaces&species=any
	 */
	private static final String[] grinderAnyKeySpaces = {
			"DAVID_Alias_Gene_Symbols", "DAVID_Gene_Names",
			"DAVID_Gene_Symbols", "DAVID_IDs", "DAVID_RefSeq_RNAs",
			"Gene_Names_From_DAVID" };

	/**
	 * Grinder keyspaces gotten from:
	 * http://disco.cse.ucsc.edu:8089/Grinder/data
	 * /GrinderServlet?request=keyspaces&species=human
	 */
	private static final String[] grinderHumanKeySpaces = {
			"Affymetrix_GC_HG_U133_Plus_2_0_", "Affymetrix_GC_HG_U95_IDs",
			"Affymetrix_Probe_Set_10_IDs", "Affymetrix_Probe_Set_169_IDs",
			"Affymetrix_Probe_Set_1977_IDs", "Affymetrix_Probe_Set_201_IDs",
			"Affymetrix_Probe_Set_340_IDs", "Affymetrix_Probe_Set_371_IDs",
			"Affymetrix_Probe_Set_550_IDs", "Affymetrix_Probe_Set_570_IDs",
			"Affymetrix_Probe_Set_571_IDs", "Affymetrix_Probe_Set_7_IDs",
			"Affymetrix_Probe_Set_80_IDs", "Affymetrix_Probe_Set_91_IDs",
			"Affymetrix_Probe_Set_92_IDs", "Affymetrix_Probe_Set_93_IDs",
			"Affymetrix_Probe_Set_94_IDs", "Affymetrix_Probe_Set_95_IDs",
			"Affymetrix_Probe_Set_96_IDs", "Affymetrix_Probe_Set_97_IDs",
			"Affymetrix_Probe_Set_9_IDs", "Compugen_Probe_IDs", "Ensembl_IDs",
			"GDB_IDs", "GenBank_Accession_Numbers",
			"GenBank_EST_Accession_Numbers", "GPL_RefSeq_Transcript_IDs",
			"HGNC_Approved_Names", "HGNC_Approved_Symbols", "HGNC_ID",
			"Human_EntrezGene", "IMAGE_clone_IDs", "OMIM_IDs",
			"RefSeq_mRNA_IDs", "RZPD_IDs", "UCSC_IDs",
			"UniGene_Approved_Symbols", "UniGene_Gene_ClusterIDs",
			"UniGene_Gene_Names", "UniProt_IDs" };

	/**
	 * Grinder keyspaces gotten from:
	 * http://disco.cse.ucsc.edu:8089/Grinder/data
	 * /GrinderServlet?request=keyspaces&species=mouse
	 */
	private static final String[] grinderMouseKeySpaces = {
			"Affymetrix_Probe_Set_1261_IDs", "Affymetrix_Probe_Set_260_IDs",
			"Affymetrix_Probe_Set_313_IDs", "Affymetrix_Probe_Set_32_IDs",
			"Affymetrix_Probe_Set_339_IDs", "Affymetrix_Probe_Set_560_IDs",
			"Affymetrix_Probe_Set_75_IDs", "Affymetrix_Probe_Set_76_IDs",
			"Affymetrix_Probe_Set_81_IDs", "Affymetrix_Probe_Set_82_IDs",
			"Affymetrix_Probe_Set_83_IDs", "Affymetrix_Probe_Set_891_IDs",
			"GPL_Mouse_RefSeq_IDs", "MGD_IDs", "Mouse_Ensembl_Gene_IDs",
			"Mouse_EntrezGene", "Mouse_GenBank_Acc_Numbers",
			"Mouse_TIGR_Gene_IDs", "Mouse_UniGene_Gene_ClusterIDs",
			"Mouse_UniGene_Gene_Names", "Mouse_UniGene_Symbols" };

	/**
	 * Grinder keyspaces gotten from:
	 * http://disco.cse.ucsc.edu:8089/Grinder/data
	 * /GrinderServlet?request=keyspaces&species=yeast
	 */
	private static final String[] grinderYeastKeySpaces = { "SGD_Alias_Name",
			"SGD_ID", "SGD_Locus_Name", "SGD_ORF_Name" };

	private String sourceSystemSpace;

	private final FormPanel submitFileForm;

	// TODO /////////////////////////////////////////////

	public BiodeUserInputPanel(SearchSpaceControl ssc, NetworkVisualization nv) {

		panel = new VerticalPanel();

		BiodeUserInputPanel.ssc = ssc;
		SearchSpaceControl.addSystemSpaceListener(this);

		BiodeUserInputPanel.netviz = nv;

		input.setWidth("100%");
		input.setVisibleLines(10);
		input.setStyleName(CSS_CLASS + "-inputArea");
		input.setTitle("TextBox for entering biodes.");

		// input.setText("U12980.1\nS000000034\ntext");

		this.submitFileForm = new FormPanel();
		this.submitFileForm.addSubmitHandler(this);
		this.submitFileForm.addSubmitCompleteHandler(this);
		// this.submitFileForm.addFormHandler(new UploadFileFormHandler());
		// this.form.setAction("data/submit/track/");
		this.submitFileForm.setAction("fileBounce/returnList");
		// this.form.setEncoding(FormPanel.ENCODING_URLENCODED);
		this.submitFileForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		this.submitFileForm.setMethod(FormPanel.METHOD_POST);
		FileUpload upload = new FileUpload();
		upload.setName("uploadFormElement");
		this.submitFileForm.add(upload);

		HorizontalPanel hp = new HorizontalPanel();
		hp.add(this.submitFileForm);

		Button submitFileButton = new Button("get IDs from file",
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						submitFileForm.submit();
					}
				});

		hp.add(submitFileButton);

		this.buttonPanel = new VerticalPanel();
		this.buttonPanel.add(buildSubmitLookupButton());
		this.buttonPanel.add(buildSubmitButton());
		this.buttonPanel.add(buildClearButton());

		this.grinderSourceListBox = new ListBox();
		this.grinderSourceListBox.setName("grinderSourceListBox");
		this.grinderSourceListBox.setTitle("select Grinder source keyspace");
		this.grinderSourceListBox.setVisibleItemCount(1);
		this.grinderSourceListBox.addItem("choices should appear here");

		this.grinderTargetKeyspace = "Human_EntrezGene";

		HorizontalPanel grinderPanel = new HorizontalPanel();
		grinderPanel.add(this.grinderSourceListBox);
		grinderPanel.add(this.buildSubmitToGrinderButton());
		this.buttonPanel.add(grinderPanel);

		grinderPanel.setVisible(false);

		this.panel.add(hp);
		this.panel.add(BiodeUserInputPanel.input);
		this.panel.add(this.buttonPanel);

		// String[] preload = { "Lepirudin", "AQ500634.1", "PA10040", "a1bg" };
		//
		// for (String id : preload) {
		// appendInputArea(id + " ");
		// }

		initWidget(this.panel);
		setStyleName(CSS_CLASS);
	}

	/**
	 * Get the text in the input TextArea.
	 * 
	 */
	String getInputText() {
		return input.getText().trim();
	}

	/**
	 * Get a String[] from the input TextArea.
	 * 
	 * @return
	 */
	String[] getInputIds() {
//		return input.getText().trim().replaceAll(",", " ").split("\\s+");
		return input.getText().trim().replaceAll(",", " ").split("\\n+");
	}

	/**
	 * Clear the text in the input TextArea.
	 * 
	 */
	static void clearInputArea() {
		input.setText(null);
	}

	/**
	 * Appends the parameter String to the existing text in the input TextArea.
	 * 
	 * @param newInput
	 *            The String to append
	 */
	static void appendInputArea(String newInput) {
		StringBuffer strBuf = new StringBuffer(input.getText().trim());
		strBuf.append("\n");
		strBuf.append(newInput);
		input.setText(strBuf.toString());
	}

	/**
	 * Append the biodes in a BiodeSet to the input area.
	 * 
	 * @param b
	 *            BiodeSet
	 */
	static void appendInputArea(BiodeSet b) {
		StringBuffer strBuf = new StringBuffer();

		for (String biode : b) {
			strBuf.append("\n" + biode);
		}

		appendInputArea(strBuf.toString().trim());
	}

	private Button buildSubmitButton() {
		Button b = new Button("add entities directly", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				processSubmission(getInputText());
			}
		});
		b.setTitle("submit the text bypassing alias lookup");
		return b;
	}

	private Button buildSubmitLookupButton() {
		Button b = new Button("lookup entities in database, then add",
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						processSubmissionWithLookupService_single_sp(
								SearchSpaceControl.getSystemspace(),
								getInputIds(), false);
					}
				});
		b.setTitle("submit the text for alias lookup");
		return b;
	}

	private Button buildSubmitToGrinderButton() {
		Button b = new Button("Grinder lookup", new ClickHandler() {
			Request currentRequest;

			@Override
			public void onClick(ClickEvent event) {
				sendRequestToGrinder(currentRequest);
			}
		});
		b.setTitle("submit the text to Grinder");
		return b;
	}

	private Button buildClearButton() {
		Button b = new Button("clear", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				clearInputArea();
			}
		});
		b.setTitle("clear the text area");
		return b;
	}

	/**
	 * Parses the parameter string for the user's input. For each input, find an
	 * internal ID to use as Biode. Then, add a NetworkNode to NetViz using a
	 * BiodeInfo object.
	 * 
	 * @param entityArray
	 *            list of entities to lookup
	 * @param displayUnmapped
	 *            If true, display unmapped IDs. If false, alert user of
	 *            unmapped IDs and send unmapped IDs to user input area.
	 * @param commonNeighborsCurrentRequest
	 * 
	 * @return
	 */
	public static int processSubmissionWithLookupService_single_sp(
			String organismID, String[] entityArray,
			final boolean displayUnmapped) {

		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		RequestCallback rc = new RequestCallback() {

			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("BiodeUserInputPanel: ID search failed");
				LoggingDialogBox.log("=====================================");
				LoggingDialogBox.log(exception.toString());
				LoggingDialogBox.log("=====================================");
			}

			public void onResponseReceived(Request request, Response response) {

				// get the results
				String jsonResultString = response.getText();

				if (jsonResultString == null) {
					LoggingDialogBox
							.log("BiodeUserInputPanel: ID search returned null object");
					// turn off throbber
					// throbber.setUrl(STOPPED_THROBBER);
					return;
				}

				// LoggingDialogBox
				// .log("BiodeUserInputPanel: ID search got non-null object");

				// clear the input area
				clearInputArea();

				// get the JSON result
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						jsonResultString);

				// check for error message in JSON-RPC response
				if (jsonRpcResp.hasError()) {
					LoggingDialogBox.log("got an error: "
							+ jsonRpcResp.getError().toString());
					return;
				}

				JSONObject resultJO = jsonRpcResp.getResult();

				String organism = resultJO.get("organism").isString()
						.stringValue();

				String biodeSpace = SearchSpaceControl.getBiodespace();

				// ready to handle resultJO

				// get biodeInfos for mapped ids
				HashMap<String, BiodeInfo> annotationsHashMap = BiodeControlPanel
						.jsonArrayToBiodeHash(resultJO.get("annotations")
								.isArray());

				// create biodeInfos for unmapped ids
				BiodeSet unmappedBiodes = new BiodeSet();
				if (resultJO.containsKey("unmapped")) {
					JSONArray unmappedJA = resultJO.get("unmapped").isArray();

					for (int i = 0; i < unmappedJA.size(); i++) {
						String unmappedID = unmappedJA.get(i).isString()
								.stringValue();

						if (displayUnmapped) {
							BiodeInfo bi = getBiodeInfoForUnmappedID(unmappedID);
							annotationsHashMap.put(unmappedID, bi);
						} else {
							unmappedBiodes.add(unmappedID);
						}

					}
				}

				// set some biodeInfo properties
				for (String biode : annotationsHashMap.keySet()) {
					BiodeInfo bi = annotationsHashMap.get(biode);

					// no specified systemSpace
					if (bi.getSystemSpace().equalsIgnoreCase("none")) {
						bi.setSystemSpace(organism);
					}

					// no specified biodeSpace
					if (bi.getSystemSpace().equalsIgnoreCase("drug")) {
						bi.setBiodeSpace("chemical");
					} else {
						if (bi.getBiodeSpace().equalsIgnoreCase("none")) {
							bi.setBiodeSpace(biodeSpace);
						}
					}
				}

				// update BIC with biodeInfos for mapped and unmapped ids
				netviz.addBiodeInfoToBIC(annotationsHashMap);

				// draw nodes
				netviz.addBiode(new BiodeSet(annotationsHashMap.keySet()));

				if (unmappedBiodes.size() > 0) {
					appendInputArea(unmappedBiodes);

					int num = unmappedBiodes.size();

					Window.alert(num
							+ " IDs were sent to the User Input Area because they were not mapped to gene.");
				}
			}
		};

		String urlStr = SINGLE_SPECIES_LOOKUP_SERVICE_URL + "?organism="
				+ organismID + "&list="
				+ BiodeControlPanel.arrayToCommaSeparatedString(entityArray);

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlStr);
		try {
			// LoggingDialogBox.log("BiodeUserInputPanel ... Beginning request:\t"
			// + urlStr);
			currentRequest = rb.sendRequest(null, rc);
			// LoggingDialogBox.log("request sent");
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
		}

		return entityArray.length;
	}

	/**
	 * Parses the parameter string for the user's input. For each input, find an
	 * internal ID to use as Biode. Then, add a NetworkNode to NetViz using a
	 * BiodeInfo object.
	 * 
	 * @param commonNeighborsCurrentRequest
	 * @param entityArray
	 *            list of entities to lookup
	 * @return
	 */
	public static int processSubmissionWithLookupService_multi_spp(
			String[] entityArray) {

		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		RequestCallback rc = new RequestCallback() {

			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("BiodeUserInputPanel: ID search failed");
				LoggingDialogBox.log("=====================================");
				LoggingDialogBox.log(exception.toString());
				LoggingDialogBox.log("=====================================");
			}

			public void onResponseReceived(Request request, Response response) {

				// get the results
				String jsonResultString = response.getText();

				if (jsonResultString == null) {
					LoggingDialogBox
							.log("BiodeUserInputPanel: ID search returned null object");
					// turn off throbber
					// throbber.setUrl(STOPPED_THROBBER);
					return;
				}

				// LoggingDialogBox
				// .log("BiodeUserInputPanel: ID search got non-null response");

				// clear the input area
				clearInputArea();

				// process the JSON result
				// get the results
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						jsonResultString);

				// check for error message in JSON-RPC response
				if (jsonRpcResp.hasError()) {
					LoggingDialogBox.log("got an error: "
							+ jsonRpcResp.getError().toString());
					return;
				}

				// LoggingDialogBox
				// .log("no error in JSON-RPC response object, continue");

				JSONObject resultJO = jsonRpcResp.getResult();

				JSONArray organismsJA = resultJO.get("organisms").isArray();

				BiodeSet bs = new BiodeSet();

				if (organismsJA.size() > 0) {
					for (int i = 0; i < organismsJA.size(); i++) {
						JSONObject organismJO = organismsJA.get(i).isObject();
						String organism = organismJO.get("organism").isString()
								.stringValue();
						// HashMap<String, BiodeInfo> resultHash =
						// BiodeControlPanel
						// .jsonArrayToBiodeHash(organismJO.toString());
						HashMap<String, BiodeInfo> annotationsHashMap = BiodeControlPanel
								.jsonArrayToBiodeHash(organismJO.get(
										"annotations").isArray());
						// LoggingDialogBox
						// .log("BiodeUserInputPanel: number results "
						// + annotationsHashMap.size());

						// set biodeinfospaces
						for (String biode : annotationsHashMap.keySet()) {
							BiodeInfo bi = annotationsHashMap.get(biode);
							bi.setSystemSpace(organism);
							if (bi.getSystemSpace().equalsIgnoreCase("drug")) {
								bi.setBiodeSpace("chemical");
							} else {
								bi.setBiodeSpace(SearchSpaceControl
										.getBiodespace());
							}
						}

						// add biodeinfo to BIC
						netviz.addBiodeInfoToBIC(annotationsHashMap);

						// append the BiodeSet
						bs.addAll(annotationsHashMap.keySet());
					}
				}

				// create/add NetworkNodes
				netviz.addBiode(bs);

				// handle unmapped IDs
				if (resultJO.containsKey("unmapped")) {
					JSONArray unmappedJA = resultJO.get("unmapped").isArray();
					handleUnmappedIds(unmappedJA);
				}
			}
		};

		String urlStr = MULTI_SPECIES_LOOKUP_SERVICE_URL + "?organism="
				+ SearchSpaceControl.getSystemspace() + "&list="
				+ BiodeControlPanel.arrayToCommaSeparatedString(entityArray);

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlStr);
		try {
			// LoggingDialogBox.log("BiodeUserInputPanel ... Beginning request:\t"
			// + urlStr);
			currentRequest = rb.sendRequest(null, rc);
			// LoggingDialogBox.log("request sent");
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
		}

		return entityArray.length;
	}

	/**
	 * Parses the parameter string for the user's input. Use each input as
	 * Biode. Then, add a NetworkNode to NetViz using a BiodeInfo object. This
	 * method does NOT lookup biode information on server.
	 * 
	 * @param text
	 *            the String to parse
	 */
	private void processSubmission(String text) {
		HashSet<String> entriesHashSet = new HashSet<String>(Arrays.asList(text
				.split("\\s+")));

		addNetworkNodes_without_annot(entriesHashSet);

		clearInputArea();
	}

	/**
	 * Get a BiodeInfo object for a biode that has no annotations. Fills in some
	 * default values for BiodeInfo such as the currently selected sysbioSpace
	 * and a default description.
	 * 
	 * @param biode
	 * @return
	 */
	public static BiodeInfo getBiodeInfoForUnmappedID(final String biode) {
		BiodeInfo BI = new BiodeInfo(biode);
		ssc.setBiodeInfoSpaces(BI);
		BI.setCommonName(biode);
		BI.setDescription("not available");

		return BI;
	}

	/**
	 * Add NetworkNode objects to netViz using IDs from the HashSet.
	 * 
	 * @param entriesHashSet
	 */
	public static void addNetworkNodes_without_annot(
			final HashSet<String> entriesHashSet) {
		BiodeSet bs = new BiodeSet();

		for (String id : entriesHashSet) {
			if (id.length() != 0) {

				// Fill in BiodeInfo object
				BiodeInfo BI = new BiodeInfo(id);
				ssc.setBiodeInfoSpaces(BI);
				BI.setCommonName(id);
				BI.setDescription("not available");

				// add a node using the BiodeInfo
				netviz.addBiodeInfoToBIC(BI);

				bs.add(BI.getSystematicName());
			}
		}
		// create NetworkNode objects in NetViz
		netviz.addBiode(bs);
	}

	/**
	 * Add NetworkNode objects to netViz using IDs from a JSONArray.
	 * 
	 * @param unmappedJA
	 */
	public static void handleUnmappedIds(final JSONArray unmappedJA) {

		LoggingDialogBox.log("buip about to handle unmapped ids");

		HashSet<String> unmappedHashSet = new HashSet<String>();
		for (int i = 0; i < unmappedJA.size(); i++) {
			String unmapped = unmappedJA.get(i).isString().stringValue();
			unmappedHashSet.add(unmapped);
		}

		LoggingDialogBox.log("buip got " + unmappedHashSet.size()
				+ " unmapped IDs to add");

		addNetworkNodes_without_annot(unmappedHashSet);
	}

	/**
	 * Prepare a query string for Grinder servlet.
	 * 
	 * @param queryList
	 * @return
	 */
	private String makeGrinderQueryString(String[] queryList) {
		// example of a grinder query:
		// http://disco.cse.ucsc.edu:8089/Grinder/data/GrinderServlet?request=map&source=UCSC_IDs&target=Human_EntrezGene&ids=uc003bdb.1,uc003uiz.1
		// look here for more help:
		// https://twiki.soe.ucsc.edu/twiki/bin/view/SysBio/GrinderServlet
		StringBuffer strBuf = new StringBuffer("request=map");
		strBuf.append("&source=" + this.getGrinderSourceKeyspace());
		strBuf.append("&target=" + this.grinderTargetKeyspace);
		strBuf.append("&ids=");
		// String query =
		// "request=map&source=Human_EntrezGene&target=UniGene_Approved_Symbols&ids=";
		int counter = 0;

		for (String id : queryList) {
			if (id.length() > 0) {
				strBuf.append(id + ",");
				counter++;
			}
		}
		// LoggingDialogBox.log("number of source IDs:\t" + counter);
		return strBuf.toString();
	}

	/**
	 * Send a request to Grinder servlet and handle the response.
	 * 
	 * @param queryStr
	 */
	private void sendRequestToGrinder(Request currentRequest) {
		final String[] inputIds = getInputIds();
		String queryStr = makeGrinderQueryString(inputIds);

		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		RequestCallback rc = new RequestCallback() {

			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log(exception.toString());
			}

			public void onResponseReceived(Request request, Response response) {
				// LoggingDialogBox.log("BEGIN output response");
				String[] targetIDList = response.getText().split("\n");
				clearInputArea();
				for (int i = 0; i < targetIDList.length; i++) {
					if (targetIDList[i].equalsIgnoreCase("Invalid ID")) {
						appendInputArea(inputIds[i]);
					} else {
						appendInputArea(targetIDList[i]);
					}
					// DataVizPanel.log(i + "\t" + inputIds[i] + "\t"
					// + targetIDList[i]);
				}
				// LoggingDialogBox.log("END output response");
			}
		};

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
				GRINDER_SERVICE_URL + "?" + queryStr);
		try {
			// LoggingDialogBox.log("Beginning request:\t" + GRINDER_SERVICE_URL
			// + "?" + queryStr);
			currentRequest = rb.sendRequest(null, rc);
			// LoggingDialogBox.log("request sent");
		} catch (RequestException re) {
			LoggingDialogBox.log("Unable to make request: " + re.getMessage());
		} catch (Exception e) {
			LoggingDialogBox.log("Unknown exeception: " + e.toString());
		}
	}

	/**
	 * Required by interface, SourceSystemSpaceListener.
	 */
	public void sourceSystemSpaceChanged(String systemSpace) {
		this.sourceSystemSpace = systemSpace;
		this.updateGrinderListBox();
	}

	/**
	 * Set up the Grinder ListBox objects for the current systemSpace. Grinder
	 * currently works for "Any", "Human", "Mouse", and "Yeast".
	 * 
	 * @return 'true' if ListBoxes were ready
	 */
	private boolean updateGrinderListBox() {
		boolean result = false;
		// need to make sure the ListBox objects are not null and are attached
		// otherwise, Javascript would error occur
		if (this.grinderSourceListBox != null
				&& this.grinderSourceListBox.isAttached()) {
			String[] keyspaces;
			if (this.sourceSystemSpace.equalsIgnoreCase("any")) {
				keyspaces = grinderAnyKeySpaces;
				this.grinderTargetKeyspace = "Human_EntrezGene";
			} else if (this.sourceSystemSpace.equalsIgnoreCase("human")) {
				keyspaces = grinderHumanKeySpaces;
				this.grinderTargetKeyspace = "Human_EntrezGene";
			} else if (this.sourceSystemSpace.equalsIgnoreCase("mouse")) {
				keyspaces = grinderMouseKeySpaces;
				this.grinderTargetKeyspace = "Mouse_EntrezGene";
			} else if (this.sourceSystemSpace.equalsIgnoreCase("yeast")) {
				keyspaces = grinderYeastKeySpaces;
				this.grinderTargetKeyspace = "SGD_ORF_Name";
			} else {
				keyspaces = grinderAnyKeySpaces;
				this.grinderTargetKeyspace = "Human_EntrezGene";
			}

			this.grinderSourceListBox.clear();

			for (String keyspace : keyspaces) {
				this.grinderSourceListBox.addItem(keyspace);
			}
			result = true;
		} else {
			LoggingDialogBox.log("grinder ListBoxes were not ready!");
		}
		return result;
	}

	/**
	 * Get the String value for the selected Grinder source keyspace.
	 * 
	 * @return
	 */
	private String getGrinderSourceKeyspace() {
		return this.grinderSourceListBox.getValue(this.grinderSourceListBox
				.getSelectedIndex());
	}

	public void viewSystemSpaceChanged(String viewSystemSpace) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSubmit(SubmitEvent event) {
		// TODO Auto-generated method stub

		// This event is fired just before the form is submitted. We can
		// take this opportunity to perform validation.

	}

	@Override
	public void onSubmitComplete(SubmitCompleteEvent event) {
		// TODO Auto-generated method stub

		// When the form submission is successfully completed, this
		// event is fired. Assuming the service returned a response of
		// type text/html, we can get the result text here (see the
		// FormPanel documentation for further explanation).

		String stringifiedJSONObject = event.getResults();
		JSONObject mainJO = (JSONObject) JSONParser
				.parseStrict(stringifiedJSONObject);
		boolean success = mainJO.get("success").isBoolean().booleanValue();
		if (success) {
			appendInputArea(mainJO.get("list").isString().stringValue());
			Window.alert("The IDs in the file have been added to the text area.");
		} else {
			Window.alert("Could not read the file, or maybe no file was specified.");
		}
	}
}
