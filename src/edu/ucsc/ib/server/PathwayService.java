package edu.ucsc.ib.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * servlet to get concepts and relations for pathways.
 * 
 * @author Chris
 * 
 */
public class PathwayService extends DatabaseService {
	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = -6320369040618822490L;

	private static final String RELATIONS_TABLE_PREFIX = "pathway_relations_";
	private static final String CONCEPTS_TABLE_PREFIX = "pathway_concepts_";
	private static final String PATHWAYS_METATABLE_NAME = "pathways_list";

	// TODO //////////////////////////////////

	public PathwayService() {
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

		if (!sessionHasAttribute(session, "uploadedPathwayCleaner")) {

			session.setAttribute("uploadedPathwayCleaner",
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
							deleteUserRows(userID);
						}
					});
		}

		// TODO service methods

		if (path.endsWith("getPathways")) {
			// test with:
			// http://localhost:8080/ib/data/pathway/getPathways?organism=9606

			String NCBI_organism = req.getParameter("organism");

			try {
				DatabaseService.writeTextResponse(
						getAvailablePathways(NCBI_organism), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("searchPathways")) {
			// test with:
			// http://localhost:8080/ib/data/pathway/searchPathways?organism=9606&search=signal

			String NCBI_organism = req.getParameter("organism");
			String searchString = req.getParameter("search");

			try {
				DatabaseService.writeTextResponse(
						searchPathways(NCBI_organism, searchString), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("getPathway")) {
			// test with:
			// http://localhost:8080/ib/data/pathway/getPathway?pathway=36945
			String pathwayID = req.getParameter("pathway");

			try {
				DatabaseService.writeTextResponse(getPathway(pathwayID), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// just return the path
			try {
				JSONObject jo = DatabaseService.encodeJsonRpcResponse(
						new JSONObject().put("path", path), null, 0);
				DatabaseService.writeTextResponse(jo.toString(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// TODO /////////////////////////////////////////////////////////////

	/**
	 * Get the concepts and relations for the specified pathway.
	 * 
	 * @param pathway_id
	 * @return
	 * @throws JSONException
	 */
	private String getPathway(final String pathway_id) throws JSONException {
		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		resultJO.put("pathway", pathway_id);

		Connection con = DatabaseService.getMySqlConnection();
		try {
			JSONObject metadataJO = getMetaDataJO(pathway_id, con);
			resultJO.put("metadata", metadataJO);

			JSONArray conceptsJA = getConceptsJA(pathway_id, con);
			resultJO.put("concepts", conceptsJA);

			JSONArray relationsJA = getRelationsJA(pathway_id, con);
			resultJO.put("relations", relationsJA);

			con.close();
		} catch (SQLException e) {
			error = e.getMessage();

			JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
					error, id);
			return jo.toString();
		}

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);
		return jo.toString();
	}

	/**
	 * Get the metadata for a pathway. Does not close the connection when done,
	 * so it can be reused.
	 * 
	 * @param pathway_id
	 * @param con
	 * @return
	 * @throws SQLException
	 * @throws JSONException
	 */
	private static JSONObject getMetaDataJO(final String pathway_id,
			final Connection con) throws SQLException, JSONException {
		Statement s = con.createStatement();

		String sql = "SELECT * FROM `" + PATHWAYS_METATABLE_NAME
				+ "` WHERE pathway_id='"
				+ DatabaseService.sanitizeString(pathway_id) + "'";

		s.execute(sql);

		ResultSet rs = s.getResultSet();

		JSONObject metadataJO = new JSONObject();
		while (rs.next()) {
			metadataJO.put("pathway_id", rs.getString("pathway_id"));
			metadataJO.put("name", rs.getString("name"));
			metadataJO.put("source", rs.getString("source"));
			metadataJO.put("NCBI_species", rs.getString("NCBI_species"));
			metadataJO.put("num_concepts", rs.getInt("num_concepts"));
			metadataJO.put("num_relations", rs.getInt("num_relations"));
		}
		rs.close();
		s.close();
		return metadataJO;
	}

	/**
	 * get the concepts from database. Does not close the connection when done,
	 * so it can be reused.
	 * 
	 * @param pathway_id
	 * @param con
	 * @return
	 * @throws SQLException
	 * @throws JSONException
	 */
	private static JSONArray getConceptsJA(final String pathway_id,
			final Connection con) throws SQLException, JSONException {
		Statement s = con.createStatement();

		String sql = "SELECT type,name FROM `"
				+ DatabaseService.sanitizeString(CONCEPTS_TABLE_PREFIX
						+ pathway_id) + "`";

		s.execute(sql);

		ResultSet rs = s.getResultSet();

		JSONArray conceptsJA = new JSONArray();
		while (rs.next()) {
			JSONObject conceptsJO = new JSONObject();
			conceptsJA.put(conceptsJO);

			conceptsJO.put("type", rs.getString("type"));
			conceptsJO.put("name", rs.getString("name"));
		}
		rs.close();
		s.close();
		return conceptsJA;
	}

	/**
	 * get the relations from database. Does not close the connection when done,
	 * so it can be reused.
	 * 
	 * @param pathway_id
	 * @param con
	 * @return
	 * @throws SQLException
	 * @throws JSONException
	 */
	private static JSONArray getRelationsJA(final String pathway_id,
			final Connection con) throws SQLException, JSONException {
		Statement s = con.createStatement();

		String sql = "SELECT concept1,concept2,relation FROM `"
				+ DatabaseService.sanitizeString(RELATIONS_TABLE_PREFIX
						+ pathway_id) + "`";

		s.execute(sql);

		ResultSet rs = s.getResultSet();

		JSONArray relationsJA = new JSONArray();
		while (rs.next()) {
			JSONObject relationJO = new JSONObject();
			relationsJA.put(relationJO);

			relationJO.put("1", rs.getString("concept1"));
			relationJO.put("2", rs.getString("concept2"));
			relationJO.put("relation", rs.getString("relation"));
		}
		rs.close();
		s.close();
		return relationsJA;
	}

	/**
	 * Search for pathways that contain the specified search string in the name.
	 * If the searchStr is null, then all pathways for organism are returned.
	 * 
	 * @param ncbiTaxId
	 * @param searchStr
	 * @return
	 * @throws JSONException
	 */
	private String searchPathways(final String ncbiTaxId, final String searchStr)
			throws JSONException {
		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		resultJO.put("organism", ncbiTaxId);
		resultJO.put("search", searchStr);

		Connection con = DatabaseService.getMySqlConnection();
		Statement s;
		try {
			s = con.createStatement();

			String sql = "SELECT pathway_id,name,source,num_concepts,num_relations FROM `"
					+ PATHWAYS_METATABLE_NAME
					+ "` WHERE NCBI_species='"
					+ DatabaseService.sanitizeString(ncbiTaxId)
					+ "' AND name LIKE '%"
					+ DatabaseService.sanitizeString(searchStr)
					+ "%' ORDER BY name ASC";

			s.execute(sql);

			ResultSet rs = s.getResultSet();

			JSONArray pathwaysJA = new JSONArray();
			resultJO.put("pathways", pathwaysJA);

			while (rs.next()) {
				JSONObject pathwayJO = new JSONObject();
				pathwaysJA.put(pathwayJO);

				pathwayJO.put("pathway_id", rs.getString("pathway_id"));
				pathwayJO.put("name", rs.getString("name"));
				pathwayJO.put("source", rs.getString("source"));
				pathwayJO.put("num_concepts", rs.getInt("num_concepts"));
				pathwayJO.put("num_relations", rs.getInt("num_relations"));
			}

			rs.close();
			s.close();
			con.close();
		} catch (SQLException e) {
			error = e.getMessage();

			JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
					error, id);
			return jo.toString();
		}

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);
		return jo.toString();
	}

	/**
	 * Get all the available pathways for the specified organism. Wrapper for
	 * {@link #searchPathways(String, String) searchPathways}, where the search
	 * String is null.
	 * 
	 * @param ncbiTaxId
	 * @return
	 * @throws JSONException
	 */
	private String getAvailablePathways(final String ncbiTaxId)
			throws JSONException {
		return searchPathways(ncbiTaxId, "");
	}

	/**
	 * delete a user's db rows.
	 * 
	 * @param userID
	 */
	private void deleteUserRows(final String userID) {
		Connection con = this.getMySqlConnection_custom();
		try {
			Statement s = con.createStatement();

			String sql = "DELETE FROM `" + PATHWAYS_METATABLE_NAME
					+ "` WHERE user='" + userID + "'";

			s.execute(sql);

			s.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
