
import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SuppressWarnings("unchecked")
public class OKCoin3DynamicAssess {
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static double step = 0.01;
	private static int[] dynamic = new int[]{1, 1, 1, 1, 1, 1};
	private static int[] dynamicMode = new int[]{1, 2, 3, 4, 6, 8};
	private static int[] dynamicCount = new int[dynamic.length];
	private static double[] cash = new double[dynamic.length];
	private static double[] coin = new double[dynamic.length];
	private static double[] balance = new double[dynamic.length];
	private static List<Integer>[] dealHistory = new List[dynamic.length];
	private static int[] dealCount = new int[dynamic.length];
	private static int[] dealCountH = new int[dynamic.length];
	private static int[] dealCountL = new int[dynamic.length];
	private static int[] dealCountLL = new int[dynamic.length];
	private static int[] dynamicH = new int[dynamic.length];
	private static int[] dynamicL = new int[dynamic.length];
	private static int[] dynamicLL = new int[dynamic.length];
	private static double[] coinH = new double[dynamic.length];
	private static double[] coinL = new double[dynamic.length];
	private static double[] coinLL = new double[dynamic.length];
	private static double[] cashH = new double[dynamic.length];
	private static double[] cashL = new double[dynamic.length];
	private static double[] cashLL = new double[dynamic.length];
	private static long[] lastDeal = new long[dynamic.length];
	private static Scanner scanner;
	private static long time;
	private static double price;
	
	private static void bootTrade(int i){
		double assets = coin[i] * price + cash[i];
		if(balance[i] < assets / 2)balance[i] = (long)assets / 2;
		
		dealHistory[i] = new ArrayList<Integer>();
		
		dynamicH[i] = dynamic[i] * 2;
		dynamicL[i] = dynamic[i] / 2;
		if(dynamicL[i] < 1)dynamicL[i] = 1;
		dynamicLL[i] = dynamicL[i] / 2;
		if(dynamicLL[i] < 1)dynamicLL[i] = 1;
		coinH[i] = coin[i];
		coinL[i] = coin[i];
		coinLL[i] = coin[i];
		cashH[i] = cash[i];
		cashL[i] = cash[i];
		cashLL[i] = cash[i];
		lastDeal[i] = time;
	}
	
