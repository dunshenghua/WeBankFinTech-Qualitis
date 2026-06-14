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

package com.webank.wedatasphere.qualitis.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized status mapping utility for the task/application module.
 *
 * Maintains two business-critical mappings that were previously scattered
 * as magic-number maps and inline if/else chains in service methods:
 *
 * 1. commentType → ApplicationStatus (used by filter/status endpoint)
 * 2. ApplicationStatus → TaskStatus (used by filter/advance endpoint)
 *
 * @author refactored
 */
public final class StatusMapping {

    private StatusMapping() {
    }

    /**
     * Maps commentType codes to ApplicationStatusEnum codes.
     *
     * commentType semantics (from ApplicationComment domain):
     *  1  → submit failed       → TASK_SUBMIT_FAILED(9)
     *  2  → check failed        → FAILED(7)
     *  3  → verify failed       → FAILED(7)
     *  4  → multi-table failed  → FAILED(7)
     *  5  → custom failed       → FAILED(7)
     *  6  → file failed         → FAILED(7)
     *  7  → single not pass     → NOT_PASS(8)
     *  8  → multi not pass      → NOT_PASS(8)
     *  9  → single passed       → FINISHED(4)
     *  10 → multi passed        → FINISHED(4)
     *  11 → custom not pass     → TASK_SUBMIT_FAILED(9)
     *  12 → file check failed   → FAILED(7)
     *  13 → custom not pass     → NOT_PASS(8)
     *  14 → file not pass       → TASK_SUBMIT_FAILED(9)
     */
    private static final Map<Integer, Integer> COMMENT_TO_APPLICATION_STATUS;

    static {
        Map<Integer, Integer> map = new HashMap<>(16);
        map.put(1, ApplicationStatusEnum.TASK_SUBMIT_FAILED.getCode());
        map.put(2, ApplicationStatusEnum.FAILED.getCode());
        map.put(3, ApplicationStatusEnum.FAILED.getCode());
        map.put(4, ApplicationStatusEnum.FAILED.getCode());
        map.put(5, ApplicationStatusEnum.FAILED.getCode());
        map.put(6, ApplicationStatusEnum.FAILED.getCode());
        map.put(7, ApplicationStatusEnum.NOT_PASS.getCode());
        map.put(8, ApplicationStatusEnum.NOT_PASS.getCode());
        map.put(9, ApplicationStatusEnum.FINISHED.getCode());
        map.put(10, ApplicationStatusEnum.FINISHED.getCode());
        map.put(11, ApplicationStatusEnum.TASK_SUBMIT_FAILED.getCode());
        map.put(12, ApplicationStatusEnum.FAILED.getCode());
        map.put(13, ApplicationStatusEnum.NOT_PASS.getCode());
        map.put(14, ApplicationStatusEnum.TASK_SUBMIT_FAILED.getCode());
        COMMENT_TO_APPLICATION_STATUS = Collections.unmodifiableMap(map);
    }

    /**
     * Maps ApplicationStatus codes to TaskStatus codes.
     * Used when querying by datasource conditions (filter/advance with cluster).
     */
    private static final Map<Integer, Integer> APP_TO_TASK_STATUS;

    static {
        Map<Integer, Integer> map = new HashMap<>(4);
        map.put(ApplicationStatusEnum.FINISHED.getCode(), TaskStatusEnum.PASS_CHECKOUT.getCode());
        map.put(ApplicationStatusEnum.NOT_PASS.getCode(), TaskStatusEnum.FAIL_CHECKOUT.getCode());
        map.put(ApplicationStatusEnum.SUCCESSFUL_CREATE_APPLICATION.getCode(), TaskStatusEnum.INITED.getCode());
        map.put(ApplicationStatusEnum.RUNNING.getCode(), TaskStatusEnum.RUNNING.getCode());
        APP_TO_TASK_STATUS = Collections.unmodifiableMap(map);
    }

    /**
     * Convert a commentType code to the corresponding application status code.
     *
     * @param commentType the comment type code (1-14)
     * @return the application status code, or null if commentType is null or unmapped
     */
    public static Integer commentTypeToApplicationStatus(Integer commentType) {
        if (commentType == null) {
            return null;
        }
        return COMMENT_TO_APPLICATION_STATUS.get(commentType);
    }

    /**
     * Convert an application status code to the corresponding task status code.
     * Used in the datasource branch of advance filtering.
     *
     * @param applicationStatus the application status code
     * @return the task status code, or null if unmapped or input is null/0
     */
    public static Integer applicationStatusToTaskStatus(Integer applicationStatus) {
        if (applicationStatus == null || applicationStatus == 0) {
            return null;
        }
        return APP_TO_TASK_STATUS.get(applicationStatus);
    }
}
