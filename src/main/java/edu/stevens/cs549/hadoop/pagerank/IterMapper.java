package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;

public class IterMapper extends Mapper<LongWritable, Text, Text, Text> {

	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException,
			IllegalArgumentException {
		String line = value.toString(); // Converts Line to a String
		String[] sections = line.split("\t"); // Splits it into two parts. Part 1: node;rank | Part 2: adj list

		if (sections.length > 2) // Checks if the data is in the incorrect format
		{
			throw new IOException("Incorrect data format");
		}
		if (sections.length != 2) {
			return;
		}
		
		/* 
		 * Parse the node;rank format and adjacency list.
		 * Emit (neighbor, weight) pairs where weight = rank / numNeighbors
		 * Also emit (vertex, |adjacency_list) to preserve the adjacency list for reducer
		 * The "|" marker indicates this is an adjacency list, not a weight
		 */
		
		// Parse vertex;rank
		String[] nodeParts = sections[0].split(";");
		if (nodeParts.length != 2) {
			return;
		}
		String vertex = nodeParts[0];
		double rank = Double.parseDouble(nodeParts[1]);
		
		// Parse adjacency list
		String adjListStr = sections[1].trim();
		String[] neighbors = adjListStr.split(",");
		
		// Distribute rank equally among neighbors (PageRank formula)
		double weight = rank / neighbors.length;
		
		// Emit weight contributions to each neighbor
		for (String neighbor : neighbors) {
			neighbor = neighbor.trim();
			if (!neighbor.isEmpty()) {
				context.write(new Text(neighbor), new Text(String.valueOf(weight)));
			}
		}
		
		// Emit adjacency list with marker for this vertex
		// Use "|" prefix to distinguish from weight contributions
		context.write(new Text(vertex), new Text("|" + adjListStr));
	}

}
