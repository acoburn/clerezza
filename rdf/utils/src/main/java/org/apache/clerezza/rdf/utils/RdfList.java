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
package org.apache.clerezza.rdf.utils;

import java.io.FileOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.commons.rdf.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of an <code>java.util.List</code> backed by an RDF
 * collection (rdf:List). The list allows modification that are reflected
 * to the underlying <code>Graph</code>. It reads the data from the
 * <code>Graph</code> when it is first needed, so changes to the
 * Graph affecting the rdf:List may or may not have an effect on the
 * values returned by instances of this class. For that reason only one
 * instance of this class should be used for accessing an rdf:List of sublists
 * thereof when the lists are being modified, having multiple lists exclusively
 * for read operations (such as for immutable <code>Graph</code>s) is
 * not problematic.
 *
 * @author rbn, mir
 */
public class RdfList extends AbstractList<RDFTerm> {

    private static final Logger logger = LoggerFactory.getLogger(RdfList.class);

    private final static IRI RDF_NIL =
            new IRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#nil");
    /**
     * a list of the linked rdf:List elements in order
     */
    private List<BlankNodeOrIRI> listList = new ArrayList<BlankNodeOrIRI>();
    private List<RDFTerm> valueList = new ArrayList<RDFTerm>();
    private BlankNodeOrIRI firstList;
    private Graph tc;
    private boolean totallyExpanded = false;

    /**
     * Get a list for the specified resource.
     *
     * If the list is modified using the created instance
     * <code>listRDFTerm</code> will always be the first list.
     *
     * @param listRDFTerm
     * @param tc
     */
    public RdfList(BlankNodeOrIRI listRDFTerm, Graph tc) {
        firstList = listRDFTerm;
        this.tc = tc;

    }

    /**
     * Get a list for the specified resource node.
     *
     * @param listNode
     */
    public RdfList(GraphNode listNode) {
        this((BlankNodeOrIRI)listNode.getNode(), listNode.getGraph());
    }

    /**
     * Creates an empty RdfList by writing a triple
     * "{@code listRDFTerm} owl:sameAs rdf.nil ." to {@code tc}.
     *
     * @param listRDFTerm
     * @param tc
     * @return    an empty rdf:List.
     * @throws IllegalArgumentException
     *        if the provided {@code  listRDFTerm} is a non-empty rdf:List.
     */
    public static RdfList createEmptyList(BlankNodeOrIRI listRDFTerm, Graph tc)
            throws IllegalArgumentException {

        if (!tc.filter(listRDFTerm, RDF.first, null).hasNext()) {
            RdfList list = new RdfList(listRDFTerm, tc);
            list.tc.add(new TripleImpl(listRDFTerm, OWL.sameAs, RDF_NIL));
            return list;
        } else {
            throw new IllegalArgumentException(listRDFTerm + "is a non-empty rdf:List.");
        }
    }

    private void expandTill(int pos) {
        if (totallyExpanded) {
            return;
        }
        BlankNodeOrIRI currentList;
        if (listList.size() > 0) {
            currentList = listList.get(listList.size()-1);
        } else {
            currentList = firstList;
            if (!tc.filter(currentList, RDF.first, null).hasNext()) {
                return;
            }
            listList.add(currentList);
            valueList.add(getFirstEntry(currentList));
        }
        if (listList.size() >= pos) {
            return;
        }
        while (true) {                
            currentList = getRest(currentList);
            if (currentList.equals(RDF_NIL)) {
                totallyExpanded = true;
                break;
            }
            if (listList.size() == pos) {
                break;
            }
            valueList.add(getFirstEntry(currentList));
            listList.add(currentList);
        }
    }



    @Override
    public RDFTerm get(int index) {
        expandTill(index + 1);
        return valueList.get(index);
    }

    @Override
    public int size() {
        expandTill(Integer.MAX_VALUE);        
        return valueList.size();
    }

    @Override
    public void add(int index, RDFTerm element) {
        expandTill(index);
        if (index == 0) {
            //special casing to make sure the first list remains the same resource
            if (listList.size() == 0) {
                tc.remove(new TripleImpl(firstList, OWL.sameAs, RDF_NIL));
                tc.add(new TripleImpl(firstList, RDF.rest, RDF_NIL));
                tc.add(new TripleImpl(firstList, RDF.first, element));
                listList.add(firstList);
            } else {
                tc.remove(new TripleImpl(listList.get(0), RDF.first, valueList.get(0)));
                tc.add(new TripleImpl(listList.get(0), RDF.first, element));
                addInRdfList(1, valueList.get(0));
            }
        } else {
            addInRdfList(index, element);
        }
        valueList.add(index, element);
    }
    
    /**
     *
     * @param index is > 0
     * @param element
     */
    private void addInRdfList(int index, RDFTerm element) {
        expandTill(index+1);
        BlankNodeOrIRI newList = new BlankNode() {
        };
        tc.add(new TripleImpl(newList, RDF.first, element));
        if (index < listList.size()) {
            tc.add(new TripleImpl(newList, RDF.rest, listList.get(index)));
            tc.remove(new TripleImpl(listList.get(index - 1), RDF.rest, listList.get(index)));
        } else {
            tc.remove(new TripleImpl(listList.get(index - 1), RDF.rest, RDF_NIL));
            tc.add(new TripleImpl(newList, RDF.rest, RDF_NIL));

        }
        tc.add(new TripleImpl(listList.get(index - 1), RDF.rest, newList));
        listList.add(index, newList);
    }

