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
import com.webank.wedatasphere.qualitis.project.request.CommonChecker;

/**
 * @author howeye
 */
public class GroupExecutionRequest extends AbstractExecutionRequest {
    @JsonProperty("group_id")
    private Long groupId;

    @JsonProperty("node_name")
    private String nodeName;

    public GroupExecutionRequest() {
    }

    public GroupExecutionRequest(Long groupId, String executionUser, String createUser, String nodeName) {
        this.groupId = groupId;
        setExecutionUser(executionUser);
        setCreateUser(createUser);
        this.nodeName = nodeName;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public static void checkRequest(GroupExecutionRequest request) throws UnExpectedRequestException {
        checkCommonFields(request);
        CommonChecker.checkObject(request.getGroupId(), "Group Id");
    }

    @Override
    public String toString() {
        return "GroupExecutionRequest{" +
            "groupId=" + groupId +
            ", executionParam='" + getExecutionParam() + '\'' +
            ", executionUser='" + getExecutionUser() + '\'' +
            ", createUser='" + getCreateUser() + '\'' +
            ", nodeName='" + nodeName + '\'' +
            ", fpsFileId='" + getFpsFileId() + '\'' +
            ", fpsHashValue='" + getFpsHashValue() + '\'' +
            ", clusterName='" + getClusterName() + '\'' +
            ", startupParamName='" + getStartupParamName() + '\'' +
            ", setFlag='" + getSetFlag() + '\'' +
            ", dyNamicPartition=" + getDyNamicPartition() +
            ", dyNamicPartitionPrefix='" + getDyNamicPartitionPrefix() + '\'' +
            ", async=" + getAsync() +
            ", startTime='" + getStartTime() + '\'' +
            ", endTime='" + getEndTime() + '\'' +
            ", splitBy='" + getSplitBy() + '\'' +
            ", engineReuse=" + getEngineReuse() +
            '}';
    }
}
