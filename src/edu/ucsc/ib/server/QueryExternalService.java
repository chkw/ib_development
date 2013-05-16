package edu.ucsc.ib.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Servlet to query an external service such as Grinder or Synergizer.
 * 
 * @author Chris
 * 
 */
public class QueryExternalService extends HttpServlet {

	/**
	 * Synergizer requests that there be minimum of 3 seconds between queries.
	 * ref: http://llama.med.harvard.edu/synergizer/doc/
	 */
	private static final int SYNERGIZER_MIN_SECONDS_BETWEEN_QUERIES = 3;

	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 2052301857407379021L;

	private static Properties localProperties;
	private static final String PROP_FILE = "ib.properties";

	private static final HashMap<String, String> NCBItoGrinderTargetHash = new HashMap<String, String>();
	private static final HashMap<String, ArrayList<String>> grinderSourceNSlist = new HashMap<String, ArrayList<String>>();

	/**
	 * A String maps to a HashMap. The inner HashMap has the following keys:
	 * "name", "domain", "range". "name" is something like "Homo sapiens".
	 * "domain" is a comma-separated list of origin namespaces. "range" is the
	 * target namespace.
	 */
	private static final HashMap<String, HashMap<String, String>> synergizerOrganismHashMap = new HashMap<String, HashMap<String, String>>();

	private Date timeOfLastSynergizerQuery = null;

	public QueryExternalService() {
		super();

		// load properties
		// specifically need GRINDER_SERVLET_URL
		localProperties = new Properties();
		InputStream is = DatabaseService.class.getResourceAsStream(PROP_FILE);
		try {
			localProperties.load(is);
		} catch (IOException ioe) { /* leave localProperties empty */
		}

		setupGrinderHashes();

		setupSynergizerHashes();
	}

	/**
	 * Setup HashMaps for working with Synergizer.
	 */
	private final void setupSynergizerHashes() {
		synergizerOrganismHashMap.put("4932", new HashMap<String, String>());
		synergizerOrganismHashMap.get("4932").put("name",
				"Saccharomyces cerevisiae");
		synergizerOrganismHashMap.get("4932").put("domain", "entrezgene");
		synergizerOrganismHashMap.get("4932").put("range", "ensembl_gene_id");

		synergizerOrganismHashMap.put("6239", new HashMap<String, String>());
		synergizerOrganismHashMap.get("6239").put("name",
				"Caenorhabditis elegans");
		synergizerOrganismHashMap.get("6239").put("domain", "entrezgene");
		synergizerOrganismHashMap.get("6239").put("range",
				"wormbase_transcript");

		synergizerOrganismHashMap.put("9606", new HashMap<String, String>());
		synergizerOrganismHashMap.get("9606").put("name", "Homo sapiens");
		synergizerOrganismHashMap
				.get("9606")
				.put("domain",
						"hgnc_symbol,refseq_peptide,uniprot_accession,ensembl_peptide_id,ensembl_gene_id,ensembl_transcript_id");
		synergizerOrganismHashMap.get("9606").put("range", "entrezgene");

		synergizerOrganismHashMap.put("10090", new HashMap<String, String>());
		synergizerOrganismHashMap.get("10090").put("name", "Mus musculus");
		synergizerOrganismHashMap.get("10090").put("domain",
				"mgi_curated_gene_symbol");
		synergizerOrganismHashMap.get("10090").put("range", "entrezgene");
	}

