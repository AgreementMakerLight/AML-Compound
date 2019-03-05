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
 * Stores all system information for common access by all AgreementMakerLight  *
 * classes. This is a singleton java pattern, so there can be only one         *
 * instance of this class in the whole application. All classes can access it  *
 * without reference, by invoking the static method AML.getInstance()          *
 *                                                                             *
 * @originalauthor Daniel Faria                                                *
 * @author Daniela Oliveira                                                    *
 * @date 14-10-2015                                                            *
 * @version 1.1                                                                *
 ******************************************************************************/
package aml;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.PropertyConfigurator;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import aml.match.Alignment;
import aml.match.CompoundAlignment;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.settings.LanguageSetting;
import aml.settings.MatchStep;
import aml.settings.NeighborSimilarityStrategy;
import aml.settings.SelectionType;
import aml.settings.SizeCategory;
import aml.settings.StringSimMeasure;
import aml.settings.WordMatchStrategy;
import aml.translate.Dictionary;
import aml.util.ExtensionFilter;

public class AML
{

	//Attributes

	//Singleton pattern: unique instance
	private static AML aml = new AML();
	//The ontology and alignment data structures
	private URIMap uris;
	private RelationshipMap rels;
	private Ontology source;
	private Ontology target;
	private Ontology target2;
	private Ontology bk;
	private Alignment a;
	private CompoundAlignment aC;
	private Alignment ref;
	private CompoundAlignment refC;
	private CompoundAlignment refC2;
	private String evaluation;
	private String evaluationC;
	//General matching settings
	private boolean useReasoner = true;
	private final String BK_PATH = "store/knowledge/";
	private Vector<String> bkSources;
	private LanguageSetting lang;
	private String language;
	private SizeCategory size;
	private Set<String> languages;
	private SelectionType sType;
	//Manual matching settings
	private double threshold = 0.6;
	private Vector<MatchStep> selectedSteps;
	private Vector<String> selectedSources;
	private WordMatchStrategy wms;
	private StringSimMeasure ssm;
	private boolean primaryStringMatcher;
	private NeighborSimilarityStrategy nss;
	private boolean directNeighbors;
	private boolean removeObsolete;
	private boolean structuralSelection;    

	//Constructors

	//It's private so that no other instances can be created 
	private AML()
	{
		uris = new URIMap();
		rels = new RelationshipMap();
		bkSources = new Vector<String>();		
	}

	//Public Methods

	public void closeAlignment()
	{
		a = null;
	}

	public void closeOntologies()
	{
		source = null;
		target = null;
		uris = null;
		rels = null;
		a = null;
	}

	public void defaultConfig()
	{
		File ontRoot = new File(BK_PATH);
		if(ontRoot.exists())
		{
			FileFilter ont = new ExtensionFilter("Ontology Files (*.owl, *.rdf, *.rdfs, *.xml)",
					new String[] { ".owl", ".rdf", ".rdfs", ".xml" }, false);
			File[] ontFiles = ontRoot.listFiles(ont);
			for(File f : ontFiles)
				bkSources.add(f.getName());
			FileFilter lex = new ExtensionFilter("Lexicon Files (*.lexicon)",
					new String[] { ".lexicon" }, false);
			File[] lexFiles = ontRoot.listFiles(lex);
			for(File f : lexFiles)
				bkSources.add(f.getName());
			bkSources.add("WordNet");
		}
		/*else
		{
			System.out.println("WARNING: 'store/knowledge' directory not found!");
		}*/
		selectedSources = new Vector<String>(bkSources);

		size = SizeCategory.getSizeCategory();
		lang = LanguageSetting.getLanguageSetting();
		languages = new HashSet<String>();
		for(String s : source.getLexicon().getLanguages())
			if(target.getLexicon().getLanguages().contains(s))
				languages.add(s);
		if(languages.contains("en") || languages.size() == 0)
			language = "en";
		else
			language = languages.iterator().next();
		selectedSteps = new Vector<MatchStep>();
		if(lang.equals(LanguageSetting.TRANSLATE))
			selectedSteps.add(MatchStep.TRANSLATE);
		selectedSteps.add(MatchStep.BK);
		if(!size.equals(SizeCategory.HUGE))
			selectedSteps.add(MatchStep.WORD);
		selectedSteps.add(MatchStep.STRING);
		if(size.equals(SizeCategory.SMALL) || size.equals(SizeCategory.MEDIUM))
			selectedSteps.add(MatchStep.STRUCT);
		double sourceRatio = source.propertyCount() * 1.0 / source.classCount();
		double targetRatio = target.propertyCount() * 1.0 / target.classCount();
		if(sourceRatio >= 0.05 && targetRatio >= 0.05)
			selectedSteps.add(MatchStep.PROPERTY);
		selectedSteps.add(MatchStep.SELECT);
		selectedSteps.add(MatchStep.REPAIR);
		wms = WordMatchStrategy.AVERAGE;
		ssm = StringSimMeasure.ISUB;
		primaryStringMatcher = size.equals(SizeCategory.SMALL);
		nss = NeighborSimilarityStrategy.MINIMUM;
		directNeighbors = false;
		sType = SelectionType.getSelectionType();
		removeObsolete = size.equals(SizeCategory.HUGE);
		structuralSelection = size.equals(SizeCategory.HUGE);		
	}

