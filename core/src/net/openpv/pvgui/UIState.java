package net.openpv.pvgui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public abstract class UIState 
{
	protected ShapeRenderer shapeRenderer;
	protected Stage stage;
	protected PVGui parent;
	
	public void setParent(PVGui parent) { this.parent = parent; }
	public Stage getStage() { return stage; }
	
	protected void setupStage()
	{
		shapeRenderer = new ShapeRenderer();
		stage = new Stage(new ScreenViewport(parent.getCamera()), parent.getSpriteBatch());
		stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

		build(parent.getSkin());
	}
	
	public void resize(int windowWidth, int windowHeight)
	{
		stage.getViewport().update(windowWidth, windowHeight, true);
		stage.clear();
		build(parent.getSkin());
	}
	
	public abstract void init();
	public abstract void build(Skin uiSkin);  
	public abstract void update();
	public abstract void render();
	public abstract void destroy();
}
