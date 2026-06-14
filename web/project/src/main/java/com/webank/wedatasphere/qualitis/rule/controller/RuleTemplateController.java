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

package com.webank.wedatasphere.qualitis.rule.controller;

import com.webank.wedatasphere.qualitis.constants.ResponseStatusConstants;
import com.webank.wedatasphere.qualitis.exception.PermissionDeniedRequestException;
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import com.webank.wedatasphere.qualitis.request.PageRequest;
import com.webank.wedatasphere.qualitis.response.GeneralResponse;
import com.webank.wedatasphere.qualitis.response.GetAllResponse;
import com.webank.wedatasphere.qualitis.rule.request.AddRuleTemplateRequest;
import com.webank.wedatasphere.qualitis.rule.request.DeleteRuleTemplateRequest;
import com.webank.wedatasphere.qualitis.rule.request.ModifyRuleTemplateRequest;
import com.webank.wedatasphere.qualitis.rule.request.TemplatePageRequest;
import com.webank.wedatasphere.qualitis.rule.request.TemplatePullDownRequest;
import com.webank.wedatasphere.qualitis.rule.response.NamingConventionsResponse;
import com.webank.wedatasphere.qualitis.rule.response.RuleTemplatePlaceholderResponse;
import com.webank.wedatasphere.qualitis.rule.response.RuleTemplateResponse;
import com.webank.wedatasphere.qualitis.rule.response.TemplateInputDemandResponse;
import com.webank.wedatasphere.qualitis.rule.response.TemplateMetaResponse;
import com.webank.wedatasphere.qualitis.rule.service.RuleTemplateService;
import com.webank.wedatasphere.qualitis.util.RequestParametersUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * @author howeye
 */
@Path("api/v1/projector/rule_template")
public class RuleTemplateController {

