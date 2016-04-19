package net.openpv.pvgui;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.JsonValue;

public class MainState extends UIState
{
	private Table view;
	private List paramsList;
	private String mouseText = "";
	private String libraryCategory = "";
	private TiledDrawable background;
	private float lastMouseX = -1, lastMouseY = -1, cameraX = 0, cameraY = 0;;
	private PVNetwork network;
	private PVObject selected;
	private float delta = 0f;
	private boolean didIMoveSomething = false;
	private boolean didIGrabSomething = false;
	
	public void setNetwork(PVNetwork network) { this.network = network; }
	
	@Override
	public void init()
	{
		selected = null;
		network = new PVNetwork();
		network.column = parent.getDefaultColumn();
		background = new TiledDrawable(new TextureRegion(new Texture("bg.png")));
		background.getRegion().getTexture().setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
		setupStage();
	}

	@Override
	public void build(Skin uiSkin)
	{
		//stage.setDebugAll(true);
		libraryCategory = "";
		
		Table root = new Table(uiSkin);
		root.setFillParent(true);
		stage.addActor(root);

		Table libraryTable = new Table(uiSkin);
		libraryTable.setBackground(uiSkin.getDrawable("default-pane-noborder"));
		final List<String> libraryList = new List<String>(uiSkin);
		libraryList.setItems(parent.getLibraryCategories().toArray(new String[0]));
				
		libraryList.addListener(new ClickListener()
		{ 
			public void clicked(InputEvent event, float x, float y)
			{
				if(getTapCount() == 2)
				{
					if("".equals(libraryCategory))
					{
						libraryCategory =  libraryList.getSelected();
						ArrayList<JsonValue> subcategory = parent.getLibrary().get(libraryCategory);
						ArrayList<String> newList = new ArrayList<String>();
						newList.add("..");
						for(JsonValue entry : subcategory)
						{
							if(entry.isObject())
							{
								newList.add(entry.getString("name"));
							}
						}
						libraryList.setItems(newList.toArray(new String[0]));
					}
					else
					{
						if(libraryList.getSelected().equals(".."))
						{
							libraryList.setItems(parent.getLibraryCategories().toArray(new String[0]));
							libraryCategory = "";
						}
						else
						{
							ArrayList<JsonValue> category = parent.getLibrary().get(libraryCategory);
							JsonValue found = null;
							for(JsonValue search : category)
							{
								if(search.has("name") && libraryList.getSelected().equals(search.getString("name")))
								{
									found = search;
									break;
								}
							}
							if(found != null)
							{
								parent.undoable("Added " + libraryList.getSelected(), network);
								PVObject object = network.addObject(found);
								object.x = cameraX + object.width;
								object.y = -cameraY - object.height;
								object.category = libraryCategory;
								setSelected(object);
							}
						}
					}
				}
			}
		});
		
		ScrollPane libraryScroll = new ScrollPane(libraryList);
		
		libraryTable.row().expandX().fillX().colspan(4);
		libraryTable.add(libraryScroll).expand().fill().pad(2);
		libraryTable.row().expandX().fillX();
		TextButton duplicate, delete, undo, redo;
		libraryTable.add(undo = new TextButton("Undo", uiSkin));
		libraryTable.add(redo = new TextButton("Redo", uiSkin));
		libraryTable.add(duplicate = new TextButton("Duplicate", uiSkin));
		libraryTable.add(delete = new TextButton("Delete", uiSkin));

		
		Table paramsTable = new Table(uiSkin); 
		paramsTable.setBackground(uiSkin.getDrawable("default-pane-noborder"));
		paramsList = new List<PVParam>(uiSkin);
	
		setSelected(selected);
		
		paramsList.addListener(new ClickListener()
		{ 
			public void clicked(InputEvent event, float x, float y)
			{
				if(getTapCount() == 2)
				{
					if(paramsList.getSelected() != null)
					{
						parent.pushState(new ParamState(selected, (PVParam)paramsList.getSelected()));
					}
				}
			}
		});
		
		
		ScrollPane paramsScroll = new ScrollPane(paramsList);

		paramsTable.row().expandX().fillX().colspan(4);
		paramsTable.add(paramsScroll).expand().fill().pad(2);
		paramsTable.row().expandX().fillX();
		TextButton addParam, removeParam, toggleVisible, showHidden;
		paramsTable.add(addParam = new TextButton("+", uiSkin));
		paramsTable.add(removeParam = new TextButton("-", uiSkin));
		paramsTable.add(toggleVisible = new TextButton("Toggle Visible", uiSkin));
		paramsTable.add(showHidden = new TextButton("Show Hidden", uiSkin));
		
		
		SplitPane sidebar = new SplitPane(libraryTable, paramsTable, true, uiSkin);
		sidebar.setMinSplitAmount(0.1f);
		sidebar.setMaxSplitAmount(0.9f);
		sidebar.setSplitAmount(0.25f);
		
		view = new Table(uiSkin);
		view.setTouchable(Touchable.enabled);
		view.row().expand().fill();
		view.add(new Label("", uiSkin)); //This is important, the table won't expand without contents
		view.addListener(new ClickListener()
		{
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button)
			{
				PVObject select = network.whoDidIClickOn(x - stage.getWidth()/2 + cameraX + view.getX(), y - stage.getHeight()/2 - cameraY- view.getY());
				setSelected(select);
				didIMoveSomething = false;
				if(selected != network.column) didIGrabSomething = true;
				else didIGrabSomething = false;

				return true;
			}
			public boolean mouseMoved(InputEvent event, float x, float y)
			{
				if(!isPressed())
				{
					lastMouseX = x;
					lastMouseY = y;
					return true;
				}
				return false;
			}
			public void touchDragged(InputEvent event, float x, float y, int pointer)
			{
				float scrollX = (lastMouseX - x);
				float scrollY = (lastMouseY - y);
				
				if(selected == network.column)
				{
					cameraX += scrollX;
					cameraY -= scrollY;
					
					scrollX *= 1.0f / background.getRegion().getRegionHeight();
					scrollY *= 1.0f / background.getRegion().getRegionHeight();
					
					background.getRegion().scroll(scrollX, -scrollY);
				}
				else
				{
					if(didIGrabSomething && !didIMoveSomething)
					{
						parent.undoable("Moved " + selected.name, network, false);
						didIMoveSomething = true;
					}
					selected.x -= scrollX;
					selected.y -= scrollY;
				}

				lastMouseX = x;
				lastMouseY = y;
			}
		});
		
