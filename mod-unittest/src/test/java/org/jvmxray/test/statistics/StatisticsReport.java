package org.jvmxray.test.statistics;

import java.util.*;

public class StatisticsReport {
    private Map<String, Integer> map;
    private Map<String, Integer> counts;
    private List<Integer> values;

    public StatisticsReport() {
        map = new HashMap<>();
        counts = new HashMap<>();
        values = new ArrayList<>();
    }

    public void add(String key) {
        counts.put(key, counts.getOrDefault(key, 0) + 1);
    }

    public void addRnd(int value) {
        values.add(value);
    }

    public String finish() {
        StringBuilder sb = new StringBuilder();
        int n = values.size();
        if (n == 0) {
            sb.append("No data was added.\n");
            return sb.toString();
        }

        // Calculate statistical results
        double mean = 0.0;
        for (int value : values) {
            mean += value;
        }
        mean /= n;

        Collections.sort(values);
        int medianIndex = n / 2;
        double median = (n % 2 == 0) ? (values.get(medianIndex - 1) + values.get(medianIndex)) / 2.0 : values.get(medianIndex);

        double stdDev = 0.0;
        for (int value : values) {
            stdDev += (value - mean) * (value - mean);
        }
        stdDev = Math.sqrt(stdDev / n);

        // Build statistical report
        sb.append("***DATA DISTRIBUTION STATISTICS***").append(System.lineSeparator());
        sb.append("Number of samples: ").append(n).append(System.lineSeparator());
        sb.append("Mean: ").append(mean).append(System.lineSeparator());
        sb.append("Median: ").append(median).append(System.lineSeparator());
        sb.append("Standard deviation: ").append(stdDev).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        // Build counts string
        sb.append("Counts:"+System.lineSeparator());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String key = entry.getKey();
            int count = entry.getValue();
            sb.append(key).append(": ").append(count).append(System.lineSeparator());
        }

        return sb.toString();
    }
}
