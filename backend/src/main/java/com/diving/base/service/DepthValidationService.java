package com.diving.base.service;

import com.diving.base.entity.Equipment;
import com.diving.base.entity.Team;
import com.diving.base.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepthValidationService {

    private final EquipmentRepository equipmentRepository;

    /**
     * 绑定前核对证深：装备额定深度超过小组持证深度属于超证绑定，直接拦截，不允许保存。
     */
    public void validateBinding(Team team, Equipment equipment) {
        if (equipment.getMaxDepth() > team.getCertifiedDepth()) {
            log.warn("超证绑定拦截: 装备[{}]额定深度{}m > 小组[{}]持证深度{}m",
                    equipment.getName(), equipment.getMaxDepth(),
                    team.getName(), team.getCertifiedDepth());
            throw new RuntimeException(String.format(
                    "装备[%s]额定深度(%dm)超过小组[%s]持证深度(%dm)，属于超证绑定，禁止保存",
                    equipment.getName(), equipment.getMaxDepth(),
                    team.getName(), team.getCertifiedDepth()));
        }

        if (equipment.getMaxDepth() > team.getMaxDepth()) {
            log.warn("深度校验失败: 装备[{}]最大深度{}m > 小组[{}]允许深度{}m",
                    equipment.getName(), equipment.getMaxDepth(),
                    team.getName(), team.getMaxDepth());
            throw new RuntimeException(String.format(
                    "装备[%s]最大耐压深度(%dm)超过小组[%s]允许的最大深度(%dm)，禁止绑定",
                    equipment.getName(), equipment.getMaxDepth(),
                    team.getName(), team.getMaxDepth()));
        }

        log.info("深度校验通过: 装备[{}]最大深度{}m <= 小组[{}]持证深度{}m",
                equipment.getName(), equipment.getMaxDepth(),
                team.getName(), team.getCertifiedDepth());
    }

    public boolean isValidBinding(Team team, Equipment equipment) {
        return equipment.getMaxDepth() <= team.getCertifiedDepth()
                && equipment.getMaxDepth() <= team.getMaxDepth();
    }
}