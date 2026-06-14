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

package com.webank.wedatasphere.qualitis.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.webank.wedatasphere.qualitis.constant.SpecCharEnum;
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import com.webank.wedatasphere.qualitis.project.request.CommonChecker;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all execution request DTOs.
 * Extracts the 17 common fields shared across ProjectExecutionRequest, RuleListExecutionRequest,
 * GroupExecutionRequest, GroupListExecutionRequest, DataSourceExecutionRequest, GeneralExecutionRequest.
 *
 * @author refactored
 */
public class BaseExecutionRequest {

    @JsonProperty("execution_user")
    private String executionUser;

    @JsonProperty("create_user")
    private String createUser;

    @JsonProperty("execution_param")
    private String executionParam;

    @JsonProperty("fps_file_id")
    private String fpsFileId;

    @JsonProperty("fps_hash")
    private String fpsHashValue;

    @JsonProperty("env_names")
    private String envNames;

    @JsonProperty("cluster_name")
    private String clusterName;

    @JsonProperty("startup_param_name")
    private String startupParamName;

    @JsonProperty("set_flag")
    private String setFlag;

    @JsonProperty("dynamic_partition_bool")
    private Boolean dyNamicPartition;

    @JsonProperty("dynamic_partition_prefix")
    private String dyNamicPartitionPrefix;

    @JsonProperty("bool_async")
    private Boolean async;

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    @JsonProperty("split_by")
    private String splitBy;

    @JsonProperty("engine_reuse")
    private Boolean engineReuse;

    public BaseExecutionRequest() {
        // Default Constructor
    }

    // ---- Common validation methods ----

    /**
     * Validate createUser and executionUser are not null/empty.
     */
    public void checkCommonFields() throws UnExpectedRequestException {
        CommonChecker.checkString(createUser, "Create_user");
        CommonChecker.checkString(executionUser, "Execution_user");
    }

    /**
     * Validate that executionParam and startupParamName do not contain duplicate variable names.
     */
    public void validateExecutionParams() throws UnExpectedRequestException {
        sameParameterVerificationMethod(executionParam,
            "{&EXECUTION_VARIABLES_HAVE_THE_SAME_VARIABLE_NAME}: ");
        sameParameterVerificationMethod(startupParamName,
            "{&DYNAMIC_ENGINE_HAVE_THE_SAME_VARIABLE_NAME}: ");
    }

    /**
     * Check for duplicate keys in a semicolon-separated key:value parameter string.
     * Supports both colon and equals as delimiters.
     */
    public static void sameParameterVerificationMethod(String executionParam, String abnormalInformation)
            throws UnExpectedRequestException {
        if (StringUtils.isNotBlank(executionParam)) {
            Map<String, String> map = new HashMap<>();
            for (String pair : executionParam.split(SpecCharEnum.DIVIDER.getValue())) {
                if (pair.contains(SpecCharEnum.COLON.getValue())) {
                    handleArrayData(abnormalInformation, map, pair, SpecCharEnum.COLON);
                } else if (pair.contains(SpecCharEnum.EQUAL.getValue())) {
                    handleArrayData(abnormalInformation, map, pair, SpecCharEnum.EQUAL);
                }
            }
        }
    }

    private static void handleArrayData(String abnormalInformation, Map<String, String> map,
            String pair, SpecCharEnum colon) throws UnExpectedRequestException {
        String[] parts = pair.split(colon.getValue());
        if (!map.containsKey(parts[0])) {
            map.put(parts[0], parts[1]);
        } else {
            throw new UnExpectedRequestException(abnormalInformation + parts[0]);
        }
    }

    // ---- Getters and Setters ----

    public String getExecutionUser() {
        return executionUser;
    }

    public void setExecutionUser(String executionUser) {
        this.executionUser = executionUser;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getExecutionParam() {
        return executionParam;
    }

    public void setExecutionParam(String executionParam) {
        this.executionParam = executionParam;
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

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getStartupParamName() {
        return startupParamName;
    }

    public void setStartupParamName(String startupParamName) {
        this.startupParamName = startupParamName;
    }

    public String getSetFlag() {
        return setFlag;
    }

    public void setSetFlag(String setFlag) {
        this.setFlag = setFlag;
    }

    public boolean getDyNamicPartition() {
        return dyNamicPartition != null ? dyNamicPartition : false;
    }

    public void setDyNamicPartition(boolean dyNamicPartition) {
        this.dyNamicPartition = dyNamicPartition;
    }

    public String getDyNamicPartitionPrefix() {
        return dyNamicPartitionPrefix;
    }

    public void setDyNamicPartitionPrefix(String dyNamicPartitionPrefix) {
        this.dyNamicPartitionPrefix = dyNamicPartitionPrefix;
    }

    public boolean getAsync() {
        return async != null ? async : false;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSplitBy() {
        return splitBy;
    }

    public void setSplitBy(String splitBy) {
        this.splitBy = splitBy;
    }

    public Boolean getEngineReuse() {
        return engineReuse;
    }

    public void setEngineReuse(Boolean engineReuse) {
        this.engineReuse = engineReuse;
    }
}
