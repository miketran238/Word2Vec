package retrieval;

import java.util.LinkedHashMap;

import com.medallia.word2vec.SearcherImpl;

public interface RetrievalImpl {
	
	public void retrieve(Query query, LinkedHashMap<RetrievedCodeExample, Double> rankedCodeExps, 
			SearcherImpl vectorModelSrc);

}
