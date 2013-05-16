package edu.ucsc.ib.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Servlet to get track data from database.
 * 
 * @author Chris
 * 
 */
public class TrackRecommenderDbService extends DatabaseService {
	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = -3970142941459548245L;

	/**
	 * Constructor for this service. Calls DatabaseService constructor.
	 */
	public TrackRecommenderDbService() {
		super();
	}

	/**
	 * Handle GET request. Hand over to doPost() method.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		this.doPost(req, resp);
	}

	/**
	 * Handle POST request.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		String path = req.getPathInfo();

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/test

			try {
				super.writeTextResponse("success? " + testConnection(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("tr_gene_count")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr_gene_count?organism=4932&biodes=YAL021C,YCR093W,YDL165W,YER068W,YIL038C,YNR052C,YPR072W

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			try {
				super.writeTextResponse(this
						.getTrackRecommenderStats_gene_count(organism,
								csvBiodeList), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("tr_link_count")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr_link_count?organism=4932&biodes=YAL021C,YCR093W,YDL165W,YER068W,YIL038C,YNR052C,YPR072W

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			try {
				super.writeTextResponse(this
						.getTrackRecommenderStats_link_count(organism,
								csvBiodeList), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("tr0")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr0?organism=4932&biodes=YAL021C,YCR093W,YDL165W,YER068W,YIL038C,YNR052C,YPR072W

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			try {
				super.writeTextResponse(this.testGettingInvertedIndexData(
						organism, csvBiodeList), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("tr1")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr1?organism=4932&biodes=YAL021C,YCR093W,YDL165W,YER068W,YIL038C,YNR052C,YPR072W

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			try {
				super.writeTextResponse(
						this.getTrackRecommenderStats1(organism, csvBiodeList),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("tr2")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr2?organism=4932&biodes=YAL021C,YCR093W,YDL165W,YER068W,YIL038C,YNR052C,YPR072W

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			try {
				super.writeTextResponse(
						this.getTrackRecommenderStats2(organism, csvBiodeList),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("tr3")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr3?organism=4932&biodes=YAL021C,YCR093W,YDL165W,YER068W,YIL038C,YNR052C,YPR072W

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			try {
				super.writeTextResponse(
						this.getTrackRecommenderStats3(organism, csvBiodeList),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("tr4")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr4?organism=9606&biodes=8776,4659,51592,22823,26100,490,3895,2186,9802

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			try {
				super.writeTextResponse(
						this.getTrackRecommenderStats4(organism, csvBiodeList),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("tr5")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr5?organism=9606&biodes=8776,4659,51592,22823,26100,490,3895,2186,9802

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			try {
				super.writeTextResponse(
						this.getTrackRecommenderStats5(organism, csvBiodeList),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if ((path.endsWith("tr6")) || (path.endsWith("tr"))) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr6?organism=9606&biodes=8776,4659,51592,22823,26100,490,3895,2186,9802

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			try {
				super.writeTextResponse(this.getTrackRecommenderStats6(
						organism, csvBiodeList, 20), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("tr6_special")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr6_special?returnSize=50&organism=9606&biodes=8776,4659,51592,22823,26100,490,3895,2186,9802

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			int returnSize = Integer.valueOf(req.getParameter("returnSize"));
			try {
				super.writeTextResponse(this.getTrackRecommenderStats6_reduced(
						organism, csvBiodeList, returnSize), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("tr6_2_joins")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr6_2_joins?returnSize=50&organism=9606&biodes=8776,4659,51592,22823,26100,490,3895,2186,9802

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			int returnSize = Integer.valueOf(req.getParameter("returnSize"));
			try {
				super.writeTextResponse(this
						.getTrackRecommenderStats6_reduced_2_joins(organism,
								csvBiodeList, returnSize), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("tr6_in_in")) {
			// test with:
			// http://localhost:8080/ib/data/trdb/tr6_in_in?returnSize=50&organism=9606&biodes=8776,4659,51592,22823,26100,490,3895,2186,9802

			String organism = req.getParameter("organism");
			String csvBiodeList = req.getParameter("biodes");
			int returnSize = Integer.valueOf(req.getParameter("returnSize"));
			try {
				super.writeTextResponse(this
						.getTrackRecommenderStats6_reduced_in_in(organism,
								csvBiodeList, returnSize), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// nothing to do !
		}
	}

	/**
	 * for testing... gets the inverted index data for set of biodes
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @return
	 * @throws JSONException
	 */
	private String testGettingInvertedIndexData(String NCBI_organism,
			String csvBiodeList) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);
		js.key("trStats").array();

		String mySqlList = csvToMySqlList(csvBiodeList);

		String invIndexTableName = invIndexTableNameHash.get(NCBI_organism)
				+ "_split_link";

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT element1,element2,tracks FROM `"
						+ invIndexTableName + "` AS t WHERE ((t.element1 IN ("
						+ mySqlList + ") OR t.element2 IN (" + mySqlList
						+ ")) AND (NOT t.element1=t.element2))";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				// from the inverted index, get the tracks field for links
				while (rs.next()) {
					// js.object();
					// js.key("element1").value(rs.getString("element1"));
					// js.key("element2").value(rs.getString("element2"));
					// js.key("tracks").value(rs.getString("tracks"));
					// js.endObject();
				}

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.endArray();
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * From a comma-separated list of biodes, generate a String of links that
	 * can be used in MySQL list.
	 * 
	 * @param csvBiodeList
	 * @return
	 */
	private String csvToMysqlListOfLinks(String csvBiodeList) {
		String[] queryBiodesArray = csvBiodeList.split(",");
		Arrays.sort(csvBiodeList.split(","));
		ArrayList<String> queryLinks = new ArrayList<String>();

		for (int i = 0; i < queryBiodesArray.length; i++) {
			for (int j = i + 1; j < queryBiodesArray.length; j++) {
				queryLinks.add(queryBiodesArray[i] + "|" + queryBiodesArray[j]);
			}
		}

		Object[] links = queryLinks.toArray();

		StringBuffer strBuf = new StringBuffer();
		for (int i = 0; i < links.length; i++) {
			strBuf.append((String) links[i] + ",");
		}

		return csvToMySqlList(strBuf.toString());
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses inverted
	 * index with the fields, "element1" "element2". For this one, a relevant
	 * link is where AT LEAST ONE of the elements is in the query.
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats_gene_count(String NCBI_organism,
			String csvBiodeList) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);

		String mySqlList = csvToMySqlList(csvBiodeList);

		String invIndexTableName = invIndexTableNameHash.get(NCBI_organism)
				+ "_split_link";

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT element1,element2,tracks FROM `"
						+ invIndexTableName + "` AS t WHERE (t.element1 IN ("
						+ mySqlList + ") OR t.element2 IN (" + mySqlList
						+ ") AND NOT t.element1=t.element2)";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				HashMap<String, Integer> queryCountsHash = new HashMap<String, Integer>();

				String[] trackNameArrayForLink;

				// from the inverted index, get the tracks field for the links
				while (rs.next()) {
					trackNameArrayForLink = rs.getString("tracks").split(" ");
					for (String trackName : trackNameArrayForLink) {
						if (queryCountsHash.containsKey(trackName)) {
							// key exists
							queryCountsHash.put(trackName,
									queryCountsHash.get(trackName) + 1);
						} else {
							// new key initialize it to 1 count
							queryCountsHash.put(trackName, 1);
						}
					}
				}

				// TODO count up results for each track
				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();

				for (String trackName : queryCountsHash.keySet()) {
					JSONObject jo = new JSONObject();

					jo.put("name", trackName);
					int count = queryCountsHash.get(trackName);
					jo.put("matches", count);

					joList.add(jo);
				}

				// sort the results by number of matches
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"matches")));

				// write the results to the JSONStringer
				js.key("tracks").array();
				for (int i = 0; i < joList.size(); i++) {
					js.value(joList.get(i));
				}
				js.endArray(); // ends the tracks JSONArray

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses inverted
	 * index with the fields, "element1" "element2". This one counts the links.
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats_link_count(String NCBI_organism,
			String csvBiodeList) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);

		String mySqlList = csvToMySqlList(csvBiodeList);

		String invIndexTableName = invIndexTableNameHash.get(NCBI_organism)
				+ "_split_link";

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT element1,element2,tracks FROM `"
						+ invIndexTableName + "` AS t WHERE (t.element1 IN ("
						+ mySqlList + ") AND t.element2 IN (" + mySqlList
						+ ") AND NOT t.element1=t.element2)";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				HashMap<String, Integer> queryCountsHash = new HashMap<String, Integer>();

				String[] trackNameArrayForLink;

				// from the inverted index, get the tracks field for the links
				while (rs.next()) {
					trackNameArrayForLink = rs.getString("tracks").split(" ");
					for (String trackName : trackNameArrayForLink) {
						if (queryCountsHash.containsKey(trackName)) {
							// key exists
							queryCountsHash.put(trackName,
									queryCountsHash.get(trackName) + 1);
						} else {
							// new key initialize it to 1 count
							queryCountsHash.put(trackName, 1);
						}
					}
				}

				// TODO count up results for each track
				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();

				for (String trackName : queryCountsHash.keySet()) {
					JSONObject jo = new JSONObject();

					jo.put("name", trackName);
					int count = queryCountsHash.get(trackName);
					jo.put("matches", count);

					joList.add(jo);
				}

				// sort the results by number of matches
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"matches")));

				// write the results to the JSONStringer
				js.key("tracks").array();
				for (int i = 0; i < joList.size(); i++) {
					js.value(joList.get(i));
				}
				js.endArray(); // ends the tracks JSONArray

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses inverted
	 * index with the field, "link". This was the first version of a
	 * Hypergeometric Track Recommender. This version is not the favored version
	 * because it is slow. We've moved to a new database table that improves
	 * speed of queries.
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats1(String NCBI_organism,
			String csvBiodeList) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);
		js.key("trStats").array();

		String mySqlList = csvToMysqlListOfLinks(csvBiodeList);

		int numberOfDraws = crossNumberAnyOrder(csvBiodeList.split(",").length);

		String invIndexTableName = invIndexTableNameHash.get(NCBI_organism);

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT link,tracks FROM `"
						+ invIndexTableName + "` AS t WHERE link IN ("
						+ mySqlList + ")";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				HashMap<String, Integer> queryCountsHash = new HashMap<String, Integer>();

				String[] trackNameArrayForLink;

				// from the inverted index, get the tracks field for the links
				while (rs.next()) {
					trackNameArrayForLink = rs.getString("tracks").split(" ");
					for (String trackName : trackNameArrayForLink) {
						if (queryCountsHash.containsKey(trackName)) {
							// key exists
							queryCountsHash.put(trackName,
									queryCountsHash.get(trackName) + 1);
						} else {
							// new key initialize it to 1 count
							queryCountsHash.put(trackName, 1);
						}
					}
				}

				// calculate the TR stats for each track
				String mysqlTrackList = csvToMySqlList(arrayToCommaSeparatedString(queryCountsHash
						.keySet().toArray()));

				sql = "SELECT DISTINCT name,num_links,num_elements FROM tracklist AS t WHERE name IN ("
						+ mysqlTrackList + ")";

				s = con.createStatement();
				s.execute(sql);
				rs = s.getResultSet();

				while (rs.next()) {
					js.object();
					js.key("name").value(rs.getString("name"));
					int possibleSuccesses = rs.getInt("num_links");
					js.key("pos_success").value(possibleSuccesses);

					int universeCount = crossNumberAnyOrder(rs
							.getInt("num_elements"));
					js.key("universe").value(universeCount);

					int querySuccesses = queryCountsHash.get(rs
							.getString("name"));
					js.key("matches").value(querySuccesses);

					int draws = numberOfDraws;
					js.key("draws").value(draws);

					double hgScore = computeHyperPValue(querySuccesses, draws,
							possibleSuccesses, universeCount);
					js.key("hgPval").value(hgScore);

					js.endObject();
				}

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.endArray();
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses inverted
	 * index with the fields, "element1" "element2". This one is the
	 * Hypergeometric Track Recommender.
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats2(String NCBI_organism,
			String csvBiodeList) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);

		String mySqlList = csvToMySqlList(csvBiodeList);

		int numberOfDraws = crossNumberAnyOrder(csvBiodeList.split(",").length);

		String invIndexTableName = invIndexTableNameHash.get(NCBI_organism)
				+ "_split_link";

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT element1,element2,tracks FROM `"
						+ invIndexTableName + "` AS t WHERE (t.element1 IN ("
						+ mySqlList + ") AND t.element2 IN (" + mySqlList
						+ ") AND NOT t.element1=t.element2)";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				HashMap<String, Integer> queryCountsHash = new HashMap<String, Integer>();

				String[] trackNameArrayForLink;

				// from the inverted index, get the tracks field for the links
				while (rs.next()) {
					trackNameArrayForLink = rs.getString("tracks").split(" ");
					for (String trackName : trackNameArrayForLink) {
						if (queryCountsHash.containsKey(trackName)) {
							// key exists
							queryCountsHash.put(trackName,
									queryCountsHash.get(trackName) + 1);
						} else {
							// new key initialize it to 1 count
							queryCountsHash.put(trackName, 1);
						}
					}
				}

				// calculate the TR stats for each track
				String mysqlTrackList = csvToMySqlList(arrayToCommaSeparatedString(queryCountsHash
						.keySet().toArray()));

				sql = "SELECT DISTINCT name,num_links,num_elements FROM tracklist AS t WHERE name IN ("
						+ mysqlTrackList + ")";

				s = con.createStatement();
				s.execute(sql);
				rs = s.getResultSet();

				// get the results from ResultSet
				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();

				while (rs.next()) {
					JSONObject jo = new JSONObject();

					jo.put("name", rs.getString("name"));

					int possibleSuccesses = rs.getInt("num_links");
					jo.put("pos_success", possibleSuccesses);

					int universeCount = crossNumberAnyOrder(rs
							.getInt("num_elements"));
					jo.put("universe", universeCount);

					int querySuccesses = queryCountsHash.get(rs
							.getString("name"));
					jo.put("matches", querySuccesses);

					int draws = numberOfDraws;
					jo.put("draws", draws);

					double hgScore = computeHyperPValue(querySuccesses, draws,
							possibleSuccesses, universeCount);
					jo.put("hgPval", hgScore);

					joList.add(jo);
				}

				// sort the results by trScore
				Collections.sort(joList, new DatabaseService.JSONNumberComparator(
								"hgPval"));

				// write the results to the JSONStringer
				js.key("tracks").array();
				for (int i = 0; i < joList.size(); i++) {
					js.value(joList.get(i));
				}
				js.endArray(); // ends the tracks JSONArray

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses inverted
	 * index with the fields, "element1" "element2". This one is the Binomial
	 * Enhancement Track Recommender. --- INCOMPLETE, ABANDON IDEA ---
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats3(String NCBI_organism,
			String csvBiodeList) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);
		js.key("trStats").array();

		String mySqlList = csvToMySqlList(csvBiodeList);

		String invIndexTableName = invIndexTableNameHash.get(NCBI_organism)
				+ "_split_link";

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				// get the counts for links in the inverted index that have at
				// least one element in the query list of biodes
				String sql = "SELECT DISTINCT element1,element2,tracks FROM `"
						+ invIndexTableName + "` AS t WHERE ((t.element1 IN ("
						+ mySqlList + ") OR t.element2 IN (" + mySqlList
						+ ")) AND (NOT t.element1=t.element2))";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				HashMap<String, Integer> universeCountsHash = new HashMap<String, Integer>();

				String[] trackNameArrayForLink;

				while (rs.next()) {
					trackNameArrayForLink = rs.getString("tracks").split(" ");
					for (String trackName : trackNameArrayForLink) {
						if (universeCountsHash.containsKey(trackName)) {
							// key exists
							universeCountsHash.put(trackName,
									universeCountsHash.get(trackName) + 1);
						} else {
							// new key initialize it to 1 count
							universeCountsHash.put(trackName, 1);
						}
					}
				}

				// get the counts for links in the inverted index that are
				// fully within the query set of biodes
				sql = "SELECT DISTINCT element1,element2,tracks FROM `"
						+ invIndexTableName + "` AS t WHERE (t.element1 IN ("
						+ mySqlList + ") AND t.element2 IN (" + mySqlList
						+ ") AND NOT t.element1=t.element2)";

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();

				HashMap<String, Integer> successCountsHash = new HashMap<String, Integer>();

				while (rs.next()) {
					trackNameArrayForLink = rs.getString("tracks").split(" ");
					for (String trackName : trackNameArrayForLink) {
						if (successCountsHash.containsKey(trackName)) {
							// key exists
							successCountsHash.put(trackName,
									successCountsHash.get(trackName) + 1);
						} else {
							// new key initialize it to 1 count
							successCountsHash.put(trackName, 1);
						}
					}
				}

				// TODO calculate Binomial TR stats for each track in this
				// query's universe
				int numberOfDraws = crossNumberAnyOrder(csvBiodeList.split(",").length);

				for (String trackName : successCountsHash.keySet()) {
					js.object();

					js.key("name").value(trackName);
					js.key("draws").value(numberOfDraws);
					js.key("successes").value(successCountsHash.get(trackName));
					js.key("universe").value(universeCountsHash.get(trackName));

					js.endObject();
				}

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.endArray();
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses modes
	 * clusters to make recommendations. Returns a stringified JSON object that
	 * is similar to the one returned by TrackDbService.getTrackList(), except
	 * that this one has a new field, trScore, for each track.
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats4(String NCBI_organism,
			String csvBiodeList) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);

		String mySqlList = csvToMySqlList(csvBiodeList);

		int querySize = csvBiodeList.split(",").length;

		String modesTableName = modesTableNameHash.get(NCBI_organism);

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				// get list of modules in which query set is involved
				String sql = "SELECT trackName,clusterID FROM `"
						+ modesTableName + "` AS t WHERE t.element IN ("
						+ mySqlList + ")";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				// count up matches for each track/module combination
				HashMap<String, HashMap<String, Integer>> trackHash = new HashMap<String, HashMap<String, Integer>>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");

					if (!trackHash.containsKey(track)) {
						trackHash.put(track, new HashMap<String, Integer>());
					}

					if (trackHash.get(track).get(clusterID) == null) {
						trackHash.get(track).put(clusterID, 1);
					} else {
						int count = trackHash.get(track).get(clusterID) + 1;
						trackHash.get(track).put(clusterID, count);
					}
				}

				// build up sql statement to get cluster sizes
				// This statement can get very long... the query is sped up if
				// index on trackName and clusterID columns.
				StringBuffer sb = new StringBuffer();
				for (String track : trackHash.keySet()) {
					for (String cluster : trackHash.get(track).keySet()) {

						// only if intersection is more than 1
						if (trackHash.get(track).get(cluster) > 1) {
							if (sb.toString().isEmpty()) {
							} else {
								sb.append(" OR ");
							}
							sb.append("(m.trackName='" + track
									+ "' AND m.clusterID='" + cluster + "')");
						}
					}
				}
				sql = "SELECT trackName,clusterID,cluster_size FROM `modeslist` AS m WHERE "
						+ sb.toString();

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();

				// calculate the scores ... keeping the top one for each track
				HashMap<String, Float> scoreHash = new HashMap<String, Float>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					// int clusterSize = rs.getInt("cluster_size");

					int intersection = trackHash.get(track).get(clusterID);

					int union = querySize + rs.getInt("cluster_size")
							- intersection;

					float score = new Float(intersection) / new Float(union);

					if (!scoreHash.containsKey(track)) {
						scoreHash.put(track, score);
					} else {
						if (scoreHash.get(track) < score) {
							scoreHash.put(track, score);
						}
					}
				}

				// get the track info to send back to client
				sb = new StringBuffer();
				for (String name : scoreHash.keySet()) {
					if (sb.toString().isEmpty()) {
					} else {
						sb.append(",");
					}
					sb.append(name);
				}

				sql = "SELECT DISTINCT * FROM tracklist AS t WHERE t.name IN ("
						+ csvToMySqlList(sb.toString())
						+ ") ORDER BY t.datatype,t.name ASC";

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();
				ResultSetMetaData metaData = rs.getMetaData();
				String colname;

				// get all columns selected + get the score
				js.key("tracks").array();

				String curDatatype = "";
				while (rs.next()) {
					if (!curDatatype.equalsIgnoreCase(rs.getString("datatype"))) {
						if (!curDatatype.equalsIgnoreCase("")) {
							js.endArray().endObject();
						}
						curDatatype = rs.getString("datatype");
						js.object().key("datatype").value(curDatatype);
						js.key("categoryArray").array();
					}

					js.object();
					// get all columns selected
					for (int i = 1; i <= metaData.getColumnCount(); i++) {
						colname = metaData.getColumnName(i);
						js.key(colname).value(rs.getString(colname));
					}
					js.key("trScore")
							.value(scoreHash.get(rs.getString("name")));

					js.endObject();
				}

				if (curDatatype != "") {
					js.endArray().endObject();
				}

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.endArray(); // ends the tracks JSONArray
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses modes
	 * clusters to make recommendations based on a score calculated from module
	 * overlap. The results are sorted by trScore and returned in a JSONArray
	 * called "tracks".
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats5_full(String NCBI_organism,
			String csvBiodeList) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);

		String mySqlList = csvToMySqlList(super.sanitizeString(csvBiodeList));

		int querySize = csvBiodeList.split(",").length;

		String modesTableName = modesTableNameHash.get(NCBI_organism);

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				// get list of modules in which query set is involved

				String sql = "SELECT y.trackName,y.clusterID,count(*) FROM `"
						+ modesTableName + "` AS y WHERE y.element IN ("
						+ mySqlList + ") GROUP BY y.trackName,y.clusterID";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				// check the time to this point
				js.key("time1").value(((new Date()).getTime() - start));

				// count up matches for each track/module combination
				HashMap<String, HashMap<String, Integer>> trackHash = new HashMap<String, HashMap<String, Integer>>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					int intersection = rs.getInt("count(*)");

					// encountered a new track
					if (!trackHash.containsKey(track)) {
						trackHash.put(track, new HashMap<String, Integer>());
					}

					trackHash.get(track).put(clusterID, intersection);
				}

				// in case no results
				if (trackHash.size() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				// build up sql statement to get cluster sizes
				// This statement can get very long... the query is sped up if
				// index on trackName and clusterID columns.
				StringBuffer sb = new StringBuffer();
				for (String track : trackHash.keySet()) {
					for (String cluster : trackHash.get(track).keySet()) {

						// only if intersection is more than 1
						if (trackHash.get(track).get(cluster) > 1) {
							if (sb.toString().isEmpty()) {
							} else {
								sb.append(" OR ");
							}
							sb.append("(m.trackName='" + track
									+ "' AND m.clusterID='" + cluster + "')");
						}
					}
				}

				// in case no results
				if (sb.toString().length() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				sql = "SELECT trackName,clusterID,cluster_size FROM `modeslist` AS m WHERE "
						+ sb.toString();

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();

				// calculate the scores ... keeping the top one for each track
				HashMap<String, Float> scoreHash = new HashMap<String, Float>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					// int clusterSize = rs.getInt("cluster_size");

					int intersection = trackHash.get(track).get(clusterID);

					int union = querySize + rs.getInt("cluster_size")
							- intersection;

					float score = new Float(intersection) / new Float(union);

					if (!scoreHash.containsKey(track)) {
						scoreHash.put(track, score);
					} else {
						if (scoreHash.get(track) < score) {
							scoreHash.put(track, score);
						}
					}
				}

				// get the track info to send back to client
				sb = new StringBuffer();
				for (String name : scoreHash.keySet()) {
					if (sb.toString().isEmpty()) {
					} else {
						sb.append(",");
					}
					sb.append(name);
				}

				sql = "SELECT DISTINCT * FROM tracklist AS t WHERE t.name IN ("
						+ csvToMySqlList(sb.toString())
						+ ") ORDER BY t.datatype,t.name ASC";

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();
				ResultSetMetaData metaData = rs.getMetaData();
				String colname;

				// get all columns selected & get the score

				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();
				while (rs.next()) {

					// get all columns selected
					JSONObject jo = new JSONObject();
					for (int i = 1; i <= metaData.getColumnCount(); i++) {
						colname = metaData.getColumnName(i);
						jo.put(colname, rs.getString(colname));
					}
					jo.put("trScore", scoreHash.get(rs.getString("name")));

					joList.add(jo);
				}

				// sort the results by trScore
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"trScore")));

				// write the results to the JSONStringer
				js.key("tracks").array();
				for (int i = 0; i < joList.size(); i++) {
					js.value(joList.get(i));
				}
				js.endArray(); // ends the tracks JSONArray

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses modes
	 * clusters to make recommendations based on a score calculated from module
	 * overlap. The results are sorted by trScore and returned in a JSONArray
	 * called "tracks". This is a reduced version for testing... doesn't do the
	 * final lookup of tracklist info.
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats5(String NCBI_organism,
			String csvBiodeList) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);

		String mySqlList = csvToMySqlList(super.sanitizeString(csvBiodeList));

		int querySize = csvBiodeList.split(",").length;

		String modesTableName = modesTableNameHash.get(NCBI_organism);

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				// get list of modules in which query set is involved

				String sql = "SELECT y.trackName,y.clusterID,count(*) FROM `"
						+ modesTableName + "` AS y WHERE y.element IN ("
						+ mySqlList + ") GROUP BY y.trackName,y.clusterID";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				// check the time to this point
				js.key("time1").value(((new Date()).getTime() - start));

				// count up matches for each track/module combination
				HashMap<String, HashMap<String, Integer>> trackHash = new HashMap<String, HashMap<String, Integer>>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					int intersection = rs.getInt("count(*)");

					// encountered a new track
					if (!trackHash.containsKey(track)) {
						trackHash.put(track, new HashMap<String, Integer>());
					}

					trackHash.get(track).put(clusterID, intersection);
				}

				// in case no results
				if (trackHash.size() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				// build up sql statement to get cluster sizes
				// This statement can get very long... the query is sped up if
				// index on trackName and clusterID columns.
				StringBuffer sb = new StringBuffer();
				for (String track : trackHash.keySet()) {
					for (String cluster : trackHash.get(track).keySet()) {

						// only if intersection is more than 1
						if (trackHash.get(track).get(cluster) > 1) {
							if (sb.toString().isEmpty()) {
							} else {
								sb.append(" OR ");
							}
							sb.append("(m.trackName='" + track
									+ "' AND m.clusterID='" + cluster + "')");
						}
					}
				}

				// in case no results
				if (sb.toString().length() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				sql = "SELECT trackName,clusterID,cluster_size FROM `modeslist` AS m WHERE "
						+ sb.toString();

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();

				// calculate the scores ... keeping the top one for each track
				HashMap<String, Float> scoreHash = new HashMap<String, Float>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					// int clusterSize = rs.getInt("cluster_size");

					int intersection = trackHash.get(track).get(clusterID);

					int union = querySize + rs.getInt("cluster_size")
							- intersection;

					float score = new Float(intersection) / new Float(union);

					if (!scoreHash.containsKey(track)) {
						scoreHash.put(track, score);
					} else {
						if (scoreHash.get(track) < score) {
							scoreHash.put(track, score);
						}
					}
				}

				// get the score for each track
				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();
				for (String trackName : scoreHash.keySet()) {
					JSONObject jo = new JSONObject();
					jo.put("name", trackName);
					jo.put("trScore", scoreHash.get(trackName));
					joList.add(jo);
				}

				// sort the results by trScore
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"trScore")));

				// write the results to the JSONStringer
				js.key("tracks").array();
				for (int i = 0; i < joList.size(); i++) {
					JSONObject jo = joList.get(i);
					if (i == 0) {

						// create temp tables
						String tempQueryTableName = "tempQueryElements";
						String tempQueryTableName2 = "tempQueryElements2";

						// drop table if it exists
						sql = "DROP TABLE IF EXISTS `" + tempQueryTableName;
						s = con.createStatement();
						s.execute(sql);

						sql = "DROP TABLE IF EXISTS `" + tempQueryTableName2;
						s = con.createStatement();
						s.execute(sql);

						// create temp table
						sql = "CREATE TEMPORARY TABLE `"
								+ tempQueryTableName
								+ "` ( `element` varchar(50) NOT NULL, PRIMARY KEY (`element`))";

						s = con.createStatement();
						s.execute(sql);

						// INSERT rows into temp table

						sql = "INSERT INTO `" + tempQueryTableName
								+ "` VALUES (" + mySqlList.replace(",", "),(")
								+ ")";

						s = con.createStatement();
						s.execute(sql);

						// clone the tempQueryTable
						// CREATE TABLE recipes_new LIKE production.recipes;
						sql = "CREATE TEMPORARY TABLE `" + tempQueryTableName2
								+ "` LIKE `" + tempQueryTableName + "`";

						s = con.createStatement();
						s.execute(sql);

						// INSERT recipes_new SELECT * FROM production.recipes;
						sql = "INSERT `" + tempQueryTableName2
								+ "` SELECT * FROM `" + tempQueryTableName
								+ "`";

						s = con.createStatement();
						s.execute(sql);

						// TODO join!

						sql = "SELECT count(*) FROM (SELECT t.element1,t.element2 FROM `"
								+ jo.getString("name")
								+ "` AS t INNER JOIN `"
								+ tempQueryTableName2
								+ "` q2 ON q2.element=t.element1) AS t INNER JOIN `"
								+ tempQueryTableName
								+ "` q1 ON q1.element=t.element2";

						s = con.createStatement();
						s.execute(sql);
						rs = s.getResultSet();
						rs.next();
						String linkCount = rs.getString("count(*)");

						// record linkCount
						jo.put("linkCount", Float.parseFloat(linkCount));

						// DROP temporary tables
						sql = "DROP TABLE `" + tempQueryTableName + "`";
						s = con.createStatement();
						s.execute(sql);

						sql = "DROP TABLE `" + tempQueryTableName2 + "`";
						s = con.createStatement();
						s.execute(sql);
					}
					js.value(jo);
				}
				js.endArray(); // ends the tracks JSONArray

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses modes
	 * clusters to make recommendations based on a score calculated from module
	 * overlap. Additionally, the 20 top-scoring tracks will be scored again
	 * based on link count. The results are sorted by trScore and returned in a
	 * JSONArray called "tracks".
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @param returnSize
	 *            specify the number of tracks in result
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats6(String NCBI_organism,
			String csvBiodeList, int returnSize) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);

		String mySqlList = csvToMySqlList(super.sanitizeString(csvBiodeList));

		int querySize = csvBiodeList.split(",").length;

		String modesTableName = modesTableNameHash.get(NCBI_organism);

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				// get list of modules in which query set is involved

				String sql = "SELECT y.trackName,y.clusterID,count(*) FROM `"
						+ modesTableName + "` AS y WHERE y.element IN ("
						+ mySqlList + ") GROUP BY y.trackName,y.clusterID";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				// count up matches for each track/module combination
				HashMap<String, HashMap<String, Integer>> trackHash = new HashMap<String, HashMap<String, Integer>>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					int intersection = rs.getInt("count(*)");

					// encountered a new track
					if (!trackHash.containsKey(track)) {
						trackHash.put(track, new HashMap<String, Integer>());
					}

					trackHash.get(track).put(clusterID, intersection);
				}

				// in case no results
				if (trackHash.size() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				// build up sql statement to get cluster sizes
				// This statement can get very long... the query is sped up if
				// index on trackName and clusterID columns.
				StringBuffer sb = new StringBuffer();
				for (String track : trackHash.keySet()) {
					for (String cluster : trackHash.get(track).keySet()) {

						// only if intersection is more than 1
						if (trackHash.get(track).get(cluster) > 1) {
							if (sb.toString().isEmpty()) {
							} else {
								sb.append(" OR ");
							}
							sb.append("(trackName='" + track
									+ "' AND clusterID='" + cluster + "')");
						}
					}
				}

				// in case no results
				if (sb.toString().length() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				sql = "SELECT trackName,clusterID,cluster_size FROM `modeslist` WHERE "
						+ sb.toString();

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();

				// calculate the scores ... keeping the top one for each track
				HashMap<String, Float> scoreHash = new HashMap<String, Float>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					// int clusterSize = rs.getInt("cluster_size");

					int intersection = trackHash.get(track).get(clusterID);

					int union = querySize + rs.getInt("cluster_size")
							- intersection;

					float score = new Float(intersection) / new Float(union);

					if (!scoreHash.containsKey(track)) {
						scoreHash.put(track, score);
					} else {
						if (scoreHash.get(track) < score) {
							scoreHash.put(track, score);
						}
					}
				}

				// get the track info to send back to client with the trScore
				sb = new StringBuffer();
				for (String name : scoreHash.keySet()) {
					if (sb.toString().isEmpty()) {
					} else {
						sb.append(",");
					}
					sb.append(name);
				}

				sql = "SELECT DISTINCT * FROM tracklist AS t WHERE t.name IN ("
						+ csvToMySqlList(sb.toString()) + ")";

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();
				ResultSetMetaData metaData = rs.getMetaData();
				String colname;

				// get all columns selected & get the score
				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();
				while (rs.next()) {

					// get all columns selected
					JSONObject jo = new JSONObject();
					for (int i = 1; i <= metaData.getColumnCount(); i++) {
						colname = metaData.getColumnName(i);
						jo.put(colname, rs.getString(colname));
					}
					jo.put("trScore", scoreHash.get(rs.getString("name")));

					joList.add(jo);
				}

				// sort the results by trScore
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"trScore")));

				// get the link counts for top tracks
				while (joList.size() > returnSize) {
					joList.remove(joList.size() - 1);
				}

				// create temp tables
				String tempLinkTableName = "tempLinks";

				// TODO join!
				for (Iterator<JSONObject> iter = joList.iterator(); iter
						.hasNext();) {
					// drop table if it exists
					sql = "DROP TABLE IF EXISTS `" + tempLinkTableName;
					s = con.createStatement();
					s.execute(sql);

					// join on element1
					JSONObject jo = iter.next();
					sql = "CREATE TEMPORARY TABLE `" + tempLinkTableName
							+ "` SELECT t.element1,t.element2 FROM `"
							+ jo.getString("name")
							+ "` AS t WHERE t.element1 in (" + mySqlList + ")";
					s = con.createStatement();
					s.execute(sql);

					// WHERE on element2
					sql = "SELECT count(*) FROM `" + tempLinkTableName
							+ "` AS t WHERE t.element2 in (" + mySqlList + ")";
					s = con.createStatement();
					s.execute(sql);
					rs = s.getResultSet();
					rs.next();
					String linkCount = rs.getString("count(*)");

					// record linkCount
					jo.put("linkCount", Float.parseFloat(linkCount));
				}

				// DROP temporary tables
				sql = "DROP TABLE `" + tempLinkTableName + "`";
				s = con.createStatement();
				s.execute(sql);

				// sort by linkCount
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"linkCount")));

				// write the results to the JSONStringer
				js.key("tracks").array();
				for (int i = 0; i < joList.size(); i++) {
					js.value(joList.get(i));
				}
				js.endArray(); // ends the tracks JSONArray

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses modes
	 * clusters to make recommendations based on a score calculated from module
	 * overlap. Additionally, the 20 top-scoring tracks will be scored again
	 * based on link count. The results are sorted by trScore and returned in a
	 * JSONArray called "tracks". This is a reduced version for testing...
	 * doesn't do the final lookup of tracklist info.
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @param returnSize
	 *            specify the number of tracks in result
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats6_reduced(String NCBI_organism,
			String csvBiodeList, int returnSize) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);

		String mySqlList = csvToMySqlList(super.sanitizeString(csvBiodeList));

		int querySize = csvBiodeList.split(",").length;

		String modesTableName = modesTableNameHash.get(NCBI_organism);

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				// get list of modules in which query set is involved

				String sql = "SELECT y.trackName,y.clusterID,count(*) FROM `"
						+ modesTableName + "` AS y WHERE y.element IN ("
						+ mySqlList + ") GROUP BY y.trackName,y.clusterID";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				// check the time to this point
				js.key("time1").value(((new Date()).getTime() - start));

				// count up matches for each track/module combination
				HashMap<String, HashMap<String, Integer>> trackHash = new HashMap<String, HashMap<String, Integer>>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					int intersection = rs.getInt("count(*)");

					// encountered a new track
					if (!trackHash.containsKey(track)) {
						trackHash.put(track, new HashMap<String, Integer>());
					}

					trackHash.get(track).put(clusterID, intersection);
				}

				// in case no results
				if (trackHash.size() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				// build up sql statement to get cluster sizes
				// This statement can get very long... the query is sped up if
				// index on trackName and clusterID columns.
				StringBuffer sb = new StringBuffer();
				for (String track : trackHash.keySet()) {
					for (String cluster : trackHash.get(track).keySet()) {

						// only if intersection is more than 1
						if (trackHash.get(track).get(cluster) > 1) {
							if (sb.toString().isEmpty()) {
							} else {
								sb.append(" OR ");
							}
							sb.append("(m.trackName='" + track
									+ "' AND m.clusterID='" + cluster + "')");
						}
					}
				}

				// in case no results
				if (sb.toString().length() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				// check the time to this point
				js.key("time2").value(((new Date()).getTime() - start));

				sql = "SELECT trackName,clusterID,cluster_size FROM `modeslist` AS m WHERE "
						+ sb.toString();

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();

				// check the time to this point
				js.key("time3").value(((new Date()).getTime() - start));

				// calculate the scores ... keeping the top one for each track
				HashMap<String, Float> scoreHash = new HashMap<String, Float>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					// int clusterSize = rs.getInt("cluster_size");

					int intersection = trackHash.get(track).get(clusterID);

					int union = querySize + rs.getInt("cluster_size")
							- intersection;

					float score = new Float(intersection) / new Float(union);

					if (!scoreHash.containsKey(track)) {
						scoreHash.put(track, score);
					} else {
						if (scoreHash.get(track) < score) {
							scoreHash.put(track, score);
						}
					}
				}

				// check the time to this point
				js.key("time4").value(((new Date()).getTime() - start));

				// get the score
				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();
				for (String trackName : scoreHash.keySet()) {
					JSONObject jo = new JSONObject();
					jo.put("name", trackName);
					jo.put("trScore", scoreHash.get(trackName));

					joList.add(jo);
				}

				// check the time to this point
				js.key("time5").value(((new Date()).getTime() - start));

				// sort the results by trScore
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"trScore")));

				// check the time to this point
				js.key("time6").value(((new Date()).getTime() - start));

				// get the link counts for top tracks
				// remove the extra tracks
				while (joList.size() > returnSize) {
					joList.remove(joList.size() - 1);
				}

				// check the time to this point
				js.key("time7").value(((new Date()).getTime() - start));

				// create temp tables
				String tempLinkTableName = "tempLinks";

				// TODO join!
				for (Iterator<JSONObject> iter = joList.iterator(); iter
						.hasNext();) {
					// drop table if it exists
					sql = "DROP TABLE IF EXISTS `" + tempLinkTableName;
					s = con.createStatement();
					s.execute(sql);

					// SELECT WHERE on element1
					JSONObject jo = iter.next();
					sql = "CREATE TEMPORARY TABLE `" + tempLinkTableName
							+ "` SELECT t.element1,t.element2 FROM `"
							+ jo.getString("name")
							+ "` AS t WHERE t.element1 in (" + mySqlList + ")";
					s = con.createStatement();
					s.execute(sql);

					// SELECT WHERE on element2
					sql = "SELECT count(*) FROM `" + tempLinkTableName
							+ "` AS t WHERE t.element2 in (" + mySqlList + ")";
					s = con.createStatement();
					s.execute(sql);
					rs = s.getResultSet();
					rs.next();
					String linkCount = rs.getString("count(*)");

					// record linkCount
					jo.put("linkCount", Float.parseFloat(linkCount));
				}

				// DROP temporary tables
				sql = "DROP TABLE `" + tempLinkTableName + "`";
				s = con.createStatement();
				s.execute(sql);

				// check the time to this point
				js.key("time8").value(((new Date()).getTime() - start));

				// sort by linkCount
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"linkCount")));

				// check the time to this point
				js.key("time9").value(((new Date()).getTime() - start));

				// write the results to the JSONStringer
				js.key("tracks").array();
				for (int i = 0; i < joList.size(); i++) {
					js.value(joList.get(i));
				}
				js.endArray(); // ends the tracks JSONArray

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses modes
	 * clusters to make recommendations based on a score calculated from module
	 * overlap. Additionally, the 20 top-scoring tracks will be scored again
	 * based on link count. The results are sorted by trScore and returned in a
	 * JSONArray called "tracks". This is a reduced version for testing...
	 * doesn't do the final lookup of tracklist info.
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @param returnSize
	 *            specify the number of tracks in result
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats6_reduced_2_joins_1st(
			String NCBI_organism, String csvBiodeList, int returnSize)
			throws JSONException {

		// TODO

		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);

		String mySqlList = csvToMySqlList(super.sanitizeString(csvBiodeList));

		int querySize = csvBiodeList.split(",").length;

		String modesTableName = modesTableNameHash.get(NCBI_organism);

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				// get list of modules in which query set is involved

				String sql = "SELECT y.trackName,y.clusterID,count(*) FROM `"
						+ modesTableName + "` AS y WHERE y.element IN ("
						+ mySqlList + ") GROUP BY y.trackName,y.clusterID";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				// check the time to this point
				js.key("time1").value(((new Date()).getTime() - start));

				// count up matches for each track/module combination
				HashMap<String, HashMap<String, Integer>> trackHash = new HashMap<String, HashMap<String, Integer>>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					int intersection = rs.getInt("count(*)");

					// encountered a new track
					if (!trackHash.containsKey(track)) {
						trackHash.put(track, new HashMap<String, Integer>());
					}

					trackHash.get(track).put(clusterID, intersection);
				}

				// in case no results
				if (trackHash.size() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				// build up sql statement to get cluster sizes
				// This statement can get very long... the query is sped up if
				// index on trackName and clusterID columns.
				StringBuffer sb = new StringBuffer();
				for (String track : trackHash.keySet()) {
					for (String cluster : trackHash.get(track).keySet()) {

						// only if intersection is more than 1
						if (trackHash.get(track).get(cluster) > 1) {
							if (sb.toString().isEmpty()) {
							} else {
								sb.append(" OR ");
							}
							sb.append("(m.trackName='" + track
									+ "' AND m.clusterID='" + cluster + "')");
						}
					}
				}

				// in case no results
				if (sb.toString().length() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				// check the time to this point
				js.key("time2").value(((new Date()).getTime() - start));

				sql = "SELECT trackName,clusterID,cluster_size FROM `modeslist` AS m WHERE "
						+ sb.toString();

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();

				// check the time to this point
				js.key("time3").value(((new Date()).getTime() - start));

				// calculate the scores ... keeping the top one for each track
				HashMap<String, Float> scoreHash = new HashMap<String, Float>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					// int clusterSize = rs.getInt("cluster_size");

					int intersection = trackHash.get(track).get(clusterID);

					int union = querySize + rs.getInt("cluster_size")
							- intersection;

					float score = new Float(intersection) / new Float(union);

					if (!scoreHash.containsKey(track)) {
						scoreHash.put(track, score);
					} else {
						if (scoreHash.get(track) < score) {
							scoreHash.put(track, score);
						}
					}
				}

				// check the time to this point
				js.key("time4").value(((new Date()).getTime() - start));

				// get the score
				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();
				for (String trackName : scoreHash.keySet()) {
					JSONObject jo = new JSONObject();
					jo.put("name", trackName);
					jo.put("trScore", scoreHash.get(trackName));

					joList.add(jo);
				}

				// check the time to this point
				js.key("time5").value(((new Date()).getTime() - start));

				// sort the results by trScore
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"trScore")));

				// check the time to this point
				js.key("time6").value(((new Date()).getTime() - start));

				// get the link counts for top tracks
				// remove the extra tracks
				while (joList.size() > returnSize) {
					joList.remove(joList.size() - 1);
				}

				// check the time to this point
				js.key("time7").value(((new Date()).getTime() - start));

				// create temp tables
				String tempQueryTableName = "tempQueryElements";
				String tempLinkTableName = "tempLinks";

				// drop table if it exists
				sql = "DROP TABLE IF EXISTS `" + tempQueryTableName;
				s = con.createStatement();
				s.execute(sql);

				// create temp table
				sql = "CREATE TEMPORARY TABLE `"
						+ tempQueryTableName
						+ "` ( `element` varchar(50) NOT NULL, PRIMARY KEY (`element`))";

				s = con.createStatement();
				s.execute(sql);

				// INSERT rows into temp table using prepared statement

				sql = "INSERT INTO `" + tempQueryTableName
						+ "` (element) VALUES (?)";
				PreparedStatement ps = con.prepareStatement(sql);

				// from the client's list of biodes, get a set of query items
				Set<String> itemSet = new HashSet<String>(Arrays.asList(super
						.sanitizeString(csvBiodeList).split(",")));

				// insert rows
				for (String queryItem : itemSet) {
					ps.setString(1, queryItem);
					ps.execute();
					ps.clearParameters();
				}

				// TODO join!
				for (Iterator<JSONObject> iter = joList.iterator(); iter
						.hasNext();) {
					// drop table if it exists
					sql = "DROP TABLE IF EXISTS `" + tempLinkTableName;
					s = con.createStatement();
					s.execute(sql);

					// join on element1
					JSONObject jo = iter.next();
					sql = "CREATE TEMPORARY TABLE `" + tempLinkTableName
							+ "` SELECT t.element1,t.element2 FROM `"
							+ jo.getString("name") + "` AS t INNER JOIN `"
							+ tempQueryTableName
							+ "` q ON q.element=t.element1";
					s = con.createStatement();
					s.execute(sql);

					// join on element2
					sql = "SELECT count(*) FROM `" + tempLinkTableName
							+ "` AS t INNER JOIN `" + tempQueryTableName
							+ "` q ON q.element=t.element2";
					s = con.createStatement();
					s.execute(sql);
					rs = s.getResultSet();
					rs.next();
					String linkCount = rs.getString("count(*)");

					// record linkCount
					jo.put("linkCount", Float.parseFloat(linkCount));
				}

				// DROP temporary tables
				sql = "DROP TABLE `" + tempQueryTableName + "`";
				s = con.createStatement();
				s.execute(sql);

				sql = "DROP TABLE `" + tempLinkTableName + "`";
				s = con.createStatement();
				s.execute(sql);

				// check the time to this point
				js.key("time8").value(((new Date()).getTime() - start));

				// sort by linkCount
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"linkCount")));

				// check the time to this point
				js.key("time9").value(((new Date()).getTime() - start));

				// write the results to the JSONStringer
				js.key("tracks").array();
				for (int i = 0; i < joList.size(); i++) {
					js.value(joList.get(i));
				}
				js.endArray(); // ends the tracks JSONArray

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses modes
	 * clusters to make recommendations based on a score calculated from module
	 * overlap. Additionally, the 20 top-scoring tracks will be scored again
	 * based on link count. The results are sorted by trScore and returned in a
	 * JSONArray called "tracks". This is a reduced version for testing...
	 * doesn't do the final lookup of tracklist info.
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @param returnSize
	 *            specify the number of tracks in result
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats6_reduced_2_joins(
			String NCBI_organism, String csvBiodeList, int returnSize)
			throws JSONException {

		// TODO

		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);

		String mySqlList = csvToMySqlList(super.sanitizeString(csvBiodeList));

		int querySize = csvBiodeList.split(",").length;

		String modesTableName = modesTableNameHash.get(NCBI_organism);

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				// get list of modules in which query set is involved

				String sql = "SELECT y.trackName,y.clusterID,count(*) FROM `"
						+ modesTableName + "` AS y WHERE y.element IN ("
						+ mySqlList + ") GROUP BY y.trackName,y.clusterID";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				// check the time to this point
				js.key("time1").value(((new Date()).getTime() - start));

				// count up matches for each track/module combination
				HashMap<String, HashMap<String, Integer>> trackHash = new HashMap<String, HashMap<String, Integer>>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					int intersection = rs.getInt("count(*)");

					// encountered a new track
					if (!trackHash.containsKey(track)) {
						trackHash.put(track, new HashMap<String, Integer>());
					}

					trackHash.get(track).put(clusterID, intersection);
				}

				// in case no results
				if (trackHash.size() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				// build up sql statement to get cluster sizes
				// This statement can get very long... the query is sped up if
				// index on trackName and clusterID columns.
				StringBuffer sb = new StringBuffer();
				for (String track : trackHash.keySet()) {
					for (String cluster : trackHash.get(track).keySet()) {

						// only if intersection is more than 1
						if (trackHash.get(track).get(cluster) > 1) {
							if (sb.toString().isEmpty()) {
							} else {
								sb.append(" OR ");
							}
							sb.append("(m.trackName='" + track
									+ "' AND m.clusterID='" + cluster + "')");
						}
					}
				}

				// in case no results
				if (sb.toString().length() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				// check the time to this point
				js.key("time2").value(((new Date()).getTime() - start));

				sql = "SELECT trackName,clusterID,cluster_size FROM `modeslist` AS m WHERE "
						+ sb.toString();

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();

				// check the time to this point
				js.key("time3").value(((new Date()).getTime() - start));

				// calculate the scores ... keeping the top one for each track
				HashMap<String, Float> scoreHash = new HashMap<String, Float>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					// int clusterSize = rs.getInt("cluster_size");

					int intersection = trackHash.get(track).get(clusterID);

					int union = querySize + rs.getInt("cluster_size")
							- intersection;

					float score = new Float(intersection) / new Float(union);

					if (!scoreHash.containsKey(track)) {
						scoreHash.put(track, score);
					} else {
						if (scoreHash.get(track) < score) {
							scoreHash.put(track, score);
						}
					}
				}

				// check the time to this point
				js.key("time4").value(((new Date()).getTime() - start));

				// get the score
				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();
				for (String trackName : scoreHash.keySet()) {
					JSONObject jo = new JSONObject();
					jo.put("name", trackName);
					jo.put("trScore", scoreHash.get(trackName));

					joList.add(jo);
				}

				// check the time to this point
				js.key("time5").value(((new Date()).getTime() - start));

				// sort the results by trScore
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"trScore")));

				// check the time to this point
				js.key("time6").value(((new Date()).getTime() - start));

				// get the link counts for top tracks
				// remove the extra tracks
				while (joList.size() > returnSize) {
					joList.remove(joList.size() - 1);
				}

				// check the time to this point
				js.key("time7").value(((new Date()).getTime() - start));

				// create temp tables
				String tempQueryTableName = "tempQueryElements";
				String tempQueryTableName2 = "tempQueryElements2";

				// drop table if it exists
				sql = "DROP TABLE IF EXISTS `" + tempQueryTableName;
				s = con.createStatement();
				s.execute(sql);

				sql = "DROP TABLE IF EXISTS `" + tempQueryTableName2;
				s = con.createStatement();
				s.execute(sql);

				// create temp table
				sql = "CREATE TEMPORARY TABLE `"
						+ tempQueryTableName
						+ "` ( `element` varchar(50) NOT NULL, PRIMARY KEY (`element`))";

				s = con.createStatement();
				s.execute(sql);

				// INSERT rows into temp table

				sql = "INSERT INTO `" + tempQueryTableName + "` VALUES ("
						+ mySqlList.replace(",", "),(") + ")";

				s = con.createStatement();
				s.execute(sql);

				// clone the tempQueryTable
				// CREATE TABLE recipes_new LIKE production.recipes;
				sql = "CREATE TEMPORARY TABLE `" + tempQueryTableName2
						+ "` LIKE `" + tempQueryTableName + "`";

				s = con.createStatement();
				s.execute(sql);

				// INSERT recipes_new SELECT * FROM production.recipes;
				sql = "INSERT `" + tempQueryTableName2 + "` SELECT * FROM `"
						+ tempQueryTableName + "`";

				s = con.createStatement();
				s.execute(sql);

				// TODO join!
				for (Iterator<JSONObject> iter = joList.iterator(); iter
						.hasNext();) {

					// do joins
					JSONObject jo = iter.next();

					sql = "SELECT count(*) FROM (SELECT t.element1,t.element2 FROM `"
							+ jo.getString("name")
							+ "` AS t INNER JOIN `"
							+ tempQueryTableName2
							+ "` q2 ON q2.element=t.element1) AS t INNER JOIN `"
							+ tempQueryTableName
							+ "` q1 ON q1.element=t.element2";

					// sql = "SELECT count(*) FROM `"
					// + jo.getString("name") + "`,`" + tempQueryTableName
					// + "`,`" + tempQueryTableName2
					// + "` WHERE ( (`" + jo.getString("name")
					// + "`.element1=`" + tempQueryTableName
					// + "`.element) AND (`" + jo.getString("name")
					// + "`.element2=`" + tempQueryTableName2
					// + "`.element) )";

					s = con.createStatement();
					s.execute(sql);
					rs = s.getResultSet();
					rs.next();
					String linkCount = rs.getString("count(*)");

					// record linkCount
					jo.put("linkCount", Float.parseFloat(linkCount));
				}

				// DROP temporary tables
				sql = "DROP TABLE `" + tempQueryTableName + "`";
				s = con.createStatement();
				s.execute(sql);

				sql = "DROP TABLE `" + tempQueryTableName2 + "`";
				s = con.createStatement();
				s.execute(sql);

				// check the time to this point
				js.key("time8").value(((new Date()).getTime() - start));

				// sort by linkCount
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"linkCount")));

				// check the time to this point
				js.key("time9").value(((new Date()).getTime() - start));

				// write the results to the JSONStringer
				js.key("tracks").array();
				for (int i = 0; i < joList.size(); i++) {
					js.value(joList.get(i));
				}
				js.endArray(); // ends the tracks JSONArray

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}

	/**
	 * Get some values to report as Track Recommender scores. Uses modes
	 * clusters to make recommendations based on a score calculated from module
	 * overlap. Additionally, the 20 top-scoring tracks will be scored again
	 * based on link count. The results are sorted by trScore and returned in a
	 * JSONArray called "tracks". This is a reduced version for testing...
	 * doesn't do the final lookup of tracklist info.
	 * 
	 * @param NCBI_organism
	 * @param csvBiodeList
	 * @param returnSize
	 *            specify the number of tracks in result
	 * @return
	 * @throws JSONException
	 */
	private String getTrackRecommenderStats6_reduced_in_in(
			String NCBI_organism, String csvBiodeList, int returnSize)
			throws JSONException {

		// TODO

		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("biodeList").value(csvBiodeList);

		String mySqlList = csvToMySqlList(super.sanitizeString(csvBiodeList));

		int querySize = csvBiodeList.split(",").length;

		String modesTableName = modesTableNameHash.get(NCBI_organism);

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				// get list of modules in which query set is involved

				String sql = "SELECT y.trackName,y.clusterID,count(*) FROM `"
						+ modesTableName + "` AS y WHERE y.element IN ("
						+ mySqlList + ") GROUP BY y.trackName,y.clusterID";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				// check the time to this point
				js.key("time1").value(((new Date()).getTime() - start));

				// count up matches for each track/module combination
				HashMap<String, HashMap<String, Integer>> trackHash = new HashMap<String, HashMap<String, Integer>>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					int intersection = rs.getInt("count(*)");

					// encountered a new track
					if (!trackHash.containsKey(track)) {
						trackHash.put(track, new HashMap<String, Integer>());
					}

					trackHash.get(track).put(clusterID, intersection);
				}

				// in case no results
				if (trackHash.size() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				// build up sql statement to get cluster sizes
				// This statement can get very long... the query is sped up if
				// index on trackName and clusterID columns.
				StringBuffer sb = new StringBuffer();
				for (String track : trackHash.keySet()) {
					for (String cluster : trackHash.get(track).keySet()) {

						// only if intersection is more than 1
						if (trackHash.get(track).get(cluster) > 1) {
							if (sb.toString().isEmpty()) {
							} else {
								sb.append(" OR ");
							}
							sb.append("(m.trackName='" + track
									+ "' AND m.clusterID='" + cluster + "')");
						}
					}
				}

				// in case no results
				if (sb.toString().length() == 0) {
					js.key("time").value(((new Date()).getTime() - start));
					js.endObject();
					con.close();
					return js.toString();
				}

				// check the time to this point
				js.key("time2").value(((new Date()).getTime() - start));

				sql = "SELECT trackName,clusterID,cluster_size FROM `modeslist` AS m WHERE "
						+ sb.toString();

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();

				// check the time to this point
				js.key("time3").value(((new Date()).getTime() - start));

				// calculate the scores ... keeping the top one for each track
				HashMap<String, Float> scoreHash = new HashMap<String, Float>();
				while (rs.next()) {
					String track = rs.getString("trackName");
					String clusterID = rs.getString("clusterID");
					// int clusterSize = rs.getInt("cluster_size");

					int intersection = trackHash.get(track).get(clusterID);

					int union = querySize + rs.getInt("cluster_size")
							- intersection;

					float score = new Float(intersection) / new Float(union);

					if (!scoreHash.containsKey(track)) {
						scoreHash.put(track, score);
					} else {
						if (scoreHash.get(track) < score) {
							scoreHash.put(track, score);
						}
					}
				}

				// check the time to this point
				js.key("time4").value(((new Date()).getTime() - start));

				// get the score
				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();
				for (String trackName : scoreHash.keySet()) {
					JSONObject jo = new JSONObject();
					jo.put("name", trackName);
					jo.put("trScore", scoreHash.get(trackName));

					joList.add(jo);
				}

				// check the time to this point
				js.key("time5").value(((new Date()).getTime() - start));

				// sort the results by trScore
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"trScore")));

				// check the time to this point
				js.key("time6").value(((new Date()).getTime() - start));

				// get the link counts for top tracks
				// remove the extra tracks
				while (joList.size() > returnSize) {
					joList.remove(joList.size() - 1);
				}

				// check the time to this point
				js.key("time7").value(((new Date()).getTime() - start));

				// create temp tables
				String tempLinkTableName = "tempLinks";

				// TODO join!
				for (Iterator<JSONObject> iter = joList.iterator(); iter
						.hasNext();) {
					// drop table if it exists
					sql = "DROP TABLE IF EXISTS `" + tempLinkTableName;
					s = con.createStatement();
					s.execute(sql);

					// SELECT WHERE on element1
					JSONObject jo = iter.next();
					sql = "CREATE TEMPORARY TABLE `" + tempLinkTableName
							+ "` SELECT t.element1,t.element2 FROM `"
							+ jo.getString("name")
							+ "` AS t WHERE t.element1 in (" + mySqlList + ")";
					s = con.createStatement();
					s.execute(sql);

					// SELECT WHERE on element2
					sql = "SELECT count(*) FROM `" + tempLinkTableName
							+ "` AS t WHERE t.element2 in (" + mySqlList + ")";
					s = con.createStatement();
					s.execute(sql);
					rs = s.getResultSet();
					rs.next();
					String linkCount = rs.getString("count(*)");

					// record linkCount
					jo.put("linkCount", Float.parseFloat(linkCount));
				}

				// DROP temporary tables
				sql = "DROP TABLE `" + tempLinkTableName + "`";
				s = con.createStatement();
				s.execute(sql);

				// check the time to this point
				js.key("time8").value(((new Date()).getTime() - start));

				// sort by linkCount
				Collections.sort(joList, Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"linkCount")));

				// check the time to this point
				js.key("time9").value(((new Date()).getTime() - start));

				// write the results to the JSONStringer
				js.key("tracks").array();
				for (int i = 0; i < joList.size(); i++) {
					js.value(joList.get(i));
				}
				js.endArray(); // ends the tracks JSONArray

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.key("time").value(((new Date()).getTime() - start));
		js.endObject();

		return js.toString();
	}
}
