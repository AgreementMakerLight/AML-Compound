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
 * Selector that reduces an Alignment to strict, permissive or hybrid 1-to-1   *
 * cardinality.                                                                *
 *                                                                             *
 * @author Daniel Faria                                                        *
 * @date 07-07-2014                                                            *
 * @version 2.1                                                                *
 ******************************************************************************/
package aml.filter;

import java.util.HashMap;

import aml.AML;
import aml.match.CompoundAlignment;
import aml.match.CompoundMapping;
import aml.ontology.URIMap;
import aml.settings.CompoundSelectionType;
import aml.settings.SelectionType;

public class CompoundRankedSelector implements CompoundSelector
{

	//Attributes

	private CompoundSelectionType type;

	//Constructors

	public CompoundRankedSelector()
	{
		type = null;
	}

	public CompoundRankedSelector(CompoundSelectionType s)
	{
		type = s;
	}

	//Public Methods

	@Override
	public CompoundAlignment select(CompoundAlignment a)
	{
		System.out.println("Performing Ranked Selection");
		long time = System.currentTimeMillis()/1000;
		if(type == null)
			return a;
		//Initialize Alignment to return
		AML aml = AML.getInstance();
		HashMap<Integer, String> rec = aml.getTarget2().getReciprocalClasses();
		URIMap uris = aml.getURIMap();
		CompoundAlignment selected = new CompoundAlignment();
		//Then sort the alignment
		a.sort();
		//Then select Mappings in ranking order (by similarity)
		for(CompoundMapping m : a)
		{
			if(rec.containsKey(m.getTargetId2()))
				m.setTargetId2(uris.getIndex(rec.get(m.getTargetId2())));
			if(type.equals(CompoundSelectionType.PERMISSIVE))
			{
				if(!selected.containsSource(m.getSourceId()))
					selected.add(new CompoundMapping(m));
				else
				{
					if(a.countMappings(m.getSourceId()) > 1 && selected.countMappings(m.getSourceId()) < 3)
						selected.add(new CompoundMapping(m));
				}
			}
			else if(type.equals(CompoundSelectionType.STRICT))
			{
				if(!selected.containsSource(m.getSourceId()))
					selected.add(m.getSourceId(), m.getTargetId1(), m.getTargetId2(), m.getSimilarity());
			}
			else
				return a;
		}
		
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return selected;
	}
}