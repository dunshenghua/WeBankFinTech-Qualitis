package com.webank.wedatasphere.qualitis.service.impl;

import com.webank.wedatasphere.qualitis.dao.UserDao;
import com.webank.wedatasphere.qualitis.dao.UserRoleDao;
import com.webank.wedatasphere.qualitis.entity.Department;
import com.webank.wedatasphere.qualitis.entity.Role;
import com.webank.wedatasphere.qualitis.entity.User;
import com.webank.wedatasphere.qualitis.entity.UserRole;
import com.webank.wedatasphere.qualitis.service.RoleService;
import com.webank.wedatasphere.qualitis.service.SubDepartmentPermissionService;
import com.webank.wedatasphere.qualitis.util.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Lightweight helper that consolidates user-resolution and department-collection
 * boilerplate shared across rule-metric services.
 */
@Component
public class RuleMetricQueryHelper {

    @Autowired
    private UserDao userDao;
    @Autowired
    private UserRoleDao userRoleDao;
    @Autowired
    private RoleService roleService;
    @Autowired
    private SubDepartmentPermissionService subDepartmentPermissionService;

    /**
     * Immutable snapshot of the current request's user, roles, and computed role-type.
     */
    public static class UserContext {
        private final User user;
        private final List<UserRole> userRoles;
        private final Integer roleType;

        public UserContext(User user, List<UserRole> userRoles, Integer roleType) {
            this.user = user;
            this.userRoles = userRoles;
            this.roleType = roleType;
        }

        public User getUser() {
            return user;
        }

        public String getUsername() {
            return user.getUsername();
        }

        public List<UserRole> getUserRoles() {
            return userRoles;
        }

        public Integer getRoleType() {
            return roleType;
        }
    }

    /**
     * Resolves the logged-in user, their roles, and the computed role-type
     * from the current HTTP request.
     */
    public UserContext resolveCurrentUser(HttpServletRequest httpServletRequest) {
        return resolveUser(HttpUtils.getUserName(httpServletRequest));
    }

    /**
     * Resolves a user by username, returning their roles and computed role-type.
     */
    public UserContext resolveUser(String userName) {
        User loginUser = userDao.findByUsername(userName);
        List<UserRole> userRoles = userRoleDao.findByUser(loginUser);
        Integer roleType = roleService.getRoleType(userRoles);
        return new UserContext(loginUser, userRoles, roleType);
    }

    /**
     * Collects all department IDs visible to a DEPARTMENT_ADMIN user:
     * department IDs from each role plus the user's own department (if non-null).
     */
    public List<Long> collectDepartmentIds(List<UserRole> userRoles, User loginUser) {
        List<Long> ids = userRoles.stream()
                .map(UserRole::getRole).filter(Objects::nonNull)
                .map(Role::getDepartment).filter(Objects::nonNull)
                .map(Department::getId)
                .collect(Collectors.toList());
        if (loginUser.getDepartment() != null) {
            ids.add(loginUser.getDepartment().getId());
        }
        return ids;
    }

    /**
     * Collects department IDs and expands them to include sub-department IDs
     * via {@link SubDepartmentPermissionService}.
     */
    public List<Long> getDevAndOpsInfoList(List<UserRole> userRoles, User loginUser) {
        List<Long> departmentIds = collectDepartmentIds(userRoles, loginUser);
        return subDepartmentPermissionService.getSubDepartmentIdList(departmentIds);
    }
}
