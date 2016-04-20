package retrieval;

import java.util.LinkedHashMap;
import java.util.List;

import com.medallia.word2vec.SearcherImpl;

public abstract class AbstractRetrievalAlgorithm implements RetrievalImpl {

	// configuration
	
	/* Number of top examples to be considered for accuracy*/
	public final static int topThreshold = 5;
	
	/* The top at which this algorithm correctly retrieves the example */
	protected List<Integer> corrRetrievedTop;
	
	/* main retrieval algorithm for retrieving code example given a query */
	public abstract void retrieve(Query query, LinkedHashMap<RetrievedCodeExample, Double> rankedCodeExps, 
			SearcherImpl vectorModelSrc);
	
	/* Given the correct example, ask for the top */
	public int getCorrectRetrievedTop(RetrievedCodeExample expCodeExa) {
		return 0;
	}
}
