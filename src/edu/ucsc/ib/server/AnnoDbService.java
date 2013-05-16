package edu.ucsc.ib.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Servlet to get annotation data from database
 * 
 * @author Chris
 * 
 */
public class AnnoDbService extends DatabaseService {
	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 5667030428139482095L;

	/**
	 * Constructor for this service. Calls DatabaseService constructor.
	 */
	public AnnoDbService() {
		super();
	}

	/**
	 * Handle GET request. Hand over to doPost() method.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		// System.out.println("AnnoDbService.doGet");
		// System.out.println("--handing over to AnnoDbService.doPost");
		this.doPost(req, resp);
	}

	/**
	 * Handle POST request.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		// System.out.println("AnnoDbService.doPost");
		// System.out.println("requestURI is:\t" + req.getRequestURI());

		String path = req.getPathInfo();

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/annodb/test

			try {
				super.writeTextResponse("success? " + testConnection(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("annot")) {
			// test with:
			// http://localhost:8080/ib/data/annodb/annot?organism=4932&searchString=budding

			String NCBI_organism = req.getParameter("organism");
			String searchString = req.getParameter("searchString");

			try {
				super.writeTextResponse(
						searchAnnotation(NCBI_organism, searchString), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("alias")) {
			// test with:
			// http://localhost:8080/ib/data/annodb/alias?organism=4932&list=AQ500634.1,AQ500650.1,AQ500657.1,AQ500660.1,AQ500666.1,AQ500691.1,YGL196W,YGL203C,YGL204C,YGL205W,YGL206C,YGL207W,YGL208W,YGL209W

			String NCBI_organism = req.getParameter("organism");
			String csvAliasList = req.getParameter("list");

			try {
				writeTextResponse(searchAlias(NCBI_organism, csvAliasList)
						.toString(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("aliasToAnnot2")) {
			// test with:
			// http://localhost:8080/ib/data/annodb/aliasToAnnot2?organism=4932&list=AQ500634.1,AQ500650.1,AQ500657.1,AQ500660.1,AQ500666.1,AQ500691.1,YGL196W,YGL203C,YGL204C,YGL205W,YGL206C,YGL207W,YGL208W,YGL209W

			String NCBI_organism = req.getParameter("organism");
			String csvAliasList = req.getParameter("list");

			try {
				super.writeTextResponse(
						aliasToAnnotation2(NCBI_organism, csvAliasList), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("aliasToAnnot_multiSpace")) {
			// test with:
			// http://localhost:8080/ib/data/annodb/aliasToAnnot_multiSpace?list=Lepirudin,AQ500634.1,PA10040,a1bg
			String csvAliasList = req.getParameter("list");

			try {
				super.writeTextResponse(
						aliasToAnnotation_multiSpace(csvAliasList), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("bestPBlast")) {
			// test with:
			// http://localhost:8080/ib/data/annodb/bestPBlast?9606=7157,1234,6712,79109,3936,26072,51696,146988,57573,4153,8434,3976&10090=22059

			Map<String, String[]> paramHashMap = req.getParameterMap();

			// for (String key : paramHashMap.keySet()) {
			// for (String arrayVal : paramHashMap.get(key)) {
			// System.out.println(key + " : " + arrayVal);
			// }
			// }

			HashMap<String, String> queryHashMap = new HashMap<String, String>();

			for (String supportedOrg : AnnoDbService.bestBlastTableNameHash
					.keySet()) {
				if (paramHashMap.containsKey(supportedOrg)) {
					queryHashMap.put(supportedOrg,
							paramHashMap.get(supportedOrg)[0]);
				}
			}

			try {
				super.writeTextResponse(searchBestPBlast(queryHashMap), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			// nothing to do !
		}
	}

	/**
	 * Get the best pBLAST for specified genes' proteins. Returns a JSON-RPC
	 * v1.0 - compliant JSON result. No target organism needs to be specified,
	 * as all available best pBLAST are returned. ID's that did not get mapped
	 * are returned in the result as well.
	 * 
	 * @param queryHashMap
	 * @return
	 * @throws JSONException
	 * @throws SQLException
	 */
	private static String searchBestPBlast(
			final HashMap<String, String> queryHashMap) throws JSONException,
			SQLException {
		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		// get a connection
		Connection con = getMySqlConnection();
		if (con == null) {
			error = "could not get an SQL connection";

			JSONObject jo = DatabaseService.encodeJsonRpcResponse(null, error,
					id);

			return jo.toString();
		}

		for (String organism : queryHashMap.keySet()) {
			String csvIds = queryHashMap.get(organism);
			String mysqlList = DatabaseService.csvToMySqlList(csvIds);

			// skip this organism if there are no query IDs for it
			if (mysqlList.length() == 0) {
				continue;
			}

			// create a Set object to keep track of which query IDs did not get
			// result
			List<String> list = Arrays.asList(csvIds.split(","));
			Set<String> queryIdSet = new HashSet<String>(list);

			// MySQL table to use for this query.
			String pBlastTable = DatabaseService.bestBlastTableNameHash
					.get(organism);

			String sql = "SELECT DISTINCT * FROM " + pBlastTable
					+ " AS pblastTable WHERE pblastTable.query IN ("
					+ mysqlList + ")";

			Statement s = con.createStatement();
			s.execute(sql);

			ResultSet rs = s.getResultSet();

			// get column names
			ResultSetMetaData rsMetaData = rs.getMetaData();
			HashSet<String> colNames = new HashSet<String>();
			// use "1"-based indexing
			for (int col = 1; col <= rsMetaData.getColumnCount(); col++) {
				String colName = rsMetaData.getColumnName(col);
				if ((!colName.equalsIgnoreCase("ID"))
						&& (!colName.equalsIgnoreCase(organism))) {
					colNames.add(colName);
				}
			}

			// get results
			JSONArray orgResultsJA = new JSONArray();

			while (rs.next()) {
				JSONObject idResultsJO = new JSONObject();
				String queryID = rs.getString("query");
				idResultsJO.put("query", queryID);
				queryIdSet.remove(queryID);
				for (String colName : colNames) {
					String value = rs.getString(colName);
					if (value.length() != 0) {
						if (!colName.equalsIgnoreCase("query")) {
							idResultsJO.put(colName, rs.getString(colName));
						}
					}
				}
				orgResultsJA.put(idResultsJO);
			}

			// handle queries with no results
			for (String queryWithNoResult : queryIdSet) {
				JSONObject idResultsJO = new JSONObject();
				idResultsJO.put("query", queryWithNoResult);
				orgResultsJA.put(idResultsJO);
			}

			resultJO.put(organism, orgResultsJA);

		}

		// close connection
		con.close();

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);

