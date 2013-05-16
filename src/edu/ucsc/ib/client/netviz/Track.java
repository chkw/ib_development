/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.netviz;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;

import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.viewcontrols.BiodeControlPanel;
import edu.ucsc.ib.client.viewcontrols.JSONTrackListing;
import edu.ucsc.ib.client.viewcontrols.TrackControlPanel;
import edu.ucsc.ib.drawpanel.client.TrackLine;

/**
 * A Track is a set of known edges. It may know about much more than what is
 * currently in the NetworkVisualization, and as biodes are added and removed
 * from the graph, it should add the appropriate edges.
 * 
 */
public class Track implements WorkingSetListener {

	private static final String HEAD_MARKER_NAME = "headCap";

	private static final String TAIL_MARKER_NAME = "tailCap";

	protected static final String URL_FOR_NORMAL_TRACK_SERVICE = "data/trackdb/newEdges";

	protected static final String URL_FOR_CUSTOM_TRACK_SERVICE = "data/customTrackDB/newEdges";

	protected final String name;

	protected final NetworkVisualization nv;

	Request currentRequest;

	// color is the SVG line's stroke attribute
	protected String color;

	/**
	 * These are the edges in the track. An edge is defined by two biodes. The
	 * key is the 'lower' biode. The value is a BiodeSet of 'higher' biodes. It
	 * is data for the track obtained from some external datasource.
	 * 
	 */
	final protected Map<String, BiodeSet> edges;

	final protected Map<String, BiodeSet> backEdges;

	protected JSONTrackListing trackListing;

	// TODO ///////////////////////////////////////////////

	public Track(JSONTrackListing tl, NetworkVisualization netViz) {

		trackListing = tl;

		name = tl.getName();
		nv = netViz;

		edges = new HashMap<String, BiodeSet>();
		backEdges = new HashMap<String, BiodeSet>();

		color = trackListing.getColor();

		if (color == null || color.equalsIgnoreCase(null)) {
			// color = TrackControlPanel.CUSTOM_COLOR_LIST[(int)
			// ((Math.floor(Math
			// .random() * 1000)) %
			// TrackControlPanel.CUSTOM_COLOR_LIST.length)];

			color = TrackControlPanel.CUSTOM_COLOR_LIST[name.length()
					% TrackControlPanel.CUSTOM_COLOR_LIST.length];
		} else {
			color = tl.getColor();
		}
	}

	/**
	 * Called by NetworkVisualization in response to additions to its working
	 * set. Adds new edges for the new BiodeSet.
	 * 
	 * @param b
	 *            the BiodeSet of additions
	 */
	public void addedBiodes(BiodeSet b) {
		RequestCallback rc = new RequestCallback() {
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox
						.log("Track.addedBiodes did not get a response: "
								+ exception);
			}

			public void onResponseReceived(Request request, Response response) {

				JsonRpcResponse rpcResp = new JsonRpcResponse(
						response.getText());

				// check for error message in JSON-RPC response
				if (rpcResp.hasError()) {
					LoggingDialogBox.log("got an error: "
							+ rpcResp.getError().toString());
					return;
				}

				JSONObject resultJO = rpcResp.getResult();

				// get the JSONArray from the results JSONObject.
				JSONArray newEdgesJA = resultJO.get("edges").isArray();

				handleDirectedEdges(newEdgesJA);
			}
		};

		// cancel any running request
		// if (currentRequest != null) {
		// currentRequest.cancel();
		// currentRequest = null;
		// }

		String[] oldArray = nv.getCurrentNodeIds().getArray();
		String[] newArray = b.getArray();

		String trackServiceBaseURL;
		if (trackListing.isCustom()) {
			trackServiceBaseURL = URL_FOR_CUSTOM_TRACK_SERVICE;
		} else {
			trackServiceBaseURL = URL_FOR_NORMAL_TRACK_SERVICE;
		}

