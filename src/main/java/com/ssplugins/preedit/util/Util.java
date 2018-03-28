package com.ssplugins.preedit.util;

import com.ssplugins.preedit.exceptions.InvalidInputException;
import com.ssplugins.preedit.exceptions.SilentFailException;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Effect;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Util {
	
	public static void runFXSafe(Runnable runnable) {
		if (Platform.isFxApplicationThread()) runnable.run();
		else Platform.runLater(runnable);
	}
	
	public static <T> Optional<T> runFXSafe(Callable<T> callable) {
		FutureTask<T> task = new FutureTask<>(callable);
		runFXSafe(task);
		try {
			return Optional.ofNullable(task.get());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}
	
	public static <T> Optional<T> runFXSafeFlat(Callable<Optional<T>> callable) {
		return runFXSafe(callable).flatMap(Function.identity());
	}
	
	public static void log(String msg) {
		System.out.println(msg);
	}
	
	public static String exceptionMessage(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}
	
	public static Supplier<InvalidInputException> invalidInput() {
		return InvalidInputException::new;
	}
	
	public static Supplier<SilentFailException> silentFail() {
		return SilentFailException::new;
	}
	
	public static Border border(Color color) {
		return new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT));
	}
	
}
