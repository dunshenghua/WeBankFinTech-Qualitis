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

package com.webank.wedatasphere.qualitis.project.service.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Sheet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.JsonPath;
import com.webank.wedatasphere.qualitis.config.LinkisConfig;
import com.webank.wedatasphere.qualitis.constant.SpecCharEnum;
import com.webank.wedatasphere.qualitis.constants.QualitisConstants;
import com.webank.wedatasphere.qualitis.constants.ResponseStatusConstants;
import com.webank.wedatasphere.qualitis.dao.*;
import com.webank.wedatasphere.qualitis.entity.ClusterInfo;
import com.webank.wedatasphere.qualitis.entity.RuleMetric;
import com.webank.wedatasphere.qualitis.entity.User;
import com.webank.wedatasphere.qualitis.entity.UserRole;
import com.webank.wedatasphere.qualitis.exception.PermissionDeniedRequestException;
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import com.webank.wedatasphere.qualitis.metadata.client.LinkisMetaDataManager;
import com.webank.wedatasphere.qualitis.metadata.client.MetaDataClient;
import com.webank.wedatasphere.qualitis.metadata.client.OperateCiService;
import com.webank.wedatasphere.qualitis.metadata.exception.MetaDataAcquireFailedException;
import com.webank.wedatasphere.qualitis.metadata.request.LinkisConnectParamsRequest;
import com.webank.wedatasphere.qualitis.metadata.request.LinkisDataSourceEnvRequest;
import com.webank.wedatasphere.qualitis.metadata.request.LinkisDataSourceRequest;
import com.webank.wedatasphere.qualitis.metadata.request.ModifyDataSourceParameterRequest;
import com.webank.wedatasphere.qualitis.metadata.response.datasource.LinkisDataSourceInfoDetail;
import com.webank.wedatasphere.qualitis.metadata.response.datasource.LinkisDataSourceParamsResponse;
import com.webank.wedatasphere.qualitis.project.constant.*;
import com.webank.wedatasphere.qualitis.project.dao.ProjectDao;
import com.webank.wedatasphere.qualitis.project.dao.ProjectLabelDao;
import com.webank.wedatasphere.qualitis.project.dao.ProjectUserDao;
import com.webank.wedatasphere.qualitis.project.dao.repository.ProjectRepository;
import com.webank.wedatasphere.qualitis.project.entity.Project;
import com.webank.wedatasphere.qualitis.project.entity.ProjectLabel;
import com.webank.wedatasphere.qualitis.project.entity.ProjectUser;
import com.webank.wedatasphere.qualitis.project.excel.*;
import com.webank.wedatasphere.qualitis.project.request.DiffVariableRequest;
import com.webank.wedatasphere.qualitis.project.request.DownloadProjectRequest;
import com.webank.wedatasphere.qualitis.project.request.UploadProjectRequest;
import com.webank.wedatasphere.qualitis.project.service.ProjectBatchService;
import com.webank.wedatasphere.qualitis.project.service.ProjectEventService;
import com.webank.wedatasphere.qualitis.project.service.ProjectService;
import com.webank.wedatasphere.qualitis.response.GeneralResponse;
import com.webank.wedatasphere.qualitis.rule.constant.DynamicEngineEnum;
import com.webank.wedatasphere.qualitis.rule.constant.TableDataTypeEnum;
import com.webank.wedatasphere.qualitis.rule.dao.*;
import com.webank.wedatasphere.qualitis.rule.dao.repository.DiffVariableRepository;
import com.webank.wedatasphere.qualitis.rule.entity.*;
import com.webank.wedatasphere.qualitis.rule.excel.ExcelRuleListener;
import com.webank.wedatasphere.qualitis.rule.request.GetLinkisDataSourceEnvRequest;
import com.webank.wedatasphere.qualitis.rule.service.*;
import com.webank.wedatasphere.qualitis.rule.constant.RuleTypeEnum;
import com.webank.wedatasphere.qualitis.service.DataVisibilityService;
import com.webank.wedatasphere.qualitis.service.FileService;
import com.webank.wedatasphere.qualitis.service.RoleService;
import com.webank.wedatasphere.qualitis.service.SubDepartmentPermissionService;
import com.webank.wedatasphere.qualitis.util.DateUtils;
import com.webank.wedatasphere.qualitis.util.HttpUtils;
import com.webank.wedatasphere.qualitis.util.UuidGenerator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author howeye
 */
@Service
public class ProjectBatchServiceImpl implements ProjectBatchService {
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private RuleDataSourceMappingDao ruleDataSourceMappingDao;
    @Autowired
    private DiffVariableRepository diffVariableRepository;
    @Autowired
    private RuleDatasourceEnvDao ruleDatasourceEnvDao;
    @Autowired
    private RuleDataSourceDao ruleDataSourceDao;
    @Autowired
    private ClusterInfoDao clusterInfoDao;
    @Autowired
    private RuleGroupDao ruleGroupDao;
    @Autowired
    private ProjectDao projectDao;
    @Autowired
    private RuleDao ruleDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private UserRoleDao userRoleDao;
    @Autowired
    private RuleMetricDao ruleMetricDao;
    @Autowired
    private ProjectUserDao projectUserDao;
    @Autowired
    private ProjectLabelDao projectLabelDao;
    @Autowired
    private ExecutionVariableDao executionVariableDao;
    @Autowired
    private ExecutionParametersDao executionParametersDao;
    @Autowired
    private StandardValueVersionDao standardValueVersionDao;
    @Autowired
    private StaticExecutionParametersDao staticExecutionParametersDao;
    @Autowired
    private NoiseEliminationManagementDao noiseEliminationManagementDao;
    @Autowired
    private AlarmArgumentsExecutionParametersDao alarmArgumentsExecutionParametersDao;
    @Autowired
    private RuleMetricDepartmentUserDao ruleMetricDepartmentUserDao;

    @Autowired
    private SubDepartmentPermissionService subDepartmentPermissionService;
    @Autowired
    private ExecutionParametersService executionParametersService;
    @Autowired
    private DataVisibilityService dataVisibilityService;
    @Autowired
    private ProjectEventService projectEventService;
    @Autowired
    private RuleBatchService ruleBatchService;
    @Autowired
    private OperateCiService operateCiService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private MetaDataClient metaDataClient;
    @Autowired
    private FileService fileService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private LinkisConfig linkisConfig;
    @Autowired
    private RuleTemplateDao ruleTemplateDao;
    @Autowired
    private TemplateMidTableInputMetaService templateMidTableInputMetaService;
    @Autowired
    private TemplateStatisticsInputMetaService templateStatisticsInputMetaService;
    @Autowired
    private TemplateOutputMetaDao templateOutputMetaDao;
    @Autowired
    private DataVisibilityDao dataVisibilityDao;
    @Autowired
    private LinkisMetaDataManager linkisMetaDataManager;
    @Autowired
    private LinkisDataSourceService linkisDataSourceService;
    @Autowired
    private LinkisDataSourceEnvService linkisDataSourceEnvService;
    @Autowired
    private ConfigurableEnvironment environment;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectBatchServiceImpl.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private HttpServletRequest httpServletRequest;

