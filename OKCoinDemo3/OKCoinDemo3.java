
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

public class OKCoinDemo3 {
	private static FileWriter logger = null;
	private static double btc[] = new double[]{0.01, 0.02, 0.03, 0.05, 0.07, 0.1, 0.2, 0.3, 0.5, 0.7, 1, 2, 3, 5, 7};
	private static double balance = 100000;
	private static double money = 200000;
	private static boolean reset = true;
	private static int reset_hour = 9;
	private static double accounts[] = new double[btc.length];
	private static double coins[] = new double[btc.length];
	private static double price = 0;
	private static DecimalFormat df2 = new DecimalFormat("#.##");
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
			if(logger == null)logger = new FileWriter("OKCoinDemo3.txt", true);
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

	public static boolean OKTrade(int b, String type, double amount, double rate){
		if(type.equals("buy")){
			if(accounts[b] < (amount * rate))return false;
			accounts[b] -= amount * rate;
			coins[b] += amount;
		}else{
			if(coins[b] < amount)return false;
			coins[b] -= amount;
			accounts[b] += amount * rate;
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

		for(int b = 0; b < btc.length; b++){
			accounts[b] = money;
			coins[b] = 0;
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

		double highestValue[][] = new double[5][2];
		StringBuffer result = new StringBuffer(df2.format(price));
		for(int b = 0; b < btc.length; b++){
			double value = coins[b] * price + accounts[b];
			for(int k = 0; k < highestValue.length; k++){
				if(value > highestValue[k][0]){
					System.arraycopy(highestValue, k, highestValue, k + 1, highestValue.length - k - 1);
					highestValue[k] = new double[]{value, btc[b]};
					break;
				}
			}
			result.append("\t").append(btc[b]).append(":").append(df2.format(value));
		}
		//log(result);

		result = new StringBuffer(df2.format(price));
		for(int k = 0; k < highestValue.length; k++){
			result.append("\t").append(highestValue[k][1]).append(":").append(df2.format(highestValue[k][0]));
		}
		log(result.append("\r\n"));

		doPrint = false;
	}

	public static void autoTrade() throws Exception{
		while(true){
			if(reset)autoReset();

			autoLog();
			System.out.println(price);
			Thread.sleep(2000);

			JSONObject json = getOKTicker("btc_cny");
			double btc_cny = json.getDouble("last");
			double ok_btc_buy = json.getDouble("buy");
			double ok_btc_sell = json.getDouble("sell");

			price = btc_cny;

			if(ok_btc_buy >= ok_btc_sell){
				System.out.println("price error.");
				continue;
			}

			for(int b = 0; b < btc.length; b++){
				if((coins[b] + btc[b]) * ok_btc_sell < balance){
					OKTrade(b, "buy", btc[b], ok_btc_sell);
				}else if((coins[b] - btc[b]) * ok_btc_buy > balance){
					OKTrade(b, "sell", btc[b], ok_btc_buy);
				}
			}
		}
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = OKCoinDemo3.class.getResourceAsStream("config.properties");
		config.load(in);
		in.close();
		balance = Double.parseDouble(config.getProperty("balance"));
		money = Double.parseDouble(config.getProperty("money"));
		reset = Boolean.parseBoolean(config.getProperty("reset"));
		reset_hour = Integer.parseInt(config.getProperty("reset_hour"));
		for(int b = 0; b < btc.length; b++){
			accounts[b] = money;
			coins[b] = 0;
		}
	}

	public static void main(String[] args) throws Exception{
		System.out.println("Starting...");
		init();
		System.out.println("inited.");

		autoTrade();
	}
}