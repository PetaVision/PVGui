package net.openpv.pvgui;

import com.badlogic.gdx.utils.JsonValue.ValueType;

public class PVParam<T>
{
	String name;
	ValueType type;
	T value;
	
	public PVParam() { } 
	
	public PVParam(String name, T value, ValueType type)
	{
		this.name = name;
		this.value = value;
		this.type = type;
	}
	
	public String toOutputString()
	{
		if(type == ValueType.doubleValue && (Double)value == Double.POSITIVE_INFINITY)
		{
			return name + " = infinity";
		}
		if(type == ValueType.doubleValue && (Double)value == Double.NEGATIVE_INFINITY)
		{
			return name + " = -infinity";
		}
		if(type == ValueType.stringValue && "".equals(value)) return name + " = NULL";
		return toString();
	}
	
	public String toString()
	{
		String val = value.toString();
		if(type == ValueType.stringValue) val = "\"" + val + "\"";
		return name + " = " + val;
	}
	
	public PVParam<T> clone()
	{
		return new PVParam<T>(name, value, type);
	}
}
