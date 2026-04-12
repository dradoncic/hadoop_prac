package edu.stevens.cs549.hadoop.pagerank;

import java.io.*;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;

public class DiffRed1 extends Reducer<Text, Text, Text, Text> {

	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		double[] ranks = new double[2];
		/* 
		 * TODO: The list of values should contain two ranks.  Compute and output their difference.
		 */
		int count = 0;
		for (Text value : values) {
			if (count < 2) {
				ranks[count] = Double.parseDouble(value.toString());
				count++;
			}
		}
		
		// Should have exactly two rank values from the two iterations being compared
		if (count == 2) {
			// Compute absolute difference between the two rank values
			double difference = Math.abs(ranks[0] - ranks[1]);
			// Output difference as key with null value
			context.write(new Text(String.valueOf(difference)), null);
		}
	}
}
