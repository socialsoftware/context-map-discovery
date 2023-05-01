/*
 * Copyright 2019 The Context Mapper Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.contextmapper.discovery.strategies.boundedcontexts;

import org.contextmapper.discovery.ContextMapDiscoverer;
import org.contextmapper.discovery.model.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class Mono2MicroBoundedContextDiscoveryStrategyTest {

    @Test
    public void canDiscoverBoundedContextsFromClusters() {
        // given
        ContextMapDiscoverer discoverer = new ContextMapDiscoverer()
                .usingBoundedContextDiscoveryStrategies(
                        new Mono2MicroBoundedContextDiscoveryStrategy(
                                new File("./src/test/resources/test/mono2micro/valid-contract")));

        // when
        Set<BoundedContext> boundedContexts = discoverer.discoverContextMap().getBoundedContexts();

        // then
        assertEquals(5, boundedContexts.size());
        assertTrue(boundedContexts.stream()
                .map(BoundedContext::getName)
                .collect(Collectors.toList())
                .containsAll(Arrays.asList("Cluster0", "Cluster1", "Cluster2", "Cluster3", "Cluster4")));
    }

    @Test
    public void canDiscoverAggregatesFromClusters() {
        // given
        ContextMapDiscoverer discoverer = new ContextMapDiscoverer()
                .usingBoundedContextDiscoveryStrategies(
                        new Mono2MicroBoundedContextDiscoveryStrategy(
                                new File("./src/test/resources/test/mono2micro/valid-contract")));

        // when
        Set<BoundedContext> boundedContexts = discoverer.discoverContextMap().getBoundedContexts();

        // then
        assertTrue(boundedContexts.size() > 0);
        BoundedContext boundedContext = boundedContexts.iterator().next();
        assertEquals(1, boundedContext.getAggregates().size());
        Aggregate aggregate = boundedContext.getAggregates().iterator().next();
        assertEquals(aggregate.getName(), boundedContext.getName());
    }

    @Test
    public void canDiscoverDomainObjectsFromClusterElements() {
        // given
        ContextMapDiscoverer discoverer = new ContextMapDiscoverer()
                .usingBoundedContextDiscoveryStrategies(
                        new Mono2MicroBoundedContextDiscoveryStrategy(
                                new File("./src/test/resources/test/mono2micro/valid-contract")));

        // when
        Set<BoundedContext> boundedContexts = discoverer.discoverContextMap().getBoundedContexts();

        // then
        assertTrue(boundedContexts.size() > 0);
        BoundedContext boundedContext = boundedContexts.stream()
                .filter(bc -> bc.getName().equals("Cluster0"))
                .findFirst().orElse(null);
        assertNotNull(boundedContext);
        assertEquals(1, boundedContext.getAggregates().size());
        Set<DomainObject> domainObjects = boundedContext.getAggregates().iterator().next().getDomainObjects();
        assertEquals(5, domainObjects.size());
        assertTrue(domainObjects.stream().allMatch(bc -> bc.getType().equals(DomainObjectType.ENTITY)));
        assertTrue(domainObjects.stream()
                .map(DomainObject::getName)
                .collect(Collectors.toList())
                .containsAll(Arrays.asList("CodeOrderAnswer", "AnswerDetails", "QuizAnswerItem", "CodeFillInAnswer", "MultipleChoiceAnswer")));
    }

    @Test
    public void emptyResultIfM2MContractHasInvalidFormat() {
        // given
        ContextMapDiscoverer discoverer = new ContextMapDiscoverer()
                .usingBoundedContextDiscoveryStrategies(
                        new Mono2MicroBoundedContextDiscoveryStrategy(
                                new File("./src/test/resources/test/mono2micro/invalid-contract")));

        // when
        Set<BoundedContext> boundedContexts = discoverer.discoverContextMap().getBoundedContexts();

        // then
        assertEquals(0, boundedContexts.size());
    }
}