    public ProjectBatchServiceImpl(@Context HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    private GeneralResponse uploadProjectsReal(ExcelProjectListener listener, String userName, boolean aomp, Long updateProjectId
            , List<DiffVariableRequest> diffVariableRequestList, List<File> udfFiles, StringBuilder operateComment, Boolean increment) throws UnExpectedRequestException, PermissionDeniedRequestException, IOException, JSONException, MetaDataAcquireFailedException, ParseException {

        if (userName == null) {
            return new GeneralResponse<>("401", "{&PLEASE_LOGIN}", null);
        }

        User user = userDao.findByUsername(userName);

        if (user == null) {
            return new GeneralResponse<>("401", "{&PLEASE_LOGIN}", null);
        }

        // Check if excel file is empty
        if (listener.getExcelProjectContent().isEmpty() && listener.getExcelMetricContent().isEmpty() && listener.getExcelExecutionParametersContent().isEmpty() && listener.getExcelGroupByProjects().isEmpty()) {
            throw new UnExpectedRequestException("{&FILE_CAN_NOT_BE_EMPTY_OR_FILE_CAN_NOT_BE_RECOGNIZED}", 422);
        }

        updateProjectId = handleProject(user, listener.getExcelProjectContent(), updateProjectId, aomp);

        handleExecutionParameters(listener.getExcelExecutionParametersContent(), userName, updateProjectId);
        Map<Long, List<Long>> oldIdAndNewEnvIdsMapHook = new HashMap<>();
        executeHandler("handleMetricData", operateComment, () -> handleMetricData(user, listener.getExcelMetricContent()));
        executeHandler("handleDataSource", operateComment, () -> {
            List<DiffVariableRequest> datasourceEnvDiffVariableRequestList = diffVariableRequestList.stream().filter(
                diffVariableRequest -> DiffRequestTypeEnum.DATASOURCE_ENV.getCode().equals(diffVariableRequest.getType())).collect(Collectors.toList());
            handleDataSource(user, listener.getExcelDatasourceEnvContent(), datasourceEnvDiffVariableRequestList, oldIdAndNewEnvIdsMapHook);
        });
        executeHandler("handleStandardValue", operateComment, () -> handleStandardValue(user, listener.getExcelStandardVauleContent()));

        replaceEnvsInRuleDataSource(listener, oldIdAndNewEnvIdsMapHook);

        // Create rules according to excel sheet
        List<ExcelRuleByProject> excelRuleByProject = listener.getExcelRuleContent();

        if (updateProjectId != null) {

            List<Project> projectList = new ArrayList<>();
            Project projectInDb = projectDao.findById(updateProjectId);
            if (projectInDb == null) {
                throw new UnExpectedRequestException("Project: [ID=" + updateProjectId + "] {&DOES_NOT_EXIST}");
            }

            projectList.add(projectInDb);
            try {
                handleTableGroup(listener, updateProjectId, diffVariableRequestList);

                List<String> newRuleNames = new ArrayList<>();
                ruleBatchService.getAndSaveRule(excelRuleByProject, projectInDb, newRuleNames, userName, diffVariableRequestList);

                if (Boolean.FALSE.equals(increment)) {
                    // No increment, disable rules not be modified.
                    List<Rule> rules = ruleDao.findByProject(projectInDb);
                    rules = rules.stream().filter(rule -> ! newRuleNames.contains(rule.getName())).map(rule -> {
                        rule.setEnable(Boolean.FALSE);
                        return rule;
                    }).collect(Collectors.toList());
                    ruleDao.saveRules(rules);
                }
            } catch (Exception e) {
                LOGGER.error("uploadProjectsReal, failed to save rules", e);
                operateComment.append(SpecCharEnum.LINE.getValue()).append(e.getMessage());
            }

            projectEventService.recordBatch(projectList, userName, operateComment.append(SpecCharEnum.LINE.getValue()).toString(), OperateTypeEnum.IMPORT_PROJECT);
            // update the running status of the project to normal
            for (Project project : projectList) {
                project.setRunStatus(ProjectStatusEnum.OPERABLE_STATUS.getCode());
                projectDao.saveProject(project);
            }
        }

        return new GeneralResponse<>(ResponseStatusConstants.OK, "{&SUCCEED_TO_UPLOAD_FILE}", null);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private void executeHandler(String handlerName, StringBuilder operateComment, ThrowingRunnable handler) {
        try {
            handler.run();
        } catch (Exception e) {
            LOGGER.error("{} of uploadProjectsReal, failed message: {}", handlerName, e.getMessage(), e);
            operateComment.append(SpecCharEnum.LINE.getValue()).append(e.getMessage());
        }
    }

    /**
     * Conflict: The env_name, composed of dcn fields(vip、port), was inconsistent between dev/sit/uat and prod.
     * Target: To re-generate RuleDataSourceEnv with new LinkisDataSourceEnv, then override the original RuleDataSourceEnv.
     * Steps:
     * If the input_type of LinkisDataSource was 1(manual input), skip.
     * If the env_names were chosen by dcn_num or logic_area way, then to replace them with re-generating env_names, and write back in the ExcelProjectListener.ExcelRuleContent.
     * If the env_names were chosen directly, set them to empty and write back in the ExcelProjectListener.ExcelRuleContent.
     * @param listener
     * @param oldIdAndNewEnvIdsMapHook The argument from handleDataSource(). key: old linkisDataSourceId from export environment, value: new env_ids written into database in handleDataSource()
     */
    private void replaceEnvsInRuleDataSource(ExcelProjectListener listener, Map<Long, List<Long>> oldIdAndNewEnvIdsMapHook) throws IOException {
        LOGGER.info("Ready to replace envs in the RuleDataSource: {}", objectMapper.writeValueAsString(oldIdAndNewEnvIdsMapHook));
        if (MapUtils.isEmpty(oldIdAndNewEnvIdsMapHook)) {
            return;
        }
        List<ExcelRuleByProject> excelRuleByProjects = listener.getExcelRuleContent();
        for (ExcelRuleByProject excelRuleByProject: excelRuleByProjects) {
                Rule rule = objectMapper.readValue(excelRuleByProject.getRuleJsonObject(), Rule.class);
                Iterator<RuleDataSource> ruleDataSourceIterator = rule.getRuleDataSources().iterator();
                while (ruleDataSourceIterator.hasNext()) {
                    RuleDataSource ruleDataSource = ruleDataSourceIterator.next();
                    if (StringUtils.isBlank(ruleDataSource.getLinkisDataSourceName())) {
                        continue;
                    }
                    LinkisDataSource linkisDataSourceInDb = linkisDataSourceService.getByLinkisDataSourceName(ruleDataSource.getLinkisDataSourceName());
                    LOGGER.info("To get LinkisDataSource by ruleDataSource.linkisDataSourceName, result: {}", linkisDataSourceInDb != null ? objectMapper.writeValueAsString(linkisDataSourceInDb): "LinkisDataSource isn't existing");
                    if (Objects.isNull(linkisDataSourceInDb)) {
                        continue;
                    }
                    if (Integer.valueOf(QualitisConstants.DATASOURCE_MANAGER_INPUT_TYPE_MANUAL).equals(linkisDataSourceInDb.getInputType())) {
                        continue;
                    }
//                    To re-generate RuleDataSourceEnv and write back in Excel Json
                    if (QualitisConstants.CMDB_KEY_DCN_NUM.equals(ruleDataSource.getDcnRangeType())
                            || QualitisConstants.CMDB_KEY_LOGIC_AREA.equals(ruleDataSource.getDcnRangeType())) {
                        Long oldLinkisDataSourceId = ruleDataSource.getLinkisDataSourceId();
                        List<Long> newEnvIdsWithinOldDcnRangeValues = oldIdAndNewEnvIdsMapHook.get(oldLinkisDataSourceId);
                        if (CollectionUtils.isNotEmpty(newEnvIdsWithinOldDcnRangeValues)) {
                            GetLinkisDataSourceEnvRequest getLinkisDataSourceEnvRequest = new GetLinkisDataSourceEnvRequest();
                            getLinkisDataSourceEnvRequest.setLinkisDataSourceId(linkisDataSourceInDb.getLinkisDataSourceId());
                            getLinkisDataSourceEnvRequest.setEnvIdList(newEnvIdsWithinOldDcnRangeValues);
                            List<LinkisDataSourceEnv> newEnvsList = linkisDataSourceEnvService.queryEnvsInAdvance(getLinkisDataSourceEnvRequest);
                            List<RuleDataSourceEnv> ruleDataSourceEnvList = newEnvsList.stream().map(newEnv -> {
                                RuleDataSourceEnv ruleDataSourceEnv = new RuleDataSourceEnv();
                                ruleDataSourceEnv.setEnvId(newEnv.getEnvId());
                                String pureEnvName = newEnv.getEnvName().replace(linkisDataSourceInDb.getLinkisDataSourceId() + SpecCharEnum.BOTTOM_BAR.getValue(), "");
                                ruleDataSourceEnv.setEnvName(pureEnvName);
                                ruleDataSourceEnv.setRuleDataSource(ruleDataSource);
                                return ruleDataSourceEnv;
                            }).collect(Collectors.toList());
                            ruleDataSource.setRuleDataSourceEnvs(ruleDataSourceEnvList);
                        }
                    } else {
                        ruleDataSource.setRuleDataSourceEnvs(Collections.emptyList());
                    }
                }

//                回写Excel字段
                String ruleJson = objectMapper.writeValueAsString(rule);
                excelRuleByProject.setRuleJsonObject(ruleJson);

                LOGGER.info("Success to replace envs in the RuleDataSource, new ruleJson: {}", ruleJson);
        }
    }

    private Long handleProject(User user, List<ExcelProject> excelProjectContent, Long updateProjectId, boolean aomp) throws UnExpectedRequestException
            , IOException, PermissionDeniedRequestException, ParseException {

        for (ExcelProject excelProject : excelProjectContent) {
            Project project = objectMapper.readValue(excelProject.getProjectObject(), Project.class);

            if (aomp) {
                Project projectInDb = projectDao.findByNameAndCreateUser(project.getName(), user.getUsername());
                if (projectInDb != null) {
                    updateProjectId = projectInDb.getId();
                }
            }
            if (updateProjectId != null) {
                LOGGER.info("Start to modfiy project[ID={}] with upload project file.", updateProjectId);

                // Check existence of project
                Project projectInDb = projectService.checkProjectExistence(updateProjectId, user.getUsername());
                // Check permissions of project
                List<Integer> permissions = new ArrayList<>();
                permissions.add(ProjectUserPermissionEnum.DEVELOPER.getCode());
                projectService.checkProjectPermission(projectInDb, user.getUsername(), permissions);

                checkServiceIsNotabnormalAndRestoredProjectStatus(projectInDb);

                //Check the running status of the project and update the running status to import
                if(ProjectStatusEnum.INOPERABLE_STATUS.getCode().equals(projectInDb.getRunStatus())){
                    throw new UnExpectedRequestException("{&PROJECT_ID} : [" + updateProjectId + "] import or export operation in progress, cannot repeat operation");
                }
                projectInDb.setRunStatus(ProjectStatusEnum.INOPERABLE_STATUS.getCode());
                setUserModifyInfo(user, projectInDb);
                projectService.saveAndFlushProject(projectInDb);

                project.setId(updateProjectId);
                project.setCreateUser(projectInDb.getCreateUser());
                project.setCreateUserFullName(projectInDb.getCreateUserFullName());
                project.setCreateTime(projectInDb.getCreateTime());

                Project otherProject = projectDao.findByNameAndCreateUser(project.getName(), project.getCreateUser());
                if (otherProject != null && !otherProject.getId().equals(updateProjectId)) {
                    throw new UnExpectedRequestException(String.format("Project name: %s already exist", otherProject.getName()));
                }
                setUserModifyInfo(user, project);
                project.setRunStatus(ProjectStatusEnum.INOPERABLE_STATUS.getCode());
                Project savedProject = projectDao.saveProject(project);
                handleProjectUserAndLabel(excelProject, savedProject);
                updateProjectId = savedProject.getId();
            } else {
                LOGGER.info("Start to add project with upload project file.");

                project.setId(updateProjectId);

                Project otherProject = projectDao.findByNameAndCreateUser(project.getName(), user.getUsername());
                if (otherProject != null) {
                    throw new UnExpectedRequestException(String.format("Project name: %s already exist", otherProject.getName()));
                }
                project.setCreateUser(user.getUsername());
                project.setCreateTime(QualitisConstants.PRINT_TIME_FORMAT.format(new Date()));
                project.setCreateUserFullName(user.getUsername() + "(" + (StringUtils.isNotEmpty(user.getChineseName()) ? user.getChineseName() : "") + ")");
                project.setModifyTime(null);
                project.setModifyUser(null);
                project.setModifyUserFullName(null);
                project.setRunStatus(ProjectStatusEnum.INOPERABLE_STATUS.getCode());
                Project savedProject = projectDao.saveProject(project);
                projectService.autoAuthAdminAndProxy(user, savedProject);
                handleProjectUserAndLabel(excelProject, savedProject);
                updateProjectId = savedProject.getId();
            }
        }

        return updateProjectId;
    }

    private void setUserModifyInfo(User user, Project projectInDb) {
        projectInDb.setModifyUser(user.getUsername());
        projectInDb.setModifyTime(QualitisConstants.PRINT_TIME_FORMAT.format(new Date()));
        projectInDb.setModifyUserFullName(user.getUsername() + "(" + (StringUtils.isNotEmpty(user.getChineseName()) ? user.getChineseName() : "") + ")");
    }

    @Override
    public void handleMetricData(User user, List<ExcelRuleMetric> excelMetricContent) throws UnExpectedRequestException, PermissionDeniedRequestException, IOException {
        List<UserRole> userRoles = userRoleDao.findByUser(user);
        Integer roleType = roleService.getRoleType(userRoles);

        for (ExcelRuleMetric excelRuleMetric : excelMetricContent) {
            RuleMetric ruleMetric = objectMapper.readValue(excelRuleMetric.getRuleMetricJsonObject(), RuleMetric.class);

            RuleMetric ruleMetricInDb = ruleMetricDao.findByName(ruleMetric.getName());

            RuleMetric savedRuleMetric;
            if (ruleMetricInDb == null) {
                ruleMetric.setId(null);
                ruleMetric.setCreateUser(user.getUsername());
                ruleMetric.setCreateTime(QualitisConstants.PRINT_TIME_FORMAT.format(new Date()));

                savedRuleMetric = ruleMetricDao.add(ruleMetric);
            } else {
                subDepartmentPermissionService.checkEditablePermission(roleType, user, null, ruleMetric.getDevDepartmentId(), ruleMetric.getOpsDepartmentId(), false);

                ruleMetric.setId(ruleMetricInDb.getId());
                ruleMetric.setModifyUser(user.getUsername());
                ruleMetric.setModifyTime(QualitisConstants.PRINT_TIME_FORMAT.format(new Date()));
                savedRuleMetric = ruleMetricDao.add(ruleMetric);
                List<DataVisibility> dataVisibilityList = dataVisibilityService.filter(savedRuleMetric.getId(), TableDataTypeEnum.RULE_METRIC);
                if (CollectionUtils.isNotEmpty(dataVisibilityList)) {
                    dataVisibilityService.delete(savedRuleMetric.getId(), TableDataTypeEnum.RULE_METRIC);
                }
            }
            if (StringUtils.isNotEmpty(excelRuleMetric.getDataVisibilityJsonObject())) {
                List<DataVisibility> dataVisibilitys = objectMapper.readValue(excelRuleMetric.getDataVisibilityJsonObject(), new TypeReference<List<DataVisibility>>() {});

                if (CollectionUtils.isNotEmpty(dataVisibilitys)) {
                    dataVisibilitys = dataVisibilitys.stream().map(dataVisibility -> {
                        dataVisibility.setId(null);
                        dataVisibility.setTableDataId(savedRuleMetric.getId());
                        dataVisibility.setTableDataType(TableDataTypeEnum.RULE_METRIC.getCode());
                        return dataVisibility;
                    }).collect(Collectors.toList());

                    dataVisibilityDao.saveAll(dataVisibilitys);
                }
            }
        }
    }

    private void handleProjectUserAndLabel(ExcelProject excelProject, Project savedProject) throws IOException {
        List<ProjectUser> projectUserList = projectUserDao.findByProject(savedProject);
        if (StringUtils.isNotEmpty(excelProject.getProjectUserObject())) {
            Set<ProjectUser> projectUsers = objectMapper.readValue(excelProject.getProjectUserObject(), new TypeReference<Set<ProjectUser>>() {});

            if (CollectionUtils.isNotEmpty(projectUsers)) {
                projectUsers = projectUsers.stream().map(projectUser -> {
                    projectUser.setId(null);
                    projectUser.setProject(savedProject);
                    return projectUser;
                }).collect(Collectors.toSet());

                if (CollectionUtils.isNotEmpty(projectUserList)) {
                    Set<ProjectUser> newProjectUsers = new HashSet<>(projectUsers.size());
                    for (ProjectUser newProjectUser : projectUsers) {
                        boolean exists = false;
                        for (ProjectUser projectUser: projectUserList) {
                            if (newProjectUser.getUserName().equals(projectUser.getUserName()) && newProjectUser.getPermission().equals(projectUser.getPermission())) {
                                exists = true;
                            }
                        }
                        if (! exists) {
                            newProjectUsers.add(newProjectUser);
                        }
                    }
                    projectUserDao.saveAll(newProjectUsers);
                } else {
                    projectUserDao.saveAll(projectUsers);
                }
            }

        }

        projectLabelDao.deleteByProject(savedProject);
        projectLabelDao.flush();
        if (StringUtils.isNotEmpty(excelProject.getProjectLabelObject())) {
            Set<ProjectLabel> projectLabels = objectMapper.readValue(excelProject.getProjectLabelObject(), new TypeReference<Set<ProjectLabel>>() {});
            projectLabels = projectLabels.stream().map(projectLabel -> {
                projectLabel.setId(null);
                projectLabel.setProject(savedProject);
                return projectLabel;
            }).collect(Collectors.toSet());
            projectLabelDao.saveAll(projectLabels);
        }
    }


    @Override
    public void handleExecutionParameters(List<ExcelExecutionParametersByProject> excelExecutionParametersByProjects, String userName, Long updateProjectId) throws IOException {
        for (ExcelExecutionParametersByProject excelExecutionParametersByProject : excelExecutionParametersByProjects) {
            LOGGER.info("{} start to handle current execution param json {}", userName, excelExecutionParametersByProject.getExecutionParameterJsonObject());
            handleExecutionParametersReal(excelExecutionParametersByProject.getExecutionParameterJsonObject(), userName, updateProjectId);
            LOGGER.info("Finish to handle execution param", userName);
        }
    }

    @Override
    public void handleExecutionParametersReal(String executionParameterJsonObject, String userName, Long updateProjectId) throws IOException {
        ExecutionParameters executionParameters = objectMapper.readValue(executionParameterJsonObject, ExecutionParameters.class);
        ExecutionParameters executionParametersInDb = executionParametersDao.findByNameAndProjectId(executionParameters.getName(), updateProjectId);
        Set<AlarmArgumentsExecutionParameters> alarmArgumentsExecutionParameters = executionParameters.getAlarmArgumentsExecutionParameters();
        Set<NoiseEliminationManagement> noiseEliminationManagement = executionParameters.getNoiseEliminationManagement();
        Set<StaticExecutionParameters> staticExecutionParameters = executionParameters.getStaticExecutionParameters();
        Set<ExecutionVariable> executionVariableSets = executionParameters.getExecutionVariableSets();

        if (executionParametersInDb != null) {
            executionParameters.setId(executionParametersInDb.getId());
            executionParameters.setProjectId(executionParametersInDb.getProjectId());
            executionParameters.setModifyTime(QualitisConstants.PRINT_TIME_FORMAT.format(new Date()));
            executionParameters.setModifyUser(userName);

            // Clear
            clearExecutionParametersRelations(executionParametersInDb);
        } else {
            executionParameters.setId(null);
            executionParameters.setProjectId(updateProjectId);
            executionParameters.setCreateTime(QualitisConstants.PRINT_TIME_FORMAT.format(new Date()));
            executionParameters.setCreateUser(userName);
            executionParameters.setExecutionVariableSets(null);
            executionParameters.setStaticExecutionParameters(null);
            executionParameters.setNoiseEliminationManagement(null);
            executionParameters.setAlarmArgumentsExecutionParameters(null);
            executionParametersInDb = executionParametersDao.saveExecutionParameters(executionParameters);
        }
        BeanUtils.copyProperties(executionParameters, executionParametersInDb);

        createExecutionParametersRelations(alarmArgumentsExecutionParameters, noiseEliminationManagement, staticExecutionParameters, executionVariableSets, executionParametersInDb);
        executionParametersDao.saveExecutionParameters(executionParametersInDb);
    }

    private void createExecutionParametersRelations(Set<AlarmArgumentsExecutionParameters> alarmArgumentsExecutionParameterSet, Set<NoiseEliminationManagement> noiseEliminationManagementSet
            , Set<StaticExecutionParameters> staticExecutionParameterSet, Set<ExecutionVariable> executionVariableSet, ExecutionParameters executionParametersInDb) {
        if (CollectionUtils.isNotEmpty(alarmArgumentsExecutionParameterSet)) {
            List<AlarmArgumentsExecutionParameters> alarmArgumentsExecutionParametersList = alarmArgumentsExecutionParameterSet.stream().map(alarmArgumentsExecutionParameters -> {
                alarmArgumentsExecutionParameters.setId(null);
                alarmArgumentsExecutionParameters.setExecutionParameters(executionParametersInDb);
                return alarmArgumentsExecutionParameters;
            }).collect(Collectors.toList());

            executionParametersInDb.setAlarmArgumentsExecutionParameters(alarmArgumentsExecutionParametersDao.saveAll(alarmArgumentsExecutionParametersList));
        }
        if (CollectionUtils.isNotEmpty(noiseEliminationManagementSet)) {
            List<NoiseEliminationManagement> noiseEliminationManagementList = noiseEliminationManagementSet.stream().map(noiseEliminationManagement -> {
                noiseEliminationManagement.setId(null);
                noiseEliminationManagement.setExecutionParameters(executionParametersInDb);
                return noiseEliminationManagement;
            }).collect(Collectors.toList());

            executionParametersInDb.setNoiseEliminationManagement(noiseEliminationManagementDao.saveAll(noiseEliminationManagementList));
        }
        if (CollectionUtils.isNotEmpty(staticExecutionParameterSet)) {
            List<StaticExecutionParameters> staticExecutionParametersList = staticExecutionParameterSet.stream().map(staticExecutionParameters -> {
                staticExecutionParameters.setId(null);
                staticExecutionParameters.setExecutionParameters(executionParametersInDb);
                return staticExecutionParameters;
            }).collect(Collectors.toList());

            executionParametersInDb.setStaticExecutionParameters(staticExecutionParametersDao.saveAll(staticExecutionParametersList));
            //DynamicEngineEnum中YARN、SPARK、FLINK、CUSTOM
            String staticStartupParam = staticExecutionParameterSet.stream()
                    .filter(staticExecutionParametersRequest -> DynamicEngineEnum.YARN_ARGUMENTS.getCode().equals(staticExecutionParametersRequest.getParameterType()) ||
                            DynamicEngineEnum.SPARK_ARGUMENTS.getCode().equals(staticExecutionParametersRequest.getParameterType()) ||
                            DynamicEngineEnum.FLINK_ARGUMENTS.getCode().equals(staticExecutionParametersRequest.getParameterType()) ||
                            DynamicEngineEnum.CUSTOM_ARGUMENTS.getCode().equals(staticExecutionParametersRequest.getParameterType())
                    )
                    .map(staticExecutionParametersRequest -> staticExecutionParametersRequest.getParameterName() + SpecCharEnum.EQUAL.getValue() + staticExecutionParametersRequest.getParameterValue())
                    .collect(Collectors.joining(SpecCharEnum.DIVIDER.getValue()));

            executionParametersInDb.setStaticStartupParam(staticStartupParam);
        }
        if (CollectionUtils.isNotEmpty(executionVariableSet)) {
            List<ExecutionVariable> executionVariableList = executionVariableSet.stream().map(executionVariable -> {
                executionVariable.setId(null);
                executionVariable.setExecutionParameters(executionParametersInDb);
                return executionVariable;
            }).collect(Collectors.toList());

            executionParametersInDb.setExecutionVariableSets(executionVariableDao.saveAll(executionVariableList));
            String executionParam = executionVariableSet.stream()
                    .map(executionManagementRequest -> executionManagementRequest.getVariableName() + SpecCharEnum.COLON.getValue() + executionManagementRequest.getVariableValue())
                    .collect(Collectors.joining(SpecCharEnum.DIVIDER.getValue()));
            //并发粒度：库粒度、表粒度，引擎复用参数，拼接到执行变量------>因规则执行时，表单的默认会传发粒度、引擎复用等且优先级最高，所以这里不需拼接；

            executionParametersInDb.setExecutionParam(executionParam);
        }
    }

    private void clearExecutionParametersRelations(ExecutionParameters executionParametersInDb) {
        executionVariableDao.deleteByExecutionParameters(executionParametersInDb);
        staticExecutionParametersDao.deleteByExecutionParameters(executionParametersInDb);
        noiseEliminationManagementDao.deleteByExecutionParameters(executionParametersInDb);
        alarmArgumentsExecutionParametersDao.deleteByExecutionParameters(executionParametersInDb);
    }

    private void handleStandardValue(User user, List<ExcelStandardValue> excelStandardVauleContent) throws IOException, PermissionDeniedRequestException, UnExpectedRequestException {
        List<UserRole> userRoles = userRoleDao.findByUser(user);
        Integer roleType = roleService.getRoleType(userRoles);
        for (ExcelStandardValue excelStandardValue : excelStandardVauleContent) {
            StandardValueVersion response = objectMapper.readValue(excelStandardValue.getStandardValueJsonObject(), StandardValueVersion.class);
            if (null == response) {
                LOGGER.warn("Failed to format StandardValueVersionResponse: {}", excelStandardValue);
                continue;
            }

            StandardValueVersion standardValueVersion = new StandardValueVersion();
            BeanUtils.copyProperties(response, standardValueVersion);

            StandardValueVersion standardValueVersionInDb = standardValueVersionDao.findByEnName(standardValueVersion.getEnName());

            List<DataVisibility> dataVisibilitys = new ArrayList<>();
            if (StringUtils.isNotEmpty(excelStandardValue.getDataVisibilityJsonObject())) {
                dataVisibilitys = objectMapper.readValue(excelStandardValue.getDataVisibilityJsonObject(), new TypeReference<List<DataVisibility>>() {
                });
            }

            if (standardValueVersionInDb == null) {
                standardValueVersion.setId(null);
                standardValueVersion.setCreateUser(user.getUsername());
                standardValueVersion.setCreateTime(QualitisConstants.PRINT_TIME_FORMAT.format(new Date()));
                //因1.1.0版本取消版本管理、标签字段 action字段，这里要做赋空处理(兼容旧数据)
                standardValueVersion.setStandardValue(null);
                standardValueVersion.setStandardValueActionVersion(null);
                standardValueVersion.setStandardValueLabelVersion(null);
                StandardValueVersion savedStandardValueVersion = standardValueVersionDao.saveStandardValueVersion(standardValueVersion);

                if (CollectionUtils.isNotEmpty(dataVisibilitys)) {
                    dataVisibilitys = dataVisibilitys.stream().map(dataVisibility -> {
                        dataVisibility.setId(null);
                        dataVisibility.setTableDataId(savedStandardValueVersion.getId());
                        dataVisibility.setTableDataType(TableDataTypeEnum.STANDARD_VALUE.getCode());
                        return dataVisibility;
                    }).collect(Collectors.toList());

                    dataVisibilityDao.saveAll(dataVisibilitys);
                }
            } else {
                subDepartmentPermissionService.checkEditablePermission(roleType, user, null, standardValueVersion.getDevDepartmentId(), standardValueVersion.getOpsDepartmentId(), false);

                //因1.1.0版本取消版本管理、标签字段 action字段，这里要做赋空处理(兼容旧数据)
                standardValueVersion.setStandardValue(null);
                standardValueVersion.setStandardValueActionVersion(null);
                standardValueVersion.setStandardValueLabelVersion(null);

                standardValueVersion.setId(standardValueVersionInDb.getId());
                standardValueVersion.setModifyUser(user.getUsername());
                standardValueVersion.setModifyTime(QualitisConstants.PRINT_TIME_FORMAT.format(new Date()));

                StandardValueVersion savedStandardValueVersion = standardValueVersionDao.saveStandardValueVersion(standardValueVersion);

                List<DataVisibility> dataVisibilityList = dataVisibilityService.filter(savedStandardValueVersion.getId(), TableDataTypeEnum.STANDARD_VALUE);
                if (CollectionUtils.isNotEmpty(dataVisibilityList)) {
                    dataVisibilityService.delete(savedStandardValueVersion.getId(), TableDataTypeEnum.STANDARD_VALUE);
                }
                if (CollectionUtils.isNotEmpty(dataVisibilitys)) {
                    dataVisibilitys = dataVisibilitys.stream().map(dataVisibility -> {
                        dataVisibility.setId(null);
                        dataVisibility.setTableDataId(standardValueVersionInDb.getId());
                        dataVisibility.setTableDataType(TableDataTypeEnum.STANDARD_VALUE.getCode());
                        return dataVisibility;
                    }).collect(Collectors.toList());

                    dataVisibilityDao.saveAll(dataVisibilitys);
                }
            }

        }

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {RuntimeException.class, UnExpectedRequestException.class})
    public void handleTableGroup(AnalysisEventListener listener, Long projectId, List<DiffVariableRequest> diffVariableRequestList)
            throws IOException, UnExpectedRequestException {
        List<ExcelGroupByProject> excelGroupByProjects = new ArrayList<>();

        if (listener instanceof ExcelProjectListener) {
            excelGroupByProjects = ((ExcelProjectListener) listener).getExcelGroupByProjects();
        } else if (listener instanceof ExcelRuleListener) {
            excelGroupByProjects = ((ExcelRuleListener) listener).getExcelGroupByProjects();
        }

        List<DiffVariableRequest> groupJsonDiffVariableRequestList = diffVariableRequestList.stream()
                .filter(diffVariableRequest -> DiffRequestTypeEnum.JSON_REPLACEMENT.getCode().equals(diffVariableRequest.getType()))
                .filter(diffVariableRequest -> diffVariableRequest.getName().startsWith(ExcelSheetName.TABLE_GROUP + "$"))
                .collect(Collectors.toList());

        for (ExcelGroupByProject excelGroupByProject : excelGroupByProjects) {
            String jsonStr = excelGroupByProject.getRuleGroupJsonObject();

            // Replace json value with diff variable request.
            String modifiedJsonStr = replaceJsonValue(groupJsonDiffVariableRequestList, jsonStr);

            RuleGroup ruleGroup = objectMapper.readValue(modifiedJsonStr, RuleGroup.class);
            RuleGroup ruleGroupInDb = ruleGroupDao.findByRuleGroupNameAndProjectId(ruleGroup.getRuleGroupName(), projectId);

            Set<RuleDataSource> ruleDataSources = ruleGroup.getRuleDataSources();
            if (ruleGroupInDb != null) {
                ruleGroup.setId(ruleGroupInDb.getId());
                ruleGroup.setProjectId(ruleGroupInDb.getProjectId());
                // Clear
                clearDatasourceEnv(ruleGroupInDb);
            } else {
                ruleGroup.setId(null);
                ruleGroup.setProjectId(projectId);
                ruleGroup.setRuleDataSources(null);
            }
            ruleGroupInDb = ruleGroupDao.saveRuleGroup(ruleGroup);
            createDatasourceEnv(ruleDataSources, ruleGroupInDb, diffVariableRequestList);
            ruleGroupDao.saveRuleGroup(ruleGroupInDb);
        }
    }

    @Override
    public String replaceJsonValue(List<DiffVariableRequest> jsonDiffVariableRequestList, String jsonStr) {
        if (CollectionUtils.isNotEmpty(jsonDiffVariableRequestList)) {
            for (DiffVariableRequest diffVariableRequest : jsonDiffVariableRequestList) {
                int index = diffVariableRequest.getName().indexOf("$");
                if (index != -1) {
                    diffVariableRequest.setName(diffVariableRequest.getName().substring(index));
                    LOGGER.info("Real json path is " + diffVariableRequest.getName());
                    LOGGER.info("Real json value is " + diffVariableRequest.getValue());

                    jsonStr = JsonPath.parse(jsonStr).set(diffVariableRequest.getName(), diffVariableRequest.getValue()).jsonString();
                }
            }
        }

        return jsonStr;
    }

    @Override
    public void createDatasourceEnv(Set<RuleDataSource> ruleDataSourcesFromJson, Object object, List<DiffVariableRequest> diffVariableRequestList) throws UnExpectedRequestException {
        List<RuleDataSource> ruleDataSources = new ArrayList<>();
        List<RuleDataSourceEnv> ruleDataSourceEnvs = new ArrayList<>();

        Map<String, Long> envs = new HashMap<>();
        if (CollectionUtils.isNotEmpty(ruleDataSourcesFromJson)) {
            for (RuleDataSource ruleDataSource : ruleDataSourcesFromJson) {
                ruleDataSource.setId(null);
                if (object instanceof RuleGroup) {
                    ruleDataSource.setRuleGroup((RuleGroup) object);
                    ruleDataSource.setProjectId(((RuleGroup) object).getProjectId());
                } else {
                    ruleDataSource.setRule((Rule) object);
                    ruleDataSource.setProjectId(((Rule) object).getProject().getId());
                    if (RuleTypeEnum.CUSTOM_RULE.getCode().equals(((Rule) object).getRuleType()) && ! QualitisConstants.ORIGINAL_INDEX.equals(ruleDataSource.getDatasourceIndex())) {
                        continue;
                    }
                }
                List<DiffVariableRequest> systemInnerDiffVariableRequestList = diffVariableRequestList.stream().filter(
                        diffVariableRequest -> DiffRequestTypeEnum.SYSTEM_INNER.getCode().equals(diffVariableRequest.getType())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(systemInnerDiffVariableRequestList)) {
                    for (DiffVariableRequest diffVariableRequest : systemInnerDiffVariableRequestList) {
                        if (QualitisConstants.EXECUTE_USER.equals(diffVariableRequest.getName())) {
                            User user = userDao.findByUsername(diffVariableRequest.getValue());
                            if (user == null) {
                                throw new UnExpectedRequestException(diffVariableRequest.getValue() + "not in the system's user list.");
                            }
                            String proxyUser = ruleDataSource.getProxyUser();
                            if (StringUtils.isNotEmpty(proxyUser)) {
                                proxyUser = proxyUser.replace("[@" + diffVariableRequest.getName() + "]", diffVariableRequest.getValue());
                                ruleDataSource.setProxyUser(proxyUser);
                            }
                        } else if (QualitisConstants.EXECUTE_CLUSETER.equals(diffVariableRequest.getName())) {
                            String clusterName = ruleDataSource.getClusterName();
                            ClusterInfo clusterInfo = clusterInfoDao.findByClusterName(diffVariableRequest.getValue());
                            if (clusterInfo == null) {
                                throw new UnExpectedRequestException(diffVariableRequest.getValue() + "not in the system's cluster list.");
                            }
                            if (StringUtils.isNotEmpty(clusterName)) {
                                clusterName = clusterName.replace("[@" + diffVariableRequest.getName() + "]", diffVariableRequest.getValue());
                                ruleDataSource.setClusterName(clusterName);
                            }
                        }
                    }
                }
                if (StringUtils.isNotEmpty(ruleDataSource.getLinkisDataSourceName())) {
                    LinkisDataSource linkisDataSource = linkisDataSourceService.getByLinkisDataSourceName(ruleDataSource.getLinkisDataSourceName());
                    if (linkisDataSource != null) {
                        ruleDataSource.setLinkisDataSourceVersionId(linkisDataSource.getVersionId());
                        ruleDataSource.setLinkisDataSourceId(linkisDataSource.getLinkisDataSourceId());
                        envs.putAll(linkisDataSourceService.getEnvNameAndIdMap(linkisDataSource));
                    }
                }
                if (CollectionUtils.isNotEmpty(ruleDataSource.getRuleDataSourceEnvs())) {
                    Map<String, Map<String, String>> dataSourceEnvVariableMapping = getDataSourceEnvVariableMapping(diffVariableRequestList);
                    ruleDataSourceEnvs.addAll(ruleDataSource.getRuleDataSourceEnvs().stream().map(currRuleDataSourceEnv -> {
                        String realEnvName = replaceAndReturnRealEnvName(ruleDataSource, currRuleDataSourceEnv.getEnvName(), dataSourceEnvVariableMapping);
                        currRuleDataSourceEnv.setRuleDataSource(ruleDataSource);
                        currRuleDataSourceEnv.setEnvId(envs.get(realEnvName));
                        currRuleDataSourceEnv.setEnvName(realEnvName);
                        currRuleDataSourceEnv.setId(null);
                        return currRuleDataSourceEnv;
                    }).collect(Collectors.toList()));
                }
                ruleDataSources.add(ruleDataSource);
            }
        }

        if (CollectionUtils.isNotEmpty(ruleDataSources)) {
            Set<RuleDataSource> ruleDataSourceSet = ruleDataSourceDao.saveAllRuleDataSource(ruleDataSources).stream().collect(Collectors.toSet());
            if (object instanceof RuleGroup) {
                ((RuleGroup) object).setRuleDataSources(ruleDataSourceSet);
            } else {
                ((Rule) object).setRuleDataSources(ruleDataSourceSet);
            }
        }

        if (CollectionUtils.isNotEmpty(ruleDataSourceEnvs)) {
            ruleDatasourceEnvDao.saveAllRuleDataSourceEnv(ruleDataSourceEnvs);
        }
    }

    private String replaceAndReturnRealEnvName(RuleDataSource ruleDataSource
            , String variableEnvName
            , Map<String, Map<String, String>> dataSourceEnvVariableMapping) {
        if (dataSourceEnvVariableMapping.containsKey(ruleDataSource.getLinkisDataSourceName())) {
            Map<String, String> variableAndReplaceMapping = dataSourceEnvVariableMapping.get(ruleDataSource.getLinkisDataSourceName());
            if (MapUtils.isNotEmpty(variableAndReplaceMapping) && variableAndReplaceMapping.containsKey(variableEnvName)) {
                return variableAndReplaceMapping.get(variableEnvName);
            }
        }
        return variableEnvName;
    }

    @Override
    public void clearDatasourceEnv(Object object) {
        if (object instanceof RuleGroup) {
            if (CollectionUtils.isNotEmpty(((RuleGroup) object).getRuleDataSources())) {
                ruleDataSourceDao.deleteByRuleGroup(((RuleGroup) object));
            }
        } else {
            if (CollectionUtils.isNotEmpty(((Rule) object).getRuleDataSources())) {
                ruleDataSourceDao.deleteByRule(((Rule) object));
            }
        }
    }

    private ExcelProjectListener readExcel(InputStream inputStream, ExcelProjectListener listener) {
        LOGGER.info("Start to read project excel");
        ExcelReader excelReader = new ExcelReader(new BufferedInputStream(inputStream), null, listener);
        List<Sheet> sheets = excelReader.getSheets();
        for (Sheet sheet : sheets) {
            if (sheet.getSheetName().equals(ExcelSheetName.RULE_NAME)) {
                sheet.setClazz(ExcelRuleByProject.class);
                sheet.setHeadLineMun(1);
                excelReader.read(sheet);
            } else if (sheet.getSheetName().equals(ExcelSheetName.PROJECT_NAME)) {
                sheet.setClazz(ExcelProject.class);
                sheet.setHeadLineMun(1);
                excelReader.read(sheet);
            } else if (sheet.getSheetName().equals(ExcelSheetName.RULE_METRIC_NAME)) {
                sheet.setClazz(ExcelRuleMetric.class);
                sheet.setHeadLineMun(1);
                excelReader.read(sheet);
            } else if (sheet.getSheetName().equals(ExcelSheetName.EXECUTION_PARAMETERS_NAME)) {
                sheet.setClazz(ExcelExecutionParametersByProject.class);
                sheet.setHeadLineMun(1);
                excelReader.read(sheet);
            } else if (sheet.getSheetName().equals(ExcelSheetName.STANDARD_VAULE)) {
                sheet.setClazz(ExcelStandardValue.class);
                sheet.setHeadLineMun(1);
                excelReader.read(sheet);
            } else if (sheet.getSheetName().equals(ExcelSheetName.TABLE_GROUP)) {
                sheet.setClazz(ExcelGroupByProject.class);
                sheet.setHeadLineMun(1);
                excelReader.read(sheet);
            } else if (sheet.getSheetName().equals(ExcelSheetName.RULE_UDF)) {
                sheet.setClazz(ExcelRuleUdf.class);
                sheet.setHeadLineMun(1);
                excelReader.read(sheet);
            } else if (sheet.getSheetName().equals(ExcelSheetName.DATASOURCE_ENV)) {
                sheet.setClazz(ExcelDatasourceEnv.class);
                sheet.setHeadLineMun(1);
                excelReader.read(sheet);
            }
        }

        LOGGER.info("Finish to read project excel. excel content: rule sheet {}, project sheet {}", listener.getExcelRuleContent(), listener.getExcelProjectContent());
        return listener;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {RuntimeException.class, UnExpectedRequestException.class})
    public GeneralResponse uploadProjectFromAomp(InputStream fileInputStream, FormDataContentDisposition fileDisposition, String userName)
            throws IOException, UnExpectedRequestException, PermissionDeniedRequestException, JSONException, MetaDataAcquireFailedException, ParseException {

        UploadProjectRequest uploadProjectRequest = new UploadProjectRequest();
        String localZipPath = fileService.uploadFile(fileInputStream, fileDisposition, userName).getData();
        uploadProjectRequest.setUploadType(ProjectTransportTypeEnum.LOCAL.getCode());
        uploadProjectRequest.setZipPath(localZipPath);
        uploadProjectRequest.setLoginUser(userName);

        return uploadProjectFromLocalOrGit(uploadProjectRequest, true);
    }

    private List<ExcelGroupByProject> getGroup(List<Project> projects, List<DiffVariableRequest> diffVariableRequestList, List<Long> ruleId, List<String> ruleName) throws IOException {
        List<ExcelGroupByProject> excelGroupByProjects = new ArrayList<>();
        for (Project project : projects) {
            Set<Rule> rules = getRules(ruleId, ruleName, project);

            if (CollectionUtils.isEmpty(rules)) {
                continue;
            }
            excelGroupByProjects = getTableGroup(rules.stream().collect(Collectors.toList()), diffVariableRequestList);
        }

        return excelGroupByProjects;
    }

    @Override
    public List<ExcelGroupByProject> getTableGroup(List<Rule> rules, List<DiffVariableRequest> diffVariableRequestList) throws IOException {
        Set<RuleGroup> ruleGroups = rules.stream().map(rule -> rule.getRuleGroup()).filter(ruleGroup -> CollectionUtils.isNotEmpty(ruleGroup.getRuleDataSources())).collect(Collectors.toSet());
        List<ExcelGroupByProject> excelGroupByProjects = new ArrayList<>();
        List<String> existNames = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ruleGroups)) {
            for (RuleGroup ruleGroup : ruleGroups) {
                if (existNames.contains(ruleGroup.getRuleGroupName())) {
                    continue;
                }
                if (CollectionUtils.isNotEmpty(diffVariableRequestList)) {
                    replaceDiffVariable(ruleGroup, diffVariableRequestList);
                }
                ExcelGroupByProject excelGroupByProject = new ExcelGroupByProject();
                excelGroupByProject.setRuleGroupJsonObject(objectMapper.writeValueAsString(ruleGroup));

                excelGroupByProjects.add(excelGroupByProject);
                existNames.add(ruleGroup.getRuleGroupName());
            }
        }

        return excelGroupByProjects;
    }

