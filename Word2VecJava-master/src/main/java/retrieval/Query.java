package retrieval;

import java.util.HashMap;

public class Query {
	
	// index the query by the line number in the file 
	public int queryId;
	
	public String text;
	
	// words in query
	public HashMap<String, double[]> words = new HashMap<>();
	
	// average vector of this query
	double[] avgVector;

}
