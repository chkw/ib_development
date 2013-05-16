package edu.ucsc.ib.server;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet for iterative bayes pathway expansion. The basic algorithm is
 * originally developed by Corey Powell in his MS thesis:
 * 
 * Powell, C. An Iterative Bayesian Updating Method for Biological Pathway
 * Prediction UCSC, 2006.
 * 
 * @author Chris
 * 
 */
public class IterativeBayesPathwayExpanderService extends TrackDbService {
	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 4003352137282266338L;

	/**
	 * Hash key for identifying a query gene
	 */
	private static final String QUERY_GENE = "query";

	/**
	 * Hash key for current score
	 */
	private static final String CURRENT_SCORE = "prior_log_odds";

	/**
	 * Hash key for cumulative score
	 */
	private static final String CUMULATIVE_PROB = "cumulative_prob";

	/**
	 * Must be greater than 0.
	 */
	private static final double MIN_PROB = 0.000001;

	/**
	 * Must be less than 1.
	 */
	private static final double MAX_PROB = 1 - MIN_PROB;

	/**
	 * Prior expectation for percentage of pathway genes that are not in the
	 * pathway
	 */
	private static final double PRIOR_FOR_BAD_PROB = 0.10;

	/**
	 * Prior expectation for the ratio: (number of expected pathway genes) /
	 * (number of given pathway genes)
	 */
	private static final double PRIOR_FOR_NEW_GENES_RATIO = 2;

	/**
	 * Distribution smoothing constant. Small value like 5 means lots of
	 * smoothing.
	 */
	private static final int BETA = 30;

	/**
	 * The number of bins to divide the distributions into. Originally 200
	 */
	private static final int NUM_SMOOTHING_BINS = 200;

	/**
	 * Constructor for this service.
	 */
	public IterativeBayesPathwayExpanderService() {
		super();
	}

