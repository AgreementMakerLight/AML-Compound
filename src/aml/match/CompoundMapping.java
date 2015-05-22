/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
*                                                                             *
* Licensed under the Apache License, Version 2.0 (the "License"); you may     *
* not use this file except in compliance with the License. You may obtain a   *
* copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
*                                                                             *
* Unless required by applicable law or agreed to in writing, software         *
* distributed under the License is distributed on an "AS IS" BASIS,           *
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
* See the License for the specific language governing permissions and         *
* limitations under the License.                                              *
*                                                                             *
*******************************************************************************
* A mapping between terms or properties of two Ontologies, including the      *
* similarity and type of relationship between them.                           *
* An element in an Alignment.                                                 *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.1                                                                *
******************************************************************************/
package aml.match;

import aml.AML;
import aml.ontology.URIMap;
import aml.settings.MappingRelation;

public class CompoundMapping implements Comparable<CompoundMapping>
{

//Attributes

	//The id of the first source ontology term
	private int sourceId;
	//The id of the first target ontology term
	private int targetId1;
	//The id of the second target ontology term
	private int targetId2;
	//The similarity between the terms
	private double similarity;
	//The relationship between the terms
	private MappingRelation rel;
	
//Constructors
	public CompoundMapping(int sId)
	{
		sourceId = sId;
		targetId1 = 0;
		targetId2 = 0;
		similarity = 1.0;
		rel = MappingRelation.EQUIVALENCE;
	}

	/**
	 * Creates a mapping between sId and tId
	 * @param sId: the id of the source ontology term
	 * @param tId: the id of the target ontology term
	 * @return 
	 */
	public CompoundMapping(int sId, int tId1, int tId2)
	{
		sourceId = sId;
		targetId1 = tId1;
		targetId2 = tId2;
		similarity = 1.0;
		rel = MappingRelation.EQUIVALENCE;
	}
	
	/**
	 * Creates a mapping between sId and tId with similarity = sim
	 * @param sId: the id of the source ontology term
	 * @param tId: the id of the target ontology term
	 * @param sim: the similarity between the terms
	 * @return 
	 */
	public CompoundMapping(int sId, int tId1, int tId2, double sim)
	{
		sourceId = sId;
		targetId1 = tId1;
		targetId2 = tId2;
		similarity = Math.round(sim*10000)/10000.0;
		rel = MappingRelation.EQUIVALENCE;
	}
	
	/**
	 * Creates a mapping between sId and tId with similarity = sim
	 * @param sId: the id of the source ontology term
	 * @param tId: the id of the target ontology term
	 * @param sim: the similarity between the terms
	 * @param r: the mapping relationship between the terms
	 * @return 
	 */
	public CompoundMapping(int sId, int tId1, int tId2, double sim, MappingRelation r)
	{
		sourceId = sId;
		targetId1 = tId1;
		targetId2 = tId2;
		similarity = Math.round(sim*10000)/10000.0;
		rel = r;
	}
	
	/**
	 * Creates a new mapping that is a copy of m
	 * @param m: the mapping to copy
	 * @return 
	 */
	public CompoundMapping(CompoundMapping m)
	{
		sourceId = m.sourceId;
		targetId1 = m.targetId1;
		targetId2 = m.targetId2;
		similarity = m.similarity;
		rel = m.rel;
	}

//Public Methods

	@Override
	/**
	 * Mappings are compared based only on their similarity,
	 * which enables sorting the Alignment and selecting the
	 * best Mapping for a given term
	 */
	public int compareTo(CompoundMapping o)
	{
		double diff = this.similarity - o.similarity;
		if(diff < 0)
			return -1;
		if(diff > 0)
			return 1;
		return 0;
	}
	
	/**
	 * Two Mappings are equal if they map the same three terms
	 * irrespective of the similarity or relationship, which
	 * enables finding redundant Mappings
	 */
	public boolean equals(Object o)
	{
		CompoundMapping m = (CompoundMapping)o;
		return (this.sourceId == m.sourceId && (this.targetId1 == m.targetId1 &&
				this.targetId2 == m.targetId2) || (this.targetId1 == m.targetId2 && this.targetId2 == m.targetId1));
	}
	
	/**
	 * @return the mapping relation between the mapped terms
	 */
	public MappingRelation getRelationship()
	{
		return rel;
	}
	
	/**
	 * @return the similarity between the mapped terms
	 */
	public double getSimilarity()
	{
		return similarity;
	}
	
	/**
	 * @return the id of the first source term
	 */
	public int getSourceId()
	{
		return sourceId;
	}
	
	/**
	 * @return the id of the target 1 term
	 */
	public int getTargetId1()
	{
		return targetId1;
	}	

	/**
	 * @return the id of the target 2 term
	 */
	public int getTargetId2()
	{
		return targetId2;
	}
	
	/**
	 * Sets the value for the target 2 id to tId
	 * @param tId: id of the target 1 term
	 */
	public void setTargetId1(int tId)
	{
		this.targetId1 = tId;
	}
	
	/**
	 * Sets the value for the target 2 id to tId
	 * @param tId: id of the target 2 term
	 */
	public void setTargetId2(int tId)
	{
		this.targetId2 = tId;
	}
	
	/**
	 * Sets the target 1 and target 2 id and the similarity of the mapping to tId, tId2 and sim, respectively.
	 * @param tId: id of the target 1 term
	 * @param tId2: id of the target 2 term
	 * @param sim: value of similarity
	 */
	public void setValues(int tId, int tId2, double sim)
	{
		targetId1 = tId;
		targetId2 = tId2;
		similarity = sim;
	}
	
	/**
	 * Sets the relationship of the mapping to r
	 * @param r: the relationship between the mapped terms
	 */
	public void setRelationship(MappingRelation r)
	{
		rel = r;
	}
	
	/**
	 * Sets the similarity of the Mapping to sim
	 * @param sim: the similarity between the mapped terms
	 */
	public void setSimilarity(double sim)
	{
		similarity = Math.round(sim*10000)/10000.0;
	}

	@Override
	public String toString()
	{
		URIMap uris = AML.getInstance().getURIMap();
		return uris.getURI(sourceId) + " " + rel.toString() + " " +
				uris.getURI(targetId1) + " " + rel.toString()  + " " +
				uris.getURI(targetId2) + " " + similarity;
	}
}

