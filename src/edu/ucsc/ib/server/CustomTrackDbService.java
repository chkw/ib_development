package edu.ucsc.ib.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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

/**
 * Servlet to handle custom track data.
 * 
 * @author Chris
 * 
 */
public class CustomTrackDbService extends DatabaseService {
	public static final String CUSTOM_TRACKLIST_TABLE_NAME = "uploadTracklist";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1715821172847469525L;

	/**
	 * Constructor for this service. Calls DatabaseService constructor.
	 */
	public CustomTrackDbService() {
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

		HttpSession session = InitSession(req);
		String userID = session.getId();

		if (!sessionHasAttribute(session, "customTableCleaner")) {

			session.setAttribute("customTableCleaner",
					new HttpSessionBindingListener() {

						/**
						 * Does nothing. Required by interface.
						 */
						public void valueBound(HttpSessionBindingEvent event) {
						}

						/**
						 * Clean up the custom track databases for session.
						 */
						public void valueUnbound(HttpSessionBindingEvent event) {
							try {
								cleanTables(event.getSession().getId());
							} catch (SQLException e) {
								e.printStackTrace();
							}
						}
					});
		}
		;

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/customTrackDB/test

			try {
				super.writeTextResponse("success? " + testConnection(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("clean")) {
			// test with:
			// http://localhost:8080/ib/data/customTrackDB/clean

			try {
				this.cleanTables(userID);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("submit")) {
			// test with:
			// http://localhost:8080/ib/data/customTrackDB/submit

			try {
				this.submitTrackData(req, resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("trackList")) {
			// test with:
			// http://localhost:8080/ib/data/customTrackDB/trackList

			try {
				super.writeTextResponse(this.getTrackList(userID), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("trackAnnot")) {
			// test with:
			// http://localhost:8080/ib/data/customTrackDB/trackAnnot?tracks=yeast_biogrid_compendium,huttenhower06,oshea03

			String csvTracks = req.getParameter("tracks");

			try {
				super.writeTextResponse(this.getTrackAnnotation(csvTracks),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("edges")) {
			// test with:
			// http://localhost:8080/ib/data/customTrackDB/edges?trackName=Tachibana05_16197558&biodeList=6712,79109,3936,26072,51696,146988,57573,4153,8434,3976

			String trackName = req.getParameter("trackName");
			String csvBiodeList = req.getParameter("biodeList");

			try {
				super.writeTextResponse(
						this.getEdges(trackName, "", csvBiodeList), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("newEdges")) {
			// test with:
			// http://localhost:8080/ib/data/customTrackDB/newEdges?trackName=Tachibana05_16197558&biodeList1=6712,3936,26072,51696,146988,4153&biodeList2=55709,3976,64839

			String trackName = req.getParameter("trackName");
			String csvBiodeList1 = req.getParameter("biodeList1");
			String csvBiodeList2 = req.getParameter("biodeList2");

			try {
				super.writeTextResponse(
						this.getEdges(trackName, csvBiodeList1, csvBiodeList2),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("neighbors")) {
			// test with:
			// http://localhost:8080/ib/data/customTrackDB/neighbors?trackList=trus05_15902297,Tachibana05_16197558&biodeList=6712,3936,26072,51696,146988,4153

			String csvTrackList = req.getParameter("trackList");
			String csvBiodeList = req.getParameter("biodeList");

			try {
				super.writeTextResponse(
						this.getNeighbors(csvTrackList, csvBiodeList), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// nothing to do !
		}
	}

	/**
	 * Deletes a user's custom track tables and removes the rows from the
	 * tracklist.
	 * 
	 * @param userID
	 * @throws SQLException
	 */
	private void cleanTables(String userID) throws SQLException {
		Connection con = getMySqlConnection_custom();
		if (con != null) {
			// find which tables need to delete
			String sql = "SELECT name FROM " + CUSTOM_TRACKLIST_TABLE_NAME
					+ " WHERE userid=?";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, userID);
			ps.execute();
			ps.clearParameters();
			ResultSet rs = ps.getResultSet();

			ArrayList<String> trackNames = new ArrayList<String>();
			while (rs.next()) {
				trackNames.add(rs.getString("name"));
			}

			// delete tables
			Statement s;
			for (String tableName : trackNames) {
				sql = "DROP TABLE IF EXISTS `" + tableName + "`";
				s = con.createStatement();
				s.execute(sql);
			}

			// DELETE rows from tracklist
			sql = "DELETE FROM " + CUSTOM_TRACKLIST_TABLE_NAME
					+ " WHERE userid=?";

			ps = con.prepareStatement(sql);
			ps.setString(1, userID);
			ps.execute();
			ps.clearParameters();
		}
		con.close();
	}

	/**
	 * Wrapper method for receiving request to submit custom track data and
	 * writing it to database tables.
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException
	 * @throws JSONException
	 * @throws SQLException
	 */
	private void submitTrackData(HttpServletRequest req,
			HttpServletResponse resp) throws IOException, JSONException,
			SQLException {
		JSONObject resultJO = new JSONObject();

		HttpSession session = req.getSession(true);
		String userID = session.getId();

		// Check that we have a file upload request
		if (ServletFileUpload.isMultipartContent(req)) {

			List<FileItem> items = parseMultipartRequest(req);

			Map<String, String> parameterMap = getParamaterMapping(items);

			if (validateParamData(parameterMap)) {
				// write data to DB
				writeCustomTrackToDB(userID, parameterMap);
				resultJO.put("submitSuccess", true);
			} else {
				// don't write anything to DB ... generate message for client
				resultJO.put("submitSuccess", false);
			}
		}

		writeTextResponse(resultJO.toString(), resp);
	}

	/**
	 * Write custom track data to database tables.
	 * 
	 * @param userID
	 * @param parameterMap
	 * @param js
	 * @return
	 * @throws JSONException
	 * @throws SQLException
	 */
	private void writeCustomTrackToDB(String userID,
			Map<String, String> parameterMap) throws SQLException {

		// String trackName = parameterMap.get("trackName");
		String trackName = makeCustomTrackName(parameterMap.get("trackName"),
				userID);
		String desc = parameterMap.get("trackDescription");
		String color = parameterMap.get("color");
		String element1_type = parameterMap.get("element1_type");
		String element2_type = parameterMap.get("element2_type");

		// TODO data format
		String dataFormat = "tab";
		if (parameterMap.containsKey("dataFormat")) {
			dataFormat = parameterMap.get("dataFormat");
		}

		Connection con = getMySqlConnection_custom();
		if (con != null) {
			createCustomTrackList(con);

			// DELETE row from tracklist if there is one that matches
			// already
			String sql = "DELETE FROM " + CUSTOM_TRACKLIST_TABLE_NAME
					+ " WHERE ( (name=?) AND (userid=?) )";

			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, trackName);
			ps.setString(2, userID);
			ps.execute();
			ps.clearParameters();

			// INSERT row into tracklist
			sql = "INSERT INTO "
					+ CUSTOM_TRACKLIST_TABLE_NAME
					+ " (name,element1_type,element2_type,description,color,userid) "
					+ "VALUES ( ?,?,?,?,?,? )";

			ps = con.prepareStatement(sql);
			ps.setString(1, trackName);
			ps.setString(2, element1_type);
			ps.setString(3, element2_type);
			ps.setString(4, desc);
			ps.setString(5, color);
			ps.setString(6, userID);
			ps.execute();
			ps.clearParameters();

			String tableName = trackName;

			// DROP pre-existing track table
			sql = "DROP TABLE IF EXISTS `" + tableName + "`";
			Statement s = con.createStatement();
			s.execute(sql);

			// CREATE table for custom track data
			sql = "CREATE TABLE IF NOT EXISTS `"
					+ tableName
					+ "` ( `ID` int(11) NOT NULL AUTO_INCREMENT, `element1` varchar(50) NOT NULL, `element2` varchar(50) NOT NULL, `score` float, PRIMARY KEY (`ID`))";

			s = con.createStatement();
			s.execute(sql);

			// INSERT rows into table for custom track data using prepared
			// statement

			sql = "INSERT INTO `" + tableName
					+ "` (element1,element2,score) VALUES (?,?,?)";
			ps = con.prepareStatement(sql);

			// get list of edges
			String[] edgeList;
			if ((parameterMap.get("trackData") != null)
					&& (parameterMap.get("trackData").length() > 0)) {
				// track data from form field has higher priority
				edgeList = parameterMap.get("trackData").split("\\n+");
			} else {
				// track data from uploaded file
				edgeList = parameterMap.get("uploadFormElement").split("\\n+");
			}

			// TODO collect all nodes
			HashSet<String> nodesSet = new HashSet<String>();
			for (String strEdge : edgeList) {
				String[] edgeNodes = strEdge.split("\\s+");
				if (edgeNodes.length >= 2) {
					nodesSet.add(edgeNodes[0]);
					nodesSet.add(edgeNodes[1]);
				}
			}

			// TODO find biodes for nodes

			// These IDs don't need to be mapped.
			Set<String> confirmedIdSet = AnnoDbService.checkAnnotData("9606",
					nodesSet, s);

			// want to keep just the unconfirmed ones
			nodesSet.removeAll(confirmedIdSet);

			HashMap<String, HashMap<String, String>> aliasData = AnnoDbService
					.getAliasData("9606", nodesSet, s);

			HashMap<String, String> idMap = new HashMap<String, String>();
			for (String key : aliasData.keySet()) {
				String alias = aliasData.get(key).get("alias");
				idMap.put(alias, key);
			}
			for (String id : confirmedIdSet) {
				idMap.put(id, id);
			}

			// TODO problem: ID given by user may map to multiple internal IDs.
			// Example: ERK maps to MAPK1 and EPHB2.
			// Should expand the network to include all possible mappings?
			// System.out.println(aliasData.toString());
			//
			// System.out.println(idMap.toString());

			float default_score = new Float(1);

			// for each edge, get the nodes
			String[] edgeNodes;
			for (String strEdge : edgeList) {
				edgeNodes = strEdge.split("\\s+");
				if (edgeNodes.length >= 2) {
					// write the nodes to DB
					String inputNode1 = edgeNodes[0].toUpperCase();
					String inputNode2 = edgeNodes[1].toUpperCase();

					if (idMap.containsKey(inputNode1)) {
						inputNode1 = idMap.get(inputNode1);
					}

					if (idMap.containsKey(inputNode2)) {
						inputNode2 = idMap.get(inputNode2);
					}

					if (edgeNodes.length >= 3) {
						// 3rd column may be a score
						try {
							ps.setFloat(3, new Float(edgeNodes[2]));
						} catch (NumberFormatException e) {
							ps.setFloat(3, default_score);
						}
					} else {
						ps.setFloat(3, default_score);
					}

					ps.setString(1, inputNode1);
					ps.setString(2, inputNode2);
					ps.execute();
					ps.clearParameters();
				}
			}

			// con.commit();
			ps.close();

			con.close();
		}
	}

	/**
	 * Get a name for the custom track.
	 * 
	 * @param submittedTrackName
	 * @param userID
	 * @return
	 */
	private static String makeCustomTrackName(String submittedTrackName,
			String userID) {
		// String str = sanitizeString(submittedTrackName) + "_for_" + userID;
		String str = "uploadTrack" + userID + "_xx_"
				+ sanitizeString(submittedTrackName);
		// int strLength = str.length();
		// if (strLength > 99) {
		// return str.substring(0, 99);
		// } else {
		// return str;
		// }
		return str;
	}

	/**
	 * Check the parameter mapping for correctness. There must be a trackName
	 * and also either trackData or uploadFormElement.
	 * 
	 * @param parameterMap
	 * @return
	 */
	private boolean validateParamData(Map<String, String> parameterMap) {
		if (parameterMap.get("trackName").length() < 0) {
			return false;
		}

		if ((parameterMap.get("trackData").length() <= 0)
				&& (parameterMap.get("uploadFormElement").length() <= 0)) {
			return false;
		}
		return true;
	}

	/**
	 * Get track annotations from MySQL database "tracklist" table
	 * 
	 * @param csvTracks
	 * @return
	 */
	private String getTrackAnnotation(String csvTracks) {
		JSONObject resultJO = new JSONObject();

		try {
			resultJO.put("tracks", csvTracks);

			JSONArray trackAnnotsJA = new JSONArray();
			resultJO.put("trackAnnotations", trackAnnotsJA);

			String mySqlList = super.csvToMySqlList(super
					.sanitizeString(csvTracks));

			Connection con = getMySqlConnection_custom();
			String sql = "SELECT DISTINCT * FROM "
					+ CUSTOM_TRACKLIST_TABLE_NAME + " AS t WHERE name IN ("
					+ mySqlList + ")";

			Statement s = con.createStatement();
			s.execute(sql);

			ResultSet rs = s.getResultSet();

			ResultSetMetaData metaData = rs.getMetaData();
			String colname;
			while (rs.next()) {
				JSONObject annotJO = new JSONObject();
				trackAnnotsJA.put(annotJO);

				// get all columns selected
				for (int i = 1; i <= metaData.getColumnCount(); i++) {
					colname = metaData.getColumnName(i);
					annotJO.put(colname, rs.getString(colname));
				}
			}

			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return resultJO.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return resultJO.toString();
		}

		return resultJO.toString();
	}

	/**
	 * Create the custom tracklist table if it doesn't exist. Connection is not
	 * closed.
	 * 
	 * @param connection
	 */
	private void createCustomTrackList(final Connection connection) {
		StringBuffer sb = new StringBuffer();
		sb.append("CREATE TABLE IF NOT EXISTS `" + CUSTOM_TRACKLIST_TABLE_NAME
				+ "` (");
		sb.append("`ID` int(11) NOT NULL DEFAULT '0',");
		sb.append("`name` varchar(250) NOT NULL,");
		sb.append("`element1_type` varchar(50) DEFAULT NULL,");
		sb.append("`element2_type` varchar(50) DEFAULT NULL,");
		sb.append("`directional` varchar(10) DEFAULT NULL,");
		sb.append("`description` varchar(250) DEFAULT NULL,");
		sb.append("`NCBI_species` varchar(10) DEFAULT NULL,");
		sb.append("`datatype` varchar(20) DEFAULT NULL,");
		sb.append("`category` varchar(20) DEFAULT NULL,");
		sb.append("`color` varchar(20) DEFAULT NULL,");
		sb.append("`num_links` int(11) DEFAULT NULL,");
		sb.append("`num_elements` int(11) DEFAULT NULL,");
		sb.append("`userid` varchar(50) NOT NULL,");
		sb.append("`inserted_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,");
		sb.append("`custom` tinyint(1) DEFAULT '1' ");
		sb.append(")");

		try {
			Statement statement = connection.createStatement();
			statement.execute(sb.toString());
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the list of tracks for a userID.
	 * 
	 * @param userID
	 * @return String representation of JSON object
	 */
	private String getTrackList(String userID) {
		JSONObject resultJO = new JSONObject();
		try {
			resultJO.put("userID", userID);

			JSONArray tracksJA = new JSONArray();
			resultJO.put("tracks", tracksJA);

			Connection con = getMySqlConnection_custom();

			createCustomTrackList(con);

			if (con != null) {
				String sql = "SELECT DISTINCT * FROM "
						+ CUSTOM_TRACKLIST_TABLE_NAME + " AS t "
						+ "WHERE t.userid=? ORDER BY t.name ASC";

				PreparedStatement ps = con.prepareStatement(sql);
				ps.setString(1, userID);
				ps.execute();

				ResultSet rs = ps.getResultSet();
				ps.clearParameters();

				ResultSetMetaData metaData = rs.getMetaData();
				String colname;
				while (rs.next()) {

					// get all columns selected
					JSONObject trackJO = new JSONObject();
					tracksJA.put(trackJO);
					for (int i = 1; i <= metaData.getColumnCount(); i++) {
						colname = metaData.getColumnName(i);
						trackJO.put(colname, rs.getString(colname));
					}
				}

				con.close();
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
			return resultJO.toString();
		} catch (SQLException e) {
			e.printStackTrace();
			return resultJO.toString();
		}

		return resultJO.toString();
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
	private String getEdges(final String trackName, final String csvBiodeList1,
			String csvBiodeList2) {
		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		try {
			resultJO.put("track", trackName);
			resultJO.put("biodeList1", csvBiodeList1);
			resultJO.put("biodeList2", csvBiodeList2);

			JSONArray edgesJA = new JSONArray();
			resultJO.put("edges", edgesJA);

			String mySqlList1 = super.csvToMySqlList(super
					.sanitizeString(csvBiodeList1))
					+ ","
					+ super.csvToMySqlList(super.sanitizeString(csvBiodeList2));
			String mySqlList2 = super.csvToMySqlList(super
					.sanitizeString(csvBiodeList2));

			Connection con = getMySqlConnection_custom();
			if (con != null) {
				String sql = "SELECT DISTINCT * FROM `"
						+ super.sanitizeString(trackName)
						+ "` AS t WHERE ((t.element1 IN (" + mySqlList1
						+ ")) AND (t.element2 IN (" + mySqlList2
						+ "))) OR ((t.element2 IN (" + mySqlList1
						+ ")) AND (t.element1 IN (" + mySqlList2 + ")))";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();
				while (rs.next()) {
					JSONObject edgeJO = new JSONObject();
					edgesJA.put(edgeJO);

					edgeJO.put("1", rs.getString("element1"));
					edgeJO.put("2", rs.getString("element2"));

					// TODO get edge weight
					try {
						edgeJO.put("weight", rs.getDouble("score"));
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}

				con.close();
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
	private String getNeighbors(String csvTrackList, String csvBiodeList) {
		JSONObject resultJO = new JSONObject();

		try {
			resultJO.put("trackList", csvTrackList);
			resultJO.put("biodeList", csvBiodeList);

			JSONArray neighborsJA = new JSONArray();
			resultJO.put("neighbors", neighborsJA);

			String[] tracks = csvTrackList.split(",");

			String mySqlList = csvToMySqlList(sanitizeString(csvBiodeList));

			HashMap<String, String> biodesMap = new HashMap<String, String>();

			Connection con = getMySqlConnection_custom();
			for (String track : tracks) {
				String sql = "SELECT DISTINCT * FROM `" + sanitizeString(track)
						+ "` AS t WHERE (( t.element1 IN (" + mySqlList
						+ ") ) OR ( t.element2 IN (" + mySqlList + ") ))";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();
				while (rs.next()) {
					biodesMap.put(rs.getString("element1"), "");
					biodesMap.put(rs.getString("element2"), "");
				}
			}
			con.close();
			for (String biode : biodesMap.keySet().toArray(new String[0])) {
				neighborsJA.put(biode);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return resultJO.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return resultJO.toString();
		}

		return resultJO.toString();
	}
}
