package net.openpv.pvgui;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.ValueType;

public class PVNetwork
{
	protected LinkedList<PVObject> objects;
	public PVObject column;
	
	public static PVNetwork buildFromJson(String json)
	{
		return new Json().fromJson(PVNetwork.class, json);
	}
	
	public PVNetwork()
	{
		objects = new LinkedList<PVObject>();
	}
	
	public PVNetwork(String fileName, HashMap<String, ArrayList<JsonValue>> objectCategories)
	{
		readParamsFile(fileName, objectCategories);
	}
	
	public PVObject addObject(JsonValue libraryEntry)
	{
		PVObject object = new PVObject(libraryEntry);
		return addObject(object);
	}
	
	public PVObject addObject(PVObject object)
	{
		//Find a unique name
		Integer suffix = 0;
		boolean needSuffix = false;
		boolean done = false;
		while(!done)
		{
			done = true;
			for(PVObject other : objects)
			{
				if(!needSuffix && other.name.equals(object.name))
				{
					needSuffix = true;
					done = false;
					break;
				}
				if(other.name.equals(object.name + suffix.toString()))
				{
					suffix++;
					done = false;
					break;
				}
			}
		}
		if(needSuffix) object.name += suffix.toString();
		objects.add(object);
		return object;
	}
	
	public List<PVObject> getObjects() { return Collections.unmodifiableList(objects); }
	
	public PVObject whoDidIClickOn(float x, float y)
	{
		Rectangle me = new Rectangle();
		for(PVObject object : objects)
		{
			me.set(object.x-object.width/2, object.y-object.height/2, object.width, object.height);
			if(me.contains(x, y)) return object;
		}
		return null;
	}
	
	public void updateParams()
	{
		for(PVObject object : objects)
		{
			if(object.params != null && "name".equals(object.params.get(0).name))
			{
				object.name = (String)object.params.get(0).value;
			}
			else
			{
				object.params.add(0, new PVParam<String>("name", object.name, ValueType.stringValue));
			}
			if(object.params != null && "type".equals(object.params.get(1).name))
			{
				object.type = (String)object.params.get(1).value;
			}
			else
			{
				object.params.add(1, new PVParam<String>("type", object.type, ValueType.stringValue));
			}
		}
	}
	
	public PVObject getObject(String name, String category)
	{
		for(PVObject search : objects)
		{
			if(category != null && !"".equals(category) && !category.equals(search.category)) continue;
			if(name.equals(search.name)) return search;
		}
		return null;
	}
	
	public void avoidOverlap()
	{
		Rectangle me = new Rectangle();
		Rectangle you = new Rectangle();
		
		for(PVObject object : objects)
		{
			me.set(object.x-object.width/2, object.y-object.height/2, object.width, object.height);
			for(PVObject other : objects)
			{
				if(object == other) continue;
				you.set(other.x-other.width/2, other.y-other.height/2, other.width, other.height);
				if(me.overlaps(you) || me.equals(you))
				{
			        float xIntersect = Math.max(me.x, you.x);
			        float xOverlap = Math.min(me.x + me.width, you.x + you.width) - xIntersect;
			        float yIntersect = Math.max(me.y, you.y);
			        float yOverlap = Math.min(me.y + me.height, you.y + you.height) - yIntersect;
					float force = xOverlap * yOverlap / (me.area() + you.area());
					if(me.area() == you.area()) //A perfect overlap would get stuck without this
					{
						object.x += Math.random() * 0.25f - 0.125f;
						object.y += Math.random() * 0.25f - 0.125f;
					}
					
					//TODO: When an object is overlapped, pull it towards its connections almost as much as you push it outside other objects
					
					object.x += Math.sqrt(force) * (object.x - other.x) * 0.1f;
					object.y += Math.sqrt(force) * (object.y - other.y) * 0.1f;
				}
			}
		}
	}
	
	public void centerObjects()
	{
		float xSum = 0;
		float ySum = 0;
		int count = 0;
		
		for(PVObject object : objects)
		{
			xSum += object.x;
			ySum += object.y;
			count++;
		}
		float xAvg = xSum / count;
		float yAvg = ySum / count;
		
		for(PVObject object : objects)
		{
			object.x -= xAvg;
			object.y -= yAvg;
		}
	}
	
