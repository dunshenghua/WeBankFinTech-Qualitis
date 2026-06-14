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

import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import org.apache.commons.lang.StringUtils;

/**
 * Shared parameter validation utilities for the governance module.
 * Consolidates duplicate checkId / checkString / checkUuid patterns
 * from UserServiceImpl, UserRoleServiceImpl, PermissionServiceImpl and LoginServiceImpl.
 */
public final class ParamChecker {

    private ParamChecker() {
        // Utility class - no instantiation
    }

    /**
     * Validates that a Long id is not null.
     *
     * @param id   the value to check
     * @param name display name used in the error message (e.g. "userId", "roleId")
     * @throws UnExpectedRequestException if id is null
     */
    public static void checkId(Long id, String name) throws UnExpectedRequestException {
        if (id == null) {
            throw new UnExpectedRequestException(name + " {&CAN_NOT_BE_NULL_OR_EMPTY}");
        }
    }

    /**
     * Validates that a String value is not blank (null, empty, or whitespace-only).
     *
     * @param value the value to check
     * @param name  display name used in the error message (e.g. "username", "uuid")
     * @throws UnExpectedRequestException if value is blank
     */
    public static void checkNotBlank(String value, String name) throws UnExpectedRequestException {
        if (StringUtils.isBlank(value)) {
            throw new UnExpectedRequestException(name + " {&CAN_NOT_BE_NULL_OR_EMPTY}");
        }
    }
}
