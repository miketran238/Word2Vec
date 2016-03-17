package edu.iastate.ece.api2vec;

import java.io.File;
import java.util.LinkedHashMap;

import com.medallia.word2vec.Word2VecModel;
import com.medallia.word2vec.Word2VecProcessingJava;
import com.medallia.word2vec.util.FileUtils;

public class APIMappings {
	
	public static void main(String[] args) {
		APIMappings.subsample(100);
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
	
	public static void mapAPIs() {
	}
	
	/** @return Next random value to use */
	static long incrementRandom(long r) {
		return r * 25_214_903_917L + 11;
	}
	

}