	/**
	 * Handle GET request.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		String path = req.getPathInfo();

		if (path.endsWith("test")) {
			// test with:
			// http://localhost:8080/ib/data/iterativeBayesPathwayExpander/test

			try {
				super.writeTextResponse("success? " + testConnection(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (path.endsWith("expand")) {
			// test with:
			// http://localhost:8080/ib/data/iterativeBayesPathwayExpander/expand?superimpose=false&organism=9606&iterations=5&cutoff=0.5&queryset=1017,1019,1029,1058,1059,1063,11200,1163,1387,2033,2099,2119,2305,2353,255626,2619,2931,3175,3304,332,3910,4313,4609,4751,4775,5347,5604,5925,595,6502,6667,675,7039,7515,891,898,9133,9212,994&networks=Ma05_15994924,Rieger04_15356296,Frasor04_14973112,human_biogrid_Affinity_Capture-Western

			String error = null;

			@SuppressWarnings("unchecked")
			Map<String, String[]> paramMap = req.getParameterMap();

			// check for missing parameters
			if (!paramMap.containsKey("iterations")
					|| !paramMap.containsKey("cutoff")
					|| !paramMap.containsKey("networks")
					|| !paramMap.containsKey("queryset")
					|| !paramMap.containsKey("organism")) {
				try {
					error = "missing parameter";
					DatabaseService.writeJSONRPCTextResponse(null, error, 0,
							resp);
					return;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}

			String organism = paramMap.get("organism")[0];
			int iterations = Integer.parseInt(paramMap.get("iterations")[0]);
			double cutoff = Double.parseDouble(paramMap.get("cutoff")[0]);
			String networks[] = paramMap.get("networks")[0].split(",");
			String queryset[] = paramMap.get("queryset")[0].split(",");

			// check switch to superimpose networks
			boolean superimpose = false;
			if (paramMap.containsKey("superimpose")
					&& paramMap.get("superimpose")[0].equalsIgnoreCase("true")) {
				superimpose = true;
			}

			// check for valid parameters
			if (!DatabaseService.aliasTableNameHash.containsKey(organism)
					|| iterations < 1 || cutoff < 0 || networks.length < 1
					|| queryset.length < 1) {
				try {
					error = "invalid parameters";
					DatabaseService.writeJSONRPCTextResponse(null, error, 0,
							resp);
					return;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}

			try {
				DatabaseService.writeTextResponse(
						this.processQuery(organism, networks, queryset,
								iterations, PRIOR_FOR_BAD_PROB, cutoff,
								superimpose).toString(), resp);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			// nothing to do !
		}
	}

	/**
	 * Handle POST request.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
	}
	
	// TODO ///////////////////////////////////////////////////

	/**
	 * Perform rounds of iterative Bayes pathway expansion. Returns a JSON-RPC
	 * result object.
	 * 
	 * @param organism
	 *            NCBI taxonomy ID
	 * @param networks
	 * @param queryset
	 * @param iterations
	 * @param bad_prob
	 *            Prior expectation for percentage of pathway genes that are not
	 *            in the pathway
	 * @param cutoff
	 *            Cutoff for reporting results
	 * @param superimpose
	 *            superimpose query networks before expansion
	 * @return
	 * @throws Exception
	 */
	private JSONObject processQuery(final String organism,
			final String[] networks, final String[] queryset,
			final int iterations, final double bad_prob, final double cutoff,
			final boolean superimpose) throws Exception {
		JSONObject resultJO = new JSONObject();
		String error = null;
		int jsonRpcID = 0;

		long start = (new Date()).getTime();
		JSONObject timeJO = new JSONObject();

		// encode query information
		JSONObject jo = new JSONObject();
		jo.put("organism", organism);
		jo.put("iterations", iterations);
		jo.put("superimpose", superimpose);

		// query nets
		List<String> networkList = Arrays.asList(networks);
		HashSet<String> networkSet = new HashSet<String>(networkList);

		JSONArray ja = new JSONArray();
		for (String network : networkSet) {
			ja.put(network);
		}
		jo.put("networks", ja);

		// query set
		List<String> queryIDList = Arrays.asList(queryset);
		HashSet<String> queryIDSet = new HashSet<String>(queryIDList);
		final int query_size = queryIDSet.size();

		resultJO.put("query_size", query_size);

		ja = new JSONArray();
		for (String queryID : queryIDSet) {
			ja.put(queryID);
		}
		jo.put("queryset", ja);

		resultJO.put("query", jo);

		final HashMap<String, NetworkLinkTreeMap> networkLinksHashMap = new HashMap<String, NetworkLinkTreeMap>();

		if (superimpose) {
			// setup superimposed network data
			networkLinksHashMap.put("superimposed", new NetworkLinkTreeMap());

			// get network data
			for (String network : networkSet) {
				NetworkLinkTreeMap links = getAllEdgesAsNetworkLinkTreeMap(network);

				networkLinksHashMap.get("superimposed").append(links);
			}
		} else {
			// get network data
			for (String network : networkSet) {
				NetworkLinkTreeMap links = getAllEdgesAsNetworkLinkTreeMap(network);
				networkLinksHashMap.put(network, links);
			}
		}

		// compute average weight of networks
		final HashMap<String, Double> networkAveWeightHashMap = new HashMap<String, Double>();
		for (String network : networkLinksHashMap.keySet()) {
			final NetworkLinkTreeMap linkData = networkLinksHashMap
					.get(network);

			double averageScore = linkData.getAverageScore();
			networkAveWeightHashMap.put(network, averageScore);
		}

		// element universe
		final HashSet<String> genesUniverse = this
				.getGenesUniverse(networkLinksHashMap);
		final int universe_size = genesUniverse.size();

		resultJO.put("universe_size", universe_size);

		// missing query genes
		final HashSet<String> missingQueryGenes = findMissingElements(
				queryIDSet, genesUniverse);
		final int num_missing_query_genes = missingQueryGenes.size();

		jo = new JSONObject();
		jo.put("number", num_missing_query_genes);

		ja = new JSONArray();
		for (String id : missingQueryGenes) {
			ja.put(id);
		}
		jo.put("list", ja);

		resultJO.put("missing_query_genes", jo);

		// set priors
		HashMap<String, HashMap<String, Double>> geneScoreHashMap = setPriorLogOdds(
				queryIDSet, genesUniverse, PRIOR_FOR_NEW_GENES_RATIO,
				PRIOR_FOR_BAD_PROB);

		timeJO.put("setup", ((new Date()).getTime() - start));
		start = (new Date()).getTime();

		// iterate
		for (int i = 0; i < iterations; i++) {
			bayesUpdate(geneScoreHashMap, networkLinksHashMap,
					networkAveWeightHashMap);
		}

		timeJO.put("iteration", ((new Date()).getTime() - start));
		start = (new Date()).getTime();

		// output
		ja = resultsToJson(geneScoreHashMap, iterations, cutoff);
		resultJO.put("expansionScores", ja);

		timeJO.put("output", ((new Date()).getTime() - start));
		start = (new Date()).getTime();

		resultJO.put("time", timeJO);

		return DatabaseService
				.encodeJsonRpcResponse(resultJO, error, jsonRpcID);
	}

