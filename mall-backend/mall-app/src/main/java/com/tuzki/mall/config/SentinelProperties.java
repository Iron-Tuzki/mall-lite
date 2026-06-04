package com.tuzki.mall.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.tuzki.mall.product.sentinel.ProductHotSentinelResources;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Sentinel 客户端配置属性，负责把 Spring 配置转换为 Sentinel Dashboard 识别的系统参数。
 */
@Component
@ConfigurationProperties(prefix = "mall.sentinel")
public class SentinelProperties {

    private boolean enabled = true;

    private String projectName = "mall-lite";

    private String dashboardServer = "localhost:8858";

    private int apiPort = 8719;

    private HotProductDetail hotProductDetail = new HotProductDetail();

    @PostConstruct
    public void applySystemProperties() {
        if (!enabled) {
            return;
        }
        if (StringUtils.hasText(projectName)) {
            System.setProperty("project.name", projectName);
        }
        if (StringUtils.hasText(dashboardServer)) {
            System.setProperty("csp.sentinel.dashboard.server", dashboardServer);
        }
        System.setProperty("csp.sentinel.api.port", String.valueOf(apiPort));
        // 加载热门商品的限流降级配置
        loadHotProductDetailRules();
    }

    private void loadHotProductDetailRules() {
        if (!hotProductDetail.isEnabled()) {
            return;
        }
        FlowRule flowRule = new FlowRule(ProductHotSentinelResources.HOT_PRODUCT_DETAIL);
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setCount(hotProductDetail.getFlowQps());
        FlowRuleManager.loadRules(List.of(flowRule));

        DegradeRule degradeRule = new DegradeRule(ProductHotSentinelResources.HOT_PRODUCT_DETAIL);
        degradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        degradeRule.setCount(hotProductDetail.getSlowRequestRtMs());
        degradeRule.setSlowRatioThreshold(hotProductDetail.getSlowRequestRatio());
        degradeRule.setMinRequestAmount(hotProductDetail.getMinRequestAmount());
        degradeRule.setStatIntervalMs(hotProductDetail.getStatIntervalMs());
        degradeRule.setTimeWindow(hotProductDetail.getTimeWindowSeconds());
        DegradeRuleManager.loadRules(List.of(degradeRule));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDashboardServer() {
        return dashboardServer;
    }

    public void setDashboardServer(String dashboardServer) {
        this.dashboardServer = dashboardServer;
    }

    public int getApiPort() {
        return apiPort;
    }

    public void setApiPort(int apiPort) {
        this.apiPort = apiPort;
    }

    public HotProductDetail getHotProductDetail() {
        return hotProductDetail;
    }

    public void setHotProductDetail(HotProductDetail hotProductDetail) {
        this.hotProductDetail = hotProductDetail;
    }

    /**
     * 热门商品详情 Sentinel 默认规则配置，用于启动时加载限流和慢调用熔断规则。
     */
    public static class HotProductDetail {

        private boolean enabled = true;

        private double flowQps = 50;

        private double slowRequestRtMs = 500;

        private double slowRequestRatio = 0.5;

        private int minRequestAmount = 5;

        private int statIntervalMs = 10000;

        private int timeWindowSeconds = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getFlowQps() {
            return flowQps;
        }

        public void setFlowQps(double flowQps) {
            this.flowQps = flowQps;
        }

        public double getSlowRequestRtMs() {
            return slowRequestRtMs;
        }

        public void setSlowRequestRtMs(double slowRequestRtMs) {
            this.slowRequestRtMs = slowRequestRtMs;
        }

        public double getSlowRequestRatio() {
            return slowRequestRatio;
        }

        public void setSlowRequestRatio(double slowRequestRatio) {
            this.slowRequestRatio = slowRequestRatio;
        }

        public int getMinRequestAmount() {
            return minRequestAmount;
        }

        public void setMinRequestAmount(int minRequestAmount) {
            this.minRequestAmount = minRequestAmount;
        }

        public int getStatIntervalMs() {
            return statIntervalMs;
        }

        public void setStatIntervalMs(int statIntervalMs) {
            this.statIntervalMs = statIntervalMs;
        }

        public int getTimeWindowSeconds() {
            return timeWindowSeconds;
        }

        public void setTimeWindowSeconds(int timeWindowSeconds) {
            this.timeWindowSeconds = timeWindowSeconds;
        }
    }
}