    @Override
    public void replaceDiffVariable(Object ruleOrGroup, List<DiffVariableRequest> diffVariableRequestList) {
        List<DiffVariable> diffVariableList = diffVariableRepository.findAll();
        List<String> diffVariableNameList = diffVariableList.stream().map(diffVariable -> diffVariable.getName()).collect(Collectors.toList());

        for (DiffVariableRequest diffVariableRequest : diffVariableRequestList) {
            if (! diffVariableNameList.contains(diffVariableRequest.getName())) {
                continue;
            }
            Set<RuleDataSource> ruleDatasources;
            if (ruleOrGroup instanceof Rule) {
                ruleDatasources = ((Rule) ruleOrGroup).getRuleDataSources();
            } else {
                ruleDatasources = ((RuleGroup) ruleOrGroup).getRuleDataSources();
            }
            if (QualitisConstants.EXECUTE_USER.equals(diffVariableRequest.getName())) {
                ruleDatasources = ruleDatasources.stream().map(ruleDataSource -> {
                    ruleDataSource.setProxyUser("[@" + diffVariableRequest.getName() + "]");
                    return ruleDataSource;
                }).collect(Collectors.toSet());

            } else if (QualitisConstants.EXECUTE_CLUSETER.equals(diffVariableRequest.getName())) {
                ruleDatasources = ruleDatasources.stream().map(ruleDataSource -> {
                    ruleDataSource.setClusterName("[@" + diffVariableRequest.getName() + "]");
                    return ruleDataSource;
                }).collect(Collectors.toSet());
            }
            if (ruleOrGroup instanceof Rule) {
                ((Rule) ruleOrGroup).setRuleDataSources(ruleDatasources);
            } else {
                ((RuleGroup) ruleOrGroup).setRuleDataSources(ruleDatasources);
            }

        }
    }