	/**
	 * Setup HashMaps for working with Grinder.
	 */
	private final void setupGrinderHashes() {
		NCBItoGrinderTargetHash.put("4932", "SGD_ID");
		NCBItoGrinderTargetHash.put("9606", "Human_EntrezGene");
		NCBItoGrinderTargetHash.put("10090", "Mouse_EntrezGene");

		grinderSourceNSlist.put("Human_EntrezGene", new ArrayList<String>());
		grinderSourceNSlist.get("Human_EntrezGene").add("Human_EntrezGene");
		grinderSourceNSlist.get("Human_EntrezGene").add("HGNC_Approved_Names");
		grinderSourceNSlist.get("Human_EntrezGene")
				.add("HGNC_Approved_Symbols");
		grinderSourceNSlist.get("Human_EntrezGene").add("HGNC_ID");
		grinderSourceNSlist.get("Human_EntrezGene").add(
				"UniGene_Approved_Symbols");
		grinderSourceNSlist.get("Human_EntrezGene").add("UniGene_Gene_Names");
		grinderSourceNSlist.get("Human_EntrezGene").add(
				"UniGene_Gene_ClusterIDs");
		grinderSourceNSlist.get("Human_EntrezGene").add("Ensembl_IDs");
		grinderSourceNSlist.get("Human_EntrezGene").add("GDB_IDs");
		grinderSourceNSlist.get("Human_EntrezGene").add(
				"GenBank_Accession_Numbers");
		grinderSourceNSlist.get("Human_EntrezGene").add(
				"GenBank_EST_Accession_Numbers");
		grinderSourceNSlist.get("Human_EntrezGene").add(
				"GPL_RefSeq_Transcript_IDs");
		grinderSourceNSlist.get("Human_EntrezGene").add("IMAGE_clone_IDs");
		grinderSourceNSlist.get("Human_EntrezGene").add("OMIM_IDs");
		grinderSourceNSlist.get("Human_EntrezGene").add("UniProt_IDs");
		grinderSourceNSlist.get("Human_EntrezGene").add("RefSeq_mRNA_IDs");
		grinderSourceNSlist.get("Human_EntrezGene").add("RZPD_IDs");
		grinderSourceNSlist.get("Human_EntrezGene").add("UCSC_IDs");
		grinderSourceNSlist.get("Human_EntrezGene").add("Compugen_Probe_IDs");
		grinderSourceNSlist.get("Human_EntrezGene").add(
				"Affymetrix_GC_HG_U133_Plus_2_0_");
		grinderSourceNSlist.get("Human_EntrezGene").add(
				"Affymetrix_GC_HG_U95_IDs");

		grinderSourceNSlist.put("SGD_ID", new ArrayList<String>());
		grinderSourceNSlist.get("SGD_ID").add("SGD_ID");
		grinderSourceNSlist.get("SGD_ID").add("SGD_Locus_Name");
		grinderSourceNSlist.get("SGD_ID").add("SGD_ORF_Name");
		grinderSourceNSlist.get("SGD_ID").add("SGD_Alias_Name");

		grinderSourceNSlist.put("Mouse_EntrezGene", new ArrayList<String>());
		grinderSourceNSlist.get("Mouse_EntrezGene").add("Mouse_EntrezGene");
		grinderSourceNSlist.get("Mouse_EntrezGene").add("MGD_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Mouse_UniGene_Gene_Names");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Mouse_Ensembl_Gene_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene")
				.add("Mouse_UniGene_Symbols");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Mouse_UniGene_Gene_ClusterIDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add("Mouse_TIGR_Gene_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Mouse_GenBank_Acc_Numbers");
		grinderSourceNSlist.get("Mouse_EntrezGene").add("GPL_Mouse_RefSeq_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_1261_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_260_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_313_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_32_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_339_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_340_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_560_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_75_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_76_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_81_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_82_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_83_IDs");
		grinderSourceNSlist.get("Mouse_EntrezGene").add(
				"Affymetrix_Probe_Set_891_IDs");
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

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/queryExternal/test

			try {
				DatabaseService.writeTextResponse("success? " + test(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("grinderMap")) {
			// test with:
			// http://localhost:8080/ib/data/queryExternal/grinderMap?organism=9606&fromNS=HGNC_Approved_Symbols&ids=TP53,gjklwar,ERBB2,ERBB3
			String NCBI_organism = req.getParameter("organism");
			String toNamespace = NCBItoGrinderTargetHash.get(NCBI_organism);

			if (toNamespace == null) {
				try {
					DatabaseService
							.writeJSONRPCTextResponse(null,
									"no target namespace for " + NCBI_organism,
									0, resp);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}

			String fromNamespace = req.getParameter("fromNS");
			String CSVids = req.getParameter("ids");

			String[] ids = CSVids.split(",");

			try {
				DatabaseService.writeTextResponse(
						idMappingHashMapToJSON(getGrinderMapping(fromNamespace,
								toNamespace, ids)), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (path.endsWith("grinderMapMulti")) {
			// intended for attempting mapping from mixed namespaces
			// test with:
			// http://localhost:8080/ib/data/queryExternal/grinderMapMulti?organism=9606&ids=TP53,gjklwar,ERBB2,ERBB3
			String NCBI_organism = req.getParameter("organism");
			String toNamespace = NCBItoGrinderTargetHash.get(NCBI_organism);

			if (toNamespace == null) {
				try {
					DatabaseService
							.writeJSONRPCTextResponse(null,
									"no target namespace for " + NCBI_organism,
									0, resp);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}

			String CSVids = req.getParameter("ids");

			String[] ids = CSVids.split(",");

			try {
				String JSONtext = idMappingHashMapToJSON(getGrinderMultiMapping(
						toNamespace, ids));
				JSONObject jo = new JSONObject(JSONtext);
				jo.put("targetNS", toNamespace);
				DatabaseService.writeJSONRPCTextResponse(jo, null, 0, resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("synergizerMap")) {
			// test with:
			// http://localhost:8080/ib/data/queryExternal/synergizerMap?authority=ensembl&species=Homo
			// sapiens&domain=hgnc_symbol&range=entrezgene&ids=snph,chac1,actn3,maybe_a_typo,pja1,prkdc,RAD21L1,Rorc,kcnk16

			// http://localhost:8080/ib/data/queryExternal/synergizerMap?authority=ensembl&species=Homo
			// sapiens&domain=entrezgene&range=hgnc_symbol&ids=7157,1234,6712,79109,3936,26072,51696,146988,57573,4153,8434,3976
			String authority = req.getParameter("authority");
			String species = req.getParameter("species");
			String domain = req.getParameter("domain");
			String range = req.getParameter("range");
			String CSVids = req.getParameter("ids");

			try {
				DatabaseService.writeTextResponse(
						idMappingHashMapToJSON(getSynergizerMap(authority,
								species, domain, range, CSVids,
								timeOfLastSynergizerQuery)), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (path.endsWith("synergizerMultiMap")) {
			// test with:
			// http://localhost:8080/ib/data/queryExternal/synergizerMultiMap?organism=9606&ids=snph,chac1,actn3,maybe_a_typo,pja1,prkdc,RAD21L1,Rorc,kcnk16
			String NCBI_organism = req.getParameter("organism");
			String CSVids = req.getParameter("ids");

			try {
				DatabaseService.writeTextResponse(
				// name is something like "Homo sapiens"
						this.idMappingHashMapToJSON(getSynergizerMultiMap(
								"ensembl", NCBI_organism, CSVids)), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// nothing to do !
		}
	}

	/**
	 * Synergizer limits the traffic to its servers.
	 * 
	 * @param seconds
	 * @param timeOfLastQuery
	 */
	private final static void scheduleSynergizerRequest(final int seconds,
			Date timeOfLastQuery) {
		long timeDiff = 0;
		if (!(timeOfLastQuery == null)) {
			timeDiff = new Date().getTime() - timeOfLastQuery.getTime();
			if ((timeDiff < (seconds * 1000))) {
				try {
					Thread.sleep((seconds * 1000) - timeDiff);
					timeOfLastQuery = new Date();
					// System.out.println("waited");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				timeOfLastQuery = new Date();
				// System.out.println("didn't wait");
			}
		} else {
			timeOfLastQuery = new Date();
			// System.out.println("didn't wait");
		}
	}

	/**
	 * Query the Synergizer service repeatedly, attempting to map unknown
	 * namespace to known namespace. The results are checked after each round.
	 * IDs with mappings are not included in subsequent queries. Just reports
	 * one mapping per query.
	 * 
	 * @param authority
	 * @param NCBItaxID
	 * @param cSVids
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	private HashMap<String, String> getSynergizerMultiMap(String authority,
			String NCBItaxID, String cSVids) throws IOException, JSONException {
		HashMap<String, String> resultsHash = new HashMap<String, String>();

		HashMap<String, String> parameterMap = synergizerOrganismHashMap
				.get(NCBItaxID);

		if (parameterMap == null) {
			return new HashMap<String, String>();
		}

		String name = parameterMap.get("name");
		String range = parameterMap.get("range");

		Collection<String> idsCollection = Arrays.asList(cSVids.split(","));
		String[] sourceNSarray = parameterMap.get("domain").split(",");

		for (String sourceNS : sourceNSarray) {

			HashSet<String> nullIDs = new HashSet<String>(idsCollection);

			if (!nullIDs.isEmpty()) {

				nullIDs = new HashSet<String>();

				StringBuffer idSB = new StringBuffer();
				for (String id : idsCollection) {
					idSB.append(id + ",");
				}
				idSB.deleteCharAt(idSB.lastIndexOf(","));

				HashMap<String, String> mappingHashMap = getSynergizerMap(
						authority, name, sourceNS, range, idSB.toString(),
						timeOfLastSynergizerQuery);

				for (String key : mappingHashMap.keySet()) {
					String csvMappings = mappingHashMap.get(key);
					if (csvMappings.equalsIgnoreCase("null")) {
						nullIDs.add(key);
					} else {
						String[] mappingsList = csvMappings.split(",");
						resultsHash.put(key, mappingsList[0]);
					}
				}
				idsCollection = new HashSet<String>(nullIDs);
			}
		}

		// results for IDs that didn't return a result
		for (String id : idsCollection) {
			resultsHash.put(id, "null");
		}

		return resultsHash;
	}

	/**
	 * ref: http://llama.med.harvard.edu/synergizer/doc/ Synergizer returns
	 * "null" when it can't find a mapping. The value of the HashMap is a
	 * comma-separated list of IDs. This could be a performance bottleneck since
	 * Synergizer allows just 1 query per 3 seconds.
	 * 
	 * @param authority
	 * @param species
	 * @param fromNamespace
	 * @param toNamespace
	 * @param csvIds
	 * @param timeOfLastQuery
	 * 
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	private static HashMap<String, String> getSynergizerMap(String authority,
			String species, String fromNamespace, String toNamespace,
			String csvIds, Date timeOfLastQuery) throws IOException,
			JSONException {
		String url = localProperties.getProperty("SYNERGIZER_SERVLET_URL");

		String requestJSON = "";
		try {
			requestJSON = createSynergizerTranslateJSON(authority, species,
					fromNamespace, toNamespace, csvIds);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		// System.out.println("url:	" + url);
		// System.out.println("requestJSON:	" + requestJSON);

		// /////////////////////////////////////////////////////

		scheduleSynergizerRequest(SYNERGIZER_MIN_SECONDS_BETWEEN_QUERIES,
				timeOfLastQuery);

		URL u = new URL(url);
		URLConnection conn = u.openConnection();

		// setup connection
		conn.setDoOutput(true);
		if (conn instanceof HttpURLConnection) {
			((HttpURLConnection) conn).setRequestMethod("POST");
			((HttpURLConnection) conn).setRequestProperty("Content-type",
					"application/json");
		}

		OutputStreamWriter outStreamWriter = new OutputStreamWriter(
				conn.getOutputStream());

		// write request JSON to connection
		outStreamWriter.write(requestJSON);
		outStreamWriter.close();

		// get response
		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));

		// String s = null;
		// while ((s = in.readLine()) != null) {
		// System.out.println("response:	" + s);
		// }

		String JSONresponseString = in.readLine();
		in.close();

		// ///////////////////////////////////////////////////////

		JSONObject JOresponse = new JSONObject(JSONresponseString);

		// System.out.println("response:	" + JOresponse.toString());

		if (!JOresponse.isNull("error")) {
			return null;
		}

		JSONArray JAresult = JOresponse.getJSONArray("result");
		HashMap<String, String> idHashMap = new HashMap<String, String>();
		for (int i = 0; i < JAresult.length(); i++) {
			JSONArray idMappingJA = JAresult.getJSONArray(i);
			// possible mapping to several IDs
			StringBuffer csvMappingsSB = new StringBuffer();
			for (int j = 1; j < idMappingJA.length(); j++) {
				csvMappingsSB.append(idMappingJA.getString(j) + ",");
				idHashMap.put(idMappingJA.getString(0),
						idMappingJA.getString(j));
			}
			if (csvMappingsSB.indexOf(",") != -1) {
				csvMappingsSB.deleteCharAt(csvMappingsSB.lastIndexOf(","));
			}
			idHashMap.put(idMappingJA.getString(0), csvMappingsSB.toString());
		}

		return idHashMap;
	}

	/**
	 * Create a JSON object that can be used to request Synergizer to translate.
	 * Synergizer will take the request as a POST with the "Content-type"
	 * request property of "application/json"
	 * 
	 * Refer to http://llama.med.harvard.edu/synergizer/doc/ .
	 * 
	 * @param authority
	 * @param species
	 * @param domain
	 * @param range
	 * @param csvIds
	 * @return
	 * @throws JSONException
	 */
	private static String createSynergizerTranslateJSON(String authority,
			Object species, Object domain, Object range, String csvIds)
			throws JSONException {
		JSONStringer js = new JSONStringer();
		js.object();

		// js.key("method").value("available_authorities");
		js.key("method").value("translate");

		js.key("params").array().object();

		js.key("authority").value(authority);
		js.key("species").value(species);
		js.key("domain").value(domain);
		js.key("range").value(range);
		js.key("ids").array();

		for (String id : csvIds.split(",")) {
			js.value(id);
		}

		js.endArray(); // ids
		js.endObject().endArray(); // params

		js.key("id").value(0);

		js.endObject();

		return js.toString();
	}

	/**
	 * Query Grinder for ID mappings from unknown namespace for an organism.
	 * This method opens multiple URL connections, one for each possible source
	 * namespace. It does every available namespace each time. It does not stop
	 * when an ID has been identified. Consequently, a query ID may have a list
	 * of possible IDs in the target namespace. It is also possible that the
	 * target IDs may be repeated if the query ID mapped to the same target ID
	 * in more than one source namespace.
	 * 
	 * @param toNamespace
	 * @param ids
	 * @return
	 * @throws IOException
	 */
	private HashMap<String, String> getGrinderMultiMapping(String toNamespace,
			String[] ids) throws IOException {
		HashMap<String, String> resultsHash = new HashMap<String, String>();

		ArrayList<String> sourceNSlist = grinderSourceNSlist.get(toNamespace);

		// check if Grinder supports the namespace
		if (sourceNSlist == null) {
			// TODO need error message in JSON response
			return null;
		}

		for (String sourceNS : sourceNSlist) {
			// query Grinder for each sourceNS
			HashMap<String, String> NSresultsHash = getGrinderMapping(sourceNS,
					toNamespace, ids);
			// Grinder may return the following:
			// "Invalid ID" means there was no mapping found
			// "" means it maps to itself
			// If there was a mapping, then it is returned.
			for (String queryID : NSresultsHash.keySet()) {
				if (NSresultsHash.get(queryID).equalsIgnoreCase("Invalid ID")) {
					if (resultsHash.get(queryID) == null) {
						resultsHash.put(queryID, NSresultsHash.get(queryID));
					} else if (resultsHash.get(queryID).equalsIgnoreCase(
							"Invalid ID")) {
						// do nothing - already "Invalid ID"
					} else {
						// do nothing - don't need to record it
					}
				} else if (NSresultsHash.get(queryID).equalsIgnoreCase("")) {
					if (resultsHash.get(queryID) == null) {
						resultsHash.put(queryID, queryID);
					} else if (resultsHash.get(queryID).equalsIgnoreCase(
							"Invalid ID")) {
						resultsHash.put(queryID, queryID);
					} else {
						resultsHash.put(queryID, resultsHash.get(queryID) + ","
								+ queryID);
					}
				} else {
					if (resultsHash.get(queryID) == null) {
						resultsHash.put(queryID, NSresultsHash.get(queryID));
					} else if (resultsHash.get(queryID).equalsIgnoreCase(
							"Invalid ID")) {
						resultsHash.put(queryID, NSresultsHash.get(queryID));
					} else {
						resultsHash.put(queryID, resultsHash.get(queryID) + ","
								+ NSresultsHash.get(queryID));
					}
				}
			}
		}

		return resultsHash;
	}

	/**
	 * Open a URLConnection to query the Grinder servlet for ID mappings. Valid
	 * namespace parameters can be obtained by a query like:
	 * http://disco.cse.ucsc
	 * .edu:8089/Grinder/data/GrinderServlet?request=keyspaces&species=Human .
	 * "Invalid ID" means there was no mapping found.
	 * 
	 * @param fromNamespace
	 * @param toNamespace
	 * @param ids
	 * @return
	 * @throws IOException
	 */
	private HashMap<String, String> getGrinderMapping(String fromNamespace,
			String toNamespace, String[] ids) throws IOException {
		StringBuffer urlSB = new StringBuffer(
				localProperties.getProperty("GRINDER_SERVLET_URL"));
		urlSB.append("?request=map");
		urlSB.append("&target=" + toNamespace);
		urlSB.append("&source=" + fromNamespace);
		urlSB.append("&ids=");

		for (String id : ids) {
			urlSB.append(id + ",");
		}

		urlSB.deleteCharAt(urlSB.lastIndexOf(","));

		System.out.println(urlSB.toString());

		URL url = new URL(urlSB.toString());
		URLConnection conn = url.openConnection();
		BufferedReader rd = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));

		String line;
		ArrayList<String> results = new ArrayList<String>();
		while ((line = rd.readLine()) != null) {
			results.add(line);
		}
		rd.close();

		HashMap<String, String> resultsMap = new HashMap<String, String>();
		if (ids.length == results.size()) {
			for (int i = 0; i < ids.length; i++) {
				resultsMap.put(ids[i], results.remove(0));
			}
		} else {
			// TODO need error message in JSON response
			System.out.println("number of results doesn't match query!");
		}

		return resultsMap;
	}

	/**
	 * Create a JSON-RPC result text from a HashMap<String,String>. It consists
	 * of a JSONArray called "idMappings". Each JSONObject in the JSONArray has
	 * a query to ID mapping in a JSONArray.
	 * 
	 * @param stringMapping
	 * @return
	 * @throws JSONException
	 */
	public String idMappingHashMapToJSON(HashMap<String, String> stringMapping)
			throws JSONException {
		if (stringMapping == null) {
			JSONObject jo = DatabaseService.encodeJsonRpcResponse(null,
					"got no mappings", 0);

			return jo.toString();
		}

		JSONStringer js = new JSONStringer();
		js.object();

		js.key("idMappings").array();

		for (String key : stringMapping.keySet()) {
			JSONArray ja = new JSONArray();
			if (stringMapping.get(key) == null) {
				ja.put(JSONObject.NULL);
			} else {
				String[] mappingList = stringMapping.get(key).split(",");
				for (String id : mappingList) {
					if (id.isEmpty() || id.equalsIgnoreCase("null")) {
						ja.put(JSONObject.NULL);
					} else {
						ja.put(id);
					}
				}
			}

			JSONObject jo = new JSONObject();
			jo.put(key, ja);
			js.value(jo);
		}

		js.endArray();

		js.endObject();

		JSONObject jo = DatabaseService.encodeJsonRpcResponse(
				new JSONObject(js.toString()), null, 0);

		return jo.toString();
	}

	/**
	 * Simple method for returning some simple text.
	 * 
	 * @return
	 */
	static String test() {
		localProperties = new Properties();
		InputStream is = DatabaseService.class.getResourceAsStream(PROP_FILE);
		try {
			localProperties.load(is);
		} catch (IOException ioe) { /* leave localProperties empty */
		}
		String result = localProperties.getProperty("GRINDER_SERVLET_URL");
		// String result = "this is QueryExternalService.test";
		System.out.println(result);
		return result;
	}

}
