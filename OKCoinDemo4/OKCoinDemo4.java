
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

public class OKCoinDemo4 {
	private static FileWriter logger = null;
	private static double money = 10000;
	private static boolean reset = true;
	private static int reset_hour = 9;
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static double percent[] = new double[]{0.1, 0.2, 0.3, 0.5, 0.7, 1, 2, 3, 5, 7};
	private static double btc[] = new double[]{0.01, 0.02, 0.03, 0.05, 0.07, 0.1, 0.2, 0.3, 0.5, 0.7, 1};
	private static int circles[] = new int[]{5, 10, 15, 20, 30, 60, 90, 120, 150, 300, 450, 600, 900, 1800, 2700, 3600};
	private static double accounts[][][][][] = new double [circles.length][circles.length][percent.length][btc.length][2];
	private static double coins[][][][][] = new double [circles.length][circles.length][percent.length][btc.length][2];
	private static int longCircle = circles[circles.length - 1];
	private static double prices[] = new double[longCircle + 1];
	private static boolean buy_signal[] = new boolean[circles.length];
	private static boolean sell_signal[] = new boolean[circles.length];
	private static double buy_price[][][][][] = new double [circles.length][circles.length][percent.length][btc.length][2];
	private static double sell_price[][][][][] = new double [circles.length][circles.length][percent.length][btc.length][2];
	private static double cut_price[][][][][] = new double [circles.length][circles.length][percent.length][btc.length][2];
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
			if(logger == null)logger = new FileWriter("OKCoinDemo4.txt", true);
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

