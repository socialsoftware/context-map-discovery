package org.contextmapper.discovery.strategies.boundedcontexts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.contextmapper.discovery.model.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.Set;

public class Mono2MicroBoundedContextDiscoveryStrategy extends AbstractBoundedContextDiscoveryStrategy implements BoundedContextDiscoveryStrategy {

    private File sourcePath;
    private Map<String, DomainObject> domainObjectsByName;

    public Mono2MicroBoundedContextDiscoveryStrategy(File sourcePath) {
        this.sourcePath = sourcePath;
        this.domainObjectsByName = new HashMap<>();
    }

    @Override
    public Set<BoundedContext> discoverBoundedContexts() {
        Set<BoundedContext> boundedContexts = new HashSet<>();
        for (File m2mClustersFile : findM2MDecompositionFiles()) {
            boundedContexts.addAll(discoverBoundedContexts(m2mClustersFile));
        }

        // TODO : Update discovery model to also consider Domain Object superclass
        for (File m2mStructureFile : findM2MStructureFiles()) {
            updateDomainObjectsAttributes(m2mStructureFile);
        }

        return boundedContexts;
    }

    private Set<BoundedContext> discoverBoundedContexts(File m2mClustersFile) {
        Set<BoundedContext> boundedContexts = new HashSet<>();
        for (M2MCluster m2mCluster : parseM2MFile(m2mClustersFile).getClusters()) {
            BoundedContext boundedContext = new BoundedContext(m2mCluster.getName());
            boundedContext.addAggregate(discoverAggregate(m2mCluster));
            boundedContexts.add(boundedContext);
        }

        return boundedContexts;
    }

    private Aggregate discoverAggregate(M2MCluster m2mCluster) {
        Aggregate aggregate = new Aggregate(m2mCluster.getName());
        aggregate.addDomainObjects(discoverDomainObjects(m2mCluster));
        return aggregate;
    }

    private Set<DomainObject> discoverDomainObjects(M2MCluster m2mCluster) {
        Set<DomainObject> domainObjects = new HashSet<>();
        for (M2MEntity m2mEntity : m2mCluster.getElements()) {
            DomainObject domainObject = new DomainObject(DomainObjectType.ENTITY, m2mEntity.getName());
            domainObjects.add(domainObject);
            domainObjectsByName.put(m2mEntity.getName(), domainObject);
        }

        return domainObjects;
    }

    private void updateDomainObjectsAttributes(File m2mStructureFile) {
        M2MDecomposition m2mDecomposition = parseM2MFile(m2mStructureFile);
        for (M2MEntity m2mEntity : m2mDecomposition.getEntities()) {
            DomainObject domainObject = domainObjectsByName.get(m2mEntity.getName());
            if (domainObject == null) {
                // Structural data may collect other domain objects not considered in the decomposition.
                // References to these will be mapped as primitive types.
                continue;
            }
            discoverAttributes(domainObject, m2mEntity.getFields());
        }
    }

    private void discoverAttributes(DomainObject domainObject, List<M2MField> m2mFields) {
        for (M2MField m2mField : m2mFields) {
            M2MDataType m2mDataType = m2mField.getType();
            domainObject.addAttribute(new Attribute(discoverType(m2mDataType), m2mField.getName()));
        }
    }

    private Type discoverType(M2MDataType m2mDataType) {
        Type type;

        if (domainObjectsByName.containsKey(m2mDataType.getName())) {
            type = new Type(domainObjectsByName.get(m2mDataType.getName()));
        } else if (m2mDataType.isParameterizedType()) {
            // Only considering 1 parameter (List, Set, Collection)
            type = discoverType(m2mDataType.getParameters().get(0));
        } else {
            type = new Type(m2mDataType.getName());
        }

        if (m2mDataType.isCollectionType()) {
            type.setCollectionType(m2mDataType.getName());
        }

        return type;
    }

