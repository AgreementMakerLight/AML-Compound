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
 * An alignment between three Ontologies, stored both as a list of Mappings and  *
 * as a Table of indexes, and including methods for input and output.          *
 *                                                                             *
 * @author Daniel Faria                                                        *
 * @date 12-09-2014                                                            *
 * @version 2.1                                                                *
 ******************************************************************************/
package aml.match;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import aml.AML;
import aml.match.CompoundMapping;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.settings.MappingRelation;
import aml.util.Table3Map;

public class CompoundAlignment implements Iterable<CompoundMapping>
{

	//Attributes

	//Term mappings organized in list
	private Vector<CompoundMapping> maps;
	//Term mappings organized by the source class (Source, Target 1 , Target 2)
	private Table3Map<Integer,Integer,Integer,CompoundMapping> sourceMaps;
	//Term mappings organized by the target 1 class (Target 1 , Source, Target 2)
	private Table3Map<Integer,Integer,Integer,CompoundMapping> targetMaps1;
	//Term mappings organized by the target 2 class (Target 2, Source, Target 1)
	private Table3Map<Integer,Integer,Integer,CompoundMapping> targetMaps2;
	private boolean internal;

	//Constructors

	/**
	 * Creates a new empty Alignment
	 */
	public CompoundAlignment()
	{
		maps = new Vector<CompoundMapping>(0,1);
		sourceMaps = new Table3Map<Integer,Integer,Integer,CompoundMapping>();
		targetMaps1 = new Table3Map<Integer,Integer,Integer,CompoundMapping>();
		targetMaps2 = new Table3Map<Integer,Integer,Integer,CompoundMapping>();
		internal = false;
	}

	/**
	 * Creates a new empty Alignment
	 * @return 
	 */
	public CompoundAlignment(boolean internal)
	{

		maps = new Vector<CompoundMapping>(0,1);
		sourceMaps = new Table3Map<Integer,Integer,Integer,CompoundMapping>();
		targetMaps1 = new Table3Map<Integer,Integer,Integer,CompoundMapping>();
		targetMaps2 = new Table3Map<Integer,Integer,Integer,CompoundMapping>();
		this.internal = internal;
	}

	/**
	 * Reads an Alignment from an input file
	 * @param file: the path to the input file
	 */
	public CompoundAlignment(String file) throws Exception
	{
		this();
		if(file.endsWith(".rdf"))
			loadMappingsRDF(file);
		else if(file.endsWith(".tsv"))
			loadMappingsTSV(file);
		else
			throw new Exception("Unrecognized alignment format!");
	}


	/**
	 * Creates a new Alignment that is a copy of the input alignment
	 * @param a: the Alignment to copy
	 */
	public CompoundAlignment(CompoundAlignment a)
	{
		this();
		addAll(a);
	}

	//Public Methods

	/**
	 * Adds a new Mapping to the alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param sourceId: the index of the source class to add to the alignment
	 * @param targetId1: the index of the target 1 class to add to the alignment
	 * @param targetId2: the index of the target 2 class to add to the alignment
	 * @param sim: the similarity between the classes
	 */
	public void add(int sourceId, int targetId1, int targetId2, double sim)
	{
		//Unless the alignment is internal, we can't have a mapping
		//between entities with the same id (which corresponds to URI)
		if(!internal && sourceId == targetId1 || sourceId == targetId2 || targetId1 == targetId2)
			return;
		//Construct the Mapping
		CompoundMapping m = new CompoundMapping(sourceId, targetId1, targetId2,
				sim, MappingRelation.EQUIVALENCE);

		//If it isn't listed yet, add it
		if(!sourceMaps.contains(sourceId,targetId1,targetId2))
		{
			maps.add(m);
			sourceMaps.add(sourceId, targetId1, targetId2, m);
			targetMaps1.add(targetId1, sourceId, targetId2, m);
			targetMaps2.add(targetId2,sourceId, targetId1, m);
		}
		//Otherwise update the similarity
		else
		{
			m = sourceMaps.get(sourceId,targetId1,targetId2);
			if(m.getSimilarity() < sim)
				m.setSimilarity(sim);
			if(!m.getRelationship().equals(MappingRelation.EQUIVALENCE))
				m.setRelationship(MappingRelation.EQUIVALENCE);		
		}
	}

	/**
	 * Adds a new Mapping to the alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param sourceId: the index of the source class to add to the alignment
	 * @param targetId1: the index of the target 1 class to add to the alignment
	 * @param targetId2: the index of the target 2 class to add to the alignment
	 * @param sim: the similarity between the classes
	 * @param r: the mapping relationship between the classes
	 */
	public void add(int sourceId, int targetId1, int targetId2, double sim, MappingRelation r)
	{

		//We can't have a mapping between entities with the same URI
		if(sourceId == targetId1 || sourceId == targetId2 || targetId1 == targetId2)
			return;

		//Construct the Mapping
		CompoundMapping m = new CompoundMapping(sourceId, targetId1, targetId2, sim, r);
		//If it isn't listed yet, add it

		if(sourceMaps == null || !sourceMaps.contains(sourceId,targetId1,targetId2))
		{

			maps.add(m);
			sourceMaps.add(sourceId, targetId1, targetId2, m);
			targetMaps1.add(targetId1, sourceId, targetId2, m);
			targetMaps2.add(targetId2, sourceId, targetId1, m);
		}
		//Otherwise update the similarity

		else
		{
			CompoundMapping map = sourceMaps.get(sourceId,targetId1, targetId2);

			if(map.getSimilarity() < sim)
				map.setSimilarity(sim);
			if(!map.getRelationship().equals(r))
				map.setRelationship(r);		
		}

	}

