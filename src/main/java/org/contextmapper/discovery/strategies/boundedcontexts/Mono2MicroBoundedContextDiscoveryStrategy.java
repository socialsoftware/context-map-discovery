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

    private static final String M2M_DATA_FILE_CONTRACT = "m2m_decomposition_data.json";

    public Mono2MicroBoundedContextDiscoveryStrategy(File sourcePath) {
        this.sourcePath = sourcePath;
    }

    @Override
    public Set<BoundedContext> discoverBoundedContexts() {
        Set<BoundedContext> boundedContexts = new HashSet<>();
        for (File decompositionFile : findMono2microDecompositionFiles()) {
            boundedContexts.addAll(discoverBoundedContexts(decompositionFile));
        }

        return boundedContexts;
    }

    private Set<BoundedContext> discoverBoundedContexts(File m2mDataFile) {
        Set<BoundedContext> boundedContexts = new HashSet<>();
        for (Cluster cluster : parseM2MDecomposition(m2mDataFile)) {
            BoundedContext boundedContext = new BoundedContext(cluster.getName());
            boundedContext.addAggregate(discoverAggregate(cluster));
            boundedContexts.add(boundedContext);
        }

        return boundedContexts;
    }

    private Aggregate discoverAggregate(Cluster cluster) {
        Aggregate aggregate = new Aggregate(cluster.getName());
        aggregate.addDomainObjects(discoverDomainObjects(cluster));
        return aggregate;
    }

    private Set<DomainObject> discoverDomainObjects(Cluster cluster) {
        Set<DomainObject> domainObjects = new HashSet<>();
        for (String elementName : cluster.getElementNames()) {
            domainObjects.add(new DomainObject(DomainObjectType.ENTITY, elementName));
        }

        return domainObjects;
    }

    protected Set<Cluster> parseM2MDecomposition(File m2mDataFile) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Map<String, Object> objectMap = objectMapper.readValue(
                    new FileInputStream(m2mDataFile),
                    new TypeReference<HashMap<String, Object>>() {});

            return parseClusters(objectMap);

        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("The file '" + m2mDataFile + "' does not exist!", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("The file '" + m2mDataFile + "' could not be read by the JSON parser!", e);
        }
    }

    private Set<Cluster> parseClusters(Map<String, Object> objectMap) {
        Set<Cluster> clusters = new HashSet<>();

        Map<String, Object> decomposition = (Map<String, Object>) objectMap.get("decomposition");
        if (decomposition == null) {
            return clusters;
        }

        for (Object clusterObj : (List<Object>) decomposition.get("clusters")) {
            Map<String, Object> clusterMap = (Map<String, Object>) clusterObj;

            String clusterName = (String) clusterMap.get("name");
            if (clusterName == null) {
                continue;
            }

            Cluster cluster = new Cluster(clusterName);
            cluster.addElementNames(parseClusterElements(clusterMap));
            clusters.add(cluster);
        }

        return clusters;
    }

    private Set<String> parseClusterElements(Map<String, Object> clusterMap) {
        Set<String> elementNames = new HashSet<>();

        for (Object element : (List<Object>) clusterMap.get("elements")) {
            String elementName = (String) ((Map<String, Object>) element).get("name");
            if (elementName == null) {
                continue;
            }
            elementNames.add(elementName);
        }

        return elementNames;
    }

    private Collection<File> findMono2microDecompositionFiles() {
        return FileUtils.listFiles(sourcePath, new NameFileFilter(M2M_DATA_FILE_CONTRACT), TrueFileFilter.INSTANCE);
    }

    protected class Cluster {

        private String name;
        private Set<String> elementNames;

        Cluster(String name) {
            this.name = name;
            this.elementNames = new HashSet<>();
        }

        protected String getName() {
            return name;
        }

        protected Set<String> getElementNames() {
            return elementNames;
        }

        protected void addElementNames(Set<String> elementNames) {
            this.elementNames.addAll(elementNames);
        }
    }
}