    protected M2MDecomposition parseM2MFile(File m2mFile) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(m2mFile, M2MDecomposition.class);

        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("The file '" + m2mFile + "' does not exist!", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("The file '" + m2mFile + "' could not be parsed!", e);
        }
    }

    private Collection<File> findM2MDecompositionFiles() {
        return FileUtils.listFiles(sourcePath, new NameFileFilter("m2m_decomposition.json"), TrueFileFilter.INSTANCE);
    }

    private Collection<File> findM2MStructureFiles() {
        return FileUtils.listFiles(sourcePath, new SuffixFileFilter("m2m_structure.json"), TrueFileFilter.INSTANCE);
    }

    protected static class M2MDecomposition {

        private String name;
        private Set<M2MCluster> clusters;
        private Set<M2MEntity> entities;
        private Set<M2MFunctionality> functionalities;

        public M2MDecomposition(String name) {
            this.name = name;
            this.clusters = new HashSet<>();
            this.entities = new HashSet<>();
        }

        public M2MDecomposition() {
            this.clusters = new HashSet<>();
            this.entities = new HashSet<>();
            this.functionalities = new HashSet<>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Set<M2MCluster> getClusters() {
            return clusters;
        }

        public Set<M2MEntity> getEntities() {
            return entities;
        }
    }

    protected static class M2MCluster {

        private String name;
        private Set<M2MEntity> elements;

        public M2MCluster(String name) {
            this.name = name;
            this.elements = new HashSet<>();
        }

        public M2MCluster() {
            this.elements = new HashSet<>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Set<M2MEntity> getElements() {
            return elements;
        }

        public void addElement(M2MEntity m2mEntity) {
            this.elements.add(m2mEntity);
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof M2MCluster && getName().equals(((M2MCluster) obj).getName());
        }
    }

    protected static class M2MEntity {

        private String name;
        private int id;
        private List<M2MField> fields;
        private M2MDataType superclass;

        public M2MEntity(String name) {
            this.name = name;
            this.fields = new ArrayList<>();
        }

        public M2MEntity() {
            this.fields = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        public List<M2MField> getFields() {
            return fields;
        }

        public M2MDataType getSuperclass() {
            return superclass;
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof M2MEntity && getName().equals(((M2MEntity) obj).getName());
        }
    }

    protected static class M2MField {

        private String name;
        private M2MDataType type;

        public M2MField(String name, M2MDataType type) {
            this.name = name;
            this.type = type;
        }

        public M2MField() {
        }

        public String getName() {
            return name;
        }

        public M2MDataType getType() {
            return type;
        }
    }

    protected static class M2MDataType {

        private String name;
        private List<M2MDataType> parameters;

        public M2MDataType(String name) {
            this.name = name;
            this.parameters = new ArrayList<>();
        }

        public M2MDataType() {
            this.parameters = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public List<M2MDataType> getParameters() {
            return parameters;
        }

        public boolean isParameterizedType() {
            return !parameters.isEmpty();
        }

        public boolean isCollectionType() {
            return isParameterizedType() &&
                    (name.equals("List") || name.equals("Set") || name.equals("Collection"));
        }
    }

    protected static class M2MFunctionality {

        private String name;
        private String orchestrator;
        private List<M2MSagaStep> steps;

        public M2MFunctionality() {
            this.steps = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public String getOrchestrator() {
            return orchestrator;
        }

        public List<M2MSagaStep> getSteps() {
            return steps;
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof M2MFunctionality && getName().equals(((M2MFunctionality) obj).getName());
        }
    }

    protected static class M2MSagaStep {

        private String cluster;
        private List<M2MEntityAccess> accesses;

        public M2MSagaStep() {
            this.accesses = new ArrayList<>();
        }

        public String getCluster() {
            return cluster;
        }

        public List<M2MEntityAccess> getAccesses() {
            return accesses;
        }
    }

    protected static class M2MEntityAccess {

        private String type;
        private String entity;

        public M2MEntityAccess() {
        }

        public String getType() {
            return type;
        }

        public String getEntity() {
            return entity;
        }
    }
}
