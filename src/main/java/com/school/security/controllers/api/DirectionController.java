package com.school.security.controllers.api;

import com.school.security.dtos.requests.DirectionReqDto;
import com.school.security.dtos.responses.DirectionResDto;
import com.school.security.services.contracts.DirectionService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/directions")
public class DirectionController {

    private final DirectionService directionService;

    public DirectionController(DirectionService directionService) {
        this.directionService = directionService;
    }

    @GetMapping
    public List<DirectionResDto> findAllDirection() {
        return this.directionService.findAll();
    }

    @GetMapping("/{id}")
    public DirectionResDto getByIdDirection(@PathVariable Long id) {
        return this.directionService.findById(id);
    }

    @PutMapping("{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_DIRECTIONS')")
    public DirectionResDto registerDirection(
            @RequestBody DirectionReqDto toSave, @PathVariable Long id) {
        return this.directionService.save(toSave, id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_DIRECTIONS')")
    public DirectionResDto deleteDirection(@PathVariable Long id) {
        return this.directionService.deleteById(id);
    }

    @PostMapping
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_DIRECTIONS')")
    public DirectionResDto save(@RequestBody DirectionReqDto directionReqDto) {
        return this.directionService.createOrUpdate(directionReqDto);
    }
}
