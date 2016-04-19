package net.openpv.pvgui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class OpenState extends UIState
{
	String path;
	TextField newValue;
	PVNetwork network;
	MainState mainState;
	
	public OpenState(String path, PVNetwork network, MainState mainState)
	{
		this.path = path;
		this.network = network;
		this.mainState = mainState;
	}
	@Override
	public void init()
	{
		setupStage();
	}

	@Override
	public void build(Skin uiSkin)
	{
		//ListFileChooser temp;
		Table root = new Table();
		root.setFillParent(true);
		root.addAction(Actions.sequence(Actions.alpha(0f), Actions.fadeIn(0.25f, Interpolation.exp5Out)));
		stage.addActor(root);
		root.setBackground(uiSkin.getDrawable("dialogDim"));
		
		Table dialog = new Table();
		dialog.pad(8);
		dialog.setBackground(uiSkin.getDrawable("default-pane"));
		root.row().expand().fill();
		
		root.add(dialog).maxHeight(stage.getHeight()/3).maxWidth(stage.getWidth()/2).minWidth(stage.getWidth()/3).minHeight(stage.getHeight()/4);
		
		dialog.row().pad(8);
		dialog.add(new Label("Open project", uiSkin)).expandX().fillX().colspan(2);
		dialog.row().pad(8);
		dialog.add(new Label("Working directory: " + Gdx.files.getLocalStoragePath(), uiSkin)).expandX().fillX().left().colspan(2);
		dialog.row().pad(8);
		dialog.add(newValue = new TextField(path, uiSkin)).expandX().fillX().left().colspan(2);
		dialog.row().pad(16);
		TextButton cancel = new TextButton("Cancel", uiSkin);
		TextButton confirm = new TextButton("Open", uiSkin);
		dialog.add(cancel).expandX().fillX().right().minHeight(24);
		dialog.add(confirm).expandX().fillX().left().minHeight(24);

		cancel.addListener(new ChangeListener() {
	        public void changed (ChangeEvent event, Actor actor) {
	            parent.popState();
	        }
	    });
		
		confirm.addListener(new ChangeListener() {
	        public void changed (ChangeEvent event, Actor actor) {
	        	confirm();
	        }
	    });
		
		stage.setKeyboardFocus(newValue);
	}

	private void confirm()
	{
		PVNetwork newNetwork;
		try
		{
			newNetwork = PVNetwork.buildFromJson(new FileHandle(newValue.getText()).readString());
			parent.undoable("Opened " + newValue.getText(), network);
			mainState.setNetwork(newNetwork);
		}
		catch(Exception e) { System.err.println("Failed to open " + newValue.getText() + ": " + e); }
        parent.popState();
	}
	
	@Override
	public void update()
	{
		stage.act();
		if(Gdx.input.isKeyJustPressed(Keys.ESCAPE)) parent.popState();
		if(Gdx.input.isKeyJustPressed(Keys.ENTER)) confirm();
	}
	@Override
	public void render() { stage.draw(); } 
	@Override
	public void destroy() { stage.dispose(); }

}