	/**
	 * Adds a clone of the given Mapping to the alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param m: the Mapping to add to the alignment
	 */
	public void add(CompoundMapping m)
	{
		add(m.getSourceId(), m.getTargetId1(), m.getTargetId2(),
				m.getSimilarity(), m.getRelationship());
	}

	/**
	 * Adds all Mappings in a to this Alignment
	 * @param a: the CompoundAlignment to add to this Alignment
	 */
	public void addAll(CompoundAlignment a)
	{
		addAll(a.maps);
	}

	/**
	 * Adds all CompoundMappings in the given list to this CompoundAlignment
	 * @param maps: the list of CompoundMappings to add to this CompoundAlignment
	 */
	public void addAll(List<CompoundMapping> maps)
	{
		for(CompoundMapping m : maps)
			add(m);
	}

	/**
	 * Adds all CompoundMappings in a to this CompoundAlignment as long as
	 * they don't conflict with any CompoundMapping in a
	 * @param a: the CompoundAlignment to add to this ACompoundlignment
	 */
	public void addAllNonConflicting(CompoundAlignment a)
	{
		Vector<CompoundMapping> nonConflicting = new Vector<CompoundMapping>();
		for(CompoundMapping m : a.maps)
			if(!this.containsConflict(m))
				nonConflicting.add(m);
		addAll(nonConflicting);
	}

	/**
	 * @return the average cardinality of this alignment
	 */
	public double cardinality()
	{
		double cardinality = 0.0;

		Set<Integer> sources = sourceMaps.keySet();
		for(Integer i : sources)
			cardinality += sourceMaps.keySet(i).size();

		Set<Integer> targets1 = targetMaps1.keySet();
		for(Integer i : targets1)
			cardinality += targetMaps1.keySet(i).size();

		Set<Integer> targets2 = targetMaps2.keySet();
		for(Integer i : targets2)
			cardinality += targetMaps2.keySet(i).size();

		cardinality /= sources.size() + targets1.size() + targets2.size();

		return cardinality;		
	}

	/**
	 * @param sourceId: the index of the source class to check in the alignment
	 * @param targetId1: the index of the target 1 class to add to the alignment
	 * @param targetId2: the index of the target 2 class to add to the alignment
	 * @return whether the alignment contains a CompoundMapping that is ancestral to the given pair of classes
	 * (i.e. includes one ancestor of sourceId and one ancestor of targetId)
	 */
	public boolean containsAncestralMapping(int sourceId, int targetId1, int targetId2)
	{
		AML aml = AML.getInstance();
		RelationshipMap rels = aml.getRelationshipMap();

		Set<Integer> sourceAncestors = rels.getAncestors(sourceId);
		Set<Integer> targetAncestors1 = rels.getAncestors(targetId1);
		Set<Integer> targetAncestors2 = rels.getAncestors(targetId2);

		for(Integer sa : sourceAncestors)
		{
			Set<Integer> over = getSourceMappings(sa);
			for(Integer ta1 : targetAncestors1)
				if(over.contains(ta1))
					return true;

			for(Integer ta2 : targetAncestors2)
				if(over.contains(ta2))
					return true;
		}
		return false;
	}

	/**
	 * @param m: the CompoundMapping to check in the alignment 
	 * @return whether the CompoundAlignment contains a CompoundMapping that conflicts with the given
	 * Mapping and has a higher similarity
	 */

	public boolean containsBetterMapping(CompoundMapping m)
	{
		int source = m.getSourceId();
		int target1 = m.getTargetId1();
		int target2 = m.getTargetId2();
		double sim = m.getSimilarity();

		if(containsSource(source))
		{
			Set<Integer> targets = sourceMaps.keySet(source);

			for(Integer i : targets)
			{
				for(Integer j : sourceMaps.keySet(source, i))
				{

					if(getSimilarity(source,i,j) > sim)
						return true;
				}

			}
		}
		if(containsTarget1(target1))
		{
			Set<Integer> sources = targetMaps1.keySet(target1);
			for(Integer i : sources)
			{
				for(Integer j : targetMaps1.keySet(target1, i))
				{
					if(getSimilarity(i,target1, j) > sim)
						return true;					
				}

			}
		}
		if(containsTarget2(target2))
		{
			Set<Integer> sources = targetMaps2.keySet(target2);
			for(Integer i : sources)
				for(Integer j : targetMaps2.keySet(target2, i))
				{
					if(getSimilarity(i,j, target2) > sim)
						return true;
				}
		}
		return false;
	}

	/**
	 * @param classId: the index of the class to check in the alignment 
	 * @return whether the CompoundAlignment contains a CompoundMapping with that class
	 * (either as a source or as a target class)
	 */
	public boolean containsClass(int classId)
	{
		return containsSource(classId) || containsTarget1(classId) || containsTarget2(classId);
	}

	/**
	 * @param sourceId: the index of the source class to check in the alignment
	 * @param targetId1: the index of the target 1 class to check in the alignment
	 * @param targetId2: the index of the target 2 class to check in the alignment
	 * @return whether the CompoundAlignment contains a CompoundMapping for sourceId or for targetId
	 */
	public boolean containsConflict(int sourceId, int targetId1, int targetId2)
	{
		return containsSource(sourceId) || containsTarget1(targetId1) || containsTarget2(targetId2);
	}

