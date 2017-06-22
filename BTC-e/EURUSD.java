
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;

public class EURUSD {
	private static String api_url = "https://btc-e.com/tapi";
	private static String api_key = "xxx";
	private static String api_secret = "xxx";
	private static long nonce = -1;
	private static double btc_usd_fee = 0.002;
	private static double eur_usd_fee = 0.002;
	private static double btc_eur_fee = 0.002;
	private static FileWriter logger = null;

	public static String getNow(){
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}

	public static void log(Object text) throws Exception{
		if(logger == null)logger = new FileWriter("eur-usd.txt", true);
		logger.write(getNow() + ": " + String.valueOf(text));
		logger.write("\r\n");
		logger.flush();
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

	private static String HmacSHA512(String text) throws Exception{
		if(text == null)return "";
		SecretKeySpec key = new SecretKeySpec(api_secret.getBytes("UTF-8"), "HmacSHA512");
		Mac mac = Mac.getInstance("HmacSHA512");
		mac.init(key);
		byte[] sha512 = mac.doFinal(text.getBytes("UTF-8"));
		return byte2Hex(sha512);
	}

	private static String readStream(InputStream in) throws Exception{
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

	public static JSONObject getAPI(String data) throws Exception{
		data = "nonce=" + (++nonce) + "&" + data;
		URL url = new URL(api_url);
		HttpURLConnection con = (HttpURLConnection)url.openConnection();
		con.setReadTimeout(6000);
		con.setUseCaches(false);
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Key", api_key);
		con.setRequestProperty("Sign", HmacSHA512(data));
		try{
			con.setConnectTimeout(2000);
			con.connect();
		}catch(Exception e){
			try{
				con.setConnectTimeout(4000);
				con.connect();
			}catch(Exception ee){
				con.setConnectTimeout(6000);
				con.connect();
			}
		}
		con.getOutputStream().write(data.getBytes("UTF-8"));
		con.getOutputStream().flush();
		con.getOutputStream().close();
		String api = readStream(con.getInputStream());
		con.disconnect();
		return new JSONObject(api);
	}

	public static JSONObject getPublicAPI(String path) throws Exception{
		URL url = new URL(path);
		HttpURLConnection con = (HttpURLConnection)url.openConnection();
		con.setReadTimeout(6000);
		con.setUseCaches(false);
		con.setDoOutput(false);
		con.setDoInput(true);
		try{
			con.setConnectTimeout(2000);
			con.connect();
		}catch(Exception e){
			try{
				con.setConnectTimeout(4000);
				con.connect();
			}catch(Exception ee){
				con.setConnectTimeout(6000);
				con.connect();
			}
		}
		String api = readStream(con.getInputStream());
		con.disconnect();
		return new JSONObject(api);
	}

	public static void init() throws Exception{
		JSONObject json = getAPI("");
		String error = json.getString("error");
		nonce = Long.parseLong(error.substring(error.indexOf("key:") + 4, error.indexOf(",")));
		json = getPublicAPI("https://btc-e.com/api/2/btc_usd/fee");
		btc_usd_fee = json.getDouble("trade") / 100;
		json = getPublicAPI("https://btc-e.com/api/2/eur_usd/fee");
		eur_usd_fee = json.getDouble("trade") / 100;
		json = getPublicAPI("https://btc-e.com/api/2/btc_eur/fee");
		btc_eur_fee = json.getDouble("trade") / 100;
	}

	public static void main(String[] args) throws Exception{
		System.out.println("Starting...");
		init();

		while(true){
			JSONObject json = getAPI("method=getInfo").getJSONObject("return").getJSONObject("funds");
			double btc = json.getDouble("btc");
			double usd = json.getDouble("usd");
			double eur = json.getDouble("eur");

			JSONObject btc_usd_depth = getPublicAPI("https://btc-e.com/api/2/btc_usd/depth");
			JSONObject eur_usd_depth = getPublicAPI("https://btc-e.com/api/2/eur_usd/depth");
			JSONObject btc_eur_depth = getPublicAPI("https://btc-e.com/api/2/btc_eur/depth");

			double btc_usd_eur_btc = 1 * btc_usd_depth.getJSONArray("bids").getJSONArray(0).getDouble(0) * (1 - btc_usd_fee);
			btc_usd_eur_btc = btc_usd_eur_btc / eur_usd_depth.getJSONArray("asks").getJSONArray(0).getDouble(0) * (1 - eur_usd_fee);
			btc_usd_eur_btc = btc_usd_eur_btc / btc_eur_depth.getJSONArray("asks").getJSONArray(0).getDouble(0) * (1 - btc_eur_fee);

			double btc_eur_usd_btc = 1 * btc_eur_depth.getJSONArray("bids").getJSONArray(0).getDouble(0) * (1 - btc_eur_fee);
			btc_eur_usd_btc = btc_eur_usd_btc * eur_usd_depth.getJSONArray("bids").getJSONArray(0).getDouble(0) * (1 - eur_usd_fee);
			btc_eur_usd_btc = btc_eur_usd_btc / btc_usd_depth.getJSONArray("asks").getJSONArray(0).getDouble(0) * (1 - btc_usd_fee);

			System.out.println(btc_usd_eur_btc);log(btc_usd_eur_btc);
			System.out.println(btc_eur_usd_btc);log(btc_eur_usd_btc);
			System.out.println();log("");
			Thread.sleep(1000);
		}
	}
}