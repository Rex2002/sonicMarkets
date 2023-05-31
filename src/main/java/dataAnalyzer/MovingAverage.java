package dataAnalyzer;

import java.util.ArrayList;
import java.util.List;

import dataRepo.Price;

public class MovingAverage implements TrendAnalyzer {
	@Override
	public double[] calculateMovingAverage(List<Price> priceList) {
		// Implementiere die Berechnung des gleitenden Durchschnitts (Moving Average)
		// Gib den Durchschnitt zu jedem Zeitpunkt als Liste von Double-Wert zurück

		double[] priceAverages = new double[priceList.size()];
		double[] previousPrices = new double[200];

		for (int i = 0; i < priceList.size(); i++) {
			Price price = priceList.get(i);
			previousPrices[i
					% previousPrices.length] = (price.getLow() + price.getHigh() + price.getOpen() + price.getClose())
							/ 4;

			priceAverages[i] = calculateAverage(previousPrices);
		}

		return priceAverages;
	}

	private double calculateAverage(double[] prices) {
		double sum = 0.0;
		for (double price : prices) {
			sum += price;
		}
		return sum / prices.length;
	}

	@Override
	public boolean[] AverageIntersectsStock(double[] priceAverages, List<Price> prices) {
		boolean[] intersections = new boolean[priceAverages.length];

		// Implementiert eine Funktion welche Testet, ob der Moving Average für einen
		// Zeitpunkt gleich dem Stockpreis ist
		// Gibt die Ergebnise als Liste von Booleans zurück

		for (int i = 0; i <= intersections.length; i++) {
			intersections[i] = prices.get(i).getLow() <= priceAverages[i]
					&& priceAverages[i] <= prices.get(i).getHigh();
		}

		return intersections;
	}

	@Override
	public List<Double> calculateMinAndMax(List<Price> priceList) {
		List<Double> minMax = new ArrayList<>();
		// Implementiert die Erkennung von lokalen Minima/Maxima
		// Vergleicht alle gegebenen Min/Max Werte
		// Gibt die jeweils höchsten(/niedrigsten) Werte als Liste von doubles aus
		// Maximum gesamt = Widerstand
		// Minimum gesamt = Unterstützung
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;

		for (Price price : priceList) {
			if (price.getLow() < min) {
				min = price.getLow();
			}

			if (price.getHigh() > max) {
				max = price.getHigh();
			}
		}
		minMax.add(min);
		minMax.add(max);
		return minMax;
	}

	@Override
	public List<Boolean> GraphIntersectsMinMax(List<Price> priceList, List<Double> minMax) {
		List<Boolean> intersectsminMax = new ArrayList<>();
		double min = minMax.get(0);
		double max = minMax.get(1);
		for (int i = 0; i <= priceList.size(); i++) {
			double graphLow = priceList.get(i).getLow();
			double graphHigh = priceList.get(i).getHigh();
			double graphOpen = priceList.get(i).getOpen();
			double graphClose = priceList.get(i).getClose();
			if (min == graphLow || min == graphClose || min == graphOpen || max == graphHigh || max == graphClose
					|| max == graphOpen) {
				boolean intersects = true;
				intersectsminMax.add(intersects);
			}
		}
		return intersectsminMax;
	}
}