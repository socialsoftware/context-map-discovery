package org.contextmapper.discovery.strategies.boundedcontexts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.contextmapper.discovery.model.Aggregate;
import org.contextmapper.discovery.model.BoundedContext;
import org.contextmapper.discovery.model.DomainObject;
import org.contextmapper.discovery.model.DomainObjectType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Mono2MicroBoundedContextDiscoveryStrategy extends AbstractBoundedContextDiscoveryStrategy implements BoundedContextDiscoveryStrategy {

    private File sourcePath;

    public Mono2MicroBoundedContextDiscoveryStrategy(File sourcePath) {
        this.sourcePath = sourcePath;
    }

    @Override
    public Set<BoundedContext> discoverBoundedContexts() {
        Set<BoundedContext> boundedContexts = new HashSet<>();

        for (File decompositionFile : findMono2microDecompositionFiles()) {
            boundedContexts.addAll(readFile(decompositionFile));
        }

        return boundedContexts;
    }

    public Set<BoundedContext> readFile(File fileToRead) {
        Set<BoundedContext> boundedContexts = new HashSet<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Map<String, Object> map = objectMapper.readValue(
                    new FileInputStream(fileToRead),
                    new TypeReference<HashMap<String, Object>>() {}
            );

            Map<String, List<Object>> decomposition = (Map<String, List<Object>>) map.get("decomposition");
            List<Object> clusters = decomposition.get("clusters");

            for (Object cluster : clusters) {
                Map<String, Object> clusterMap = (Map<String, Object>) cluster;

                Set<DomainObject> domainObjects = discoverDomainEntities(clusterMap);
                String clusterName = (String) clusterMap.get("name");

                Aggregate aggregate = new Aggregate(clusterName);
                aggregate.addDomainObjects(domainObjects);

                BoundedContext boundedContext = new BoundedContext(clusterName);
                boundedContext.addAggregate(aggregate);

                boundedContexts.add(boundedContext);
            }

            return boundedContexts;

        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("The file '" + fileToRead + "' does not exist!", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("The file '" + fileToRead + "' could not be read by the JSON parser", e);
        }
    }

    public void discoverAggregates() {
    }

    public Set<DomainObject> discoverDomainEntities(Map<String, Object> cluster) {
        List<Object> elements = (List<Object>) cluster.get("elements");
        Set<DomainObject> domainObjects = new HashSet<>();

        for (Object element : elements) {
            String elementName = (String) ((Map<String, Object>) element).get("name");
            domainObjects.add(new DomainObject(DomainObjectType.ENTITY, elementName));
        }

        return domainObjects;
    }

    private Collection<File> findMono2microDecompositionFiles() {
        return FileUtils.listFiles(sourcePath, new NameFileFilter("m2m_decomposition_data.json"), TrueFileFilter.INSTANCE);
    }
}
