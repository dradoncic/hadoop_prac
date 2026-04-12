package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;

public class DiffMap2 extends Mapper<LongWritable, Text, Text, Text> {

	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException,
			IllegalArgumentException {
		String s = value.toString(); // Converts Line to a String

		/* 
		 * Parse the difference value from DiffRed1 output and emit with "Difference" key
		 * DiffRed1 outputs: difference_value as key, null as value
		 */
		
		// Extract the difference value (first part before any tab)
		String[] parts = s.trim().split("\t");
		if (parts.length >= 1) {
			String difference = parts[0];
			// Emit key="Difference", value=difference
			context.write(new Text("Difference"), new Text(difference));
		}
	}

}
