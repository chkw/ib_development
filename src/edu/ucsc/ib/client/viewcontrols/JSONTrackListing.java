package edu.ucsc.ib.client.viewcontrols;

import java.util.Set;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

/**
 * The data here is retrieved from the MySQL table, tracklist. JSONTracklisting
 * only contains data that originated from the server. Client does not modify
 * anything here. (Only get methods.. no set methods.)
 * 
 * @author cw
 * 
 */
public class JSONTrackListing {

	private JSONObject jsonObject;

	/**
	 * JSONTrackListing is used to get the track information, which may be the
	 * categories of tracks for an organism or the tracks from a category of
	 * tracks.
	 * 
	 * @param jsonValue
	 *            JSONValue
	 */
	public JSONTrackListing(final JSONValue jsonValue) {
		this.jsonObject = jsonValue.isObject();
		if (this.jsonObject == null) {
			throw new IllegalArgumentException(
					"TrackListing must be a JSON object");
		}
	}

	/**
	 * Get a value from a JSONObject as a String.
	 * 
	 * @param obj
	 * @param key
	 * @return
	 */
	private static String getStringValue(JSONObject obj, String key) {
		if (obj != null && obj.containsKey(key)
				&& (obj.get(key).isString() != null)) {
			return obj.get(key).isString().stringValue();
		} else {
			return null;
		}
	}

	/**
	 * Get a value of a JSONObject as a double.
	 * 
	 * @param obj
	 * @param key
	 * @return
	 */
	private static double getNumericalValue(JSONObject obj, String key) {
		if (obj != null && obj.containsKey(key)
				&& (obj.get(key).isNumber() != null)) {
			return obj.get(key).isNumber().doubleValue();
		} else {
			return Double.NaN;
		}
	}

	/**
	 * Get a value of a JSONObject as boolean. If null, returns false.
	 * 
	 * @param obj
	 * @param key
	 * @return
	 */
	private static boolean getBooleanValue(JSONObject obj, String key) {
		if (((obj != null) && (obj.containsKey(key)))
				&& (obj.get(key).isString().stringValue().equals("1"))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get the trScore field for this JSONTrackListing
	 * 
	 * @return
	 */
	public double getTrScore() {
		return getNumericalValue(this.jsonObject, "trScore");
	}

	/**
	 * Get the linkCount field for this JSONTrackListing
	 * 
	 * @return
	 */
	public double getLinkCount() {
		return getNumericalValue(this.jsonObject, "linkCount");
	}

	/**
	 * Get the num_links field for this JSONTrackListing. This field, although
	 * it is a number, is saved as a string in the JSON object, so it must be
	 * converted back to a number here.
	 * 
	 * @return
	 */
	public double getNumLinks() {
		return Double.valueOf(getStringValue(this.jsonObject, "num_links"));
	}

	/**
	 * This is something that looks like: "Ayoubi05_15760455"
	 * 
	 * @return
	 */
	public String getName() {
		return getStringValue(this.jsonObject, "name");
	}

	/**
	 * Get the JSONObject for this JSONTrackListing.
	 * 
	 * @return
	 */
	public JSONObject getJOSNObject() {
		return this.jsonObject;
	}

	/**
	 * Get the "description" from the JSON object.
	 * 
	 * @return
	 */
	public String getDescription() {
		return getStringValue(this.jsonObject, "description");
	}

	public String getColor() {
		return getStringValue(this.jsonObject, "color");
	}

	/**
	 * This is something that looks like:
	 * "Human/Coexpression/Individual/Ayoubi05_15760455"
	 * 
	 * @return
	 */
	public String getTrackName() {
		return getStringValue(this.jsonObject, "name");
	}

	/**
	 * This gets the track groups, a space separated list.
	 * 
	 * @return
	 */
	public String getCategory() {
		return getStringValue(this.jsonObject, "category");
	}

	/**
	 * Gets the String value of "directed" and parses it as a boolean.
	 * 
	 * @return
	 */
	public boolean isDirected() {
		boolean result = false;

		if (jsonObject.containsKey("directional")
				&& (getStringValue(jsonObject, "directional"))
						.equalsIgnoreCase("true")) {
			result = true;
		}

		return result;
	}

	/**
	 * Get the boolean value of "custom".
	 * 
	 * @return
	 */
	public boolean isCustom() {
		return getBooleanValue(this.jsonObject, "custom");
	}

	/**
	 * Find the display name for the track.
	 * 
	 * @return
	 */
	public String getDisplayName() {
		if (this.isCustom()) {
			String dbTrackName = this.getName();
			return dbTrackName.substring(dbTrackName.lastIndexOf("_xx_") + 4,
					dbTrackName.length());
		} else {
			return this.getName();
		}
	}

	/**
	 * Get the NCBI tax ID for this JSONTrackListing.
	 * 
	 * @return
	 */
	public String getOrganism() {
		return getStringValue(this.jsonObject, "NCBI_species");
	}

	/**
	 * Return the set of properties defined in the JSONObject for this
	 * JSONTrackListing.
	 * 
	 * @return
	 */
	public Set<String> getJSONFields() {
		return this.jsonObject.keySet();
	}

	/**
	 * Get the PubMed ID from the track name. It checks the last part of the
	 * name, considered a PMID if it has at least 7 characters and is all
	 * digits. If it's not PMID, return "not found".
	 * 
	 * @return
	 */
	public String getPMID() {
		final String not_found_pmid = "not found";

		String[] splitStr = this.getName().split("_");

		// not a pmid if there isn't a 2nd part to the name
		if (splitStr.length < 2) {
			return not_found_pmid;
		}

		String possible_pmid = splitStr[splitStr.length - 1];

		// not a pmid if it has less than 7 characters
		if (possible_pmid.length() < 7) {
			return not_found_pmid;
		}

		try {
			Integer.parseInt(possible_pmid);
			return possible_pmid;
		} catch (Exception e) {
			// not a pmid if it isn't all digits
			return not_found_pmid;
		}
	}
}