		SplitPane sidebarDivider = new SplitPane(sidebar, view, false, uiSkin);
		
		float maxSplit = Math.min(1f, 512 / stage.getWidth());
		sidebarDivider.setMaxSplitAmount(maxSplit);
		sidebarDivider.setSplitAmount(maxSplit * 0.75f);
		sidebarDivider.setMinSplitAmount(Math.max(0.1f, 128 / stage.getWidth()));
		
		Table menuBar = new Table(uiSkin);
		menuBar.setBackground(uiSkin.getDrawable("default-rect"));
		menuBar.row().fill().left();
		TextButton newBut, openBut, saveBut;
		menuBar.add(newBut = new TextButton("New", uiSkin)).minWidth(64);
		menuBar.add(openBut = new TextButton("Open", uiSkin)).minWidth(64);
		menuBar.add(saveBut = new TextButton("Save", uiSkin)).minWidth(64);
		TextButton importBut = new TextButton("Import", uiSkin);
		menuBar.add(importBut).minWidth(64);
		TextButton exportBut = new TextButton("Export", uiSkin);
		menuBar.add(exportBut).minWidth(64);
		menuBar.add().expand();
		
		root.row().expandX().fillX();
		root.add(menuBar);
		root.row();
		root.add(sidebarDivider).expand().fill();
		
		root.layout();
		
		DragAndDrop dnd = new DragAndDrop();
		
