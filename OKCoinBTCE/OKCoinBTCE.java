
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
import org.json.JSONArray;
import org.json.JSONObject;

public class OKCoinBTCE {
	private static String ok_partner = "";
	private static String ok_secretKey = "";
	private static String api_url = "https://btc-e.com/tapi";
	private static String api_key = "";
	private static String api_secret = "";
	private static long nonce = -1;
	private static double btc_usd_fee = 0.002;
	private static double ltc_usd_fee = 0.002;
	private static double ltc_btc_fee = 0.002;
	private static FileWriter logger = null;
	private static double btc = 0.01;
	private static double ltc = 0;
	private static double min_ok_cny = 120;
	private static double min_btce_usd = 20;
	private static double min_margin = 1.001;
	private static double btce_btc_fee = 0.004;
	private static double btce_ltc_fee = 0.001;
	private static DecimalFormat df0 = new DecimalFormat("#");
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static DecimalFormat df3 = new DecimalFormat("#.###");
	private static DecimalFormat df8 = new DecimalFormat("#.########");
	private static double ok_btc = 0;
	private static double ok_ltc = 0;
	private static double ok_cny = 0;
	private static double btce_btc = 0;
	private static double btce_ltc = 0;
	private static double btce_usd = 0;
	private static double last_total_btc = 0;
	private static double last_total_ltc = 0;
	private static double last_total_usd = 0;
	private static double last_total_cny = 0;
	private static double btc_cny = 0;
	private static double ltc_cny = 0;
	private static double usd_cny = 6.2;
	private static double ok_ready = 0;
	private static double btce_ready = 0;
	private static double ok_btc_buy = 0;
	private static double ok_btc_sell = 0;
	private static double ok_ltc_buy = 0;
	private static double ok_ltc_sell = 0;
	private static double btce_btc_buy = 0;
	private static double btce_btc_sell = 0;
	private static double btce_ltc_buy = 0;
	private static double btce_ltc_sell = 0;
	private static double btce_ltc_btc = 0;
	private static double btce_btc_ltc = 0;

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
			if(logger == null)logger = new FileWriter("OKCoinBTCE.txt", true);
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