	public void shiftConnections(float percent)
	{
		for(PVObject object : objects)
		{
			if(!"Connection".equals(object.category)) continue;
			PVParam<String> pre = object.getParam("preLayerName");
			PVParam<String> post = object.getParam("postLayerName");
			
			if(pre != null && post != null)
			{
				PVObject preLayer = getObject(pre.value, "Layer");
				PVObject postLayer = getObject(post.value, "Layer");
				if(preLayer != null && postLayer != null)
				{
					float centerX = (preLayer.x + postLayer.x) / 2f;
					float centerY = (preLayer.y + postLayer.y) / 2f;
					
					object.x = object.x * (1 - percent) + centerX * percent;
					object.y = object.y * (1 - percent) + centerY * percent;
				}
			}
		}
	}
	
	public void averageConnections(float targetAvg)
	{
		float minDistance = Float.MAX_VALUE;
		float maxDistance = Float.MIN_VALUE;
		float sum = 0;
		int count = 0;
		
		//Find our shortest and longest connections
		for(PVObject object : objects)
		{
			if(!"Connection".equals(object.category)) continue;
			PVParam<String> pre = object.getParam("preLayerName");
			PVParam<String> post = object.getParam("postLayerName");
			
			if(pre != null)
			{
				PVObject preLayer = getObject(pre.value, "Layer");
				if(preLayer != null)
				{
					float dist = Vector2.len(object.x-preLayer.x, object.y-preLayer.y);
					minDistance = dist < minDistance ? dist : minDistance;
					maxDistance = dist > maxDistance ? dist : maxDistance;
					sum += dist;
					count++;
				}
			}
			if(post != null)
			{
				PVObject postLayer = getObject(post.value, "Layer");
				if(postLayer != null)
				{
					float dist = Vector2.len(object.x-postLayer.x, object.y-postLayer.y);
					minDistance = dist < minDistance ? dist : minDistance;
					maxDistance = dist > maxDistance ? dist : maxDistance;
					sum += dist;
					count++;
				}
			}
		}
		
		float avg = (sum / count + targetAvg) * 0.5f;
		
		//Move everything towards having the same connection length
		for(PVObject object : objects)
		{
			if(!"Connection".equals(object.category)) continue;
			PVParam<String> pre = object.getParam("preLayerName");
			PVParam<String> post = object.getParam("postLayerName");
			
			if(pre != null)
			{
				PVObject preLayer = getObject(pre.value, "Layer");
				if(preLayer != null)
				{
					Vector2 dist = new Vector2(preLayer.x-object.x, preLayer.y-object.y);
					float moveAmt = 0f;
					if(dist.len() < avg)
					{
						moveAmt = 1f / minDistance;
					}
					if(dist.len() > avg)
					{
						moveAmt = -1f / maxDistance;
					}
					dist.scl(moveAmt);
					preLayer.x += dist.x;
					preLayer.y += dist.y;
				}
			}
			if(post != null)
			{
				PVObject postLayer = getObject(post.value, "Layer");
				if(postLayer != null)
				{
					Vector2 dist = new Vector2(postLayer.x-object.x, postLayer.y-object.y);
					float moveAmt = 0f;
					if(dist.len() < avg)
					{
						moveAmt = 1f / minDistance;
					}
					if(dist.len() > avg)
					{
						moveAmt = -1f / maxDistance;
					}
					dist.scl(moveAmt);
					postLayer.x += dist.x;
					postLayer.y += dist.y;
				}
			}
		}
	}
	
