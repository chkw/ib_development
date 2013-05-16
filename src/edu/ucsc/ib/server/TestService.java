package edu.ucsc.ib.server;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.fileupload.FileItem;

/**
 * servlet.
 * 
 * @author Chris
 * 
 */
public class TestService extends DatabaseService {
	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 5661857285957361482L;

	// TODO //////////////////////////////////

	public TestService() {
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

		System.out.println("TESTSERVICE.DOPOST");

		String path = req.getPathInfo();

		// do some session stuff

		HttpSession session = InitSession(req);
		final String userID = session.getId();
		final int id = 0;

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
							// deleteUserRows(userID);
						}
					});
		}

		// TODO service methods

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/test/test

			try {
				DatabaseService.writeTextResponse("hello", resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("save_pathway")) {
			// test with:
			// http://localhost:8080/ib/data/test/save_pathway

			List<FileItem> fileItemList = DatabaseService
					.parseMultipartRequest(req);
			Map<String, String> parameterMap = getParamaterMapping(fileItemList);

			logSysOut(parameterMap.toString());

			try {
				DatabaseService
						.writeTextResponse(parameterMap.toString(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			// nothing to do !
		}
	}

	// TODO /////////////////////////////////////////////////////////////

}
