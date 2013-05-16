/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.netviz;

import java.util.HashMap;
import java.util.Set;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.drawpanel.client.DrawPanel;
import edu.ucsc.ib.drawpanel.client.Shape;

/**
 * retrieves scores and assigns scores to nodes' biodes as they are added to the
 * netviz.
 * 
 */
public class EntityScoreAgent implements WorkingSetListener {

	// private static final int RESCALING_FACTOR = 5;
	private static final int RESCALING_FACTOR = 1;

	/**
	 * Mode setting for inactive mode.
	 */
	public static final int INACTIVE_MODE = 0;

	/**
	 * Mode setting for size_and_fill mode. Here, scores obtained from query may
	 * affect size and fill of nodes.
	 */
	public static final int SIZE_AND_FILL_MODE = 1;

	/**
	 * Mode setting for fill mode. Scores obtained in this mode affect the fill
	 * of nodes.
	 */
	public static final int FILL_MODE = 2;

	/**
	 * Mode setting for stroke mode. Scores obtained in this mode affect the
	 * stroke of nodes.
	 */
	public static final int STROKE_MODE = 3;

	private static final String NO_SCORE_COLOR = "lime";

	private static final String NEUTRAL_SCORE_COLOR = "white";

	private static final String URL_FOR_SCORES_SERVICE = "data/conceptScores/getScores";

	private final String name;

	private final double mean;

	private final double sd;

	private final NetworkVisualization nv;

	public Request currentRequest;

	private int mode = INACTIVE_MODE;

	/**
	 * The RGB for maximum positive score. White is rgb(255, 255, 255). Red is
	 * rgb(255, 0, 0). Black is rgb(0, 0, 0). To fade from red to white, hold r
	 * at 255 while increasing g and b from 0 to 255. To fade from red to black,
	 * hold g and b at 0 while decreasing r from 255 to 0.
	 */
	private static final HashMap<String, Integer> posRGB = new HashMap<String, Integer>();
	static {
		posRGB.put("maxR", 255);
		posRGB.put("maxG", 0);
		posRGB.put("maxB", 0);

		posRGB.put("minR", 255);
		posRGB.put("minG", 255);
		posRGB.put("minB", 255);
	}

	/**
	 * The RGB for maximum negative score. White is rgb(255, 255, 255). Blue is
	 * rgb(0, 0, 255). Black is rgb(0, 0, 0). To fade from blue to white, hold b
	 * at 255 while increasing r and g from 0 to 255. To fade from blue to
	 * black, hold r and g at 0 while decreasing b from 255 to 0.
	 */
	private static final HashMap<String, Integer> negRGB = new HashMap<String, Integer>();
	static {
		negRGB.put("maxR", 0);
		negRGB.put("maxG", 0);
		negRGB.put("maxB", 255);

		negRGB.put("minR", 255);
		negRGB.put("minG", 255);
		negRGB.put("minB", 255);
	}

	// TODO ///////////////////////////////////////

	public EntityScoreAgent(String name, double mean, double sd,
			NetworkVisualization netViz) {
		this.nv = netViz;
		this.name = name;
		this.mean = mean;
		this.sd = sd;
	}

	@Override
	public void addedBiodes(final BiodeSet addedBiodeSet) {

		// LoggingDialogBox.log("begin addedBiodes in EntityScoreAgent for "
		// + name);

		queryScoresServer(addedBiodeSet, getMode());
	}

