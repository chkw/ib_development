package edu.ucsc.ib.client;

/**
 * Some stats and mathematical formulae for the client.
 * 
 * @author chrisw
 * 
 */
public class Stats {

	/**
	 * Routine to handle n Choose x, the outcomes of x unordered draws from a
	 * population of size n, without replacement between draws.
	 * 
	 * @param n
	 *            number of choices (i.e. number of cards in a deck of cards)
	 * @param x
	 *            number of draws (number of cards drawn, without replacement)
	 * @return number of possible outcomes: n! / (x! * (n - x)!)
	 */
	public static final double nChooseX(int n, int x) {
		double result = 1.0;
		int divisor = n - x;

		if (n < x)
			return 0.0;

		// Shortcut. Code below would produce same result, just slower
		if (x == 0)
			return 1.0;

		while (n > x) {
			result *= ((double) n) / divisor;
			--n;
			--divisor;
		}

		return result;
	}

	/**
	 * Routine to handle n Choose x, the outcomes of x unordered draws from a
	 * population of size n, without replacement between draws. Done strictly as
	 * integers, so only call if know there won't be any overflow at any point
	 * during the calculations
	 * 
	 * @param n
	 *            number of choices (i.e. number of cards in a deck of cards)
	 * @param x
	 *            number of draws (number of cards drawn, without replacement)
	 * @return number of possible outcomes: n! / (x! * (n - x)!)
	 */
	public static final int nChooseXi(int n, int x) {
		int result = 1;
		int divisor = n - x;

		if (n < x)
			return 0;

		// Shortcut. Code below would produce same result, just slower
		if (x == 0)
			return 1;

		while (n > x) {
			result *= n;
			--n;
		}

		// Must be done separately because otherwise would face rounding issues
		// on the intermediate results.
		while (divisor > 1) {
			result /= divisor;
			--divisor;
		}

		return result;
	}

	/**
	 * Computes the hypergeometric probability of getting exactly numHits hits
	 * when doing numDraws draws from a sample space of size totalPop, where
	 * there are searchPop possible hits, when doing unordered drawing without
	 * replacement.
	 * 
	 * @param totalPop
	 *            Total size of the population we're drawing from
	 * @param searchPop
	 *            Number of elements in the population that count as hits
	 * @param numDraws
	 *            Number of times we draw from the total population
	 * @param numHits
	 *            Exact number of hits we got with our draws
	 * @return p value of getting exactly that many hits
	 */
	public static final double hypergeometric(int totalPop, int searchPop,
			int numDraws, int numHits) {
		double waysToDrawHits = nChooseX(searchPop, numHits);
		double waysToDrawMisses = nChooseX(totalPop - searchPop, numDraws
				- numHits);
		double waysToDraw = nChooseX(totalPop, numDraws);

		return (waysToDrawHits * waysToDrawMisses) / waysToDraw;
	}

	/**
	 * Computes the hypergeometric probability of getting numHits hits or more
	 * (less) when doing numDraws draws from a sample space of size totalPop,
	 * where there are searchPop possible hits, when doing unordered drawing
	 * without replacement.
	 * 
	 * @param totalPop
	 *            Total size of the population we're drawing from
	 * @param searchPop
	 *            Number of elements in the population that count as hits
	 * @param numDraws
	 *            Number of times we draw from the total population
	 * @param numHits
	 *            Number of hits we got with our draws
	 * @param orBetter
	 *            If true, compute for numHits hits or MORE, otherwise for
	 *            numHits hits or fewer
	 * @return p value of getting exactly that many hits
	 */
	public static final double hypergeometric(int totalPop, int searchPop,
			int numDraws, int numHits, boolean orBetter) {
		double pValue = 0.0;
		double waysToDraw = nChooseX(totalPop, numDraws);
		double waysToDrawHits, waysToDrawMisses;
		int nonHitPop = totalPop - searchPop;

		if (orBetter) {
			int max = numDraws;
			if (max > searchPop)
				max = searchPop;

			while (numHits <= max) {
				waysToDrawHits = nChooseX(searchPop, numHits);
				waysToDrawMisses = nChooseX(nonHitPop, numDraws - numHits);
				pValue += (waysToDrawHits * waysToDrawMisses) / waysToDraw;
				++numHits;
			}
		} else {
			while (numHits >= 0) {
				waysToDrawHits = nChooseX(searchPop, numHits);
				waysToDrawMisses = nChooseX(nonHitPop, numDraws - numHits);
				pValue += (waysToDrawHits * waysToDrawMisses) / waysToDraw;
				--numHits;
			}
		}

		return pValue;
	}

	/**
	 * Calculate Euclidean distance between two points on an X-Y coordinate
	 * system.
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public static double euclideanDist(double x1, double y1, double x2,
			double y2) {
		double dX = x2 - x1;
		double dY = y2 - y1;
		return Math.sqrt((dX * dX) + (dY * dY));
	}

	/**
	 * Interpolate the value along a line between two points in one dimension.
	 * 
	 * @param percent
	 * @param minVal
	 * @param maxVal
	 * @return
	 */
	public static double linearInterpolation(final double percent,
			final double minVal, final double maxVal) {
		return ((maxVal - minVal) * percent) + minVal;
	}

}