    @Autowired
    private RuleTemplateService ruleTemplateService;

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleTemplateController.class);

    /**
     * Functional interface for controller actions that may throw checked exceptions.
     */
    @FunctionalInterface
    private interface ControllerAction<T> {
        GeneralResponse<T> execute() throws Exception;
    }

    /**
     * Unified exception handling for controller endpoints.
     * Rethrows UnExpectedRequestException and PermissionDeniedRequestException,
     * catches all other exceptions and returns a SERVER_ERROR response.
     */
    private <T> GeneralResponse<T> withExceptionHandling(String errorDescription, String errorMessageKey,
                                                         ControllerAction<T> action)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        try {
            return action.execute();
        } catch (UnExpectedRequestException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        } catch (PermissionDeniedRequestException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to {}, caused by: {}", errorDescription, e.getMessage(), e);
            return new GeneralResponse<>(ResponseStatusConstants.SERVER_ERROR, errorMessageKey, null);
        }
    }

    @POST
    @Path("multi/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<RuleTemplateResponse>> getMultiRuleTemplate(TemplatePageRequest request)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("find multi rule_template", "{&FAILED_TO_FIND_MULTI_RULE_TEMPLATE}",
                () -> {
                    RequestParametersUtils.transcoding(request);
                    return ruleTemplateService.getMultiRuleTemplate(request);
                });
    }

    @POST
    @Path("custom/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<RuleTemplateResponse>> getCustomRuleTemplateByUser(PageRequest request)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("find custom rule_template", "{&FAILED_TO_FIND_CUSTOM_RULE_TEMPLATE}",
                () -> ruleTemplateService.getCustomRuleTemplateByUser(request));
    }

    @POST
    @Path("default/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<RuleTemplateResponse>> getDefaultRuleTemplate(TemplatePageRequest request)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        request.checkRequest();
        return withExceptionHandling("find default rule_template", "{&FAILED_TO_FIND_DEFAULT_RULE_TEMPLATE}",
                () -> {
                    RequestParametersUtils.transcoding(request);
                    return ruleTemplateService.getDefaultRuleTemplate(request);
                });
    }

    @GET
    @Path("meta/{rule_template_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse<TemplateMetaResponse> getRuleTemplateMeta(@PathParam("rule_template_id") Long ruleTemplateId)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("get rule_template. rule_template_id: " + ruleTemplateId,
                "{&FAILED_TO_GET_RULE_TEMPLATE}",
                () -> ruleTemplateService.getRuleTemplateMeta(ruleTemplateId));
    }

    @GET
    @Path("meta_input/{rule_template_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse<TemplateInputDemandResponse> getRuleTemplateInputMeta(@PathParam("rule_template_id") Long ruleTemplateId)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("find the input of rule_template. rule_template_id: " + ruleTemplateId,
                "{&FAILED_TO_FIND_THE_INPUT_OF_RULE_TEMPLATE}",
                () -> ruleTemplateService.getRuleTemplateInputMeta(ruleTemplateId));
    }

    @POST
    @Path("default/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<RuleTemplateResponse> addDefaultRuleTemplate(AddRuleTemplateRequest request)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("add rule_template", "{&FAILED_TO_ADD_RULE_TEMPLATE}",
                () -> {
                    RequestParametersUtils.transcoding(request);
                    return new GeneralResponse<>(ResponseStatusConstants.OK, "{&ADD_RULE_TEMPLATE_SUCCESSFULLY}",
                            ruleTemplateService.addRuleTemplate(request));
                });
    }

    @POST
    @Path("default/modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<RuleTemplateResponse> modifyDefaultRuleTemplate(ModifyRuleTemplateRequest request)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("modify rule_template", "{&FAILED_TO_MODIFY_RULE_TEMPLATE}",
                () -> {
                    RequestParametersUtils.transcoding(request);
                    return new GeneralResponse<>(ResponseStatusConstants.OK, "{&MODIFY_RULE_TEMPLATE_SUCCESSFULLY}",
                            ruleTemplateService.modifyRuleTemplate(request));
                });
    }

    @POST
    @Path("default/delete/{template_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<RuleTemplateResponse>> deleteDefaultRuleTemplate(@PathParam("template_id") Long templateId)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("delete rule templates", "{&FAILED_TO_DELETE_RULE_TEMPLATE}",
                () -> {
                    ruleTemplateService.deleteRuleTemplate(templateId);
                    return new GeneralResponse<>(ResponseStatusConstants.OK, "{&DELETE_RULE_TEMPLATE_SUCCESSFULLY}", null);
                });
    }

    @POST
    @Path("delete/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<GetAllResponse<RuleTemplateResponse>> deleteAllRuleTemplate(DeleteRuleTemplateRequest request)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("delete all rule templates", "{&FAILED_TO_DELETE_RULE_TEMPLATE}",
                () -> {
                    DeleteRuleTemplateRequest.checkRequest(request);
                    for (Long templateId : request.getRuleTemplateIdList()) {
                        ruleTemplateService.deleteRuleTemplate(templateId);
                    }
                    return new GeneralResponse<>(ResponseStatusConstants.OK, "{&DELETE_RULE_TEMPLATE_SUCCESSFULLY}", null);
                });
    }

    @GET
    @Path("modify/detail/{template_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<RuleTemplateResponse> getModifyRuleTemplateDetail(@PathParam("template_id") Long templateId)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("get modify rule_template detail", "{&FAILED_TO_GET_RULE_TEMPLATE}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_RULE_TEMPLATE_SUCCESSFULLY}",
                        ruleTemplateService.getModifyRuleTemplateDetail(templateId)));
    }

    @POST
    @Path("option/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<Map<String, Object>>> getAllOptionListByProject(TemplatePullDownRequest request)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("query option list of template by project", "{&FAILED_TO_GET_RULE_TEMPLATE}",
                () -> {
                    List<Map<String, Object>> ruleTemplateResponses = ruleTemplateService.getAllTemplateInRule(request);
                    return new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_RULE_TEMPLATE_SUCCESSFULLY}",
                            ruleTemplateResponses);
                });
    }

    @GET
    @Path("user/option/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<Map<String, Object>>> getOptionListByUser(@QueryParam("template_type") Integer templateType)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("query option list of template", "{&FAILED_TO_GET_RULE_TEMPLATE}",
                () -> {
                    List<Map<String, Object>> ruleTemplateResponses = ruleTemplateService.getTemplateOptionList(templateType);
                    return new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_RULE_TEMPLATE_SUCCESSFULLY}",
                            ruleTemplateResponses);
                });
    }

    @POST
    @Path("check/level/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<Map<String, Object>>> getCheckLevelEnumn()
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("get check level enum", "{&FAILED_TO_CHECK_LEVEL_ENUMN}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_CHECK_LEVEL_ENUMN_SUCCESSFULLY}",
                        ruleTemplateService.getTemplateCheckLevelList()));
    }

    @POST
    @Path("check/type/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<Map<String, Object>>> getCheckTypeEnumn()
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("get check type enum", "{&FAILED_TO_CHECK_TYPE_ENUMN}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_CHECK_TYPE_ENUMN_SUCCESSFULLY}",
                        ruleTemplateService.getTemplateCheckTypeList()));
    }

    @POST
    @Path("file/type/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<Map<String, Object>>> getFileTypeEnumn()
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("get file type enum", "{&FAILED_TO_FILE_TYPE_ENUMN}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_FILE_TYPE_ENUMN_SUCCESSFULLY}",
                        ruleTemplateService.getTemplateFileTypeList()));
    }

    @POST
    @Path("statistical/function/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<Map<String, Object>>> getStatisticalFunctionEnumn()
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("get statistical function list", "{&FAILED_TO_STATISTICAL_FUNCTION_ENUMN}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_STATISTICAL_FUNCTION_ENUMN_SUCCESSFULLY}",
                        ruleTemplateService.getStatisticalFunctionList()));
    }

    @POST
    @Path("placeholder/list/{template_type}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<RuleTemplatePlaceholderResponse> getPlaceholderList(@PathParam("template_type") Integer templateType)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("get placeholder list", "{&FAILED_TO_GET_PLACEHOLDER_LIST}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_PLACEHOLDER_LIST_SUCCESSFULLY}",
                        ruleTemplateService.getPlaceholderData(templateType)));
    }

    @POST
    @Path("naming/method/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<Map<String, Object>>> getNamingMethodEnumn()
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("get naming method enum", "{&FAILED_TO_NAMING_METHOD_ENUMN}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_NAMING_METHOD_ENUMN_SUCCESSFULLY}",
                        ruleTemplateService.getNamingMethodList()));
    }

    @POST
    @Path("naming/conventions/config")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralResponse<List<NamingConventionsResponse>> getNamingConventionsConfig()
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        return withExceptionHandling("get naming conventions config", "{&FAILED_TO_NAMING_CONVENTIONS_CONFIG}",
                () -> new GeneralResponse<>(ResponseStatusConstants.OK, "{&GET_NAMING_CONVENTIONS_CONFIG_SUCCESSFULLY}",
                        ruleTemplateService.getConventionsNamingQuery()));
    }

}
