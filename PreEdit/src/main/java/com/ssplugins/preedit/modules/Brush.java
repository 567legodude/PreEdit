package com.ssplugins.preedit.modules;

import com.ssplugins.preedit.edit.CanvasLayer;
import com.ssplugins.preedit.edit.Module;
import com.ssplugins.preedit.exceptions.SilentFailException;
import com.ssplugins.preedit.gui.EditorTab;
import com.ssplugins.preedit.input.*;
import com.ssplugins.preedit.nodes.ResizeHandle;
import com.ssplugins.preedit.util.Util;
import com.ssplugins.preedit.util.data.Range;
import com.ssplugins.preedit.util.wrapper.SafePixelWriter;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class Brush extends Module {
    
    private ObjectProperty<WritableImage> image;
    private ObjectProperty<Mode> mode;
    
    private boolean circle;
    private IntegerProperty cx, cy;
    
    @Override
    protected void preload() {
        image = new SimpleObjectProperty<>();
        mode = new SimpleObjectProperty<>(Mode.DRAW);
        cx = new SimpleIntegerProperty();
        cy = new SimpleIntegerProperty();
    }
    
    @Override
    public void onMouseEvent(MouseEvent event, boolean editor) {
        EventType<? extends MouseEvent> type = event.getEventType();
        cx.set((int) event.getX());
        cy.set((int) event.getY());
        if (type == MouseEvent.MOUSE_CLICKED || type == MouseEvent.MOUSE_DRAGGED) {
            try {
                if (!editor) return;
                int size = getInputs().getValue("Size", NumberInput.class).intValue();
                circle((int) event.getX(), (int) event.getY(), size, true);
            } catch (SilentFailException ignored) {
            }
        }
        getInputs().getInput("hidden", HiddenInput.class).ifPresent(HiddenInput::callUpdate);
    }
    
    @Override
    public void onSelectionChange(EditorTab tab, boolean selected) {
        circle = selected;
    }
    
    private void circle(int cx, int cy, int radius, boolean fill) throws SilentFailException {
        if (image.get() == null) return;
        int x = radius - 1;
        int y = 0;
        int dx = 1;
        int dy = 1;
        int err = dx - (radius << 1);
        SafePixelWriter writer = new SafePixelWriter(image.get());
        Color c = null;
        if (mode.get() == Mode.DRAW) {
            c = getInputs().getValue("Color", ColorInput.class);
        }
        else if (mode.get() == Mode.ERASE) {
            c = Color.TRANSPARENT;
        }
        while (x >= y) {
            if (fill) {
                for (int i = cx - x; i <= cx + x; i++) {
                    writer.setColor(i, cy + y, c);
                    writer.setColor(i, cy - y, c);
                }
                for (int i = cx - y; i <= cx + y; i++) {
                    writer.setColor(i, cy + x, c);
                    writer.setColor(i, cy - x, c);
                }
            }
            else {
                writer.setColor(cx + x, cy + y, c);
                writer.setColor(cx + y, cy + x, c);
                writer.setColor(cx - y, cy + x, c);
                writer.setColor(cx - x, cy + y, c);
                writer.setColor(cx - x, cy - y, c);
                writer.setColor(cx - y, cy - x, c);
                writer.setColor(cx + y, cy - x, c);
                writer.setColor(cx + x, cy - y, c);
            }
            if (err <= 0) {
                y++;
                err += dy;
                dy += 2;
            }
            if (err > 0) {
                x--;
                dx += 2;
                err += dx - (radius << 1);
            }
        }
    }
    
    @Override
    public void linkResizeHandle(ResizeHandle handle) {
        handle.hide();
    }
    
    @Override
    public ObservableValue<Bounds> getBounds() {
        WritableImage img = image.get();
        if (img == null) return new SimpleObjectProperty<>(new BoundingBox(0, 0, 0, 0));
        return new SimpleObjectProperty<>(new BoundingBox(0, 0, img.getWidth(), img.getHeight()));
    }
    
    @Override
    public String getName() {
        return "Brush";
    }
    
    @Override
    public void draw(CanvasLayer canvasLayer, boolean editor) throws SilentFailException {
        Canvas canvas = canvasLayer.getCanvas();
        GraphicsContext context = canvasLayer.getGraphics();
        if (image.get() == null) {
            image.set(new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight()));
        }
        context.drawImage(image.get(), 0, 0);
        if (circle) {
            if (!editor) return;
            int size = getInputs().getValue("Size", NumberInput.class).intValue();
            int d = size * 2;
            context.setFill(Color.GRAY);
            context.strokeRoundRect(cx.get() - size, cy.get() - size, d, d, d, d);
        }
    }
    
    @Override
    protected void defineInputs(InputMap map) {
        ChoiceInput<Mode> mode = new ChoiceInput<>(Mode.values(), Mode.DRAW, Util.enumConverter(Mode.class));
        mode.valueProperty().bindBidirectional(this.mode);
        map.addInput("Mode", mode);
        NumberInput size = new NumberInput(false);
        size.setRange(Range.lowerBound(1));
        size.setValue(10);
        map.addInput("Size", size);
        map.addInput("Color", new ColorInput());
        map.addInput("hidden", new HiddenInput());
        DataInput<WritableImage> imageData = new DataInput<>(Util.imageConverter());
        imageData.valueProperty().bindBidirectional(image);
        map.addInput("data", imageData);
    }
    
    private enum Mode {
        DRAW, ERASE;
    }
    
}
