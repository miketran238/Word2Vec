package retrieval;

import java.util.HashMap;

public class RetrievedCodeExample {
	
	// index by line number
	public int exId;
	
	// cosine distance against the retrieval query
	public double score = 0.0;
	
	// count number of APIs needed to compute score wrt the query. Reset for another query
	public int count = 0;
	
	public String example;
	
	public HashMap<String, double[]> codeElements = new HashMap<>();

}
