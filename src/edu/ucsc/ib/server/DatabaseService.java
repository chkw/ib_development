package edu.ucsc.ib.server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Superclass for servlet classes that need to access the Interaction Browser
 * data from the MySQL datasource.
 * 
 * datasource configuration help:
 * http://www.crazysquirrel.com/computing/java/connection-pooling.jspx
 * http://jianbo.myebill.co.uk/2007/02/configure-mysql-jdbc-datasource-in.html
 * http://www.isocra.com/2007/10/jndi-problems-with-tomcat-5515/
 * http://www.jdocs
 * .com/dbcp/1.2.1/org/apache/commons/dbcp/datasources/package-summary.html
 * 
 * https://twiki.soe.ucsc.edu/twiki/bin/view/SysBio/DataSource
 * 
 * @author Chris
 * 
 */
public class DatabaseService extends HttpServlet {

	/**
	 * If true, log messages to System.out via logSysOut() method.
	 */
	private static final boolean isLoggerOn = true;

	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 3275299141862818963L;

	private static Properties localProperties;
	private static final String PROP_FILE = "ib.properties";

	/**
	 * Number of seconds to use for maximum inactive interval of session.
	 */
	private static final int SESSION_MAX_INACTIVE_INTERVAL = 60 * 60 * 24 * 2;

	private static final String JNDI_CONTEXT_NAME = "comp/env";
	private static final String JNDI_OBJECT_NAME_SYSBIODATA = "jdbc/sysbioData";
	private static final String JNDI_OBJECT_NAME_IBCUSTOM = "jdbc/ibCustom";

	private static javax.naming.Context context;
	private static javax.sql.DataSource dataSource_sysbioData;
	private static javax.sql.DataSource dataSource_ibCustom;

	protected static final HashMap<String, String> aliasTableNameHash = new HashMap<String, String>();
	protected static final HashMap<String, String> annotTableNameHash = new HashMap<String, String>();
	protected static final HashMap<String, String> invIndexTableNameHash = new HashMap<String, String>();
	protected static final HashMap<String, String> modesTableNameHash = new HashMap<String, String>();

	protected static final HashMap<String, String> bestBlastTableNameHash = new HashMap<String, String>();

	private static final ArrayList<Character> allowedList = new ArrayList<Character>();
	static {
		String allowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789():@.,-_+/\\";
		char[] allowChars = allowed.toCharArray();
		for (char c : allowChars) {
			allowedList.add(c);
		}
	}

	// TODO ///////////////////////////////////////////

	/**
	 * Constructor for this service. Creates and caches the JNDI dataSource and
	 * JNDI context of the connection pool. The dataSource can be used to get a
	 * pooled connection to a MySQL database.
	 */
	public DatabaseService() {
		super();

		// load properties
		localProperties = new Properties();
		InputStream is = DatabaseService.class.getResourceAsStream(PROP_FILE);
		try {
			localProperties.load(is);
		} catch (IOException ioe) { /* leave localProperties empty */
		}

		// set up JNDI
		context = this.getContext(DatabaseService.JNDI_CONTEXT_NAME);
		dataSource_sysbioData = this.getJNDIdatasource(context,
				JNDI_OBJECT_NAME_SYSBIODATA);
		dataSource_ibCustom = this.getJNDIdatasource(context,
				JNDI_OBJECT_NAME_IBCUSTOM);

		// set up table mappings

		// aliasTableNameHash.put("210", "alias_Hpylori");
		aliasTableNameHash.put("4932", "alias_Yeast");
		aliasTableNameHash.put("6239", "alias_Worm");
		// aliasTableNameHash.put("7227", "alias_Fly");
		aliasTableNameHash.put("9606", "alias_Human");
		aliasTableNameHash.put("10090", "alias_Mouse");
		// aliasTableNameHash.put("10116", "alias_Rat");
		aliasTableNameHash.put("drug", "alias_drug");

		// annotTableNameHash.put("210", "annot_Hpylori");
		annotTableNameHash.put("4932", "annot_Yeast");
		annotTableNameHash.put("6239", "annot_Worm");
		// annotTableNameHash.put("7227", "annot_Fly");
		annotTableNameHash.put("9606", "annot_Human");
		annotTableNameHash.put("10090", "annot_Mouse");
		// annotTableNameHash.put("10116", "annot_Rat");
		annotTableNameHash.put("drug", "annot_drug");

		// invIndexTableNameHash.put("210", "hpylori_inverted_index");
		invIndexTableNameHash.put("4932", "yeast_inverted_index");
		invIndexTableNameHash.put("6239", "worm_inverted_index");
		// invIndexTableNameHash.put("7227", "fly_inverted_index");
		invIndexTableNameHash.put("9606", "human_inverted_index");
		invIndexTableNameHash.put("10090", "mouse_inverted_index");
		// invIndexTableNameHash.put("10116", "rat_inverted_index");

		// modesTableNameHash.put("210", "hpylori_modes");
		modesTableNameHash.put("4932", "yeast_modes");
		modesTableNameHash.put("6239", "worm_modes");
		// modesTableNameHash.put("7227", "fly_modes");
		modesTableNameHash.put("9606", "human_modes");
		modesTableNameHash.put("10090", "mouse_modes");
		// modesTableNameHash.put("10116", "rat_modes");

		bestBlastTableNameHash.put("9606", "best_pBlast_Human");
		bestBlastTableNameHash.put("6239", "best_pBlast_Worm");
		bestBlastTableNameHash.put("4932", "best_pBlast_Yeast");
		bestBlastTableNameHash.put("10090", "best_pBlast_Mouse");
	}

