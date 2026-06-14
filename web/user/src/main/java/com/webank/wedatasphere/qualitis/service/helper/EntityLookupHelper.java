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

package com.webank.wedatasphere.qualitis.service.helper;

import com.webank.wedatasphere.qualitis.dao.RoleDao;
import com.webank.wedatasphere.qualitis.dao.UserDao;
import com.webank.wedatasphere.qualitis.dao.UserRoleDao;
import com.webank.wedatasphere.qualitis.entity.Role;
import com.webank.wedatasphere.qualitis.entity.User;
import com.webank.wedatasphere.qualitis.entity.UserRole;
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Consolidates repeated find-by-ID-or-throw patterns for core governance entities.
 */
@Component
public class EntityLookupHelper {

    @Autowired
    private UserDao userDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private UserRoleDao userRoleDao;

    public User findUserByIdOrFail(Long userId) throws UnExpectedRequestException {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new UnExpectedRequestException("userId {&DOES_NOT_EXIST}");
        }
        return user;
    }

    public Role findRoleByIdOrFail(Long roleId) throws UnExpectedRequestException {
        Role role = roleDao.findById(roleId);
        if (role == null) {
            throw new UnExpectedRequestException("roleId {&DOES_NOT_EXIST}");
        }
        return role;
    }

    public UserRole findUserRoleByUuidOrFail(String uuid) throws UnExpectedRequestException {
        UserRole userRole = userRoleDao.findByUuid(uuid);
        if (userRole == null) {
            throw new UnExpectedRequestException("user role id {&DOES_NOT_EXIST}");
        }
        return userRole;
    }
}
