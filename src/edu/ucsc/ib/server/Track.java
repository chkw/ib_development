package edu.ucsc.ib.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This is the Track object to be used on the server-side of the Interaction
 * Browser.
 * 
 * Uses java.nio.channels.FileChannel.map() to memory map the file.
 * 
 * @author Chris
 * 
 */
public class Track {
    /**
     * field delimiter as String: "\t"
     */
    public static final String FIELD_DELIMITER = "\t";

    /**
     * Link data is read into here.
     */
    private final TreeMap<String, TreeSet<String>> links;

    /**
     * Maps a link to its position in the memory mapped track file. The position
     * is where to set the map.position(int) in order to get the first character
     * of the link's attributes.
     */
    private final HashMap<String, Integer> linkToPositionMap;

    /**
     * Maps an attribute's name to its numbered field in the memory mapped track
     * file.
     */
    private final HashMap<String, Integer> attrNameToNumberMap;

    /**
     * Count of links. This is 1/2 the number of links in the links TreeMap,
     * since it keeps track of keys keyed on both sides of the link.
     */
    private int count;

    /**
     * Memory mapped track data file.
     */
    private final MappedByteBuffer map;

    /**
     * Default constructor doesn't do much. Sets final variables to null.
     */
    public Track() {
	this.links = null;
	this.linkToPositionMap = null;
	this.map = null;
	this.attrNameToNumberMap = null;
    }

    /**
     * Construct a Track object given a file name.
     * 
     * @param fileName
     * @throws IOException
     */
    public Track(String fileName) throws IOException {
	this(new File(fileName));
    }

    /**
     * Construct a Track object given a File. Assumes the File is readable.
     * 
     * @param f
     * @throws IOException
     */
    public Track(File f) throws IOException {
	links = new TreeMap<String, TreeSet<String>>();
	count = 0;
	linkToPositionMap = new HashMap<String, Integer>();
	attrNameToNumberMap = new HashMap<String, Integer>();
	map = this.mapDataFile(f);
	
	StringBuffer strBuf = new StringBuffer();
	strBuf.append(f.getPath() + " mapped track file size: "
		+ map.capacity() + " AND ");

	long start = (new Date()).getTime();
	this.readInLinks();
	
	strBuf.append("read " + this.count + " links in "
		+ ((new Date()).getTime() - start) + " ms" + " AND ");

	strBuf.append("found the following attribute names: "
		+ this.attrNameToNumberMap.toString() + "\n");
	
	System.out.println(strBuf.toString());
    }

    /**
     * Read in links from the memory mapped track data file and store them in
     * the links TreeSet.
     */
    private void readInLinks() {
	int startIndex;
	int lastIndex;
	int returnIndex;
	int size;
	byte[] bytes;
	String line;

	// start at the top!
	map.position(0);
	while (map.hasRemaining()) {
	    // read in chars until we get to eof
	    startIndex = map.position();
	    while ((map.hasRemaining()) && (map.get() != '\n')) {
		// keep going until eof or eol
		// hasRemaining tells us if eof
		// != '\n' tells us if eol

		// we use get() to get bytes instead of trying to get chars
		// This is due to Java using UTF-16 encoding versus the file
		// system's ASCII encoding.

		// position is moving down the file
	    }

	    returnIndex = map.position();

	    // save the last valid position of line
	    if (map.hasRemaining()) {
		// don't count the new line char
		lastIndex = map.position() - 1; // each char takes 2 bytes
	    } else {
		lastIndex = map.position();
	    }

	    // grab section of map into a String using saved indices
	    size = lastIndex - startIndex;
	    bytes = new byte[size];
	    map.position(startIndex);
	    map.get(bytes, 0, size);
	    line = new String(bytes);

	    // check for comment line
	    if (line.startsWith("#") && (startIndex == 0)) {
		// save attribute fields from header
		processHeaderLine(line);
	    } else {
		processLinkLine(startIndex, line);
	    }
	    // go to where we left off in the mapped file;
	    map.position(returnIndex);
	}
	// reset position to beginning of file
	map.position(0);
    }

    /**
     * Process the header line. Saves the names of the attributes found in the
     * header and also the column number that it's found in. The first 2 columns
     * ignored because they'll always be the 2 elements of the link.
     * 
     * @param line
     */
    private void processHeaderLine(String line) {
	// remove the first character... should be the # comment tag
	// split the line into fields
	String[] fields = line.substring(1).split(FIELD_DELIMITER);

	// save the fields into the HashMap... k:fieldName v:fieldNum
	// skip first two fields... these are just the link's node pair
	for (int i = 2; i < fields.length; i++) {
	    this.attrNameToNumberMap.put(fields[i], i);
	}
    }

