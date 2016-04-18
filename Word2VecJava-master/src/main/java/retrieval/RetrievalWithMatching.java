package retrieval;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Map.Entry;

import com.medallia.word2vec.SearcherImpl;
import com.medallia.word2vec.Word2VecModel;
import com.medallia.word2vec.Word2VecProcessingJava;
import com.medallia.word2vec.util.FileUtils;

public class RetrievalWithMatching {
	
	public static void main(String[] args) {
		RetrievalWithMatching retrieval = new RetrievalWithMatching();
		retrieval.buildGroundTruth();
	}
	
	
	/* Create tube <word, API> with score */
	
	public void buildGroundTruth() {
			// Get model to determine vector representations for each sequence
			Word2VecModel model = null;
			SearcherImpl searchImpl = null;
			try {
				model = Word2VecModel.fromBinFile(new File("text8.bin"));
				searchImpl = new SearcherImpl(model);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		
		
		/* Read database of corresponding text and code sequences and store a mapping data -Oracle data, pair <query, code example> */
		HashMap<Query, RetrievedCodeExample> oracleQueryCodeEx = new HashMap<>();
		HashMap<String, double[]> retAPIList = new HashMap<>();
		
		
		// For each query, calculate cosine distance to ever code element, sort by descending order
		// get first element. Retrieve examples including this first element
		// store accumulate cosine. If there are > 1 examples, start with the second best
		// getting one who has the best score in term of second best
			
		HashMap<String, HashSet<RetrievedCodeExample>> codeExContainsGivenAPI = new HashMap<>(); 
		
		// tokenize query and code => get average vector for query while get vector for each code element
		// Each code element, note to store examples that contains this element

		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		
		@SuppressWarnings("unchecked")
		HashSet<Integer> skipLines = (HashSet<Integer>) FileUtils.readObjectFile(s + "/data/retrieval/KJ_API2VECTop5.dat"); //KodeJava_topKOver5
		
		try {
			Scanner textFR = new Scanner(new File(s+ "/data/retrieval/KodeJava_439.en"));
			Scanner apiFR = new Scanner(new File(s+ "/data/retrieval/KodeJava_439.cod"));
			
			
			int lineCount = 0, limit = 50;
			while(textFR.hasNextLine() && apiFR.hasNextLine()) {
				lineCount ++;
				String text = textFR.nextLine();
				String code = apiFR.nextLine();
				
				if(!skipLines.contains(lineCount))
					continue;
//				if(lineCount > limit)
//					continue;
				
				// Query information
				Query query = new Query();
				query.queryId = lineCount;
				query.text = text;
				
				String[] textTokens = text.split("\\s");
				query.avgVector = searchImpl.getAverageVector(textTokens);
				if(query.avgVector == null)
					continue;
				
				// Code example information
				RetrievedCodeExample codeEx = new RetrievedCodeExample();
				codeEx.exId = lineCount;
				codeEx.example = code;
				
				String[] APIs = code.split("\\s");
				for(String API : APIs) {
//					if(API.contains("PrintStream::") || API.contains("String::"))
//						continue;
					double[] apiVec;
					if(retAPIList.containsKey(API))
						apiVec = retAPIList.get(API);
					else
						apiVec = searchImpl.getVectorOrNull(API);
					if(apiVec != null) {
						codeEx.codeElements.put(API, apiVec);
						
						// add this API to the list of all API in the retrieval database
						retAPIList.put(API, apiVec);
						
						HashSet<RetrievedCodeExample> exContainThisAPI = codeExContainsGivenAPI.get(API);
						if(exContainThisAPI == null) {
							exContainThisAPI = new HashSet<>();
							codeExContainsGivenAPI.put(API, exContainThisAPI);
						}
						exContainThisAPI.add(codeEx);
					}
				}
				
				oracleQueryCodeEx.put(query, codeEx);
			}
			
			textFR.close();
			apiFR.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		/** There are several support method
		 * 1. Given a vector and a list of candidate query, find the best and also its score
		 *  */

		
		// Retrieval part, setup top K
		int K = 1;
		// count number of example get accurate in top K
		int countAccurate = 0;
		HashSet<Integer> filteredExamples = new HashSet<>();
		
		
		for(Query query : oracleQueryCodeEx.keySet()) {
			double[] avgQueryVec = query.avgVector;
			// calculate cosine distance between this query average vector and every API and store results in Map
			LinkedHashMap<String, Double> cosineScoreToSingleAPI = new LinkedHashMap<>();
			for(String API : retAPIList.keySet()) {
				double[] apiVec = retAPIList.get(API);
				double score = searchImpl.calculateDistance(avgQueryVec, apiVec);
				cosineScoreToSingleAPI.put(API, score);
			}
			
			LinkedHashMap<String, Double> sortedCosineMeasure = Word2VecProcessingJava.sortMap(cosineScoreToSingleAPI);
			// J is the maximum index (ordered) of APIs that more than 1 examples share common API 
			int J = 0;
			for(String closestAPIsToQuery : sortedCosineMeasure.keySet()) {
				HashSet<RetrievedCodeExample> candCodeExams = codeExContainsGivenAPI.get(closestAPIsToQuery);
				J ++;
				if(candCodeExams.size() == 1)
						break;
			}
			
			// reset counter and score to 0 for all examples from the last query to process the current one
			for(RetrievedCodeExample candCodeExp : oracleQueryCodeEx.values()) {
				candCodeExp.count = 0;
				candCodeExp.score = 0.0;
			}
			
			// score examples with regard to query and rank
			LinkedHashMap<RetrievedCodeExample, Double> retMeasureWrtQuery = new LinkedHashMap<>();
			
			for(String closestAPIsToQuery : sortedCosineMeasure.keySet()) {
				for(RetrievedCodeExample candCodeExp : oracleQueryCodeEx.values()) {
					if(candCodeExp.count < J && candCodeExp.codeElements.containsKey(closestAPIsToQuery)) {
						candCodeExp.count ++;
						candCodeExp.score += sortedCosineMeasure.get(closestAPIsToQuery);
					}
					if (candCodeExp.count == J)
						retMeasureWrtQuery.put(candCodeExp, candCodeExp.score / (double) J);
				}
			}
			
			if(query.queryId < 50)
				filteredExamples.add(query.queryId);
				// Order by new retrieval score and get top K
				int k = 0;
				
				LinkedHashMap<RetrievedCodeExample, Double> sortedRetMeasureWrtQuery = sortObjMap(retMeasureWrtQuery);
				for(RetrievedCodeExample sortedEx : sortedRetMeasureWrtQuery.keySet()) {
					if(k++ < K && sortedEx == oracleQueryCodeEx.get(query)) {
						countAccurate ++;
						filteredExamples.add(query.queryId);
						break;
					}
					if(sortedEx == oracleQueryCodeEx.get(query)) {
//						System.out.println(query.text + "\t" + sortedEx.example + "\t" + k);
						break;
					}
				}
		}
//		FileUtils.writeObjectFile(filteredExamples, s + "/data/retrieval/KJ_API2VECTop5.dat");
		System.out.printf("Retrieval top-%d accuracy: %f", K, countAccurate/(double) oracleQueryCodeEx.size());
	}
	
	public static LinkedHashMap<RetrievedCodeExample, Double> sortObjMap(LinkedHashMap<RetrievedCodeExample, Double> unsortedMap) {
		List<Entry<RetrievedCodeExample, Double>> list = new LinkedList<>(unsortedMap.entrySet());
		
		Collections.sort(list, new Comparator<Entry<RetrievedCodeExample, Double>>() {
			public int compare(Entry<RetrievedCodeExample, Double> o1, Entry<RetrievedCodeExample, Double> o2) {
				return o2.getValue().compareTo(o1.getValue()); // descending order
			}
		});
		
		LinkedHashMap<RetrievedCodeExample, Double> sortedMap = new LinkedHashMap<>();
		for(Entry<RetrievedCodeExample, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		
		return sortedMap;
	}
}
