package dataRepo;

import app.AppError;
import dataRepo.api.APIErr;
import dataRepo.api.APIReq;
import dataRepo.api.AuthPolicy;
import dataRepo.api.PaginationHandler;
import dataRepo.json.JsonPrimitive;
import dataRepo.json.Parser;
import util.ArrayFunctions;
import util.DateUtil;
import util.FutureList;
import util.UnorderedList;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class DataRepo {
	private static final boolean GET_PRICES_DYNAMICALLY = true;

	private static final String[] apiToksLeeway = { "pgz64a5qiuvw4qhkoullnx", "9pe3xyaplenvfvbnyxtomm", "r7splaijduabfpcu2z2l14",
			"o5npdx6elm2pcpp395uaun", "2ja5gszii8g63hzjd41x78", "ftd5l4hscm9biueu5ptptr", "42dreongzzo5yqkyg2r3cr",
			"axt2o7xkx1cxd9nsinqfu6", "rumn8riwt3oo18z1cluvz1", "9rjaye34gvir42j113qf7r", "6gqkwy7si5lv1tgicp28le", "6yylrshzzhlsdhf26ysmq9",
			"xaqiip44tivsl5a963zvft" };
	private static final String[] apiToksMarketstack = { "4b6a78c092537f07bbdedff8f134372d",
			"0c2e8a9c96f2a74c0049f4b662f47b40",
			"621fc5e0add038cc7d9697bcb7f15caa", "4312dfd8788579ec14ee9e9c9bec4557",
			"0a99047c49080d975013978d3609ca9e" };
	private static final String[] apiToksTwelvedata = { "04ed9e666cbb4873ac6d29651e2b4d7e",
			"7e51ed4d1d5f4cbfa6e6bcc8569c1e54",
			"98888ec975884e98a9555233c3dd59da", "af38d2454c2c4a579768b8262d3e039e",
			"facbd6808e6d436e95c4935ab8cc082e" };

	// to prevent race conditions when making requests via the same APIReq object in different threads,
	// we create a new APIReq object for each sequential task
	private static APIReq getApiTwelvedata() {
		return new APIReq("https://api.twelvedata.com/", apiToksTwelvedata, AuthPolicy.QUERY, "apikey");
	}
	private static APIReq getApiLeeway() {
		return new APIReq("https://api.leeway.tech/api/v1/public/", apiToksLeeway, AuthPolicy.QUERY, "apitoken",
			null, null, APIReq.rateLimitErrHandler(res -> {
				if (res.statusCode() == 429) return true;
				if (res.body().startsWith("Your limit of")) return true;
				return false;
			}));
	}
	private static APIReq getApiMarketstack() {
		return new APIReq("http://api.marketstack.com/v1/", apiToksMarketstack, AuthPolicy.QUERY, "access_key",
			new PaginationHandler(json -> {
				// @Cleanup Remove hardcoded limit
				return 30;
			}, json -> json.asMap().get("data")),
			(page) -> {
				// @Cleanup Remove hardcoded offset
				// The function signature might have to change too, bc we need the current limit
				// for calculating the offset and I don't know how to get the limit without
				// having access to the APIReq object
				String[] res = { "offset", Integer.toString(10 * page) };
				return res;
			}, APIReq.defaultErrHandler());
	}

	// @Scalability If more than one component would need to react to updated data,
	// a single boolean flag would not be sufficient of course. Since we know,
	// however, that only the StateManager reacts to this information, having a
	// single boolean flag is completely sufficient
	public static AtomicBoolean updatedData = new AtomicBoolean(false);
	private static BlockingQueue<Runnable> tpQueue = new LinkedBlockingQueue<>();
	private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(16, 64, 60, TimeUnit.SECONDS,
			tpQueue);

	private static UnorderedList<Stock> stocks = new UnorderedList<>(128);
	private static UnorderedList<ETF> etfs = new UnorderedList<>(128);
	private static UnorderedList<Index> indices = new UnorderedList<>(128);

	// When updatedStocksTradingPeriods == stocks.size(), all stocks are updated set by setTradingPeriods()
	private static AtomicInteger updatedStocksTradingPeriods = new AtomicInteger(stocks.size());
	private static AtomicInteger updatedETFsTradingPeriods = new AtomicInteger(etfs.size());
	private static AtomicInteger updatedIndicesTradingPeriods = new AtomicInteger(indices.size());
	private static int updatedTradingPeriodsCounter = 0; // Amount of sonifiables lists, whose trading periods have updated

	private static List<Price> testPrices() {
		try {
			String fname = "./src/main/resources/TestPrices.json";
			String json = Files.readString(Path.of(fname));
			Parser parser = new Parser();
			List<Price> prices = parser.parse(fname, json).applyList(x -> {
				try {
					HashMap<String, JsonPrimitive<?>> m = x.asMap();
					Calendar startDay = DateUtil.calFromDateStr(m.get("datetime").asStr());
					Instant startTime = DateUtil.fmtDatetime.parse(m.get("datetime").asStr()).toInstant();
					Instant endTime = IntervalLength.HOUR.addToInstant(startTime);

					return new Price(startDay, startTime, endTime, m.get("open").asDouble(),
							m.get("close").asDouble(), m.get("low").asDouble(), m.get("high").asDouble());
				} catch (Exception e) {
					return null;
				}
			}, true);
			return prices;
		} catch (Exception e) {
			return List.of();
		}
	}

	// Note: You shouldn't call this function twice
	public static void init() throws AppError {
		try {
			// Read cached stocks
			Parser parser = new Parser();
			String stocksRes = Files.readString(Path.of("./src/main/resources/stocks.json"));
			JsonPrimitive<?> json = parser.parse(stocksRes);
			stocks = json
					.applyList(x -> {
						Calendar earliest = null, latest = null;
						try {
							earliest = DateUtil.calFromDateStr(x.asMap().get("earliest").asStr());
							latest = DateUtil.calFromDateStr(x.asMap().get("latest").asStr());
						} catch (Exception e) {
						}
						return new Stock(x.asMap().get("name").asStr(),
								new SonifiableID(x.asMap().get("id").asMap().get("symbol").asStr(),
										x.asMap().get("id").asMap().get("exchange").asStr()),
								earliest,
								latest);
					}, new UnorderedList<>(json.asList().size()), true);

			// TODO: Read cached ETFs/Indices

			// @Cleanup uncomment for production
			// Start updating stocks in background
			// updateSonifiablesList();
		} catch (Exception e) {
			e.printStackTrace();
			throw new AppError(e.getMessage());
		}
	}

	public static List<Sonifiable> findByPrefix(String prefix, FilterFlag... filters) {
		int flag = FilterFlag.getFilterVal(filters);
		List<Sonifiable> l = new ArrayList<>(128);
		if ((flag & FilterFlag.STOCK.getVal()) > 0)
			findByPrefix(prefix, stocks, l);
		if ((flag & FilterFlag.ETF.getVal()) > 0)
			findByPrefix(prefix, etfs, l);
		if ((flag & FilterFlag.INDEX.getVal()) > 0)
			findByPrefix(prefix, indices, l);
		return l;
	}

	private static void findByPrefix(String prefix, List<? extends Sonifiable> src, List<Sonifiable> dst) {
		for (Sonifiable s : src) {
			if (s != null && (s.name.toLowerCase().startsWith(prefix.toLowerCase())
					|| s.getId().symbol.toLowerCase().startsWith(prefix.toLowerCase()))) {
				dst.add(s);
			}
		}
	}

	public static List<Sonifiable> getAll(FilterFlag... filters) {
		int flag = FilterFlag.getFilterVal(filters);
		List<Sonifiable> l = new ArrayList<>(128);
		if ((flag & FilterFlag.STOCK.getVal()) > 0)
			l.addAll(stocks);
		if ((flag & FilterFlag.ETF.getVal()) > 0)
			l.addAll(etfs);
		if ((flag & FilterFlag.INDEX.getVal()) > 0)
			l.addAll(indices);
		return l;
	}

	public static String getSonifiableName(SonifiableID id) {
		Sonifiable s = getSonifable(id, stocks);
		if (s == null) s = getSonifable(id, etfs);
		if (s == null) s = getSonifable(id, indices);
		if (s == null) return null;
		else return s.getName();
	}

	public static Stock getStock(SonifiableID id) {
		return getSonifable(id, stocks);
	}

	public static ETF getETF(SonifiableID id) {
		return getSonifable(id, etfs);
	}

	public static Index getIndex(SonifiableID id) {
		return getSonifable(id, indices);
	}

	private static <T extends Sonifiable> T getSonifable(SonifiableID id, List<T> list) {
		for (T x : list) {
			if (x.getId() == id)
				return x;
		}
		return null;
	}

	public static Future<List<Price>> getPrices(SonifiableID s, Calendar start, Calendar end, IntervalLength interval) {
		return threadPool.submit(() -> {
			if (!GET_PRICES_DYNAMICALLY) return testPrices();

			APIReq apiLeeway = getApiLeeway();
			Calendar[] startEnd = {start, end};

			// TODO: Add de-facto pagination to price-requests to allow parallelization
			// Idea for doing this: See how long the date-range in the first response was
			// then split the rest of the range for start->end into chunks of that range
			// To achieve this, the signatures for pagination handlers most certainly have to be changed again

			// TODO: Use interval - at the moment we assume that interval == DAY and just make end-of-day API-requests
			try {
				List<Price> out = new ArrayList<>(1024);
				Calendar earliestDay = startEnd[1];
				// Because leeway's range is exclusive, we need to decrease the startdate that we put in the request and keep the same for comparison of the loop
				Calendar startCmp = (Calendar) startEnd[0].clone();
				startEnd[0].roll(Calendar.DATE, false);
				// TODO: Leeway throws error if range is more than 600 days

				// TODO Bug: There seems to be an issue with DateUtil parsing the dates of the prices wrong or something
				// until that bug is fixed, I commented the otherwise potentially endless loop out
				boolean isEoD = interval == IntervalLength.DAY;
				String endpoint = (isEoD ? "historicalquotes/" : "intraday/") + s;
				String[] intervalQueries = {"interval", interval.toString(API.LEEWAY)};
				do {
					// We increase end by 1, because Leeway's range is exclusive
					// and after each iteration end = earliestDay and it might be that we missed some data from that day
					startEnd[1].roll(Calendar.DATE, true);
					String[] queries = {"from", DateUtil.formatDate(startEnd[0]), "to", DateUtil.formatDate(startEnd[1])};
					if (!isEoD) queries = ArrayFunctions.add(queries, intervalQueries);

					List<Price> prices = apiLeeway.getJSONList(endpoint,
							json -> {
								try {
									HashMap<String, JsonPrimitive<?>> m = json.asMap();
									Calendar day;
									Instant startTime;
									Instant endTime;
									if (isEoD) {
										day = DateUtil.calFromDateStr(m.get("date").asStr());
										day.set(Calendar.HOUR_OF_DAY, 0);
										startTime = day.toInstant();
										day.set(Calendar.HOUR_OF_DAY, 23);
										endTime = day.toInstant();
									} else {
										day = DateUtil.calFromDateTimeStr(m.get("datetime").asStr());
										startTime = new Timestamp(m.get("timestamp").asLong()).toInstant();
										endTime = interval.addToInstant(startTime);
									}
									return new Price(day, startTime, endTime, m.get("open").asDouble(), m.get("close").asDouble(), m.get("low").asDouble(), m.get("high").asDouble());
								} catch (Exception e) {
									return null;
								}
							}, true, queries);

					if (prices.isEmpty()) break;
					Calendar nextEarliestDay = prices.get(0).getDay();
					if (nextEarliestDay.equals(earliestDay)) // we would get trapped in an infinite loop now
						break;
					earliestDay = nextEarliestDay;
					startEnd[1] = (Calendar) earliestDay.clone();
					out.addAll(0, prices);
				} while (startCmp.before(earliestDay));

				ArrayFunctions.rmDuplicates(out, 300, (x, y) -> x.getStart().equals(y.getStart()));
				return out;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error in getting Price-Data for " + s + " from " + DateUtil.formatDate(start) + " to " + DateUtil.formatDate(end));
				return null;
			}
		});
	}


	//////////////////////////
	// Potentially obsolete???
	//////////////////////////

	public static void updateSonifiablesList() throws AppError {
		try {
			// @Cleanup increase limit to the maximum (1000) for production
			// @Cleanup As of now this limit value has to be synced with the offset when creating the Marketstack APIReq bc of hardcoded values
			APIReq apiMarketstack = getApiMarketstack();
			apiMarketstack.setQueries("limit", "10");
			FutureList<UnorderedList<Stock>> fl = apiMarketstack.getPaginatedJSONList("tickers", threadPool, 8,
					(Function<Integer, UnorderedList<Stock>>) (size -> {
						UnorderedList<Stock> l = new UnorderedList<>(size);
						for (int i = 0; i < size; i++)
							l.add(i, null);
						return l;
					}),
					x -> x, (Function<JsonPrimitive<?>, Stock>) (x -> {
						try {
							String symbol = x.asMap().get("symbol").asStr();
							String exchange = x.asMap().get("stock_exchange").asMap().get("acronym").asStr();
							if (symbol.contains(".") || exchange.contains("."))
								return null;
							return new Stock(x.asMap().get("name").asStr(),
									new SonifiableID(symbol, exchange));
						} catch (Exception e) {
							return null;
						}
					}));
			Timer checkFLTimer = new Timer();
			checkFLTimer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					try {
						if (fl.isDone()) {
							// Remove nulls from stocks-list
							stocks = fl.get();
							stocks.applyRemoves();
							for (int i = 0; i < stocks.size(); i++) {
								if (stocks.get(i) == null) {
									stocks.remove(i);
									i--;
								}
							}

							setTradingPeriods(stocks, updatedStocksTradingPeriods);

							// TODO: Update ETFs and Stocks too

							Timer timer = new Timer();
							timer.scheduleAtFixedRate(new TimerTask() {
								public void run() {
									// TODO: Refactor to reduce code duplication
									boolean updatedTradingPeriods = false;
									if (updatedStocksTradingPeriods.compareAndSet(stocks.size(), 0)) {
										updatedTradingPeriods = true;
										updatedTradingPeriodsCounter++;
										for (int i = 0; i < stocks.size(); i++) {
											if (stocks.get(i) == null || stocks.get(i).getEarliest() == null) {
												stocks.remove(i);
												i--;
											}
										}
									}
									// @Cleanup uncomment once we actually get data for etfs/indices
									// needs to be commented out for now, bc the code doesn't work when list.size()
									// == 0
									// if (updatedETFsTradingPeriods.compareAndSet(etfs.size(), 0)) {
									// updatedTradingPeriods = true;
									// updatedTradingPeriodsCounter++;
									// etfs.applyRemoves();
									// }
									// if (updatedIndicesTradingPeriods.compareAndSet(indices.size(), 0)) {
									// updatedTradingPeriods = true;
									// updatedTradingPeriodsCounter++;
									// indices.applyRemoves();
									// }

									System.out.println(
											"Timer check: ThreadPool = " + threadPool.toString());

									if (updatedTradingPeriods)
										updatedData.set(true);
									// @Cleanup exchange 1 with 3, once we get data for etfs/indices too
									if (updatedTradingPeriodsCounter >= 1) {
										updatedTradingPeriodsCounter = 0;
										System.out.println("Setting Trading Periods done");
										timer.cancel();
										try {
											writeToJSON("stocks.json", ArrayFunctions.toStringArr(stocks.getArray(), x -> ((Sonifiable) x).toJSON(), true));
										} catch (AppError e) {
											e.printStackTrace();
										}
									}
								}
							}, 500, 200);

							checkFLTimer.cancel();
						}
					} catch (Throwable e) {
						e.printStackTrace();
						checkFLTimer.cancel();
					}
				}
			}, 1000, 200);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AppError(e.getMessage());
		}
	}

	private static <T> void writeToJSON(String filename, String data) throws AppError {
		try {
			PrintWriter out = new PrintWriter("./src/main/resources/" + filename);
			out.write(data);
			out.close();
		} catch (Exception e) {
			throw new AppError(e.getMessage());
		}
	}

	private static <T extends Sonifiable> void setTradingPeriods(UnorderedList<T> list,
			AtomicInteger updatedTradingPeriods) {
		updatedTradingPeriods.set(0);
		for (int i = 0; i < list.size(); i++) {
			T s = list.get(i);
			final int idx = i;
			getTradingPeriod(s, dates -> {
				if (dates == null || dates[0] == null || dates[1] == null) {
					list.removeLater(idx);
				} else {
					s.setEarliest(dates[0]);
					s.setLatest(dates[1]);
				}
				int x = updatedTradingPeriods.incrementAndGet();
				System.out.println("Set trading periods. idx = " + idx + ", updatedCount = " + x + ", tpQueue.size() = "
						+ tpQueue.size());
				return s;
			});
		}
	}

	// Returns list of two Calendar objects. The first element is the starting date,
	// the second the ending date. If an error happened, `null` is returned
	public static <T> Future<T> getTradingPeriod(Sonifiable s, Function<Calendar[], T> callback) {
		return threadPool.submit(() -> {
			APIReq apiLeeway = getApiLeeway();
			APIReq apiTwelvedata = getApiTwelvedata();
			Calendar[] res = { null, null };
			T out = null;
			try {
				HashMap<String, JsonPrimitive<?>> json = apiLeeway.getJSON("general/tradingperiod/" + s.getId().toString(), x -> x.asMap());
				res[0] = DateUtil.calFromDateStr(json.get("start").asStr());
				res[1] = DateUtil.calFromDateStr(json.get("end").asStr());
				out = callback.apply(res);
			} catch (APIErr e) {
				// Try again using Twelvedata's API instead
				try {
					apiTwelvedata.setQueries("symbol", s.getId().getSymbol(), "exchange", s.getId().getExchange(), "outputsize", "10", "interval", IntervalLength.DAY.toString(API.TWELVEDATA));

					List<JsonPrimitive<?>> vals = apiTwelvedata.getJSON("time_series", x -> x.asMap().get("values").asList(), "order", "ASC", "start_date", "1990-01-01");
					res[0] = DateUtil.calFromDateStr(vals.get(0).asMap().get("datetime").asStr());

					vals = apiTwelvedata.getJSON("time_series", x -> x.asMap().get("values").asList(), "order", "DESC", "end_date", DateUtil.formatDate(Calendar.getInstance()));
					res[1] = DateUtil.calFromDateStr(vals.get(0).asMap().get("datetime").asStr());

					apiTwelvedata.resetQueries();
					out = callback.apply(res);
				} catch (Exception e2) {
					// @Debug
					// System.out.println("Error in inner getTradingPeriod:");
					// e2.printStackTrace();
					out = callback.apply(null);
				}
			} catch (Exception e) {
				// @Debug
				// System.out.println("Error in outer getTradingPeriod:");
				// e.printStackTrace();
				out = callback.apply(null);
			}
			return out;
		});
	}
}