	public static boolean OKTrade(int i, int j, int p, int b, int d, String type, double amount, double rate){
		if(type.equals("buy")){
			if(accounts[i][j][p][b][d] < (amount * rate))return false;
			accounts[i][j][p][b][d] -= amount * rate;
			coins[i][j][p][b][d] += amount;
		}else{
			if(coins[i][j][p][b][d] < amount)return false;
			coins[i][j][p][b][d] -= amount;
			accounts[i][j][p][b][d] += amount * rate;
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

		for(int i = 0; i < circles.length; i++){
			for(int j = 0; j < circles.length; j++){
				for(int p = 0; p < percent.length; p++){
					for(int b = 0; b < btc.length; b++){
						accounts[i][j][p][b][0] = money;
						coins[i][j][p][b][0] = 0;
						buy_price[i][j][p][b][0] = 0;
						sell_price[i][j][p][b][0] = 0;
						cut_price[i][j][p][b][0] = 0;

						accounts[i][j][p][b][1] = money;
						coins[i][j][p][b][1] = 0;
						buy_price[i][j][p][b][1] = 0;
						sell_price[i][j][p][b][1] = 0;
						cut_price[i][j][p][b][1] = 0;
					}
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

		double highestValue[][] = new double[10][5];
		double highestValue2[][] = new double[10][5];
		StringBuffer result = new StringBuffer(df2.format(prices[0]));
		StringBuffer result2 = new StringBuffer(df2.format(prices[0]));
		for(int i = 0; i < circles.length; i++){
			for(int j = i; j < circles.length; j++){
				for(int p = 0; p < percent.length; p++){
					for(int b = 0; b < btc.length; b++){
						double value = coins[i][j][p][b][0] * prices[0] + accounts[i][j][p][b][0];
						for(int k = 0; k < highestValue.length; k++){
							if(value > highestValue[k][0]){
								System.arraycopy(highestValue, k, highestValue, k + 1, highestValue.length - k - 1);
								highestValue[k] = new double[]{value, circles[i], circles[j], percent[p], btc[b]};
								break;
							}
						}
						result.append("\t").append(circles[i]).append("-").append(circles[j]).append("-").append(percent[p]).append("-").append(btc[b]).append(":").append(df2.format(value));

						double value2 = coins[i][j][p][b][1] * prices[0] + accounts[i][j][p][b][1];
						for(int k = 0; k < highestValue2.length; k++){
							if(value2 > highestValue2[k][0]){
								System.arraycopy(highestValue2, k, highestValue2, k + 1, highestValue2.length - k - 1);
								highestValue2[k] = new double[]{value2, circles[i], circles[j], percent[p], btc[b]};
								break;
							}
						}
						result2.append("\t").append(circles[i]).append("-").append(circles[j]).append("-").append(percent[p]).append("-").append(btc[b]).append(":").append(df2.format(value2));
					}
				}
			}
		}
		//log(result);
		//log(result2);

		result = new StringBuffer(df2.format(prices[0]));
		for(int k = 0; k < highestValue.length; k++){
			result.append("\t").append(highestValue[k][1]).append("-").append(highestValue[k][2]).append("-").append(highestValue[k][3]).append("-").append(highestValue[k][4]).append(":").append(df2.format(highestValue[k][0]));
		}
		log(result);

		result2 = new StringBuffer(df2.format(prices[0]));
		for(int k = 0; k < highestValue2.length; k++){
			result2.append("\t").append(highestValue2[k][1]).append("-").append(highestValue2[k][2]).append("-").append(highestValue2[k][3]).append("-").append(highestValue2[k][4]).append(":").append(df2.format(highestValue2[k][0]));
		}
		log(result2.append("\r\n"));

		doPrint = false;
	}

	public static void autoTrade() throws Exception{
		while(true){
			if(reset)autoReset();

			autoLog();
			System.out.println(prices[0]);
			Thread.sleep(2000);

			JSONObject json = getOKTicker("btc_cny");
			double btc_cny = json.getDouble("last");
			double ok_btc_buy = json.getDouble("buy");
			double ok_btc_sell = json.getDouble("sell");

			System.arraycopy(prices, 0, prices, 1, prices.length - 1);
			prices[0] = btc_cny;

			if(ok_btc_buy >= ok_btc_sell){
				System.out.println("price error.");
				continue;
			}

			for(int i = 0; i < circles.length; i++){
				buy_signal[i] = true;
				sell_signal[i] = true;
			}
			for(int i = 1; i < prices.length; i++){
				for(int j = 0; j < circles.length; j++){
					if(i <= circles[j] && prices[i] >= prices[0])buy_signal[j] = false;
					if(i <= circles[j] && prices[i] <= prices[0])sell_signal[j] = false;
				}
			}

			for(int i = 0; i < circles.length; i++){
				for(int j = i; j < circles.length; j++){
					for(int p = 0; p < percent.length; p++){
						for(int b = 0; b < btc.length; b++){
							if(sell_signal[i]){
								buy_price[i][j][p][b][0] = prices[0] * (100 + percent[p]) / 100;
								sell_price[i][j][p][b][0] = prices[0] * (100 - percent[p]) / 100;

								OKTrade(i, j, p, b, 0, "sell", coins[i][j][p][b][0], ok_btc_buy);
							}else if(buy_signal[j] && prices[0] > buy_price[i][j][p][b][0]){
								buy_price[i][j][p][b][0] = prices[0] * (100 + percent[p]) / 100;
								sell_price[i][j][p][b][0] = prices[0] * (100 - percent[p]) / 100;

								OKTrade(i, j, p, b, 0, "buy", btc[b], ok_btc_sell);
							}else if(prices[0] < sell_price[i][j][p][b][0]){
								buy_price[i][j][p][b][0] = prices[0] * (100 + percent[p]) / 100;
								sell_price[i][j][p][b][0] = prices[0] * (100 - percent[p]) / 100;

								OKTrade(i, j, p, b, 0, "sell", btc[b], ok_btc_buy);
							}

							if(sell_signal[i] || prices[0] < cut_price[i][j][p][b][1]){
								buy_price[i][j][p][b][1] = prices[0] * (100 + percent[p]) / 100;
								sell_price[i][j][p][b][1] = prices[0] * (100 - percent[p]) / 100;

								OKTrade(i, j, p, b, 1, "sell", coins[i][j][p][b][1], ok_btc_buy);
								cut_price[i][j][p][b][1] = 0;
							}else if(buy_signal[j] && prices[0] > buy_price[i][j][p][b][1]){
								buy_price[i][j][p][b][1] = prices[0] * (100 + percent[p]) / 100;
								sell_price[i][j][p][b][1] = prices[0] * (100 - percent[p]) / 100;

								OKTrade(i, j, p, b, 1, "buy", btc[b], ok_btc_sell);
								cut_price[i][j][p][b][1] = prices[0] / (100 + percent[p] * (coins[i][j][p][b][1] / btc[b] - 1) / 2) * (100 - percent[p] / (coins[i][j][p][b][1] / btc[b]));
							}
						}
					}
				}
			}
		}
	}

	public static void init() throws Exception{
		Properties config = new Properties();
		InputStream in = OKCoinDemo4.class.getResourceAsStream("config.properties");
		config.load(in);
		in.close();
		money = Double.parseDouble(config.getProperty("money"));
		reset = Boolean.parseBoolean(config.getProperty("reset"));
		reset_hour = Integer.parseInt(config.getProperty("reset_hour"));
		for(int i = 0; i < circles.length; i++){
			for(int j = 0; j < circles.length; j++){
				for(int p = 0; p < percent.length; p++){
					for(int b = 0; b < btc.length; b++){
						accounts[i][j][p][b][0] = money;
						coins[i][j][p][b][0] = 0;
						buy_price[i][j][p][b][0] = 0;
						sell_price[i][j][p][b][0] = 0;
						cut_price[i][j][p][b][0] = 0;

						accounts[i][j][p][b][1] = money;
						coins[i][j][p][b][1] = 0;
						buy_price[i][j][p][b][1] = 0;
						sell_price[i][j][p][b][1] = 0;
						cut_price[i][j][p][b][1] = 0;
					}
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