package app;

import java.util.List;

import app.communication.EventQueues;
import app.communication.Msg;
import app.communication.MsgToUIType;
import app.mapping.InstrumentMapping;
import app.ui.App;
import dataRepo.DataRepo;
import dataRepo.Price;
import dataRepo.Sonifiable;
import dataRepo.DataRepo.FilterFlag;
import dataRepo.DataRepo.IntervalLength;
import javafx.application.Application;
import audio.synth.InstrumentEnum;

// This class runs in the main thread and coordinates all tasks and the creation of the UI thread
// This is atypical, as JavaFX's UI thread is usually the main thread as well
// (see: https://stackoverflow.com/a/37580083/13764271)
// however, it makes conceptually more sense, as the app's logic should be done in the main thread

public class StateManager {
	// THis mapping assumes that each instrument can only be mapped exactly once
	public static InstrumentMapping[] mapping = new InstrumentMapping[InstrumentEnum.size];

	public static void main(String[] args) {
		testUI(args);
	}

	public static void testUI(String[] args) {
		Thread th = new Thread(() -> Application.launch(App.class, args));
		th.start();

		try {
			call(() -> DataRepo.init());

			List<Sonifiable> l = DataRepo.getAll(FilterFlag.ALL);
			l = l.subList(0, Math.min(l.size(), 10));
			if (!l.isEmpty()) {
				for (Sonifiable x : l)
					System.out.println(x);
				Sonifiable x = l.get(0);
				List<Price> prices = DataRepo.getPrices(x, x.getEarliest(), x.getLatest(),
						IntervalLength.DAY);
				for (Price p : prices.subList(0, Math.min(prices.size(), 10)))
					System.out.println(p);
				EventQueues.toUI.put(new Msg<>(MsgToUIType.FILTERED_SONIFIABLES, l));
			}
		} catch (InterruptedException e) {
			// TODO: What should we do here?
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				EventQueues.toUI.put(new Msg<>(MsgToUIType.ERROR, "Internal Error."));
			} catch (InterruptedException e2) {
				// TODO: DO same as above
				e2.printStackTrace();
				System.exit(1);
			}
		}
	}

	public static <T> T call(AppSupplier<T> func, T alternative) throws InterruptedException {
		try {
			return func.call();
		} catch (AppError e) {
			EventQueues.toUI.put(new Msg<>(MsgToUIType.ERROR, e.getMessage()));
			return alternative;
		}
	}

	public static void call(AppFunction func) throws InterruptedException {
		try {
			func.call();
		} catch (AppError e) {
			EventQueues.toUI.put(new Msg<>(MsgToUIType.ERROR, e.getMessage()));
		}
	}

	public static void testSound(String[] args) {
		// DataRepo.init();

		// List<Sonifiable> data = DataRepo.getAll(FilterFlag.ALL);
		// TODO: Call functions in Harmonizer
	}
}
