package com.webank.wedatasphere.qualitis.service.impl;

import com.webank.wedatasphere.qualitis.dao.RuleMetricDao;
import com.webank.wedatasphere.qualitis.dao.UserDao;
import com.webank.wedatasphere.qualitis.dao.UserRoleDao;
import com.webank.wedatasphere.qualitis.entity.Department;
import com.webank.wedatasphere.qualitis.entity.Role;
import com.webank.wedatasphere.qualitis.entity.RuleMetric;
import com.webank.wedatasphere.qualitis.entity.User;
import com.webank.wedatasphere.qualitis.entity.UserRole;
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import com.webank.wedatasphere.qualitis.project.response.HiveRuleDetail;
import com.webank.wedatasphere.qualitis.request.RuleMetricQueryRequest;
import com.webank.wedatasphere.qualitis.response.DepartmentSubInfoResponse;
import com.webank.wedatasphere.qualitis.response.RuleMetricResponse;
import com.webank.wedatasphere.qualitis.response.RuleMetricValueResponse;
import com.webank.wedatasphere.qualitis.rule.constant.TableDataTypeEnum;
import com.webank.wedatasphere.qualitis.rule.dao.RuleDao;
import com.webank.wedatasphere.qualitis.rule.entity.DataVisibility;
import com.webank.wedatasphere.qualitis.rule.entity.Rule;
import com.webank.wedatasphere.qualitis.rule.entity.RuleDataSource;
import com.webank.wedatasphere.qualitis.service.DataVisibilityService;
import com.webank.wedatasphere.qualitis.service.RoleService;
import com.webank.wedatasphere.qualitis.service.SubDepartmentPermissionService;
import com.webank.wedatasphere.qualitis.util.HttpUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared query and assembly helpers for rule-metric services.
 * Extracted to reduce duplication across list / detail / value queries.
 *
 * @author refactored by code review
 */
