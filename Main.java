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

    public static void project1() throws IOException {

        // Task 1 start
        // Open all 3 VAERS files for each year from 1990 to 2025 and prepare them to combine and save it in an output file
        for (int year =1990; year <= 2026; year++) {
            String dataFile = "src/dataset/" + year + "VAERSDATA.csv";
            String symptomFile = "src/dataset/" + year + "VAERSSYMPTOMS.csv";
            String vaxFile = "src/dataset/" + year + "VAERSVAX.csv";
            String outputFile = "src/output/VAERS_COVID_" + year + ".csv";
            if(year == 2026) {
                dataFile = "src/dataset/NonDomesticVAERSDATA.csv";
                symptomFile = "src/dataset/NonDomesticVAERSSYMPTOMS.csv";
                vaxFile = "src/dataset/NonDomesticVAERSVAX.csv";
                outputFile = "src/output/VAERS_COVID_NonDomestic.csv";
            }

            File dFile = new File(dataFile);
            File sFile = new File(symptomFile);
            File vFile = new File(vaxFile);

            if (!dFile.exists() || !sFile.exists() || !vFile.exists()) {
                System.out.println("Skipping missing year: " + year);
                continue;
            }

            // Step 1: Read Vaccine Data to find COVID records (get COVID VAERS_IDs) and COVID Vaccine data
            Map<String, String> vaxMap = new HashMap<>(); // Vax data
            Set<String> covidVaersIds = new HashSet<>(); // COVID Vax ID
            List<String> vaxHeaders = new ArrayList<>(); // Vax headers
            try (BufferedReader reader = new BufferedReader(new FileReader(vaxFile));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                for (String header : csvParser.getHeaderNames()){
                    if(!header.equalsIgnoreCase("VAERS_ID")){
                        vaxHeaders.add(header);
                    }
                }
                for (CSVRecord record : csvParser) {
                    if (record.get("VAX_TYPE").equalsIgnoreCase("COVID19")) {
                        covidVaersIds.add(record.get("VAERS_ID").trim());
                        vaxMap.put(record.get("VAERS_ID").trim(), String.join(",", record.toMap().values()));
                    }
                }
            }
            System.out.println("Found " + covidVaersIds.size() + " COVID-19 records in " + vaxFile);

            // Step 2: Load Symptoms Data
            Map<String, String> symptomsMap = new HashMap<>(); // Symptom data
            List<String> symptomHeaders = new ArrayList<>(); // Symptom header
            try (BufferedReader reader = new BufferedReader(new FileReader(symptomFile));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                for (String header : csvParser.getHeaderNames()) {
                    if (!header.equalsIgnoreCase("VAERS_ID")) {
                        symptomHeaders.add(header);
                    }
                }
                for (CSVRecord record : csvParser) {
                    String vaersId = record.get("VAERS_ID").trim();
                    if (covidVaersIds.contains(vaersId)) {
                        symptomsMap.put(vaersId, String.join(",", record.toMap().values()));
                    }
                }
            }

            // Step 3: Read VAERSDATA file into memory
            List<CSVRecord> dataRecords = new ArrayList<>(); // VAERSDATA data
            List<String> dataHeaders = new ArrayList<>(); // VAERSDATA header
            try (BufferedReader reader = new BufferedReader(new FileReader(dataFile));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

                dataHeaders.addAll(csvParser.getHeaderNames());
                for (CSVRecord record : csvParser) {
                    String vaersId = record.get("VAERS_ID").trim();
                    if (covidVaersIds.contains(vaersId)) {
                        dataRecords.add(record);
                    }
                }
            }

            if (dataRecords.isEmpty()) {
                System.out.println("No matching records found for year: " + year + ". Skipping CSV write.");
                Files.deleteIfExists(Paths.get(outputFile));
                continue;
            }


            // Step 4: Combine headers for the three files
            List<String> combinedHeaders = new ArrayList<>(dataHeaders);
            combinedHeaders.addAll(symptomHeaders);
            combinedHeaders.addAll(vaxHeaders);

            // Step 5: Write combined data to CSV
            int recordsWritten = 0;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                // Write header row
                csvPrinter.printRecord(combinedHeaders);

                // Write each record with combined details
                for (CSVRecord record : dataRecords) {
                    List<String> row = new ArrayList<>(record.toMap().values());
                    String vaersId = record.get("VAERS_ID").trim();

                    String symptomData = symptomsMap.get(vaersId);
                    if (symptomData != null) {
                        String[] symptomArray = symptomData.split(",", -1);
                        for (int i = 1; i < symptomArray.length; i++) { // Start from index 1 to skip the first element since the first element is the id and we already have the id from the data file
                            row.add(symptomArray[i]);
                        }
                    }else {
                        for (int i = 0; i < symptomHeaders.size(); i++) {
                            row.add(" "); // Add empty data if we dont have any symptom data for the id
                        }
                    }

                    String vaxData = vaxMap.get(vaersId);
                    if (vaxData != null) {
                        String[] vaxArray = vaxData.split(",", -1);
                        for (int i = 1; i < vaxArray.length; i++) {  // Start from index 1 to skip the first element since the first element is the id and we already have the id from the data file

                            row.add(vaxArray[i]);
                        }
                    } else {
                        for (int i = 0; i < vaxHeaders.size(); i++) {
                            row.add(" ");  // Add empty data if we dont have any vax data for the id
                        }
                    }
                    csvPrinter.printRecord(row);
                    recordsWritten++;
                }
            }

            if (recordsWritten == 0) {
                Files.deleteIfExists(Paths.get(outputFile));
                System.out.println("No data written for " + outputFile + ", file deleted.");
            } else {
                System.out.println("Saved " + recordsWritten + " records to " + outputFile);
            }
            if(year== 2026){
                System.out.println("Processed: Non Domestic");

            } else {
                System.out.println("Processed: " + year);
            }
        }

        System.out.println("Processing initiated for all years.");
        // Task 1 end


        // Task 2 start
        List<String[]> dataset = new ArrayList<>();

        // List headers for the required output file
        String[] headers = {"VAERS_ID", "AGE_YRS", "SEX", "VAX_NAME", "RPT_DATE", "SYMPTOM", "DIED", "DATEDIED", "SYMPTOM_TEXT"};

        // Open the created covid cases files in task one and merge into a single data
        for (int year =1990; year <= 2026; year++) {
            String filePath = "src/output/VAERS_COVID_" + year + ".csv";
            if(year == 2026){
                filePath = "src/output/VAERS_COVID_NonDomestic.csv";
            }
            File file = new File(filePath);
            if (!file.exists()) continue;
            System.out.println("Reading file: " + filePath);
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                for (CSVRecord record : csvParser) {
                    String vaersId = record.get("VAERS_ID").trim();
                    String age = record.get("AGE_YRS").trim();
                    String sex = record.get("SEX").trim();
                    String vaxName = record.get("VAX_NAME").trim();
                    String rptDate = record.get("RPT_DATE").trim();
                    String died = record.get("DIED").trim();
                    String dateDied = record.get("DATEDIED").trim();
                    String symptomText = record.get("SYMPTOM_TEXT").trim();

                    for (int i = 1; i <= 5; i++) {
                        String symptomKey = "SYMPTOM" + i;
                        if (record.isMapped(symptomKey) && record.get(symptomKey) != null && !record.get(symptomKey).isEmpty()) {
                            dataset.add(new String[]{vaersId, age, sex, vaxName, rptDate, record.get(symptomKey), died, dateDied, symptomText});
                        }
                    }
                }
            }
        }

        int totalCases = dataset.size();
        Set<String> uniqueCases= new HashSet<>();

        Set<String> uniqueDeathIds = new HashSet<>();

        for (String[] row : dataset) {
            String vaersId = row[0];
            uniqueCases.add(vaersId);
            String died = row[6];  // "DIED" column
            if (died.equalsIgnoreCase("Y")) {
                uniqueDeathIds.add(vaersId);  // Store unique VAERS_IDs for deaths
            }
        }

        int totalUniqueCases = uniqueCases.size();
        int totalDeaths = uniqueDeathIds.size(); // Count unique deaths

        System.out.println("\n=== Summary Before Sorting ===");
        System.out.println("Total Cases: " + totalUniqueCases);
        System.out.println("Total Deaths: " + totalDeaths);
        System.out.println("==============================");



        Scanner scanner = new Scanner(System.in);
        System.out.println("Select Sorting Algorithm:");
        System.out.println("1. QuickSort");
        System.out.println("2. MergeSort");
        System.out.println("3. InsertionSort");
        System.out.print("Enter choice (1-3): ");
        int choice = scanner.nextInt();

        String[][] dataArr = dataset.toArray(new String[0][0]);
        long startTime = System.nanoTime();

        // Apply sorting algorithm to the data array
        // Column index 0 means VAERS_ID
        // Index number corresponds to header index
        applySortingAlgorithm(dataArr, choice, 0);
        long endTime = System.nanoTime();

        // Calculating time required to complete the sorting process
        System.out.println("Sorting completed in " + (endTime - startTime) / 1_000_000 + " ms.");


        // Write sorted data by id to a file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("SYMPTOMDATA.csv"));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers))) {
            for (String[] row : dataArr) {
                csvPrinter.printRecord(row);
            }
        }
        System.out.println("Sorted data saved to SYMPTOMDATA.csv.");
        // Task 2 end

        // Task 3 start
        // Open the file created in task 2
        String task3InputFile = "SYMPTOMDATA.csv";
        List<String[]> task3Dataset = new ArrayList<>();
        Set<String> task3DeathSet = new HashSet<>();

        // Open the data and create an age group using the classifyAgeGroup method
        try (BufferedReader reader = new BufferedReader(new FileReader(task3InputFile));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : csvParser) {
                String vaersId = record.get("VAERS_ID").trim();
                String age = record.get("AGE_YRS").trim();
                String ageGroup = classifyAgeGroup(age);
                String sex = record.get("SEX").trim();
                String vaxName = record.get("VAX_NAME").trim();
                String rptDate = record.get("RPT_DATE").trim();
                String symptom = record.get("SYMPTOM").trim();
                String died = record.get("DIED").trim();
                String dateDied = record.get("DATEDIED").trim();
                String symptomText = record.get("SYMPTOM_TEXT").trim();

                task3Dataset.add(new String[]{vaersId, ageGroup, sex, vaxName, rptDate, symptom, died, dateDied, symptomText});

                if (died.equalsIgnoreCase("Y")) {
                    task3DeathSet.add(vaersId);
                }
            }
        }

        // Sort and print the death stats by the respective column_index (see header) / category
        // Print: true to print data and false to not.
        Collections.shuffle(task3Dataset, new Random());
        String[][] task3DataArr = task3Dataset.toArray(new String[0][0]);
        sortAndPrintGroupedStats(task3DataArr, choice, 1, "Age Group", true );

        sortAndPrintGroupedStats(task3DataArr, choice, 2, "Gender", true);

        sortAndPrintGroupedStats(task3DataArr, choice, 3, "Vaccine Name", true);

        sortAndPrintGroupedStats(task3DataArr, choice, 5, "Symptom", false);

        // Task 3 end


    }

    private static void applySortingAlgorithm(String[][] data, int choice, int columnIndex) {
        switch (choice) {
            case 1:
                quickSort(data, 0, data.length - 1, columnIndex);
                System.out.println("QuickSort applied.");
                break;
            case 2:
                mergeSort(data, 0, data.length - 1, columnIndex);
                System.out.println("MergeSort applied.");
                break;
            case 3:
                insertionSort(data, columnIndex);
                System.out.println("InsertionSort applied.");
                break;
            default:
                System.out.println("Invalid choice. Exiting the program");
        }
    }

    private static String classifyAgeGroup(String ageStr) {
        try {
            double age = Double.parseDouble(ageStr);
            if (age < 1) return "<1 year";
            else if (age <= 3) return "1-3 years";
            else if (age <= 11) return "4-11";
            else if (age <= 18) return "12-18";
            else if (age <= 30) return "19-30";
            else if (age <= 40) return "31-40";
            else if (age <= 50) return "41-50";
            else if (age <= 60) return "51-60";
            else if (age <= 70) return "61-70";
            else if (age <= 80) return "71-80";
            else return ">80";
        } catch (NumberFormatException e) {
            return "Unknown";
        }
    }


    private static void quickSort(String[][] data, int low, int high, int columnIndex) {
        if (low < high) {
            int pi = partition(data, low, high, columnIndex);
            quickSort(data, low, pi-1, columnIndex);
            quickSort(data, pi + 1, high, columnIndex);
        }
    }

    private static int partition(String[][] data, int low, int high, int columnIndex) {
        String pivot = data[(low + high) / 2][columnIndex]; // Middle pivot
        int left = low;
        int right = high;

        while (left <= right) {
            while (data[left][columnIndex].compareTo(pivot) < 0) {
                left++;
            }
            while (data[right][columnIndex].compareTo(pivot) > 0) {
                right--;
            }
            if (left <= right) {
                swap(data, left, right);
                left++;
                right--;
            }
        }
        return left;
    }


    private static void mergeSort(String[][] data, int left, int right, int columnIndex) {

        if (left < right) {
            int mid = left + (right - left) / 2;
            mergeSort(data, left, mid, columnIndex);
            mergeSort(data, mid + 1, right, columnIndex);
            merge(data, left, mid, right, columnIndex);
        }

    }

    private static void merge(String[][] data, int left, int mid, int right, int columnIndex) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        String[][] leftArray = new String[n1][];
        String[][] rightArray = new String[n2][];

        System.arraycopy(data, left, leftArray, 0, n1);
        System.arraycopy(data, mid + 1, rightArray, 0, n2);

        int i = 0, j = 0, k = left;
        while (i < n1 && j < n2) {
            if (leftArray[i][columnIndex].compareTo(rightArray[j][columnIndex]) <= 0) {
                data[k] = leftArray[i++];
            } else {
                data[k] = rightArray[j++];
            }
            k++;
        }

        while (i < n1) data[k++] = leftArray[i++];
        while (j < n2) data[k++] = rightArray[j++];
    }


    private static void insertionSort(String[][] data, int columnIndex) {

        int n = data.length;
        for (int i = 1; i < n; i++) {
            String[] key = data[i];
            int j = i - 1;
            while (j >= 0 && data[j][columnIndex].compareTo(key[0]) > 0) {
                data[j + 1] = data[j];
                j--;
            }
            data[j + 1] = key;
        }
    }

    private static void swap(String[][] data, int i, int j) {
        String[] temp = data[i];
        data[i] = data[j];
        data[j] = temp;
    }


    private static void sortAndPrintGroupedStats(String[][] data, int choice, int columnIndex, String category, boolean print) throws IOException {
        long startTime = System.nanoTime();
        applySortingAlgorithm(data, choice, columnIndex);
        long endTime = System.nanoTime();
        System.out.println("Sorting completed in " + (endTime - startTime) / 1_000_000 + " ms. for " + category);

        if(print) {
            Map<String, Set<String>> deathsByCategory = new LinkedHashMap<>();
            for (String[] row : data) {
                String key = row[columnIndex];
                String vaersId = row[0];
                String died = row[6];  // "DIED" column

                if (died.equalsIgnoreCase("Y")) {
                    deathsByCategory.computeIfAbsent(key, k -> new HashSet<>()).add(vaersId);
                }
            }

            System.out.println("\n=== Death Count by " + category + " (Sorted) ===");
            for (Map.Entry<String, Set<String>> entry : deathsByCategory.entrySet()) {
                System.out.println(entry.getKey() + " -> " + entry.getValue().size() + " deaths");
            }
            System.out.println("==============================");
        }
        System.out.println("Sorted data for category" + category);

//        String[] task3Headers = {"VAERS_ID", "AGE_GROUP", "SEX", "VAX_NAME", "RPT_DATE", "SYMPTOM", "DIED", "DATEDIED", "SYMPTOM_TEXT"};

//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("GROUPED_DATA_" + category + ".csv"));
//             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(task3Headers))) {
//            for (String[] row : data) {
//                csvPrinter.printRecord(row);
//            }
//        }
//        System.out.println("Sorted data saved to GROUPED_DATA_" + category + ".csv");
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Project");
        System.out.println("1.");
        System.out.println("2.");
        System.out.println("3.");
        System.out.print("Enter choice (1-3): ");
        int choice = scanner.nextInt();

        if(choice == 1) {
            project1();
        } else if(choice == 2) {
            project2();
        } else if(choice == 3) {
            project3();
        } else {
            System.out.println("Invalid choice");
        }
    }

    private static void project2() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter max degree of B+ Tree: ");
        int degree = scanner.nextInt();

        BPlusTree tree = new BPlusTree(degree);
        loadCSVIntoTree("src/output/VAERS_COVID_2020.csv", tree);
        loadCSVIntoTree("src/output/VAERS_COVID_2021.csv", tree);
        loadCSVIntoTree("src/output/VAERS_COVID_2022.csv", tree);
        loadCSVIntoTree("src/output/VAERS_COVID_2022.csv", tree);
        loadCSVIntoTree("src/output/VAERS_COVID_2023.csv", tree);
        loadCSVIntoTree("src/output/VAERS_COVID_2024.csv", tree);
        loadCSVIntoTree("src/output/VAERS_COVID_2025.csv", tree);
        System.out.println("All Data loaded into tree.");

        // Extra: load new 2025 updated data
        System.out.print("Do you want to load updated 2025 entries? (yes/no): ");
        String response = scanner.next().trim();
        if (response.equalsIgnoreCase("yes")) {
            insertNewEntries("src/output/dataToInsert.csv", tree);
            System.out.println("B+ Tree is now updated with new records.");
        } else {
            System.out.println("B+ Tree not updated");

        }

        tree.writeTreeStructureToFile("tree.txt");

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



    private static void project3() throws IOException {
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
        System.out.println("Loaded " + transactions.size() + " transactions.");

        // Define thresholds
        double minSupport = 0.01;
        double minConfidence = 0.20;
        int minSupportCount = (int) Math.ceil(minSupport * transactions.size());

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