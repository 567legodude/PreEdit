package com.ssplugins.preedit.input;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ssplugins.preedit.exceptions.InvalidInputException;
import com.ssplugins.preedit.util.GridMap;
import com.ssplugins.preedit.util.JsonConverter;
import com.ssplugins.preedit.util.Util;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;

public class LocationInput extends Input<GridMap, LocationInput.Region> {
	
	private int x, y, width, height;
	
	public LocationInput() {
		this(0, 0, 100, 100);
	}
	
	public LocationInput(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.ready();
	}
	
	@Override
	protected void setNodeValue(GridMap node, Region value) {
		node.get("x", TextField.class).ifPresent(field -> field.setText(String.valueOf(value.getX())));
		node.get("y", TextField.class).ifPresent(field -> field.setText(String.valueOf(value.getY())));
		node.get("width", TextField.class).ifPresent(field -> field.setText(String.valueOf(value.getWidth())));
		node.get("height", TextField.class).ifPresent(field -> field.setText(String.valueOf(value.getHeight())));
	}
	
	@Override
	protected void setUpdateTrigger(GridMap node, Runnable update) {
		node.get("x", TextField.class).ifPresent(textField -> textField.textProperty().addListener(observable -> update.run()));
		node.get("y", TextField.class).ifPresent(textField -> textField.textProperty().addListener(observable -> update.run()));
		node.get("width", TextField.class).ifPresent(textField -> textField.textProperty().addListener(observable -> update.run()));
		node.get("height", TextField.class).ifPresent(textField -> textField.textProperty().addListener(observable -> update.run()));
	}
	
	@Override
	protected JsonConverter<Region> getJsonConverter() {
		return new JsonConverter<Region>() {
			@Override
			public JsonElement toJson(Region region) {
				JsonObject out = new JsonObject();
				out.addProperty("x", region.getX());
				out.addProperty("y", region.getY());
				out.addProperty("width", region.getWidth());
				out.addProperty("height", region.getHeight());
				return out;
			}
			
			@Override
			public Region fromJson(JsonElement element) {
				JsonObject json = element.getAsJsonObject();
				return new Region(json.get("x").getAsInt(),
								  json.get("y").getAsInt(),
								  json.get("width").getAsInt(),
								  json.get("height").getAsInt());
			}
		};
	}
	
	@Override
	protected GridMap createInputNode() {
		GridMap map = new GridMap();
		map.add(null, 0, 0, new Label("x:"));
		map.add("x", 0, 1, new TextField(String.valueOf(x)));
		map.add(null, 0, 2, new Label("y:"));
		map.add("y", 0, 3, new TextField(String.valueOf(y)));
		map.add(null, 1, 0, new Label("width:"));
		map.add("width", 1, 1, new TextField(String.valueOf(width)));
		map.add(null, 1, 2, new Label("height:"));
		map.add("height", 1, 3, new TextField(String.valueOf(height)));
		return map;
	}
	
	@Override
	protected Region getNodeValue(GridMap node) throws InvalidInputException {
		int x = node.get("x", TextField.class).map(TextInputControl::getText).map(Integer::parseInt).orElseThrow(Util.invalidInput());
		int y = node.get("y", TextField.class).map(TextInputControl::getText).map(Integer::parseInt).orElseThrow(Util.invalidInput());
		int width = node.get("width", TextField.class).map(TextInputControl::getText).map(Integer::parseInt).orElseThrow(Util.invalidInput());
		int height = node.get("height", TextField.class).map(TextInputControl::getText).map(Integer::parseInt).orElseThrow(Util.invalidInput());
		return new Region(x, y, width, height);
	}
	
	@Override
	protected boolean isValid(Region value) {
		return true;
	}
	
	public static class Region {
		
		private int x, y, width, height;
		
		public Region(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
		
		public int getX() {
			return x;
		}
		
		public int getY() {
			return y;
		}
		
		public int getWidth() {
			return width;
		}
		
		public int getHeight() {
			return height;
		}
	}
	
}
