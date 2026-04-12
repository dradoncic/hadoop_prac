package edu.stevens.cs549.hadoop.pagerank;

import java.io.*;
import java.util.*;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;

public class InitReducer extends Reducer<Text, Text, Text, Text> {

	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		/* 
		 * Collect all neighbors for this node and output in intermediate format.
		 * Output key: "node;rank" where initial rank is 1.0
		 * Output value: comma-separated list of neighbors
		 */
		StringBuilder adjList = new StringBuilder();
		boolean first = true;
		
		// Collect all neighbors into a comma-separated list
		for (Text value : values) {
			String neighbor = value.toString();
			// Skip empty values (from nodes with no outgoing edges)
			if (!neighbor.isEmpty()) {
				if (!first) {
					adjList.append(",");
				}
				adjList.append(neighbor);
				first = false;
			}
		}
		
		// Output format: "vertex;1.0" -> "neighbor1,neighbor2,..."
		// Initial rank is 1.0
		String outputKey = key.toString() + ";1.0";
		String outputValue = adjList.toString();
		
		context.write(new Text(outputKey), new Text(outputValue));
	}
}