	public boolean directNeighbors()
	{
		return directNeighbors;
	}

	public String evaluate()
	{
		evaluation = a.evaluate(ref);
		return evaluation;
	}
	public String evaluate(Alignment align)
	{
		a = align;
		evaluation = a.evaluate(ref);
		return evaluation;
	}

	public String evaluateC(CompoundAlignment a)
	{
		aC = a;
		evaluationC = aC.evaluate(refC);
		return evaluationC;
	}	



	public Alignment getAlignment()
	{
		return a;
	}

	public CompoundAlignment getCompoundAlignment()
	{
		return aC;
	}

	public Ontology getBKOntology()
	{
		return bk;
	}

	public Vector<String> getBKSources()
	{
		return bkSources;
	}

	public String getEvaluation()
	{
		return evaluation;
	}

	public String getCompoundEvaluation()
	{
		return evaluationC;
	}

	public static AML getInstance()
	{
		return aml;
	}

	public String getLabelLanguage()
	{
		return language;
	}

	public Set<String> getLanguages()
	{
		return languages;
	}

	public LanguageSetting getLanguageSetting()
	{
		return lang;
	}

	public NeighborSimilarityStrategy getNeighborSimilarityStrategy()
	{
		return nss;
	}

	public Alignment getReferenceAlignment()
	{
		return ref;
	}

	public CompoundAlignment getCompoundReferenceAlignment()
	{
		return refC;
	}

	public CompoundAlignment getCompoundReferenceAlignment2()
	{
		return refC2;
	}

	public RelationshipMap getRelationshipMap()
	{
		return rels;
	}

	public Vector<String> getSelectedBKSources()
	{
		return selectedSources;
	}

	public Vector<MatchStep> getSelectedSteps()
	{
		return selectedSteps;
	}

	public SelectionType getSelectionType()
	{
		return sType;
	}

	public SizeCategory getSizeCategory()
	{
		return size;
	}

	public Ontology getSource()
	{
		return source;
	}

	public StringSimMeasure getStringSimMeasure()
	{
		return ssm;
	}

	public Ontology getTarget()
	{
		return target;
	}

	public Ontology getTarget2()
	{
		return target2;
	}

	public double getThreshold()
	{
		return threshold;
	}

	public URIMap getURIMap()
	{
		return uris;
	}

	public WordMatchStrategy getWordMatchStrategy()
	{
		return wms;
	}

	public boolean hasAlignment()
	{
		return a != null && a.size() > 0;
	}

	public boolean hasOntologies()
	{
		return source != null && target != null;
	}

	public void openAlignment(String path) throws Exception
	{
		a = new Alignment(path);
		evaluation = null;
	}

