# PageRank Implementation Guide

## Overview

This document describes the complete implementation of the PageRank algorithm using Hadoop MapReduce. The implementation calculates the importance of Wikipedia pages based on their link structure and outputs results sorted by page rank in descending order.

## Algorithm Overview

PageRank is an iterative algorithm that models the probability of a random user following links on a web page. The formula for each iteration is:

```
rank(v) = (1 - d) + d * Σ(rank(u) / |outgoing(u)|)
```

Where:
- `d` = 0.85 (decay factor, set in `PageRankDriver.DECAY`)
- `u` = nodes linking to `v`
- `|outgoing(u)|` = number of outgoing links from `u`

The algorithm iterates until convergence (when rank differences fall below a threshold of 30).

## Intermediate Data Format

Data is processed in an intermediate format throughout the iterations:

```
vertex;rank    adjacency_list
```

For example:
```
1234;0.95      5678,9012,3456
5678;1.02      1234
```

This format preserves the adjacency list through iterations, which is necessary for PageRank computation.

## Job Types

### 1. Init Job

**Purpose:** Convert input data into the intermediate format with initial rank values.

#### InitMapper
- **Input:** Lines in format `node-id: to-node1 to-node2 ...`
- **Output:** Key=`node-id`, Value=`to-node`
- **Logic:** Splits each node's adjacency list into individual neighbor pairs

#### InitReducer
- **Input:** Key=`node-id`, Values=list of neighbors
- **Output:** Key=`node-id;1.0`, Value=`neighbor1,neighbor2,...`
- **Logic:** 
  - Collects all neighbors for a node
  - Assigns initial rank of 1.0 to all nodes
  - Outputs in intermediate format

### 2. Iter Job (Iterative Computation)

**Purpose:** Perform one round of PageRank computation.

#### IterMapper
- **Input:** Intermediate format lines
- **Output:** Key=`neighbor`, Value=`weight` OR Key=`vertex`, Value=`|adjacency_list`
- **Logic:**
  - Parses `vertex;rank` and adjacency list
  - Computes weight = `rank / number_of_neighbors`
  - Emits weight contributions to each neighbor
  - Marks and preserves adjacency list with `|` prefix

#### IterReducer
- **Input:** Key=`vertex`, Values=list of weights + one marked adjacency list
- **Output:** Key=`vertex;new_rank`, Value=`adjacency_list`
- **Logic:**
  - Accumulates weights from incoming edges: `(1 - d) + d * Σ(weights)`
  - Preserves adjacency list for next iteration
  - Outputs updated intermediate format

### 3. Diff Job (Convergence Detection)

**Purpose:** Compute maximum difference between successive iterations.

#### DiffMap1
- **Input:** Intermediate format lines from two consecutive iterations
- **Output:** Key=`vertex`, Value=`rank`
- **Logic:** Extracts vertex and rank from intermediate format

#### DiffRed1
- **Input:** Key=`vertex`, Values=list of two ranks (from two iterations)
- **Output:** Key=`difference`, Value=`null`
- **Logic:** Computes absolute difference between the two rank values

#### DiffMap2
- **Input:** Difference values from DiffRed1
- **Output:** Key=`"Difference"`, Value=`difference_value`
- **Logic:** Emits all differences under a common key

#### DiffRed2
- **Input:** Key=`"Difference"`, Values=all difference values
- **Output:** Key=`maximum_difference`, Value=`null`
- **Logic:** Finds and outputs the maximum difference across all vertices

### 4. Join Job (Vertex Names)

**Purpose:** Join page ranks with vertex names for human-readable output.

#### JoinNameMapper
- **Input:** Format `node-id: page-name`
- **Output:** Key=`TextPair(node-id, "0")`, Value=`page-name`
- **Logic:** Tags name data with "0" for ordering in reduce phase

#### JoinRankMapper
- **Input:** Intermediate format lines
- **Output:** Key=`TextPair(vertex, "1")`, Value=`rank`
- **Logic:** 
  - Extracts `vertex` and `rank` from intermediate format
  - Tags rank data with "1" for ordering in reduce phase
  - Ignores adjacency list (no longer needed)

#### JoinReducer
- **Input:** Key=`vertex`, Values=list containing name (tagged "0") then rank (tagged "1")
- **Output:** Key=`vertex_name`, Value=`rank`
- **Logic:**
  - Receives name first (due to "0" tag ordering)
  - Receives rank second (due to "1" tag ordering)
  - Outputs (name, rank) pair

**Custom Partitioner (KeyPartitioner):**
- Partitions on the first component of TextPair (vertex id)
- Ensures same vertex data goes to same reducer