	/**
	 * @param m: the CompoundMapping to check in the alignment 
	 * @return whether the CompoundAlignment contains a CompoundMapping involving either class in m
	 */
	public boolean containsConflict(CompoundMapping m)
	{
		return containsConflict(m.getSourceId(),m.getTargetId1(), m.getTargetId2());
	}

	/**
	 * @param sourceId: the index of the source class to check in the alignment
	 * @param targetId1: the index of the target 1 class to check in the alignment
	 * @param targetId2: the index of the target 2 class to check in the alignment
	 * @return whether the alignment contains a CompoundMapping that is descendant of the given pair of classes
	 * (i.e. includes one descendant of sourceId and one descendant of targetId)
	 */
	public boolean containsDescendantMapping(int sourceId, int targetId1, int targetId2)
	{
		AML aml = AML.getInstance();
		RelationshipMap rels = aml.getRelationshipMap();

		Set<Integer> sourceDescendants = rels.getDescendants(sourceId);
		Set<Integer> targetDescendants1 = rels.getDescendants(targetId1);
		Set<Integer> targetDescendants2 = rels.getDescendants(targetId2);

		for(Integer sa : sourceDescendants)
		{
			Set<Integer> over = getSourceMappings(sa);
			for(Integer ta : targetDescendants1)
				if(over.contains(ta))
					return true;

			for(Integer ta2 : targetDescendants2)
				if(over.contains(ta2))
					return true;
		}
		return false;
	}

	/**
	 * @param sourceId: the index of the source class to check in the alignment
	 * @param targetId1: the index of the target 1 class to check in the alignment
	 * @param targetId2: the index of the target 2 class to check in the alignment
	 * @return whether the CompoundAlignment contains a CompoundMapping between sourceId and targetId
	 */
	public boolean containsMapping(int sourceId, int targetId1, int targetId2)
	{
		return sourceMaps.contains(sourceId, targetId1, targetId2);
	}

	/**
	 * @param m: the CompoundMapping to check in the alignment
	 * @return whether the CompoundAlignment contains a CompoundMapping equivalent to m
	 */
	public boolean containsMapping(CompoundMapping m)
	{
		return sourceMaps.contains(m.getSourceId(), m.getTargetId1(), m.getTargetId2());
	}

	/**
	 * @param lm: the List of CompoundMapping to check in the alignment
	 * @return whether the CompoundAlignment contains all the CompoundMapping listed in m
	 */
	public boolean containsMappings(List<CompoundMapping> lm)
	{
		for(CompoundMapping m: lm)
			if(!containsMapping(m))
				return false;
		return true;
	}

	/**
	 * @param sourceId: the index of the source class to check in the alignment
	 * @param targetId1: the index of the target 1 class to check in the alignment
	 * @param targetId2: the index of the target 2 class to check in the alignment
	 * @return whether the alignment contains a Mapping that is parent to the
	 * given pair of classes on one side only
	 */
	public boolean containsParentMapping(int sourceId, int targetId1, int targetId2)
	{
		AML aml = AML.getInstance();
		RelationshipMap rels = aml.getRelationshipMap();

		Set<Integer> sourceAncestors = rels.getParents(sourceId);
		Set<Integer> targetAncestors1 = rels.getParents(targetId1);
		Set<Integer> targetAncestors2 = rels.getParents(targetId2);

		for(Integer sa : sourceAncestors)
			if(containsMapping(sa,targetId1, targetId2))
				return true;
		for(Integer ta : targetAncestors1)
			if(containsMapping(sourceId,ta,targetId2))
				return true;
		for(Integer ta2 : targetAncestors2)
			if(containsMapping(sourceId,targetId1,ta2))
				return true;
		return false;
	}

	/**
	 * @param sourceId: the index of the source class to check in the alignment
	 * @return whether the Alignment contains a Mapping for sourceId
	 */
	public boolean containsSource(int sourceId)
	{
		return sourceMaps.contains(sourceId);
	}

	/**
	 * @param targetId1: the index of the target 1 class to check in the alignment
	 * @return whether the CompoundAlignment contains a CompoundMapping for targetId1
	 */
	public boolean containsTarget1(int targetId1)
	{
		return targetMaps1.contains(targetId1);
	}
	
	/**
	 * 
	 * @param targetId2: the index of the target 2 class to check in the alignment
	 * @return whether the CompoundAlignment contains a CompoundMapping for targetId2
	 */
	public boolean containsTarget2(int targetId2)
	{
		return targetMaps2.contains(targetId2);
	}
	


	/**
	 * @param a: the Alignment to subtract from this Alignment 
	 * @return the Alignment corresponding to the difference between this Alignment and a
	 */
	public CompoundAlignment difference(CompoundAlignment a)
	{
		CompoundAlignment diff = new CompoundAlignment();
		for(CompoundMapping m : maps)
			if(!a.containsMapping(m))
				diff.add(m);
		return diff;
	}

