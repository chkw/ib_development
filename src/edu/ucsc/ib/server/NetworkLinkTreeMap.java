package edu.ucsc.ib.server;

import java.util.Set;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Child class of TreeMap for storing network non-directed link data with score.
 * 
 * @author cw
 * 
 */
public class NetworkLinkTreeMap extends
		TreeMap<String, TreeMap<String, Double>> {

	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 6933162310985705586L;

	/**
	 * Append the links to this object. If link exists, the larger score is
	 * saved. **currently assumes undirected links**
	 * 
	 * @param toAppend
	 */
	public void append(final NetworkLinkTreeMap toAppend) {
		for (String appendElement1 : toAppend.keySet()) {
			TreeMap<String, Double> appendElement2TreeMap = toAppend
					.get(appendElement1);
			for (String appendElement2 : appendElement2TreeMap.keySet()) {
				double appendScore = appendElement2TreeMap.get(appendElement2);

				addLink(appendElement1, appendElement2, appendScore, false);
			}
		}
	}

	/**
	 * Add a relation. Links will be directional unless it is a component link
	 * or a member link, which will be represented as undirected links.
	 * 
	 * @param element1
	 * @param element2
	 * @param relation
	 */
	public void addRelation(final String element1, final String element2,
			final String relation) {
		// check for self-link
		if (element1.equalsIgnoreCase(element2)) {
			return;
		}

		// handle element1 as key
		if (!this.containsKey(element1)) {
			// element1 does not exist as a key, create it
			this.put(element1, new TreeMap<String, Double>());
		} else {
			// element1 exists as a key
			// nothing to do
		}
		// assign value to link
		this.get(element1).put(element2, new Double(1));

		// handle element2 as key
		// if (!relation.contains("component") && !relation.contains("member"))
		// {
		if (!relation.contains("component")) {
			// do nothing, do not store the mirror edge
			return;
		}

		if (!this.containsKey(element2)) {
			// element2 does not exist as a key
			this.put(element2, new TreeMap<String, Double>());
		} else {
			// element2 exists as a key
			// nothing to do
		}
		// assign value to link
		this.get(element2).put(element1, new Double(1));
	}

	/**
	 * Add a link. If undirected, adds 2 entries: one using element1 as the key
	 * and a second using element2 as the key. Self-links are ignored. Larger
	 * score is saved.
	 * 
	 * @param element1
	 * @param element2
	 * @param score
	 * @param directed
	 */
	public void addLink(final String element1, final String element2,
			final Double score, final boolean directed) {
		// check for self-link
		if (element1.equalsIgnoreCase(element2)) {
			return;
		}

		double score_to_record = score;

		// handle element1 as key
		if (!this.containsKey(element1)) {
			// element1 does not exist as a key
			this.put(element1, new TreeMap<String, Double>());
		} else {
			// element1 exists as a key
			if (this.get(element1).containsKey(element2)) {
				// element2 exists as a key
				if (this.getScore(element1, element2) > score_to_record) {
					// use the bigger score
					score_to_record = this.getScore(element1, element2);
				}
			}
		}

		// handle element2 as key

		if (directed) {
			// do nothing, do not store the mirror edge
		} else if (!this.containsKey(element2)) {
			// element2 does not exist as a key
			this.put(element2, new TreeMap<String, Double>());
		} else {
			// element2 exists as a key
			if (this.get(element2).containsKey(element1)) {
				// element1 exists as a key
				if (this.getScore(element2, element1) > score_to_record) {
					// use the bigger score
					score_to_record = this.getScore(element2, element1);
				}
			}
		}

		// record score
		this.get(element1).put(element2, score_to_record);
		if (!directed) {
			// store the mirror edge only for non-directed link
			this.get(element2).put(element1, score_to_record);
		}
	}

	/**
	 * Set the score for a link. Ignores self-links. Replaces previous value, if
	 * there was one. If undirected, sets the score for going both directions.
	 * 
	 * @param element1
	 * @param element2
	 * @param score
	 * @param directed
	 */
	public void setScore(final String element1, final String element2,
			final Double score, final boolean directed) {
		// check for self-link
		if (element1.equalsIgnoreCase(element2)) {
			return;
		}

		double score_to_record = score;

		// handle element1 as key
		if (!this.containsKey(element1)) {
			// element1 does not exist as a key
			this.put(element1, new TreeMap<String, Double>());
		}
		// record score
		this.get(element1).put(element2, score_to_record);

		// skip the mirror link if directed
		if (directed) {
			return;
		}

		// handle element2 as key
		if (!this.containsKey(element2)) {
			// element2 does not exist as a key
			this.put(element2, new TreeMap<String, Double>());
		}
		// record score
		this.get(element2).put(element1, score_to_record);
	}

	/**
	 * Compute the current average score.
	 * 
	 * @return
	 */
	public double getAverageScore() {
		int num_links = 0;
		double cumulative_score = 0;

		for (String element1 : this.keySet()) {
			Set<String> neighbors = this.getNeighbors(element1);
			for (String element2 : neighbors) {
				num_links++;
				cumulative_score += this.getScore(element1, element2);
			}
		}

		if (num_links == 0) {
			return 0;
		}

		return cumulative_score / num_links;
	}

	/**
	 * Get the score for the edge.
	 * 
	 * @param element1
	 * @param element2
	 * @return
	 */
	public double getScore(final String element1, final String element2) {
		return this.get(element1).get(element2);
	}

	/**
	 * Get the neighbors of the specified element
	 * 
	 * @param element
	 * @return
	 */
	public Set<String> getNeighbors(final String element) {
		return this.get(element).keySet();
	}

	/**
	 * Get network links in a JSONObject. Note that the returned object may be
	 * double the expected size since each link appears twice, one time for each
	 * element used as the key.
	 * 
	 * @return
	 * @throws JSONException
	 */
	public JSONObject toJson() throws JSONException {
		JSONObject resultJO = new JSONObject();

		for (String element1 : this.keySet()) {
			JSONArray ja = new JSONArray();
			for (String element2 : this.get(element1).keySet()) {
				ja.put(element2);
			}
			resultJO.put(element1, ja);
		}

		return resultJO;
	}
}
