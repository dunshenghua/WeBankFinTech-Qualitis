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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.webank.wedatasphere.qualitis.concurrent.RuleContext;
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import com.webank.wedatasphere.qualitis.rule.entity.Rule;
import com.webank.wedatasphere.qualitis.rule.response.RuleResponse;

import java.util.*;

/**
 * @author howeye
 */
public class RuleListExecutionRequest extends AbstractExecutionRequest {
    @JsonProperty("rule_list")
    private List<Long> ruleList;

    @JsonIgnore
    private List<Rule> executableRuleList;
    @JsonIgnore
    private RuleContext ruleContext;

    public RuleListExecutionRequest() {
        // Default Constructor
    }

    public RuleListExecutionRequest(Long ruleId, String createUser, String executionUser) {
        ruleList = new ArrayList<>(1);
        ruleList.add(ruleId);
        setCreateUser(createUser);
        setExecutionUser(executionUser);
    }

    public RuleListExecutionRequest(RuleResponse ruleResponse, String createUser, String executionUser) {
        ruleList = new ArrayList<>(1);
        ruleList.add(ruleResponse.getRuleId());
        if (Objects.nonNull(ruleResponse.getRule())) {
            executableRuleList = Lists.newArrayListWithExpectedSize(1);
            executableRuleList.add(ruleResponse.getRule());
        }
        setCreateUser(createUser);
        setExecutionUser(executionUser);
    }

    public RuleContext getRuleContext() {
        return ruleContext;
    }

    public void setRuleContext(RuleContext ruleContext) {
        this.ruleContext = ruleContext;
    }

    public List<Rule> getExecutableRuleList() {
        return executableRuleList;
    }

    public void setExecutableRuleList(List<Rule> executableRuleList) {
        this.executableRuleList = executableRuleList;
    }

    public List<Long> getRuleList() {
        return ruleList;
    }

    public void setRuleList(List<Long> ruleList) {
        this.ruleList = ruleList;
    }

    public static void checkRequest(RuleListExecutionRequest request) throws UnExpectedRequestException {
        checkCommonFields(request);
        if (null == request.getRuleList() || request.getRuleList().isEmpty()) {
            throw new UnExpectedRequestException("Rules can not be null or empty");
        }
    }
}