	/**
	 * Collect pathway expansion scores. Score is the cumulative probability
	 * divided by (number of iterations + 1).
	 * 
	 * @param geneScoreHashMap
	 * @param iterations
	 * @param cutoff
	 * @return
	 * @throws JSONException
	 */
	private JSONArray resultsToJson(
			final HashMap<String, HashMap<String, Double>> geneScoreHashMap,
			final double iterations, final double cutoff) throws JSONException {
		ArrayList<JSONObject> joList = new ArrayList<JSONObject>();

		HashMap<String, Double> scoresHashMap;
		double reportedScore;

		for (String gene : geneScoreHashMap.keySet()) {
			scoresHashMap = geneScoreHashMap.get(gene);

			// compute reported score
			reportedScore = scoresHashMap.get(CUMULATIVE_PROB) / iterations;

			// enforce cutoff
			if (reportedScore < cutoff) {
				continue;
			}

			// get results for gene
			JSONObject jo = new JSONObject();
			jo.put("ID", gene);
			jo.put("query", scoresHashMap.get(QUERY_GENE));
			jo.put("score", reportedScore);
			joList.add(jo);
		}

		// sort the results by score
		Collections.sort(joList,
				Collections
						.reverseOrder(new DatabaseService.JSONNumberComparator(
								"score")));

		JSONArray ja = new JSONArray();

		// for specifying sig-fig
		DecimalFormat score = new DecimalFormat("0.000000");

		for (JSONObject jo : joList) {
			double longScore = jo.getDouble("score");
			jo.put("score", Double.valueOf(score.format(longScore)));
			ja.put(jo);
		}

		return ja;
	}

