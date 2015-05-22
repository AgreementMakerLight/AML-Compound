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
 * @author Daniel Faria                                                        *
 * @date 22-08-2014                                                            *
 * @version 2.1                                                                *
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

import aml.settings.WordMatchStrategy;
import aml.settings.SimilarityStrategy;
import aml.util.Table2List;
import aml.util.Table2Map;

public class WordMatcher implements PrimaryMatcher, Rematcher
{

	//Attributes

	private WordLexicon sourceLex;
	private WordLexicon targetLex;
	private WordMatchStrategy strategy = WordMatchStrategy.AVERAGE;
	private String language;
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
		language = "";

	}

	public WordMatcher(Ontology target)
	{
		AML aml = AML.getInstance();
		sourceLex = aml.getSource().getWordLexicon();
		targetLex = target.getWordLexicon();
		language = "";
	}

	public WordMatcher(Ontology target, WordMatchStrategy s)
	{
		AML aml = AML.getInstance();
		strategy = s;
		sourceLex = aml.getSource().getWordLexicon();
		targetLex = target.getWordLexicon();
		language = "";
	}

	public WordMatcher(Ontology target, SimilarityStrategy s)
	{
		AML aml = AML.getInstance();
		sourceLex = aml.getSource().getWordLexicon();
		targetLex = target.getWordLexicon();
		language = "";
	}

	/**
	 * Constructs a new WordMatcher for the given language
	 * @param lang: the language on which to match Ontologies
	 */
	public WordMatcher(String lang)
	{
		AML aml = AML.getInstance();
		sourceLex = aml.getSource().getWordLexicon(lang);
		targetLex = aml.getTarget().getWordLexicon(lang);
		language = lang;
	}

	/**
	 * Constructs a new WordMatcher with the given strategy
	 * @param s: the WordMatchStrategy to use
	 */
	public WordMatcher(WordMatchStrategy s)
	{
		this();
		strategy = s;
	}

	/**
	 * Constructs a new WordMatcher for the given language
	 * @param lang: the language on which to match Ontologies
	 * @param s: the WordMatchStrategy to use
	 * @throws FileNotFoundException 
	 */
	public WordMatcher(String lang, WordMatchStrategy s)
	{
		this(lang);
		strategy = s;
	}

	//Public Methods
	/**
	 * Matches two ontologies with the nameSimilarity method.
	 * @param thresh: threshold
	 */
	@Override
	public Alignment match(double thresh)
	{
		System.out.println("Running Word Matcher");
		if(!language.isEmpty())
			System.out.println("Language: " + language);
		long time = System.currentTimeMillis()/1000;
		Alignment a = new Alignment();
		//If the strategy is BY_CLASS, the alignment can be computed
		//globally. Otherwise we need to compute a preliminary
		//alignment and then rematch according to the strategy.
		double t;
		if(strategy.equals(WordMatchStrategy.BY_CLASS))
			t = thresh;
		else
			t = thresh * 0.5;
		//Global matching is done by chunks so as not to overload the memory
		System.out.println("Blocks to match: " + sourceLex.blockCount() +
				"x" + targetLex.blockCount());
		//Match each chunk of both WordLexicons
		for(int i = 0; i < sourceLex.blockCount(); i++)
		{
			Table2List<String,Integer> sWLex = sourceLex.getWordTable(i);
			for(int j = 0; j < targetLex.blockCount(); j++)
			{
				Table2List<String,Integer> tWLex = targetLex.getWordTable(j);
				Vector<Mapping> temp = matchBlocks(sWLex,tWLex,t);
				//If the strategy is BY_CLASS, just add the alignment
				if(strategy.equals(WordMatchStrategy.BY_CLASS))
					a.addAll(temp);
				//Otherwise, update the similarity according to the strategy
				else
				{
					for(Mapping m : temp)
					{
						//First compute the name similarity
						double nameSim = nameSimilarity(m.getSourceId(),m.getTargetId());
						//Then update the final similarity according to the strategy
						double sim = m.getSimilarity();
						if(strategy.equals(WordMatchStrategy.BY_NAME))
						{
							sim = nameSim;
						}
						else if(strategy.equals(WordMatchStrategy.AVERAGE))
							sim = Math.sqrt(nameSim * sim);
						else if(strategy.equals(WordMatchStrategy.MAXIMUM))
							sim = Math.max(nameSim,sim);
						else if(strategy.equals(WordMatchStrategy.MINIMUM))
							sim = Math.min(nameSim,sim);

						if(sim >= thresh)
							a.add(m.getSourceId(),m.getTargetId(),sim);
					}
				}
				System.out.print(".");
			}

			System.out.println();
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}

	/**
	 * Matches two ontologies using the targetNameSimilaruty Method-
	 * @param thresh: threshold
	 * @return
	 */

	public Alignment targetMatch(double thresh)
	{
		if(!language.isEmpty())
			System.out.println("Language: " + language);

		Alignment a = new Alignment();
		//If the strategy is BY_CLASS, the alignment can be computed
		//globally. Otherwise we need to compute a preliminary
		//alignment and then rematch according to the strategy.
		double t;

		if(strategy.equals(WordMatchStrategy.BY_CLASS))
			t = thresh;
		else
			t = thresh * 0.5;

		//Global matching is done by chunks so as not to overload the memory
		//System.out.println("Blocks to match: " + sourceLex.blockCount() +
		//"x" + targetLex.blockCount());
		//Match each chunk of both WordLexicons
		for(int i = 0; i < sourceLex.blockCount(); i++)
		{
			Table2List<String,Integer> sWLex = sourceLex.getWordTable(i);
			for(int j = 0; j < targetLex.blockCount(); j++)
			{
				Table2List<String,Integer> tWLex = targetLex.getWordTable(j);
				Vector<Mapping> temp = matchBlocks(sWLex,tWLex,t);
				//If the strategy is BY_CLASS, just add the alignment
				if(strategy.equals(WordMatchStrategy.BY_CLASS))
					a.addAll(temp);
				//Otherwise, update the similarity according to the strategy
				else
				{
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
		}
		return a;
	}

	/**
	 * Matches two ontologies and if there is a previous match between the same source and a different
	 * target, only matches the words that didn't align in that match.
	 * @param thresh: threshold
	 * @param map: HashMap with an Integer key that correspondes to the source's id
	 * and a Set<String> as value that saves the words that didn't align in the first match
	 * @return
	 */
	public CompoundAlignment sequentialTargetMatch(double thresh, HashMap<Mapping,List<String>> map)
	{
		double nameSim = 0.0;
		AML aml = AML.getInstance();
		//System.out.println("Running Word Matcher");
		if(!language.isEmpty())
			System.out.println("Language: " + language);
		long time = System.currentTimeMillis()/1000;
		CompoundAlignment compAlign = new CompoundAlignment();

		for(Mapping m:map.keySet() )
		{
			Set<Integer> target2ids = aml.getTarget2().getClasses();
			for(Integer id: target2ids)
			{
				//First compute the name similarity
				List<String> words = map.get(m);

				nameSim = sequentialSimilarity(id,words);
				nameSim *= m.getWeight();
				double sim = nameSim;


				double finalSim = (sim +m.getSimilarity())/2;

				if(finalSim >= thresh)
				{
					compAlign.add(m.getSourceId(),m.getTargetId(),id,finalSim);
				}
			}
		}


		time = System.currentTimeMillis()/1000 - time;
		return compAlign;
	}

	public Alignment match(double thresh, List<Integer> lista)
	{
		System.out.println("Running Word Matcher");
		if(!language.isEmpty())
			System.out.println("Language: " + language);
		long time = System.currentTimeMillis()/1000;
		Alignment a = new Alignment();
		//If the strategy is BY_CLASS, the alignment can be computed
		//globally. Otherwise we need to compute a preliminary
		//alignment and then rematch according to the strategy.
		double t;
		if(strategy.equals(WordMatchStrategy.BY_CLASS))
			t = thresh;
		else
			t = thresh * 0.5;
		//Global matching is done by chunks so as not to overload the memory
		System.out.println("Blocks to match: " + sourceLex.blockCount() +
				"x" + targetLex.blockCount());
		//Match each chunk of both WordLexicons
		for(int i = 0; i < sourceLex.blockCount(); i++)
		{	
			Table2List<String,Integer> sWLex = sourceLex.getWordTable(i);
			/*
			for (String name: sWLex.keySet())
			{

	            String key = name.toString();
	            String value = sWLex.get(name).toString();   


			} 
			 */

			for(int j = 0; j < targetLex.blockCount(); j++)
			{
				Table2List<String,Integer> tWLex = targetLex.getWordTable(j);
				Vector<Mapping> temp = matchBlocks(sWLex,tWLex,t);
				//If the strategy is BY_CLASS, just add the alignment
				if(strategy.equals(WordMatchStrategy.BY_CLASS))
					a.addAll(temp);
				//Otherwise, update the similarity according to the strategy
				else
				{
					for(Mapping m : temp)
					{
						if (lista.contains(m.getSourceId()))
						{
							//First compute the name similarity
							double nameSim = nameSimilarity(m.getSourceId(),m.getTargetId());
							//Then update the final similarity according to the strategy
							double sim = m.getSimilarity();
							if(strategy.equals(WordMatchStrategy.BY_NAME))
								sim = nameSim;
							else if(strategy.equals(WordMatchStrategy.AVERAGE))
								sim = Math.sqrt(nameSim * sim);
							else if(strategy.equals(WordMatchStrategy.MAXIMUM))
								sim = Math.max(nameSim,sim);
							else if(strategy.equals(WordMatchStrategy.MINIMUM))
								sim = Math.min(nameSim,sim);
							if(sim >= thresh)
								a.add(m.getSourceId(),m.getTargetId(),sim);
						}

					}
				}
				System.out.print(".");
			}
			System.out.println();
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}

	@Override
	public Alignment rematch(Alignment a)
	{
		System.out.println("Computing Word Similarity");
		long time = System.currentTimeMillis()/1000;
		Alignment maps = new Alignment();
		for(Mapping m : a)
			maps.add(mapTwoClasses(m.getSourceId(),m.getTargetId()));
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}

	//Private

	//Computes the word-based (bag-of-words) similarity between two
	//classes, for use by rematch()
	private double classSimilarity(int sourceId, int targetId)
	{
		Set<String> sourceWords = sourceLex.getWords(sourceId);
		Set<String> targetWords = targetLex.getWords(targetId);
		double intersection = 0.0;
		double union = sourceLex.getClassEC(sourceId) + 
				targetLex.getClassEC(targetId);
		for(String w : sourceWords)
		{
			double weight = sourceLex.getWordEC(w) * sourceLex.getWordWeight(w,sourceId);
			if(targetWords.contains(w))
				intersection += Math.sqrt(weight * targetLex.getWordEC(w) *
						targetLex.getWordWeight(w,targetId));
		}			
		union -= intersection;
		return intersection / union;
	}

	//Matches two WordLexicon blocks by class.
	//Used by match() method either to compute the final BY_CLASS alignment
	//or to compute a preliminary alignment which is then refined according
	//to the WordMatchStrategy.
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

	//Maps two classes according to the selected strategy.
	//Used by rematch() only.
	private Mapping mapTwoClasses(int sourceId, int targetId)
	{
		//If the strategy is not by name, compute the class similarity
		double classSim = 0.0;
		if(!strategy.equals(WordMatchStrategy.BY_NAME))
		{
			classSim = classSimilarity(sourceId,targetId);
			//If the class similarity is very low, return the mapping
			//so as not to waste time computing name similarity
			if(classSim < 0.25)
				return new Mapping(sourceId,targetId,classSim);
		}
		//If the strategy is not by class, compute the name similarity
		double nameSim = 0.0;
		if(!strategy.equals(WordMatchStrategy.BY_CLASS))
			nameSim = nameSimilarity(sourceId,targetId);

		//Combine the similarities according to the strategy
		double sim = 0.0;
		if(strategy.equals(WordMatchStrategy.BY_NAME))
			sim = nameSim;
		else if(strategy.equals(WordMatchStrategy.BY_CLASS))
			sim = classSim;
		else if(strategy.equals(WordMatchStrategy.AVERAGE))
			sim = Math.sqrt(nameSim * classSim);
		else if(strategy.equals(WordMatchStrategy.MAXIMUM))
			sim = Math.max(nameSim,classSim);
		else if(strategy.equals(WordMatchStrategy.MINIMUM))
			sim = Math.min(nameSim,classSim);
		//Return the mapping with the combined similarity
		return new Mapping(sourceId,targetId,sim);
	}

	//Computes the maximum word-based (bag-of-words) similarity between
	//two classes' names, for use by both match() and rematch()
	private double nameSimilarity(int sourceId, int targetId)
	{
		double nameSim = 0;
		double sim, weight;
		Set<String> sourceNames = sourceLex.getNames(sourceId);
		Set<String> targetNames = targetLex.getNames(targetId);
		for(String s : sourceNames)
		{
			weight = sourceLex.getNameWeight(s,sourceId);
			for(String t : targetNames)
			{
				sim = weight * targetLex.getNameWeight(t, targetId);
				sim *= nameSimilarity(s,t);
				if(sim > nameSim)
					nameSim = sim;
			}
		}
		return nameSim;
	}

	//Computes the word-based (bag-of-words) similarity between two names
	private double nameSimilarity(String s, String t)
	{
		Set<String> sourceWords = sourceLex.getWords(s);
		Set<String> targetWords = targetLex.getWords(t);
		double intersection = 0.0;
		double union = sourceLex.getNameEC(s) + targetLex.getNameEC(t);
		for(String w : sourceWords)
			if(targetWords.contains(w))
			{
				intersection += Math.sqrt(sourceLex.getWordEC(w) * targetLex.getWordEC(w));
			}

		union -= intersection;
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

			if (sim>nameSim)
				nameSim=sim;
		}


		return nameSim;
	}

	//Computes the word-based (bag-of-words) similarity between two names
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
	//Computes the word-based (bag-of-words) similarity between two names
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
}
