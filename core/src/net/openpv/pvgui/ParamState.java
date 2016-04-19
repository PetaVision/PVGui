package net.openpv.pvgui;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class ParamState extends UIState
{
	@SuppressWarnings("rawtypes")
	private PVParam param = null;
	private PVObject object = null;
	private TextField newValue;
	
	@SuppressWarnings("rawtypes")
	public ParamState(PVObject object, PVParam param) { this.param = param; this.object = object; }
	
	@Override
	public void init()
	{
		setupStage();
	}

	@Override
	public void build(Skin uiSkin)
	{
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
		dialog.add(new Label("Object Name: " + object.name, uiSkin)).expandX().fillX().left();
		dialog.add(new Label("Object Type: " + object.type + " (" + object.category + ")", uiSkin)).expandX().fillX().right();
		dialog.row().pad(8);
		dialog.add(new Label("Param Value: " + param.value.toString(), uiSkin)).expandX().fillX().left();
		dialog.add(new Label("Param Type: " + param.type.toString(), uiSkin)).expandX().fillX().right();
		dialog.row().pad(8);
		dialog.add(new Label("Enter new value for '" + param.name + "':", uiSkin)).expandX().fillX().left();
		dialog.add(newValue = new TextField(param.value.toString(), uiSkin)).expandX().fillX().right();
		dialog.row().pad(16);
		TextButton cancel = new TextButton("Cancel", uiSkin);
		TextButton confirm = new TextButton("Confirm", uiSkin);
		dialog.add(cancel).expandX().fillX().right().minHeight(24);
		dialog.add(confirm).expandX().fillX().left().minHeight(24);

		//TODO: Sliders and data-type specific controls?
		
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
		String val = newValue.getText();
    	try
    	{
    		switch(param.type)
    		{
				case booleanValue:
					param.value = Boolean.parseBoolean(val);
					break;
				case doubleValue:
					param.value = Double.parseDouble(val);
					break;
				case longValue:
					param.value = Long.parseLong(val);
					break;
				case stringValue:
					param.value = val;
					break;
				default:
					break;
    		}
    	}
    	catch(Exception e)
    	{
    		System.err.println("Invalid entry, parameter " + param.name + " has not been changed.");
    	}
        parent.popState();
	}

	@Override
	public void update()
	{
		stage.act();
		if(Gdx.input.isKeyJustPressed(Keys.ENTER)) confirm();
		if(Gdx.input.isKeyJustPressed(Keys.ESCAPE)) parent.popState();
	}
	@Override
	public void render() { stage.draw(); } 
	@Override
	public void destroy() { stage.dispose(); }

}