**Custom Grouping Comparator (TextPair.FirstComparator):**
- Groups by first component only (vertex id)
- Allows all tags ("0" and "1") for same vertex to be grouped together

### 5. Finish Job (Output Formatting)

**Purpose:** Sort results by page rank in descending order.

#### FinMapper
- **Input:** Format `vertex_name\trank` (from JoinReducer)
- **Output:** Key=`-rank` (negative for reverse sorting), Value=`vertex_name`
- **Logic:** Inverts rank sign to enable descending sort order

#### FinReducer
- **Input:** Key=`-rank`, Values=list of vertex names with that rank
- **Output:** Key=`vertex_name`, Value=`rank` (positive)
- **Logic:**
  - Converts rank back to positive
  - Emits (vertex_name, rank) pairs in descending order

## Composite Job Flow

The `composite()` function orchestrates the complete PageRank pipeline:

1. **Initialization:** Runs `init()` to create intermediate format with initial ranks
2. **Iteration Loop:** Alternates between:
   - `iter()`: Updates page ranks (runs every iteration)
   - `diff()`: Checks convergence (runs every 3rd iteration)
   - Alternates using `interim1` and `interim2` directories to avoid I/O overhead
3. **Convergence Check:** Continues until max rank difference < 30
4. **Join Step:** Merges page ranks with vertex names
5. **Finish Step:** Sorts by rank in descending order
6. **Summarization:** Creates human-readable output summary

## Data Flow Summary

```
Input (adjacency lists)
        ↓
    [init]
        ↓
Intermediate Format (vertex;rank → neighbors)
        ↓
    [iter] ←→ [diff] (convergence check)
        ↓
Final Ranks (vertex;rank → neighbors)
        ↓
    [join] (merge with vertex names)
        ↓
Named Ranks (vertex_name → rank)
        ↓
    [finish] (sort by rank descending)
        ↓
Output (sorted vertex_name → rank)
```

## Key Implementation Details

### Marker for Adjacency Lists
In IterMapper, adjacency lists are prefixed with `|` to distinguish them from weight contributions:
- Weight values: `"0.5"`, `"0.25"`, etc.
- Adjacency lists: `"|5678,9012,3456"`

This allows the IterReducer to identify which value contains the adjacency list.

### Directory Management
- Each job deletes its output directory at the start (via FileOutputFormat.setOutputPath)
- The `join()` function deletes its input (ranks) directory after completing
- The `finish()` function deletes its input (joined data) directory after completing
- This keeps the filesystem clean during long runs

### Sorting for Join Operation
The join requires both datasets to be sorted by vertex id:
- JoinNameMapper and JoinRankMapper emit TextPair keys
- KeyPartitioner ensures same vertex goes to same reducer
- TextPair.FirstComparator groups by vertex id, allowing multiple tags per vertex
- This enables a reduce-side join without explicit pre-sorting

### Reverse Sorting
Hadoop sorts keys in ascending order by default. To get descending ranks:
1. FinMapper emits negative ranks as keys
2. Hadoop sorts these in ascending order (most negative first = highest rank)
3. FinReducer converts back to positive for output

## Input Format

The input directory should contain one or more text files in the format:

```
1: 2 3 4
2: 1 5
3: 1
4: 1 2
5: 2
```

Each line represents a node and its outgoing links.

## Output Format

The final output file `output.txt` in the output directory contains:

```
Wikipedia_Page_Name	0.95432
Another_Page	0.87654
Third_Page	0.76543
...
```

Sorted by rank in descending order.

## Names File Format

The vertex names file (required for the composite job) should have format:

```
1: Wikipedia_Page_Name
2: Another_Page
3: Third_Page
4: Fourth_Page
5: Fifth_Page
```

One vertex id and name per line, separated by `: `.

## Testing

To test the implementation:

1. Create test input with a small graph
2. Create a names file with vertex ids and page names
3. Run composite job:
   ```
   hadoop jar pagerank.jar composite input output interim1 interim2 names.txt difftemp 2
   ```
4. Check `output/output.txt` for results
5. Verify ranks sum to approximately (or slightly more than) the number of vertices

## Constants

- `DECAY = 0.85`: Standard PageRank decay factor
- `THRESHOLD = 30`: Convergence threshold (max rank difference in absolute value)

## Error Handling

The implementation includes error handling for:
- Malformed input lines (checked in mappers)
- Missing or incomplete data (checked in reducers)
- Non-numeric rank values (try-catch blocks)
- Missing adjacency lists (handled in IterReducer)
- Missing vertex data in join (checked in JoinReducer)