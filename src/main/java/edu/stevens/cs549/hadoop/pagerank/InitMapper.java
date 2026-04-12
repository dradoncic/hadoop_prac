package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;

public class InitMapper extends Mapper<LongWritable, Text, Text, Text> {

	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException,
			IllegalArgumentException {
		String line = value.toString(); // Converts Line to a String
		/* 
		 * Parse the input line in format "node-id: to-node1 to-node2 ..."
		 * Emit each neighbor as a separate key-value pair so the reducer can collect all neighbors.
		 * Also ensure nodes with no outgoing edges are still emitted to the reducer.
		 */
		
		// Split on ": " to separate node ID from adjacency list
		String[] parts = line.split(": ");
		if (parts.length != 2) {
			return; // Skip malformed lines
		}
		
		String nodeId = parts[0].trim();
		String adjacencyPart = parts[1].trim();
		
		// If node has neighbors, emit each one
		if (!adjacencyPart.isEmpty()) {
			String[] neighbors = adjacencyPart.split(" ");
			for (String neighbor : neighbors) {
				if (!neighbor.isEmpty()) {
					context.write(new Text(nodeId), new Text(neighbor.trim()));
				}
			}
		} else {
			// Node has no outgoing edges - emit with empty value to trigger reducer
			context.write(new Text(nodeId), new Text(""));
		}
	}

}