	/**
	 * Executes one round of Bayesian updating and updates %$logdataodds. For
	 * each data source and each vertex, the updating computes the sum of the
	 * weights of the neighbors $total_wt and the weighted sum of the
	 * probabilities $pathway_wt. Considers the vertex as having
	 * $total_wt/$avg_wt[data source] neighbors with $pathway_wt/$avg_wt[data
	 * source] neighbors in the pathway. The score with respect to the data
	 * source for the vertex is -log(int_{N_0}^{n}{p^x(1-p)^n-x
	 * dx}/int_{0}^{n}{p^x(1-p)^n-x dx}), where p is the probability that a
	 * randomly chosen gene in the data source would be in the pathway, N_0 =
	 * $pathway_wt/$avg_wt[data source], and N = $total_wt/$avg_wt[data source].
	 * This score represents the probability that a randomly chosen gene with N
	 * neighbors would have at least N_0 pathway neighbors, assuming that the
	 * neighbors are independent. The updating then creates conditional score
	 * distributions P(score | pathway) and P(score | not pathway) by counting
	 * the score of the gene as P(gene) pathway observations and 1 - P(gene)
	 * non-pathway observations, where P(gene) is the probability that the gene
	 * is in the pathway. These score distributions are binned and exponentially
	 * smoothed, and the data log odds are then updated based on these
	 * distributions and the prior log odds.
	 * 
	 * @param geneScoreHashMapParam
	 * @param networkLinksHashMapParam
	 * @param networkAveWeightHashMapParam
	 */
	private void bayesUpdate(
			final HashMap<String, HashMap<String, Double>> geneScoreHashMapParam,
			final HashMap<String, NetworkLinkTreeMap> networkLinksHashMapParam,
			final HashMap<String, Double> networkAveWeightHashMapParam) {

		// calculate scores
		/*
		 * network, gene, score
		 */
		HashMap<String, HashMap<String, Double>> networkScoresHashMap = new HashMap<String, HashMap<String, Double>>();
		for (String network : networkLinksHashMapParam.keySet()) {
			/**
			 * raw scores for this network
			 */
			HashMap<String, Double> rawScoresHashMap = new HashMap<String, Double>();

			NetworkLinkTreeMap networkData = networkLinksHashMapParam
					.get(network);

			double baseline_prob = computeBaselineProb(geneScoreHashMapParam,
					networkData);

			for (String gene : geneScoreHashMapParam.keySet()) {
				if (!networkData.containsKey(gene)) {
					// only consider genes that participate in the network
					continue;
				}
				double network_ave_weight = networkAveWeightHashMapParam
						.get(network);

				double pathway_weight = 0;
				double total_weight = 0;

				Set<String> neighborsSet = networkData.getNeighbors(gene);

				// get weight of neighbors on this gene
				for (String neighbor : neighborsSet) {
					// edge weight
					double edge_weight = networkData.getScore(gene, neighbor);

					// total weight of edges
					total_weight += edge_weight;

					// neighbor's pathway score
					double prob = logOddsToProb(geneScoreHashMapParam.get(
							neighbor).get(CURRENT_SCORE));

					// contribution to this gene's pathway weight
					pathway_weight += edge_weight * prob;
				}

				// compute raw score for this gene in this network
				double raw_score;
				if ((network_ave_weight > 0) && (baseline_prob > 0)
						&& (baseline_prob < 1)) {
					pathway_weight /= network_ave_weight;
					total_weight /= network_ave_weight;

					double baselineLogOdds = probToLogOdds(baseline_prob);

					double prob = (Math.exp(total_weight * baselineLogOdds) - Math
							.exp(pathway_weight * baselineLogOdds))
							/ (Math.exp(total_weight * baselineLogOdds) - 1);

					if (prob == 0) {
						prob = MIN_PROB;
					}
					raw_score = -1 * Math.log(prob);
				} else {
					raw_score = 0;
				}
				rawScoresHashMap.put(gene, raw_score);
			}

			// normalize scores
			normalizeScores(rawScoresHashMap, MIN_PROB);
			networkScoresHashMap.put(network, rawScoresHashMap);
		}

		// Create and smooth positive and negative conditional distributions
		HashMap<String, HashMap<Integer, Double>> smoothedPosCondDistributions = new HashMap<String, HashMap<Integer, Double>>();
		HashMap<String, HashMap<Integer, Double>> smoothedNegCondDistributions = new HashMap<String, HashMap<Integer, Double>>();

		for (String network : networkLinksHashMapParam.keySet()) {
			// get distribution of scores
			HashMap<String, Double> normalizedGeneScoreHashMap = networkScoresHashMap
					.get(network);

			/**
			 * (Key: score) & (Value: count or prob)
			 */
			HashMap<Double, Double> posCondDistrHashMap = new HashMap<Double, Double>();

			/**
			 * (Key: score) & (Value: count or prob)
			 */
			HashMap<Double, Double> negCondDistrHashMap = new HashMap<Double, Double>();

			for (String gene : normalizedGeneScoreHashMap.keySet()) {
				double normalized_gene_score = normalizedGeneScoreHashMap
						.get(gene);
				double current_gene_prob = logOddsToProb(geneScoreHashMapParam
						.get(gene).get(CURRENT_SCORE));

				double pos_prob = 0;
				double neg_prob = 0;

				if (posCondDistrHashMap.containsKey(normalized_gene_score)) {
					// accumulate previously encountered scores
					pos_prob = posCondDistrHashMap.get(normalized_gene_score);
					neg_prob = negCondDistrHashMap.get(normalized_gene_score);
				}

				pos_prob += (current_gene_prob);
				neg_prob += (1 - current_gene_prob);

				posCondDistrHashMap.put(normalized_gene_score, pos_prob);
				negCondDistrHashMap.put(normalized_gene_score, neg_prob);
			}

			// perform smoothing of score distributions
			HashMap<Integer, Double> smoothedPosCondDistrHashMap = performExponentialSmoothing(
					posCondDistrHashMap, NUM_SMOOTHING_BINS, BETA);

			HashMap<Integer, Double> smoothedNegCondDistrHashMap = performExponentialSmoothing(
					negCondDistrHashMap, NUM_SMOOTHING_BINS, BETA);

			smoothedPosCondDistributions.put(network,
					smoothedPosCondDistrHashMap);

			smoothedNegCondDistributions.put(network,
					smoothedNegCondDistrHashMap);
		}

		// Determine the positive and negative conditional
		// probabilities and update overall probabilities.
		// Uses a Naive Bayes assumption to decompose data
		// from multiple data sources.
		for (String gene : geneScoreHashMapParam.keySet()) {
			double currentLogOdds = geneScoreHashMapParam.get(gene).get(
					CURRENT_SCORE);

			for (String network : networkScoresHashMap.keySet()) {
				HashMap<String, Double> scoresHashMap = networkScoresHashMap
						.get(network);

				if (scoresHashMap.containsKey(gene)) {
					double score = scoresHashMap.get(gene);
					int bin = (int) Math.floor(NUM_SMOOTHING_BINS * score);

					double posProb = smoothedPosCondDistributions.get(network)
							.get(bin);

					double negProb = smoothedNegCondDistributions.get(network)
							.get(bin);

					if ((posProb > 0) && (negProb > 0)) {
						currentLogOdds += (Math.log(posProb) - Math
								.log(negProb));
					} else if (posProb > 0) {
						currentLogOdds += (Math.log(posProb) - Math
								.log(MIN_PROB));
					}
				}
			}

			double cumulativeProbs = geneScoreHashMapParam.get(gene).get(
					CUMULATIVE_PROB)
					+ logOddsToProb(currentLogOdds);

			geneScoreHashMapParam.get(gene).put(CUMULATIVE_PROB,
					cumulativeProbs);
			geneScoreHashMapParam.get(gene).put(CURRENT_SCORE, currentLogOdds);
		}
	}

