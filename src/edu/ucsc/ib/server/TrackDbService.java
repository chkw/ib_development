package edu.ucsc.ib.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Servlet to get track data from database.
 * 
 * @author Chris
 * 
 */
public class TrackDbService extends DatabaseService {
	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 6823473387367784106L;

	/**
	 * Constructor for this service. Calls DatabaseService constructor.
	 */
	public TrackDbService() {
		super();
	}

	/**
	 * Handle GET request.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		// System.out.println("TrackDbService.doGet");
		// System.out.println("--handing over to TrackDbService.doPost");

		String path = req.getPathInfo();

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/trackdb/test

			try {
				writeTextResponse("success? " + testConnection(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("trackList")) {
			// test with:
			// http://localhost:8080/ib/data/trackdb/trackList?organism=6239

			String NCBI_organism = req.getParameter("organism");

			try {
				writeTextResponse(getTrackList(NCBI_organism), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("trackAnnot")) {
			// test with:
			// http://localhost:8080/ib/data/trackdb/trackAnnot?tracks=yeast_biogrid_compendium,huttenhower06,oshea03

			String csvTracks = req.getParameter("tracks");

			try {
				writeTextResponse(getTrackAnnotation(csvTracks), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("edges")) {
			// test with:
			// http://localhost:8080/ib/data/trackdb/edges?trackName=Tachibana05_16197558&biodeList=6712,79109,3936,26072,51696,146988,57573,4153,8434,3976

			String trackName = req.getParameter("trackName");
			String csvBiodeList = req.getParameter("biodeList");

			try {
				writeTextResponse(getEdges(trackName, "", csvBiodeList), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("allEdges")) {
			// test with:
			// http://localhost:8080/ib/data/trackdb/allEdges?trackName=Tachibana05_16197558

			String trackName = req.getParameter("trackName");

			try {
				writeTextResponse(getAllEdges(trackName), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("newEdges")) {
			// test with:
			// http://localhost:8080/ib/data/trackdb/newEdges?trackName=Tachibana05_16197558&biodeList1=6712,3936,26072,51696,146988,4153&biodeList2=55709,3976,64839

			String trackName = req.getParameter("trackName");
			String csvBiodeList1 = req.getParameter("biodeList1");
			String csvBiodeList2 = req.getParameter("biodeList2");

			try {
				writeTextResponse(
						getEdges(trackName, csvBiodeList1, csvBiodeList2), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("crawlEdges")) {
			// test with:
			// http://localhost:8080/ib/data/trackdb/crawlEdges?trackName=Tachibana05_16197558&biodeList1=6712,3936,26072,51696,146988,4153&biodeList2=55709,3976,64839

			String trackName = req.getParameter("trackName");
			String csvBiodeList1 = req.getParameter("biodeList1");
			String csvBiodeList2 = req.getParameter("biodeList2");
			if (csvBiodeList2 == null) {
				csvBiodeList2 = "";
			}

			try {
				writeTextResponse(
						getCrawlEdges(trackName, csvBiodeList1, csvBiodeList2),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("neighbors")) {
			// test with:
			// http://localhost:8080/ib/data/trackdb/neighbors?trackList=UCSC_Superpathway&biodeList=BRCA1,tp53,egfr,mdm2,erbb2,erbb3,akt1

			String csvTrackList = req.getParameter("trackList");
			String csvBiodeList = req.getParameter("biodeList");

			try {
				writeTextResponse(getNeighbors(csvTrackList, csvBiodeList),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("commonNeighbors")) {
			// test with:
			// http://localhost:8080/ib/data/trackdb/commonNeighbors?trackList=trus05_15902297,Tachibana05_16197558&biodeList=6712,3936,26072,51696,146988,4153

			String csvTrackList = req.getParameter("trackList");
			String csvBiodeList = req.getParameter("biodeList");

			try {
				writeTextResponse(
						getCommonNeighbors(csvTrackList, csvBiodeList), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("shortestPath")) {
			// test with:
			// http://localhost:8080/ib/data/trackdb/shortestPath?trackList=UCSC_Superpathway,human_biogrid&origins=4609,1499&destinations=2064,324
			// http://localhost:8080/ib/data/trackdb/shortestPath?trackList=UCSC_Superpathway&origins=1499&destinations=2064
			// http://localhost:8080/ib/data/trackdb/shortestPath?trackList=human_biogrid&origins=1111&destinations=55743
			// http://localhost:8080/ib/data/trackdb/shortestPath?trackList=human_biogrid,Trus05_15902297,Tachibana05_16197558&origins=1029&destinations=699

			String csvTrackList = req.getParameter("trackList");
			String csvOrigins = req.getParameter("origins");
			String csvDestinations = req.getParameter("destinations");

			try {
				writeTextResponse(
						getShortestPath(csvTrackList, csvOrigins,
								csvDestinations), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// nothing to do !
		}

	}

	/**
	 * Handle POST request.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		System.out.println("TrackDbService.doPost");
		System.out.println("requestURI is:\t" + req.getRequestURI());

		// TrackDbService.printHeaderInfo(req);
		// TrackDbService.printReqParams(req);

		String path = req.getPathInfo();

		if (path.endsWith("crawlEdges")) {

			String trackName = req.getHeader("trackName");
			String csvBiodeList1 = req.getHeader("biodeList1");
			String csvBiodeList2 = req.getHeader("biodeList2");

			if (csvBiodeList2 == null) {
				csvBiodeList2 = "";
			}

			// String[] stringArray = csvBiodeList1.split(",");
			// System.out.println("size list 1: " + stringArray.length);
			// stringArray = csvBiodeList2.split(",");
			// System.out.println("size list 2: " + stringArray.length);

			try {
				super.writeTextResponse(
						getCrawlEdges(trackName, csvBiodeList1, csvBiodeList2),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// nothing to do !
		}

	}

	// TODO ////////////////////////////////////////

	private static String getShortestPath(final String csvTrackList,
			final String csvOriginList, final String csvDestinationList)
			throws JSONException {
		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		JSONObject queryJO = new JSONObject();
		resultJO.put("query", queryJO);

		String[] origins = csvOriginList.split(",");
		String[] destinations = csvDestinationList.split(",");

		JSONArray originJA = new JSONArray();
		queryJO.put("origins", originJA);
		for (String origin : origins) {
			originJA.put(origin);
		}

		JSONArray destinationJA = new JSONArray();
		queryJO.put("destinations", destinationJA);
		for (String destination : destinations) {
			destinationJA.put(destination);
		}

		JSONArray networksJA = new JSONArray();
		queryJO.put("networks", networksJA);

		// get network data
		NetworkLinkTreeMap links = new NetworkLinkTreeMap();
		for (String trackName : csvTrackList.split(",")) {
			if (!trackName.equalsIgnoreCase("")) {
				networksJA.put(trackName);
				if (trackName.equalsIgnoreCase("UCSC_Superpathway")) {
					getRelationsAsNetworkLinkTreeMap(trackName, links);
				} else {
					getAllEdgesAsNetworkLinkTreeMap(trackName, links);
				}
			}
		}

		// load network into Dijkstras
		DijkstrasShortestPath dsp = new DijkstrasShortestPath(links);

		// get all origin/destination pairs
		JSONArray pathsJA = new JSONArray();
		resultJO.put("shortestPaths", pathsJA);
		for (String origin : origins) {
			for (String destination : destinations) {
				JSONObject pathJO = dsp.returnVerticesOnShortestPath(origin,
						destination);
				pathsJA.put(pathJO);
			}
		}

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);
		return jo.toString();
	}

	/**
	 * Get network links from the specified track where at least one of the
	 * elements is in the list. Also, if a second list is specified, edges in
	 * which one of the elements are in the second list will be ignored.
	 * 
	 * @param trackName
	 * @param csvBiodeQueryList
	 * @param csvBiodeStopList
	 * @return
	 * @throws JSONException
	 */
	private static String getCrawlEdges(String trackName,
			String csvBiodeQueryList, String csvBiodeStopList)
			throws JSONException {

		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		resultJO.put("track", trackName);
		resultJO.put("biodeList1", csvBiodeQueryList);
		resultJO.put("biodeList2", csvBiodeStopList);

		JSONArray edgesJA = new JSONArray();
		resultJO.put("edges", edgesJA);

		String mySqlQueryList = csvToMySqlList(sanitizeString(csvBiodeQueryList));

		String mySqlStopList = csvToMySqlList(sanitizeString(csvBiodeStopList));

		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				StringBuffer sb = new StringBuffer("SELECT DISTINCT * FROM `");
				sb.append(sanitizeString(trackName));
				sb.append("` AS t WHERE ((t.element1 IN (");
				sb.append(mySqlQueryList);
				sb.append(")) OR (t.element2 IN (");
				sb.append(mySqlQueryList);
				sb.append(")))");

				if (mySqlStopList.length() > 0) {
					sb.append(" AND NOT ((t.element1 IN (");
					sb.append(mySqlStopList);
					sb.append(")) AND (t.element2 IN (");
					sb.append(mySqlStopList);
					sb.append(")))");
				}

				Statement s = con.createStatement();
				s.execute(sb.toString());

				ResultSet rs = s.getResultSet();
				while (rs.next()) {
					JSONObject edgeJO = new JSONObject();
					edgesJA.put(edgeJO);

					edgeJO.put("1", rs.getString("element1"));
					edgeJO.put("2", rs.getString("element2"));
				}

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
				error = "sql error";
				JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
						error, id);
				return jo.toString();
			}
		}

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);
		return jo.toString();
	}

	/**
	 * Check with database if named track is directional.
	 * 
	 * @param trackName
	 * @return
	 * @throws JSONException
	 */
	private static boolean isDirectional(final String trackName)
			throws JSONException {

		JSONObject resultJO = new JSONObject(getTrackAnnotation(trackName));
		boolean result = resultJO.getJSONArray("trackAnnotations")
				.getJSONObject(0).getBoolean("directional");

		return result;
	}

	/**
	 * Get track annotations from MySQL database "tracklist" table
	 * 
	 * @param csvTracks
	 * @return
	 * @throws JSONException
	 */
	private static String getTrackAnnotation(String csvTracks)
			throws JSONException {
		JSONStringer js = new JSONStringer();
		js.object();

		String mySqlList = csvToMySqlList(sanitizeString(csvTracks));

		js.key("tracks").value(csvTracks);
		js.key("trackAnnotations").array();

		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT * FROM tracklist AS t WHERE name IN ("
						+ mySqlList + ")";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				ResultSetMetaData metaData = rs.getMetaData();
				String colname;
				while (rs.next()) {
					js.object();

					// get all columns selected
					for (int i = 1; i <= metaData.getColumnCount(); i++) {
						colname = metaData.getColumnName(i);
						js.key(colname).value(rs.getString(colname));
					}

					js.endObject();
				}

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		js.endArray();
		js.endObject();

		return js.toString();
	}

	/**
	 * Get the list of tracks for an organism.
	 * 
	 * @param NCBI_organism
	 * @return String representation of JSON object
	 * @throws JSONException
	 */
	private static String getTrackList(String NCBI_organism)
			throws JSONException {
		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("tracks").array();

		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT * FROM tracklist AS t "
						+ "WHERE NCBI_species=? ORDER BY datatype,name ASC";

				PreparedStatement ps = con.prepareStatement(sql);
				ps.setString(1, NCBI_organism);
				ps.execute();
				ps.clearParameters();

				ResultSet rs = ps.getResultSet();

				ResultSetMetaData metaData = rs.getMetaData();
				String colname;

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
						if (colname.equalsIgnoreCase("date_added")) {
							// skip - timestamp sql type
							continue;
						}
						js.key(colname).value(rs.getString(colname));
					}

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
		js.endArray();
		js.endObject();

		return js.toString();
	}

	/**
	 * Get a track data for edges spanning between 2 sets of biodes, as well the
	 * "inter-edges" within the second set.
	 * 
	 * @param trackName
	 * @param csvBiodeList1
	 * @param csvBiodeList2
	 * @return String representation of JSON object
	 * @throws JSONException
	 */
	private static String getEdges(String trackName, String csvBiodeList1,
			String csvBiodeList2) throws JSONException {

		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		resultJO.put("track", trackName);
		resultJO.put("biodeList1", csvBiodeList1);
		resultJO.put("biodeList2", csvBiodeList2);

		JSONArray edgesJA = new JSONArray();
		resultJO.put("edges", edgesJA);

		String mySqlList1 = csvToMySqlList(sanitizeString(csvBiodeList1) + ","
				+ sanitizeString(csvBiodeList2));
		String mySqlList2 = csvToMySqlList(sanitizeString(csvBiodeList2));

		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT * FROM `"
						+ sanitizeString(trackName)
						+ "` AS t WHERE ((t.element1 IN (" + mySqlList1
						+ ")) AND (t.element2 IN (" + mySqlList2
						+ "))) OR ((t.element2 IN (" + mySqlList1
						+ ")) AND (t.element1 IN (" + mySqlList2 + ")))";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				// check if there is relation or score data in ResultSet
				ResultSetMetaData rsmd = rs.getMetaData();
				boolean hasRelationCol = false;
				boolean hasScoreCol = false;
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					if (rsmd.getColumnName(i).equalsIgnoreCase("relation")) {
						hasRelationCol = true;
					} else if (rsmd.getColumnName(i).equalsIgnoreCase("score")) {
						hasScoreCol = true;
					}
				}

				while (rs.next()) {

					JSONObject edgeJO = new JSONObject();

					edgeJO.put("1", rs.getString("element1"));
					edgeJO.put("2", rs.getString("element2"));

					if (hasRelationCol) {
						edgeJO.put("relation", rs.getString("relation"));
					}

					if (hasScoreCol) {
						edgeJO.put("score", rs.getDouble("score"));
					}

					edgesJA.put(edgeJO);
				}

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);

		return jo.toString();
	}

	/**
	 * For a given list of tracks, find all biodes that are neighbors of all the
	 * query biodes.
	 * 
	 * @param csvTrackList
	 * @param csvBiodeList
	 * @return
	 */
	private static String getCommonNeighbors(String csvTrackList,
			String csvBiodeList) throws JSONException {

		Collection<String> queryIDcollection = Arrays.asList(csvBiodeList
				.split(","));

		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		resultJO.put("trackList", csvTrackList);
		resultJO.put("biodeList", csvBiodeList);

		JSONArray neighborsJA = new JSONArray();
		resultJO.put("neighbors", neighborsJA);

		String[] tracks = csvTrackList.split(",");

		String mySqlList = csvToMySqlList(sanitizeString(csvBiodeList));

		/**
		 * Store link data here. The key is a query ID. The value is a
		 * Collection of its neighbor IDs.
		 */
		HashMap<String, HashSet<String>> neighborsMap = new HashMap<String, HashSet<String>>();

		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				for (String track : tracks) {
					String sql = "SELECT DISTINCT * FROM `"
							+ sanitizeString(track)
							+ "` AS t WHERE (( t.element1 IN (" + mySqlList
							+ ") ) OR ( t.element2 IN (" + mySqlList + ") ))";

					Statement s = con.createStatement();
					s.execute(sql);

					ResultSet rs = s.getResultSet();
					while (rs.next()) {
						String element1 = rs.getString("element1");
						String element2 = rs.getString("element2");
						if (!element1.equalsIgnoreCase(element2)
								&& (!queryIDcollection.contains(element1) || !queryIDcollection
										.contains(element2))) {
							// ignore self links
							// ignore links within query set

							// this part should record links, not biodes
							String query;
							String neighbor;
							if (queryIDcollection.contains(element1)) {
								query = element1;
								neighbor = element2;
							} else {
								query = element2;
								neighbor = element1;
							}

							if (neighborsMap.containsKey(query)) {
								neighborsMap.get(query).add(neighbor);
							} else {
								neighborsMap.put(query, new HashSet<String>());
								neighborsMap.get(query).add(neighbor);
							}

						} else {
							// System.out.println("ignore link: " + element1
							// + " - " + element2);
						}
					}
				}
				con.close();

				// figure out which biodes to return
				// TODO this part is kind of a hack. Oh well, it seems to work.

				if (neighborsMap.size() > 0) {

					HashSet<String> commonNeighborsSet = new HashSet<String>();

					String smallestNeighborhood = "";
					int smallestNeighborhoodSize = 99999999;
					for (String queryID : neighborsMap.keySet()) {
						int currentSize = neighborsMap.get(queryID).size();
						if (currentSize < smallestNeighborhoodSize) {
							smallestNeighborhoodSize = currentSize;
							smallestNeighborhood = queryID;
						}
					}

					// System.out.println(smallestNeighborhood + " is size "
					// + smallestNeighborhoodSize);

					commonNeighborsSet = neighborsMap.get(smallestNeighborhood);

					int querySize = queryIDcollection.size();
					for (String possiblePrintID : commonNeighborsSet) {
						int neighborCount = 0;
						for (String queryID : neighborsMap.keySet()) {
							if (neighborsMap.get(queryID).contains(
									possiblePrintID)) {
								neighborCount++;
							}
						}
						if (neighborCount == querySize) {
							neighborsJA.put(possiblePrintID);
						}
					}

				} else {
					// do nothing - no results from MySQL query
					// System.out.println("neighborsMap is empty");
				}

			} catch (SQLException e) {
				e.printStackTrace();
				error = "sql error";
				JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
						error, id);

				return jo.toString();
			}
		}

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);

		return jo.toString();
	}

	/**
	 * For a given list of tracks, find all biodes that are neighbors of at
	 * least one node in the query list of biodes.
	 * 
	 * @param csvTrackList
	 * @param csvBiodeList
	 * @return
	 */
	private static String getNeighbors(String csvTrackList, String csvBiodeList)
			throws JSONException {

		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		resultJO.put("trackList", csvTrackList);
		resultJO.put("biodeList", csvBiodeList);

		String[] tracks = csvTrackList.split(",");

		String mySqlList = csvToMySqlList(sanitizeString(csvBiodeList));

		HashSet<String> neighborsHashSet = new HashSet<String>();

		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				for (String track : tracks) {
					String sql = "SELECT DISTINCT * FROM `"
							+ sanitizeString(track)
							+ "` AS t WHERE (( t.element1 IN (" + mySqlList
							+ ") ) OR ( t.element2 IN (" + mySqlList + ") ))";

					Statement s = con.createStatement();
					s.execute(sql);

					ResultSet rs = s.getResultSet();
					while (rs.next()) {
						neighborsHashSet.add(rs.getString("element1"));
						neighborsHashSet.add(rs.getString("element2"));
					}
					rs.close();
					s.close();
				}
				con.close();

				resultJO.put("neighbors", neighborsHashSet);
			} catch (SQLException e) {
				e.printStackTrace();
				error = "sql error";
				JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
						error, id);
				return jo.toString();
			}
		}

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);
		return jo.toString();
	}

	/**
	 * Get the network neighborhood. Results are stored in linksTreeMap with the
	 * score of Double.NaN.
	 */
	public static void getNeighborhood(final String networkName,
			final Set<String> conceptsSet, NetworkLinkTreeMap linksTreeMap) {

		DatabaseService.setToMysqlList(conceptsSet);

		String mySqlList = csvToMySqlList(sanitizeString(setToMysqlList(conceptsSet)));

		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT * FROM `"
						+ sanitizeString(networkName)
						+ "` AS t WHERE (( t.element1 IN (" + mySqlList
						+ ") ) OR ( t.element2 IN (" + mySqlList + ") ))";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();
				while (rs.next()) {
					linksTreeMap.addLink(rs.getString("element1"),
							rs.getString("element2"), Double.NaN, false);
				}
				con.close();

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get the edges of the track. Returned in the <code> edgesTreeMap </code>.
	 * 
	 * @param trackName
	 * @param preserveElementOrder
	 *            preserve the order of the elements in the edge Not closed in
	 *            this method.
	 * @param edgesTreeMap
	 *            append results to this object
	 * @param connection
	 */
	public static void getAllEdges(final String trackName,
			final boolean preserveElementOrder,
			TreeMap<String, HashSet<String>> edgesTreeMap,
			final Connection connection) {

		try {
			String sql = "SELECT t.element1,t.element2 FROM `"
					+ sanitizeString(trackName) + "` as t";

			Statement s = connection.createStatement();
			s.execute(sql);

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				String element1 = rs.getString("element1");
				String element2 = rs.getString("element2");

				if (!preserveElementOrder && element1.compareTo(element2) > 0) {
					String newElement2 = element1;
					element1 = element2;
					element2 = newElement2;
				}

				if (!edgesTreeMap.containsKey(element1)) {
					edgesTreeMap.put(element1, new HashSet<String>());
				}

				edgesTreeMap.get(element1).add(element2);
			}

			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the edges of the track.
	 * 
	 * @param trackName
	 * @param preserveElementOrder
	 *            preserve the order of the elements in the edge
	 * @param connection
	 *            Not closed in this method.
	 * @return
	 */
	public static TreeMap<String, HashSet<String>> getAllEdges(
			final String trackName, final boolean preserveElementOrder,
			final Connection connection) {
		TreeMap<String, HashSet<String>> edgesTreeMap = new TreeMap<String, HashSet<String>>();

		getAllEdges(trackName, preserveElementOrder, edgesTreeMap, connection);

		return edgesTreeMap;
	}

	/**
	 * Get all edge data for a track.
	 * 
	 * @param trackName
	 * @return String representation of JSON object
	 */
	protected static String getAllEdges(String trackName) {
		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		try {
			resultJO.put("track", trackName);

			JSONArray edgesJA = new JSONArray();
			resultJO.put("edges", edgesJA);

			Connection con = getMySqlConnection();

			TreeMap<String, HashSet<String>> edgesTreeMap = getAllEdges(
					trackName, false, con);

			con.close();

			for (String element1 : edgesTreeMap.keySet()) {
				for (String element2 : edgesTreeMap.get(element1)) {
					JSONObject edgeJO = new JSONObject();
					edgesJA.put(edgeJO);

					edgeJO.put("1", element1);
					edgeJO.put("2", element2);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			error = "SQL error";
			JSONObject jo = encodeJsonRpcResponse(resultJO, error, id);
			return jo.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			error = "JSON error";
			JSONObject jo = encodeJsonRpcResponse(resultJO, error, id);
			return jo.toString();
		}

		JSONObject jo = encodeJsonRpcResponse(resultJO, error, id);
		return jo.toString();
	}

	/**
	 * Get representation of relations in the network. Currently, all existing
	 * links are assigned a score of "1". Appends edge data to the specified
	 * NetworkLinkTreeMap. The DB table for the network must have the fields:
	 * element1, element2, relation.
	 * 
	 * @param trackName
	 * @param networkLinkTreeMap
	 */
	public static void getRelationsAsNetworkLinkTreeMap(final String trackName,
			final NetworkLinkTreeMap networkLinkTreeMap) {
		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT t.element1,t.element2,t.relation FROM `"
						+ sanitizeString(trackName) + "` as t";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();
				while (rs.next()) {
					String element1 = rs.getString("element1");
					String element2 = rs.getString("element2");
					String relation = rs.getString("relation");

					networkLinkTreeMap
							.addRelation(element1, element2, relation);
				}

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get all edge data for a track. Currently, all existing links are assigned
	 * a score of "1". Appends edge data to the specified NetworkLinkTreeMap.
	 * 
	 * @param trackName
	 * @param networkLinkTreeMap
	 */
	public static void getAllEdgesAsNetworkLinkTreeMap(final String trackName,
			final NetworkLinkTreeMap networkLinkTreeMap) {
		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				// determine directionality status of network data
				boolean isDirectional = isDirectional(sanitizeString(trackName));

				String sql = "SELECT t.element1,t.element2 FROM `"
						+ sanitizeString(trackName) + "` as t";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();
				while (rs.next()) {
					String element1 = rs.getString("element1");
					String element2 = rs.getString("element2");

					networkLinkTreeMap.addLink(element1, element2,
							Double.valueOf(1), isDirectional);
				}

				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get all edge data for a track. Currently, all existing links are assigned
	 * a score of "1".
	 * 
	 * @param trackName
	 * @return
	 */
	public static NetworkLinkTreeMap getAllEdgesAsNetworkLinkTreeMap(
			final String trackName) {
		NetworkLinkTreeMap links = new NetworkLinkTreeMap();

		getAllEdgesAsNetworkLinkTreeMap(trackName, links);

		return links;
	}

	/**
	 * Get all the elements represented in the named network. Connection is not
	 * closed in this method.
	 * 
	 * @param networkName
	 * @param con
	 * @return
	 */
	protected static HashSet<String> getAllNetworkElements(
			final String networkName, final Connection con) {
		HashSet<String> geneUniverse = new HashSet<String>();

		if (con != null) {
			try {
				// element1
				String sql = "SELECT DISTINCT t.element1 FROM `"
						+ sanitizeString(networkName) + "` as t";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();
				while (rs.next()) {
					geneUniverse.add(rs.getString("element1"));
				}

				// element2
				sql = "SELECT DISTINCT t.element2 FROM `"
						+ sanitizeString(networkName) + "` as t";

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();
				while (rs.next()) {
					geneUniverse.add(rs.getString("element2"));
				}

				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
		}

		return geneUniverse;
	}

	/**
	 * Get all the elements represented in the named network.
	 * 
	 * @param networkName
	 * @return
	 */
	protected static HashSet<String> getAllNetworkElements(
			final String networkName) {
		Connection con = getMySqlConnection();

		HashSet<String> geneUniverse = getAllNetworkElements(networkName, con);

		try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return geneUniverse;
		}
		return geneUniverse;
	}
}
