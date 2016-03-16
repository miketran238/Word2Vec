package edu.iastate.ece.api2vec;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.jujutsu.utils.MatrixUtils;

public class RandomWordPairs {
	
	private static Hashtable<String, Double> stopwordList = new Hashtable<>();
	static {
		Double dummny = new Double(0);
		stopwordList.put("a", dummny);
		stopwordList.put("the", dummny);
		stopwordList.put("was", dummny);
		stopwordList.put("with", dummny);
		stopwordList.put("to", dummny);
		stopwordList.put("by", dummny);
		stopwordList.put("from", dummny);
		stopwordList.put("for", dummny);
		stopwordList.put("and", dummny);
		stopwordList.put("when", dummny);
		stopwordList.put("on", dummny);
		stopwordList.put("of", dummny);
		stopwordList.put("get", dummny);
		stopwordList.put("set", dummny);
		stopwordList.put("class", dummny);
		stopwordList.put("object", dummny);
	}
	
	public static void main(String[] args) {
		RandomWordPairs randomizer = new RandomWordPairs();
		randomizer.readAnyCodeExamples();
		
		long nextRandom = 2;
		int window = 52; // number of possible pairs
		for(int i = 0; i < 52; i ++) {
			nextRandom = incrementRandom(nextRandom);
			int b = (int)((nextRandom % window) + window) % window;
			System.out.println(b);
		}
	}
	
	public void createUnitPairs() {
		/* Read database of corresponding text and code sequences and store a mapping data (String, String) */
		List<String> wordAPIPairSouce = new ArrayList<>();
		try {
			Scanner textFR = new Scanner(new File("52_trunc_En.txt"));
			Scanner apiFR = new Scanner(new File("52_trunc_fqnAPI.txt"));
			
			while(textFR.hasNextLine() && apiFR.hasNextLine()) {
				String text = textFR.nextLine();
				HashSet<String> hashedWordList = new HashSet<>();
				for(String word : text.split("\\s")) {
					if(stopwordList.containsKey(word))
						continue;
					hashedWordList.add(word);
				}
				
				String APIs = apiFR.nextLine();
				HashSet<String> hashedAPIList = new HashSet<>();
				
				for(String API : APIs.split("\\s")) {
					hashedAPIList.add(API);
				}
				
				for(String word : hashedWordList) {
					for(String API : hashedAPIList) {
						String wordAPI = word + "\t" + API;
						wordAPIPairSouce.add(wordAPI);
					}
				}
			}
			
			textFR.close();
			apiFR.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		System.out.println("Number of (duplicate) word-API pairs " + wordAPIPairSouce.size());
		generateRandomPairs(wordAPIPairSouce, 100);
	}
	
	
	public void readAnyCodeExamples() {
		/* English queries */
		String [] englishText = MatrixUtils.simpleReadLines(new File("52_trunc_En.txt"));
		List<String> wordList = new ArrayList<>();
		HashSet<String> hashedWordList = new HashSet<>();
		for(String sentence : englishText) {
			String[] words = sentence.split("\\s");
			for(String word : words) {
				if(stopwordList.containsKey(word))
					continue;
				wordList.add(word);
				hashedWordList.add(word);
			}
		}
		System.out.println("Number of (duplicate) words " + wordList.size() + "/" + hashedWordList.size());
		
		/* API sequence*/
		String [] APISequences = MatrixUtils.simpleReadLines(new File("52_trunc_fqnAPI.txt"));
		List<String> APIList = new ArrayList<>();
		HashSet<String> hashedAPIList = new HashSet<>();
		for(String sequence : APISequences) {
			String[] APIs = sequence.split("\\s");
			for(String API : APIs) {
				APIList.add(API);
				hashedAPIList.add(API);
			}
		}
		System.out.println("Number of (duplicate) API elements " + APIList.size()+ "/" + hashedAPIList.size());
		
//		for(String code : hashedAPIList) {
//			System.out.println(code.replace("::", "."));
//		}
		
		/* Generate random pairs of words and API and APIs*/
//		generateRandomPairs(wordList, APIList, 100);
		generatePseudoRandomPairs(wordList, APIList, 100);
	}
	
	public void generateRandomPairs(List<String> pairList, int size) {
		Random pairRandonmizer = new Random();
		Hashtable<Integer, Double> usedPairs = new Hashtable<>();
		for(int i = 0; i < size; i ++) {
			int randIdx = 0;
			while (true) {
				randIdx = pairRandonmizer.nextInt(pairList.size());
				if(!usedPairs.containsKey(randIdx)) {
					usedPairs.put(randIdx, new Double(0.0));
					break;
				}
			}
			String pair = pairList.get(randIdx);
			System.out.println(pair);
		}
	}
	
	public void generateRandomPairs(List<String> wordList, List<String> APIList, int size) {
		Random wordRandonmizer = new Random();
		Random apiRandonmizer = new Random();
		Hashtable<Integer, Double> usedPairs = new Hashtable<>();
		for(int i = 0; i < size; i ++) {
			int randIdx1 = -1;
			int randIdx2 = -1;
			while (true) {
				randIdx1 = wordRandonmizer.nextInt(wordList.size());
				randIdx2 = apiRandonmizer.nextInt(APIList.size());
				if(!usedPairs.containsKey(randIdx1 + 1000 * randIdx2)) { // occurs if and only 2 two numbers have been already generated
					break;
				}
			}
			String word = wordList.get(wordRandonmizer.nextInt(wordList.size()));
			String API = APIList.get(apiRandonmizer.nextInt(APIList.size()));
			System.out.println(word + "\t" + API);
		}
	}
	
	public void generatePseudoRandomPairs(List<String> wordList, List<String> APIList, int size) {
		Hashtable<Integer, Double> usedPairs = new Hashtable<>();
		long randIdx1 = 0;
		long randIdx2 = 0;
		for(int i = 0; i < size; i ++) {
			int rd1 = (new Random()).nextInt(1000);
			int rd2 = (new Random()).nextInt(1000);
			while (true) {
				randIdx1 = incrementRandom(randIdx1);
				randIdx2 = incrementRandom(randIdx2);
				
				int window1 = wordList.size();
				rd1 = (int)((randIdx1 % window1) + window1) % window1;
				
				int window2 = APIList.size();
				rd2 = (int)((randIdx1 % window2) + window2) % window2;
				
				if(!usedPairs.containsKey(rd1 + 1000 * rd2)) { // occurs if and only 2 two numbers have been already generated
					break;
				}
			}
			String word = wordList.get(rd1);
			String API = APIList.get(rd2);
			usedPairs.put(rd1 + 1000 * rd2, 0.0);
			System.out.println(word + "\t" + API);
		}
	}
	
	static long incrementRandom(long r) {
		return r * 25_214_903_917L + 11;
	}

}
