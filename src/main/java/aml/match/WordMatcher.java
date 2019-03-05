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
 * Matches Ontologies by measuring the word similarity between their classes,  *
 * using a weighted Jaccard index.                                             *
 *                                                                             *
 * @originalauthor Daniel Faria                                                *
 * @author Daniela Oliveira                                                    *
 * @date 14-10-2015                                                            *
 * @version 1.1                                                              *
 ******************************************************************************/
package aml.match;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;
import java.util.HashMap;

import aml.AML;
import aml.match.CompoundAlignment;
import aml.ontology.Ontology;
import aml.ontology.WordLexicon;
import aml.util.Table2List;
import aml.util.Table2Map;

public class WordMatcher
{
	//Attributes
	private WordLexicon sourceLex;
	private WordLexicon targetLex;
	AML aml = AML.getInstance();

	//Constructors

	/**
	 * Constructs a new WordMatcher with default options
	 */
	public WordMatcher()
	{
		AML aml = AML.getInstance();
		sourceLex = aml.getSource().getWordLexicon();
		targetLex = aml.getTarget().getWordLexicon();
	}

	public WordMatcher(Ontology target)
	{
		AML aml = AML.getInstance();
		sourceLex = aml.getSource().getWordLexicon();
		targetLex = target.getWordLexicon();
	}

	//Public Methods

	public Alignment match(double thresh)
	{
		Alignment a = new Alignment();
		//We need to compute a preliminary
		//alignment and then apply the compound matching algorithms.
		double t = thresh * 0.5;

		//Global matching is done by chunks so as not to overload the memory
		//Match each chunk of both WordLexicons
		for(int i = 0; i < sourceLex.blockCount(); i++)
		{
			Table2List<String,Integer> sWLex = sourceLex.getWordTable(i);
			for(int j = 0; j < targetLex.blockCount(); j++)
			{
				Table2List<String,Integer> tWLex = targetLex.getWordTable(j);
				Vector<Mapping> temp = matchBlocks(sWLex,tWLex,t);
				for(Mapping m : temp)
				{
					//First compute the name similarity
					List<SubMapping> sMaps = targetNameSimilarity(m.getSourceId(),m.getTargetId());
					double nameSim = 0.0;

					for(SubMapping s : sMaps)
					{
						nameSim = s.getSimilarity();
						if(nameSim >= thresh)
							m.addSubMapping(s);
					}

					if(m.getSubMappings().size()>0)	{
						for(SubMapping sm : m.getSubMappings()){	
							a.add(sm.getSourceId(), sm.getTargetId(), sm.getSimilarity(), m.getSubMappings());
						}
					}
				}
			}
		}		
		return a;
	}

	/**
	 * Matches the source with the second target in a set of three but only matches 
	 * the words that did not have a match with the first target ontology.
	 * @param thresh: threshold
	 * @param map: HashMap with a Mapping as key and a List<String> which contains 
	 *  the words that didn't align in the first match
	 */
	public CompoundAlignment sequentialTargetMatch(double thresh, HashMap<Mapping,List<String>> map)
	{
		double nameSim = 0.0;
		AML aml = AML.getInstance();
		CompoundAlignment compAlign = new CompoundAlignment();
		Set<Integer> target2ids = aml.getTarget2().getClasses();

		for(Mapping m:map.keySet() )
		{
			for(Integer id: target2ids)
			{
				List<String> words = map.get(m);
				//First compute the name similarity
				nameSim = sequentialSimilarity(id,words);
				nameSim *= m.getWeight();
				double sim = nameSim;

				//Computes the average of this similarity with the 
				//similarity of the first matching step.
				double finalSim = (sim +m.getSimilarity())/2;
				if(finalSim >= thresh)
					compAlign.add(m.getSourceId(),m.getTargetId(),id,finalSim);
			}
		}
		return compAlign;
	}

	//Private

