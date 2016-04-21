package retrieval;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.medallia.word2vec.SearcherImpl;
import com.medallia.word2vec.Word2VecModel;
import com.medallia.word2vec.util.FileUtils;

public class CosineData4CombinedrVSM {
	
	public static void main(String[] args) throws Exception {
		CosineData4CombinedrVSM generator = new CosineData4CombinedrVSM();
		generator.generateWordPairsWithCos();
	}

	
	@SuppressWarnings("unchecked")
	public void generateWordPairsWithCos() {
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		LinkedHashMap<String, LinkedHashMap<String, Integer>> KJ_TokenAPIPairs = 
				(LinkedHashMap<String, LinkedHashMap<String, Integer>>) FileUtils.readObjectFile(s + "/data/retrieval/KJ_TokensWithAPICode.dat");
		
		LinkedHashMap<String, Integer> KodeJavaAPIDictionary = 
				(LinkedHashMap<String, Integer>) FileUtils.readObjectFile(s + "/data/retrieval/KodeJavaAPIDictionary.dat");
		
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
		
		/* Return pairs of word and API with cosine */
		LinkedHashMap<Pair<String, String>, Double> WordPairsWithCos = new LinkedHashMap<Pair<String,String>, Double>();
		
		/* Just for debugging */
		HashSet<String> APIWithoutDoc = new HashSet<String>();
		
		for(String word : KJ_TokenAPIPairs.keySet()) {
			LinkedHashMap<String, Integer> APIContainsThisToken = KJ_TokenAPIPairs.get(word);
			Iterator<Map.Entry<String, Integer>> entries = APIContainsThisToken.entrySet().iterator();
			
			while(entries.hasNext()) {
				Map.Entry<String, Integer> entry = entries.next();
				String API = entry.getKey();

				Pair<String, String> wordAPIPair = Pair.create(word, API);
				
				double cosine = 0.0;
				try {
					cosine = searchImpl.cosineDistance(word, API);
				} catch(Exception ex) {
					if(!APIWithoutDoc.contains(API)) {
						System.out.println(API);
						APIWithoutDoc.add(API);
					}
					cosine = 1.0;
				}

				if(!WordPairsWithCos.containsKey(wordAPIPair))
					WordPairsWithCos.put(wordAPIPair, cosine);
			}
		}
		
		/* Write the data into output file */
		FileUtils.writeObjectFile(WordPairsWithCos, s + "/data/retrieval/WordPairsWithCos.dat");
		
		/* */
		System.out.printf("Number of pair:%d\t%d\n", WordPairsWithCos.size(), APIWithoutDoc.size());
	}
}
