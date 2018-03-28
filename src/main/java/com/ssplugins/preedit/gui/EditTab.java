package com.ssplugins.preedit.gui;

import com.ssplugins.preedit.PreEdit;
import com.ssplugins.preedit.edit.Catalog;
import com.ssplugins.preedit.edit.Effect;
import com.ssplugins.preedit.edit.Module;
import com.ssplugins.preedit.edit.Template;
import com.ssplugins.preedit.exceptions.SilentFailException;
import com.ssplugins.preedit.input.InputMap;
import com.ssplugins.preedit.nodes.EditorCanvas;
import com.ssplugins.preedit.nodes.UserInput;
import com.ssplugins.preedit.util.Dialogs;
import com.ssplugins.preedit.util.State;
import com.ssplugins.preedit.util.TemplateInfo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Optional;

public class EditTab extends BorderPane {
	
	//
	private final Insets PADDING = new Insets(10);
	private final int CANVAS_MIN = 400;
	
	private Stage stage;
	private State state;

	private ToolBar toolbar;
	private ComboBox<String> selector;
	private Button btnNew;
	private Button btnSave;
	
	private ScrollPane canvasArea;
	private EditorCanvas canvas;
	
	private GridPane controls;
	private Label labelLayers;
	private ListView<Module> layers;
	private VBox layerButtons;
	private Button addLayer;
	private Button removeLayer;
	private Button layerUp;
	private Button layerDown;
	private Label labelEffects;
	private ListView<Effect> effects;
	private VBox effectButtons;
	private Button addEffect;
	private Button removeEffect;
	private Button effectUp;
	private Button effectDown;
	
	private ScrollPane paramContainer;
	private FlowPane paramArea;
	
	public EditTab(Stage stage) {
		this.stage = stage;
		state = new State();
		state.setRenderCall(() -> {
			try {
				canvas.renderImage(true, layers.getItems());
			} catch (SilentFailException ignored) {
			}
		});
		stage.sceneProperty().addListener((observable, oldValue, newValue) -> {
			newValue.focusOwnerProperty().addListener((observable1, oldNode, newNode) -> {
				if (newNode == layers) {
					Module m = layers.getSelectionModel().getSelectedItem();
					if (m == null) return;
					m.linkResizeHandle(canvas.getHandle());
					setInputs(m.getInputs());
				}
				else if (newNode == effects) {
					Effect e = effects.getSelectionModel().getSelectedItem();
					if (e == null) return;
					canvas.getHandle().hide();
					setInputs(e.getInputs());
				}
			});
		});
		defineNodes();
		Platform.runLater(() -> {
			selector.setItems(FXCollections.observableArrayList(PreEdit.getCatalog().getTemplates()));
		});
	}
	
