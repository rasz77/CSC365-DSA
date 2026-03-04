package org.example;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter max degree of B+ Tree: ");
        int degree = scanner.nextInt();

        BPlusTree tree = new BPlusTree(degree);
        String[] files = {
                "src/outputfromProject1/VAERS_COVID_2020.csv",
                "src/outputfromProject1/VAERS_COVID_2021.csv",
                "src/outputfromProject1/VAERS_COVID_2022.csv",
                "src/outputfromProject1/VAERS_COVID_2022.csv",
                "src/outputfromProject1/VAERS_COVID_2023.csv",
                "src/outputfromProject1/VAERS_COVID_2024.csv",
                "src/outputfromProject1/VAERS_COVID_2025.csv"
        };

        long totalStart = System.currentTimeMillis();
        for (String file : files) {
            long start = System.currentTimeMillis();
            loadCSVIntoTree(file, tree);
            long end = System.currentTimeMillis();
            System.out.println("Time to load " + file + ": " + (end - start) + " ms");
        }
        long totalEnd = System.currentTimeMillis();
        System.out.println("All data loaded into tree in " + (totalEnd - totalStart) + " ms");


        // Extra: load new 2025 updated data
        System.out.print("Do you want to load updated 2025 entries? (yes/no): ");
        String response = scanner.next().trim();
        if (response.equalsIgnoreCase("yes")) {
            long insertStart = System.currentTimeMillis();
            insertNewEntries("src/dataToInsert.csv", tree);
            long insertEnd = System.currentTimeMillis();
            System.out.println("Update took " + (insertEnd - insertStart) + " ms");
            System.out.println("B+ Tree is now updated with new records.");
        } else {
            System.out.println("B+ Tree not updated");

        }

        System.out.println("Tree Height: " + tree.getHeight());   // assumes this method exists


        tree.writeTreeStructureToFile("tree.txt");
        System.out.println("Tree written to a file tree.txt");

        while (true) {
            System.out.print("\nSearch VAERS_ID (or type 'exit'): ");
            String input = scanner.next().trim();
            if (input.equalsIgnoreCase("exit")) break;
            String[] result = tree.search(input);
            if (result != null) {
                System.out.println("Record Found:");
                System.out.println(String.join(" | ", result));
            } else {
                System.out.println("Record not found.");
            }
        }
    }


    private static void loadCSVIntoTree(String path, BPlusTree tree) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("File not found: " + path);
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String key = record.get("VAERS_ID").trim();
                String[] data = new String[record.size()];
                for (int i = 0; i < record.size(); i++) {
                    data[i] = record.get(i).trim();
                }
                tree.insert(Integer.parseInt(key), data);
            }
        }
        System.out.println("Loaded: " + path);
    }

    private static void insertNewEntries(String newFile, BPlusTree tree) throws IOException {
        File file = new File(newFile);
        if (!file.exists()) {
            System.out.println("Updated 2025 file not found.");
            return;
        }
        int inserted = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String key = record.get("VAERS_ID").trim();
                int keyInt = Integer.parseInt(key);
                if (tree.search(keyInt) == false) {  // Only insert if not already present
                    String[] data = new String[record.size()];
                    for (int i = 0; i < record.size(); i++) {
                        data[i] = record.get(i).trim();
                    }
                    tree.insert(keyInt, data);
                    inserted++;
                }
            }
        }
        System.out.println("Inserted " + inserted + " new records from updated file.");
    }
}