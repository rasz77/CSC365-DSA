# CSC365-> DSA
Projects 1‬
‭Description:‬
‭In this project, I used three sorting algorithms in Java. The data was preprocessed to select only the data that had a history of‬
‭COVID-19 information. All of the files were in CSV format, so we used the Commons CSV‬
‭package by Apache to parse the CSV files.‬ Before sorting, the data was shuffled randomly using Java's default `Collection.shuffle`method.‬
‭I analyzed the time required to sort the data by each sorting method. We used all the obtained‬
‭COVID-19 case data for sorting, i.e., I had 1615925 pieces of data to sort. The results of them‬
‭sorting by ‘VAERSID’ is as follows:‬
Quick Sort‬-  The complexity of the quicksort algorithm in the average case is O(n logn). This‬ ‭algorithm took 3699 ms to complete the sorting process.‬
Merge Sort‬- The complexity of the merge sort algorithm in the average case is O(n logn). This‬ ‭algorithm took 2089ms to complete the sorting process.‬
Insertion Sort‬- The complexity of the insertion sort algorithm in the average case is O(n^2). This‬ algorithm took 9992331 ms, approximately took about 3 hours to complete.


Project 2‬‬
This project implements a B+ Tree data structure to store and retrieve VAERS efficiently‬
‭(Vaccine Adverse Event Reporting System) records from multiple CSV datasets. Key‬ functionalities developed include:‬ CSV Data Integration: VAERS datasets from 2020–2025 from the output of Project 1 are parsed‬ using Apache Commons CSV and loaded into the B+ Tree using VAERSID as the key.‬
‭B+ Tree Operations: Custom implementation of insertions, node splitting (leaf and internal), and‬ parent-child management supports dynamic balanced indexing.‬
‭ Searching: Records can be searched by VAERSID, offering a fast lookup due to the tree’s‬
‭ logarithmic time complexity.‬ Incremental Updates: Optional loading of newer entries (e.g., updated 2025 records) prevents‬
‭ duplication by checking existing keys before insertion.‬
‭ Time Complexity:‬‭ O(logn)‬‭ for searching, inserting, and finding the height of the tree.‬
‭ Console Output:‬
‭ Enter max degree of B+ Tree: 1000‬
‭ Loaded: src/outputfromProject1/VAERSCOVID2020.csv‬
-Time to load src/outputfromProject1/VAERSCOVID2020.csv: 252 ms‬
Loaded: src/outputfromProject1/VAERSCOVID2021.csv‬
-Time to load src/outputfromProject1/VAERSCOVID2021.csv: 16094 ms‬
Loaded: src/outputfromProject1/VAERSCOVID2022.csv‬
-Time to load src/outputfromProject1/VAERSCOVID2022.csv: 5858 ms‬
Loaded: src/outputfromProject1/VAERSCOVID2022.csv‬
_Time to load src/outputfromProject1/VAERSCOVID2022.csv: 5581 ms‬
Loaded: src/outputfromProject1/VAERSCOVID2023.csv‬
-Time to load src/outputfromProject1/VAERSCOVID2023.csv: 1815 ms‬
Loaded: src/outputfromProject1/VAERSCOVID2024.csv‬
-Time to load src/outputfromProject1/VAERSCOVID2024.csv: 471 ms‬
Loaded: src/outputfromProject1/VAERSCOVID2025.csv‬
-Time to load src/outputfromProject1/VAERSCOVID2025.csv: 25 ms‬
All data loaded into the tree in 30096 ms‬‬


Project 3 ‬
Data Preprocessing‬: The VAERS dataset is known for high dimensionality and variability in textual fields. To structure‬ the data for association rule mining, the preprocessing focused on extracting clean, usable‬ categorical symptom features:‬ Selected structured columns: SYMPTOM1 to SYMPTOM5.‬
-All symptoms are in these columns. Using these columns means we are using all the‬ available symptoms in each transaction in the database‬
-Removed entries with null/short/irrelevant symptom descriptions (length ≤ 3).‬
-Symptom fields with values Null/None are ignored‬
-None/Null symptoms were padded with empty strings to ensure uniformity in the output CSV‬
‭ formatting.‬‭
This step transforms each vaccine report into a transaction, where symptoms act as items,‬ making the data suitable for the Apriori algorithm.‬
Motivation for Using Apriori‬: Apriori is a classical frequent pattern mining algorithm that fits well in this use case. Apriori's‬ interpretable rule structure makes it appropriate for medical datasets. There is also no need for‬‭complex embeddings or distance functions; it works directly on categorical items. The support‬
‭and confidence thresholds allow filtering significant relationships among symptoms.‬
‭Parameter Selection‬
‭Parameter values were chosen based on the dataset size and the nature of adverse event data‬
‭and the results of running on different values beforehand:‬
-Minimum Support‬‭ = 0.01‬(This ensures only symptoms appearing in at least 1% of all reports (~1.6 million records)‬
‭ are considered.)‬
-Minimum Confidence‬‭ = 0.05‬
‭This enables the detection of rules with moderate strength without overfitting to random‬
co-occurrences,‬ Dataset Summary:‬
‭ Total processed: 1.6 million records‬
‭ Total transactions used in Apriori: 1,603,049‬
‭ Processing time:‬
‭ CSV merge: 31.67 seconds‬
‭ Apriori mining: 61.76 seconds‬‭‭

Frequent Itemsets:‬
-Total frequent symptoms (≥ 1% support): 40+ single symptoms‬
- 1-itemsets:‬
‭ SARS-CoV-2 test (13.9%)‬
‭ COVID-19 (11.0%)‬
‭ Pyrexia, Headache, Fatigue, Pain (< 10%)‬
‭
‭
‭‬‬
