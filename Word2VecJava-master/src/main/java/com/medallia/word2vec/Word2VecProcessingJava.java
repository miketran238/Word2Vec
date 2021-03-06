package com.medallia.word2vec;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.jujutsu.tsne.PrincipalComponentAnalysis;
import com.jujutsu.tsne.SimpleTSne;
import com.jujutsu.tsne.TSne;
import com.jujutsu.utils.MatrixUtils;
import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Searcher.Match;
import com.medallia.word2vec.Searcher.UnknownWordException;
import com.medallia.word2vec.Word2VecModel;
import com.medallia.word2vec.Word2VecTrainerBuilder.TrainingProgressListener;
import com.medallia.word2vec.neuralnetwork.NeuralNetworkType;
import com.medallia.word2vec.thrift.Word2VecModelThrift;
import com.medallia.word2vec.util.AutoLog;
import com.medallia.word2vec.util.Common;
import com.medallia.word2vec.util.Format;
import com.medallia.word2vec.util.ProfilingTimer;
import com.medallia.word2vec.util.Strings;
import com.medallia.word2vec.util.ThriftUtils;
import com.medallia.word2vec.util.FileUtils;

import org.apache.commons.logging.Log;
import org.apache.thrift.TException;
import org.math.plot.FrameView;
import org.math.plot.Plot2DPanel;
import org.math.plot.plots.ColoredScatterPlot;

import retrieval.RConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.swing.JFrame;

/** Example usages of {@link Word2VecModel} */
public class Word2VecProcessingJava {
	//	private static final Log LOG = AutoLog.getLog();

	/** Runs the example */
	public static void main(String[] args) throws IOException, TException, UnknownWordException, InterruptedException {
//				createModel();
		//		demoWordWithSim();

//				demoWordWithMath();
//		demoWordDisplay1();
		demoWordDisplay2();
//		retrieveAPISequence();
//		calculateSpearmanRHO();

	}

	/** 
	 * Trains a model and allows user to find similar words
	 * demo-word.sh example from the open source C implementation
	 */
	public static void createModel() throws IOException, TException, InterruptedException, UnknownWordException {
		File f = new File("GlobalConfig.JavaAPIcontentsPath");
		List<String> read = Common.readToList(f);
		List<List<String>> partitioned = Lists.transform(read, new Function<
				String, List<String>>() {
			@Override
			public List<String> apply(String input) {
				return Arrays.asList(input.split(" "));
			}
		});

		Word2VecModel model = Word2VecModel.trainer()
				.setMinVocabFrequency(5)
//				.useHierarchicalSoftmax()
				.useNumThreads(10)
				.setWindowSize(10)
				.type(NeuralNetworkType.CBOW)
				.setLayerSize(100)
				.useNegativeSamples(25)
				.setDownSamplingRate(1e-5)
				.setNumIterations(4)
				.setListener(new TrainingProgressListener() {
					@Override public void update(Stage stage, double progress) {
						System.out.println(String.format("%s is %.2f%% complete", Format.formatEnum(stage), progress * 100));
					}
				})
				.train(partitioned);

		// Writes model to a thrift file
		//		try (ProfilingTimer timer = ProfilingTimer.create(LOG, "Writing output to file")) 
		//		{
		//			FileUtils.writeStringToFile(new File("text8.model"), ThriftUtils.serializeJson(model.toThrift()));
		//		}

		// Alternatively, you can write the model to a bin file that's compatible with the C
		// implementation.
		try(final OutputStream os = Files.newOutputStream(Paths.get(""))) {
			model.toBinFile(os);
		}

//		interactWithMath(model.forSearch());
	}

	public static void demoWordWithSim() throws IOException, TException, InterruptedException, UnknownWordException {


		// Alternatively, you can write the model to a bin file that's compatible with the C
		// implementation.


		try {
			Word2VecModel model = Word2VecModel.fromBinFile(new File(""));
			interactWithSim(model.forSearch());
		}
		catch(Exception e){
			e.printStackTrace();
		}

	}

	public static void demoWordWithMath() throws IOException, TException, InterruptedException, UnknownWordException {
		// Alternatively, you can write the model to a bin file that's compatible with the C
		// implementation.
		try {
			Word2VecModel model = Word2VecModel.fromBinFile(new File(""));
			interactWithMath(model.forSearch());
		}
		catch(Exception e){
		}
	}