    private List<ExcelStandardValue> getExcelStandardValue(List<Project> projects, List<Long> ruleId, List<String> ruleName) throws IOException {
        List<ExcelStandardValue> excelExcelStandardValues = new ArrayList<>();
        for (Project project : projects) {
            Set<Rule> rules = Sets.newHashSet();
            if (CollectionUtils.isNotEmpty(ruleId)) {
                rules.addAll(ruleDao.findByIdsAndProject(ruleId,project.getId()));
            }
            if (CollectionUtils.isNotEmpty(ruleName)) {
                for (String name : ruleName) {
                    Rule rule = ruleDao.findByProjectAndRuleName(project, name);
                    if (null != rule) {
                        rules.add(rule);
                    }

                }
            }

            if (CollectionUtils.isEmpty(rules)) {
                rules = ruleDao.findExistStandardVaule(project.getId()).stream().collect(Collectors.toSet());
            } else {
                rules = rules.stream().filter(item -> item.getProject().getId().toString().equals(project.getId().toString()) && item.getStandardValueVersionId() != null && StringUtils.isNotBlank(item.getStandardValueVersionEnName())).collect(Collectors.toSet());
            }

            if (CollectionUtils.isEmpty(rules)) {
                continue;
            }
            List<Long> standardValueVersionIds = Lists.newArrayList();
            for (Rule rule : rules) {
                ExcelStandardValue excelStandardValue = new ExcelStandardValue();
                StandardValueVersion standardValueVersion = standardValueVersionDao.findById(rule.getStandardValueVersionId());
                if (standardValueVersion != null) {

                    excelStandardValue.setStandardValueJsonObject(objectMapper.writeValueAsString(standardValueVersion));
                    List<DataVisibility> dataVisibilityList = dataVisibilityService.filter(standardValueVersion.getId(), TableDataTypeEnum.STANDARD_VALUE);

                    if (CollectionUtils.isNotEmpty(dataVisibilityList)) {
                        excelStandardValue.setDataVisibilityJsonObject(objectMapper.writeValueAsString(dataVisibilityList));
                    }
                }
                LOGGER.info("Collect excel line of StandardValueVersion: {}", excelStandardValue);
                if (null != excelStandardValue && null != standardValueVersion) {
                    Long mateData = standardValueVersionIds.stream().filter(item -> item.toString().equals(standardValueVersion.getId().toString())).findAny().orElse(null);
                    if (null == mateData) {
                        standardValueVersionIds.add(standardValueVersion.getId());
                        excelExcelStandardValues.add(excelStandardValue);
                    }

                }

            }
        }
        return excelExcelStandardValues;
    }

    private List<ExcelRuleByProject> getExcelRuleByProject(List<Project> projects, List<DiffVariableRequest> diffVariableRequestList, List<Long> ruleId, List<String> ruleName) throws IOException {
        List<ExcelRuleByProject> excelRuleByProjects = new ArrayList<>();
        for (Project project : projects) {
            Set<Rule> rules = getRules(ruleId, ruleName, project);

            List<ExcelRuleByProject> excelRuleByProjectList = ruleBatchService.getRule(rules, diffVariableRequestList);

            excelRuleByProjects.addAll(excelRuleByProjectList);
        }
        return excelRuleByProjects;
    }

    private Set<Rule> getRules(List<Long> ruleIdList, List<String> ruleNameList, Project project) {
        Set<Rule> rules = Sets.newHashSet();
        if (CollectionUtils.isNotEmpty(ruleIdList)) {
            rules.addAll(ruleDao.findByIdsAndProject(ruleIdList, project.getId()));
        }
        if (CollectionUtils.isNotEmpty(ruleNameList)) {
            for (String name : ruleNameList) {
                Rule rule = ruleDao.findByProjectAndRuleName(project, name);
                if (null != rule) {
                    rules.add(rule);
                }

            }
        }

        if (CollectionUtils.isEmpty(rules)) {
            rules = ruleDao.findByProject(project).stream().collect(Collectors.toSet());
        }
        return rules;
    }