@Component
public class RuleMetricHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleMetricHelper.class);

    @Autowired
    private UserDao userDao;
    @Autowired
    private UserRoleDao userRoleDao;
    @Autowired
    private RoleService roleService;
    @Autowired
    private RuleMetricDao ruleMetricDao;
    @Autowired
    private RuleDao ruleDao;
    @Autowired
    private DataVisibilityService dataVisibilityService;
    @Autowired
    private SubDepartmentPermissionService subDepartmentPermissionService;

    private HttpServletRequest httpServletRequest;

    public RuleMetricHelper(@Context HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    // -------------------------------------------------------------------------
    // Login context
    // -------------------------------------------------------------------------

    /**
     * Snapshot of the current login user, their roles and resolved role type.
     */
    public static class LoginContext {
        private final User loginUser;
        private final List<UserRole> userRoles;
        private final Integer roleType;

        public LoginContext(User loginUser, List<UserRole> userRoles, Integer roleType) {
            this.loginUser = loginUser;
            this.userRoles = userRoles;
            this.roleType = roleType;
        }

        public User getLoginUser() {
            return loginUser;
        }

        public List<UserRole> getUserRoles() {
            return userRoles;
        }

        public Integer getRoleType() {
            return roleType;
        }

        public String getUserName() {
            return loginUser.getUsername();
        }
    }

    /**
     * Build a {@link LoginContext} for the current HTTP request.
     *
     * @throws UnExpectedRequestException if the user cannot be found
     */
    public LoginContext getLoginContext() throws UnExpectedRequestException {
        String userName = HttpUtils.getUserName(httpServletRequest);
        return getLoginContext(userName);
    }

    /**
     * Build a {@link LoginContext} for the given user name (used by outer / batch
     * callers that already know the user).
     *
     * @throws UnExpectedRequestException if the user cannot be found
     */
    public LoginContext getLoginContext(String userName) throws UnExpectedRequestException {
        User loginUser = userDao.findByUsername(userName);
        if (loginUser == null) {
            throw new UnExpectedRequestException("username is not exists.");
        }
        List<UserRole> userRoles = userRoleDao.findByUser(loginUser);
        Integer roleType = roleService.getRoleType(userRoles);
        return new LoginContext(loginUser, userRoles, roleType);
    }

    // -------------------------------------------------------------------------
    // Query parameter normalization
    // -------------------------------------------------------------------------

    /**
     * Normalize blank query fields to their DAO-expected defaults and return the
     * resolved {@code actionRangeSet}.
     */
    public Set<String> normalizeQueryParams(RuleMetricQueryRequest request) {
        if (StringUtils.isBlank(request.getSubSystemName())) {
            request.setSubSystemName("");
        }
        if (StringUtils.isBlank(request.getRuleMetricName())) {
            request.setRuleMetricName("%");
        } else {
            request.setRuleMetricName("%" + request.getRuleMetricName() + "%");
        }
        if (StringUtils.isBlank(request.getDevDepartmentId())) {
            request.setDevDepartmentId("");
        }
        if (StringUtils.isBlank(request.getOpsDepartmentId())) {
            request.setOpsDepartmentId("");
        }
        if (StringUtils.isBlank(request.getCreateUser())) {
            request.setCreateUser("");
        }
        if (StringUtils.isBlank(request.getModifyUser())) {
            request.setModifyUser("");
        }
        return CollectionUtils.isEmpty(request.getActionRange()) ? null : request.getActionRange();
    }

    // -------------------------------------------------------------------------
    // Existence check
    // -------------------------------------------------------------------------

    /**
     * Find a {@link RuleMetric} by ID or throw {@link UnExpectedRequestException}.
     */
    public RuleMetric ensureRuleMetricExists(long id) throws UnExpectedRequestException {
        if (id <= 0) {
            throw new UnExpectedRequestException("{&REQUEST_CAN_NOT_BE_NULL}");
        }
        RuleMetric ruleMetric = ruleMetricDao.findById(id);
        if (ruleMetric == null) {
            throw new UnExpectedRequestException("Rule Metric ID [" + id + "] {&DOES_NOT_EXIST}");
        }
        return ruleMetric;
    }

    // -------------------------------------------------------------------------
    // Visibility department population
    // -------------------------------------------------------------------------

    /**
     * Populate the visibility department list on the given response from persisted
     * {@link DataVisibility} records.
     */
    public void populateVisibility(RuleMetricResponse response, RuleMetric ruleMetric) {
        List<DataVisibility> dataVisibilityList = dataVisibilityService.filter(ruleMetric.getId(), TableDataTypeEnum.RULE_METRIC);
        if (CollectionUtils.isNotEmpty(dataVisibilityList)) {
            List<DepartmentSubInfoResponse> departmentInfoResponses =
                    dataVisibilityList.stream().map(DepartmentSubInfoResponse::new).collect(Collectors.toList());
            response.setVisibilityDepartmentList(departmentInfoResponses);
        }
    }

    // -------------------------------------------------------------------------
    // TaskResult conversion
    // -------------------------------------------------------------------------

    /**
     * Convert a single {@link com.webank.wedatasphere.qualitis.entity.TaskResult}
     * into a {@link RuleMetricValueResponse}.
     *
     * @param taskResult the source entity (must not be null)
     * @param includeDatasourceNames whether to populate datasource names from rule data sources
     * @return a fully populated response DTO
     */
    public RuleMetricValueResponse toRuleMetricValueResponse(
            com.webank.wedatasphere.qualitis.entity.TaskResult taskResult,
            boolean includeDatasourceNames) {
        RuleMetricValueResponse resp = new RuleMetricValueResponse();
        resp.setGenerateTime(taskResult.getCreateTime());

        Rule currentRule = ruleDao.findById(taskResult.getRuleId());
        if (currentRule != null) {
            HiveRuleDetail hiveRuleDetail = new HiveRuleDetail(currentRule);
            resp.setHiveRuleDetail(hiveRuleDetail);
            if (includeDatasourceNames) {
                Set<RuleDataSource> ruleDataSources = currentRule.getRuleDataSources();
                if (CollectionUtils.isNotEmpty(ruleDataSources)) {
                    List<String> datasourceNameList = ruleDataSources.stream()
                            .map(RuleDataSource::getLinkisDataSourceName)
                            .filter(StringUtils::isNotEmpty)
                            .collect(Collectors.toList());
                    resp.setDatasourceNames(datasourceNameList);
                }
            }
        }
        resp.setEnvName(taskResult.getEnvName());
        resp.setRelatedRuleName(currentRule == null ? "Deleted" : currentRule.getName());
        resp.setRuleMetricValue(StringUtils.isBlank(taskResult.getValue()) ? "0" : taskResult.getValue());
        return resp;
    }

    // -------------------------------------------------------------------------
    // Role-based department ID resolution (used by DEPARTMENT_ADMIN branch)
    // -------------------------------------------------------------------------

    /**
     * Collect department IDs visible to a DEPARTMENT_ADMIN, including the user's
     * own department and all departments from their admin roles.
     */
    public List<Long> resolveDepartmentAdminDeptIds(LoginContext ctx) {
        List<Long> departmentIds = ctx.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getDepartment)
                .filter(Objects::nonNull)
                .map(Department::getId)
                .collect(Collectors.toList());
        if (Objects.nonNull(ctx.getLoginUser().getDepartment())) {
            departmentIds.add(ctx.getLoginUser().getDepartment().getId());
        }
        return departmentIds;
    }

    /**
     * Build the sub-department ID list used for visibility filtering.
     * Returns null for empty lists (DAO convention).
     */
    public List<Long> resolveSubDepartmentIds(List<Long> departmentIds) {
        List<Long> subDeptIds = subDepartmentPermissionService.getSubDepartmentIdList(departmentIds);
        return subDeptIds.isEmpty() ? null : subDeptIds;
    }
}