	/**
	 * Query the server for scores.
	 * 
	 * @param addedBiodeSet
	 * @param mode
	 */
	private void queryScoresServer(final BiodeSet addedBiodeSet, final int mode) {
		RequestCallback rc = new RequestCallback() {

			@Override
			public void onResponseReceived(Request request, Response response) {
				// check for OK status code
				if (response.getStatusCode() != 200) {
					LoggingDialogBox.log("EntityScoreAgent for " + name
							+ " did not get OK status(200).  Status code: "
							+ response.getStatusCode());
					return;
				}

				// expect a JSON-RPC compliant result
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						response.getText());

				// check for error message in JSON-RPC response
				if (jsonRpcResp.hasError()) {
					LoggingDialogBox.log("EntityScoreAgent for " + name
							+ " got an error: "
							+ jsonRpcResp.getError().toString());
					return;
				}

				// LoggingDialogBox.log("EntityScoreAgent for " + name
				// + " no error in JSON-RPC result object, continue");

				JSONObject resultJO = jsonRpcResp.getResult();

				JSONArray scoresJA = resultJO.get("scores").isArray();

				// LoggingDialogBox.log("EntityScoreAgent for " + name
				// + " scoresJA.size: " + scoresJA.size());

				switch (mode) {
				case SIZE_AND_FILL_MODE:
					handleSizeAndFillResults(addedBiodeSet, scoresJA);
					break;

				case FILL_MODE:
					handleFillResults(addedBiodeSet, scoresJA);
					break;

				case STROKE_MODE:
					handleStrokeResults(addedBiodeSet, scoresJA);
					break;

				default:
					handleScoreUpdateResults(addedBiodeSet, scoresJA);
					;
				}

			}

			@Override
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("EntityScoreAgent for " + name
						+ " got an error");
			}

		};

		// get parameters
		StringBuffer sb = new StringBuffer(URL_FOR_SCORES_SERVICE + "?");
		sb.append("scoreSetName=" + name);
		sb.append("&concepts=");
		for (String biode : addedBiodeSet) {
			sb.append(biode + ",");
		}
		sb.deleteCharAt(sb.lastIndexOf(","));

		// LoggingDialogBox.log("EntityScoreAgent for " + name +
		// " sb.toString: "
		// + sb.toString());

		// send request
		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
				sb.toString());

		// LoggingDialogBox.log("EntityScoreAgent for " + name + " b.size: " +
		// b.size());

		try {
			if (addedBiodeSet.size() > 0) {
				currentRequest = rb.sendRequest(null, rc);
			}
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
		}
	}

	/**
	 * Use a JSONArray of node scores to update scores.
	 * 
	 * @param addedBiodeSet
	 * @param scoresJA
	 */
	protected void handleScoreUpdateResults(BiodeSet addedBiodeSet,
			JSONArray scoresJA) {

		// set new node fill
		for (int i = 0; i < scoresJA.size(); i++) {
			JSONObject jo = scoresJA.get(i).isObject();
			String concept = jo.get("concept").isString().stringValue();
			double score = jo.get("score").isNumber().doubleValue();

			// update the info with score
			BiodeInfo bi = nv.BIC.getBiodeInfo(concept);

			if (bi == null) {
				LoggingDialogBox.log("null ei for: " + concept);
				continue;
			}

			addScoreToBiode(bi, score);
		}
	}

	/**
	 * Use a JSONArray of node scores to set stroke.
	 * 
	 * @param addedBiodeSet
	 * @param scoresJA
	 */
	protected void handleStrokeResults(BiodeSet addedBiodeSet,
			JSONArray scoresJA) {
		BiodeSet scored = new BiodeSet();

		// set new node fill
		for (int i = 0; i < scoresJA.size(); i++) {
			JSONObject jo = scoresJA.get(i).isObject();
			String concept = jo.get("concept").isString().stringValue();
			double score = jo.get("score").isNumber().doubleValue();

			// update the info with score
			BiodeInfo bi = nv.BIC.getBiodeInfo(concept);

			if (bi == null) {
				LoggingDialogBox.log("null ei for: " + concept);
				continue;
			}

			addScoreToBiode(bi, score);

			// update the visualization

			BasicNode node = nv.getNode(concept);

			setNodeStroke(node, score);

			scored.add(concept);
		}

		// set color of nodes that didn't get scored
		for (String biode : addedBiodeSet) {
			if (!scored.contains(biode)) {
				BasicNode node = nv.getNode(biode);
				setNodeStroke_unscored(node);
			}
		}

	}

	/**
	 * Set the visualization for an unscored node. The fill color is set.
	 * 
	 * @param node
	 */
	private static void setNodeStroke_unscored(BasicNode node) {
		node.setStroke(NEUTRAL_SCORE_COLOR);
	}

	/**
	 * Set the visualization for a scored node. The stroke color is set.
	 * 
	 * @param node
	 * @param score
	 */
	private static void setNodeStroke(BasicNode node, double score) {
		// set color
		if (score > 0) {
			node.setStroke(getGradientLevelColorCode(score / RESCALING_FACTOR,
					posRGB));
		} else if (score < 0) {
			node.setStroke(getGradientLevelColorCode(score
					/ (-1 * RESCALING_FACTOR), negRGB));
		} else {
			node.setStroke(NEUTRAL_SCORE_COLOR);
		}
	}

	/**
	 * Use a JSONArray of node scores to set fill.
	 * 
	 * @param addedBiodeSet
	 * @param scoresJA
	 */
	protected void handleFillResults(BiodeSet addedBiodeSet, JSONArray scoresJA) {
		BiodeSet scored = new BiodeSet();

		// set new node fill
		for (int i = 0; i < scoresJA.size(); i++) {
			JSONObject jo = scoresJA.get(i).isObject();
			String concept = jo.get("concept").isString().stringValue();
			double score = jo.get("score").isNumber().doubleValue();

			// update the info with score
			BiodeInfo bi = nv.BIC.getBiodeInfo(concept);

			if (bi == null) {
				LoggingDialogBox.log("null ei for: " + concept);
				continue;
			}

			addScoreToBiode(bi, score);

			// update the visualization

			BasicNode node = nv.getNode(concept);

			setNodeFill(node, score);

			scored.add(concept);
		}

		// set color of nodes that didn't get scored
		for (String biode : addedBiodeSet) {
			if (!scored.contains(biode)) {
				BasicNode node = nv.getNode(biode);
				setNodeFill_unscored(node);
			}
		}

	}

	/**
	 * Use a JSONArray of node scores to set node size and fill.
	 * 
	 * @param addedBiodeSet
	 * @param scoresJA
	 */
	private void handleSizeAndFillResults(final BiodeSet addedBiodeSet,
			JSONArray scoresJA) {
		BiodeSet scored = new BiodeSet();

		// set new node sizing
		for (int i = 0; i < scoresJA.size(); i++) {
			JSONObject jo = scoresJA.get(i).isObject();
			String concept = jo.get("concept").isString().stringValue();
			double score = jo.get("score").isNumber().doubleValue();

			// update the info with score
			BiodeInfo bi = nv.BIC.getBiodeInfo(concept);

			if (bi == null) {
				LoggingDialogBox.log("null ei for: " + concept);
				continue;
			}

			addScoreToBiode(bi, score);

			// update the visualization
			double sizingFactor = computeNodeSizingFactor(score, mean, sd);

			BasicNode node = nv.getNode(concept);

			setNodeSizeAndFill(node, score, sizingFactor);

			scored.add(concept);
		}

		// set color of nodes that didn't get scored
		for (String biode : addedBiodeSet) {
			if (!scored.contains(biode)) {
				BasicNode node = nv.getNode(biode);
				setNodeSizeAndFill_unscored(node);
			}
		}

		// since size of nodes may have changed, need to update the edge lengths
		nv.updateEdgePositions(nv.getEdgeGroups(addedBiodeSet));
	}

	/**
	 * set the visualization for an unscored node.
	 * 
	 * @param node
	 */
	private static void setNodeSizeAndFill_unscored(final BasicNode node) {
		node.setSize(Shape.DEFAULT_SIZE);
		node.setColor(NO_SCORE_COLOR);
	}

	/**
	 * Set the visualization for a scored node. The fill color is set.
	 * 
	 * @param node
	 * @param score
	 */
	private static void setNodeFill(final BasicNode node, final double score) {

		// set color
		if (score > 0) {
			// node.setColor(POSITIVE_SCORE_COLOR);
			node.setColor(getGradientLevelColorCode(score / RESCALING_FACTOR,
					posRGB));
		} else if (score < 0) {
			// node.setColor(NEGATIVE_SCORE_COLOR);
			node.setColor(getGradientLevelColorCode(score
					/ (-1 * RESCALING_FACTOR), negRGB));
		} else {
			node.setColor(NEUTRAL_SCORE_COLOR);
		}

	}

	/**
	 * Set the node fill to indicate no score.
	 * 
	 * @param node
	 */
	private static void setNodeFill_unscored(BasicNode node) {
		node.setColor(NO_SCORE_COLOR);
	}

	/**
	 * Set the visualization for a scored node. The size and fill color is set.
	 * 
	 * @param node
	 * @param score
	 * @param sizingFactor
	 */
	private static void setNodeSizeAndFill(final BasicNode node,
			final double score, final double sizingFactor) {

		// resize node
		node.setSize(Shape.DEFAULT_SIZE * sizingFactor);

		// set color
		if (score > 0) {
			// node.setColor(POSITIVE_SCORE_COLOR);
			node.setColor(getGradientLevelColorCode(score / RESCALING_FACTOR,
					posRGB));
		} else if (score < 0) {
			// node.setColor(NEGATIVE_SCORE_COLOR);
			node.setColor(getGradientLevelColorCode(score
					/ (-1 * RESCALING_FACTOR), negRGB));
		} else {
			node.setColor(NEUTRAL_SCORE_COLOR);
		}

	}

	@Override
	public void removedBiodes(BiodeSet b) {
		// TODO Auto-generated method stub

	}

	public String getName() {
		return name;
	}

	public double getMean() {
		return mean;
	}

	public double getSd() {
		return sd;
	}

	/**
	 * Set the mode attribute for this object, making sure no other ESA has the
	 * same mode. Use this method after this object has been added to the
	 * NetViz's workingSetListeners.
	 * 
	 * @param newMode
	 */
	public void setMode(final int newMode) {
		// only do something if the mode actually is to be changed
		if (getMode() == newMode) {
			return;
		}

		// turn off all ESA that have this same active mode
		// multiple ESA may be inactive at the same time
		for (WorkingSetListener wsl : nv.getWorkingSetListeners()) {
			if (wsl instanceof EntityScoreAgent) {
				EntityScoreAgent esa = (EntityScoreAgent) wsl;
				if ((newMode != INACTIVE_MODE) && (esa.getMode() == newMode)
						&& (esa != this)) {
					// no 2 ESA may have the same mode
					esa.setMode(INACTIVE_MODE);
				}
			}
		}

		// set mode for this object
		this.mode = newMode;

		// switch over all biodes to this scoreset
		BiodeSet bs = nv.getCurrentNodeIds();
		this.addedBiodes(bs);
	}

	/**
	 * Get the current mode of this object.
	 * 
	 * @return
	 */
	public int getMode() {
		return this.mode;
	}

	/**
	 * If this object's mode is set to inactive, return false. Otherwise, return
	 * true.
	 * 
	 * @return
	 */
	public boolean isActive() {
		if (getMode() == INACTIVE_MODE) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * removes this score from all of netviz's entityinfo objects
	 */
	public void clearScoreFromAllBiodeInfo() {
		for (String biode : nv.BIC.getKeys()) {
			BiodeInfo bi = nv.BIC.getBiodeInfo(biode);
			bi.removeScore(name);

			BasicNode node = nv.getNode(biode);

			node.setSize(Shape.DEFAULT_SIZE);
			node.setColor(bi.getColor());
		}

		nv.updateEdgePositions(nv.getEdgeGroups());
	}

	/**
	 * remove self from netviz's working set listeners and clear self out of
	 * netviz objects
	 */
	public void removeSelfFromNetViz() {
		nv.removeWorkingSetListener(this);
		clearScoreFromAllBiodeInfo();

		LoggingDialogBox.log("ESA for " + this.name
				+ " removed self from netviz");
	}

	/**
	 * add self to netviz's working set listeners. also calls its own
	 * addedbiodes on the current set of biodes to get itself up to speed
	 */
	public void addSelfToNetViz() {
		nv.addWorkingSetListener(this);

		LoggingDialogBox.log("ESA for " + this.name + " added self to netviz");

		// this.addedBiodes(nv.getCurrentNodeIds());
	}

	/**
	 * adds the score to the BiodeInfo
	 * 
	 * @param bi
	 * @param scoreValue
	 */
	private void addScoreToBiode(BiodeInfo bi, final double scoreValue) {
		bi.setScore(name, scoreValue);
	}

	/**
	 * compute a node sizing factor
	 * 
	 * @param score
	 * @param mean
	 * @param sd
	 * @return
	 */
	private static double computeNodeSizingFactor(final double score,
			final double mean, final double sd) {
		double factor = Math.abs((score - mean) / sd) + 1;
		// double factor = Math.log(Math.abs((score - mean) / sd) + 1) + 1;
		// double factor = Math.sqrt(Math.abs((score - mean) / sd)) + 1;
		return factor;
	}

	/**
	 * Get a color gradient level.
	 * 
	 * @param percent
	 * @param colorHashMap
	 * @return
	 */
	public static String getGradientLevelColorCode(double percent,
			HashMap<String, Integer> colorHashMap) {

		String code = DrawPanel.gradientLevel(percent,
				colorHashMap.get("minR"), colorHashMap.get("minG"),
				colorHashMap.get("minB"), colorHashMap.get("maxR"),
				colorHashMap.get("maxG"), colorHashMap.get("maxB"));

		return code;
	}
}
