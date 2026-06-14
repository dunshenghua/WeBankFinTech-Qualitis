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

package com.webank.wedatasphere.qualitis.converter;

import com.webank.wedatasphere.qualitis.constants.ResponseStatusConstants;
import com.webank.wedatasphere.qualitis.dao.ApplicationCommentDao;
import com.webank.wedatasphere.qualitis.dao.TaskDao;
import com.webank.wedatasphere.qualitis.entity.Application;
import com.webank.wedatasphere.qualitis.entity.ApplicationComment;
import com.webank.wedatasphere.qualitis.entity.Task;
import com.webank.wedatasphere.qualitis.response.ApplicationResponse;
import com.webank.wedatasphere.qualitis.response.GeneralResponse;
import com.webank.wedatasphere.qualitis.response.GetAllResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Centralized assembler for converting Application entities to paginated response DTOs.
 *
 * Consolidates the previously duplicated pattern found in 5 methods of ApplicationServiceImpl:
 * 1. Batch-fetch tasks for all applications (fixes N+1 queries)
 * 2. Pre-fetch ApplicationComment for i18n messages
 * 3. Build ApplicationResponse with killOption and messages
 * 4. Wrap in GetAllResponse + GeneralResponse
 *
 * @author refactored
 */
@Component
public class ApplicationResponseAssembler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationResponseAssembler.class);

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private ApplicationCommentDao applicationCommentDao;

    /**
     * Assemble a paginated response for a list of applications.
     * Batch-fetches tasks (fixing N+1) and pre-fetches ApplicationComment.
     *
     * @param applicationList the applications to convert
     * @param total           the total count for pagination
     * @param username        the current user's username (for killOption)
     * @return the wrapped GeneralResponse with GetAllResponse
     */
    public GeneralResponse<GetAllResponse<ApplicationResponse>> assembleResponse(
            List<Application> applicationList, long total, String username) {

        List<Task> taskList = taskDao.findByApplicationList(applicationList);
        Map<String, List<Task>> taskMap = taskList.stream()
                .collect(Collectors.groupingBy(task -> task.getApplication().getId()));

        return assembleResponse(applicationList, total, username, taskMap);
    }

    /**
     * Assemble a paginated response using a pre-fetched task map.
     * Used by filterAdvanceApplication which already fetches tasks in batch.
     *
     * @param applicationList the applications to convert
     * @param total           the total count for pagination
     * @param username        the current user's username (for killOption)
     * @param preFetchedTaskMap pre-grouped tasks by application ID
     * @return the wrapped GeneralResponse with GetAllResponse
     */
    public GeneralResponse<GetAllResponse<ApplicationResponse>> assembleResponse(
            List<Application> applicationList, long total, String username,
            Map<String, List<Task>> preFetchedTaskMap) {

        Map<Integer, ApplicationComment> commentCache = fetchApplicationComments(applicationList);

        List<ApplicationResponse> applicationResponses = new ArrayList<>(applicationList.size());
        for (Application application : applicationList) {
            List<Task> tasks = preFetchedTaskMap.getOrDefault(application.getId(), Collections.emptyList());
            ApplicationResponse response = new ApplicationResponse(application, tasks);

            // Set killOption: true if current user is creator or executor
            boolean canKill = username != null
                    && (username.equals(application.getCreateUser()) || username.equals(application.getExecuteUser()));
            response.setKillOption(canKill);

            // Set i18n messages from pre-fetched comments
            if (application.getApplicationComment() != null) {
                ApplicationComment comment = commentCache.get(application.getApplicationComment());
                if (comment != null) {
                    response.setZhMessage(comment.getZhMessage());
                    response.setEnMessage(comment.getEnMessage());
                }
            }

            applicationResponses.add(response);
        }

        return buildGeneralResponse(applicationResponses, total);
    }

    /**
     * Assemble a response with per-application task pagination.
     * Used by filterApplicationId which requires taskPage/taskSize.
     *
     * @param applicationList the applications to convert
     * @param total           the total count for pagination
     * @param username        the current user's username (for killOption)
     * @param logSelect       whether to filter by non-pass status
     * @param filterStatus    the filter status code (1 = non-pass)
     * @param taskPage        the task-level page number
     * @param taskSize        the task-level page size
     * @return the wrapped GeneralResponse with GetAllResponse
     */
    public GeneralResponse<GetAllResponse<ApplicationResponse>> assembleResponseWithTaskPaging(
            List<Application> applicationList, long total, String username,
            boolean logSelect, Integer filterStatus, int taskPage, int taskSize) {

        Map<Integer, ApplicationComment> commentCache = fetchApplicationComments(applicationList);

        List<ApplicationResponse> applicationResponses = new ArrayList<>(applicationList.size());
        for (Application application : applicationList) {
            List<Task> tasks = taskDao.findByApplicationPageable(
                    application, logSelect && Objects.equals(filterStatus, 1), taskPage, taskSize);
            int taskTotal = taskDao.countByApplication(application);

            ApplicationResponse response = new ApplicationResponse(application, tasks);

            boolean canKill = username != null
                    && (username.equals(application.getCreateUser()) || username.equals(application.getExecuteUser()));
            response.setKillOption(canKill);
            response.setTaskTotal(taskTotal);

            if (application.getApplicationComment() != null) {
                ApplicationComment comment = commentCache.get(application.getApplicationComment());
                if (comment != null) {
                    response.setZhMessage(comment.getZhMessage());
                    response.setEnMessage(comment.getEnMessage());
                }
            }

            applicationResponses.add(response);
        }

        return buildGeneralResponse(applicationResponses, total);
    }

    /**
     * Pre-fetch ApplicationComment entities for the given applications.
     * Collects distinct comment codes and queries them once.
     */
    private Map<Integer, ApplicationComment> fetchApplicationComments(List<Application> applicationList) {
        Map<Integer, ApplicationComment> cache = new HashMap<>();
        applicationList.stream()
                .map(Application::getApplicationComment)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(code -> {
                    ApplicationComment comment = applicationCommentDao.getByCode(code);
                    if (comment != null) {
                        cache.put(code, comment);
                    }
                });
        return cache;
    }

    /**
     * Build the standard GetAllResponse + GeneralResponse wrapper with logging.
     */
    private GeneralResponse<GetAllResponse<ApplicationResponse>> buildGeneralResponse(
            List<ApplicationResponse> applicationResponses, long total) {

        GetAllResponse<ApplicationResponse> getAllResponse = new GetAllResponse<>();
        getAllResponse.setData(applicationResponses);
        getAllResponse.setTotal(total);

        List<String> applicationIdList = applicationResponses.stream()
                .map(ApplicationResponse::getApplicationId)
                .collect(Collectors.toList());
        LOGGER.info("Succeed to find applications. size: {}, id of applications: {}", total, applicationIdList);

        return new GeneralResponse<>(ResponseStatusConstants.OK, "{&SUCCEED_TO_GET_APPLICATIONS}", getAllResponse);
    }
}
