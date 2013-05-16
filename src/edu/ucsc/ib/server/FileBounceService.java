package edu.ucsc.ib.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * This is a servlet for bouncing a String back to the client as a file. It is
 * intended as a workaround for JavaScript's inability to save a file directly
 * on the client machine.
 * 
 * @author Chris
 * 
 */
public class FileBounceService extends HttpServlet {

	/**
	 * Compiler generated serial version ID
	 */
	private static final long serialVersionUID = -615068100356524893L;

	/**
	 * Path suffix for SVG file.
	 */
	private static final String SVG_SUFFIX = "SVG";

	/**
	 * Path suffix for links file.
	 */
	private static final String TAB_SUFFIX = "tab";

	/**
	 * Path suffix for node list file.
	 */
	private static final String LIST_SUFFIX = "list";

	/**
	 * Path suffix for encoding xgmml file.
	 */
	private static final String XGMML_ENCODER = "xgmml_encode";

	/**
	 * Constructor for this service
	 */
	public FileBounceService() {
		super();
	}

	/**
	 * Initialize this service.
	 */
	public void init() {

	}

	/**
	 * Handle GET request.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		// System.out.println("FileBounceService.doGet");
		this.doPost(req, resp);
	}

	/**
	 * Handle POST request.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		// System.out.println("FileBounceService.doPost");
		// System.out.println("requestURI is:\t" + req.getRequestURI());

		int id = 0;

		String path = req.getPathInfo();

		if (path.endsWith(SVG_SUFFIX)) {
			String svgString = req.getParameter("fileString");
			doBounceStringToFile(resp, svgString, "graph.svg");
		} else if (path.endsWith(TAB_SUFFIX)) {
			doBounceStringToFile(resp, req.getParameter("fileString"),
					"links.tab");
		} else if (path.endsWith(LIST_SUFFIX)) {
			doBounceStringToFile(resp, req.getParameter("fileString"),
					"list.tab");
		} else if (path.endsWith(XGMML_ENCODER)) {
			PathwayBounceService.doJsonPathwayToXgmml(resp,
					req.getParameter("fileString"), "pathway.xgmml");
		} else if (path.endsWith("returnList")) {
			// test with:
			// http://localhost:8080/ib/fileBounce/returnList
			try {
				DatabaseService.writeTextResponse(this.bounceTabFileList(req),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("returnFile")) {
			// test with:
			// http://localhost:8080/ib/fileBounce/returnFile
			try {
				DatabaseService.writeTextResponse(this.bounceFileContents(req),
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// nothing to do !
		}
	}

	/**
	 * Get the "uploadFormElement" parameter from a multipartrequest and return
	 * it in a JSON-RPC compliant result under the key "fileContents".
	 * 
	 * @param req
	 * @return
	 */
	private String bounceFileContents(HttpServletRequest req) {
		JSONObject resultJO = new JSONObject();
		String error = null;
		int id = 0;

		// Check that we have a file upload request
		if (ServletFileUpload.isMultipartContent(req)) {

			List<FileItem> items = DatabaseService.parseMultipartRequest(req);

			Map<String, String> parameterMap = DatabaseService
					.getParamaterMapping(items);

			// create and write the response
			if (parameterMap.get("uploadFormElement").length() > 0) {
				// parse the parameterMap
				String fileContents;
				if (parameterMap.containsKey("limit")) {
					int limit = Integer.parseInt(parameterMap.get("limit"));
					String[] rows = parameterMap.get("uploadFormElement")
							.split("\n", limit + 2);
					StringBuffer sb = new StringBuffer();
					int last = rows.length - 1;
					for (int i = 0; i < last; i++) {
						sb.append(rows[i] + "\n");
					}
					fileContents = sb.toString().trim();
				} else {
					fileContents = parameterMap.get("uploadFormElement");
				}
				try {
					resultJO.put("fileContents", fileContents);
				} catch (JSONException e) {
					e.printStackTrace();
					error = e.toString();
					JSONObject jo = DatabaseService.encodeJsonRpcResponse(null,
							error, id);
					return jo.toString();
				}

			} else {
				error = "no uploadFormElement";
			}
		} else {
			error = "no multipart content";
		}

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(resultJO, error,
				id);

		return jo.toString();
	}

	/**
	 * Wrapper method for bouncing a tab-delimited file back as a
	 * space-separated list in the JSON string called "list".
	 * 
	 * @param req
	 * @return
	 * @throws JSONException
	 */
	private String bounceTabFileList(HttpServletRequest req)
			throws JSONException {
		JSONStringer js = new JSONStringer();
		js.object();

		// Check that we have a file upload request
		if (ServletFileUpload.isMultipartContent(req)) {

			List<FileItem> items = DatabaseService.parseMultipartRequest(req);

			Map<String, String> parameterMap = DatabaseService
					.getParamaterMapping(items);

			// create and write the response
			if (parameterMap.get("uploadFormElement").length() > 0) {
				// parse the parameterMap
				String[] stringsArray = parameterMap.get("uploadFormElement")
						.split("\\s+");

				// don't keep duplicates
				HashSet<String> stringsHashSet = new HashSet<String>();
				for (String s : stringsArray) {
					stringsHashSet.add(s);
				}

				StringBuffer sb = new StringBuffer();
				for (String s : stringsHashSet) {
					sb.append(s + " ");
				}
				js.key("list").value(sb.toString().trim());

				js.key("success").value(true);
			} else {
				js.key("success").value(false);
			}
		}

		js.endObject();
		return js.toString();
	}

	/**
	 * Write a String as a text file response.
	 * 
	 * @param resp
	 * @param bounceString
	 * @param fileName
	 */
	public static void doBounceStringToFile(HttpServletResponse resp,
			String bounceString, String fileName) {
		if (fileName.equalsIgnoreCase("graph.svg")) {
			resp.setHeader("Content-disposition", "attachment; filename=\""
					+ fileName + ".xhtml" + "\"");
			resp.setContentType("text/xhtml");
		} else {
			resp.setHeader("Content-disposition", "attachment; filename=\""
					+ fileName + "\"");
			resp.setContentType("text/html");
		}
		try {
			PrintWriter respWriter = resp.getWriter();
			String newString = bounceString.replaceAll("__NEW_LINE__", "\n");
			respWriter.println(newString);
			respWriter.close();
		} catch (IOException e) {
			System.out.println("FileBounceService:\t" + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Output parameter key-value pairs in an HttpServletRequest. Used for
	 * debugging.
	 * 
	 * @param req
	 */
	protected static void printReqParams(HttpServletRequest req) {
		System.out.println("BEGIN printing out name-value pairs");
		Enumeration<String> paraNamesEnum = req.getParameterNames();
		String paraName;
		while (paraNamesEnum.hasMoreElements()) {
			paraName = paraNamesEnum.nextElement();
			System.out.println(paraName + "=" + req.getParameter(paraName));
		}
		System.out.println("END printing out name-value pairs");
	}
}
