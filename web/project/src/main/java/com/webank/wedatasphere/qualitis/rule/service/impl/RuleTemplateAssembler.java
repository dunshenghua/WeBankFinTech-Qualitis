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

package com.webank.wedatasphere.qualitis.rule.service.impl;

import com.webank.wedatasphere.qualitis.constants.QualitisConstants;
import com.webank.wedatasphere.qualitis.rule.constant.FunctionTypeEnum;
import com.webank.wedatasphere.qualitis.rule.entity.Template;
import com.webank.wedatasphere.qualitis.rule.request.AddRuleTemplateRequest;
import org.apache.commons.lang.StringUtils;

/**
 * Stateless helper for assembling Template entities from request DTOs.
 * Package-private — not Spring-managed. All methods are static pure functions.
 */
final class RuleTemplateAssembler {

    private RuleTemplateAssembler() {
        // prevent instantiation
    }

    /**
     * Populates the common Template fields shared between add and modify operations.
     * Fields unique to add (clusterNum, createUser, createTime, importExportName) or
     * modify (modifyUser, modifyTime) should be set by the caller before or after this call.
     */
    static void populateTemplateFromRequest(Template template, AddRuleTemplateRequest request) {
        template.setName(request.getTemplateName());
        template.setActionType(request.getActionType());
        template.setMidTableAction(request.getMidTableAction());
        template.setTemplateType(request.getTemplateType());
        template.setDevDepartmentName(request.getDevDepartmentName());
        template.setOpsDepartmentName(request.getOpsDepartmentName());
        template.setDevDepartmentId(request.getDevDepartmentId());
        template.setOpsDepartmentId(request.getOpsDepartmentId());
        template.setEnName(request.getEnName());
        template.setDescription(request.getDescription());
        template.setVerificationLevel(request.getVerificationLevel());
        template.setVerificationType(request.getVerificationType());
        template.setSaveMidTable(request.getSaveMidTable() != null ? request.getSaveMidTable() : false);
        template.setFilterFields(request.getFilterFields() != null ? request.getFilterFields() : false);
        template.setWhetherUsingFunctions(request.getWhetherUsingFunctions() != null ? request.getWhetherUsingFunctions() : false);
        template.setVerificationCnName(request.getVerificationCnName());
        template.setVerificationEnName(request.getVerificationEnName());
        template.setNamingMethod(request.getNamingMethod());
        template.setWhetherSolidification(request.getWhetherSolidification() != null ? request.getWhetherSolidification() : false);
        template.setCheckTemplate(request.getCheckTemplate());
        template.setMajorType(request.getMajorType());
        template.setTemplateNumber(request.getTemplateNumber());
        template.setCustomZhCode(request.getCustomZhCode());
    }

    /**
     * Generates the showSql string from the mid-table action and count function config.
     *
     * @return the showSql string, or null if countFunctionName is blank
     */
    static String buildShowSql(String midTableAction, String countFunctionName, String countFunctionAlias) {
        if (StringUtils.isBlank(countFunctionName)) {
            return null;
        }
        String value = "";
        if (FunctionTypeEnum.COUNT_FUNCTION.getFunction().equals(countFunctionName)) {
            value = "*";
        } else if (StringUtils.isNotBlank(countFunctionAlias)) {
            value = countFunctionAlias;
        }
        return midTableAction.replaceFirst(QualitisConstants.ASTERISK, countFunctionName + "(" + value + ")");
    }
}
