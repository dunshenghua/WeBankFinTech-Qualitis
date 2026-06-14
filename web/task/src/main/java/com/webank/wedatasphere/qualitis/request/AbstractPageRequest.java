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

package com.webank.wedatasphere.qualitis.request;

import com.webank.wedatasphere.qualitis.exception.UnExpectedRequestException;

/**
 * Base class for paginated request DTOs.
 * Provides common page/size fields with defaults and a shared validation method.
 *
 * @author refactored
 */
public abstract class AbstractPageRequest {

    private Integer page = 0;
    private Integer size = 5;

    public AbstractPageRequest() {
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * Validate pagination parameters.
     * page must be non-null and >= 0, size must be non-null and > 0.
     *
     * @throws UnExpectedRequestException if validation fails
     */
    public void validatePagination() throws UnExpectedRequestException {
        if (getPage() == null || getPage() < 0) {
            throw new UnExpectedRequestException("page should >= 0");
        }
        if (getSize() == null || getSize() <= 0) {
            throw new UnExpectedRequestException("size should > 0");
        }
    }
}
