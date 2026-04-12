package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class JoinRankMapper extends Mapper<LongWritable, Text, TextPair, Text> {

	@Override
	protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

        /*
         * We assume join is coming after IterReducer and before FinMapper.
         */

        String line = value.toString(); // Converts Line to a String
        String[] sections = line.split("\t"); // Splits it into two parts. Part 1: node;rank | Part 2: adj list

        if (sections.length > 2) // Checks if the data is in the incorrect format
        {
            throw new IOException("Incorrect data format");
        }
        if (sections.length != 2) {
            return;
        }

        // Parse the node;rank format
        String nodeRankStr = sections[0];
        String[] nodeParts = nodeRankStr.split(";");
        
        if (nodeParts.length != 2) {
            return;
        }
        
        String node = nodeParts[0];
        String rank = nodeParts[1];
        
        // Emit (TextPair(node, "1"), rank)
        // The "1" tag indicates this is rank data (as opposed to "0" for names from JoinNameMapper)
        context.write(new TextPair(node, "1"), new Text(rank));
	}
}
