package edu.ucsc.ib.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ucsc.ib.client.viewcontrols.ScoredNodeFilterDialogBox;

/**
 * Servlet to upload and retrieve node score data from mysql db.
 * 
 * @author Chris
 * 
 */
public class ConceptScoresService extends TrackDbService {

	private static final String UPLOADED_SCORES_TABLE_NAME_PREFIX = "uploadScores";
	private static final String PREFILTER_TABLE_NAME_PREFIX = "uploadPrefilter";

	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 5661857285957361482L;

	public ConceptScoresService() {
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

		// do some session stuff

		HttpSession session = InitSession(req);
		final String userID = session.getId();

		if (!sessionHasAttribute(session, "uploadedScoreCleaner")) {

			session.setAttribute("uploadedScoreCleaner",
					new HttpSessionBindingListener() {

						/**
						 * Does nothing. Required by interface.
						 */
						public void valueBound(HttpSessionBindingEvent event) {
						}

						/**
						 * Clean up the databases for session.
						 */
						public void valueUnbound(HttpSessionBindingEvent event) {
							deleteUserTables(userID);
						}
					});
		}

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/conceptScores/test

			try {
				DatabaseService.writeTextResponse(
						"hello from ConceptScoresService", resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("saveScores")) {
			// uses multipartContent

			JSONObject resultJO = new JSONObject();
			String error = null;
			int id = 0;

			// Check that we have a file upload request
			if (ServletFileUpload.isMultipartContent(req)) {

				List<FileItem> items = parseMultipartRequest(req);

				Map<String, String> parameterMap = getParamaterMapping(items);

				if (validateParamData(parameterMap)) {
					// write data to DB
					try {
						this.writeScoresToDB(userID, parameterMap);
					} catch (SQLException e) {
						e.printStackTrace();
						error = "SQL error writing to DB";
					}
					try {
						resultJO = this.getSavedScoreInfo(userID);
					} catch (SQLException e) {
						e.printStackTrace();
						error = "SQL error getting saved score info";
					} catch (JSONException e) {
						e.printStackTrace();
						error = "JSON error";
					}
				} else {
					// don't write anything to DB ... generate message for
					// client
					error = "nothing written to db";
				}
			}

			JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
					error, id);

			try {
				super.writeTextResponse(jo.toString(), resp);
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else if (path.endsWith("getScoreSetInfo")) {
			// get the saved score set information for the user
			// http://localhost:8080/ib/data/conceptScores/getScoreSetInfo

			JSONObject resultJO = new JSONObject();
			String error = null;
			int id = 0;

			try {
				resultJO = this.getSavedScoreInfo(userID);
			} catch (SQLException e) {
				if (e.getErrorCode() == 1146) {
					// http://dev.mysql.com/doc/refman/5.0/en/error-messages-server.html
					error = "no uploaded scores table";
				} else {
					e.printStackTrace();
					error = "SQL error getting saved score info";
				}
			} catch (JSONException e) {
				e.printStackTrace();
				error = "JSON error";
			}

			JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
					error, id);

			try {
				super.writeTextResponse(jo.toString(), resp);
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else if (path.endsWith("getScores")) {
			// test with:
			// http://localhost:8080/ib/data/conceptScores/getScores?scoreSetName=uploaded_ERBB2AMP_SUBTYPE&concepts=,10270,117,136,181,211,5321,5589,5594,5595,65057,6794,6944,8574,9370,9744

			@SuppressWarnings("unchecked")
			Map<String, String[]> paramMap = req.getParameterMap();

			String scoreSetName = paramMap.get("scoreSetName")[0];
			String[] conceptsArray = paramMap.get("concepts")[0].split(",");

			List<String> conceptsList = Arrays.asList(conceptsArray);
			Set<String> conceptsSet = new HashSet<String>(conceptsList);

			try {
				writeTextResponse(
						getConceptScores(conceptsSet, scoreSetName, userID)
								.toString(), resp);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("graphFilter2")) {
			// test with:
			// http://localhost:8080/ib/data/conceptScores/graphFilter2?scoreSetName=uploaded_ERBB2AMP_SUBTYPE&method=0&threshold=0&maxResults=20&networks=UCSC_Superpathway,human_biogrid
			String error = null;
			int id = 0;

			@SuppressWarnings("unchecked")
			Map<String, String[]> paramMap = req.getParameterMap();

			// check for missing parameters
			if (!paramMap.containsKey("networks")
					|| !paramMap.containsKey("scoreSetName")) {
				try {
					error = "missing parameter";
					DatabaseService.writeJSONRPCTextResponse(null, error, id,
							resp);
					return;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}

			String networks[] = paramMap.get("networks")[0].split(",");
			String scoreSetName = paramMap.get("scoreSetName")[0];
			int maxResults = Integer.valueOf(paramMap.get("maxResults")[0]);
			double threshold = Double.valueOf(paramMap.get("threshold")[0]);

			String method = "0";
			if (paramMap.containsKey("method")) {
				method = paramMap.get("method")[0];
			}

			if (!method.equalsIgnoreCase("2")) {
				threshold = 0;
			}

			HashSet<String> networksHashSet = new HashSet<String>(
					Arrays.asList(networks));

			boolean restrictToScoredNodes = false;
			if (!method.equalsIgnoreCase("0")) {
				restrictToScoredNodes = true;
			}

			// networksHashSet, scoreSetName, method, threshold, maxResults,
			// restrictToScoredNodes, userID

			System.out.println("netowrksHashSet: " + networksHashSet);
			System.out.println("scoreSetName: " + scoreSetName);
			System.out.println("method: " + method);
			System.out.println("threshold: " + threshold);
			System.out.println("maxResults: " + maxResults);
			System.out.println("restrictToScoredNodes: "
					+ restrictToScoredNodes);
			System.out.println("userID: " + userID);

			try {
				writeTextResponse(
						getGraphFilterResults0(networksHashSet, scoreSetName,
								method, threshold, maxResults,
								restrictToScoredNodes, userID).toString(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("edgeFilter")) {
			// test with:
			// http://localhost:8080/ib/data/conceptScores/edgeFilter?scoreSetName=uploaded_ERBB2AMP_SUBTYPE&networks=UCSC_Superpathway,human_biogrid

			// System.out.println(getCurrentTime() + ": begin edgeFilter");

			String error = null;
			int id = 0;

			@SuppressWarnings("unchecked")
			Map<String, String[]> paramMap = req.getParameterMap();

			// check for missing parameters
			if (!paramMap.containsKey("networks")
					|| !paramMap.containsKey("scoreSetName")
					|| !paramMap.containsKey("thresholdValue")
					|| !paramMap.containsKey("thresholdComparisonMethod")
					|| !paramMap.containsKey("edgeScoringMethod")) {
				try {
					error = "missing parameter";
					DatabaseService.writeJSONRPCTextResponse(null, error, id,
							resp);
					return;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}

			String networks[] = paramMap.get("networks")[0].split(",");
			String scoreSetName = paramMap.get("scoreSetName")[0];
			double thresholdValue = Double.parseDouble(paramMap
					.get("thresholdValue")[0]);
			String thresholdComparisonMethod = paramMap
					.get("thresholdComparisonMethod")[0];
			String edgeScoringMethod = paramMap.get("edgeScoringMethod")[0];

			String method = "0";
			if (paramMap.containsKey("method")) {
				method = paramMap.get("method")[0];
			}

			HashSet<String> networksHashSet = new HashSet<String>(
					Arrays.asList(networks));

			try {
				writeTextResponse(
						getGraphFilterResults_byEdges(networksHashSet,
								scoreSetName, edgeScoringMethod, method,
								thresholdValue, thresholdComparisonMethod,
								userID).toString(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// System.out.println(getCurrentTime() + ": end edgeFilter");
		} else {
			// nothing to do !
		}
	}

	// TODO /////////////////////////////////////////////////////////////

	private JSONObject getGraphFilterResults_byEdges(
			final HashSet<String> networksHashSet, final String scoreSetName,
			final String edgeScoringMethod, final String method,
			final Double thresholdValue,
			final String thresholdComparisonMethod, final String userID) {

		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		// check if score table exists
		Connection connection = getMySqlConnection_custom();
		if (!tableExists(getSavedScoreTableName(userID), connection)) {
			error = "score table not found";
			JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
					error, id);
			return jo;
		}
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// create edge filtering table
		createPreFilterTable(networksHashSet, userID);

		// set node scores
		String columnName = scoreSetName;
		if (scoreSetName
				.startsWith(ScoredNodeFilterDialogBox.UPLOAD_NAME_PREFIX)) {
			columnName = scoreSetName.replaceFirst(
					ScoredNodeFilterDialogBox.UPLOAD_NAME_PREFIX, "");
		}

		setNodeScores(columnName, userID);

		// set edge scores
		if (edgeScoringMethod
				.equals(ScoredNodeFilterDialogBox.EDGE_SCORE_METHOD_2)) {
			setEdgeScores_0(false, userID);
		} else {
			setEdgeScores_0(true, userID);
		}

		// TODO code above this line are setting up for filtering

		// get edge filter results
		boolean keepLargerScores = true;
		if (!thresholdComparisonMethod.equalsIgnoreCase("gt")) {
			keepLargerScores = false;
		}
		JSONArray edgesJA = getFilteredEdges_simpleEdgeScoreThreshold(
				keepLargerScores, thresholdValue, userID);

		// System.err.println(edgesJA.length() + " edges to report");

		try {
			resultJO.put("scoreSetName", scoreSetName);
			resultJO.put("edges", edgesJA);
		} catch (JSONException e) {
			e.printStackTrace();
			error = "JSON error";
			JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
					error, id);
			return jo;
		}

		// return JSON-RPC response
		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);

		return jo;
	}

	/**
	 * Create a table of links. Table has no score information.
	 * 
	 * @param networksHashSet
	 * @param userID
	 */
	private void createPreFilterTable(HashSet<String> networksHashSet,
			String userID) {
		// ? http://www.codingforums.com/showthread.php?t=59630

		String prefilterTableName = getPrefilterTableName(userID);

		Connection connection = getMySqlConnection();

		try {
			Statement statement = connection.createStatement();

			// drop existing table
			String sql = "DROP TABLE IF EXISTS `" + prefilterTableName + "`";
			statement.execute(sql);

			// create new table
			sql = "CREATE TABLE IF NOT EXISTS `" + prefilterTableName
					+ "` ( `element1` VARCHAR(250) NOT NULL, "
					+ "`element2` VARCHAR(250) NOT NULL, "
					+ "`element1_score` FLOAT(5,3) DEFAULT 0, "
					+ "`element2_score` FLOAT(5,3) DEFAULT 0, "
					+ "`edge_score` FLOAT(5,3) DEFAULT 0, "
					+ "PRIMARY KEY (`element1`,`element2`) " + " )";

			statement.execute(sql);

			// copy network elements
			for (String networkName : networksHashSet) {
				sql = "INSERT IGNORE INTO `" + prefilterTableName
						+ "` (element1,element2) "
						+ "SELECT DISTINCT t.element1,t.element2 FROM `"
						+ sanitizeString(networkName)
						+ "` AS t WHERE t.element1 != t.element2";

				statement.execute(sql);
			}

			// close statements and connections
			statement.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get edges based on simple edge score threshold.
	 * 
	 * @param keepLargerScores
	 * @param threshold
	 * @param userID
	 * @return
	 */
	private JSONArray getFilteredEdges_simpleEdgeScoreThreshold(
			final boolean keepLargerScores, final double threshold,
			final String userID) {
		String prefilterTableName = getPrefilterTableName(userID);

		JSONArray edgesJA = new JSONArray();

		try {
			Connection connection = getMySqlConnection_custom();
			Statement statement = connection.createStatement();

			StringBuffer sb = new StringBuffer();
			sb.append("SELECT t.element1,t.element2,t.edge_score FROM `"
					+ prefilterTableName + "` AS t ");
			sb.append("WHERE ABS(t.edge_score)");

			if (keepLargerScores) {
				sb.append(">");
			} else {
				sb.append("<=");
			}
			sb.append(threshold);

			sb.append(" ORDER BY ABS(t.edge_score)");

			if (keepLargerScores) {
				sb.append(" DESC");
			} else {
				sb.append(" ASC");
			}

			statement.execute(sb.toString());

			ResultSet rs = statement.getResultSet();

			while (rs.next()) {
				JSONObject edgeJO = new JSONObject();
				edgesJA.put(edgeJO);

				edgeJO.put("1", rs.getString("element1"));
				edgeJO.put("2", rs.getString("element2"));
				edgeJO.put("score", rs.getDouble("edge_score"));
			}

			statement.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return edgesJA;
		} catch (JSONException e) {
			e.printStackTrace();
			return edgesJA;
		}

		return edgesJA;
	}

	/**
	 * Set the edge score of links. Uses the element score that is closer to 0.
	 * 
	 * @param userID
	 */
	private void setEdgeScores_0(final boolean useScoreCloserToZero,
			final String userID) {
		String prefilterTableName = getPrefilterTableName(userID);
		String comparator = "<";
		if (!useScoreCloserToZero) {
			comparator = ">";
		}

		try {
			Connection connection = getMySqlConnection_custom();
			Statement statement = connection.createStatement();

			StringBuffer sb = new StringBuffer();
			sb.append("UPDATE `" + prefilterTableName + "` AS t ");
			sb.append("SET t.edge_score = IF(ABS(t.element1_score)"
					+ comparator
					+ "ABS(t.element2_score),t.element1_score,t.element2_score) ");
			sb.append("WHERE (t.element1_score IS NOT NULL) AND (t.element2_score IS NOT NULL)");

			statement.execute(sb.toString());

			statement.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Update user's prefilter network table with specified node score
	 * 
	 * @param scoreColName
	 * @param userID
	 */
	private void setNodeScores(final String scoreColName, final String userID) {
		String prefilterTableName = getPrefilterTableName(userID);
		String scoreTableName = getSavedScoreTableName(userID);

		try {
			Connection connection = getMySqlConnection_custom();
			Statement statement = connection.createStatement();

			StringBuffer sb;

			// temp score table
			sb = new StringBuffer();

			String tempScoresTable = "tempScores";

			sb.append("DROP TABLE IF EXISTS `" + tempScoresTable + "`");
			statement.execute(sb.toString());

			sb = new StringBuffer();
			sb.append("CREATE TEMPORARY TABLE `" + tempScoresTable
					+ "` SELECT t.concept,t." + scoreColName + " FROM `"
					+ scoreTableName + "` AS t");

			statement.execute(sb.toString());

			statement.execute("CREATE INDEX `concept_idx` ON "
					+ tempScoresTable + " (concept)");

			// find duplicates
			HashSet<String> duplicatesHashSet = getDuplicatesFromTable(
					"concept", tempScoresTable, connection);

			// resolve duplicates
			resolveScoreForDuplicateConcepts(duplicatesHashSet, scoreColName,
					tempScoresTable, connection);

			// element1
			sb = new StringBuffer();
			sb.append("UPDATE `" + prefilterTableName + "` AS left_table ");
			sb.append("LEFT JOIN `" + tempScoresTable + "` AS right_table ");
			sb.append("ON left_table.element1 = right_table.concept ");
			sb.append("SET left_table.element1_score = right_table."
					+ scoreColName);

			statement.execute(sb.toString());

			// element2
			sb = new StringBuffer();
			sb.append("UPDATE `" + prefilterTableName + "` AS left_table ");
			sb.append("LEFT JOIN `" + tempScoresTable + "` AS right_table ");
			sb.append("ON left_table.element2 = right_table.concept ");
			sb.append("SET left_table.element2_score = right_table."
					+ scoreColName);

			statement.execute(sb.toString());

			// DROP temporary tables
			// sb = new StringBuffer();
			// sb.append("DROP TABLE IF EXISTS `" + tempScoresTable + "`");
			// statement.execute(sb.toString());

			statement.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * For a score table with column for concept and column for score, ensure
	 * that only one score is recorded for the specified concepts. Score with
	 * the largest magnitude is used.
	 * 
	 * @param duplicatesHashSet
	 * @param scoreColName
	 * @param tempScoresTable
	 * @param connection
	 *            remains open throughout the method
	 */
	private static void resolveScoreForDuplicateConcepts(
			final HashSet<String> duplicatesHashSet, final String scoreColName,
			final String tempScoresTable, final Connection connection) {
		try {
			Statement statement = connection.createStatement();
			PreparedStatement ps = connection.prepareStatement("INSERT INTO `"
					+ tempScoresTable + "` (concept," + scoreColName
					+ ") VALUES (?,?)");

			for (String concept : duplicatesHashSet) {

				StringBuffer sb = new StringBuffer();

				// find which score to keep
				sb.append("SELECT DISTINCT t." + scoreColName + " ");
				sb.append("FROM `" + tempScoresTable + "` AS t ");
				sb.append("WHERE t.concept='" + concept + "'");

				statement.execute(sb.toString());

				float scoreKeeper = 0;

				ResultSet rs = statement.getResultSet();
				while (rs.next()) {
					float score = rs.getFloat("t." + scoreColName);
					if (Math.abs(score) > Math.abs(scoreKeeper)) {
						scoreKeeper = score;
					}
				}
				rs.close();

				// delete all of concept's rows
				sb = new StringBuffer();
				sb.append("DELETE FROM `" + tempScoresTable + "` ");
				sb.append("WHERE concept='" + concept + "'");

				statement.execute(sb.toString());

				// assign score to concept
				ps.setString(1, concept);
				ps.setFloat(2, scoreKeeper);
				ps.execute();
			}
			// clean up
			ps.close();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get scores for specified concepts.
	 * 
	 * @param conceptsSet
	 * @param scoreSetName
	 * @param userID
	 * @return JSON-RPC result with a JSONArray called "scores".
	 */
	private JSONObject getConceptScores(Set<String> conceptsSet,
			String scoreSetName, String userID) {
		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		JSONArray scoresJA = new JSONArray();
		try {
			resultJO.put("scores", scoresJA);
			HashMap<String, Double> scoresHashMap = new HashMap<String, Double>();

			Connection con;
			Statement s;

			// for scores in uploaded db table
			con = getMySqlConnection_custom();
			s = con.createStatement();

			// String scoreColumn = scoreSetName.replaceFirst(
			// ScoredNodeFilterDialogBox.UPLOAD_NAME_PREFIX, "");

			String scoreColumn = scoreSetName;

			scoresHashMap = getConceptScores_from_uploaded_scores(conceptsSet,
					scoreColumn, userID, s);

			s.close();
			con.close();

			for (String key : scoresHashMap.keySet()) {
				JSONObject conceptJO = new JSONObject();
				scoresJA.put(conceptJO);
				conceptJO.put("concept", key);
				conceptJO.put("score", scoresHashMap.get(key));
			}

			resultJO.put("scoreSetName", scoreSetName);

		} catch (JSONException e1) {
			e1.printStackTrace();
			error = "JSON error";
		} catch (SQLException e) {
			e.printStackTrace();
			error = "sql error";
		}

		// return JSON-RPC response
		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);

		return jo;
	}

	/**
	 * Method 0 of graph filtering
	 * 
	 * @param networksHashSet
	 * @param scoreSetName
	 * @param method
	 * @param maxResults
	 * @param threshold
	 * @param restrictToScoredNodes
	 * @param userID
	 * @return JSON-RPC response
	 * @throws JSONException
	 */
	private JSONObject getGraphFilterResults0(
			final HashSet<String> networksHashSet, final String scoreSetName,
			final String method, final double threshold, final int maxResults,
			final boolean restrictToScoredNodes, final String userID)
			throws JSONException {

		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		// TODO output params for debug - this section may throw JSONException
		JSONObject paramsJO = new JSONObject();
		resultJO.put("params", paramsJO);
		paramsJO.put("scoreSetName", scoreSetName);
		paramsJO.put("method", method);
		paramsJO.put("threshold", threshold);
		paramsJO.put("maxResults", maxResults);
		paramsJO.put("restrictToScoredNodes", restrictToScoredNodes);
		paramsJO.put("userID", userID);
		JSONArray netsJA = new JSONArray();
		paramsJO.put("nets", netsJA);
		for (String net : networksHashSet) {
			netsJA.put(net);
		}

		getGraphFilterResults0_for_uploaded_score(networksHashSet,
				scoreSetName, threshold, maxResults, restrictToScoredNodes,
				userID, resultJO, error);

		// return JSON-RPC response
		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);

		return jo;
	}

	/**
	 * This is a method to get concepts via filtering. First, a sorted list of
	 * concepts is obtained. Sorting is done on the absolute value of the score.
	 * Next, the network neighborhoods of the concepts are taken from the top of
	 * the sorted list. Neighborhoods are recorded until the maximum number of
	 * total results is obtained. This method returns nodes, not edges.
	 * 
	 * @param networksHashSet
	 * @param scoreSetName
	 * @param threshold
	 * @param maxResults
	 * @param userID
	 * @param resultJO
	 *            for JSON-RPC
	 * @param error
	 *            for JSON-RPC
	 * @param restrictToScoredNodes
	 * @return number of concepts found
	 */
	private int getGraphFilterResults0_for_uploaded_score(
			final HashSet<String> networksHashSet, final String scoreSetName,
			final Double threshold, final int maxResults,
			final boolean restrictToScoredNodes, final String userID,
			JSONObject resultJO, String error) {

		int result = 0;

		double absThreshold = Math.abs(threshold);

		HashMap<String, Connection> conHashMap = new HashMap<String, Connection>();
		conHashMap.put("preloaded", getMySqlConnection());
		conHashMap.put("uploaded", this.getMySqlConnection_custom());

		try {
			HashMap<String, Statement> sHashMap = new HashMap<String, Statement>();
			sHashMap.put("preloaded", conHashMap.get("preloaded")
					.createStatement());
			sHashMap.put("uploaded", conHashMap.get("uploaded")
					.createStatement());

			// get concepts that meet the threshold

			String scoreColumn = sanitizeString(scoreSetName);

			resultJO.put("scoreSetName", scoreSetName);

			// get score data
			HashMap<String, Double> allScoresHashMap = getAllConceptScores_from_uploaded_scores(
					scoreColumn, userID, sHashMap.get("uploaded"));

			ArrayList<String> sortedConceptsArrayList = getAllSortedConcepts_from_uploaded_scores(
					scoreColumn, userID, sHashMap.get("uploaded"));

			// get link data until max number of results reached
			JSONArray networksJA = new JSONArray();
			resultJO.put("networks", networksJA);
			NetworkLinkTreeMap linksTreeMap = new NetworkLinkTreeMap();

			int numResults = 0;

			HashSet<String> reportConceptsHashSet = new HashSet<String>();
			;

			for (int i = 0; i < sortedConceptsArrayList.size()
					&& numResults < maxResults; i++) {
				Set<String> conceptsSet = new HashSet<String>();
				conceptsSet.add(sortedConceptsArrayList.get(i));

				for (String networkName : networksHashSet) {
					getNeighborhood(sanitizeString(networkName), conceptsSet,
							linksTreeMap);
					if (i == 0) {
						networksJA.put(networkName);
					}
				}

				if (restrictToScoredNodes) {
					// don't count unscored concepts
					reportConceptsHashSet = new HashSet<String>();

					for (String element1 : linksTreeMap.keySet()) {
						for (String element2 : linksTreeMap.get(element1)
								.keySet()) {
							// check score for link
							if ((allScoresHashMap.containsKey(element1))
									&& (allScoresHashMap.containsKey(element2))) {
								double score1 = allScoresHashMap.get(element1);
								double score2 = allScoresHashMap.get(element2);
								// save lower score
								if (score1 > score2) {
									linksTreeMap.setScore(element1, element2,
											score2, false);
								} else {
									linksTreeMap.setScore(element1, element2,
											score1, false);
								}

								// do not report links where lowest scoring node
								// is less than threshold
								if ((absThreshold != 0)
										&& (Math.abs(linksTreeMap.getScore(
												element1, element2)) > Math
												.abs(absThreshold))) {
									reportConceptsHashSet.add(element1);
									reportConceptsHashSet.add(element2);
								} else {
									reportConceptsHashSet.add(element1);
									reportConceptsHashSet.add(element2);
								}
							}
						}

					}
					numResults = reportConceptsHashSet.size();
				} else {
					reportConceptsHashSet = new HashSet<String>();

					for (String concept : linksTreeMap.keySet()) {
						if (restrictToScoredNodes
								&& !allScoresHashMap.containsKey(concept)) {
							continue;
						}
						if (allScoresHashMap.containsKey(concept)) {
							// there may be linked concepts that do not have
							// score
							reportConceptsHashSet.add(concept);
						}
					}
					numResults = reportConceptsHashSet.size();
				}
			}

			// report resulting concepts
			JSONArray conceptsJA = new JSONArray();
			resultJO.put("concepts", conceptsJA);
			for (String concept : reportConceptsHashSet) {
				JSONObject conceptJO = new JSONObject();
				conceptsJA.put(conceptJO);
				conceptJO.put("id", concept);
				if (allScoresHashMap.containsKey(concept)) {
					// there may be linked concepts that do not have score
					conceptJO.put("score", allScoresHashMap.get(concept));
				}
			}
			result = conceptsJA.length();

			// close statements and connections
			for (String name : sHashMap.keySet()) {
				sHashMap.get(name).close();
				conHashMap.get(name).close();
			}

		} catch (SQLException e) {
			e.printStackTrace();
			error = "sql error";
			return result;
		} catch (JSONException e) {
			e.printStackTrace();
			error = "JSON error";
			return result;
		}
		return result;
	}

	/**
	 * Get concept scores from an uploaded db table. If there are multiple score
	 * for a concept, then the one with largest magnitude is reported.
	 * 
	 * @param conceptsSet
	 * @param scoreColumn
	 * @param userID
	 * @param statement
	 *            must connect to "custom" db
	 * @return
	 * @throws SQLException
	 */
	private static HashMap<String, Double> getConceptScores_from_uploaded_scores(
			final Set<String> conceptsSet, final String scoreColumn,
			final String userID, Statement statement) throws SQLException {

		String scoreTableName = getSavedScoreTableName(userID);
		String mysqlList = setToMysqlList(conceptsSet);

		String sql = "SELECT t.concept,t." + scoreColumn + " FROM `"
				+ scoreTableName + "` AS t WHERE t.concept IN (" + mysqlList
				+ ")";

		statement.execute(sql);
		ResultSet rs = statement.getResultSet();

		HashMap<String, Double> resultHashSet = new HashMap<String, Double>();
		while (rs.next()) {
			String concept = rs.getString("concept");
			double score = rs.getDouble(scoreColumn);
			if (resultHashSet.containsKey(concept)
					&& Math.abs(resultHashSet.get(concept)) > Math.abs(score)) {
				continue;
			} else {
				resultHashSet.put(concept, score);
			}
		}

		rs.close();

		return resultHashSet;
	}

	/**
	 * Get sorted list of all concepts in score set, sorted by descending
	 * absolute value of score
	 * 
	 * @param scoreColumn
	 * @param userID
	 * @param statement
	 *            must connect to "custom" db
	 * @return ArrayList of concepts sorted by their descending absolute value
	 *         of score
	 * @throws SQLException
	 */
	private static ArrayList<String> getAllSortedConcepts_from_uploaded_scores(
			final String scoreColumn, final String userID,
			final Statement statement) throws SQLException {

		String scoreTableName = getSavedScoreTableName(userID);

		String sql = "SELECT t.concept,t." + scoreColumn + ",ABS(t."
				+ scoreColumn + ") FROM `" + scoreTableName
				+ "` AS t ORDER BY ABS(t." + scoreColumn + ") DESC";

		statement.execute(sql);
		ResultSet rs = statement.getResultSet();

		ArrayList<String> result = new ArrayList<String>();
		while (rs.next()) {
			result.add(rs.getString("concept"));
		}

		rs.close();

		return result;
	}

	/**
	 * Get concept scores for all the concepts in the db table.
	 * 
	 * @param scoreColumn
	 * @param userID
	 * @param statement
	 *            must connect to "custom" db
	 * @return HashMap of scores keyed by concept
	 * @throws SQLException
	 */
	private static HashMap<String, Double> getAllConceptScores_from_uploaded_scores(
			final String scoreColumn, final String userID,
			final Statement statement) throws SQLException {

		String scoreTableName = getSavedScoreTableName(userID);

		String sql = "SELECT t.concept,t." + scoreColumn + " FROM `"
				+ scoreTableName + "` AS t";

		statement.execute(sql);
		ResultSet rs = statement.getResultSet();

		HashMap<String, Double> result = new HashMap<String, Double>();
		while (rs.next()) {
			result.put(rs.getString("concept"), rs.getDouble(scoreColumn));
		}

		rs.close();

		return result;
	}

	/**
	 * Write score data to database tables
	 * 
	 * @param userID
	 * @param parameterMap
	 * @throws SQLException
	 */
	private void writeScoresToDB(String userID, Map<String, String> parameterMap)
			throws SQLException {
		String scoreTableName = getSavedScoreTableName(userID);

		Connection con_custom = getMySqlConnection_custom();
		Statement s_custom = con_custom.createStatement();

		String sanitizedScoreTableName = sanitizeString(scoreTableName);

		// CREATE table for score data
		// want each column to be a score set
		String sql = "DROP TABLE IF EXISTS `" + sanitizedScoreTableName + "`";
		s_custom.execute(sql);

		sql = "CREATE TABLE IF NOT EXISTS `"
				+ sanitizedScoreTableName
				+ "` ( `concept` VARCHAR(250) NOT NULL, `uploadedConcept` VARCHAR(250) NOT NULL, PRIMARY KEY (`uploadedConcept`))";

		s_custom.execute(sql);

		// get list of rows
		String[] rows;
		if ((parameterMap.get("scoreData") != null)
				&& (parameterMap.get("scoreData").length() > 0)) {
			// data from form field has higher priority
			rows = parameterMap.get("scoreData").split("\\n+");
		} else {
			// data from uploaded file
			rows = parameterMap.get("uploadFormElement").split("\\n+");
		}

		// get list of column headings
		String[] colHeaders = rows[0].split("\t");

		// TODO check list of existing columns

		// add a column for each score set
		StringBuffer sb = new StringBuffer("ALTER TABLE `"
				+ sanitizedScoreTableName + "` ADD COLUMN (");
		for (int i = 1; i < colHeaders.length; i++) {
			// the first position is used for the concept
			String header = sanitizeString(colHeaders[i]);
			header = header.replaceAll(" ", "_");

			sb.append(" " + header + " FLOAT(6,3) DEFAULT 0 ,");
		}
		sb.deleteCharAt(sb.lastIndexOf(","));

		sb.append(")");

		s_custom.execute(sb.toString());

		// INSERT rows into table for score data using prepared statement

		sb = new StringBuffer("INSERT INTO `" + sanitizedScoreTableName
				+ "` (concept,uploadedConcept,");

		for (int i = 1; i < colHeaders.length; i++) {
			// the first position is used for the concept
			String header = sanitizeString(colHeaders[i]);
			header = header.replaceAll(" ", "_");

			sb.append(header + ",");
		}
		sb.deleteCharAt(sb.lastIndexOf(","));
		sb.append(") VALUES (?,?,");

		for (int i = 1; i < colHeaders.length; i++) {
			// the first position is used for the concept
			sb.append("?,");
		}
		sb.deleteCharAt(sb.lastIndexOf(","));
		sb.append(")");

		PreparedStatement ps = con_custom.prepareStatement(sb.toString());

		// get data from each row

		HashSet<String> uploadedConceptsHashMap = new HashSet<String>();
		String[] fields;
		for (int rowNum = 1; rowNum < rows.length; rowNum++) {
			fields = rows[rowNum].split("\t");
			if (fields.length == colHeaders.length) {
				// write the fields to DB
				String concept = fields[0];

				if (concept.length() > 250) {
					concept = sanitizeString(concept.substring(0, 249));
				}

				// Due to inconsistent capitalization of paradigm output,
				// include this hack to keep abstract and concept names in upper
				// case.
				if (concept.toLowerCase().endsWith("(complex)")
						|| concept.toLowerCase().endsWith("(abstract)")
						|| concept.toLowerCase().endsWith("(family)")
						|| concept.toLowerCase().endsWith("(miRNA)")
						|| concept.toLowerCase().endsWith("(rna)")) {
					ps.setString(1, concept.toUpperCase());
				} else {
					ps.setString(1, concept);
				}

				ps.setString(2, concept);
				uploadedConceptsHashMap.add(concept);
				for (int i = 1; i < fields.length; i++) {
					Float score;
					try {
						score = Float.parseFloat(sanitizeString(fields[i]));
					} catch (NumberFormatException e) {
						score = Float.parseFloat("0");
					}
					ps.setFloat(i + 2, score);
				}
				try {
					ps.execute();
				} catch (Exception e) {
					String warning = ps.getWarnings().toString();
					System.out.println("warning: " + warning);
				}
				ps.clearParameters();
			}
		}

		// using alias table, map uploaded concepts where possible
		String speciesID = parameterMap.get("speciesID");
		if (speciesID == null) {
			speciesID = "9606";
		}

		sb = new StringBuffer();

		sb.append("UPDATE `" + sanitizedScoreTableName + "` AS left_table ");
		sb.append("LEFT JOIN `" + aliasTableNameHash.get(speciesID)
				+ "` AS right_table ");
		sb.append("ON left_table.uploadedConcept = right_table.alias ");
		sb.append("SET left_table.concept = right_table.identifier ");
		sb.append("WHERE right_table.identifier IS NOT NULL");

		s_custom.execute(sb.toString());

		// create index on concept column
		sql = "CREATE INDEX `concept_idx` ON " + sanitizedScoreTableName
				+ " (concept)";
		s_custom.execute(sql);

		// con.commit();
		s_custom.close();
		ps.close();
		con_custom.close();
	}

	/**
	 * Get the name of a score table.
	 * 
	 * @param userID
	 * @return
	 */
	private static String getSavedScoreTableName(String userID) {
		return UPLOADED_SCORES_TABLE_NAME_PREFIX + userID;
	}

	/**
	 * Get the name of a prefilter track table.
	 * 
	 * @param userID
	 * @return
	 */
	private static String getPrefilterTableName(String userID) {
		return PREFILTER_TABLE_NAME_PREFIX + userID;
	}

	/**
	 * Get the saved score information for the specified user
	 * 
	 * @param userID
	 * @return
	 * @throws SQLException
	 * @throws JSONException
	 */
	private JSONObject getSavedScoreInfo(final String userID)
			throws SQLException, JSONException {
		JSONObject scoreInfoJO = new JSONObject();

		// get the stored score set names (column names)

		String scoreTableName = getSavedScoreTableName(userID);

		Connection con = this.getMySqlConnection_custom();
		if (con == null) {
			return scoreInfoJO;
		}
		Statement s = con.createStatement();

		String sql = "DESC `" + scoreTableName + "`";
		s.execute(sql);

		ResultSet rs = s.getResultSet();

		ArrayList<String> setNamesArrayList = new ArrayList<String>();
		while (rs.next()) {
			String str = rs.getString("Field");
			if (!str.equalsIgnoreCase("ID") && !str.equalsIgnoreCase("concept")
					&& !str.equalsIgnoreCase("uploadedConcept")) {
				setNamesArrayList.add(str);
			}
		}

		// get some stats for each score set

		JSONArray scoreSetStatsJA = new JSONArray();
		DecimalFormat df = new DecimalFormat("##.###");
		for (String scoreSetName : setNamesArrayList) {
			sql = "SELECT AVG(" + scoreSetName + ") AS avg, STD("
					+ scoreSetName + ") AS std FROM `" + scoreTableName
					+ "` LIMIT 1";

			s.execute(sql);

			rs = s.getResultSet();
			while (rs.next()) {
				JSONObject jo = new JSONObject();
				jo.put("name", scoreSetName);
				jo.put("avg",
						Double.parseDouble(df.format(rs.getDouble("avg"))));
				jo.put("std",
						Double.parseDouble(df.format(rs.getDouble("std"))));

				scoreSetStatsJA.put(jo);
			}
			scoreInfoJO.put("savedScoreSets", scoreSetStatsJA);
		}

		s.close();
		con.close();

		return scoreInfoJO;
	}

	/**
	 * Check the parameter mapping for correctness. There must be
	 * uploadFormElement.
	 * 
	 * @param parameterMap
	 * @return
	 */
	private static boolean validateParamData(Map<String, String> parameterMap) {
		if (parameterMap.get("uploadFormElement").length() <= 0) {
			return false;
		}
		return true;
	}

	/**
	 * drop a user's db tables.
	 * 
	 * @param userID
	 */
	private void deleteUserTables(final String userID) {
		try {
			Connection con = getMySqlConnection_custom();
			Statement s = con.createStatement();
			String sql = "DROP TABLE IF EXISTS `"
					+ getSavedScoreTableName(userID) + "`";
			s.execute(sql);

			sql = "DROP TABLE IF EXISTS `" + getPrefilterTableName(userID)
					+ "`";
			s.execute(sql);

			s.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
