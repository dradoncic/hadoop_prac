package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;

public class FinMapper extends Mapper<LongWritable, Text, DoubleWritable, Text> {

	public void map(LongWritable key, Text value, Context context)
			throws IOException, InterruptedException, IllegalArgumentException {
		String line = value.toString(); // Converts Line to a String
		/*
		 * Parse the output of JoinReducer which outputs:
		 * key: vertex name, value: page rank
		 * 
		 * Output: key: -rank (negative for reverse sorting), value: vertex name
		 */
		
		String[] parts = line.split("\t");
		if (parts.length != 2) {
			return;
		}
		
		String vertexName = parts[0];
		double rank = Double.parseDouble(parts[1]);
		
		// Emit key as negative rank (for reverse sorting), value as vertex name
		context.write(new DoubleWritable(-rank), new Text(vertexName));
	}

}
