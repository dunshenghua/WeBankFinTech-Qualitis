package com.webank.wedatasphere.qualitis.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;

/**
 * IMS rule metric controller — currently inactive.
 *
 * <p>All IMS-specific endpoints (metric data, alarm data, metric collect,
 * metric templates) have been disabled. The class shell is retained so that
 * component scanning and JAX-RS registration do not break, and to serve as a
 * placeholder if the IMS integration is re-enabled in the future.</p>
 *
 * @author v_wenxuanzhang
 */
@Path("api/v1/projector/imsmetric/")
public class ImsRuleMetricController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImsRuleMetricController.class);
}
