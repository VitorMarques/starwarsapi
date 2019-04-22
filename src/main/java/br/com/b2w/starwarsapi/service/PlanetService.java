package br.com.b2w.starwarsapi.service;

import br.com.b2w.starwarsapi.exception.NotFoundException;
import br.com.b2w.starwarsapi.exception.PlanetAlreadyInsertedException;
import br.com.b2w.starwarsapi.model.Planet;
import br.com.b2w.starwarsapi.model.SwapiPlanet;
import br.com.b2w.starwarsapi.repository.PlanetRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PlanetService {

    private final PlanetRepository repository;

    private final SwapiService swapiService;

    public Planet save(Planet planet) throws RestClientException {

        Optional<Planet> optionalPlanet = repository.findByNameIgnoreCase(planet.getName());

        if(optionalPlanet.isPresent())
            throw new PlanetAlreadyInsertedException("Já existe um planeta cadastrado com o nome " + planet.getName());

        SwapiPlanet swapiPlanet = swapiService.getSwapiPlanet(planet.getName());
        planet.setUri(swapiPlanet.getUrl());

        Planet savedPlanet = repository.save(planet);

        savedPlanet.setAmountMoviesApeared(swapiPlanet.getAmountMoviesApeared());

        return savedPlanet;
    }

    public void delete(Planet planet) {
        repository.delete(planet);
    }

    public Page<Planet> findAll(Pageable pageable) {
        Page<Planet> allPlanets = repository.findAll(pageable);

        allPlanets.stream().forEach(planet -> getUpdatedPlanet(Optional.of(planet)));

        return allPlanets;
    }

    @Cacheable("findByName")
    public Optional<Planet> findByName(String name) {
        Optional<Planet> optionalPlanet = repository.findByNameIgnoreCase(name);

        if(!optionalPlanet.isPresent())
            throw new NotFoundException("Planeta " + name + " não cadastrado.");

        return Optional.of(getUpdatedPlanet(optionalPlanet));
    }

    @Cacheable("findByUuid")
    public Optional<Planet> findByUuid(UUID uuid) {
        Optional<Planet> optionalPlanet = repository.findByUuid(uuid);

        if(!optionalPlanet.isPresent())
            throw new NotFoundException("Planeta de ID " + uuid + " não cadastrado.");

        return Optional.of(getUpdatedPlanet(optionalPlanet));
    }

    private Planet getUpdatedPlanet(Optional<Planet> optionalPlanet) {
        Planet planet = optionalPlanet.get();
        planet.setAmountMoviesApeared(swapiService.getSwapiPlanetByUri(planet.getUri()).getAmountMoviesApeared());
        return planet;
    }

}
