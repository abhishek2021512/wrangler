/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.cdap.directives.aggregates;

import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.Collections;
import java.util.List;

@Categories(categories = {"aggregate"})
public class AggregateStatsDirective implements Directive {
    private static final String BYTE_TOTAL = "byteTotal";
    private static final String TIME_TOTAL = "timeTotal";
    private static final String ROW_COUNT = "rowCount";

    private String sizeSourceCol;
    private String timeSourceCol;
    private String sizeTargetCol;
    private String timeTargetCol;
    private String sizeUnit = "bytes";
    private String timeUnit = "milliseconds";
    private AggregationType aggType = AggregationType.SUM;

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-stats");
        // Required arguments: column names
        builder.define("size-source", TokenType.COLUMN_NAME);
        builder.define("time-source", TokenType.COLUMN_NAME);
        builder.define("size-target", TokenType.COLUMN_NAME);
        builder.define("time-target", TokenType.COLUMN_NAME);
        // Optional arguments: units and aggregation type
        builder.define("size-unit", TokenType.IDENTIFIER, true);
        builder.define("time-unit", TokenType.IDENTIFIER, true);
        builder.define("aggregation", TokenType.IDENTIFIER, true);
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) {
        this.sizeSourceCol = ((Text) args.value("size-source")).value();
        this.timeSourceCol = ((Text) args.value("time-source")).value();
        this.sizeTargetCol = ((Text) args.value("size-target")).value();
        this.timeTargetCol = ((Text) args.value("time-target")).value();

        if (args.contains("size-unit")) {
            this.sizeUnit = ((Text) args.value("size-unit")).value();
        }
        if (args.contains("time-unit")) {
            this.timeUnit = ((Text) args.value("time-unit")).value();
        }
        if (args.contains("aggregation")) {
            String agg = ((Text) args.value("aggregation")).value().toUpperCase();
            try {
                this.aggType = AggregationType.valueOf(agg);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid aggregation type: " + agg);
            }
        }
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) {
        TransientStore store = context.getTransientStore();
        long totalBytes = store.get(BYTE_TOTAL) instanceof Long ? (Long) store.get(BYTE_TOTAL) : 0L;
        long totalTime = store.get(TIME_TOTAL) instanceof Long ? (Long) store.get(TIME_TOTAL) : 0L;
        int count = store.get(ROW_COUNT) instanceof Integer ? (Integer) store.get(ROW_COUNT) : 0;

        // Process each row
        for (Row row : rows) {
            // Read byte size
            Object sizeValue = row.getValue(sizeSourceCol);
            if (sizeValue instanceof Long) {
                totalBytes += (Long) sizeValue; // Assuming bytes
            } else if (sizeValue != null) {
                System.err.println("Unexpected size value type: " + sizeValue.getClass());
            }

            // Read time duration
            Object timeValue = row.getValue(timeSourceCol);
            if (timeValue instanceof Long) {
                totalTime += (Long) timeValue; // Assuming milliseconds
            } else if (timeValue != null) {
                System.err.println("Unexpected time value type: " + timeValue.getClass());
            }
            count++;
        }

        // Update store
        store.set(TransientVariableScope.GLOBAL, BYTE_TOTAL, Long.valueOf(totalBytes));
        store.set(TransientVariableScope.GLOBAL, TIME_TOTAL, Long.valueOf(totalTime));
        store.set(TransientVariableScope.GLOBAL, ROW_COUNT, Integer.valueOf(count));

        // Finalization: Return result only if rows are empty (indicating final call)
        if (rows.isEmpty()) {
            return createResultRow(totalBytes, totalTime, count);
        }
        return Collections.emptyList();
    }

    private List<Row> createResultRow(long totalBytes, long totalTime, int count) {
        Row result = new Row();
        result.add(sizeTargetCol, convertBytes(totalBytes, sizeUnit));
        result.add(timeTargetCol, convertTime(totalTime, timeUnit, count));
        return Collections.singletonList(result);
    }

    private double convertTime(long millis, String unit, int count) {
        double value = millis;
        switch (unit.toLowerCase()) {
            case "seconds":
                value /= 1000.0;
                break;
            case "minutes":
                value /= (1000.0 * 60);
                break;
            case "hours":
                value /= (1000.0 * 60 * 60);
                break;
            case "milliseconds":
            default:
                break;
        }
        return aggType == AggregationType.AVG && count > 0 ? value / count : value;
    }

    private double convertBytes(long bytes, String unit) {
        switch (unit.toLowerCase()) {
            case "kb":
                return bytes / 1024.0;
            case "mb":
                return bytes / (1024.0 * 1024);
            case "gb":
                return bytes / (1024.0 * 1024 * 1024);
            case "tb":
                return bytes / (1024.0 * 1024 * 1024 * 1024);
            case "bytes":
            default:
                return bytes;
        }
    }

    enum AggregationType {
        SUM, AVG
    }
}