    /**
     * Process a line that contains link data. The is method finds the two nodes
     * IDs and then saves the new link to the links TreeMap. Then, the file
     * position information is saved to linkToPositionMap. Also, the link count
     * is incrememented.
     * 
     * @param startIndex
     *                file position to save - index of first byte in first
     *                character of this link's line in memory mapped file
     * @param line
     *                the line to process
     */
    private void processLinkLine(int startIndex, String line) {
	// parse out the two nodes of the link
	String[] fields = line.split(FIELD_DELIMITER, 3);
	if (fields.length >= 2) { // is a link
	    // first key of a pair
	    if (!links.containsKey(fields[0])) {
		links.put(fields[0], new TreeSet<String>());
	    }
	    // using intern() here to fix out of memory errors when
	    // selecting large tracks ... it helps a bit, but still have
	    // problems with huge tracks
	    links.get(fields[0]).add(fields[1].intern());

	    // second key of a pair
	    if (!links.containsKey(fields[1])) {
		links.put(fields[1], new TreeSet<String>());
	    }
	    // using intern() here to fix out of memory errors when
	    // selecting large tracks
	    links.get(fields[1]).add(fields[0].intern());
	    ++count;

	    // save the position of the link to linkToPositionMap
	    // position of the first character of the link's data
	    this.setPositionMap(fields[0], fields[1], startIndex);
	}
    }

    /**
     * Map the data file to memory.
     * 
     * @param f
     * 
     * @return
     * @throws IOException
     */
    private MappedByteBuffer mapDataFile(File f) throws IOException {
	FileInputStream fis = new FileInputStream(f);
	FileChannel fc = fis.getChannel();
	MappedByteBuffer mbb = fc.map(MapMode.READ_ONLY, 0, fc.size());
	return mbb;
    }

    /**
     * Total number of links in the track.
     * 
     * @return
     */
    public int getLinkCount() {
	return count;
    }

    /**
     * Get all links that cross between two sets, as well as any inter-set links
     * in the <code>inter</code> argument
     * 
     * @param inter
     *                set of biodes where interbiode links are of interest
     * @param outer
     *                set of biodes where links to the inter set are of interest
     * @return an array of two-element String arrays, each two-element string
     *         array a link. For example, the first link in the result is
     *         String[0][0] with String[0][1].
     */
    public String[][] getSpanningLinks(TreeSet<String> inter,
	    TreeSet<String> outer) {
	if (inter == null) {
	    return new String[0][0];
	}
	if (outer != null) {
	    outer = new TreeSet<String>(outer);
	    outer.removeAll(inter);
	}

	List<String[]> result = new LinkedList<String[]>();

	for (String q : inter) {
	    TreeSet<String> all = links.get(q);
	    if (all != null) {
		all = new TreeSet<String>(all);

		TreeSet<String> inner = new TreeSet<String>(all.headSet(q));
		inner.retainAll(inter);
		for (String r : inner.headSet(q)) {
		    result.add(new String[] { r, q });
		}
		if (outer != null) {
		    all.retainAll(outer);
		    for (String r : all) {
			result.add(new String[] { q, r });
		    }
		}
	    }
	}

	return result.toArray(new String[0][0]);
    }

    /**
     * Get all the relevant lines of data from the memory mapped file for the
     * specified links.
     * 
     * @param linksArray
     * @return
     */
    private HashSet<String> getLinksHash(String[][] linksArray) {
	HashSet<String> result = new HashSet<String>();
	for (String[] linkNodes : linksArray) {
	    result.add(this.getLinkLine(linkNodes[0], linkNodes[1]));
	}
	return result;
    }

    /**
     * Get all link data for links that cross between two sets, as well as any
     * inter-set links in the <code>inter</code> argument
     * 
     * @param inter
     * @param outer
     * @return HashSet<String> containing rows of link data.
     */
    public HashSet<String> getLinksData(TreeSet<String> inter,
	    TreeSet<String> outer) {
	return this.getLinksHash(this.getSpanningLinks(inter, outer));
    }

    /**
     * Get all nodes in the track.
     * 
     * @return
     */
    public Set<String> getNodeSet() {
	return new HashSet<String>(links.keySet());
    }

