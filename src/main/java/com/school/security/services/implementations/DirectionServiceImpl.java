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

/**
 * Service de gestion des directions (catégories de classification).
 *
 * <p>Règles d'autorisation constatées (documentées, non modifiées) :
 * <ul>
 *   <li>aucune restriction {@code @PreAuthorize} n'est appliquée dans ce service ;
 *       l'ensemble des opérations ({@code findByName}, {@code save},
 *       {@code createOrUpdate}, {@code findAll}, {@code findById},
 *       {@code deleteById}) est accessible de manière publique via la
 *       filter-chain {@code SecurityConfig} (chemins {@code /directions} en
 *       {@code permitAll}).</li>
 *   <li>les contrôles d'écriture (création, modification, suppression) s'appliquent
 *       au niveau des contrôleurs ou via les annotations {@code @PreAuthorize}
 *       {@code MANAGE_DIRECTIONS} si configuré.</li>
 * </ul>
 */
@Service
@Transactional
@AllArgsConstructor
public class DirectionServiceImpl implements DirectionService {

    private DirectionRepository directionRepository;
    private DirectionMapper directionMapper;

    /**
     * Retourne la direction par son nom.
     *
     * <p>Lève une {@code IllegalArgumentException} si la direction n'existe pas.
     */
    @Override
    public Direction findByName(String name) {
        return this.directionRepository
                .findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Direction not found"));
    }

    /**
     * Crée ou met à jour une direction.
     *
     * <p>Si l'identifiant {@code id} est présent, la direction existante est
     * mise à jour ; sinon, une nouvelle direction est créée.</p>
     */
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
    /**
     * Crée ou met à jour une direction.
     *
     * <p>Identique à {@link #save(DirectionReqDto, Long)} mais sans identifiant :
     * une nouvelle direction est toujours créée à partir du DTO fourni.</p>
     */
    @Override
    public DirectionResDto createOrUpdate(DirectionReqDto toSave) {
        return this.directionMapper.toDto(
                this.directionRepository.save(this.directionMapper.fromDto(toSave)));
    }

    /** Retourne l'ensemble des directions. */
    @Override
    public List<DirectionResDto> findAll() {
        return this.directionRepository.findAll().stream()
                .map(this.directionMapper::toDto)
                .collect(Collectors.toList());
    }

    /** Retourne la direction par son identifiant.
     *
     * <p>Lève une {@code EntityException} si la direction n'existe pas.</p>
     */
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

    /** Supprime la direction par son identifiant.
     *
     * <p>Lève une {@code EntityException} si la direction à supprimer est introuvable.</p>
     */
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