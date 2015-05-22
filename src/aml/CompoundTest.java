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
 * Test-runs CompoundAgreementMakerLight in Eclipse.                           *
 *                                                                             *
 * @originalauthor Daniel Faria                                                *
 * @author Daniela Oliveira                                                    *
 * @date 21-05-2015                                                            *
 * @version 1                                                                  *
 ******************************************************************************/
package aml;

import aml.filter.CompoundRankedSelector;
import aml.match.CompoundAlignment;
import aml.match.CompoundMapping;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.match.SubMapping;
import aml.match.WordMatcher;
import aml.ontology.Lexicon;
import aml.ontology.URIMap;
import aml.settings.SelectionType;
import aml.util.StopList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class CompoundTest 
{
	static AML aml = AML.getInstance();

	public static void main(String[] args) throws Exception
	{
		double threshold = 0.5;
		double threshold2 = 0.9;

		//true for performing a permissive ranked selection. false for performing a greedy selection step.
		boolean ranked = false;
		
		//true to open the ontologies with transitive closure.
		boolean transitive = false;

		//Paths for the .owl files to align. targetPath2 should be the qualifier ontology.
		String sourcePath = "store/ontologies/mp.owl";
		String targetPath1 = "store/ontologies/cl.owl";
		String targetPath2 = "store/ontologies/pato.owl";

		//referencePath1 is used for the evaluation of the first step. It must be a 1:1 alignment.
		String referencePath1 = "store/ontologies/mp-cl-ref.rdf";
		
		//referencePath2 is used for the evaluation of the second step. It must be a compound alignment.
		String referencePath2 = "store/ontologies/mp-cl-pato-ref.rdf";

		long time = System.currentTimeMillis()/1000;

		String outputTSV = "store/compoundAlign.tsv";
		String outputRDF = "store/compoundAlign.rdf";

		System.out.println("Opening Ontologies...");
		if(transitive)
			aml.openOntologies(sourcePath, targetPath1, targetPath2,true);
		else
			aml.openOntologies(sourcePath, targetPath1, targetPath2,false);

		System.out.println("Running first WordMatcher");
		WordMatcher wm1 = new WordMatcher();
		Alignment w1 = wm1.targetMatch(threshold);
		
		//Test evaluation after the first WordMatcher
		if(!referencePath1.equals(""))
		{
			aml.openReferenceAlignment(referencePath1);
			aml.evaluate(w1);
			System.out.println(aml.getEvaluation().replaceAll("%", ""));
		}

		
		HashMap<Mapping,List<String>> combMap = addSubMap(w1);

		System.out.println("Running second WordMatcher..");
		WordMatcher wm2 = new WordMatcher(aml.getTarget2());
		CompoundAlignment compAlign = wm2.sequentialTargetMatch(threshold2, combMap);
		aml.setCompoundAlignment(compAlign);

		CompoundAlignment compAlignFinal = new CompoundAlignment();

		if(ranked)
		{
			CompoundRankedSelector select = new CompoundRankedSelector(SelectionType.PERMISSIVE);
			compAlignFinal = select.select(compAlign, threshold2);
		}
		else
		{
			compAlignFinal = new CompoundAlignment();
			HashMap<Integer, String> rec = aml.getTarget2().getReciprocalClasses();
			URIMap uris = aml.getURIMap();
			for(CompoundMapping m : compAlign)
			{

				CompoundMapping best = compAlign.getBestSourceCompoundMatch(m);
				if(rec.containsKey(best.getTargetId2()))
				{
					best.setTargetId2(uris.getIndex(rec.get(best.getTargetId2())));
				}
				compAlignFinal.add(best.getSourceId(), best.getTargetId1(), best.getTargetId2(), best.getSimilarity());
			}
		}

		aml.setCompoundAlignment(compAlignFinal);

		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Ran in " + time + " seconds");

		if(!outputTSV.equals(""))
			aml.saveCompoundAlignmentTSV(outputTSV);
		if(!outputRDF.equals(""))	
			aml.saveCompoundAlignmentRDF(outputRDF);

		//Test evaluation after the second WordMatcher
		if(!referencePath2.equals(""))
		{
			aml.openCompoundReferenceAlignment(referencePath2);
			aml.evaluateC(compAlignFinal);
			System.out.println(aml.getCompoundEvaluation().replaceAll("%", ""));
		}
		System.out.println("Finished.");
	}
	
	/**
	 * 
	 * @param w1: 1:1 alignment
	 * @return HashMap with each of the mappings and the corresponding list of unmatched words.
	 */
	public static HashMap<Mapping,List<String>> addSubMap (Alignment w1)
	{
		Set<String> stopSet = StopList.read();
		
		//HashMap keeps the mapping as key and a list of unmatched words of the mapping's source.
		HashMap<Mapping,List<String>> combMap = new HashMap<Mapping, List<String>>();
		
		Lexicon sLex = aml.getSource().getLexicon();
		Lexicon tLex = aml.getTarget().getLexicon();

		for(Mapping m : w1)
		{
			List<SubMapping> subMap = m.getSubMappings();
			for(SubMapping s : subMap)
			{

				int srcId = -1;
				int tgtId = -1;
				double sim = s.getSimilarity(); 
				double weight = s.getWeight();
				for(int i : sLex.getClasses(s.getLabelSource()))
				{
					srcId = i;

					for(int j : tLex.getClasses(s.getLabelTarget()))
					{
						tgtId = j;

						String wordsSource = s.getLabelSource();
						String wordsTarget = s.getLabelTarget();
						List<String> sWords = new ArrayList<String>();				
						List<String> tWords = new ArrayList<String>();	
						for(String w:wordsSource.split(" "))
							sWords.add(w);
						for(String w:wordsTarget.split(" "))
							tWords.add(w);
						List<String> newSet = new ArrayList<String>();
						HashMap<String, Integer> aligned = new HashMap<String, Integer>();
						for(String word:sWords)
						{
							if(!aligned.containsKey(word))
								aligned.put(word,1);
							else
								aligned.put(word,aligned.get(word)+1);
							if ((!tWords.contains(word)) && !stopSet.contains(word) && word.matches("^[a-zA-Z0-9]*$"))
							{
								word = word.replaceAll("[()]", "");
								newSet.add(word);
							}
						}
						
						HashMap<String,Integer> mapped = new HashMap<String,Integer>();
						for(String word:tWords)
						{
							if(!mapped.containsKey(word))
								mapped.put(word,1);
							else
								mapped.put(word,mapped.get(word)+1);
						}
						
						//If the a mapping as repeated unmatched words, a 0.1 weight multiplied to the original weight.
						for(String w:mapped.keySet())
						{
							if(mapped.get(w)>1)
								weight *= 0.1;
						}
						combMap.put(new Mapping(srcId, tgtId, sim, weight),newSet);


					}
				}
			}
		}
		return combMap;
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

