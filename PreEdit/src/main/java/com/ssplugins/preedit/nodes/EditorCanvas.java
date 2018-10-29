package com.ssplugins.preedit.nodes;

import com.ssplugins.preedit.edit.Effect;
import com.ssplugins.preedit.edit.Module;
import com.ssplugins.preedit.edit.NodeModule;
import com.ssplugins.preedit.exceptions.SilentFailException;
import com.ssplugins.preedit.util.ExpandableBounds;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class EditorCanvas extends StackPane {
    
    private Pane posPane;
    private Pane bgPane;
    private ResizeHandle handle;
    
    private Canvas transparentLayer;
    private static Canvas debug;
    
    private ExpandableBounds viewport;
    
    public EditorCanvas(double width, double height) {
        this.prefWidthProperty().bind(this.minWidthProperty());
        this.prefHeightProperty().bind(this.minHeightProperty());
        transparentLayer = new Canvas(width, height);
        debug = new Canvas(width, height);
        debug.setMouseTransparent(true);
        setCanvasSize(width, height);
        bgPane = new Pane();
        this.getChildren().add(bgPane);
        posPane = new Pane();
        this.getChildren().add(posPane);
        handle = new ResizeHandle();
        posPane.getChildren().add(handle);
        posPane.getChildren().add(debug);
        bgPane.getChildren().add(transparentLayer);
    
        viewport = new ExpandableBounds(0, 0, width, height);
    }
    
    public static Canvas debugCanvas() {
        return debug;
    }
    
    public static GraphicsContext debugContext() {
        return debugCanvas().getGraphicsContext2D();
    }
    
    public static void clearDebug() {
        debugContext().clearRect(0, 0, debug.getWidth(), debug.getHeight());
    }
    
    public static void rotate(GraphicsContext context, double cx, double cy, double deg) {
        if (deg == 0) return;
        context.translate(cx, cy);
        context.rotate(deg);
        context.translate(-cx, -cy);
    }
    
    public ExpandableBounds getViewport() {
        return viewport;
    }
    
    public NodeHandle createNodeHandle() {
        return new NodeHandle(posPane);
    }
    
    public ResizeHandle getHandle() {
        return handle;
    }
    
    public ResizeHandle getHandleUnbound() {
        handle.unlink();
        handle.hide();
        handle.setDraggable(true);
        handle.hide();
        return getHandle();
    }
    
    public Canvas getTransparentLayer() {
        return transparentLayer;
    }
    
    public void setCanvasSize(double width, double height) {
        this.setMinWidth(width);
        this.setMinHeight(height);
        transparentLayer.setWidth(width);
        transparentLayer.setHeight(height);
        debug.setWidth(width);
        debug.setHeight(height);
    }
    
    public void addLayer() {
        newLayer();
    }
    
    public void removeLayer() {
        this.getChildren().stream().filter(node -> node instanceof PaneCanvas).findFirst().ifPresent(node -> this.getChildren().remove(node));
    }
    
    public void setLayerCount(int layers) {
        long count = this.getChildren().stream().filter(node -> node instanceof PaneCanvas).count();
        long d = layers - count;
        if (d > 0) {
            LongStream.range(0, d).forEach(value -> this.addLayer());
        }
        else if (d < 0) {
            LongStream.range(0, d).forEach(value -> this.removeLayer());
        }
    }
    
    public void clearAll() {
        this.getChildren().stream().filter(node -> node instanceof PaneCanvas).forEach(node -> {
            ((PaneCanvas) node).clearNode();
            clear(((PaneCanvas) node).getCanvas());
        });
        clear(getTransparentLayer());
    }
    
    public void fillTransparent() {
        GraphicsContext gc = transparentLayer.getGraphicsContext2D();
        gc.clearRect(0, 0, transparentLayer.getWidth(), transparentLayer.getHeight());
        for (int x = 0; x < transparentLayer.getWidth(); x += 5) {
            for (int y = 0; y < transparentLayer.getHeight(); y += 5) {
                Color c = ((x + y) % 2 == 0 ? Color.WHITE : Color.LIGHTGRAY);
                gc.setFill(c);
                gc.fillRect(x, y, 5, 5);
            }
        }
    }
    
    public void renderImage(boolean display, List<Module> modules, boolean editor) throws SilentFailException {
        clearAll();
        if (display) fillTransparent();
        else handle.hide();
        ListIterator<Module> it = modules.listIterator(modules.size());
        for (Node node : this.getChildren()) {
            if (!(node instanceof PaneCanvas)) continue;
            PaneCanvas paneCanvas = (PaneCanvas) node;
            paneCanvas.clearNode();
            if (!it.hasPrevious()) break;
            Module m = it.previous();
            Canvas canvas = paneCanvas.getCanvas();
            canvas.setEffect(null);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.save();
            if (m instanceof NodeModule) {
                Node n = ((NodeModule) m).getNode();
                n.setEffect(null);
                paneCanvas.setNode(n);
                renderEffects(m.getEffects(), canvas, gc, n, editor);
            }
            else {
                m.draw(canvas, gc, editor);
                renderEffects(m.getEffects(), canvas, gc, null, editor);
            }
            gc.restore();
        }
    }
    
    private void renderEffects(List<Effect> list, Canvas c, GraphicsContext context, Node node, boolean editor) throws SilentFailException {
        ListIterator<Effect> it = list.listIterator();
        while (it.hasNext()) {
            Effect e = it.next();
            e.reset();
            e.apply(c, context, node, editor);
        }
    }
    
    private PaneCanvas newLayer() {
        PaneCanvas c = new PaneCanvas(this.getMinWidth(), this.getMinHeight());
        c.minWidthProperty().bind(this.minWidthProperty());
        c.minHeightProperty().bind(this.minHeightProperty());
        this.getChildren().add(this.getChildren().size() - 1, c);
        return c;
    }
    
    private List<Canvas> getLayers() {
        return this.getChildren().stream().filter(node -> node instanceof PaneCanvas).map(node -> ((PaneCanvas) node).getCanvas()).collect(Collectors.toList());
    }
    
    private void clear(Canvas canvas) {
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
    
    public class NodeHandle {
        private Pane pane;
        private boolean added;
        
        private Node node;
        
        private NodeHandle(Pane pane) {
            this.pane = pane;
        }
        
        public Node getNode() {
            return node;
        }
        
        public void setNode(Node node) {
            boolean a = added;
            if (a) remove();
            this.node = node;
            if (a) add();
        }
        
        public void toggle(boolean add) {
            if (add) add();
            else remove();
        }
        
        public void add() {
            if (!added) {
                if (node == null) return;
                added = true;
                pane.getChildren().add(node);
            }
        }
        
        public void remove() {
            if (added) {
                added = false;
                pane.getChildren().remove(node);
            }
        }
        
    }
    
}
