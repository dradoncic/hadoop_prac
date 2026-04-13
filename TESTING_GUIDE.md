# PageRank Testing and Build Guide

## Prerequisites

1. **Hadoop Installation**: Ensure Hadoop 3.4.1 (or compatible) is installed
   - Set `HADOOP_HOME` environment variable
   - Add `$HADOOP_HOME/bin` to your PATH

2. **Java**: Java 8 or higher (Maven requires Java 8+)

3. **Maven**: Apache Maven 3.6 or higher for building the project

4. **Test Data**: Prepare input files in the correct format

## Building the Project

### Step 1: Navigate to Project Directory
```bash
cd /home/hadoop/Desktop/Assignment4-Code/page-rank
```

### Step 2: Build with Maven
```bash
mvn clean package
```

This will:
- Compile all source code
- Run any tests (if configured)
- Create `pagerank.jar` in the `target/` directory
- Copy the JAR to `~/tmp/cs549/hadoop-test/pagerank.jar`

### Step 3: Verify Build Success
```bash
ls -la ~/tmp/cs549/hadoop-test/pagerank.jar
```

If the JAR exists and is recent, the build was successful.

## Input Data Format

### Adjacency List File (links.txt)
Each line contains a node ID followed by its outgoing links:
```
1: 2 3 4
2: 1 5
3: 1
4: 1 2
5: 2
```

- First component: source node ID
- `: ` (colon and space): separator
- Remaining components: space-separated list of target node IDs

### Names File (names.txt)
Each line maps a node ID to a human-readable name:
```
1: Home
2: About
3: Contact
4: Blog
5: Privacy
```

- First component: node ID
- `: ` (colon and space): separator
- Remaining component: page name (no tabs, single line)

## Testing Procedures

### Option 1: Manual Job Testing (Recommended First)

This approach lets you test each job individually to verify correctness.

#### Test 1: Init Job
```bash
# Copy test data to HDFS
hadoop fs -mkdir -p /test/input
hadoop fs -put /home/hadoop/Desktop/Assignment4-Code/example_test_data/links.txt /test/input/

# Run init job
hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar \
  edu.stevens.cs549.hadoop.pagerank.PageRankDriver \
  init /test/input /test/iter1 2

# Check output
hadoop fs -cat /test/iter1/part-r-00000
```

**Expected Output Format:**
```
1;1.0    2,3,4
2;1.0    1,5
3;1.0    1
4;1.0    1,2
5;1.0    2
```

Each line has:
- vertex;rank (tab separator)
- comma-separated adjacency list

#### Test 2: Iter Job
```bash
# Run one iteration
hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar \
  edu.stevens.cs549.hadoop.pagerank.PageRankDriver \
  iter /test/iter1 /test/iter2 2

# Check output
hadoop fs -cat /test/iter2/part-r-00000
```

**Expected Output Format:**
```
1;2.2775    2,3,4
2;1.425     1,5
3;0.2075    1
4;1.0075    1,2
5;0.64      2
```

- Ranks should change from initial 1.0
- All nodes should still have their adjacency lists
- Ranks should be greater than (1-d) = 0.15 due to incoming contributions

#### Test 3: Diff Job
```bash
# Run diff between two iterations
hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar \
  edu.stevens.cs549.hadoop.pagerank.PageRankDriver \
  diff /test/iter1 /test/iter2 /test/diff 2

# Check output
hadoop fs -cat /test/diff/part-r-00000
```

**Expected Output:**
A single number representing the maximum difference between ranks in consecutive iterations:
```
2.0775
```

This is the largest difference between any vertex's rank in the two iterations.

#### Test 4: Join Job
```bash
# Put names file in HDFS
hadoop fs -put /home/hadoop/Desktop/Assignment4-Code/example_test_data/names.txt /test/names.txt

# Run join (note: join deletes first input directory)
hadoop fs -cp /test/iter2 /test/iter2_copy
hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar \
  edu.stevens.cs549.hadoop.pagerank.PageRankDriver \
  join /test/iter2_copy /test/names.txt /test/joined 2

# Check output
hadoop fs -cat /test/joined/part-r-00000
```

**Expected Output Format:**
```
Home	2.2775
About	1.425
Contact	0.2075
Blog	1.0075
Privacy	0.64
```

Format: vertex_name (tab) rank

#### Test 5: Finish Job
```bash
# Run finish (note: finish deletes its input directory)
hadoop fs -cp /test/joined /test/joined_copy
hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar \
  edu.stevens.cs549.hadoop.pagerank.PageRankDriver \
  finish /test/joined_copy /test/final 2

# Check output
hadoop fs -cat /test/final/part-r-00000
hadoop fs -cat /test/final/output.txt
```

**Expected Output Format (part-r files):**
```
Home	2.2775
About	1.425
Blog	1.0075
Privacy	0.64
Contact	0.2075
```

Sorted in descending order by rank.

**Expected Output Format (output.txt):**
Same as above, plus a summary message in the console.

### Option 2: Composite Job Testing

Once individual jobs work correctly, test the complete pipeline:

