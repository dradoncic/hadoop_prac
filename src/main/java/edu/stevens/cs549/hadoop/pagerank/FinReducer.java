package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class FinReducer extends Reducer<DoubleWritable, Text, Text, Text> {

	public void reduce(DoubleWritable key, Iterable<Text> values, Context context) throws IOException,
			InterruptedException {
		/* 
		 * The key is the negative rank (for reverse sorting in FinMapper)
		 * Convert it back to positive rank and emit (vertex name, positive rank)
		 * for each vertex name in the values
		 */
		double negRank = key.get();
		double posRank = -negRank; // Convert negative rank back to positive
		
		// Emit each vertex name with its positive rank
		for (Text vertexName : values) {
			context.write(vertexName, new Text(String.valueOf(posRank)));
		}
	}
}
