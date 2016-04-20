package retrieval;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class Utils {

	public static LinkedHashMap<RetrievedCodeExample, Double> sortObjMap(LinkedHashMap<RetrievedCodeExample, Double> unsortedMap) {
		List<Entry<RetrievedCodeExample, Double>> list = new LinkedList<>(unsortedMap.entrySet());
		
		Collections.sort(list, new Comparator<Entry<RetrievedCodeExample, Double>>() {
			public int compare(Entry<RetrievedCodeExample, Double> o1, Entry<RetrievedCodeExample, Double> o2) {
				return o2.getValue().compareTo(o1.getValue()); // descending order
			}
		});
		
		LinkedHashMap<RetrievedCodeExample, Double> sortedMap = new LinkedHashMap<>();
		for(Entry<RetrievedCodeExample, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		
		return sortedMap;
	}
}