	/**
	 * @param ref: the reference Alignment to evaluate this Alignment
	 * @param forGUI: whether the evaluation is for display in the GUI
	 * or for output to the console
	 * @return the evaluation of this Alignment
	 */
	public String evaluate(CompoundAlignment ref,boolean forGUI)
	{
		int found = size();		
		int correct = 0;
		int total = 0;
		int conflict = 0;

		for(CompoundMapping m : maps)
		{

			if(ref.containsMapping(m))
			{
				if(ref.getRelationship(m.getSourceId(),m.getTargetId1(),m.getTargetId2()).
						equals(MappingRelation.UNKNOWN))
					conflict++;
				else

					correct++;
			}

		}
		for(CompoundMapping m : ref)
			if(!m.getRelationship().equals(MappingRelation.UNKNOWN))
				total++;

		double precision = 1.0*correct/(found-conflict);
		String prc = Math.round(precision*1000)/10.0 + "%";
		double recall = 1.0*correct/total;
		String rec = Math.round(recall*1000)/10.0 + "%";
		double fmeasure = 2*precision*recall/(precision+recall);
		String fms = Math.round(fmeasure*1000)/10.0 + "%";

		if(forGUI)
			return "Precision: " + prc + "; Recall: " + rec + "; F-measure: " + fms;
		else
			return "Precision\tRecall\tF-measure\tFound\tCorrect\tReference\n" + prc +
					"\t" + rec + "\t" + fms + "\t" + found + "\t" + correct + "\t" + total;
	}

	public Double[] evaluateNoPrint(CompoundAlignment ref)
	{
		double found = size();		
		double correct = 0;
		double total = 0;
		double conflict = 0;

		for(CompoundMapping m : maps)
		{
			if(ref.containsMapping(m))
			{

				if(ref.getRelationship(m.getSourceId(),m.getTargetId1(),m.getTargetId2()).
						equals(MappingRelation.UNKNOWN))
					conflict++;
				else

					correct++;
			}

		}
		for(CompoundMapping m : ref)
			if(!m.getRelationship().equals(MappingRelation.UNKNOWN))
				total++;

		double precision = 1.0*correct/(found-conflict);
		double recall = 1.0*correct/total;
		double fmeasure = 2*precision*recall/(precision+recall);

		return  new Double[] {precision, recall, fmeasure, found, correct, total,};
	}


	/**
	 * @param a: the base Alignment to which this Alignment will be compared 
	 * @return the gain (i.e. the fraction of new Mappings) of this Alignment
	 * in comparison with the base Alignment
	 */
	public double gain(CompoundAlignment a)
	{
		double gain = 0.0;
		for(CompoundMapping m : maps)
			if(!a.containsMapping(m))
				gain++;
		gain /= a.size();
		return gain;
	}

	/**
	 * @param a: the base Alignment to which this Alignment will be compared 
	 * @return the gain (i.e. the fraction of new Mappings) of this Alignment
	 * in comparison with the base Alignment
	 */
	/*
	public double gainOneToOne(CompoundAlignment a)
	{
		double sourceGain = 0.0;
		Set<Integer> sources = sourceMaps.keySet();
		for(Integer i : sources)
			if(!a.containsSource(i))
				sourceGain++;
		sourceGain /= a.sourceCount();

		double targetGain1 = 0.0;
		Set<Integer> targets1 = targetMaps1.keySet();
		for(Integer i : targets1)
			if(!a.containsTarget1(i))
				targetGain1++;

		double targetGain2 = 0.0;
		Set<Integer> targets2 = targetMaps2.keySet();
		for(Integer i : targets2)
			if(!a.containsTarget2(i)))
				targetGain2++;
		targetGain1 /= a.targetCount();
		return Math.min(sourceGain, targetGain);
	}
	 */
	/**
	 * @param index: the index of the Mapping to return in the list of Mappings
	 * @return the Mapping at the input index (note that the index will change
	 * during sorting) or null if the index falls outside the list
	 */
	public CompoundMapping get(int index)
	{
		if(index < 0 || index >= maps.size())
			return null;
		return maps.get(index);
	}

	/**
	 * @param sourceId: the index of the source class to check in the alignment
	 * @param targetId: the index of the target class to check in the alignment
	 * @return the Mapping between the source and target classes or null if no
	 * such Mapping exists
	 */
	public CompoundMapping get(int sourceId, int targetId1, int targetId2)
	{
		return sourceMaps.get(sourceId, targetId1, targetId2);
	}

	/**
	 * @param id1: the index of the first class to check in the alignment
	 * @param targetId: the index of the second class to check in the alignment
	 * @return the Mapping between the classes or null if no such Mapping exists
	 * in either direction
	 */
	public CompoundMapping getBidirectional(int id1, int id2, int id3)
	{
		if(sourceMaps.contains(id1, id2, id3))
			return sourceMaps.get(id1, id2, id3);
		else if(sourceMaps.contains(id2, id1, id3))
			return  sourceMaps.get(id2, id1, id3);
		else if(sourceMaps.contains(id3, id1, id2))
			return  sourceMaps.get(id3, id1, id2);
		else if(sourceMaps.contains(id2, id1, id3))
			return  sourceMaps.get(id2, id3, id1);
		else
			return null;
	}

	public CompoundMapping getBestSourceCompoundMatch(CompoundMapping m)
	{
		double max = 0;
		CompoundMapping target = new CompoundMapping(m.getSourceId());
		Set<Integer> targets1 = sourceMaps.keySet(m.getSourceId());
		for(Integer i : targets1)
		{
			Set<Integer> targets2 = sourceMaps.keySet(m.getSourceId() , i);
			for(Integer j : targets2)
			{
				double sim = getSimilarity(m.getSourceId(),i, j);

				if(sim > max)
				{
					max = sim;
					target.setTargetId1(i);
					target.setTargetId2(j);
					target.setSimilarity(max);
				}
			}

		}
		return target;
	}