		return jo.toString();
	}

	/**
	 * Search for annotations from multiple (unspecified) systemspaces
	 * 
	 * @param ncbi_organism
	 * @param csvAliasList
	 * @return String representation of JSON object
	 * @throws JSONException
	 */
	public static String aliasToAnnotation_multiSpace(String csvList)
			throws JSONException {

		HashSet<String> queryIdHashSet = new HashSet<String>(
				Arrays.asList(csvList.split(",")));

		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		resultJO.put("csvList", csvList);

		JSONArray organismsJA = new JSONArray();
		resultJO.put("organisms", organismsJA);

		String mySqlList = csvToMySqlList(sanitizeString(csvList));

		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				for (String organismCode : aliasTableNameHash.keySet()) {

					String aliasTableName = aliasTableNameHash
							.get(organismCode);
					String annotTableName = annotTableNameHash
							.get(organismCode);

					String sql = "SELECT DISTINCT aliasTable.identifier FROM "
							+ aliasTableName
							+ " AS aliasTable WHERE aliasTable.alias IN ("
							+ mySqlList + ")";

					Statement s = con.createStatement();
					s.execute(sql);

					ResultSet rs = s.getResultSet();

					StringBuffer sb = new StringBuffer();

					while (rs.next()) {
						sb.append("," + rs.getString("identifier"));
					}

					if (sb.length() > 1) {
						String mySqlList_for_query2 = csvToMySqlList(sb
								.toString());

						sql = "SELECT DISTINCT annoTable.identifier,annoTable.common_name,annoTable.description FROM "
								+ annotTableName
								+ " AS annoTable WHERE annoTable.identifier IN ("
								+ mySqlList_for_query2
								+ ") ORDER BY identifier,common_name ASC";

						s = con.createStatement();
						s.execute(sql);

						rs = s.getResultSet();

						JSONObject organismJO = new JSONObject();
						organismJO.put("organism", organismCode);

						JSONArray annotationsJA = new JSONArray();
						organismJO.put("annotations", annotationsJA);

						organismsJA.put(organismJO);

						while (rs.next()) {
							JSONObject annotationJO = new JSONObject();
							String identifier = rs.getString("identifier");
							annotationJO.put("ID", identifier);
							queryIdHashSet.remove(identifier);
							if (rs.getString("common_name").equalsIgnoreCase(
									"no name")) {
								annotationJO.put("common",
										rs.getString("identifier"));
							} else {
								annotationJO.put("common",
										rs.getString("common_name"));
							}
							annotationJO.put("desc",
									rs.getString("description"));
							annotationsJA.put(annotationJO);
						}
					}
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

		// report unmapped ids
		JSONArray unmappedJA = new JSONArray();
		resultJO.put("unmapped", unmappedJA);
		for (String identifier : queryIdHashSet) {
			unmappedJA.put(identifier);
		}

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);

		return jo.toString();
	}

	/**
	 * Convenience method for getting human annotations for array of IDs.
	 * 
	 * @param idList
	 * @return String representation of JSON object. The result JSONObject
	 *         contains the JSONArrays named "unmapped" and "annotations". Each
	 *         JSONObject in annotations JSONArray has K-V pairs for "ID",
	 *         "common", and "desc".
	 * @throws JSONException 
	 */
	public static String aliasToAnnotation2(String[] idList) throws JSONException {
		String csv = DatabaseService.arrayToCommaSeparatedString(idList);
		String jsonString = aliasToAnnotation2("9606", csv);

		return jsonString;
	}

	/**
	 * Get the annotation data given a list of aliases. Uses 2 SQL statements.
	 * It turns out this is faster than using SQL's LEFT OUTER JOIN.
	 * 
	 * @param ncbi_organism
	 * @param csvAliasList
	 * @return String representation of JSON object. The result JSONObject
	 *         contains the JSONArrays named "unmapped" and "annotations". Each
	 *         JSONObject in annotations JSONArray has K-V pairs for "ID",
	 *         "common", and "desc".
	 * @throws JSONException
	 */
	private static String aliasToAnnotation2(String NCBI_organism,
			String csvList) throws JSONException {

		HashSet<String> queryIdHashSet = new HashSet<String>(
				Arrays.asList(csvList.split(",")));

		HashMap<String, HashMap<String, String>> keepingTabsHashMap = new HashMap<String, HashMap<String, String>>();
		for (String s : queryIdHashSet) {
			keepingTabsHashMap.put(s.toUpperCase(),
					new HashMap<String, String>());
			keepingTabsHashMap.get(s.toUpperCase()).put("original_key", s);
			keepingTabsHashMap.get(s.toUpperCase()).put("identifier", null);
		}

		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		resultJO.put("organism", NCBI_organism);
		resultJO.put("csvList", csvList);

		JSONArray annotationsJA = new JSONArray();
		resultJO.put("annotations", annotationsJA);

		String aliasTableName = aliasTableNameHash.get(NCBI_organism);
		String annotTableName = annotTableNameHash.get(NCBI_organism);
		String mySqlList = csvToMySqlList(sanitizeString(csvList));

		// report unmapped ids (no annotation found)
		JSONArray unmappedJA = new JSONArray();
		resultJO.put("unmapped", unmappedJA);
		HashMap<String, String> identifier_to_original_key = new HashMap<String, String>();

		Connection con = getMySqlConnection();
		try {

			if (mySqlList.equalsIgnoreCase("")) {
				// no db query if list of IDs is empty
				JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
						error, id);

				return jo.toString();
			}

			String sql = "SELECT DISTINCT aliasTable.identifier,aliasTable.alias FROM "
					+ aliasTableName
					+ " AS aliasTable WHERE aliasTable.alias IN ("
					+ mySqlList
					+ ")";

			Statement s = con.createStatement();

			s.execute(sql);

			ResultSet rs = s.getResultSet();

			StringBuffer sb = new StringBuffer();

			while (rs.next()) {
				String identifier = rs.getString("identifier");
				String alias = rs.getString("alias");

				sb.append("," + identifier);

				keepingTabsHashMap.get(alias.toUpperCase()).put("identifier",
						identifier);
			}

			for (String query : keepingTabsHashMap.keySet()) {
				HashMap<String, String> dataHashMap = keepingTabsHashMap
						.get(query);
				if (dataHashMap.get("identifier") == null) {
					unmappedJA.put(dataHashMap.get("original_key"));
				} else {
					identifier_to_original_key.put(
							dataHashMap.get("identifier"),
							dataHashMap.get("original_key"));
				}
			}

			if (sb.length() > 1) {
				String mySqlList2 = csvToMySqlList(sb.toString());

				sql = "SELECT DISTINCT annoTable.identifier,annoTable.common_name,annoTable.description FROM "
						+ annotTableName
						+ " AS annoTable WHERE annoTable.identifier IN ("
						+ mySqlList2 + ") ORDER BY identifier,common_name ASC";

				s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();

				while (rs.next()) {
					JSONObject annotationJO = new JSONObject();
					annotationsJA.put(annotationJO);

					String identifier = rs.getString("identifier");
					annotationJO.put("ID", identifier);

					if (rs.getString("common_name").equalsIgnoreCase("no name")) {
						annotationJO.put("common", rs.getString("identifier"));
					} else {
						annotationJO.put("common", rs.getString("common_name"));
					}

					annotationJO.put("desc", rs.getString("description"));

					// remove from list of queries without annotations
					// leftover ones should be ones that didn't get
					// annotated
					identifier_to_original_key.remove(identifier);
				}

				for (String key : identifier_to_original_key.keySet()) {
					HashMap<String, String> dataHashMap = keepingTabsHashMap
							.get(key);
					unmappedJA.put(dataHashMap.get("original_key"));
				}

			}

			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
			error = "sql error";
			JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
					error, id);
			return jo.toString();
		}

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);

		return jo.toString();
	}

	/**
	 * Get the annotation data given a list of aliases. Uses SQL LEFT OUTER JOIN
	 * statement get identifiers for aliases. Then, uses the identifiers to get
	 * annotations.
	 * 
	 * @param ncbi_organism
	 * @param csvAliasList
	 * @return String representation of JSON object
	 * @throws JSONException
	 */
	private static String aliasToAnnotation(String NCBI_organism, String csvList)
			throws JSONException {
		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("csvList").value(csvList);
		js.key("annotations").array();

		String aliasTableName = aliasTableNameHash.get(NCBI_organism);
		String annotTableName = annotTableNameHash.get(NCBI_organism);
		String mySqlList = csvToMySqlList(csvList);

		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT annoTable.identifier,annoTable.common_name,annoTable.description FROM "
						+ annotTableName
						+ " AS annoTable LEFT OUTER JOIN "
						+ aliasTableName
						+ " AS aliasTable ON annoTable.identifier=aliasTable.identifier WHERE aliasTable.alias IN ("
						+ mySqlList + ")";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				while (rs.next()) {
					js.object();
					js.key("ID").value(rs.getString("identifier"));
					js.key("common").value(rs.getString("common_name"));
					js.key("desc").value(rs.getString("description"));
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
	 * Search the database for proper identifiers given a list of aliases.
	 * 
	 * @param NCBI_organism
	 * @param csvList
	 * @return String representation of JSON object
	 */
	private static JSONObject searchAlias(String NCBI_organism, String csvList) {

		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		try {
			Connection con = getMySqlConnection();
			Statement s = con.createStatement();

			HashSet<String> queryHashSet = new HashSet<String>(
					Arrays.asList(csvList.split(",")));

			HashMap<String, HashMap<String, String>> aliasDataHashMap = getAliasData(
					NCBI_organism, queryHashSet, s);

			s.close();
			con.close();

			resultJO.put("organism", NCBI_organism);
			resultJO.put("csvList", csvList);

			JSONArray aliasesJA = new JSONArray();
			resultJO.put("aliases", aliasesJA);

			for (String key : aliasDataHashMap.keySet()) {
				JSONObject aliasJO = new JSONObject();
				aliasesJA.put(aliasJO);

				HashMap<String, String> aliasData = aliasDataHashMap.get(key);

				aliasJO.put("ID", key);
				aliasJO.put("alias", aliasData.get("alias"));
				aliasJO.put("type", aliasData.get("type"));
			}

		} catch (SQLException e) {
			e.printStackTrace();
			error = "sql error";
		} catch (JSONException e) {
			e.printStackTrace();
			error = "JSON error";
		}

		JSONObject jo = encodeJsonRpcResponse(resultJO, error, id);

		return jo;
	}

	/**
	 * Check if IDs have annotation entries.
	 * 
	 * @param NCBI_organism
	 * @param querySet
	 * @param statement
	 * @return set of IDs that have annotation entries. Presumably, the IDs in
	 *         querySet that are not in the result Set need alias mapping.
	 * @throws SQLException
	 */
	public static Set<String> checkAnnotData(String NCBI_organism,
			Set<String> querySet, Statement statement) throws SQLException {

		Set<String> hasAnnotSet = new HashSet<String>();

		String mySqlList;
		String tableName = aliasTableNameHash.get(NCBI_organism);

		mySqlList = setToMysqlList(querySet);

		String sql = "SELECT identifier FROM " + tableName
				+ " AS a WHERE identifier IN (" + mySqlList
				+ ") ORDER BY identifier ASC";

		statement.execute(sql);

		ResultSet rs = statement.getResultSet();

		while (rs.next()) {
			String id = rs.getString("identifier");
			hasAnnotSet.add(id);
		}
		rs.close();

		return hasAnnotSet;
	}

	/**
	 * Get alias data. Does not return results for query IDs that were not
	 * found.
	 * 
	 * @param NCBI_organism
	 * @param queryHashSet
	 * @param statement
	 * @return each key has a HashMap for a value. Inner HashMap has keys
	 *         "alias" and "type".
	 * @throws SQLException
	 */
	public static HashMap<String, HashMap<String, String>> getAliasData(
			String NCBI_organism, HashSet<String> queryHashSet,
			Statement statement) throws SQLException {
		String mySqlList;
		String tableName = aliasTableNameHash.get(NCBI_organism);

		mySqlList = setToMysqlList(queryHashSet);

		String sql = "SELECT identifier,alias,type FROM " + tableName
				+ " AS a WHERE alias IN (" + mySqlList
				+ ") ORDER BY identifier,alias,type ASC";

		statement.execute(sql);

		ResultSet rs = statement.getResultSet();

		HashMap<String, HashMap<String, String>> aliasDataHashMap = new HashMap<String, HashMap<String, String>>();

		while (rs.next()) {
			HashMap<String, String> aliasHashMap = new HashMap<String, String>();
			aliasHashMap.put("alias", rs.getString("alias"));
			aliasHashMap.put("type", rs.getString("type"));

			aliasDataHashMap.put(rs.getString("identifier"), aliasHashMap);
		}
		rs.close();

		return aliasDataHashMap;
	}

	/**
	 * Search the database for biodes that match the search string.
	 * 
	 * @param NCBI_organism
	 * @param searchString
	 * @return String representation of JSON object
	 * @throws JSONException
	 */
	private static String searchAnnotation(String NCBI_organism,
			String searchString) throws JSONException {

		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		resultJO.put("organism", NCBI_organism);
		resultJO.put("searchString", searchString);

		JSONArray annotationsJA = new JSONArray();
		resultJO.put("annotations", annotationsJA);

		String tableName = annotTableNameHash.get(NCBI_organism);
		String sanitizedSearchString = sanitizeString(searchString);

		Connection con = getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT identifier,common_name,description FROM "
						+ tableName + " AS a WHERE description LIKE '%"
						+ sanitizedSearchString + "%' OR common_name LIKE '%"
						+ sanitizedSearchString + "%' OR identifier LIKE '%"
						+ sanitizedSearchString
						+ "%' ORDER BY common_name,identifier,description ASC";

				Statement s = con.createStatement();
				s.execute(sql);

				ResultSet rs = s.getResultSet();

				while (rs.next()) {
					JSONObject annotationJO = new JSONObject();
					annotationsJA.put(annotationJO);

					annotationJO.put("ID", rs.getString("identifier"));
					annotationJO.put("common", rs.getString("common_name"));
					annotationJO.put("desc", rs.getString("description"));
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

}
