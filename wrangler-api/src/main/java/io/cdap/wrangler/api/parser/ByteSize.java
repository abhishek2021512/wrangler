package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.cdap.wrangler.api.*;

public class ByteSize implements Token {
    private final long bytes;
    private final String val;

    public ByteSize(String data) {
        this.val = data.trim();
        this.bytes = parseByteSize(this.val);
    }

    public double getBytes() {
        return bytes;
    }

    public String getvalue() {
        return val;
    }

    private long parseByteSize(String data) {
        try {
            String num = data.replaceAll("[^0-9.]", "");
            String unit = data.replaceAll("[0-9.]", "").trim().toLowerCase();
            if (num.isEmpty() || unit.isEmpty()) {
                throw new IllegalArgumentException("Invalid Format: " + data);
            }
            double n = Double.parseDouble(num);
            if (n < 0) {
                throw new IllegalArgumentException("Negative values not allowed: " + data);
            }
            long factor = getByteFactor(unit);
            return (long) (n * factor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid format: " + data);
        }
    }

    private long getByteFactor(String unit) {
        switch (unit) {
            case "kb":
                return 1024L;
            case "mb":
                return 1024L * 1024L;
            case "gb":
                return 1024L * 1024L * 1024L;
            case "tb":
                return 1024L * 1024L * 1024L * 1024L;
            case "pb":
                return 1024L * 1024L * 1024L * 1024L * 1024L;
            default:
                throw new IllegalArgumentException("Unsupported unit: " + unit);
        }
    }

    public double toUnit(String unit){
        switch(unit.toLowerCase()){
            case "kb": return bytes / 1024.0;
            case "mb": return bytes / (1024.0 * 1024.0);
            case "gb": return bytes / (1024.0 * 1024.0 * 1024.0);
            case "tb": return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
            case "pb":return bytes/ (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0);
            default: throw new IllegalArgumentException("Unsupported unit: " + unit);
        }
    }
    @Override
    public TokenType type(){
        return TokenType.BYTE_SIZE;
    }
    @Override
    public JsonElement toJson(){
        JsonObject json=new JsonObject();
        json.addProperty("type", TokenType.BYTE_SIZE.name());
        json.addProperty("val", val);
        json.addProperty("bytes", bytes);
        return json;
    }
    @Override
    public Object value(){
        return bytes;
    }
}