	private void defineNodes() {
		toolbar = new ToolBar();
		this.setTop(toolbar);
		//
		selector = new ComboBox<>();
		selector.setPromptText("<select template>");
		selector.valueProperty().addListener((observable, oldValue, newValue) -> {
			// load new template
		});
		toolbar.getItems().add(selector);
		//
		btnNew = new Button("New");
		btnNew.setOnAction(event -> {
			Optional<TemplateInfo> op = Dialogs.newTemplate(null);
			op.ifPresent(info -> {
				Catalog catalog = PreEdit.getCatalog();
				if (catalog.templateExists(info.getName())) {
					Dialogs.show("Template already exists.", null, AlertType.INFORMATION);
					return;
				}
				resetNodes();
				Template template = catalog.newTemplate(info.getName(), info.getWidth(), info.getHeight());
				state.templateProperty().set(template);
				selector.getItems().add(template.getName());
				selector.setValue(template.getName());
			});
		});
		toolbar.getItems().add(btnNew);
		//
		btnSave = new Button("Save");
		btnSave.setDisable(true);
		btnSave.disableProperty().bind(state.savedProperty());
		toolbar.getItems().add(btnSave);
		//
		canvasArea = new ScrollPane();
		canvasArea.setMinWidth(CANVAS_MIN);
		canvasArea.setMinHeight(CANVAS_MIN);
		canvasArea.setMinViewportWidth(CANVAS_MIN);
		canvasArea.setMinViewportHeight(CANVAS_MIN);
		canvasArea.setFitToWidth(true);
		canvasArea.setFitToHeight(true);
		BorderPane.setMargin(canvasArea, new Insets(10, 0, 10, 10));
		this.setCenter(canvasArea);
		//
		GridPane canvasPane = new GridPane();
		canvasPane.setAlignment(Pos.CENTER);
		canvasPane.prefWidthProperty().bind(canvasArea.widthProperty());
		canvasPane.prefHeightProperty().bind(canvasArea.heightProperty());
		canvasArea.setContent(canvasPane);
		//
		canvas = new EditorCanvas(CANVAS_MIN, CANVAS_MIN);
		state.templateProperty().addListener((observable, oldValue, newValue) -> {
			canvas.clearAll();
			canvas.setCanvasSize(newValue.getWidth(), newValue.getHeight());
			layers.setItems(newValue.getModules());
			addLayer.setDisable(false);
			state.render();
		});
//		canvasArea.setContent(canvas);
		canvasPane.add(canvas, 0, 0);
		//
		controls = new GridPane();
		controls.setPadding(PADDING);
		controls.setHgap(5);
		controls.setVgap(3);
		this.setRight(controls);
		//
		labelLayers = new Label("Layers:");
		controls.add(labelLayers, 0, 0);
		//
		layers = new ListView<>();
		layers.setPrefWidth(150);
		layers.setPrefHeight(200);
		layers.setCellFactory(Module.getCellFactory());
		layers.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null) return; // TODO disable anything when no item
			newValue.linkResizeHandle(canvas.getHandle());
			effects.setItems(newValue.getEffects());
			addEffect.setDisable(false);
			setInputs(newValue.getInputs());
		});
		controls.add(layers, 0, 1);
		//
		layerButtons = new VBox(3);
		controls.add(layerButtons, 1, 1);
		//
		addLayer = smallButton("+");
		addLayer.setDisable(true);
		addLayer.setOnAction(event -> {
			Optional<String> op = Dialogs.choose("Choose a module to add:", null, PreEdit.getCatalog().getModules());
			op.flatMap(PreEdit.getCatalog()::createModule).ifPresent(module -> {
				layers.getItems().add(module);
				state.render();
			});
		});
		layerButtons.getChildren().add(addLayer);
		//
		removeLayer = smallButton("-");
		removeLayer.setDisable(true);
		layerButtons.getChildren().add(removeLayer);
		//
		layerUp = smallButton("^");
		layerUp.setDisable(true);
		layerButtons.getChildren().add(layerUp);
		//
		layerDown = smallButton("v");
		layerDown.setDisable(true);
		layerButtons.getChildren().add(layerDown);
		//
		labelEffects = new Label("Effects:");
		controls.add(labelEffects, 2, 0);
		//
		effects = new ListView<>();
		effects.setPrefWidth(150);
		effects.setPrefHeight(200);
		effects.setCellFactory(Effect.getCellFactory());
		effects.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null) return;
			canvas.getHandle().hide();
			setInputs(newValue.getInputs());
		});
		controls.add(effects, 2, 1);
		//
		effectButtons = new VBox(3);
		controls.add(effectButtons, 3, 1);
		//
		addEffect = smallButton("+");
		addEffect.setDisable(true);
		addEffect.setOnAction(event -> {
			Optional<String> op = Dialogs.choose("Choose an effect to add:", null, PreEdit.getCatalog().getEffects());
			op.flatMap(PreEdit.getCatalog()::createEffect).ifPresent(effect -> {
				effects.getItems().add(effect);
				state.render();
			});
		});
		effectButtons.getChildren().add(addEffect);
		//
		removeEffect = smallButton("-");
		removeEffect.setDisable(true);
		effectButtons.getChildren().add(removeEffect);
		//
		effectUp = smallButton("^");
		effectUp.setDisable(true);
		effectButtons.getChildren().add(effectUp);
		//
		effectDown = smallButton("v");
		effectDown.setDisable(true);
		effectButtons.getChildren().add(effectDown);
		//
		paramContainer = new ScrollPane();
		paramContainer.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
		paramContainer.setMinViewportHeight(150);
		paramContainer.setFitToWidth(true);
		paramContainer.setFitToHeight(true);
		GridPane.setVgrow(paramContainer, Priority.ALWAYS);
		controls.add(paramContainer, 0, 2, 4, 1);
		//
		paramArea = new FlowPane(10, 10);
		paramArea.setMaxWidth(paramContainer.getViewportBounds().getWidth());
		paramArea.prefWrapLengthProperty().bind(paramArea.maxWidthProperty());
		paramContainer.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
			paramArea.setMaxWidth(newValue.getWidth());
		});
		StackPane.setMargin(paramArea, PADDING);
		paramContainer.setContent(paramArea);
		//
	}
	
	private Button smallButton(String text) {
		Button button = new Button(text);
		button.setMinWidth(25);
		button.setMinHeight(25);
		return button;
	}
	
	private void resetNodes() {
		state.savedProperty().set(true);
		canvas.clearAll();
		layers.getItems().clear();
		addLayer.setDisable(true);
		removeLayer.setDisable(true);
		layerUp.setDisable(true);
		layerDown.setDisable(true);
		effects.getItems().clear();
		addEffect.setDisable(true);
		removeEffect.setDisable(true);
		effectUp.setDisable(true);
		effectDown.setDisable(true);
		paramArea.getChildren().clear();
	}
	
	private void setInputs(InputMap map) {
		paramArea.getChildren().clear();
		map.getInputs().forEach((s, input) -> {
			input.setUpdateTrigger(state::render);
			UserInput displayNode = input.getDisplayNode();
			displayNode.update(s);
			paramArea.getChildren().add(displayNode);
		});
	}
	
}