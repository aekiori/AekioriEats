package com.delivery.store.service.option;

import com.delivery.store.domain.menu.Menu;
import com.delivery.store.domain.option.MenuOption;
import com.delivery.store.domain.option.MenuOptionGroup;
import com.delivery.store.dto.request.owner.ReplaceMenuOptionGroupsRequestDto;
import com.delivery.store.dto.response.ReplaceMenuOptionGroupsResponseDto;
import com.delivery.store.repository.option.MenuOptionGroupRepository;
import com.delivery.store.repository.option.MenuOptionRepository;
import com.delivery.store.service.store.StoreDomainSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuOptionService {
    private final StoreDomainSupport storeDomainSupport;
    private final MenuOptionGroupRepository menuOptionGroupRepository;
    private final MenuOptionRepository menuOptionRepository;

    @Transactional
    public ReplaceMenuOptionGroupsResponseDto replaceMenuOptionGroups(
        Long storeId,
        Long menuId,
        ReplaceMenuOptionGroupsRequestDto request,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId, authenticatedUserRole);
        Menu menu = storeDomainSupport.findMenu(menuId, storeId);

        List<MenuOptionGroup> existingGroups = menuOptionGroupRepository.findByMenuIdOrderByDisplayOrderAscIdAsc(menuId);
        if (!existingGroups.isEmpty()) {
            List<Long> groupIds = existingGroups.stream().map(MenuOptionGroup::getId).toList();
            menuOptionRepository.deleteByOptionGroupIdIn(groupIds);
        }
        menuOptionGroupRepository.deleteByMenuId(menuId);

        List<ReplaceMenuOptionGroupsResponseDto.OptionGroupResponseDto> groupResults = new ArrayList<>();
        for (int i = 0; i < request.optionGroups().size(); i++) {
            ReplaceMenuOptionGroupsRequestDto.OptionGroupRequest optionGroupRequest = request.optionGroups().get(i);
            MenuOptionGroup savedGroup = menuOptionGroupRepository.save(MenuOptionGroup.create(
                menu,
                optionGroupRequest.name().trim(),
                optionGroupRequest.isRequired(),
                optionGroupRequest.isMultiple(),
                optionGroupRequest.minSelectCount(),
                optionGroupRequest.maxSelectCount(),
                i
            ));

            List<MenuOption> options = new ArrayList<>();
            for (int j = 0; j < optionGroupRequest.options().size(); j++) {
                ReplaceMenuOptionGroupsRequestDto.OptionRequest optionRequest = optionGroupRequest.options().get(j);
                options.add(MenuOption.create(
                    savedGroup,
                    optionRequest.name().trim(),
                    optionRequest.extraPrice(),
                    optionRequest.isAvailable(),
                    j
                ));
            }
            List<MenuOption> savedOptions = menuOptionRepository.saveAll(options);

            List<ReplaceMenuOptionGroupsResponseDto.OptionResponseDto> optionResults = savedOptions.stream()
                .map(option -> new ReplaceMenuOptionGroupsResponseDto.OptionResponseDto(
                    option.getName(),
                    option.getExtraPrice(),
                    option.isAvailable()
                ))
                .toList();

            groupResults.add(new ReplaceMenuOptionGroupsResponseDto.OptionGroupResponseDto(
                savedGroup.getName(),
                savedGroup.isRequired(),
                savedGroup.isMultiple(),
                savedGroup.getMinSelectCount(),
                savedGroup.getMaxSelectCount(),
                optionResults
            ));
        }

        return new ReplaceMenuOptionGroupsResponseDto(menuId, groupResults);
    }
}