    private List<ExcelProject> getExcelProject(List<Project> projects) throws IOException {
        List<ExcelProject> excelProjects = new ArrayList<>();
        for (Project project : projects) {
            project.setRunStatus(ProjectStatusEnum.OPERABLE_STATUS.getCode());
            ExcelProject excelProject = new ExcelProject();
            excelProject.setProjectObject(objectMapper.writeValueAsString(project));

            Set<ProjectUser> projectUsers = project.getProjectUsers();
            if (CollectionUtils.isNotEmpty(projectUsers)) {
                excelProject.setProjectUserObject(objectMapper.writerWithType(new TypeReference<Set<ProjectUser>>() {
                }).writeValueAsString(projectUsers));
            }

            Set<ProjectLabel> projectLabels = project.getProjectLabels();
            if (CollectionUtils.isNotEmpty(projectLabels)) {
                excelProject.setProjectLabelObject(objectMapper.writerWithType(new TypeReference<Set<ProjectLabel>>() {
                }).writeValueAsString(projectLabels));
            }

            LOGGER.info("Collect excel line: {}", excelProject);
            excelProjects.add(excelProject);
        }
        return excelProjects;
    }

    @Override
    public List<ExcelExecutionParametersByProject> getExecutionParameters(List<Project> projects, List<String> executionParamNames, boolean batchRules) throws IOException {
        List<ExcelExecutionParametersByProject> excelList = new ArrayList<>();
        for (Project project : projects) {
            List<ExecutionParameters> allExecutionParameters = executionParametersDao.getAllExecutionParameters(project.getId());

            for (ExecutionParameters allExecutionParameter : allExecutionParameters) {
                if (batchRules && CollectionUtils.isNotEmpty(executionParamNames) && !executionParamNames.contains(allExecutionParameter.getName())) {
                    continue;
                }
                ExcelExecutionParametersByProject excelExecutionParameters = new ExcelExecutionParametersByProject();
                excelExecutionParameters.setExecutionParameterJsonObject(objectMapper.writeValueAsString(allExecutionParameter));
                LOGGER.info("Collect excel line of ExecutionParameters: {}", excelExecutionParameters);
                excelList.add(excelExecutionParameters);
            }
        }
        return excelList;
    }

