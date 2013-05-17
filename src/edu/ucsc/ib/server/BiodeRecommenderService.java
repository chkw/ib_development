package edu.ucsc.ib.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONStringer;

import edu.ucsc.ib.server.rpc.AnnotationSearchResults;

/**
 * Service for recommending biodes. Handles requests to use service such as
 * ClueGene and GeneRecommender.
 * 
 * @author cw
 * 
 */
public class BiodeRecommenderService extends HttpServlet {

	/**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = 9083245323322838441L;

	private static Properties localProperties;
	private static final String PROP_FILE = "ib.properties";

	private static final String[][] clueGeneSpeciesCodesArray = {
			{ "6239", "cel" }, { "7227", "dme" }, { "9606", "hsa" },
			{ "10090", "mmu" }, { "4932", "sce" } };

	private static final HashMap<String, String> clueGeneSpeciesCodes = new HashMap<String, String>();
	{
		clueGeneClusterCompendiumName.put("6239", "cel");
		clueGeneClusterCompendiumName.put("7227", "dme");
		clueGeneClusterCompendiumName.put("9606", "hsa");
		clueGeneClusterCompendiumName.put("10090", "mmu");
		clueGeneClusterCompendiumName.put("4932", "sce");
	}

	private static final HashMap<String, String> clueGeneClusterCompendiumName = new HashMap<String, String>();
	{
		clueGeneClusterCompendiumName.put("cel", "worm");
		clueGeneClusterCompendiumName.put("dme", "fly");
		clueGeneClusterCompendiumName.put("hsa", "human");
		clueGeneClusterCompendiumName.put("mmu", "mouse");
		clueGeneClusterCompendiumName.put("sce", "yeast");
	}

	public BiodeRecommenderService() {
		super();

		// load properties
		localProperties = new Properties();
		InputStream is = DatabaseService.class.getResourceAsStream(PROP_FILE);
		try {
			localProperties.load(is);
		} catch (IOException ioe) { /* leave localProperties empty */
		}
	}

	/**
	 * Get the value of a property from the properties file.
	 * 
	 * @param propertyName
	 *            property's key
	 * @return String
	 */
	String getProperty(String propertyName) {
		String result = null;
		ServletContext sc = getServletContext();
		if (sc != null) {
			result = getServletContext().getInitParameter(propertyName);
		}
		if ((result == null) && (localProperties != null)) {
			result = localProperties.getProperty(propertyName);
		}
		return result;
	}

	/**
	 * Handle GET request. Hand over to doPost() method.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		// System.out.println("BiodeRecommenderService.doGet");
		// System.out.println("--handing over to BiodeRecommenderService.doPost");
		this.doPost(req, resp);
	}

	/**
	 * Handle POST request.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		// System.out.println("BiodeRecommenderService.doPost");
		// System.out.println("requestURI is:\t" + req.getRequestURI());

		String path = req.getPathInfo();

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/biodeRecommender/test

			try {
				DatabaseService.writeTextResponse(
						"BiodeRecommenderService says HI!", resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("recommender")) {
			// test with:
			// http://localhost:8080/ib/data/biodeRecommender/recommender?recommenderType=clueGene&organism=human&queryList=5682
			// 5683 5684 5685 5686 5687 5688 5689 5690 5691 5692 5693 5694
			// 5695&maxResults=20

			String recommenderType = req.getParameter("recommenderType");
			String organism = req.getParameter("organism");
			String queryGenes = req.getParameter("queryList");
			String maxResults = req.getParameter("maxResults");

			boolean debug = false;
			if (req.getParameter("debug") != null
					&& req.getParameter("debug").equalsIgnoreCase("true")) {
				debug = true;
			}

			if (recommenderType.equalsIgnoreCase("clueGene")) {
				// query cluegene
				try {
					DatabaseService.writeTextResponse(this.getClueGeneResults(
							organism, maxResults, queryGenes, debug), resp);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (recommenderType.equalsIgnoreCase("geneRecommender")) {
				// query generecommender
				try {
					DatabaseService.writeTextResponse(this
							.getGeneRecommenderResults(organism, maxResults,
									queryGenes), resp);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			// nothing to do !
		}
	}

	/**
	 * Use ClueGene to go shopping for genes. The script is located at:
	 * /projects/sysbio/www/cgi-bin/ClueGene/cluegene_web.pl
	 * 
	 * The ShellProcess command looks something like:
	 * 
	 * 
	 * 
	 * <code>perl -I /projects/sysbio/www/cgi-bin/ClueGene /projects/sysbio/www/cgi-bin/ClueGene/cluegene_web.pl --ib --maxresults 10 --species sce --compendium "/projects/sysbio/cluegene/visant_support/yeast.cc" --queryset "YDL184C YEL054C YGL135W YLL045C YLR185W YMR121C YNL301C" </code>
	 * 
	 * @param species
	 *            clueGeneSpeciesCode String for ClueGene's species code worm is
	 *            "cel", fly is "dme", human is "hsa", mouse is "mmu", yeast is
	 *            "sce"
	 * @param maxResults
	 *            int number of genes to return (defaults to 20)
	 * @param queryGenes
	 *            String list of query genes
	 * @param debug
	 *            TODO
	 * @return ClueGene's recommendations
	 * @throws JSONException
	 */
	public String getClueGeneResults(String species, String maxResults,
			String queryGenes, boolean debug) throws JSONException {
		JSONStringer js = new JSONStringer();
		js.object();

		js.key("recommenderType").value("clueGene");
		js.key("species").value(species);
		js.key("maxResults").value(maxResults);
		js.key("queryGenes").value(queryGenes);

		final String pathToClueGene = getProperty("cluegene.path");
		final String pathToClusterCompendium = getProperty("cluegene.clustercompendium.path");
		final String speciesCode = this.getClueGeneSpeciesCode(species);

		String[] cmd = {
				"perl",
				"-I",
				pathToClueGene,
				pathToClueGene + "cluegene_web.pl",
				"--ib",
				"--maxresults",
				DatabaseService.sanitizeString(maxResults),
				"--species",
				speciesCode,
				"--compendium",
				pathToClusterCompendium
						+ clueGeneClusterCompendiumName.get(speciesCode)
						+ ".cc", "--queryset",
				DatabaseService.sanitizeString(queryGenes) };

		if (debug) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < cmd.length; i++) {
				sb.append(cmd[i] + " ");
			}

			System.err.println(sb.toString());
		}

		ShellProcess sp = new ShellProcess(cmd);

		if (sp.getExitStatus() == 0) {
			AnnotationSearchResults asr = new AnnotationSearchResults(
					sp.getOutput().length, sp.getOutput());

			String[][] rm = asr.getResultMatrix();

			// check for errors - lines beginning with #
			if (rm[0][0].indexOf("#") != 0) {
				String[] results = rm[0];
				js.key("results").array();
				for (String result : results) {
					js.value(result);
				}
				js.endArray();
			} else {
				// got an error
			}
			js.endObject();
			return js.toString();
		} else {
			js.endObject();
			return js.toString();
		}
	}

	/**
	 * Look up a ClueGene species code.
	 * 
	 * @param species
	 *            String
	 * @return species code as String
	 */
	private String getClueGeneSpeciesCode(String species) {
		for (int i = 0; i < clueGeneSpeciesCodesArray.length; i++) {
			if (clueGeneSpeciesCodesArray[i][0].equalsIgnoreCase(species)) {
				return clueGeneSpeciesCodesArray[i][1];
			}
		}
		return null;
	}

	/**
	 * Similar to getClueGeneResults, except goes shopping with GeneRecommender.
	 * 
	 * @throws JSONException
	 */
	public String getGeneRecommenderResults(String species, String maxResults,
			String queryGenes) throws JSONException {
		JSONStringer js = new JSONStringer();
		js.object();

		js.key("recommenderType").value("geneRecommender");
		js.key("species").value(species);
		js.key("maxResults").value(maxResults);
		js.key("queryGenes").value(queryGenes);

		final String pathToGR = getProperty("geneRecommender.path");

		String[] cmd = { "perl", "-I", pathToGR,
				pathToGR + "generecommender.pl", "--ib", "--maxresults",
				DatabaseService.sanitizeString(maxResults), "--species",
				getClueGeneSpeciesCode(species), "--queryset",
				DatabaseService.sanitizeString(queryGenes) };

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < cmd.length; i++) {
			sb.append(cmd[i] + " ");
		}

		ShellProcess sp = new ShellProcess(cmd);

		if (sp.getExitStatus() == 0) {
			AnnotationSearchResults asr = new AnnotationSearchResults(
					sp.getOutput().length, sp.getOutput());

			String[][] rm = asr.getResultMatrix();

			// check for errors - lines beginning with #
			if (rm[0][0].indexOf("#") != 0) {
				String[] results = rm[0];
				js.key("results").array();
				for (String result : results) {
					js.value(result);
				}
				js.endArray();
			} else {
				// got an error
			}
			js.endObject();
			return js.toString();
		} else {
			js.endObject();
			return js.toString();
		}
	}
}
