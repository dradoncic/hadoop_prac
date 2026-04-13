#!/bin/bash
# Deen Radoncic

# create test files
cat > /tmp/graph.txt << 'EOF'
1: 2 3
2: 3
3: 1 4 5
4: 1
5: 2 4
EOF

cat > /tmp/names.txt << 'EOF'
1: PageA
2: PageB
3: PageC
4: PageD
5: PageE
EOF

# clean HDFS
hadoop fs -rm -r /graph 2>/dev/null || true
hadoop fs -mkdir -p /graph/input

# upload files
hadoop fs -put /tmp/graph.txt /graph/input/
hadoop fs -put /tmp/names.txt /graph/names.txt

# echo "===== STEP 1: INIT ====="
# hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar init /graph/input /graph/iter1 2
# hadoop fs -cat /graph/iter1/part-r-*

# echo -e "\n===== STEP 2: ITER 1 ====="
# hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar iter /graph/iter1 /graph/iter2 2
# hadoop fs -cat /graph/iter2/part-r-*

# echo -e "\n===== STEP 3: ITER 2 ====="
# hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar iter /graph/iter2 /graph/iter3 2
# hadoop fs -cat /graph/iter3/part-r-*

# echo -e "\n===== STEP 4: ITER 3 ====="
# hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar iter /graph/iter3 /graph/iter4 2
# hadoop fs -cat /graph/iter4/part-r-*

# echo -e "\n===== STEP 5: DIFF (iter3 vs iter4) ====="
# hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar diff /graph/iter3 /graph/iter4 /graph/diff_test 2
# hadoop fs -cat /graph/diff_test/part-r-*

# echo -e "\n===== STEP 6: JOIN with Names ====="
# hadoop fs -cp /graph/iter4 /graph/final_ranks
# hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar join /graph/final_ranks /graph/names.txt /graph/joined 2
# hadoop fs -cat /graph/joined/part-r-*

# echo -e "\n===== STEP 7: FINISH (Sort by Rank) ====="
# hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar finish /graph/joined /graph/final_output 2
# hadoop fs -cat /graph/final_output/part-r-*

echo -e "\n===== COMPOSITE (Full Pipeline) ====="
hadoop fs -rm -r /graph/composite_output /graph/interim1 /graph/interim2 /graph/diff 2>/dev/null || true
hadoop jar ~/tmp/cs549/hadoop-test/pagerank.jar \
  composite \
  /graph/input \
  /graph/composite_output \
  /graph/interim1 \
  /graph/interim2 \
  /graph/names.txt \
  /graph/diff \
  2
echo -e "\n===== COMPOSITE OUTPUT ====="
hadoop fs -cat /graph/composite_output/output.txt
