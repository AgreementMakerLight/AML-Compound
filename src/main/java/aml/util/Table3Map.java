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
 * A table with two variable columns and one fixed column, represented by a    *
 * HashMap of HashMaps.                                                        *
 *                                                                             *
 * @author Daniel Faria                                                        *
 * @date 23-06-2014                                                            *
 * @version 2.1                                                                *
 ******************************************************************************/
package aml.util;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

public class Table3Map<A,B,C,D extends Comparable<D>>
{

	//Attributes
	private HashMap<A,Table2Map<B,C,D>> multimap;
	private int size;

	//Constructors

	/**
	 * Constructs a new empty Table
	 * @return 
	 */
	public Table3Map()
	{
		multimap = new HashMap<A,Table2Map<B,C,D>>();
		size = 0;
	}

	/**
	 * Constructs a new Table that is a copy of
	 * the given Table
	 * @param m: the Table to copy
	 * @return 
	 */

	public Table3Map(Table3Map<A,B,C,D> m)
	{

		multimap = new HashMap<A,Table2Map<B,C,D>>();
		size = m.size;
		Set<A> keys = m.keySet();
		for(A a : keys)
		{
			multimap.put(a, new Table2Map<B,C,D>(m.get(a)));
		}
	}	

	//Public Methods

	/**
	 * Adds the value for the given keys to the Table
	 * If there is already a value for the given keys, the
	 * value will be replaced
	 * @param keyA: the first level key to add to the Table
	 * @param keyB: the second level key to add to the Table
	 * @param keyC: the third level key to add to the Table
	 * @param valueD: the value for the pair of keys to add to the Table
	 */
	public void add(A keyA, B keyB, C keyC, D valueD)
	{

		Table2Map<B,C,D> mapsA = multimap.get(keyA);
		//HashMap<B,HashMap<D,C>> mapsB = new HashMap<B,HashMap<D,C>>();
		//HashMap<D,C> mapsD = new HashMap<D,C>();
		if(mapsA == null)
		{
			mapsA = new Table2Map<B,C,D>();
			mapsA.add(keyB, keyC, valueD);
			multimap.put(keyA, mapsA);
			

		}
		else
			mapsA.add(keyB, keyC, valueD);
		size++;
	}


	/**
	 * Adds the value for the given keys to the Table
	 * unless there is already a value for the given keys
	 * @param keyA: the first level key to add to the Table
	 * @param keyB: the second level key to add to the Table
	 * @param keyC: the third level key to add to the Table
	 * @param valueD: the value for the pair of keys to add to the Table
	 */
	public void addIgnore(A keyA, B keyB, C keyC, D valueD)
	{
		Table2Map<B,C,D> mapsA = multimap.get(keyA);
		if(mapsA == null)
		{
			mapsA = new Table2Map<B,C,D>();
			mapsA.add(keyB, keyC, valueD);
			multimap.put(keyA, mapsA);
			size++;

		}

		else if(!mapsA.contains(keyB))
		{
			mapsA.add(keyB, keyC, valueD);
			size++;
		}
	}