	public void readParamsFile(String fileName, HashMap<String, ArrayList<JsonValue>> library)
	{
		//Reads a params file into the current network.
		//TODO: Give the option of ignoring the column to add it to the current network
		
		objects.clear();
		try
		{
			StringBuilder token = new StringBuilder();
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			boolean definingObject = false;
			PVObject object = null;
			while(true)
			{
				String line = in.readLine();
				if(line == null)
				{
					if(definingObject) { throw new IOException("Error: EOF while reading object."); }
					else break;
				}
				line = line.trim();
				if(line.length() <= 0) continue;
				if(line.startsWith("//")) continue;
				if(!definingObject)
				{
					int firstQuote = line.indexOf('"'); 
					if(firstQuote != -1)
					{
						int secondQuote = line.indexOf('"', firstQuote+1);
						String type = line.substring(0, firstQuote).trim();
						String name = line.substring(firstQuote+1, secondQuote).trim();
						if(line.indexOf('=') > secondQuote && line.indexOf('{') > line.indexOf('='))
						{
							//We have the start of a valid object definition, let's get on that
							definingObject = true;
							object = new PVObject();
							object.name = name;
							object.type = type;
							if("HyPerCol".equals(object.type))
							{
								object.category = "Column";
								column = object;
							}
							else
							{
								//This isn't perfect but it should work for most cases
								if(object.type.contains("Conn") && !object.type.contains("Layer")) object.category = "Connection";
								else if(object.type.contains("Probe")) object.category = "Probe";
								else object.category = "Layer";
								
								ArrayList<JsonValue> category = library.get(object.category);
								JsonValue found = null;
								for(JsonValue search : category)
								{
									if(search.has("name") && object.type.equals(search.getString("name")))
									{
										found = search;
										break;
									}
								}
								if(found != null && found.hasChild("style")) object.applyStyle(found);
								else
								{
									//The first library entry for each category is the default
									//configuration for that object type
									object.applyStyle(category.get(0));
								}
							}
						}
					}
				}
				else
				{
					if(object == null) { throw new IOException("Error: Object is null"); }
					int equalsIndex = line.indexOf('=');
					if(equalsIndex != -1)
					{
						int semiIndex = line.indexOf(';');
						if(semiIndex == -1) { throw new IOException("Error: Expecting ; at end of line while definining " + object.type + "::" + object.name); }
						
						String paramName = line.substring(0, equalsIndex).trim();
						String paramValue = line.substring(equalsIndex+1, semiIndex).trim();
						ValueType paramType = ValueType.longValue;
						
						//We have our param, now we have to guess what type we're dealing with
						if(paramValue.equals("infinity"))
						{
							paramValue = ""+Double.POSITIVE_INFINITY;
							paramType = ValueType.doubleValue;
						}
						else if(paramValue.equals("-infinity"))
						{
							paramValue = ""+ Double.NEGATIVE_INFINITY;
							paramType = ValueType.doubleValue;
						}
						else if(paramValue.indexOf('"') != -1 && paramValue.lastIndexOf('"') != -1 && paramValue.indexOf('"') != paramValue.lastIndexOf('"'))
						{
							paramType = ValueType.stringValue;
							paramValue = paramValue.substring(1, paramValue.length()-1);
						}
						else if(paramValue.equals("NULL"))
						{
							paramType = ValueType.stringValue;
							paramValue = "";
						}
						else if(paramValue.equals("true") || paramValue.equals("false"))
						{
							paramType = ValueType.booleanValue;
						}
						else if(paramValue.startsWith("[") && paramValue.endsWith("]"))
						{
							paramType = ValueType.array;
						}
						else if(paramValue.indexOf('.') != -1 && paramValue.indexOf('.') == paramValue.lastIndexOf('.'))
						{
							paramType = ValueType.doubleValue;
						}
						else paramType = ValueType.longValue;
						
						switch(paramType)
						{
							case booleanValue:
								object.params.add(new PVParam<Boolean>(paramName, Boolean.parseBoolean(paramValue), paramType));
								break;
							case doubleValue:
								object.params.add(new PVParam<Double>(paramName, Double.parseDouble(paramValue), paramType));
								break;
							case longValue:
								object.params.add(new PVParam<Long>(paramName, Long.parseLong(paramValue), paramType));
								break;
							default:
								object.params.add(new PVParam<String>(paramName, paramValue, paramType));
								break;
						}
					}
					else if(line.indexOf('}') != -1 && line.indexOf(';') > line.indexOf('}'))
					{
						if(!"Column".equals(object.category)) { addObject(object); }
						object = null;
						definingObject = false;
					}
					else
					{
						throw new IOException("Error: Expecting parameter or };"); 
					}
				}
			}
		}
		catch(IOException e) { System.err.println(e.toString()); }
		
		//Try to arrange the objects in a usable way
		float scatterFactor = 1;
		for(PVObject object : objects)
		{
			if(object.width > scatterFactor) scatterFactor = object.width;
			if(object.height > scatterFactor) scatterFactor = object.height;
		}
		scatterFactor = (float)Math.sqrt(scatterFactor);
		float scatterRange = objects.size() * scatterFactor;
		//First, layers
		for(PVObject object : objects)
		{
			if(!"Layer".equals(object.category)) continue;
			if(object.getParam("phase") == null)
			{
				object.x = -scatterRange + (float)Math.random() * scatterRange * 2f;
				object.y = -scatterRange + (float)Math.random() * scatterRange * 2f;
			}
			else
			{
				System.out.println(object.name + ": " + object.getParam("phase").value);
				object.x = 0;
				object.y = ((Long)object.getParam("phase").value) * scatterFactor * scatterFactor;
			}
		}
		//Now connections
		for(PVObject object : objects)
		{
			if(!"Connection".equals(object.category)) continue;
			if(object.getParam("preLayerName") == null || object.getParam("postLayerName") == null)
			{
				object.x = -scatterRange + (float)Math.random() * scatterRange * 2f;
				object.y = -scatterRange + (float)Math.random() * scatterRange * 2f;
			}
			else
			{
				PVObject preLayer = getObject(object.getParam("preLayerName").value.toString(), "Layer");
				PVObject postLayer = getObject(object.getParam("postLayerName").value.toString(), "Layer");
				if(preLayer == null || postLayer == null)
				{
					object.x = -scatterRange + (float)Math.random() * scatterRange * 2f;
					object.y = -scatterRange + (float)Math.random() * scatterRange * 2f;
				}
				else //Position connections between their layers
				{
					object.x = (preLayer.x + postLayer.x) / 2;
					object.y = (preLayer.y + postLayer.y) / 2;
				}
			}
		}
		//Finally, probes. We don't want them cluttering up the graph, so they're off in the corner
		int probeY = 0;
		for(PVObject object : objects)
		{
			if(!"Probe".equals(object.category)) continue;
			object.x = scatterRange * 2f;
			object.y = scatterRange * 2f + probeY;
			probeY += object.height * 1.5f;
		}
		
		//Lastly, let's try arranging the objects to something readable
		
		for(int i = 0; i < 1000; i++)
		{
			avoidOverlap();
			shiftConnections(0.25f);
			averageConnections(scatterFactor * 10);
			centerObjects();
		}
	}
	