	/**
	 * Performs exponential smoothing with constant $beta and resolution $res.
	 * The distribution to be smoothed is assumed to have values on [0, 1), but
	 * can be represented in terms of raw frequencies. The smoothed relative
	 * contribution of a bin with left endpoint j to a bin with left endpoint i
	 * is int_i^{i+1} {e^{-$beta|x - i|/l} dx} / int_0^{n}{e^{-$beta|x - i|/l}
	 * dx}. The final output is a binned probability distribution with $res
	 * bins.
	 * 
	 * @param distrHashMap
	 * @param resolution
	 * @param betaParameter
	 * @return
	 */
	private static HashMap<Integer, Double> performExponentialSmoothing(
			final HashMap<Double, Double> distrHashMap,
			final double resolution, final double betaParameter) {
		HashMap<Integer, Double> binnedDistrHashMap = new HashMap<Integer, Double>();
		HashMap<Integer, Double> smoothedDistrHashMap = new HashMap<Integer, Double>();

		// bin the input distribution
		for (int bin = 0; bin < resolution; bin++) {
			binnedDistrHashMap.put(bin, Double.valueOf(0));
			smoothedDistrHashMap.put(bin, Double.valueOf(0));
		}
		for (double score : distrHashMap.keySet()) {
			int bin = (int) Math.floor(score * resolution);
			double count = binnedDistrHashMap.get(bin)
					+ distrHashMap.get(score);

			binnedDistrHashMap.put(bin, count);
		}

		// Perform exponential smoothing
		for (int i = 0; i < resolution; i++) {

			double weight = (resolution / betaParameter)
					* (2 - Math.exp(-1 * betaParameter * i / resolution) - Math
							.exp(-1 * betaParameter
									* ((resolution - i) / resolution)));

			if (Double.compare(weight, Double.valueOf(0)) == 0) {
				continue;
			}

			// right side of bin
			for (int j = 0; j < i; j++) {
				double smoothed_count = smoothedDistrHashMap.get(j);
				smoothed_count += binnedDistrHashMap.get(i)
						* resolution
						* Math.exp(-1 * betaParameter * i / resolution)
						* (Math.exp(betaParameter * (j + 1) / resolution) - Math
								.exp(betaParameter * j / resolution))
						/ (betaParameter * weight);

				if (Double.isNaN(smoothed_count)) {
					continue;
				}
				smoothedDistrHashMap.put(j, smoothed_count);
			}

			// left side of bin
			for (int j = i; j < resolution; j++) {
				double smoothed_count = smoothedDistrHashMap.get(j);
				smoothed_count += binnedDistrHashMap.get(i)
						* resolution
						* Math.exp(betaParameter * i / resolution)
						* (Math.exp(-1 * betaParameter * j / resolution) - Math
								.exp(-1 * betaParameter * (j + 1) / resolution))
						/ (betaParameter * weight);
				if (Double.isNaN(smoothed_count)) {
					continue;
				}
				smoothedDistrHashMap.put(j, smoothed_count);
			}
		}

		// Normalize to a probability
		double total_weight = 0;
		for (int i = 0; i < resolution; i++) {
			total_weight += smoothedDistrHashMap.get(i);
		}
		if (total_weight > 0) {
			for (int i = 0; i < resolution; i++) {
				double normalized = smoothedDistrHashMap.get(i) / total_weight;
				smoothedDistrHashMap.put(i, normalized);
			}
		}

		return smoothedDistrHashMap;
	}