	public static void autoTrade() throws Exception{
		for(int i = 0; i < dynamic.length; i++){
			bootTrade(i);
		}
		
		boolean[] changeL = new boolean[dynamic.length];
		boolean[] changeH = new boolean[dynamic.length];
		while(scanner.hasNextLine()){
			String line = scanner.nextLine();
			if(line.contains("Changing"))continue;
			time = sdf.parse(line.substring(0, 23)).getTime();
			price = Double.parseDouble(line.substring(24, 36).trim());
			for(int i = 0; i < dynamic.length; i++){
				if(dynamicMode[i] > 0){
					if(changeL[i] && dynamic[i] > 1){
						dynamicCount[i]++;
						dynamic[i] = dynamic[i] / 2;

						bootTrade(i);
					}else if(changeH[i]){
						dynamicCount[i]++;
						dynamic[i] = dynamic[i] * 2;

						bootTrade(i);
					}
				}
				
				dealHistory[i].add(0, 0);
				if(cash[i] > step * dynamic[i] * price){
					double buy_price = balance[i] / (coin[i] + step * dynamic[i]);
					buy_price = Double.parseDouble(df2.format(buy_price));
					if(buy_price >= price){
						coin[i] += step * dynamic[i];
						cash[i] -= step * dynamic[i] * price;
						int value = dealHistory[i].get(0);
						dealHistory[i].set(0, value + 4);
					}
				}
				
				if(coin[i] > step * dynamic[i]){
					double sell_price = balance[i] / (coin[i] - step * dynamic[i]) + 0.01;
					sell_price = Double.parseDouble(df2.format(sell_price));
					if(sell_price <= price){
						coin[i] -= step * dynamic[i];
						cash[i] += step * dynamic[i] * price;
						int value = dealHistory[i].get(0);
						dealHistory[i].set(0, value + 4);
					}
				}
				
				if(cashH[i] > step * dynamicH[i] * price){
					double buyH = balance[i] / (coinH[i] + step * dynamicH[i]);
					buyH = Double.parseDouble(df2.format(buyH));
					if(buyH > price){
						coinH[i] += step * dynamicH[i];
						cashH[i] -= step * dynamicH[i] * price;
						int value = dealHistory[i].get(0);
						dealHistory[i].set(0, value + 8);
					}
				}
				
				if(coinH[i] > step * dynamicH[i]){
					double sellH = balance[i] / (coinH[i] - step * dynamicH[i]) + 0.01;
					sellH = Double.parseDouble(df2.format(sellH));
					if(sellH < price){
						coinH[i] -= step * dynamicH[i];
						cashH[i] += step * dynamicH[i] * price;
						int value = dealHistory[i].get(0);
						dealHistory[i].set(0, value + 8);
					}
				}
				
				if(cashL[i] > step * dynamicL[i] * price){
					double buyL = balance[i] / (coinL[i] + step * dynamicL[i]);
					buyL = Double.parseDouble(df2.format(buyL));
					if(buyL > price){
						coinL[i] += step * dynamicL[i];
						cashL[i] -= step * dynamicL[i] * price;
						int value = dealHistory[i].get(0);
						dealHistory[i].set(0, value + 2);
					}
				}
				
				if(coinL[i] > step * dynamicL[i]){
					double sellL = balance[i] / (coinL[i] - step * dynamicL[i]) + 0.01;
					sellL = Double.parseDouble(df2.format(sellL));
					if(sellL < price){
						coinL[i] -= step * dynamicL[i];
						cashL[i] += step * dynamicL[i] * price;
						int value = dealHistory[i].get(0);
						dealHistory[i].set(0, value + 2);
					}
				}
				
				if(cashLL[i] > step * dynamicLL[i] * price){
					double buyLL = balance[i] / (coinLL[i] + step * dynamicLL[i]);
					buyLL = Double.parseDouble(df2.format(buyLL));
					if(buyLL > price){
						coinLL[i] += step * dynamicLL[i];
						cashLL[i] -= step * dynamicLL[i] * price;
						int value = dealHistory[i].get(0);
						dealHistory[i].set(0, value + 1);
					}
				}
				
				if(coinLL[i] > step * dynamicLL[i]){
					double sellLL = balance[i] / (coinLL[i] - step * dynamicLL[i]) + 0.01;
					sellLL = Double.parseDouble(df2.format(sellLL));
					if(sellLL < price){
						coinLL[i] -= step * dynamicLL[i];
						cashLL[i] += step * dynamicLL[i] * price;
						int value = dealHistory[i].get(0);
						dealHistory[i].set(0, value + 1);
					}
				}
				
				int value = dealHistory[i].get(0);
				if(value == 0){
					dealHistory[i].remove(0);
				}else{
					lastDeal[i] = time;
				}
				while(dealHistory[i].size() > 4096){
					dealHistory[i].remove(dealHistory[i].size() - 1);
				}
				
				dealCount[i] = 0;
				dealCountH[i] = 0;
				dealCountL[i] = 0;
				dealCountLL[i] = 0;
				changeL[i] = false;
				changeH[i] = false;
				for(int deal : dealHistory[i]){
					if(deal >= 8){
						dealCountH[i]++;
						deal -= 8;
					}
					if(deal >= 4){
						dealCount[i]++;
						deal -= 4;
					}
					if(deal >= 2){
						dealCountL[i]++;
						deal -= 2;
					}
					if(deal == 1)dealCountLL[i]++;

					if(dealCountLL[i] >= 16 * dynamicMode[i] && (double)dealCount[i] / dealCountLL[i] < 0.0625){
						changeL[i] = true;
						break;
					}
					if(dealCountL[i] >= 4 * dynamicMode[i] && (double)dealCount[i] / dealCountL[i] < 0.25){
						changeL[i] = true;
						break;
					}
					if(dealCountH[i] >= 2 * dynamicMode[i] && dealCount[i] > 0 && (double)dealCountH[i] / dealCount[i] > 0.25){
						changeH[i] = true;
						break;
					}
				}
				if(time - lastDeal[i] > 1000 * 60 * 60)changeL[i] = true;
			}
		}
	}
	
	public static void main(String[] args) throws Exception{
		scanner = new Scanner(new FileInputStream("OKCoin.txt"));
		String line = scanner.nextLine();
		time = sdf.parse(line.substring(0, 23)).getTime();
		price = Double.parseDouble(line.substring(24, 36).trim());
		
		for(int i = 0; i < dynamic.length; i++){
			balance[i] = 100000 * 0.75;
			cash[i] = 100000 * 0.25;
			coin[i] = Double.parseDouble(df2.format(balance[i] / price));
		}
		
		autoTrade();
		scanner.close();
		
		for(int i = 0; i < dynamic.length; i++){
			double assets = coin[i] * price + cash[i];
			System.out.println(dynamic[i] + "\t" + dynamicCount[i] + "\t" + assets);
		}
	}
}