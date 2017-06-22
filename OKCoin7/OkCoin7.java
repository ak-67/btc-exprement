
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

public class OkCoin7 {
	private static boolean saveLog = true;
	private static boolean saveErr = false;
	private static String ok_apiKey = "";
	private static String ok_secretKey = "";
	private static FileWriter logger = null;
	private static FileWriter errorer = null;
	private static String symbol = "btc_cny";
	private static double coin = 0.05;
	private static double step = 0.1;
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static DecimalFormat df3 = new DecimalFormat("#.###");
	private static double lastPrice = 0;
	private static double startAssets = 0;
	private static boolean full = false;
	private static boolean empty = false;

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
			if(logger == null)logger = new FileWriter("OkCoin7.txt", true);
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
			if(errorer == null)errorer = new FileWriter("OkCoin7Err.txt", true);
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

	public static boolean OKTrade(String type, double amount, double price, String symbol){
		while(true){
			String data = "amount=" + df3.format(amount) + "&api_key=" + ok_apiKey + "&price=" + df2.format(price) + "&symbol=" + symbol + "&type=" + type;
			JSONObject json = getOKAPI("https://www.okcoin.cn/api/v1/trade.do", data, true);
			if(json == null)return true;
			if(json.getBoolean("result"))return true;
			if(json.getInt("error_code") == 10001)continue;
			if(json.getInt("error_code") == 10010)return false;
			if(json.getInt("error_code") == 10016)return false;
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

		lastPrice = getOKTicker(symbol).getJSONObject("ticker").getDouble("last");
		JSONObject json = getOKInfo();
		double coins = json.getJSONObject("free").getDouble(symbol.substring(0, 3));
		coins += json.getJSONObject("freezed").getDouble(symbol.substring(0, 3));
		double cash = json.getJSONObject("free").getDouble("cny");
		cash += json.getJSONObject("freezed").getDouble("cny");
		startAssets = coins * lastPrice + cash;
		System.out.println("start at price:" + rightS(df2.format(lastPrice), 7) + " with assets:" + df2.format(startAssets));
	}

	public static void autoTrade() throws Exception{
		bootTrade();

		while(true){
			Thread.sleep(2000);

			double price = getOKTicker(symbol).getJSONObject("ticker").getDouble("last");
			if(price - lastPrice > step){
				if(!full){
					if(OKTrade("buy", coin, price * 1.2, symbol)){
						empty = false;
					}else{
						full = true;
					}
				}
				lastPrice = price;
			}else if(lastPrice - price > step){
				if(!empty){
					if(OKTrade("sell", coin, price * 0.8, symbol)){
						full = false;
					}else{
						empty = true;
					}
				}
				lastPrice = price;
			}

			JSONObject json = getOKInfo();
			double coins = json.getJSONObject("free").getDouble(symbol.substring(0, 3));
			coins += json.getJSONObject("freezed").getDouble(symbol.substring(0, 3));
			double cash = json.getJSONObject("free").getDouble("cny");
			cash += json.getJSONObject("freezed").getDouble("cny");
			double assets = coins * price + cash;

			log(rightS(df2.format(price), 7) + " coins:" + rightS(df3.format(coins), 7)
					+ ((full && empty)?" error":(full?"  full":(empty?" empty":"      ")))
					+ " assets:" + rightS(df2.format(assets), 9) + " profits:" + df2.format(assets - startAssets));
		}
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = OkCoin7.class.getResourceAsStream("config.properties");
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
	}

	public static void main(String[] args) throws Exception{
		System.out.println("starting...");
		init();
		System.out.println("inited.");

		autoTrade();
	}
}
