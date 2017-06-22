
import java.io.BufferedReader;
import java.io.FileReader;
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
import org.json.JSONObject;

public class OKCoin {
	private static String ok_partner = "";
	private static String ok_secretKey = "";
	private static FileWriter logger = null;
	private static double btc = 0.01;
	private static double percent = 1;
	private static int short_circle = 900;
	private static int long_circle = 3600;
	private static boolean dynamic = true;
	private static double coin = 0;
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static DecimalFormat df3 = new DecimalFormat("#.###");

	public static String n(Object input){
		return input == null?"":String.valueOf(input);
	}

	public static boolean e(Object input){
		return (input == null || input.equals(""))?true:false;
	}

	public static String getNow(){
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}

	public static void log(Object text){
		try{
			if(logger == null)logger = new FileWriter("OKCoin.txt", true);
			logger.write(getNow() + ": " + String.valueOf(text));
			logger.write("\r\n");
			logger.flush();
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
				if(timeout < 60000)timeout += 2000;
			}catch(Exception e){
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	public static JSONObject getOKAPI(String api, String data, boolean trade){
		int timeout = 2000;
		while(true){
			try{
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
					if(trade)return null;
					throw e;
				}
			}catch(IOException e){
				if(timeout < 60000)timeout += 2000;
			}catch(Exception e){
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	public static JSONObject getOKInfo(){
		while(true){
			JSONObject json = getOKAPI("https://www.okcoin.com/api/userinfo.do", "partner=" + ok_partner, false);
			if(json.getBoolean("result"))return json.getJSONObject("info").getJSONObject("funds");
			if(json.getInt("errorCode") == 10001)continue;
			log(json.getInt("errorCode"));
			throw new RuntimeException(n(json.getInt("errorCode")));
		}
	}

	public static boolean OKTrade(String type, double amount, double rate, String symbol){
		while(true){
			JSONObject json = getOKAPI("https://www.okcoin.com/api/trade.do", "amount=" + df3.format(amount) + "&partner=" + ok_partner + "&rate=" + df2.format(rate) + "&symbol=" + symbol + "&type=" + type, true);
			if(json == null)return true;
			if(json.getBoolean("result"))return true;
			if(json.getInt("errorCode") == 10001)continue;
			if(json.getInt("errorCode") == 10010)return false;
			log(json.getInt("errorCode"));
			throw new RuntimeException(n(json.getInt("errorCode")));
		}
	}

	public static JSONObject getOKTicker(String symbol){
		return getPublicAPI("https://www.okcoin.com/api/ticker.do?symbol=" + symbol).getJSONObject("ticker");
	}

	public static void autoTrade() throws Exception{
		double prices[] = new double[long_circle];
		double short_average = 0;
		double long_average = 0;
		double buy_price = 0;
		double sell_price = 0;
		double cut_price = 0;
		boolean full = false;
		boolean empty = (coin == 0);
		while(true){
			log(prices[0] + "     \t" + df2.format(short_average) + "     \t" + df2.format(long_average) + "     \t" + df3.format(coin) + (full?"\tFull":"") + (empty?"\tEmpty":""));
			Thread.sleep(2000);

			JSONObject json = getOKTicker("btc_cny");
			double btc_cny = json.getDouble("last");
			double ok_btc_buy = json.getDouble("buy");
			double ok_btc_sell = json.getDouble("sell");

			System.arraycopy(prices, 0, prices, 1, prices.length - 1);
			prices[0] = btc_cny;

			if(ok_btc_buy >= ok_btc_sell){
				log("price error.");
				continue;
			}

			double total = 0;
			int count = 0;
			for(int i = 0; i < prices.length; i++){
				total += prices[i];
				if(prices[i] != 0)count++;
				if((i + 1) == short_circle)short_average = total / count;
				if((i + 1) == long_circle)long_average = total / count;
			}

			if(dynamic){
				if(short_average < long_average){
					buy_price = prices[0] * (100 + percent) / 100;
					sell_price = prices[0] * (100 - percent) / 100;

					if(empty)continue;
					if(OKTrade("sell", coin, ok_btc_buy * 0.5, "btc_cny")){
						full = false;
						empty = true;
						coin = 0;
					}else{
						log("cut loss failed.");
						throw new RuntimeException("cut loss failed.");
					}
				}else if(short_average > long_average && prices[0] > buy_price){
					buy_price = prices[0] * (100 + percent) / 100;
					sell_price = prices[0] * (100 - percent) / 100;

					if(full)continue;
					if(OKTrade("buy", btc, ok_btc_sell * 1.5, "btc_cny")){
						empty = false;
						coin += btc;
					}else{
						full = true;
					}
				}else if(prices[0] < sell_price){
					buy_price = prices[0] * (100 + percent) / 100;
					sell_price = prices[0] * (100 - percent) / 100;

					if(empty)continue;
					if(OKTrade("sell", btc, ok_btc_buy * 0.5, "btc_cny")){
						full = false;
						coin -= btc;
						if(coin == 0)empty = true;
					}else{
						empty = true;
					}
				}
			}else{
				if(short_average < long_average || prices[0] < cut_price){
					buy_price = prices[0] * (100 + percent) / 100;
					sell_price = prices[0] * (100 - percent) / 100;

					if(empty)continue;
					if(OKTrade("sell", coin, ok_btc_buy * 0.5, "btc_cny")){
						full = false;
						empty = true;
						coin = 0;
						cut_price = 0;
					}else{
						log("cut loss failed.");
						throw new RuntimeException("cut loss failed.");
					}
				}else if(short_average > long_average && prices[0] > buy_price){
					buy_price = prices[0] * (100 + percent) / 100;
					sell_price = prices[0] * (100 - percent) / 100;

					if(full)continue;
					if(OKTrade("buy", btc, ok_btc_sell * 1.5, "btc_cny")){
						empty = false;
						coin += btc;
						cut_price = prices[0] / (100 + percent * (coin / btc - 1) / 2) * (100 - percent / (coin / btc));
					}else{
						full = true;
					}
				}
			}
		}
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = OKCoin.class.getResourceAsStream("config.properties");
		config.load(in);
		in.close();
		ok_partner = config.getProperty("ok_partner");
		ok_secretKey = config.getProperty("ok_secretKey");
		btc = Double.parseDouble(config.getProperty("btc"));
		percent = Double.parseDouble(config.getProperty("percent"));
		short_circle = Integer.parseInt(config.getProperty("short_circle"));
		long_circle = Integer.parseInt(config.getProperty("long_circle"));
		if(short_circle >= long_circle)throw new Exception("wrong circle value");
		dynamic = Boolean.parseBoolean(config.getProperty("dynamic"));

		JSONObject json = getOKInfo();
		json = json.getJSONObject("free");
		coin = json.getDouble("btc");
	}

	public static void main(String[] args) throws Exception{
		System.out.println("Starting...");
		init();
		System.out.println("inited.");

		autoTrade();
	}
}