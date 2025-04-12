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
package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TimeDuration implements Token{
    private final long time;
    private final String val;
    public TimeDuration(String data) {
        this.val = data.trim();
        this.time = parsetime(this.val);
    }
    public long gettime(){
        return time;
    }
    public String getvalue(){
        return val;
    }
    private long parsetime(String data){
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
            long factor = getTimeFactor(unit);
            return (long) (n * factor);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Format: " + data);
        }
    }
    private long getTimeFactor(String unit){
        switch(unit){
            case "ms": return 1L;                   
            case "s":  return 1_000L;               
            case "m":  return 60_000L;              
            case "h":  return 3_600_000L;          
            case "d":  return 86_400_000L;          
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
        json.addProperty("bytes", time);
        return json;
    }
    @Override
    public Object value(){
        return time;
    }
}
