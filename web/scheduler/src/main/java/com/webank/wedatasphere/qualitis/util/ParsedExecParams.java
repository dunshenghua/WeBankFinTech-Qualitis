/*
 * Copyright 2019 WeBank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.qualitis.util;

import java.util.Map;

/**
 * Result object from {@link ExecutionParamParser#parse}.
 * Contains all parsed execution parameter fields after the unified parsing pipeline.
 *
 * @author refactored
 */
public class ParsedExecParams {

    /** Cleaned executionParam (after set_flag and fps entries are extracted) */
    private String executionParam;

    /** Merged setFlag string */
    private String setFlag;

    /** Extracted or original fpsFileId */
    private String fpsFileId;

    /** Extracted or original fpsHashValue */
    private String fpsHashValue;

    /** Extracted or original envNames */
    private String envNames;

    /** Parsed partition expression */
    private StringBuilder partition;

    /** Parsed run_date value */
    private StringBuilder runDate;

    /** Parsed run_today value */
    private StringBuilder runToday;

    /** Parsed split_by value */
    private StringBuilder splitBy;

    /** Map of all key:value pairs from the final executionParam */
    private Map<String, String> execParamMap;

    public ParsedExecParams() {
    }

    public String getExecutionParam() {
        return executionParam;
    }

    public void setExecutionParam(String executionParam) {
        this.executionParam = executionParam;
    }

    public String getSetFlag() {
        return setFlag;
    }

    public void setSetFlag(String setFlag) {
        this.setFlag = setFlag;
    }

    public String getFpsFileId() {
        return fpsFileId;
    }

    public void setFpsFileId(String fpsFileId) {
        this.fpsFileId = fpsFileId;
    }

    public String getFpsHashValue() {
        return fpsHashValue;
    }

    public void setFpsHashValue(String fpsHashValue) {
        this.fpsHashValue = fpsHashValue;
    }

    public String getEnvNames() {
        return envNames;
    }

    public void setEnvNames(String envNames) {
        this.envNames = envNames;
    }

    public StringBuilder getPartition() {
        return partition;
    }

    public void setPartition(StringBuilder partition) {
        this.partition = partition;
    }

    public StringBuilder getRunDate() {
        return runDate;
    }

    public void setRunDate(StringBuilder runDate) {
        this.runDate = runDate;
    }

    public StringBuilder getRunToday() {
        return runToday;
    }

    public void setRunToday(StringBuilder runToday) {
        this.runToday = runToday;
    }

    public StringBuilder getSplitBy() {
        return splitBy;
    }

    public void setSplitBy(StringBuilder splitBy) {
        this.splitBy = splitBy;
    }

    public Map<String, String> getExecParamMap() {
        return execParamMap;
    }

    public void setExecParamMap(Map<String, String> execParamMap) {
        this.execParamMap = execParamMap;
    }
}
