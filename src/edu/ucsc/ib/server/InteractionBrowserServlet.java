package edu.ucsc.ib.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import edu.ucsc.ib.client.BiodeInfo;

public class InteractionBrowserServlet extends RemoteServiceServlet {

	public static final String JSON_MIME_TYPE = "application/json";

	private static final long serialVersionUID = 2694633730232699545L;

	protected static final String PROP_FILE = "ib.properties";

	protected static final String ROOT_PATH_PROP = "ib.rootpath";

	protected Properties localProperties;

	protected String rootPath;

	protected boolean isLoggerOn = true;

	public void init() {
		localProperties = new Properties();
		InputStream is = InteractionBrowserServlet.class
				.getResourceAsStream(PROP_FILE);
		try {
			localProperties.load(is);
		} catch (IOException ioe) { /* leave localProperties empty */
		}
		rootPath = getParameter(ROOT_PATH_PROP);
		if (rootPath == null) {
			throw new IllegalArgumentException("Unable to read root path from "
					+ "either servlet configuration or " + PROP_FILE);
		}
		if (File.pathSeparator.equals("\\")) {
			rootPath = rootPath.replace('/', '\\');
		}
		if (!(new File(rootPath)).isAbsolute()) {
			String base = getServletContext().getRealPath(".");
			rootPath = base + File.separatorChar + rootPath;
		}
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

	protected String getParameter(String param) {
		String result = null;
		ServletContext sc = getServletContext();
		if (sc != null) {
			result = getServletContext().getInitParameter(param);
		}
		if (result == null) {
			result = localProperties.getProperty(param);
		}
		return result;
	}

	protected void permanentRedirect(String url, HttpServletResponse resp)
			throws IOException {
		resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
		resp.setHeader("Location", url);

		PrintWriter p = resp.getWriter();
		p.append("Resource located at..."); // TODO use proper redirect template
	}

	/**
	 * Write a string to the response as "text/html" content type.
	 * 
	 * @param response
	 * @param resp
	 * @return "true" if successful
	 * @throws IOException
	 */
	protected boolean writeTextResponse(String response,
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
	 * CAREFUL!!! THIS IS NOT YET COMPLETE
	 * 
	 * TODO examine bash man page extensively
	 * 
	 * @param s
	 *            string to be escaped
	 * @return a String that will be interpreted as only a string by bash
	 */
	public final static String escapeBashString(String s) {
		return s;
	}

	public final static String sanitizeBiodeSpaceID(String biodeSpace) {
		return sanitizeLettersAndSlash(biodeSpace);
	}

	public final static String sanitizeTrackName(String trackName) {
		return sanitizeLettersAndSlash(trackName);
	}

	/**
	 * NOTE: THERE IS PROBABLY A BETTER WAY TO DO THIS. IT IS PROBABLY BETTER TO
	 * RETURN 404 ERROR OR SOMETHING IN CASE A TRACK NAME IS BAD.
	 * 
	 * @param s
	 * @return
	 */
	private final static String sanitizeLettersAndSlash(String s) {
		return (s == null) ? null : s.replaceAll("[^_a-zA-Z0-9/]", "")
				.replaceAll("/+", "/").replaceAll("^/", "")
				.replaceAll("/$", "");
	}

	/**
	 * Get a JSONObject representation of a BiodeInfo object.
	 * 
	 * @param bi
	 * @return an org.json.JSONObject object
	 * @throws JSONException
	 */
	protected final static JSONObject getJSONObjectFromBiodeInfo(BiodeInfo bi)
			throws JSONException {
		JSONObject jo = new JSONObject();

		jo.put("commonName", bi.getCommonName());
		jo.put("systematicName", bi.getSystematicName());
		jo.put("description", bi.getDescription());
		jo.put("systemSpace", bi.getSystemSpace());
		jo.put("biodeSpace", bi.getBiodeSpace());

		return jo;
	}
}