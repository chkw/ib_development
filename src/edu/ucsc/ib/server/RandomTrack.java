package edu.ucsc.ib.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

public class RandomTrack extends Track {
    public RandomTrack() {
	
    }
    
    @Override
    public String[][] getSpanningLinks(TreeSet<String> q, TreeSet<String> u) {
	ArrayList<String[]> result = new ArrayList<String[]>();
	HashSet<String> pb = new HashSet<String>(q);
	pb.addAll(u);
	double edgePercentage = 5.0 / (u.size() + 1);

	for (String b : q) {
	    for (String b2 : pb) {
		if ((b.compareTo(b2) < 0) && Math.random() < edgePercentage) {
		    result.add((b.compareTo(b2) > 0) ? new String[] { b2, b }
			    : new String[] { b, b2 });
		}
	    }
	}

	return result.toArray(new String[0][]);
    }
}
