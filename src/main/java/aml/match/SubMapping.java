package aml.match;

public class SubMapping
{
	//Attributes
	private Integer sId;
	private Integer tId;
	private String labelSource;
	private String labelTarget;
	private double similarity;
	private double weight;

	public SubMapping()
	{
	}

	public SubMapping(Integer srcId, Integer tgtId,String src, String tgt, double sim, double w)
	{
		this.labelSource = src;
		this.labelTarget = tgt;
		this.sId = srcId;
		this.tId = tgtId;
		this.similarity = sim;		
		this.weight = w;
	}

	public void setLabelSource(String src)
	{
		labelSource = src;
	}
	
	public boolean contains(String s)
	{
		if(labelSource.contains(s) || labelTarget.contains(s))
			return true;
		return false;
	}

	public void setLabelTarget(String tgt)
	{
		labelTarget = tgt;
	}

	public void setLabelSimilarity(double sim)
	{
		similarity = sim;
	}

	public void set(String src, String tgt, double sim)
	{
		labelSource = src;
		labelTarget = tgt;
		similarity = sim;

	}

	public Integer getSourceId()
	{
		return sId;
	}
	
	public Integer getTargetId()
	{
		return tId;
	}
	public String getLabelSource()
	{
		return labelSource;
	}


	public String getLabelTarget()
	{
		return labelTarget;
	}

	public double getSimilarity()
	{
		return similarity;
	}
	public double getWeight()
	{
		return weight;
	}

	public String toString()
	{
		return labelSource + " = " + labelTarget + ": "+ Double.toString(similarity);
	}

}