	public void openCompoundAlignment(String path) throws Exception
	{
		uris = new URIMap();
		rels = new RelationshipMap();	
		aC = new CompoundAlignment(path);

		evaluation = null;
	}
	public void setCompoundAlignment(CompoundAlignment a)
	{
		aC = a;
	}

	public void openBKOntology(String name) throws OWLOntologyCreationException
	{
		long time = System.currentTimeMillis()/1000;
		bk = new Ontology(BK_PATH + name,false);
		String refName = name.substring(0,name.lastIndexOf(".")) + ".xrefs";
		File f = new File(BK_PATH + refName);
		if(f.exists())
			bk.getReferenceMap().extend(BK_PATH + refName);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(bk.getURI() + " loaded in " + time + " seconds");
	}

	public void openOntologies(String src, String tgt) throws OWLOntologyCreationException
	{
		//Initialize the URIMap and RelationshipMap
		uris = new URIMap();
		rels = new RelationshipMap();
		if(useReasoner)
			PropertyConfigurator.configure("log4j.properties");
		long time = System.currentTimeMillis()/1000;
		System.out.println("Loading source ontology");	
		source = new Ontology(src,true);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(source.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + source.classCount());	
		System.out.println("Names: " + source.getLexicon().size());
		System.out.println("Properties: " + source.propertyCount());
		time = System.currentTimeMillis()/1000;

		System.out.println("Loading first target ontology");
		target = new Ontology(tgt,true);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(target.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + target.classCount());
		System.out.println("Names: " + target.getLexicon().size());
		System.out.println("Properties: " + target.propertyCount());
		System.out.println("Direct Relationships: " + rels.relationshipCount());
		time = System.currentTimeMillis()/1000;

		System.out.println("Running transitive closure on RelationshipMap");
		rels.transitiveClosure();
		//time = System.currentTimeMillis()/1000 - time;
		//System.out.println("Transitive closure finished in " + time + " seconds");	
		//System.out.println("Extended Relationships: " + rels.relationshipCount());
		//System.out.println("Disjoints: " + rels.disjointCount());
		//Reset the alignment, mapping, and evaluation
		a = null;
		aC = null;

		evaluation = null;
		//Refresh the user interface
		defaultConfig();
		System.out.println("Finished!");	
	}


	public void openOntologies(String src, String tgt, String tgt2, boolean closure, boolean stemmer) throws OWLOntologyCreationException
	{
		//Initialize the URIMap and RelationshipMap
		uris = new URIMap();
		rels = new RelationshipMap();
		if(useReasoner)
			PropertyConfigurator.configure("log4j.properties");
		long time = System.currentTimeMillis()/1000;
		System.out.println("Loading source ontology");
		if(stemmer)
			source = new Ontology(src,true,true);
		else
			source = new Ontology(src,true,false);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(source.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + source.classCount());	
		System.out.println("Names: " + source.getLexicon().size());
		System.out.println("Properties: " + source.propertyCount());
		time = System.currentTimeMillis()/1000;
		System.out.println("Loading target ontology");	
		if(stemmer)
			target = new Ontology(tgt,true,true);
		else
			target = new Ontology(tgt,true,false);
		
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(target.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + target.classCount());
		System.out.println("Names: " + target.getLexicon().size());
		System.out.println("Properties: " + target.propertyCount());
		System.out.println("Direct Relationships: " + rels.relationshipCount());
		time = System.currentTimeMillis()/1000;
		if(stemmer)
			target2 = new Ontology(tgt2,true,true);
		else
			target2 = new Ontology(tgt2,true,false);
		
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(target2.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + target2.classCount());
		System.out.println("Names: " + target2.getLexicon().size());
		System.out.println("Properties: " + target2.propertyCount());
		System.out.println("Direct Relationships: " + rels.relationshipCount());
		time = System.currentTimeMillis()/1000;
		 
		if(closure)
		{
			System.out.println("Running transitive closure on RelationshipMap");	
			rels.transitiveClosure();
		}
		//time = System.currentTimeMillis()/1000 - time;
		//System.out.println("Transitive closure finished in " + time + " seconds");		
		//System.out.println("Extended Relationships: " + rels.relationshipCount());
		//System.out.println("Disjoints: " + rels.disjointCount());
		//Reset the alignment, mapping, and evaluation
		a = null;
		evaluation = null;
		defaultConfig();
		//System.out.println("Finished!");	
	}


