package edu.iastate.ece.api2vec;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;

import com.jujutsu.utils.MatrixUtils;
import com.medallia.word2vec.Searcher.Match;
import com.medallia.word2vec.Word2VecModel;
import com.medallia.word2vec.Word2VecProcessingJava;
import com.medallia.word2vec.util.FileUtils;
import com.medallia.word2vec.util.Strings;

public class APIMappings {
	
	public static void main(String[] args) {
		APIMappings mapper = new APIMappings();
		
//		APIMappings.subsample(100);
		
		mapper.doJDKApacheMapping(100);
	}
	
	public void doJDKApacheMapping(int numbSamples) {
		LinkedHashMap<String, Integer> APIListWithHighestFreq = getJDKAPIVocabulary();
		
		Word2VecModel model = null;
		try {
			model = Word2VecModel.fromBinFile(new File("text8_annot.bin"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		int smplCount = 0;
		for(String API : APIListWithHighestFreq.keySet()) {
			if(smplCount ++ < numbSamples) {
				List<Match> matches = Word2VecProcessingJava.getMappedAPIs(API, model);
				if(matches != null)
					System.out.println(API + "\n" +Strings.joinObjects("\n", matches));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void subsample(int numbSamples) {
		
		LinkedHashMap<String, Integer> APIOccurrences = (LinkedHashMap<String, Integer>)
				FileUtils.readObjectFile("JDKAPIOccurrences.dat");
		
		int totalAPIOccurrences = 0;
		for(Integer frequency : APIOccurrences.values()) {
			totalAPIOccurrences += frequency;
		}
		System.out.println("Total number of API occurrences: " + totalAPIOccurrences);
		
		LinkedHashMap<String, Double> APIFrequencies = new LinkedHashMap<>();
		for(String API : APIOccurrences.keySet()) {
			APIFrequencies.put(API, APIOccurrences.get(API) / (double) totalAPIOccurrences);
		}
		
		LinkedHashMap<String, Double> sortedAPIFrequencies = Word2VecProcessingJava.sortMap(APIFrequencies);
		
		int sampleCount = 0;

		long nextRandom = totalAPIOccurrences;
		
		double downSampleRate = 1e-2;
		
		Word2VecModel model = null;
		try {
			model = Word2VecModel.fromBinFile(new File("text8.bin"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		for(String API : sortedAPIFrequencies.keySet()) {
			if(++sampleCount < numbSamples) {
				String mappedAPI = Word2VecProcessingJava.getMappedAPI(API, model);
				if(mappedAPI != null) {
					System.out.println(API + "\t" + mappedAPI);
				}
			}
		}
		
		
		for(String API : APIOccurrences.keySet()) {
			int count = APIOccurrences.get(API);
			double random = (Math.sqrt(count / (downSampleRate * totalAPIOccurrences)) + 1)
					* (downSampleRate * totalAPIOccurrences) / count;
			
			nextRandom = incrementRandom(nextRandom);
			if (random > (nextRandom & 0xFFFF) / (double)65_536) {
				++ sampleCount;
//				System.out.println(API + "\t frequency: " + count);
				String mappedAPI = Word2VecProcessingJava.getMappedAPI(API, model);
				if(mappedAPI != null) {
					System.out.println(API + "\t" + mappedAPI);
				}
			}
			
			if(sampleCount > numbSamples)
				return;
		}
		
	}
	
	public LinkedHashMap<String, Integer> getJDKAPIVocabulary() {
		String [] apiListWithFreq = MatrixUtils.simpleReadLines(new File("JDKAPIs.txt"));
		
		LinkedHashMap<String, Integer> APIListWithHighestFreq = new LinkedHashMap<String, Integer>();
		
		for(String record : apiListWithFreq) {
			String [] fields = record.split("\t");
			if(fields.length == 3) {
				String type = fields[0];
				String fQualifiedName = fields[1];
				Integer freq = Integer.parseInt(fields[2]);
				
				if(/*type.equals("method") && */!fQualifiedName.contains("new")) {
					String[] names = fQualifiedName.split("\\.");
					int length = names.length;
					String fmtAPI = "jdk::" + names[length-2] + "::" + names[length-1];
					APIListWithHighestFreq.put(fmtAPI, freq);
				}
			}
		}
		
		return APIListWithHighestFreq;
	}
	
	public static void mapAPIs() {
	}
	
	/** @return Next random value to use */
	static long incrementRandom(long r) {
		return r * 25_214_903_917L + 11;
	}
	

}
