package retrieval;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Scanner;

import com.medallia.word2vec.SearcherImpl;
import com.medallia.word2vec.Word2VecModel;
import com.medallia.word2vec.util.FileUtils;

public class BipartiteMatchingRetrieval {
	
	public static void main(String[] args) {
		BipartiteMatchingRetrieval retrieval = new BipartiteMatchingRetrieval();
		retrieval.matchQueryAndCode();
	}

	public void matchQueryAndCode() {
	// Get model to determine vector representations for each sequence
				Word2VecModel model = null;
				SearcherImpl searchImpl = null;
				try {
					model = Word2VecModel.fromBinFile(new File("text_" + RConfig.outputExtension + ".bin"));
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
			HashSet<Integer> skipLines = (HashSet<Integer>) FileUtils.readObjectFile(s + "/data/retrieval/KodeJava_topKOver5.dat");
			
			try {
				Scanner textFR = new Scanner(new File(s+ "/data/retrieval/KodeJava_439.en"));
				Scanner apiFR = new Scanner(new File(s+ "/data/retrieval/KodeJava_439.cod"));
				
				int lineCount = 0, limit = 500;
				while(textFR.hasNextLine() && apiFR.hasNextLine()) {
					lineCount ++;
					String text = textFR.nextLine();
					String code = apiFR.nextLine();
					
//					if(!skipLines.contains(lineCount))
//						continue;
					
					if(lineCount > limit)
						continue;
					
					
					// Query information
					Query query = new Query();
					query.queryId = lineCount;
					query.text = text;
					
					String[] textTokens = text.split("\\s");
					query.avgVector = searchImpl.getAverageVector(textTokens);
//					if(query.avgVector != null)
//						query.words.put(text, query.avgVector);
					for(String word : textTokens) {
						double[] wordVec;
						wordVec = searchImpl.getVectorOrNull(word);
						if(wordVec != null)
							query.words.put(word, wordVec);
					}
					
					
					// Code example information
					RetrievedCodeExample codeEx = new RetrievedCodeExample();
					codeEx.exId = lineCount;
					codeEx.example = code;
					
					String[] APIs = code.split("\\s");
					for(String API : APIs) {
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
			
			// Retrieval part, setup top K
			int K = 5;
			// count number of example get accurate in top K
			int countAccurate = 0;
			
			MWBMatchingAlgorithm bipartiteMatcher;
			
			for(Query query : oracleQueryCodeEx.keySet()) {
				LinkedHashMap<RetrievedCodeExample, Double> retMeasureWrtQuery = new LinkedHashMap<>();

				for(RetrievedCodeExample codeExample : oracleQueryCodeEx.values()) {
					// bipartite matching - the graph of size nxm
					int n = query.words.size();
					int m = codeExample.codeElements.size();
					bipartiteMatcher = new MWBMatchingAlgorithm(n, m);
					
					HashMap<Pair<Integer, Integer>, Double> fullGraph = new HashMap<>();
					int i = 0;
					for(String word : query.words.keySet()) {
						int j = 0;
						for (String api : codeExample.codeElements.keySet()) {
							// cosine measure between word i and code j
							double w = searchImpl.calculateDistance(query.words.get(word), codeExample.codeElements.get(api));
							bipartiteMatcher.setWeight(i, j, w);
							Pair<Integer, Integer> edge = Pair.create(i, j); 
							fullGraph.put(edge, w);
							j ++;
						}
						i ++;
					}
					
					int[] wordAPIMatch = bipartiteMatcher.getMatching();
					// matching weight of query and code example
					double matchWeight = 0.0;
					int numbMatches = 0;
					for(i = 0; i < wordAPIMatch.length; i ++) {
						int j = wordAPIMatch[i];
						if(j != -1) {
							matchWeight += fullGraph.get(Pair.create(i, j));
							numbMatches ++;
						}
					}
					retMeasureWrtQuery.put(codeExample, matchWeight);
				}
				
				// Order by new retrieval score and get top K
				int k = 0;
				LinkedHashMap<RetrievedCodeExample, Double> sortedRetMeasureWrtQuery = RetrievalWithMatching.sortObjMap(retMeasureWrtQuery);
//				System.out.println(query.text + "\t" + sortedRetMeasureWrtQuery.keySet().iterator().next().example + "\t" + k);
				for(RetrievedCodeExample sortedEx : sortedRetMeasureWrtQuery.keySet()) {
					if(k++ < K && sortedEx == oracleQueryCodeEx.get(query)) {
						countAccurate ++;
					}
					if(k == K)
						break;
				}
			}
			System.out.printf("Retrieval top-%d accuracy: %f", K, countAccurate/(double) oracleQueryCodeEx.size());
	}
}
