
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
import java.util.Date;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OkCoin6 {
	private static boolean saveLog = true;
	private static boolean saveErr = false;
	private static String ok_apiKey = "";
	private static String ok_secretKey = "";
	private static FileWriter logger = null;
	private static FileWriter errorer = null;
	private static String symbol = "btc_cny";
	private static double coin = 0.1;
	private static double step = 0.5;
	private static int max = 8;
	private static int timeout = 60;
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static DecimalFormat df3 = new DecimalFormat("#.###");

	public static String n(Object input){
		return input == null?"":String.valueOf(input);
	}

	public static boolean e(Object input){
		return (input == null || input.equals(""))?true:false;
	}

	private static String rightS(Object value, int length){
		StringBuffer s = new StringBuffer(n(value));
		for(int i = length - s.length(); i > 0; i--){
			s.append(" ");
		}
		return s.toString();
	}

	public static String getNow(){
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date());
	}

	public synchronized static void log(Object text){
		System.out.println(String.valueOf(text));
		if(!saveLog)return;

		try{
			if(logger == null)logger = new FileWriter("OkCoin6.txt", true);
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
			if(errorer == null)errorer = new FileWriter("OkCoin6Err.txt", true);
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
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
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
				String sign = md5(data + "&secret_key=" + ok_secretKey).toUpperCase();
				String tosend = data + "&sign=" + sign;
				URL url = new URL(api);
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
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
			String data = "api_key=" + ok_apiKey;
			JSONObject json = getOKAPI("https://www.okcoin.cn/api/v1/userinfo.do", data, false);
			if(json.getBoolean("result"))return json.getJSONObject("info").getJSONObject("funds");
			if(json.getInt("error_code") == 10001)continue;
			err(json.getInt("error_code") + ":" + data);
			throw new RuntimeException(n(json.getInt("error_code")));
		}
	}

	public static void OKTrade(String type, double amount, double price, String symbol){
		while(true){
			String data = "amount=" + df3.format(amount) + "&api_key=" + ok_apiKey + "&price=" + df2.format(price) + "&symbol=" + symbol + "&type=" + type;
			JSONObject json = getOKAPI("https://www.okcoin.cn/api/v1/trade.do", data, true);
			if(json == null)return;
			if(json.getBoolean("result"))return;
			if(json.getInt("error_code") == 10001)continue;
			if(json.getInt("error_code") == 10010)throw new RuntimeException("not enough cash to " + type);
			if(json.getInt("error_code") == 10016)throw new RuntimeException("not enough coin to " + type);
			err(json.getInt("error_code") + ":" + data);
			throw new RuntimeException(n(json.getInt("error_code")));
		}
	}

	public static JSONArray getOKOrder(String symbol, long orderId){
		while(true){
			String data = "api_key=" + ok_apiKey + "&order_id=" + orderId + "&symbol=" + symbol;
			JSONObject json = getOKAPI("https://www.okcoin.cn/api/v1/order_info.do", data, false);
			if(json.getBoolean("result"))return json.getJSONArray("orders");
			if(json.getInt("error_code") == 10001)continue;
			err(json.getInt("error_code") + ":" + data);
			throw new RuntimeException(n(json.getInt("error_code")));
		}
	}

	public static void cancelOKOrder(String symbol, long orderId){
		while(true){
			String data = "api_key=" + ok_apiKey + "&order_id=" + orderId + "&symbol=" + symbol;
			JSONObject json = getOKAPI("https://www.okcoin.cn/api/v1/cancel_order.do", data, false);
			if(json.getBoolean("result"))return;
			if(json.getInt("error_code") == 10001)continue;
			if(json.getInt("error_code") == 10009)return;
			err(json.getInt("error_code") + ":" + data);
			throw new RuntimeException(n(json.getInt("error_code")));
		}
	}

	public static void cancelAllOKOrder(String symbol){
		JSONArray orders = getOKOrder(symbol, -1);
		for(int i = 0; i < orders.length(); i++){
			long orderId = orders.getJSONObject(i).getLong("order_id");
			cancelOKOrder(symbol, orderId);
		}
	}

	public static JSONObject getOKTicker(String symbol){
		return getPublicAPI("https://www.okcoin.cn/api/v1/ticker.do?symbol=" + symbol);
	}

	private static void bootTrade() throws Exception{
		System.out.println("clean orders...");
		cancelAllOKOrder(symbol);

		System.out.println("waiting...");
		Thread.sleep(5000);

		double lastPrice = getOKTicker(symbol).getJSONObject("ticker").getDouble("last");
		JSONObject json = getOKInfo();
		double freeCoin = json.getJSONObject("free").getDouble(symbol.substring(0, 3));
		double freeCash = json.getJSONObject("free").getDouble("cny");
		System.out.println("coin:" + rightS(df3.format(freeCoin), 7) + " cash:" + rightS(df2.format(freeCash), 9) + " asset:" + df2.format(freeCoin * lastPrice + freeCash));

		System.out.println("rebalancing...");
		if(freeCoin < coin * max){
			double buy = coin * max - freeCoin + 0.001;
			if(buy < 0.01)buy = 0.01;
			OKTrade("buy", buy, lastPrice * 1.2, symbol);
		}else{
			double sell = freeCoin - coin * max - 0.001;
			if(sell >= 0.01)OKTrade("sell", sell, lastPrice * 0.8, symbol);
		}
		System.out.println("waiting...");
		Thread.sleep(5000);

		lastPrice = getOKTicker(symbol).getJSONObject("ticker").getDouble("last");
		json = getOKInfo();
		freeCoin = json.getJSONObject("free").getDouble(symbol.substring(0, 3));
		freeCash = json.getJSONObject("free").getDouble("cny");
		System.out.println("coin:" + rightS(df3.format(freeCoin), 7) + " cash:" + rightS(df2.format(freeCash), 9) + " asset:" + df2.format(freeCoin * lastPrice + freeCash));

		if(freeCash < coin * max * lastPrice * 1.1)throw new RuntimeException("not enough money.");
		System.out.println("start trade.");
	}

	public static void autoTrade() throws Exception{
		bootTrade();

		double profits = 0;
		long cancelId = 0;
		long dealCount = 0;
		long cancelCount = 0;
		while(true){
			Thread.sleep(2000);

			JSONObject ticker = getOKTicker(symbol);
			long date = ticker.getLong("date");
			double lastPrice = ticker.getJSONObject("ticker").getDouble("last");

			if(cancelId != 0){
				JSONObject order = getOKOrder(symbol, cancelId).getJSONObject(0);
				int status = order.getInt("status");
				if(status == 4){
					continue;
				}else if(status == -1){
					cancelCount++;
					double price = order.getDouble("price");
					double remain = order.getDouble("amount") - order.getDouble("deal_amount");
					double lost = 0;
					if("buy".equals(order.getString("type"))){
						if(remain < 0.01)remain = 0.01;
						OKTrade("buy", remain, lastPrice - step, symbol);
						lost = (lastPrice - step - price) * remain;
					}else if(remain >= 0.01){
						OKTrade("sell", remain, lastPrice + step, symbol);
						lost = (price - lastPrice + step) * remain;
					}
					profits -= lost;
					System.out.println("remain:" + df3.format(remain) + " lost:" + df3.format(lost));
					cancelId = 0;
					continue;
				}else{
					cancelId = 0;
					continue;
				}
			}

			JSONArray orders = getOKOrder(symbol, -1);
			int buyOrders = 0;
			int sellOrders = 0;
			for(int i = 0; i < orders.length(); i++){
				if("buy".equals(orders.getJSONObject(i).getString("type"))){
					buyOrders++;
				}else{
					sellOrders++;
				}
			}

			if(buyOrders < max && sellOrders < max){
				OKTrade("buy", coin, lastPrice - step, symbol);
				OKTrade("sell", coin, lastPrice + step, symbol);
				profits += coin * step * 2;
				dealCount++;
			}

			log(rightS(df2.format(lastPrice), 8) + " buy:" + rightS(buyOrders, 3) + " sell:" + rightS(sellOrders, 3) + " " + dealCount + ":" + cancelCount + "\t" + df3.format(profits));

			for(int i = 0; i < orders.length(); i++){
				JSONObject order =  orders.getJSONObject(i);
				long orderId = order.getLong("order_id");
				long createDate = order.getLong("create_date") / 1000;
				String type = order.getString("type");
				double price = order.getDouble("price");
				if(date - createDate > timeout){
					cancelId = orderId;
					cancelOKOrder(symbol, orderId);
					System.out.println("cancel order:" + type + " at " + df2.format(price));
					break;
				}
			}
		}
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = OkCoin6.class.getResourceAsStream("config.properties");
		config.load(in);
		in.close();
		saveLog = Boolean.parseBoolean(config.getProperty("saveLog"));
		saveErr = Boolean.parseBoolean(config.getProperty("saveErr"));
		ok_apiKey = config.getProperty("ok_apiKey");
		ok_secretKey = config.getProperty("ok_secretKey");
		symbol = config.getProperty("symbol");
		coin = Double.parseDouble(config.getProperty("coin"));
		if(coin < 0.01)coin = 0.01;
		step = Double.parseDouble(config.getProperty("step"));
		if(step < 0.01)step = 0.01;
		max = Integer.parseInt(config.getProperty("max"));
		if(max < 1)max = 1;
		timeout = Integer.parseInt(config.getProperty("timeout"));
		if(timeout < 1)timeout = 1;
	}

	public static void main(String[] args) throws Exception{
		System.out.println("starting...");
		init();
		System.out.println("inited.");

		autoTrade();
	}
}
