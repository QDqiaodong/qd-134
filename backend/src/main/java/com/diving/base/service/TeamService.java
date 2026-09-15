package com.diving.base.service;

import com.diving.base.dto.request.TeamCreateRequest;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.dto.response.TeamResponse;
import com.diving.base.dto.response.TeamWeightView;
import com.diving.base.entity.Team;
import com.diving.base.repository.BindingRepository;
import com.diving.base.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final BindingRepository bindingRepository;

    private BindingService bindingService;

    @org.springframework.beans.factory.annotation.Autowired
    public void setBindingService(@Lazy BindingService bindingService) {
        this.bindingService = bindingService;
    }

    @Cacheable(value = "team", key = "#id")
    public Team findById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("小组不存在: " + id));
    }

    @Cacheable(value = "teamList", key = "#page + '_' + #size + '_' + #keyword")
    public PageResponse<TeamResponse> findAll(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Team> teamPage;

        if (keyword != null && !keyword.isEmpty()) {
            teamPage = teamRepository.findByNameContaining(keyword, pageable);
        } else {
            teamPage = teamRepository.findAll(pageable);
        }

        List<Long> teamIds = teamPage.getContent().stream()
                .map(Team::getId)
                .collect(Collectors.toList());
        Map<Long, BigDecimal> weightByTeam = sumActiveWeight(teamIds);

        List<TeamResponse> content = teamPage.getContent().stream()
                .map(team -> TeamResponse.of(team, weightByTeam.get(team.getId())))
                .collect(Collectors.toList());

        Page<TeamResponse> responsePage = new PageImpl<>(content, pageable, teamPage.getTotalElements());
        return PageResponse.from(responsePage);
    }

    /**
     * 汇总各小组当前占用（ACTIVE 绑定）装备的登记重量；
     * 已解绑装备不计入，未填写重量按 0 处理。无占用装备的小组合计为 0。
     */
    private Map<Long, BigDecimal> sumActiveWeight(List<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return bindingRepository.sumActiveWeightByTeamIds(teamIds).stream()
                .collect(Collectors.toMap(
                        TeamWeightView::getTeamId,
                        view -> view.getTotalWeight() != null ? view.getTotalWeight() : BigDecimal.ZERO,
                        (a, b) -> a));
    }

    @Transactional
    @CacheEvict(value = "teamList", allEntries = true)
    public Team create(TeamCreateRequest request) {
        if (teamRepository.existsByName(request.getName())) {
            throw new RuntimeException("小组名称已存在: " + request.getName());
        }

        if (request.getMinDepth() != null && request.getMinDepth() >= request.getMaxDepth()) {
            throw new RuntimeException("最小深度必须小于最大深度");
        }

        if (request.getMaxDepth() > request.getCertifiedDepth()) {
            throw new RuntimeException("最大允许深度不能超过持证深度，证深不符禁止保存");
        }

        Team team = Team.builder()
                .name(request.getName())
                .memberCount(request.getMemberCount() != null ? request.getMemberCount() : 0)
                .minDepth(request.getMinDepth() != null ? request.getMinDepth() : 0)
                .maxDepth(request.getMaxDepth())
                .certifiedDepth(request.getCertifiedDepth())
                .depthChangeNotify(request.getDepthChangeNotify() != null ? request.getDepthChangeNotify() : true)
                .status(request.getStatus() != null ? request.getStatus() : Team.STATUS_FILMING)
                .description(request.getDescription())
                .build();

        return teamRepository.save(team);
    }

    @Transactional
    @CacheEvict(value = {"team", "teamList"}, allEntries = true)
    public Team update(Long id, TeamCreateRequest request) {
        Team team = findById(id);
        Integer oldMaxDepth = team.getMaxDepth();

        if (!team.getName().equals(request.getName()) && teamRepository.existsByName(request.getName())) {
            throw new RuntimeException("小组名称已存在: " + request.getName());
        }

        if (request.getMinDepth() != null && request.getMinDepth() >= request.getMaxDepth()) {
            throw new RuntimeException("最小深度必须小于最大深度");
        }

        if (request.getMaxDepth() > request.getCertifiedDepth()) {
            throw new RuntimeException("最大允许深度不能超过持证深度，证深不符禁止保存");
        }

        team.setName(request.getName());
        team.setMemberCount(request.getMemberCount() != null ? request.getMemberCount() : 0);
        team.setMinDepth(request.getMinDepth() != null ? request.getMinDepth() : 0);
        team.setMaxDepth(request.getMaxDepth());
        team.setCertifiedDepth(request.getCertifiedDepth());
        team.setDepthChangeNotify(request.getDepthChangeNotify() != null
                ? request.getDepthChangeNotify() : team.getDepthChangeNotify());
        // 管理员可在此收队（WRAPPED）或改回拍摄中（FILMING），失效剧组改回后才能继续挂占用
        if (request.getStatus() != null) {
            team.setStatus(request.getStatus());
        }
        team.setDescription(request.getDescription());

        Team savedTeam = teamRepository.save(team);

        if (request.getMaxDepth() < oldMaxDepth && bindingService != null) {
            log.info("小组深度从{}调整为{}，触发深度同步", oldMaxDepth, request.getMaxDepth());
            try {
                com.diving.base.dto.request.BindingSyncRequest syncRequest = 
                    new com.diving.base.dto.request.BindingSyncRequest();
                syncRequest.setTeamId(id);
                syncRequest.setNewMaxDepth(request.getMaxDepth());
                syncRequest.setOperator("system");
                bindingService.syncDepth(syncRequest);
            } catch (Exception e) {
                log.error("深度同步失败", e);
            }
        }

        return savedTeam;
    }

    @Transactional
    @CacheEvict(value = {"team", "teamList"}, allEntries = true)
    public void delete(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new RuntimeException("小组不存在: " + id);
        }
        teamRepository.deleteById(id);
    }
}