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
 * Shared validation primitives for request parameter checking.
 */
public final class RequestPreconditions {

    private RequestPreconditions() {
    }

    public static void checkNotNull(Object request) throws UnExpectedRequestException {
        if (request == null) {
            throw new UnExpectedRequestException("{&REQUEST_CAN_NOT_BE_NULL}");
        }
    }

    public static void checkId(Long id, String fieldName) throws UnExpectedRequestException {
        if (id == null) {
            throw new UnExpectedRequestException(fieldName + " {&CAN_NOT_BE_NULL_OR_EMPTY}");
        }
    }

    public static void checkString(String value, String fieldName) throws UnExpectedRequestException {
        if (StringUtils.isBlank(value)) {
            throw new UnExpectedRequestException(fieldName + " {&CAN_NOT_BE_NULL_OR_EMPTY}");
        }
    }

    public static void checkUuid(String uuid) throws UnExpectedRequestException {
        if (StringUtils.isBlank(uuid)) {
            throw new UnExpectedRequestException("uuid {&CAN_NOT_BE_NULL_OR_EMPTY}");
        }
    }
}
