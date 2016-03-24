package com.medallia.word2vec.neuralnetwork;

import com.google.common.collect.Multiset;
import com.medallia.word2vec.Word2VecTrainerBuilder.TrainingProgressListener;
import com.medallia.word2vec.huffman.HuffmanCoding.HuffmanNode;

import java.util.List;
import java.util.Map;

/**
 * Trainer for neural network using continuous bag of words
 * The main difference of this model, this includes an extra token at the beginning of every
 * context window. This token is considered as a topic of the whole sentence. This topic
 * contributes in the model as context to predict centered word
 */
class TopicCBOWModelTrainer extends NeuralNetworkTrainer {
	
	TopicCBOWModelTrainer(NeuralNetworkConfig config, Multiset<String> counts, Map<String, HuffmanNode> huffmanNodes, TrainingProgressListener listener) {
		super(config, counts, huffmanNodes, listener);
	}
	
	/** {@link Worker} for {@link TopicCBOWModelTrainer} */
	private class CBOWWorker extends Worker {
		private CBOWWorker(int randomSeed, int iter, Iterable<List<String>> batch) {
			super(randomSeed, iter, batch);
		}
		
		@Override void trainSentence(List<String> sentence) {
			int sentenceLength = sentence.size();
			
			if(sentenceLength == 0)
				return;
			
			/* First token as topic */
			String topicToken = sentence.get(0);
			int topicIdx = huffmanNodes.get(topicToken).idx;
			
			/** Should guarantee that the first token of any sentences is always an API considered as topic */
			for (int sentencePosition = 0; sentencePosition < sentenceLength; sentencePosition++) {
				String word = sentence.get(sentencePosition);
				HuffmanNode huffmanNode = huffmanNodes.get(word);

				for (int c = 0; c < layer1_size; c++)
					neu1[c] = 0;
				for (int c = 0; c < layer1_size; c++)
					neu1e[c] = 0;
				
				nextRandom = incrementRandom(nextRandom);
				int b = (int)((nextRandom % window) + window) % window;
				
				// in -> hidden
				/* Also include topic token into context */
				for (int d = 0; d < layer1_size; d++) {
					neu1[d] += syn0[topicIdx][d];
				}
				
				/* Number of words counted in window starts from 1 (rather 0) */
				int cw = 1;
				for (int a = b; a < window * 2 + 1 - b; a++) {
					if (a == window)
						continue;
					// want to predict the word at sentencePosition given (a-window) words before and after that word
					int c = (sentencePosition - window) + a; // sentencePosition - window is about to determine words in the context window
					if (c < 0 || c >= sentenceLength)
						continue;
					int idx = huffmanNodes.get(sentence.get(c)).idx;
					/// TODO: get rid of API already considered as the topic (already included in this window)
					for (int d = 0; d < layer1_size; d++) {
						neu1[d] += syn0[idx][d];
					}
					
					cw++;
				}
				
				if (cw == 0)
					continue;
				
				for (int c = 0; c < layer1_size; c++)
					neu1[c] /= cw;
				
				if (config.useHierarchicalSoftmax) {
					for (int d = 0; d < huffmanNode.code.length; d++) {
						double f = 0;
						int l2 = huffmanNode.point[d];
						// Propagate hidden -> output                                                                                                                                                                     
						for (int c = 0; c < layer1_size; c++)
							f += neu1[c] * syn1[l2][c];
						if (f <= -MAX_EXP || f >= MAX_EXP)
							continue;
						else
							f = EXP_TABLE[(int)((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))];
						// 'g' is the gradient multiplied by the learning rate                                                                                                                                            
						double g = (1 - huffmanNode.code[d] - f) * alpha;
						// Propagate errors output -> hidden                                                                                                                                                              
						for (int c = 0; c < layer1_size; c++)
							neu1e[c] += g * syn1[l2][c];
						// Learn weights hidden -> output                                                                                                                                                                 
						for (int c = 0; c < layer1_size; c++)
							syn1[l2][c] += g * neu1[c];
					}
				}
				
				handleNegativeSampling(huffmanNode);
				
				// hidden -> in
				/* Also include topic token to update calculation */
				for (int d = 0; d < layer1_size; d++)
					syn0[topicIdx][d] += neu1e[d];
				
				for (int a = b; a < window * 2 + 1 - b; a++) {
					if (a == window)
						continue;
					int c = sentencePosition - window + a;
					if (c < 0 || c >= sentenceLength)
						continue;
					int idx = huffmanNodes.get(sentence.get(c)).idx;
					for (int d = 0; d < layer1_size; d++)
						syn0[idx][d] += neu1e[d];
				}
			}
		}
	}

	@Override Worker createWorker(int randomSeed, int iter, Iterable<List<String>> batch) {
		return new CBOWWorker(randomSeed, iter, batch);
	}
}