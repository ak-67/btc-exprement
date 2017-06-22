
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONObject;

public class OKCoinDemo2 {
	private static FileWriter logger = null;
	private static double money = 10000;
	private static boolean reset = true;
	private static int reset_hour = 9;
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static double percent[] = new double[]{0.1, 0.2, 0.3, 0.5, 0.7, 1, 2, 3, 5, 7};
	private static double btc[] = new double[]{0.01, 0.02, 0.03, 0.05, 0.07, 0.1, 0.2, 0.3, 0.5, 0.7, 1};
	private static int rate[] = new int[]{1, 2, 3, 4, 5};
	private static double accounts[][][] = new double[percent.length][rate.length][btc.length];
	private static double coins[][][] = new double[percent.length][rate.length][btc.length];
	private static double last_price = 0;
	private static double buy_price[][][] = new double[percent.length][rate.length][btc.length];
	private static double sell_price[][][] = new double[percent.length][rate.length][btc.length];
	private static double cut_price[][][] = new double[percent.length][rate.length][btc.length];
	private static boolean doReset = false;
	private static boolean doPrint = false;

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
			if(logger == null)logger = new FileWriter("OKCoinDemo2.txt", true);
			logger.write(getNow() + ": " + String.valueOf(text));
			logger.write("\r\n");
			logger.flush();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
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

	public static boolean OKTrade(int p, int r, int b, String type, double amount, double rate){
		if(type.equals("buy")){
			if(accounts[p][r][b] < (amount * rate))return false;
			accounts[p][r][b] -= amount * rate;
			coins[p][r][b] += amount;
		}else{
			if(coins[p][r][b] < amount)return false;
			coins[p][r][b] -= amount;
			accounts[p][r][b] += amount * rate;
		}
		return true;
	}

	public static JSONObject getOKTicker(String symbol){
		return getPublicAPI("https://www.okcoin.com/api/ticker.do?symbol=" + symbol).getJSONObject("ticker");
	}

	public static void autoReset(){
		Calendar time = Calendar.getInstance();

		if(time.get(Calendar.HOUR) != reset_hour){
			doReset = true;
			return;
		}

		if(!doReset)return;

		for(int p = 0; p < percent.length; p++){
			for(int r = 0; r < rate.length; r++){
				for(int b = 0; b < btc.length; b++){
					accounts[p][r][b] = money;
					coins[p][r][b] = 0;
					buy_price[p][r][b] = 0;
					sell_price[p][r][b] = 0;
					cut_price[p][r][b] = 0;
				}
			}
		}

		doReset = false;
	}

	public static void autoLog(){
		Calendar time = Calendar.getInstance();

		if(time.get(Calendar.MINUTE) != 0){
			doPrint = true;
			return;
		}

		if(!doPrint)return;

		double highestValue[][] = new double[10][4];
		StringBuffer result = new StringBuffer(df2.format(last_price));
		for(int p = 0; p < percent.length; p++){
			for(int r = 0; r < rate.length; r++){
				for(int b = 0; b < btc.length; b++){
					double value = coins[p][r][b] * last_price + accounts[p][r][b];
					for(int k = 0; k < highestValue.length; k++){
						if(value > highestValue[k][0]){
							System.arraycopy(highestValue, k, highestValue, k + 1, highestValue.length - k - 1);
							highestValue[k] = new double[]{value, percent[p], rate[r], btc[b]};
							break;
						}
					}
					result.append("\t").append(percent[p]).append("-").append(rate[r]).append("-").append(btc[b]).append(":").append(df2.format(value));
				}
			}
		}
		//log(result);

		result = new StringBuffer(df2.format(last_price));
		for(int k = 0; k < highestValue.length; k++){
			result.append("\t").append(highestValue[k][1]).append("-").append(highestValue[k][2]).append("-").append(highestValue[k][3]).append(":").append(df2.format(highestValue[k][0]));
		}
		log(result.append("\r\n"));

		doPrint = false;
	}

	public static void autoTrade() throws Exception{
		while(true){
			if(reset)autoReset();

			autoLog();
			System.out.println(last_price);
			Thread.sleep(2000);

			JSONObject json = getOKTicker("btc_cny");
			double btc_cny = json.getDouble("last");
			double ok_btc_buy = json.getDouble("buy");
			double ok_btc_sell = json.getDouble("sell");

			last_price = btc_cny;

			if(ok_btc_buy >= ok_btc_sell){
				System.out.println("price error.");
				continue;
			}

			for(int p = 0; p < percent.length; p++){
				for(int r = 0; r < rate.length; r++){
					for(int b = 0; b < btc.length; b++){
						if(last_price < cut_price[p][r][b]){
							buy_price[p][r][b] = last_price * (100 + percent[p]) / 100;
							sell_price[p][r][b] = last_price * (100 - percent[p]) / 100;

							OKTrade(p, r, b, "sell", coins[p][r][b], ok_btc_buy);
							cut_price[p][r][b] = 0;
						}else if(last_price > buy_price[p][r][b]){
							buy_price[p][r][b] = last_price * (100 + percent[p]) / 100;
							sell_price[p][r][b] = last_price * (100 - percent[p]) / 100;

							OKTrade(p, r, b, "buy", btc[b], ok_btc_sell);
							cut_price[p][r][b] = sell_price[p][r][b];
						}else if(last_price < sell_price[p][r][b]){
							buy_price[p][r][b] = last_price * (100 + percent[p]) / 100;
							sell_price[p][r][b] = last_price * (100 - percent[p]) / 100;

							for(int i = 0; i < rate[r]; i++){
								if(OKTrade(p, r, b, "sell", btc[b], ok_btc_buy)){
									if(coins[p][r][b] == 0)cut_price[p][r][b] = 0;
								}else{
									cut_price[p][r][b] = 0;
								}
							}
						}
					}
				}
			}
		}
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = OKCoinDemo2.class.getResourceAsStream("config.properties");
		config.load(in);
		in.close();
		money = Double.parseDouble(config.getProperty("money"));
		reset = Boolean.parseBoolean(config.getProperty("reset"));
		reset_hour = Integer.parseInt(config.getProperty("reset_hour"));
		for(int p = 0; p < percent.length; p++){
			for(int r = 0; r < rate.length; r++){
				for(int b = 0; b < btc.length; b++){
					accounts[p][r][b] = money;
					coins[p][r][b] = 0;
					buy_price[p][r][b] = 0;
					sell_price[p][r][b] = 0;
					cut_price[p][r][b] = 0;
				}
			}
		}
	}

	public static void main(String[] args) throws Exception{
		System.out.println("Starting...");
		init();
		System.out.println("inited.");

		autoTrade();
	}
}