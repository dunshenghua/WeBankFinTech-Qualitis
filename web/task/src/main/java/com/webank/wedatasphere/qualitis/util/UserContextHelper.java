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

package com.webank.wedatasphere.qualitis.util;

import com.webank.wedatasphere.qualitis.dao.UserDao;
import com.webank.wedatasphere.qualitis.entity.User;
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * Centralized helper for retrieving the current authenticated user from the HTTP context.
 * Replaces the duplicated pattern of HttpUtils.getUserId() + userDao.findById() that
 * appeared in 6+ locations across ApplicationServiceImpl.
 *
 * @author refactored
 */
@Component
public class UserContextHelper {

    @Autowired
    private UserDao userDao;

    /**
     * Retrieve the current user entity from the HTTP request context.
     *
     * @param request the current HTTP servlet request (injected via @Context)
     * @return the authenticated User entity
     * @throws UnExpectedRequestException if user ID cannot be resolved or user does not exist
     */
    public User getCurrentUser(HttpServletRequest request) throws UnExpectedRequestException {
        Long userId = HttpUtils.getUserId(request);
        User user = userDao.findById(userId);
        if (user == null) {
            throw new UnExpectedRequestException("User {&DOES_NOT_EXIST}");
        }
        return user;
    }

    /**
     * Retrieve the current username string from the HTTP request context.
     *
     * @param request the current HTTP servlet request (injected via @Context)
     * @return the username of the authenticated user
     */
    public String getCurrentUsername(HttpServletRequest request) {
        return HttpUtils.getUserName(request);
    }
}
