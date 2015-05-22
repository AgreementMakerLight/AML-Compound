package aml.settings;


public enum SimilarityStrategy
	{
		COMBINED ("combined"),
		JACCARD ("jaccard"),
		SOURCE ("source"),
		TARGET ("target");
		
		
		String label;
		
		SimilarityStrategy(String s)
	    {
	    	label = s;
	    }
		
		public static SimilarityStrategy parseStrategy(String strat)
		{
			for(SimilarityStrategy s : SimilarityStrategy.values())
				if(strat.equalsIgnoreCase(s.toString()))
					return s;
			return null;
		}
		
	    public String toString()
	    {
	    	return label;
		}
}