	public static void demoWordDisplay1() throws IOException, TException, InterruptedException, UnknownWordException {
		try {
			Word2VecModel model = Word2VecModel.fromBinFile(new File(""));
			writeLabelandVectors1(model);			
			pca_disp();
//			tsne_disp();
		}
		catch(Exception e){

		}
	}
	
	public static void demoWordDisplay2() throws IOException, TException, InterruptedException, UnknownWordException {

		try {
			Word2VecModel model = Word2VecModel.fromBinFile(new File("text_" + RConfig.outputExtension + ".bin"));
			writeLabelnValues(model);
			pca_disp();
//			tsne_disp();
		}
		catch(Exception e){

		}
	}
	
	public static String getMappedAPI(String API, Word2VecModel model) {
		try {
//			Word2VecModel model = Word2VecModel.fromBinFile(new File("text8.bin"));
			Searcher searcher = model.forSearch();
			
			List<Match> matches = searcher.getMatches(API, 100);
			for(Match match : matches) {
				String crossLibAPI = match.toString();
				if(crossLibAPI.contains("apache::")) {
					return crossLibAPI;
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static List<Match> getMappedAPIs(String API, Word2VecModel model) {
		try {
			Searcher searcher = model.forSearch();
			List<Match> matches = searcher.getMatches(API, 100);
			List<Match> apcMatches = new ArrayList<Searcher.Match>();
			
			int count = 0;
			for(Match match : matches) {
				String crossLibAPI = match.toString();
				if(crossLibAPI.contains("::") && count++ < 15) { //apache::, 10
					apcMatches.add(match);
				}
			}
			return apcMatches;
		} catch(Exception e){
//			e.printStackTrace();
		}
		
		return null;
	}
	
	public static void calculateSpearmanRHO() throws IOException, TException, InterruptedException, UnknownWordException {

		try {
			Word2VecModel model = Word2VecModel.fromBinFile(new File("text8.bin"));
			SearcherImpl searchImpl = new SearcherImpl(model);
			
			Path currentRelativePath = Paths.get("");
			String s = currentRelativePath.toAbsolutePath().toString();
			
			String [] pairTermAPIs = MatrixUtils.simpleReadLines(new File(s + "/data/survey/Survey_Eng-API.txt"));
			
			for(String pair : pairTermAPIs) {
				// This tube includes term-API-score
				String[] splitted = pair.split("\\s");
				String term = splitted[0];
				String fqnAPI = splitted[1];
				String[] splittedName = fqnAPI.split("\\.");
				int length = splittedName.length;
				String formattedAPI = splittedName[length-2] + "::" + splittedName[length-1]; 
//				Double score = Double.parseDouble(splitted[2]);
				
				
				// calculate cosine distance
				Double cosine = searchImpl.cosineDistance(term, formattedAPI);
//				Double euclide = searchImpl.euclideanDistance(term, formattedAPI);
//				System.out.println(term + "\t" + fqnAPI + "\t" + cosine);
				System.out.println(cosine);
			}
		}
		catch(Exception e){
			e.printStackTrace();

		}
	}

	private static void interactWithSim(Searcher searcher) throws IOException, UnknownWordException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
			while (true) {
				System.out.print("Enter word or sentence (EXIT to break): ");
				String word = br.readLine();
				if (word.equals("EXIT")) {
					break;
				}
				List<Match> matches = searcher.getMatches(word, 20);
				System.out.println(Strings.joinObjects("\n", matches));
			}
		}
	}

	private static void interactWithMath(Searcher searcher) throws IOException, UnknownWordException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
			while (true) {
				System.out.print("Enter 3 words (EXIT to break): ");
				String word = br.readLine();
				if (word.equals("EXIT")) {
					break;
				}
				String[] splits = word.split("\\s");
				if (splits.length<3)
					continue;
				String word11 = splits[0];
				String word12 = splits[1];
				String word21 = splits[2];
				try{
					List<Double> vector11 = searcher.getRawVector(word11);
					List<Double> vector12 = searcher.getRawVector(word12);
					List<Double> vector21 = searcher.getRawVector(word21);
					double[] simVal = calcSimVal(vector11, vector12, vector21);
					List <Match> matches= searcher.getMatches(simVal, 20);
					System.out.println(Strings.joinObjects("\n", matches));

				}
				catch(Exception e){

				}
			}
		}
	}

