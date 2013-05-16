package edu.ucsc.ib.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet for handling requests to submit and retrieve Interaction Browser
 * saved states.
 * 
 * @author cw
 * 
 */
public class SavedStateDBService extends DatabaseService {

	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = -1511421252547036727L;

	public static final String SAVED_STATES_TABLE_NAME = "savedstates";

	private static final int RANDOM_ID_LENGTH = 6;

	public SavedStateDBService() {
		super();
	}

	/**
	 * Handle HTTP form submission.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		HttpSession session = InitSession(req);
		String sessionID = session.getId();

		String path = req.getPathInfo();
		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/savedStateDB/test
			try {
				super.writeTextResponse("success? " + testConnection(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("submit")) {
			// test with POST to:
			// http://localhost:8080/ib/data/savedStateDB/submit
			// TODO
			String randomString = generateRandomString(RANDOM_ID_LENGTH);
			String savedStateString = req.getParameter("saveStateString");
			try {
				super.writeTextResponse(this.writeSavedStateToDB(randomString,
						sessionID, savedStateString), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("retrieve")) {
			// test with:
			// http://localhost:8080/ib/data/savedStateDB/retrieve
			// TODO saveStateID retrieveID
			String retrieveID = req.getParameter("saveStateID");
			try {
				super.writeTextResponse(this.retrieveSavedState(retrieveID),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// do nothing
		}
	}

	/**
	 * Get the saved JSON text for a saved state.
	 * 
	 * @param retrieveID
	 * @return
	 */
	private String retrieveSavedState(String retrieveID) {
		JSONObject resultJO = new JSONObject();

		try {

			resultJO.put("saveStateID", retrieveID);

			Connection con = this.getMySqlConnection_custom();

			createSavedStatesTable(con);

			String sql = "SELECT DISTINCT deflatedJson FROM savedstates AS s WHERE s.retrieveID=?";

			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, retrieveID);
			ps.execute();
			ps.clearParameters();

			ResultSet rs = ps.getResultSet();

			if (rs.next()) {
				resultJO.put("gotResult", true);

				resultJO.put("savedJSON",
						new JSONObject(
								extractBytes(rs.getBytes("deflatedJson"))));

				// resultJO.put("savedJSON",
				// new JSONObject(rs.getString("savedJSON")));

			} else {
				resultJO.put("gotResult", false);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return resultJO.toString();
		} catch (SQLException e) {
			e.printStackTrace();
			return resultJO.toString();
		}

		return resultJO.toString();
	}

	/**
	 * Write a saved state to the database table. The saved state is actually a
	 * JSON text that contains the status of nodes and tracks in a network
	 * graph.
	 * 
	 * @param randomString
	 *            This will be used to retrieve the saved state.
	 * @param sessionID
	 *            This is the ID for the session that saved the state.
	 * @param savedStateString
	 * @return
	 */
	private String writeSavedStateToDB(String randomString, String sessionID,
			String savedStateString) {
		Connection con = this.getMySqlConnection_custom();

		createSavedStatesTable(con);
		deleteOldSavedStates(4, con);

		try {
			// INSERT row into savedstates table
			String sql = "INSERT INTO savedstates (retrieveID,userid,deflatedJson)"
					+ " VALUES ( ?,?,? )";

			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, randomString);
			ps.setString(2, sessionID);

			// ps.setString(3, savedStateString);

			byte[] compressedBytes = compressBytes(savedStateString);
			ps.setBytes(3, compressedBytes);

			ps.execute();
			ps.clearParameters();
			con.close();
			return randomString;
		} catch (SQLException e) {
			e.printStackTrace();
			return "FAILED";
		}
	}

	/**
	 * Generate a String of pseudorandom characters.
	 * 
	 * @param length
	 *            length of String to generate.
	 * @return
	 */
	private static String generateRandomString(int length) {
		String pool = "abcdefghijklmnopqrstuvwxyz0123456789";
		Random r = new Random();
		StringBuffer strBuf = new StringBuffer();
		while (strBuf.length() < length) {
			strBuf.append(pool.charAt(r.nextInt(pool.length())));
		}
		return strBuf.toString();
	}

	/**
	 * Delete rows from savedstates table that is older than the specified
	 * number of months.
	 * 
	 * @param thresholdMonths
	 * @param connection
	 *            remains open throughout the method
	 */
	private static void deleteOldSavedStates(final int thresholdMonths,
			final Connection connection) {
		String sql = "DELETE IGNORE FROM " + SAVED_STATES_TABLE_NAME + " "
				+ "WHERE DATE_SUB(CURDATE(),INTERVAL " + thresholdMonths
				+ " MONTH) > inserted_timestamp";

		try {
			Statement statement = connection.createStatement();
			statement.execute(sql);
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the saved states table if it does not exist.
	 * 
	 * @param connection
	 *            remains open throughout the method
	 */
	private static void createSavedStatesTable(final Connection connection) {
		StringBuffer sb = new StringBuffer();
		sb.append("CREATE TABLE IF NOT EXISTS `" + SAVED_STATES_TABLE_NAME
				+ "` ( ");
		sb.append("`retrieveID` varchar(50) NOT NULL, ");
		sb.append("`userid` varchar(50) NOT NULL, ");
		sb.append("`inserted_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, ");
		sb.append("`savedJSON` mediumtext , ");
		sb.append("`deflatedJson` blob , ");
		sb.append("PRIMARY KEY (`retrieveID`) )");

		try {
			Statement statement = connection.createStatement();
			statement.execute(sb.toString());
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Drop saved states table if exists.
	 * 
	 * @param connection
	 *            remains open throughout the method
	 */
	private static void dropSavedStatesTable(final Connection connection) {
		StringBuffer sb = new StringBuffer();
		sb.append("DROP TABLE IF EXISTS `" + SAVED_STATES_TABLE_NAME + "`");

		try {
			Statement statement = connection.createStatement();
			statement.execute(sb.toString());
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
