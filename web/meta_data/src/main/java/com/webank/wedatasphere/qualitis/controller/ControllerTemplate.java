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

package com.webank.wedatasphere.qualitis.controller;

import com.webank.wedatasphere.qualitis.constants.ResponseStatusConstants;
import com.webank.wedatasphere.qualitis.exception.PermissionDeniedRequestException;
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import com.webank.wedatasphere.qualitis.metadata.exception.MetaDataAcquireFailedException;
import com.webank.wedatasphere.qualitis.response.GeneralResponse;
import org.slf4j.Logger;
import org.springframework.web.client.ResourceAccessException;

/**
 * Provides reusable exception-handling templates for controller endpoints.
 *
 * Three patterns are available:
 * - metaDataQuery: for standard metadata/cluster/department queries
 * - dataSourceOperation: for data source CRUD operations (adds ResourceAccessException handling)
 * - udfOperation: for UDF operations (re-throws all typed exceptions)
 *
 * Each template ensures backward-compatible error responses:
 * - UnExpectedRequestException is propagated to its ExceptionMapper (HTTP 400 + i18n)
 * - MetaDataAcquireFailedException is caught and wrapped as code "500" (not propagated,
 *   because the mapper would return code "400", breaking backward compatibility)
 * - PermissionDeniedRequestException is propagated to its ExceptionMapper
 * - ResourceAccessException is caught and wrapped as code "500"
 * - Generic Exception is caught and wrapped as code "500" with a configurable error message key
 */
public final class ControllerTemplate {

    private ControllerTemplate() {
    }

    /**
     * Functional interface for service calls that may throw checked exceptions.
     */
    @FunctionalInterface
    public interface ServiceCall<T> {
        GeneralResponse<T> execute() throws Exception;
    }

    /**
     * Pattern A: Standard metadata query.
     * Re-throws UnExpectedRequestException; catches MetaDataAcquireFailedException and generic Exception as 500.
     */
    public static <T> GeneralResponse<T> metaDataQuery(Logger logger, String errorMsgKey,
            ServiceCall<T> call) throws UnExpectedRequestException {
        try {
            return call.execute();
        } catch (UnExpectedRequestException e) {
            throw e;
        } catch (MetaDataAcquireFailedException e) {
            logger.error("External API error: {}", e.getMessage(), e);
            return new GeneralResponse<>(ResponseStatusConstants.SERVER_ERROR, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Metadata query failed: {}", e.getMessage(), e);
            return new GeneralResponse<>(ResponseStatusConstants.SERVER_ERROR, errorMsgKey, null);
        }
    }

    /**
     * Pattern B: Data source operations.
     * Same as metaDataQuery, plus catches ResourceAccessException and re-throws PermissionDeniedRequestException.
     */
    public static <T> GeneralResponse<T> dataSourceOperation(Logger logger, String errorMsgKey,
            ServiceCall<T> call) throws UnExpectedRequestException, PermissionDeniedRequestException {
        try {
            return call.execute();
        } catch (UnExpectedRequestException e) {
            throw e;
        } catch (PermissionDeniedRequestException e) {
            throw e;
        } catch (MetaDataAcquireFailedException e) {
            logger.error("External API error: {}", e.getMessage(), e);
            return new GeneralResponse<>(ResponseStatusConstants.SERVER_ERROR, e.getMessage(), null);
        } catch (ResourceAccessException e) {
            logger.error("Resource access failed: {}", e.getMessage(), e);
            return new GeneralResponse<>(ResponseStatusConstants.SERVER_ERROR,
                    "{&PARAMS_ERROR_FOR_THIRD_PART_SERVICE}", null);
        } catch (Exception e) {
            logger.error("Data source operation failed: {}", e.getMessage(), e);
            return new GeneralResponse<>(ResponseStatusConstants.SERVER_ERROR, errorMsgKey, null);
        }
    }

    /**
     * Pattern D: UDF operations.
     * Re-throws all typed exceptions (UnExpectedRequestException, PermissionDeniedRequestException,
     * MetaDataAcquireFailedException); catches generic Exception as 500.
     */
    public static <T> GeneralResponse<T> udfOperation(Logger logger, String errorMsgKey,
            ServiceCall<T> call) throws UnExpectedRequestException, PermissionDeniedRequestException,
            MetaDataAcquireFailedException {
        try {
            return call.execute();
        } catch (UnExpectedRequestException | PermissionDeniedRequestException
                | MetaDataAcquireFailedException e) {
            throw e;
        } catch (Exception e) {
            logger.error("UDF operation failed: {}", e.getMessage(), e);
            return new GeneralResponse<>(ResponseStatusConstants.SERVER_ERROR, errorMsgKey, null);
        }
    }
}