	public void openReferenceAlignment(String path) throws Exception
	{
		ref = new Alignment(path);
	}

	public void openCompoundReferenceAlignment(String path) throws Exception
	{
		refC = new CompoundAlignment(path);
	}

	public void openCompoundReferenceAlignment2(String path) throws Exception
	{
		refC2 = new CompoundAlignment(path);
	}

	public boolean primaryStringMatcher()
	{
		return primaryStringMatcher;
	}

	public boolean removeObsolete()
	{
		return removeObsolete;
	}

	public void saveAlignmentRDF(String file) throws Exception
	{
		a.saveRDF(file);
	}

	public void saveCompoundAlignmentRDF(String file) throws Exception
	{
		aC.saveRDF(file);
	}

	public void saveCompoundAlignmentTSV(String file) throws Exception
	{
		aC.saveTSV3(file);
	}

	public void saveAlignmentTSV(String file) throws Exception
	{
		a.saveTSV(file);
	}

	public void setAlignment(Alignment maps)
	{
		a = maps;
	}

	public void setDirectNeighbors(boolean directNeighbors)
	{
		this.directNeighbors = directNeighbors;
	}

	public void setLabelLanguage(String language)
	{
		this.language = language;
	}

	public void setLanguageSetting()
	{
		lang = LanguageSetting.getLanguageSetting();
	}

	public void setNeighborSimilarityStrategy(NeighborSimilarityStrategy nss)
	{
		this.nss = nss;
	}

	public void setOntologies(Ontology s, Ontology t)
	{
		source = s;
		target = t;
		defaultConfig();
	}

	public void setPrimaryStringMatcher(boolean primary)
	{
		primaryStringMatcher = primary;
	}

	public void setRemoveObsolete(boolean removeObsolete)
	{
		this.removeObsolete = removeObsolete;
	}

	public void setSelectedSources(Vector<String> sources)
	{
		selectedSources = sources;
	}

	public void setSelectedSteps(Vector<MatchStep> steps)
	{
		selectedSteps = steps;
	}


	public void setSelectionType(SelectionType s)
	{
		if(s == null)
			sType = SelectionType.getSelectionType();
		else
			sType = s;
	}

	public void setStringSimMeasure(StringSimMeasure ssm)
	{
		this.ssm = ssm;
	}

	public void setStructuralSelection(boolean structuralSelection)
	{
		this.structuralSelection = structuralSelection;
	}

	public void setThreshold(double thresh)
	{
		threshold = thresh;
	}

	public void setUseReasoner(boolean b)
	{
		useReasoner = b;
	}

	public void setWordMatchStrategy(WordMatchStrategy wms)
	{
		this.wms = wms;
	}

	public boolean structuralSelection()
	{
		return structuralSelection;
	}

	public void translateOntologies()
	{
		Set<String> sLangs = source.getLexicon().getLanguages();
		Set<String> tLangs = target.getLexicon().getLanguages();
		for(String l1 : sLangs)
		{
			for(String l2 : tLangs)
			{
				if(!l1.equals(l2))
				{
					Dictionary d = new Dictionary(l1,l2);
					d.translateLexicon(source.getLexicon());
					d.translateProperties(source);
					d = new Dictionary(l2,l1);
					d.translateLexicon(target.getLexicon());
					d.translateProperties(target);
				}
			}
		}
		languages = new HashSet<String>();
		for(String s : source.getLexicon().getLanguages())
			if(target.getLexicon().getLanguages().contains(s))
				languages.add(s);
	}

	public boolean useReasoner()
	{
		return useReasoner;
	}
}