	/**
	 * @param sourceId: the index of the source class
	 * @param targetId: the index of the target class
	 * @return the index of the Mapping between the given classes in
	 * the list of Mappings, or -1 if the Mapping doesn't exist
	 */
	public int getIndex(int sourceId, int targetId1, int targetId2)
	{
		if(sourceMaps.contains(sourceId, targetId1, targetId2))
			return maps.indexOf(sourceMaps.get(sourceId, targetId1, targetId2));
		else
			return -1;
	}

	/**
	 * @param id1: the index of the first class
	 * @param id2: the index of the second class
	 * @return the index of the Mapping between the given classes in
	 * the list of Mappings (in any order), or -1 if the Mapping doesn't exist
	 */
	/*
	public int getIndexBidirectional(int id1, int id2, int id3)
	{
		if(sourceMaps.contains(id1, id2, id3))
			return maps.indexOf(sourceMaps.get(id1, id2));
		else if(targetMaps.contains(id1, id2))
			return maps.indexOf(targetMaps.get(id1, id2));
		else
			return -1;
	}
	 */
	/**
	 * @param id: the index of the class to check in the alignment
	 * @return the list of all classes mapped to the given class
	 */
	public Set<Integer> getMappingsBidirectional(int id)
	{
		HashSet<Integer> mappings = new HashSet<Integer>();
		if(sourceMaps.contains(id))
			mappings.addAll(sourceMaps.keySet(id));
		if(targetMaps1.contains(id))
			mappings.addAll(targetMaps1.keySet(id));
		if(targetMaps2.contains(id))
			mappings.addAll(targetMaps2.keySet(id));
		return mappings;
	}

	/**
	 * @param sourceId: the index of the source class to check in the alignment
	 * @return the index of the target class that best matches source
	 */
	/*
	public double getMaxSourceSim(int sourceId)
	{
		double max = 0;
		Set<Integer> targets = sourceMaps.keySet(sourceId);
		for(Integer i : targets)
		{
			double sim = getSimilarity(sourceId,i);
			if(sim > max)
				max = sim;
		}
		return max;
	}
	 */
	/**
	 * @param targetId: the index of the target class to check in the alignment
	 * @return the index of the source class that best matches target
	 */
	/*
	public double getMaxTargetSim(int targetId)
	{
		double max = 0;
		Set<Integer> sources = targetMaps.keySet(targetId);
		for(Integer i : sources)
		{
			double sim = getSimilarity(i,targetId);
			if(sim > max)
				max = sim;
		}
		return max;
	}
	 */
	/**
	 * @param sourceId: the index of the source class in the alignment
	 * @param targetId: the index of the target class in the alignment
	 * @return the mapping relationship between source and target
	 */
	public MappingRelation getRelationship(int sourceId, int targetId1, int targetId2)
	{
		CompoundMapping m = sourceMaps.get(sourceId, targetId1, targetId2);
		if(m == null)
			return null;
		return m.getRelationship();

	}

	/**
	 * @param sourceId: the index of the source class in the alignment
	 * @param targetId: the index of the target class in the alignment
	 * @return the similarity between source and target
	 */
	public double getSimilarity(int sourceId, int targetId1, int targetId2)
	{
		CompoundMapping m = sourceMaps.get(sourceId, targetId1, targetId2);
		if(m == null)
			return 0.0;
		return m.getSimilarity();
	}

	/**
	 * @param sourceId: the index of the source class to check in the alignment
	 * @return the list of all target classes mapped to the source class
	 */
	public Set<Integer> getSourceMappings(int sourceId)
	{
		if(sourceMaps.contains(sourceId))
			return sourceMaps.keySet(sourceId);
		return new HashSet<Integer>();
	}

	/**
	 * @return the list of all source classes that have mappings
	 */
	public Set<Integer> getSources()
	{
		HashSet<Integer> sMaps = new HashSet<Integer>();
		sMaps.addAll(sourceMaps.keySet());
		return sMaps;
	}

	/**
	 * @param targetId: the index of the target class to check in the alignment
	 * @return the list of all source classes mapped to the target class
	 */
	public Set<Integer> getTarget1Mappings(int targetId1)
	{
		if(targetMaps1.contains(targetId1))
			return targetMaps1.keySet(targetId1);
		return new HashSet<Integer>();
	}

	/**
	 * @param targetId: the index of the target class to check in the alignment
	 * @return the list of all source classes mapped to the target class
	 */
	public Set<Integer> getTarget2Mappings(int targetId2)
	{
		if(targetMaps2.contains(targetId2))
			return targetMaps2.keySet(targetId2);
		return new HashSet<Integer>();
	}

	/**
	 * @return the list of all target classes that have mappings
	 */
	public Set<Integer> getTargets1()
	{
		HashSet<Integer> tMaps = new HashSet<Integer>();
		tMaps.addAll(targetMaps1.keySet());
		return tMaps;
	}

	/**
	 * @return the list of all target classes that have mappings
	 */
	public Set<Integer> getTargets2()
	{
		HashSet<Integer> tMaps = new HashSet<Integer>();
		tMaps.addAll(targetMaps2.keySet());
		return tMaps;
	}

