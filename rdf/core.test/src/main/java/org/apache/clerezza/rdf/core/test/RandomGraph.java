/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.clerezza.rdf.core.test;

import java.util.Iterator;
import java.util.UUID;
import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.lang.RandomStringUtils;

/**
 * A <code>Graph</code> wrapper that allows growing and shrinking of
 * the wrapped mgraph.
 *
 * @author mir
 */
public class RandomGraph extends GraphWrapper {
    
    private int interconnectivity = 2;

    public RandomGraph(Graph mGraph, int interconnectivity) {
        super(mGraph);
        this.interconnectivity = interconnectivity;
    }

    /**
     * Creates a new random mutual graph.
     *
     * @param initialSize Determines the initial size of the content graph
     * @param interconnectivity Determines the probability of using already existing
     *        resource when creating a new triple. The probability of using an existing
     *        resource over creating a new resouce is 1-(1/interconnectivity).
     * @param mGraph
     */
    public RandomGraph(int initialSize, int interconnectivity, Graph mGraph) {
        super(mGraph);
        if (interconnectivity <= 0) {
            throw new IllegalArgumentException("growth speed and the interconnectivity "
                    + "value have to be equals or highter one");
        }
        this.interconnectivity = interconnectivity;

        setupInitialSize(initialSize);
    }

    /**
     * Add or removes randomly a triple.
     *
     * @return the triple that was added or removed.
     */
    public Triple evolve() {
        Triple triple;
        int random = rollDice(2);
        if (random == 0 && size() != 0) {
            triple = getRandomTriple();
            remove(triple);
        } else {
            triple = createRandomTriple();
            add(triple);
        }
        return triple;
    }

    /**
     * Removes a random triple.
     *
     * @return the triple that was removed.
     */
    public Triple removeRandomTriple() {
        Triple randomTriple = getRandomTriple();
        remove(randomTriple);
        return randomTriple;
    }

    /**
     * Adds a random triple.
     *
     * @return the triple that was added.
     */
    public Triple addRandomTriple() {
        Triple randomTriple;
        do {
         randomTriple = createRandomTriple();
        } while(contains(randomTriple));
        
        add(randomTriple);
        return randomTriple;
    }
    
    private Triple createRandomTriple() {
        return new TripleImpl(getSubject(), getPredicate(), getObject());
    }

    private BlankNodeOrIRI getSubject() {
        int random = rollDice(interconnectivity);
        if (size() == 0) {
            random = 0;
        }
        switch (random) {
            case 0: // create new BlankNodeOrIRI
                RDFTerm newRDFTerm;
                do {
                    newRDFTerm = createRandomRDFTerm();
                } while (!(newRDFTerm instanceof BlankNodeOrIRI));
                return (BlankNodeOrIRI) newRDFTerm;
            default: // get existing BlankNodeOrIRI
                RDFTerm existingRDFTerm;
                do {
                    existingRDFTerm = getExistingRDFTerm();
                    if (existingRDFTerm == null) {
                        random = 0;
                    }
                } while (!(existingRDFTerm instanceof BlankNodeOrIRI));

                return (BlankNodeOrIRI) existingRDFTerm;
        }
    }

    private IRI getPredicate() {
        int random = rollDice(interconnectivity);
        if (size() == 0) {
            random = 0;
        }
        switch (random) {
            case 0: // create new IRI
                return createRandomIRI();
            default: // get existing IRI
                RDFTerm existingRDFTerm;
                do {
                    existingRDFTerm = getExistingRDFTerm();
                    if (existingRDFTerm == null) {
                        random = 0;
                    }
                } while (!(existingRDFTerm instanceof IRI));
                return (IRI) existingRDFTerm;
        }
    }

    private RDFTerm getObject() {
        int random = rollDice(interconnectivity);
        if (size() == 0) {
            random = 0;
        }        
        switch (random) {
            case 0: // create new resource
                return createRandomRDFTerm();
            default: // get existing resource
                RDFTerm existingRDFTerm = getExistingRDFTerm();
                if (existingRDFTerm == null) {
                    random = 0;
                }
                return existingRDFTerm;
        }
    }

    private static int rollDice(int faces) {
        return Double.valueOf(Math.random() * faces).intValue();
    }

    private RDFTerm createRandomRDFTerm() {
        switch (rollDice(3)) {
            case 0:
                return new BlankNode();
            case 1:
                return createRandomIRI();
            case 2:
                return new PlainLiteralImpl(RandomStringUtils.random(rollDice(100) + 1));
        }
        throw new RuntimeException("in createRandomRDFTerm()");
    }

    private RDFTerm getExistingRDFTerm() {
        Triple triple = getRandomTriple();
        if (triple == null) {
            return null;
        }
        switch (rollDice(3)) {
            case 0:
                return triple.getSubject();
            case 1:
                return triple.getPredicate();
            case 2:
                return triple.getObject();
        }
        return null;
    }

    private IRI createRandomIRI() {
        return new IRI("http://" + UUID.randomUUID().toString());
    }

    /**
     * Returns a random triple contained in the Graph.
     */
    public Triple getRandomTriple() {
        int size = this.size();
        if (size == 0) {
            return null;
        }
        Iterator<Triple> triples = iterator();
        while (triples.hasNext()) {
            Triple triple = triples.next();
            if (rollDice(this.size()) == 0) {
                return triple;
            }
        }
        return getRandomTriple();
    }

    private void setupInitialSize(int initialSize) {
        for (int i = 0; i < initialSize; i++) {
            addRandomTriple();
        }
    }
}
