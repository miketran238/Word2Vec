package retrieval;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import com.medallia.word2vec.SearcherImpl;
import com.medallia.word2vec.Word2VecModel;
import com.medallia.word2vec.Word2VecProcessingJava;
import com.medallia.word2vec.util.FileUtils;

public class CombinedrVSMandAPI2VEC {
	
	public static void main(String[] args) {
		CombinedrVSMandAPI2VEC combiner = new CombinedrVSMandAPI2VEC();
		combiner.retrieve();
	}

	@SuppressWarnings("unchecked")
	public void retrieve() {
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
		
		HashSet<Integer> API2VEC_Top1 = (HashSet<Integer>) FileUtils.readObjectFile(s + "/data/retrieval/KJ_API2VECTop1NonOverlap.dat"); //KJ_API2VECTop1Retrieved,  KodeJava_topKOver5, rVSM_260
		
		LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> rVSMRankedData = 
				(LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>) FileUtils.readObjectFile(s + "/data/retrieval/rVSM_allResult.dat");
		
		LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> dataJaccardDistance = (LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>) 
				FileUtils.readObjectFile(s + "/data/retrieval/Jaccard_439x439.dat");
		
		HashSet<Integer> outOfVocabExamples = new HashSet<Integer>();
		
		try {
			Scanner textFR = new Scanner(new File(s+ "/data/retrieval/KodeJava_439.en"));
			Scanner apiFR = new Scanner(new File(s+ "/data/retrieval/KodeJava_439.cod"));
			
			
			int lineCount = 0, limit = 50;
			while(textFR.hasNextLine() && apiFR.hasNextLine()) {
				lineCount ++;
				String text = textFR.nextLine();
				String code = apiFR.nextLine();
				
				boolean outOfVocab = false;
				
//					if(!skipLines.contains(lineCount))
//						continue;
//					if(lineCount > limit)
//						continue;
//				if(dataJaccardDistance.get(lineCount) > 0.05)
//					continue;
				
				// Query information
				Query query = new Query();
				query.queryId = lineCount;
				query.text = text;
				
				String[] textTokens = text.split("\\s");
				query.avgVector = searchImpl.getAverageVector(textTokens);
				if(query.avgVector == null) {
					outOfVocab = true;
					outOfVocabExamples.add(lineCount);
					continue;
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
					else {
						System.out.println(API);
						outOfVocab = true;
					}
				}
//				if( codeEx.codeElements.size() != 0) // !outOfVocab
					oracleQueryCodeEx.put(query, codeEx);
//				else
//					outOfVocabExamples.add(lineCount);
			}
			
//			FileUtils.writeObjectFile(outOfVocabExamples, s + "/data/retrieval/outOfVocabExamples.dat");
			
			textFR.close();
			apiFR.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		/* Remove out-of-vocabulary examples from the results of rVSM, Jaccard */
//		outOfVocabExamples.clear();
		
		// Retrieval part, setup top K
		int KThreshold = 1, K = 0; // counter K
		HashSet<Integer> API2VECTop1Retrieved = new HashSet<>();
		System.out.printf("Retrieval top-K accuracy for %d examples:\n", oracleQueryCodeEx.size());
		
		while (K++ < KThreshold) { // start from top 1 to 5
			// count number of example get accurate in top K
			int countAccurate = 0;
			
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
//						if (candCodeExp.count == J)
//							retMeasureWrtQuery.put(candCodeExp, candCodeExp.score / (double) J);
					}
				}
				
				for(RetrievedCodeExample candCodeExp : oracleQueryCodeEx.values()) {
					if(candCodeExp.count != 0)
						retMeasureWrtQuery.put(candCodeExp, candCodeExp.score / J); //candCodeExp.count
				}
				
				/* Remove query whose code elements are out of vocabulary of API2VEC */
				
				if(RConfig.isScoring1) {
					for(RetrievedCodeExample candCodeExp : oracleQueryCodeEx.values()) {
						double totalScore = 0.0;
						for(String API : candCodeExp.codeElements.keySet()) {
							totalScore += cosineScoreToSingleAPI.get(API);
						}
						if(candCodeExp.codeElements.size() != 0)
							retMeasureWrtQuery.put(candCodeExp, totalScore / (double) candCodeExp.codeElements.size());
					}
				}
				
				
				/* Get score from rVSM */
				int queryLineIdx = query.queryId;
				LinkedHashMap<Integer, Double> rVSMScoredExms = rVSMRankedData.get(queryLineIdx);
				
				/* Normalize Jaccard distance into probability for a mixture linear model */
				LinkedHashMap<Integer, Double> jaccardDistanceWrtQuery = dataJaccardDistance.get(queryLineIdx);
				double normJaccard = 0.0;
				Iterator<Map.Entry<Integer, Double>> jaccardEntries = jaccardDistanceWrtQuery.entrySet().iterator();
				while(jaccardEntries.hasNext()) {
					Map.Entry<Integer, Double> entry = jaccardEntries.next();
					if(outOfVocabExamples.contains(entry.getKey())) {
						jaccardEntries.remove();
						continue;
					}
					Double jaccard = entry.getValue();
					if(jaccard > normJaccard)
						normJaccard = jaccard;
				}
				jaccardEntries = jaccardDistanceWrtQuery.entrySet().iterator();
				while(jaccardEntries.hasNext()) {
					Map.Entry<Integer, Double> entry = jaccardEntries.next();
					entry.setValue(entry.getValue()/normJaccard);
				}
				
				/* Normalize rVSM score to probability for a mixture linear model*/
				Iterator<Map.Entry<Integer, Double>> rVSMSentries = rVSMScoredExms.entrySet().iterator();
				double rVSMNormalProb = 0.0;
				while(rVSMSentries.hasNext()) {
					Map.Entry<Integer, Double> entry = rVSMSentries.next();
					if(outOfVocabExamples.contains(entry.getKey())) {
						rVSMSentries.remove();
						continue;
					}
					double rVSMScore = Math.exp(entry.getValue());
//					entry.setValue(rVSMScore);
					if(rVSMNormalProb < rVSMScore)
					rVSMNormalProb =rVSMScore; 
				}
				
				rVSMSentries = rVSMScoredExms.entrySet().iterator();
				while(rVSMSentries.hasNext()) {
					Map.Entry<Integer, Double> entry = rVSMSentries.next();
					Double rVSMProb = entry.getValue();
					rVSMProb /= rVSMNormalProb;
					entry.setValue(rVSMProb);
				}

				/* Convert cosine score to normalized probability */
				Iterator<Map.Entry<RetrievedCodeExample, Double>> cosSEntries = retMeasureWrtQuery.entrySet().iterator();
				double cosNormalProb = 0.0;
				while(cosSEntries.hasNext()) {
					Map.Entry<RetrievedCodeExample, Double> entry = cosSEntries.next();
					double cosScore = Math.exp(entry.getValue());
//					entry.setValue(cosScore);
					if(cosNormalProb < cosScore)
						cosNormalProb =cosScore; 
				}
				
				cosSEntries = retMeasureWrtQuery.entrySet().iterator();
				while(cosSEntries.hasNext()) {
					Map.Entry<RetrievedCodeExample, Double> entry = cosSEntries.next();
					Double a2vProb = entry.getValue();
					a2vProb /= cosNormalProb;
					entry.setValue(a2vProb);
				}
				
				/* Mixture part with linear model */
				cosSEntries = retMeasureWrtQuery.entrySet().iterator();
				while(cosSEntries.hasNext()) {
					Map.Entry<RetrievedCodeExample, Double> entry = cosSEntries.next();
					RetrievedCodeExample candCodeEx = entry.getKey();
					
					/* Make sure that data received from Hung contains all examples indexed by line #*/
					assert rVSMScoredExms.containsKey(candCodeEx.exId);
						
					double jaccard = jaccardDistanceWrtQuery.get(queryLineIdx);
					if(jaccard < 0.65) // 0.65 may be optimal
						RConfig.alpha = 0.0; // 0.35 may be optimal (wrt top-K accuracy)
					else // if(jaccard > 0.65)
						RConfig.alpha = 1.0;
					double combinedScore = RConfig.alpha * rVSMScoredExms.get(candCodeEx.exId) + 
							(1-RConfig.alpha) * candCodeEx.score;
					candCodeEx.score = combinedScore;
					entry.setValue(combinedScore); //Math.log(combinedScore));
				}
				
				// Order by new retrieval score and get top K
				int k = 0;
				
				LinkedHashMap<RetrievedCodeExample, Double> sortedRetMeasureWrtQuery = RetrievalWithMatching.sortObjMap(retMeasureWrtQuery);
				
				
				for(RetrievedCodeExample sortedEx : sortedRetMeasureWrtQuery.keySet()) {
					if(k++ < K && sortedEx == oracleQueryCodeEx.get(query)) {
						countAccurate ++;
						API2VECTop1Retrieved.add(queryLineIdx);
						if(K == 1)
							System.out.println(query.text + "\t" + sortedEx.example + "\t" + k);
						if(query.queryId == 14)
							System.out.println(query.text + "\t" + sortedEx.example + "\t" + k);
						break;
					}
					if(sortedEx == oracleQueryCodeEx.get(query)) {
//								System.out.println(query.text + "\t" + sortedEx.example + "\t" + k);
						break;
					}
				}
			}
			
			System.out.printf("%f", countAccurate/(double) oracleQueryCodeEx.size());
			System.out.println();
		}
//		FileUtils.writeObjectFile(API2VECTop1Retrieved, s + "/data/retrieval/KJ_API2VECTop2Retrieved.dat");
		
		int nonOverlap = 0;
		HashSet<Integer> nonOverlapTop1 = new HashSet<Integer>();
		for(Integer API2VECRe : API2VEC_Top1) {
			if(API2VECTop1Retrieved.contains(API2VECRe)) {
				System.out.println(API2VECRe);
				nonOverlap ++;
				nonOverlapTop1.add(API2VECRe);
			}
		}
//		FileUtils.writeObjectFile(nonOverlapTop1, s + "/data/retrieval/KJ_API2VECTop1NonOverlap.dat");
		System.out.printf("Number of queries retreived correctly on top 1 and non-overlaped: %d\t%d\n", API2VECTop1Retrieved.size(), nonOverlap );
	}
}
