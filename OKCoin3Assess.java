
import java.text.DecimalFormat;

public class OKCoin3Assess {
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static double max = 2;
	private static double min = 0.5;
	private static double step;
	private static double coin;
	private static double cash;
	private static double balance;
	private static double price;
	
	private static void trade(){
		double assets = coin * price + cash;
		if(balance < assets * min)balance = (long)assets * min;
		if(balance > assets * max)balance = (long)assets * max;
		
		if(cash > step * price){
			double buy = balance / (coin + step);
			while(buy >= price && cash > step * price){
				coin += step;
				cash -= step * price;
				buy = balance / (coin + step);
			}
		}
		
		if(coin > step){
			double sell = balance / (coin - step);
			while(sell <= price && coin > step){
				coin -= step;
				cash += step * price;
				sell = balance / (coin - step);
			}
		}
	}
	
	public static void main(String[] args){
		for(step = 0.01; step <= 1.28; step *= 2){
			coin = 0;
			cash = 100000;
			balance = cash * 0.5;
			
			for(price = 4000; price <= 8000; price++)trade();
			
			double assets = coin * price + cash;
			System.out.print(df2.format(assets));
			
			for(price = 8000; price >= 4000; price--)trade();
			
			assets = coin * price + cash;
			System.out.println("   \t" + df2.format(assets));
		}
	}
}