	public static double[] calcSimVal(List<Double> vector11, List<Double> vector12, List<Double> vector21 ){
		int size = vector11.size();
		double[] simVal = new double[size];
		for (int i=0; i<size; i++){
			simVal[i] =vector21.get(i) -( vector11.get(i)-vector12.get(i));
		}
		return simVal;
	}

	public static void writeLabelandVectors1(Word2VecModel model ){

		Iterable<String> words = model.getVocab();
		try{ 
			FileWriter labelFW = new FileWriter("dataLabel.txt");
			FileWriter valueFW = new FileWriter("dataValues.txt");

			for (String word:words){
				if (!(word.contains("java.util.Scanner") || word.contains("java.lang.StringBuffer")||word.contains("java.lang.StringBuilder"))){
					continue;
				}
				if (word.contains("#")){
					continue;
				};
				String shortWord = word; 

				if (word.contains("#") ){
					int idx = word.lastIndexOf(".");
					shortWord = word.substring(idx+1);
				}
				else if (word.contains(".")){
					int idx = word.lastIndexOf(".");
					String tword = word.substring(0,idx);
					int idx2 = tword.lastIndexOf(".");
					shortWord = word.substring(idx2+1);
				}

				labelFW.append(shortWord + System.lineSeparator());
				//				labelFW.flush();
				try {
					List<Double> rawVector = model.forSearch().getRawVector(word);
					//					Logger.log(rawVector);
					for (double value:rawVector)
						valueFW.append(value + " ");
					valueFW.append(System.lineSeparator());
					//					valueFW.flush();
				} catch (UnknownWordException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			valueFW.close();
			labelFW.close();

		}
		catch(Exception e){
		}
	}
	
	public static void writeLabelnValues(Word2VecModel model) {
		Iterable<String> words = model.getVocab();
		try{
			Path currentRelativePath = Paths.get("");
			String s = currentRelativePath.toAbsolutePath().toString();
			FileWriter labelFW = new FileWriter(s + "/data/test/dataLabel.txt");
			FileWriter valueFW = new FileWriter(s + "/data/test/dataValues.txt");
			
			String [] samples = MatrixUtils.simpleReadLines(new File(s + "/data/test/Code-Code_PCA_ASE.txt"));
			HashSet<String> hashedSamples = new HashSet<String>();
			for(String sample : samples) {
				hashedSamples.add(sample);
			}

			for (String word:words){
				if(!hashedSamples.contains(word))
					continue;

				labelFW.append(word + System.lineSeparator());
				try {
					List<Double> rawVector = model.forSearch().getRawVector(word);
					//					Logger.log(rawVector);
					for (double value:rawVector)
						valueFW.append(value + " ");
					valueFW.append(System.lineSeparator());
					//					valueFW.flush();
				} catch (UnknownWordException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			valueFW.close();
			labelFW.close();

		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	public static void writeLabelandVectors2(Word2VecModel model ){

		Iterable<String> words = model.getVocab();
		try{ 
			FileWriter labelFW = new FileWriter("dataLabel.txt");
			FileWriter valueFW = new FileWriter("dataValues.txt");

			for (String word:words){
				if (word.contains("#")){
					continue;
				}
				String tmp = word;
				if (word.contains(".")){
					int lastIdx = word.lastIndexOf(".");
					tmp = word.substring(0, lastIdx);
				}
//				Logger.log(word);

				if (!(tmp.equals("java.lang.StringBuffer")
						||tmp.equals("java.lang.StringBuilder"))){
					continue;
				}
				if (!(word.endsWith("new")||word.endsWith("append")
						||word.endsWith("charAt")||word.endsWith("toString")||word.endsWith("indexOf")||word.endsWith("insert")
						||word.endsWith("reverse"))){
					continue;
				}
//				Logger.log(word);

				
				String shortWord = word; 

				if (word.contains("#") ){
					int idx = word.lastIndexOf(".");
					shortWord = word.substring(idx+1);
				}
				else if (word.contains(".")){
					int idx = word.lastIndexOf(".");
					String tword = word.substring(0,idx);
					int idx2 = tword.lastIndexOf(".");
					shortWord = word.substring(idx2+1);
				}

				labelFW.append(shortWord + System.lineSeparator());
				//				labelFW.flush();
				try {
					List<Double> rawVector = model.forSearch().getRawVector(word);
					//					Logger.log(rawVector);
					for (double value:rawVector)
						valueFW.append(value + " ");
					valueFW.append(System.lineSeparator());
					//					valueFW.flush();
				} catch (UnknownWordException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			valueFW.close();
			labelFW.close();

		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void pca_disp() {
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		double [][] X = MatrixUtils.simpleRead2DMatrix(new File(s + "/data/test/dataValues.txt"));
		String [] labels = MatrixUtils.simpleReadLines(new File(s + "/data/test/dataLabel.txt"));
		String [] labelsType = new String[labels.length];

		for (int i = 0; i < labels.length; i++) {
			String tmp = labels[i];
			if (tmp.contains("#")){
				tmp = tmp.substring(0, tmp.lastIndexOf("#"));
			}
			else if (tmp.contains(".")){
				tmp = tmp.substring(0, tmp.lastIndexOf("."));
			}
			labelsType[i] = tmp;
		}
		PrincipalComponentAnalysis pca = new PrincipalComponentAnalysis();
		double [][] Y = pca.pca(X,2);
		for(int i = 0; i < Y.length; i ++) {
			System.out.println(Y[i][0] +"\t" + Y[i][1] + "\t" + labels[i].replace("::", "."));
//			for(int j = 0; j < Y[i].length; j ++) {
//			}
		}
		
		Plot2DPanel plot = new Plot2DPanel();

		ColoredScatterPlot setosaPlot = new ColoredScatterPlot("pca", Y, labelsType, labels);
		plot.plotCanvas.setNotable(true);
		plot.plotCanvas.setNoteCoords(true);
		plot.plotCanvas.addPlot(setosaPlot);

		FrameView plotframe = new FrameView(plot);
		plotframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		plotframe.setVisible(true);
	}

	public static void tsne_disp() {
		TSne tsne =  new SimpleTSne();

		double [][] X = MatrixUtils.simpleRead2DMatrix(new File("dataValues.txt"));
		int initial_dims = X[0].length;
		double perplexity = 7.0;

		String [] labels = MatrixUtils.simpleReadLines(new File("dataLabel.txt"));
		String [] labelsType = new String[labels.length];

		for (int i = 0; i < labels.length; i++) {
			String tmp = labels[i];
			if (tmp.contains("#")){
				tmp = tmp.substring(0, tmp.lastIndexOf("#"));
			}
			else if (tmp.contains(".")){
				tmp = tmp.substring(0, tmp.lastIndexOf("."));
			}
			labelsType[i] = tmp;
		}

		System.out.println("Shape is: " + X.length + " x " + X[0].length);
		System.out.println("Starting TSNE: " + new Date());
		double [][] Y = tsne.tsne(X, 2, initial_dims, perplexity);
		System.out.println("Finished TSNE: " + new Date());
		//System.out.println("Result is = " + Y.length + " x " + Y[0].length + " => \n" + MatrixOps.doubleArrayToString(Y));
		System.out.println("Result is = " + Y.length + " x " + Y[0].length);
		//	        saveFile(new File("Java-tsne-result.txt"), MatrixOps.doubleArrayToString(Y));
		Plot2DPanel plot = new Plot2DPanel();

		ColoredScatterPlot setosaPlot = new ColoredScatterPlot("tsne", Y, labelsType, labels);
		plot.plotCanvas.setNotable(true);
		plot.plotCanvas.setNoteCoords(true);
		plot.plotCanvas.addPlot(setosaPlot);

		FrameView plotframe = new FrameView(plot);
		plotframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		plotframe.setVisible(true);
	}
	
	public static void retrieveAPISequence() {
		/* Read database of corresponding text and code sequences and store a mapping data (String, String) */
		LinkedHashMap<String, String> oracleData = new LinkedHashMap<>();

		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		@SuppressWarnings("unchecked")
		HashSet<Integer> skipLines = (HashSet<Integer>) FileUtils.readObjectFile(s + "/data/retrieval/KJ_API2VECTop5.dat");
		
		try {
//			Scanner textFR = new Scanner(new File(s+ "/data/survey/52_En.txt"));
//			Scanner apiFR = new Scanner(new File(s+ "/data/survey/52_Jv.txt"));
			
			Scanner textFR = new Scanner(new File(s+ "/data/retrieval/KodeJava_437.en"));
			Scanner apiFR = new Scanner(new File(s+ "/data/retrieval/KodeJava_437.cod"));
			
			int lineCount = 0, limit = 500;
			
			while(textFR.hasNextLine() && apiFR.hasNextLine()) {
				lineCount ++;
				String text = textFR.nextLine();
				String api = apiFR.nextLine();

				if(!skipLines.contains(lineCount))
					continue;

				oracleData.put(text, api);
			}
			
			textFR.close();
			apiFR.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
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

		// Store each sequence and its vector value in a HashMap for search
		HashMap<String, double[]> wordVectors = new HashMap<>();
		HashMap<String, double[]> cltVectors = new HashMap<>();
		
		for(String text : oracleData.keySet()) {
			String apiSeq = oracleData.get(text);
			
			// get averaging vector
			String[] textTokens = text.split("\\s");
			String[] CLTs = apiSeq.split("\\s");
			
			double[] wordVec = searchImpl.getAverageVector(textTokens);
			double[] cltVec = searchImpl.getAverageVector(CLTs);
			
			if(wordVec == null || cltVec == null)
				continue;
			
			wordVectors.put(text, wordVec);
			cltVectors.put(apiSeq, cltVec);
		}
		
		// Again, each text sequence, find a top K (=10) code sequence with smallest distance
		LinkedHashMap<String, LinkedHashMap<String, Double>> searchResults = new LinkedHashMap<>();
		for(String text : wordVectors.keySet()) {
			double[] wordVec = wordVectors.get(text);
			LinkedHashMap<String, Double> corrDistances = new LinkedHashMap<>();
			for(String apiSeq : cltVectors.keySet()) {
				double[] cltVec = cltVectors.get(apiSeq);
				double distance = searchImpl.calculateDistance(wordVec, cltVec);
				corrDistances.put(apiSeq, distance);
			}
			
			LinkedHashMap<String, Double> sortedMap = sortMap(corrDistances);
			searchResults.put(text, sortedMap);
		}
		
		// Check whether for each English text its expected code sequence belongs to its top list
		int K = 1;
		
		int count = 0;
		for(String text : wordVectors.keySet()) {
			LinkedHashMap<String, Double> rankedAPISeqList = searchResults.get(text);
			int k = 0;
			for(String apiSeq : rankedAPISeqList.keySet()) {
				if(++k <= K && oracleData.get(text).equals(apiSeq)) {
//					System.out.println(text + "\t" + apiSeq + "\t" + k);
					++ count;
					break;
				}
				else if (oracleData.get(text).equals(apiSeq)) {
//					System.out.println(text + "\t" + apiSeq + "\t" + k);
					break;
				}
			}
		}
		System.out.printf("Retrieval top-%d accuracy: %f", K, count/(double) oracleData.size());
		
		// Calculate MRR and thing like that
	}
	
	public static LinkedHashMap<String, Double> sortMap(LinkedHashMap<String, Double> unsortedMap) {
		List<Entry<String, Double>> list = new LinkedList<>(unsortedMap.entrySet());
		
		Collections.sort(list, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
				return o2.getValue().compareTo(o1.getValue()); // descending order
			}
		});
		
		LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
		for(Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		
		return sortedMap;
	}
	
	public static LinkedHashMap<Object, Double> sortObjMap(LinkedHashMap<Object, Double> unsortedMap) {
		List<Entry<Object, Double>> list = new LinkedList<>(unsortedMap.entrySet());
		
		Collections.sort(list, new Comparator<Entry<Object, Double>>() {
			public int compare(Entry<Object, Double> o1, Entry<Object, Double> o2) {
				return o2.getValue().compareTo(o1.getValue()); // descending order
			}
		});
		
		LinkedHashMap<Object, Double> sortedMap = new LinkedHashMap<>();
		for(Entry<Object, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		
		return sortedMap;
	}
}
