package edu.stevens.cs549.hadoop.pagerank;

import java.io.*;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;

public class DiffRed2 extends Reducer<Text, Text, Text, Text> {

	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		double diff_max = 0.0; // sets diff_max to a default value
		/* 
		 * Iterate through all difference values and find the maximum
		 */
		for (Text value : values) {
			try {
				double diff = Double.parseDouble(value.toString());
				if (diff > diff_max) {
					diff_max = diff;
				}
			} catch (NumberFormatException e) {
				// Skip malformed values
			}
		}
		
		// Emit the maximum difference as key with null value
		context.write(new Text(String.valueOf(diff_max)), null);
	}
}