	/**
	 * @param a: the Alignment to intersect with this Alignment 
	 * @return the Alignment corresponding to the intersection between this Alignment and a
	 */
	public CompoundAlignment intersection(CompoundAlignment a)
	{
		//Otherwise, compute the intersection
		CompoundAlignment intersection = new CompoundAlignment();
		for(CompoundMapping m : maps)
			if(a.containsMapping(m))
				intersection.add(m);
		return intersection;
	}

	@Override
	/**
	 * @return an Iterator over the list of class Mappings
	 */
	public Iterator<CompoundMapping> iterator()
	{
		return maps.iterator();
	}

	/**
	 * @return the maximum cardinality of this alignment
	 */
	public double maxCardinality()
	{
		double cardinality;
		double max = 0.0;

		Set<Integer> sources = sourceMaps.keySet();
		for(Integer i : sources)
		{
			cardinality = sourceMaps.keySet(i).size();
			if(cardinality > max)
				max = cardinality;
		}
		Set<Integer> targets1 = targetMaps1.keySet();
		for(Integer i : targets1)
		{
			cardinality = targetMaps1.keySet(i).size();
			if(cardinality > max)
				max = cardinality;
		}

		Set<Integer> targets2 = targetMaps2.keySet();
		for(Integer i : targets2)
		{
			cardinality = targetMaps2.keySet(i).size();
			if(cardinality > max)
				max = cardinality;
		}
		return max;		
	}

	/**
	 * Removes the given Mapping from the Alignment
	 * @param m: the Mapping to remove from the Alignment
	 */
	public void remove(CompoundMapping m)
	{
		int sourceId = m.getSourceId();
		int targetId1 = m.getTargetId1();
		int targetId2 = m.getTargetId2();
		sourceMaps.remove(sourceId, targetId1);
		targetMaps1.remove(targetId1, sourceId);
		targetMaps2.remove(targetId2, sourceId);

		maps.remove(m);
	}

	/**
	 * Removes the Mapping between the given classes from the Alignment
	 * @param sourceId: the source class to remove from the Alignment
	 * @param targetId: the target class to remove from the Alignment
	 */
	public void remove(int sourceId, int targetId1, int targetId2)
	{
		CompoundMapping m = new CompoundMapping(sourceId, targetId1, targetId2, 1.0);
		sourceMaps.remove(sourceId, targetId1);
		targetMaps1.remove(targetId1, sourceId);
		targetMaps2.remove(targetId2,sourceId);
		maps.remove(m);
	}

	/**
	 * Removes a list of Mappings from the alignment.
	 * @param maps: the list of Mappings to remove to this Alignment
	 */
	public void removeAll(List<CompoundMapping> maps)
	{
		for(CompoundMapping m : maps)
			remove(m);
	}

	/**
	 * Saves the alignment into an .rdf file in OAEI format
	 * @param file: the output file
	 */
	public void saveRDF(String file) throws FileNotFoundException
	{
		AML aml = AML.getInstance();
		String sourceURI = aml.getSource().getURI();
		String target1URI = aml.getTarget().getURI();
		String target2URI = aml.getTarget2().getURI();
		URIMap uris = aml.getURIMap();

		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("<?xml version='1.0' encoding='utf-8'?>");
		outStream.println("<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment'"); 
		outStream.println("\t xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' "); 
		outStream.println("\t xmlns:xsd='http://www.w3.org/2001/XMLSchema#' ");
		outStream.println("\t alignmentSource='AgreementMakerLight'>\n");
		outStream.println("<Alignment>");
		outStream.println("\t<xml>yes</xml>");
		outStream.println("\t<level>0</level>");
		double card = cardinality();
		if(card < 1.02)
			outStream.println("\t<type>11</type>");
		else
			outStream.println("\t<type>??</type>");
		outStream.println("\t<onto1>" + sourceURI + "</onto1>");
		outStream.println("\t<onto2>" + target1URI + "</onto2>");
		outStream.println("\t<onto3>" + target2URI + "</onto3>");
		outStream.println("\t<uri1>" + sourceURI + "</uri1>");
		outStream.println("\t<uri2>" + target1URI + "</uri2>");
		outStream.println("\t<uri3>" + target2URI + "</uri3>");
		for(CompoundMapping m : maps)
		{
			outStream.println("\t<map>");
			outStream.println("\t\t<Cell>");
			outStream.println("\t\t\t<entity1 rdf:resource=\""+uris.getURI(m.getSourceId())+"\"/>");
			outStream.println("\t\t\t<entity2 rdf:resource=\""+uris.getURI(m.getTargetId1())+"\"/>");
			outStream.println("\t\t\t<entity3 rdf:resource=\""+uris.getURI(m.getTargetId2())+"\"/>");
			outStream.println("\t\t\t<measure rdf:datatype=\"http://www.w3.org/2001/XMLSchema#float\">"+
					m.getSimilarity()+"</measure>");
			outStream.println("\t\t\t<relation>" + StringEscapeUtils.escapeXml(m.getRelationship().toString()) +
					"</relation>");
			outStream.println("\t\t</Cell>");
			outStream.println("\t</map>");
		}
		outStream.println("</Alignment>");
		outStream.println("</rdf:RDF>");		
		outStream.close();
	}

