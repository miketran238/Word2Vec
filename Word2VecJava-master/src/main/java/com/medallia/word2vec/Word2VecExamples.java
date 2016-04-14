package com.medallia.word2vec;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.medallia.word2vec.Searcher.Match;
import com.medallia.word2vec.Searcher.UnknownWordException;
import com.medallia.word2vec.Word2VecTrainerBuilder.TrainingProgressListener;
import com.medallia.word2vec.neuralnetwork.NeuralNetworkType;
import com.medallia.word2vec.thrift.Word2VecModelThrift;
import com.medallia.word2vec.util.AutoLog;
import com.medallia.word2vec.util.Common;
import com.medallia.word2vec.util.Format;
import com.medallia.word2vec.util.ProfilingTimer;
import com.medallia.word2vec.util.Strings;
import com.medallia.word2vec.util.ThriftUtils;

import org.apache.commons.logging.Log;
import org.apache.thrift.TException;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/** Example usages of {@link Word2VecModel} */
public class Word2VecExamples {
	private static final Log LOG = AutoLog.getLog();
	
	/** Runs the example */
	public static void main(String[] args) throws IOException, TException, UnknownWordException, InterruptedException {
//		demoWord();
		loadModel();
	}
	
	/** 
	 * Trains a model and allows user to find similar words
	 * demo-word.sh example from the open source C implementation
	 */
	public static void demoWord() throws IOException, TException, InterruptedException, UnknownWordException {
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		File f = new File(s + "/data/train/java-apache.l");
		if (!f.exists())
	       	       throw new IllegalStateException("Please download and unzip the text8 example from http://mattmahoney.net/dc/text8.zip");
		List<String> read = Common.readToList(f);
		List<List<String>> partitioned = Lists.transform(read, new Function<String, List<String>>() {
			@Override
			public List<String> apply(String input) {
				return Arrays.asList(input.split(" "));
			}
		});
		
		Word2VecModel model = Word2VecModel.trainer()
				.setMinVocabFrequency(1)
				.useNumThreads(20)
				.setWindowSize(7)
				.type(NeuralNetworkType.TopicCBOW)
				.setLayerSize(100)
				.useNegativeSamples(25)
				.setDownSamplingRate(1e-4)
				.setNumIterations(5)
				.setListener(new TrainingProgressListener() {
					@Override public void update(Stage stage, double progress) {
						System.out.println(String.format("%s is %.2f%% complete", Format.formatEnum(stage), progress * 100));
					}
				})
				.train(partitioned);

		// Writes model to a thrift file
		try (ProfilingTimer timer = ProfilingTimer.create(LOG, "Writing output to file")) {
			FileUtils.writeStringToFile(new File("text8.model"), ThriftUtils.serializeJson(model.toThrift()));
		}

		// Alternatively, you can write the model to a bin file that's compatible with the C
		// implementation.
		try(final OutputStream os = Files.newOutputStream(Paths.get("text8.bin"))) {
			model.toBinFile(os);
		}
		
		interact(model.forSearch(), new SearcherImpl(model));
	}
	
	/** Loads a model and allows user to find similar words */
	public static void loadModel() throws IOException, TException, UnknownWordException {
		final Word2VecModel model;
		try (ProfilingTimer timer = ProfilingTimer.create(LOG, "Loading model")) {
			String json = Common.readFileToString(new File("text8.model"));
			model = Word2VecModel.fromThrift(ThriftUtils.deserializeJson(new Word2VecModelThrift(), json));
		}
		interact(model.forSearch(), new SearcherImpl(model));
	}
	
	/** Example using Skip-Gram model */
	public static void skipGram() throws IOException, TException, InterruptedException, UnknownWordException {
		List<String> read = Common.readToList(new File("sents.cleaned.word2vec.txt"));
		List<List<String>> partitioned = Lists.transform(read, new Function<String, List<String>>() {
			@Override
			public List<String> apply(String input) {
				return Arrays.asList(input.split(" "));
			}
		});
		
		Word2VecModel model = Word2VecModel.trainer()
				.setMinVocabFrequency(100)
				.useNumThreads(20)
				.setWindowSize(7)
				.type(NeuralNetworkType.CBOW)
				.useHierarchicalSoftmax()
				.setLayerSize(300)
				.useNegativeSamples(0)
				.setDownSamplingRate(1e-3)
				.setNumIterations(5)
				.setListener(new TrainingProgressListener() {
					@Override public void update(Stage stage, double progress) {
						System.out.println(String.format("%s is %.2f%% complete", Format.formatEnum(stage), progress * 100));
					}
				})
				.train(partitioned);
		
		try (ProfilingTimer timer = ProfilingTimer.create(LOG, "Writing output to file")) {
			FileUtils.writeStringToFile(new File("300layer.20threads.5iter.model"), ThriftUtils.serializeJson(model.toThrift()));
		}
		
		interact(model.forSearch(), new SearcherImpl(model));
	}
	
	private static void interact(Searcher searcher, SearcherImpl searchImpl) throws IOException, UnknownWordException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
			while (true) {
				System.out.print("Enter word or sentence (EXIT to break): ");
				String word = br.readLine();
				
				if (word.equals("EXIT")) {
					break;
				}
				String[] numbInputs = word.split("\\s");
				
				if(numbInputs.length == 2) {
					List<Match> matches = searcher.getMatches(word, 300);
//					for (Match match : matches) {
////						if(match.match().contains("::"))
//						if(match.match().contains("CS::"))
//							System.out.println(match.match());
//					}
					System.out.println(Strings.joinObjects("\n", matches));
				}
				else if(numbInputs.length == 3) {
					double[] diff = searchImpl.getVectorFrom3Words(numbInputs[0], numbInputs[1], numbInputs[2]);
					if(diff == null)
						break;
					List<Match> matches = searcher.getMatches(diff, 20);
					for (Match match : matches) {
						if(match.match().contains("::"))
							System.out.println(match.match());
					}
				}
				else {
					double[] average = searchImpl.getAverageVector(numbInputs);
					List<Match> matches = searcher.getMatches(average, 100);
					for (Match match : matches) {
						if(word.contains("apache::")) { // Apache
							if(match.match().contains("jdk::"))
								System.out.println(match);
						}
						if(word.contains("jdk::")) { // Java 
							if(match.match().contains("apache::"))
								System.out.println(match);
						}
						else { //word
//							if(match.match().contains("::"))
								System.out.println(match);
						}
					}
				}
			}
		}
	}
}
