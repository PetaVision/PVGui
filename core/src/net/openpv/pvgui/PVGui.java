package net.openpv.pvgui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.ValueType;

public class PVGui extends ApplicationAdapter
{
	LinkedList<UIState> stateStack;
	SpriteBatch spriteBatch;
	OrthographicCamera orthoCam;
	PVObject defaultColumn;
	LinkedList<String> redoLabels;
	LinkedList<String> undoLabels;
	LinkedList<String> undoStack;
	LinkedList<String> redoStack;
	//Utility methods
	
	
	public SpriteBatch getSpriteBatch() { return spriteBatch; }
	public OrthographicCamera getCamera() { return orthoCam; }
	public HashMap<String, ArrayList<JsonValue>> getLibrary() { return library; }
	public ArrayList<String> getLibraryCategories() { return libraryCategories; }
	public PVObject getDefaultColumn() { return defaultColumn; } //TODO: This needs to return a copy
	private Skin uiSkin = null;
	private HashMap<String, ArrayList<JsonValue>> library;
	private ArrayList<String> libraryCategories;
	private LinkedList<Message> messages;
	private final int messageLifetime = 300;
	private Texture gradient;
	
	private class Message
	{
		public String message;
		public int timer;
		public Message(String message, int timer) { this.message = message; this.timer = timer; }
	}
	
	public void undoable(String message, PVNetwork network)
	{
		undoable(message, network, true);
	}
	public void undoable(String message, PVNetwork network, boolean showMessage)
	{
		if(showMessage) message(message);
		undoLabels.push(message);
		undoStack.push(network.toJsonString(false));
		redoLabels.clear();
		redoStack.clear();
	}
	
	public PVNetwork undo(PVNetwork currentState)
	{
		if(undoStack.isEmpty()) return null;
		redoStack.push(currentState.toJsonString(false));
		String label = undoLabels.pop();
		redoLabels.push(label);
		message("UNDO: " + label);
		return PVNetwork.buildFromJson(undoStack.pop());
	}
	
	public PVNetwork redo(PVNetwork currentState)
	{
		if(redoStack.isEmpty()) return null;
		undoStack.push(currentState.toJsonString(false));
		String label = redoLabels.pop();
		undoLabels.push(label);
		message("REDO: " + label);
		return PVNetwork.buildFromJson(redoStack.pop());
	}
	
	public void message(String message)
	{
		messages.push(new Message(message, messageLifetime));
	}
	
	public void pushState(UIState newState)
	{
		//TODO: Focus / unfocus methods if needed
		newState.setParent(this);
		newState.init();
		stateStack.push(newState);
		Gdx.input.setInputProcessor(newState.getStage());
	}
	
	public void popState()
	{
		UIState oldState = stateStack.pop();
		if(oldState != null) oldState.destroy();
		if(!stateStack.isEmpty())
		{
			Gdx.input.setInputProcessor(stateStack.peek().getStage());
		}
	}
	
	public Skin getSkin() { return uiSkin; }
	
	//Meat 'n potatoes
	
	@Override
	public void create ()
	{
		undoLabels = new LinkedList<String>();
		redoLabels = new LinkedList<String>();
		undoStack = new LinkedList<String>();
		redoStack = new LinkedList<String>();
		messages = new LinkedList<Message>();
		gradient = new Texture("gradient.png");
		orthoCam = new OrthographicCamera();
		spriteBatch = new SpriteBatch();
		stateStack = new LinkedList<UIState>();
		library = new HashMap<String, ArrayList<JsonValue>>();
		JsonValue libraryRoot = new JsonReader().parse(Gdx.files.local("library.json"));
		libraryCategories = new ArrayList<String>();
		uiSkin = new Skin(Gdx.files.internal("uiskin.json"));
		
		for(JsonValue value : libraryRoot)
		{
			if(value.isArray())
			{
				libraryCategories.add(value.name);
				ArrayList<JsonValue> objects = new ArrayList<JsonValue>();
				library.put(value.name, objects);
				
				for(JsonValue objectEntry : value)
				{
					objects.add(objectEntry);
				}
			}
			else if(value.isObject() && "column".equals(value.name.toLowerCase()))
			{
				defaultColumn = new PVObject(value);
				defaultColumn.params.add(0, new PVParam<String>("name", value.getString("name"), ValueType.stringValue));
				defaultColumn.category = "Column";
			}
		}
		
		pushState(new MainState());
	}

	@Override
	public void render ()
	{
		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		BitmapFont font = getSkin().getFont("default-font");
		GlyphLayout layout = new GlyphLayout();
		Iterator reverse = stateStack.descendingIterator();
		while(reverse.hasNext())
		{
			UIState state = (UIState)reverse.next();
			state.update();
			state.render();
		}
		
		getSpriteBatch().begin();
		float yOff = 0;
		LinkedList<Message> toRemove = new LinkedList<Message>();
		for(Message m : messages)
		{
			float nextYOff = yOff + 24;
			float xOff = 0;
			if(m.timer > messageLifetime-30)
			{
				float factor = (messageLifetime - m.timer) / 30f;
				font.setColor(1,1,1, factor);
				xOff = -256f * (1-factor)*(1-factor);
				if(yOff == 0)
				{
					nextYOff = (1f-(1f-factor)*(1f-factor)) * 24; //This looks ridiculous but it's smooth
				}
			}
			if(m.timer < 60)
			{
				font.setColor(1,1,1,m.timer / 60f);
				if(m.timer <= 0) toRemove.add(m);
			}
			else
			{
				font.setColor(1,1,1,1f);
			}
			getSpriteBatch().setColor(font.getColor());
			getSpriteBatch().draw(gradient, Gdx.graphics.getWidth()-xOff, 8+yOff, -512, 20);
			layout.setText(font, m.message);
			font.draw(getSpriteBatch(), m.message, Gdx.graphics.getWidth()-xOff-8-layout.width, 24 + yOff);
			m.timer--;
			yOff = nextYOff;
		}
		while(!toRemove.isEmpty()) { messages.remove(toRemove.pop()); }
		getSpriteBatch().end();
		
		font.setColor(1,1,1,1);
		getSpriteBatch().setColor(1,1,1,1);
	}
	
	@Override
	public void resize(int width, int height)
	{
		orthoCam.setToOrtho(false, width, height);
		for(UIState state : stateStack) { state.resize(width, height); }
	}
	
	@Override
	public void dispose()
	{
		for(UIState state : stateStack) { state.destroy(); }
	}

}
