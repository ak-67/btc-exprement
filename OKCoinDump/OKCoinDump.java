
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

public class OKCoinDump {
	private static String ok_partner = "";
	private static String ok_secretKey = "";
	private static FileWriter logger = null;
	private static FileWriter errorer = null;
	private static String symbol = "ltc_cny";
	private static String type = "sell";
	private static double coin = 0.1;
	private static double price = 200;
	private static int clean = 100;
	private static int orderCount = 0;
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static DecimalFormat df3 = new DecimalFormat("#.###");

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
		try{
			if(logger == null)logger = new FileWriter("OKCoinDump.txt", true);
			logger.write(getNow() + ": " + String.valueOf(text));
			logger.write("\r\n");
			logger.flush();
			System.out.println(String.valueOf(text));
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	public synchronized static void err(Object text){
		try{
			if(errorer == null)errorer = new FileWriter("OKCoinDumpErr.txt", true);
			errorer.write(getNow() + ": " + String.valueOf(text));
			errorer.write("\r\n");
			errorer.flush();
			System.out.println(String.valueOf(text));
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
			JSONObject json = getOKAPI("https://www.okcoin.com/api/userinfo.do", data, false);
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
			JSONObject json = getOKAPI("https://www.okcoin.com/api/trade.do", data, true);
			if(json == null)return true;
			if(json.getBoolean("result"))return true;
			if(json.getInt("errorCode") == 10001)continue;
			if(json.getInt("errorCode") == 10010)return false;
			err(json.getInt("errorCode") + ":" + data);
			if(json.getInt("errorCode") == 10005)continue;
			throw new RuntimeException(n(json.getInt("errorCode")));
		}
	}

	public static void OKTrade(List<Thread> threads, String type, double amount, double rate, String symbol){
		class OKTradeThread extends Thread {
			private String type;
			private double amount;
			private double rate;
			private String symbol;

			public OKTradeThread(String type, double amount, double rate, String symbol){
				this.type = type;
				this.amount = amount;
				this.rate = rate;
				this.symbol = symbol;
			}

			public void run(){
				OKTrade(type, amount, rate, symbol);
				System.out.println(++orderCount);
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
			if(json.getInt("errorCode") == 10005)continue;
			throw new RuntimeException(n(json.getInt("errorCode")));
		}
	}

	public static void cancelOKOrder(long orderId, String symbol){
		while(true){
			String data = "order_id=" + orderId + "&partner=" + ok_partner + "&symbol=" + symbol;
			JSONObject json = getOKAPI("https://www.okcoin.com/api/cancelorder.do", data, false);
			if(json.getBoolean("result"))break;
			if(json.getInt("errorCode") == 10001)continue;
			if(json.getInt("errorCode") == 10009)break;
			err(json.getInt("errorCode") + ":" + data);
			if(json.getInt("errorCode") == 10005)continue;
			throw new RuntimeException(n(json.getInt("errorCode")));
		}
	}

	public static void cancelAllOKOrder(String symbol) throws Exception{
		while(true){
			JSONArray orders = getOKOrder(symbol);
			if(orders.length() == 0){
				orderCount = 0;
				return;
			}

			for(int i = 0; i < orders.length(); i++){
				long orderId = orders.getJSONObject(i).getLong("orders_id");
				cancelOKOrder(orderId, symbol);
				System.out.println(--orderCount);
			}
		}
	}

	public static void wait4Threads(List<Thread> threads) throws Exception{
		for(Thread thread : threads){
			thread.join();
		}
	}

	public static void autoTrade() throws Exception{
		while(true){
			cancelAllOKOrder(symbol);

			JSONObject json = getOKInfo();
			JSONObject free = json.getJSONObject("free");
			JSONObject freezed = json.getJSONObject("freezed");

			log("free: cny=" + df2.format(free.getDouble("cny")) + " btc=" + df2.format(free.getDouble("btc")) + " ltc=" + df2.format(free.getDouble("ltc"))
				+ "\tfreezed: cny=" + df2.format(freezed.getDouble("cny")) + " btc=" + df2.format(freezed.getDouble("btc")) + " ltc=" + df2.format(freezed.getDouble("ltc")));

			List<Thread> threads = new ArrayList<Thread>();
			for(int i = 0; i < clean; i++){
				OKTrade(threads, type, coin, price, symbol);
			}

			Thread.sleep(5000);
			cancelAllOKOrder(symbol);
			wait4Threads(threads);

			Thread.sleep(2000);
		}
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = OKCoinDump.class.getResourceAsStream("config.properties");
		config.load(in);
		in.close();
		ok_partner = config.getProperty("ok_partner");
		ok_secretKey = config.getProperty("ok_secretKey");
		symbol = config.getProperty("symbol");
		type = config.getProperty("type");
		coin = Double.parseDouble(config.getProperty("coin"));
		price = Double.parseDouble(config.getProperty("price"));
		clean = Integer.parseInt(config.getProperty("clean"));
	}

	public static void main(String[] args) throws Exception{
		System.out.println("Starting...");
		init();
		System.out.println("inited.");

		autoTrade();
	}
}