```bash
# Clean up any previous test data
hadoop fs -rm -r -f /test/composite_test

# Create input directories
hadoop fs -mkdir -p /test/composite_test/input
hadoop fs -mkdir -p /test/composite_test/interim
hadoop fs -mkdir -p /test/composite_test/diff

# Copy test data
hadoop fs -put /home/hadoop/Desktop/Assignment4-Code/example_test_data/links.txt \
  /test/composite_test/input/
hadoop fs -put /home/hadoop/Desktop/Assignment4-Code/example_test_data/names.txt \
  /test/composite_test/names.txt

# Run composite job
hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar \
  edu.stevens.cs549.hadoop.pagerank.PageRankDriver \
  composite \
  /test/composite_test/input \
  /test/composite_test/output \
  /test/composite_test/interim1 \
  /test/composite_test/interim2 \
  /test/composite_test/names.txt \
  /test/composite_test/diff \
  2
```

**Monitor Progress:**
- Job should run several iterations
- Console will print "Difference updates to: X.XXXX"
- Job completes when difference < 30 (THRESHOLD)

**Check Final Output:**
```bash
hadoop fs -cat /test/composite_test/output/output.txt
```

**Expected Output:**
```
Home	3.XXX
About	2.YYY
Blog	1.ZZZ
Privacy	1.AAA
Contact	0.BBB
```

## Verifying Results

### Sanity Checks

1. **All vertices present**: Output should contain all vertices from input
   ```bash
   hadoop fs -cat /test/composite_test/output/output.txt | wc -l
   ```
   Should equal number of vertices in input

2. **Sorted by rank**: Ranks should be in descending order
   ```bash
   hadoop fs -cat /test/composite_test/output/output.txt | awk '{print $2}' | sort -nr
   ```
   Should match the actual output

3. **Rank range check**: Ranks should be between 0 and roughly (# vertices * 2)
   - Minimum rank: > (1 - DECAY) = 0.15
   - Maximum rank: typically < (# vertices)

4. **No duplicates**: Each vertex should appear exactly once
   ```bash
   hadoop fs -cat /test/composite_test/output/output.txt | awk '{print $1}' | sort | uniq -d
   ```
   Should output nothing

### Debugging Output Discrepancies

If output doesn't match expectations:

1. **Check mapper input format**:
   ```bash
   hadoop fs -cat /test/input/links.txt
   ```

2. **Check after init job**:
   ```bash
   hadoop fs -cat /test/iter1/part-r-*
   ```
   - Verify format: vertex;1.0 (tab) neighbors
   - Verify all vertices are present

3. **Check after first iteration**:
   ```bash
   hadoop fs -cat /test/iter2/part-r-*
   ```
   - Verify ranks changed from 1.0
   - Verify no adjacency lists are lost

4. **Check join results**:
   ```bash
   hadoop fs -cat /test/joined/part-r-*
   ```
   - Verify vertex names are correct
   - Verify one entry per vertex

5. **Enable verbose logging** (for debugging):
   Edit `PageRankDriver.java` to add System.out.println() statements
   in mapper/reducer classes

## Cleaning Up Test Data

```bash
# Remove all test data from HDFS
hadoop fs -rm -r -f /test

# Remove local JAR files
rm -f ~/tmp/cs549/hadoop-test/pagerank.jar

# Clean Maven build artifacts
cd /home/hadoop/Desktop/Assignment4-Code/page-rank
mvn clean
```

## Common Issues and Solutions

### Issue: "No such file or directory" for names.txt
**Cause**: Names file path is wrong or file doesn't exist
**Solution**: Verify file exists and path is correct in composite command

### Issue: Join job produces empty output
**Cause**: Names file format incorrect or vertex IDs don't match
**Solution**: 
1. Check names file format: `node-id: name`
2. Verify node IDs match those in ranks file
3. Ensure no extra whitespace

### Issue: Diff value doesn't decrease
**Cause**: PageRank computation incorrect
**Solution**:
1. Check IterMapper weight calculation: `weight = rank / neighbors.count`
2. Check IterReducer rank formula: `(1-d) + d * sum(weights)`
3. Verify adjacency list is being preserved correctly

### Issue: Final ranks are all the same
**Cause**: Adjacency lists not being preserved
**Solution**: 
1. Check IterMapper marks adjacency list with `|` prefix
2. Check IterReducer extracts adjacency list correctly
3. Verify no data loss in shuffle phase

### Issue: Out of memory errors
**Cause**: Too many reducers for small input, or large metadata file
**Solution**:
1. Reduce number of reducers (e.g., use 1 for test data)
2. Check for infinite loops in data processing

## Performance Notes

- **Test data (5-10 nodes)**: Should complete in seconds
- **Small dataset (100-1000 nodes)**: Should complete in 1-2 minutes
- **Diff job overhead**: Runs every 3 iterations to save time
- **Join step**: Required to output vertex names (not optional)

## Expected Convergence for Example Data

For the 5-node example graph:
- Iteration 0: rank = 1.0 for all nodes
- Iteration 1: differences ≈ 1.0-2.0
- Iteration 2-3: differences ≈ 0.3-0.5
- Convergence typically by iteration 15-20

Note: Threshold of 30 is generous; convergence usually happens much faster.

## Next Steps After Successful Testing

1. Test with larger, more realistic graphs
2. Verify output matches expected PageRank distribution
3. Compare results with reference implementations (if available)
4. Optimize for larger datasets (adjust reducer count)
5. Consider implementing caching for large name mappings