    private String getUserName(boolean aomp, String loginUser, String operateUser) {
        if (aomp) {
            return loginUser;
        }
        return StringUtils.isNotBlank(HttpUtils.getUserName(httpServletRequest)) ? HttpUtils.getUserName(httpServletRequest) : operateUser;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {RuntimeException.class, UnExpectedRequestException.class})
    public GeneralResponse uploadProjectFromLocalOrGit(UploadProjectRequest request, boolean aomp) throws UnExpectedRequestException, PermissionDeniedRequestException, IOException, JSONException, MetaDataAcquireFailedException, ParseException {
        if (null == request.getIncrement()) {
            request.setIncrement(Boolean.FALSE);
        }
        String userName = getUserName(aomp, request.getLoginUser(), request.getOperateUser());
        List<File> udfFiles = new ArrayList<>();
        List<File> projectFiles = new ArrayList<>();
        StringBuilder operateComment = new StringBuilder();
        if (ProjectTransportTypeEnum.LOCAL.getCode().equals(request.getUploadType())) {
            // Check file and unzip
            projectFiles = checkFileAndUnzip(request.getZipPath());
            operateComment.append(ProjectTransportTypeEnum.LOCAL.name());
        } else if (ProjectTransportTypeEnum.GIT.getCode().equals(request.getUploadType())) {
            // Git upload not currently supported
        } else {
            throw new UnExpectedRequestException("Not support upload type.");
        }

        ExcelProjectListener listener = new ExcelProjectListener();
        List<DiffVariableRequest> diffVariableRequests = new ArrayList<>(QualitisConstants.LENGTH_FOUR);
        // Read files, generate objects and clear files
        for (File currentFile : projectFiles) {
            if (currentFile.getName().endsWith(QualitisConstants.SUPPORT_SCALA_SUFFIX_NAME) || currentFile.getName().endsWith(QualitisConstants.SUPPORT_PYTHON_SUFFIX_NAME) || currentFile.getName().endsWith(QualitisConstants.SUPPORT_JAR_SUFFIX_NAME)) {
                udfFiles.add(currentFile);
                continue;
            }
            if (aomp && currentFile.getName().endsWith(QualitisConstants.SUPPORT_CONFIG_SUFFIX_NAME)) {
                Properties prop = new Properties();
                try (FileInputStream inputStream = new FileInputStream(currentFile);
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    prop.load(inputStreamReader);
                    Set<String> propKeys = prop.stringPropertyNames();
                    for (String propKey : propKeys) {
                        diffVariableRequests.add(new DiffVariableRequest(currentFile.getName(), propKey, prop.getProperty(propKey)));
                    }
                    continue;
                } catch (Exception e) {
                    LOGGER.error("Failed to upload projects, caused by: {}", e.getMessage(), e);
                }

            }
            if (!currentFile.getName().endsWith(QualitisConstants.SUPPORT_EXCEL_SUFFIX_NAME)) {
                continue;
            }
            try (InputStream currentFileInputStream = new FileInputStream(currentFile)) {
                listener = readExcel(currentFileInputStream, listener);
            } catch (Exception e) {
                LOGGER.error("Failed to upload projects, caused by: {}", e.getMessage(), e);
            } finally {
                if (currentFile.exists() && currentFile.getName().endsWith(QualitisConstants.SUPPORT_EXCEL_SUFFIX_NAME) && ProjectTransportTypeEnum.LOCAL.getCode().equals(request.getUploadType())) {
                    try {
                        Files.delete(currentFile.toPath());
                    } catch (IOException e) {
                        LOGGER.error("Failed to delete {}", currentFile.getName());
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(diffVariableRequests)) {
            request.setDiffVariableRequestList(diffVariableRequests);
        }
        return uploadProjectsReal(listener, userName, StringUtils.isNotEmpty(request.getLoginUser()), request.getProjectId(), request.getDiffVariableRequestList(), udfFiles, operateComment, request.getIncrement());
    }

    @Override
    public GeneralResponse downloadProjectsToLocalOrGit(DownloadProjectRequest request, HttpServletResponse response) throws UnExpectedRequestException
            , PermissionDeniedRequestException, IOException, ParseException {
        // Check Arguments
        DownloadProjectRequest.checkRequest(request);
        String loginUser = StringUtils.isNotBlank(HttpUtils.getUserName(httpServletRequest)) ? HttpUtils.getUserName(httpServletRequest) : request.getOperateUser();
        User user = userDao.findByUsername(loginUser);
        if (user == null) {
            throw new UnExpectedRequestException("user : [" + loginUser + "] {&DOES_NOT_EXIST} or {&PLEASE_LOGIN}");
        }

        List<Project> projectsInDb = validateAndLockProjects(request.getProjectId(), loginUser, user);

        Set<Rule> rules = Sets.newHashSet();
        for (Project project : projectsInDb) {
            rules = getRules(request.getRuleIds(), request.getRuleNames(), project);
        }

        List<String> executionParamNames = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(rules)) {
            executionParamNames = rules.stream().filter(rule -> StringUtils.isNotEmpty(rule.getExecutionParametersName())).map(rule -> rule.getExecutionParametersName()).collect(Collectors.toList());
        }

        // Generate file in temp path and zip them
        String tmp = UuidGenerator.generate();
        String zipFileName = tmp + QualitisConstants.SUPPORT_ZIP_SUFFIX_NAME;

        StringBuilder tempDirForGenFiles = new StringBuilder();
        tempDirForGenFiles.append(linkisConfig.getUploadTmpPath()).append(File.separator).append(loginUser).append(File.separator).append(tmp);

        File forGenFilesDirFile = new File(tempDirForGenFiles.toString());
        if (!forGenFilesDirFile.exists()) {
            forGenFilesDirFile.mkdirs();
        }

        // Get excel content
        List<ExcelProject> excelProject = getExcelProject(projectsInDb);
        List<ExcelRuleMetric> excelRuleMetrics = getRuleMetric(projectsInDb, request.getRuleIds(), request.getRuleNames());
        List<ExcelStandardValue> standardVaules = getExcelStandardValue(projectsInDb, request.getRuleIds(), request.getRuleNames());
        List<ExcelDatasourceEnv> excelDatasourceEnvs = getDataSourceSheet(projectsInDb, request.getRuleIds(), request.getRuleNames());
        List<ExcelRuleUdf> excelRuleUdfs = getExcelUdf(projectsInDb, forGenFilesDirFile, request.getRuleIds(), request.getRuleNames());
        List<ExcelGroupByProject> excelGroupByProjects = getGroup(projectsInDb, request.getDiffVariableRequestList(), request.getRuleIds(), request.getRuleNames());
        List<ExcelRuleByProject> excelRuleByProject = getExcelRuleByProject(projectsInDb, request.getDiffVariableRequestList(), request.getRuleIds(), request.getRuleNames());
        List<ExcelExecutionParametersByProject> excelExecutionParametersByProject = getExecutionParameters(projectsInDb, CollectionUtils.isNotEmpty(executionParamNames) ? executionParamNames : Collections.emptyList(), true);

        generateFiles(forGenFilesDirFile, excelRuleUdfs, excelProject, excelRuleMetrics, excelGroupByProjects, excelRuleByProject, excelExecutionParametersByProject, standardVaules
                , request.getDiffVariableRequestList(), excelDatasourceEnvs);
        String operateComment;
        if (ProjectTransportTypeEnum.LOCAL.getCode().equals(request.getDownloadType())) {
            writeZipToResponse(tempDirForGenFiles.toString(), zipFileName, forGenFilesDirFile, response);
            operateComment = ProjectTransportTypeEnum.LOCAL.name();
        } else if (ProjectTransportTypeEnum.GIT.getCode().equals(request.getDownloadType())) {
            // Git download not currently supported
            operateComment = ProjectTransportTypeEnum.GIT.name();
        } else {
            throw new UnExpectedRequestException("Not support download type.");
        }

        projectEventService.recordBatch(projectsInDb, loginUser, operateComment, OperateTypeEnum.EXPORT_PROJECT);
        // update the running status of the project to normal
        projectService.batchSaveAndFlushProject(projectsInDb);
        return new GeneralResponse<>(ResponseStatusConstants.OK, "SUCCESS", null);
    }

    private List<Project> validateAndLockProjects(List<Long> projectIds, String loginUser, User user)
            throws UnExpectedRequestException, PermissionDeniedRequestException {
        List<Project> projectsInDb = new ArrayList<>();
        for (Long projectId : projectIds) {
            Project projectInDb = projectDao.findById(projectId);
            if (projectInDb == null) {
                throw new UnExpectedRequestException("{&PROJECT_ID} : [" + projectId + "] {&DOES_NOT_EXIST}");
            }
            List<Integer> permissions = new ArrayList<>();
            permissions.add(ProjectUserPermissionEnum.DEVELOPER.getCode());
            projectService.checkProjectPermission(projectInDb, loginUser, permissions);

            checkServiceIsNotabnormalAndRestoredProjectStatus(projectInDb);

            if (ProjectStatusEnum.INOPERABLE_STATUS.getCode().equals(projectInDb.getRunStatus())) {
                throw new UnExpectedRequestException("{&PROJECT_ID} : [" + projectId + "] import or export operation in progress, cannot repeat operation");
            }
            projectInDb.setRunStatus(ProjectStatusEnum.INOPERABLE_STATUS.getCode());

            setUserModifyInfo(user, projectInDb);
            projectService.saveAndFlushProject(projectInDb);

            projectsInDb.add(projectInDb);
        }
        return projectsInDb;
    }

    private void writeZipToResponse(String tempDir, String zipFileName, File genFilesDir, HttpServletResponse response) throws IOException {
        String zipFilePath = tempDir + QualitisConstants.SUPPORT_ZIP_SUFFIX_NAME;
        FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8);
        addFolderToZip(tempDir, "", zos);
        zos.close();
        fos.close();

        response.setContentType("application/octet-stream");
        String encodedFileName = URLEncoder.encode(zipFileName, "UTF-8");
        response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.addHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        try (InputStream inputStream = new FileInputStream(zipFilePath);
             OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
        File zipFile = new File(zipFilePath);
        if (zipFile.exists()) {
            LOGGER.info("Delete: {}", zipFile.getName());
            Files.delete(zipFile.toPath());
        }
        deleteDirectory(genFilesDir);
    }

    private List<ExcelRuleMetric> getRuleMetric(List<Project> projectsInDb, List<Long> ruleId, List<String> ruleName) throws IOException {
        List<ExcelRuleMetric> excelRuleMetrics = new ArrayList<>();
        for (Project project : projectsInDb) {
            Set<Rule> rules = getRules(ruleId, ruleName, project);

            List<RuleMetric> ruleMetrics = rules.stream().map(rule -> rule.getAlarmConfigs()).flatMap(alarmConfigs -> alarmConfigs.stream()).filter(alarmConfig -> alarmConfig.getRuleMetric() != null).map(alarmConfig -> alarmConfig.getRuleMetric()).distinct().collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(ruleMetrics)) {
                for (RuleMetric ruleMetric : ruleMetrics) {
                    ExcelRuleMetric excelRuleMetric = new ExcelRuleMetric();
                    excelRuleMetric.setRuleMetricJsonObject(objectMapper.writeValueAsString(ruleMetric));
                    List<DataVisibility> dataVisibilityList = dataVisibilityService.filter(ruleMetric.getId(), TableDataTypeEnum.RULE_METRIC);

                    if (CollectionUtils.isNotEmpty(dataVisibilityList)) {
                        excelRuleMetric.setDataVisibilityJsonObject(objectMapper.writeValueAsString(dataVisibilityList));
                    }
                    excelRuleMetrics.add(excelRuleMetric);
                }
            }
        }
        return excelRuleMetrics;
    }

    private List<ExcelRuleUdf> getExcelUdf(List<Project> projectsInDb, File zipDirFile, List<Long> ruleId, List<String> ruleName) throws IOException {
        List<ExcelRuleUdf> excelRuleUdfs = new ArrayList<>();
        return excelRuleUdfs;
    }

    private void deleteDirectory(File directory) {
        Path rootPath = Paths.get(directory.getPath());

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    LOGGER.info("Delete file: " + file.toString());
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    LOGGER.info("delete dir: " + dir.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to delete dir: " + directory.getPath());
        }
    }

    @Override
    public GeneralResponse diffVariables() {
        List<DiffVariable> diffVariables = diffVariableRepository.findAll();
        return new GeneralResponse(ResponseStatusConstants.OK, "Success to list diff variables", diffVariables);
    }

    @Override
    public List<Project> checkProjects(List<Long> projectIds) throws UnExpectedRequestException {
        List<Project> projectInDbs = Lists.newArrayList();
        for (Long projectId : projectIds) {
            Project projectInDb = projectDao.findById(projectId);
            if (projectInDb == null) {
                throw new UnExpectedRequestException("{&PROJECT_ID} : [" + projectId + "] {&DOES_NOT_EXIST}");
            }
            projectInDbs.add(projectInDb);
        }
        return projectInDbs;
    }

    private void addFolderToZip(String folderPath, String parentFolderPath, ZipOutputStream zos) throws IOException {
        File folder = new File(folderPath);
        String folderName = folder.getName();

        // Create a new ZipEntry for the folder
        ZipEntry zipEntry = new ZipEntry(parentFolderPath + folderName + "/");

        // Add the ZipEntry to the ZipOutputStream
        zos.putNextEntry(zipEntry);

        // Traverse the folder and add all files to the zip file
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                addFolderToZip(file.getAbsolutePath(), parentFolderPath + folderName + "/", zos);
            } else {
                byte[] buffer = new byte[1024];
                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(parentFolderPath + folderName + "/" + file.getName()));
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }

            }
        }
    }

    private void generateFiles(File zipDirFile,
        List<ExcelRuleUdf> excelRuleUdfs,
        List<ExcelProject> excelProject,
        List<ExcelRuleMetric> excelRuleMetrics,
        List<ExcelGroupByProject> excelGroupByProjects,
        List<ExcelRuleByProject> excelRulesByProject,
        List<ExcelExecutionParametersByProject> excelExecutionParametersByProject,
        List<ExcelStandardValue> standardValues,
        List<DiffVariableRequest> diffVariableRequestList,
        List<ExcelDatasourceEnv> excelDatasourceEnvs) throws IOException {
        if (CollectionUtils.isNotEmpty(diffVariableRequestList)) {
            List<DiffVariableRequest> systemInnerDiffVariableRequestList = diffVariableRequestList.stream().filter(
                diffVariableRequest -> DiffRequestTypeEnum.SYSTEM_INNER.getCode().equals(diffVariableRequest.getType())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(systemInnerDiffVariableRequestList)) {
                savePropertiesFiles(systemInnerDiffVariableRequestList, zipDirFile, "project-info", DiffRequestTypeEnum.SYSTEM_INNER.getPrefixFile());
            }
            List<DiffVariableRequest> datasourceEnvDiffVariableRequestList = diffVariableRequestList.stream().filter(
                diffVariableRequest -> DiffRequestTypeEnum.DATASOURCE_ENV.getCode().equals(diffVariableRequest.getType())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(datasourceEnvDiffVariableRequestList)) {
                savePropertiesFiles(datasourceEnvDiffVariableRequestList, zipDirFile, "project-cus-datasource", DiffRequestTypeEnum.DATASOURCE_ENV.getPrefixFile());
            }
            List<DiffVariableRequest> sqlDiffVariableRequestList = diffVariableRequestList.stream().filter(
                diffVariableRequest -> DiffRequestTypeEnum.SQL_REPLACEMENT.getCode().equals(diffVariableRequest.getType())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(sqlDiffVariableRequestList)) {
                savePropertiesFiles(sqlDiffVariableRequestList, zipDirFile, "project-rule", DiffRequestTypeEnum.SQL_REPLACEMENT.getPrefixFile());
            }
            List<DiffVariableRequest> jsonDiffVariableRequestList = diffVariableRequestList.stream().filter(
                diffVariableRequest -> DiffRequestTypeEnum.JSON_REPLACEMENT.getCode().equals(diffVariableRequest.getType())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(jsonDiffVariableRequestList)) {
                savePropertiesFiles(jsonDiffVariableRequestList, zipDirFile, "project-rule", DiffRequestTypeEnum.JSON_REPLACEMENT.getPrefixFile());
            }
        }
        writeExcelSheet(zipDirFile, "project-cus-udf", "batch_udf_export", ExcelSheetName.RULE_UDF, ExcelRuleUdf.class, excelRuleUdfs, "udf");
        writeExcelSheet(zipDirFile, "project-info", "batch_project_export", ExcelSheetName.PROJECT_NAME, ExcelProject.class, excelProject, "project");
        writeExcelSheet(zipDirFile, "project-ref-metric", "batch_metric_export", ExcelSheetName.RULE_METRIC_NAME, ExcelRuleMetric.class, excelRuleMetrics, "metric");
        writeRuleAndGroupExcelFile(zipDirFile, excelRulesByProject, excelGroupByProjects);
        writeExcelSheet(zipDirFile, "project-parameter", "batch_execution_parameters_export", ExcelSheetName.EXECUTION_PARAMETERS_NAME, ExcelExecutionParametersByProject.class, excelExecutionParametersByProject, "execution parameter");
        writeExcelSheet(zipDirFile, "project-cus-datasource", "batch_datasource_env_export", ExcelSheetName.DATASOURCE_ENV, ExcelDatasourceEnv.class, excelDatasourceEnvs, "datasource env");
        writeExcelSheet(zipDirFile, "project-cus-standard-value", "batch_standard_value_export", ExcelSheetName.STANDARD_VAULE, ExcelStandardValue.class, standardValues, "standard value");
    }

    private <T> void writeExcelSheet(File zipDirFile, String dirName, String fileName,
                                      String sheetName, Class<T> clazz, List<T> data, String description) throws IOException {
        if (CollectionUtils.isEmpty(data)) {
            return;
        }
        LOGGER.info("Start to write {} excel", description);

        File projectDir = new File(zipDirFile, dirName);
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }

        String fullFileName = fileName + QualitisConstants.SUPPORT_EXCEL_SUFFIX_NAME;
        File projectExcel = new File(projectDir, fullFileName);
        if (!projectExcel.exists()) {
            boolean newFile = projectExcel.createNewFile();
            if (!newFile) {
                LOGGER.error("{&FAILED_TO_CREATE_NEW_FILE}");
            }
        }

        ExcelWriter writer = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(projectDir.getPath().concat(File.separator).concat(fullFileName));
            writer = EasyExcelFactory.getWriter(fos);
            Sheet sheet = new Sheet(1, 1, clazz);
            sheet.setSheetName(sheetName);
            writer.write(data, sheet);
            LOGGER.info("Finish to write {} excel", description);
        } catch (FileNotFoundException e) {
            LOGGER.error("Failed to write {} content to excel.", description);
        } finally {
            if (writer != null) {
                writer.finish();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    private void writeRuleAndGroupExcelFile(File zipDirFile, List<ExcelRuleByProject> excelRulesByProject,
                                             List<ExcelGroupByProject> excelGroupByProjects) throws IOException {
        if (CollectionUtils.isEmpty(excelRulesByProject)) {
            return;
        }
        LOGGER.info("Start to write rule excel");

        File projectDir = new File(zipDirFile, "project-rule");
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }

        String fileName = "batch_rules_export" + QualitisConstants.SUPPORT_EXCEL_SUFFIX_NAME;
        File projectExcel = new File(projectDir, fileName);
        if (!projectExcel.exists()) {
            boolean newFile = projectExcel.createNewFile();
            if (!newFile) {
                LOGGER.error("{&FAILED_TO_CREATE_NEW_FILE}");
            }
        }

        ExcelWriter writer = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(projectDir.getPath().concat(File.separator).concat(fileName));
            writer = EasyExcelFactory.getWriter(fos);
            Sheet sheetRule = new Sheet(1, 1, ExcelRuleByProject.class);
            sheetRule.setSheetName(ExcelSheetName.RULE_NAME);
            writer.write(excelRulesByProject, sheetRule);

            if (CollectionUtils.isNotEmpty(excelGroupByProjects)) {
                LOGGER.info("Start to write group excel");
                Sheet sheetGroup = new Sheet(2, 1, ExcelGroupByProject.class);
                sheetGroup.setSheetName(ExcelSheetName.TABLE_GROUP);
                writer.write(excelGroupByProjects, sheetGroup);
                LOGGER.info("Finish to write group excel");
            }
            LOGGER.info("Finish to write rule excel");
        } catch (FileNotFoundException e) {
            LOGGER.error("Failed to write rule & group to excel.");
        } finally {
            if (writer != null) {
                writer.finish();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    private void savePropertiesFiles(List<DiffVariableRequest> diffVariableRequestList, File zipDirFile, String childDirName, String prefix)
        throws IOException {
        File projectDir = new File(zipDirFile, childDirName);
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }
        Properties prop = new Properties();
        String fileName = prefix + "diff_variables_config.properties";
        File diffVariablesConfigFile = new File(projectDir, fileName);
        if (diffVariablesConfigFile.exists()) {
            Files.delete(diffVariablesConfigFile.toPath());
            LOGGER.info("Delete old diff variables config file {}", diffVariablesConfigFile.getName());
        }
        if (!diffVariablesConfigFile.exists()) {
            boolean newFile = diffVariablesConfigFile.createNewFile();
            if (!newFile) {
                LOGGER.error("{&FAILED_TO_CREATE_NEW_FILE}");
            }
        }
        try (OutputStream output = new FileOutputStream(diffVariablesConfigFile)) {
            // 设置属性值
            for (DiffVariableRequest diffVariableRequest : diffVariableRequestList) {
                prop.setProperty(diffVariableRequest.getName(), "[@" + diffVariableRequest.getName() + "]");
            }

            // 将属性写入文件
            prop.store(output, null);

            LOGGER.info(fileName + " 差异化变量配置文件已创建并写入完成！");
        } catch (IOException io) {
            LOGGER.error(io.getMessage(), io);
        }
    }

    private List<File> checkFileAndUnzip(String zipPath) throws UnExpectedRequestException, IOException {
        List<File> extractedFileList = new ArrayList<>();
        if (StringUtils.isEmpty(zipPath) || !zipPath.endsWith(QualitisConstants.SUPPORT_ZIP_SUFFIX_NAME)) {
            throw new UnExpectedRequestException("Zip path is illegal");
        }

        File zipFile = new File(zipPath);
        if (!zipFile.exists()) {
            throw new UnExpectedRequestException("Zip file {&DOES_NOT_EXIST}");
        }

        // Unzip the zip file and get excels.
        try {
            extract(zipFile.getPath(), extractedFileList, zipFile.getParentFile().getPath());
        } catch (Exception e) {
            LOGGER.error("Failed to extracte zip files.", e);
            throw new UnExpectedRequestException("Failed to get zip files.");
        } finally {
            if (zipFile.exists()) {
                Files.delete(zipFile.toPath());
            }
        }

        return extractedFileList;
    }

    private void extract(String zipFilePath, List<File> extractedFileList, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath), StandardCharsets.UTF_8)) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                    extractedFileList.add(new File(filePath));
                } else {
                    File dir = new File(filePath);
                    dir.mkdir();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }

    }

    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[4096];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }

    }

    private List<ExcelDatasourceEnv> getDataSourceSheet(List<Project> projects, List<Long> ruleId, List<String> ruleName) {
        List<ExcelDatasourceEnv> excelRuleDataSourceList = Lists.newArrayList();
        try {
            List<RuleDataSource> allRuleDataSourceList = Lists.newArrayList();
            for (Project project : projects) {
                Set<Rule> rules = Sets.newHashSet();
                if (CollectionUtils.isNotEmpty(ruleId)) {
                    rules.addAll(ruleDao.findByIdsAndProject(ruleId, project.getId()));
                }
                if (CollectionUtils.isNotEmpty(ruleName)) {
                    for (String name : ruleName) {
                        Rule rule = ruleDao.findByProjectAndRuleName(project, name);
                        if (null != rule) {
                            rules.add(rule);
                        }

                    }
                }

                List<RuleDataSource> ruleDataSourceList;
                if (CollectionUtils.isEmpty(rules)) {
                    ruleDataSourceList = ruleDataSourceDao.findByProjectId(project.getId());
                } else {
                    List<Long> ruleIds = rules.stream().map(Rule::getId).collect(Collectors.toList());
                    ruleDataSourceList = ruleDataSourceDao.findByRuleId(ruleIds);
                }

                if (CollectionUtils.isEmpty(ruleDataSourceList)) {
                    continue;
                }
                allRuleDataSourceList.addAll(ruleDataSourceList);
            }
//                导出规则依赖的数据源
            List<Long> linkisDataSourceIds = allRuleDataSourceList.stream().map(RuleDataSource::getLinkisDataSourceId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            List<LinkisDataSource> linkisDataSourceInDbList = linkisDataSourceService.getByLinkisDataSourceIds(linkisDataSourceIds);

//                导出规则依赖的环境
            List<RuleDataSourceEnv> ruleDataSourceEnvList = ruleDatasourceEnvDao.findByRuleDataSourceList(allRuleDataSourceList);
//                将规则依赖的环境名称，填充到依赖的数据源中去，而不是直接导出数据源下的所有环境信息
            Map<String, List<RuleDataSourceEnv>> linkisDataSourceNameMap = ruleDataSourceEnvList.stream().collect(Collectors.groupingBy(ruleDataSourceEnv -> ruleDataSourceEnv.getRuleDataSource().getLinkisDataSourceName()));
            Map<String, List<String>> dsNameAndEnvsMap = new HashMap<>();
            Map<String, List<String>> dsNameAndDcnRangeMap = new HashMap<>();
            for (LinkisDataSource linkisDataSource : linkisDataSourceInDbList) {
                if (!linkisDataSourceNameMap.containsKey(linkisDataSource.getLinkisDataSourceName())) {
                    continue;
                }
                List<RuleDataSourceEnv> ruleDataSourceEnvs = linkisDataSourceNameMap.get(linkisDataSource.getLinkisDataSourceName());
                if (Arrays.asList(QualitisConstants.CMDB_KEY_DCN_NUM, QualitisConstants.CMDB_KEY_LOGIC_AREA).contains(linkisDataSource.getDcnRangeType())) {
                    List<Long> envIds = ruleDataSourceEnvs.stream().map(RuleDataSourceEnv::getEnvId).collect(Collectors.toList());
                    GetLinkisDataSourceEnvRequest getLinkisDataSourceEnvRequest = new GetLinkisDataSourceEnvRequest();
                    getLinkisDataSourceEnvRequest.setLinkisDataSourceId(linkisDataSource.getLinkisDataSourceId());
                    getLinkisDataSourceEnvRequest.setEnvIdList(envIds);
                    List<LinkisDataSourceEnv> linkisDataSourceEnvList = linkisDataSourceEnvService.queryEnvsInAdvance(getLinkisDataSourceEnvRequest);
                    List<String> dcnRangeValues = linkisDataSourceEnvList.stream().map(linkisDataSourceEnv -> {
                        if (QualitisConstants.CMDB_KEY_DCN_NUM.equals(linkisDataSource.getDcnRangeType())) {
                            return linkisDataSourceEnv.getDcnNum();
                        } else {
                            return linkisDataSourceEnv.getLogicArea();
                        }
                    }).distinct().collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(dcnRangeValues)) {
                        dsNameAndDcnRangeMap.put(linkisDataSource.getLinkisDataSourceName(), dcnRangeValues);
                    }
                } else {
                    List<String> envNames = ruleDataSourceEnvs.stream().map(RuleDataSourceEnv::getEnvName).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(envNames)) {
                        dsNameAndEnvsMap.put(linkisDataSource.getLinkisDataSourceName(), envNames);
                    }
                }
            }

//            设置部门可见范围
            List<Long> linkisDataSourcePrimaryIds = linkisDataSourceInDbList.stream().map(LinkisDataSource::getId).collect(Collectors.toList());
            List<DataVisibility> dataVisibilityList = dataVisibilityService.filterByIds(linkisDataSourcePrimaryIds, TableDataTypeEnum.LINKIS_DATA_SOURCE);
            Map<Long, List<DataVisibility>> dtaVisibilityListMap = dataVisibilityList.stream().collect(Collectors.groupingBy(DataVisibility::getTableDataId));

            for (LinkisDataSource linkisDataSource : linkisDataSourceInDbList) {
                ExcelDatasourceEnv excelDatasourceEnv = ExcelDatasourceEnv.fromLinkisDataSource(linkisDataSource, dtaVisibilityListMap.get(linkisDataSource.getId()));
                if (dsNameAndEnvsMap.containsKey(linkisDataSource.getLinkisDataSourceName())) {
                    excelDatasourceEnv.setDatasourceEnvNameListJsonObject(objectMapper.writeValueAsString(dsNameAndEnvsMap.get(linkisDataSource.getLinkisDataSourceName())));
                }
                if (dsNameAndDcnRangeMap.containsKey(linkisDataSource.getLinkisDataSourceName())) {
                    excelDatasourceEnv.setDcnRangeValueListJsonObject(objectMapper.writeValueAsString(dsNameAndDcnRangeMap.get(linkisDataSource.getLinkisDataSourceName())));
                }
                excelRuleDataSourceList.add(excelDatasourceEnv);
            }

        } catch (IOException e) {
            LOGGER.error("Failed to generate JSON of rule datasource", e);
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        return excelRuleDataSourceList;
    }

    /**
     * Check if there are some envs that exist in the diffVariableRequestList that will be replaced.
     * The variable must be meet the specific pattern:
     *          The variable name: data_source_name[env_name1, nv_name2];
     *          The variable value: replace_env_name1,replace_env_name2
     * @param diffVariableRequestList
     * @return {key: dataSourceName, value：{key: left env, value: right env}}
     */
    private Map<String, Map<String, String>> getDataSourceEnvVariableMapping(List<DiffVariableRequest> diffVariableRequestList) throws UnExpectedRequestException {
        Map<String, Map<String, String>> dsAndVariableEnvsMap = new HashMap<>();
        for (DiffVariableRequest diffVariableRequest: diffVariableRequestList) {
            if (StringUtils.isBlank(diffVariableRequest.getValue())) {
                continue;
            }
            Matcher matcherName = QualitisConstants.DATASOURCE_DIFF_VARIABLE_PATTERN.matcher(diffVariableRequest.getName());
            if (matcherName.find()) {
                String datasourceVariableName = matcherName.group(0);
                Integer leftBracketIndex = datasourceVariableName.indexOf(SpecCharEnum.LEFT_BRACKET.getValue());
                String dsName = datasourceVariableName.substring(0, leftBracketIndex);
                String envNames = datasourceVariableName.substring(leftBracketIndex+1,datasourceVariableName.indexOf(SpecCharEnum.RIGHT_BRACKET.getValue()));
                String[] variableNameEnvs = StringUtils.split(envNames, SpecCharEnum.COMMA.getValue());
                String[] variableValueEnvs = StringUtils.split(diffVariableRequest.getValue(), SpecCharEnum.COMMA.getValue());

                if (variableNameEnvs.length != variableValueEnvs.length) {
                    String formatErrorMsg = String.format("The number of envs on both sides are not equal, variable name: %s", diffVariableRequest.getName());
                    throw new UnExpectedRequestException(formatErrorMsg);
                }
                Map<String, String> variableAndReplaceEnvMap = Maps.newHashMapWithExpectedSize(variableNameEnvs.length);
                for (int i=0;i<variableNameEnvs.length;i++) {
                    variableAndReplaceEnvMap.put(variableNameEnvs[i], variableValueEnvs[i]);
                }
                dsAndVariableEnvsMap.put(dsName, variableAndReplaceEnvMap);
            }
        }
        return dsAndVariableEnvsMap;
    }

    private void replaceImportedEnvsWithDiffVariable(List<String> importedEnvs, Map<String, String> variableAndReplaceEnvMapping) {
        Iterator<Map.Entry<String, String>> it = variableAndReplaceEnvMapping.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> variableAndReplaceEnvEntry = it.next();
            if (StringUtils.isNotBlank(variableAndReplaceEnvEntry.getValue())) {
                importedEnvs.remove(variableAndReplaceEnvEntry.getKey());
                importedEnvs.add(variableAndReplaceEnvEntry.getValue());
            }
        }
    }

    private void handleDataSource(User user, List<ExcelDatasourceEnv> excelRuleDataSourceList, List<DiffVariableRequest> diffVariableRequestList, Map<Long, List<Long>> oldIdAndNewEnvIdsMapHook) throws UnExpectedRequestException, MetaDataAcquireFailedException {
        Map<String, Map<String, String>> dsAndDiffVariableEnvsMap = getDataSourceEnvVariableMapping(diffVariableRequestList);

        String operateTime = DateUtils.now();
        for (ExcelDatasourceEnv excelDatasourceEnv : excelRuleDataSourceList) {
            LinkisDataSource importLinkisDataSource = excelDatasourceEnv.getLinkisDataSource(objectMapper);
            if (null == importLinkisDataSource) {
                throw new UnExpectedRequestException("Failed to deserialize LinkisDataSource");
            }
            Long oldLinkisDataSourceId = importLinkisDataSource.getLinkisDataSourceId();
            List<String> oldDcnRangeValueList = excelDatasourceEnv.getDcnRangeValuesList(objectMapper);

            LinkisDataSource linkisDataSourceInDb = linkisDataSourceService.getByLinkisDataSourceName(importLinkisDataSource.getLinkisDataSourceName());

//            Create or Update LinkisDataSource ?
            if (linkisDataSourceInDb == null) {
                importLinkisDataSource.setCreateUser(user.getUsername());
                importLinkisDataSource.setCreateTime(operateTime);
            } else {
                importLinkisDataSource.setId(linkisDataSourceInDb.getId());
                importLinkisDataSource.setModifyUser(user.getUsername());
                importLinkisDataSource.setModifyTime(operateTime);
                importLinkisDataSource.setVersionId(linkisDataSourceInDb.getVersionId());
                importLinkisDataSource.setCreateUser(linkisDataSourceInDb.getCreateUser());
                importLinkisDataSource.setCreateTime(linkisDataSourceInDb.getCreateTime());
            }

            Long linkisDataSourceId = pushDataSourceToLinkis(importLinkisDataSource);
            if (linkisDataSourceId == null) {
                throw new UnExpectedRequestException("Failed to push dataSource to Linkis: linkisDataSourceId is null.");
            }
            importLinkisDataSource.setLinkisDataSourceId(linkisDataSourceId);

            saveNewEnvAndPushToLinkis(linkisDataSourceId, importLinkisDataSource, linkisDataSourceInDb, excelDatasourceEnv, dsAndDiffVariableEnvsMap);

//            Querying all env from db, contain old and new envs
            List<String> envIdArray = new ArrayList<>();
            List<LinkisDataSourceEnv> linkisDataSourceEnvListInDb = linkisDataSourceEnvService.queryAllEnvs(linkisDataSourceId);
            if (CollectionUtils.isNotEmpty(linkisDataSourceEnvListInDb)) {
                linkisDataSourceEnvListInDb.forEach(linkisDataSourceEnv -> envIdArray.add(String.valueOf(linkisDataSourceEnv.getEnvId())));
            }

            Long versionId = updateDataSourceParamWithEnvIdsToLinkis(importLinkisDataSource, envIdArray);
            importLinkisDataSource.setVersionId(versionId);
            linkisDataSourceService.save(importLinkisDataSource);

//            To publish datasource with the newest versionId
            if (Integer.valueOf(QualitisConstants.DATASOURCE_MANAGER_VERIFY_TYPE_SHARE).equals(importLinkisDataSource.getVerifyType())
                && linkisDataSourceInDb != null) {
                metaDataClient.publishDataSource(linkisConfig.getDatasourceCluster(), linkisConfig.getDatasourceAdmin(), importLinkisDataSource.getLinkisDataSourceId(), versionId);
            }

            // The following business logic for replaceEnvsInRuleDataSource()
            List<Long> newEnvIdsWithinOldDcnRangeValues = linkisDataSourceEnvListInDb.stream().filter(linkisDataSourceEnv -> {
                if (QualitisConstants.CMDB_KEY_DCN_NUM.equals(importLinkisDataSource.getDcnRangeType())) {
                    return oldDcnRangeValueList.contains(linkisDataSourceEnv.getDcnNum());
                }
                return oldDcnRangeValueList.contains(linkisDataSourceEnv.getLogicArea());
            }).map(LinkisDataSourceEnv::getEnvId).collect(Collectors.toList());
            oldIdAndNewEnvIdsMapHook.put(oldLinkisDataSourceId, newEnvIdsWithinOldDcnRangeValues);

            updateDataVisibilityOfDatSource(importLinkisDataSource.getId(), excelDatasourceEnv);
        }
    }
    
    private void updateDataVisibilityOfDatSource(Long dataId, ExcelDatasourceEnv excelDatasourceEnv) {
        List<DataVisibility> importDataVisibilityList = excelDatasourceEnv.getDataVisibilityList(objectMapper);
        if (CollectionUtils.isNotEmpty(importDataVisibilityList)) {
            List<DataVisibility> dataVisibilityListInDb = dataVisibilityService.filter(dataId, TableDataTypeEnum.LINKIS_DATA_SOURCE);
            List<String> deptNamesInDb = dataVisibilityListInDb.stream().map(DataVisibility::getDepartmentSubName).collect(Collectors.toList());
            List<DataVisibility> importNewDataVisibilityList = importDataVisibilityList.stream()
                    .filter(importDataVisibility -> !deptNamesInDb.contains(importDataVisibility.getDepartmentSubName()))
                    .map(importDataVisibility -> {
                        importDataVisibility.setTableDataId(dataId);
                        return importDataVisibility;
                    }).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(importNewDataVisibilityList)) {
                dataVisibilityDao.saveAll(importNewDataVisibilityList);
            }
        }
    }

    private List<String> saveNewEnvAndPushToLinkis(Long linkisDataSourceId, LinkisDataSource importLinkisDataSource, LinkisDataSource linkisDataSourceInDb, ExcelDatasourceEnv excelDatasourceEnv, Map<String, Map<String, String>> dsAndDiffVariableEnvsMap) throws UnExpectedRequestException {
        List<LinkisDataSourceEnvRequest> newDataSourceEnvRequestList = Collections.emptyList();

//        To save envs of dataSource, Either by a range of dcn(dcn_num or logic_area), or by individual envs
        if (Arrays.asList(QualitisConstants.CMDB_KEY_DCN_NUM, QualitisConstants.CMDB_KEY_LOGIC_AREA).contains(importLinkisDataSource.getDcnRangeType())
                && Integer.valueOf(QualitisConstants.DATASOURCE_MANAGER_INPUT_TYPE_AUTO).equals(importLinkisDataSource.getInputType())) {
            List<String> dcnRangeValueList = excelDatasourceEnv.getDcnRangeValuesList(objectMapper);
            LOGGER.info("Get imported dcnRangeValueList: {}", StringUtils.join(dcnRangeValueList, ","));
//          Is the LinkisDataSource new or not? if not, check if there were new dcn_num or logic_area.
            if (linkisDataSourceInDb != null) {
                List<LinkisDataSourceEnv> linkisDataSourceEnvList = linkisDataSourceEnvService.queryAllEnvs(linkisDataSourceInDb.getLinkisDataSourceId());
                List<String> dcnRangeValueListInDb = linkisDataSourceEnvList.stream().map(linkisDataSourceEnv -> {
                    if (QualitisConstants.CMDB_KEY_DCN_NUM.equals(linkisDataSourceInDb.getDcnRangeType())) {
                        return linkisDataSourceEnv.getDcnNum();
                    } else {
                        return linkisDataSourceEnv.getLogicArea();
                    }
                }).distinct().collect(Collectors.toList());
                dcnRangeValueList.removeAll(dcnRangeValueListInDb);
            }
            LOGGER.info("After estimated these dcn_range_values stored in db, the rest of the imported dcnRangeValueList is: {}", StringUtils.join(dcnRangeValueList, ","));

//            To fetch DCN instances with dcnRangeValueList and subSystem from cmdb
            if (CollectionUtils.isNotEmpty(dcnRangeValueList)) {
                String subSystemId = operateCiService.getSubSystemIdByName(importLinkisDataSource.getSubSystem());
                LOGGER.info("To get the ID of subSystem [{}]. The subSystemId is: {}", importLinkisDataSource.getSubSystem(), subSystemId);
                if (Objects.nonNull(subSystemId)) {
                    GeneralResponse generalResponse = operateCiService.getDcn(subSystemId, importLinkisDataSource.getDcnRangeType(), dcnRangeValueList);
                    Map<Object, List<Map<String, Object>>> dcnRangeTypeAndDCNMap = (Map<Object, List<Map<String, Object>>>) generalResponse.getData();
                    if (MapUtils.isNotEmpty(dcnRangeTypeAndDCNMap)) {
                        List<Map<String, Object>> dcnList = new ArrayList<>();
                        dcnRangeTypeAndDCNMap.values().forEach(dcnList::addAll);
                        newDataSourceEnvRequestList = dcnList.stream().map(entry -> {
                            LinkisDataSourceEnvRequest dataSourceEnv = new LinkisDataSourceEnvRequest();
                            dataSourceEnv.setDcnNum(MapUtils.getString(entry, QualitisConstants.CMDB_KEY_DCN_NUM));
                            dataSourceEnv.setLogicArea(MapUtils.getString(entry, QualitisConstants.CMDB_KEY_LOGIC_AREA));
                            dataSourceEnv.setDatabaseInstance(MapUtils.getString(entry, "dbinstance_name"));
                            dataSourceEnv.setDataSourceTypeId(importLinkisDataSource.getDataSourceTypeId());
                            LinkisConnectParamsRequest linkisConnectParamsRequest = new LinkisConnectParamsRequest();
                            linkisConnectParamsRequest.setHost(MapUtils.getString(entry, "vip"));
                            linkisConnectParamsRequest.setPort(MapUtils.getString(entry, "gwport"));
                            dataSourceEnv.setConnectParamsRequest(linkisConnectParamsRequest);
                            String linkisEnvName = linkisDataSourceService.convertOriginalEnvNameToLinkis(linkisDataSourceId
                                    , MapUtils.getString(entry, QualitisConstants.CMDB_KEY_DCN_NUM), importLinkisDataSource.getInputType()
                                    , dataSourceEnv.getConnectParamsRequest().getHost(), dataSourceEnv.getConnectParamsRequest().getPort(), dataSourceEnv.getDatabaseInstance());
                            dataSourceEnv.setEnvName(linkisEnvName);
                            return dataSourceEnv;
                        }).collect(Collectors.toList());
                    } else {
                        LOGGER.warn("Failed to fetch DCN list from cmdb, subSystemId is: {}, dcnRangeType: {}", subSystemId, importLinkisDataSource.getDcnRangeType());
                    }
                }
            }
        } else {
            List<String> linkisDataSourceEnvNameList = excelDatasourceEnv.getLinkisDataSourceEnvNameList(objectMapper);
            if (CollectionUtils.isEmpty(linkisDataSourceEnvNameList)) {
                throw new UnExpectedRequestException("Failed to deserialize LinkisDataSourceEnvNameList");
            }

//           Replace name of environments within imported project with diffVariable name
            boolean checkIfReplaceEnv = dsAndDiffVariableEnvsMap.containsKey(importLinkisDataSource.getLinkisDataSourceName());
            if (checkIfReplaceEnv) {
                Map<String, String> variableAndReplaceEnvMap = dsAndDiffVariableEnvsMap.get(importLinkisDataSource.getLinkisDataSourceName());
                replaceImportedEnvsWithDiffVariable(linkisDataSourceEnvNameList, variableAndReplaceEnvMap);
            }

            List<String> newImportEnvNames;
            if (linkisDataSourceInDb != null) {
                List<LinkisDataSourceEnv> linkisDataSourceEnvListInDb = linkisDataSourceEnvService.queryAllEnvs(linkisDataSourceInDb.getLinkisDataSourceId());
                List<String> originalEnvNameInDbList = linkisDataSourceEnvListInDb.stream().map(linkisDataSourceEnv ->
                        linkisDataSourceService.convertLinkisEnvNameToOriginal(linkisDataSourceInDb.getLinkisDataSourceId(), linkisDataSourceEnv.getEnvName(), linkisDataSourceInDb.getInputType())).collect(Collectors.toList());
                newImportEnvNames = extractNewEnvsFromImport(linkisDataSourceId, originalEnvNameInDbList, linkisDataSourceEnvNameList);
            } else {
                newImportEnvNames = linkisDataSourceEnvNameList.stream()
                        .map(envName -> linkisDataSourceId + SpecCharEnum.BOTTOM_BAR.getValue() + envName.replace(importLinkisDataSource.getLinkisDataSourceId()+SpecCharEnum.BOTTOM_BAR.getValue(), "")).collect(Collectors.toList());
            }

            newDataSourceEnvRequestList = newImportEnvNames.stream().map(linkisEnvName -> {
                LinkisDataSourceEnvRequest envRequest = new LinkisDataSourceEnvRequest();
                envRequest.setDataSourceTypeId(importLinkisDataSource.getDataSourceTypeId());
                envRequest.setEnvName(linkisEnvName);
                return envRequest;
            }).collect(Collectors.toList());
        }
        
//            Update envs if new envs exists.
        if (CollectionUtils.isNotEmpty(newDataSourceEnvRequestList)) {
            try {
                newDataSourceEnvRequestList = linkisMetaDataManager.createDataSourceEnvAndSetEnvId(importLinkisDataSource.getInputType(), importLinkisDataSource.getVerifyType(), newDataSourceEnvRequestList, linkisConfig.getDatasourceCluster(), linkisConfig.getDatasourceAdmin());
            } catch (UnExpectedRequestException | MetaDataAcquireFailedException e) {
                LOGGER.error("Failed to create dataSource env", e);
                throw new UnExpectedRequestException(e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Failed to create dataSource env", e);
                throw new UnExpectedRequestException("Failed to create dataSource env");
            }
            linkisDataSourceEnvService.createBatch(linkisDataSourceId, newDataSourceEnvRequestList);
            return newDataSourceEnvRequestList.stream().map(LinkisDataSourceEnvRequest::getId).filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private Long updateDataSourceParamWithEnvIdsToLinkis(LinkisDataSource importLinkisDataSource, List<String> envIdArray) throws UnExpectedRequestException {
        try {
//            modify parameter of DataSource, establish link between env_id and dataSource
            ModifyDataSourceParameterRequest modifyDataSourceParameterRequest = new ModifyDataSourceParameterRequest();
            modifyDataSourceParameterRequest.setLinkisDataSourceId(importLinkisDataSource.getLinkisDataSourceId());
            modifyDataSourceParameterRequest.setComment("Update");
            LinkisDataSourceInfoDetail linkisDataSourceInfoDetail = metaDataClient.getDataSourceInfoById(linkisConfig.getDatasourceCluster(), linkisConfig.getDatasourceAdmin(), importLinkisDataSource.getLinkisDataSourceId());
            modifyDataSourceParameterRequest.setConnectParams(linkisDataSourceInfoDetail.getConnectParams());
            modifyDataSourceParameterRequest.setEnvIdArray(envIdArray);
            LinkisDataSourceParamsResponse linkisDataSourceParamsResponse = linkisMetaDataManager.modifyDataSourceParams(modifyDataSourceParameterRequest, linkisConfig.getDatasourceCluster(), linkisConfig.getDatasourceAdmin());
            return linkisDataSourceParamsResponse.getVersionId();
        } catch (UnExpectedRequestException | MetaDataAcquireFailedException e) {
            LOGGER.error("Failed to update dataSource param", e);
            throw new UnExpectedRequestException(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to update dataSource param", e);
            throw new UnExpectedRequestException("Failed to update dataSource param");
        }
    }

    /**
     * To modify or create the LinkisDataSource on the Linkis-side
     *
     * @param importLinkisDataSource
     * @return
     */
    private Long pushDataSourceToLinkis(LinkisDataSource importLinkisDataSource) throws UnExpectedRequestException {
        try {
            LinkisDataSourceRequest linkisDataSourceRequest = buildLinkisDataSourceRequest(importLinkisDataSource);

            if (linkisDataSourceRequest.getLinkisDataSourceId() != null) {
                return linkisMetaDataManager.modifyDataSource(linkisDataSourceRequest, linkisConfig.getDatasourceCluster(), linkisConfig.getDatasourceAdmin());
            } else {
                try {
                    return linkisMetaDataManager.createDataSource(linkisDataSourceRequest, linkisConfig.getDatasourceCluster(), linkisConfig.getDatasourceAdmin());
                } catch (MetaDataAcquireFailedException e) {
                    LOGGER.error("Failed to create LinkisDataSource", e);
                    LOGGER.info("Query DataSource by name: {}", importLinkisDataSource.getLinkisDataSourceName());
                    Map<String, Object> infoMap = getLinkisDataSourceInfoByName(importLinkisDataSource.getLinkisDataSourceName());
                    if (MapUtils.isNotEmpty(infoMap)) {
                        return getLinkisDataSourceIdByInfo(infoMap);
                    }
                    LOGGER.info("DataSource is not existed, name: {}", importLinkisDataSource.getLinkisDataSourceName());
                }
            }
        } catch (MetaDataAcquireFailedException e) {
            throw new UnExpectedRequestException(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to push DataSource to Linkis", e);
            throw new UnExpectedRequestException("Failed to push DataSource to Linkis");
        }
        throw new UnExpectedRequestException("Failed to push DataSource to Linkis, dataSource name: " + importLinkisDataSource.getLinkisDataSourceName());
    }

    private Map<String, Object> getLinkisDataSourceInfoByName(String linkisDataSourceName) {
        GeneralResponse<Map<String, Object>> generalResponse = null;
        try {
            generalResponse = metaDataClient.getDataSourceInfoDetailByName(linkisConfig.getDatasourceCluster(), linkisConfig.getDatasourceAdmin(), linkisDataSourceName);
        } catch (UnExpectedRequestException e) {
            LOGGER.error(e.getMessage());
        } catch (MetaDataAcquireFailedException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        if (null != generalResponse) {
            Map<String, Object> dataMap = generalResponse.getData();
            if (Objects.nonNull(dataMap) && dataMap.containsKey("info")) {
                return ((Map) dataMap.get("info"));
            }
        }
        return Collections.emptyMap();
    }

    private Long getLinkisDataSourceIdByInfo(Map<String, Object> infoMap) {
        if (infoMap == null || !infoMap.containsKey("id")) {
            return null;
        }
        return Long.valueOf((Integer) infoMap.get("id"));
    }

    private LinkisDataSourceRequest buildLinkisDataSourceRequest(LinkisDataSource importLinkisDataSource) {
        Map<String, Object> connectParams = new HashMap<>();
        Map<String, Object> dataSourceInfo = getLinkisDataSourceInfoByName(importLinkisDataSource.getLinkisDataSourceName());
        if (MapUtils.isNotEmpty(dataSourceInfo) && dataSourceInfo.containsKey("connectParams")) {
            Map<String, Object> remoteConnectParams = ((Map) dataSourceInfo.get("connectParams"));
            String authType = String.valueOf(remoteConnectParams.get("authType"));
            connectParams.put("authType", authType);
//         Remaining the authentication info
            if (Boolean.TRUE.equals(remoteConnectParams.get("share")) && Integer.valueOf(QualitisConstants.DATASOURCE_MANAGER_VERIFY_TYPE_SHARE).equals(importLinkisDataSource.getVerifyType())) {
                if (QualitisConstants.AUTH_TYPE_ACCOUNT_PWD.equals(authType)) {
                    connectParams.put("username", remoteConnectParams.get("username"));
                    connectParams.put("password", remoteConnectParams.get("password"));
                } else if (QualitisConstants.AUTH_TYPE_DPM.equals(authType)) {
                    connectParams.put("appid", remoteConnectParams.get("appid"));
                    connectParams.put("objectid", remoteConnectParams.get("objectid"));
//                    To bypass the handling that re-generate the dk with mkPrivate, and remain the dk within connectParams so write into database, on the Linkis-side
                    String salt = QualitisConstants.AUTH_TYPE_DPM;
                    connectParams.put("mkPrivate", remoteConnectParams.get("mkPrivate") + salt);
                    connectParams.put("dk", remoteConnectParams.get("dk"));
                }
            }
        }

        connectParams.put("share", Integer.valueOf(QualitisConstants.DATASOURCE_MANAGER_VERIFY_TYPE_SHARE).equals(importLinkisDataSource.getVerifyType()));
        connectParams.put("dcn", Integer.valueOf(QualitisConstants.DATASOURCE_MANAGER_INPUT_TYPE_AUTO).equals(importLinkisDataSource.getInputType()));
        connectParams.put("multi_env", true);
        connectParams.put("subSystem", importLinkisDataSource.getSubSystem());

        LinkisDataSourceRequest linkisDataSourceRequest = new LinkisDataSourceRequest();
        linkisDataSourceRequest.setLinkisDataSourceId(getLinkisDataSourceIdByInfo(dataSourceInfo));
        linkisDataSourceRequest.setDataSourceTypeId(importLinkisDataSource.getDataSourceTypeId());
        linkisDataSourceRequest.setLabels(importLinkisDataSource.getLabels());
        linkisDataSourceRequest.setDataSourceName(importLinkisDataSource.getLinkisDataSourceName());
        linkisDataSourceRequest.setDataSourceDesc(importLinkisDataSource.getDatasourceDesc());
        linkisDataSourceRequest.setConnectParams(connectParams);
        return linkisDataSourceRequest;
    }

    private List<String> extractNewEnvsFromImport(Long linkisDataSourceId, List<String> originalEnvNameInDbList, List<String> importOriginalEnvNameList) {
        return importOriginalEnvNameList.stream()
                .filter(originalEnvName -> !originalEnvNameInDbList.contains(originalEnvName) && StringUtils.isNotBlank(originalEnvName))
                .distinct()
                .map(originalEnvName -> linkisDataSourceId + SpecCharEnum.BOTTOM_BAR.getValue() + originalEnvName)
                .collect(Collectors.toList());
    }
    /**
     * 验证服务是否正常且恢复项目状态为可操作
     *
     * @param project
     * @throws ParseException
     */
    private void checkServiceIsNotabnormalAndRestoredProjectStatus(Project project) throws ParseException {
        if (null == project.getModifyTime()) {
            return;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date currentTime = simpleDateFormat.parse(QualitisConstants.PRINT_TIME_FORMAT.format(new Date()));
        Date projectModifyTime = simpleDateFormat.parse(project.getModifyTime());

        // 从配置文件yml获timeOut(超时时间  单位：秒)
        String timeOut = environment.getProperty("import_and_export" + SpecCharEnum.PERIOD_NO_ESCAPE.getValue() + "time_out");
        int result = Integer.parseInt(timeOut);

        //求出两个时间段相差的秒数
        int minute = (int) ((currentTime.getTime() - projectModifyTime.getTime()) / 1000);
        //如果求出的秒数在正负result之外就报错  两个时间段的差大于10分钟    10分钟等于600秒
        if (minute >= result || minute <= -result) {
            project.setRunStatus(ProjectStatusEnum.OPERABLE_STATUS.getCode());
            projectService.saveAndFlushProject(project);
        }
    }

}