	public void writeParamsFile(String fileName)
	{
		FileHandle out = new FileHandle(fileName);
		LinkedList<PVObject> layers = new LinkedList<PVObject>();
		LinkedList<PVObject> connections = new LinkedList<PVObject>();
		LinkedList<PVObject> probes = new LinkedList<PVObject>();
		for(PVObject obj : objects)
		{
			if("Layer".equals(obj.category)) layers.add(obj);
			if("Connection".equals(obj.category)) connections.add(obj);
			if("Probe".equals(obj.category)) probes.add(obj);
		}
		LinkedList<PVObject> sorted = new LinkedList<PVObject>();
		sorted.addAll(layers);
		sorted.addAll(connections);
		sorted.addAll(probes);
		
		//Write column
		out.writeString(column.type + " \"" + column.name + "\" = {\n", false); //Don't append, overwrite
		for(PVParam param : column.params)
		{
			if("name".equals(param.name)) continue;
			out.writeString("    " + param.toString() + ";\n", true);
		}
		out.writeString("};\n\n", true);
		
		for(PVObject obj : sorted)
		{
			out.writeString(obj.type + " \"" + obj.name + "\" = {\n", true);
			for(PVParam param : obj.params)
			{
				if("name".equals(param.name)) continue;
				out.writeString("    " + param.toOutputString() + ";\n", true);
			}
			out.writeString("};\n\n", true);
		}
	}
	
	public String toJsonString(boolean pretty)
	{
		if(!pretty) return new Json().toJson(this);
		return new Json().prettyPrint(this);
	}
}
