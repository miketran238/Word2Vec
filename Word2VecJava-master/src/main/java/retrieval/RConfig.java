package retrieval;

public class RConfig {
	
	public static String modelType = "API2VEC"; // Word2Vec, API2VEC
	public static int window = 8;
	public static int dimension = 200;
	public static double sampleRate = 1e-4; // how to choose
	public static String outputExtension = RConfig.modelType + "_Mapping1_" + RConfig.window + RConfig.dimension + sampleRate; // + "ManFix"
	
	/* Mixture/Linear combination of score */
	public static double alpha = 0.5;
	
	public static boolean isScoring1 = false;
}
