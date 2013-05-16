package edu.ucsc.ib.client.viewcontrols;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.StaticTrack;

/**
 * Wrapper to collect together the data that is required to visualize a pathway.
 * Along with some metadata, it contains a discrete set of concepts and
 * relations. To visualize the pathway, there is a method,
 * {@link #visualize(NetworkVisualization nv)}.
 * 
 * @author chrisw
 * 
 */
public class PathwayData {

	/**
	 * Contains metadata.
	 */
	private final JSONObject metaDataJO;

	/**
	 * Contains concept JSONObjects with values keyed on "name".
	 */
	private final JSONArray conceptsJA;

	/**
	 * Contains relations JSONObjects with values keyed on "1", "2", "relation",
	 * and/or "score".
	 */
	private final JSONArray relationsJA;

	/**
	 * Wrapper for
	 * {@link #PathwayData(JSONObject metaDataJO, JSONArray conceptsJA, JSONArray relationsJA)}
	 * . Uses the resultObject of a JSON-RPC response to instantiate a
	 * PathwayData object. Simple parameter validation can be done via
	 * {@link #resultJoIsValid(JSONObject)}.
	 * 
	 * @param resultJO
	 */
	public PathwayData(JSONObject resultJO) {
		this(resultJO.get("metadata").isObject(), resultJO.get("concepts")
				.isArray(), resultJO.get("relations").isArray());
	}

	/**
	 * Constructor.
	 * 
	 * @param metaDataJO
	 * @param conceptsJA
	 * @param relationsJA
	 */
	public PathwayData(JSONObject metaDataJO, JSONArray conceptsJA,
			JSONArray relationsJA) {
		this.metaDataJO = metaDataJO;
		this.conceptsJA = conceptsJA;
		this.relationsJA = relationsJA;
	}

	/**
	 * Basic check to see if the result object from a JsonRpcResponse is valid
	 * for constructing a PathwayData object.
	 * 
	 * @param resultJO
	 *            the result JSONObject from a JSON-RPC response
	 * @return
	 */
	public static boolean resultJoIsValid(JSONObject resultJO) {
		// check for metadata, concepts, and relations objects
		if (!(resultJO.containsKey("concepts")
				&& resultJO.containsKey("relations") && resultJO
					.containsKey("metadata"))) {
			return false;
		}

		// check metadata
		JSONObject jo = resultJO.get("metadata").isObject();
		if (!(jo.containsKey("name") && jo.containsKey("source") && jo
				.containsKey("NCBI_species"))) {
			return false;
		}

		// check if concepts and relations objects are JSONArray
		if (!(resultJO.get("concepts").isArray() != null && resultJO.get(
				"relations").isArray() != null)) {
			return false;
		}

		return true;
	}

	public void visualize_annotations_included(NetworkVisualization nv) {
		String organism = metaDataJO.get("NCBI_species").isString()
				.stringValue();

		// handle concepts
		BiodeSet bs = new BiodeSet();
		for (int i = 0; i < conceptsJA.size(); i++) {
			JSONObject conceptJO = conceptsJA.get(i).isObject();

			BiodeInfo bi = new BiodeInfo(conceptJO.get("ID").isString()
					.stringValue());
			bi.setDescription(conceptJO.get("desc").isString().stringValue());
			bi.setBiodeSpace("gene");
			bi.setSystemSpace(organism);
			bi.setCommonName(conceptJO.get("common").isString().stringValue());

			// set position
			if (conceptJO.containsKey("x") && conceptJO.containsKey("y")) {
				double x = conceptJO.get("x").isNumber().doubleValue();
				double y = conceptJO.get("y").isNumber().doubleValue();
				bi.setPosition(x, y);
			}

			nv.BIC.addBiodeInfo(bi);

			bs.add(bi.getSystematicName());
		}

		nv.addBiode(bs);

		// handle relations
		String trackListingName = metaDataJO.get("name").isString()
				.stringValue()
				+ " from " + metaDataJO.get("source").isString().stringValue();

		// check if track is in NetViz
		if (nv.getTrackNamesHash().contains(trackListingName)) {
			// do not create a duplicate of a previously activated track
			return;

		} else {
			// new track

			JSONObject jo = new JSONObject();

			jo.put("name", new JSONString(trackListingName));
			// jo.put("color", new JSONString("lime"));
			JSONTrackListing jtl = new JSONTrackListing(jo);

			StaticTrack pathwayTrack = new StaticTrack(jtl, relationsJA, nv);

			nv.addTrack(pathwayTrack);
		}
	}

	/**
	 * Turn on visualization of this pathway. Adds nodes and edges to a
	 * NetworkVisualization. Submits request for biode annotations.
	 * 
	 * @param nv
	 *            NetworkVisualization to visualize on
	 */
	public void visualize(NetworkVisualization nv) {
		String organism = metaDataJO.get("NCBI_species").isString()
				.stringValue();

		// handle concepts
		BiodeSet bs = new BiodeSet();
		for (int i = 0; i < conceptsJA.size(); i++) {
			bs.add(conceptsJA.get(i).isObject().get("name").isString()
					.stringValue());
		}

		BiodeUserInputPanel.processSubmissionWithLookupService_single_sp(
				organism, bs.getArray(), true);

		// handle relations
		String trackListingName = metaDataJO.get("name").isString()
				.stringValue()
				+ " from " + metaDataJO.get("source").isString().stringValue();

		// check if track is in NetViz
		if (nv.getTrackNamesHash().contains(trackListingName)) {
			// do not create a duplicate of a previously activated track
			return;

		} else {
			// new track

			JSONObject jo = new JSONObject();

			jo.put("name", new JSONString(trackListingName));
			// jo.put("color", new JSONString("lime"));
			JSONTrackListing jtl = new JSONTrackListing(jo);

			StaticTrack pathwayTrack = new StaticTrack(jtl, relationsJA, nv);

			nv.addTrack(pathwayTrack);
		}
	}
}