	/**
	 * Saves the alignment into a .tsv file in AML format
	 * @param file: the output file
	 */
	public void saveTSV(String file) throws FileNotFoundException
	{
		AML aml = AML.getInstance();
		Ontology source = aml.getSource();
		Ontology target1 = aml.getTarget();
		Ontology target2 = aml.getTarget2();
		URIMap uris = aml.getURIMap();

		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("#AgreementMakerLight Alignment File");
		outStream.println("#Source ontology:\t" + source.getURI());
		outStream.println("#First Target ontology:\t" + target1.getURI());
		outStream.println("#Second Target ontology:\t" + target2.getURI());
		outStream.println("Source URI\tSource Label\t"
				+ "Target 1 URI\tTarget 1 Label\t"
				+ "Target 2 URI\tTarget 2Label\tSimilarity\tRelationship");
		for(CompoundMapping m : maps)
		{
			outStream.println(uris.getURI(m.getSourceId()) + "\t" + source.getName(m.getSourceId()) +
					"\t" + uris.getURI(m.getTargetId1()) + "\t" + target1.getName(m.getTargetId1()) +
					"\t" + uris.getURI(m.getTargetId2()) + "\t" + target2.getName(m.getTargetId2()) +
					"\t" + m.getSimilarity() + "\t" + m.getRelationship().toString());
			
		}
			outStream.close();
	}
	/**
	 * Saves the alignment into a .tsv file in AML format. Also shows the ancestors for each class
	 * @param file: the output file
	 */
	public void saveTSV2(String file) throws FileNotFoundException
	{
		AML aml = AML.getInstance();
		RelationshipMap rels = aml.getRelationshipMap();
		Ontology source = aml.getSource();
		Ontology target1 = aml.getTarget();
		Ontology target2 = aml.getTarget2();
		URIMap uris = aml.getURIMap();

		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("#AgreementMakerLight Alignment File");
		outStream.println("#Source ontology:\t" + source.getURI());
		outStream.println("#First Target ontology:\t" + target1.getURI());
		outStream.println("#Second Target ontology:\t" + target2.getURI());
		outStream.println("Source URI\tSource Label\tSource Ancestors\t"
				+ "Target 1 URI\tTarget 1 Label\tTarget 1 Ancestors\t"
				+ "Target 2 URI\tTarget 2Label\tTarget 2 Ancestors\tSimilarity\tRelationship");
		for(CompoundMapping m : maps)
		{


			Set<Integer> srcAnc =  rels.getAncestors(m.getSourceId(), 1);
			Set<Integer> tgtAnc1 =  rels.getAncestors(m.getTargetId1(), 1);
			Set<Integer> tgtAnc2 =  rels.getAncestors(m.getTargetId2(), 1);
			String a1 = "";
			String a2 = "";
			String a3 = "";
			
			for(Integer s : srcAnc)
			{
				a1 += source.getName(s);
				if(!a1.equals(""))
					break;
			}
			
			for(Integer t : tgtAnc1)
			{
				a2 += target1.getName(t);
				if(!a2.equals(""))
					break;
			}
			for(Integer t2 : tgtAnc2)
			{
				a3 += target2.getName(t2);
				if(!a3.equals(""))
					break;
			}
			

			outStream.println(uris.getURI(m.getSourceId()) + "\t" + source.getName(m.getSourceId()) +
					"\t" + a1 + "\t" + uris.getURI(m.getTargetId1()) + "\t" + target1.getName(m.getTargetId1()) +
					"\t" + a2 + "\t" + uris.getURI(m.getTargetId2()) + "\t" + target2.getName(m.getTargetId2()) +
					"\t" + a3 + "\t" + m.getSimilarity() + "\t" + m.getRelationship().toString());
			
		}
			outStream.close();
	}
	
	public void saveTSV3(String file) throws FileNotFoundException
	{
		AML aml = AML.getInstance();
		Ontology source = aml.getSource();
		Ontology target1 = aml.getTarget();
		Ontology target2 = aml.getTarget2();
		URIMap uris = aml.getURIMap();

		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("#AgreementMakerLight Alignment File");
		outStream.println("#Source ontology:\t" + source.getURI());
		outStream.println("#First Target ontology:\t" + target1.getURI());
		outStream.println("#Second Target ontology:\t" + target2.getURI());
		outStream.println("Source URI\tSource Label\t"
				+ "Target 1 URI\tTarget 1 Label\t"
				+ "Target 2 URI\tTarget 2Label\tSimilarity\tRelationship");
		for(CompoundMapping m : maps)
		{
			outStream.println(uris.getURI(m.getSourceId()) + "\t" + source.getLexicon().getCorrectedName(m.getSourceId()) +
					"\t" + uris.getURI(m.getTargetId1()) + "\t" + target1.getLexicon().getCorrectedName(m.getTargetId1()) +
					"\t" + uris.getURI(m.getTargetId2()) + "\t" + target2.getLexicon().getCorrectedName(m.getTargetId2()) +
					"\t" + m.getSimilarity() + "\t" + m.getRelationship().toString());
			
		}
			outStream.close();
	}
	/**
	 * @return the number of Mappings in this Alignment
	 */
	public int size()
	{
		return maps.size();
	}

	/**
	 * Sorts the Alignment descendingly, by similarity
	 */

	public void sort()
	{
		Collections.sort(maps,new Comparator<CompoundMapping>()
				{
			public int compare(CompoundMapping m1, CompoundMapping m2)
			{
				double diff = m2.getSimilarity() - m1.getSimilarity();
				if(diff < 0)
					return -1;
				if(diff > 0)
					return 1;
				return 0;
			}
				} );
	}

