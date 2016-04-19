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

public class SaveState extends UIState
{
	String path;
	TextField newValue;
	PVNetwork network;
	public SaveState(String path, PVNetwork network) { this.path = path; this.network = network; }
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
		stage.addActor(root);
		root.setBackground(uiSkin.getDrawable("dialogDim"));
		root.addAction(Actions.sequence(Actions.alpha(0f), Actions.fadeIn(0.25f, Interpolation.exp5Out)));

		Table dialog = new Table();
		dialog.pad(8);
		dialog.setBackground(uiSkin.getDrawable("default-pane"));
		root.row().expand().fill();
		
		root.add(dialog).maxHeight(stage.getHeight()/3).maxWidth(stage.getWidth()/2).minWidth(stage.getWidth()/3).minHeight(stage.getHeight()/4);
		
		dialog.row().pad(8);
		dialog.add(new Label("Save project", uiSkin)).expandX().fillX().colspan(2);
		dialog.row().pad(8);
		dialog.add(new Label("Working directory: " + Gdx.files.getLocalStoragePath(), uiSkin)).expandX().fillX().left().colspan(2);
		dialog.row().pad(8);
		dialog.add(newValue = new TextField(path, uiSkin)).expandX().fillX().left().colspan(2);
		dialog.row().pad(16);
		TextButton cancel = new TextButton("Cancel", uiSkin);
		TextButton confirm = new TextButton("Save", uiSkin);
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
		newValue.validate();
		FileHandle f = new FileHandle(path);
		f.writeString(network.toJsonString(true), false);
		parent.message("Saved " + newValue.getText());
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
