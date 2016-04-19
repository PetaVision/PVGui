package net.openpv.pvgui;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.ValueType;

public class PVObject
{
	public enum ObjectShape
	{
		Box,
		Diamond,
		Triangle,
		Oval
	};
	
	public float x = 0.0f;
	public float y = 0.0f;
	public float width = 100f;
	public float height = 32f;
	public float text_offset = 0f;
	public String name = "Object";
	public String category = "Unknown";
	public String type = "Unknown";
	public ObjectShape shape = ObjectShape.Box;
	public Color color = new Color(0.5f, 0.5f, 0.5f, 1f);
	public Color outline = new Color(0f, 0f, 0f, 1f);
	@SuppressWarnings("rawtypes")
	public ArrayList<PVParam> params = new ArrayList<PVParam>();
	
	public PVObject() {}
	
	@SuppressWarnings("rawtypes")
	public PVObject(JsonValue libraryEntry)
	{
		if(libraryEntry.has("name"))
		{
			type = libraryEntry.getString("name");
			name = type;
		}
		if(libraryEntry.has("style"))
		{
			applyStyle(libraryEntry);
		}
		JsonValue paramsTemplate = libraryEntry.get("params");

		if(paramsTemplate != null)
		{
			for(JsonValue template : paramsTemplate)
			{
				String type = template.getString("type");
				String valueAsString = template.getString("default").trim();
				PVParam param = null;
				switch(type)
				{
					case "boolean":
					case "bool":
						param = new PVParam<Boolean>(template.getString("name"), Boolean.parseBoolean(valueAsString), ValueType.booleanValue);
						break;
					case "float":
					case "double":
						param = new PVParam<Double>(template.getString("name"), Double.parseDouble(valueAsString), ValueType.doubleValue);
						break;
					case "int":
					case "integer":
					case "byte":
					case "long":
						param = new PVParam<Long>(template.getString("name"), Long.parseLong(valueAsString), ValueType.longValue);
						break;
					case "string":
						param = new PVParam<String>(template.getString("name"), valueAsString, ValueType.stringValue);
						break;
					case "array": //TODO: Implement array params
					default:
						continue;
				}
				if(param != null)
				{
					params.add(param);
				}
			}
		}
	}
	
	public void applyStyle(JsonValue libraryEntry)
	{
		JsonValue style = libraryEntry.get("style");
		if(style == null) return;
		if(style.has("color"))
		{
			color.r = style.get("color").asFloatArray()[0];
			color.g = style.get("color").asFloatArray()[1];
			color.b = style.get("color").asFloatArray()[2];
		}
		if(style.has("outline"))
		{
			outline.r = style.get("outline").asFloatArray()[0];
			outline.g = style.get("outline").asFloatArray()[1];
			outline.b = style.get("outline").asFloatArray()[2];
		}
		if(style.has("shape")) shape = ObjectShape.valueOf(style.getString("shape"));
		if(style.has("width")) width = style.getFloat("width");
		if(style.has("height")) height = style.getFloat("height");
		if(style.has("text_offset")) text_offset = style.getFloat("text_offset");
	}
	
	public PVParam getParam(String name)
	{
		for(PVParam search : params)
		{
			if(name.equals(search.name)) return search;
		}
		return null;
	}
	
	public PVObject clone() //Return a new, identical instance (Except the name)
	{
		PVObject result = new PVObject();
		result.x = x;
		result.y = y;
		result.type = type;
		result.category = category;
		if(!"Column".equals(category)) result.name = name + "_Copy";
		else result.name = name;
		result.shape = shape;
		result.color = color;
		result.outline = outline;
		result.width = width;
		result.height = height;
		result.text_offset = text_offset;
		for(PVParam param : params)
		{
			if("name".equals(param.name)) continue;
			result.params.add(param.clone());
		}
		return result;
	}
}
