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
package org.apache.clerezza.rdf.utils.smushing;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility to equate duplicate nodes in an Mgarph, currently only nodes with 
 * a shared ifp are equated.
 *
 * @author reto
 */
public class IfpSmusher extends BaseSmusher {
    
    static final Logger log = LoggerFactory.getLogger(IfpSmusher.class);

    /**
     * smush mGaph given the ontological facts. Currently it does only
     * one step ifp smushin, i.e. only ifps are taken in account and only
     * nodes that have the same node as ifp object in the orignal graph are
     * equates. (calling the method a second time might lead to additional
     * smushings.)
     *
     * @param mGraph
     * @param tBox
     */
    public void smush(Graph mGraph, Graph tBox) {
        final Set<IRI> ifps = getIfps(tBox);
        final Map<PredicateObject, Set<BlankNodeOrIRI>> ifp2nodesMap = new HashMap<PredicateObject, Set<BlankNodeOrIRI>>();
        for (Iterator<Triple> it = mGraph.iterator(); it.hasNext();) {
            final Triple triple = it.next();
            final IRI predicate = triple.getPredicate();
            if (!ifps.contains(predicate)) {
                continue;
            }
            final PredicateObject po = new PredicateObject(predicate, triple.getObject());
            Set<BlankNodeOrIRI> equivalentNodes = ifp2nodesMap.get(po);
            if (equivalentNodes == null) {
                equivalentNodes = new HashSet<BlankNodeOrIRI>();
                ifp2nodesMap.put(po, equivalentNodes);
            }
            equivalentNodes.add(triple.getSubject());
        }
        Set<Set<BlankNodeOrIRI>> unitedEquivalenceSets = uniteSetsWithCommonElement(ifp2nodesMap.values());
        smush(mGraph, unitedEquivalenceSets, true);
    }
    

    private Set<IRI> getIfps(Graph tBox) {
        final Iterator<Triple> ifpDefinitions = tBox.filter(null, RDF.type,
                OWL.InverseFunctionalProperty);
        final Set<IRI> ifps = new HashSet<IRI>();
        while (ifpDefinitions.hasNext()) {
            final Triple triple = ifpDefinitions.next();
            ifps.add((IRI) triple.getSubject());
        }
        return ifps;
    }

    private <T> Set<Set<T>> uniteSetsWithCommonElement(
            Collection<Set<T>> originalSets) {
        Set<Set<T>> result = new HashSet<Set<T>>();
        Iterator<Set<T>> iter = originalSets.iterator();
        while (iter.hasNext()) {
            Set<T> originalSet = iter.next();
            //TODO this could be done more efficiently with a map
            Set<T> matchingSet = getMatchinSet(originalSet, result);
            if (matchingSet != null) {
                matchingSet.addAll(originalSet);
            } else {
                result.add(new HashSet<T>(originalSet));
            }
        }
        if (result.size() < originalSets.size()) {
            return uniteSetsWithCommonElement(result);
        } else {
            return result;
        }
    }

    private <T> Set<T> getMatchinSet(Set<T> set, Set<Set<T>> setOfSet) {
        for (Set<T> current : setOfSet) {
            if (shareElements(set,current)) {
                return current;
            }
        }
        return null;
    }

    private <T> boolean shareElements(Set<T> set1, Set<T> set2) {
        for (T elem : set2) {
            if (set1.contains(elem)) {
                return true;
            }
        }
        return false;
    }
    

    class PredicateObject {

        final IRI predicate;
        final RDFTerm object;

        public PredicateObject(IRI predicate, RDFTerm object) {
            this.predicate = predicate;
            this.object = object;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PredicateObject other = (PredicateObject) obj;
            if (this.predicate != other.predicate && !this.predicate.equals(other.predicate)) {
                return false;
            }
            if (this.object != other.object && !this.object.equals(other.object)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + this.predicate.hashCode();
            hash = 13 * hash + this.object.hashCode();
            return hash;
        }

        @Override
        public String toString() {
            return "("+predicate+", "+object+")";
        }


    };
}
