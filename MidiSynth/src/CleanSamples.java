import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class CleanSamples {

	static final String file = "crowd1.txt";
	
	public static void main(String [] args){
		new CleanSamples(file, 5);
	}

	public static void quick_sort(int[] data, int low, int high)
	{  // 1 or 0 items are sorted by default
		if(high - low < 1)
			return;

		int left = low;
		int right = high;
		int pivot = data[low + (high - low) / 2];  

		while(left <= right)
		{  // Increment left pointer until left >= pivot
			while(data[left] < pivot)
				left++;

			// Increment right pointer until right <= pivot
			while(data[right] > pivot)
				right--;

			// If left < right; swap values
			if(left <= right)
			{  int temp = data[left];
			data[left] = data[right];
			data[right] = temp;
			left++;
			right--;
			}

		}
		// quick_sort 'lesser values'
		quick_sort(data, low, right);

		// quick_sort 'greater values'
		quick_sort(data, left, high);
	}

	public CleanSamples(String file, int minRating) {
		ArrayList<Long> rateList = new ArrayList<Long>();
		ArrayList<Integer> ratings = new ArrayList<Integer>();
		String newFile = file.substring(0, file.lastIndexOf('.'))+"_sort."+file.substring(file.lastIndexOf('.')+1);
		try {
			Scanner in = new Scanner(new FileReader(file));
			while(in.hasNextLine()){
				rateList.add(in.nextLong());
				ratings.add(in.nextInt());
			}
			sortLoops(ratings, rateList, 0, rateList.size()-1);
			PrintWriter out = new PrintWriter(new FileOutputStream(newFile));
			for (int i = 0; i < ratings.size(); i++) {
				int count = 1;
				while (i+1<rateList.size() && rateList.get(i+1).equals(rateList.get(i))) {
					ratings.set(i, ratings.get(i) + ratings.remove(i+1));
					rateList.remove(i+1);
					count++;
				}
				ratings.set(i, ratings.get(i)/count);
				if (ratings.get(i) >= minRating)
					out.println(rateList.get(i) + " " + ratings.get(i));
			}
			out.close();
			System.out.println(ratings);
		} catch (FileNotFoundException e) {

		}
	}

	protected static void sortLoops(ArrayList<Integer> rating, ArrayList<Long> seed, int base, int top){
		int low = base;
		int high = top;
		if (high - low < 1)
			return;
		double pivot = seed.get((low + high) / 2);
		while (low <= high) {
			while (seed.get(low) < pivot)
				low++;
			while (seed.get(high) > pivot)
				high--;
			if (low <= high) {
				int temp = rating.get(low);
				rating.set(low, rating.get(high));
				rating.set(high, temp);
				long temp2 = seed.get(low);
				seed.set(low, seed.get(high));
				seed.set(high, temp2);
				low++;
				high--;
			}
		}
		sortLoops(rating, seed, base, high);
		sortLoops(rating, seed, low, top);

	}
}