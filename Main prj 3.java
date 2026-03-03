package org.example;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException {
        Object[][] inputFiles = {
                {"src/output/VAERS_COVID_2020.csv", '\"'},
                {"src/output/VAERS_COVID_2021.csv", '\"'},
                {"src/output/VAERS_COVID_2022.csv", '\"'},
                {"src/output/VAERS_COVID_2023.csv", '\"'},
                {"src/output/VAERS_COVID_2024.csv", '\"'},
                {"src/output/VAERS_COVID_2025.csv", '\"'},
                {"src/output/VAERS_COVID_NonDomestic.csv", null}
        };
        String outputCsvPath = "vaers_output_ml.csv";

        processMultipleCSVTransactions(inputFiles, outputCsvPath);
        apriori(outputCsvPath);
    }


    private static void processMultipleCSVTransactions(Object[][] inputPaths, String outputCsvPath) throws IOException {
        long totalStartTime = System.currentTimeMillis();  // Total timer start

        try (
                Writer writer = new FileWriter(outputCsvPath);
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                        .withHeader("VAERS_ID", "VAX_MANU", "RECVDATE", "AGE_YRS", "SEX", "DIED", "DATEDIED", "VAX_DATE",
                                "no_of_symptoms", "symptom_1", "symptom_2", "symptom_3", "symptom_4", "symptom_5"))
        ) {
            for (Object[] fileInfo : inputPaths) {
                String inputPath = (String) fileInfo[0];
                Character quoteChar = (Character) fileInfo[1];
                long startTime = System.currentTimeMillis();  // Per-file timer start
                int recordCount = 0;

                try (Reader reader = new FileReader(inputPath);
                     CSVParser parser = CSVFormat.DEFAULT
                             .withFirstRecordAsHeader()
                             .withIgnoreSurroundingSpaces()
                             .withQuote(quoteChar)
                             .parse(reader)) {

                    for (CSVRecord record : parser) {
                        String vaersId = record.get("VAERS_ID");
                        String vaxManu = record.get("VAX_MANU");
                        String recvDate = record.get("RECVDATE");
                        String ageYrs = record.get("AGE_YRS");
                        String sex = record.get("SEX");
                        String died = record.get("DIED");
                        String dateDied = record.get("DATEDIED");
                        String vaxDate = record.get("VAX_DATE");

                        List<String> symptoms = new ArrayList<>();
                        for (int i = 1; i <= 5; i++) {
                            String symptom = record.get("SYMPTOM" + i);
                            if (symptom != null && !symptom.trim().isEmpty() && symptom.length() > 3) {
                                symptoms.add(symptom.trim());
                            }
                        }

                        int noOfSymptoms = symptoms.size();
                        while (symptoms.size() < 5) symptoms.add("");

                        printer.printRecord(vaersId, vaxManu, recvDate, ageYrs, sex, died, dateDied, vaxDate,
                                noOfSymptoms,
                                symptoms.get(0), symptoms.get(1), symptoms.get(2), symptoms.get(3), symptoms.get(4));

                        recordCount++;
                    }

                    long endTime = System.currentTimeMillis();  // Per-file timer end
                    double seconds = (endTime - startTime) / 1000.0;
                    System.out.printf("Processed %d records from %s in %.2f seconds.%n", recordCount, inputPath, seconds);

                } catch (IOException e) {
                    System.err.println("Error reading " + inputPath);
                    e.printStackTrace();
                }
            }

            long totalEndTime = System.currentTimeMillis();  // Total timer end
            double totalSeconds = (totalEndTime - totalStartTime) / 1000.0;
            System.out.printf("All files processed. Output written to %s%n", outputCsvPath);
            System.out.printf("Total processing time : %.2f seconds.%n", totalSeconds);
        }
    }

    private static void apriori(String csvPath) throws IOException {
        // 1. Load CSV data using Apache Commons CSV
        long totalStartTime = System.currentTimeMillis();  // Total timer start

        Reader in = new FileReader(csvPath);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(in);
        List<Set<String>> transactions = new ArrayList<>();
        for (CSVRecord record : records) {
            // Collect non-empty symptom fields into a transaction set
            Set<String> transaction = new HashSet<>();
            for (int i = 1; i <= 5; i++) {
                String symptom = record.get("symptom_" + i);
                if (symptom != null && !symptom.isEmpty()) {
                    transaction.add(symptom.trim());
                }
            }
            if (!transaction.isEmpty()) {
                transactions.add(transaction);
            }
        }

        long totalEndTime = System.currentTimeMillis();  // Total timer end
        double totalSeconds = (totalEndTime - totalStartTime) / 1000.0;
        System.out.printf("Total processing time to load transactions : %.2f seconds.%n", totalSeconds);
        System.out.println("Loaded " + transactions.size() + " transactions.");

        // Define thresholds
        double minSupport = 0.01;
        double minConfidence = 0.05;
        int minSupportCount = (int) Math.ceil(minSupport * transactions.size());


        long totalStartTimeApriori = System.currentTimeMillis();  // Total timer start


        // 2. Find frequent 1-itemsets (individual symptoms)
        Map<Set<String>, Integer> itemsetCounts = new HashMap<>();
        List<Set<String>> frequentItemsets = new ArrayList<>();
        // Count occurrences of each symptom
        Map<String, Integer> symptomCount = new HashMap<>();
        for (Set<String> trans : transactions) {
            for (String symptom : trans) {
                symptomCount.put(symptom, symptomCount.getOrDefault(symptom, 0) + 1);
            }
        }
        // Collect those meeting min support
        List<Set<String>> currentFrequent = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : symptomCount.entrySet()) {
            if (entry.getValue() >= minSupportCount) {
                Set<String> itemset = new HashSet<>(Collections.singleton(entry.getKey()));
                frequentItemsets.add(itemset);
                currentFrequent.add(itemset);
                itemsetCounts.put(itemset, entry.getValue());
            }
        }

        // 3. Iteratively find frequent k-itemsets for k=2,3,...
        int k = 2;
        while (!currentFrequent.isEmpty()) {
            List<Set<String>> candidates = new ArrayList<>();
            // Generate candidate k-itemsets by joining frequent (k-1)-itemsets
            int n = currentFrequent.size();
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    Set<String> set1 = currentFrequent.get(i);
                    Set<String> set2 = currentFrequent.get(j);
                    // Join step: union two itemsets if they differ in exactly one item
                    Set<String> union = new HashSet<>(set1);
                    union.addAll(set2);
                    if (union.size() == k) {
                        // Prune step: only consider this candidate if all (k-1)-subsets are frequent
                        boolean allSubsetsFrequent = true;
                        for (String item : union) {
                            Set<String> subset = new HashSet<>(union);
                            subset.remove(item);
                            if (!itemsetCounts.containsKey(subset)) {
                                allSubsetsFrequent = false;
                                break;
                            }
                        }
                        if (allSubsetsFrequent && !candidates.contains(union)) {
                            candidates.add(union);
                        }
                    }
                }
            }
            // Count support for candidate k-itemsets
            Map<Set<String>, Integer> candidateCount = new HashMap<>();
            for (Set<String> trans : transactions) {
                for (Set<String> cand : candidates) {
                    if (trans.containsAll(cand)) {
                        candidateCount.put(cand, candidateCount.getOrDefault(cand, 0) + 1);
                    }
                }
            }
            // Collect candidates that meet min support
            currentFrequent.clear();
            for (Map.Entry<Set<String>, Integer> entry : candidateCount.entrySet()) {
                if (entry.getValue() >= minSupportCount) {
                    frequentItemsets.add(entry.getKey());
                    currentFrequent.add(entry.getKey());
                    itemsetCounts.put(entry.getKey(), entry.getValue());
                }
            }
            k++;
        }

        // 4. Generate association rules from frequent itemsets
        List<String> strongRules = new ArrayList<>();
        for (Set<String> itemset : frequentItemsets) {
            if (itemset.size() < 2) continue;  // need at least 2 items for a rule
            int itemsetSupportCount = itemsetCounts.get(itemset);
            // Generate all non-empty proper subsets of the itemset as antecedents
            List<Set<String>> subsets = getNonEmptySubsets(itemset);
            for (Set<String> antecedent : subsets) {
                if (antecedent.equals(itemset)) continue;
                Set<String> consequent = new HashSet<>(itemset);
                consequent.removeAll(antecedent);
                if (consequent.isEmpty()) continue;
                // Calculate confidence = support(itemset) / support(antecedent)
                int antecedentCount = itemsetCounts.getOrDefault(antecedent, 0);
                if (antecedentCount == 0) continue;
                double confidence = (double) itemsetSupportCount / antecedentCount;
                if (confidence >= minConfidence) {
                    double support = (double) itemsetSupportCount / transactions.size();
                    strongRules.add(antecedent + " => " + consequent +
                            String.format(" (support=%.3f, confidence=%.3f)", support, confidence));
                }
            }
        }

        // 5. Output frequent itemsets
        System.out.println("\nFrequent itemsets (support ≥ " + minSupport + "):");
        for (Set<String> itemset : frequentItemsets) {
            double support = (double) itemsetCounts.get(itemset) / transactions.size();
            System.out.printf("  %s  (support = %.3f)\n", itemset, support);
        }
        // Output strong association rules
        System.out.println("\nStrong association rules (confidence ≥ " + minConfidence + "):");
        for (String rule : strongRules) {
            System.out.println("  " + rule);
        }

        long totalEndTimeApriori = System.currentTimeMillis();  // Total timer end
        double totalSecondsApriori = (totalEndTimeApriori - totalStartTimeApriori) / 1000.0;
        System.out.printf("Total processing time to complete Apriori: %.2f seconds.%n", totalSecondsApriori);
    }

    // Helper to get all non-empty subsets of a set (excluding the set itself)
    private static List<Set<String>> getNonEmptySubsets(Set<String> fullSet) {
        List<Set<String>> subsets = new ArrayList<>();
        List<String> items = new ArrayList<>(fullSet);
        int n = items.size();
        // Use bit mask from 1 to 2^n - 1 to generate subsets
        for (int mask = 1; mask < (1 << n); mask++) {
            Set<String> subset = new HashSet<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(items.get(i));
                }
            }
            if (!subset.isEmpty() && subset.size() < fullSet.size()) {
                subsets.add(subset);
            }
        }
        return subsets;
    }
}