	/**
	 * @return the number of source classes mapped in this Alignment
	 */
	public int sourceCount()
	{
		return sourceMaps.keyCount();
	}

	/**
	 * @return the fraction of source classes mapped in this Alignment
	 */
	public double sourceCoverage()
	{
		AML aml = AML.getInstance();
		double coverage = sourceMaps.keyCount();
		int count = aml.getSource().classCount();
		coverage /= count;
		return coverage;
	}

	/**
	 * @return the number of target classes mapped in this Alignment
	 */
	public int targetCount1()
	{
		return targetMaps1.keyCount();
	}

	public int targetCount2()
	{
		return targetMaps2.keyCount();
	}

	/**
	 * @return the fraction of target classes mapped in this Alignment
	 */
	public double[] targetCoverage()
	{
		AML aml = AML.getInstance();
		double coverage1 = targetMaps1.keyCount();
		int count1 = aml.getTarget().classCount();

		double coverage2 = targetMaps2.keyCount();
		int count2 = aml.getTarget2().classCount();

		coverage1 /= count1;
		coverage2 /= count2;
		return new double[] {coverage1, coverage2};
	}

	//Private Methods

	private void loadMappingsRDF(String file) throws DocumentException
	{
		AML aml = AML.getInstance();
		URIMap uris = aml.getURIMap();

		//Open the alignment file using SAXReader
		SAXReader reader = new SAXReader();

		File f = new File(file);

		Document doc = reader.read(f);
		//Read the root, then go to the "Alignment" element
		Element root = doc.getRootElement();

		Element align = root.element("Alignment");
		//Get an iterator over the mappings
		Iterator<?> map = align.elementIterator("map");

		while(map.hasNext())
		{

			//Get the "Cell" in each mapping
			Element e = ((Element)map.next()).element("Cell");
			if(e == null)
			{
				continue;
			}
			//Get the source class
			String sourceURI = e.element("entity1").element("Class").
					attributeValue("about");
			uris.addURI(sourceURI);
			//Get the target class

			//Get the both target classes
			List<Element> elements = e.element("entity2").element("Class").element("and").elements("Class");

			//Get the target 1
			String targetURI = elements.get(0).attributeValue("about");
			uris.addURI(targetURI);
			//Get the target 2
			String target2URI = elements.get(1).attributeValue("about");

			uris.addURI(target2URI);
			//Get the similarity measure
			String measure = e.elementText("measure");

			//Parse it, assuming 1 if a valid measure is not found
			double similarity = 1;
			if(measure != null)
			{
				try
				{
					similarity = Double.parseDouble(measure);
					if(similarity < 0 || similarity > 1)
						similarity = 1;
				}
				catch(Exception ex){/*Do nothing - use the default value*/};
			}


			//Get the relation
			String r = e.elementText("relation");
			if(r == null)
				r = "?";
			MappingRelation rel = MappingRelation.parseRelation(StringEscapeUtils.unescapeXml(r));
			//Check if the URIs are listed in the URI map 
			int sourceIndex = uris.getIndex(sourceURI);
			int targetIndex = uris.getIndex(targetURI);
			int targetIndex2 = uris.getIndex(target2URI);
			//If they are, add the mapping to the maps and proceed to next mapping

			if(sourceIndex > -1 && targetIndex > -1 && targetIndex2 > -1)
			{
				add(sourceIndex, targetIndex, targetIndex2, similarity, rel);
			}

		}
	}

	private void loadMappingsTSV(String file) throws Exception
	{
		AML aml = AML.getInstance();
		URIMap uris = aml.getURIMap();

		BufferedReader inStream = new BufferedReader(new FileReader(file));
		//First line contains the reference to AML
		inStream.readLine();
		//Second line contains the source ontology
		inStream.readLine();
		//Third line contains the target ontology
		inStream.readLine();
		//Fourth line contains the headers
		inStream.readLine();
		//And from the fifth line forward we have mappings
		String line;
		while((line = inStream.readLine()) != null)
		{
			String[] col = line.split("\t");
			//First column contains the source uri
			String sourceURI = col[0];
			//Third contains the target uri
			String targetURI = col[2];
			//Fifth contains the similarity
			String target2URI = col[4];
			String measure = col[6];
			//Parse it, assuming 1 if a valid measure is not found
			double similarity = 1;
			if(measure != null)
			{
				try
				{
					similarity = Double.parseDouble(measure);
					if(similarity < 0 || similarity > 1)
						similarity = 1;
				}
				catch(Exception ex){/*Do nothing - use the default value*/};
			}
			//Finally, sixth column contains the type of relation
			MappingRelation rel;
			if(col.length > 5)
				rel = MappingRelation.parseRelation(col[5]);
			//For compatibility with previous tsv format without listed relation
			else
				rel = MappingRelation.EQUIVALENCE;
			//Get the indexes
			int sourceIndex = uris.getIndex(sourceURI);
			int targetIndex = uris.getIndex(targetURI);
			int targetIndex2 = uris.getIndex(target2URI);
			//If they are, add the mapping to the maps and proceed to next mapping
			if(sourceIndex > -1 && targetIndex > -1 && targetIndex2 > -1)
			{
				add(sourceIndex, targetIndex, targetIndex2, similarity, rel);
			}
		}
		inStream.close();
	}

}
