package edu.ucsc.ib.client.netviz;

import java.util.HashMap;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.viewcontrols.JSONTrackListing;

/**
 * Class for displaying graph elements (nodes & edges) in a static manner. This
 * class is called "StaticTrack" because it does not listen for addition of
 * nodes; It is not dynamic. As such, edges must be passed directly for this
 * class to know about them. This class *does* listen for node removal.
 * 
 * @author cw
 * 
 */
public class StaticTrack extends Track {

	/**
	 * nested HashMap objects: concept1, concept2, propertiesHashMap. The
	 * propertiesHashMap has keys on relation, score, etc.
	 */
	private final HashMap<String, HashMap<String, HashMap<String, String>>> trackDataHashMap = new HashMap<String, HashMap<String, HashMap<String, String>>>();

	// TODO //////////////////////////////////////////

	/**
	 * Constructor.
	 * 
	 * @param tl
	 * @param relationsJA
	 *            JSONArray with JSONObjects with JSONString values for 1, 2,
	 *            and relation and/or score.
	 * @param netViz
	 */
	public StaticTrack(JSONTrackListing tl, JSONArray edgesJA,
			NetworkVisualization netViz) {
		super(tl, netViz);
		loadEdges(edgesJA);
	}

	/**
	 * Load edges into this object. Loads "relation" and "weight" property, if
	 * they are found.
	 * 
	 * @param edgesJA
	 */
	public void loadEdges(final JSONArray edgesJA) {
		for (int i = 0; i < edgesJA.size(); i++) {
			JSONObject relationJO = edgesJA.get(i).isObject();

			String concept1 = relationJO.get("1").isString().stringValue();
			String concept2 = relationJO.get("2").isString().stringValue();

			// store relation
			String relation = "other";
			if (relationJO.containsKey("relation")) {
				relation = relationJO.get("relation").isString().stringValue();
			}
			addEdgeProperty(concept1, concept2, "relation", relation);

			// store edge weight
			if (relationJO.containsKey("weight")) {
				double weight = relationJO.get("weight").isNumber()
						.doubleValue();

				addEdgeProperty(concept1, concept2, "weight", "" + weight);
			}
		}
	}

	/**
	 * Add an edge with a property. Does not draw the edge. Just adds the data
	 * to this object's data.
	 * 
	 * @param concept1
	 * @param concept2
	 * @param propName
	 * @param propValue
	 */
	private void addEdgeProperty(final String concept1, final String concept2,
			final String propName, final String propValue) {
		if (!trackDataHashMap.containsKey(concept1)) {
			// new concept1
			// need to add new concept2 and new properties
			HashMap<String, String> propertiesHashMap = new HashMap<String, String>();

			HashMap<String, HashMap<String, String>> concept2HashMap = new HashMap<String, HashMap<String, String>>();
			concept2HashMap.put(concept2, propertiesHashMap);

			trackDataHashMap.put(concept1, concept2HashMap);
		} else if (!trackDataHashMap.get(concept1).containsKey(concept2)) {
			// new concept2 for this concept1
			// need to add new properties
			HashMap<String, String> propertiesHashMap = new HashMap<String, String>();
			trackDataHashMap.get(concept1).put(concept2, propertiesHashMap);
		}

		// update the relation
		trackDataHashMap.get(concept1).get(concept2).put(propName, propValue);
	}

	/**
	 * Get the edge data that is relevant to the BiodeSet.
	 * 
	 * @param b
	 * @return
	 */
	private JSONArray getRelevantEdges(final BiodeSet b) {
		JSONArray edgesJA = new JSONArray();

		for (String concept1 : trackDataHashMap.keySet()) {
			if (b.contains(concept1)) {
				for (String concept2 : trackDataHashMap.get(concept1).keySet()) {
					if (b.contains(concept2)) {
						// create a JSONObject to add to edgesJA
						JSONObject edgeJO = new JSONObject();
						edgesJA.set(edgesJA.size(), edgeJO);

						edgeJO.put("1", new JSONString(concept1));
						edgeJO.put("2", new JSONString(concept2));

						HashMap<String, String> propsHashMap = trackDataHashMap
								.get(concept1).get(concept2);

						edgeJO.put("relation",
								new JSONString(propsHashMap.get("relation")));

						if (propsHashMap.containsKey("weight")) {
							double weight = Double.valueOf(propsHashMap
									.get("weight"));
							edgeJO.put("weight", new JSONNumber(weight));
						}
					} else {
						// skip it
					}
				}
			} else {
				// skip it
			}
		}

		return edgesJA;
	}

	/**
	 * This class does not query trackservice on node additions. It uses its own
	 * record of edges.
	 * 
	 * @param newBiodeSet
	 */
	public void addedBiodes(BiodeSet newBiodeSet) {
		BiodeSet allNetVizBiodes = nv.getCurrentNodeIds();
		allNetVizBiodes.addAll(newBiodeSet);

		JSONArray edgesJA = getRelevantEdges(newBiodeSet);

		handleDirectedEdges(edgesJA);
	}
}
