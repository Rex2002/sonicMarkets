package apiTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import json.JsonPrimitive;

// knowledgeVault69@gmail.com
// data.parliament@gmail.com
// wildling.princess.val@gmail.com
// sansasucks1@gmail.com
// vale.morghulis@gmail.com

public class Test {
	static final String[] leewayAPIToks = { "pgz64a5qiuvw4qhkoullnx", "9pe3xyaplenvfvbnyxtomm",
			"r7splaijduabfpcu2z2l14", "o5npdx6elm2pcpp395uaun", "2ja5gszii8g63hzjd41x78" };
	static final String[] marketstackAPIToks = { "4b6a78c092537f07bbdedff8f134372d", "0c2e8a9c96f2a74c0049f4b662f47b40",
			"621fc5e0add038cc7d9697bcb7f15caa", "4312dfd8788579ec14ee9e9c9bec4557",
			"0a99047c49080d975013978d3609ca9e" };

	public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
		// testLeeway();
		testMarketstack();
	}

	public static void testMarketstack() throws URISyntaxException, IOException, InterruptedException {
		APIReq marketstackAPI = new APIReq("http://api.marketstack.com/v1/", marketstackAPIToks, AuthPolicy.QUERY,
				"access_key");
		marketstackAPI.setQueries("limit", "1000");
		marketstackAPI.setPaginationHandler(json -> {
			JsonPrimitive<?> rest = json.asMap().get("data");
			HashMap<String, JsonPrimitive<?>> pageMap = json.asMap().get("pagination").asMap();
			Integer x = pageMap.get("offset").asInt() + pageMap.get("count").asInt();
			boolean done = x >= 5000; // pageMap.get("total").asInt();
			return new HandledPagination(rest, done);
		}, counter -> {
			marketstackAPI.setQuery("offset", Integer.toString(counter * 1000));
		});

		PrintWriter out = new PrintWriter("marketstack.txt");
		List<String> res = marketstackAPI.getPaginatedList(x -> x.asMap().get("symbol").asStr(), "tickers");
		for (String x : res) {
			out.println(x);
		}
		out.close();
	}

	public static void testLeeway() throws URISyntaxException, IOException, InterruptedException {
		APIReq leewayAPI = new APIReq("https://api.leeway.tech/api/v1/public/", leewayAPIToks, AuthPolicy.QUERY,
				"apitoken");

		List<Exchange> exchanges = leewayAPI.getJSONList(
				x -> new Exchange(x.asMap().get("Name").asStr(), x.asMap().get("Code").asStr()),
				"general/exchanges");

		List<Stock> stocks = new ArrayList<>();
		for (Exchange exchange : exchanges) {
			stocks.addAll(leewayAPI.getJSONList(
					x -> new Stock(x.asMap().get("name").asStr(), x.asMap().get("code").asStr(),
							x.asMap().get("exchange").asStr(), x.asMap().get("type").asStr()),
					"general/symbols/" + exchange.code));
		}

		System.out.println("Amount of available stocks: " + stocks.size());
	}
}