		String urlStr = trackServiceBaseURL + "?trackName=" + name
				+ "&biodeList1="
				+ BiodeControlPanel.arrayToCommaSeparatedString(oldArray)
				+ "&biodeList2="
				+ BiodeControlPanel.arrayToCommaSeparatedString(newArray);

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlStr);
		try {
			if (b.size() > 0) {
				currentRequest = rb.sendRequest(null, rc);
			}
		} catch (Exception e) {
			LoggingDialogBox.log(e.toString());
		}
	}

	/**
	 * Handle incoming directed edges data. Adds edges with markers where
	 * appropropriate.
	 * 
	 * @param newEdgesJA
	 */
	protected void handleDirectedEdges(final JSONArray newEdgesJA) {
		/**
		 * HashMap keyed on edgeNames. Values are HashMaps of marker
		 * information. Need to have this because edge data may include multiple
		 * edges for the same 2 nodes. For example, a regulatory cycle may
		 * occur.
		 */
		JSONObject edgePropsJO = new JSONObject();

		// Get edge marker settings
		for (int i = 0; i < newEdgesJA.size(); i++) {
			JSONObject newEdgeJO = (JSONObject) newEdgesJA.get(i);

			// get biodes - still in original order as found in data
			// file
			String biode1 = newEdgeJO.get("1").isString().stringValue();
			String biode2 = newEdgeJO.get("2").isString().stringValue();

			String relation = "other";
			if (newEdgeJO.containsKey("relation")) {
				relation = newEdgeJO.get("relation").isString().stringValue();
			}

			double edgeWeight = 1;
			if (newEdgeJO.containsKey("weight")) {
				edgeWeight = newEdgeJO.get("weight").isNumber().doubleValue();
			} else if (newEdgeJO.containsKey("score")) {
				edgeWeight = newEdgeJO.get("score").isNumber().doubleValue();
			}

			// swap order, if needed
			String markerName = TAIL_MARKER_NAME;
			if (biode1.compareTo(biode2) > 0) {
				String t = biode2;
				biode2 = biode1;
				biode1 = t;
				markerName = HEAD_MARKER_NAME;
			}

			String edgeName = NetworkEdgeGroup.getEdgeName(biode1, biode2);

			// set default edge properties
			if (!edgePropsJO.containsKey(edgeName)) {

				// default line markers
				JSONObject markersJO = new JSONObject();

				markersJO
						.put(TAIL_MARKER_NAME, new JSONNumber(TrackLine.PLAIN));
				markersJO
						.put(HEAD_MARKER_NAME, new JSONNumber(TrackLine.PLAIN));

				// useDash status
				JSONBoolean useDashJB = JSONBoolean.getInstance(false);

				// edge weight
				JSONNumber edgeWeightJN = new JSONNumber(1);

				// set edge props
				JSONObject newEdgePropsJO = new JSONObject();
				edgePropsJO.put(edgeName, newEdgePropsJO);
				newEdgePropsJO.put("markers", markersJO);
				newEdgePropsJO.put("useDash", useDashJB);
				newEdgePropsJO.put("edgeWeight", edgeWeightJN);
			}

			JSONObject edgeProps = edgePropsJO.get(edgeName).isObject();

			// set marker
			int marker = getLineMarkerType(relation);

			JSONObject markersJO = edgeProps.get("markers").isObject();

			if (markersJO.get(markerName).isNumber().doubleValue() == new Double(
					TrackLine.PLAIN)) {
				markersJO.put(markerName, new JSONNumber(marker));
			} else {
				// do not overwrite a non-plain marker
			}

			// set dash status
			boolean useDash = determineUseDashedLine(relation);
			if (useDash) {
				edgeProps.put("useDash", JSONBoolean.getInstance(true));
			}

			// TODO set edge weight
			edgeProps.put("edgeWeight", new JSONNumber(edgeWeight));
		}

		// draw the edges
		drawEdges(edgePropsJO);
	}

	/**
	 * Draw edges.
	 * 
	 * @param edgeProps
	 *            Each key in this JSONObject is the name of an edge. Each value
	 *            is a JSONObject with the following:
	 *            <DL>
	 *            <DT>markers</DT>
	 *            <DD>JSONObject with key:value pairs for head marker and tail
	 *            marker.</DD>
	 *            <DT>useDash</DT>
	 *            <DD>JSONBoolean for specifying dashed line.</DD>
	 *            <DT>edgeWeight</DT>
	 *            <DD>JSONNumber for specifying edge weight.</DD>
	 *            </DL>
	 */
	private void drawEdges(JSONObject edgeProps) {
		for (String edgeName : edgeProps.keySet()) {

			String[] biodes = NetworkEdgeGroup.separateEdgeName(edgeName);

			// get properties of edge
			JSONObject edgePropsJO = edgeProps.get(edgeName).isObject();

			int headMarker = (int) edgePropsJO.get("markers").isObject()
					.get(HEAD_MARKER_NAME).isNumber().doubleValue();

			int tailMarker = (int) edgePropsJO.get("markers").isObject()
					.get(TAIL_MARKER_NAME).isNumber().doubleValue();

			boolean useDash = edgePropsJO.get("useDash").isBoolean()
					.booleanValue();

			double edgeWeight = edgePropsJO.get("edgeWeight").isNumber()
					.doubleValue();

			// add edge
			addEdge(biodes[0], biodes[1], headMarker, tailMarker, useDash,
					edgeWeight);
		}
	}

	/**
	 * Get the line marker type for a relation.
	 * 
	 * @param relation
	 * @return
	 */
	private static int getLineMarkerType(final String relation) {
		int marker;
		if (relation.contains("component")) {
			marker = TrackLine.PLAIN;
		} else if (relation.contains(">")) {
			marker = TrackLine.ARROW;
		} else if (relation.contains("|")) {
			marker = TrackLine.BAR;
		} else {
			marker = TrackLine.PLAIN;
		}
		return marker;
	}

	/**
	 * Component and "a" relations use dashed lines. "t" relations and all
	 * others use solid lines.
	 * 
	 * @param relation
	 * @return
	 */
	private static boolean determineUseDashedLine(String relation) {
		boolean useDash;
		if (relation.contains("component")) {
			useDash = true;
		} else if (relation.contains("a>") || relation.contains("a|")) {
			useDash = true;
		} else {
			useDash = false;
		}
		return useDash;
	}

	/**
	 * Add an edge to a NetworkEdgeGroup. The NEG is retrieved from nv and then
	 * a line, representing a link in this track, is added. Finds alphabetical
	 * ordering of biodes before doing anything.
	 * 
	 * @param headBiode
	 *            one biode of the edge
	 * @param tailBiode
	 *            the other biode of the edge
	 * @param headCap
	 *            Marker to use on "head" end of the edge
	 * @param tailCap
	 *            Marker to use on "tail" end of the edge
	 * @param isDashed
	 *            If true, draw a dashed line. Otherwise, draw a solid line.
	 * @param edgeWeight
	 *            Factor by which the line's default stroke width will be
	 *            multiplied for display. This is meant to indicate some kind of
	 *            score for the edge.
	 */
	private void addEdge(String headBiode, String tailBiode, int headCap,
			int tailCap, boolean isDashed, double edgeWeight) {

		// update record keeping
		addEdgeData(headBiode, tailBiode);

		// determine flipped status
		boolean flipped = false;
		if (headBiode.compareTo(tailBiode) > 0) {
			String t = tailBiode;
			tailBiode = headBiode;
			headBiode = t;
			flipped = true;
		}

		if (!flipped) {
			nv.getEdgeGroup(headBiode, tailBiode).addTrack(this, headCap,
					tailCap, isDashed, edgeWeight);
		} else {
			nv.getEdgeGroup(headBiode, tailBiode).addTrack(this, tailCap,
					headCap, isDashed, edgeWeight);
		}

	}

	/**
	 * update the edge data. does not draw lines.
	 * 
	 * @param headBiode
	 * @param tailBiode
	 */
	protected void addEdgeData(final String headBiode, final String tailBiode) {
		// forward edge
		BiodeSet b = (BiodeSet) edges.get(headBiode);
		if (b == null) {
			b = new BiodeSet();
			edges.put(headBiode, b);
		}
		b.add(tailBiode);

		// backward edge
		BiodeSet bBack = (BiodeSet) backEdges.get(tailBiode);
		if (bBack == null) {
			bBack = new BiodeSet();
			backEdges.put(tailBiode, bBack);
		}
		bBack.add(headBiode);
	}

	/**
	 * Get the neighbors in this object's edge data. Does not query the server
	 * for neighbors. This uses the edge data that the client has stored.
	 * 
	 * @param biode
	 * @return
	 */
	protected BiodeSet getEdgeDataNeighbors(final String biode) {
		BiodeSet neighbors = new BiodeSet();

		neighbors.addAll(edges.get(biode));
		neighbors.addAll(backEdges.get(biode));

		return neighbors;
	}

	/**
	 * Get the color of this track.
	 * 
	 * @return a String representation of this track's color
	 */
	public String getColor() {
		return color;
	}

	/**
	 * Gets the current working set of biodes.
	 * 
	 * @return BiodeSet of current working set
	 */
	public BiodeSet getCurrentBiodeSet() {
		return nv.getCurrentNodeIds();
	}

	/**
	 * This track needs a way to get the edgeGroup connecting two biodes.
	 * 
	 * @param firstBiode
	 *            one biode of the edge
	 * @param secondBiode
	 *            the other biode of the edge
	 * @return the edge group
	 */
	public NetworkEdgeGroup getEdgeGroup(String firstBiode, String secondBiode) {
		return nv.getEdgeGroup(firstBiode, secondBiode);
	}

	/**
	 * Get the line style (solid, dashed, etc) of this track.
	 * 
	 * @return a int representing the stroke style
	 */
	public int getLineStyle() {
		return 0;
	}

	/**
	 * Get the JSONTrackListing for this Track.
	 * 
	 * @return
	 */
	public JSONTrackListing getTrackListing() {
		return trackListing;
	}

	/**
	 * Removes this track from the appropriate edge group.
	 * 
	 * @param biodeA
	 *            one biode of the edge
	 * @param biodeB
	 *            the other biode of the edge
	 */
	public void removeEdge(String biodeA, String biodeB) {
		NetworkEdgeGroup neg = nv.getEdgeGroup(biodeA, biodeB);
		neg.removeTrack(this);
	}

	/**
	 * Remove all edges from this track
	 */
	public void removeAllEdges() {
		String biodeA;
		String biodeB;
		BiodeSet b;
		for (Iterator<String> i = edges.keySet().iterator(); i.hasNext();) {
			biodeA = i.next();
			b = edges.get(biodeA);
			for (Iterator<String> j = b.iterator(); j.hasNext();) {
				biodeB = j.next();
				removeEdge(biodeA, biodeB);
			}
		}
		edges.clear();
		backEdges.clear();
	}

	/**
	 * Called by NetworkVisualization in response to removals from its working
	 * set. Note that it does not need to deal with the NetworkEdgeGroups, as
	 * those are handled by the NetworkVisualization
	 * 
	 * @param b
	 *            the BiodeSet of nodes to remove
	 */
	public void removedBiodes(BiodeSet b) {
		for (Iterator<String> i = b.iterator(); i.hasNext();) {
			String biode = i.next();
			purgeEdges(biode, edges, backEdges);
			purgeEdges(biode, backEdges, edges);
		}
	}

	/**
	 * @param biode
	 *            the biode to remove
	 * @param edgesTo
	 *            the source of the which edges to remove
	 * @param edgesFrom
	 *            where edges are removed from
	 */
	private final void purgeEdges(String biode, Map<String, BiodeSet> edgesTo,
			Map<String, BiodeSet> edgesFrom) {
		BiodeSet to = edgesTo.get(biode);
		if (to != null) {
			for (Iterator<String> i = to.iterator(); i.hasNext();) {
				String other = i.next();
				BiodeSet bBack = edgesFrom.get(other);
				bBack.remove(biode);
				if (bBack.size() == 0) {
					edgesFrom.remove(other);
				}
			}
			edgesTo.remove(biode);
		}
	}

	/**
	 * Get the name for this Track. This is the name of the DB table for the
	 * track.
	 * 
	 * @return
	 */
	public String getName() {
		return trackListing.getName();
	}

	/**
	 * Get the display name.
	 * 
	 * @return
	 */
	public String getDisplayName() {
		return trackListing.getDisplayName();
	}

	/**
	 * Dumping some information to the DataVizPanel's logger for debugging use.
	 */
	public void dumpToLog() {
		LoggingDialogBox.log("Dumping track structure, name: " + name);
		LoggingDialogBox.log("Color: " + color);
		LoggingDialogBox.log("Edges Structure:");
		for (Iterator<String> i = edges.keySet().iterator(); i.hasNext();) {
			String biodeA = i.next();
			LoggingDialogBox.log("Biode " + biodeA + " has destinations "
					+ edges.get(biodeA));
		}
		LoggingDialogBox.log("backEdgesStructure:");
		for (Iterator<String> i = backEdges.keySet().iterator(); i.hasNext();) {
			String biode = i.next();
			LoggingDialogBox.log("Biode " + biode + " has sources + "
					+ backEdges.get(biode));
		}
		LoggingDialogBox.log("Dump of track structure complete");
	}

	/**
	 * Set the color for the track.
	 * 
	 * @param color
	 */
	public void setColor(String color) {
		this.color = color;
	}

	/**
	 * If directed, "true". If undirected, "false".
	 * 
	 * @return
	 */
	// public boolean isDirected() {
	// return trackListing.isDirected();
	// }

	/**
	 * Return true if custom track.
	 * 
	 * @return
	 */
	public boolean isCustom() {
		return trackListing.isCustom();
	}
}
