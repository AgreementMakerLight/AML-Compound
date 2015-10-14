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
 * @date 14-10-2015                                                            *
 * @version 1.1                                                                  *
 ******************************************************************************/
package aml;

import aml.filter.CompoundRankedSelector;
import aml.match.CompoundAlignment;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.match.SubMapping;
import aml.match.WordMatcher;
import aml.settings.CompoundSelectionType;
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
		//Threshold for the first matching step
		double threshold = 0.4;
		//Threshold for the second matching step
		double threshold2 = 0.9;

		//Chose the selector to use.
		//STRICT for the strict ranked selector
		//PERMISSIVE for the permissive ranked selector
		//NONE to ignore the selection step
		CompoundSelectionType type = CompoundSelectionType.STRICT;
		
		//true to apply the Snowball stemmer to the Lexicon
		boolean stemmer = true;

		//Paths for the .owl files to align.
		String sourcePath = "store/ontologies/mp.owl";
		String targetPath1 = "store/ontologies/cl.owl";
		String targetPath2 = "store/ontologies/pato.owl";

		//Binary reference alignment .rdf file
		String referencePath1 = "store/ontologies/mp-cl-ref.rdf";

		//Compound reference alignment .rdf file
		String referencePath2 = "store/ontologies/mp-cl-pato-ref.rdf";
		
		//Output files
		String outputTSV = "store/compoundAlignment.tsv";
		String outputRDF = "store/compoundAlignment.rdf";
		
		System.out.println("Opening Ontologies...");
		aml.openOntologies(sourcePath, targetPath1, targetPath2,false,stemmer);
		
		long time = System.currentTimeMillis()/1000;
		System.out.println("Running first WordMatcher");
		WordMatcher wm1 = new WordMatcher();
		Alignment w1 = wm1.match(threshold);

		//Evaluation after the first WordMatcher
		if(!referencePath1.equals(""))
		{
			aml.openReferenceAlignment(referencePath1);
			aml.evaluate(w1);
			System.out.println(aml.getEvaluation());
		}

		//Creates an HashMap with each mapping and the correspondent words
		//left to align.
		HashMap<Mapping,List<String>> combMap = addSubMap(w1);

		System.out.println("Running second WordMatcher..");
		WordMatcher wm2 = new WordMatcher(aml.getTarget2());
		CompoundAlignment compAlign = wm2.sequentialTargetMatch(threshold2, combMap);
		aml.setCompoundAlignment(compAlign);

		CompoundAlignment compAlignFinal = new CompoundAlignment();

		CompoundRankedSelector selected = new CompoundRankedSelector(type);
		compAlignFinal = selected.select(compAlign);
		aml.setCompoundAlignment(compAlignFinal);

		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Ran for " + time + " seconds");

		if(!outputTSV.equals(""))
			aml.saveCompoundAlignmentTSV(outputTSV);
		if(!outputRDF.equals(""))	
			aml.saveCompoundAlignmentRDF(outputRDF);

		//Test evaluation after the second WordMatcher
		if(!referencePath2.equals(""))
		{
			aml.openCompoundReferenceAlignment(referencePath2);
			aml.evaluateC(compAlignFinal);
			System.out.println(aml.getCompoundEvaluation());
		}
		System.out.println("Finished.");
	}

	/**
	 * For each of the labels of the source class, removes the words already matched
	 * in the first matching step and saves the ones left to match in the second
	 * matching step.
	 * @param w1: alignment from the first matching step
	 * @return HashMap with each of the mappings and the corresponding list 
	 * of unmatched words.
	 */
	public static HashMap<Mapping,List<String>> addSubMap (Alignment w1)
	{
		Set<String> stopSet = StopList.read();
		
		//HashMap keeps the mapping as key and a list of unmatched words of the mapping's source.
		HashMap<Mapping,List<String>> combMap = new HashMap<Mapping, List<String>>();

		for(Mapping m : w1)
		{
			List<SubMapping> subMap = m.getSubMappings();
			for(SubMapping s : subMap)
			{
				int srcId = -1;
				int tgtId = -1;
				double sim = s.getSimilarity(); 
				double weight = s.getWeight();

				srcId = s.getSourceId();
				tgtId = s.getTargetId();

				List<String> sWords = new ArrayList<String>();				
				List<String> tWords = new ArrayList<String>();	
				
				String wordsSource = s.getLabelSource();
				String wordsTarget = s.getLabelTarget();

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
					if (!tWords.contains(word) && !stopSet.contains(word) && word.matches("^[a-zA-Z0-9]*$"))
					{
						word = word.replaceAll("[()]", "");
						newSet.add(word);
					}
				}

				HashMap<String,Integer> mapped = new HashMap<String,Integer>();
				for(String word:tWords)
				{
					if(!stopSet.contains(word))
					{
						if(!mapped.containsKey(word))
							mapped.put(word,1);
						else
							mapped.put(word,mapped.get(word)+1);
					}
				}

				//If the a mapping has repeated unmatched words, the unaligned duplicated
				//word is added to the set.
				for(String w:mapped.keySet())
				{
					if(mapped.get(w)>1)
						newSet.add(w);
				}
				combMap.put(new Mapping(srcId, tgtId, sim, weight),newSet);
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

