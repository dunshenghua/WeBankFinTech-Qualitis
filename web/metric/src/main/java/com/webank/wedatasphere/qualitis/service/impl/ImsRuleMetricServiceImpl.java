package com.webank.wedatasphere.qualitis.service.impl;

import com.webank.wedatasphere.qualitis.service.ImsRuleMetricService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * IMS rule metric service — currently inactive.
 *
 * <p>All IMS-specific operations (metric data query, alarm data, metric
 * collect list, metric template CRUD, data-source conditions) have been
 * disabled. The class shell is retained to satisfy Spring component scanning
 * and to serve as a placeholder if the IMS integration is re-enabled.</p>
 *
 * @author v_wenxuanzhang
 */
@Service
public class ImsRuleMetricServiceImpl implements ImsRuleMetricService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImsRuleMetricServiceImpl.class);
}