    @Override
    public RDFTerm remove(int index) {
        //keeping the first list resource
        tc.remove(new TripleImpl(listList.get(index), RDF.first, valueList.get(index)));
        if (index == (listList.size() - 1)) {
            tc.remove(new TripleImpl(listList.get(index), RDF.rest, RDF_NIL));    
            if (index > 0) {
                tc.remove(new TripleImpl(listList.get(index - 1), RDF.rest, listList.get(index)));
                tc.add(new TripleImpl(listList.get(index - 1), RDF.rest, RDF_NIL));
            } else {
                tc.add(new TripleImpl(listList.get(index), OWL.sameAs, RDF_NIL));
            }
            listList.remove(index);
        } else {
            tc.add(new TripleImpl(listList.get(index), RDF.first, valueList.get(index+1)));
            tc.remove(new TripleImpl(listList.get(index), RDF.rest, listList.get(index + 1)));
            tc.remove(new TripleImpl(listList.get(index + 1), RDF.first, valueList.get(index + 1)));
            if (index == (listList.size() - 2)) {
                tc.remove(new TripleImpl(listList.get(index + 1), RDF.rest, RDF_NIL));
                tc.add(new TripleImpl(listList.get(index), RDF.rest, RDF_NIL));
            } else {
                tc.remove(new TripleImpl(listList.get(index + 1), RDF.rest, listList.get(index + 2)));
                tc.add(new TripleImpl(listList.get(index), RDF.rest, listList.get(index + 2)));
            }
            listList.remove(index+1);
        }
        return valueList.remove(index);
    }

    private BlankNodeOrIRI getRest(BlankNodeOrIRI list) {
        return (BlankNodeOrIRI) tc.filter(list, RDF.rest, null).next().getObject();
    }

    private RDFTerm getFirstEntry(final BlankNodeOrIRI listRDFTerm) {
        try {
            return tc.filter(listRDFTerm, RDF.first, null).next().getObject();
        } catch (final NullPointerException e) {
            RuntimeException runtimeEx = AccessController.doPrivileged(new PrivilegedAction<RuntimeException>() {
                @Override
                public RuntimeException run(){
                    try {
                        final FileOutputStream fileOutputStream = new FileOutputStream("/tmp/broken-list.nt");
                        final GraphNode graphNode = new GraphNode(listRDFTerm, tc);
                        Serializer.getInstance().serialize(fileOutputStream, graphNode.getNodeContext(), SupportedFormat.N_TRIPLE);
                        fileOutputStream.flush();
                        logger.warn("GraphNode: " + graphNode);
                        final Iterator<IRI> properties = graphNode.getProperties();
                        while (properties.hasNext()) {
                            logger.warn("available: " + properties.next());
                        }
                        return new RuntimeException("broken list " + listRDFTerm, e);
                    } catch (Exception ex) {
                        return new RuntimeException(ex);
                    }

                }
            });
            throw runtimeEx;
        }
    }

    public BlankNodeOrIRI getListRDFTerm() {
        return firstList;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RdfList other = (RdfList) obj;

        if (!other.firstList.equals(this.firstList)) {
            return false;
        }

        if (!other.tc.equals(this.tc)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return 17 * this.firstList.hashCode() + this.tc.hashCode();
    }

    /**
     * Returns the rdf lists of which the specified <code>GraphNode</code> is
     * an element of. Sublists of other lists are not returned.
     *
     * @param element
     * @return
     */
    public static Set<RdfList> findContainingLists(GraphNode element) {
        Set<GraphNode> listNodes = findContainingListNodes(element);
        if (listNodes.isEmpty()) {
            return null;
        }

        Set<RdfList> rdfLists = new HashSet<RdfList>();
        for (Iterator<GraphNode> it = listNodes.iterator(); it.hasNext();) {
            GraphNode listNode = it.next();
            rdfLists.add(new RdfList(listNode));
        }
        return rdfLists;
    }

    /**
     * Returns a set of <code>GraphNode</code>S which are the first list nodes (meaning
     * they are not the beginning of a sublist) of the list containing the specified
     * <code>GraphNode</code> as an element.
     *
     * @param element
     * @return
     */
    public static Set<GraphNode> findContainingListNodes(GraphNode element) {
        Iterator<GraphNode> partOfaListNodesIter = element.getSubjectNodes(RDF.first);
        if (!partOfaListNodesIter.hasNext()) {
            return null;
        }
        Set<GraphNode> listNodes = new HashSet<GraphNode>();

        while (partOfaListNodesIter.hasNext()) {
            listNodes.addAll(findAllListNodes(partOfaListNodesIter.next()));
        }
        return listNodes;
    }
    
    private static Set<GraphNode> findAllListNodes(GraphNode listPart) {
        Iterator<GraphNode> invRestNodesIter;
        Set<GraphNode> listNodes = new HashSet<GraphNode>();
        do {
            invRestNodesIter = listPart.getSubjectNodes(RDF.rest);
            if (invRestNodesIter.hasNext()) {
                listPart = invRestNodesIter.next();
                while (invRestNodesIter.hasNext()) {
                    GraphNode graphNode = invRestNodesIter.next();
                    listNodes.addAll(findAllListNodes(graphNode));
                }
            } else {
                listNodes.add(listPart);
                break;
            }
        } while (true);
        return listNodes;
    }
}