		dnd.addSource(new Source(libraryList) {
			public Payload dragStart(InputEvent event, float x, float y, int pointer)
			{
				if("".equals(libraryCategory)) { return new Payload(); }
				String selectedLibrary = libraryList.getSelected();
				if(selectedLibrary != null && !"..".equals(selectedLibrary))
				{
					ArrayList<JsonValue> category = parent.getLibrary().get(libraryCategory);
					JsonValue found = null;
					for(JsonValue search : category)
					{
						if(search.has("name") && selectedLibrary.equals(search.getString("name")))
						{
							found = search;
							break;
						}
					}
					if(found != null)
					{
						Payload payload = new Payload();
						payload.setObject(found);
						return payload;
					}
				}
				return new Payload();
			}
		});
		dnd.addTarget(new Target(view)
		{
			public boolean drag(Source source, Payload payload, float x, float y, int pointer)
			{
				if(payload.getObject() == null) return false;
				mouseText = ((JsonValue)payload.getObject()).getString("name");
				return true;
			}
			public void reset (Source source, Payload payload)
			{
				mouseText = "";
				if(payload.getObject() == null) return;
			}
			public void drop(Source source, Payload payload, float x, float y, int pointer)
			{
				if(payload.getObject() == null) return;
				JsonValue ob = ((JsonValue)payload.getObject());
				parent.undoable("Added " + ob.getString("name"), network);
				PVObject object = network.addObject(ob);
				object.x = x - stage.getWidth()/2 + cameraX + getActor().getX() + object.width / 2;
				object.y = y - stage.getHeight()/2 - cameraY - getActor().getY() - object.height / 4;
				object.category = libraryCategory;
				setSelected(object);
			}
		});
		
		final MainState me = this;
		openBut.addListener(new ChangeListener() {
	        public void changed (ChangeEvent event, Actor actor) {
	        	parent.pushState(new OpenState(network.column.name + ".opv", network, me));
	        }
	    });
		
		saveBut.addListener(new ChangeListener() {
	        public void changed (ChangeEvent event, Actor actor) {
	        	parent.pushState(new SaveState(network.column.name + ".opv", network));
	        }
	    });
		
		exportBut.addListener(new ChangeListener() {
	        public void changed (ChangeEvent event, Actor actor) {
	        	String fname = "untitled.params";
	        	if(network.column.getParam("printParamsFilename") != null)
	        	{
	        		fname = (String)network.column.getParam("printParamsFilename").value;
	        	}
	        	parent.pushState(new ExportState((String) fname, network));
	        }
	    });
		importBut.addListener(new ChangeListener() {
	        public void changed (ChangeEvent event, Actor actor) {
	        	String fname = "untitled.params";
	        	if(network.column.getParam("printParamsFilename") != null)
	        	{
	        		fname = (String)network.column.getParam("printParamsFilename").value;
	        	}
	        	parent.pushState(new ImportState(fname, network));
	        }
	    });
		
		duplicate.addListener(new ChangeListener() {
	        public void changed (ChangeEvent event, Actor actor) {
	        	if(selected != network.column)
	        	{
	        		PVObject object = selected.clone();
	        		parent.undoable("Duplicated " + selected.name, network);
	        		network.addObject(object);
					object.x = cameraX + object.width;
					object.y = -cameraY - object.height;
					setSelected(object);
	        	}
	        }
	    });
		
		delete.addListener(new ChangeListener() {
	        public void changed (ChangeEvent event, Actor actor) {
	        	if(selected != network.column)
	        	{
	        		parent.undoable("Deleted " + selected.name, network);
	        		network.objects.remove(selected);
	        		setSelected(null);
	        	}
	        }
	    });
		
		undo.addListener(new ChangeListener() {
	        public void changed (ChangeEvent event, Actor actor) {
	        	PVNetwork change = parent.undo(network);
	        	if(change != null) network = change;
	        }
	    });
		
		redo.addListener(new ChangeListener() {
	        public void changed (ChangeEvent event, Actor actor) {
	        	PVNetwork change = parent.redo(network);
	        	if(change != null) network = change;
	        }
	    });
		