	/**
	 * Force scores to be in the range [0,1). Assumes non-negative scores.
	 * Divides everything by (max score + epsilon).
	 * 
	 * @param rawScoresHashMap
	 * @param epsilon
	 */
	private static void normalizeScores(
			HashMap<String, Double> rawScoresHashMap, final double epsilon) {
		double max_score = 0;

		// find the biggest score
		for (String gene : rawScoresHashMap.keySet()) {
			if (rawScoresHashMap.get(gene) > max_score) {
				max_score = rawScoresHashMap.get(gene);
			}
		}

		// System.out.println("largest unnormalized score: " + max_score);

		max_score += epsilon;

		// rescale
		for (String gene : rawScoresHashMap.keySet()) {
			double new_score = rawScoresHashMap.get(gene) / max_score;
			rawScoresHashMap.put(gene, new_score);
		}
	}

	/**
	 * Computes the expected probability that a randomly chosen gene from the
	 * data source %$data will be in the pathway as the sum of the probabilities
	 * of the genes in the data source divided by the number of genes in the
	 * data source.
	 * 
	 * @param geneScoreHashMap
	 * @param networkLinkTreeMap
	 * @return
	 */
	private static double computeBaselineProb(
			HashMap<String, HashMap<String, Double>> geneScoreHashMap,
			NetworkLinkTreeMap networkLinkTreeMap) {

		double pathway_total_prob = 0;
		for (String gene : networkLinkTreeMap.keySet()) {
			pathway_total_prob += logOddsToProb(geneScoreHashMap.get(gene).get(
					CURRENT_SCORE));
		}

		if (networkLinkTreeMap.keySet().size() > 0) {
			return pathway_total_prob / networkLinkTreeMap.keySet().size();
		} else {
			return 0;
		}
	}

