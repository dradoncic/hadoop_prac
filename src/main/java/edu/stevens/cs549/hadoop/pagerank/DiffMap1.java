package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;

public class DiffMap1 extends Mapper<LongWritable, Text, Text, Text> {

	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException,
			IllegalArgumentException {
		String line = value.toString(); // Converts Line to a String
		String[] sections = line.split("\t"); // Splits each line
		if (sections.length > 2) // checks for incorrect data format
		{
			throw new IOException("Incorrect data format");
		}
		/**
		 * Parse the intermediate format: "vertex;rank" and emit key:vertex, value:rank
		 */
		if (sections.length < 1) {
			return;
		}
		
		// Extract the vertex;rank part
		String nodeRankStr = sections[0];
		String[] parts = nodeRankStr.split(";");
		
		if (parts.length != 2) {
			return;
		}
		
		String vertex = parts[0];
		String rank = parts[1];
		
		// Emit vertex as key and rank as value
		context.write(new Text(vertex), new Text(rank));
	}

}
