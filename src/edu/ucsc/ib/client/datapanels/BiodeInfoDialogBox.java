package edu.ucsc.ib.client.datapanels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.Stats;
import edu.ucsc.ib.client.netviz.BasicNodeMouseEventsHandler;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.NodeListener;
import edu.ucsc.ib.client.viewcontrols.CircleMapDialogBox;

/**
 * should this widget be made into a trackSetListener and biodeSetListener so it
 * will be automatically notified of changes? It would have to be able to
 * add/remove itself from the listener sets.
 * 
 * @author cw
 * 
 */
public class BiodeInfoDialogBox extends IbDialogBox implements NodeListener,
		MouseUpHandler {

	private final BiodeInfo bi;

	private static NetworkVisualization netviz;

	private Request currentRequest;

	private static final String BOX_WIDTH = "50em";
	private static final int BORDER_WIDTH = 1;
	private static final int POSITION_LEFT = 200;
	private static final int POSITION_TOP = 120;

	private static final String CSS_CLASS = "ib-BiodeInfoDialogBox";

	// private static final String NCBI_URL =
	// "http://www.ncbi.nlm.nih.gov/sites/entrez?db=gene&cmd=Retrieve&dopt=full_report&list_uids=";
	//
	// private static final String WORMBASE_URL =
	// "http://wormbase.org/db/seq/sequence?name=";
	//
	// private static final String SGD_URL =
	// "http://db.yeastgenome.org/cgi-bin/locus.pl?locus=";
	//
	// private static final String GENECARD_URL =
	// "http://www.genecards.org/cgi-bin/cardsearch.pl?search=";

	public static final HashMap<String, String> UCSC_GENOME_BROWSER_URL = new HashMap<String, String>();
	{
		// initialize UCSC_GENOME_BROWSER_URL HashMap
		UCSC_GENOME_BROWSER_URL.put("210", "");
		UCSC_GENOME_BROWSER_URL
				.put("4932",
						"http://genome.ucsc.edu/cgi-bin/hgTracks?clade=other&org=S.+cerevisiae&position=");
		UCSC_GENOME_BROWSER_URL
				.put("6239",
						"http://genome.ucsc.edu/cgi-bin/hgTracks?clade=worm&org=C.+elegans&position=");
		UCSC_GENOME_BROWSER_URL
				.put("7227",
						"http://genome.ucsc.edu/cgi-bin/hgTracks?clade=insect&org=D.+melanogaster&position=");
		UCSC_GENOME_BROWSER_URL
				.put("9606",
						"http://genome.ucsc.edu/cgi-bin/hgTracks?clade=vertebrate&org=Human&position=");
		UCSC_GENOME_BROWSER_URL
				.put("10090",
						"http://genome.ucsc.edu/cgi-bin/hgTracks?clade=vertebrate&org=Mouse&position=");
		UCSC_GENOME_BROWSER_URL
				.put("10116",
						"http://genome.ucsc.edu/cgi-bin/hgTracks?clade=vertebrate&org=Rat&position=");
	}

	public static final HashMap<String, String> CODE_TO_ORGANISM = new HashMap<String, String>();
	{
		// initialize CODE_TO_ORGANISM HashMap
		CODE_TO_ORGANISM.put("210", "H. pylori");
		CODE_TO_ORGANISM.put("4932", "yeast");
		CODE_TO_ORGANISM.put("6239", "worm");
		CODE_TO_ORGANISM.put("7227", "fly");
		CODE_TO_ORGANISM.put("9606", "human");
		CODE_TO_ORGANISM.put("10090", "mouse");
		CODE_TO_ORGANISM.put("10116", "rat");
		CODE_TO_ORGANISM.put("drug", "drug");
		CODE_TO_ORGANISM.put("superpath", "ucsc_superpathway");
	}

	private final Label totalNeighborsLabel = new Label();
	{
		totalNeighborsLabel
				.setTitle("total number of neighbors in active background networks");
	}

	/**
	 * Image object for circleMap
	 */
	private Image circleMapImage = new Image();
	{
		circleMapImage.addMouseUpHandler(this);
		// circleMapImage.addMouseMoveHandler(this);
	}

	/**
	 * Label for displaying dataset name
	 */
	private Label ringLabel = new Label();

	/**
	 * Panel for displaying ring's color key
	 */
	private VerticalPanel colorKeyPanel = new VerticalPanel();

	// TODO /////////////////////////////////////////////////////

	public BiodeInfoDialogBox(final BiodeInfo biodeInfo, NetworkVisualization nv) {
		super("Entity Information");

		this.bi = biodeInfo;

		netviz = nv;

		nv.getNode(this.bi.getSystematicName()).addNodeListener(this);

		HashSet<String> currentNeighbors = netviz.getNode(
				bi.getSystematicName()).getAdjacentNodes();

		setPopupPosition(POSITION_LEFT, POSITION_TOP);

		FlexTable infoTable = new FlexTable();
		infoTable.setWidth(BOX_WIDTH);
		infoTable.setBorderWidth(BORDER_WIDTH);

		int row = 0;

		infoTable.setText(row, 0, "common name");
		infoTable.setText(row, 1, bi.getCommonName());
		row++;

		String organism = bi.getSystemSpace();
		if (organism.equalsIgnoreCase("9606")) {
			infoTable.setText(row, 0, "Hugo Symbol");
		} else if (organism.equalsIgnoreCase("10090")) {
			infoTable.setText(row, 0, "Entrez Gene ID");
		} else if (organism.equalsIgnoreCase("6239")) {
			infoTable.setText(row, 0, "WormBase Gene ID");
		} else if (organism.equalsIgnoreCase("drug")) {
			infoTable.setText(row, 0, "DrugBank ID");
		} else if (organism.equalsIgnoreCase("4932")) {
			infoTable.setText(row, 0, "SGD ID");
		} else {
			infoTable.setText(row, 0, "systematic name");
		}

		boolean makeLink = true;
		String systematicName = bi.getSystematicName();
		String s = "";
		if (systematicName.endsWith("(complex)")
				|| systematicName.endsWith("(abstract)")
				|| systematicName.endsWith("(family)")
				|| systematicName.endsWith("(smallMolecule)")) {
			makeLink = false;
			s = "(not available)";
		}

		if (makeLink) {
			infoTable.setHTML(
					row,
					1,
					this.makeHtmlATag(bi.getSystematicName(),
							bi.getSystemSpace()));
		} else {
			if (!s.equalsIgnoreCase("(not available)")) {
				s = bi.getCommonName();
			}
			infoTable.setText(row, 1, s);
		}
		row++;

		infoTable.setText(row, 0, "organism");
		infoTable.setText(row, 1, CODE_TO_ORGANISM.get(bi.getSystemSpace()));
		row++;

		infoTable.setText(row, 0, "entity");
		infoTable.setText(row, 1, bi.getBiodeSpace());
		row++;

		infoTable.setText(row, 0, "description");
		infoTable.setText(row, 1, bi.getDescription());
		row++;

		infoTable.setText(row, 0, currentNeighbors.size()
				+ " current neighbors");
		StringBuffer strBuff = new StringBuffer();
		for (String neighborBiode : currentNeighbors) {

			String biode = neighborBiode;
			String commonName = nv.BIC.getBiodeInfo(neighborBiode)
					.getCommonName();
			String systemSpace = nv.BIC.getBiodeInfo(neighborBiode)
					.getSystemSpace();

			makeLink = true;
			if (biode.endsWith("(complex)") || biode.endsWith("(abstract)")
					|| biode.endsWith("(family)")
					|| biode.endsWith("(smallMolecule)")) {
				makeLink = false;
			}

			if (makeLink) {
				s = this.makeHtmlATag(biode, commonName, systemSpace);
			} else {
				s = biode;
			}

			strBuff.append(s + " ");
		}
		infoTable.setHTML(row, 1, strBuff.toString().trim());
		row++;

		infoTable.setText(row, 0, "total neighbors");
		infoTable.setWidget(row, 1, totalNeighborsLabel);
		row++;

		HashSet<String> metaNodeMemberships = bi.getAllMemberships();
		infoTable.setText(row, 0, metaNodeMemberships.size() + " metanodes");
		strBuff = new StringBuffer();
		for (String metaNodeName : metaNodeMemberships) {
			strBuff.append(metaNodeName + " ");
		}
		infoTable.setHTML(row, 1, strBuff.toString().trim());
		row++;

		HashMap<String, String> pblastHashMap = bi.getAllPBlast();
		for (String pblastOrganism : pblastHashMap.keySet()) {
			infoTable.setText(row, 0, "best pBLAST in " + pblastOrganism);
			infoTable.setText(row, 1, pblastHashMap.get(pblastOrganism));
			row++;
		}

		JSONObject scoresJO = bi.getScoresInJSON();

		for (String scoreName : scoresJO.keySet()) {
			double score = scoresJO.get(scoreName).isNumber().doubleValue();

			infoTable.setText(row, 0, scoreName);
			infoTable.setText(row, 1, score + "");
			row++;
		}

		if (netviz.getNode(bi.getSystematicName()).isUsingImage()) {

			resetCircleMapImage();

			HorizontalPanel circleMapPanel = new HorizontalPanel();
			circleMapPanel.add(circleMapImage);

			VerticalPanel circleMapKeyPanel = new VerticalPanel();
			circleMapPanel.add(circleMapKeyPanel);
			circleMapKeyPanel.add(ringLabel);
			circleMapKeyPanel.add(colorKeyPanel);

			infoTable.setText(row, 0, "circle map");
			infoTable.setWidget(row, 1, circleMapPanel);

			row++;
		}

		setWidget(infoTable);

		queryUniqueNeighborsInActiveBackgroundNets(currentRequest);
	}

	/**
	 * Reset the URL for the CircleMap image to use the currently stored url in
	 * the BIC.
	 */
	public void resetCircleMapImage() {
		String imageURL = netviz.getNode(bi.getSystematicName()).getImageURL();
		this.circleMapImage.setUrl(imageURL);
	}

	/**
	 * Makes an html A tag for the id. The link goes to NCBI, Wormbase, or SGD,
	 * depending on the organism.
	 * 
	 * @param id
	 * @param organism
	 * @return
	 */
	private String makeHtmlATag(String id, String organism) {
		StringBuffer sb = new StringBuffer();

		String url = "";
		if (UCSC_GENOME_BROWSER_URL.containsKey(organism)) {
			url = ((String) UCSC_GENOME_BROWSER_URL.get(organism));
		} else if (organism.equalsIgnoreCase("drug")) {
			url = "http://www.drugbank.ca/drugs/";
		}
		// if (organism.equalsIgnoreCase("9606")) {
		// url = NCBI_URL;
		// } else if (organism.equalsIgnoreCase("6239")) {
		// url = WORMBASE_URL;
		// } else if (organism.equalsIgnoreCase("4932")) {
		// url = SGD_URL;
		// }

		sb.append("<A HREF='");
		sb.append(url);
		sb.append(id);
		sb.append("' TARGET='_blank'>");
		sb.append(id);
		sb.append("</A>");

		return sb.toString();
	}

	/**
	 * Makes an html A tag for the id. The link goes to NCBI, Wormbase, or SGD,
	 * depending on the organism.
	 * 
	 * @param id
	 * @param displayName
	 * @param organism
	 * @return
	 */
	private String makeHtmlATag(String id, String displayName, String organism) {
		StringBuffer sb = new StringBuffer();

		String url = "";
		if (UCSC_GENOME_BROWSER_URL.containsKey(organism)) {
			url = ((String) UCSC_GENOME_BROWSER_URL.get(organism));
		} else if (organism.equalsIgnoreCase("drug")) {
			url = "http://www.drugbank.ca/drugs/";
		}

		sb.append("<A HREF='");
		sb.append(url);
		sb.append(id);
		sb.append("' TARGET='_blank'>");
		sb.append(displayName);
		sb.append("</A>");

		return sb.toString();
	}

	/**
	 * Wrapper for
	 * <code>queryUniqueNeighborsInActiveBackgroundNets( biodesHashSet, trackNamesHashSet, currentRequest)</code>
	 * 
	 * @param currentRequest
	 */
	private void queryUniqueNeighborsInActiveBackgroundNets(
			Request currentRequest) {

		HashSet<String> biodesHashSet = new HashSet<String>();
		biodesHashSet.add(bi.getSystematicName());

		HashSet<String> trackNamesHashSet = netviz.getTrackNamesHash(false);

		queryUniqueNeighborsInActiveBackgroundNets(biodesHashSet,
				trackNamesHashSet, currentRequest);

	}

	/**
	 * Get the degree of a node in the active background nets. This is not the
	 * degree of the node in the currently displayed network.
	 * 
	 * @param biodesHashSet
	 * @param trackNamesHashSet
	 * @param currentRequest
	 */
	private void queryUniqueNeighborsInActiveBackgroundNets(
			final HashSet<String> biodesHashSet,
			final HashSet<String> trackNamesHashSet, Request currentRequest) {

		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		RequestCallback requestCallback = new RequestCallback() {
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("getNeighbors failed");
				LoggingDialogBox.log("=====================================");
				LoggingDialogBox.log(exception.toString());
				LoggingDialogBox.log("=====================================");
			}

			public void onResponseReceived(Request request, Response response) {
				// get the results
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						response.getText());

				// check for error message in JSON-RPC response
				if (jsonRpcResp.hasError()) {
					LoggingDialogBox.log("got an error: "
							+ jsonRpcResp.getError().toString());
					return;
				}

				LoggingDialogBox
						.log("no error in JSON-RPC result object, continue");

				JSONObject resultJO = jsonRpcResp.getResult();

				// get JSONarray of biodes
				JSONArray ja = resultJO.get("neighbors").isArray();

				// get hash set of biodes
				HashSet<String> neighborsHashSet = new HashSet<String>();

				for (int i = 0; i < ja.size(); i++) {
					String neighbor = ja.get(i).isString().stringValue();
					neighborsHashSet.add(neighbor);
				}

				// show results
				totalNeighborsLabel.setText(neighborsHashSet.size() + "");
			}
		};

		// build a request to get a result object

		StringBuffer queryStringTrackListSB = new StringBuffer();
		if (trackNamesHashSet.isEmpty()) {
			// do not send request if there are no active networks
			return;
		}
		for (String trackName : trackNamesHashSet) {
			queryStringTrackListSB.append(trackName + ",");
		}

		StringBuffer queryStringBiodeListSB = new StringBuffer();
		for (String id : biodesHashSet) {
			queryStringBiodeListSB.append(id + ",");
		}

		String trackListString = queryStringTrackListSB.toString();

		String urlString = "data/trackdb/commonNeighbors?trackList="
				+ trackListString + "&biodeList="
				+ queryStringBiodeListSB.toString();

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlString);
		try {
			currentRequest = rb.sendRequest(null, requestCallback);
		} catch (Exception e) {
			LoggingDialogBox
					.log("BiodeInfoDialogBox got an exeception sending request "
							+ urlString + ": " + e.toString());
		}
		return;
	}

	@Override
	public void updatedCircleMapURL() {
		resetCircleMapImage();
	}

	/**
	 * Remove this from the node's CircleMapUrlListeners and hide.
	 */
	protected void hideDialogBox() {
		netviz.getNode(this.bi.getSystematicName()).removeNodeListener(this);

		super.hideDialogBox();
	}

	/**
	 * For a CircleMap image, get the ring number in which a MouseEvent
	 * occurred. Returns Integer.MAX_VALUE for some error conditions.
	 * 
	 * @param event
	 * @return
	 */
	private static int getImageEventRingNumber(MouseEvent event) {
		Image image = (Image) event.getSource();

		int numRings = CircleMapDialogBox.getNumDisplayedRings();

		double fullRadius = new Double(image.getWidth()) * 0.5;

		// get radius for each displayed ring
		ArrayList<Double> ringRadii = CircleMapDialogBox.getListOfRingRadii(1,
				numRings, fullRadius);

		// where on the image did the click occur
		double imageEventX = event.getClientX()
				- new Double(image.getAbsoluteLeft());
		double imageEventY = event.getClientY()
				- new Double(image.getAbsoluteTop());

		// where is the center of the image
		double imageCenterX = new Double(image.getWidth()) * 0.5;
		double imageCenterY = new Double(image.getHeight()) * 0.5;

		double eventDistance = Stats.euclideanDist(imageCenterX, imageCenterY,
				imageEventX, imageEventY);

		if (eventDistance == Integer.MAX_VALUE) {
			// do nothing with this click
			return Integer.MAX_VALUE;
		}

		// determine clicked ring
		int eventRingNumber = BasicNodeMouseEventsHandler.determineClickedRing(
				ringRadii, eventDistance);

		return eventRingNumber;
	}

	@Override
	public void onMouseUp(MouseUpEvent event) {
		event.stopPropagation();

		// determine clicked ring
		int eventRingNumber = getImageEventRingNumber(event);

		BasicNodeMouseEventsHandler.handleRingClick(
				netviz.getNode(bi.getSystematicName()), eventRingNumber, false);
	}

	// @Override
	// public void onMouseMove(MouseMoveEvent event) {
	//
	// event.stopPropagation();
	//
	// // determine clicked ring
	// int eventRingNumber = getImageEventRingNumber(event);
	//
	// String datasetName = "";
	//
	// if (eventRingNumber == 0) {
	//
	// } else if (eventRingNumber <= CircleMapDialogBox.getRingDisplayOrder()
	// .size()) {
	// datasetName = CircleMapDialogBox.getRingDisplayOrder().get(
	// eventRingNumber - 1);
	// } else {
	// datasetName = "";
	// }
	//
	// // update ringLabel
	// updateRingLabel(datasetName);
	// }

	/**
	 * update the color key panel
	 * 
	 * @param ringName
	 */
	public void updateColorKeyPanel(String ringName) {
		colorKeyPanel.clear();

		VerticalPanel newColorKeyPanel = CircleMapDialogBox
				.getGroupRingColorKeyPanel(ringName);

		colorKeyPanel.add(newColorKeyPanel);
	}

	/**
	 * Update the ringLabel
	 * 
	 * @param s
	 */
	public void updateRingLabel(String s) {
		String labelText = "ring:" + s;

		if (s.endsWith("_clinical")) {
			labelText = labelText.replace("_clinicalMatrix__", ":");
			labelText = labelText.replace("_clinical", "");
		}

		if (!ringLabel.getText().equalsIgnoreCase(labelText)) {
			ringLabel.setText(labelText);

			// TODO color key
			updateColorKeyPanel(s);
		}
	}
}
