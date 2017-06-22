
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OKCoin3 {
	private static boolean saveLog = true;
	private static boolean saveErr = false;
	private static String ok_partner = "";
	private static String ok_secretKey = "";
	private static FileWriter logger = null;
	private static FileWriter errorer = null;
	private static String lastLog = null;
	private static String symbol = "btc_cny";
	private static double balance = 7500;
	private static double step = 0.01;
	private static double coin;
	private static double cash;
	private static double fake = 0;
	private static boolean dynamicMode = true;
	private static int dynamic = 1;
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static DecimalFormat df3 = new DecimalFormat("#.###");
	private static DecimalFormat df4 = new DecimalFormat("#.####");
	private static double last_price;
	private static List<Integer> dealHistory;
	private static int dealCount;
	private static int dealCountH;
	private static int dealCountL;
	private static int dealCountLL;
	private static int dynamicH;
	private static int dynamicL;
	private static int dynamicLL;
	private static double coinH;
	private static double coinL;
	private static double coinLL;
	private static double cashH;
	private static double cashL;
	private static double cashLL;
	private static long lastDeal;

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
			if(String.valueOf(text).equals(lastLog))return;
			if(logger == null)logger = new FileWriter("OKCoin3.txt", true);
			logger.write(getNow() + ": " + String.valueOf(text));
			logger.write("\r\n");
			logger.flush();
			lastLog = String.valueOf(text);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	public synchronized static void err(Object text){
		System.out.println(String.valueOf(text));
		if(!saveErr)return;

		try{
			if(errorer == null)errorer = new FileWriter("OKCoin3Err.txt", true);
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

	public static JSONObject getOKInfo(){
		while(true){
			String data = "partner=" + ok_partner;
			JSONObject json = getOKAPI("https://www.okcoin.cn/api/userinfo.do", data, false);
			if(json.getBoolean("result"))return json.getJSONObject("info").getJSONObject("funds");
			if(json.getInt("errorCode") == 10001)continue;
			err(json.getInt("errorCode") + ":" + data);
			if(json.getInt("errorCode") == 10005)continue;
			throw new RuntimeException(n(json.getInt("errorCode")));
		}
	}

	public static boolean OKTrade(String type, double amount, double rate, String symbol){
		while(true){
			String data = "amount=" + df3.format(amount) + "&partner=" + ok_partner + "&rate=" + df2.format(rate) + "&symbol=" + symbol + "&type=" + type;
			JSONObject json = getOKAPI("https://www.okcoin.cn/api/trade.do", data, true);
			if(json == null)return true;
			if(json.getBoolean("result"))return true;
			if(json.getInt("errorCode") == 10001)continue;
			if(json.getInt("errorCode") == 10010)return false;
			err(json.getInt("errorCode") + ":" + data);
			if(json.getInt("errorCode") == 10005)continue;
			throw new RuntimeException(n(json.getInt("errorCode")));
		}
	}

	public static JSONArray getOKOrder(String symbol){
		while(true){
			String data = "order_id=-1&partner=" + ok_partner + "&symbol=" + symbol;
			JSONObject json = getOKAPI("https://www.okcoin.cn/api/getorder.do", data, false);
			if(json.getBoolean("result"))return json.getJSONArray("orders");
			if(json.getInt("errorCode") == 10001)continue;
			err(json.getInt("errorCode") + ":" + data);
			if(json.getInt("errorCode") == 10005)continue;
			throw new RuntimeException(n(json.getInt("errorCode")));
		}
	}

	public static void cancelOKOrder(List<Long> orderIds, String symbol){
		class CancelOKOrderThread extends Thread {
			private List<Long> orderIds;
			private String symbol;

			public CancelOKOrderThread(List<Long> orderIds, String symbol){
				this.orderIds = orderIds;
				this.symbol = symbol;
			}

			public void run(){
				try{
					for(long orderId : orderIds){
						while(true){
							String data = "order_id=" + orderId + "&partner=" + ok_partner + "&symbol=" + symbol;
							JSONObject json = getOKAPI("https://www.okcoin.cn/api/cancelorder.do", data, false);
							if(json.getBoolean("result"))break;
							if(json.getInt("errorCode") == 10001)continue;
							if(json.getInt("errorCode") == 10009)break;
							err(json.getInt("errorCode") + ":" + data);
							if(json.getInt("errorCode") == 10005)continue;
							throw new RuntimeException(n(json.getInt("errorCode")));
						}
					}
				}catch(Exception e){
					e.printStackTrace();
					System.exit(0);
				}
			}
		}

		new CancelOKOrderThread(orderIds, symbol).start();
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
		return getPublicAPI("https://www.okcoin.cn/api/ticker.do?symbol=" + symbol).getJSONObject("ticker");
	}

	private static void bootTrade(){
		last_price = getOKTicker(symbol).getDouble("last");

		JSONObject json = getOKInfo();
		coin = json.getJSONObject("freezed").getDouble(symbol.substring(0, 3));
		coin += json.getJSONObject("free").getDouble(symbol.substring(0, 3));
		cash = json.getJSONObject("freezed").getDouble("cny");
		cash += json.getJSONObject("free").getDouble("cny");

		double assets = (fake + coin) * last_price + cash;
		if(balance < assets / 2)balance = (long)assets / 2;

		cancelAllOKOrder(symbol);
		dealHistory = new ArrayList<Integer>();

		dynamicH = dynamic * 2;
		dynamicL = dynamic / 2;
		if(dynamicL < 1)dynamicL = 1;
		dynamicLL = dynamicL / 2;
		if(dynamicLL < 1)dynamicLL = 1;
		coinH = coin;
		coinL = coin;
		coinLL = coin;
		cashH = cash;
		cashL = cash;
		cashLL = cash;
		lastDeal = System.currentTimeMillis();
	}

	public static void autoTrade() throws Exception{
		bootTrade();

		boolean changeL = false;
		boolean changeH = false;
		while(true){
			if(dynamicMode){
				if(changeL && dynamic > 1){
					log("Changing dynamic from " + dynamic + " to " + (dynamic / 2) + "...");
					dynamic = dynamic / 2;

					bootTrade();
				}else if(changeH){
					log("Changing dynamic from " + dynamic + " to " + (dynamic * 2) + "...");
					dynamic = dynamic * 2;

					bootTrade();
				}
			}

			Map<Double, Long> buyOrders = new HashMap<Double, Long>();
			Map<Double, Long> sellOrders = new HashMap<Double, Long>();

			JSONArray orders = getOKOrder(symbol);
			for(int i = 0; i < orders.length(); i++){
				JSONObject order = orders.getJSONObject(i);
				if(order.getString("type").equals("buy")){
					buyOrders.put(order.getDouble("rate"), order.getLong("orders_id"));
				}else{
					sellOrders.put(order.getDouble("rate"), order.getLong("orders_id"));
				}
			}

			last_price = getOKTicker(symbol).getDouble("last");

			JSONObject json = getOKInfo();
			coin = json.getJSONObject("freezed").getDouble(symbol.substring(0, 3));
			coin += json.getJSONObject("free").getDouble(symbol.substring(0, 3));
			cash = json.getJSONObject("freezed").getDouble("cny");
			cash += json.getJSONObject("free").getDouble("cny");

			boolean trade = false;
			dealHistory.add(0, 0);

			if(cash > step * dynamic * last_price){
				double buy_price = balance / (fake + coin + step * dynamic);
				if(buy_price < last_price * 0.8)buy_price = last_price * 0.8;
				if(buy_price > last_price * 1.2)buy_price = last_price * 1.2;
				buy_price = Double.parseDouble(df2.format(buy_price));
				if(!buyOrders.containsKey(buy_price)){
					if(OKTrade("buy", step * dynamic, buy_price, symbol))trade = true;
				}
			}

			if(coin > step * dynamic){
				double sell_price = balance / (fake + coin - step * dynamic) + 0.01;
				if(sell_price < last_price * 0.8)sell_price = last_price * 0.8;
				if(sell_price > last_price * 1.2)sell_price = last_price * 1.2;
				sell_price = Double.parseDouble(df2.format(sell_price));
				if(!sellOrders.containsKey(sell_price)){
					if(OKTrade("sell", step * dynamic, sell_price, symbol))trade = true;
				}
			}

			if(trade){
				int value = dealHistory.get(0);
				dealHistory.set(0, value + 4);
			}

			if(cashH > step * dynamicH * last_price){
				double buyH = balance / (fake + coinH + step * dynamicH);
				buyH = Double.parseDouble(df2.format(buyH));
				if(buyH > last_price){
					coinH += step * dynamicH;
					cashH -= step * dynamicH * last_price;
					int value = dealHistory.get(0);
					dealHistory.set(0, value + 8);
				}
			}

			if(coinH > step * dynamicH){
				double sellH = balance / (fake + coinH - step * dynamicH) + 0.01;
				sellH = Double.parseDouble(df2.format(sellH));
				if(sellH < last_price){
					coinH -= step * dynamicH;
					cashH += step * dynamicH * last_price;
					int value = dealHistory.get(0);
					dealHistory.set(0, value + 8);
				}
			}

			if(cashL > step * dynamicL * last_price){
				double buyL = balance / (fake + coinL + step * dynamicL);
				buyL = Double.parseDouble(df2.format(buyL));
				if(buyL > last_price){
					coinL += step * dynamicL;
					cashL -= step * dynamicL * last_price;
					int value = dealHistory.get(0);
					dealHistory.set(0, value + 2);
				}
			}

			if(coinL > step * dynamicL){
				double sellL = balance / (fake + coinL - step * dynamicL) + 0.01;
				sellL = Double.parseDouble(df2.format(sellL));
				if(sellL < last_price){
					coinL -= step * dynamicL;
					cashL += step * dynamicL * last_price;
					int value = dealHistory.get(0);
					dealHistory.set(0, value + 2);
				}
			}

			if(cashLL > step * dynamicLL * last_price){
				double buyLL = balance / (fake + coinLL + step * dynamicLL);
				buyLL = Double.parseDouble(df2.format(buyLL));
				if(buyLL > last_price){
					coinLL += step * dynamicLL;
					cashLL -= step * dynamicLL * last_price;
					int value = dealHistory.get(0);
					dealHistory.set(0, value + 1);
				}
			}

			if(coinLL > step * dynamicLL){
				double sellLL = balance / (fake + coinLL - step * dynamicLL) + 0.01;
				sellLL = Double.parseDouble(df2.format(sellLL));
				if(sellLL < last_price){
					coinLL -= step * dynamicLL;
					cashLL += step * dynamicLL * last_price;
					int value = dealHistory.get(0);
					dealHistory.set(0, value + 1);
				}
			}

			if(buyOrders.size() >= 999){
				List<Double> buyList = new ArrayList<Double>(buyOrders.keySet());
				Collections.sort(buyList);

				List<Long> orderIds = new ArrayList<Long>();
				orderIds.add(buyOrders.get(buyList.get(0)));
				cancelOKOrder(orderIds, symbol);
			}

			if(sellOrders.size() >= 999){
				List<Double> sellList = new ArrayList<Double>(sellOrders.keySet());
				Collections.sort(sellList);

				List<Long> orderIds = new ArrayList<Long>();
				orderIds.add(sellOrders.get(sellList.get(sellList.size() - 1)));
				cancelOKOrder(orderIds, symbol);
			}

			int value = dealHistory.get(0);
			if(value == 0){
				dealHistory.remove(0);
			}else{
				lastDeal = System.currentTimeMillis();
			}
			while(dealHistory.size() > 4096){
				dealHistory.remove(dealHistory.size() - 1);
			}

			dealCount = 0;
			dealCountH = 0;
			dealCountL = 0;
			dealCountLL = 0;
			changeL = false;
			changeH = false;
			for(int deal : dealHistory){
				if(deal >= 8){
					dealCountH++;
					deal -= 8;
				}
				if(deal >= 4){
					dealCount++;
					deal -= 4;
				}
				if(deal >= 2){
					dealCountL++;
					deal -= 2;
				}
				if(deal == 1)dealCountLL++;

				if(dealCountLL >= 32 && (double)dealCount / dealCountLL < 0.0625){
					changeL = true;
					break;
				}
				if(dealCountL >= 8 && (double)dealCount / dealCountL < 0.25){
					changeL = true;
					break;
				}
				if(dealCountH >= 4 && dealCount > 0 && (double)dealCountH / dealCount > 0.25){
					changeH = true;
					break;
				}
			}
			if(System.currentTimeMillis() - lastDeal > 1000 * 60 * 60)changeL = true;

			log(rightS(df2.format(last_price), 11) + "B " + rightS(buyOrders.size(), 4) + "S " + rightS(sellOrders.size(), 4) + "BL " + rightS(df2.format(balance), 9) + "C " + rightS(df4.format(coin), 15) + "R " + rightS(dynamic, 5) + dealCountH + " " + dealCount + " " + dealCountL + " " + dealCountLL);
			Thread.sleep(2000);
		}
	}

	private static String rightS(Object value, int length){
		StringBuffer s = new StringBuffer(n(value));
		for(int i = length - s.length(); i > 0; i--){
			s.append(" ");
		}
		return s.toString();
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = OKCoin3.class.getResourceAsStream("config.properties");
		config.load(in);
		in.close();
		saveLog = Boolean.parseBoolean(config.getProperty("saveLog"));
		saveErr = Boolean.parseBoolean(config.getProperty("saveErr"));
		ok_partner = config.getProperty("ok_partner");
		ok_secretKey = config.getProperty("ok_secretKey");
		symbol = config.getProperty("symbol");
		balance = (long)Double.parseDouble(config.getProperty("balance"));
		if(balance < 1)balance = 1;
		step = Double.parseDouble(config.getProperty("step"));
		if(step < 0.01)step = 0.01;
		fake = Double.parseDouble(config.getProperty("fake"));
		if(fake < 0) fake = 0;
		dynamicMode = Boolean.parseBoolean(config.getProperty("dynamicMode"));
	}

	public static void main(String[] args) throws Exception{
		System.out.println("Starting...");
		init();
		System.out.println("inited.");

		autoTrade();
	}
}