	/**
	 * Print a message to System.out .
	 * 
	 * @param message
	 */
	protected void logSysOut(String message) {
		if (isLoggerOn) {
			System.out.println(message);
		}
	}

	/**
	 * Look up a javax.naming.Context object from initial context. For example,
	 * retrieve the JNDI context, "comp/env", from the initial context, "java:".
	 * This should be cached as an instance or static variable, as it can be
	 * quite expensive to create a JNDI context.
	 * 
	 * @param JNDIcontextName
	 *            such as "comp/env"
	 * @return
	 */
	private javax.naming.Context getContext(String JNDIcontextName) {
		javax.naming.Context envContext = null;
		try {
			javax.naming.Context initCtx = new InitialContext();
			String envContextName = initCtx.getNameInNamespace()
					+ JNDIcontextName;
			envContext = (Context) initCtx.lookup(envContextName);
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return envContext;
	}

	/**
	 * Lookup a JNDI DataSource, which will be backed by a pool that the
	 * application server provides. DataSource instances are also a good
	 * candidate for caching as an instance variable, as JNDI lookups can be
	 * expensive as well.
	 * 
	 * @param ctx
	 * @param jndiName
	 *            such as "jdbc/ibdata"
	 * @return
	 */
	private javax.sql.DataSource getJNDIdatasource(javax.naming.Context ctx,
			String jndiName) {
		javax.sql.DataSource ds = null;
		try {
			ds = (DataSource) ctx.lookup(jndiName);
		} catch (NamingException e) {
			e.printStackTrace();
		}

		return ds;
	}

	/**
	 * Get a MySQL connection from a JNDI datasourse. The returned MySQL
	 * connection works with JDBC. Remember that the connection should be closed
	 * as soon as possible so to avoid connection pool leaks.
	 * 
	 * @param ds
	 * @return
	 */
	private static java.sql.Connection getMySqlConnection(
			javax.sql.DataSource ds) {
		java.sql.Connection con = null;
		try {
			con = ds.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return con;
	}

	/**
	 * Get a connection to DatabaseService's datasource for annotation, track
	 * data, etc.
	 * 
	 * @return
	 */
	protected static java.sql.Connection getMySqlConnection() {
		return getMySqlConnection(DatabaseService.dataSource_sysbioData);
	}

	/**
	 * Get a connection to DatabaseService's datasource for custom tracks, etc.
	 * 
	 * @return
	 */
	protected static java.sql.Connection getMySqlConnection_custom() {
		// return this.getMySqlConnection(DatabaseService.dataSource_ibCustom);
		return getMySqlConnection(DatabaseService.dataSource_sysbioData);
	}

	/**
	 * Handle GET request. Hand over to doPost() method.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		System.out.println("DatabaseService.doGet");
		System.out.println("--handing over to DatabaseService.doPost");
		this.doPost(req, resp);
	}

	/**
	 * Handle POST request.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		System.out.println("DatabaseService.doPost");
		System.out.println("requestURI is:\t" + req.getRequestURI());

		String path = req.getPathInfo();

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/db/test

			try {
				DatabaseService.writeTextResponse(
						"success? 1:"
								+ this.testConnection(dataSource_sysbioData)
								+ " 2:"
								+ this.testConnection(dataSource_ibCustom),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("classTest")) {
			// test with:
			// http://localhost:8080/ib/data/db/classTest

			this.testDataSourceClasses();
		} else {
			// nothing to do !
		}
	}

	/**
	 * Get a connection and close it.
	 * 
	 * @param dataSource
	 * @return true if it worked, false otherwise.
	 */
	private boolean testConnection(DataSource dataSource) {
		boolean result = false;
		Connection con = getMySqlConnection(dataSource);
		if (con != null) {
			try {
				String sql = "SHOW TABLES";
				Statement s = con.createStatement();
				s.execute(sql);

				con.close();
				result = true;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * Test DatabaseService's connection to its datasource.
	 * 
	 * @return
	 */
	protected boolean testConnection() {
		return this.testConnection(DatabaseService.dataSource_sysbioData);
	}

	/*
	 * Test to check presence and location of some classes.
	 */
	private void testDataSourceClasses() {
		// A list of classes to try and load and see where they come from
		String[] classNames = { "com.mysql.jdbc.Driver",
				"org.apache.tomcat.dbcp.datasources.SharedPoolDataSource",
				"org.apache.tomcat.dbcp.dbcp.BasicDataSource",
				"org.apache.tomcat.dbcp.dbcp.PoolableConnection" };

		for (int i = 0; i < classNames.length; i++) {
			String className = classNames[i];
			System.out.print("" + className + "");
			try {
				// try and load the class
				Class c = Class.forName(className);
				System.out.println(" loaded successfully:  " + c + "");
				// if successful, find out the location (gives the location of
				// the jar)
				ProtectionDomain pDomain = c.getProtectionDomain();
				CodeSource cSource = pDomain.getCodeSource();
				System.out.println("    Location:  " + cSource.getLocation()
						+ "");
			} catch (ClassNotFoundException e) {
				System.out.println("Error could not find class");
			} catch (Throwable t) {
				System.out.println("Error could not load (" + t + ")");
			}
			System.out.println("");

		}
	}

	/**
	 * Output parameter key-value pairs in an HttpServletRequest. Used for
	 * debugging.
	 * 
	 * @param req
	 */
	protected static void printReqParams(HttpServletRequest req) {
		System.out.println("PARAMETER begin");
		Enumeration<String> paraNamesEnum = req.getParameterNames();
		while (paraNamesEnum.hasMoreElements()) {
			String paraName = paraNamesEnum.nextElement();
			System.out.println("PARAMETER " + paraName + "="
					+ req.getParameter(paraName));
		}
		System.out.println("PARAMETER end");
	}

	/**
	 * Output header key-value pairs in an HttpServletRequest. Used for
	 * debugging.
	 * 
	 * @param req
	 */
	protected static void printHeaderInfo(HttpServletRequest req) {
		System.out.println("HEADER begin");
		Enumeration<String> enumHeader = req.getHeaderNames();
		while (enumHeader.hasMoreElements()) {
			String key = enumHeader.nextElement();
			System.out.println("HEADER " + key + " : " + req.getHeader(key));
		}
		System.out.println("HEADER end");
	}

	/**
	 * Write a string to the response as "text/html" content type.
	 * 
	 * @param response
	 * @param resp
	 * @return "true" if successful
	 * @throws IOException
	 */
	protected static boolean writeTextResponse(String response,
			HttpServletResponse resp) throws IOException {
		boolean result = false;
		resp.setContentType("text/html");
		PrintWriter respWriter = resp.getWriter();
		respWriter.println(response);
		respWriter.close();
		result = true;
		return result;
	}

	/**
	 * Write a JSON-RPC response object as text/html.
	 * 
	 * @param resultJO
	 * @param errorMessage
	 * @param id
	 * @param resp
	 * @return
	 * @throws IOException
	 */
	protected static boolean writeJSONRPCTextResponse(
			final JSONObject resultJO, final String errorMessage, final int id,
			HttpServletResponse resp) throws IOException {
		boolean result = false;
		resp.setContentType("text/html");
		PrintWriter respWriter = resp.getWriter();
		respWriter.println(DatabaseService.encodeJsonRpcResponse(resultJO,
				errorMessage, id));
		respWriter.close();
		result = true;
		return result;
	}

	/**
	 * Modify a comma-separated list into a String that can be used as a MySQL
	 * list. Use the result as part of an IN expression, for example. Turns
	 * a,b,c into this: 'a','b','c' . Each item appears once in the result. Each
	 * item is also sanitized.
	 * 
	 * @param csvList
	 * @return
	 */
	protected static String csvToMySqlList(String csvList) {
		Set<String> itemSet = new HashSet<String>(Arrays.asList(csvList
				.split(",")));

		if (!(itemSet.size() > 0)) {
			return "";
		} else {
			StringBuffer sb = new StringBuffer();
			for (String s : itemSet) {
				if (!s.equalsIgnoreCase("")) {
					sb.append(",'" + sanitizeString(s) + "'");
				}
			}
			return sb.toString().replaceFirst(",", "");
		}
	}

	/**
	 * Calculate the number of possible pairwise combinations, without regard to
	 * the order. [n*(n-1)*0.5]
	 * 
	 * @param count
	 * @return
	 */
	protected static int crossNumberAnyOrder(int count) {
		return (int) (count * (count - 1) * 0.5);
	}

	/**
	 * Calculate the logGamma. This method taken from Stuart lab's libstats.pl .
	 * 
	 * @param xx
	 * 
	 * @return
	 */
	private static double logGamma(int xx) {
		double[] cof = { 0.0, 76.18009172947146, -86.50532032941677,
				24.01409824083091, -1.231739572450155, 0.1208650973866179e-2,
				-0.5395239384953e-5 };
		double stp = 2.5066282746310005;

		int x = xx;
		int y = x;
		double tmp = x + 5.5;
		tmp = (x + 0.5) * Math.log(tmp) - tmp;
		double ser = 1.000000000190015;
		for (double j : cof) {
			y += 1.0;
			ser += j / y;
		}

		return tmp + Math.log(stp * ser / x);
	}

	/**
	 * Calculate log-choose. This method taken from Stuart lab's libstats.pl .
	 * 
	 * @param k
	 * @param N
	 * @return
	 */
	private static double logChoose(int k, int N) {
		return k > N ? 0 : logGamma(N + 1) - logGamma(k + 1)
				- logGamma(N - k + 1);
	}

	/**
	 * Add the log. This method taken from Stuart lab's libstats.pl .
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private static double addLog(double x, double y) {
		if (x == -Double.MAX_VALUE) {
			return y;
		}
		if (y == -Double.MAX_VALUE) {
			return x;
		}
		if (x >= y) {
			y -= x;
		} else {
			double t = x;
			x = y;
			y = t - x;
		}

		return x + Math.log(1 + Math.exp(y));
	}

	/**
	 * Compute the hypergeometric p-value. This method taken from Stuart lab's
	 * libstats.pl .
	 * 
	 * @param k
	 *            number of successes in experiment
	 * @param n
	 *            number of draws in experiment
	 * @param K
	 *            total possible successes
	 * @param N
	 *            total drawable items
	 * @return
	 */
	protected static double computeHyperPValue(int k, int n, int K, int N) {
		double p = K / N;
		double d = (k < p * n) ? -1 : +1;
		double PVal = -Double.MAX_VALUE;

		for (; k >= 0 & k <= n; k += d) {
			double x = logChoose(k, K) + logChoose(n - k, N - K)
					- logChoose(n, N);
			PVal = addLog(PVal, x);
		}
		// return Math.exp(PVal);
		return PVal;
	}

	/**
	 * Get a String of comma-separated values from an array of String
	 * 
	 * @param strArray
	 * @return
	 */
	protected static String arrayToCommaSeparatedString(Object[] strArray) {
		StringBuffer strBuf = new StringBuffer();
		for (int i = 0; i < strArray.length; i++) {
			if (((String) strArray[i]).length() > 0) {
				strBuf.append(strArray[i] + ",");
			}
		}
		return strBuf.toString();
	}

	/**
	 * Turn Set of String into a mysql list. Sanitizes each String.
	 * 
	 * @param strSet
	 * @return
	 */
	protected static String setToMysqlList(final Set<String> strSet) {
		if (!(strSet.size() > 0)) {
			return "''";
		} else {
			StringBuffer sb = new StringBuffer();
			for (String s : strSet) {
				if (!s.equalsIgnoreCase("")) {
					sb.append(",'" + sanitizeString(s) + "'");
				}
			}
			return sb.toString().replaceFirst(",", "");
		}
	}

	/**
	 * Get a list of org.apache.commons.fileupload.FileItem objects from a
	 * multipart request.
	 * 
	 * @param req
	 * @return List of org.apache.commons.fileupload.FileItem objects
	 */
	protected static List<FileItem> parseMultipartRequest(HttpServletRequest req) {
		// Create a factory for disk-based file items
		FileItemFactory factory = new DiskFileItemFactory();

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);

		// Parse the request
		// ends up with list of FileItem objects
		List<FileItem> items = null;
		try {
			items = upload.parseRequest(req);
		} catch (FileUploadException e) {
			e.printStackTrace();
		}
		return items;
	}

	/**
	 * Get a Map representation of a List of
	 * org.apache.commons.fileupload.FileItem objects.
	 * 
	 * @param items
	 * @return
	 */
	protected static Map<String, String> getParamaterMapping(
			List<FileItem> items) {
		Map<String, String> mapping = new HashMap<String, String>();

		// Process the uploaded items
		Iterator<FileItem> iter = items.iterator();
		while (iter.hasNext()) {
			FileItem item = iter.next();

			if (item.isFormField()) {
				// for form fields
				mapping.put(item.getFieldName(), item.getString());
			} else {
				// for file upload
				mapping.put(item.getFieldName(), item.getString());
			}
		}
		return mapping;
	}

	/**
	 * Get a session for the HttpServletRequest and set its MaxInactiveInterval.
	 * 
	 * @param req
	 * @return
	 */
	protected static HttpSession InitSession(HttpServletRequest req) {
		HttpSession session = req.getSession(true);
		session.setMaxInactiveInterval(SESSION_MAX_INACTIVE_INTERVAL);
		return session;
	}

	/**
	 * Check to see if a given attribute name has been used in a session.
	 * 
	 * @param session
	 * @param attrName
	 * @return
	 */
	protected static boolean sessionHasAttribute(HttpSession session,
			String attrName) {
		Enumeration<String> attrNameEnum = session.getAttributeNames();
		while (attrNameEnum.hasMoreElements()) {
			if (attrNameEnum.nextElement().equals(attrName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the value of a property from the properties file.
	 * 
	 * @param propertyName
	 *            property's key
	 * @return String
	 */
	protected String getProperty(String propertyName) {
		String result = null;
//		ServletContext sc = getServletContext();
//		if (sc != null) {
//			result = getServletContext().getInitParameter(propertyName);
//		}
		if ((result == null) && (localProperties != null)) {
			result = localProperties.getProperty(propertyName);
		}
		return result;
	}

	/**
	 * Sanitize a String by removing any Character not in the allowed list.
	 * 
	 * @param inputStr
	 * @return
	 */
	public static String sanitizeString(String inputStr) {
		StringBuffer sb = new StringBuffer();
		for (char c : inputStr.toCharArray()) {
			if (allowedList.contains(c)) {
				sb.append(c);
			} else {
				// do nothing
			}
		}
		return sb.toString();
	}

	/**
	 * Get a String appropriate for DB table name.
	 * 
	 * @param inputStr
	 * @return
	 */
	public static String sanitizedTableName(String inputStr) {
		String tableName = sanitizeString(inputStr).replaceAll(" ", "_")
				.replaceAll("\\.", "_").replaceAll("\\(", "_")
				.replaceAll("\\)", "_").replaceAll("\\:", "_")
				.replaceAll("\\@", "_").replaceAll("\\-", "_")
				.replaceAll("\\+", "_").replaceAll("\\/", "_")
				.replaceAll("\\\\", "_");
		return tableName;
	}

	/**
	 * Get a JSONObject that is a JSON-RPC v1.0 compliant response.
	 * 
	 * @param resultJO
	 *            If this is not null, then a result object will be returned,
	 *            and the error object will be null.
	 * @param errorMessage
	 *            If this is not null, then an error object will be returned,
	 *            and the result object will be null.
	 * @param id
	 * @return
	 */
	@SuppressWarnings("finally")
	protected static JSONObject encodeJsonRpcResponse(
			final JSONObject resultJO, final String errorMessage, final int id) {
		JSONObject jo = new JSONObject();

		try {
			if (errorMessage == null) {
				jo.put("error", JSONObject.NULL);
				jo.put("result", resultJO);
			} else {
				JSONObject errorJO = new JSONObject();
				errorJO.put("message", errorMessage);
				jo.put("error", errorJO);
				jo.put("result", JSONObject.NULL);
			}
			jo.put("id", id);
		} catch (JSONException e) {
			e.printStackTrace();

		} finally {
			return jo;
		}
	}

	/**
	 * Number Comparator for JSONObject. The field for comparing is specified.
	 * 
	 * @author cw
	 * 
	 */
	public static class JSONNumberComparator implements Comparator<JSONObject> {

		/**
		 * name of JSON field to compare
		 */
		private String fieldName;

		/**
		 * Number Comparator for JSONObject. The field for comparing is
		 * specified.
		 * 
		 * @param fieldName
		 */
		JSONNumberComparator(final String fieldName) {
			this.fieldName = fieldName;
		}

		@Override
		public int compare(JSONObject o1, JSONObject o2) {
			double val0, val1 = 0;
			int result = 0;
			try {
				val0 = o1.getDouble(this.fieldName);
				val1 = o2.getDouble(this.fieldName);

				if (val0 > val1) {
					result = 1;
				} else if (val0 < val1) {
					result = -1;
				} else {
					result = 0;
				}
			} catch (JSONException e) {
				e.printStackTrace();
				return 0;
			}
			return result;
		}
	}

	/**
	 * String Comparator for JSONObject. The field for comparing is specified.
	 * 
	 * @author cw
	 * 
	 */
	public static class JSONStringComparator implements Comparator<JSONObject> {

		/**
		 * name of JSON field to compare
		 */
		private String fieldName;

		/**
		 * String Comparator for JSONObject. The field for comparing is
		 * specified.
		 * 
		 * @param fieldName
		 */
		JSONStringComparator(final String fieldName) {
			this.fieldName = fieldName;
		}

		@Override
		public int compare(JSONObject o1, JSONObject o2) {
			try {
				String val0 = o1.getString(this.fieldName);
				String val1 = o2.getString(this.fieldName);

				return val0.compareToIgnoreCase(val1);
			} catch (JSONException e) {
				e.printStackTrace();
				return 0;
			}
		}
	}

	/**
	 * Turn a JSONArray of strings into an ArrayList of strings.
	 * 
	 * @param conceptsJA
	 * @return
	 */
	public static ArrayList<String> jsonArrayToStringArray(
			final JSONArray conceptsJA) {
		ArrayList<String> result = new ArrayList<String>();

		int size = conceptsJA.length();

		for (int i = 0; i < size; i++) {
			try {
				result.add(conceptsJA.getString(i));
			} catch (JSONException e) {
				e.printStackTrace();
				continue;
			}
		}
		return result;
	}

	/**
	 * Return the result of a URLConnection for the URL.
	 * 
	 * @param urlString
	 * @return
	 */
	protected static String getUrlResult(final String urlString) {

		System.err.println("url: " + urlString);

		StringBuffer sb = new StringBuffer();
		URL url;
		try {
			url = new URL(urlString);
			URLConnection conn;
			conn = url.openConnection();
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));

			String line;
			ArrayList<String> results = new ArrayList<String>();
			while ((line = rd.readLine()) != null) {
				results.add(line);
			}
			rd.close();

			sb.append(results.get(0));
		} catch (MalformedURLException e) {
			System.err
					.println("\n"
							+ "URL is not exist or protocol does not exist or there is a typo in the submitted URL"
							+ "\n");
			e.printStackTrace();
		} catch (IOException e) {
			System.err
					.println("\n"
							+ "I/O Exception in the connection try again or contact developer."
							+ "\n");
			e.printStackTrace();
		}
		return sb.toString();
	}

	/**
	 * Create a trust manager that does not validate certificate chains like the
	 * default TrustManager. This routine right is what Netscape and IE do when
	 * they receive a certificate that is not their KeyStore. The only
	 * difference is that this code does not ask you to accept it.
	 * 
	 * @see <a
	 *      href="http://www.thatsjava.com/java-tech/38173/">www.thatsjava.com/java-tech/38173/</a>
	 * 
	 * @param strUrl
	 * @return
	 */
	protected static String getHttpsResponse_trust_everybody(final String strUrl) {

		System.err.println("url: " + strUrl);

		StringBuffer sb = new StringBuffer();

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		// Let us create the factory where we can set some parameters for
		// the connection

		SSLContext sc;
		try {
			sc = SSLContext.getInstance("SSL");

			sc.init(null, trustAllCerts, new java.security.SecureRandom());

			// Create the socket connection and open it to the secure remote web
			// server

			URL url = new URL(strUrl);

			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());

			HttpsURLConnection connection = (HttpsURLConnection) url
					.openConnection();

			// Once the connection is open to the remote server we have to
			// replace the default HostnameVerifier with one of our own since we
			// want the client to bypass the peer and submitted host checks even
			// if they are not equal. If this routine were not here, then this
			// client would claim that the submitted host and the peer host are
			// not equal.

			connection.setHostnameVerifier(new HostnameVerifier() {
				public boolean verify(String rserver, SSLSession sses) {
					if (!rserver.equals(sses.getPeerHost())) {
						System.out.println("certificate <" + sses.getPeerHost()
								+ "> does not match host <" + rserver
								+ "> but " + "continuing anyway");
					}
					return true;
				}
			});

			// Make this URL connection available for input and output

			connection.setDoOutput(true);

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));

			// read the lines
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			rd.close();

			// catch lots of exceptions
		} catch (NoSuchAlgorithmException e) {
			System.err
					.println("\n"
							+ "The context specified does not exist. Check for the existence of JSSE"
							+ "\n");
			e.printStackTrace();
		} catch (KeyManagementException e) {
			System.err.println("\n" + "KeyManagementException" + "\n");
			e.printStackTrace();
		} catch (MalformedURLException e) {
			System.err
					.println("\n"
							+ "URL is not exist or protocol does not exist or there is a typo in the submitted URL"
							+ "\n");
			e.printStackTrace();
		} catch (IOException e) {
			System.err
					.println("\n"
							+ "I/O Exception in the connection try again or contact developer."
							+ "\n");
			e.printStackTrace();
		}

		return sb.toString();
	}

	/**
	 * Check if the table exists in the DB connection. Connection is not closed.
	 * 
	 * @param tableName
	 * @param connection
	 * @return
	 */
	public static boolean tableExists(final String tableName,
			final Connection connection) {
		try {
			Statement statement = connection.createStatement();
			String sql = "SELECT 1 FROM `" + sanitizeString(tableName) + "`";
			statement.execute(sql);
			statement.close();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * Get the current time in milliseconds.
	 * 
	 * @return
	 */
	public static long getCurrentTime() {
		return System.currentTimeMillis();
	}

	/**
	 * Show some stats about the JVM
	 */
	public static void showJVMstats() {
		Runtime runtime = Runtime.getRuntime();

		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		double bytesPerMegabyte = Math.pow(1024, 2);

		System.err.println("** MEMORY (MB) **");
		System.err.println("  free: " + freeMemory / bytesPerMegabyte);
		System.err
				.println("  allocated: " + allocatedMemory / bytesPerMegabyte);
		System.err.println("  max: " + maxMemory / bytesPerMegabyte);
		System.err.println("  total free: "
				+ (freeMemory + (maxMemory - allocatedMemory))
				/ bytesPerMegabyte);
		System.err.println("*****************");

		/* Get a list of all filesystem roots on this system */
		// File[] roots = File.listRoots();
		//
		// /* For each filesystem root, print some info */
		// for (File root : roots) {
		// System.out.println("File system root: " + root.getAbsolutePath());
		// System.out.println("Total space (bytes): " + root.getTotalSpace());
		// System.out.println("Free space (bytes): " + root.getFreeSpace());
		// System.out
		// .println("Usable space (bytes): " + root.getUsableSpace());
		// }

	}

	/**
	 * Get duplicated column values from a table.
	 * 
	 * @param colName
	 *            column is taken as a string
	 * @param tableName
	 * @param connection
	 *            remains open throughout the method
	 * @return
	 */
	public static HashSet<String> getDuplicatesFromTable(final String colName,
			final String tableName, final Connection connection) {
		HashSet<String> duplicateValues = new HashSet<String>();
		StringBuffer sb = new StringBuffer();

		sb.append("SELECT t." + colName + ",COUNT(t." + colName + ") ");
		sb.append("FROM `" + tableName + "` AS t ");
		sb.append("GROUP BY t." + colName + " ");
		sb.append("HAVING ( COUNT(t." + colName + ") > 1 )");

		try {
			Statement statement = connection.createStatement();
			statement.execute(sb.toString());

			ResultSet rs = statement.getResultSet();
			while (rs.next()) {
				duplicateValues.add(rs.getString("t.concept"));
			}

			rs.close();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return duplicateValues;
		}

		return duplicateValues;
	}

	/**
	 * Use java.util.zip.Deflater to compress a String into byte array. Byte
	 * array can be stored as MySql blob.
	 * 
	 * @see <a
	 *      href="http://www.dscripts.net/2010/06/04/compress-and-uncompress-a-java-byte-array-using-deflater-and-enflater/">http://www.dscripts.net/2010/06/04/compress-and-uncompress-a-java-byte-array-using-deflater-and-enflater/</a>
	 * 
	 * @param data
	 * @return
	 */
	public static byte[] compressBytes(final String data) {
		byte[] input = null;
		// input = data.getBytes("UTF-8");
		input = data.getBytes();

		// the format... data is the total string

		Deflater df = new Deflater();
		// this function mainly generate the byte code

		// df.setLevel(Deflater.BEST_COMPRESSION);
		df.setInput(input);

		ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
		// we write the generated byte code in this array

		df.finish();
		byte[] buff = new byte[1024];
		// segment segment pop....segment set 1024

		while (!df.finished()) {
			int count = df.deflate(buff);
			// returns the generated code... index
			baos.write(buff, 0, count);
			// write 4m 0 to count
		}
		try {
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] output = baos.toByteArray();

		// System.out.println("Original: "+input.length);
		// System.out.println("Compressed: "+output.length);
		return output;
	}

	/**
	 * Use java.util.zip.Inflater to get a String from a compress byte array.
	 * Byte array can be a MySql blob.
	 * 
	 * @see <a
	 *      href="http://www.dscripts.net/2010/06/04/compress-and-uncompress-a-java-byte-array-using-deflater-and-enflater/">http://www.dscripts.net/2010/06/04/compress-and-uncompress-a-java-byte-array-using-deflater-and-enflater/</a>
	 * 
	 * @param input
	 * @return
	 */
	public static String extractBytes(final byte[] input) {
		Inflater ifl = new Inflater();
		// mainly generate the extraction

		// df.setLevel(Deflater.BEST_COMPRESSION);
		ifl.setInput(input);

		ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
		byte[] buff = new byte[1024];
		try {
			while (!ifl.finished()) {
				int count;
				count = ifl.inflate(buff);
				baos.write(buff, 0, count);
			}
			baos.close();
		} catch (DataFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] output = baos.toByteArray();

		// System.out.println("Original: "+input.length);
		// System.out.println("Extracted: "+output.length);

		// System.out.println("Data:");
		// System.out.println(new String(output));
		return new String(output);
	}

	/**
	 * Generate md5 hash of a String.
	 * 
	 * @param string
	 * @return
	 */
	public static String getMD5Hash(String string) {
		String hashword = null;
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(string.getBytes());
			BigInteger hash = new BigInteger(1, md5.digest());
			hashword = hash.toString(16);

			// md5 hash may have leading 0's that got truncated
			while (hashword.length() < 32) {
				hashword = "0" + hashword;
			}
		} catch (NoSuchAlgorithmException nsae) {
			nsae.printStackTrace();
		}
		return hashword;
	}
}
