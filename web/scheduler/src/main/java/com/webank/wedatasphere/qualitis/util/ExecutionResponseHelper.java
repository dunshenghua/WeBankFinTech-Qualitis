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

import com.webank.wedatasphere.qualitis.exception.PermissionDeniedRequestException;
import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;
import com.webank.wedatasphere.qualitis.response.GeneralResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

/**
 * Utility for consistent exception translation in execution controllers.
 * Extracts the identical catch-block pattern from ExecutionController's projectExecution,
 * groupListExecution, and ruleListExecution methods.
 *
 * @author refactored
 */
public class ExecutionResponseHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionResponseHelper.class);

    private ExecutionResponseHelper() {
        // Utility class
    }

    /**
     * Execute a callable that returns a GeneralResponse, and translate common execution exceptions
     * into the standard UnExpectedRequestException / InterruptedException pattern.
     *
     * @param callable the execution logic to invoke
     * @return the GeneralResponse from the callable
     * @throws UnExpectedRequestException for request validation or permission errors
     * @throws InterruptedException if the execution thread was interrupted
     */
    public static GeneralResponse executeAndWrap(ExecutionCall callable)
            throws UnExpectedRequestException, InterruptedException {
        try {
            GeneralResponse response = callable.execute();
            return new GeneralResponse<>(response.getCode(), response.getMessage(), response.getData());
        } catch (UnExpectedRequestException e) {
            throw new UnExpectedRequestException(e.getMessage());
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted!", e);
            Thread.currentThread().interrupt();
            throw new InterruptedException(e.getMessage());
        } catch (ExecutionException | PermissionDeniedRequestException e) {
            throw new UnExpectedRequestException(e.getMessage());
        } catch (Exception e) {
            throw new UnExpectedRequestException(e.getMessage());
        }
    }

    /**
     * Functional interface for execution logic that may throw checked exceptions.
     */
    @FunctionalInterface
    public interface ExecutionCall {
        GeneralResponse execute() throws Exception;
    }
}
