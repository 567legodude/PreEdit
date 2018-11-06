package com.ssplugins.preedit.modules;

import com.ssplugins.preedit.edit.MultiNodeModule;
import com.ssplugins.preedit.exceptions.SilentFailException;
import com.ssplugins.preedit.input.InputMap;
import com.ssplugins.preedit.input.LocationInput;
import com.ssplugins.preedit.input.NumberInput;
import com.ssplugins.preedit.nodes.ResizeHandle;
import com.ssplugins.preedit.util.ExpandableBounds;
import com.ssplugins.preedit.util.Range;
import com.ssplugins.preedit.util.Util;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class TopText extends MultiNodeModule {
    
    private Solid solid;
    private TextModule text;
    
    private IntegerProperty totalHeight;
    
    @Override
    protected void preload() {
        totalHeight = new SimpleIntegerProperty();
        
        solid = new Solid();
        text = new TextModule();
    }
    
    @Override
    public void addNodes() {
        text.getFontFamily().setValue("Impact");
        text.getTextAlignment().setValue(TextAlignment.CENTER);
        addModule(solid);
        addModule(text);
    }
    
    @Override
    public void requestExpansion(ExpandableBounds viewport) throws SilentFailException {
        Text text = (Text) this.text.getNode();
        int padding = getInputs().getValue("Padding", NumberInput.class).intValue();
        int height = (int) text.getLayoutBounds().getHeight();
        int width = (int) viewport.getWidth() - padding * 2;
        totalHeight.set(height + padding * 2);
        this.text.getInputs().getInput("Location", LocationInput.class).ifPresent(locationInput -> {
            locationInput.xProperty().set(padding);
            locationInput.yProperty().set(-(height + padding));
        });
        this.text.getInputs().setValue("Wrap Width", NumberInput.class, width);
        solid.getInputs().getInput("Location", LocationInput.class).ifPresent(locationInput -> {
            locationInput.yProperty().set(-totalHeight.get());
            locationInput.widthProperty().set((int) viewport.getWidth());
            locationInput.heightProperty().set(totalHeight.get());
        });
        viewport.expand(totalHeight.get(), 0, 0, 0);
    }
    
    @Override
    public void linkResizeHandle(ResizeHandle handle) {
        handle.link(solid.getBounds());
        handle.layoutXProperty().set(0);
        handle.link(ResizeHandle.HandleProperty.Y, Util.bindingToProperty(totalHeight.negate()));
        handle.setDraggable(false);
        handle.setSizeable(false);
        handle.setSpinnable(false);
    }
    
    @Override
    public ObservableValue<Bounds> getBounds() {
        return solid.getBounds();
    }
    
    @Override
    public String getName() {
        return "TopText";
    }
    
    @Override
    protected void defineInputs(InputMap map) {
        map.copyInputs(text.getInputs(), "Text", "Placeholders", "Alignment", "Wrap Width", "Location");
        map.copyInputs(solid.getInputs(), "Background", "Location");
        NumberInput padding = new NumberInput(false);
        padding.setRange(Range.lowerBound(0));
        padding.setValue(5);
        map.addInput("Padding", padding);
    }
    
}