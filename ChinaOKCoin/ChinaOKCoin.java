
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
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChinaOKCoin {
	private static String china_api = "https://api.btcchina.com/api_trade_v1.php";
	private static String china_accessKey = "";
	private static String china_secretKey = "";
	private static double china_fee_btc = 0;
	private static double china_fee_ltc = 0;
	private static String ok_partner = "";
	private static String ok_secretKey = "";
	private static FileWriter logger = null;
	private static double btc = 0.01;
	private static double ltc = 0;
	private static double min_china_cny = 120;
	private static double min_ok_cny = 120;
	private static double min_margin = 1.001;
	private static DecimalFormat df0 = new DecimalFormat("#");
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static DecimalFormat df3 = new DecimalFormat("#.###");
	private static DecimalFormat df8 = new DecimalFormat("#.########");
	private static double china_btc = 0;
	private static double china_ltc = 0;
	private static double china_cny = 0;
	private static double ok_btc = 0;
	private static double ok_ltc = 0;
	private static double ok_cny = 0;
	private static double last_total_btc = 0;
	private static double last_total_ltc = 0;
	private static double last_total_cny = 0;
	private static double btc_cny = 0;
	private static double ltc_cny = 0;
	private static double china_ready = 0;
	private static double ok_ready = 0;
	private static double china_btc_buy = 0;
	private static double china_btc_sell = 0;
	private static double china_ltc_buy = 0;
	private static double china_ltc_sell = 0;
	private static double ok_btc_buy = 0;
	private static double ok_btc_sell = 0;
	private static double ok_ltc_buy = 0;
	private static double ok_ltc_sell = 0;

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
			if(logger == null)logger = new FileWriter("ChinaOKCoin.txt", true);
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

	private static String HmacSHA1(String text) throws Exception{
		SecretKeySpec key = new SecretKeySpec(china_secretKey.getBytes("UTF-8"), "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(key);
		byte[] sha1 = mac.doFinal(n(text).getBytes("UTF-8"));
		return byte2Hex(sha1);
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

	public static JSONObject getChinaAPI(String method, String params, boolean trade){
		int timeout = 2000;
		while(true){
			try{
				String tonce = n(System.currentTimeMillis() * 1000);
				String tosend = "tonce=" + tonce + "&accesskey=" + china_accessKey + "&requestmethod=post&id=1&method=" + method + "&params=" + params.replace("\"", "").replace("null", "");
				String Authorization = china_accessKey + ":" + HmacSHA1(tosend);
				Authorization = "Basic " + DatatypeConverter.printBase64Binary(Authorization.getBytes("UTF-8"));
				tosend = "{\"method\": \"" + method + "\", \"params\": [" + params + "], \"id\": 1}";
				URL url = new URL(china_api);
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				con.setReadTimeout(timeout * 2);
				con.setUseCaches(false);
				con.setDoOutput(true);
				con.setDoInput(true);
				con.setRequestMethod("POST");
				con.setRequestProperty("Json-Rpc-Tonce", tonce);
				con.setRequestProperty("Authorization", Authorization);
				con.setConnectTimeout(timeout);
				con.connect();
				con.getOutputStream().write(tosend.getBytes("UTF-8"));
				con.getOutputStream().flush();
				con.getOutputStream().close();
				try{
					if(con.getResponseCode() == 401){
						log("Server returned HTTP response code: 401.");
						throw new RuntimeException("Server returned HTTP response code: 401.");
					}
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

	public static JSONObject getChinaInfo(){
		JSONObject json = getChinaAPI("getAccountInfo", "", false);
		if(json.has("result"))return json.getJSONObject("result");
		log(json.getJSONObject("error").getString("message"));
		throw new RuntimeException(json.getJSONObject("error").getString("message"));
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

	public static void ChinaTrade(String method, double amount, double price, String market){//buyOrder2 or sellOrder2, BTCCNY or LTCCNY or LTCBTC
		JSONObject json = getChinaAPI(method, df2.format(price) + "," + df3.format(amount) + ",\"" + market + "\"", true);
		if(json == null)return;
		if(json.has("result"))return;
		log(json.getJSONObject("error").getString("message"));
		throw new RuntimeException(json.getJSONObject("error").getString("message"));
	}

	public static void OKTrade(String type, double amount, double rate, String symbol){
		while(true){
			JSONObject json = getOKAPI("https://www.okcoin.com/api/trade.do", "amount=" + df3.format(amount) + "&partner=" + ok_partner + "&rate=" + df2.format(rate) + "&symbol=" + symbol + "&type=" + type, true);
			if(json == null)break;
			if(json.getBoolean("result"))break;
			if(json.getInt("errorCode") == 10001)continue;
			log(json.getInt("errorCode"));
			throw new RuntimeException(n(json.getInt("errorCode")));
		}
	}

	public static JSONObject getChinaTicker(String market){//cnybtc or cnyltc
		return getPublicAPI("https://data.btcchina.com/data/ticker?market=" + market).getJSONObject("ticker");
	}

	public static JSONObject getOKTicker(String symbol){
		return getPublicAPI("https://www.okcoin.com/api/ticker.do?symbol=" + symbol).getJSONObject("ticker");
	}

	public static void getAccountInfo() throws Exception{
		System.out.println("Getting account info...");
		Thread thread1 = new Thread(){
			public void run(){
				JSONObject json = null;
				while(true){
					json = getChinaInfo();
					double freezed_btc = json.getJSONObject("frozen").getJSONObject("btc").getDouble("amount");
					double freezed_ltc = json.getJSONObject("frozen").getJSONObject("ltc").getDouble("amount");
					double freezed_cny = json.getJSONObject("frozen").getJSONObject("cny").getDouble("amount");
					double freezed_value = freezed_btc * btc_cny + freezed_ltc * ltc_cny + freezed_cny;
					if(freezed_value > 1){
						System.out.println("Waiting for BTCChina...");
						continue;
					}else{
						json = json.getJSONObject("balance");
						break;
					}
				}
				china_btc = json.getJSONObject("btc").getDouble("amount");
				china_ltc = json.getJSONObject("ltc").getDouble("amount");
				china_cny = json.getJSONObject("cny").getDouble("amount");
			}
		};

		Thread thread2 = new Thread(){
			public void run(){
				JSONObject json = null;
				while(true){
					json = getOKInfo();
					double freezed_btc = json.getJSONObject("freezed").getDouble("btc");
					double freezed_ltc = json.getJSONObject("freezed").getDouble("ltc");
					double freezed_cny = json.getJSONObject("freezed").getDouble("cny");
					double freezed_value = freezed_btc * btc_cny + freezed_ltc * ltc_cny + freezed_cny;
					if(freezed_value > 1){
						System.out.println("Waiting for OKCoin...");
						continue;
					}else{
						json = json.getJSONObject("free");
						break;
					}
				}
				ok_btc = json.getDouble("btc");
				ok_ltc = json.getDouble("ltc");
				ok_cny = json.getDouble("cny");
			}
		};

		thread1.start();
		thread2.start();

		thread1.join();
		thread2.join();

		double total_btc = china_btc + ok_btc;
		double total_ltc = china_ltc + ok_ltc;
		double total_cny = china_cny + ok_cny;
		double total_value = total_btc * btc_cny + total_ltc * ltc_cny + total_cny;
		double margin_btc = total_btc - last_total_btc;
		double margin_ltc = total_ltc - last_total_ltc;
		double margin_cny = total_cny - last_total_cny;
		double margin_value = margin_btc * btc_cny + margin_ltc * ltc_cny + margin_cny;
		log("china_btc=" + china_btc + ", china_ltc=" + china_ltc + ", china_cny=" + china_cny + ", ok_btc=" + ok_btc + ", ok_ltc=" + ok_ltc + ", ok_cny=" + ok_cny + ", total_value=" + df8.format(total_value));
		log("btc=" + df8.format(margin_btc) + ", ltc=" + df8.format(margin_ltc) + ", cny=" + df8.format(margin_cny) + ", margin_value=" + df8.format(margin_value));

		last_total_btc = total_btc;
		last_total_ltc = total_ltc;
		last_total_cny = total_cny;

		if(china_btc == 0 || china_ltc == 0){
			china_ready = 0;
		}else{
			china_ready = (china_btc * btc_cny) / (china_ltc * ltc_cny);
			if(china_ready > 1)china_ready = 1 / china_ready;
		}

		if(ok_btc == 0 || ok_ltc == 0){
			ok_ready = 0;
		}else{
			ok_ready = (ok_btc * btc_cny) / (ok_ltc * ltc_cny);
			if(ok_ready > 1)ok_ready = 1 / ok_ready;
		}

		if(china_cny < min_china_cny || ok_cny < min_ok_cny){
			log("Not enough money.");
			throw new RuntimeException("Not enough money.");
		}
	}

	public static void autoTrade() throws Exception{
		while(true){
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> C:" + df0.format(china_ready * 100) + "% O:" + df0.format(ok_ready * 100) + "%");

			System.out.println("Getting ticker...");
			Thread thread1 = new Thread(){
				public void run(){
					JSONObject json = getChinaTicker("cnybtc");
					china_btc_buy = json.getDouble("buy");
					china_btc_sell = json.getDouble("sell");
				}
			};
			Thread thread2 = new Thread(){
				public void run(){
					JSONObject json = getChinaTicker("cnyltc");
					china_ltc_buy = json.getDouble("buy");
					china_ltc_sell = json.getDouble("sell");
				}
			};
			Thread thread3 = new Thread(){
				public void run(){
					JSONObject json = getOKTicker("btc_cny");
					btc_cny = json.getDouble("last");
					ok_btc_buy = json.getDouble("buy");
					ok_btc_sell = json.getDouble("sell");
				}
			};
			Thread thread4 = new Thread(){
				public void run(){
					JSONObject json = getOKTicker("ltc_cny");
					ltc_cny = json.getDouble("last");
					ok_ltc_buy = json.getDouble("buy");
					ok_ltc_sell = json.getDouble("sell");
				}
			};

			long start = System.currentTimeMillis();

			thread1.start();
			thread2.start();
			thread3.start();
			thread4.start();

			thread1.join();
			thread2.join();
			thread3.join();
			thread4.join();

			long end = System.currentTimeMillis();

			if((end - start) > 10000){
				System.out.println("time out.");
				continue;
			}

			if(china_btc_buy >= china_btc_sell){
				System.out.println("BTCChina btc price error.");
				continue;
			}
			if(china_ltc_buy >= china_ltc_sell){
				System.out.println("BTCChina ltc price error.");
				continue;
			}
			if(ok_btc_buy >= ok_btc_sell){
				System.out.println("OKCoin btc price error.");
				continue;
			}
			if(ok_ltc_buy >= ok_ltc_sell){
				System.out.println("OKCoin ltc price error.");
				continue;
			}

			double c_ltc_btc_o = 1 * china_ltc_buy / china_btc_sell * ok_btc_buy / ok_ltc_sell;
			double c_btc_ltc_o = 1 * china_btc_buy / china_ltc_sell * ok_ltc_buy / ok_btc_sell;

			String mode = "C-O:A";
			double highest = c_ltc_btc_o;
			if(c_btc_ltc_o > highest){
				mode = "C-O:B";
				highest = c_btc_ltc_o;
			}

			if(highest < min_margin){
				System.out.println(mode + " " + highest);
				continue;
			}else{
				log(mode + " " + highest);
			}

			if(mode.equals("C-O:A")){
				ltc = btc * china_btc_sell / china_ltc_buy;
				ltc = Double.parseDouble(df3.format(ltc));

				if(china_ltc < ltc || ok_btc < btc){
					log("Not enough coin.");
					Thread.sleep(120000);
				}else{
					System.out.println("sell ltc=" + df3.format(ltc));
					System.out.println("buy btc=" + df3.format(btc));
					Thread thread5 = new Thread(){
						public void run(){
							ChinaTrade("sellOrder2", ltc, china_ltc_buy * 0.5, "LTCCNY");
							ChinaTrade("buyOrder2", btc, china_btc_sell * 1.5, "BTCCNY");
						}
					};

					System.out.println("sell btc=" + df3.format(btc));
					Thread thread6 = new Thread(){
						public void run(){
							OKTrade("sell", btc, ok_btc_buy * 0.5, "btc_cny");
						}
					};

					System.out.println("buy ltc=" + df3.format(ltc));
					Thread thread7 = new Thread(){
						public void run(){
							OKTrade("buy", ltc, ok_ltc_sell * 1.5, "ltc_cny");
						}
					};

					thread5.start();
					thread6.start();
					thread7.start();

					thread5.join();
					thread6.join();
					thread7.join();
				}
			}else if(mode.equals("C-O:B")){
				ltc = btc * china_btc_buy / china_ltc_sell;
				ltc = Double.parseDouble(df3.format(ltc));

				if(china_btc < btc || ok_ltc < ltc){
					log("Not enough coin.");
					Thread.sleep(120000);
				}else{
					System.out.println("sell btc=" + df3.format(btc));
					System.out.println("buy ltc=" + df3.format(ltc));
					Thread thread5 = new Thread(){
						public void run(){
							ChinaTrade("sellOrder2", btc, china_btc_buy * 0.5, "BTCCNY");
							ChinaTrade("buyOrder2", ltc, china_ltc_sell * 1.5, "LTCCNY");
						}
					};

					System.out.println("sell ltc=" + df3.format(ltc));
					Thread thread6 = new Thread(){
						public void run(){
							OKTrade("sell", ltc, ok_ltc_buy * 0.5, "ltc_cny");
						}
					};

					System.out.println("buy btc=" + df3.format(btc));
					Thread thread7 = new Thread(){
						public void run(){
							OKTrade("buy", btc, ok_btc_sell * 1.5, "btc_cny");
						}
					};

					thread5.start();
					thread6.start();
					thread7.start();

					thread5.join();
					thread6.join();
					thread7.join();
				}
			}

			getAccountInfo();
		}
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = ChinaOKCoin.class.getResourceAsStream("config.properties");
		config.load(in);
		in.close();
		china_accessKey = config.getProperty("china_accessKey");
		china_secretKey = config.getProperty("china_secretKey");
		ok_partner = config.getProperty("ok_partner");
		ok_secretKey = config.getProperty("ok_secretKey");
		btc = Double.parseDouble(config.getProperty("btc"));
		min_china_cny = Double.parseDouble(config.getProperty("min_china_cny"));
		min_ok_cny = Double.parseDouble(config.getProperty("min_ok_cny"));
		min_margin = Double.parseDouble(config.getProperty("min_margin"));

		JSONObject json = getChinaInfo().getJSONObject("profile");
		china_fee_btc = json.getDouble("trade_fee") / 100;
		china_fee_ltc = json.getDouble("trade_fee_cnyltc") / 100;

		json = getOKTicker("btc_cny");
		btc_cny = json.getDouble("last");
		json = getOKTicker("ltc_cny");
		ltc_cny = json.getDouble("last");

		getAccountInfo();

		if(china_fee_btc > 0 || china_fee_ltc > 0)throw new RuntimeException("BTCChina fee:" + china_fee_btc);
	}

	public static void main(String[] args) throws Exception{
		System.out.println("Starting...");
		init();
		System.out.println("inited.");

		autoTrade();
	}
}