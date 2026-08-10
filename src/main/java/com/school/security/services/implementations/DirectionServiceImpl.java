package com.school.security.services.implementations;

import com.school.security.dtos.requests.DirectionReqDto;
import com.school.security.dtos.responses.DirectionResDto;
import com.school.security.entities.Direction;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.DirectionMapper;
import com.school.security.repositories.DirectionRepository;
import com.school.security.services.contracts.DirectionService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class DirectionServiceImpl implements DirectionService {

    private DirectionRepository directionRepository;
    private DirectionMapper directionMapper;

    @Override
    public Direction findByName(String name) {
        return this.directionRepository
                .findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Direction not found"));
    }

    public DirectionResDto save(DirectionReqDto toSave, Long id) {
        Optional<Direction> direction = this.directionRepository.findById(id);
        if (direction.isPresent()) {
            Direction directionToUpdate = direction.get();
            directionToUpdate.setName(toSave.name());
            return this.directionMapper.toDto(this.directionRepository.save(directionToUpdate));
        } else {
            Direction directionToSave = this.directionMapper.fromDto(toSave);
            return this.directionMapper.toDto(this.directionRepository.save(directionToSave));
        }
    }

    @Override
    public DirectionResDto createOrUpdate(DirectionReqDto toSave) {
        return this.directionMapper.toDto(
                this.directionRepository.save(this.directionMapper.fromDto(toSave)));
    }

    @Override
    public List<DirectionResDto> findAll() {
        return this.directionRepository.findAll().stream()
                .map(this.directionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public DirectionResDto findById(Long aLong) {
        Optional<Direction> directionsOptional = this.directionRepository.findById(aLong);
        if (directionsOptional.isPresent()) {
            Direction direction = directionsOptional.get();
            return this.directionMapper.toDto(direction);
        } else {
            throw new EntityException("Direction not found ");
        }
    }

    @Override
    public DirectionResDto deleteById(Long aLong) {
        Optional<Direction> direction = this.directionRepository.findById(aLong);
        if (direction.isPresent()) {
            Direction directionToDelete = direction.get();
            this.directionRepository.deleteById(aLong);
            return this.directionMapper.toDto(directionToDelete);
        } else {
            throw new EntityException("Direction to delete not found");
        }
    }
}