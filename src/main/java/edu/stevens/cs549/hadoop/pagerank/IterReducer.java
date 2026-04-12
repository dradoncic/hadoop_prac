package edu.stevens.cs549.hadoop.pagerank;

import java.io.*;
import java.util.*;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;

public class IterReducer extends Reducer<Text, Text, Text, Text> {
	
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		/* 
		 * Compute new PageRank using the formula:
		 * rank(v) = (1 - d) + d * sum(weights from incoming edges)
		 * 
		 * The values contain:
		 * 1. Weight contributions from each incoming edge
		 * 2. One adjacency list marked with "|" prefix to distinguish it from weights
		 */
		double d = PageRankDriver.DECAY; // Decay factor (0.85)
		double rank = 1 - d; // Start with (1 - d) term of PageRank formula
		String adjList = "";
		
		// Process all incoming values
		for (Text value : values) {
			String val = value.toString();
			
			// Check if this is the adjacency list (marked with | prefix)
			if (val.startsWith("|")) {
				// Extract adjacency list without the marker
				adjList = val.substring(1);
			} else {
				// This is a weight contribution from an incoming edge
				try {
					double weight = Double.parseDouble(val);
					rank += d * weight;
				} catch (NumberFormatException e) {
					// Skip malformed weights
				}
			}
		}
		
		// Output format: key = "vertex;newrank", value = "adjacency_list"
		String outputKey = key.toString() + ";" + rank;
		context.write(new Text(outputKey), new Text(adjList));
	}
}
