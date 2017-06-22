
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

public class OKCoin2 {
	private static String ok_partner = "";
	private static String ok_secretKey = "";
	private static FileWriter logger = null;
	private static FileWriter errorer = null;
	private static String symbol = "btc_cny";
	private static double coin = 0.2;
	private static double step = 1;
	private static int max = 10;
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
			if(logger == null)logger = new FileWriter("OKCoin2.txt", true);
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
			if(errorer == null)errorer = new FileWriter("OKCoin2Err.txt", true);
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

	public static void cancelOKOrder(long orderId, String symbol){
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

	public static void cancelAllOKOrder(String symbol){
		JSONArray orders = getOKOrder(symbol);
		for(int i = 0; i < orders.length(); i++){
			long orderId = orders.getJSONObject(i).getLong("orders_id");
			cancelOKOrder(orderId, symbol);
		}
	}

	public static JSONObject getOKTicker(String symbol){
		return getPublicAPI("https://www.okcoin.com/api/ticker.do?symbol=" + symbol).getJSONObject("ticker");
	}

	public static void autoTrade() throws Exception{
		double last_price = Double.parseDouble(df2.format((int)(getOKTicker(symbol).getDouble("last") / step) * step));
		double buy_price = last_price + step;
		double sell_price = last_price - step;
		int deals = 0;
		boolean full = false;
		boolean empty = false;

		while(true){
			JSONObject json = getOKTicker(symbol);
			last_price = json.getDouble("last");
			double ok_coin_buy = json.getDouble("buy");
			double ok_coin_sell = json.getDouble("sell");

			if(ok_coin_buy >= ok_coin_sell){
				log("price error.");
				continue;
			}

			if(last_price >= buy_price){
				while(last_price >= buy_price){
					buy_price += step;
					sell_price += step;
				}

				if(!full && deals < max){
					if(OKTrade("buy", coin, ok_coin_sell * 1.2, symbol)){
						deals++;
						empty = false;
					}else{
						full = true;
					}
				}
			}else if(last_price <= sell_price){
				buy_price -= step;
				sell_price -= step;

				if(!empty){
					if(OKTrade("sell", coin, ok_coin_buy * 0.8, symbol)){
						if(deals > 0)deals--;
						full = false;
					}else{
						empty = true;
					}
				}
			}

			log(last_price + "      \tbuy:" + df2.format(buy_price) + "  \tsell:" + df2.format(sell_price) + " \tdeals:" + deals + (full?" \tFull":"") + (empty?" \tEmpty":""));
			Thread.sleep(2000);
		}
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = OKCoin2.class.getResourceAsStream("config.properties");
		config.load(in);
		in.close();
		ok_partner = config.getProperty("ok_partner");
		ok_secretKey = config.getProperty("ok_secretKey");
		symbol = config.getProperty("symbol");
		coin = Double.parseDouble(config.getProperty("coin"));
		step = Double.parseDouble(config.getProperty("step"));
		max = Integer.parseInt(config.getProperty("max"));

		cancelAllOKOrder(symbol);
	}

	public static void main(String[] args) throws Exception{
		System.out.println("Starting...");
		init();
		System.out.println("inited.");

		autoTrade();
	}
}