    /**
     * Get all neighbors of a set of nodes. The resulting set of neighbor nodes
     * will not include any query nodes.
     * 
     * @param query
     * @return
     */
    public HashSet<String> getNeighbors(String[] query) {
	HashSet<String> neighbors = new HashSet<String>();
	List<String> querySet = (List<String>) Arrays.asList(query);
	if (query == null) {
	    return neighbors;
	}

	// get neighbors
	for (String biode : querySet) {
	    if (links.containsKey(biode)) {
		neighbors.addAll(links.get(biode));
	    }
	}

	// remove neighbors that are in the query set
	neighbors.removeAll(querySet);

	return neighbors;
    }

    /**
     * Find the number of links between the specified nodes.
     * 
     * @param biodes
     * @return
     */
    public int getLinkCount(String[] biodes) {
	int result = 0;
	String[][] links = this.getSpanningLinks(new TreeSet<String>(Arrays
		.asList(biodes)), null);
	for (String[] a : links) {
	    result += a.length;
	}
	return result / 2;
	// It should be result/2 since each link is found in the links
	// TreeMap twice (once for each of its nodes)??
    }

    /**
     * Get an ordered key for two specified biodes.
     * 
     * @param biodeA
     * @param biodeB
     * @return
     */
    private String getOrderedKey(String biodeA, String biodeB) {
	if (biodeB.compareTo(biodeA) < 0) {
	    return biodeB + FIELD_DELIMITER + biodeA;
	} else {
	    return biodeA + FIELD_DELIMITER + biodeB;
	}
    }

    /**
     * Get the linkToBufferData position value for the specified link. This
     * position is used to locate the beginning of this link's attributes in the
     * memory mapped file. It is the index to the first byte of the first
     * character of the attributes.
     * 
     * @param biodeA
     * @param biodeB
     * @return
     */
    private int getPosition(String biodeA, String biodeB) {
	return this.linkToPositionMap.get(this.getOrderedKey(biodeA, biodeB));
    }

    /**
     * Get the link's line from the memory mapped file in the form of String.
     * This is the whole, unparsed string for the link. It does not required the
     * biodes to be ordered.
     * 
     * @param biodeA
     * @param biodeB
     * @return
     */
    private String getLinkLine(String biodeA, String biodeB) {
	int startIndex = this.getPosition(biodeA, biodeB);
	int lastIndex;
	int size;
	byte[] bytes = null;
	String[] fields = null;

	// start at the top!
	map.position(startIndex);
	while ((map.hasRemaining()) && (map.get() != '\n')) {
	    // keep going until eof or eol
	    // hasRemaining tells us if eof
	    // != '\n' tells us if eol

	    // we use get() to get bytes instead of trying to get chars
	    // This is due to Java using UTF-16 encoding versus the file
	    // system's ASCII encoding.

	    // position is moving down the file
	}

	// save the last valid position of line
	if (map.hasRemaining()) {
	    // don't count the new line char
	    lastIndex = map.position() - 1; // each char takes 2 bytes
	} else {
	    lastIndex = map.position();
	}

	// grab section of map into a String using saved indices
	size = lastIndex - startIndex;
	bytes = new byte[size];
	map.position(startIndex);
	map.get(bytes, 0, size);
	// fields = new String(bytes).split(FIELD_DELIMITER);

	return new String(bytes);
    }

    /**
     * Get the attribute names.
     * 
     * @return
     */
    public Set<String> getAttributeNames() {
	return new HashSet<String>(this.attrNameToNumberMap.keySet());
    }

    /**
     * Get a link's specified attribute field.
     * 
     * @param biodeA
     * @param biodeB
     * @param name
     * @return
     */
    public String getAttribute(String biodeA, String biodeB, String name) {
	int index = this.attrNameToNumberMap.get(name);
	String[] line = this.getLinkLine(biodeA, biodeB).split(FIELD_DELIMITER);
	return line[index];
    }

    /**
     * Puts a key, value pair into the linkToPositionMap HashMap based on the
     * parameters. Can use this information to pull up the line from memory
     * mapped file.
     * 
     * @param biodeA
     * @param biodeB
     * @param pos
     * @param size
     * @return
     */
    private Integer setPositionMap(String biodeA, String biodeB, int pos) {
	return this.linkToPositionMap.put(this.getOrderedKey(biodeA, biodeB),
		pos);
    }
}