	/**
	 * Matches two WordLexicon blocks by class.
	 * Used by match() method either to compute the final BY_CLASS alignment
	 * or to compute a preliminary alignment which is then refined according
	 * to the WordMatchStrategy.
	 */
	private Vector<Mapping> matchBlocks(Table2List<String,Integer> sWLex,
			Table2List<String,Integer> tWLex, double thresh)
			{
		Table2Map<Integer,Integer,Double> maps = new Table2Map<Integer,Integer,Double>();
		//To minimize iterations, we want to iterate through the smallest Lexicon
		boolean sourceIsSmaller = (sWLex.keyCount() <= tWLex.keyCount());
		Set<String> words;
		if(sourceIsSmaller)
			words = sWLex.keySet();
		else
			words = tWLex.keySet();

		for(String s : words)
		{
			List<Integer> sourceIndexes = sWLex.get(s);
			List<Integer> targetIndexes = tWLex.get(s);
			if(sourceIndexes == null || targetIndexes == null)
				continue;
			double ec = sourceLex.getWordEC(s) * targetLex.getWordEC(s);
			for(Integer i : sourceIndexes)
			{
				double sim = ec * sourceLex.getWordWeight(s,i);
				for(Integer j : targetIndexes)
				{
					double finalSim = Math.sqrt(sim * targetLex.getWordWeight(s,j));
					Double previousSim = maps.get(i,j);
					if(previousSim == null)
						previousSim = 0.0;
					finalSim += previousSim;

					maps.add(i,j,finalSim);
				}
			}
		}
		Set<Integer> sources = maps.keySet();
		Vector<Mapping> a = new Vector<Mapping>();
		for(Integer i : sources)
		{
			Set<Integer> targets = maps.keySet(i);
			for(Integer j : targets)
			{
				double sim = maps.get(i,j);
				sim /= sourceLex.getClassEC(i) + targetLex.getClassEC(j) - sim;

				if(sim >= thresh)
					a.add(new Mapping(i, j, sim));
			}
		}
		return a;
	}

	private List<SubMapping> targetNameSimilarity(int sourceId, int targetId)
	{
		double nameSim = 0;
		double sim = 0;
		double weight;

		Set<String> sourceNames = sourceLex.getNames(sourceId);
		Set<String> targetNames = targetLex.getNames(targetId);
		List<SubMapping> subMappings = new ArrayList<SubMapping>();

		for(String s : sourceNames)
		{
			weight = sourceLex.getNameWeight(s,sourceId);
			for(String t : targetNames)
			{
				sim = weight * targetLex.getNameWeight(t, targetId);
				sim *= targetNameSimilarity(s,t);
				if(sim > nameSim)
				{
					nameSim = new Double(sim);
					subMappings.add(new SubMapping(sourceId, targetId, s, t, nameSim,sourceLex.getNameWeight(s,sourceId)));
				}
			}
		}	
		return subMappings;
	}

	/**
	 * Computes the word-based (bag-of-words) similarity between two names
	 */
	private double targetNameSimilarity(String s, String t)
	{
		List<String> sourceWords = sourceLex.getWordsList(s);
		List<String> targetWords = targetLex.getWordsList(t);
		List<String> aligned = new ArrayList<String>();

		double intersection = 0.0;
		double targetEC = targetLex.getNameEC(t);
		double union = targetEC;

		for(String w : sourceWords)
		{
			if(targetWords.contains(w) && !aligned.contains(w))
			{
				intersection += targetLex.getWordEC(w);
				aligned.add(w);
			}
		}
		return intersection/union;
	}

	private double sequentialSimilarity(int targetId, List<String> sourceWords)
	{
		double nameSim = 0;
		double sim = 0;

		Set<String> targetNames = targetLex.getNames(targetId);
		for(String t:targetNames)
		{
			sim = targetLex.getNameWeight(t, targetId);
			sim *= combinedSimilarity(sourceWords,t);
			if (sim>nameSim){
				nameSim=sim;
			}
		}
		return nameSim;
	}

	private double combinedSimilarity(List<String> sourceWords, String t)
	{
		Set<String> targetWords = targetLex.getWords(t);
		double intersection = 0.0;
		double result = 0.0;
		double union = 0.0;

		if(targetWords.size()>=sourceWords.size())
		{
			union = targetLex.getNameEC(t);
			for(String w : targetWords) 
			{
				if(sourceWords.contains(w))
					intersection += targetLex.getWordEC(w);
			}
			result = intersection/union;
		}
		else
		{
			for(String w : sourceWords) 
			{
				union += sourceLex.getWordEC(w);
				if(targetWords.contains(w))
					intersection += sourceLex.getWordEC(w);
			}
			result = intersection/union;

		}
		return result;
	}

	public static String printName(int id, String origin)
	{
		AML aml = AML.getInstance();
		String name = "";
		if(origin.equals("s"))
			name = aml.getSource().getLexicon().getCorrectedName(id);
		else if(origin.equals("t"))
			name = aml.getTarget().getLexicon().getCorrectedName(id);
		else
			name = aml.getTarget2().getLexicon().getCorrectedName(id);

		return name;
	}
}