	/**
	 * Adds the value for the given keys to the Table
	 * If there is already a value for the given keys, the
	 * new value will replace the previous value only if it
	 * compares favorably as determined by the compareTo test
	 * @param keyA: the first level key to add to the Table
	 * @param keyB: the second level key to add to the Table
	 * @param keyC: the third level key to add to the Table
	 * @param valueD: the value for the pair of keys to add to the Table
	 */
	public void addUpgrade(A keyA, B keyB, C keyC, D valueD)
	{
		Table2Map<B,C,D> mapsA = multimap.get(keyA);
		
		if(mapsA == null)
		{
			mapsA = new Table2Map<B,C,D>();
			mapsA.add(keyB, keyC, valueD);
			size++;

		}

		else if(!mapsA.contains(keyB))
		{
			mapsA.add(keyB, keyC, valueD);
			size++;
		}

		else if(mapsA.get(keyB).get(keyC).compareTo(valueD) < 0)
		{
			mapsA.add(keyB,keyC,valueD);
		}
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @return whether the Table contains the first level keyA
	 */
	public boolean contains(A keyA)
	{
		return multimap.containsKey(keyA);
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @return whether the Table contains an entry with the two keys
	 */
	public boolean contains(A keyA, B keyB)
	{
		return multimap.containsKey(keyA) &&
				multimap.get(keyA).contains(keyB);
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @param keyC: the third level key to search in the Table
	 * @return whether the Table contains an entry with the two keys
	 */
	public boolean contains(A keyA, B keyB, C keyC)
	{

		return multimap.containsKey(keyA) &&
				multimap.get(keyA).contains(keyB) &&
				multimap.get(keyA).get(keyB).containsKey(keyC);
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @param keyC: the third level key to search in the Table
	 * @param valueD: the value to search in the Table
	 * @return whether the Table contains an entry with the two keys
	 * and the given value
	 */
	public boolean contains(A keyA, B keyB, C keyC, D valueD)
	{

		return multimap.containsKey(keyA) &&
				multimap.get(keyA).contains(keyB) &&
				multimap.get(keyA).get(keyB).containsKey(keyC) &&
				multimap.get(keyA).get(keyB).get(keyC).equals(valueD);
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @return the number of entries with keyA
	 */
	public int entryCount(A keyA)
	{
		Table2Map<B,C,D> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return 0;
		return mapsA.size();
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @param valueC: the value to search in the Table
	 * @return the number of entries with keyA that have valueC
	 */
	public int entryCount(A keyA, B keyB, D valueD)
	{
		int count = 0;
		HashMap<C,D> mapsA = multimap.get(keyA).get(keyB);
		if(mapsA == null)
			return count;
		Set<C> setD = mapsA.keySet();
		for(C c : setD)
			if(mapsA.get(c).equals(valueD))
				count++;
		return count;
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @return the HashMap with all entries for keyA
	 */
	public Table2Map<B,C,D> get(A keyA)
	{
		return multimap.get(keyA);
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @return the HashMap with all entries for keyB
	 */

	public HashMap<C,D> getB(A keyA, B keyB)
	{
		return multimap.get(keyA).get(keyB);
	}


	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @param keyC: the third level key to search in the Table
	 * @return the value for the entry with the three keys or null
	 * if no such entry exists
	 */	
	public D get(A keyA, B keyB, C keyC)
	{
		Table2Map<B,C,D> mapsA = multimap.get(keyA);
		if(mapsA == null || !mapsA.contains(keyB))
			return null;


		return mapsA.get(keyB).get(keyC);
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @return the maximum value in entries with keyA
	 */
	public C getKeyMaximum(A keyA, B keyB)
	{
		HashMap<C,D> mapsA = multimap.get(keyA).get(keyB);
		if(mapsA == null)
			return null;
		Vector<C> setC = new Vector<C>(mapsA.keySet());
		C max = setC.get(0);
		D maxVal = mapsA.get(max);
		for(C c : setC)
		{
			D value = mapsA.get(c);
			if(value.compareTo(maxVal) > 0)
			{
				maxVal = value;
				max = c;
			}
		}
		return max;
	}

	/**
	 * @return the set of first level keys in the Table
	 */
	public Set<A> keySet()
	{
		return multimap.keySet();
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @return the set of second level keys in all entries with keyA
	 */
	public Set<B> keySet(A keyA)
	{
		Table2Map<B,C,D> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return null;

		return mapsA.keySet();
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @return the set of second level keys in all entries with keyA
	 */
	public Set<C> keySet(A keyA, B keyB)
	{
		Table2Map<B,C,D> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return null;
		HashMap<C,D> mapsB = mapsA.get(keyB);
		if(mapsB == null)
			return null;
		return mapsB.keySet();
	}

	/**
	 * @return the number of first level keys in the Table
	 */
	public int keyCount()
	{
		return multimap.size();
	}


	/**
	 * Removes all entries for the given first level key
	 * @param keyA: the key to remove from the Table
	 */
	public void remove(A keyA)
	{
		if(multimap.get(keyA) != null)
			size -= multimap.get(keyA).size();
		multimap.remove(keyA);
	}

	/**
	 * Removes the entry for the given key pair
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to remove from the Table
	 */
	public void remove(A keyA, B keyB)
	{
		HashMap<C,D> maps = multimap.get(keyA).get(keyB);
		if(maps != null)
		{
			maps.remove(keyB);
			size--;
		}
	}
	
	/**
	 * @return the total number of entries in the Table
	 */
	public int size()
	{
		return size;
	}
}