	private static String HmacSHA512(String text) throws Exception{
		SecretKeySpec key = new SecretKeySpec(api_secret.getBytes("UTF-8"), "HmacSHA512");
		Mac mac = Mac.getInstance("HmacSHA512");
		mac.init(key);
		byte[] sha512 = mac.doFinal(n(text).getBytes("UTF-8"));
		return byte2Hex(sha512);
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

	public static JSONObject getAPI(String data, boolean trade){
		int timeout = 2000;
		while(true){
			try{
				String tosend = "nonce=" + (++nonce) + "&" + data;
				URL url = new URL(api_url);
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				con.setReadTimeout(timeout * 2);
				con.setUseCaches(false);
				con.setDoOutput(true);
				con.setDoInput(true);
				con.setRequestMethod("POST");
				con.setRequestProperty("Key", api_key);
				con.setRequestProperty("Sign", HmacSHA512(tosend));
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

	public static JSONObject getInfo(){
		while(true){
			JSONObject json = getAPI("method=getInfo", false);
			if(json.getInt("success") == 1)return json.getJSONObject("return");
			if(json.getString("error").startsWith("invalid nonce parameter;"))continue;
			log(json.getString("error"));
			throw new RuntimeException(json.getString("error"));
		}
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

	public static void Trade(String type, double amount, double rate, String pair){
		while(true){
			JSONObject json = getAPI("method=Trade&amount=" + df8.format(amount) + "&rate=" + df2.format(rate) + "&pair=" + pair + "&type=" + type, true);
			if(json == null)break;
			if(json.getInt("success") == 1)break;
			if(json.getString("error").startsWith("invalid nonce parameter;"))continue;
			log(json.getString("error"));
			throw new RuntimeException(json.getString("error"));
		}
	}

	public static JSONObject getOKTicker(String symbol){
		return getPublicAPI("https://www.okcoin.com/api/ticker.do?symbol=" + symbol).getJSONObject("ticker");
	}

	public static JSONObject getTicker(String pair){
		return getPublicAPI("https://btc-e.com/api/2/" + pair + "/ticker").getJSONObject("ticker");
	}

	public static void getAccountInfo() throws Exception{
		System.out.println("Getting account info...");
		Thread thread1 = new Thread(){
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

		Thread thread2 = new Thread(){
			public void run(){
				JSONObject json = null;
				while(true){
					json = getInfo();
					if(json.getInt("open_orders") > 0){
						System.out.println("Waiting for BTC-e...");
						continue;
					}else{
						json = json.getJSONObject("funds");
						break;
					}
				}
				btce_btc = json.getDouble("btc");
				btce_ltc = json.getDouble("ltc");
				btce_usd = json.getDouble("usd");
			}
		};

		thread1.start();
		thread2.start();

		thread1.join();
		thread2.join();

		double total_btc = ok_btc + btce_btc;
		double total_ltc = ok_ltc + btce_ltc;
		double total_value = total_btc * btc_cny + total_ltc * ltc_cny + btce_usd * usd_cny + ok_cny;
		double margin_btc = total_btc - last_total_btc;
		double margin_ltc = total_ltc - last_total_ltc;
		double margin_usd = btce_usd - last_total_usd;
		double margin_cny = ok_cny - last_total_cny;
		double margin_value = margin_btc * btc_cny + margin_ltc * ltc_cny + margin_usd * usd_cny + margin_cny;
		log("ok_btc=" + ok_btc + ", ok_ltc=" + ok_ltc + ", ok_cny=" + ok_cny + ", btce_btc=" + btce_btc + ", btce_ltc=" + btce_ltc + ", btce_usd=" + btce_usd + ", total_value=" + df8.format(total_value));
		log("btc=" + df8.format(margin_btc) + ", ltc=" + df8.format(margin_ltc) + ", usd=" + df8.format(margin_usd) + ", cny=" + df8.format(margin_cny) + ", margin_value=" + df8.format(margin_value));

		last_total_btc = total_btc;
		last_total_ltc = total_ltc;
		last_total_usd = btce_usd;
		last_total_cny = ok_cny;

		if(ok_btc == 0 || ok_ltc == 0){
			ok_ready = 0;
		}else{
			ok_ready = (ok_btc * btc_cny) / (ok_ltc * ltc_cny);
			if(ok_ready > 1)ok_ready = 1 / ok_ready;
		}

		if(btce_btc == 0 || btce_ltc == 0){
			btce_ready = 0;
		}else{
			btce_ready = (btce_btc * btc_cny) / (btce_ltc * ltc_cny);
			if(btce_ready > 1)btce_ready = 1 / btce_ready;
		}

		if(ok_cny < min_ok_cny || btce_usd < min_btce_usd){
			log("Not enough money.");
			throw new RuntimeException("Not enough money.");
		}
	}

	public static void autoTrade() throws Exception{
		while(true){
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> O:" + df0.format(ok_ready * 100) + "% E:" + df0.format(btce_ready * 100) + "%");

			System.out.println("Getting ticker...");
			Thread thread1 = new Thread(){
				public void run(){
					JSONObject json = getOKTicker("btc_cny");
					btc_cny = json.getDouble("last");
					ok_btc_buy = json.getDouble("buy");
					ok_btc_sell = json.getDouble("sell");
				}
			};
			Thread thread2 = new Thread(){
				public void run(){
					JSONObject json = getOKTicker("ltc_cny");
					ltc_cny = json.getDouble("last");
					ok_ltc_buy = json.getDouble("buy");
					ok_ltc_sell = json.getDouble("sell");
				}
			};
			Thread thread3 = new Thread(){
				public void run(){
					JSONObject json = getTicker("btc_usd");
					btce_btc_buy = json.getDouble("sell");
					btce_btc_sell = json.getDouble("buy");
				}
			};
			Thread thread4 = new Thread(){
				public void run(){
					JSONObject json = getTicker("ltc_usd");
					btce_ltc_buy = json.getDouble("sell");
					btce_ltc_sell = json.getDouble("buy");
				}
			};
			Thread thread5 = new Thread(){
				public void run(){
					JSONObject json = getTicker("ltc_btc");
					btce_ltc_btc = json.getDouble("sell");
					btce_btc_ltc = json.getDouble("buy");
				}
			};

			long start = System.currentTimeMillis();

			thread1.start();
			thread2.start();
			thread3.start();
			thread4.start();
			thread5.start();

			thread1.join();
			thread2.join();
			thread3.join();
			thread4.join();
			thread5.join();

			long end = System.currentTimeMillis();

			if((end - start) > 10000){
				System.out.println("time out.");
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
			if(btce_btc_buy >= btce_btc_sell){
				System.out.println("BTC-e btc price error.");
				continue;
			}
			if(btce_ltc_buy >= btce_ltc_sell){
				System.out.println("BTC-e ltc price error.");
				continue;
			}
			if(btce_ltc_btc >= btce_btc_ltc){
				System.out.println("BTC-e ltc/btc price error.");
				continue;
			}

			double o_ltc_btc_e = 1 * ok_ltc_buy / ok_btc_sell * btce_btc_buy * (1 - btc_usd_fee) / btce_ltc_sell * (1 - ltc_usd_fee) - btce_ltc_fee;
			double o_btc_ltc_e = 1 * ok_btc_buy / ok_ltc_sell * btce_ltc_buy * (1 - ltc_usd_fee) / btce_btc_sell * (1 - btc_usd_fee) - btce_btc_fee;
			double o_ltc_btc_es = 1 * ok_ltc_buy / ok_btc_sell / btce_btc_ltc * (1 - ltc_btc_fee) - btce_ltc_fee;
			double o_btc_ltc_es = 1 * ok_btc_buy / ok_ltc_sell * btce_ltc_btc * (1 - ltc_btc_fee) - btce_btc_fee;

			String mode = "O-E:A";
			double highest = o_ltc_btc_e;
			if(o_btc_ltc_e > highest){
				mode = "O-E:B";
				highest = o_btc_ltc_e;
			}
			if(o_ltc_btc_es >= highest){
				mode = "O-E:AS";
				highest = o_ltc_btc_es;
			}
			if(o_btc_ltc_es >= highest){
				mode = "O-E:BS";
				highest = o_btc_ltc_es;
			}

			if(highest < min_margin){
				System.out.println(mode + " " + highest);
				continue;
			}else{
				log(mode + " " + highest);
			}

			if(mode.equals("O-E:A") || mode.equals("O-E:AS")){
				ltc = btc * ok_btc_sell / ok_ltc_buy;
				ltc = Double.parseDouble(df3.format(ltc));

				if(ok_ltc < ltc || btce_btc < btc){
					log("Not enough coin.");
					Thread.sleep(120000);
				}else{
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

					Thread thread8;
					if(mode.equals("O-E:A")){
						System.out.println("sell btc=" + df8.format(btc));
						System.out.println("buy ltc=" + df8.format(ltc / (1 - ltc_usd_fee)));
						thread8 = new Thread(){
							public void run(){
								Trade("sell", btc, btce_btc_buy * 0.5, "btc_usd");
								Trade("buy", ltc / (1 - ltc_usd_fee), btce_ltc_sell * 1.5, "ltc_usd");
							}
						};
					}else{
						System.out.println("buy ltc=" + df8.format(ltc / (1 - ltc_btc_fee)));
						thread8 = new Thread(){
							public void run(){
								Trade("buy", ltc / (1 - ltc_btc_fee), btce_btc_ltc * 1.5, "ltc_btc");
							}
						};
					}

					thread6.start();
					thread7.start();
					thread8.start();

					thread6.join();
					thread7.join();
					thread8.join();
				}
			}else if(mode.equals("O-E:B") || mode.equals("O-E:BS")){
				ltc = btc * ok_btc_buy / ok_ltc_sell;
				ltc = Double.parseDouble(df3.format(ltc));

				if(ok_btc < btc || btce_ltc < ltc){
					log("Not enough coin.");
					Thread.sleep(120000);
				}else{
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

					Thread thread8;
					if(mode.equals("O-E:B")){
						System.out.println("sell ltc=" + df8.format(ltc));
						System.out.println("buy btc=" + df8.format(btc / (1 - btc_usd_fee)));
						thread8 = new Thread(){
							public void run(){
								Trade("sell", ltc, btce_ltc_buy * 0.5, "ltc_usd");
								Trade("buy", btc / (1 - btc_usd_fee), btce_btc_sell * 1.5, "btc_usd");
							}
						};
					}else{
						System.out.println("sell ltc=" + df8.format(ltc));
						thread8 = new Thread(){
							public void run(){
								Trade("sell", ltc, btce_ltc_btc * 0.5, "ltc_btc");
							}
						};
					}

					thread6.start();
					thread7.start();
					thread8.start();

					thread6.join();
					thread7.join();
					thread8.join();
				}
			}

			getAccountInfo();
		}
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = OKCoinBTCE.class.getResourceAsStream("config.properties");
		config.load(in);
		in.close();
		ok_partner = config.getProperty("ok_partner");
		ok_secretKey = config.getProperty("ok_secretKey");
		api_key = config.getProperty("api_key");
		api_secret = config.getProperty("api_secret");
		btc = Double.parseDouble(config.getProperty("btc"));
		min_ok_cny = Double.parseDouble(config.getProperty("min_ok_cny"));
		min_btce_usd = Double.parseDouble(config.getProperty("min_btce_usd"));
		min_margin = Double.parseDouble(config.getProperty("min_margin"));
		btce_btc_fee = Double.parseDouble(config.getProperty("btce_btc_fee"));
		btce_ltc_fee = Double.parseDouble(config.getProperty("btce_ltc_fee"));
		usd_cny = Double.parseDouble(config.getProperty("usd_cny"));

		JSONObject json = getAPI("", false);
		String error = json.getString("error");
		nonce = Long.parseLong(error.substring(error.indexOf("key:") + 4, error.indexOf(",")));
		json = getPublicAPI("https://btc-e.com/api/2/btc_usd/fee");
		btc_usd_fee = json.getDouble("trade") / 100;
		json = getPublicAPI("https://btc-e.com/api/2/ltc_usd/fee");
		ltc_usd_fee = json.getDouble("trade") / 100;
		json = getPublicAPI("https://btc-e.com/api/2/ltc_btc/fee");
		ltc_btc_fee = json.getDouble("trade") / 100;

		json = getOKTicker("btc_cny");
		btc_cny = json.getDouble("last");
		json = getOKTicker("ltc_cny");
		ltc_cny = json.getDouble("last");

		getAccountInfo();
	}

	public static void main(String[] args) throws Exception{
		System.out.println("Starting...");
		init();
		System.out.println("inited.");

		autoTrade();
	}
}