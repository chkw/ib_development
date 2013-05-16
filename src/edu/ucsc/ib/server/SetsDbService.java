package edu.ucsc.ib.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Servlet to get sets data from database
 * 
 * @author Chris
 * 
 */
public class SetsDbService extends DatabaseService {
	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 6098682541300300752L;

	/**
	 * Constructor for this service. Calls DatabaseService constructor.
	 */
	public SetsDbService() {
		super();
	}

	/**
	 * Handle GET request. Hand over to doPost() method.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		// System.out.println("SetsDbService.doGet");
		// System.out.println("--handing over to SetsDbService.doPost");
		this.doPost(req, resp);
	}

	/**
	 * Handle POST request.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		// System.out.println("SetsDbService.doPost");
		// System.out.println("requestURI is:\t" + req.getRequestURI());

		String path = req.getPathInfo();

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/setsdb/test

			try {
				super.writeTextResponse("success? " + testConnection(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("setsList")) {
			// test with:
			// http://localhost:8080/ib/data/setsdb/setsList?organism=6239

			String NCBI_organism = req.getParameter("organism");

			try {
				super.writeTextResponse(this.getSetsList(NCBI_organism), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("setsName")) {
			// test with:
			// http://localhost:8080/ib/data/setsdb/setsName?setType=yeast_Go&searchString=nucleus

			String setType = req.getParameter("setType");
			String searchString = req.getParameter("searchString");

			try {
				super.writeTextResponse(
						this.searchSetNames(setType, searchString), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("getSet")) {
			// test with:
			// http://localhost:8080/ib/data/setsdb/getSet?setType=yeast_Go&setName='de
			// novo' protein folding

			String setType = req.getParameter("setType");
			String setName = req.getParameter("setName");

			try {
				super.writeTextResponse(this.getSet(setType, setName), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("setEnrichment")) {
			// test with:
			// http://localhost:8080/ib/data/setsdb/setEnrichment?organism=4932&setDataName=yeast_Go&queryIds=

			// send the results back as a file

			String organism = req.getParameter("organism");
			String setType = req.getParameter("setDataName");
			String csvBiodeList = req.getParameter("queryIds");

			try {
				String JSONstr = this.calcSetEnrichment(organism, setType,
						csvBiodeList);
				String NormalStr = this
						.enrichment_JSON_results_to_String(JSONstr);
				FileBounceService.doBounceStringToFile(resp, NormalStr,
						"enrichment.tab");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// nothing to do !
		}
	}

	/**
	 * Calculate the set enrichment. Uses hypergeometric distribution.
	 * 
	 * @param organism
	 * @param setType
	 * @param csvBiodeList
	 * @return a JSONObject with the results
	 * @throws JSONException
	 */
	private String calcSetEnrichment(String organism, String setType,
			String csvBiodeList) throws JSONException {
		long start = (new Date()).getTime();

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(organism);
		js.key("setType").value(setType);
		js.key("biodeList").value(csvBiodeList);

		String[] biodes = super.sanitizeString(csvBiodeList).split(",");
		String sanitizedSetType = super.sanitizeString(setType);
		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT num_elements FROM setlist "
						+ "WHERE name=? ORDER BY name ASC";

				PreparedStatement ps = con.prepareStatement(sql);
				ps.setString(1, sanitizedSetType);
				ps.execute();
				ps.clearParameters();

				ResultSet rs = ps.getResultSet();
				rs.next();
				int setTypeNumElements = rs.getInt("num_elements");
				js.key("num_elements").value(setTypeNumElements);

				sql = "SELECT DISTINCT name,members,size FROM "
						+ sanitizedSetType + " ORDER BY name ASC";

				Statement s = con.createStatement();
				s.execute(sql);

				rs = s.getResultSet();

				ArrayList<JSONObject> joList = new ArrayList<JSONObject>();

				while (rs.next()) {
					String[] members = rs.getString("members").split(" ");
					int matches = 0;
					for (String member : members) {
						for (String biode : biodes) {
							if (biode.equalsIgnoreCase(member)) {
								matches++;
							}
						}
					}
					if (matches >= 3) {
						double hyperP = super.computeHyperPValue(matches,
								biodes.length, members.length,
								setTypeNumElements);

						JSONObject jo = new JSONObject();

						jo.put("name", rs.getString("name"));
						jo.put("size", members.length);
						jo.put("overlap", matches);
						jo.put("hyperP", hyperP);

						joList.add(jo);
					}
				}

				// sort the results by score
				Collections.sort(joList,
						new DatabaseService.JSONNumberComparator("hyperP"));

				js.key("enrichments").array();
				for (int i = 0; i < joList.size(); i++) {
					js.value(joList.get(i));
				}
				js.endArray();

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
	 * Return the results from calcSetEnrichment in the form of a String that
	 * may be returned as a file.
	 * 
	 * @param jsonString
	 * @throws JSONException
	 */
	private String enrichment_JSON_results_to_String(String jsonString)
			throws JSONException {
		JSONObject JO = new JSONObject(jsonString);

		String organism = JO.getString("organism");
		String setDataName = JO.getString("setType");

		JSONArray ja = JO.getJSONArray("enrichments");

		StringBuffer sb = new StringBuffer();
		if (ja.length() > 0) {
			sb.append("#The selected nodes are enriched for the following sets from "
					+ setDataName + " for " + organism + ":\n");
			sb.append("#module name\tscore\toverlap\tmodule size");

			String name;
			String score;
			String overlap;
			String size;
			JSONObject jo;
			for (int i = 0; i < ja.length(); i++) {
				jo = ja.getJSONObject(i);

				name = jo.getString("name");
				score = String.valueOf(jo.getDouble("hyperP"));
				overlap = String.valueOf(jo.getInt("overlap"));
				size = String.valueOf(jo.getInt("size"));

				sb.append("\n" + name + "\t" + score + "\t" + overlap + "\t"
						+ size);
			}
		} else {
			sb.append("The selected nodes were not enriched for any sets from "
					+ setDataName + " for " + organism + ".");
		}
		return sb.toString();
	}

	/**
	 * Get the set information given the set type (eg. yeast_go) and the set's
	 * name (eg. 'de novo' protein folding)
	 * 
	 * @param setType
	 *            is the name of the table to look in
	 * @param setName
	 * @return String representation of JSON object
	 * @throws JSONException
	 */
	private String getSet(String setType, String setName) throws JSONException {
		JSONStringer js = new JSONStringer();
		js.object();

		js.key("setType").value(setType);
		js.key("setName").value(setName);
		js.key("set").array();

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT name,members FROM " + setType
						+ " AS s WHERE name=?";

				PreparedStatement ps = con.prepareStatement(sql);
				ps.setString(1, setName);
				ps.execute();
				ps.clearParameters();

				ResultSet rs = ps.getResultSet();
				String[] members;
				while (rs.next()) {
					js.object();
					js.key("name").value(rs.getString("name"));

					members = rs.getString("members").split(" ");
					js.key("members").array();
					for (String member : members) {
						js.value(member);
					}
					js.endArray();
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
	 * Get the a list of sets for an organism.
	 * 
	 * @param NCBI_organism
	 * @return String representation of JSON object
	 * @throws JSONException
	 */
	private String getSetsList(String NCBI_organism) throws JSONException {
		JSONStringer js = new JSONStringer();
		js.object();

		js.key("organism").value(NCBI_organism);
		js.key("sets").array();

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT name,description FROM setlist AS s "
						+ "WHERE NCBI_species=? ORDER BY description,name ASC";

				PreparedStatement ps = con.prepareStatement(sql);
				ps.setString(1, NCBI_organism);
				ps.execute();
				ps.clearParameters();

				ResultSet rs = ps.getResultSet();
				while (rs.next()) {
					js.object();
					js.key("name").value(rs.getString("name"));
					js.key("description").value(rs.getString("description"));
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
	 * Search for set names that match the specified search string.
	 * 
	 * @param setType
	 * @param searchString
	 * 
	 * @return
	 * @throws JSONException
	 */
	private String searchSetNames(String setType, String searchString)
			throws JSONException {
		JSONStringer js = new JSONStringer();
		js.object();

		js.key("setType").value(setType);
		js.key("searchString").value(searchString);
		js.key("sets").array();

		Connection con = this.getMySqlConnection();
		if (con != null) {
			try {
				String sql = "SELECT DISTINCT name,members FROM " + setType
						+ " AS s WHERE name LIKE ? ORDER BY name ASC";

				PreparedStatement ps = con.prepareStatement(sql);
				ps.setString(1, "%" + searchString + "%");
				ps.execute();
				ps.clearParameters();

				ResultSet rs = ps.getResultSet();
				String[] members;
				while (rs.next()) {
					js.object();
					js.key("name").value(rs.getString("name"));

					members = rs.getString("members").split(" ");
					js.key("members").array();
					for (String member : members) {
						js.value(member);
					}
					js.endArray();
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
}