	/**
	 * Computes prior probabilities that a gene is in a pathway. The output is a
	 * hash with the logs of the odds associated with the probabilities. For a
	 * gene in the pathway, the log odds are set to
	 * log((1-$bad_prob/$bad_prob)), where $bad_prob is the expected percentage
	 * of erroneously included genes, and for a gene outside the pathway, the
	 * log odds are set based on a probability of ($undiscovered - 1 +
	 * $bad_prob)|$pathway|/(|$genes| - |$pathway|). This choice gives
	 * $undiscovered*$num_pathway_genes expected pathway genes.
	 * 
	 * @param queryIDSet
	 *            pathway genes
	 * @param genesUniverse
	 *            universe of genes
	 * @param newGenesRatio
	 *            Prior expectation for the ratio: (number of expected pathway
	 *            genes) / (number of given pathway genes)
	 * @param badProb
	 *            prior for percentage of erroneously included genes
	 * @return
	 */
	private static HashMap<String, HashMap<String, Double>> setPriorLogOdds(
			final HashSet<String> queryIDSet,
			final HashSet<String> genesUniverse, final double newGenesRatio,
			final double badProb) {

		double query_size = queryIDSet.size();
		double universe_size = genesUniverse.size();

		HashMap<String, HashMap<String, Double>> resultHashMap = new HashMap<String, HashMap<String, Double>>();

		for (String gene : genesUniverse) {
			HashMap<String, Double> scoresHashMap = new HashMap<String, Double>();
			double prob;

			// set to 0 for non-query gene, 1 for query gene
			double query = 0;
			if (queryIDSet.contains(gene)) {
				// a query gene
				prob = 1 - badProb;
				query = 1;
			} else {
				// not a query gene
				if (universe_size > query_size) {
					// the universe is larger than query
					prob = (newGenesRatio - 1 + badProb)
							/ (universe_size - query_size);
				} else {
					// the universe is smaller than query
					prob = MAX_PROB;
				}
			}

			double logOdds = probToLogOdds(prob);

			scoresHashMap.put(CURRENT_SCORE, logOdds);
			scoresHashMap.put(CUMULATIVE_PROB, Double.valueOf(0));
			scoresHashMap.put(QUERY_GENE, query);
			resultHashMap.put(gene, scoresHashMap);
		}

		return resultHashMap;
	}

	/**
	 * Find elements that are in the query set and not in the universe.
	 * 
	 * @param query
	 * @param universe
	 * @return
	 */
	private static HashSet<String> findMissingElements(
			final HashSet<String> query, final HashSet<String> universe) {
		// all genes in the query set that are also in the universe
		HashSet<String> queryFoundInUniverse = (HashSet<String>) universe
				.clone();
		queryFoundInUniverse.retainAll(query);

		// find out which genes where in the query set, but NOT in the universe
		HashSet<String> missingElements = (HashSet<String>) query.clone();
		missingElements.removeAll(queryFoundInUniverse);

		return missingElements;
	}

	/**
	 * Get the elements universe of the set of networks. Involves MySQL queries.
	 * 
	 * @param networkSet
	 * @return
	 */
	private HashSet<String> getGenesUniverse(final Set<String> networkSet) {
		HashSet<String> genesUniverseHashSet = new HashSet<String>();

		for (String network : networkSet) {
			HashSet<String> networkElements = getAllNetworkElements(network);
			genesUniverseHashSet.addAll(networkElements);
		}
		return genesUniverseHashSet;
	}

	/**
	 * Get the elements universe of the set of networks.
	 * 
	 * @param networkLinks
	 * @return
	 */
	private HashSet<String> getGenesUniverse(
			HashMap<String, NetworkLinkTreeMap> networkData) {
		HashSet<String> genesUniverseHashSet = new HashSet<String>();

		for (String network : networkData.keySet()) {
			NetworkLinkTreeMap links = networkData.get(network);
			genesUniverseHashSet.addAll(links.keySet());
		}
		return genesUniverseHashSet;
	}

	/**
	 * Convert logodds to prob
	 * 
	 * @param logOdds
	 * @return
	 */
	private static double logOddsToProb(final double logOdds) {
		double odds = Math.exp(logOdds);

		double prob = odds / (1 + odds);

		// check for nan, do not allow prob = 1
		if (prob * 1 != prob) {
			prob = MAX_PROB;
		} else if (prob < MIN_PROB) {
			// what about when prob = 0?
			prob = MIN_PROB;
		}

		return prob;
	}

	/**
	 * Convert prob to logodds
	 * 
	 * @param prob
	 * @return
	 */
	private static double probToLogOdds(final double prob) {
		double odds;
		if (prob > MAX_PROB) {
			odds = MAX_PROB / (1 - MAX_PROB);
		} else {
			odds = prob / (1 - prob);
		}
		double logodds = Math.log(odds);
		return (logodds);
	}
}
