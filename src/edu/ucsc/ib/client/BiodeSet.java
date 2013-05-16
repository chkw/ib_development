/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client;

import java.util.Collection;
import java.util.HashSet;

/**
 * A set of biode identifiers (Strings).
 * 
 * TODO this should be changed to HAS-A rather than IS-A
 * 
 * @gwt.typeargs <java.lang.String>
 */
public class BiodeSet extends HashSet<String> {
	private static final long serialVersionUID = -5850078436596917336L;

	public BiodeSet() {
		super();
	}

	public BiodeSet(String biode) {
		this.add(biode);
	}

	/**
	 * Construct a BiodeSet object from a String[].
	 * 
	 * @param biodes
	 */
	public BiodeSet(String[] biodes) {
		for (String biode : biodes) {
			this.add(biode);
		}
	}

	public BiodeSet(Collection<String> b) {
		super(b);
	}

	public String[] getArray() {
		String[] result = new String[size()];
		int index = 0;

		for (String biode : this) {
			result[index++] = biode;
		}
		return result;
	}

}
