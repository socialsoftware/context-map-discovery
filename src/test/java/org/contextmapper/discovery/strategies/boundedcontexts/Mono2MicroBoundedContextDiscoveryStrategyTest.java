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
import org.contextmapper.discovery.strategies.names.SeparatorToCamelCaseBoundedContextNameMappingStrategy;
import org.contextmapper.discovery.strategies.relationships.DockerComposeRelationshipDiscoveryStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Mono2MicroBoundedContextDiscoveryStrategyTest {

    @Test
    public void canReadMono2MicroDataFile() {
        // given
        ContextMapDiscoverer discoverer = new ContextMapDiscoverer()
                .usingBoundedContextDiscoveryStrategies(
                        new Mono2MicroBoundedContextDiscoveryStrategy(new File("./src/test/resources/test/mono2micro-tests"))
                );

        // when
        Set<BoundedContext> boundedContexts = discoverer.discoverContextMap().getBoundedContexts();

        // then
        assertEquals(5, boundedContexts.size());
        BoundedContext bc = boundedContexts.iterator().next();
        assertEquals(1, bc.getAggregates().size());
        Aggregate aggregate = bc.getAggregates().iterator().next();
        assertEquals("Cluster0", aggregate.getName());
        assertEquals(5, aggregate.getDomainObjects().size());
    }

}
