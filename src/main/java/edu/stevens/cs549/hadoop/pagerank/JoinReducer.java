package edu.stevens.cs549.hadoop.pagerank;

import java.io.*;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;

public class JoinReducer extends Reducer<TextPair, Text, Text, Text> {

	public void reduce(TextPair key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		/* 
		 * TextPair ensures that we have values with tag "0" first, followed by tag "1"
		 * So we know that first value is the name and second value is the rank
		 */
		String vertexName = null;
		String pageRank = null;
		int count = 0;
		
		// Due to grouping by TextPair, values are sorted by the second component
		// "0" (names from JoinNameMapper) comes before "1" (ranks from JoinRankMapper)
		for (Text value : values) {
			if (count == 0) {
				vertexName = value.toString();
			} else if (count == 1) {
				pageRank = value.toString();
			}
			count++;
		}
		
		// Only emit if we have both vertex name and page rank
		if (vertexName != null && pageRank != null) {
			context.write(new Text(vertexName), new Text(pageRank));
		}
	}
}
