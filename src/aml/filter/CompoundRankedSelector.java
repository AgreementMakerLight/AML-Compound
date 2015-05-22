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
* Selector that reduces a Compound Alignment to permissive alignment.         *
*                                                                             *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 07-07-2014                                                            *
* @version 2.1                                                                *
******************************************************************************/
package aml.filter;

import aml.match.CompoundAlignment;
import aml.match.CompoundMapping;
import aml.settings.SelectionType;

public class CompoundRankedSelector implements CompoundSelector
{
	
//Attributes
	
	private SelectionType type;
	
//Constructors
	
	public CompoundRankedSelector()
	{
		type = null;
	}
	
	public CompoundRankedSelector(SelectionType s)
	{
		type = s;
	}

//Public Methods
	
	@Override
	public CompoundAlignment select(CompoundAlignment a, double thresh)
	{
		System.out.println("Performing Ranked Selection");
		long time = System.currentTimeMillis()/1000;
		if(type == null)
			type = SelectionType.getSelectionType();
		//Initialize Alignment to return
		CompoundAlignment selected = new CompoundAlignment();
		//Then sort the alignment
		a.sort();
		//Then select Mappings in ranking order (by similarity)
		for(CompoundMapping m : a)
		{
			//If a Mapping has similarity below the threshold, end the loop
			if(m.getSimilarity() < thresh)
				break;
			if((type.equals(SelectionType.STRICT) && !selected.containsConflict(m)) ||
					(type.equals(SelectionType.PERMISSIVE) && !selected.containsBetterMapping(m)))
				selected.add(new CompoundMapping(m));

		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return selected;
	}
}