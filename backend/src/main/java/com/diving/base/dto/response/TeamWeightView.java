package com.diving.base.dto.response;

/**
 * 小组当前占用装备的登记重量合计视图。
 * 只统计状态为 ACTIVE 的绑定关系，已解绑的不计入；
 * 装备未填写重量时按 0 参与求和。
 */
public interface TeamWeightView {

    Long getTeamId();

    java.math.BigDecimal getTotalWeight();
}
