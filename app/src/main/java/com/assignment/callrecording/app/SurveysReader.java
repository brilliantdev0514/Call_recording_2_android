package com.assignment.callrecording.app;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SurveysReader {

    public enum Status {
        UNKNOWN, RED, YELLOW, GREEN
    }

    public static final int INDEX_TIMESTAMP = 0;
    public static final int INDEX_QUALITY = 1;
    public static final int INDEX_MSG = 11;

    public static final String QUALITY_LINE = "Voice Line Quality";
    public static final String QUALITY_MIC = "Mic Quality";
    public static final String QUALITY_TOTAL_SILENCE = "Total Silence";

    public ArrayList<CSVRecord> lines = new ArrayList<>();

    public static boolean filter(String[] filters, CSVRecord ss) {
        for (int i = 0; i < filters.length && i < ss.size(); i++) {
            String f = filters[i];
            if (f != null && !f.toLowerCase().equals(ss.get(i).toLowerCase()))
                return false;
        }
        return true;
    }

    public SurveysReader(InputStream is, String[] filters) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            CSVParser csvParser = new CSVParser(br, CSVFormat.DEFAULT);
            for (CSVRecord csvRecord : csvParser) {
                if (filter(filters, csvRecord))
                    lines.add(csvRecord);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public CSVRecord getApproved() { // get survey approved by mantainer
        for (CSVRecord r : lines) {
            String s = r.get(INDEX_TIMESTAMP);
            if (s == null || s.isEmpty())
                return r;
        }
        return null;
    }

    public Status getStatus(CSVRecord r) {
        String s = r.get(INDEX_QUALITY);
        if (s == null || s.isEmpty())
            return Status.UNKNOWN;
        if (s.equals(QUALITY_MIC))
            return Status.GREEN;
        if (s.equals(QUALITY_LINE))
            return Status.GREEN;
        if (s.equals(QUALITY_TOTAL_SILENCE))
            return Status.RED;
        return Status.YELLOW;
    }

    public Status getStatus() {
        if (lines.size() == 0)
            return Status.UNKNOWN;
        int good = 0;
        for (CSVRecord r : lines) {
            String s = r.get(INDEX_QUALITY);
            if (s != null && s.equals(QUALITY_LINE))
                good++;
            if (s != null && s.equals(QUALITY_MIC))
                good++;
        }
        if (good > lines.size())
            return Status.GREEN;
        if (good / (float) lines.size() < 0.3)
            return Status.RED;
        return Status.YELLOW;
    }
}
