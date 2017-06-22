
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OKCoin5 {
	private static boolean saveLog = true;
	private static boolean saveErr = false;
	private static String ok_partner = "";
	private static String ok_secretKey = "";
	private static FileWriter logger = null;
	private static FileWriter errorer = null;
	private static String symbol = "btc_cny";
	private static double coin = 0.01;
	private static double step = 1;
	private static int times = 1;
	private static int clean = 200;
	private static boolean safeMode = false;
	private static boolean dynamicMode = true;
	private static int dynamicSize = 128;
	private static int dynamic = 1;
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static DecimalFormat df3 = new DecimalFormat("#.###");
	private static double last_price;
	private static String buy_edge;
	private static String sell_edge;
	private static Map<String, Integer> buyOrders;
	private static Map<String, Integer> sellOrders;
	private static double dealRate;
	private static List<Integer> dealHistory;

	public static String n(Object input){
		return input == null?"":String.valueOf(input);
	}

	public static boolean e(Object input){
		return (input == null || input.equals(""))?true:false;
	}

	public static String getNow(){
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date());
	}

	public synchronized static void log(Object text){
		System.out.println(String.valueOf(text));
		if(!saveLog)return;

		try{
			if(logger == null)logger = new FileWriter("OKCoin5.txt", true);
			logger.write(getNow() + ": " + String.valueOf(text));
			logger.write("\r\n");
			logger.flush();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	public synchronized static void err(Object text){
		System.out.println(String.valueOf(text));
		if(!saveErr)return;

		try{
			if(errorer == null)errorer = new FileWriter("OKCoin5Err.txt", true);
			errorer.write(getNow() + ": " + String.valueOf(text));
			errorer.write("\r\n");
			errorer.flush();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	private static String byte2Hex(byte[] bytes){
		if(bytes == null)return "";
		int bLen = bytes.length;
		StringBuffer sb = new StringBuffer(bLen * 2);
		for(int i = 0;i < bLen;i++){
			int intbyte = bytes[i];
			while(intbyte < 0){
				intbyte += 256;
			}
			if(intbyte < 16)sb.append("0");
			sb.append(Integer.toString(intbyte, 16));
		}
		return sb.toString();
	}

	private static String md5(String input) throws Exception{
		return byte2Hex(MessageDigest.getInstance("MD5").digest(n(input).getBytes("UTF-8")));
	}

	private static String readStream(InputStream in) throws IOException{
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		StringBuffer buffer = new StringBuffer();
		String line = reader.readLine();
		while(line != null){
			buffer.append(line).append("\r\n");
			line = reader.readLine();
		}
		reader.close();
		return buffer.toString();
	}

	public static JSONObject getPublicAPI(String path){
		int timeout = 2000;
		while(true){
			try{
				Thread.sleep(1);
				URL url = new URL(path);
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				con.setReadTimeout(timeout * 2);
				con.setUseCaches(false);
				con.setDoOutput(false);
				con.setDoInput(true);
				con.setConnectTimeout(timeout);
				con.connect();
				String json = readStream(con.getInputStream());
				con.disconnect();
				return new JSONObject(json);
			}catch(IOException e){
				err(e.getMessage());
				if(timeout < 60000)timeout += 2000;
			}catch(JSONException e){
				err(e.getMessage());
				continue;
			}catch(Exception e){
				err(e.getMessage());
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	public static JSONObject getOKAPI(String api, String data, boolean trade){
		int timeout = 2000;
		while(true){
			try{
				Thread.sleep(1);
				String sign = md5(data + ok_secretKey).toUpperCase();
				String tosend = "sign=" + sign + "&" + data;
				URL url = new URL(api);
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				con.setReadTimeout(timeout * 2);
				con.setUseCaches(false);
				con.setDoOutput(true);
				con.setDoInput(true);
				con.setRequestMethod("POST");
				con.setConnectTimeout(timeout);
				con.connect();
				con.getOutputStream().write(tosend.getBytes("UTF-8"));
				con.getOutputStream().flush();
				con.getOutputStream().close();
				try{
					String json = readStream(con.getInputStream());
					con.disconnect();
					return new JSONObject(json);
				}catch(IOException e){
					err(e.getMessage());
					if(trade)return null;
					throw e;
				}catch(JSONException e){
					err(e.getMessage());
					if(trade)return null;
					continue;
				}
			}catch(IOException e){
				err(e.getMessage());
				if(timeout < 60000)timeout += 2000;
			}catch(Exception e){
				err(e.getMessage());
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	public static boolean OKTrade(String type, double amount, double rate, String symbol){
		while(true){
			String data = "amount=" + df3.format(amount) + "&partner=" + ok_partner + "&rate=" + df2.format(rate) + "&symbol=" + symbol + "&type=" + type;
			JSONObject json = getOKAPI("https://www.okcoin.com/api/trade.do", data, true);
			if(json == null)return true;
			if(json.getBoolean("result"))return true;
			if(json.getInt("errorCode") == 10001)continue;
			if(json.getInt("errorCode") == 10010)return false;
			err(json.getInt("errorCode") + ":" + data);
			throw new RuntimeException(n(json.getInt("errorCode")));
		}
	}

	public static void OKTrade(List<Thread> threads, String type, double amount, double rate, String symbol){
		class OKTradeThread extends Thread {
			private String type;
			private double amount;
			private double rate;
			private String symbol;
			private String untype;
			private double unrate;

			public OKTradeThread(String type, double amount, double rate, String symbol){
				this.type = type;
				this.amount = amount;
				this.rate = rate;
				this.symbol = symbol;

				if(type.equals("buy")){
					this.untype = "sell";
					this.unrate = rate * 0.8;
				}else{
					this.untype = "buy";
					this.unrate = rate * 1.2;
				}
			}

			public void run(){
				try{
					while(true){
						if(OKTrade(type, amount, rate, symbol) || safeMode)return;
						if(OKTrade(untype, amount, unrate, symbol))continue;
						throw new RuntimeException("Not enough money or coin.");
					}
				}catch(Exception e){
					e.printStackTrace();
					System.exit(0);
				}
			}
		}

		Thread thread = new OKTradeThread(type, amount, rate, symbol);
		thread.start();
		if(threads != null)threads.add(thread);
	}

	public static JSONArray getOKOrder(String symbol){
		while(true){
			String data = "order_id=-1&partner=" + ok_partner + "&symbol=" + symbol;
			JSONObject json = getOKAPI("https://www.okcoin.com/api/getorder.do", data, false);
			if(json.getBoolean("result"))return json.getJSONArray("orders");
			if(json.getInt("errorCode") == 10001)continue;
			err(json.getInt("errorCode") + ":" + data);
			throw new RuntimeException(n(json.getInt("errorCode")));
		}
	}

	public static void cancelOKOrder(List<Long> orderIds, String symbol){
		class cancelOKOrderThread extends Thread {
			private List<Long> orderIds;
			private String symbol;

			public cancelOKOrderThread(List<Long> orderIds, String symbol){
				this.orderIds = orderIds;
				this.symbol = symbol;
			}

			public void run(){
				try{
					for(long orderId : orderIds){
						while(true){
							String data = "order_id=" + orderId + "&partner=" + ok_partner + "&symbol=" + symbol;
							JSONObject json = getOKAPI("https://www.okcoin.com/api/cancelorder.do", data, false);
							if(json.getBoolean("result"))break;
							if(json.getInt("errorCode") == 10001)continue;
							if(json.getInt("errorCode") == 10009)break;
							err(json.getInt("errorCode") + ":" + data);
							throw new RuntimeException(n(json.getInt("errorCode")));
						}
					}
				}catch(Exception e){
					e.printStackTrace();
					System.exit(0);
				}
			}
		}

		new cancelOKOrderThread(orderIds, symbol).start();
	}

	public static void cancelAllOKOrder(String symbol){
		List<Long> orderIds = new ArrayList<Long>();
		JSONArray orders = getOKOrder(symbol);
		for(int i = 0; i < orders.length(); i++){
			long orderId = orders.getJSONObject(i).getLong("orders_id");
			orderIds.add(orderId);
		}
		cancelOKOrder(orderIds, symbol);
	}

	public static JSONObject getOKTicker(String symbol){
		return getPublicAPI("https://www.okcoin.com/api/ticker.do?symbol=" + symbol).getJSONObject("ticker");
	}

	public static void wait4Threads(List<Thread> threads) throws Exception{
		for(Thread thread : threads){
			thread.join();
		}
	}

	private static void addToMap(Map<String, Integer> map, double price){
		String key = df2.format(price);
		if(map.containsKey(key)){
			int time = map.get(key);
			map.put(key, time + 1);
		}else{
			map.put(key, 1);
		}
	}

	private static void removeFromMap(Map<String, Integer> map, double price){
		String key = df2.format(price);
		if(map.containsKey(key)){
			int time = map.get(key);
			if(time <= 1){
				map.remove(key);
			}else{
				map.put(key, time - 1);
			}
		}
	}

	private static void bootTrade(){
		cancelAllOKOrder(symbol);

		last_price = Double.parseDouble(df2.format((int)(getOKTicker(symbol).getDouble("last") / step) * step));
		buy_edge = df2.format(last_price);
		sell_edge = df2.format(last_price + (step * dynamic));

		buyOrders = new HashMap<String, Integer>();
		sellOrders = new HashMap<String, Integer>();
		for(int i = 0; i < times; i++){
			addToMap(buyOrders, last_price);
		}

		dealRate = 0;
		dealHistory = new ArrayList<Integer>();
	}

	public static void autoTrade() throws Exception{
		bootTrade();

		while(true){
			if(dynamicMode && dealHistory.size() >= dynamicSize){
				if(dealRate > 1 && dynamic <= (clean / 2)){
					log("Changing dynamic from " + dynamic + " to " + (dynamic * 2) + "...");
					dynamic = dynamic * 2;

					bootTrade();
				}else if(dealRate < 0.1 && dynamic > 1){
					log("Changing dynamic from " + dynamic + " to " + (dynamic / 2) + "...");
					dynamic = dynamic / 2;

					bootTrade();
				}
			}

			last_price = getOKTicker(symbol).getDouble("last");

			Map<String, Integer> buyDeals = new HashMap<String, Integer>(buyOrders);
			Map<String, Integer> sellDeals = new HashMap<String, Integer>(sellOrders);

			JSONArray orders = getOKOrder(symbol);
			for(int i = 0; i < orders.length(); i++){
				JSONObject order = orders.getJSONObject(i);
				if(order.getString("type").equals("buy")){
					removeFromMap(buyDeals, order.getDouble("rate"));
				}else{
					removeFromMap(sellDeals, order.getDouble("rate"));
				}
			}

			List<Thread> threads = new ArrayList<Thread>();
			for(Map.Entry<String, Integer> deal : buyDeals.entrySet()){
				double price = Double.parseDouble(deal.getKey());
				int time = deal.getValue();

				for(int i = 0; i < time; i++){
					removeFromMap(buyOrders, price);
					OKTrade(threads, "sell", coin * dynamic, price + (step * dynamic), symbol);
					addToMap(sellOrders, price + (step * dynamic));
				}

				if(deal.getKey().equals(buy_edge)){
					for(int i = 0; i < times; i++){
						if(safeMode){
							if(OKTrade("buy", coin * dynamic, price - (step * dynamic), symbol)){
								addToMap(buyOrders, price - (step * dynamic));
								buy_edge = df2.format(price - (step * dynamic));
							}
						}else{
							OKTrade(threads, "buy", coin * dynamic, price - (step * dynamic), symbol);
							addToMap(buyOrders, price - (step * dynamic));
							buy_edge = df2.format(price - (step * dynamic));
						}
					}
				}
			}

			for(Map.Entry<String, Integer> deal : sellDeals.entrySet()){
				double price = Double.parseDouble(deal.getKey());
				int time = deal.getValue();

				for(int i = 0; i < time; i++){
					removeFromMap(sellOrders, price);
					OKTrade(threads, "buy", coin * dynamic, price - (step * dynamic), symbol);
					addToMap(buyOrders, price - (step * dynamic));
				}

				if(deal.getKey().equals(sell_edge)){
					for(int i = 0; i < times; i++){
						if(safeMode){
							if(OKTrade("sell", coin * dynamic, price + (step * dynamic), symbol)){
								addToMap(sellOrders, price + (step * dynamic));
								sell_edge = df2.format(price + (step * dynamic));
							}
						}else{
							OKTrade(threads, "sell", coin * dynamic, price + (step * dynamic), symbol);
							addToMap(sellOrders, price + (step * dynamic));
							sell_edge = df2.format(price + (step * dynamic));
						}
					}
				}
			}

			if(buyOrders.size() > (clean / dynamic)){
				buy_edge = df2.format(Double.parseDouble(buy_edge) + (step * dynamic));
				List<Long> orderIds = new ArrayList<Long>();
				for(int i = 0; i < orders.length(); i++){
					JSONObject order = orders.getJSONObject(i);
					if(!order.getString("type").equals("buy"))continue;
					if(order.getDouble("rate") < Double.parseDouble(buy_edge)){
						removeFromMap(buyOrders, order.getDouble("rate"));
						orderIds.add(order.getLong("orders_id"));
					}
				}
				cancelOKOrder(orderIds, symbol);
			}

			if(sellOrders.size() > (clean / dynamic)){
				sell_edge = df2.format(Double.parseDouble(sell_edge) - (step * dynamic));
				List<Long> orderIds = new ArrayList<Long>();
				for(int i = 0; i < orders.length(); i++){
					JSONObject order = orders.getJSONObject(i);
					if(!order.getString("type").equals("sell"))continue;
					if(order.getDouble("rate") > Double.parseDouble(sell_edge)){
						removeFromMap(sellOrders, order.getDouble("rate"));
						orderIds.add(order.getLong("orders_id"));
					}
				}
				cancelOKOrder(orderIds, symbol);
			}

			dealHistory.add(buyDeals.size() + sellDeals.size());
			if(dealHistory.size() > dynamicSize)dealHistory.remove(0);
			dealRate = 0;
			for(int deal : dealHistory){
				dealRate += deal;
			}
			dealRate = dealRate / dealHistory.size();

			wait4Threads(threads);

			log(last_price + "      \tbuy:" + buyOrders.size() + "   \tsell:" + sellOrders.size() + "  \tdeal:" + (buyDeals.size() + sellDeals.size()) + "  \trate:" + dynamic + ":" + df2.format(dealRate));
			Thread.sleep(2000);
		}
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = OKCoin5.class.getResourceAsStream("config.properties");
		config.load(in);
		in.close();
		saveLog = Boolean.parseBoolean(config.getProperty("saveLog"));
		saveErr = Boolean.parseBoolean(config.getProperty("saveErr"));
		ok_partner = config.getProperty("ok_partner");
		ok_secretKey = config.getProperty("ok_secretKey");
		symbol = config.getProperty("symbol");
		coin = Double.parseDouble(config.getProperty("coin"));
		if(coin < 0.01)coin = 0.01;
		step = Double.parseDouble(config.getProperty("step"));
		if(step < 0.01)step = 0.01;
		times = Integer.parseInt(config.getProperty("times"));
		if(times < 1)times = 1;
		clean = Integer.parseInt(config.getProperty("clean"));
		if(clean < 1)clean = 1;
		safeMode = Boolean.parseBoolean(config.getProperty("safeMode"));
		dynamicMode = Boolean.parseBoolean(config.getProperty("dynamicMode"));
		dynamicSize = Integer.parseInt(config.getProperty("dynamicSize"));
		if(dynamicSize < 1)dynamicSize = 1;
	}

	public static void main(String[] args) throws Exception{
		System.out.println("Starting...");
		init();
		System.out.println("inited.");

		autoTrade();
	}
}