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
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;

import java.util.List;

/**
 * @author howeye
 */
public class GeneralExecutionRequest extends AbstractExecutionRequest {
    @JsonProperty("project_id")
    private Long projectId;

    @JsonProperty("group_id")
    private Long groupId;

    @JsonProperty("rule_list")
    private List<Long> ruleList;

    @JsonProperty(value = "project_name")
    private String projectName;
    @JsonProperty(value = "group_name")
    private String ruleGroupName;
    @JsonProperty(value = "rule_name_list")
    private List<String> ruleNameList;

    @JsonProperty("cross_table")
    private Boolean crossTable;
    private String database;
    private String cluster;
    private String table;

    @JsonProperty("node_name")
    private String nodeName;

    public GeneralExecutionRequest() {
        this.crossTable = false;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public List<Long> getRuleList() {
        return ruleList;
    }

    public void setRuleList(List<Long> ruleList) {
        this.ruleList = ruleList;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getRuleGroupName() {
        return ruleGroupName;
    }

    public void setRuleGroupName(String ruleGroupName) {
        this.ruleGroupName = ruleGroupName;
    }

    public List<String> getRuleNameList() {
        return ruleNameList;
    }

    public void setRuleNameList(List<String> ruleNameList) {
        this.ruleNameList = ruleNameList;
    }

    public Boolean getCrossTable() {
        return crossTable;
    }

    public void setCrossTable(Boolean crossTable) {
        this.crossTable = crossTable;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public static void checkRequest(GeneralExecutionRequest request) throws UnExpectedRequestException {
        checkCommonFields(request);
    }

    @Override
    public String toString() {
        return "GeneralExecutionRequest{" +
            "projectId=" + projectId +
            ", groupId=" + groupId +
            ", ruleList=" + ruleList +
            ", executionUser='" + getExecutionUser() + '\'' +
            ", executionParam='" + getExecutionParam() + '\'' +
            ", createUser='" + getCreateUser() + '\'' +
            ", projectName='" + projectName + '\'' +
            ", ruleNameList=" + ruleNameList +
            ", crossTable=" + crossTable +
            ", database='" + database + '\'' +
            ", cluster='" + cluster + '\'' +
            ", table='" + table + '\'' +
            ", nodeName='" + nodeName + '\'' +
            ", fpsHashValue='" + getFpsHashValue() + '\'' +
            ", fpsFileId='" + getFpsFileId() + '\'' +
            ", clusterName='" + getClusterName() + '\'' +
            ", startupParamName='" + getStartupParamName() + '\'' +
            ", setFlag='" + getSetFlag() + '\'' +
            ", dyNamicPartition=" + getDyNamicPartition() +
            ", dyNamicPartitionPrefix='" + getDyNamicPartitionPrefix() + '\'' +
            ", async=" + getAsync() +
            '}';
    }
}