		saveBut.addListener(new ChangeListener() {
	        public void changed (ChangeEvent event, Actor actor) {
	        	System.out.println(network.toJsonString(true));
	        }
	    });
	}

	@Override
	public void update()
	{
		//if(Gdx.input.isKeyJustPressed(Keys.D)) stage.setDebugAll(true);
		if(!Gdx.input.isButtonPressed(Buttons.LEFT)) network.avoidOverlap();
		network.updateParams();
		stage.act();
	}

	@Override
	public void render()
	{
		delta += 0.1f;
		
		//Draw beneath the UI
		parent.getSpriteBatch().begin();
		background.draw(parent.getSpriteBatch(), 0, 0, stage.getWidth(), stage.getHeight());
		parent.getSpriteBatch().end();
		
		float centeredX = stage.getWidth() / 2 - cameraX;
		float centeredY = stage.getHeight() / 2 + cameraY;
		
		shapeRenderer.setProjectionMatrix(parent.getSpriteBatch().getProjectionMatrix());
		shapeRenderer.setTransformMatrix(parent.getSpriteBatch().getTransformMatrix());
		
		
		
		//Connections first
		shapeRenderer.begin(ShapeType.Line);
		for(PVObject object : network.getObjects())
		{
			if(!"Connection".equals(object.category)) continue;
			
			Long channel = (Long)object.getParam("channelCode").value;
			
			String pre = object.getParam("preLayerName").value.toString();
			String post = object.getParam("postLayerName").value.toString();
			
			PVObject preObj = network.getObject(pre, "Layer");
			PVObject postObj = network.getObject(post, "Layer");
			
			if(preObj != null && postObj != null)
			{
				shapeRenderer.setColor(0.4f,0.4f,0.4f, 0.25f);
				shapeRenderer.line(centeredX+object.x, centeredY+object.y, centeredX+preObj.x, centeredY+preObj.y);
				shapeRenderer.line(centeredX+object.x, centeredY+object.y, centeredX+postObj.x, centeredY+postObj.y);
			}
			
			if(channel == -1) shapeRenderer.setColor(Color.BLACK);
			if(channel == 0) shapeRenderer.setColor(Color.LIME);
			if(channel == 1) shapeRenderer.setColor(Color.RED);
			int thick = 0;
			PVParam plastic = object.getParam("plasticityFlag");
			if(plastic != null && (Boolean)plastic.value == true) thick = 1;
			
			for(int i = -thick; i <= thick; i++)
			{
				for(int j = -thick; j <= thick; j++)
				{
					centeredX += i;
					centeredY += j;				
					if(preObj != null && postObj != null)
					{
						shapeRenderer.curve(	centeredX+preObj.x, centeredY+preObj.y,
												centeredX+(9*object.x+preObj.x)/10, centeredY+(9*object.y+preObj.y)/10,
												centeredX+(9*object.x+postObj.x)/10, centeredY+(9*object.y+postObj.y)/10,
												centeredX+postObj.x, centeredY+postObj.y, 16);
						
						{
							Vector2 direction = new Vector2(postObj.x-object.x, postObj.y-object.y).nor().scl(6);
							Vector2 L = direction.cpy().rotate(120);
							Vector2 R = direction.cpy().rotate(240);
							float xx = centeredX+(object.x+2*postObj.x)/3;
							float yy = centeredY+(object.y+2*postObj.y)/3;
							shapeRenderer.triangle(xx+direction.x, yy+direction.y, xx+L.x, yy+L.y, xx+R.x, yy+R.y);
						}
						{
							Vector2 direction = new Vector2(object.x-preObj.x, object.y-preObj.y).nor().scl(6);
							Vector2 L = direction.cpy().rotate(120);
							Vector2 R = direction.cpy().rotate(240);
							float xx = centeredX+(object.x+2*preObj.x)/3;
							float yy = centeredY+(object.y+2*preObj.y)/3;
							shapeRenderer.triangle(xx+direction.x, yy+direction.y, xx+L.x, yy+L.y, xx+R.x, yy+R.y);
						}
					}
					else
					{
						if(preObj != null)
						{
							shapeRenderer.line(centeredX+object.x, centeredY+object.y, centeredX+preObj.x, centeredY+preObj.y);
						}
						if(postObj != null)
						{
							shapeRenderer.line(centeredX+object.x, centeredY+object.y, centeredX+postObj.x, centeredY+postObj.y);
						}
					}
					centeredX -= i;
					centeredY -= j;
				}
			}
		}
		shapeRenderer.end();
		
		shapeRenderer.begin(ShapeType.Filled);
		for(PVObject object : network.getObjects())
		{
			shapeRenderer.setColor(object.color);
			switch(object.shape)
			{
				case Box:
					shapeRenderer.rect(centeredX+object.x - object.width/2, centeredY+object.y - object.height / 2, object.width, object.height);
					break;
				case Diamond:
					shapeRenderer.triangle(	centeredX+object.x - object.width/2, centeredY+object.y,
											centeredX+object.x + object.width/2, centeredY+object.y,
											centeredX+object.x, centeredY+object.y+object.height/2);
					shapeRenderer.triangle(	centeredX+object.x + object.width/2, centeredY+object.y,
											centeredX+object.x - object.width/2, centeredY+object.y,
											centeredX+object.x, centeredY+object.y-object.height/2);
					break;
				case Oval:
					shapeRenderer.ellipse(centeredX+object.x - object.width/2, centeredY+object.y - object.height / 2, object.width, object.height);
					break;
				case Triangle:
					shapeRenderer.triangle(	centeredX+object.x - object.width/2, centeredY+object.y-object.height/2,
											centeredX+object.x + object.width/2, centeredY+object.y-object.height/2,
											centeredX+object.x, centeredY+object.y+object.height/2);
					break;
			}
		}
		shapeRenderer.end();
		shapeRenderer.begin(ShapeType.Line);
		for(PVObject object : network.getObjects())
		{
			shapeRenderer.setColor(object.outline);
			int thick = 0;
			if(object == selected)
			{
				thick = 0;
			}
			for(int i = -thick; i <= thick; i++)
			{
				for(int j = -thick; j <= thick; j++)
				{
					centeredX += i * (float)(0.5f * Math.sin(delta) + 0.5f);
					centeredY += j * (float)(0.5f * Math.sin(delta) + 0.5f);
					if(object == selected)
					{
						shapeRenderer.setColor(1,1,1,1f);
					}
					switch(object.shape)
					{
						case Box:
							shapeRenderer.rect(centeredX+object.x - object.width/2, centeredY+object.y - object.height / 2, object.width, object.height);
							break;
						case Diamond:
							shapeRenderer.line(centeredX+object.x-object.width/2, centeredY+object.y, centeredX+object.x, centeredY+object.y+object.height/2);
							shapeRenderer.line(centeredX+object.x+object.width/2, centeredY+object.y, centeredX+object.x, centeredY+object.y+object.height/2);
							shapeRenderer.line(centeredX+object.x-object.width/2, centeredY+object.y, centeredX+object.x, centeredY+object.y-object.height/2);
							shapeRenderer.line(centeredX+object.x+object.width/2, centeredY+object.y, centeredX+object.x, centeredY+object.y-object.height/2);
							break;
						case Oval:
							shapeRenderer.ellipse(centeredX+object.x - object.width/2, centeredY+object.y - object.height / 2, object.width, object.height);
							break;
						case Triangle:
							shapeRenderer.triangle(	centeredX+object.x - object.width/2, centeredY+object.y-object.height/2,
													centeredX+object.x + object.width/2, centeredY+object.y-object.height/2,
													centeredX+object.x, centeredY+object.y+object.height/2);
						default:
							break;
					}
					centeredX -= i;
					centeredY -= j;
				}
			}
		}
		shapeRenderer.end();
		
		parent.getSpriteBatch().begin();
		for(PVObject object : network.getObjects())
		{
			if("Connection".equals(object.category) && object != selected) continue;
			parent.getSkin().getFont("default-font").draw(parent.getSpriteBatch(), object.name, centeredX+object.x - object.width/2 + 2 + object.text_offset, centeredY+object.y + object.height/4);
		}
		parent.getSpriteBatch().end();
		
		//Draw the UI
		stage.draw();

		//Draw above the UI
		parent.getSpriteBatch().begin();
		if(!"".equals(mouseText))
		{
			parent.getSkin().getFont("default-font").draw(parent.getSpriteBatch(), mouseText, Gdx.input.getX() + 2, Gdx.graphics.getHeight() - Gdx.input.getY());
		}
		parent.getSpriteBatch().end();
	}

	@Override
	public void destroy() { stage.dispose(); }
	
	public void setSelected(PVObject selected)
	{
		if(selected != null)
		{
			this.selected = selected;
		}
		else
		{
			this.selected = network.column;
		}
		paramsList.setItems(this.selected.params.toArray(new PVParam[0]));
		delta = 0f;
	}

}
