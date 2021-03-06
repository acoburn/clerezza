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
package org.apache.clerezza.rdf.core.sparql;

import java.util.Iterator;
import java.util.List;

/**
 * The reult of a sparql SELECT-query. This corresponds to a Solution Sequence
 * as per section 12.1.6 of http://www.w3.org/TR/rdf-sparql-query/.
 *
 * Note that the scope of blank nodes is the reult set and not the
 * Graph from where they originate.
 *
 * @author rbn
 */
public interface ResultSet extends Iterator<SolutionMapping> {

    /** Iterate over the variable names (strings) in this QuerySolution.
     * @return Iterator of strings
     */ 
    public List<String> getResultVars() ;
}
