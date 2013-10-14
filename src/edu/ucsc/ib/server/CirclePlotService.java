package edu.ucsc.ib.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
 * Servlet for querying database for circle plot data.
 * 
 * @author Chris
 * 
 */
public class CirclePlotService extends DatabaseService {

	/**
	 * width of CircleMap images
	 */
	private static final int CircleMapWidth = 300;

	/**
	 * Suffix for subtype "NA"
	 */
	public static final String SUBTYPE_NA_SUFFIX = "_NA";
	private static final String SUBTYPES_TABLE_NAME = "subtypes";
	private static final String SCORE_MATRICES_METATABLE_NAME = "matrixlist";
	private static final String CLINICAL_DATA_METATABLE_NAME = "clinicalData";

	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = -7616036350109276658L;

	// TODO //////////////////////////////////

	public CirclePlotService() {
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

		if (!sessionHasAttribute(session, "uploadedMatrixCleaner")) {

			session.setAttribute("uploadedMatrixCleaner",
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
							cleanUserTables(userID);
						}
					});
		}

		// TODO service methods

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/CirclePlot/test

			try {
				DatabaseService.writeTextResponse("hello", resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("getMatrixList")) {
			// test with:
			// http://localhost:8080/ib/data/circlePlot/getMatrixList?organism=9606

			String NCBI_organism = req.getParameter("organism");

			try {
				DatabaseService.writeTextResponse(
						getAvailableMatrices(userID, NCBI_organism), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("getImages")) {
			// test with:
			// http://localhost:8080/ib/data/circlePlot/getImages?matrixPriority=OV_AgilentG4502A_07_2,OV_AgilentG4502A_07_3&orderFeature=MAP2K4&method=getImages&features=PSEN2,MAP2K4,THPO,DYNLT3,PPWD1,VEZT,SLC5A9,TLR2,C12orf48,RUSC1,C1orf150,GCHFR,ATP1A1,EXOC3,PRPF4B,TTK,PDE6D,OR10T2,NTS,EXTL3,SHC3&matrixNameList=OV_AgilentG4502A_07_2,OV_AgilentG4502A_07_3

			JSONObject jo = processGetImageRequest(req, userID);

			// write response
			try {
				DatabaseService.writeTextResponse(jo.toString(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("getClincalDataList")) {
			// test with:
			// http://localhost:8080/ib/data/circlePlot/getClincalDataList

			// write response
			try {
				JSONArray ja = getAvailableClinicalMatrices();
				DatabaseService.writeTextResponse(ja.toString(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("getClinicalData")) {
			// test with:
			// http://localhost:8080/ib/data/circlePlot/getClinicalData?table=TCGA_BRCA_clinicalMatrix&feature=PAM50Call

			String table = req.getParameter("table");
			String feature = req.getParameter("feature");

			HashMap<String, String> clinicalData = getClinicalData(table,
					feature);
			// write response
			try {
				DatabaseService
						.writeTextResponse(clinicalData.toString(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("uploadMatrix")) {
			// uses multipartContent to upload a file containing a matrix

			JSONObject resultJO = new JSONObject();
			String error = null;
			int id = 0;

			// Check that we have a file upload request
			if (ServletFileUpload.isMultipartContent(req)) {

				List<FileItem> items = parseMultipartRequest(req);
				Map<String, String> parameterMap = getParamaterMapping(items);
				JSONArray paramsJA = new JSONArray(parameterMap.keySet());

				try {
					writeMatrixToDbTable(userID, parameterMap);

					resultJO.put("params", paramsJA);
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO,
					error, id);

			try {
				super.writeTextResponse(jo.toString(), resp);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// nothing to do !
		}
	}

	// TODO /////////////////////////////////////////////////////////////

	/**
	 * Drop user's uploaded tables. Also, delete user's rows.
	 * 
	 * @param userID
	 */
	protected void cleanUserTables(String userID) {
		try {
			Connection con = getMySqlConnection_custom();
			Statement s = con.createStatement();

			// get tableNames
			String sql = "SELECT tableName FROM `"
					+ SCORE_MATRICES_METATABLE_NAME
					+ "` WHERE tableName LIKE 'user_" + userID + "_%'";
			s.execute(sql);

			String dropTableSql = "DROP TABLE IF EXISTS ";

			ResultSet rs = s.getResultSet();

			Statement s2 = con.createStatement();
			while (rs.next()) {
				String tableName = rs.getString("tableName");

				// drop table
				sql = dropTableSql + "`" + tableName + "`";
				s2.execute(sql);
			}
			rs.close();

			// delete from metatable
			sql = "DELETE FROM `" + SCORE_MATRICES_METATABLE_NAME
					+ "` WHERE tableName LIKE 'user_" + userID + "_%'";
			s.execute(sql);

			s.close();
			s2.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * write uploaded file from multipart request to database
	 * 
	 * @param userId
	 * @param parameterMap
	 *            parameter mappings for
	 *            (name,NCBI_species,tableName,category,description
	 *            ,cohortMin,cohortMax,vector)
	 * @throws SQLException
	 */
	private static void writeMatrixToDbTable(String userId,
			Map<String, String> parameterMap) throws SQLException {

		Connection connection = getMySqlConnection_custom();
		Statement statement = connection.createStatement();

		Set<String> params = parameterMap.keySet();

		String name = DatabaseService.sanitizeString(parameterMap.get("name"));
		if (name == null || name.equalsIgnoreCase("")) {
			System.out
					.println("no 'name' given - exit out of writeMatrixToDbTable");
			return;
		}

		String delim = "/";
		String[] strArray1 = name.split(delim);

		String pattern = Pattern.quote("\\");
		String[] strArray2 = strArray1[strArray1.length - 1].split(pattern);

		String matrixName = strArray2[strArray2.length - 1];

		String category = createCategory(userId, matrixName);

		String tableName = createTableName(userId, matrixName);

		String NCBI_species = "9606";
		if (params.contains("NCBI_species")) {
			NCBI_species = parameterMap.get("NCBI_species");
		}

		String description = "uploaded matrix";

		// vector
		// get list of rows data from uploaded file
		String[] rows = parameterMap.get("uploadFormElement").split("\\n+");

		// check there is at least one row of data (header + 1 row)
		if (rows.length < 2) {
			System.out
					.println("too few lines in uploaded file - exit out of writeMatrixToDbTable");
			return;
		}

		// get list of column headings
		String[] colHeaders = rows[0].split("\t");

		StringBuffer sb = new StringBuffer();
		for (int i = 1; i < colHeaders.length; i++) {
			sb.append(colHeaders[i] + ",");
		}
		sb.deleteCharAt(sb.lastIndexOf(","));

		String sampleIDs = sb.toString();

		// remove table at last possible moment
		removeMatrixTable(tableName, connection);

		// compute cohort values over all features and samples
		double cohortMin = Double.MAX_VALUE;
		double cohortMax = Double.MIN_VALUE;

		// create matrix table

		StringBuffer sqlSB = new StringBuffer("CREATE TABLE IF NOT EXISTS `"
				+ tableName + "`");
		sqlSB.append(" ( feature VARCHAR(250), vector TEXT, PRIMARY KEY (feature) )");

		statement.execute(sqlSB.toString());

		// insert rows
		sb = new StringBuffer("INSERT INTO `" + tableName + "` ");
		sb.append("(feature,vector) VALUES (?,?)");

		PreparedStatement ps = connection.prepareStatement(sb.toString());

		Set<String> features = new HashSet<String>();

		for (int i = 1; i < rows.length; i++) {
			String[] columns = rows[i].split("\t");
			String feature = columns[0];

			// only allow one row per feature
			if (features.contains(feature)) {
				continue;
			}

			features.add(feature);

			StringBuffer vectorSB = new StringBuffer();
			for (int j = 1; j < columns.length; j++) {
				String val = columns[j];
				vectorSB.append(val + ",");

				// check max/min values
				try {
					Double valDouble = Double.parseDouble(val);

					cohortMin = Math.min(valDouble, cohortMin);
					cohortMax = Math.max(valDouble, cohortMax);

				} catch (NumberFormatException nfe) {
					continue;
				}
			}
			vectorSB.deleteCharAt(vectorSB.lastIndexOf(","));

			ps.setString(1, feature);
			ps.setString(2, vectorSB.toString());

			ps.execute();
			ps.clearParameters();
		}

		// create entry in metatable
		sb = new StringBuffer("INSERT INTO `" + SCORE_MATRICES_METATABLE_NAME
				+ "` ");
		sb.append("(name,NCBI_species,tableName,category,description,cohortMin,cohortMax,vector) ");
		sb.append("VALUES (?,?,?,?,?,?,?,?)");

		ps = connection.prepareStatement(sb.toString());
		ps.setString(1, matrixName);
		ps.setString(2, NCBI_species);
		ps.setString(3, tableName);
		ps.setString(4, category);
		ps.setString(5, description);
		ps.setDouble(6, cohortMin);
		ps.setDouble(7, cohortMax);
		ps.setString(8, sampleIDs);

		ps.execute();
		ps.clearParameters();
		ps.close();

		statement.close();
		connection.close();
	}

	/**
	 * Remove a matrix table from the database. Removes table as well as entry
	 * in metatable.
	 * 
	 * @param tableName
	 * @param connection
	 * @throws SQLException
	 */
	private static void removeMatrixTable(String tableName,
			Connection connection) throws SQLException {
		Statement statement = connection.createStatement();

		// drop table
		String sql = "DROP TABLE IF EXISTS `" + tableName + "`";
		statement.execute(sql);

		// delete from metatable
		sql = "DELETE FROM `" + SCORE_MATRICES_METATABLE_NAME
				+ "` WHERE tableName='" + tableName + "'";
		statement.execute(sql);

		statement.close();
	}

	/**
	 * create a table name from parameters
	 * 
	 * @param userId
	 * @param name
	 * @return
	 */
	private static String createTableName(String userId, String name) {
		String hash = DatabaseService.getMD5Hash(userId + "_" + name);
		String s = "user_" + hash + "_matrix";

		return DatabaseService.sanitizedTableName(s);
	}

	/**
	 * Create a category to save in db. Category is used to label sample groups.
	 * 
	 * @param userId
	 * @param name
	 * @return
	 */
	private static String createCategory(String userId, String name) {
		return DatabaseService.sanitizeString("uploaded_" + userId);
	}

	/**
	 * Process a request for getting images.
	 * 
	 * @param req
	 * @return
	 */
	private JSONObject processGetImageRequest(HttpServletRequest req,
			String userId) {

		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		JSONObject queryJO = new JSONObject();
		try {
			resultJO.put("queryJO", queryJO);

			// features and orderFeature
			String[] features = new String[0];
			String orderFeature = null;
			if (req.getParameter("features") != null) {
				features = req.getParameter("features").split(",");
				orderFeature = features[0];
			}

			// orderFeature
			if (req.getParameter("orderFeature") != null) {
				orderFeature = req.getParameter("orderFeature");
			}

			queryJO.put("orderFeature", orderFeature);

			// all features in a HashSet
			HashSet<String> featuresHashSet = new HashSet<String>(
					Arrays.asList(features));
			featuresHashSet.add(orderFeature);

			queryJO.put("features", featuresHashSet);

			// scoreRingsList
			String[] ringsArray = new String[0];
			if (req.getParameter("ringsList") != null) {
				ringsArray = req.getParameter("ringsList").split(",");
			}
			ArrayList<String> ringsList = new ArrayList<String>(
					Arrays.asList(ringsArray));

			queryJO.put("ringsList", ringsList);

			// sortingDataset
			String sortingRing = null;
			if (req.getParameter("sortingRing") == null
					|| req.getParameter("sortingRing").toLowerCase()
							.equalsIgnoreCase("null")) {
				sortingRing = ringsList.get(0);
			} else {
				sortingRing = req.getParameter("sortingRing");
			}

			queryJO.put("sortingRing", sortingRing);

			// sampleGroupSummarySwitch
			boolean sampleGroupSummarySwitch = false;
			if ((req.getParameter("sampleGroupSummarySwitch") != null)
					&& (req.getParameter("sampleGroupSummarySwitch")
							.toLowerCase().equalsIgnoreCase("true"))) {
				sampleGroupSummarySwitch = true;
			}

			queryJO.put("sampleGroupSummarySwitch", sampleGroupSummarySwitch);

			// ringMergeSwitch
			boolean ringMergeSwitch = false;
			if ((req.getParameter("ringMergeSwitch") != null)
					&& (req.getParameter("ringMergeSwitch").toLowerCase()
							.equalsIgnoreCase("true"))) {
				ringMergeSwitch = true;
			}

			queryJO.put("ringMergeSwitch", ringMergeSwitch);

			// completeSampleNamesSwitch
			boolean ignoreMissingSamples = false;
			if ((req.getParameter("ignoreMissingSamples") != null)
					&& (req.getParameter("ignoreMissingSamples").toLowerCase()
							.equalsIgnoreCase("true"))) {
				ignoreMissingSamples = true;
			}

			queryJO.put("ignoreMissingSamples", ignoreMissingSamples);

			// GATHER UP DATA

			ArrayList<String> scoreMatrixNames = new ArrayList<String>();

			// example: TCGA_BRCA_clinicalMatrix__PAM50Call_clinical
			ArrayList<String> clinicalMatrixNames = new ArrayList<String>();

			boolean uploadedRingRequested = false;
			for (String name : ringsList) {

				// identify the clinical matrices
				if (name.endsWith("_clinical")) {
					clinicalMatrixNames.add(name);
				} else {
					scoreMatrixNames.add(name);
				}

				// determine if uploaded ring was requested
				// if true, then redraw circlemaps ... since uploaded datasets
				// may have overlapping names
				if (uploadedRingRequested == false
						&& name.endsWith("_uploaded")) {
					uploadedRingRequested = true;
				}
			}

			// collect matrix samples values for each requested feature
			HashMap<String, HashMap<String, HashMap<String, String>>> scoresMatrices = getMatrixSampleScores(
					scoreMatrixNames, featuresHashSet, userId);

			// collect clinical data
			// example: TCGA_BRCA_clinicalMatrix__PAM50Call_clinical
			HashMap<String, HashMap<String, HashMap<String, String>>> clinicalData = getClinicalData(clinicalMatrixNames);

			// collect metadata for score matrices
			HashMap<String, HashMap<String, String>> scoreMatrixMetadata = getScoreMatrixMetadata(
					scoreMatrixNames, userId);

			// create images
			CirclePlotter circlePlotter = new CirclePlotter(
					getServletContext(), CircleMapWidth);

			// set up circlePlotter options
			circlePlotter.setOptions(ringsList, scoresMatrices, clinicalData,
					featuresHashSet, orderFeature, sortingRing,
					scoreMatrixMetadata, sampleGroupSummarySwitch,
					ringMergeSwitch, ignoreMissingSamples, false);

			// System.out.println((new JSONObject(scoresMatrices)).toString());
			// System.out
			// .println((new JSONObject(scoreMatrixMetadata)).toString());

			// get image urls for return
			HashMap<String, String> circleMapURLs = circlePlotter
					.drawCircleMaps();

			resultJO.put("circleImageURLs", circleMapURLs);

			// TODO circle plotting ancillary data
			resultJO.put("sortingDataset", circlePlotter.getSortingRing());
			resultJO.put("matrixDisplayOrder",
					circlePlotter.getRingDisplayOrder(true));
			resultJO.put("orderFeature", orderFeature);
			resultJO.put("sortedSampleSubtypes", new JSONArray());
			resultJO.put("groupRingColorKeys",
					circlePlotter.getSampleGroupingColorKeys());
			resultJO.put("sortedSubtypeNames", new JSONArray());

		} catch (JSONException e) {
			error = "JSONException";
			e.printStackTrace();
		}

		// encode a JSON-RPC response
		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);
		return jo;
	}

	/**
	 * Get samples scores for the specified features in the specified matrices.
	 * The resulting nested HashMap has the following keys: matrixName, feature,
	 * sampleName. Note that not all samples and/or features may be available
	 * for all matrices.
	 * 
	 * @param matrixDisplayOrder
	 *            list of matrices in display order
	 * @param featuresHashSet
	 * @return
	 */
	private static HashMap<String, HashMap<String, HashMap<String, String>>> getMatrixSampleScores(
			ArrayList<String> matrixDisplayOrder,
			HashSet<String> featuresHashSet, String userId) {

		// collect matrix metadata (tableName and sampleNames)
		HashMap<String, HashMap<String, String>> matrixMetaDataHashMap = getScoreMatrixMetadata(
				matrixDisplayOrder, userId);

		// get sample scores for each feature in each matrix
		HashMap<String, HashMap<String, HashMap<String, String>>> matrixData = new HashMap<String, HashMap<String, HashMap<String, String>>>();
		for (String matrixName : matrixDisplayOrder) {

			if (matrixName.endsWith("_uploaded")) {
				matrixName = matrixName.replaceFirst("_uploaded", "");
			}

			/**
			 * HashMap of feature to String[] of sample scores. Must match up
			 * with order of sample names in sampleNamesHashMap.
			 */
			HashMap<String, String> sampleScoresCSVByFeatureHashMap = getSampleScores(
					featuresHashSet,
					matrixMetaDataHashMap.get(matrixName).get("tableName"));

			String[] sampleNames = matrixMetaDataHashMap.get(matrixName)
					.get("sampleNames").split(",");

			HashMap<String, HashMap<String, String>> featureScoresHashMap = new HashMap<String, HashMap<String, String>>();
			matrixData.put(matrixName, featureScoresHashMap);

			for (String feature : sampleScoresCSVByFeatureHashMap.keySet()) {

				String[] scores = sampleScoresCSVByFeatureHashMap.get(feature)
						.split(",");

				HashMap<String, String> scoresHashMap = new HashMap<String, String>();
				featureScoresHashMap.put(feature, scoresHashMap);

				for (int i = 0; i < sampleNames.length; i++) {
					// iterate through sample names/values
					scoresHashMap.put(sampleNames[i], scores[i]);
				}
			}
		}
		return matrixData;
	}

	/**
	 * Get sample scores for each specified feature in the specified matrix
	 * table.
	 * 
	 * @param featuresHashSet
	 * @param matrixTableName
	 * @return
	 */
	private static HashMap<String, String> getSampleScores(
			HashSet<String> featuresHashSet, String matrixTableName) {

		HashMap<String, String> sampleScoresHashMap = new HashMap<String, String>();

		StringBuffer sb = new StringBuffer();
		sb.append("SELECT feature,vector");
		sb.append(" FROM " + sanitizeString(matrixTableName));
		sb.append(" WHERE feature IN (");
		for (String feature : featuresHashSet) {
			sb.append("'" + sanitizeString(feature) + "',");
		}
		sb.append(")");

		sb.deleteCharAt(sb.lastIndexOf(","));

		try {
			Connection connection = getMySqlConnection();
			Statement statement = connection.createStatement();

			statement.execute(sb.toString());

			ResultSet rs = statement.getResultSet();
			while (rs.next()) {
				String feature = rs.getString("feature");
				String vector = rs.getString("vector");

				sampleScoresHashMap.put(feature, vector);
			}

			rs.close();
			statement.close();
			connection.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return sampleScoresHashMap;
	}

	/**
	 * Get some metadata for the specified matrices.
	 * 
	 * @param matrixNames
	 * @return
	 */
	private static HashMap<String, HashMap<String, String>> getScoreMatrixMetadata(
			final ArrayList<String> matrixNames, String userId) {
		HashMap<String, HashMap<String, String>> matrixMetaData = new HashMap<String, HashMap<String, String>>();

		if (matrixNames.size() < 1) {
			return matrixMetaData;
		}

		StringBuffer sb = new StringBuffer();
		sb.append("SELECT name,vector,tableName,category,cohortMin,cohortMax");
		sb.append(" FROM " + SCORE_MATRICES_METATABLE_NAME);
		sb.append(" WHERE name in (");
		for (String matrixName : matrixNames) {

			if (matrixName.endsWith("_uploaded")) {
				matrixName = matrixName.replaceFirst("_uploaded", "");
			}

			sb.append("'" + sanitizeString(matrixName) + "',");
		}
		sb.append(")");

		sb.deleteCharAt(sb.lastIndexOf(","));

		try {
			Connection connection = getMySqlConnection();
			Statement statement = connection.createStatement();

			statement.execute(sb.toString());

			ResultSet rs = statement.getResultSet();
			while (rs.next()) {
				String vector = rs.getString("vector");
				String name = rs.getString("name");
				String tableName = rs.getString("tableName");
				String category = rs.getString("category");
				double cohortMin = rs.getDouble("cohortMin");
				double cohortMax = rs.getDouble("cohortMax");

				// TODO
				if (category.equalsIgnoreCase("uploaded")) {
					if (!tableName.startsWith("user_" + userId)) {
						continue;
					}
				}

				HashMap<String, String> metadataHashMap = new HashMap<String, String>();

				matrixMetaData.put(name, metadataHashMap);

				metadataHashMap.put("dataType", "sampleScores");
				metadataHashMap.put("sampleNames", vector);
				metadataHashMap.put("tableName", tableName);
				metadataHashMap.put("category", category);
				metadataHashMap.put("cohortMin", Double.toString(cohortMin));
				metadataHashMap.put("cohortMax", Double.toString(cohortMax));
			}

			rs.close();
			statement.close();
			connection.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return matrixMetaData;
	}

	/**
	 * Convenience method for getting multiple clinical feature data.
	 * 
	 * @param clinicalMatrixNames
	 *            Entries should end with "_clinical" and table and featureName
	 *            should be separated by "__".
	 * @return Keyed: tableName -> feature -> patient -> value
	 */
	private static HashMap<String, HashMap<String, HashMap<String, String>>> getClinicalData(
			ArrayList<String> clinicalMatrixNames) {

		HashMap<String, HashMap<String, HashMap<String, String>>> result = new HashMap<String, HashMap<String, HashMap<String, String>>>();

		for (String clinicalRequest : clinicalMatrixNames) {
			if (!CirclePlotter.datasetIsClinical(clinicalRequest)) {
				continue;
			}

			String[] requestFields = CirclePlotter
					.parseClinicalDatasetName(clinicalRequest);
			String tableName = requestFields[0];
			String feature = requestFields[1];

			HashMap<String, String> singleClinicalData = getClinicalData(
					tableName, feature);

			HashMap<String, HashMap<String, String>> tableHashMap;
			if (!result.containsKey(tableName)) {
				tableHashMap = new HashMap<String, HashMap<String, String>>();
				result.put(tableName, tableHashMap);
			} else {
				tableHashMap = result.get(tableName);
			}

			tableHashMap.put(feature, singleClinicalData);
		}

		return result;
	}

	/**
	 * Get specified feature value for each of the patients (patientID) in the
	 * clinical feature table.
	 * 
	 * @param tableName
	 * @param featureName
	 * @return Mapping of patientID to feature value.
	 */
	private static HashMap<String, String> getClinicalData(String tableName,
			String featureName) {

		HashMap<String, String> result = new HashMap<String, String>();

		Connection connection = DatabaseService.getMySqlConnection();
		Statement statement;

		String cleanFeatureName = sanitizeString(featureName);
		String cleanTableName = sanitizeString(tableName);

		StringBuffer sb = new StringBuffer("SELECT patientID,"
				+ cleanFeatureName);
		sb.append(" FROM `" + cleanTableName + "`");

		try {
			statement = connection.createStatement();
			statement.execute(sb.toString());

			ResultSet rs = statement.getResultSet();

			while (rs.next()) {
				String patientID = rs.getString("patientID");
				String feature = rs.getString(cleanFeatureName);

				result.put(patientID, feature);
			}

			rs.close();
			statement.close();
			connection.close();
		} catch (SQLException e) {
			System.out.println("sql error:" + sb.toString());
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Get all the available TCGA clinical data matrices.
	 * 
	 * @return
	 * @throws JSONException
	 */
	private static JSONArray getAvailableClinicalMatrices()
			throws JSONException {
		JSONArray resultJA = new JSONArray();

		Connection con = DatabaseService.getMySqlConnection();
		Statement s;
		try {
			s = con.createStatement();

			String sql = "SELECT tableName,fields FROM `"
					+ CLINICAL_DATA_METATABLE_NAME + "`";

			s.execute(sql);

			ResultSet rs = s.getResultSet();

			while (rs.next()) {
				JSONObject columnsJO = new JSONObject(rs.getString("fields"));
				JSONArray featuresJA = columnsJO.getJSONArray("columnNames");

				JSONArray filteredFeaturesJA = new JSONArray();
				for (int i = 0; i < featuresJA.length(); i++) {
					String feature = featuresJA.getString(i);
					if (feature.contains("_uuid")
							|| feature.equalsIgnoreCase("sampleid")
							|| feature.equalsIgnoreCase("patient_id")) {

						continue;
					} else {
						filteredFeaturesJA.put(feature);
					}
				}

				JSONObject tableJO = new JSONObject();
				resultJA.put(tableJO);

				tableJO.put("tableName", rs.getString("tableName"));
				tableJO.put("features", filteredFeaturesJA);
			}

			rs.close();
			s.close();
			con.close();
		} catch (SQLException e) {
			return resultJA;
		}

		return resultJA;
	}

	/**
	 * Get matrices that belong to this user.
	 * 
	 * @param userId
	 * @param ncbiTaxId
	 * @return
	 * @throws JSONException
	 */
	private static JSONArray getAvailableUserMatrices(String userId,
			String ncbiTaxId) throws JSONException {
		JSONArray matricesJA = new JSONArray();

		Connection con = DatabaseService.getMySqlConnection();
		Statement s;
		try {
			s = con.createStatement();

			StringBuffer sqlSb = new StringBuffer(
					"SELECT name,description,category");
			sqlSb.append(" FROM `" + SCORE_MATRICES_METATABLE_NAME + "`");
			sqlSb.append(" WHERE (NCBI_species='" + ncbiTaxId + "'");
			sqlSb.append(" AND category like 'uploaded_" + userId + "%')");
			sqlSb.append(" ORDER BY category,name ASC");

			s.execute(sqlSb.toString());

			ResultSet rs = s.getResultSet();

			while (rs.next()) {
				JSONObject matrixJO = new JSONObject();
				matricesJA.put(matrixJO);

				matrixJO.put("name", rs.getString("name"));
				String description = rs.getString("description");
				if (description == null) {
					description = "uploaded data";
				}
				matrixJO.put("description", description);
				matrixJO.put("category", "uploaded");
			}

			rs.close();
			s.close();
			con.close();
		} catch (SQLException e) {
			return matricesJA;
		}

		return matricesJA;
	}

	/**
	 * Get all the standard matrices for the specified organism.
	 * 
	 * @param ncbiTaxId
	 * @return
	 * @throws JSONException
	 */
	private static JSONArray getStandardMatrices(final String ncbiTaxId)
			throws JSONException {
		JSONArray matricesJA = new JSONArray();

		Connection con = DatabaseService.getMySqlConnection();
		Statement s;
		try {
			s = con.createStatement();

			StringBuffer sqlSb = new StringBuffer(
					"SELECT name,description,category");
			sqlSb.append(" FROM `" + SCORE_MATRICES_METATABLE_NAME + "`");
			sqlSb.append(" WHERE (NCBI_species='" + ncbiTaxId + "'");
			sqlSb.append(" AND category NOT LIKE 'uploaded')");
			sqlSb.append(" ORDER BY category,name ASC");

			s.execute(sqlSb.toString());

			ResultSet rs = s.getResultSet();

			while (rs.next()) {
				JSONObject matrixJO = new JSONObject();
				matricesJA.put(matrixJO);

				matrixJO.put("name", rs.getString("name"));
				matrixJO.put("description", rs.getString("description"));
				matrixJO.put("category", rs.getString("category"));
			}

			rs.close();
			s.close();
			con.close();
		} catch (SQLException e) {
			return matricesJA;
		}

		return matricesJA;
	}

	/**
	 * Get all the available matrices for the specified organism and user.
	 * Includes the standard matrices and the ones the user uploaded.
	 * 
	 * @param userId
	 * @param ncbiTaxId
	 * @return
	 * @throws JSONException
	 */
	private static String getAvailableMatrices(String userId, String ncbiTaxId)
			throws JSONException {
		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		resultJO.put("organism", ncbiTaxId);

		JSONArray standardMatricesJA = getStandardMatrices(ncbiTaxId);
		JSONArray uploadedMatricesJA = getAvailableUserMatrices(userId,
				ncbiTaxId);

		resultJO.put("matrices", standardMatricesJA);

		for (int i = 0; i < uploadedMatricesJA.length(); i++) {
			JSONObject jo = uploadedMatricesJA.getJSONObject(i);
			standardMatricesJA.put(jo);
		}

		// include clinical features
		JSONArray ja = getAvailableClinicalMatrices();
		resultJO.put("clinical", ja);

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);
		return jo.toString();
	}

}
